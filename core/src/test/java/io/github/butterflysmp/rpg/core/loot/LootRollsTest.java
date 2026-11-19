package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Item 6: the dev roll's runner. n rolls of a table, with pools or without, tallied. Pure. */
class LootRollsTest {

    private static final CascadeTable T = new CascadeTable(List.of(
            new CascadeTable.Tier(Rarity.EXOTIC, 0.01), new CascadeTable.Tier(Rarity.LEGENDARY, 0.03),
            new CascadeTable.Tier(Rarity.EPIC, 0.08), new CascadeTable.Tier(Rarity.RARE, 0.12)));

    @Test
    void itRollsExactlyCountTimesWithPools() {
        SplittableRandom r = new SplittableRandom(7);
        LootTally t = LootRolls.roll(T, Map.of(Rarity.RARE, List.of("locust")), 5000, r::nextDouble);
        assertEquals(5000, t.rolls());
        assertEquals(t.rolls(), t.gave(Rarity.RARE) + t.nothing(), "only rare has a pool: every roll gave rare or nothing");
        assertTrue(t.skipped(Rarity.EPIC) > 0 && t.skipped(Rarity.EXOTIC) > 0, "the empty pools were skipped");
        assertEquals(Map.of("locust", t.gave(Rarity.RARE)), t.items());
    }

    @Test
    void withoutPoolsItTalliesRarities() {
        SplittableRandom r = new SplittableRandom(7);
        LootTally t = LootRolls.roll(T, null, 5000, r::nextDouble);
        assertEquals(5000, t.rolls());
        int landed = 0;
        for (Rarity x : Rarity.values()) landed += t.gave(x);
        assertEquals(5000, landed + t.nothing());
        assertEquals(0, t.skipped(Rarity.EPIC), "no pools: nothing is skipped");
        assertEquals(Map.of(), t.items());
    }

    @Test
    void theCountIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> LootRolls.roll(T, null, 0, () -> 0.5));
        assertThrows(IllegalArgumentException.class, () -> LootRolls.roll(T, null, LootRolls.MAX_ROLLS + 1, () -> 0.5));
    }
}
