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
    void theDamageStaysFiniteForAnyK() {
        double huge = StrikeRamp.damageForStrike(S, Integer.MAX_VALUE);
        assertTrue(Double.isFinite(huge), "k = Integer.MAX_VALUE must not be infinite");
        assertEquals(Double.MAX_VALUE, huge, 0.0, "it saturates at the largest finite double");
        // The last k that does not saturate is still exact doubling: 20 x 2^1019 is finite.
        assertEquals(20.0 * Math.pow(2.0, 1019), StrikeRamp.damageForStrike(S, 1020), 0.0);
        assertTrue(Double.isFinite(StrikeRamp.damageForStrike(S, 1020)));
        assertThrows(IllegalArgumentException.class, () -> StrikeRamp.damageForStrike(S, 0));
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
