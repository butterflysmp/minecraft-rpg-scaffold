package io.github.butterflysmp.rpg.core.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * A patch's rolled spots, in reveal order. Re-rolled at every regrowth.
 *
 * <p>Two rules, both the seat's rulings of 2026-11-13 (PLAN-mining.md section 2, ruling 6):
 * <ul>
 *   <li><b>{@link #EDGE_MARGIN_PX} = 2:</b> a centre is at least 2 pixels from each edge pixel, so
 *       its 5 x 5 box lies wholly on the patch;</li>
 *   <li><b>{@link #SPOT_SPACING_PX} = 5:</b> two centres are at least 5 apart in the larger of
 *       their two axis distances (Chebyshev), so no two boxes overlap.</li>
 * </ul>
 *
 * @param spots in reveal order
 */
public record SpotLayout(List<Spot> spots) {

    public static final int EDGE_MARGIN_PX = 2;
    public static final int SPOT_SPACING_PX = 5;

    /** Tries per spot before a roll gives up. A legal size never comes close (see VeinSize). */
    static final int MAX_ATTEMPTS_PER_SPOT = 1000;

    public SpotLayout {
        spots = List.copyOf(spots);
    }

    /**
     * Rolls a layout: the count uniformly from {@code minSpots..maxSpots} inclusive, then each
     * centre uniformly over the margin-bounded area, re-drawn until it is far enough from every
     * spot already placed.
     *
     * @throws IllegalStateException if a spot cannot be placed in {@link #MAX_ATTEMPTS_PER_SPOT}
     *         tries. It fails loudly rather than looping forever.
     */
    public static SpotLayout roll(VeinSize size, RandomGenerator rng) {
        int count = rng.nextInt(size.minSpots(), size.maxSpots() + 1);
        List<Spot> placed = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Spot candidate = null;
            for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_SPOT; attempt++) {
                Spot s = new Spot(
                        rng.nextInt(EDGE_MARGIN_PX, size.widthPx() - EDGE_MARGIN_PX),
                        rng.nextInt(EDGE_MARGIN_PX, size.heightPx() - EDGE_MARGIN_PX));
                if (fitsEdge(s, size) && farEnough(s, placed)) {
                    candidate = s;
                    break;
                }
            }
            if (candidate == null) {
                throw new IllegalStateException("could not place spot " + (i + 1) + " of " + count
                        + " on vein size '" + size.id() + "' in " + MAX_ATTEMPTS_PER_SPOT + " tries");
            }
            placed.add(candidate);
        }
        return new SpotLayout(placed);
    }

    /** The edge rule: the centre is in {@code [2, widthPx - 3] x [2, heightPx - 3]}. */
    static boolean fitsEdge(Spot s, VeinSize size) {
        return s.cu() >= EDGE_MARGIN_PX && s.cu() <= size.widthPx() - 1 - EDGE_MARGIN_PX
                && s.cv() >= EDGE_MARGIN_PX && s.cv() <= size.heightPx() - 1 - EDGE_MARGIN_PX;
    }

    /** The spacing rule: Chebyshev distance at least 5 from every spot already placed. */
    static boolean farEnough(Spot s, List<Spot> placed) {
        for (Spot other : placed) {
            int chebyshev = Math.max(Math.abs(s.cu() - other.cu()), Math.abs(s.cv() - other.cv()));
            if (chebyshev < SPOT_SPACING_PX) return false;
        }
        return true;
    }
}
