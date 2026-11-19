package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE DEV ROLL'S TALLY (PLAN-loot.md, the wiring order's first step; the seat's queue, item 6). Pure: from a list of
 * roll results, per rarity how many gave, how many were skipped for an empty pool, how many rolls gave nothing, and each
 * item's count.
 */
class LootTallyTest {

    @Test
    void theTallyCountsGivesSkipsNothingAndItems() {
        List<CascadeLoot.Result> rolls = List.of(
                new CascadeLoot.Result(Rarity.RARE, "locust", List.of()),
                new CascadeLoot.Result(Rarity.RARE, "emberblade", List.of(Rarity.EPIC)),
                new CascadeLoot.Result(Rarity.RARE, "locust", List.of()),
                new CascadeLoot.Result(Rarity.EXOTIC, "volley_stone", List.of()),
                new CascadeLoot.Result(null, null, List.of(Rarity.EPIC)),
                new CascadeLoot.Result(null, null, List.of()));
        LootTally t = LootTally.of(rolls);
        assertEquals(6, t.rolls());
        assertEquals(3, t.gave(Rarity.RARE));
        assertEquals(1, t.gave(Rarity.EXOTIC));
        assertEquals(0, t.gave(Rarity.EPIC));
        assertEquals(2, t.skipped(Rarity.EPIC), "an epic that landed with no pool, twice");
        assertEquals(0, t.skipped(Rarity.RARE));
        assertEquals(2, t.nothing(), "two rolls gave nothing, one of them after a skip");
        assertEquals(Map.of("locust", 2, "emberblade", 1, "volley_stone", 1), t.items());
        assertEquals(4, t.itemsGiven());
    }

    /** A rarity-only table (no pools: the default) counts what landed, and never a skip. */
    @Test
    void aRarityOnlyTallyCountsLandings() {
        LootTally t = LootTally.ofRarities(java.util.Arrays.asList(Rarity.EPIC, null, Rarity.EPIC, Rarity.COMMON));
        assertEquals(4, t.rolls());
        assertEquals(2, t.gave(Rarity.EPIC));
        assertEquals(1, t.gave(Rarity.COMMON));
        assertEquals(1, t.nothing());
        assertEquals(0, t.skipped(Rarity.EPIC));
        assertEquals(Map.of(), t.items());
    }

    @Test
    void anEmptyTally() {
        LootTally t = LootTally.of(List.of());
        assertEquals(0, t.rolls());
        assertEquals(0, t.nothing());
        assertEquals(0, t.itemsGiven());
    }
}
