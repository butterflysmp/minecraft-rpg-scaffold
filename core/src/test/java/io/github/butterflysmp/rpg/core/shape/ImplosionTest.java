package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The void implosion as a pure shape, with the push's LIFT and a cap on WHERE THE VICTIM COMES TO REST (the seat's
 * ruling 2 on #204). Every expected number was EXECUTED in jshell (2026-11-17, scratch {@code flight.jsh} and
 * {@code flight2.jsh}) from the jar's model: gravity 0.08 ({@code Attributes.GRAVITY}), vertical drag {@code 0.98f}
 * and horizontal {@code (onGround ? friction : 1.0f) x 0.91f} chosen by the on-ground state at the tick's START
 * ({@code LivingEntity#travelInAir}), the landing zeroing the vertical ({@code Block#updateEntityMovementAfterFallOn}).
 * Never worked by hand.
 *
 * <p>The Phage's use: reach 6, vanilla's explosion falloff, pull 1.25, lift 1, explosion knockback resistance. A
 * victim's eye is 1.62 above its feet (a player's).
 */
class ImplosionTest {

    private static final Implosion PHAGE = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 1.25, 1.0,
            Implosion.Resistance.EXPLOSION_KNOCKBACK);
    /** The test-only fixture's form: no lift, so it stays on the ground. */
    private static final Implosion FLAT = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 1.25, 0.0,
            Implosion.Resistance.NONE);
    private static final Vec3 ORIGIN = new Vec3(10, 64, 20);
    private static final double EYE = 1.62;
    private static final double GROUND = 0.546000063419342;
    private static final double AIR = 0.9100000262260437;
    private static final float ICE = 0.98f;

    /** A victim due EAST of the origin at {@code d} blocks, on stone, ice or in the air, no resistance. */
    private static Implosion.Footing east(double d, boolean onGround, float friction) {
        return new Implosion.Footing(new Vec3(10 + d, 64, 20), EYE, onGround, friction, 0.0);
    }

    private static Implosion.Footing stone(double d) {
        return east(d, true, Implosion.DEFAULT_BLOCK_FRICTION);
    }

    /** The OLD push's modelled travel at d, on stone: its horizontal part times the flight its vertical part gives. */
    private static double oldPushTravel(double d) {
        double k = 1 - d / 6;
        double len = Math.hypot(d, EYE);
        return k * d / len * Implosion.travelFactor(k * EYE / len, true, Implosion.DEFAULT_BLOCK_FRICTION);
    }

    // --- the jar's model -------------------------------------------------------------------------------------------

    @Test
    void theTickMultiplierIsTheBlocksFrictionTimesPointNineOneOnTheGroundAndPointNineOneInTheAir() {
        assertEquals(GROUND, Implosion.tickMultiplier(true, 0.6f), 0.0, "(double) (0.6f * 0.91f), a float product");
        assertEquals(AIR, Implosion.tickMultiplier(false, 0.6f), 0.0, "airborne: the block is not read, 1.0f");
        assertEquals((double) (0.98f * 0.91f), Implosion.tickMultiplier(true, ICE), 0.0, "ice's own friction");
    }

    @Test
    void withNoLiftTheTravelIsTheGroundSlideAlone() {
        assertEquals(2.202643479494, Implosion.travelFactor(0, true, 0.6f), 1e-9, "1 / (1 - 0.546)");
        assertEquals(9.242148103833, Implosion.travelFactor(0, true, ICE), 1e-9, "1 / (1 - 0.8918)");
    }

    @Test
    void aLiftedVictimFliesThenSlidesAndTheFlightIsModelledTickByTick() {
        // d = 2's lift, 0.419614494380: the take-off tick still takes the ground's friction (it began on the ground).
        assertEquals(5.343010301086, Implosion.travelFactor(0.419614494380, true, 0.6f), 1e-9);
        assertEquals(10.318250184583, Implosion.travelFactor(0.419614494380, true, ICE), 1e-9);
    }

    /**
     * THE VERTICAL DRAG, PINNED WHERE IT MATTERS. Found by mutation: MUTDRAG (the 0.98f removed) SURVIVED every row
     * above, because the horizontal travel depends only on WHICH TICK the victim lands, and at d = 2's lift the drag
     * does not move it (12 ticks either way). Measured in jshell ({@code drag.jsh}): it moves the landing at 4265 of
     * 10001 lifts from 0 to 1 in steps of 0.0001, the first at 0.0396; at d = 0.3's lift, 0.934117904735, it lands on
     * tick 24 with the drag and 25 without. So the row is THERE.
     */
    @Test
    void theVerticalDragDecidesTheLandingTickForAHighLift() {
        assertEquals(6.510831222985, Implosion.travelFactor(0.934117904735, true, 0.6f), 1e-9, "d = 0.3's lift");
    }

    @Test
    void aVictimAlreadyInTheAirIsBoundedByTheAirsDragForever() {
        // Its height above the floor is unknown, so the bound is the slowest decay any flight can have: air's 0.91.
        assertEquals(1 / (1 - AIR), Implosion.travelFactor(0.3, false, 0.6f), 1e-12);
        assertEquals(1 / (1 - AIR), Implosion.travelFactor(0.0, false, ICE), 1e-12);
    }

    @Test
    void theApexIsTheHighestTheLiftCarriesTheFeet() {
        assertEquals(0.0, Implosion.apex(0), 0.0);
        assertEquals(1.250002994808, Implosion.apex(0.419614494380), 1e-9, "d = 2");
        assertEquals(3.200023586695, Implosion.apex(0.709113425120), 1e-9, "d = 1: above the safe fall");
    }

    // --- the two parts: the push's own, at the victim -----------------------------------------------------------

    @Test
    void theHorizontalIsOnePointTwoFiveTimesThePushsAndTheLiftIsThePushsOwn() {
        Implosion.Pull p = PHAGE.pull(ORIGIN, stone(2)).orElseThrow();
        assertEquals(0.666666666667, p.push(), 1e-9, "1 - 2/6, x (1 - 0)");
        assertEquals(0.518042585654, p.pushHorizontal(), 1e-9, "k x 2 / hypot(2, 1.62)");
        assertEquals(0.419614494380, p.pushVertical(), 1e-9, "k x 1.62 / hypot(2, 1.62)");
        assertEquals(0.647553232067, p.strength(), 1e-9, "1.25 x the push's horizontal");
        assertEquals(0.419614494380, p.vertical(), 1e-9, "the push's own lift, x 1");
        assertEquals(0.419614494380, p.impulse().y(), 1e-9, "the lift reaches the impulse unscaled");
    }

    @Test
    void theResistanceCutsBothParts() {
        Implosion.Pull none = PHAGE.pull(ORIGIN, stone(4)).orElseThrow();
        Optional<Implosion.Pull> half = PHAGE.pull(ORIGIN,
                new Implosion.Footing(new Vec3(14, 64, 20), EYE, true, 0.6f, 0.5));
        assertEquals(none.strength() / 2, half.orElseThrow().strength(), 1e-12);
        assertEquals(none.vertical() / 2, half.orElseThrow().vertical(), 1e-12);
        assertEquals(0.5, half.orElseThrow().resistance(), 0.0, "carried, for the line");
    }

    @Test
    void aZeroLiftKeepsTheVictimOnTheGround() {
        Implosion.Pull p = FLAT.pull(ORIGIN, stone(2)).orElseThrow();
        assertEquals(0.0, p.vertical(), 0.0);
        assertEquals(0.0, p.impulse().y(), 0.0);
        assertEquals(2.202643479494, p.travelFactor(), 1e-9, "the ground slide alone");
        assertEquals(0.680999904871, p.capSpeed(), 1e-9, "(2 - 0.5) x (1 - 0.546), the flat pull's cap");
    }

    // --- THE CAP: where the victim comes to rest ----------------------------------------------------------------

    @Test
    void atTwoBlocksTheCapBindsAndHeComesToRestHalfABlockShort() {
        Implosion.Pull p = PHAGE.pull(ORIGIN, stone(2)).orElseThrow();
        assertTrue(p.capped());
        assertEquals(0.280740615397, p.applied(), 1e-9, "1.5 / 5.343010301086");
        assertEquals(1.5, p.travel(), 1e-9, "the rest point: 2 less the margin");
    }

    /**
     * BEN'S "A LITTLE BIT STRONGER", PINNED: at d = 2, 3, 4 and 5 on stone, the pull's travel beside the old push's
     * (jshell). Where the cap does not bind the pull carries him EXACTLY 1.25 x as far: the flight is linear in the
     * horizontal speed, and the lift is the push's own. Where it binds (d = 2), he stops half a block short.
     */
    @Test
    void theTravelAtTwoThreeFourAndFiveBesideTheOldPush() {
        double[][] grid = {
                // d, old push travel, new travel, capped
                {2, 2.767906871550, 1.500000000000, 1},
                {3, 1.893793131547, 2.367241414434, 0},
                {4, 1.152768372745, 1.440960465932, 0},
                {5, 0.481803611640, 0.602254514550, 0}};
        for (double[] row : grid) {
            Implosion.Pull p = PHAGE.pull(ORIGIN, stone(row[0])).orElseThrow();
            assertEquals(row[1], oldPushTravel(row[0]), 1e-9, "old at d=" + row[0]);
            assertEquals(row[2], p.travel(), 1e-9, "new at d=" + row[0]);
            assertEquals(row[3] == 1, p.capped(), "capped at d=" + row[0]);
            if (row[3] == 0) assertEquals(1.25 * row[1], p.travel(), 1e-9, "1.25 x the push at d=" + row[0]);
        }
    }

    /**
     * THE SWEEP the seat asked for: every 0.01 of a block from the reach to the centre, on stone, on ice and in the
     * air. The rest point never passes the origin less the margin; the horizontal never points away; the lift is never
     * scaled; and where the cap does not bind on stone, the travel is 1.25 x the old push's (within 1e-9).
     */
    @Test
    void fromTheReachToTheCentreOnStoneIceAndInTheAir() {
        record Ground(String name, boolean onGround, float friction) {}
        for (Ground g : new Ground[] {new Ground("stone", true, 0.6f), new Ground("ice", true, ICE),
                new Ground("air", false, 0.6f)}) {
            for (int i = 600; i >= 0; i--) {
                double d = i / 100.0;
                Implosion.Pull p = PHAGE.pull(ORIGIN, east(d, g.onGround(), g.friction())).orElseThrow();
                String at = g.name() + " d=" + d;
                assertTrue(p.applied() >= 0, "never negative, " + at);
                assertTrue(p.travel() <= Math.max(0, d - Implosion.MARGIN) + 1e-9, "past the margin, " + at);
                assertTrue(p.impulse().x() <= 0, "toward the origin (west), " + at);
                assertEquals(p.pushVertical(), p.vertical(), 1e-12, "the lift unscaled, " + at);
                assertEquals(p.applied(), Math.hypot(p.impulse().x(), p.impulse().z()), 1e-12, at);
                if (g.name().equals("stone") && !p.capped()) {
                    assertEquals(1.25 * oldPushTravel(d), p.travel(), 1e-9, "1.25 x the push, " + at);
                }
            }
        }
    }

    @Test
    void onIceTheCapBindsFartherOut() {
        // Ice slides ~4x as far as stone, so the same pull is cut at d = 2, 3 and 4 and runs whole only at 5.
        Implosion.Pull two = PHAGE.pull(ORIGIN, east(2, true, ICE)).orElseThrow();
        assertEquals(0.145373486121, two.applied(), 1e-9, "1.5 / 10.318250184583");
        assertEquals(1.5, two.travel(), 1e-9);
        assertTrue(PHAGE.pull(ORIGIN, east(4, true, ICE)).orElseThrow().capped());
        assertFalse(PHAGE.pull(ORIGIN, east(5, true, ICE)).orElseThrow().capped());
    }

    @Test
    void aJumpingVictimIsCappedByTheAirBound() {
        Implosion.Pull p = PHAGE.pull(ORIGIN, east(3, false, 0.6f)).orElseThrow();
        assertTrue(p.capped());
        assertEquals(2.5 * (1 - AIR), p.applied(), 1e-12, "(3 - 0.5) / 11.111");
        assertEquals(2.5, p.travel(), 1e-9);
    }

    @Test
    void whereTheCapBindsItIsTheHorizontalDistancesNotTheFeetToFeet() {
        // 1 east and 1.5 up, on the ground: feet to feet 1.802776, horizontal 1. Resting at 1 - 0.5 = 0.5, not 1.3.
        Implosion.Pull p = PHAGE.pull(ORIGIN, new Implosion.Footing(new Vec3(11, 65.5, 20), EYE, true, 0.6f, 0))
                .orElseThrow();
        assertEquals(1.0, p.horizontal(), 1e-12);
        assertEquals(Math.hypot(1, 1.5), p.distance(), 1e-12);
        assertTrue(p.capped());
        assertEquals(0.5, p.travel(), 1e-9);
    }

    @Test
    void aVictimAlmostAtTheCentreIsNotThrownThroughButIsStillLifted() {
        Implosion.Pull p = PHAGE.pull(ORIGIN, stone(0.3)).orElseThrow();
        assertEquals(0.216230996466, p.strength(), 1e-9);
        assertEquals(0.0, p.applied(), 0.0, "inside the margin: no horizontal at all");
        assertEquals(0.0, p.impulse().x(), 0.0);
        assertEquals(0.934117904735, p.vertical(), 1e-9, "the lift is not scaled");
        Implosion.Pull at = PHAGE.pull(ORIGIN, new Implosion.Footing(ORIGIN, EYE, true, 0.6f, 0)).orElseThrow();
        assertEquals(0.0, at.impulse().x(), 0.0, "exactly at the origin: no horizontal direction, no NaN");
        assertEquals(1.0, at.impulse().y(), 1e-12, "straight up, the push's own: 1 x (1.62 / 1.62)");
    }

    // --- the fall ----------------------------------------------------------------------------------------------

    @Test
    void theSafeLandingIsAskedForWhereTheApexReachesTheSafeFallLessItsMargin() {
        assertFalse(PHAGE.pull(ORIGIN, stone(2)).orElseThrow().safeLanding(), "apex 1.25");
        assertFalse(PHAGE.pull(ORIGIN, stone(1.5)).orElseThrow().safeLanding(), "apex 2.04");
        assertTrue(PHAGE.pull(ORIGIN, stone(1)).orElseThrow().safeLanding(), "apex 3.20, above 3.0");
        // THE MARGIN'S OWN ROW. Found by mutation: MUTSAFE (the threshold at 3.0, no margin) survived the rows above,
        // whose apexes are all outside 2.5 to 3.0, the one band where the two disagree. At d = 1.2 the apex is
        // 2.687265638817 (jshell, apex.jsh), inside it: marked with the margin, not without.
        assertEquals(2.687265638817, PHAGE.pull(ORIGIN, stone(1.2)).orElseThrow().apex(), 1e-9);
        assertTrue(PHAGE.pull(ORIGIN, stone(1.2)).orElseThrow().safeLanding(), "apex 2.69: inside the margin");
        assertFalse(PHAGE.pull(ORIGIN, stone(1.3)).orElseThrow().safeLanding(), "apex 2.45: below it");
        assertTrue(PHAGE.pull(ORIGIN, stone(0.3)).orElseThrow().safeLanding(), "apex 5.24");
        assertFalse(FLAT.pull(ORIGIN, stone(0.3)).orElseThrow().safeLanding(), "no lift, no fall");
    }

    // --- the reach and the share, unchanged -----------------------------------------------------------------------

    @Test
    void theDamageShareIsVanillasCurveForTheExplosionFalloffAndOneForFlat() {
        assertEquals(1.0, PHAGE.share(0), 0.0);
        assertEquals(0.375, PHAGE.share(3), 1e-12);
        assertEquals(0.0, PHAGE.share(6), 0.0);
        Implosion flat = new Implosion(6, Implosion.Falloff.FLAT, 1.0, 0.0, Implosion.Resistance.NONE);
        assertEquals(1.0, flat.share(5.99), 0.0);
        assertEquals(0.0, flat.share(6.01), 0.0);
    }

    @Test
    void theReachIsInclusiveAndASphere() {
        assertTrue(PHAGE.inReach(ORIGIN, new Vec3(16, 64, 20)));
        assertFalse(PHAGE.inReach(ORIGIN, new Vec3(16.001, 64, 20)));
        assertFalse(PHAGE.inReach(ORIGIN, new Vec3(15, 64, 24)), "hypot(5, 4) > 6");
        assertTrue(PHAGE.pull(ORIGIN, stone(6.001)).isEmpty());
        assertEquals(0.0, PHAGE.pull(ORIGIN, stone(6.0)).orElseThrow().applied(), 0.0);
    }

    @Test
    void aBadShapeIsRefusedByName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(0, Implosion.Falloff.FLAT, 1, 0, Implosion.Resistance.NONE), "reach 0");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(6, Implosion.Falloff.FLAT, -0.1, 0, Implosion.Resistance.NONE), "a negative pull");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(6, Implosion.Falloff.FLAT, 1, -0.1, Implosion.Resistance.NONE), "a negative lift");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(6, null, 1, 0, Implosion.Resistance.NONE), "no falloff");
        assertThrows(IllegalArgumentException.class,
                () -> new Implosion(6, Implosion.Falloff.FLAT, 1, 0, null), "no resistance");
    }
}
