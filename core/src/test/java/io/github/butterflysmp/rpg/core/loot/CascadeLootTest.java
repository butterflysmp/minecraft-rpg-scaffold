package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * c: THE ITEM. A source names, per rarity, the pool it can give; within a rarity the pick is uniform (SEAT PLACEHOLDER,
 * one rule so weights can come later). A rarity that lands with an EMPTY pool falls through to the next tier, rolled as
 * usual (SEAT READING, flagged), and the result says so.
 */
class CascadeLootTest {

    private static final CascadeTable TABLE = new CascadeTable(List.of(
            new CascadeTable.Tier(Rarity.EXOTIC, 0.01),
            new CascadeTable.Tier(Rarity.LEGENDARY, 0.03),
            new CascadeTable.Tier(Rarity.EPIC, 0.08),
            new CascadeTable.Tier(Rarity.RARE, 0.12)));

    private static DoubleSupplier draws(double... d) {
        Deque<Double> q = new ArrayDeque<>();
        for (double x : d) q.add(x);
        return q::removeFirst;
    }

    private static Map<Rarity, List<String>> pools() {
        Map<Rarity, List<String>> p = new EnumMap<>(Rarity.class);
        p.put(Rarity.EXOTIC, List.of("volley_stone"));
        p.put(Rarity.LEGENDARY, List.of("dragons_breath", "dragons_plume", "blaze_kings_staff"));
        p.put(Rarity.RARE, List.of("emberblade", "locust"));
        return p; // no EPIC pool: no content has an epic item today
    }

    @Test
    void aLandedRarityGivesAnItemFromItsPool() {
        CascadeLoot.Result r = CascadeLoot.roll(TABLE, pools(), draws(0.5, 0.01, 0.40), PoolPick.UNIFORM);
        assertEquals(Rarity.LEGENDARY, r.rarity());
        assertEquals("dragons_plume", r.item(), "0.40 of three: index 1");
        assertEquals(List.of(), r.fellThrough());
        assertTrue(r.gave());
    }

    /** An EMPTY pool for a landed rarity falls through: the next tier is rolled, and the result names the skipped one. */
    @Test
    void anEmptyPoolFallsThroughToTheNextTierAndSaysSo() {
        // exotic misses, legendary misses, EPIC LANDS (0.01 < 0.08) with no pool, rare is rolled and lands.
        CascadeLoot.Result r = CascadeLoot.roll(TABLE, pools(), draws(0.5, 0.5, 0.01, 0.05, 0.99), PoolPick.UNIFORM);
        assertEquals(Rarity.RARE, r.rarity());
        assertEquals("locust", r.item());
        assertEquals(List.of(Rarity.EPIC), r.fellThrough());
    }

    @Test
    void anEmptyPoolOnTheLastTierGivesNothingAndSaysSo() {
        CascadeTable onlyEpic = new CascadeTable(List.of(new CascadeTable.Tier(Rarity.EPIC, 1.0)));
        CascadeLoot.Result r = CascadeLoot.roll(onlyEpic, pools(), draws(0.3), PoolPick.UNIFORM);
        assertNull(r.rarity());
        assertNull(r.item());
        assertEquals(List.of(Rarity.EPIC), r.fellThrough());
        assertTrue(!r.gave());
    }

    @Test
    void nothingLandingGivesNothing() {
        CascadeLoot.Result r = CascadeLoot.roll(TABLE, pools(), draws(0.9, 0.9, 0.9, 0.9), PoolPick.UNIFORM);
        assertNull(r.rarity());
        assertNull(r.item());
        assertEquals(List.of(), r.fellThrough());
    }

    /** The uniform pick: index floor(draw x size), so each of n items owns an equal 1/n of [0, 1). */
    @Test
    void theUniformPickSplitsTheDrawEvenly() {
        List<String> three = List.of("a", "b", "c");
        assertEquals("a", PoolPick.UNIFORM.pick(three, 0.0));
        assertEquals("a", PoolPick.UNIFORM.pick(three, 0.3333));
        assertEquals("b", PoolPick.UNIFORM.pick(three, 0.3334));
        assertEquals("c", PoolPick.UNIFORM.pick(three, 0.9999999));
        assertEquals("only", PoolPick.UNIFORM.pick(List.of("only"), 0.7));
        assertThrows(IllegalArgumentException.class, () -> PoolPick.UNIFORM.pick(List.of(), 0.5));
    }
}
