package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * THE DEV ROLL'S RUNNER (PLAN-loot.md; the wiring order's first step, the seat's queue item 6). n rolls of a table,
 * tallied. With pools (a source) each roll is {@link CascadeLoot#roll}; without (the default table) each is
 * {@link CascadeTable#rollRarity}. Pure; it gives nothing to anyone.
 */
public final class LootRolls {

    private LootRolls() {}

    /** The most rolls one call may make (a stated ceiling). */
    public static final int MAX_ROLLS = 100_000;

    /** @param pools a source's pools, or null for a rarity-only roll of the table */
    public static LootTally roll(CascadeTable table, Map<Rarity, List<String>> pools, int count, DoubleSupplier random) {
        if (count < 1 || count > MAX_ROLLS) {
            throw new IllegalArgumentException("a roll count is 1 to " + MAX_ROLLS + ", got " + count);
        }
        if (pools == null) {
            List<Rarity> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) out.add(table.rollRarity(random).orElse(null));
            return LootTally.ofRarities(out);
        }
        List<CascadeLoot.Result> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(CascadeLoot.roll(table, pools, random, PoolPick.UNIFORM));
        return LootTally.of(out);
    }
}
