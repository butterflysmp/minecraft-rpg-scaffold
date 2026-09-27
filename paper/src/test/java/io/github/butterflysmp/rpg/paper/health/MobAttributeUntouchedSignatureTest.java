package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** M9: A MOB'S SCALED HEALTH IS NEVER WRITTEN INTO THE VANILLA MAX_HEALTH ATTRIBUTE. ***
 *
 * <p>Two reasons, and the second is the one no other test can see:
 * <ul>
 *   <li>a GS 500 Warden is 12,500, past vanilla's attribute cap;
 *   <li><b>environmental damage on a mob is priced as vanilla amount / THAT ATTRIBUTE x the custom
 *       max</b> ({@code DamageScale.toCustom}) -- M13's proportion only while the attribute stays
 *       vanilla. Write the scaled max into it and every fall, fire and lava hit on every mob silently
 *       becomes x1, while the nameplate still reads correctly and every unit test stays green.
 * </ul>
 *
 * <p>The seed ({@code MobNameplateManager.seed}) needs a live {@code LivingEntity}, which nothing here
 * can build, so this is a SOURCE SCAN -- the same shape as {@code GearScoreWiringSignatureTest}.
 *
 * <h2>WHAT IT ASSERTS, AND WHAT IT CANNOT</h2>
 *
 * Every file in {@code paper/src/main} that both WRITES an attribute ({@code setBaseValue},
 * {@code addModifier}, {@code addTransientModifier}) and names {@code MAX_HEALTH} in CODE is exactly
 * the player heart bar, {@code EntityHeartBar}. A write reaching {@code MAX_HEALTH} through an
 * {@code AttributeInstance} handed in from another file would evade it; nothing does today, and the
 * account of why that would be wrong is on {@code MobNameplateManager.seed}.
 *
 * <p>Comment lines are filtered, because this slice EXPLAINS the rule in prose beside the code it
 * guards, and a bare grep would find the explanation and call it a violation.
 */
class MobAttributeUntouchedSignatureTest {

    private static final Path MAIN = Path.of("src", "main", "java");
    private static final Path NAMEPLATES = MAIN.resolve(Path.of(
            "io", "github", "butterflysmp", "rpg", "paper", "health", "MobNameplateManager.java"));

    private static final List<String> WRITES = List.of("setBaseValue(", "addModifier(", "addTransientModifier(");

    @Test
    void onlyThePlayerHeartBarWritesMaxHealth() throws IOException {
        Set<String> writers = new TreeSet<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                scanned++;
                List<String> code = codeLines(file);
                boolean writes = code.stream().anyMatch(l -> WRITES.stream().anyMatch(l::contains));
                boolean namesMaxHealth = code.stream().anyMatch(l -> l.contains("MAX_HEALTH"));
                if (writes && namesMaxHealth) writers.add(file.getFileName().toString());
            }
        }
        // POSITIVE CONTROL: the scan must have walked the real tree. An empty walk would find no
        // writer at all and fail the equality below -- but a walk of the WRONG tree could find a
        // coincidental match, so the file count is asserted too.
        assertTrue(scanned > 100, "the scan must have read paper/src/main, read " + scanned + " files");
        assertEquals(Set.of("EntityHeartBar.java"), writers,
                "M9: nothing but the PLAYER heart bar may write MAX_HEALTH. A mob's scaled max lives in"
                        + " the custom store ONLY -- written into the attribute, it breaks vanilla's cap AND"
                        + " turns every environmental hit on a mob into x1 (DamageScale divides by it).");
    }

    /** The seed file specifically: it READS the attribute and must never write any attribute at all. */
    @Test
    void theMobSeedWritesNoAttribute() throws IOException {
        List<String> code = codeLines(NAMEPLATES);
        assertTrue(code.size() > 100, "must have read MobNameplateManager, read " + code.size() + " code lines");
        assertTrue(code.stream().anyMatch(l -> l.contains("getAttribute(Attribute.MAX_HEALTH)")),
                "POSITIVE CONTROL: the seed reads the vanilla max -- if this is gone, the scan is"
                        + " looking at the wrong file or the seed moved");
        for (String write : WRITES) {
            assertFalse(code.stream().anyMatch(l -> l.contains(write)),
                    "the mob seed must not call " + write + " -- M9, the scaled max goes to the custom store only");
        }
    }

    /** Guards the filter itself: a comment naming the rule is not a violation, code is. */
    @Test
    void theFilterDropsCommentsAndKeepsCode() {
        assertTrue(isComment("     * never setBaseValue( the max"));
        assertTrue(isComment("        // setBaseValue(max)"));
        assertFalse(isComment("        attr.setBaseValue(max);"));
    }

    private static List<String> codeLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(l -> !isComment(l)).toList();
    }

    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }
}
