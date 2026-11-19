package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.loot.CascadeTable;
import io.github.butterflysmp.rpg.core.weapon.Rarity;

import java.util.List;
import java.util.Map;

/**
 * ONE CASCADE FILE (PLAN-loot.md, d): its default table and its sources. Nothing in the game reads it yet.
 *
 * @param id           the file's id
 * @param defaultTable the table every source uses unless it writes its own tiers
 * @param sources      each drop source by id: its table (its own tiers, or the default's) and its pool per rarity
 */
public record LootContent(String id, CascadeTable defaultTable, Map<String, Source> sources) {

    /** A source: the table it rolls, and per rarity the item ids it can give. */
    public record Source(CascadeTable table, Map<Rarity, List<String>> pools) {}
}
