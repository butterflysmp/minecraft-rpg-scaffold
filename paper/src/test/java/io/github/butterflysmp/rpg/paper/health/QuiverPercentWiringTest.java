package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Q1 and Q5 (the seat, 2026-11-25), by reading the source: the percent term is reconciled ONCE per pass in the same loop
 * as the additive one, from the set; and every main-code caller of {@code QuiverSize.resolve} passes the percent (the
 * two-argument form survives only for the untouched {@code QuiverSizeTest}).
 */
class QuiverPercentWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> code(Path p) throws IOException {
        return Files.readAllLines(MAIN.resolve(p), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    @Test
    void thePercentTermIsReconciledOnceInTheLoopFromTheSet() throws IOException {
        List<String> c = code(Path.of("health", "PlayerHealthSystem.java"));
        assertEquals(1, c.stream().filter(l -> l.contains("stats.reconcileQuiverSizePercentModifiers(id, fromSet.quiverPercent());")).count());
        int additive = -1, percent = -1;
        for (int i = 0; i < c.size(); i++) {
            if (c.get(i).contains("stats.reconcileQuiverSizeModifiers(id, ExpandedQuiverModifierItems.mergedSources(")) additive = i;
            if (c.get(i).contains("stats.reconcileQuiverSizePercentModifiers(")) percent = i;
        }
        assertTrue(additive > 0 && percent > additive, "beside the additive term, in the same pass");
    }

    @Test
    void everyMainCallerOfResolvePassesThePercent() throws IOException {
        List<String> calls = new java.util.ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String l = lines.get(i);
                    if (l.trim().startsWith("*") || l.trim().startsWith("//") || !l.contains("QuiverSize.resolve(")) continue;
                    // The call and its next two lines: every caller spans at most three.
                    String call = String.join(" ", lines.subList(i, Math.min(lines.size(), i + 3)));
                    calls.add(f.getFileName() + ": " + (call.contains("quiverSizePercentValue(") ? "percent" : "NO PERCENT"));
                }
            }
        }
        assertEquals(3, calls.size(), "the control: the three callers are found " + calls);
        assertTrue(calls.stream().allMatch(c -> c.endsWith("percent") && !c.endsWith("NO PERCENT")), calls.toString());
        // Mutation: drop the percent argument in QuiverItems.resolveCapacity -> "NO PERCENT" -> reddens.
    }
}
