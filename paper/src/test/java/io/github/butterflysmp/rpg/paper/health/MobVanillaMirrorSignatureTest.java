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
 * F20 (PLAN-mob-scaling.md §7): the mirror has exactly two callers, and no unit test reaches either,
 * because both need a live server. {@code VanillaMirrorTest} proves the arithmetic. Only a source scan
 * can see that the listener is on the seam and that the seed calls it. Delete either and every unit test
 * stays green while vanilla health goes back to sitting at full (F20) or keeps its NBT value (the seed).
 *
 * <p>Comment lines are filtered, as in {@code MobDamageWiringSignatureTest}, because both call sites
 * explain the rule in prose beside the call.
 */
class MobVanillaMirrorSignatureTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    @Test
    void theMirrorIsAListenerOnTheHealthChangeSeam() throws IOException {
        List<String> code = codeLines(MAIN.resolve("RpgPlugin.java"));
        int from = indexOf(code, "new CombatantStats(new CompositeHealthListener(");
        assertTrue(from >= 0, "POSITIVE CONTROL: the composite construction is found, so the scan is not blind");
        // Bounded by the STATEMENT, not a line count: from the construction to its terminating semicolon.
        StringBuilder statement = new StringBuilder();
        for (int i = from; i < code.size(); i++) {
            statement.append(code.get(i).trim()).append(' ');
            if (code.get(i).trim().endsWith(";")) break;
        }
        assertTrue(statement.toString().contains("new MobVanillaMirror()"),
                "F20: the mirror must be one of the store's listeners: " + statement);
        assertTrue(statement.toString().contains("mobDeath"), "the statement read is the whole composite: " + statement);
    }

    @Test
    void theSeedMirrorsTheFreshStore() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobNameplateManager.java")));
        int from = indexOf(code, "private Seeded seed(LivingEntity mob)");
        assertTrue(from >= 0, "POSITIVE CONTROL: the seed is found, so the scan is not blind");
        int to = indexOf(code.subList(from + 1, code.size()), "private ") + from + 1;   // the next member
        List<String> body = code.subList(from, to);
        assertTrue(body.stream().anyMatch(l -> l.contains("stats.bootstrapIfAbsent(")), "the bootstrap is in the body read");
        assertEquals(1, body.stream().filter(l -> l.contains("MobVanillaMirror.apply(mob, state.current(), state.max())")).count(),
                "F20: the seed, the one store write the seam does not carry, must mirror the store's CURRENT");
    }

    @Test
    void theMirrorWritesHealthNeverTheAttribute() throws IOException {
        // M9: the attribute is read for the denominator and never written. The only write is setHealth.
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobVanillaMirror.java")));
        assertTrue(code.stream().anyMatch(l -> l.contains(".ifPresent(mob::setHealth)")),
                "POSITIVE CONTROL: the one setHealth write is found");
        assertTrue(code.stream().noneMatch(l -> l.contains("setBaseValue(") || l.contains("addModifier(")
                        || l.contains("addTransientModifier(")),
                "M9: the mirror must never write the MAX_HEALTH attribute");
    }

    private static int indexOf(List<String> code, String needle) {
        for (int i = 0; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    private static List<String> codeLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(l -> !isComment(l)).toList();
    }

    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }
}
