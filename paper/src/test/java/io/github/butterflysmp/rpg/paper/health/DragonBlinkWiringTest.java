package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STEP R7b, PINNED ON THE SOURCE: the shield's one point, C1's calibration order, C2's registered work, C4's team, and
 * the blink's place in the tick. Each is a line a later change could move with no unit test going red, because the paths
 * run on a server. Comments are stripped before every search, so prose naming a call never counts as the call.
 */
class DragonBlinkWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    // --- the shield: ONE point every damage path passes ------------------------------------------------------------

    /**
     * EVERY WRITE OF DAMAGE TO A COMBATANT'S CUSTOM HEALTH IS ONE LINE, and it is inside the handle's applyDamage, after
     * the absorb. A census of the whole paper main tree: a second {@code stats().damage(} anywhere would be a path the
     * shield never sees. The absorb sits directly after the shot's strike block (a shot is never shielded) and before the
     * store.
     */
    @Test
    void theShieldIsAskedAtTheOneLineThatTakesCustomHealth() throws IOException {
        List<String> sites = new ArrayList<>();
        try (Stream<Path> all = Files.walk(MAIN)) {
            for (Path f : all.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> code = codeLines(f);
                for (String l : code) if (l.contains("stats().damage(")) sites.add(f.getFileName() + ": " + l.trim());
            }
        }
        assertEquals(1, sites.size(), "one write of custom damage in the paper tree: " + sites);
        assertTrue(sites.getFirst().startsWith("BukkitCombatant.java"), sites.toString());

        List<String> code = file("adapter", "BukkitCombatant.java");
        int dmg = indexOf(code, "@Override public void applyDamage(double amount, UUID sourceId, CritState crit,");
        int strike = indexAfter(code, dmg, "if (ctx.shots().strike(entity,");
        int absorb = indexAfter(code, dmg, "if (ctx.shields().absorb(entity, amount,");
        int store = indexAfter(code, dmg, "ctx.stats().damage(entity.getUniqueId(), amount, sourceId,");
        assertTrue(dmg > 0 && strike > dmg && absorb == strike + 4 && absorb < store,
                "strike, its return, then the absorb, then the store: " + dmg + " " + strike + " " + absorb + " " + store);
        assertTrue(code.get(absorb + 2).trim().equals("return;"), "an absorbed hit returns: " + code.get(absorb + 2));
    }

    /**
     * EVERY PATH THAT DAMAGES A COMBATANT IS A HANDLE CALL: each {@code .applyDamage(} in the paper main tree is on a
     * {@code .handle()} chain (its own line or the line before). The paths, by file: the melee exchange, its sweep and
     * the thorns reflect and the environmental reroute (fall, fire, explosion, a vanilla projectile;
     * {@code RpgListeners}), the scorch tick ({@code EntityScorchSink}), {@code /rpg damage} ({@code RpgCommand}), the
     * Knell's beam, the Phage's blast and the Phalanx's shot ({@code MobBeamer}, {@code MobBlaster}, {@code MobShooter}),
     * and our abilities through {@code CombatWorld}'s handle.
     */
    @Test
    void everyDamageCallSiteIsAHandleCall() throws IOException {
        int n = 0;
        List<String> bad = new ArrayList<>();
        try (Stream<Path> all = Files.walk(MAIN)) {
            for (Path f : all.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> code = codeLines(f);
                for (int i = 0; i < code.size(); i++) {
                    if (!code.get(i).contains(".applyDamage(")) continue;
                    n++;
                    boolean handle = code.get(i).contains("handle()") || (i > 0 && code.get(i - 1).contains("handle()"));
                    if (!handle) bad.add(f.getFileName() + ": " + code.get(i).trim());
                }
            }
        }
        assertTrue(n >= 12, "must have found the call sites, found " + n);
        assertTrue(bad.isEmpty(), "a damage call that is not a handle call: " + bad);
    }

    /** /kill AND THE VOID IGNORE IT: they are PASSed to vanilla before any of ours, so they never reach the point. */
    @Test
    void killAndVoidArePassedBeforeThePoint() throws IOException {
        List<String> policy = file("health", "VanillaDamagePolicy.java");
        assertTrue(indexOf(policy, "case KILL, VOID -> Action.PASS;") > 0, "KILL and VOID pass");
        List<String> l = file("listener", "RpgListeners.java");
        int env = indexOf(l, "public void onEnvironmentalDamage(EntityDamageEvent event) {");
        int pass = indexAfter(l, env, "if (VanillaDamagePolicy.forCause(event.getCause()) == VanillaDamagePolicy.Action.PASS) return;");
        int handle = indexAfter(l, env, ".applyDamage(applied, attributableId(event, target));");
        assertTrue(env > 0 && pass > env && pass < handle, "the pass before the reroute: " + env + " " + pass + " " + handle);
    }

    /** NOTHING RIDES THROUGH: our push, impulse and status each ask the rider check directly after the shot's strike. */
    @Test
    void theRidersAskDirectlyAfterTheStrike() throws IOException {
        List<String> code = file("adapter", "BukkitCombatant.java");
        int kb = indexOf(code, "if (ctx.shots().strike(entity, String.format(java.util.Locale.ROOT, \"ability knockback=%.3f\", strength))) {");
        assertTrue(code.get(kb + 3).contains("if (ctx.shields().suppressRider(entity,"), code.get(kb + 3));
        int imp = indexOf(code, "if (ctx.shots().strike(entity, \"ability impulse\")) return;");
        assertTrue(code.get(imp + 1).contains("if (ctx.shields().suppressRider(entity, \"impulse\")) return;"), code.get(imp + 1));
        int st = indexOf(code, "if (ctx.shots().strike(entity, \"ability status=\" + statusId)) return;");
        assertTrue(code.get(st + 1).contains("if (ctx.shields().suppressRider(entity, \"status=\" + statusId)) return;"), code.get(st + 1));
        assertEquals(4, code.stream().filter(l -> l.contains("ctx.shields().")).count(), "one absorb and three rider checks");
        List<String> l = file("listener", "RpgListeners.java");
        assertTrue(indexOf(l, "public void onShieldedKnockback(EntityKnockbackEvent event) {") > 0, "the vanilla push");
    }

    /** THE SHIELD IS UP FROM BIRTH: the byte at the stamp, inside the pre-spawn consumer like the rest of the stamp. */
    @Test
    void theStampRaisesTheShield() throws IOException {
        List<String> code = file("health", "MobSpawner.java");
        int stamp = indexOf(code, "public static void stampCustom(LivingEntity entity, MobDefinition def, int stage, Keys keys) {");
        int set = indexAfter(code, stamp, "entity.getPersistentDataContainer().set(keys.mobShield, PersistentDataType.BYTE, (byte) 1);");
        assertTrue(stamp > 0 && set > stamp, "the stamp sets mob_shield: " + stamp + " " + set);
    }

    // --- C1, C2, C4 and the blink ----------------------------------------------------------------------------------

    /** C1: the offset is measured at the START of every step, before this tick's teleport; no one-time flag remains. */
    @Test
    void c1CalibratesBeforeTheTeleportEveryTick() throws IOException {
        List<String> code = file("health", "MobShooter.java");
        int adv = indexOf(code, "private void advance(Live s) {");
        int cal = indexAfter(code, adv, "calibrate(s);");
        int tp = indexAfter(code, adv, "if (!carrier.teleport(feet(world, to, carrier.getHeight()))) {");
        assertTrue(adv > 0 && cal > adv && cal < tp, "calibrate before the teleport: " + adv + " " + cal + " " + tp);
        // Anchored to the flag's syntax, not the word: the calibration's own log line says "SHOT calibrated".
        assertFalse(code.stream().anyMatch(l -> l.contains("s.calibrated") || l.contains("boolean calibrated")),
                "no one-time calibration flag");
        assertEquals(1, code.stream().filter(l -> l.contains("calibrate(s);")).count(), "calibrated at one place");
    }

    /** C2: every point task core schedules is registered with its point, and runs only if it is still pending. */
    @Test
    void c2EveryPointTaskIsRegistered() throws IOException {
        List<String> code = file("adapter", "PaperCombatWorld.java");
        int sch = indexOf(code, "public void schedule(Vec3 near, int delayTicks, Runnable task) {");
        assertTrue(code.get(sch + 1).contains("long id = ctx.work().add(world.getUID(), near.x(), near.y(), near.z());"), code.get(sch + 1));
        assertTrue(code.get(sch + 3).contains("if (ctx.work().take(id)) task.run();"), code.get(sch + 3));
        assertEquals(1, code.stream().filter(l -> l.contains("onRegionLater(")).count(), "one point-scheduling site in the world");
    }

    /**
     * C4, BOTH PROPERTIES IN ONE TEST (the seat's ruling): the carrier is NEVER PUSHED and never pushes (a team whose
     * collision rule is NEVER, joined at the fire and left at the end) AND IT STAYS HITTABLE BY AN ARROW (no
     * {@code setCollidable} anywhere: {@code LivingEntity#isPickable} reads {@code collides}, and an arrow needs
     * {@code isPickable}; D2 on #201).
     */
    @Test
    void c4NeverPushedAndStillHittableByAnArrow() throws IOException {
        List<String> code = file("health", "MobShooter.java");
        assertFalse(code.stream().anyMatch(l -> l.contains("setCollidable(")), "never setCollidable: an arrow must hit it");
        assertTrue(indexOf(code, "t.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);") > 0,
                "the team's rule is NEVER");
        int pass = indexOf(code, "carrier.addPassenger(s.display);");
        assertTrue(code.get(pass + 1).contains("shotTeam().addEntry(carrier.getUniqueId().toString());"), code.get(pass + 1));
        int fin = indexOf(code, "private void finish(Live s, String line) {");
        int leave = indexAfter(code, fin, "shotTeam().removeEntry(s.carrier.getUniqueId().toString());");
        int rm = indexAfter(code, fin, "if (s.carrier.isValid()) s.carrier.remove();");
        assertTrue(fin > 0 && leave > fin && leave + 1 == rm, "it leaves the team as it is removed: " + leave + " " + rm);
        assertTrue(indexOf(file("RpgPlugin.java"), "shooter.clearShotTeam();") > 0, "the enable sweep empties the team");
    }

    /**
     * THE BLINK, LAST IN ITS PHALANX'S TICK, through the Knell's port: after every shot's step, so it can neither fight
     * nor orphan a shot; the landing is MobTeleporter's footing with no aim (no sight); firing makes it visible again.
     */
    @Test
    void theBlinkRunsLastThroughTheKnellsPort() throws IOException {
        List<String> code = file("health", "MobShooter.java");
        int step = indexOf(code, "for (Live s : List.copyOf(o.shots)) advance(s);");
        int blink = indexAfter(code, step, "blinkStep(o);");
        assertTrue(step > 0 && blink > step && blink <= step + 5, "the blink after the shots: " + step + " " + blink);
        assertTrue(indexOf(code, "MobBlink.Decision d = o.blink.decide(vec(from), anchor, draws, MobTeleporter.footing(mob, null));") > 0,
                "the Knell's port, no sight");
        int fire = indexOf(code, "shotTeam().addEntry(carrier.getUniqueId().toString());");
        int vis = indexAfter(code, fire, "mob.setInvisible(false);");
        assertTrue(fire > 0 && vis > fire && vis <= fire + 5, "firing clears invisibility: " + fire + " " + vis);
        List<String> tp = file("health", "MobTeleporter.java");
        assertTrue(indexOf(tp, "return footing(mob, beam.aimPoint(vec(target.getLocation()), target.getHeight()));") > 0,
                "the Knell's own footing is the shared one, with its aim");
    }

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
