package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE GUARD {@code CastSpec.Homing} OWED, AND EVERY ROW HERE CAUSES ITS CONDITION.
 *
 * <p>The record's javadoc used to say the guard would be a <b>dead catch</b>: no content could
 * author a homing block, so a compact constructor could only ever be exercised by a test asserting
 * the arm was present. <b>That stopped being true when {@code AbilitySchema} learned to read
 * {@code homing:}</b> -- every value below is now authorable by anyone editing a yml.
 *
 * <p><b>SO NOT ONE ROW HERE ASSERTS AN ARM EXISTS.</b> Each constructs the bad value and reads the
 * throw, which is {@code CLAUDE.md}'s rule for a guard whose triggering case does not occur in
 * shipped content: <i>write the test that makes the guard fire, and watch it fire.</i> The arms are
 * unreachable from bundled content -- shipped weapons author no homing block, or three sane numbers
 * -- so <b>this file is their only exercise</b>, and the record's javadoc says so.
 *
 * <h2>THE THREE ACCEPTED ROWS ARE CONTROLS, NOT DECORATION</h2>
 *
 * <p>A validator that refuses everything passes every refusal row on this page. The shipped triple
 * {@code (0.65, 15, 10)} and both BOUNDARIES -- {@code lerp == 1} and {@code activation == 0} --
 * are constructed and kept, so a guard that widened by one comparison goes red here rather than
 * silently taking the Plume's own numbers out of the language.
 *
 * <h2>EVERY REFUSAL IS OF A VALUE THAT WOULD HAVE RESOLVED AND DONE NOTHING</h2>
 *
 * <p>None of these produce an exception downstream, a wrong trajectory or a visible defect. They
 * produce a homing block that parses, loads, renders and <b>homes at nothing</b> -- the failure
 * {@code CLAUDE.md} names as a mechanism advertised and never received, with no test going red.
 * That is why the guard is at the door rather than in the flight loop.
 *
 * <h2>THE MEASURED KILL MATRIX FOR THIS SLICE, ACROSS BOTH MODULES</h2>
 *
 * <p>Five mutations were run, each verified applied by a marker grep in <b>both</b> directions
 * (marker present, original gone) and each restored to a byte-identical file checked with
 * {@code cmp}. <b>Coverage is a property of what a mutation KILLS, not of what a test is named
 * after</b>, so the map is written down rather than assumed:
 *
 * <pre>
 * MUT-LERP   the lerp guard      -> 4 rows here            0 loader rows
 * MUT-RAD    the radius guard    -> 3 rows here            0 loader rows
 * MUT-ACT    the activation guard-> 2 rows here            1 loader row
 * MUT-MAP    parseHoming call    -> 0 rows here            3 loader rows
 * MUT-REQ    the presence check  -> 0 rows here            1 loader row
 * </pre>
 *
 * <p><b>MUT-ACT is the only mutation that crosses the module boundary, and that is what it is
 * for.</b> A guard in {@code core} that the loader's {@code catch(RuntimeException)} swallowed
 * silently would be indistinguishable from one that works. That one loader row is the sole evidence
 * these throws actually come back out as a NAMED, SKIPPED FILE rather than vanishing.
 *
 * <p><b>AND ONE ROW OVER IN {@code AbilityLoaderTest} HAS NO KILLER AT ALL:</b>
 * {@code aProjectileWITHOUTAHomingBlockGetsNullAndFliesWhereItWasAimed}. It cannot be reddened by
 * any mutation above -- it asserts null, and every mutation makes things MORE null. <b>It is not
 * thereby removable:</b> it is the row that records that this slice changed nothing for the five
 * shipped projectiles that author no homing block, which is a claim about the slice rather than a
 * guard on the code. A sweep pruning rows by unique kills would delete exactly it.
 */
class CastSpecHomingTest {

    /** {@code PLAN-dragons-plume.md} §5's inherited triple -- the numbers that actually ship. */
    @Test
    void theShippedTripleIsAccepted() {
        CastSpec.Homing homing = new CastSpec.Homing(0.65, 15.0, 10.0);

        assertEquals(0.65, homing.lerp());
        assertEquals(15.0, homing.activationBlocks());
        assertEquals(10.0, homing.searchRadius());
    }

    /**
     * {@code lerp == 1} is the hardest legal turn: the direction snaps straight onto the target
     * every tick. Extreme, well defined, and NOT refused -- the guard's job is meaninglessness, not
     * taste.
     */
    @Test
    void aLerpOfExactlyOneIsAcceptedBecauseItMEANSSomething() {
        assertEquals(1.0, new CastSpec.Homing(1.0, 15.0, 10.0).lerp());
    }

    /**
     * {@code activation == 0} means steer from the spawn point, with no ballistic run-up. Also a
     * real weapon, and the boundary the {@code >= 0} comparison sits on.
     */
    @Test
    void aZeroActivationIsAcceptedBecauseSteeringImmediatelyIsAWeapon() {
        assertEquals(0.0, new CastSpec.Homing(0.65, 0.0, 10.0).activationBlocks());
    }

    /** At zero the blend keeps 100% of the current direction: authored homing, no turn, forever. */
    @Test
    void aZeroLerpIsRefused() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Homing(0.0, 15.0, 10.0));

        assertTrue(thrown.getMessage().contains("lerp"), thrown.getMessage());
    }

    /**
     * A NEGATIVE lerp does not merely fail to steer -- it blends toward the REVERSE of the target
     * direction, so the bolt turns away from what it is chasing.
     */
    @Test
    void aNegativeLerpIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Homing(-0.65, 15.0, 10.0));
    }

    /**
     * Above one the blend passes THROUGH the target direction and out the far side:
     * {@code normalize().scale(1 - lerp)} goes negative, and the bolt mirrors rather than converges.
     */
    @Test
    void aLerpAboveOneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Homing(1.5, 15.0, 10.0));
    }

    /**
     * THE ONE THAT IS NOT OBVIOUS FROM THE FIELD NAME, AND THE REASON THE GUARD EXISTS RATHER THAN
     * A COMMENT.
     *
     * <p>{@code ProjectileFlight.steer} gates activation with
     * {@code from.distanceSquared(spawn) < activation * activation}. <b>The sign is squared away</b>,
     * so {@code -15} is not "steer immediately" and is not an error either -- it behaves EXACTLY
     * like {@code 15}. A number that silently means its own opposite is the shape this project
     * refuses at the door, and the message says so rather than leaving the next author to find it in
     * the flight loop.
     */
    @Test
    void aNegativeActivationIsRefusedBECAUSETheSignIsSquaredAway() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Homing(0.65, -15.0, 10.0));

        assertTrue(thrown.getMessage().contains("activation_blocks"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("squared"), thrown.getMessage());
    }

    /** {@code combatantsNear(pos, 0)} finds nothing, so the bolt never acquires a target. */
    @Test
    void aZeroSearchRadiusIsRefused() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Homing(0.65, 15.0, 0.0));

        assertTrue(thrown.getMessage().contains("search_radius"), thrown.getMessage());
    }

    @Test
    void aNegativeSearchRadiusIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Homing(0.65, 15.0, -10.0));
    }

    /**
     * NaN FALLS OUT OF THE POSITIVE FORM FOR FREE, AND THAT IS WHY THE GUARDS ARE WRITTEN
     * {@code !(x > 0)} RATHER THAN {@code x <= 0}.
     *
     * <p>Every comparison against NaN is false, so {@code !(lerp > 0 && lerp <= 1)} is TRUE for NaN
     * and the value is named. Written the other way round -- {@code if (lerp <= 0) throw} -- NaN
     * would pass the guard and fly a bolt whose direction is undefined from the first steer.
     *
     * <p>Reachable from a file: Bukkit's {@code getDouble} on a non-numeric scalar answers the
     * default, but a YAML {@code .nan} is a real double and arrives here intact.
     */
    @Test
    void naNIsRefusedByTheFormOfTheComparison() {
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Homing(Double.NaN, 15.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Homing(0.65, Double.NaN, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Homing(0.65, 15.0, Double.NaN));
    }
}
