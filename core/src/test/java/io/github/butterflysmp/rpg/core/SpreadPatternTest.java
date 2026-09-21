package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.combat.SpreadPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE VIEW-PLANE RING, AND THE POLE ROWS ARE THE POINT OF THIS FILE.
 *
 * <h2>*** WHY A ROW STAGED AT THE HORIZON MEASURES THE FIXTURE ***</h2>
 *
 * <p>The mutation this class exists to catch is <b>deriving the basis by crossing the look vector
 * with world up.</b> That derivation is <b>correct at every pitch except two</b>:
 * {@code forward x worldUp} is horizontal and perpendicular to forward's horizontal projection,
 * which is precisely the shooter's right, for all {@code |pitch| < 90}. It is the ZERO VECTOR only
 * AT the pole.
 *
 * <p><b>So the pitch-0 rows below cannot tell the two derivations apart, and no amount of care in
 * writing them would change that.</b> They are there to pin the geometry -- counts, angles, the
 * ring's shape -- and they are honestly labelled as blind to the basis. {@code MUT14-WORLDUP}'s
 * entire kill set is {@link #theRingSurvivesLookingStraightUp} and
 * {@link #theRingSurvivesLookingStraightDown}. <b>Delete those two and the basis ships
 * unguarded with a full-looking kill set.</b>
 *
 * <h2>THE SIGN OF UP IS UNGUARDABLE AND NO ROW HERE PRETENDS OTHERWISE</h2>
 *
 * <p>Six points spaced 60 degrees apart map onto themselves under reflection -- {@code {0, 60, 120,
 * 180, 240, 300}} reversed is the same SET -- so flipping the {@code up} axis produces a
 * byte-identical ring. <b>No assertion on the handedness of the basis can fail</b>, and writing one
 * would be a row that cannot redden. Named here in the manner {@code HitDamage} names its three
 * commutative joints as UNGUARDABLE rather than unguarded.
 *
 * <p>Consequently every row below asserts SET properties -- each direction's angle to the aim, the
 * ring's closure, the plane it lies in -- and never "the first ring body is on the right".
 */
class SpreadPatternTest {

    /** The shipped spread, read off {@code scatter_shot.yml} rather than invented. */
    private static CastSpec.Spread shipped() {
        return new CastSpec.Spread(7, 5);
    }

    /** Looking due south (+Z) at the horizon: Bukkit yaw 0. */
    private static final Vec3 SOUTH = new Vec3(0, 0, 1);

    /**
     * The shooter's right when facing south. West, {@code -X} -- facing south, east is on your
     * LEFT. Checked against {@code DashAim}'s table rather than reasoned from a compass, because
     * the sign is easy to talk oneself into.
     */
    private static final Vec3 RIGHT_AT_SOUTH = new Vec3(-1, 0, 0);

    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 DOWN = new Vec3(0, -1, 0);

    // --- the count, and the aim vector's place in it -------------------------------------------

    /**
     * SEVEN DIRECTIONS FOR A COUNT OF SEVEN, AND THE FIRST IS THE AIM VECTOR.
     *
     * <p>The ordering is a documented guarantee rather than an accident -- a caller rendering the
     * centre body differently needs to know which one it is -- so it is asserted rather than left
     * to whoever reads the loop. {@code DrawFan} states the opposite for its own output and this
     * row is why the two files disagree on purpose.
     */
    @Test
    void theCountIsTotalAndTheAimVectorComesFirst() {
        List<Vec3> directions = SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH, shipped());

        assertEquals(7, directions.size(), "count is the TOTAL, not the ring size");
        assertEquals(1.0, directions.get(0).dot(SOUTH), 1e-9,
                "the first direction IS the aim vector");
    }

    /**
     * EVERY DIRECTION IS A UNIT VECTOR.
     *
     * <p>{@code CastExecutor.launchOne} scales by {@code speed}, so a ring body whose direction was
     * longer than one would fly FASTER than the centre body -- a difference invisible in any count
     * and visible in the game as arrows that arrive at different times.
     */
    @Test
    void everyDirectionIsNormalised() {
        for (Vec3 direction : SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH, shipped())) {
            assertEquals(1.0, direction.length(), 1e-9,
                    "a longer direction means a faster arrow: " + direction);
        }
    }

    /**
     * EVERY RING BODY SITS AT EXACTLY THE AUTHORED ANGLE FROM THE AIM.
     *
     * <p>This is the row {@code MUT14-COLLAPSE} (all seven on the aim vector) reddens, and the one
     * that makes {@code 5} mean five degrees rather than five of something else. The angle is
     * recovered with {@code acos(dot)}, which for unit vectors IS the angle -- not re-derived from
     * the construction, or the row would assert the implementation against itself.
     *
     * <p>Staged at 5 against a count of 7 against a ring of 6: <b>no two quantities this row reads
     * are equal</b>, so a transposition has nowhere to hide.
     */
    @Test
    void everyRingBodySitsAtTheAuthoredAngleFromTheAim() {
        List<Vec3> directions = SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH, shipped());

        for (int i = 1; i < directions.size(); i++) {
            double degrees = Math.toDegrees(Math.acos(directions.get(i).dot(SOUTH)));
            assertEquals(5.0, degrees, 1e-9, "ring body " + i + " is off-angle");
        }
    }

    /**
     * THE RING IS EVENLY SPACED, WHICH IS WHAT MAKES SIX BODIES A HEXAGON.
     *
     * <p>Measured as the angle between NEIGHBOURS in the plane, recovered from each body's lateral
     * component. Six bodies means 60 degrees apart; a ring that bunched would still pass the
     * off-angle row above.
     */
    @Test
    void theRingIsEvenlySpacedIntoAHexagon() {
        List<Vec3> directions = SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH, shipped());
        Vec3 up = RIGHT_AT_SOUTH.cross(SOUTH).normalize();

        double[] bearings = new double[6];
        for (int i = 1; i < directions.size(); i++) {
            Vec3 lateral = directions.get(i).subtract(SOUTH.scale(directions.get(i).dot(SOUTH)));
            bearings[i - 1] = Math.toDegrees(Math.atan2(lateral.dot(up), lateral.dot(RIGHT_AT_SOUTH)));
        }

        for (int i = 0; i < bearings.length; i++) {
            double expected = 60.0 * i;
            double actual = (bearings[i] + 360.0) % 360.0;
            assertEquals(expected, actual, 1e-9, "ring body " + (i + 1) + " is mis-spaced");
        }
    }

    /**
     * THE RING CLOSES: its lateral offsets sum to zero.
     *
     * <p>A symmetric ring has no net bias, so a spread does not silently pull a burst to one side.
     * Independent of the spacing row -- a ring could be evenly spaced over less than a full turn
     * and fail this -- and it is the property a player would feel rather than see.
     */
    @Test
    void theRingIsBalancedAndPullsNowhere() {
        List<Vec3> directions = SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH, shipped());

        Vec3 sum = Vec3.ZERO;
        for (int i = 1; i < directions.size(); i++) {
            Vec3 d = directions.get(i);
            sum = sum.add(d.subtract(SOUTH.scale(d.dot(SOUTH))));
        }
        assertEquals(0.0, sum.length(), 1e-9, "the ring leans: " + sum);
    }

    // --- THE POLE. THESE TWO ROWS ARE THE ONLY GUARD OF THE BASIS. -----------------------------

    /**
     * *** LOOKING STRAIGHT UP, THE HEXAGON IS STILL A HEXAGON. ***
     *
     * <p><b>THIS ROW AND ITS TWIN ARE THE SOLE GUARD OF THE BASIS DERIVATION</b>, and the class
     * javadoc says why: a world-up basis agrees with the shooter's own at every other pitch, so
     * every row above is blind to it. Here {@code forward} IS world up, {@code forward x worldUp}
     * is the zero vector, and a world-up derivation collapses the ring onto the aim vector or
     * produces NaN.
     *
     * <p><b>Ben's ruling is that the pattern looks identical from the shooter's side at every
     * angle, including straight up</b>, and this is where that is checked. It is not an edge case
     * a player has to look for: it is what happens the first time someone shoots at a bird.
     *
     * <p>The right vector staged here is a real one -- the shooter is facing up while YAWED south,
     * so their right is still west. That is exactly what {@code ViewAim} supplies, because it reads
     * the yaw rather than the look vector.
     */
    @Test
    void theRingSurvivesLookingStraightUp() {
        List<Vec3> directions = SpreadPattern.directionsFor(UP, RIGHT_AT_SOUTH, shipped());

        assertEquals(7, directions.size());
        for (int i = 1; i < directions.size(); i++) {
            Vec3 d = directions.get(i);
            assertFalse(Double.isNaN(d.x() + d.y() + d.z()), "ring body " + i + " is NaN");
            assertEquals(1.0, d.length(), 1e-9);
            assertEquals(5.0, Math.toDegrees(Math.acos(d.dot(UP))), 1e-9,
                    "ring body " + i + " collapsed onto the aim -- the basis degenerated");
        }
    }

    /** The other pole, and it fails independently: a sign error reaches one and not the other. */
    @Test
    void theRingSurvivesLookingStraightDown() {
        List<Vec3> directions = SpreadPattern.directionsFor(DOWN, RIGHT_AT_SOUTH, shipped());

        assertEquals(7, directions.size());
        for (int i = 1; i < directions.size(); i++) {
            Vec3 d = directions.get(i);
            assertFalse(Double.isNaN(d.x() + d.y() + d.z()), "ring body " + i + " is NaN");
            assertEquals(5.0, Math.toDegrees(Math.acos(d.dot(DOWN))), 1e-9,
                    "ring body " + i + " collapsed onto the aim -- the basis degenerated");
        }
    }

    /**
     * THE RING IS STILL EVENLY SPACED AT THE POLE, NOT MERELY PRESENT.
     *
     * <p>The two rows above would pass a basis that was well-conditioned but SKEWED -- six bodies
     * all at 5 degrees and bunched into half a turn. Stated separately because "it did not
     * collapse" and "it is still a hexagon" are different claims and only one of them is Ben's
     * ruling.
     *
     * <p>Each body is assigned to the nearest 60-degree spoke and the spokes are required to be
     * occupied exactly once. <b>The comparison is CIRCULAR</b> -- a bearing of 359.999 belongs to
     * spoke 0, not to a spoke 360 degrees away -- because a linear difference would fail this row
     * on a correct ring whenever floating point put a body a hair below the wrap.
     */
    @Test
    void theHexagonIsStillAHexagonAtThePole() {
        List<Vec3> directions = SpreadPattern.directionsFor(UP, RIGHT_AT_SOUTH, shipped());
        Vec3 up = RIGHT_AT_SOUTH.cross(UP).normalize();

        boolean[] seen = new boolean[6];
        for (int i = 1; i < directions.size(); i++) {
            Vec3 d = directions.get(i);
            Vec3 lateral = d.subtract(UP.scale(d.dot(UP)));
            double bearing = (Math.toDegrees(Math.atan2(lateral.dot(up), lateral.dot(RIGHT_AT_SOUTH)))
                    + 360.0) % 360.0;

            int slot = (int) Math.round(bearing / 60.0) % 6;
            double offSpoke = Math.abs(((bearing - 60.0 * slot) + 540.0) % 360.0 - 180.0);
            assertEquals(0.0, offSpoke, 1e-6,
                    "ring body " + i + " is not on a 60-degree spoke; bearing " + bearing);
            assertFalse(seen[slot], "two ring bodies share spoke " + slot);
            seen[slot] = true;
        }
        for (int slot = 0; slot < 6; slot++) {
            assertTrue(seen[slot], "spoke " + slot + " is empty -- the hexagon is incomplete");
        }
    }

    /**
     * A NEAR-POLE PITCH IS WELL-CONDITIONED, WHICH IS A DIFFERENT QUESTION FROM THE POLE ITSELF.
     *
     * <p>A world-up basis degenerates AT 90 and merely gets small NEAR it, so a reader could
     * reasonably worry that the yaw basis has its own precision cliff approaching the pole. It does
     * not -- the right vector is a function of yaw alone and is a unit vector at every pitch -- and
     * <b>this row is the measurement rather than the reassurance.</b> Swept across the last degree.
     */
    @Test
    void theRingHoldsItsShapeApproachingThePole() {
        for (double pitch : new double[] {80, 89, 89.9, 89.99, 89.999}) {
            double p = Math.toRadians(pitch);
            Vec3 forward = new Vec3(0, Math.sin(p), Math.cos(p));

            for (Vec3 d : SpreadPattern.directionsFor(forward, RIGHT_AT_SOUTH, shipped()).subList(1, 7)) {
                assertEquals(5.0, Math.toDegrees(Math.acos(d.dot(forward.normalize()))), 1e-6,
                        "the ring degrades at pitch " + pitch);
            }
        }
    }

    // --- the degenerate basis, which is a REACHABLE guard --------------------------------------

    /**
     * A DEGENERATE BASIS THROWS, AND THE ARM IS REACHABLE RATHER THAN DECORATIVE.
     *
     * <p>{@code Aim}'s two-argument convenience derives the right horizontally and yields ZERO at
     * pitch +/-90, so a caller who reaches for the shorter constructor and fires a spread straight
     * up arrives here. <b>This row CAUSES that condition</b> rather than asserting the arm exists,
     * and the message is checked because it is the thing that tells the next person where to look.
     *
     * <p>Without the throw the cast would fire seven bodies down one line while the tooltip
     * advertised a spread -- resolving perfectly and doing nothing, which is the failure this
     * project treats as worst.
     */
    @Test
    void aZeroRightIsRefusedRatherThanCollapsingTheRing() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> SpreadPattern.directionsFor(UP, Vec3.ZERO, shipped()));
        assertTrue(thrown.getMessage().contains("two-argument"),
                "the message must name where a zero right comes from; got: " + thrown.getMessage());
    }

    /**
     * A RIGHT PARALLEL TO THE AIM IS THE SAME DEFECT, ARRIVED AT DIFFERENTLY.
     *
     * <p>Not reachable from {@code ViewAim}, which is why it is stated: a future caller computing a
     * basis by hand can produce a non-zero right that is useless, and "not zero" is the check they
     * would write.
     *
     * <h2>*** AND IT IS THE SOLE ROW THAT DISTINGUISHES THE BASIS AXIS. THE NAME DOES NOT SAY SO,
     * WHICH IS WHY THIS PARAGRAPH DOES. ***</h2>
     *
     * <p>Measured 2026-09-21 by comparing two mutation kill sets over the whole reactor:
     *
     * <pre>
     * MUT14WORLDUP   the basis derived from world up   -> 4 rows
     * MUT14COLLAPSE  the ring offset forced to zero    -> 7 rows
     *
     * shared by both        theRingSurvivesLookingStraightUp
     *                       theRingSurvivesLookingStraightDown
     *                       theHexagonIsStillAHexagonAtThePole
     * unique to WORLDUP     THIS ROW
     * </pre>
     *
     * <p><b>The three pole rows cannot distinguish the two axes, because the geometry mutation
     * kills them too.</b> This row is the only one that dies under a basis mutation and survives a
     * geometry one -- it fires because a world-up derivation IGNORES the {@code right} parameter
     * entirely, and ignoring the parameter is precisely what the basis axis is about.
     *
     * <p>Recorded here per {@code CLAUDE.md}'s rule that <b>a row can be the only guard of
     * something it does not mention</b>, and that such a row is the one the next person deletes
     * while tidying. It reads as a validation row. It is also the basis row.
     */
    @Test
    void aRightParallelToTheAimIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> SpreadPattern.directionsFor(SOUTH, SOUTH, shipped()));
        assertThrows(IllegalArgumentException.class,
                () -> SpreadPattern.directionsFor(SOUTH, SOUTH.negate(), shipped()));
    }

    /**
     * THE RIGHT NEED NOT ARRIVE NORMALISED, and a caller should not have to remember.
     *
     * <p>The control for the two refusal rows above: a guard keyed on length rather than on
     * direction would refuse this, and it is a perfectly good basis.
     */
    @Test
    void anUnnormalisedRightIsAcceptedAndNormalisedHere() {
        List<Vec3> scaled = SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH.scale(37.5), shipped());
        List<Vec3> unit = SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH, shipped());

        for (int i = 0; i < unit.size(); i++) {
            assertEquals(1.0, scaled.get(i).dot(unit.get(i)), 1e-9,
                    "direction " + i + " moved when the right vector was merely longer");
        }
    }

    /**
     * A SMALLEST-LEGAL SPREAD PUTS ITS ONE RING BODY AT THE AUTHORED ANGLE.
     *
     * <p>{@code count: 2} has a ring of one, where "evenly spaced" is vacuous and the loop's
     * {@code 2 * PI * i / ring} divides by one. A count nobody ships, reachable by anyone editing a
     * yml, and the arithmetic degenerates exactly where a reader would not look.
     */
    @Test
    void theSmallestLegalSpreadStillPlacesItsOneRingBody() {
        List<Vec3> directions = SpreadPattern.directionsFor(SOUTH, RIGHT_AT_SOUTH,
                new CastSpec.Spread(2, 12.5));

        assertEquals(2, directions.size());
        assertEquals(12.5, Math.toDegrees(Math.acos(directions.get(1).dot(SOUTH))), 1e-9);
    }
}
