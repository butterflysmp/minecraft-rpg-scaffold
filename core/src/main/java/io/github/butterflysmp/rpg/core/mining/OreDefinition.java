package io.github.butterflysmp.rpg.core.mining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * <p><b>S2 (the seat's rulings 3, 6 and 8, 2026-11-14):</b> {@code sizes} is a map of size id to a
 * roll WEIGHT, an integer at least 1, kept in authored order ({@link SizeRoll} walks it in that
 * order); each size has its own patch model; and the ore names a DENIED sound.
 *
 * @param id              the ore id
 * @param displayName     shown to players
 * @param payoutItem      the vanilla item a break pays, e.g. {@code raw_iron}. Plain, NOT minted
 * @param perSpotMin      fewest items per spot, at least 0
 * @param perSpotMax      most items per spot, at least {@code perSpotMin}
 * @param completionBonus items added per break, keyed by vein size id. Its keys must equal the
 *                        sizes exactly (the seat's ruling 2, 2026-11-13)
 * @param regrowthTicks   server ticks before a broken patch regrows, at least 1
 * @param sizeWeights     the vein sizes this ore can roll, each with its relative roll weight (at
 *                        least 1), in authored order. At least one
 * @param patchModels     the item_model id for the patch, keyed by vein size id; keys equal the sizes
 * @param spotModel       the item_model id for a live spot
 * @param crackModel      the item_model id for a hit spot
 * @param missSound       the sound key a Miss plays. A PLACEHOLDER
 * @param breakSound      the sound key a Broke plays. A PLACEHOLDER
 * @param deniedSound     the sound key a DENIED hold plays when it starts with none live (S2). A PLACEHOLDER
 * @param breakParticle   the particle key a Broke shows. A PLACEHOLDER (the seat's ruling 7)
 */
public record OreDefinition(String id, String displayName, String payoutItem,
                            int perSpotMin, int perSpotMax, Map<String, Integer> completionBonus,
                            int regrowthTicks, Map<String, Integer> sizeWeights,
                            Map<String, String> patchModels, String spotModel, String crackModel,
                            String missSound, String breakSound, String deniedSound, String breakParticle) {

    public OreDefinition {
        requireText(id, "id", "?");
        requireText(displayName, "display_name", id);
        requireText(payoutItem, "payout.item", id);
        requireText(spotModel, "models.spot", id);
        requireText(crackModel, "models.crack", id);
        requireText(missSound, "sounds.miss", id);
        requireText(breakSound, "sounds.break", id);
        requireText(deniedSound, "sounds.denied", id);
        requireText(breakParticle, "particles.break", id);
        if (regrowthTicks < 1) {
            throw new IllegalArgumentException("ore '" + id + "' has regrowth_ticks " + regrowthTicks
                    + "; it must be at least 1");
        }
        if (sizeWeights == null || sizeWeights.isEmpty()) {
            throw new IllegalArgumentException("ore '" + id + "' lists no sizes; it could never appear");
        }
        // Authored order is kept: Map.copyOf would lose it, and the roll walks the sizes in order.
        sizeWeights = Collections.unmodifiableMap(new LinkedHashMap<>(sizeWeights));
        for (Map.Entry<String, Integer> e : sizeWeights.entrySet()) {
            // NAME THE QUANTITY: a relative roll weight, a whole number. 0 would list a size that can
            // never roll, which is a size the ore does not have; refuse it rather than ship it silently.
            if (e.getValue() == null || e.getValue() < 1) {
                throw new IllegalArgumentException("ore '" + id + "' has size '" + e.getKey() + "' with weight "
                        + e.getValue() + "; a roll weight must be a whole number at least 1");
            }
        }
        completionBonus = Map.copyOf(completionBonus == null ? Map.of() : completionBonus);
        patchModels = Map.copyOf(patchModels == null ? Map.of() : patchModels);

        // THE BONUS MAP AND THE MODEL MAP MUST EACH NAME THE SIZE SET, IN BOTH DIRECTIONS.
        sameKeys(id, "completion_bonus", completionBonus.keySet(), sizeWeights.keySet());
        sameKeys(id, "models.patch", patchModels.keySet(), sizeWeights.keySet());
        for (Map.Entry<String, String> e : patchModels.entrySet()) {
            requireText(e.getValue(), "models.patch." + e.getKey(), id);
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

    private static void sameKeys(String id, String key, Set<String> have, Set<String> sizes) {
        Set<String> missing = new TreeSet<>(sizes);
        missing.removeAll(have);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("ore '" + id + "' lists size(s) " + missing
                    + " in sizes: but has no " + key + " entry for them");
        }
        Set<String> extra = new TreeSet<>(have);
        extra.removeAll(sizes);
        if (!extra.isEmpty()) {
            throw new IllegalArgumentException("ore '" + id + "' has a " + key + " entry for size(s) "
                    + extra + " that are not in its sizes:");
        }
    }

    private static void requireText(String value, String key, String id) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ore '" + id + "' has a blank or missing " + key);
        }
    }

    /** The size ids this ore can roll, in authored order. */
    public List<String> sizes() {
        return List.copyOf(new ArrayList<>(sizeWeights.keySet()));
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

    /** The patch model for a size this ore lists. */
    public String patchModel(String sizeId) {
        String model = patchModels.get(sizeId);
        if (model == null) throw new IllegalArgumentException("ore '" + id + "' does not list size '" + sizeId + "'");
        return model;
    }
}
