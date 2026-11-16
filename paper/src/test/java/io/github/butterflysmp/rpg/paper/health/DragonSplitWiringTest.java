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
 * Dragon-mobs step R2 (PLAN-dragon-mobs.md): the split's wiring, which needs a live server and so has no other unit
 * row. The rule itself is core's ({@code MobSplitTest}). Comment lines are filtered from the scans.
 */
class DragonSplitWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    /** The split acts only on a death that HAPPENS: MONITOR, and ignoreCancelled, on the death event. */
    @Test
    void theHandlerRunsAtMonitorOnDeathsThatHappen() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        // Renamed onMobDeath in the growth step, which runs growth first and then the split in this one handler.
        int handler = indexOf(code, "public void onMobDeath(EntityDeathEvent event) {");
        assertTrue(handler > 0, "the death handler exists");
        assertEquals("@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)",
                code.get(handler - 1).trim(), "its annotation");
        int split = indexOf(code, "MobSplitter.onDeath(event.getEntity(),");
        assertTrue(split > handler && split <= handler + 6, "the split is called inside it: " + handler + " " + split);
    }

    /**
     * THE ONCE-ONLY GUARD, as wired: the mark is READ before the rule is asked, and WRITTEN before the first child
     * is spawned, so a second delivery of the death (or a re-entrant one) finds it.
     */
    @Test
    void theMarkIsReadThenWrittenBeforeAnyChildSpawns() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobSplitter.java")));
        int read = indexOf(code, "boolean alreadySplit = pdc.has(keys.mobSplit, PersistentDataType.BYTE);");
        int rule = indexOf(code, "MobSplit.Children children = MobSplit.childrenFor(def, stage, alreadySplit);");
        int write = indexOf(code, "pdc.set(keys.mobSplit, PersistentDataType.BYTE, (byte) 1);");
        int spawn = indexOf(code, "LivingEntity child = world.spawn(");
        assertTrue(read > 0 && read < rule && rule < write && write < spawn,
                "read < rule < write < spawn: " + read + " " + rule + " " + write + " " + spawn);
    }

    /**
     * The child is made ours on the shared path, takes the PARENT'S gear score through M21's own method, and is
     * offered to the instrument, all inside its pre-spawn consumer.
     */
    @Test
    void theChildIsStampedTakesTheParentsScoreAndIsAdoptedInsideTheConsumer() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobSplitter.java")));
        int spawn = indexOf(code, "LivingEntity child = world.spawn(at, type, CreatureSpawnEvent.SpawnReason.CUSTOM, false, e -> {");
        int stamp = indexOf(code, "MobSpawner.stampCustom(e, def, children.stage(), keys);");
        int score = indexOf(code, "nameplates.inheritGearScore(parent, List.<org.bukkit.entity.Entity>of(e));");
        int adopt = indexOf(code, "adopt.accept(parent, e);");
        int close = indexOf(code, "});");
        assertTrue(spawn > 0 && spawn < stamp && stamp < score && score < adopt && adopt < close,
                "all three inside the consumer: " + spawn + " " + stamp + " " + score + " " + adopt + " " + close);
    }

    private static int indexOf(List<String> code, String needle) {
        for (int i = 0; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
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
