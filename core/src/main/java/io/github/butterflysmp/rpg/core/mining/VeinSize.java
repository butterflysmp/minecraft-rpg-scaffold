package io.github.butterflysmp.rpg.core.mining;

/**
 * A vein size: the patch's footprint in blocks and its spot-count range. Loaded from
 * {@code content/vein_sizes/}. Slice 1 ships only {@code small}: 3 wide, 2 high, 2 to 3 spots
 * (Ben, 2026-11-13: "yes").
 *
 * <p>NAME THE QUANTITY: width and height are whole blocks of wall; the spot counts are whole
 * spots. The refusals here need no server, so they are unit-tested.
 *
 * <h2>The spot ceiling is the WORST-CASE guarantee, not the perfect tiling</h2>
 * The seat's F-A, 2026-11-13. {@link SpotLayout} places spots at random, and a random packing can
 * stop far short of a perfect tiling, so "how many 5 x 5 boxes tile the patch" promised more than
 * it could keep. The bound refused here is the number that EVERY layout can reach:
 * <ul>
 *   <li>Centres lie in an {@code aw x ah} grid of positions, with {@code aw = widthPx - 2 * margin}
 *       and {@code ah = heightPx - 2 * margin} (margin = {@link SpotLayout#EDGE_MARGIN_PX}).</li>
 *   <li>A placed centre blocks every centre at Chebyshev distance under the spacing
 *       ({@link SpotLayout#SPOT_SPACING_PX} = 5): a square of side {@code 2 * (5 - 1) + 1 = 9}.</li>
 *   <li>Take the grid points {@code (9i, 9j)}. There are {@code ceil(aw / 9) * ceil(ah / 9)} of
 *       them inside the grid, and any two differ by at least 9 on some axis, so no single 9 x 9
 *       square holds two of them.</li>
 *   <li>So fewer spots than that cannot block all of those points, and a free legal position
 *       always remains. Any {@code maxSpots} at or under the bound can be laid out.</li>
 * </ul>
 * For Small: {@code aw = 44}, {@code ah = 28}, so {@code ceil(44/9) * ceil(28/9) = 5 * 4 = 20}.
 * Small asks for at most 3. The rejection sampler still has to FIND the free position; its
 * {@code IllegalStateException} stays as the loud backstop.
 *
 * @param id       the size id an ore's {@code sizes:} names
 * @param width    blocks along the face, at least 1
 * @param height   blocks up the face, at least 1
 * @param minSpots fewest spots a patch rolls, at least 1
 * @param maxSpots most spots a patch rolls, at least {@code minSpots}, at most {@link #guaranteedSpots()}
 */
public record VeinSize(String id, int width, int height, int minSpots, int maxSpots) {

    public VeinSize {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("vein size id must not be blank");
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("vein size '" + id + "' must be at least 1 x 1 blocks, got "
                    + width + " x " + height);
        }
        if (minSpots < 1) {
            throw new IllegalArgumentException("vein size '" + id + "' needs at least 1 spot, got min " + minSpots);
        }
        if (maxSpots < minSpots) {
            throw new IllegalArgumentException("vein size '" + id + "' has spots.min " + minSpots
                    + " above spots.max " + maxSpots);
        }
        int guaranteed = guaranteedSpots(width, height);
        if (maxSpots > guaranteed) {
            throw new IllegalArgumentException("vein size '" + id + "' asks for up to " + maxSpots
                    + " spots, but every random layout of a " + width + " x " + height
                    + " patch is only guaranteed to fit " + guaranteed);
        }
    }

    public int widthPx() { return width * 16; }

    public int heightPx() { return height * 16; }

    /** The worst-case spot count every layout of this size can reach (see the class javadoc). */
    public int guaranteedSpots() {
        return guaranteedSpots(width, height);
    }

    private static int guaranteedSpots(int width, int height) {
        int blocked = 2 * (SpotLayout.SPOT_SPACING_PX - 1) + 1;          // 9
        int aw = width * 16 - 2 * SpotLayout.EDGE_MARGIN_PX;             // centre positions across
        int ah = height * 16 - 2 * SpotLayout.EDGE_MARGIN_PX;            // centre positions up
        return ceilDiv(aw, blocked) * ceilDiv(ah, blocked);
    }

    private static int ceilDiv(int a, int b) {
        return Math.floorDiv(a + b - 1, b);
    }
}
