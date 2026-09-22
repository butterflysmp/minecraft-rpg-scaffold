package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** WHAT SEVEN FIRE ARROWS IN ONE FRAME ACTUALLY ACCRUE. EXECUTED, NOT REASONED. ***
 *
 * <h2>THE QUESTION, AND WHY IT HAD TO BE RUN</h2>
 *
 * <p>The Dragon's Breath is the first weapon to deliver SEVEN fire payloads in a single frame, and
 * a full magazine at point blank is <b>thirty-five</b>. Nothing in the tree has ever delivered fire
 * at that rate, and the plausible reading is that a press stacks scorch seven times faster than the
 * mechanism was tuned for.
 *
 * <p><b>IT DOES NOT, AND THE REASON IS A PROPERTY OF SCORCH RATHER THAN A PROPERTY OF THE WEAPON.</b>
 * The rows below drive the real {@code ScorchStatus} through the real clock and read the real burns
 * off the sink. They are here rather than in a plan document because the figures are the finding,
 * and a figure in a plan is a claim.
 *
 * <h2>THE MECHANISM, AS THE MEASUREMENT FOUND IT</h2>
 *
 * <pre>
 * stacks    Scorch.stacksFor(dealt) -- and the count HAS NO CONSUMER. scorch.yml says so:
 *           "read ONLY as a yes/no gate ... Stacks do NOT scale the damage and are NOT
 *           accumulated". Seven applications buy 7 x 4 = 28 stacks and 28 does nothing.
 * cap       ScorchStatus: "most recent applier owns the cap". Seven identical arrows
 *           overwrite it with the same number six times.
 * window    refreshed by each application. Seven refreshes in ONE tick is one refresh.
 * burn      min(5% of victim max, cap) every 20 ticks -- reads the cap and nothing else.
 * </pre>
 *
 * <p><b>So a seven-arrow press and a one-arrow hit produce an IDENTICAL burn</b>, and the seven-fold
 * delivery buys nothing at all in scorch. That is the opposite of the worry, and it is the kind of
 * answer that only falls out of running it.
 *
 * <h2>WHAT THE PRESS DOES BUY, WHICH IS THE PART TO WATCH</h2>
 *
 * <p>The burn does not scale, but <b>the DIRECT damage does</b>: seven arrows at 9 is 63 in one
 * frame against one arrow's 9. The scorch is a rounding error beside it. The weapon's fire identity
 * is therefore almost entirely its direct damage, and its burn is the same burn a single weak fire
 * hit would light.
 *
 * <p><b>AND THE CAP IS WHAT MAKES ITS BURN WEAK, not the stack count.</b> The cap is HALF the
 * authored per-arrow amount -- {@code Scorch.CAP_FRACTION} -- so a 9-damage arrow caps the burn at
 * 4.5, where the Flint Staff's 20-damage bolt caps at 10. <b>A weapon that fires seven small
 * payloads has a WEAKER burn than one that fires a single large one</b>, which is worth knowing
 * before anybody tunes the arrow damage down.
 *
 * <h2>ALL FIGURES ARE AT GEAR SCORE 100</h2>
 *
 * <p>The authored 9 is a literal, so {@code GearScore.scaledDamage} multiplies it by
 * {@code score/100} over a legal band of 100..500. <b>At the cap the per-arrow figure is 45 and the
 * scorch cap is 22.5</b>, which is where the percent arm stops binding on ordinary mobs. A figure
 * here with no score anchor would be one point on a five-fold range.
 */
class SevenArrowScorchTest {

    /** A vanilla-ish mob: 20 max, so the 5% arm gives 1.0 and every cap below is idle. */
    private static final double SMALL_MOB_MAX = 20.0;

    /** A knell: 360 max, so the 5% arm gives 18 and every shipped cap BINDS. */
    private static final double KNELL_MAX = 360.0;

    /** The Dragon's Breath, per arrow, at gear score 100. */
    private static final double BREATH_ARROW = 9.0;

    /** The Flint Staff's bolt -- the fire weapon whose burn anybody has actually watched. */
    private static final double FLINT_BOLT = 20.0;

    /** The Emberblade's swing: attack_damage 7, a weapon_damage payload. */
    private static final double EMBERBLADE_SWING = 7.0;

    private static double capOf(double authoredAmount) {
        return authoredAmount * Scorch.CAP_FRACTION;
    }

    /**
     * Drive one press of {@code applications} payloads of {@code amount} into a fresh victim and
     * return everything the burn did over its whole life.
     *
     * <p>All applications land on the SAME TICK, which is what a spread is: the clock is not
     * advanced between them.
     */
    private static FakeScorchSink burnOf(int applications, double amount, double victimMax) {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, victimMax);
        UUID victim = UUID.randomUUID();
        UUID applier = UUID.randomUUID();

        for (int i = 0; i < applications; i++) {
            scorch.apply(victim, clock, sink, Scorch.stacksFor(amount), capOf(amount), applier,
                    Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
        }

        // Past the whole window, so every burn the press bought has landed.
        clock.advance(Scorch.DEFAULT_DURATION_TICKS + Scorch.PERIOD_TICKS);
        return sink;
    }

    /**
     * *** THE HEADLINE: SEVEN ARROWS AND ONE ARROW BURN IDENTICALLY. ***
     *
     * <p>The row Ben's question asked for. Both figures are read off the sink rather than computed,
     * and they are compared to each other rather than to a constant -- so this cannot pass by both
     * being wrong in the same way.
     */
    @Test
    void sevenArrowsInOneFrameBurnExactlyAsMuchAsOne() {
        FakeScorchSink seven = burnOf(7, BREATH_ARROW, KNELL_MAX);
        FakeScorchSink one = burnOf(1, BREATH_ARROW, KNELL_MAX);

        assertEquals(one.count(), seven.count(),
                "seven applications in one frame must not buy more burn TICKS than one");
        assertEquals(one.totalDealt(), seven.totalDealt(), 1e-9,
                "nor more burn DAMAGE. The stack count has no consumer and the cap is overwritten"
                        + " with the same number six times.");
    }

    /**
     * THE ABSOLUTE FIGURES, PINNED -- what one press of the Dragon's Breath actually does.
     *
     * <p>Against a knell (360 max), the cap binds: {@code min(18, 4.5) = 4.5} per tick, six ticks
     * over the 120-tick window.
     */
    @Test
    void onePressAgainstAKnellBurnsSixTimesForFourAndAHalf() {
        FakeScorchSink press = burnOf(7, BREATH_ARROW, KNELL_MAX);

        assertEquals(6, press.count(), "six burn ticks over the 120-tick window");
        assertEquals(27.0, press.totalDealt(), 1e-9,
                "6 x 4.5 = 27 total scorch, against 63 direct damage in the same press");
    }

    /**
     * *** THE COMPARISON BEN ASKED FOR: AGAINST THE TWO FIRE WEAPONS ANYBODY HAS WATCHED. ***
     *
     * <p><b>The Dragon's Breath's burn is the WEAKEST of the three</b>, and by a factor of more than
     * two against the Flint Staff. Its seven-fold delivery does not enter the comparison at all --
     * the cap is half of ONE arrow's amount, and a small payload caps low however many of them land.
     *
     * <pre>
     *   weapon           per-payload   cap    per tick on a knell   total over the window
     *   Dragon's Breath      9         4.5           4.5                    27
     *   Emberblade           7         3.5           3.5                    21
     *   Flint Staff         20        10.0          10.0                    60
     * </pre>
     */
    @Test
    void theSevenArrowPressBurnsLessThanAFlintStaffBolt() {
        double breath = burnOf(7, BREATH_ARROW, KNELL_MAX).totalDealt();
        double flint = burnOf(1, FLINT_BOLT, KNELL_MAX).totalDealt();
        double ember = burnOf(1, EMBERBLADE_SWING, KNELL_MAX).totalDealt();

        assertEquals(27.0, breath, 1e-9);
        assertEquals(60.0, flint, 1e-9);
        assertEquals(21.0, ember, 1e-9);

        assertTrue(breath < flint,
                "a seven-arrow fire press burns LESS than one Flint Staff bolt: the cap is half of"
                        + " ONE payload, and seven small payloads cap lower than one large one");
    }

    /**
     * ON AN ORDINARY MOB THE CAP IS IDLE AND ALL THREE WEAPONS BURN IDENTICALLY.
     *
     * <p>At 20 max health the 5% arm gives 1.0 and every cap above exceeds it, so the burn is the
     * same for a 9-damage arrow and a 20-damage bolt. <b>The comparison above is a big-target
     * fact</b>, and reporting it without this row would make the Dragon's Breath look weak
     * everywhere when it is weak only where the cap binds.
     */
    @Test
    void againstAnOrdinaryMobEveryFireWeaponBurnsTheSame() {
        double breath = burnOf(7, BREATH_ARROW, SMALL_MOB_MAX).totalDealt();
        double flint = burnOf(1, FLINT_BOLT, SMALL_MOB_MAX).totalDealt();

        assertEquals(6.0, breath, 1e-9, "6 ticks x min(1.0, 4.5) = 6");
        assertEquals(breath, flint, 1e-9, "the cap is idle at 20 max: both burn the 5% arm");
    }

    /**
     * A FULL MAGAZINE IS FIVE PRESSES AND STILL ONE BURN AT A TIME.
     *
     * <p>Ben's thirty-five-accrual figure is real as a count of accrual EVENTS and buys nothing:
     * the window is 120 ticks and the weapon's cooldown is 32, so presses land inside one another's
     * windows and each merely refreshes it. <b>The burn never overlaps itself</b> -- there is one
     * {@code Active} per victim -- so a magazine emptied into one target produces a continuous burn
     * at the same 4.5 per tick, not five concurrent ones.
     */
    @Test
    void aWholeMagazineRefreshesOneBurnRatherThanStackingFive() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, KNELL_MAX);
        UUID victim = UUID.randomUUID();
        UUID applier = UUID.randomUUID();

        // Five presses, 32 ticks apart, seven arrows each: the whole magazine at point blank.
        for (int press = 0; press < 5; press++) {
            for (int arrow = 0; arrow < 7; arrow++) {
                scorch.apply(victim, clock, sink, Scorch.stacksFor(BREATH_ARROW), capOf(BREATH_ARROW),
                        applier, Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
            }
            clock.advance(32);
        }
        clock.advance(Scorch.DEFAULT_DURATION_TICKS + Scorch.PERIOD_TICKS);

        for (FakeScorchSink.Burn burn : sink.burns) {
            assertEquals(4.5, burn.amount(), 1e-9,
                    "every tick of a 35-accrual magazine burns the SAME 4.5 -- the stack count has"
                            + " no consumer, so thirty-five events are one burn refreshed");
        }
    }

    /**
     * THE CAP IS HALF THE AUTHORED AMOUNT, AND THAT IS WHERE THE WEAKNESS COMES FROM.
     *
     * <p>Pinned separately because it is the lever anybody tuning this weapon will reach for, and
     * because it is the one figure in this file that is a property of {@code Scorch} rather than of
     * the weapon. Raising per-arrow damage raises the burn linearly; adding arrows does not raise it
     * at all.
     */
    @Test
    void theCapIsHalfOfONEPayloadHoweverManyOfThemLand() {
        assertEquals(4.5, capOf(BREATH_ARROW), 1e-9);
        assertEquals(0.5, Scorch.CAP_FRACTION, 1e-9,
                "if this moves, every figure in this file moves with it");
    }
}
