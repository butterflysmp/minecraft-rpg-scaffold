package io.github.butterflysmp.rpg.core.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Which vein size a new patch takes (Mining S2, the seat's rulings 1 to 3, 2026-11-14).
 *
 * <ul>
 *   <li><b>A slot has a MAXIMUM size</b>: the size {@code /rpg mining place} named.</li>
 *   <li><b>The FIT rule:</b> a size is eligible at a slot when its width AND its height are both at
 *       most the maximum's, and the ore lists it.</li>
 *   <li><b>The ROLL:</b> one draw from {@code [0, sum of the ELIGIBLE weights)}, walked over the
 *       eligible sizes in the ore's authored order. Weights are renormalised over the eligible sizes
 *       by construction: an ineligible size has no slice of the draw at all.</li>
 * </ul>
 *
 * <p>NAME THE QUANTITY: a weight is a relative integer, at least 1 ({@link OreDefinition} refuses
 * anything less). A size's chance at a slot is its weight over the sum of the eligible weights; for
 * iron at a medium slot that is small 50/80, medium 30/80.
 */
public final class SizeRoll {

    private SizeRoll() {}

    /** The fit rule: both dimensions at most the maximum's. */
    public static boolean fits(VeinSize size, VeinSize max) {
        return size.width() <= max.width() && size.height() <= max.height();
    }

    /**
     * The sizes this ore can roll at a slot whose maximum is {@code max}, in the ore's authored order.
     * A listed size that did not load is skipped (ContentValidator names it at boot).
     */
    public static List<VeinSize> eligible(OreDefinition ore, VeinSize max, VeinSizeRegistry sizes) {
        List<VeinSize> out = new ArrayList<>();
        for (String id : ore.sizes()) {
            Optional<VeinSize> size = sizes.find(id);
            if (size.isPresent() && fits(size.get(), max)) out.add(size.get());
        }
        return out;
    }

    /**
     * Rolls one size.
     *
     * @throws IllegalStateException when no size is eligible. UNREACHABLE from shipped content:
     *         {@code /rpg mining place} refuses a maximum the ore does not list, and a listed maximum
     *         always fits itself. {@code SizeRollTest} is its only exercise.
     */
    public static VeinSize roll(OreDefinition ore, VeinSize max, VeinSizeRegistry sizes, RandomGenerator rng) {
        List<VeinSize> eligible = eligible(ore, max, sizes);
        if (eligible.isEmpty()) {
            throw new IllegalStateException("ore '" + ore.id() + "' lists no size that fits a slot of maximum '"
                    + max.id() + "'");
        }
        int total = 0;
        for (VeinSize s : eligible) total += ore.sizeWeights().get(s.id());
        int draw = rng.nextInt(0, total);
        for (VeinSize s : eligible) {
            int weight = ore.sizeWeights().get(s.id());
            if (draw < weight) return s;
            draw -= weight;
        }
        throw new IllegalStateException("unreachable: the draw is below the eligible total");
    }
}
