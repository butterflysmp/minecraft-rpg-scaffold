package io.github.butterflysmp.rpg.core.build;

/**
 * THE Q-SWING GUARD: a swing in the SAME TICK as an Ability Stone drop attempt never casts Active 1.
 *
 * <h2>Why it exists -- measured, not suspected</h2>
 *
 * Every Q-drop the spike measured at {@code d7b4087} came with an arm swing in the same server tick,
 * from the hotbar and from an open inventory, in survival and creative (PLAN-build-system.md section
 * 3.1.0.1). Unguarded, one Q press casts the Ultimate AND Active 1.
 *
 * <h2>Why the caller decides a tick LATE</h2>
 *
 * From the hotbar the drop arrives BEFORE the swing; from an open inventory the swing arrives BEFORE
 * the drop click. A decision taken at the swing would miss the second case. So the paper side records
 * the swing's tick, waits one tick, and only then asks this -- by which time any drop attempt from
 * the same tick has been recorded.
 */
public final class SwingGuard {

    /** "No drop attempt recorded yet." No real server tick equals it. */
    public static final long NEVER = Long.MIN_VALUE;

    private SwingGuard() {}

    /**
     * @param swingTick           the server tick the swing arrived in
     * @param lastDropAttemptTick the tick of this player's most recent stone drop attempt, or {@link #NEVER}
     * @return true if the swing may cast Active 1
     */
    public static boolean castsActive1(long swingTick, long lastDropAttemptTick) {
        return swingTick != lastDropAttemptTick;
    }
}
