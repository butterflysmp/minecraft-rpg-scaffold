package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobBeam;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The beam's log lines (PLAN-dragon-mobs.md step R5, the brief's ruling 7): one at the telegraph and one at the fire,
 * plus the report on a hit, the cancel when the Knell is gone mid-telegraph, and a forced attempt that found no one.
 * GATE-dragon-beam.md reads the beam from exactly these texts, so each is pinned whole.
 */
class MobBeamLinesTest {

    private static final UUID K = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID P = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID H = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void theTelegraphLineNamesTheKnellItsScoreTheTargetAndTheLockedLine() {
        assertEquals("BEAM telegraph mob=knell id=" + K + " gs=100 forced=true tick=5000 lived=37"
                        + " at=8.500 64.000 0.500 target=player " + P
                        + " from=8.500 67.360 0.500 to=0.500 65.080 0.500 dir=-0.961692 -0.274082 0.000000"
                        + " range=40.00 ticks=12",
                MobBeamLines.telegraph("knell", K, 100, true, 5000, 37, new Vec3(8.5, 64, 0.5), "player", P,
                        new Vec3(8.5, 67.36, 0.5), new Vec3(0.5, 65.08, 0.5),
                        new Vec3(-0.961692, -0.274082, 0.0), 40.0, 12));
    }

    @Test
    void theFireLineListsEachTestedCreatureWithItsGapAndOutcome_andTheDamageDealt() {
        List<MobBeam.Tested> tested = List.of(new MobBeam.Tested(P, 0.0, MobBeam.Outcome.HIT),
                new MobBeam.Tested(H, 0.1436, MobBeam.Outcome.SPARED));
        assertEquals("BEAM fire mob=knell id=" + K + " gs=100 base=35.000 tick=5012 at=8.500 64.000 0.500"
                        + " from=8.500 67.360 0.500 dir=-0.961692 -0.274082 0.000000 length=40.000 clip=range"
                        + " tolerance=1.00 tested=2 [player " + P + " gap=0.000 hit, husk " + H
                        + " gap=0.144 spared] dealt=35.000",
                MobBeamLines.fire("knell", K, 100, 35.0, 5012, new Vec3(8.5, 64, 0.5), new Vec3(8.5, 67.36, 0.5),
                        new Vec3(-0.961692, -0.274082, 0.0), 40.0, false, 1.0, tested,
                        List.of("player", "husk"), true));
    }

    @Test
    void aMissDealsNoneAndAGoneTargetPrintsNoGap() {
        List<MobBeam.Tested> tested = List.of(new MobBeam.Tested(P, Double.NaN, MobBeam.Outcome.GONE));
        assertEquals("BEAM fire mob=knell id=" + K + " gs=300 base=105.000 tick=7 at=0.000 0.000 0.000"
                        + " from=0.000 3.360 0.000 dir=1.000000 0.000000 0.000000 length=3.639 clip=block"
                        + " tolerance=1.00 tested=1 [player " + P + " gap=none gone] dealt=none",
                MobBeamLines.fire("knell", K, 300, 105.0, 7, Vec3.ZERO, new Vec3(0, 3.36, 0), new Vec3(1, 0, 0),
                        3.639, true, 1.0, tested, List.of("player"), false));
    }

    @Test
    void theReportLineSaysWhoLandedOnWhomWhenAndAlongWhatLine() {
        assertEquals("BEAM report mob=knell id=" + K + " target=player " + P + " tick=5012"
                        + " from=8.500 67.360 0.500 to=0.554 65.095 0.500",
                MobBeamLines.report("knell", K, "player", P, 5012, new Vec3(8.5, 67.36, 0.5),
                        new Vec3(0.5537, 65.0954, 0.5)));
    }

    @Test
    void theCancelAndTheForcedNoneLines() {
        assertEquals("BEAM cancelled mob=knell id=" + K + " elapsed=4/12 reason=gone",
                MobBeamLines.cancelled("knell", K, 4, 12));
        assertEquals("BEAM none mob=knell id=" + K + " forced=true tick=99 range=40.00 reason=no-target",
                MobBeamLines.none("knell", K, 99, 40.0));
    }
}
