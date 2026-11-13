package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What an ore refuses at construction, with no server: blanks, inverted or negative counts, a
 * non-positive regrowth, and a completion-bonus map that does not match {@code sizes:} exactly in
 * either direction (the seat's ruling 2, 2026-11-13). Cross-file checks (does the size exist, does
 * the item or sound resolve) are ContentValidator's.
 */
class OreDefinitionTest {

    static OreDefinition iron(Map<String, Integer> bonus, List<String> sizes) {
        return new OreDefinition("iron", "Iron", "raw_iron", 2, 4, bonus, 1200, sizes,
                "rpg:mining/iron_patch", "rpg:mining/spot", "rpg:mining/crack",
                "minecraft:block.stone.hit", "minecraft:block.stone.break", "minecraft:poof");
    }

    @Test
    void theRuledIronOreBuilds() {
        OreDefinition ore = iron(Map.of("small", 5), List.of("small"));
        assertEquals(5, ore.completionBonus("small"));
        assertEquals(new PayoutRule(2, 4, 5), ore.payoutRule("small"));
        assertEquals(1200, ore.regrowthTicks());
    }

    @Test
    void aListedSizeWithNoBonusIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> iron(Map.of("small", 5), List.of("small", "medium")));
        assertTrue(ex.getMessage().contains("medium"), ex.getMessage());
    }

    @Test
    void aBonusForAnUnlistedSizeIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> iron(Map.of("small", 5, "large", 9), List.of("small")));
        assertTrue(ex.getMessage().contains("large"), ex.getMessage());
        // Mutation: check only one direction of the key comparison -> one of these two reddens.
    }

    @Test
    void theRestIsRefusedToo() {
        assertThrows(IllegalArgumentException.class, () -> iron(Map.of(), List.of()), "no sizes");
        assertThrows(IllegalArgumentException.class, () -> iron(Map.of("small", -1), List.of("small")));
        assertThrows(IllegalArgumentException.class, () -> new OreDefinition("iron", "Iron", "raw_iron",
                3, 2, Map.of("small", 5), 1200, List.of("small"), "a:b", "a:c", "a:d", "a:e", "a:f", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> new OreDefinition("iron", "Iron", "raw_iron",
                -1, 2, Map.of("small", 5), 1200, List.of("small"), "a:b", "a:c", "a:d", "a:e", "a:f", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> new OreDefinition("iron", "Iron", "raw_iron",
                2, 4, Map.of("small", 5), 0, List.of("small"), "a:b", "a:c", "a:d", "a:e", "a:f", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> new OreDefinition("iron", "Iron", " ",
                2, 4, Map.of("small", 5), 1200, List.of("small"), "a:b", "a:c", "a:d", "a:e", "a:f", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> new OreDefinition("iron", "Iron", "raw_iron",
                2, 4, Map.of("small", 5), 1200, List.of("small"), "a:b", "a:c", "a:d", " ", "a:f", "a:g"),
                "a blank miss sound");
        assertThrows(IllegalArgumentException.class, () -> new OreDefinition("iron", "Iron", "raw_iron",
                2, 4, Map.of("small", 5), 1200, List.of("small"), "a:b", "a:c", "a:d", "a:e", "a:f", " "),
                "a blank break particle");
    }
}
