package io.github.butterflysmp.rpg.core.build;

/**
 * Is this Ability Stone input a RECAST? PLAN-build-system.md section 7.4, rulings 28-30.
 *
 * <p>An aspect's {@code recast:} lets the player cast a follow-up by pressing its target's input again,
 * shortly after casting it. The input is a recast when ALL of these hold:
 * <ul>
 *   <li>the window is unused -- <b>one recast per cast</b>;</li>
 *   <li>{@code fromTicks <= inputTick - castTick <= windowTicks}, both edges inclusive (ruling 29: 50 ticks;
 *       the floor of 10 is the seat's, and lets the target's own dash finish first);</li>
 *   <li><b>the input's hold began AFTER the cast</b> ({@link InputHold}). Ruling 19 re-casts on every held
 *       input, so without this a held click would fire the recast the instant the floor allowed it.</li>
 * </ul>
 *
 * <p>Pure: the paper side records ticks and asks.
 */
public final class RecastRule {

    private RecastRule() {}

    /**
     * @param castTick       the tick of the input that cast the target
     * @param inputTick      the tick of this input
     * @param used           this window already carried its one recast
     * @param inputHoldStart the tick this input's hold began ({@link InputHold#holdStartTick})
     */
    public static boolean accepts(long castTick, long inputTick, int fromTicks, int windowTicks, boolean used,
                                  long inputHoldStart) {
        if (used) return false;
        long since = inputTick - castTick;
        return since >= fromTicks && since <= windowTicks && inputHoldStart > castTick;
    }
}
