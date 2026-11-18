package io.github.butterflysmp.rpg.core.mob;

/**
 * A DISTANCE THAT FOLLOWS A STAGED MOB'S SIZE (the seat's ruling on #204, Part C item 3). Ben: "make the size of the
 * Phage affect the Fuse radius, explosion radius, etc. The Large phage being the current distances and scaling
 * everything down to the small phage."
 *
 * <p><b>NAME THE QUANTITY:</b> BLOCKS. {@code last} is the LAST stage's distance, the one content authors. With no
 * {@code first} ({@link #byHeight}), stage n's distance is {@code last x height(n) / height(last)}: the body's HEIGHT
 * is the size it follows (the seat's reading, put to Ben in those terms). With an authored {@code first}, the stages
 * step LINEARLY from it to {@code last}, as the stage bodies do (G3): the content-only way to tune the small one apart.
 */
public record StagedDistance(Double first, double last) {

    public StagedDistance {
        if (!(last > 0 && Double.isFinite(last))) {
            throw new IllegalArgumentException("a staged distance's last stage must be a positive number of blocks, was "
                    + last);
        }
        if (first != null && !(first > 0 && Double.isFinite(first))) {
            throw new IllegalArgumentException("a staged distance's first stage must be a positive number of blocks, was "
                    + first);
        }
    }

    /** The last stage's distance, every earlier stage's by its height. */
    public static StagedDistance byHeight(double last) {
        return new StagedDistance(null, last);
    }

    /** Stage {@code stage}'s distance, BLOCKS. A stage outside the table is refused, never clamped. */
    public double at(int stage, MobStages stages) {
        if (!stages.isValid(stage)) {
            throw new IllegalArgumentException("stage " + stage + " is outside 1.." + stages.count());
        }
        if (first == null) return last * stages.at(stage).height() / stages.last().height();
        double t = (double) (stage - 1) / (stages.count() - 1);
        return first + (last - first) * t;
    }
}
