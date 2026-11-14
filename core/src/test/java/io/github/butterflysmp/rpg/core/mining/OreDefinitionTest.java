package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What an ore refuses at construction, with no server: blanks, inverted or negative counts, a
 * non-positive regrowth, a size weight that is not positive (S2, the seat's ruling 3), and a
 * completion-bonus map or a patch-model map that does not name exactly the ore's sizes, in either
 * direction (the seat's ruling 2, 2026-11-13; S2 ruling 6). Cross-file checks (does the size exist,
 * does the item or sound resolve) are ContentValidator's.
 */
class OreDefinitionTest {

    private static Map<String, Integer> ints(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static Map<String, String> models(String... ids) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String id : ids) m.put(id, "rpg:mining/iron_patch_" + id);
        return m;
    }

    @Test
    void theShippedIronOreBuildsWithFourWeightedSizesInAuthoredOrder() {
        OreDefinition ore = MiningFixtures.iron();
        assertEquals(List.of("small", "medium", "large", "extra_large"), ore.sizes(), "authored order");
        assertEquals(50, ore.sizeWeights().get("small"));
        assertEquals(5, ore.sizeWeights().get("extra_large"));
        assertEquals(new PayoutRule(2, 4, 5), ore.payoutRule("small"));
        assertEquals(new PayoutRule(2, 4, 10), ore.payoutRule("extra_large"));
        assertEquals("rpg:mining/iron_patch_large", ore.patchModel("large"));
        assertEquals("minecraft:entity.villager.no", ore.deniedSound());
        assertEquals(1200, ore.regrowthTicks());
    }

    @Test
    void aSizeWeightMustBeAtLeastOne() {
        var zero = assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", 0), ints("small", 5), models("small")));
        assertTrue(zero.getMessage().contains("weight") && zero.getMessage().contains("small"), zero.getMessage());
        assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", -3), ints("small", 5), models("small")));
        // 1 is the smallest legal weight.
        assertEquals(1, MiningFixtures.ore(ints("small", 1), ints("small", 5), models("small")).sizeWeights().get("small"));
        // Mutation: "< 1" -> "< 0" -> a zero weight builds -> reddens.
    }

    @Test
    void aListedSizeWithNoBonusIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", 50, "medium", 30), ints("small", 5), models("small", "medium")));
        assertTrue(ex.getMessage().contains("medium"), ex.getMessage());
    }

    @Test
    void aBonusForAnUnlistedSizeIsRefused() {
        var ex = assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", 50), ints("small", 5, "large", 8), models("small")));
        assertTrue(ex.getMessage().contains("large"), ex.getMessage());
    }

    @Test
    void aListedSizeWithNoPatchModelIsRefusedAndSoIsAModelForAnUnlistedSize() {
        var missing = assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", 50, "medium", 30), ints("small", 5, "medium", 6), models("small")));
        assertTrue(missing.getMessage().contains("models.patch") && missing.getMessage().contains("medium"),
                missing.getMessage());
        var extra = assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", 50), ints("small", 5), models("small", "large")));
        assertTrue(extra.getMessage().contains("models.patch") && extra.getMessage().contains("large"),
                extra.getMessage());
        var blank = assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", 50), ints("small", 5), Map.of("small", " ")));
        assertTrue(blank.getMessage().contains("models.patch.small"), blank.getMessage());
        // Mutation: check only one direction of the key comparison -> one of the first two reddens.
    }

    @Test
    void theRestIsRefusedToo() {
        assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(ints(), ints(), models()), "no sizes");
        assertThrows(IllegalArgumentException.class, () -> MiningFixtures.ore(
                ints("small", 50), ints("small", -1), models("small")));
        assertThrows(IllegalArgumentException.class, () -> ore(3, 2, 1200, "raw_iron", "a:e", "a:h", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> ore(-1, 2, 1200, "raw_iron", "a:e", "a:h", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> ore(2, 4, 0, "raw_iron", "a:e", "a:h", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> ore(2, 4, 1200, " ", "a:e", "a:h", "a:g"));
        assertThrows(IllegalArgumentException.class, () -> ore(2, 4, 1200, "raw_iron", " ", "a:h", "a:g"),
                "a blank miss sound");
        var denied = assertThrows(IllegalArgumentException.class, () -> ore(2, 4, 1200, "raw_iron", "a:e", " ", "a:g"),
                "a blank denied sound");
        assertTrue(denied.getMessage().contains("sounds.denied"), denied.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ore(2, 4, 1200, "raw_iron", "a:e", "a:h", " "),
                "a blank break particle");
    }

    private static OreDefinition ore(int min, int max, int regrow, String item, String miss, String denied, String particle) {
        return new OreDefinition("iron", "Iron", item, min, max, ints("small", 5), regrow, ints("small", 50),
                models("small"), "a:c", "a:d", miss, "a:f", denied, particle);
    }
}
