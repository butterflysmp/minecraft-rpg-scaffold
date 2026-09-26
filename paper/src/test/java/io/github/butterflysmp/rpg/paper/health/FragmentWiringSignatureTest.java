package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fragments ride the ONE reconcile per stat (PLAN-build-system.md section 1.6), read from source.
 *
 * <p>{@code ModifierReconciler} removes every applied key absent from the desired map, so a SECOND
 * reconcile call for a stat -- one for gear and accessories, another for fragments -- would have each call
 * wipe the other's keys every pass. The guard: at every merge point, the fragments' map for a stat is on the
 * SAME line, in the SAME {@code merged(...)} call, as the accessories' map for that stat.
 */
class FragmentWiringSignatureTest {

    private static final Path HEALTH = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "health", "PlayerHealthSystem.java");

    /** The seven universal stats fragments may carry: every AccessoryStat but class_damage. */
    private static final List<AccessoryStat> MERGED = List.of(AccessoryStat.MAX_HEALTH, AccessoryStat.MAX_MANA,
            AccessoryStat.MANA_REGEN, AccessoryStat.CRIT_CHANCE, AccessoryStat.CRIT_DAMAGE,
            AccessoryStat.HEALTH_REGEN, AccessoryStat.DEFENSE);

    @Test
    void everyMergePointCarriesTheFragmentsBesideTheAccessories() throws IOException {
        List<String> lines = Files.readAllLines(HEALTH, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 300, "the scan must have read the file, read " + lines.size());
        assertEquals(AccessoryStat.values().length - 1, MERGED.size(), "every stat but class_damage");

        int fragmentReads = 0;
        for (AccessoryStat stat : MERGED) {
            String accessories = "fromAccessories.sources(AccessoryStat." + stat.name() + ")";
            String fragments = "fromFragments.sources(AccessoryStat." + stat.name() + ")";
            int site = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(accessories)) {
                    assertEquals(-1, site, stat + ": ONE accessory merge point, found a second at line " + (i + 1));
                    site = i;
                }
            }
            assertTrue(site >= 0, "CONTROL: " + stat + "'s accessory merge point is found");
            assertTrue(lines.get(site).contains(accessories + ", " + fragments),
                    stat + ": the fragments' map must be merged in the SAME call as the accessories' -- line "
                            + (site + 1) + ": " + lines.get(site).strip());
        }
        for (String line : lines) if (line.contains("fromFragments.sources(")) fragmentReads++;
        assertEquals(MERGED.size(), fragmentReads,
                "exactly one fragment read per merged stat: a second read is a second reconcile's input");
    }
}
