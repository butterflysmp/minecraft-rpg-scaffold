package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Knell's beam, its numbers and its fire (PLAN-dragon-mobs.md step R5). Ben, 2026-11-17, to the old game's
 * numbers: "yes" (every 100-200 ticks, a 12-tick telegraph, 40 blocks, 35 damage). The tolerance (1.0) is cfde822's,
 * not asked; the gear-score pricing is the seat's provisional.
 *
 * <p>The fixture's numbers are NOT the shipped ones where a shipped one would collide with another quantity the row
 * reads (a damage of 35 at GS 200 is 70, a range of 40 is not a tolerance of 1): the shipped values are pinned in
 * paper's {@code MobLoaderTest} and {@code ShippedKnellBeamTest}.
 */
class MobBeamTest {

    private static final MobBeam BEAM = new MobBeam(100, 200, 12, List.of(0, 9), 40.0, 0.6, 1.0, 35.0);
    private static final String WORLD = "world";
    private static final UUID TARGET = UUID.randomUUID();
    private static final UUID HUSK = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();
    private static final UUID KNELL = UUID.randomUUID();

    /** A line along -x from (8, 3, 0), as a Knell 8 blocks east aims west. 40 blocks long. */
    private static final BeamLine LINE = new BeamLine(new Vec3(8, 3, 0), new Vec3(-1, 0, 0), 40);

    private static BeamBox bodyAt(double x, double z) {
        return BeamBox.standing(new Vec3(x, 2, z), 0.5, 2.0);   // spans y 2..4, so the line at y 3 runs through it
    }

    private static MobBeam.Candidate player(UUID id, BeamBox box) {
        return new MobBeam.Candidate(id, MobHostility.Side.PLAYER, false, WORLD, box);
    }

    private static MobBeam.Candidate mob(UUID id, BeamBox box) {
        return new MobBeam.Candidate(id, MobHostility.Side.MOB, false, WORLD, box);
    }

    // --- the damage --------------------------------------------------------------------------------------------------

    @Test
    void theDamageIsTheAuthoredAmountTimesGearScoreOverAHundred() {
        assertEquals(35.0, BEAM.price(true, 100), 1e-9, "GS 100: the authored 35");
        assertEquals(70.0, BEAM.price(true, 200), 1e-9, "GS 200: double");
        assertEquals(105.0, BEAM.price(true, 300), 1e-9, "GS 300 (the End): treble");
    }

    @Test
    void aPassiveMobsGearScoreIsIgnoredAsMobScalingIgnoresIt() {
        assertEquals(35.0, BEAM.price(false, 300), 1e-9);
    }

    @Test
    void theDamageNumberIsTheBeamsOwnNotAConstant() {
        MobBeam other = new MobBeam(100, 200, 12, List.of(0, 9), 40.0, 0.6, 1.0, 22.0);
        assertEquals(44.0, other.price(true, 200), 1e-9);
    }

    // --- the range and the aim point ------------------------------------------------------------------------------

    @Test
    void theRangeIsInclusiveStraightLineBlocksFromTheEye() {
        Vec3 eye = new Vec3(0, 64, 0);
        assertTrue(BEAM.inRange(eye, new Vec3(40, 64, 0)), "exactly 40 along x");
        assertTrue(BEAM.inRange(eye, new Vec3(24, 96, 0)), "exactly 40 on the diagonal (24, 32)");
        assertFalse(BEAM.inRange(eye, new Vec3(40 + 1.0 / (1 << 20), 64, 0)), "just beyond");
        assertFalse(BEAM.inRange(eye, new Vec3(24, 96, 1.0 / (1 << 10))), "the diagonal, just beyond");
    }

    @Test
    void theAimPointIsTheFractionOfTheBodysHeightAboveItsFeet() {
        assertEquals(new Vec3(1, 2 + 0.6 * 2.0, 3), BEAM.aimPoint(new Vec3(1, 2, 3), 2.0));
        MobBeam half = new MobBeam(100, 200, 12, List.of(0, 9), 40.0, 0.5, 1.0, 35.0);
        assertEquals(new Vec3(1, 3, 3), half.aimPoint(new Vec3(1, 2, 3), 2.0));
    }

    @Test
    void theFlashTicksAreTheAuthoredOnes() {
        assertTrue(BEAM.flashes(0));
        assertTrue(BEAM.flashes(9));
        assertFalse(BEAM.flashes(1));
        assertFalse(BEAM.flashes(11));
    }

    // --- the fire: players hit, mobs never ------------------------------------------------------------------------

    @Test
    void theLockedPlayerInTheLineIsHit() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of(player(TARGET, bodyAt(0, 0))));
        assertTrue(s.hit());
        assertEquals(List.of(new MobBeam.Tested(TARGET, 0.0, MobBeam.Outcome.HIT)), s.tested());
    }

    @Test
    void theLockedPlayerOutOfTheLineIsAMissWithItsGap() {
        // Moved 3 blocks in z: the near face is 2.75 off the line.
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of(player(TARGET, bodyAt(0, 3))));
        assertFalse(s.hit());
        assertEquals(1, s.tested().size());
        assertEquals(MobBeam.Outcome.MISS, s.tested().get(0).outcome());
        assertEquals(2.75, s.tested().get(0).gap(), 1e-9);
    }

    @Test
    void aMobInTheLineIsListedAsSparedAndNeverHit() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD,
                List.of(mob(HUSK, bodyAt(4, 0)), player(TARGET, bodyAt(0, 0))));
        assertTrue(s.hit(), "the husk does not stop the beam: only blocks do");
        assertEquals(List.of(new MobBeam.Tested(TARGET, 0.0, MobBeam.Outcome.HIT),
                new MobBeam.Tested(HUSK, 0.0, MobBeam.Outcome.SPARED)), s.tested(), "the target first, then the spared");
    }

    @Test
    void aMobAsTheLockedTargetIsNeverHit() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of(mob(TARGET, bodyAt(0, 0))));
        assertFalse(s.hit());
        assertEquals(MobBeam.Outcome.EXEMPT, s.tested().get(0).outcome());
    }

    @Test
    void aSecondPlayerInTheLineIsSparedBecauseOnlyTheLockedTargetIsHit() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD,
                List.of(player(OTHER, bodyAt(4, 0)), player(TARGET, bodyAt(0, 0))));
        assertEquals(MobBeam.Outcome.SPARED, s.tested().get(1).outcome());
    }

    @Test
    void aCreatureOutOfTheLineThatIsNotTheTargetIsNotListed() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD,
                List.of(mob(HUSK, bodyAt(4, 5)), player(TARGET, bodyAt(0, 0))));
        assertEquals(1, s.tested().size());
    }

    @Test
    void aTargetThatTurnedCreativeIsExempt() {
        var creative = new MobBeam.Candidate(TARGET, MobHostility.Side.PLAYER, true, WORLD, bodyAt(0, 0));
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of(creative));
        assertFalse(s.hit());
        assertEquals(MobBeam.Outcome.EXEMPT, s.tested().get(0).outcome());
    }

    @Test
    void aTargetNoLongerAmongTheCandidatesIsGone() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of());
        assertFalse(s.hit());
        assertEquals(List.of(new MobBeam.Tested(TARGET, Double.NaN, MobBeam.Outcome.GONE)), s.tested());
    }

    @Test
    void aTargetInAnotherWorldIsGoneAndAnotherWorldsMobIsNotListed() {
        var elsewhere = new MobBeam.Candidate(TARGET, MobHostility.Side.PLAYER, false, "world_nether", bodyAt(0, 0));
        var elsewhereMob = new MobBeam.Candidate(HUSK, MobHostility.Side.MOB, false, "world_nether", bodyAt(4, 0));
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of(elsewhere, elsewhereMob));
        assertEquals(List.of(new MobBeam.Tested(TARGET, Double.NaN, MobBeam.Outcome.GONE)), s.tested());
    }

    @Test
    void theToleranceIsTheBeamsOwn() {
        // 1.25 off in z: the near face 1.0 off. Met at tolerance 1.0, missed at 0.5.
        MobBeam narrow = new MobBeam(100, 200, 12, List.of(0, 9), 40.0, 0.6, 0.5, 35.0);
        List<MobBeam.Candidate> c = List.of(player(TARGET, bodyAt(0, 1.25)));
        assertTrue(BEAM.resolve(LINE, TARGET, WORLD, c).hit());
        assertFalse(narrow.resolve(LINE, TARGET, WORLD, c).hit());
    }

    // --- the report (E5) -----------------------------------------------------------------------------------------

    @Test
    void aHitRaisesTheReportFromTheEyeToTheNearestPointOfTheSegment() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of(player(TARGET, bodyAt(0, 0.5))));
        Optional<BeamHit> r = MobBeam.report(s, KNELL, "knell", TARGET, 4242L, LINE);
        assertTrue(r.isPresent());
        // The body's centre is (0, 3, 0.5); the segment's nearest point to it is (0, 3, 0).
        assertEquals(new BeamHit(KNELL, "knell", TARGET, 4242L, new Vec3(8, 3, 0), new Vec3(0, 3, 0)), r.get());
    }

    @Test
    void aMissRaisesNoReport() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD, List.of(player(TARGET, bodyAt(0, 3))));
        assertTrue(MobBeam.report(s, KNELL, "knell", TARGET, 1L, LINE).isEmpty());
    }

    @Test
    void aSparedMobRaisesNoReport() {
        MobBeam.Shot s = BEAM.resolve(LINE, TARGET, WORLD,
                List.of(mob(HUSK, bodyAt(4, 0)), player(TARGET, bodyAt(0, 3))));
        assertTrue(MobBeam.report(s, KNELL, "knell", TARGET, 1L, LINE).isEmpty());
    }

    // --- validation --------------------------------------------------------------------------------------------------

    @Test
    void eachBadNumberIsRefusedByName() {
        assertRefused("beam.cycle_ticks.min", () -> new MobBeam(0, 200, 12, List.of(0), 40, 0.6, 1, 35));
        assertRefused("beam.cycle_ticks.max", () -> new MobBeam(100, 99, 12, List.of(0), 40, 0.6, 1, 35));
        assertRefused("beam.telegraph_ticks", () -> new MobBeam(100, 200, 0, List.of(0), 40, 0.6, 1, 35));
        assertRefused("beam.flash_at", () -> new MobBeam(100, 200, 12, List.of(0, 12), 40, 0.6, 1, 35));
        assertRefused("beam.flash_at", () -> new MobBeam(100, 200, 12, List.of(-1), 40, 0.6, 1, 35));
        assertRefused("beam.range", () -> new MobBeam(100, 200, 12, List.of(0), 0, 0.6, 1, 35));
        assertRefused("beam.aim_height", () -> new MobBeam(100, 200, 12, List.of(0), 40, 1.5, 1, 35));
        assertRefused("beam.tolerance", () -> new MobBeam(100, 200, 12, List.of(0), 40, 0.6, -0.1, 35));
        assertRefused("beam.damage", () -> new MobBeam(100, 200, 12, List.of(0), 40, 0.6, 1, -1));
        assertRefused("beam.damage", () -> new MobBeam(100, 200, 12, List.of(0), 40, 0.6, 1, Double.NaN));
    }

    @Test
    void aMinEqualToTheMaxIsAFixedWaitAndIsAllowed() {
        new MobBeam(150, 150, 12, List.of(0), 40, 0.6, 1, 35);
    }

    private static void assertRefused(String name, Runnable r) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, r::run);
        assertTrue(e.getMessage().contains(name), e.getMessage());
    }
}
