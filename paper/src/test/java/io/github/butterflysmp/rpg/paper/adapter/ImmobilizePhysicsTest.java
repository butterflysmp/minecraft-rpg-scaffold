package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pure suppression decisions the anchor and the teleport-cancel gate on. Bukkit does the
 * teleport and event-cancel (boot-witnessed); the decisions here carry the mutations. The
 * movement-class matrix maps onto these: horizontal drift = walk/strafe, vertical rise = hop.
 */
class ImmobilizePhysicsTest {

    private static final double DRIFT_SQ = 0.01; // ANCHOR_DRIFT 0.1 blocks, squared
    private static final double MIN_TP_SQ = 4.0; // MIN_TELEPORT 2 blocks, squared

    @Test
    void withinToleranceNeedsNoCorrection() {
        assertNull(ImmobilizePhysics.correction(1.05, 64.0, 1.0, 1.0, 64.0, 1.0, DRIFT_SQ),
                "a sub-threshold drift is left alone -- this is the anti-vibrate half of the knob");
    }

    @Test
    void horizontalDriftSnapsBackToTheAnchorXZ() {
        double[] fix = ImmobilizePhysics.correction(2.0, 64.0, 3.0, 1.0, 64.0, 1.0, DRIFT_SQ);
        assertNotNull(fix, "drift past tolerance must correct -- the walk/strafe lock");
        assertEquals(1.0, fix[0], 1e-9);
        assertEquals(1.0, fix[2], 1e-9);
        // Mutation: drop the `drifted` check -> returns null when drifted -> reddens (mob creeps).
    }

    @Test
    void risingIsCappedAtTheAnchorYButFallingIsAllowed() {
        double[] risen = ImmobilizePhysics.correction(1.0, 66.0, 1.0, 1.0, 64.0, 1.0, DRIFT_SQ);
        assertNotNull(risen, "a hop/jump above the anchor must correct");
        assertEquals(64.0, risen[1], 1e-9, "Y capped at the anchor -- the slime-hop suppression");

        double[] falling = ImmobilizePhysics.correction(2.0, 60.0, 1.0, 1.0, 64.0, 1.0, DRIFT_SQ);
        assertEquals(60.0, falling[1], 1e-9, "falling below the anchor is kept -- gravity still works");
        // Mutation: drop the `rose`/Math.min cap -> Y not capped, a hopping slime rises -> reddens.
    }

    @Test
    void teleportSuppressedOnlyWhenImmobilizedAndReallyTeleporting() {
        assertTrue(ImmobilizePhysics.suppressTeleport(true, 100.0, MIN_TP_SQ), "frozen enderman teleport: cancel");
        assertFalse(ImmobilizePhysics.suppressTeleport(true, 0.02, MIN_TP_SQ),
                "our own sub-block anchor correction must pass, not self-cancel");
        assertFalse(ImmobilizePhysics.suppressTeleport(false, 100.0, MIN_TP_SQ), "a free mob teleports normally");
        // Mutation: drop the immobilized check -> cancels every mob's teleport; drop the distance
        //           gate -> cancels our own anchor corrections (mob never repositions) -> each reddens.
    }
}
