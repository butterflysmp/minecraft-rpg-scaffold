package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The void implosion as a pure shape (PLAN-dragon-mobs.md, Part D of the 2026-11-17 brief). Every expected number was
 * EXECUTED in jshell (2026-11-17, scratchpad {@code imp.jsh}): {@code (double) (0.6f * 0.91f)} = 0.546000063419342,
 * {@code (double) (1.0f * 0.91f)} = 0.9100000262260437, and the rows of strength, cap and travel at reach 6 and pull
 * 1.25 quoted in each test. Never worked by hand (memory: never predict floating point).
 *
 * <p>The Phage's use: reach 6, vanilla's explosion falloff, pull 1.25, explosion knockback resistance.
 */
class ImplosionTest {

    private static final Implosion PHAGE = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 1.25,
            Implosion.Resistance.EXPLOSION_KNOCKBACK);
    private static final Vec3 ORIGIN = new Vec3(10, 64, 20);
    private static final double GROUND = 0.546000063419342;
    private static final double AIR = 0.9100000262260437;

    /** A victim due EAST of the origin at {@code d} blocks, on stone (friction 0.6f) or in the air, no resistance. */
    private static Implosion.Footing east(double d, boolean onGround) {
        return new Implosion.Footing(new Vec3(10 + d, 64, 20), onGround, Implosion.DEFAULT_BLOCK_FRICTION, 0.0);
    }

    // --- the per-tick multiplier: READ, LivingEntity#travelInAir (see Implosion#tickMultiplier) ------------------------

    @Test
    void theTickMultiplierIsTheBlocksFrictionTimesPointNineOneOnTheGroundAndPointNineOneInTheAir() {
        assertEquals(GROUND, Implosion.tickMultiplier(true, 0.6f), 0.0, "(double) (0.6f * 0.91f), a float product");
        assertEquals(AIR, Implosion.tickMultiplier(false, 0.6f), 0.0, "airborne: the block is not read, 1.0f");
        assertEquals((double) (0.98f * 0.91f), Implosion.tickMultiplier(true, 0.98f), 0.0, "ice's own friction");
    }

    @Test
    void travelIsTheGeometricSumOfTheSpeedMovedFirstThenScaledEachTick() {
        // move(delta) then delta *= m: v + v m + v m^2 + ... = v / (1 - m)
        assertEquals(0.5, Implosion.travel(0.226999968290, GROUND), 1e-9);
        assertEquals(1.376652174684, Implosion.travel(0.625, GROUND), 1e-9);
        assertEquals(0.0, Implosion.travel(0.0, GROUND), 0.0);
    }

    // --- the strength: vanilla's push curve, x the use's multiplier, x (1 - resistance) --------------------------------

    @Test
    void theStrengthIsVanillasLinearShareTimesThePullTimesOneMinusResistance() {
        assertEquals(1.25, PHAGE.strength(0, 0), 1e-12);
        assertEquals(0.833333333333, PHAGE.strength(2, 0), 1e-9);
        assertEquals(0.625, PHAGE.strength(3, 0), 1e-12);
        assertEquals(0.3125, PHAGE.strength(3, 0.5), 1e-12, "resistance 0.5 halves it");
        assertEquals(0.0, PHAGE.strength(6, 0), 0.0, "nothing at the reach");
        assertEquals(0.0, PHAGE.strength(6.5, 0), 0.0, "nothing beyond it, never negative");
    }

    @Test
    void theDamageShareIsVanillasCurveForTheExplosionFalloffAndOneForFlat() {
        assertEquals(1.0, PHAGE.share(0), 0.0);
        assertEquals(0.375, PHAGE.share(3), 1e-12, "three eighths at half the reach");
        assertEquals(0.0, PHAGE.share(6), 0.0);
        assertEquals(0.0, PHAGE.share(7), 0.0);
        Implosion flat = new Implosion(6, Implosion.Falloff.FLAT, 1.0, Implosion.Resistance.NONE);
        assertEquals(1.0, flat.share(0), 0.0);
        assertEquals(1.0, flat.share(5.99), 0.0);
        assertEquals(0.0, flat.share(6.01), 0.0, "flat inside, nothing beyond");
    }

    // --- the reach: feet to feet, a sphere, inclusive -----------------------------------------------------------------

    @Test
    void theReachIsInclusiveAndASphere() {
        assertTrue(PHAGE.inReach(ORIGIN, new Vec3(16, 64, 20)), "exactly 6");
        assertFalse(PHAGE.inReach(ORIGIN, new Vec3(16.001, 64, 20)), "just beyond");
        assertFalse(PHAGE.inReach(ORIGIN, new Vec3(15, 64, 24)), "inside the cube, outside the sphere: hypot(5,4)");
        assertTrue(PHAGE.pull(ORIGIN, east(6.001, true)).isEmpty(), "beyond the reach: no pull at all");
        assertTrue(PHAGE.pull(ORIGIN, east(6.0, true)).isPresent(), "at the reach: listed, with nothing applied");
        assertEquals(0.0, PHAGE.pull(ORIGIN, east(6.0, true)).orElseThrow().applied(), 0.0);
    }

    // --- THE CAP: never carried past the origin -----------------------------------------------------------------------

    @Test
    void theCapIsTheSpeedWhoseModelledTravelIsTheHorizontalDistanceLessTheMargin() {
        assertEquals(0.680999904871, Implosion.capSpeed(2.0, GROUND), 1e-9);
        assertEquals(0.0, Implosion.capSpeed(0.5, GROUND), 0.0, "at the margin: nothing");
        assertEquals(0.0, Implosion.capSpeed(0.2, GROUND), 0.0, "inside the margin: nothing, never negative");
        assertEquals(0.134999960661, Implosion.capSpeed(2.0, AIR), 1e-9, "the air's drag is far weaker");
    }

    @Test
    void nearTheCentreTheCapBindsAndFartherOutTheStrengthIsAppliedWhole() {
        Implosion.Pull at2 = PHAGE.pull(ORIGIN, east(2, true)).orElseThrow();
        assertTrue(at2.capped());
        assertEquals(0.833333333333, at2.strength(), 1e-9, "the uncapped strength is still reported");
        assertEquals(0.680999904871, at2.applied(), 1e-9);
        Implosion.Pull at3 = PHAGE.pull(ORIGIN, east(3, true)).orElseThrow();
        assertFalse(at3.capped());
        assertEquals(0.625, at3.applied(), 1e-12);
    }

    @Test
    void fromTheEdgeToTheCentreTheModelledTravelNeverPassesTheOriginLessTheMargin() {
        // Every 0.01 of a block from 6 in to 0, on the ground and in the air: the travel the model gives the applied speed
        // is never beyond the horizontal distance less the margin, never negative, and the impulse points AT the origin.
        for (boolean onGround : new boolean[] {true, false}) {
            for (int i = 600; i >= 0; i--) {
                double d = i / 100.0;
                Implosion.Pull p = PHAGE.pull(ORIGIN, east(d, onGround)).orElseThrow();
                double t = Implosion.travel(p.applied(), p.tickMultiplier());
                assertTrue(p.applied() >= 0, "never negative at d=" + d);
                assertTrue(t <= Math.max(0, d - Implosion.MARGIN) + 1e-9, "past the margin at d=" + d + " t=" + t);
                assertTrue(p.impulse().x() <= 0, "toward the origin (west) at d=" + d);
                assertEquals(0.0, p.impulse().y(), 0.0, "no vertical part");
                assertEquals(p.applied(), p.impulse().length(), 1e-12, "the impulse's length is the applied speed");
            }
        }
    }

    @Test
    void aVictimAlmostAtTheCentreIsNotThrownThrough() {
        Implosion.Pull p = PHAGE.pull(ORIGIN, east(0.3, true)).orElseThrow();
        assertEquals(1.1875, p.strength(), 1e-12, "the strongest strengths are nearest the centre...");
        assertEquals(0.0, p.applied(), 0.0, "...and the cap takes them to nothing");
        assertEquals(Vec3.ZERO, p.impulse());
        Implosion.Pull at = PHAGE.pull(ORIGIN, new Implosion.Footing(ORIGIN, true, 0.6f, 0)).orElseThrow();
        assertEquals(Vec3.ZERO, at.impulse(), "exactly at the origin: no direction, no NaN");
    }

    @Test
    void thePullIsHorizontalAndUsesTheHorizontalDistanceForTheCapButTheFeetToFeetDistanceForTheStrength() {
        // 3 east and 4 up: feet to feet 5 (the strength's), horizontal 3 (the cap's).
        Implosion.Pull p = PHAGE.pull(ORIGIN,
                new Implosion.Footing(new Vec3(13, 68, 20), false, 0.6f, 0)).orElseThrow();
        assertEquals(5.0, p.distance(), 1e-12);
        assertEquals(3.0, p.horizontal(), 1e-12);
        assertEquals((1 - 5.0 / 6) * 1.25, p.strength(), 1e-12);
        assertEquals(0.0, p.impulse().y(), 0.0);
        assertTrue(p.impulse().x() < 0);
    }

    /**
     * THE CAP USES THE HORIZONTAL DISTANCE, WHERE IT BINDS. Found by mutation (MUTHORIZ, the cap from the feet-to-feet
     * distance, survived the row above, whose pull is never capped under either). 1 east and 1.5 up, on the ground:
     * feet to feet 1.802776 (jshell, Math.hypot(1, 1.5)), horizontal 1. The horizontal cap is 0.5 x 0.454 = 0.227; the
     * feet-to-feet one would be 1.302776 x 0.454 = 0.591460, above the strength's 0.874422, so it would not bind.
     */
    @Test
    void whereTheCapBindsItIsTheHorizontalDistancesNotTheFeetToFeet() {
        Implosion.Pull p = PHAGE.pull(ORIGIN, new Implosion.Footing(new Vec3(11, 65.5, 20), true, 0.6f, 0))
                .orElseThrow();
        assertEquals(1.0, p.horizontal(), 1e-12);
        assertEquals(Math.hypot(1, 1.5), p.distance(), 1e-12);
        assertTrue(p.capped());
        assertEquals(0.226999968290, p.applied(), 1e-9, "(1 - 0.5) x (1 - 0.546000063419342)");
    }

    @Test
    void theDirectionIsFromTheFeetToTheOriginOnABearing() {
        // 3 east and 4 south (+z): pulled north-west, along (-3, -4) / 5. Uncapped: strength (1 - 5/6) x 1.25.
        Implosion.Pull p = PHAGE.pull(ORIGIN, new Implosion.Footing(new Vec3(13, 64, 24), true, 0.6f, 0))
                .orElseThrow();
        double s = (1 - 5.0 / 6) * 1.25;
        assertFalse(p.capped());
        assertEquals(-0.6 * s, p.impulse().x(), 1e-12);
        assertEquals(-0.8 * s, p.impulse().z(), 1e-12);
    }

    @Test
    void theResistanceCutsTheStrengthBeforeTheCap() {
        // d = 3, resistance 0.5: 0.3125, under the ground cap 1.135, so applied whole.
        Optional<Implosion.Pull> p = PHAGE.pull(ORIGIN,
                new Implosion.Footing(new Vec3(13, 64, 20), true, 0.6f, 0.5));
        assertEquals(0.3125, p.orElseThrow().applied(), 1e-12);
        assertEquals(0.5, p.orElseThrow().resistance(), 0.0, "the pull carries the resistance it used, for the line");
        assertTrue(p.orElseThrow().onGround(), "and whether it stood on the ground");
        assertFalse(PHAGE.pull(ORIGIN, east(3, false)).orElseThrow().onGround());
    }

    @Test
    void aBadShapeIsRefusedByName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(0, Implosion.Falloff.FLAT, 1, Implosion.Resistance.NONE), "reach 0");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(Double.NaN, Implosion.Falloff.FLAT, 1, Implosion.Resistance.NONE), "reach NaN");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(6, Implosion.Falloff.FLAT, -0.1, Implosion.Resistance.NONE), "a negative pull");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(6, null, 1, Implosion.Resistance.NONE), "no falloff");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(6, Implosion.Falloff.FLAT, 1, null), "no resistance");
        assertEquals(0.0, new Implosion(6, Implosion.Falloff.FLAT, 0, Implosion.Resistance.NONE).pull(), 0.0,
                "0 is allowed: it turns the pull off");
    }
}
