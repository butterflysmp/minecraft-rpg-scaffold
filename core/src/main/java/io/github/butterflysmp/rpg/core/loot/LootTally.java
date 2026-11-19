package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A TALLY OF ROLLS (PLAN-loot.md; the dev roll command, the wiring order's first step). Pure: per rarity how many gave
 * (or, for a rarity-only table, landed), how many were SKIPPED for an empty pool (Ben, 2026-11-19: "general skip that
 * rarity"), how many rolls gave nothing, and each item's count, by id.
 */
public final class LootTally {

    private final int rolls;
    private final Map<Rarity, Integer> gave;
    private final Map<Rarity, Integer> skipped;
    private final int nothing;
    private final Map<String, Integer> items;

    private LootTally(int rolls, Map<Rarity, Integer> gave, Map<Rarity, Integer> skipped, int nothing,
                      Map<String, Integer> items) {
        this.rolls = rolls;
        this.gave = gave;
        this.skipped = skipped;
        this.nothing = nothing;
        this.items = items;
    }

    /** From item rolls (a source with pools). */
    public static LootTally of(List<CascadeLoot.Result> results) {
        Map<Rarity, Integer> gave = new EnumMap<>(Rarity.class);
        Map<Rarity, Integer> skipped = new EnumMap<>(Rarity.class);
        Map<String, Integer> items = new TreeMap<>();
        int nothing = 0;
        for (CascadeLoot.Result r : results) {
            for (Rarity s : r.fellThrough()) skipped.merge(s, 1, Integer::sum);
            if (r.gave()) {
                gave.merge(r.rarity(), 1, Integer::sum);
                items.merge(r.item(), 1, Integer::sum);
            } else {
                nothing++;
            }
        }
        return new LootTally(results.size(), gave, skipped, nothing, Collections.unmodifiableMap(items));
    }

    /** From rarity-only rolls (the default table, no pools): a null is a roll that came to nothing. */
    public static LootTally ofRarities(List<Rarity> results) {
        Map<Rarity, Integer> gave = new EnumMap<>(Rarity.class);
        int nothing = 0;
        for (Rarity r : results) {
            if (r == null) nothing++;
            else gave.merge(r, 1, Integer::sum);
        }
        return new LootTally(results.size(), gave, new EnumMap<>(Rarity.class), nothing, Map.of());
    }

    public int rolls() { return rolls; }

    public int gave(Rarity r) { return gave.getOrDefault(r, 0); }

    public int skipped(Rarity r) { return skipped.getOrDefault(r, 0); }

    public int nothing() { return nothing; }

    /** Each item id given, and how many times, by id. */
    public Map<String, Integer> items() { return items; }

    public int itemsGiven() {
        int n = 0;
        for (int c : items.values()) n += c;
        return n;
    }
}
