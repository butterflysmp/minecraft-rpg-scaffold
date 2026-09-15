package io.github.butterflysmp.rpg.core.combat;

/**
 * WHAT LETTING GO OF THE DRAW ACTUALLY DOES: nothing, a tap, or a charged release.
 *
 * <p>The Dragon's Plume's release, as arithmetic. {@code paper} reads a live player and dispatches;
 * <b>every DECISION is here</b>, where a test reaches it without a server -- the same split
 * {@link DrawCharge} argues for on the charge side.
 *
 * <h2>THREE OUTCOMES, AND THE THIRD IS THE ONLY ONE ANYONE PICTURES</h2>
 *
 * <pre>
 * Nothing   below R10's floor, or an empty magazine
 * Tap       R4': ONE arrow, NO homing, 12 damage, one round -- a DIFFERENT SHOT
 * Charged   the tracked step: 1, 3 or 5 arrows at 48, homing, one round each
 * </pre>
 *
 * <h2>R4' IS AN OVERTURN AND THE BOUNDARY IS SHARP, WHICH IS THE PART TO KNOW</h2>
 *
 * <p><b>At 19 ticks held a release is a {@link Tap}; at 20 it is a {@link Charged} of ONE ARROW.</b>
 * Both fire a single arrow. They are not the same shot and neither is a scaled version of the other:
 *
 * <pre>
 * held 19   Tap          1 arrow   12 damage   NO homing
 * held 20   Charged(1)   1 arrow   48 damage   homing        R6: full charge IS arrow one
 * </pre>
 *
 * <p><b>R4 originally ruled that a partial draw fired one arrow AND STILL HOMED.</b> The
 * homing-versus-no-homing question was put to Ben when R4 was made and he chose homing; R4' is him
 * choosing the option he previously declined, with a damage cut added. <b>It is an OVERTURN, not an
 * amendment</b>, and the superseded version is written down because a silently rewritten ruling
 * reads as one nobody ever questioned.
 *
 * <p>The tap is priced to be a <b>last resort rather than a rate choice</b> -- at the amended
 * charge it sits about 66% behind on damage per second (`PLAN-dragons-plume.md` §7.2), and that is
 * the design working. <b>R4' exists so the weapon is never dead at close range</b>, where a
 * three-second hold is not available.
 */
public sealed interface DrawRelease {

    /**
     * R10'S FLOOR -- <b>AND IT IS OURS NOW, NOT THE PLATFORM'S. THAT CHANGED AND NOTHING ANNOUNCED
     * IT.</b>
     *
     * <h2>WHAT R10 RULED, AND THE ASSUMPTION UNDERNEATH IT THAT STOPPED HOLDING</h2>
     *
     * <p>R10 ruled <i>quick taps keep vanilla's floor</i> -- under three ticks, nothing -- and the
     * plan recorded that as a SIMPLIFICATION: the floor was <i>"taken from the platform rather than
     * reimplemented"</i>, so hanging the shot on {@code EntityShootBowEvent} would inherit it with
     * no second implementation of the same rule.
     *
     * <p><b>H1's release makes that impossible, and H1 shipped after R10 was written.</b>
     * {@code PlumeDraw.onRelease} calls {@code clearActiveItem()}, so
     * {@code LivingEntity.releaseUsingItem}'s re-read yields EMPTY and <b>{@code BowItem.releaseUsing}
     * never runs.</b> The power gate lives inside that method. <b>The gate never runs, so the floor
     * never applies, so there is nothing to inherit.</b>
     *
     * <p><b>H-3 IS THE EVIDENCE RATHER THAN THE BYTECODE READING.</b> That row read <i>arrow YES,
     * log YES</i> -- the off-hand arrow survived and the loud {@code EntityShootBowEvent} guard
     * stayed silent, which is a booted measurement that vanilla's release path did not execute.
     *
     * <h2>SO IT IS A DERIVED CONSTANT, AND IT CAN DRIFT FROM VANILLA SILENTLY</h2>
     *
     * <p>MEASURED from the pinned jar (`PLAN-dragons-plume.md` §3.1): {@code BowItem.releaseUsing}
     * returns with no shot when {@code getPowerForTime(heldTicks) < 0.1}, offsets 54-65, over the
     * curve {@code ((t/20)^2 + 2*(t/20)) / 3}:
     *
     * <pre>
     * t = 2   power 0.0700   NO SHOT, and no EntityShootBowEvent either
     * t = 3   power 0.1075   shoots
     * </pre>
     *
     * <p><b>INHERITED BEHAVIOUR CANNOT DRIFT. A COPIED NUMBER CAN</b>, and that is the whole
     * character change: while the floor came from {@code BowItem}, a Paper update moving it moved
     * ours too, for free and invisibly. Now a Paper update moving it leaves this 3 behind, and
     * <b>nothing in this repository would fail.</b>
     *
     * <p><b>{@link #vanillaPowerFor} EXISTS SO THE CONSTANT IS CHECKABLE AGAINST ITS SOURCE RATHER
     * THAN MERELY ANNOTATED WITH IT.</b> {@code DrawReleaseTest} asserts that 3 is the smallest
     * {@code t} the curve puts at or above {@code 0.1} -- so the constant and the formula cannot
     * quietly disagree.
     *
     * <p><b>THAT COVERS HALF THE DRIFT AND THE OTHER HALF IS UNCOVERED, WHICH IS SAID HERE RATHER
     * THAN LEFT TO BE ASSUMED.</b> The test pins our constant against <b>our transcription</b> of
     * vanilla's curve. If a future Paper changes the curve or the {@code 0.1} threshold, the
     * transcription is stale too and <b>the test stays green while both are wrong together.</b>
     * There is no automatic detector for that; the upgrade procedure's smoke test is the only thing
     * that would notice, and only if somebody taps.
     *
     * <h2>AND A SECOND CONSEQUENCE OF THE SAME EVENT, WHICH IS NOT ABOUT THIS CONSTANT'S VALUE AT
     * ALL -- IT IS ABOUT WHAT THE FLOOR IS HOLDING UP</h2>
     *
     * <p><b>THE DRAGON'S PLUME'S BALANCE RESTS ON THIS 3 BY A MARGIN OF SIX HUNDREDTHS OF A TICK,
     * AND NOTHING CHOSE THAT.</b> Computed at the ruled values (charged 34, tap 8,
     * {@code reload_ticks} 60): the tap interval at which tapping would draw level with charging
     * <b>on a zero reload</b> is
     *
     * <pre>
     * T = 2.9412     against this constant's 3     -- a margin of 0.0588 ticks
     * </pre>
     *
     * <p>So the charged release stays ahead partly because <b>vanilla's power curve happens to
     * refuse a two-tick tap.</b> {@code PLAN-dragons-plume.md} §7.3 records the coupling as dead --
     * no reload value lets the tap back in front -- and <b>that conclusion is true only while the
     * floor sits above 2.9412.</b>
     *
     * <p><b>IT IS A COINCIDENCE, NOT A DESIGN MARGIN, AND THE DIFFERENCE MATTERS.</b> Nobody picked
     * 3; it is read off {@code BowItem.getPowerForTime} and was inherited before it was ours. Nobody
     * picked 2.9412 either -- it falls out of three ruled numbers that were chosen for other
     * reasons. <b>Two independent quantities landed 0.06 apart by accident</b>, and a margin nobody
     * chose is a margin nobody is watching.
     *
     * <p><b>ITS CONDITION, stated as an event rather than a date:</b> if a future Paper moves the
     * power curve such that this floor drops <b>below 2.9412</b> -- which is a change of THEIRS,
     * not of ours -- <b>the tap retakes the lead at a zero reload and §7.3's "dead coupling" is
     * alive again.</b> That is the same upgrade this javadoc already warns about one paragraph up,
     * and it has two consequences rather than one: the constant and its transcription go stale
     * together, AND a balance conclusion three files away quietly stops holding.
     *
     * <p>Named here rather than only in the plan, because <b>this is the file an upgrade makes
     * somebody open</b>, and the plan is not.
     */
    int MIN_RELEASE_TICKS = 3;

    /** The power threshold {@code BowItem.releaseUsing} refuses below. MEASURED, §3.1. */
    double MIN_RELEASE_POWER = 0.1;

    /**
     * Vanilla's {@code BowItem.getPowerForTime}, transcribed from the pinned jar so
     * {@link #MIN_RELEASE_TICKS} can be checked against it instead of being taken on trust.
     *
     * <p><b>It is a TRANSCRIPTION and it is not called in production</b> -- the release uses the
     * integer constant, because recomputing a fixed threshold every shot would be arithmetic
     * pretending to be a lookup. Its only caller is the test that pins the constant.
     *
     * <p>Named a transcription rather than an implementation deliberately: nothing here reads the
     * platform, so this is a copy of a formula and ages exactly like one.
     */
    static double vanillaPowerFor(int heldTicks) {
        double t = heldTicks / (double) DrawCharge.FULL_DRAW_TICKS;
        return (t * t + 2 * t) / 3;
    }

    /** A release that fires nothing at all, and WHY -- the two reasons want different handling. */
    record Nothing(Reason reason) implements DrawRelease {}

    /**
     * Why a release fired nothing.
     *
     * <p><b>They are kept apart because one is silent BY DESIGN and the other owes the player a
     * sentence.</b> A sub-floor release is a twitch vanilla would not have shot either; an empty
     * magazine is a weapon that has stopped working, and a refusal with no remedy reads as a bug.
     * A single {@code Nothing} would have made that distinction unavailable to the caller.
     */
    enum Reason {
        /** Under {@link #MIN_RELEASE_TICKS}. Silent: nothing happened, and nothing should be said. */
        BELOW_VANILLA_FLOOR,
        /** The magazine cannot pay for even one arrow. The weapon is dead until reloaded -- ruled. */
        NO_ROUNDS
    }

    /** R4': one arrow, no homing, 12 damage, one round. A DIFFERENT shot, not a scaled one. */
    record Tap() implements DrawRelease {}

    /**
     * The tracked step, fired. {@code arrows} is {@code DrawCharge.arrowsForStep(step)} and is
     * carried rather than recomputed so a caller cannot derive it a second way.
     *
     * <p><b>{@code step} is kept even though {@code arrows} determines it</b>, because the step is
     * what the SOUND announced and what the fan width showed -- see R3a. A caller reporting a
     * mismatch needs the number the player heard, not just the number they received.
     */
    record Charged(int step, int arrows) implements DrawRelease {}

    /**
     * THE WHOLE RELEASE DECISION, FROM THE TWO THINGS A RELEASE KNOWS.
     *
     * <h2>THE ORDER OF THE TWO REFUSALS IS A DECISION, NOT AN ACCIDENT</h2>
     *
     * <p><b>The floor is checked FIRST, so a sub-floor twitch on an empty magazine is silent rather
     * than a "you are out of ammunition" notice.</b> Below the floor nothing happened at all --
     * vanilla would not have fired either, and the player has not asked for a shot. Telling them
     * about their magazine would answer a question they did not ask, on an input they may not know
     * they made.
     *
     * <p>The other order is defensible and is what a caller would get by gating on the quiver
     * first, which {@code WeaponFire} does. That is why this is stated here rather than left to
     * whichever check happens to run earlier in {@code paper}.
     *
     * @param heldTicks       what the platform says, {@code PlayerStopUsingItemEvent.getTicksHeldFor()}
     * @param roundsRemaining the LIVE magazine, {@code QuiverState.roundsRemaining()}
     */
    static DrawRelease decide(int heldTicks, int roundsRemaining) {
        if (heldTicks < MIN_RELEASE_TICKS) return new Nothing(Reason.BELOW_VANILLA_FLOOR);
        if (roundsRemaining < 1) return new Nothing(Reason.NO_ROUNDS);

        int step = DrawCharge.affordableStep(DrawCharge.stepsFor(heldTicks), roundsRemaining);
        if (step < 1) return new Tap();
        return new Charged(step, DrawCharge.arrowsForStep(step));
    }
}
