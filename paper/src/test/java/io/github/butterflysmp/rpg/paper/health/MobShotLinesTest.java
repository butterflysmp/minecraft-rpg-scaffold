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

    /** The blink's ONE line (step R7b): the trigger, where it stood, every attempt with its verdict, and the landing. */
    @Test
    void theBlinkLine() {
        io.github.butterflysmp.rpg.core.mob.MobBlink.Decision landed = new io.github.butterflysmp.rpg.core.mob.MobBlink.Decision(
                java.util.List.of(
                        new io.github.butterflysmp.rpg.core.mob.MobBlink.Verdict(new io.github.butterflysmp.rpg.core.mob.MobBlink.Draw(0, 10),
                                new Vec3(10, 64, 0), io.github.butterflysmp.rpg.core.mob.BeamTeleport.Outcome.NO_FOOTING,
                                java.util.Optional.empty(), 7, 0),
                        new io.github.butterflysmp.rpg.core.mob.MobBlink.Verdict(new io.github.butterflysmp.rpg.core.mob.MobBlink.Draw(90, 8),
                                new Vec3(0, 64, 8), io.github.butterflysmp.rpg.core.mob.BeamTeleport.Outcome.OK,
                                java.util.Optional.of(new Vec3(0, 65, 8)), 0, 0)),
                java.util.Optional.of(new Vec3(0, 65, 8)));
        assertEquals("BLINK mob=phalanx id=" + MOB + " tick=9 forced=false trigger=player " + P
                        + " from=0.000 64.000 0.000 tried=2 [0.000 10.000 NO_FOOTING blocked=7 ungrounded=0,"
                        + " 90.000 8.000 OK at=0.000 65.000 8.000] landed=0.000 65.000 8.000 anchor=64.000 cooldown=0",
                MobShotLines.blink("phalanx", MOB, 9, false, "player", P, new Vec3(0, 64, 0), 64.0, 0, landed));
        io.github.butterflysmp.rpg.core.mob.MobBlink.Decision stays = new io.github.butterflysmp.rpg.core.mob.MobBlink.Decision(
                java.util.List.of(new io.github.butterflysmp.rpg.core.mob.MobBlink.Verdict(new io.github.butterflysmp.rpg.core.mob.MobBlink.Draw(0, 6),
                        new Vec3(6, 64, 0), io.github.butterflysmp.rpg.core.mob.BeamTeleport.Outcome.UNLOADED,
                        java.util.Optional.empty(), 0, 0)),
                java.util.Optional.empty());
        assertEquals("BLINK mob=phalanx id=" + MOB + " tick=9 forced=true trigger=none from=0.000 64.000 0.000 tried=1"
                        + " [0.000 6.000 UNLOADED] landed=none anchor=63.000 cooldown=0",
                MobShotLines.blink("phalanx", MOB, 9, true, null, null, new Vec3(0, 64, 0), 63.0, 0, stays));
        // Step R8: a FORCED blink during a cooldown ignores it, and says so.
        assertEquals("BLINK mob=phalanx id=" + MOB + " tick=9 forced=true trigger=none from=0.000 64.000 0.000 tried=1"
                        + " [0.000 6.000 UNLOADED] landed=none anchor=64.000 cooldown=120 ignored",
                MobShotLines.blink("phalanx", MOB, 9, true, null, null, new Vec3(0, 64, 0), 64.0, 120, stays));
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
