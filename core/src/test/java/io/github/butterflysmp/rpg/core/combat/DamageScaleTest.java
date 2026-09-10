package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.combat.stat.HeartScale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vanilla-to-custom conversion, and the denominator decision underneath it.
 *
 * <p>Expected values were produced by EXECUTING the expression, never by algebra —
 * {@code DefenseTest}'s convention. Each test names the mutation it forces red.
 */
class DamageScaleTest {

    private static final double EPS = 1e-9;

    // Vanilla's measured numbers, from the 2026-09-06 capture.
    private static final double LAVA = 4.0;
    private static final double DROWN = 2.0;
    private static final double LETHAL_FALL = 25.0;

    private static final double PLAYER_MAX = 100.0;
    private static final double BIG_PLAYER_MAX = 400.0;
    private static final double SPIDER_MAX = 16.0;
    private static final double KNELL_MAX = 360.0;
    private static final double MOB_BODY = 20.0;   // a wither skeleton's real bar, the Knell's body

    private static double puppeted(double amount, double customMax) {
        return DamageScale.toCustom(amount, customMax, Double.NaN, true);
    }

    private static double mob(double amount, double customMax, double vanillaMax) {
        return DamageScale.toCustom(amount, customMax, vanillaMax, false);
    }

    // --- The three populations --------------------------------------------------------------------

    @Test
    void aPlayerConvertsAtFiveAndALethalFallBecomesLethal() {
        assertEquals(20.0, puppeted(LAVA, PLAYER_MAX), EPS, "lava 4 on a 100-max player is 20 custom");
        assertEquals(125.0, puppeted(LETHAL_FALL, PLAYER_MAX), EPS,
                "a fall vanilla kills with must kill: 25 unconverted left the player at 87/100, which is "
                        + "the measured defect");
        assertTrue(puppeted(LETHAL_FALL, PLAYER_MAX) > PLAYER_MAX, "and 125 > 100 is what makes it lethal");
        // Mutation M2: hardcode k = 1 -> 25.0 -> the shipped defect returns -> reddens.
    }

    @Test
    void anUNTAGGEDMobConvertsAtONEBecauseItsCustomMaxWasSeededFromItsOwnBar() {
        assertEquals(DROWN, mob(DROWN, SPIDER_MAX, SPIDER_MAX), EPS,
                "spider 16/16: measured on the wire at 2 of 16 per drowning tick, exactly vanilla");
        assertEquals(8.0, mob(8.0, SPIDER_MAX, SPIDER_MAX), EPS, "and its 8-damage fall stays 8 of 16");
        // Mutation M1: hardcode k = 5 -> every untagged mob takes five times vanilla -> reddens.
    }

    @Test
    void theKNELLConvertsAtEIGHTEENAndItIsTheBiggestChangeInTheCommit() {
        // Shipped content: knell.yml declares max_health 360 on a ~20-point body. Unconverted, a lava
        // tick took 1.1% of its bar where vanilla intends 20%.
        assertEquals(72.0, mob(LAVA, KNELL_MAX, MOB_BODY), EPS, "lava 4 x k=18");
        assertEquals(0.2, mob(LAVA, KNELL_MAX, MOB_BODY) / KNELL_MAX, EPS,
                "which is 20% of its bar -- the same fraction vanilla takes from any mob");
        assertEquals(LAVA / MOB_BODY, mob(LAVA, KNELL_MAX, MOB_BODY) / KNELL_MAX, EPS,
                "and identical to the fraction an untagged mob on the same body loses");
        // Mutation M1 or M2: any hardcoded k -> the Knell moves by 18x the wrong way -> reddens.
    }

    // --- THE DENOMINATOR DECISION -----------------------------------------------------------------

    @Test
    void thePuppetedDenominatorIsTWENTYAndNOTTheRenderedHeartCount() {
        // THE MUTATION THAT WOULD SHIP SILENTLY. At max 100 the two denominators are both 20, so every
        // test written at the base max passes either way. The case has to be at 400.
        assertEquals(20, HeartScale.heartCount(PLAYER_MAX) * 2, "at max 100 the two agree -- 20 and 20");
        assertEquals(26, HeartScale.heartCount(BIG_PLAYER_MAX) * 2, "at max 400 they do NOT: 20 vs 26");

        assertEquals(80.0, puppeted(LAVA, BIG_PLAYER_MAX), EPS,
                "4 x 400/20. Keying this to heartCount*2 would give 4 x 400/26 = 61.5, letting a DISPLAY "
                        + "function -- which compresses a heart to 100 points above max 100 so the bar "
                        + "fits on screen -- decide every environmental damage number in the game");
        assertNotEquals(LAVA * BIG_PLAYER_MAX / (HeartScale.heartCount(BIG_PLAYER_MAX) * 2.0),
                puppeted(LAVA, BIG_PLAYER_MAX),
                "the rendered-bar denominator must NOT be what this returns");
        // Mutation M7: denominator = heartCount(customMax)*2 -> reddens ONLY here, and only because
        // this case is at max 400. Every base-max row stays green under it.
    }

    @Test
    void theOperatorsDrowningRuleIsExactlyTrueAtEveryPUPPETEDMax() {
        // "10% of max health REGARDLESS of how much defense or health they have" -- the only statement
        // made about non-base maximums, and the reason the denominator is 20. A display-scaled
        // denominator gives 10% at max 100 and 7.7% at max 400; this gives 10% at both.
        //
        // NARROWED FROM ...AtEVERYMax ON 2026-09-10. THE OLD NAME WAS A CLAIM THAT DID NOT SURVIVE
        // CHECKING: this loop calls puppeted() only, and "every max" is true on the puppeted path and
        // FALSE on the untagged-mob path, where the denominator MOVES WITH the max. A reader who
        // trusted the name would conclude the rule holds for any combatant at any pool. The companion
        // row below is the case the name used to swallow.
        for (double max : new double[] {100.0, 150.0, 400.0, 1000.0}) {
            assertEquals(0.10, puppeted(DROWN, max) / max, EPS,
                    "drowning must take exactly a tenth of the bar at max " + max);
        }
        // Mutation M7 again, from the property side: heartCount*2 -> 7.7% at 400 -> reddens.
    }

    @Test
    void onAnUntaggedMobDrowningIsFLATBecauseTheDenominatorMovesWithTheMax() {
        // THE CASE THE OTHER ROW'S NAME USED TO SWALLOW, and the one that makes "every max" false.
        //
        // An untagged mob's denominator is its OWN max-health attribute, and its customMax was SEEDED
        // from that same attribute -- so k = 1 by construction and the vanilla number passes through
        // unscaled. The bonus cancels: the conversion divides by the number it multiplies by.
        //
        // THIS MATTERS BECAUSE A VANILLA MOB'S MAX IS NOT A CONSTANT. A leader zombie carries a
        // permanent MULTIPLY_TOTAL modifier and can sit anywhere in 40-100, so this row is written
        // across that range rather than at 20.
        for (double max : new double[] {20.0, 51.0, 79.0, 96.0}) {
            assertEquals(DROWN, mob(DROWN, max, max), EPS,
                    "an untagged mob at max " + max + " takes the raw vanilla tick, unscaled");
        }
        // So a 96 HP zombie takes 2 -- about 2% of its pool, not 10%.
        assertEquals(0.02, mob(DROWN, 96.0, 96.0) / 96.0, 0.001,
                "which is ~2% of the pool, NOT the tenth the rule names");

        // WHETHER THAT IS CORRECT IS UNRULED, AND THIS ROW DELIBERATELY DOES NOT DECIDE IT. It pins
        // what the code does TODAY so that changing it has to be a decision rather than a drift --
        // see NEXT.md, where both readings are recorded: either k = 1 IS the invariant (an untagged
        // mob is unchanged from vanilla, which is what seeding from the attribute exists to
        // guarantee), or the rule means "regardless of max health" and does not reach the one mob
        // where it would show. If that is ruled the other way, this row is the thing that reddens.
    }

    // --- Fail-soft, on BOTH operands --------------------------------------------------------------

    @Test
    void aMissingMaxHealthAttributeFailsSoftToONEAndNeverToCurrentHealth() {
        assertEquals(LAVA, mob(LAVA, SPIDER_MAX, Double.NaN), EPS, "absent attribute -> unscaled");
        assertEquals(LAVA, mob(LAVA, SPIDER_MAX, 0.0), EPS, "and a zero one -> unscaled");

        // THE TEMPORAL TRAP, NAMED. MobNameplateManager.maxHealthOf falls back to mob.getHealth(),
        // which is correct for SEEDING at EntityAddToWorldEvent -- where health IS max -- and unsafe
        // here, because k is computed at DAMAGE time where health is arbitrary. A mob one hit from
        // death would give k = customMax / ~0.
        double nearDeathHealth = 0.01;
        assertTrue(mob(LAVA, KNELL_MAX, nearDeathHealth) > 100_000,
                "which would deal this, if anyone reused that fallback: " + mob(LAVA, KNELL_MAX, nearDeathHealth));
        // Mutation M4: fall back to current health instead of k = 1 -> a near-dead mob's next tick
        // deals 144000 -> the row above reddens with the absent-attribute case.
    }

    @Test
    void aNonPositiveCustomMaxFailsSoftToONEBecauseZeroWouldBeUNKILLABLE() {
        // GUARDING ONLY THE DENOMINATOR LEAVES THE WORSE HOLE OPEN. k = 0 converts every hit to
        // nothing and the combatant never drains -- exactly what MobSeeding's javadoc calls worse than
        // reverting to vanilla numbers, and that is the same sentence this class cites for the
        // denominator's own fallback.
        assertEquals(LAVA, puppeted(LAVA, 0.0), EPS, "a zero-max combatant takes vanilla damage, not zero");
        assertEquals(LAVA, puppeted(LAVA, -5.0), EPS, "and a negative one likewise");
        assertEquals(LAVA, mob(LAVA, Double.NaN, SPIDER_MAX), EPS, "NaN too");
        // Mutation: drop the customMax guard -> every row here returns 0.0 -> a zero-max combatant is
        // INVULNERABLE, which no other test in the repo would notice.
    }

    @Test
    void aNonPositiveAmountConvertsToZeroAndNotToANegative() {
        assertEquals(0.0, puppeted(0.0, PLAYER_MAX), EPS, "nothing in, nothing out");
        assertEquals(0.0, puppeted(-4.0, PLAYER_MAX), EPS,
                "and a negative amount must never become a heal on the way through");
        // Mutation: drop the amount guard -> -4 becomes -20 custom, which CombatantStats.damage
        // ignores (amount <= 0 returns false) but which would surface the moment anything sums it.
    }

    // --- The shape of the conversion --------------------------------------------------------------

    @Test
    void theConversionIsAScalarSoItCommutesWithTheShieldFraction() {
        // Shield DR and Thorns are PERCENTAGES. Converting before or after them is the same number,
        // which is why the call site's ordering is documentation rather than correctness -- and why
        // DamageWindow can stay upstream in vanilla units.
        double halfBlocked = 0.5;
        assertEquals(puppeted(LAVA, PLAYER_MAX) * halfBlocked,
                puppeted(LAVA * halfBlocked, PLAYER_MAX), EPS,
                "k * (x * f) == (k * x) * f");
        // Mutation M3: swap numerator and denominator -> still commutes, so this row stays GREEN.
        // It is the population rows above that catch an inverted k. Said out loud because a
        // commuting property proves shape, never direction.
    }

    @Test
    void theFactorIsNotAppliedTwice() {
        // k^2 at max 100 is 25x, which is also what an open "fall -> 5x" tuning item applied ON TOP of
        // this fix would produce. Both are the same arithmetic mistake from different directions.
        assertEquals(100.0, puppeted(LAVA * 5, PLAYER_MAX), EPS,
                "an already-converted 20 must not convert again to 500");
        assertEquals(20.0, puppeted(LAVA, PLAYER_MAX), EPS, "one application only");
        // Mutation M5: convert twice at the call site -> the rider's own numbers become k^2 -> the
        // in-game rows redden; this row pins the arithmetic that makes it recognisable.
    }
}
