package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamBox;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE INSTRUMENT READS A REAL SWING (the seat's ruling 4 on #201): the distance from his eye to a shot's box ALONG HIS
 * LOOK, or a miss with the gap; and the grammar of the aimed strikes. Pure, so each line is exact.
 */
class SelfTestStrikeTest {

    private static final BeamBox BOX = new BeamBox(new Vec3(2, -0.4, -0.4), new Vec3(2.8, 0.4, 0.4));

    @Test
    void alongHisLookTheEntryDistanceIntoTheBox() {
        assertEquals(OptionalDouble.of(2.0), SelfTestStrike.along(Vec3.ZERO, new Vec3(1, 0, 0), 3.0, BOX));
    }

    @Test
    void aLookThatPassesTheBoxIsAMissAndItsGapIsTheGrowthThatWouldMeet() {
        Vec3 up = new Vec3(0, 0.6, 0);
        assertEquals(OptionalDouble.empty(), SelfTestStrike.along(up, new Vec3(1, 0, 0), 3.0, BOX));
        assertEquals(0.2, SelfTestStrike.gap(up, new Vec3(1, 0, 0), 3.0, BOX), 1e-9);
    }

    @Test
    void aBoxBeyondTheReachIsAMiss() {
        assertEquals(OptionalDouble.empty(), SelfTestStrike.along(Vec3.ZERO, new Vec3(1, 0, 0), 1.5, BOX));
    }

    @Test
    void theSwingLine() {
        UUID s = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        assertEquals("SWING kind=arm shot=" + s + " along=2.000 gap=0.000 eye=0.000 1.620 0.000 look=1.000 0.000 0.000",
                SelfTestStrike.swingLine("arm", s, OptionalDouble.of(2.0), 0.0, new Vec3(0, 1.62, 0), new Vec3(1, 0, 0)));
        assertEquals("SWING kind=attack shot=none", SelfTestStrike.swingLine("attack", null, OptionalDouble.empty(), 0,
                Vec3.ZERO, Vec3.ZERO));
        assertEquals("SWING kind=arm shot=" + s + " along=miss gap=0.200 eye=0.000 0.000 0.000 look=1.000 0.000 0.000",
                SelfTestStrike.swingLine("arm", s, OptionalDouble.empty(), 0.2, Vec3.ZERO, new Vec3(1, 0, 0)));
        assertEquals("SWING after shot=" + s + " live=false", SelfTestStrike.afterLine(s, false));
    }

    @Test
    void strikeAtAgeTakesATagAnAgeAndATimeout() {
        assertEquals(Optional.of(new SelfTestStrike.AtAge("sh15", 32, 80)), SelfTestStrike.parseAtAge("sh15 32 80"));
        for (String bad : new String[]{"sh15 32", "sh15 x 80", "sh15 32 0", " 32 80", "sh15 -1 80"}) {
            assertTrue(SelfTestStrike.parseAtAge(bad).isEmpty(), bad);
        }
    }
}
