package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * A CASCADE ROLL FOR AN ITEM (PLAN-loot.md, c): a table's tiers, and a source's pool per rarity. Pure.
 * <ul>
 *   <li>Each tier is rolled in turn, as {@link CascadeTable#rollRarity}. The first to land with a NON-EMPTY pool gives
 *       one item of that pool, by {@link PoolPick}.</li>
 *   <li>SEAT READING, flagged: a rarity that lands with an EMPTY pool for this source FALLS THROUGH. It counts as a
 *       miss and the next tier is rolled as usual, rather than giving nothing. The result lists every rarity that fell
 *       through.</li>
 * </ul>
 */
public final class CascadeLoot {

    private CascadeLoot() {}

    /**
     * @param rarity      the rarity given, or null when nothing was
     * @param item        the item id given, or null
     * @param fellThrough every rarity that landed with an empty pool, in the order rolled
     */
    public record Result(Rarity rarity, String item, List<Rarity> fellThrough) {
        public boolean gave() {
            return item != null;
        }
    }

    public static Result roll(CascadeTable table, Map<Rarity, List<String>> pools, DoubleSupplier random, PoolPick pick) {
        List<Rarity> fell = new ArrayList<>();
        for (CascadeTable.Tier t : table.tiers()) {
            if (!(random.getAsDouble() < t.chance())) continue;
            List<String> pool = pools.getOrDefault(t.rarity(), List.of());
            if (pool.isEmpty()) {
                fell.add(t.rarity());
                continue;
            }
            return new Result(t.rarity(), pick.pick(pool, random.getAsDouble()), List.copyOf(fell));
        }
        return new Result(null, null, List.copyOf(fell));
    }
}
