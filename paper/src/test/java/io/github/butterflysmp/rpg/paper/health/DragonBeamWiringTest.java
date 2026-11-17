package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Knell's beam (PLAN-dragon-mobs.md step R5): the wiring that needs a live server and so has no other unit row.
 * The rules are core's ({@code BeamLineTest}, {@code MobBeamTest}, {@code BeamCycleTest}); the lines are
 * {@code MobBeamLinesTest}'s. Comment lines are filtered from the scans, so a javadoc that quotes a call cannot satisfy
 * a needle.
 */
class DragonBeamWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> beamer() throws IOException {
        return codeLines(MAIN.resolve(Path.of("health", "MobBeamer.java")));
    }

    /**
     * THE LOOP DIES WITH THE MOB (the brief's ruling 6): started at the add event and the startup sweep, cancelled at
     * the remove event. The isActive guard itself is RepeatingTask's, pinned by its own tests.
     */
    @Test
    void theLoopStartsOnTheAddAndStopsOnTheRemove() throws IOException {
        List<String> l = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        int add = indexOf(l, "public void onEntityAdd(EntityAddToWorldEvent event) {");
        int appear = indexAfter(l, add, "beams.onAppear(mob);");
        assertTrue(add > 0 && appear > add && appear < add + 6, "the add handler starts the loop");
        int remove = indexOf(l, "public void onEntityRemove(EntityRemoveFromWorldEvent event) {");
        int gone = indexAfter(l, remove, "beams.onRemove(mob.getUniqueId());");
        assertTrue(remove > 0 && gone > remove && gone < remove + 6, "the remove handler stops it");

        List<String> plugin = codeLines(MAIN.resolve("RpgPlugin.java"));
        int sweep = indexOf(plugin, "for (LivingEntity mob : world.getLivingEntities()) {");
        int swept = indexAfter(plugin, sweep, "beams.onAppear(mob);");
        assertTrue(sweep > 0 && swept > sweep && swept < sweep + 6, "a mob loaded before the listener gets its loop");
        assertEquals(1, plugin.stream().filter(s -> s.contains("new io.github.butterflysmp.rpg.paper.health.MobBeamer(")).count(),
                "ONE beamer, built in one place");
    }

    /** The loop is a period-1 RepeatingTask on the mob's own entity scheduler, whose onStop forgets it. */
    @Test
    void theLoopIsAPeriodOneEntityTaskThatForgetsItselfOnStop() throws IOException {
        List<String> b = beamer();
        int start = indexOf(b, "a.task = RepeatingTask.start(new EntityTaskTarget(mob, adapters.scheduler(), adapters.log()), 1, \"knell-beam\",");
        assertTrue(start > 0, "the loop is a RepeatingTask on an EntityTaskTarget, period 1");
        int forget = indexAfter(b, start, "active.remove(a.mob.getUniqueId(), a);");
        int unroot = indexAfter(b, start, "if (a.mob.isValid()) unroot(a.mob);");
        assertTrue(forget > start && unroot > start && unroot < forget, "onStop releases the root (if valid), then forgets");
    }

    /** THE ROOT CANNOT BE SAVED ONTO A MOB: transient only, never a saved modifier. */
    @Test
    void theRootIsATransientModifier() throws IOException {
        List<String> b = beamer();
        assertTrue(b.stream().anyMatch(s -> s.contains("speed.addTransientModifier(new AttributeModifier(adapters.keys().beamRoot, -1.0,")));
        assertTrue(b.stream().noneMatch(s -> s.contains(".addModifier(")), "no saved modifier anywhere in the beamer");
    }

    /** I23: a mob with no AI begins no beam of its own; only a forced attempt passes the guard. */
    @Test
    void aFrozenMobsOwnAttemptIsAFreshWait() throws IOException {
        List<String> b = beamer();
        int attempt = indexOf(b, "private void attempt(Active a) {");
        int guard = indexAfter(b, attempt, "if (!forced && !mob.hasAI()) {");
        int redraw = indexAfter(b, guard, "a.cycle.noTarget();");
        int choose = indexAfter(b, attempt, "LivingEntity target = choose(mob, a.beam, eye);");
        assertTrue(attempt > 0 && guard > attempt && redraw == guard + 1 && choose > redraw,
                "the guard redraws before any target is chosen");
    }

    /**
     * THE DAMAGE IS THE MOB-HIT PATH'S FINAL CALL (the brief's ruling 4), and THE REPORT FOLLOWS ONLY A HIT (ruling
     * 5): the two-argument applyDamage inside {@code if (shot.hit())}, and the listeners told only through
     * {@code MobBeam.report}, which is empty on a miss.
     */
    @Test
    void theDamageAndTheReportAreBothGatedOnTheHit() throws IOException {
        List<String> b = beamer();
        int gate = indexOf(b, "if (shot.hit()) {");
        int dmg = indexAfter(b, gate, "BukkitCombatant.of(victim, adapters).handle().applyDamage(dealt, dealer);");
        assertTrue(gate > 0 && dmg > gate && dmg < gate + 8, "applyDamage(amount, dealer), inside the hit branch");
        assertEquals(2, b.stream().filter(s -> s.contains("applyDamage(")).count(), "the owned and the scheduled call only");
        int report = indexOf(b, "MobBeam.report(shot, mob.getUniqueId(), a.mobId, k.target.getUniqueId(), tick, fired).ifPresent(hit -> {");
        int tell = indexAfter(b, report, "for (Listener l : listeners) l.onBeamHit(mob, k.target, hit);");
        assertTrue(report > dmg && tell > report && tell < report + 4, "the listeners are told inside the report");
        assertEquals(1, b.stream().filter(s -> s.contains("l.onBeamHit(")).count(), "and nowhere else");
    }

    /**
     * THE SOUNDS ARE CONTENT (the follow-up of 2026-11-17): the beamer names no Sound constant and plays each authored
     * sound through one method, which logs a line for each; the telegraph plays its list at the lock and the fire
     * plays its list at the fire. Production's loader refuses a key the server's sound registry does not know.
     */
    @Test
    void theSoundsComeFromContentAndEachIsLogged() throws IOException {
        List<String> b = beamer();
        assertTrue(b.stream().noneMatch(s -> s.contains("Sound.")), "no Sound constant in the beamer");
        assertEquals(1, b.stream().filter(s -> s.contains("playSounds(mob, a, \"telegraph\", a.beam.telegraphSounds());")).count());
        assertEquals(1, b.stream().filter(s -> s.contains("playSounds(mob, a, \"fire\", a.beam.fireSounds());")).count());
        int play = indexOf(b, "private void playSounds(Mob mob, Active a, String when, List<BeamSound> sounds) {");
        int world = indexAfter(b, play, "mob.getWorld().playSound(mob.getLocation(), s.key(), SoundCategory.HOSTILE, (float) s.volume(),");
        int line = indexAfter(b, play, "adapters.log().info(MobBeamLines.sound(a.mobId, mob.getUniqueId(), when, tick, s));");
        assertTrue(play > 0 && world > play && line > world && line < play + 8, "each sound is played, then logged");
        List<String> plugin = codeLines(MAIN.resolve("RpgPlugin.java"));
        assertTrue(plugin.stream().anyMatch(s -> s.contains("RpgPlugin::soundKnown, k -> org.bukkit.Material.matchMaterial(k) != null)")) && plugin.stream().anyMatch(s -> s.contains("return k != null && org.bukkit.Registry.SOUND_EVENT.get(k) != null;")),
                "production's loader asks the server's sound registry");
    }

    /** A force only marks the attempt due; the attempt runs in the loop's own step, so the fire's timing is the loop's. */
    @Test
    void aForceNeverRunsTheAttemptItself() throws IOException {
        List<String> b = beamer();
        int force = indexOf(b, "public Optional<String> force(LivingEntity entity) {");
        int end = indexAfter(b, force, "return Optional.empty();");
        assertTrue(force > 0 && end > force);
        for (int i = force; i <= end; i++) {
            assertTrue(!b.get(i).contains("attempt("), "line " + i + ": " + b.get(i));
        }
        assertEquals(1, b.stream().filter(s -> s.contains("attempt(a);")).count(), "the loop's step is the one caller");
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
