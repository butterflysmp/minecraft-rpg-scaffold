package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The regeneration arithmetic: the flat rate, the saturation multiplier, the three zero cases, the
 * headroom cap, and the exhaustion charge.
 *
 * <p>Pure math with no clock and no Bukkit, so every value here is pinned exactly rather than
 * boot-witnessed. The boot gate witnesses the things this cannot: that saturation is read from the
 * right bar, and whether the exhaustion charge is restorative rather than additive.
 *
 * <p><b>Every expected value below was EXECUTED and pasted, never derived.</b> Two of them are not
 * what the arithmetic reads like it should give -- a one-tick window yields
 * {@code 0.010000000000000002} and the 99.9/100 headroom cap yields {@code 0.09999999999999432} --
 * which is exactly why the grid is asserted whole and against a stated epsilon. The error lands in
 * one cell.
 *
 * <p>Each test names the mutation it forces red.
 */
class HealthRegenTest {

    private static final double EPS = 1e-9;

    /** A ratio the tests own, so they stay discriminating while EXHAUSTION_PER_HP ships at 0. */
    private static final double RATIO = 1.2;

    private static final int ONE_SECOND = 20;

    // --- The rate and the multiplier -------------------------------------------------------------

    @Test
    void theFlatRateIsTheFLOORAndTheSaturatedRateIsExactlyFOURTimesIt() {
        assertEquals(0.2, HealthRegen.healAmount(0.2, false, ONE_SECOND, 50, 100), EPS,
                "base 0.2 HP/s over one second is 0.2 HP -- the floor, and it is never zero");
        assertEquals(0.8, HealthRegen.healAmount(0.2, true, ONE_SECOND, 50, 100), EPS,
                "saturated, the same second is four times it");
        assertEquals(1.0, HealthRegen.healAmount(1.0, false, ONE_SECOND, 50, 100), EPS,
                "a gear-boosted rate rides the same arithmetic");
        assertEquals(4.0, HealthRegen.healAmount(1.0, true, ONE_SECOND, 50, 100), EPS,
                "ONE KNOB: gear that boosts the rate boosts the saturated rate with it");
        // Mutation: SATURATED_MULTIPLIER -> 1.0 -> the two saturated rows collapse onto the flat ones -> reddens.
    }

    @Test
    void theWindowScalesWithThePERIODSoTheRateIsHPPerSECONDNotPerFIRE() {
        // The whole reason a 20-tick period was chosen is that at one fire per second these two
        // numbers coincide -- so this is the test that stops a later cadence change from silently
        // multiplying everyone's regeneration by four.
        assertEquals(0.05, HealthRegen.healAmount(0.2, false, 5, 50, 100), EPS,
                "a quarter-second window pays a quarter of the per-second rate");
        assertEquals(0.2, HealthRegen.healAmount(0.2, true, 5, 50, 100), EPS,
                "saturated over a quarter second is the unsaturated per-second amount");
        assertEquals(0.01, HealthRegen.healAmount(0.2, false, 1, 50, 100), EPS,
                "one tick is 0.01 HP -- executed as 0.010000000000000002, hence EPS");
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, 0, 50, 100), EPS,
                "a zero-length window pays nothing rather than dividing into one");
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, -5, 50, 100), EPS,
                "and a NEGATIVE period must not run the arithmetic backwards into a negative heal -- "
                        + "this row is the only thing that makes the periodTicks guard load-bearing, "
                        + "since a zero period would yield 0.0 through the window arithmetic anyway");
        // Mutation: drop the periodTicks/TICKS_PER_SECOND divisor -> the 5-tick rows pay a full
        // second each -> reddens. Separately: drop the periodTicks <= 0 half of the guard -> the
        // negative-period row heals -0.2 -> reddens.
    }

    // --- The three zero cases, each a decision ---------------------------------------------------

    @Test
    void aFULLCombatantIsHealedNOTHINGSoNoRenderIsEmittedForNothing() {
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, 100, 100), EPS,
                "exactly full heals nothing");
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, 120, 100), EPS,
                "over full -- a ceiling that just fell -- also heals nothing, and never a negative");
        // This is not tidiness. CombatantStats.heal emits a HealthChange whenever the state exists,
        // EVEN when HealthState.heal clamped the whole amount away, and that change drives a
        // setHealth write and a heart-bar render. Without this branch every full-health player on
        // the server takes a render every second, forever, for nothing.
        // Mutation: drop the current >= max guard -> 0.8 is returned and requested-equals-applied
        // dies with it -> reddens.
    }

    @Test
    void aCombatantAtZeroIsDeadOrDYINGAndMustNotRegenerate() {
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, 0, 100), EPS,
                "zero custom HP is the death-screen state, not a very injured one");
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, -5, 100), EPS,
                "and below zero, defensively");
        // HealthState.damage floors current at 0 and the kill is dispatched from the change;
        // onQuit does not run on death, so custom HP SITS at 0 through the whole death screen.
        // Mutation: drop the current <= 0 guard -> a corpse regenerates mid-death-screen -> reddens.
    }

    @Test
    void aZeroOrNEGATIVERateHealsNothingRatherThanDamagingThroughHeal() {
        assertEquals(0.0, HealthRegen.healAmount(0.0, true, ONE_SECOND, 50, 100), EPS,
                "a mob, whose stat bases at 0");
        assertEquals(0.0, HealthRegen.healAmount(-1.0, true, ONE_SECOND, 50, 100), EPS,
                "a future debuff must not heal a negative amount through a method named heal");
        // Mutation: drop the ratePerSecond <= 0 guard -> the negative row returns -4.0 -> reddens.
    }

    // --- The headroom cap, which is what makes the exhaustion charge honest ----------------------

    @Test
    void theAmountIsCappedAtTheHEADROOMSoREQUESTEDEqualsAPPLIED() {
        assertEquals(0.1, HealthRegen.healAmount(0.2, true, ONE_SECOND, 99.9, 100), EPS,
                "0.8 would overshoot; only the remaining 0.1 is asked for "
                        + "(executed as 0.09999999999999432, hence EPS)");
        assertEquals(0.8, HealthRegen.healAmount(0.2, true, ONE_SECOND, 99.0, 100), EPS,
                "and with room to spare the cap does not bite");
        // Not a duplicate of HealthState.heal's own Math.min. CombatantStats.heal reports the
        // REQUESTED amount in its event, unlike damage which reports what it dealt -- so a caller
        // charging exhaustion for what it asked for would overcharge on the window that tops a
        // player off. Capping here is what makes requested == applied.
        // Mutation: drop the Math.min against max - current -> 0.8 is requested where 0.1 lands,
        // an 8x overcharge on the last window -> reddens.
    }

    // --- The exhaustion charge -------------------------------------------------------------------

    @Test
    void anUNSATURATEDWindowIsFREEAndASaturatedOneChargesTheWHOLEHeal() {
        assertEquals(0.0, HealthRegen.exhaustionFor(0.8, false, RATIO), EPS,
                "the floor rate has no food gate, so charging for it would starve an idle player "
                        + "in exchange for nothing");
        assertEquals(0.96, HealthRegen.exhaustionFor(0.8, true, RATIO), EPS,
                "saturated charges for the WHOLE heal -- the floor's share included, not merely the "
                        + "extra the multiplier added");
        assertEquals(0.0, HealthRegen.exhaustionFor(0.0, true, RATIO), EPS, "no heal, no charge");
        assertEquals(0.0, HealthRegen.exhaustionFor(0.8, true, 0.0), EPS,
                "and a zero ratio charges nothing -- the state this ships in until the gate runs");
        // Mutation: charge when unsaturated (drop the !saturated guard) -> the first row returns
        // 0.96 -> reddens. The ratio is a PARAMETER precisely so this row can exist while
        // EXHAUSTION_PER_HP is still 0; a version reading the constant returns 0 on both branches
        // and cannot fail.
    }

    @Test
    void exhaustionScalesWithHPHEALEDSoTheChargeIsCADENCEInvariant() {
        double oneFireOfASecond =
                HealthRegen.exhaustionFor(HealthRegen.healAmount(0.2, true, 20, 50, 100), true, RATIO);

        double fourQuarterSecondFires = 0;
        for (int i = 0; i < 4; i++) {
            fourQuarterSecondFires +=
                    HealthRegen.exhaustionFor(HealthRegen.healAmount(0.2, true, 5, 50, 100), true, RATIO);
        }

        double twentySingleTickFires = 0;
        for (int i = 0; i < 20; i++) {
            twentySingleTickFires +=
                    HealthRegen.exhaustionFor(HealthRegen.healAmount(0.2, true, 1, 50, 100), true, RATIO);
        }

        assertEquals(0.96, oneFireOfASecond, EPS, "one second, one fire");
        assertEquals(oneFireOfASecond, fourQuarterSecondFires, EPS,
                "the same second in four fires costs the same");
        assertEquals(oneFireOfASecond, twentySingleTickFires, EPS,
                "and in twenty (executed as 0.9600000000000004)");
        // This is why the charge is per HP HEALED and not per tick: changing REGEN_PERIOD_TICKS
        // changes how often it is charged and how much each charge covers, and those cancel. A
        // per-tick charge would silently retune the food economy the first time the period moved.
        // Mutation: make exhaustionFor ignore `healed` and return a flat per-call ratio -> the
        // four- and twenty-fire sums become 4x and 20x the one-fire figure -> reddens.
    }

    // --- The bonus surface, shaped like ManaBank --------------------------------------------------

    @Test
    void boostsIsSTRICTSoAZeroBonusDeclaresNothing() {
        assertFalse(HealthRegen.boosts(HealthRegen.NONE), "0.0 is absent, not a grant");
        assertFalse(HealthRegen.boosts(-0.1), "and a negative certainly is not");
        assertTrue(HealthRegen.boosts(0.0001), "anything above zero is");
        // Mutation: boosts uses >= -> a zero bonus becomes a modifier source and the scan writes a
        // no-op entry every reconcile -> reddens.
    }

    @Test
    void contributionIsTheBonusItselfButNamedSoAFutureCurveHasSomewhereToLive() {
        assertEquals(0.8, HealthRegen.contribution(0.8), EPS, "identity today");
        assertEquals(0.0, HealthRegen.contribution(0.0), EPS);
        // Mutation: contribution halves its input -> the fixture's +0.8 resolves to 0.6 HP/s rather
        // than 1.0 -> reddens.
    }
}
