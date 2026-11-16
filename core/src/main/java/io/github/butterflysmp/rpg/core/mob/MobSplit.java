package io.github.butterflysmp.rpg.core.mob;

/**
 * A staged mob that SPLITS when it dies at its LAST stage: the Phage (PLAN-dragon-mobs.md step R2). Ben: "Only the
 * 5th stage Phage splits"; a killed stage 5 splits into two stage 1s (G4); "Any deaths" split it; its own death does
 * not grow the children ("No,"). Authored in the mob file's {@code split:} block.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code count}: how many children, a whole number of mobs, at least 1. The Phage's is 2.</li>
 *   <li>{@code childStage}: the stage each child is born at, one of the mob's own stages. The Phage's is 1.</li>
 *   <li>{@code spread}: BLOCKS from the parent to the nearest child, along one axis (x). Two children sit at
 *       {@code -spread} and {@code +spread}; more are spaced {@code 2 x spread} apart, centred on the parent. The
 *       Phage's is 0.5, the old game's offset, put to Ben as a starting number and not yet confirmed by him (P6).</li>
 * </ul>
 * WHICH stage splits is not a field: it is always the LAST, as Ben ruled, and the file says so ({@code at: last}).
 */
public record MobSplit(int count, int childStage, double spread) {

    public MobSplit {
        if (count < 1) throw new IllegalArgumentException("split.count must be at least 1, was " + count);
        if (childStage < 1) throw new IllegalArgumentException("split.child_stage must be at least 1, was " + childStage);
        if (!(spread >= 0 && Double.isFinite(spread))) {
            throw new IllegalArgumentException("split.spread must be 0 or more blocks, was " + spread);
        }
    }

    /** What one death produces: how many children, at what stage. {@link #NONE} when it produces none. */
    public record Children(int count, int stage) {
        public static final Children NONE = new Children(0, 0);
    }

    /**
     * THE RULE. {@code def} may be null (a vanilla mob). {@code alreadySplit} is the once-only guard: the death event
     * can be delivered twice for one mob (a cancelled death revives it), and a mob that has split never splits again.
     */
    public static Children childrenFor(MobDefinition def, int stageAtDeath, boolean alreadySplit) {
        if (alreadySplit || def == null || def.split() == null || def.stages() == null) return Children.NONE;
        if (stageAtDeath != def.stages().count()) return Children.NONE;
        return new Children(def.split().count(), def.split().childStage());
    }

    /** Each child's offset from the parent along x, in blocks, centred on the parent. */
    public double[] offsets() {
        double[] out = new double[count];
        for (int i = 0; i < count; i++) out[i] = (i - (count - 1) / 2.0) * 2.0 * spread;
        return out;
    }

    /** A child is the parent's shape at another height, so its width scales with its height. */
    public static double childWidth(double parentWidth, double parentHeight, double childHeight) {
        return parentWidth * childHeight / parentHeight;
    }
}
