package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PAPER HALF OF STEP R8, PINNED ON THE SOURCE (PLAN-dragon-mobs.md; the core half is MobPounceTest, PounceCycleTest
 * and MobBlinkCooldownTest). These read lines, not behaviour: GATE-dragon-skreel.md's rows are their first server
 * reading. Comment lines are skipped, so a javadoc that quotes a line cannot satisfy its needle.
 */
class DragonSkreelWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    // --- the pair is one spawn ------------------------------------------------------------------------------------------

    /** /rpg spawn's one path spawns the mount after the rider, with the rider's score and the same pre-add, then pairs. */
    @Test
    void theOneSpawnPathSpawnsTheMountWithTheRidersScoreAndPairsThem() throws IOException {
        List<String> code = file("command", "RpgCommand.java");
        int tail = indexOf(code, "if (mountDef != null) spawnMount(player, spawned, mountDef, mountType, stored, adapters, preAdd);");
        int ret = indexAfter(code, tail, "return Optional.of(spawned);");
        assertTrue(tail > 0 && ret == tail + 1, "the mount after the rider, before the return: " + tail + " " + ret);
        int m = indexOf(code, "private static void spawnMount(");
        int stamp = indexAfter(code, m, "MobSpawner.stampCustom(entity, mountDef, 1, adapters.keys());");
        int score = indexAfter(code, stamp, ".set(adapters.keys().mobGearScore, PersistentDataType.INTEGER, riderScore);");
        // The mount's score IS the rider's whenever the rider has one (a hostile rider always does): the guard is only
        // the null of a passive rider. A mutation that skips it (the mount rolling its own) survived every row until this
        // pin; the gate's server reading is PS1, spawned at 300 where the overworld rolls 100.
        assertTrue(score > 0 && code.get(score - 2).trim().equals("if (riderScore != null) {"), "the guard: " + score);
        int pre = indexAfter(code, score, "preAdd.accept(entity);");
        // THE MARK INSIDE THE CONSUMER, before the add event, or the add event finds no pair and the loop never starts.
        int mark = indexAfter(code, pre, "MobPouncer.mark(entity, rider.getUniqueId(), adapters.keys());");
        int close = indexAfter(code, pre, "});");
        int pair = indexAfter(code, pre, "MobPouncer.pair((org.bukkit.entity.Mob) mount,");
        assertTrue(m > 0 && stamp > m && score > stamp && pre > score && mark > pre && close > mark && pair > close,
                "stamp, the rider's score, the pre-add, the mark, inside the consumer, then the pair: " + stamp + " "
                        + score + " " + pre + " " + mark + " " + close + " " + pair);
        List<String> p = file("health", "MobPouncer.java");
        int appear = indexOf(p, "public void onAppear(Entity entity) {");
        assertTrue(indexAfter(p, appear, "if (!pdc.has(adapters.keys().mountRider) && !pdc.has(adapters.keys().mountWidowed)) return;")
                == appear + 3, "the add event looks for the mark");
    }

    // --- the ridden speed: transient, never a base ---------------------------------------------------------------------

    @Test
    void theRiddenSpeedAndTheRootAreTransientModifiersAndNothingSetsABase() throws IOException {
        List<String> code = file("health", "MobPouncer.java");
        assertTrue(indexOf(code, "speed.addTransientModifier(new AttributeModifier(keys.mountRidden, mountDef.riddenSpeed() - 1.0,") > 0,
                "the ridden 1.3x is transient, of the base");
        assertTrue(indexOf(code, "AttributeModifier.Operation.ADD_SCALAR));") > 0, "ADD_SCALAR: a multiple of the base");
        assertTrue(indexOf(code, "speed.addTransientModifier(new AttributeModifier(adapters.keys().pounceRoot, -1.0,") > 0,
                "the root is transient");
        assertTrue(code.stream().noneMatch(l -> l.contains("setBaseValue(")), "nothing in the pair writes a base");
        assertTrue(code.stream().noneMatch(l -> l.contains("addModifier(")), "no saved modifier");
        // The mount's file no longer authors a base speed: R1's permanent 1.3x is gone.
        List<String> mount = Files.readAllLines(Path.of("src", "main", "resources", "content", "mobs", "withered_mount.yml"));
        assertTrue(mount.stream().noneMatch(l -> l.startsWith("speed:")), "no base speed on the mount");
        assertTrue(mount.stream().anyMatch(l -> l.equals("ridden_speed: 1.3")), "the ridden speed: " + mount);
    }

    // --- the loop dies with the pair -------------------------------------------------------------------------------------

    @Test
    void theLoopRunsOnTheMountsSchedulerAndEndsWithEitherHalf() throws IOException {
        List<String> code = file("health", "MobPouncer.java");
        assertTrue(indexOf(code, "p.task = RepeatingTask.start(new EntityTaskTarget(mount, adapters.scheduler(), adapters.log()), 1, \"skreel-pounce\",") > 0,
                "a loop of period 1 on the MOUNT'S entity scheduler");
        int step = indexOf(code, "private boolean step(Pair p) {");
        assertTrue(indexAfter(code, step, "if (rider.isDead()) broke = \"rider-died\";") == step + 4, "the rider's death first");
        assertTrue(indexAfter(code, step, "else if (!mount.getPassengers().contains(rider)) broke = \"separated\";") == step + 6,
                "then a separation");
        int brk = indexAfter(code, step, "if (broke != null) {");
        assertTrue(indexAfter(code, brk, "return false;") <= brk + 6, "a broken pair ends the loop");
        int rm = indexOf(code, "public void onRemove(UUID id) {");
        assertTrue(indexAfter(code, rm, "if (p != null) p.task.cancel();") == rm + 2, "the mount's removal cancels it");
        List<String> l = file("listener", "RpgListeners.java");
        assertTrue(indexOf(l, "pounces.onAppear(mob);") > 0 && indexOf(l, "pounces.onRemove(mob.getUniqueId());") > 0,
                "the add and remove events reach it");
    }

    @Test
    void aDeadMountsRiderDismountsWhereItStands() throws IOException {
        List<String> code = file("health", "MobPouncer.java");
        int stop = indexOf(code, "private void stopped(Pair p) {");
        int cond = indexAfter(code, stop, "if (mountDead && !p.rider.isDead() && p.rider.isValid() && p.mount.getPassengers().contains(p.rider)) {");
        assertTrue(stop > 0 && cond > stop && code.get(cond + 1).contains("p.mount.removePassenger(p.rider);"),
                "the rider leaves a dead mount: " + cond);
    }

    // --- no vanilla target while paired -----------------------------------------------------------------------------------

    @Test
    void aPairedMobsTargetEventsAreCancelled() throws IOException {
        List<String> l = file("listener", "RpgListeners.java");
        int h = indexOf(l, "public void onPairTargeted(org.bukkit.event.entity.EntityTargetEvent event) {");
        assertTrue(h > 0 && l.get(h + 1).contains("if (event.getTarget() != null && pounces.blocksTarget(event.getEntity())) event.setCancelled(true);"),
                "the target block: " + h);
        List<String> code = file("health", "MobPouncer.java");
        int start = indexOf(code, "private void start(Mob mount, Mob rider, MobDefinition riderDef, MobDefinition mountDef) {");
        assertTrue(indexAfter(code, start, "paired.add(rider.getUniqueId());") > start, "both halves are blocked");
        int stop = indexOf(code, "private void stopped(Pair p) {");
        assertTrue(indexAfter(code, stop, "paired.remove(p.rider.getUniqueId());") == stop + 3, "and released at its end");
    }

    /**
     * THE PAIR ENDS IN ITS BAND FROM ANY BEARING (step R8, the seat's ruling after its second diff): every tick it is
     * outside the band (CLOSE_IN), its goal is recomputed from where he stands NOW by {@code MobPounce.goal}, with the
     * stopping tolerance of the mount's own width, and handed to the pathfinder. Never the bare stand-off point.
     */
    @Test
    void outsideTheBandEveryTickItPathsToTheBlockWhoseStopLiesInTheBand() throws IOException {
        List<String> code = file("health", "MobPouncer.java");
        int c = indexOf(code, "case CLOSE_IN -> {");
        assertTrue(c > 0 && code.get(c + 1).trim().equals(
                "Vec3 to = p.pounce.goal(vec(target.getLocation()), feet, MobPounce.waypointTolerance(mount.getWidth()));"),
                "the goal, from his feet now and the mount's width: " + c);
        assertTrue(code.get(c + 2).contains("mount.getPathfinder().moveTo(new Location(mount.getWorld(), to.x(), to.y(), to.z()), p.pounce.pathSpeed());"),
                "handed to the pathfinder the same tick");
        assertTrue(code.stream().noneMatch(l -> l.contains(".standOff(")), "the bare stand-off point is never a goal");
    }

    // --- the hit: the shared final call and the blast's push ------------------------------------------------------------

    @Test
    void theLandingHitsThroughTheSharedFinalCallAndPushesByVelocity() throws IOException {
        List<String> code = file("health", "MobPouncer.java");
        int run = indexOf(code, "Runnable strike = () -> {");
        assertTrue(code.get(run + 1).contains("BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, dealer);"),
                "the mob-hit path's final call, the rider the dealer");
        assertTrue(code.get(run + 2).contains("victim.setVelocity(victim.getVelocity().add(velocity));"), "the blast's route");
        assertTrue(indexOf(code, "var attr = victim.getAttribute(Attribute.KNOCKBACK_RESISTANCE);") > 0, "cut by resistance");
        assertTrue(indexOf(code, "double dealt = p.pounce.price(MobClassifier.isHostile(p.rider), gs);") > 0,
                "priced by MobScaling.attackDamage through MobPounce.price");
    }

    // --- the Phalanx's cooldown ----------------------------------------------------------------------------------------

    @Test
    void theBlinkCooldownCountsFirstBlocksOnlyItsOwnBlinkAndStartsAtALanding() throws IOException {
        List<String> code = file("health", "MobShooter.java");
        int step = indexOf(code, "private void blinkStep(Owner o) {");
        int tick = indexAfter(code, step, "o.cooldown.tick();");
        int forced = indexAfter(code, step, "if (o.forced != null) {");
        int ai = indexAfter(code, forced, "if (!mob.hasAI() || mob.isInvisible()) return;");
        int cd = indexAfter(code, forced, "if (o.cooldown.running()) return;");
        assertTrue(step > 0 && tick > step && forced > tick && cd == ai + 1,
                "counted first, the force before it, then the block: " + tick + " " + forced + " " + ai + " " + cd);
        int inv = indexOf(code, "mob.setInvisible(true);");
        int landed = indexAfter(code, inv, "o.cooldown.landed();");
        int refused = indexOf(code, "if (!mob.teleport(to)) {");
        assertTrue(refused > 0 && inv > refused && landed > inv && landed <= inv + 2,
                "started after the teleport succeeded: " + refused + " " + inv + " " + landed);
        assertTrue(code.stream().filter(l -> l.contains("o.cooldown.landed();")).count() == 1, "and nowhere else");
    }

    /** R3: the decision is anchored on core's whole-block rule, computed from its feet and passed straight in. */
    @Test
    void theBlinkIsDecidedFromTheWholeBlockItsFeetAreIn() throws IOException {
        List<String> code = file("health", "MobShooter.java");
        int a = indexOf(code, "double anchor = MobBlink.anchor(from.getY());");
        int d = indexAfter(code, a, "MobBlink.Decision d = o.blink.decide(vec(from), anchor, draws, MobTeleporter.footing(mob, null));");
        assertTrue(a > 0 && d == a + 1, "the anchor, then the decision from it: " + a + " " + d);
    }

    /**
     * BEN'S 12-24 BLINK (step R8, after the boot of 45bc33ce): from a blink that LANDS until its next shot, whom the
     * Phalanx may fire at is measured by {@code MobShot.inChooseRange(…, blinked)}, out to the re-acquire range; its
     * vanilla target and its own search alike. Set at the landing, beside the cooldown; cleared by the fire.
     */
    @Test
    void aBlinkedPhalanxChoosesOutToTheReacquireRangeUntilItsNextShot() throws IOException {
        List<String> code = file("health", "MobShooter.java");
        assertTrue(indexOf(code, "LivingEntity target = choose(mob, o.shot, eye, o.blinked);") > 0, "the attempt passes it");
        int c = indexOf(code, "private LivingEntity choose(Mob mob, MobShot shot, Vec3 eye, boolean blinked) {");
        assertTrue(c > 0 && code.get(c + 2).contains("shot.inChooseRange(eye, vec(vanilla.getEyeLocation()), blinked)"),
                "the vanilla target, by the same range: " + c);
        int r = indexAfter(code, c, "double r = shot.chooseRange(blinked);");
        assertTrue(r == c + 5, "the search's radius: " + r);
        assertTrue(indexAfter(code, r, "shot.inChooseRange(eye, vec(l.getEyeLocation()), blinked)") <= r + 3, "and its test");
        assertTrue(code.stream().noneMatch(l -> l.contains("inAcquireRange(")), "no fire site left on the bare acquire range");
        int landed = indexOf(code, "o.cooldown.landed();");
        assertTrue(code.get(landed + 1).contains("o.blinked = true;"), "set where the cooldown starts: a blink that LANDS");
        int added = indexOf(code, "o.shots.add(s);");
        assertTrue(code.get(added + 1).contains("o.blinked = false;"), "cleared by the fire");
        assertTrue(code.stream().filter(l -> l.contains("o.blinked = ")).count() == 2, "and nowhere else");
        // The SHOT none line names the range it searched (PB4's second shot reads range=30.0; PNONERANGE survived until
        // this needle).
        int none = indexOf(code, "if (forced) adapters.log().info(MobShotLines.none(o.mobId, mob.getUniqueId(), Bukkit.getCurrentTick(),");
        assertTrue(none > 0 && code.get(none + 1).trim().equals("o.shot.chooseRange(o.blinked)));"), "the none line's range: " + none);
    }

    // --- helpers --------------------------------------------------------------------------------------------------------

    private static List<String> file(String... parts) throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("", parts)));
        assertTrue(code.size() > 20, "must have read " + String.join("/", parts) + ", read " + code.size());
        return code;
    }

    private static int indexOf(List<String> code, String needle) {
        return indexAfter(code, -1, needle);
    }

    private static int indexAfter(List<String> code, int from, String needle) {
        for (int i = from + 1; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    private static List<String> codeLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(l -> {
                    String t = l.trim();
                    return !(t.startsWith("//") || t.startsWith("*") || t.startsWith("/*"));
                })
                .toList();
    }
}
