package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamTeleport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The teleport's log lines (dragon-mobs step R6): the GATE reads the teleport from exactly these texts. */
class MobTeleportLinesTest {

    private static final UUID K = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID P = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static BeamTeleport.Candidate c(BeamTeleport.Side s, double a) {
        return new BeamTeleport.Candidate(s, a, new Vec3(0, 64, 0));
    }

    @Test
    void theDecisionLineListsEveryCandidateWithItsVerdictAndTheLanding() {
        Vec3 land = new Vec3(105.1423, 64, 206.1284);
        BeamTeleport.Decision d = new BeamTeleport.Decision(8.0, 0.0, BeamTeleport.Side.RIGHT, List.of(
                new BeamTeleport.Verdict(c(BeamTeleport.Side.RIGHT, 51.25), BeamTeleport.Outcome.NO_FOOTING,
                        Optional.empty(), 3, 4),
                new BeamTeleport.Verdict(c(BeamTeleport.Side.RIGHT, 30), BeamTeleport.Outcome.UNLOADED,
                        Optional.empty(), 0, 0),
                new BeamTeleport.Verdict(c(BeamTeleport.Side.RIGHT, 43.333333), BeamTeleport.Outcome.NO_SIGHT,
                        Optional.of(new Vec3(0, 62, 0)), 0, 0),
                new BeamTeleport.Verdict(c(BeamTeleport.Side.LEFT, 70), BeamTeleport.Outcome.OK, Optional.of(land), 0, 0)),
                Optional.of(land));
        assertEquals("TELEPORT mob=knell id=" + K + " target=player " + P + " tick=900 from=108.000 64.000 200.000"
                        + " center=100.000 64.000 200.000 radius=8.000 bearing=0.000 side=right first=51.250 tried=4"
                        + " [right 51.250 no-footing blocked=3 ungrounded=4, right 30.000 unloaded,"
                        + " right 43.333 no-sight y=62.000, left 70.000 ok y=64.000 at=105.142 64.000 206.128]"
                        + " landed=105.142 64.000 206.128 moved=true",
                MobTeleportLines.decision("knell", K, "player", P, 900, new Vec3(108, 64, 200), d,
                        new Vec3(100, 64, 200), 51.25, "true"));
    }

    @Test
    void noLandingPrintsNoneAndAMoveThatWasNeverAttempted() {
        BeamTeleport.Decision d = new BeamTeleport.Decision(8.0, 180.0, BeamTeleport.Side.LEFT, List.of(),
                Optional.empty());
        assertEquals("TELEPORT mob=knell id=" + K + " target=player " + P + " tick=1 from=0.000 0.000 0.000"
                        + " center=0.000 0.000 0.000 radius=8.000 bearing=180.000 side=left first=30.000 tried=0 []"
                        + " landed=none moved=-",
                MobTeleportLines.decision("knell", K, "player", P, 1, Vec3.ZERO, d, Vec3.ZERO, 30, "-"));
    }

    @Test
    void theSkipAndIgnoreLinesNameTheReason() {
        assertEquals("TELEPORT skipped mob=knell id=" + K + " target=" + P + " reason=knell-gone",
                MobTeleportLines.skipped("knell", K, P, BeamTeleport.Skip.KNELL_GONE));
        assertEquals("TELEPORT skipped mob=knell id=" + K + " target=" + P + " reason=target-exempt",
                MobTeleportLines.skipped("knell", K, P, BeamTeleport.Skip.TARGET_EXEMPT));
        assertEquals("TELEPORT ignored mob=knell id=" + K + " reason=pending", MobTeleportLines.ignored("knell", K));
    }
}
