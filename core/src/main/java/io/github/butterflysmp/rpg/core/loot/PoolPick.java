package io.github.butterflysmp.rpg.core.loot;

import java.util.List;

/**
 * HOW AN ITEM IS PICKED FROM A RARITY'S POOL (PLAN-loot.md, c). ONE rule, so weights can replace it later in one place.
 * Today it is {@link #UNIFORM} (SEAT PLACEHOLDER, flagged for Ben).
 */
@FunctionalInterface
public interface PoolPick {

    /** One item of a non-empty pool, from one draw in [0, 1). */
    String pick(List<String> pool, double draw);

    /** Uniform: index floor(draw x size), so each of n items owns an equal 1/n of [0, 1). */
    PoolPick UNIFORM = (pool, draw) -> {
        if (pool == null || pool.isEmpty()) throw new IllegalArgumentException("a pick needs a non-empty pool");
        int i = (int) Math.floor(draw * pool.size());
        return pool.get(Math.max(0, Math.min(pool.size() - 1, i)));
    };
}
