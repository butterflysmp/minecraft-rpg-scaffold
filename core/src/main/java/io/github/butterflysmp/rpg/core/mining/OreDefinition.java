package io.github.butterflysmp.rpg.core.mining;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * An ore, loaded from {@code content/ores/}. Slice 1 ships only {@code iron} (Ben: "we'll do iron").
 *
 * <p>The refusals here need no server. Whether a size id names a loaded vein size, whether the
 * payout item is a real item, and whether a sound key resolves are ContentValidator's, because they
 * need the other registry or the Bukkit registry.
 *
 * @param id              the ore id
 * @param displayName     shown to players
 * @param payoutItem      the vanilla item a break pays, e.g. {@code raw_iron}. Plain, NOT minted
 * @param perSpotMin      fewest items per spot, at least 0
 * @param perSpotMax      most items per spot, at least {@code perSpotMin}
 * @param completionBonus items added per break, keyed by vein size id. Its keys must equal
 *                        {@code sizes} exactly (the seat's ruling 2, 2026-11-13)
 * @param regrowthTicks   server ticks before a broken patch regrows, at least 1
 * @param sizes           the vein sizes this ore can roll, at least one
 * @param patchModel      the item_model id for the patch
 * @param spotModel       the item_model id for a live spot
 * @param crackModel      the item_model id for a hit spot
 * @param missSound       the sound key a Miss plays. A PLACEHOLDER in slice 1
 * @param breakSound      the sound key a Broke plays. A PLACEHOLDER in slice 1
 */
public record OreDefinition(String id, String displayName, String payoutItem,
                            int perSpotMin, int perSpotMax, Map<String, Integer> completionBonus,
                            int regrowthTicks, List<String> sizes,
                            String patchModel, String spotModel, String crackModel,
                            String missSound, String breakSound) {

    public OreDefinition {
        requireText(id, "id", "?");
        requireText(displayName, "display_name", id);
        requireText(payoutItem, "payout.item", id);
        requireText(patchModel, "models.patch", id);
        requireText(spotModel, "models.spot", id);
        requireText(crackModel, "models.crack", id);
        requireText(missSound, "sounds.miss", id);
        requireText(breakSound, "sounds.break", id);
        if (regrowthTicks < 1) {
            throw new IllegalArgumentException("ore '" + id + "' has regrowth_ticks " + regrowthTicks
                    + "; it must be at least 1");
        }
        if (sizes == null || sizes.isEmpty()) {
            throw new IllegalArgumentException("ore '" + id + "' lists no sizes; it could never appear");
        }
        sizes = List.copyOf(sizes);
        completionBonus = Map.copyOf(completionBonus == null ? Map.of() : completionBonus);

        // THE BONUS MAP AND THE SIZE LIST MUST NAME THE SAME SET, IN BOTH DIRECTIONS.
        Set<String> missing = new TreeSet<>(sizes);
        missing.removeAll(completionBonus.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("ore '" + id + "' lists size(s) " + missing
                    + " in sizes: but has no completion_bonus entry for them");
        }
        Set<String> extra = new TreeSet<>(completionBonus.keySet());
        extra.removeAll(sizes);
        if (!extra.isEmpty()) {
            throw new IllegalArgumentException("ore '" + id + "' has a completion_bonus for size(s) "
                    + extra + " that are not in its sizes:");
        }
        // Every bonus is validated by building the rule it feeds, so the two cannot disagree.
        for (Map.Entry<String, Integer> e : completionBonus.entrySet()) {
            try {
                new PayoutRule(perSpotMin, perSpotMax, e.getValue());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("ore '" + id + "' (size " + e.getKey() + "): " + ex.getMessage());
            }
        }
    }

    private static void requireText(String value, String key, String id) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ore '" + id + "' has a blank or missing " + key);
        }
    }

    /** The completion bonus for a size this ore lists. */
    public int completionBonus(String sizeId) {
        Integer bonus = completionBonus.get(sizeId);
        if (bonus == null) throw new IllegalArgumentException("ore '" + id + "' does not list size '" + sizeId + "'");
        return bonus;
    }

    /** The payout rule for a patch of this ore at that size. */
    public PayoutRule payoutRule(String sizeId) {
        return new PayoutRule(perSpotMin, perSpotMax, completionBonus(sizeId));
    }
}
