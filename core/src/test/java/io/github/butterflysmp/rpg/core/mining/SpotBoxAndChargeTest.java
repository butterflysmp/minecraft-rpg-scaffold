package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two rules a click is judged by: is the aim inside the live spot's 5 x 5 box, and was the
 * swing fully charged.
 *
 * <p>The box for a spot centred on pixel (10, 10) covers pixels 8..12, which is the half-open
 * fractional range [8, 13). The LOW edge is in and the HIGH edge is out, so two adjacent boxes
 * never both claim a point (PLAN-mining.md 5.2).
 */
class SpotBoxAndChargeTest {

    private static final Spot SPOT = new Spot(10, 10);

    private static boolean in(double u, double v) {
        return SpotBox.contains(SPOT, new PatchPoint(u, v));
    }

    @Test
    void theLowEdgeIsInAndJustBelowItIsOut() {
        assertTrue(in(8.0, 10.5));
        assertFalse(in(7.999, 10.5));
        assertTrue(in(10.5, 8.0));
        assertFalse(in(10.5, 7.999));
        // Mutation: '>=' -> '>' on the low edge -> in(8.0, ..) reddens.
    }

    @Test
    void theHighEdgeIsOutAndJustBelowItIsIn() {
        assertTrue(in(12.999, 10.5));
        assertFalse(in(13.0, 10.5));
        assertTrue(in(10.5, 12.999));
        assertFalse(in(10.5, 13.0));
        // Mutation: '<' -> '<=' on the high edge -> in(13.0, ..) reddens.
    }

    @Test
    void theCornersFollowTheSameRule() {
        assertTrue(in(8.0, 8.0));
        assertTrue(in(12.999, 12.999));
        assertFalse(in(13.0, 13.0));
        assertFalse(in(7.999, 7.999));
    }

    @Test
    void theCentreIsIn() {
        assertTrue(in(10.5, 10.5));
    }

    @Test
    void theBoxIsFivePixelsAndTheMarkThree() {
        assertEquals(5, SpotBox.BOX_PX, "Ben: \"the hit box for it can be 5x5\"");
        assertEquals(3, SpotBox.MARK_PX, "Ben: \"The visible weak spot will be 3x3\"");
    }

    // --- The charged rule ------------------------------------------------------------------------

    @Test
    void onlyExactlyOnePointZeroIsCharged() {
        assertEquals(1.0f, ChargeRule.FULL_CHARGE);
        assertTrue(ChargeRule.isCharged(1.0f));
    }

    @Test
    void theReadOneTickBeforeFullIsNotCharged() {
        // At attack speed 1.2 the read at ticker 16 is (16 + 0.5) / 16.666666 = 0.99 (PLAN 5.7's
        // table). A tolerance such as ">= 0.99" would accept that uncharged swing.
        assertFalse(ChargeRule.isCharged(0.99f));
        assertFalse(ChargeRule.isCharged(0.9999f));
        assertFalse(ChargeRule.isCharged(Math.nextDown(1.0f)));
        assertFalse(ChargeRule.isCharged(0.0f));
        // Mutation: 'cooldown == FULL_CHARGE' -> 'cooldown >= 0.99f' -> reddens.
    }
}
