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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Knell's teleport (PLAN-dragon-mobs.md step R6): the wiring that needs a live server. The decision is core's
 * ({@code BeamTeleportTest}); the lines are {@code MobTeleportLinesTest}'s. Comment lines are filtered from the scans.
 */
class DragonTeleportWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> teleporter() throws IOException {
        return codeLines(MAIN.resolve(Path.of("health", "MobTeleporter.java")));
    }

    /**
     * THE TRIGGER IS THE BEAM'S REPORT AND NOTHING ELSE (the brief's ruling 1): the teleporter is a beam listener,
     * registered once, and nothing else in the plugin calls {@code teleport(} on a mob in response to damage or a
     * projectile. The old arrow dodge exists nowhere: no main source names {@code dodge}.
     */
    @Test
    void onlyTheBeamsReportMovesAKnell() throws IOException {
        List<String> plugin = codeLines(MAIN.resolve("RpgPlugin.java"));
        assertEquals(1, plugin.stream().filter(s -> s.contains("beams.addListener(teleports);")).count());
        assertEquals(1, plugin.stream().filter(s -> s.contains("new io.github.butterflysmp.rpg.paper.health.MobTeleporter(")).count());
        List<String> t = teleporter();
        assertTrue(t.stream().anyMatch(s -> s.contains("public final class MobTeleporter implements MobBeamer.Listener {")));
        assertEquals(1, t.stream().filter(s -> s.contains("mob.teleport(")).count(), "one move, in one place");

        List<String> dodges = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : codeLines(f)) if (line.toLowerCase().contains("dodge")) dodges.add(f + ": " + line);
            }
        }
        assertEquals(List.of(), dodges, "no arrow dodge in our code");
    }

    /**
     * THE DELAY IS A ONE-SHOT TASK ON THE KNELL'S OWN SCHEDULER, PERIOD delay_ticks (ruling 2), and its onStop names
     * a move that never happened. The removal path is the beamer's: it tells its listeners.
     */
    @Test
    void theDelayIsAOneShotEntityTaskThatSaysWhenItNeverRan() throws IOException {
        List<String> t = teleporter();
        int start = indexOf(t, "p.task = RepeatingTask.start(new EntityTaskTarget(mob, adapters.scheduler(), adapters.log()), t.delayTicks(),");
        int once = indexAfter(t, start, "return false;   // once");
        int stop = indexAfter(t, start, "if (!p.done) {");
        int why = indexAfter(t, stop, "BeamTeleport.Skip.KNELL_GONE));");
        assertTrue(start > 0 && once > start && stop > once && why > stop && why < stop + 4);
        List<String> b = codeLines(MAIN.resolve(Path.of("health", "MobBeamer.java")));
        int remove = indexOf(b, "public void onRemove(UUID id) {");
        int tell = indexAfter(b, remove, "for (Listener l : listeners) l.onRemove(id);");
        assertTrue(remove > 0 && tell > remove && tell < remove + 4, "a removal reaches the teleporter");
    }

    /** THE MOVE: the decision first, then the move, then the path stopped (nothing in the jar stops it). */
    @Test
    void theMoveFollowsTheDecisionAndStopsTheOldPath() throws IOException {
        List<String> t = teleporter();
        int decide = indexOf(t, "BeamTeleport.Decision d = BeamTeleport.decide(t, center, from, draw, footing(mob, target, beam));");
        int move = indexAfter(t, decide, "boolean ok = mob.teleport(to);");
        int path = indexAfter(t, move, "mob.getPathfinder().stopPathfinding();");
        assertTrue(decide > 0 && move > decide && path == move + 3, "decide, move, then stop the path");
        int skip = indexOf(t, "Optional<BeamTeleport.Skip> skip = BeamTeleport.skip(true, LiveTargets.isLive(target),");
        assertTrue(skip > 0 && skip < decide, "the target is re-checked before deciding");
    }

    /** VALID through the API (ruling 4), never hand-rolled: each of the four checks names its call. */
    @Test
    void validIsAskedOfTheCollisionApi() throws IOException {
        List<String> t = teleporter();
        assertTrue(t.stream().anyMatch(s -> s.contains("return world.isChunkLoaded(")));
        assertTrue(t.stream().anyMatch(s -> s.contains("return !mob.wouldCollideUsing(new BoundingBox(")));
        assertTrue(t.stream().anyMatch(s -> s.contains("return world.hasCollisionsIn(new BoundingBox(")));
        assertTrue(t.stream().anyMatch(s -> s.contains("new Vector(u.x(), u.y(), u.z()), len, FluidCollisionMode.NEVER, true) == null;")),
                "sight by the beam's own block rule");
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
                    String s = l.trim();
                    return !(s.startsWith("//") || s.startsWith("*") || s.startsWith("/*"));
                })
                .toList();
    }
}
