package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scorch's arithmetic, pinned exactly.
 *
 * <b>THE HEADLINE IS {@link #theCapBINDSOnlyAtAHighMaxWhichIsTheONLYPlaceItIsVisible}.</b> At the
 * default max of 100 a 20-damage weapon's cap NEVER binds, so a working cap and a deleted cap return
 * the same number for every test written at that max. A cap tested only there is untested. This suite
 * therefore carries its own blindness demonstration -- a row that shows the naive test passing against
 * a deliberately capless expression -- so the reason for the odd-looking 5000 is on the page rather
 * than in someone's memory.
 *
 * Each test names the mutation it forces red. The values were EXECUTED, not reasoned.
 *
 * Note what is NOT here: "stacks do not scale damage" cannot be guarded in this class, because
 * {@link Scorch#damagePerTick} takes no stack argument -- the property is structural. The mutation
 * that matters (multiplying by the live stack count in the tick body) is reachable only in
 * {@code ScorchStatus}, and {@code ScorchStatusTest.stacksDoNOTScaleTheDamage} is where it reddens.
 */
class ScorchTest {

    private static final double EPS = 1e-9;

    // --- The rate ----------------------------------------------------------------------------

    @Test
    void theBurnIsFivePercentOfTheVICTIMSMaxPerSecond() {
        // Percent of MAX, not a flat number and not percent of current -- so it scales up with the
        // pool, which is what makes it the anti-tank tool and what makes the cap necessary.
        assertEquals(5.0, Scorch.damagePerTick(100, 20), EPS, "5% of a 100 pool");
        assertEquals(18.0, Scorch.damagePerTick(360, 20), EPS, "5% of the Knell's 360");
        // Mutation: change RATE_PER_SECOND off 0.05 -> both reddens.
    }

    @Test
    void theRateMatchesWhatVANILLAFireAlreadyDeliversWhichIsWhySliceOneIsNotABalanceChange() {
        // GATE-vanilla-damage.md D15 measured 391 FIRE_TICK events: a converted vanilla fire tick is
        // 1 * (customMax/20) = exactly 5% of custom max per second, on both a 20 HP skeleton and a
        // 100 max player. So for a STANDARD entity this slice changes attribution, pacing and the cap
        // -- not magnitude. If this row ever reddens, that claim in the PR body is no longer true and
        // the blast radius of the slice has changed.
        for (double vanillaMax20CustomMax : new double[] {20, 100, 360, 1000}) {
            double convertedVanillaFireTick = 1.0 * (vanillaMax20CustomMax / 20.0);
            assertEquals(convertedVanillaFireTick,
                    Scorch.RATE_PER_SECOND * vanillaMax20CustomMax, EPS,
                    "scorch's rate must equal the converted vanilla fire tick at custom max "
                            + vanillaMax20CustomMax + " -- D15 measured them equal");
        }
        // Mutation: change RATE_PER_SECOND -> reddens, naming the max.
    }

    // --- The cap -----------------------------------------------------------------------------

    @Test
    void theCapBINDSOnlyAtAHighMaxWhichIsTheONLYPlaceItIsVisible() {
        // THE ROW THE WHOLE CLASS EXISTS FOR.
        //
        // Percent-max-health runs away on a big pool: 5% of 5000 is 250 per second, from a staff that
        // hits for 20. The cap is the brake, and it is also how a stronger fire weapon buys more
        // scorch damage -- the job stacks would otherwise have had.
        assertEquals(20.0, Scorch.damagePerTick(5000, 20), EPS,
                "5% of 5000 is 250; the cap must hold it to the weapon's 20");
        assertEquals(2.0, Scorch.damagePerTick(5000, Scorch.UNDECLARED_CAP), EPS,
                "and an UNDECLARED cap must bind too -- a non-binding fallback is not a fallback");
        // Mutation: replace the Math.min with `RATE_PER_SECOND * victimMaxHealth` -> returns 250 and
        // 250 -> reddens on both. Verified.
    }

    @Test
    void aTestWrittenAtTheDEFAULTMaxCannotSeeTheCapAtAll() {
        // THE BLINDNESS, DEMONSTRATED RATHER THAN ASSERTED -- this is why the row above uses 5000.
        //
        // capless() is deliberately WRONG: it has no cap. At max 100 with a 20-damage weapon it agrees
        // with the real function to the last bit, so every assertion written at the default max passes
        // against an implementation with the cap deleted. That is M7's shape: a defect invisible at the
        // value everyone naturally tests at.
        for (double cap : new double[] {20, 16, 12, 8, 6}) {
            assertEquals(capless(100, cap), Scorch.damagePerTick(100, cap), EPS,
                    "at max 100 a capless implementation is INDISTINGUISHABLE at cap " + cap
                            + " -- which is why the cap is pinned at a max where it binds");
        }
        // And the same comparison at a binding max separates them, which is the other half of the
        // demonstration: the test is capable of telling them apart, just not at 100.
        assertTrue(capless(5000, 20) > Scorch.damagePerTick(5000, 20),
                "at max 5000 the capless implementation is 250 against the capped 20 -- the row above "
                        + "is the one that can see the difference");
        // Mutation: none needed -- this row IS the mutation, held permanently beside the real one.
    }

    /** A deliberately capless scorch, kept here to demonstrate what a test at max 100 cannot see. */
    private static double capless(double victimMaxHealth, double ignoredCap) {
        return Scorch.RATE_PER_SECOND * victimMaxHealth;
    }

    @Test
    void theUndeclaredCapNeverExceedsTheSmallestDeclaredOne() {
        // The invariant that FIXES UNDECLARED_CAP's value instead of picking one by taste: forgetting
        // to declare a cap must never be STRONGER than declaring one. 2.0 is solar_grenade's field
        // tick, the smallest authored fire damage in shipped content.
        //
        // This row pins the CONSTANT against the rule. The rule against CONTENT is enforced by
        // ScorchContentInvariantTest, which walks the bundled files -- because this assertion cannot
        // see a new ability authored at 1.5 tomorrow, and that is exactly the case the rule is for.
        assertEquals(2.0, Scorch.UNDECLARED_CAP, EPS,
                "the smallest declared fire damage in content is solar_grenade's field tick at 2.0");
        assertTrue(Scorch.UNDECLARED_CAP > 0,
                "a non-positive undeclared cap would zero the DoT rather than restrain it");
        // Mutation: raise UNDECLARED_CAP above 2.0 -> reddens here AND in the content walk.
    }

    // --- Stack accrual -----------------------------------------------------------------------

    @Test
    void oneStackPerTwoDamageDEALT() {
        assertEquals(10, Scorch.stacksFor(20), "the Flint Staff's 20 damage is ten stacks");
        assertEquals(1, Scorch.stacksFor(2), "exactly two damage is exactly one stack");
        assertEquals(4, Scorch.stacksFor(8), "ember_step's 8");
        // Mutation: change DAMAGE_PER_STACK -> every row reddens.
    }

    @Test
    void accrualFLOORSSoChipDamageScorchesNothing() {
        // Rounded rather than floored, 1 damage buys a stack and every glancing hit scorches. Floored
        // is also what makes "1 per 2" literally true rather than "1 per 2, ish".
        assertEquals(0, Scorch.stacksFor(1), "one damage buys nothing");
        assertEquals(0, Scorch.stacksFor(1.999), "and nor does just under two");
        assertEquals(1, Scorch.stacksFor(3.999), "three-and-a-bit is one stack, not two");
        // Mutation: Math.round instead of the integer cast -> 1 and 1.999 become 1, 3.999 becomes 2
        // -> reddens on all three.
    }

    @Test
    void aNonPositiveAmountAccruesNothing() {
        // Reachable: DamageWindow returns 0.0 for a fully absorbed hit, and that must not mint a stack
        // from damage that did not land.
        assertEquals(0, Scorch.stacksFor(0), "an absorbed hit accrues nothing");
        assertEquals(0, Scorch.stacksFor(-5), "and a negative amount is not a scorch");
        // Mutation: drop the guard -> -5/2 casts to -2 -> a NEGATIVE stack count -> reddens.
    }

    // --- The tick count ----------------------------------------------------------------------

    @Test
    void eightDamageTicksForTheEightSecondDefault() {
        // First tick INCLUDED, on application. 160 ticks at period 20 is EIGHT, not nine -- and the
        // difference is a full second of burn. See ScorchStatusTest, where the LIFETIME is asserted
        // separately: the tick count alone cannot see an off-by-one-period overrun.
        assertEquals(8, Scorch.damageTicksFor(Scorch.DEFAULT_DURATION_TICKS), "160 ticks is 8 ticks");
        assertEquals(4, Scorch.damageTicksFor(80), "flint_staff's 80 is 4");
        assertEquals(3, Scorch.damageTicksFor(60), "solar_lance, ember_step, rekindle, ability_stone");
        assertEquals(2, Scorch.damageTicksFor(40), "solar_grenade's burst and field");
        // Mutation: drop the +PERIOD-1 rounding -> 160 stays 8 but 50 becomes 2 -> reddens below.
    }

    @Test
    void aDurationThatIsNotAWholeNumberOfPeriodsRoundsUP() {
        // Stated rather than discovered. Reachable only by a future author -- every authored value in
        // content today is a multiple of 20 -- but a partial period must still burn, or a 19-tick
        // scorch would be a no-op that looked applied.
        assertEquals(3, Scorch.damageTicksFor(50), "50 ticks burns 3 times and lives 60");
        assertEquals(1, Scorch.damageTicksFor(1), "even one tick of scorch burns once");
        assertEquals(0, Scorch.damageTicksFor(0), "and a zero duration burns not at all");
        // Mutation: integer-divide without the rounding -> 50 becomes 2 and 1 becomes 0 -> reddens.
    }

    // --- The total, which is what gets tuned against -------------------------------------------

    @Test
    void theFULLWINDOWTotalIsFortyPercentOfMaxUncappedAndEightTimesTheCapWhenItBinds() {
        // THE RATE IS WHAT THE CODE DOES; THE TOTAL IS WHAT THE GAME DOES.
        //
        // Pinned because it is the number that will be tuned against, and nothing else in the codebase
        // states it. If the period, the rate or the default duration moves, this is the row that says
        // what actually changed for a player.
        int ticks = Scorch.damageTicksFor(Scorch.DEFAULT_DURATION_TICKS);

        assertEquals(40.0, ticks * Scorch.damagePerTick(100, 20), EPS,
                "8 ticks x 5% = 40% of a 100 pool over one full window, cap idle");
        assertEquals(0.40, ticks * Scorch.RATE_PER_SECOND, EPS,
                "which is 40% of max for ANY pool the cap does not bind on");
        assertEquals(160.0, ticks * Scorch.damagePerTick(5000, 20), EPS,
                "against a capped target it is 8 x cap -- 160 from a 20-damage staff, whatever the pool");
        assertEquals(2000.0, ticks * capless(5000, 20), EPS,
                "uncapped that same window would be 2000 -- the cap is a 12.5x cut on a 5000 pool");
        // Mutation: any change to RATE_PER_SECOND, PERIOD_TICKS or DEFAULT_DURATION_TICKS -> reddens,
        // which is the point: the total must not drift silently when a constant is tuned.
    }
}
