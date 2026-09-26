package io.github.butterflysmp.rpg.core.build;

/**
 * Is this input part of a HOLD, and when did that hold start? PLAN-build-system.md section 7.4.
 *
 * <p><b>A hold is a stream of inputs on one button whose gaps never exceed {@link #MAX_GAP_TICKS}.</b> The
 * spike measured a held right-click as an input exactly every 4 ticks, a held left click on a block as one
 * every tick, and a held left click in air as one swing and nothing more (section 3.1.0.1). So a hold can
 * never show a gap over 8, and this never mistakes a hold for a new press.
 *
 * <p><b>The error it does make runs the other way, and it is only a DELAY.</b> Separate presses can come 1 tick
 * apart, so a player mashing faster than every 8 ticks reads as holding until they pause. That is why ruling
 * 19 could not use a gap to tell EVERY press from a hold, and why this can: it only needs one direction.
 *
 * <p>8 is twice the measured held period. The margin is for network jitter and is NOT measured: the spike ran
 * on localhost, where the gap was exactly 4.
 *
 * <p>A record: an immutable value. {@link #next} returns the state after one more input.
 */
public record InputHold(long lastInputTick, long holdStartTick) {

    /** The largest gap, in ticks, that still continues a hold. */
    public static final int MAX_GAP_TICKS = 8;

    /** No input yet. No real server tick equals it. */
    public static final InputHold NONE = new InputHold(Long.MIN_VALUE, Long.MIN_VALUE);

    /** The state after an input at {@code tick}: it continues the current hold, or starts a new one. */
    public InputHold next(long tick) {
        boolean continues = lastInputTick != Long.MIN_VALUE && tick - lastInputTick <= MAX_GAP_TICKS;
        return new InputHold(tick, continues ? holdStartTick : tick);
    }
}
