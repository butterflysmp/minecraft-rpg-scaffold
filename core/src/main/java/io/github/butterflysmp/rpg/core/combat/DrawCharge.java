package io.github.butterflysmp.rpg.core.combat;

import java.util.Optional;

/**
 * How long a bow has been held, in arrows -- and what each arrow gained should SOUND like.
 *
 * <p>The Dragon's Plume's charge, as arithmetic. Slice H1 owns the input layer in {@code paper}
 * because it reads a live player; <b>every DECISION in it lives here</b>, where a test can reach it
 * without a server -- the same split {@code Durability} / {@code WeaponDurability} argues for.
 *
 * <h2>THE CHARGE IS THREE STEPS OF TWO ARROWS -- R1 AS AMENDED, AND WHAT IT REPLACED</h2>
 *
 * <pre>
 * step 1   held 20t   1 arrow    tick 1
 * step 2   held 40t   3 arrows   tick 2
 * step 3   held 60t   5 arrows   tick 3      <- maximum, TWO seconds past full draw
 * </pre>
 *
 * <p><b>R1 ORIGINALLY READ <i>"every second adds one arrow, to five"</i>, WHICH IS FOUR SECONDS PAST
 * FULL DRAW AND FIVE SOUNDS.</b> It now reads <i>"every second adds TWO, to five"</i> -- three steps,
 * three sounds, two seconds. <b>Ruled after hearing the weapon rather than on paper</b>, which is
 * why the record carries the amendment and not a quiet rewrite: a silently replaced ruling reads as
 * one nobody ever questioned.
 *
 * <p><b>TWO RULINGS LOOK LIKE THEY SHOULD HAVE MOVED WITH IT AND DID NOT, SO THEY ARE NAMED:</b>
 *
 * <pre>
 * R6    FULL CHARGE IS ARROW 1     UNCHANGED. The first step still yields exactly one arrow;
 *                                  only the INCREMENT moved (+1 -> +2).
 * c=20  THE CHARGE SECOND          UNCHANGED (PLAN-dragons-plume.md 7.5). A step is still one
 *                                  second. What moved is the step's YIELD and the NUMBER of
 *                                  steps (5 -> 3). `c` was never the quantity in question.
 * </pre>
 *
 * <h2>THE FOUR NUMBERS, AND WHERE EACH ONE COMES FROM</h2>
 *
 * <pre>
 * FULL_DRAW_TICKS      20   MEASURED from the pinned jar: BowItem.MAX_DRAW_DURATION, and
 *                           getPowerForTime(20) = 1.0 exactly. A platform fact, not a choice.
 * CHARGE_SECOND_TICKS  20   RULED (R6, and PLAN-dragons-plume.md 7.5): `c`, the charge second.
 *                           It equals the draw duration by coincidence of both being one second;
 *                           they are different quantities and are kept apart.
 * MAX_STEPS             3   RULED, R1 as amended. The number of TICKS a full draw sounds.
 * ARROWS_PER_STEP       2   RULED, R1 as amended -- the increment, NOT the first step's yield.
 * </pre>
 *
 * <p><b>The two 20s are not the same 20.</b> One is how long vanilla takes to reach full draw; the
 * other is how long a held bow takes to earn its next step. A future ruling can move either without
 * the other, which is why they are two constants and not one.
 *
 * <p><b>{@link #MAX_ARROWS} IS DERIVED NOW, NOT AUTHORED.</b> Five is
 * {@code 1 + (MAX_STEPS - 1) * ARROWS_PER_STEP}, so moving either constant re-derives it instead of
 * leaving a five behind that nothing produces. It stays public because the quiver, the fan and the
 * damage tables all quote it.
 *
 * <h2>R6: FULL CHARGE IS STEP ONE, AND IT IS THE OFF-BY-ONE THIS FILE EXISTS TO FIX IN PUBLIC</h2>
 *
 * <pre>
 * held  19 ticks -> 0 steps, 0 arrows    not yet at full draw
 * held  20 ticks -> 1 step,  1 arrow     R6: reaching full charge GIVES one, it does not start
 *                                        a count -- and the increment does not apply to it
 * held  39 ticks -> 1 step,  1 arrow
 * held  40 ticks -> 2 steps, 3 arrows    and TWO per charge second after
 * held  60 ticks -> 3 steps, 5 arrows    the cap
 * held 999 ticks -> 3 steps, 5 arrows
 * </pre>
 */
public final class DrawCharge {

    private DrawCharge() {}

    /** MEASURED: {@code BowItem.MAX_DRAW_DURATION} on the pinned jar. */
    public static final int FULL_DRAW_TICKS = 20;

    /** RULED: {@code c}, the charge second. */
    public static final int CHARGE_SECOND_TICKS = 20;

    /** RULED (R1 as amended): three steps, so a full draw sounds three times. */
    public static final int MAX_STEPS = 3;

    /**
     * RULED (R1 as amended): each step after the first adds TWO arrows.
     *
     * <p><b>It is the INCREMENT, not the first step's yield.</b> R6 rules that reaching full draw
     * gives one arrow, and this does not apply to it -- which is the whole content of
     * {@link #arrowsForStep}'s {@code 1 + (step - 1) * ARROWS_PER_STEP} rather than
     * {@code step * ARROWS_PER_STEP}.
     */
    public static final int ARROWS_PER_STEP = 2;

    /**
     * Five arrows, and the quiver pays one round each -- DERIVED from the two constants above
     * rather than authored beside them.
     *
     * <p>It was an authored {@code 5} while the charge was one arrow per second. Under R1 as
     * amended it is {@code 1 + 2 * 2}, and leaving it authored would leave a number that nothing
     * produces: move {@link #MAX_STEPS} and an authored five becomes a claim the arithmetic
     * contradicts, silently, in every table that quotes it.
     */
    public static final int MAX_ARROWS = 1 + (MAX_STEPS - 1) * ARROWS_PER_STEP;

    /**
     * R13's pitch span. The mapping is GEOMETRIC because pitch is a playback RATE, so what an ear
     * hears as a step is the RATIO between two pitches and not the difference.
     *
     * <p>A mapping with even DIFFERENCES has shrinking steps, and it shrinks exactly where the
     * decision matters most -- at the top, when the player is deciding whether to let go:
     *
     * <pre>
     *                1        2        3      each step, as a RATIO
     * linear      1.00     1.50     2.00      1.500   1.333    <- the step shrinks
     * RULED       0.80   1.2649     2.00      1.5811  1.5811   <- even
     * </pre>
     *
     * <h2>THREE STEPS NOW, AND THE RULE IS RE-EVALUATED RATHER THAN THE OLD FIVE BEING SAMPLED</h2>
     *
     * <p>R1 as amended makes the charge <b>three</b> steps. The ladder is
     * {@code PITCH_FLOOR * r^(n-1)} recomputed with {@code r = (2.0/0.8)^(1/2) = 1.5811} -- the same
     * rule, <i>every step the same ratio, spanning the whole range</i>, evaluated at the new count.
     *
     * <p><b>AND IT LANDS EXACTLY ON THE OLD LADDER'S 1st, 3rd AND 5th RUNGS, WHICH IS ARITHMETIC
     * RATHER THAN LUCK:</b>
     *
     * <pre>
     * old five    0.80000  1.00595  1.26491  1.59054  2.00000     r = 1.257433
     * new three   0.80000           1.26491           2.00000     r = 1.581139 = 1.257433^2
     * </pre>
     *
     * <p>Every other term of a geometric sequence is geometric with ratio {@code r^2}, and the
     * amendment kept arrow counts <b>1, 3 and 5 -- the ODD POSITIONS</b>. So re-evaluating and
     * sampling agree here and cannot disagree.
     *
     * <p><b>The method is still RE-EVALUATION, and the agreement is a property of this particular
     * amendment.</b> A ruling of 1/2/3 arrows, or 1/4/5, would put steps where the old ladder has no
     * rung at all. Sampling happens to work once; the rule works every time.
     *
     * <p><b>The step is now about EIGHT semitones instead of four</b> -- {@code 12*log2(1.5811) =
     * 7.93} against {@code 12*log2(1.2574) = 3.97}. That figure is offered only as a familiar scale
     * for how far apart the steps are; <b>it is not an interval anyone hears</b>, for the reason
     * below.
     *
     * <h2>TWO INDEPENDENT RULINGS HAVE MOVED THIS LADDER, AND THEY MOVED DIFFERENT PARTS OF IT</h2>
     *
     * <p><b>The distinction is worth one paragraph, because "the ladder changed" is true of both and
     * says nothing useful:</b>
     *
     * <pre>
     * THE MATERIAL  pling -> basedrum -> hat     ruled on FEEL, twice, by ear
     *                                            THE RULE DID NOT MOVE. Not once, either time.
     * THE COUNT     5 steps -> 3 steps           ruled by amending R1 -- a decision about the
     *                                            CHARGE, which the sound merely reports
     * </pre>
     *
     * <p><b>Twice the material moved and this formula did not</b>, which is the strongest available
     * evidence that the ladder is about RATIOS rather than about the sound it is played on. <b>And
     * when the count moved, the formula was RE-EVALUATED rather than replaced</b> -- so neither
     * ruling has yet touched the rule itself, only its instrument and its argument.
     *
     * <p><b>BEN RULED THE TIMBRE, NOT THE LADDER.</b> {@code block.note_block.hat} stands across the
     * amendment: he heard that sound and it suits the weapon, and nothing about three steps rather
     * than five bears on which drum it is.
     *
     * <p><b>The RATIO half of the reasoning survives: equal ratios are still probably
     * equal perceptual steps, so the ladder stands unchanged.</b> The half that does not survive is
     * the one that talked about SEMITONES and called each step a major third -- <b>a note-block drum
     * is NOISE, not a tone.</b> Shifting its playback rate makes it shorter and brighter; there is no
     * interval to hear. That sentence was about an instrument the weapon no longer uses, and it is
     * removed rather than left to be re-derived.
     *
     * <p><b>The values in {@code PLAN-dragons-plume.md} are this formula rounded for display.</b>
     * The formula is the ruled thing -- <i>authored for SEPARATION</i> -- and it is what ships, so
     * the tests assert the PROPERTY (equal ratios, both endpoints) rather than pinning a rounding.
     * <b>That is also what made the step-count amendment cheap:</b> nothing had to be renumbered,
     * because nothing was numbered.
     *
     * <p><b>{@link #PITCH_CEILING} at 2.0 IS OUTSIDE KNOWLEDGE THIS MACHINE CANNOT MEASURE, AND THE
     * INSTRUMENT CHANGE WIDENED WHAT IS UNKNOWN ABOUT IT.</b> The pinned API documents no range for
     * {@code pitch} and {@code ClientboundSoundPacket} carries a raw float, so any cap is the
     * client's -- unchanged. <b>But the way the top of the range FAILS is not the same question it
     * was on a tone:</b> pitch is a PLAYBACK RATE, so any note-block percussion at 2.0 is
     * <b>half as long</b> and brighter, where a pling at 2.0 was simply a high note. So the ceiling
     * is no longer only a question about whether the client clamps; it is a question about whether
     * the TOP step is still the same sound.
     *
     * <p><b>And the amendment made that question arrive SOONER rather than going away.</b> The
     * ceiling used to be the fifth of five steps, reached after four seconds of holding; it is now
     * the THIRD of three, reached after two. <b>Same pitch, same unknown, encountered twice as
     * often</b> -- so if 2.0 does not survive, a player meets it on every full release rather than
     * on the patient ones.
     *
     * <p><b>That half is a property of note-block percussion and survived the swap from the kick to
     * the hat; what the swap moved is UNMEASURED.</b> A hat is already the shortest and driest of
     * the three, so it has less body to lose than a kick did -- but <b>whether a hat at 2.0 still
     * reads as a hat has not been listened to</b>, and no reading on this page or in
     * {@code GATE-plume-draw.md} answers it. <b>A person listening is the only instrument for either
     * half, and H-1b is the row that owes both.</b>
     */
    public static final double PITCH_FLOOR = 0.8;
    public static final double PITCH_CEILING = 2.0;

    /**
     * How many STEPS a draw held for {@code heldTicks} has EARNED, before the magazine is consulted.
     *
     * <p>This is the time half only. R3's cap is the other half and is applied by
     * {@link #affordableStep(int, int)}, because it must be re-read from the live magazine as the
     * count climbs rather than once at the end.
     *
     * <p><b>STEPS, NOT ARROWS, AND THE UNIT CHANGED WITH R1's AMENDMENT.</b> While a step was worth
     * one arrow the two were the same number and nothing chose between them. At two arrows a step
     * they are different quantities, and <b>the step is the one the tracker, the sound and the cap
     * all work in</b> -- see {@link #affordableStep}, where firing a number of arrows the ladder
     * never sounded is the thing being refused.
     */
    public static int stepsFor(int heldTicks) {
        if (heldTicks < FULL_DRAW_TICKS) return 0;
        int afterFullDraw = heldTicks - FULL_DRAW_TICKS;
        return Math.min(MAX_STEPS, 1 + afterFullDraw / CHARGE_SECOND_TICKS);
    }

    /**
     * What a given step is worth in arrows: {@code 1 + (step - 1) * ARROWS_PER_STEP}.
     *
     * <p>R6 is the {@code 1}: reaching full draw GIVES an arrow rather than starting a count, so the
     * increment applies only to the steps after it. Step 0 -- a draw that never reached full -- is
     * worth nothing, which is R10's floor seen from the other side.
     */
    public static int arrowsForStep(int step) {
        if (step <= 0) return 0;
        return Math.min(MAX_ARROWS, 1 + (Math.min(MAX_STEPS, step) - 1) * ARROWS_PER_STEP);
    }

    /**
     * How many arrows a draw held for {@code heldTicks} has earned. {@link #stepsFor} composed with
     * {@link #arrowsForStep}, kept as one call because the damage tables and the fan both ask it.
     */
    public static int arrowsFor(int heldTicks) {
        return arrowsForStep(stepsFor(heldTicks));
    }

    /**
     * R3'S CAP, AS AMENDED: THE HIGHEST STEP THE MAGAZINE CAN PAY FOR IN FULL.
     *
     * <p>{@code roundsRemaining} is {@code QuiverState.roundsRemaining()} -- slice G's accessor, read
     * LIVE from the held item each time the tracker would tick.
     *
     * <h2>THE CAP CAN NOW LAND BETWEEN STEPS, WHICH IT COULD NOT BEFORE</h2>
     *
     * <p>Under one arrow per step the cap always landed ON a step: any number of rounds was some
     * step's exact yield. <b>Under 1/3/5 it can land between</b> -- two rounds pays for step 1 and
     * not step 3, and four rounds pays for step 2 and not step 3.
     *
     * <p><b>RULED: THE TICKS STOP WHEN THE NEXT STEP IS UNAFFORDABLE, AND THE RELEASE FIRES EXACTLY
     * THE TRACKED STEP.</b> The remainder is STRANDED until a reload.
     *
     * <pre>
     * rounds 0  -> step 0, 0 arrows                rounds 4  -> step 2, 3 arrows, 1 stranded
     * rounds 1  -> step 1, 1 arrow                 rounds 5  -> step 3, 5 arrows, 0 stranded
     * rounds 2  -> step 1, 1 arrow,  1 stranded    rounds 9  -> step 3, 5 arrows (time permitting)
     * rounds 3  -> step 2, 3 arrows, 0 stranded
     * </pre>
     *
     * <h2>WHY NOT {@code min(stepYield, rounds)}, WHICH STRANDS NOTHING</h2>
     *
     * <p>Because <b>three channels now report the same quantity to two audiences.</b> The SOUND
     * tells the shooter which step they are on; the FAN WIDTH tells everyone watching; and the
     * arrows are the thing itself. A release that fires more than it sounded, and wider than it
     * fanned, puts all three in disagreement.
     *
     * <p><b>A disagreement between readouts is worse than a stranded round</b>, which the player
     * clears with a reload they were going to make anyway. Stranding is visible and recoverable;
     * a sound that lied is neither.
     *
     * <p><b>OVERTURNABLE, and cheap in either direction</b> -- operator's ruling, and it is one
     * comparison here rather than a shape anything else depends on.
     *
     * <p>Negative rounds floor to zero rather than being refused: a magazine cannot owe arrows, and
     * a caller that somehow obtains one should get a dead tracker, not an exception mid-draw.
     */
    public static int affordableStep(int stepsEarned, int roundsRemaining) {
        int rounds = Math.max(0, roundsRemaining);
        int earned = Math.max(0, Math.min(MAX_STEPS, stepsEarned));
        int step = earned;
        while (step > 0 && arrowsForStep(step) > rounds) step--;
        return step;
    }

    /**
     * What step {@code n} sounds like: {@code PITCH_FLOOR * r^(n-1)}, where {@code r} is the ratio
     * that lands the {@link #MAX_STEPS}th exactly on {@link #PITCH_CEILING}.
     *
     * <p>Computed rather than tabulated, so changing {@link #MAX_STEPS} re-spaces the ladder instead
     * of running off the end of an array -- <b>which is exactly what R1's amendment did</b>, and why
     * it cost no renumbering.
     *
     * @param step 1-based; clamped into {@code 1..MAX_STEPS} so a caller cannot produce a pitch
     *             outside the span by miscounting.
     */
    public static float pitchFor(int step) {
        int n = Math.max(1, Math.min(MAX_STEPS, step));
        if (MAX_STEPS <= 1) return (float) PITCH_FLOOR;
        double ratio = Math.pow(PITCH_CEILING / PITCH_FLOOR, 1.0 / (MAX_STEPS - 1));
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
     * normal case. It may never sit ABOVE it: no cap can manufacture a step, so a tracker holding
     * more than the elapsed time earned means <b>the tracker and the platform disagree about how long
     * the bow was held</b>, which has no other detector in this system.
     *
     * <p><b>IT COMPARES STEPS, NOT ARROWS, AND UNDER R1's AMENDMENT THAT IS NO LONGER THE SAME
     * CHECK.</b> While a step was one arrow the two units coincided and either would have done.
     * They no longer do, and <b>steps is the strictly finer of the two</b>: a tracker one step ahead
     * of the platform is three arrows against one, so an arrow comparison would have reported the
     * same fault as a larger number without detecting any fault an arrow comparison would not.
     * <b>More importantly the tracker's own unit is steps</b>, so comparing arrows would mean
     * converting first, and a converted quantity checked against an authority is a check on the
     * conversion as much as on the quantity.
     *
     * @param trackerSteps the step the tracker believes it reached -- NOT an arrow count
     * @return empty when the two agree (or differ only in the direction the cap explains)
     */
    public static Optional<String> disagreement(int trackerSteps, int heldTicks) {
        int earned = stepsFor(heldTicks);
        if (trackerSteps <= earned) return Optional.empty();
        return Optional.of("draw tracker holds step " + trackerSteps + " ("
                + arrowsForStep(trackerSteps) + " arrows) but the platform reports "
                + heldTicks + " ticks held, which earns step " + earned
                + " -- no cap can manufacture a step, so the tracker and the server disagree about"
                + " the length of this draw");
    }
}
