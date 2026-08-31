package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The regeneration arithmetic: the flat rate, the saturation multiplier, the three zero cases, and
 * the headroom cap.
 *
 * <p>Pure math with no clock and no Bukkit, so every value here is pinned exactly rather than
 * boot-witnessed. The boot gate witnesses the thing this cannot: that saturation is read from the
 * right bar, and that vanilla drains it.
 *
 * <p><b>Every expected value below was EXECUTED and pasted, never derived.</b> Two are not what the
 * arithmetic reads like it should give -- a one-tick window yields {@code 0.010000000000000002} and
 * the 99.9/100 headroom cap yields {@code 0.09999999999999432} -- which is exactly why the grid is
 * asserted whole and against a stated epsilon. The error lands in one cell.
 *
 * <p>There is no exhaustion test because there is no exhaustion. Gate row 4 measured that vanilla
 * drains saturation whether or not its own regen tick was allowed to heal, so the charge this class
 * was designed to make would have been a second one. See {@link HealthRegen}'s class javadoc.
 *
 * <p>Each test names the mutation it forces red.
 */
class HealthRegenTest {

    private static final double EPS = 1e-9;

    private static final int ONE_SECOND = 20;

    // --- The rate and the multiplier -------------------------------------------------------------

    @Test
    void theFlatRateIsTheFLOORAndTheSaturatedRateIsExactlyFIVETimesIt() {
        assertEquals(0.2, HealthRegen.healAmount(0.2, false, ONE_SECOND, 50, 100), EPS,
                "base 0.2 HP/s over one second is 0.2 HP -- the floor, and it is never zero");
        assertEquals(1.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, 50, 100), EPS,
                "fed, the same second is five times it -- a round 1.0 HP/s, which is the number tuned for");
        assertEquals(1.0, HealthRegen.healAmount(1.0, false, ONE_SECOND, 50, 100), EPS,
                "a gear-boosted rate rides the same arithmetic");
        assertEquals(5.0, HealthRegen.healAmount(1.0, true, ONE_SECOND, 50, 100), EPS,
                "ONE KNOB: gear that boosts the rate boosts the saturated rate with it");
        // The two tiers ARE the food economy of this system. Nothing here charges for the saturated
        // one -- vanilla drains saturation on its own (gate row 4), so being fed is already
        // self-limiting and a custom charge would have doubled the drain.
        // Mutation: SATURATED_MULTIPLIER -> 1.0 -> the fed rows collapse onto the floor,
        // expected: <1.0> but was: <0.2> -> reddens.
    }

    @Test
    void theWindowScalesWithThePERIODSoTheRateIsHPPerSECONDNotPerFIRE() {
        // The whole reason a 20-tick period was chosen is that at one fire per second these two
        // numbers coincide -- so this is the test that stops a later cadence change from silently
        // multiplying everyone's regeneration.
        assertEquals(0.05, HealthRegen.healAmount(0.2, false, 5, 50, 100), EPS,
                "a quarter-second window pays a quarter of the per-second rate");
        assertEquals(0.25, HealthRegen.healAmount(0.2, true, 5, 50, 100), EPS,
                "and a quarter of the fed rate when saturated");
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
        // negative-period row heals -1.0 -> reddens.
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
        // Mutation: drop the current >= max guard -> the over-full row returns -20.0 -> reddens.
    }

    @Test
    void aCombatantAtZeroIsDeadOrDYINGAndMustNotRegenerate() {
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, 0, 100), EPS,
                "zero custom HP is the death-screen state, not a very injured one");
        assertEquals(0.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, -5, 100), EPS,
                "and below zero, defensively");
        // HealthState.damage floors current at 0 and the kill is dispatched from the change;
        // onQuit does not run on death, so custom HP SITS at 0 through the whole death screen.
        // Mutation: drop the current <= 0 guard -> a corpse regenerates mid-death-screen,
        // expected: <0.0> but was: <1.0> -> reddens.
    }

    @Test
    void aZeroOrNEGATIVERateHealsNothingRatherThanDamagingThroughHeal() {
        assertEquals(0.0, HealthRegen.healAmount(0.0, true, ONE_SECOND, 50, 100), EPS,
                "a mob, whose stat bases at 0");
        assertEquals(0.0, HealthRegen.healAmount(-1.0, true, ONE_SECOND, 50, 100), EPS,
                "a future debuff must not heal a negative amount through a method named heal");
        // Mutation: drop the ratePerSecond <= 0 guard -> the negative row returns -5.0 -> reddens.
    }

    // --- The headroom cap ------------------------------------------------------------------------

    @Test
    void theAmountIsCappedAtTheHEADROOMSoREQUESTEDEqualsAPPLIED() {
        assertEquals(0.1, HealthRegen.healAmount(0.2, true, ONE_SECOND, 99.9, 100), EPS,
                "1.0 would overshoot; only the remaining 0.1 is asked for "
                        + "(executed as 0.09999999999999432, hence EPS)");
        assertEquals(1.0, HealthRegen.healAmount(0.2, true, ONE_SECOND, 98.0, 100), EPS,
                "and with room to spare the cap does not bite");
        // 98.0, not 99.0. At the x5 multiplier a fed second pays exactly 1.0, so 99.0/100 leaves
        // headroom of exactly 1.0 and Math.min(1.0, 1.0) is 1.0 either way -- the row would have
        // asserted the right number for the wrong reason and could not have reddened.
        //
        // The cap is not a duplicate of HealthState.heal's own Math.min. CombatantStats.heal reports
        // the REQUESTED amount in its event, unlike damage which reports what it dealt, so any
        // future consumer of a HEAL change reads this number -- and without the cap it would report
        // a heal larger than the one that landed.
        // Mutation: drop the Math.min against max - current -> the 99.9 row requests 1.0 where 0.1
        // lands -> reddens.
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
