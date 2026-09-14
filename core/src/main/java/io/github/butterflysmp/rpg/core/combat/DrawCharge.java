package io.github.butterflysmp.rpg.core.combat;

import java.util.Optional;

/**
 * How long a bow has been held, in arrows -- and what each arrow gained should SOUND like.
 *
 * <p>The Dragon's Plume's charge, as arithmetic. Slice H1 owns the input layer in {@code paper}
 * because it reads a live player; <b>every DECISION in it lives here</b>, where a test can reach it
 * without a server -- the same split {@code Durability} / {@code WeaponDurability} argues for.
 *
 * <h2>THE THREE NUMBERS, AND WHERE EACH ONE COMES FROM</h2>
 *
 * <pre>
 * FULL_DRAW_TICKS      20   MEASURED from the pinned jar: BowItem.MAX_DRAW_DURATION, and
 *                           getPowerForTime(20) = 1.0 exactly. A platform fact, not a choice.
 * CHARGE_SECOND_TICKS  20   RULED (R6, and PLAN-dragons-plume.md 7.5): `c`, the charge second.
 *                           It equals the draw duration by coincidence of both being one second;
 *                           they are different quantities and are kept apart.
 * MAX_ARROWS            5   RULED.
 * </pre>
 *
 * <p><b>The two 20s are not the same 20.</b> One is how long vanilla takes to reach full draw; the
 * other is how long a held bow takes to earn its next arrow. A future ruling can move either without
 * the other, which is why they are two constants and not one.
 *
 * <h2>R6: FULL CHARGE IS ARROW ONE, AND IT IS THE OFF-BY-ONE THIS FILE EXISTS TO FIX IN PUBLIC</h2>
 *
 * <pre>
 * held  19 ticks -> 0 arrows      not yet at full draw
 * held  20 ticks -> 1 arrow       R6: reaching full charge GIVES one, it does not start a count
 * held  39 ticks -> 1 arrow
 * held  40 ticks -> 2 arrows      and one per charge second after
 * held 100 ticks -> 5 arrows      the cap
 * held 999 ticks -> 5 arrows
 * </pre>
 */
public final class DrawCharge {

    private DrawCharge() {}

    /** MEASURED: {@code BowItem.MAX_DRAW_DURATION} on the pinned jar. */
    public static final int FULL_DRAW_TICKS = 20;

    /** RULED: {@code c}, the charge second. */
    public static final int CHARGE_SECOND_TICKS = 20;

    /** RULED: five arrows, and the quiver pays one round each. */
    public static final int MAX_ARROWS = 5;

    /**
     * R13's pitch span. The mapping is GEOMETRIC because pitch is a playback RATE, so what an ear
     * hears as a step is the RATIO between two pitches and not the difference.
     *
     * <p>A mapping with even DIFFERENCES has shrinking steps, and it shrinks exactly where the count
     * matters most -- at four and five, when the player is deciding whether to let go:
     *
     * <pre>
     *              1      2      3      4      5     steps, in semitones (12 log2 of the ratio)
     * linear    1.00   1.25   1.50   1.75   2.00     3.86  3.16  2.67  2.31   <- top step 40% smaller
     * RULED     0.80   1.01   1.27   1.59   2.00     3.86  3.97  3.97  3.97   <- even, ~a major third
     * </pre>
     *
     * <p><b>The five values in {@code PLAN-dragons-plume.md} are this formula rounded for display.</b>
     * The formula is the ruled thing -- <i>authored for SEPARATION</i> -- and it is what ships, so
     * the tests assert the PROPERTY (equal ratios, both endpoints) rather than pinning a rounding.
     *
     * <p><b>{@link #PITCH_CEILING} at 2.0 is OUTSIDE KNOWLEDGE THIS MACHINE CANNOT MEASURE.</b> The
     * pinned API documents no range for {@code pitch} and {@code ClientboundSoundPacket} carries a
     * raw float, so any cap is the client's. If it is wrong the mapping still works; it simply stops
     * short of the top. Do not restate it as a fact of the platform.
     */
    public static final double PITCH_FLOOR = 0.8;
    public static final double PITCH_CEILING = 2.0;

    /**
     * How many arrows a draw held for {@code heldTicks} has EARNED, before the magazine is consulted.
     *
     * <p>This is the time half only. R3's cap is the other half and is applied by
     * {@link #capped(int, int)}, because it must be re-read from the live magazine as the count
     * climbs rather than once at the end.
     */
    public static int arrowsFor(int heldTicks) {
        if (heldTicks < FULL_DRAW_TICKS) return 0;
        int afterFullDraw = heldTicks - FULL_DRAW_TICKS;
        return Math.min(MAX_ARROWS, 1 + afterFullDraw / CHARGE_SECOND_TICKS);
    }

    /**
     * R3: the tracker never climbs past what the quiver can pay for.
     *
     * <p>{@code roundsRemaining} is {@code QuiverState.roundsRemaining()} -- slice G's accessor, read
     * LIVE from the held item each time the tracker would tick. <b>Two rounds left means the ticks
     * stop at two, and the sound never promises an arrow that is not coming.</b>
     *
     * <p>Negative is floored to zero rather than refused: a magazine cannot owe arrows, and a
     * caller that somehow obtains one should get a dead tracker, not an exception mid-draw.
     */
    public static int capped(int earned, int roundsRemaining) {
        return Math.max(0, Math.min(earned, roundsRemaining));
    }

    /**
     * What the {@code n}th arrow sounds like: {@code PITCH_FLOOR * r^(n-1)}, where {@code r} is the
     * ratio that lands the {@link #MAX_ARROWS}th exactly on {@link #PITCH_CEILING}.
     *
     * <p>Computed rather than tabulated, so changing {@code MAX_ARROWS} re-spaces the ladder instead
     * of running off the end of a five-element array.
     *
     * @param arrowsReady 1-based; clamped into {@code 1..MAX_ARROWS} so a caller cannot produce a
     *                    pitch outside the span by miscounting.
     */
    public static float pitchFor(int arrowsReady) {
        int n = Math.max(1, Math.min(MAX_ARROWS, arrowsReady));
        if (MAX_ARROWS <= 1) return (float) PITCH_FLOOR;
        double ratio = Math.pow(PITCH_CEILING / PITCH_FLOOR, 1.0 / (MAX_ARROWS - 1));
        return (float) (PITCH_FLOOR * Math.pow(ratio, n - 1));
    }

    /**
     * THE TWO MEASURES OF ONE DRAW, CHECKED AGAINST EACH OTHER AT THE RELEASE.
     *
     * <p>The tracker accumulates its own count as the draw climbs, because R3's cap has to be
     * applied AS IT CLIMBS. {@code PlayerStopUsingItemEvent} independently carries
     * {@code getTicksHeldFor()}. <b>Two measures of the same draw, arrived at by different routes.</b>
     *
     * <h2>THE TRACKER IS AUTHORITATIVE. THIS IS A CHECK, NEVER A SECOND SOURCE.</h2>
     *
     * <p>It returns a DESCRIPTION of a disagreement, not a corrected count, and the caller reports
     * it and keeps its own number. A second source of truth is how two subsystems start quietly
     * diverging; a check that only ever reports is how one of them gets found out.
     *
     * <p><b>The comparison is ONE-DIRECTIONAL, and the cap is why.</b> The tracker may legitimately
     * sit BELOW the time-derived count -- that is R3 doing its job on a short magazine, and it is the
     * normal case. It may never sit ABOVE it: no cap can manufacture an arrow, so a tracker holding
     * more than the elapsed time earned means <b>the tracker and the platform disagree about how long
     * the bow was held</b>, which has no other detector in this system.
     *
     * @return empty when the two agree (or differ only in the direction the cap explains)
     */
    public static Optional<String> disagreement(int trackerCount, int heldTicks) {
        int earned = arrowsFor(heldTicks);
        if (trackerCount <= earned) return Optional.empty();
        return Optional.of("draw tracker holds " + trackerCount + " arrows but the platform reports "
                + heldTicks + " ticks held, which earns " + earned
                + " -- no cap can manufacture an arrow, so the tracker and the server disagree about"
                + " the length of this draw");
    }
}
