package io.github.butterflysmp.rpg.core.mining;

/**
 * A vein size: the patch's footprint in blocks and its spot-count range. Loaded from
 * {@code content/vein_sizes/}. Slice 1 ships only {@code small}: 3 wide, 2 high, 2 to 3 spots
 * (Ben, 2026-11-13: "yes").
 *
 * <p>NAME THE QUANTITY: width and height are whole blocks of wall; the spot counts are whole
 * spots. The refusals here need no server, so they are unit-tested.
 *
 * @param id       the size id an ore's {@code sizes:} names
 * @param width    blocks along the face, at least 1
 * @param height   blocks up the face, at least 1
 * @param minSpots fewest spots a patch rolls, at least 1
 * @param maxSpots most spots a patch rolls, at least {@code minSpots}, at most {@link #tileCapacity()}
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
        int capacity = (width * 16 / SpotBox.BOX_PX) * (height * 16 / SpotBox.BOX_PX);
        if (maxSpots > capacity) {
            throw new IllegalArgumentException("vein size '" + id + "' asks for up to " + maxSpots
                    + " spots, but a " + width + " x " + height + " patch has room for only " + capacity
                    + " non-overlapping 5 x 5 boxes, so a layout could not be guaranteed");
        }
    }

    public int widthPx() { return width * 16; }

    public int heightPx() { return height * 16; }

    /**
     * How many disjoint 5 x 5 tiles fit on the patch: {@code floor(widthPx / 5) * floor(heightPx / 5)}.
     * A {@code maxSpots} at or under this can always be laid out.
     */
    public int tileCapacity() {
        return (widthPx() / SpotBox.BOX_PX) * (heightPx() / SpotBox.BOX_PX);
    }
}
