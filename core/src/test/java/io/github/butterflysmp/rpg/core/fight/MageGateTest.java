package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** F6 + F7's pure pieces: the damage gate, the crystal spots, the standable column, the random landing. */
class MageGateTest {

    private static final UUID MAGE = UUID.fromString("00000000-0000-0000-0000-0000000000ee");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    @Test
    void theGateRefusesOnlyItsGuardedMobAndOnlyWhileNotDamageable() {
        DamageGate gate = new DamageGate();
        AtomicBoolean damageable = new AtomicBoolean(false);
        List<String> refused = new ArrayList<>();
        gate.guard(MAGE, new DamageGate.Rule() {
            @Override public boolean damageable() { return damageable.get(); }
            @Override public void refused(double amount, String source) { refused.add(amount + " " + source); }
        });
        assertTrue(gate.refuses(MAGE, 12.5, "ability"));
        assertEquals(List.of("12.5 ability"), refused);
        assertFalse(gate.refuses(OTHER, 12.5, "ability"), "an unguarded mob is never refused");
        damageable.set(true);
        assertFalse(gate.refuses(MAGE, 7.0, "melee"), "damageable: allowed");
        assertEquals(1, refused.size(), "and nothing reported");
        damageable.set(false);
        gate.release(MAGE);
        assertFalse(gate.refuses(MAGE, 1.0, "melee"), "released: no longer guarded");
    }

    @Test
    void allowNextLetsExactlyOneHitThrough() {
        DamageGate gate = new DamageGate();
        gate.guard(MAGE, new DamageGate.Rule() {
            @Override public boolean damageable() { return false; }
            @Override public void refused(double amount, String source) {}
        });
        gate.allowNext(MAGE);
        assertFalse(gate.refuses(MAGE, 1e9, "fixture"), "the fixture's own kill passes once");
        assertTrue(gate.refuses(MAGE, 1.0, "melee"), "and only once");
    }

    @Test
    void theCrystalSpotsAreEvenlyRoundTheCircleInOrderFromAngleZero() {
        List<double[]> spots = CrystalRing.offsets(10, 40.0, 12.0);
        assertEquals(10, spots.size());
        for (int i = 0; i < 10; i++) {
            double a = 2 * Math.PI * i / 10;
            assertEquals(40.0 * Math.cos(a), spots.get(i)[0], 1e-9, "spot " + (i + 1) + " x");
            assertEquals(12.0, spots.get(i)[1], 0.0, "spot " + (i + 1) + " height");
            assertEquals(40.0 * Math.sin(a), spots.get(i)[2], 1e-9, "spot " + (i + 1) + " z");
        }
    }

    @Test
    void theStandableBlockIsTheHighestSolidOneWithClearanceWithinTheBand() {
        // A column: solid at 60 and 64, air elsewhere. From C's y 63, band 8: 55 to 71. Clearance 3.
        java.util.function.IntPredicate solid = y -> y == 60 || y == 64;
        assertEquals(64, StandableColumn.highest(63, 8, 3, solid).getAsInt(), "64 is solid with 65-67 clear");
        // Put a solid ceiling from 66 up: 64 no longer has clearance, nothing above it does, so 60 (61-63 clear) is the
        // answer.
        java.util.function.IntPredicate ceiling = y -> y == 60 || y == 64 || y >= 66;
        assertEquals(60, StandableColumn.highest(63, 8, 3, ceiling).getAsInt());
        assertTrue(StandableColumn.highest(63, 8, 3, y -> false).isEmpty(), "no floor within the band: none");
        assertTrue(StandableColumn.highest(63, 8, 3, y -> y == 54).isEmpty(), "a floor just below the band: none");
    }

    @Test
    void aRandomLandingIsUniformOverTheDiscAndNeverPastTheMaximum() {
        Random rng = new Random(42);
        int inner = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            double[] p = RandomLanding.pick(36.0, rng);
            double d = Math.hypot(p[0], p[1]);
            assertTrue(d <= 36.0, "within the maximum: " + d);
            if (d <= 18.0) inner++;
        }
        // Uniform over the AREA: a quarter of the points within half the radius (a uniform radius would give half).
        assertEquals(0.25, inner / (double) n, 0.02);
    }
}
