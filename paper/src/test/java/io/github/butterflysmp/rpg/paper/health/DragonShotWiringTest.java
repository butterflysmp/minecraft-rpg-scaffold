package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHALANX'S SHOT IS A COMBATANT ONLY WHERE IT MUST BE (dragon-mobs step R7a, the seat's ruling 2), pinned on the
 * SOURCE, because each "does not" is a line a later change could delete with no unit test going red: the paths run on a
 * server. Comments are stripped before every search ({@link #codeLines}), so prose naming a call never counts as the
 * call (the false-presence rule).
 */
class DragonShotWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> file(String... parts) throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("", parts)));
        assertTrue(code.size() > 20, "must have read " + String.join("/", parts) + ", read " + code.size());
        return code;
    }

    /**
     * EVERY ONE OF OUR HANDLE CALLS ASKS THE SINK FIRST: damage, a push, an impulse, a status. Each strike is the first
     * line inside the entity-thread task, before any store write, accrual, velocity or status. The heal refuses a shot
     * without destroying it. Four strikes in the file, no more (a fifth would be an unaccounted path).
     */
    @Test
    void everyHandleCallAsksTheSinkBeforeItActs() throws IOException {
        List<String> code = file("adapter", "BukkitCombatant.java");
        // F9b fix B: applyDamage's body moved, unchanged, into applyNow, which applyDamage AND applyDamageIf both run (the
        // second after its condition); so the strike is asked first on either path.
        int outer = indexOf(code, "@Override public void applyDamage(double amount, UUID sourceId, CritState crit,");
        assertTrue(outer > 0 && code.get(outer + 3).contains(
                "ctx.scheduler().onEntity(entity, () -> applyNow(null, amount, sourceId, crit, defense, element, accrual));"),
                "applyDamage runs applyNow: " + code.get(outer + 3));
        int cond = indexOf(code, "@Override public void applyDamageIf(java.util.function.BooleanSupplier applies, Runnable refused,");
        assertTrue(cond > 0 && indexAfter(code, cond, "applyNow(null, amount, sourceId, crit, defense, null, HitAccrual.weapon());")
                < cond + 10, "and so does applyDamageIf");
        int dmg = indexOf(code, "private void applyNow(io.github.butterflysmp.rpg.core.mob.MobHostility.Side attackerSide, double amount, UUID sourceId,");
        int dmgStrike = indexAfter(code, dmg, "if (ctx.shots().strike(entity,");
        int store = indexAfter(code, dmg, "ctx.stats().damage(entity.getUniqueId(), amount, sourceId,");
        assertTrue(dmg > 0 && dmgStrike > dmg && dmgStrike < store && dmgStrike <= dmg + 5,
                "applyDamage: strike before the store: " + dmg + " " + dmgStrike + " " + store);

        int kb = indexOf(code, "@Override public void applyKnockback(Vec3 direction, double strength) {");
        int kbStrike = indexAfter(code, kb, "if (ctx.shots().strike(entity,");
        int kbVel = indexAfter(code, kb, "entity.setVelocity(");
        assertTrue(kb > 0 && kbStrike < kbVel && kbStrike <= kb + 2, "applyKnockback: " + kb + " " + kbStrike + " " + kbVel);

        int imp = indexOf(code, "@Override public void applyImpulse(Vec3 velocity) {");
        int impStrike = indexAfter(code, imp, "if (ctx.shots().strike(entity, \"ability impulse\")) return;");
        int impVel = indexAfter(code, imp, "entity.setVelocity(");
        assertTrue(imp > 0 && impStrike < impVel && impStrike <= imp + 2, "applyImpulse: " + imp + " " + impStrike);

        int st = indexOf(code, "@Override public void applyStatus(String statusId, int durationTicks, int amplifier,");
        int stStrike = indexAfter(code, st, "if (ctx.shots().strike(entity, \"ability status=\" + statusId)) return;");
        int sw = indexAfter(code, st, "switch (def) {");
        // Step R7b: the shield's rider line sits between the strike and the switch (DragonBlinkWiringTest pins it).
        assertTrue(st > 0 && stStrike > st && stStrike + 2 == sw, "applyStatus: strike, the shield, then the switch: " + stStrike + " " + sw);

        int heal = indexOf(code, "@Override public void applyHeal(double amount) {");
        int healGuard = indexAfter(code, heal, "if (entity.getPersistentDataContainer().has(ctx.keys().mobShot)) return;");
        int healStore = indexAfter(code, heal, "ctx.stats().heal(");
        assertTrue(heal > 0 && healGuard < healStore && healGuard <= heal + 2, "applyHeal: " + heal + " " + healGuard);

        assertEquals(4, code.stream().filter(l -> l.contains("ctx.shots().strike(")).count(), "four strikes");
    }

    /** NO HEALTH BAR, NO NAMEPLATE, NO GEAR SCORE: the ONE seed refuses a shot, right after the player/armour-stand gate. */
    @Test
    void theSeedRefusesAShot() throws IOException {
        List<String> code = file("health", "MobNameplateManager.java");
        int seed = indexOf(code, "private Seeded seed(LivingEntity mob) {");
        int refuse = indexAfter(code, seed, "if (mob.getPersistentDataContainer().has(keys.mobShot)) return null;");
        int base = indexAfter(code, seed, "MobSeeding.isCustom(mobs, mobId)");
        assertTrue(seed > 0 && refuse == seed + 2 && refuse < base, "seed: " + seed + " " + refuse + " " + base);
    }

    /**
     * VANILLA DAMAGE: LOWEST, NOT ignoreCancelled, CANCELLED, THEN STRUCK. And the add handler asks the shooter FIRST
     * and returns for a shot before the nameplate or the beam; the death handler returns for a shot before growth and
     * the split; nothing targets a shot; a portal is refused; a bow shot of a shooting mob is cancelled.
     */
    @Test
    void theListenersMeetAShotFirstEverywhere() throws IOException {
        List<String> code = file("listener", "RpgListeners.java");
        int dmg = indexOf(code, "public void onShotDamaged(EntityDamageEvent event) {");
        assertTrue(code.get(dmg - 1).contains("@EventHandler(priority = EventPriority.LOWEST)"), code.get(dmg - 1));
        int isShot = indexAfter(code, dmg, "if (!shots.isShot(event.getEntity())) return;");
        int cancel = indexAfter(code, dmg, "event.setCancelled(true);");
        // The seat's ruling 1 on #201: ONLY AN ATTACK destroys it. The verdict is core's; each arm does one thing.
        int verdict = indexAfter(code, dmg, "switch (io.github.butterflysmp.rpg.core.mob.ShotDamage.classify(event.getCause().name(), byCombatant)) {");
        int attack = indexAfter(code, dmg, "case ATTACK -> shots.strike(event.getEntity(), by);");
        int end = indexAfter(code, dmg, "case END -> shots.end(event.getEntity(), \"kill\");");
        int refused = indexAfter(code, dmg, "case REFUSED -> shots.refuse(event.getEntity(), event.getCause().name());");
        assertTrue(isShot == dmg + 1 && cancel == dmg + 2 && verdict > cancel && attack == verdict + 1
                        && end == verdict + 2 && refused == verdict + 3 && refused <= dmg + 18,
                "onShotDamaged: " + dmg + " " + isShot + " " + cancel + " " + verdict + " " + attack + " " + end + " " + refused);
        assertEquals(1, code.stream().filter(l -> l.contains("shots.strike(event.getEntity(),")).count(),
                "the only vanilla strike is the ATTACK arm's");

        int add = indexOf(code, "public void onEntityAdd(EntityAddToWorldEvent event) {");
        assertTrue(code.get(add + 1).contains("shots.onAppear(event.getEntity());"), code.get(add + 1));
        assertTrue(code.get(add + 2).contains("if (shots.isShot(event.getEntity())) return;"), code.get(add + 2));
        assertTrue(indexAfter(code, add, "nameplates.onMobAppear(mob);") > add + 2, "the nameplate after the shot");

        int remove = indexOf(code, "public void onEntityRemove(EntityRemoveFromWorldEvent event) {");
        assertTrue(code.get(remove + 1).contains("shots.onRemove(event.getEntity().getUniqueId());"), code.get(remove + 1));

        int death = indexOf(code, "public void onMobDeath(EntityDeathEvent event) {");
        int deathGuard = indexAfter(code, death, "if (shots.isShot(event.getEntity())) return;");
        int grow = indexAfter(code, death, "MobGrower.onDeath(");
        int split = indexAfter(code, death, "MobSplitter.onDeath(");
        assertTrue(death > 0 && deathGuard == death + 2 && deathGuard < grow && grow < split,
                "onMobDeath: " + death + " " + deathGuard + " " + grow + " " + split);

        int target = indexOf(code, "public void onShotTargeted(org.bukkit.event.entity.EntityTargetEvent event) {");
        assertTrue(code.get(target + 1).contains("shots.isShot(event.getTarget())) event.setCancelled(true);"),
                code.get(target + 1));
        int portal = indexOf(code, "public void onShotPortal(org.bukkit.event.entity.EntityPortalEvent event) {");
        assertTrue(code.get(portal + 1).contains("if (shots.isShot(event.getEntity())) event.setCancelled(true);"),
                code.get(portal + 1));
        int bow = indexOf(code, "public void onShooterBow(org.bukkit.event.entity.EntityShootBowEvent event) {");
        assertTrue(code.get(bow + 2).contains("event.setCancelled(true);"), code.get(bow + 2));
    }

    /**
     * THE CARRIER IS NEVER SAVED, NEVER MOVES ITSELF, NEVER SOUNDS, AND NEVER DIES: it is set up in the pre-spawn
     * consumer, registered there (so its own add never reads as an orphan), and it ends only by {@code remove()}.
     * The shooter never kills, damages or heals a carrier.
     */
    @Test
    void theCarrierIsSetUpBeforeItsAddAndOnlyEverRemoved() throws IOException {
        List<String> code = file("health", "MobShooter.java");
        int spawn = indexOf(code, "Frog carrier = world.spawn(feet(world, eye, look.hitBox()), Frog.class, b -> {");
        int put = indexAfter(code, spawn, "carriers.put(b.getUniqueId(), made[0]);");
        assertTrue(spawn > 0 && put > spawn && put < spawn + 18, "registered in the consumer: " + spawn + " " + put);
        for (String set : List.of("b.getPersistentDataContainer().set(adapters.keys().mobShot,", "b.setAI(false);",
                "b.setSilent(true);", "b.setInvisible(true);", "b.setGravity(false);", "b.setPersistent(false);",
                "b.setRemoveWhenFarAway(false);", "b.setAdult();",
                "scale.setBaseValue(look.hitBox() / FROG_EDGE);")) {
            int at = indexAfter(code, spawn, set);
            assertTrue(at > spawn && at < put, set + " inside the consumer: " + at);
        }
        // THE SEAT'S D2 ON #201: setCollidable(false) clears LivingEntity.collides, which LivingEntity#isPickable reads,
        // and Entity#canBeHitByProjectile asks isPickable: no vanilla arrow could hit the carrier. Never again.
        assertTrue(code.stream().noneMatch(l -> l.contains("setCollidable(")), "the carrier stays pickable");
        // THE BLOCK CHECK IS THE CARRIER'S BOX swept along the tick's path (the seat's ruling 1), not the centre's ray.
        assertTrue(indexOf(code, "Optional<org.bukkit.block.Block> wall = firstColliding(world, swept(carrier, from, to));") > 0,
                "the swept box decides blocked");
        assertTrue(code.stream().noneMatch(l -> l.contains("rayTraceBlocks(")), "no centre ray decides a block");
        // THE ORDER IN A TICK (the seat's ruling 3): a struck shot is ended in strike(), and advance() returns for an
        // ended shot before anything else, so a strike that lands before the step wins over the reach.
        int adv = indexOf(code, "private void advance(Live s) {");
        assertTrue(code.get(adv + 1).contains("if (s.ended) return;"), code.get(adv + 1));
        assertTrue(indexOf(code, "d.setPersistent(false);") > 0, "the display is never saved either");
        assertTrue(indexOf(code, "displays.add(d.getUniqueId());") > 0, "the display is registered in its consumer");
        assertTrue(indexOf(code, "if (s.carrier.isValid()) s.carrier.remove();") > 0, "ended by remove()");
        for (String never : List.of("setHealth(", ".damage(", "carrier.damage", "MobGrower", "MobSplitter",
                "nameplates", "stats().")) {
            assertFalse(code.stream().anyMatch(l -> l.contains(never)), "MobShooter must not contain " + never);
        }
        // The Phalanx's loop's stop ends every shot it owns: no shot outlives its Phalanx.
        int stop = indexOf(code, "for (Live s : List.copyOf(o.shots)) end(s, \"owner-gone\");");
        assertTrue(stop > 0, "the owner's onStop ends its shots");
    }

    /** A STOP ENDS EVERY LIVE SHOT: onDisable calls the shooter's shutdown. */
    @Test
    void theDisableEndsEveryShot() throws IOException {
        List<String> code = file("RpgPlugin.java");
        int disable = indexOf(code, "public void onDisable() {");
        int shut = indexAfter(code, disable, "if (shooter != null) shooter.shutdown();");
        assertTrue(disable > 0 && shut > disable && shut < disable + 20, "onDisable: " + disable + " " + shut);
        assertTrue(indexOf(code, "for (org.bukkit.entity.Entity e : world.getEntities()) scheduler.onEntity(e, () -> shooter.onAppear(e));") > 0,
                "the enable sweep meets every entity, displays included");
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
