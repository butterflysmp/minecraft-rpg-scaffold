package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The shot's log lines (dragon-mobs step R7a), each exact: a gate predicts them character for character. */
class MobShotLinesTest {

    private static final UUID MOB = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID SHOT = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID P = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
    private static final Vec3 ORIGIN = new Vec3(10, 2.1, 0);
    /** 3 and 4 off the origin on two axes: flown is exactly 5. */
    private static final Vec3 AT = new Vec3(7, 2.1, 4);

    @Test
    void theFiredLine() {
        assertEquals("SHOT fired mob=phalanx id=" + MOB + " gs=100 forced=true tick=500 shot=" + SHOT
                        + " target=player " + P + " from=10.000 2.100 0.000 aim=0.000 1.620 0.000 dealt=40.000",
                MobShotLines.fired("phalanx", MOB, 100, true, 500, SHOT, "player", P, ORIGIN, new Vec3(0, 1.62, 0), 40));
    }

    @Test
    void theEndingLinesCarryAgeAtAndTheStraightLineFromTheOrigin() {
        String head = "id=" + MOB + " shot=" + SHOT + " age=25 at=7.000 2.100 4.000 flown=5.000";
        assertEquals("SHOT hit mob=phalanx " + head + " victim=player " + P + " dealt=40.000",
                MobShotLines.hit("phalanx", MOB, SHOT, 25, AT, ORIGIN, "player", P, 40));
        assertEquals("SHOT destroyed mob=phalanx " + head + " by=vanilla cause=ENTITY_ATTACK",
                MobShotLines.destroyed("phalanx", MOB, SHOT, 25, AT, ORIGIN, "vanilla cause=ENTITY_ATTACK"));
        assertEquals("SHOT expired mob=phalanx " + head + " limit=range",
                MobShotLines.expired("phalanx", MOB, SHOT, 25, AT, ORIGIN, "range"));
        assertEquals("SHOT blocked mob=phalanx " + head + " block=stone",
                MobShotLines.blocked("phalanx", MOB, SHOT, 25, AT, ORIGIN, "stone"));
        assertEquals("SHOT ended mob=phalanx " + head + " reason=owner-gone",
                MobShotLines.ended("phalanx", MOB, SHOT, 25, AT, ORIGIN, "owner-gone"));
        assertEquals("SHOT refused mob=phalanx " + head + " cause=SUFFOCATION",
                MobShotLines.refused("phalanx", MOB, SHOT, 25, AT, ORIGIN, "SUFFOCATION"));
        // The probe carries the server's HITTABLE box and the visible SHAPE as numbers (the seat's ruling 2 on #201).
        io.github.butterflysmp.rpg.core.mob.BeamBox box = new io.github.butterflysmp.rpg.core.mob.BeamBox(
                new Vec3(6.6, 1.7, 3.6), new Vec3(7.4, 2.5, 4.4));
        io.github.butterflysmp.rpg.core.mob.BeamBox shape = new io.github.butterflysmp.rpg.core.mob.BeamBox(
                new Vec3(6.8, 1.9, 3.8), new Vec3(7.2, 2.3, 4.2));
        assertEquals("SHOT at mob=phalanx " + head + " v=-0.200 0.000 0.000 box=6.600 1.700 3.600..7.400 2.500 4.400"
                        + " shape=6.800 1.900 3.800..7.200 2.300 4.200 inside=true",
                MobShotLines.probe("phalanx", MOB, SHOT, 25, AT, ORIGIN, new Vec3(-0.2, 0, 0), box, shape));
    }

    @Test
    void theOtherLines() {
        assertEquals("SHOT none mob=phalanx id=" + MOB + " tick=7 range=30.0", MobShotLines.none("phalanx", MOB, 7, 30));
        assertEquals("SHOT retarget mob=phalanx id=" + MOB + " shot=" + SHOT + " age=3 target=none",
                MobShotLines.retarget("phalanx", MOB, SHOT, 3, null, null));
        assertEquals("SHOT orphan removed type=bat uuid=" + SHOT, MobShotLines.orphan("bat", SHOT));
        assertEquals("BOW suppressed mob=phalanx id=" + MOB + " tick=9", MobShotLines.bowSuppressed("phalanx", MOB, 9));
    }
}
