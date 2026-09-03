package io.github.butterflysmp.rpg.core.weapon;

import java.util.Comparator;

/**
 * THE ONE DEFINITION of how two craftable things order relative to each other.
 *
 * <h2>WHY AN INTERFACE AND NOT A COMPARATOR ON EACH TYPE</h2>
 *
 * Three surfaces order craftable things, and they are not the same type:
 *
 * <ul>
 *   <li>the Quick Craft <b>suggestion column</b> ranks {@link CraftCount.Craftable};
 *   <li>the <b>recipe browser</b> ranks the same {@code Craftable} after filtering;
 *   <li>the <b>recipe catalogue</b> (in {@code paper}) sorts its own {@code Entry} record.
 * </ul>
 *
 * <p>Written per-type, that is three copies of "armor sorts head, chest, legs, feet" -- and
 * <b>armor is squeezed out of the three-cell column today</b>, so two of those copies would be
 * invisible in play and could disagree for a whole release without anyone noticing. That is
 * precisely how the craft path ended up needing {@code InventoryCraft}: two callers that agree
 * today. This closes it the same way -- one definition, implemented by whoever needs ordering.
 *
 * <h2>THE COLUMN AND THE BROWSER DO NOT SHARE A TOTAL ORDER, AND MUST NOT</h2>
 *
 * They share the <b>within-tier tiebreak</b>, which is what {@link #WITHIN_TIER} is:
 *
 * <pre>
 * column   tier -> COUNT (most first) -> WITHIN_TIER
 * browser  tier ->                       WITHIN_TIER
 * catalogue tier ->                      WITHIN_TIER          ({@link #TIER_FIRST})
 * </pre>
 *
 * The column leads with count because it has three cells and should spend them on what the player
 * can make most of. The browser lists everything craftable, so a count-first order would reshuffle
 * the whole list every time a player crafted one item. <b>Saying "one comparator" and meaning "one
 * total order" would have been wrong</b>; what must not be duplicated is the rule underneath both.
 */
public interface CraftOrder {

    /** The recipe key. Unique and stable across restarts, so it is the final tiebreak. */
    String key();

    /** What kind of thing it makes. The primary sort everywhere. */
    SuggestionTier tier();

    /**
     * Which body slot, when this makes armor. <b>Null for everything else</b>, which is every tier
     * other than {@link SuggestionTier#ARMOR}.
     */
    ArmorSlot armorSlot();

    /**
     * How two things in the SAME tier order: armor by body slot, then by recipe key.
     *
     * <h2>Armor sorts HEAD, CHEST, LEGS, FEET -- {@link ArmorSlot}'s declaration order</h2>
     *
     * Head-down is how a player reads a character sheet and how the vanilla inventory stacks the
     * four slots, so it is the order they expect. <b>It is emphatically not alphabetical</b>, and
     * that is what makes the gate row for it discriminating: falling back to the recipe key would
     * give boots, chestplate, helmet, leggings -- an order that looks deliberate and is wrong.
     *
     * <h2>The null arm cannot mix, and is defined anyway</h2>
     *
     * A non-armor entry sorts as {@link #NOT_ARMOR}, below every real slot. Within a single tier
     * this can never actually mix -- either the tier is {@code ARMOR} and every member has a slot,
     * or it is not and none do -- so the arm is unreachable in practice. It is still defined rather
     * than left to throw, because "unreachable today" is the assumption this repo has been wrong
     * about most often, and a comparator that throws does so from inside a sort deep in a click
     * handler.
     */
    Comparator<CraftOrder> WITHIN_TIER =
            Comparator.comparingInt(CraftOrder::armorSlotIndex)
                    .thenComparing(order -> order.key() == null ? "" : order.key());

    /**
     * Tier first, then {@link #WITHIN_TIER}. The browser's order, and the catalogue's.
     *
     * <p><b>The invariant is "all gear sorts ahead of all vanilla".</b> It is deliberately NOT
     * "page 1 is the gear page": that is arithmetic over two numbers that can both move, and nothing
     * would warn anyone when it stopped holding.
     */
    Comparator<CraftOrder> TIER_FIRST =
            Comparator.comparingInt((CraftOrder order) -> order.tier() == null
                            ? SuggestionTier.values().length
                            : order.tier().ordinal())
                    .thenComparing(WITHIN_TIER);

    /** Where a non-armor entry sorts among body slots. Below all four; see {@link #WITHIN_TIER}. */
    int NOT_ARMOR = -1;

    private int armorSlotIndex() {
        return armorSlot() == null ? NOT_ARMOR : armorSlot().ordinal();
    }
}
