package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F2, THE STRIKE RAMP (Ben, sixth message: "start at 20, 40, 80 and keeps doubling"). */
class StrikeRampTest {

    private static final FightSettings S = FightTestSettings.placeholders();

    @Test
    void theKthStrikeDeals20TimesTwoToTheKMinusOne() {
        assertEquals(20.0, StrikeRamp.damageForStrike(S, 1), 0.0);
        assertEquals(40.0, StrikeRamp.damageForStrike(S, 2), 0.0);
        assertEquals(80.0, StrikeRamp.damageForStrike(S, 3), 0.0);
        assertEquals(2560.0, StrikeRamp.damageForStrike(S, 8), 0.0);
    }

    @Test
    void theDamageSaturatesAtTheNamedCeilingForAnyK() {
        double huge = StrikeRamp.damageForStrike(S, Integer.MAX_VALUE);
        assertEquals(StrikeRamp.CEILING_DAMAGE, huge, 0.0, "it saturates at the ceiling");
        assertTrue(StrikeRamp.CEILING_DAMAGE < Double.MAX_VALUE);
        // Below the ceiling it is exact doubling: 20 x 2^999 is far under it.
        assertEquals(20.0 * Math.pow(2.0, 999), StrikeRamp.damageForStrike(S, 1000), 0.0);
        assertThrows(IllegalArgumentException.class, () -> StrikeRamp.damageForStrike(S, 0));
    }

    @Test
    void theCeilingKeepsTheDefenceCurveFiniteAtAnyDefence() {
        // EXECUTED, not reasoned (the defence curve multiplies by 100 before it divides; PLAN section 6.2).
        for (double defence : new double[] {0.0, 0.5, 10.0, 40.0, 1e6, 1e300, Double.MAX_VALUE}) {
            double landed = io.github.butterflysmp.rpg.core.combat.Defense.applyDefense(StrikeRamp.CEILING_DAMAGE, defence);
            assertTrue(Double.isFinite(landed), "defence " + defence + " gives " + landed);
        }
        // CONTROL: the uncapped figure the ceiling replaces does overflow on the same curve.
        assertTrue(Double.isInfinite(io.github.butterflysmp.rpg.core.combat.Defense.applyDefense(Double.MAX_VALUE, 40.0)));
    }

    @Test
    void consecutiveStrikesDoubleAndBeingInsideResetsTheCount() {
        StrikeRamp<String> ramp = new StrikeRamp<>(S);
        assertEquals(20.0, ramp.strike("ana", false), 0.0);
        assertEquals(40.0, ramp.strike("ana", false), 0.0);
        assertEquals(80.0, ramp.strike("ana", false), 0.0);
        assertEquals(3, ramp.consecutiveStrikes("ana"));
        assertEquals(0.0, ramp.strike("ana", true), 0.0, "inside at a strike tick: no damage");
        assertEquals(0, ramp.consecutiveStrikes("ana"), "and the count resets to 0");
        assertEquals(20.0, ramp.strike("ana", false), 0.0, "so the next one starts again at 20");
    }

    @Test
    void eachPlayerHasHisOwnCount() {
        StrikeRamp<String> ramp = new StrikeRamp<>(S);
        ramp.strike("ana", false);
        ramp.strike("ana", false);
        assertEquals(20.0, ramp.strike("bo", false), 0.0, "bo's first strike is 20 whatever ana's count is");
        assertEquals(80.0, ramp.strike("ana", false), 0.0);
    }

    @Test
    void aPlayerLeavingTheFightIsDropped() {
        StrikeRamp<String> ramp = new StrikeRamp<>(S);
        ramp.strike("ana", false);
        ramp.strike("ana", false);
        ramp.drop("ana");
        assertEquals(0, ramp.consecutiveStrikes("ana"));
        assertEquals(0, ramp.trackedPlayers(), "nothing kept for a player who left");
        assertEquals(20.0, ramp.strike("ana", false), 0.0, "rejoining starts again at 20");
    }

    @Test
    void twoFightsDoNotShareCounts() {
        StrikeRamp<String> first = new StrikeRamp<>(S);
        StrikeRamp<String> second = new StrikeRamp<>(S);
        first.strike("ana", false);
        first.strike("ana", false);
        assertEquals(20.0, second.strike("ana", false), 0.0, "the counts belong to the fight instance");
    }

    @Test
    void aStrikeTickComesEvery40Ticks() {
        StrikeRamp<String> ramp = new StrikeRamp<>(S);
        assertTrue(ramp.isStrikeTick(0L));
        assertFalse(ramp.isStrikeTick(39L));
        assertTrue(ramp.isStrikeTick(40L));
        assertFalse(ramp.isStrikeTick(41L));
        assertTrue(ramp.isStrikeTick(800L));
    }

    @Test
    void outsideTheBorderCountsAsOutside() {
        // While no Mage is channelling the ring IS the 48 border about C; a player at 60 blocks is outside it.
        Ring border = new Ring(0.0, 0.0, S.borderRadiusBlocks());
        StrikeRamp<String> ramp = new StrikeRamp<>(S);
        assertEquals(20.0, ramp.strike("ana", border.contains(60.0, 0.0)), 0.0);
        assertEquals(40.0, ramp.strike("ana", border.contains(0.0, -60.0)), 0.0);
        assertEquals(0.0, ramp.strike("ana", border.contains(47.0, 0.0)), 0.0, "and inside it is safe");
    }
}
