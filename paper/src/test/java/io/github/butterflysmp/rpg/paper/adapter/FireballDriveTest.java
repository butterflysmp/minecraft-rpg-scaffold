package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fireball drive (LEGACY-B, Q-B3 COMPENSATE): a body driven with {@link FireballDrive#velocityFor}
 * moves exactly the computed step through vanilla's {@code applyInertia}.
 *
 * <p><b>VANILLA'S LAW IS WRITTEN OUT HERE, NOT CALLED FROM PRODUCTION.</b> A test that inverted the
 * law with the same code that applies it would pass whatever the law was. {@link #vanillaMove} is
 * transcribed from {@code AbstractHurtingProjectile.applyInertia} in the pinned server jar
 * ({@code javap -c}): {@code (delta + normalize(delta) x 0.1) x (double) 0.95f}, then
 * {@code setPos(position + delta)}. The test's constants are typed from the jar, separately from
 * {@code FireballDrive}'s, so a wrong constant in production reddens here.
 *
 * <p><b>WHAT THIS CANNOT SEE: the jar itself changing.</b> Then both copies are stale together and
 * this stays green. Gate row LB9 is the only witness of the law on a live server.
 */
class FireballDriveTest {

    /** The jar's {@code accelerationPower} default and {@code getInertia()}, typed independently. */
    private static final double JAR_PUSH = 0.1;
    private static final double JAR_INERTIA = (double) 0.95f;

    /** One tick of {@code AbstractHurtingProjectile}: the displacement a delta of {@code delta} produces. */
    private static Vec3 vanillaMove(Vec3 delta) {
        return delta.add(delta.normalize().scale(JAR_PUSH)).scale(JAR_INERTIA);
    }

    /**
     * THE CONTROL FIRST: UNCOMPENSATED, THE STAFF'S STEP OVERSHOOTS, SO THE ROW BELOW HAS SOMETHING TO FIX.
     *
     * <p>Without this a {@code velocityFor} that returned its argument would only be caught if the
     * vanilla law were right in this file. The plan's figure is 1.52 a tick at speed 1.5 (awk);
     * asserted as a computed overshoot, not as a typed literal.
     */
    @Test
    void uncompensatedTheStaffsStepRunsAhead() {
        Vec3 step = new Vec3(1.5, 0, 0);
        double moved = vanillaMove(step).length();
        assertEquals((1.5 + JAR_PUSH) * JAR_INERTIA, moved, 1e-12);
        assertTrue(moved > 1.5, "an uncompensated fireball leads the flight: " + moved);
    }

    /**
     * COMPENSATED, EVERY STEP COMES BACK EXACTLY -- OVER A GRID OF DIRECTIONS AND SPEEDS, NOT ONE.
     *
     * <p>The staff's own step (1.5 east, gravity 0) is in the grid, with the other axes, a diagonal,
     * a downward arc's step and the Plume's 2.5, because an inversion that was only right along +X
     * (a swapped component, a dropped sign) would pass a one-vector row.
     *
     * <p>Mutations: drop the {@code - ACCELERATION_POWER} -> every row reddens by about 0.095. Use
     * {@code 0.95} for the inertia -> the tolerance 1e-12 reddens (the residual is ~2e-8 at 1.5).
     * Return {@code step} unchanged -> every row reddens by the control's overshoot.
     */
    @Test
    void aCompensatedDriveMovesExactlyTheComputedStep() {
        List<Vec3> steps = List.of(
                new Vec3(1.5, 0, 0),                     // the Blaze King's Staff, east
                new Vec3(-1.5, 0, 0),
                new Vec3(0, 0, 1.5),
                new Vec3(0, 1.5, 0),
                new Vec3(1.0, -0.4, 0.7),                // an arbitrary diagonal
                new Vec3(1.2, -0.25, 0),                 // an arcing step, gravity already in it
                new Vec3(0, 0, -2.5),                    // the Plume's speed
                new Vec3(0.2, 0, 0));                    // just above the floor
        for (Vec3 step : steps) {
            Vec3 moved = vanillaMove(FireballDrive.velocityFor(step));
            assertEquals(step.x(), moved.x(), 1e-12, "x of " + step);
            assertEquals(step.y(), moved.y(), 1e-12, "y of " + step);
            assertEquals(step.z(), moved.z(), 1e-12, "z of " + step);
        }
    }

    /**
     * BELOW THE FLOOR THE BODY STALLS, IT DOES NOT REVERSE -- AN UNREACHABLE ARM, PINNED SO NOBODY
     * "FIXES" IT INTO A NEGATIVE MAGNITUDE.
     *
     * <p>No shipped fireball is slower than the 0.095 floor (the staff is 1.5 with no gravity), so
     * production never takes this arm. A negative magnitude would point the delta backwards and
     * vanilla's push would carry the body backwards: the opposite of the flight.
     */
    @Test
    void belowTheFloorTheDriveIsZeroNeverBackwards() {
        assertEquals(Vec3.ZERO, FireballDrive.velocityFor(new Vec3(0.05, 0, 0)));
        assertEquals(Vec3.ZERO, FireballDrive.velocityFor(Vec3.ZERO));
        assertEquals(JAR_PUSH * JAR_INERTIA, FireballDrive.FLOOR, 0.0, "the floor is the jar's product");
    }
}
