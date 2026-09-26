package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentContributionsTest {

    private static FragmentDefinition f(String id, AccessoryStat stat, double amount) {
        return new FragmentDefinition(id, id, "red_dye", List.of(), Map.of(stat, amount));
    }

    @Test
    void eachSlotContributesUnderItsOwnKey() {
        FragmentContributions c = FragmentContributions.of(Arrays.asList(
                f("vigor", AccessoryStat.MAX_HEALTH, 4.0), null, f("ward", AccessoryStat.DEFENSE, 1.0), null));
        assertEquals(Map.of("fragment:0", 4.0), c.sources(AccessoryStat.MAX_HEALTH));
        assertEquals(Map.of("fragment:2", 1.0), c.sources(AccessoryStat.DEFENSE));
        assertEquals(Map.of(), c.sources(AccessoryStat.CRIT_CHANCE), "a stat nothing moves has no sources");
    }

    /** The same per-stat conversion an accessory's value goes through -- health regen is not raw. */
    @Test
    void valuesGoThroughTheAccessorySourceConversion() {
        FragmentContributions c = FragmentContributions.of(Arrays.asList(
                f("mend", AccessoryStat.HEALTH_REGEN, 0.1), null, null, null));
        assertEquals(HealthRegen.contribution(0.1), c.sources(AccessoryStat.HEALTH_REGEN).get("fragment:0"), 1e-12);
        assertEquals(AccessoryContributions.sourceValue(AccessoryStat.HEALTH_REGEN, 0.1),
                c.sources(AccessoryStat.HEALTH_REGEN).get("fragment:0"), 1e-12);
    }

    /** Ruling 11: one of each. A hand-edited duplicate contributes ONCE, from its first slot. */
    @Test
    void aDuplicateFragmentContributesOnce() {
        FragmentDefinition vigor = f("vigor", AccessoryStat.MAX_HEALTH, 4.0);
        FragmentContributions c = FragmentContributions.of(Arrays.asList(null, vigor, vigor, null));
        assertEquals(Map.of("fragment:1", 4.0), c.sources(AccessoryStat.MAX_HEALTH));
    }

    @Test
    void theListIsExactlyFourSlots() {
        assertThrows(IllegalArgumentException.class, () -> FragmentContributions.of(Arrays.asList(null, null, null)));
        assertTrue(FragmentContributions.NONE.sources(AccessoryStat.MAX_HEALTH).isEmpty());
    }

    /** The n-way merge keeps every source: gear, accessories AND fragments survive one reconcile's map. */
    @Test
    void theThreeWayMergeKeepsEverySource() {
        Map<String, Double> merged = AccessoryContributions.merged(
                Map.of("gear:0", 2.0), Map.of("accessory:1", 3.0), Map.of("fragment:2", 4.0));
        assertEquals(Map.of("gear:0", 2.0, "accessory:1", 3.0, "fragment:2", 4.0), merged);
    }
}
