package io.github.butterflysmp.rpg.core.mining.spike;

import io.github.butterflysmp.rpg.core.mining.spike.MineSpikeLine.Src;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins every MINESPIKE format byte for byte. GATE-mining-spike.md's rows quote these shapes, so a
 * drift here is a drift in what the gate can read. No two fields in a row share a value, so a
 * transposed argument has nowhere to hide.
 */
class MineSpikeLineTest {

    @Test
    void thePacketLineIsExact() {
        // Mutation it forces red: swapping seq and tick, or printing cooldown with 3 places.
        assertEquals("MINESPIKE seq=7 tick=1234 src=PACKET type=SWING cooldown=0.1000 entity=- aim=-",
                new MineSpikeLine(7, 1234, Src.PACKET, "SWING", 0.1f, null, null).line());
    }

    @Test
    void theEventLineCarriesEntityAndAim() {
        assertEquals("MINESPIKE seq=8 tick=1235 src=EVENT type=UNKNOWN_ENTITY cooldown=1.0000"
                        + " entity=1500000000:box aim=+0.75,-1.25,in",
                new MineSpikeLine(8, 1235, Src.EVENT, "UNKNOWN_ENTITY", 1.0f,
                        "1500000000:box", "+0.75,-1.25,in").line());
    }

    @Test
    void anAbsentCooldownAndEmptyFieldsPrintADash() {
        // Mutation it forces red: printing "null" or an empty field, which would break a split on spaces.
        assertEquals("MINESPIKE seq=9 tick=1236 src=EVENT type=PACK_ACCEPTED cooldown=- entity=- aim=-",
                new MineSpikeLine(9, 1236, Src.EVENT, "PACK_ACCEPTED", null, "", "").line());
    }

    @Test
    void theCooldownIsRoundedToFourPlacesNotTruncated() {
        // 0.99 as a float is 0.98999995..., and %.4f must round it to 0.9900.
        assertEquals("MINESPIKE seq=1 tick=2 src=EVENT type=ARM_SWING cooldown=0.9900 entity=- aim=-",
                new MineSpikeLine(1, 2, Src.EVENT, "ARM_SWING", 0.99f, null, null).line());
    }

    @Test
    void aLineWithoutSrcOrTypeIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new MineSpikeLine(1, 2, null, "SWING", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MineSpikeLine(1, 2, Src.PACKET, " ", null, null, null));
    }

    @Test
    void thePlaceLineIsExact() {
        assertEquals("MINESPIKE PLACE mode=box block=-12,64,30 face=SOUTH spot=-11.5313,64.4688,31.0010"
                        + " spotId=1500000000 boxId=1499999999",
                MineSpikeLine.place("box", -12, 64, 30, "SOUTH",
                        -11.53125, 64.46875, 31.001, 1_500_000_000, 1_499_999_999));
    }

    @Test
    void thePlaceLineWithNoBoxPrintsADash() {
        assertEquals("MINESPIKE PLACE mode=block block=1,2,3 face=UP spot=1.5313,3.0010,3.4688"
                        + " spotId=1500000000 boxId=-",
                MineSpikeLine.place("block", 1, 2, 3, "UP", 1.53125, 3.001, 3.46875, 1_500_000_000, null));
    }

    @Test
    void theStopLineIsExact() {
        assertEquals("MINESPIKE STOP mode=model reason=second_run", MineSpikeLine.stop("model", "second_run"));
    }

    @Test
    void thePackLineIsExact() {
        assertEquals("MINESPIKE PACK url=http://127.0.0.1:51234/mining-spike.zip"
                        + " sha1=0123456789abcdef0123456789abcdef01234567 bytes=2048",
                MineSpikeLine.pack("http://127.0.0.1:51234/mining-spike.zip",
                        "0123456789abcdef0123456789abcdef01234567", 2048));
    }

    @Test
    void thePickLinePrintsTheServersFloat() {
        // (float)(1.0 / 1.2 * 20.0) is 16.666666f; Float.toString prints exactly that, not 16.67.
        assertEquals("MINESPIKE PICK speed=1.20 modifier=-2.80 delay=16.666666",
                MineSpikeLine.pick(1.2, 1.2 - 4.0, (float) (1.0 / 1.2 * 20.0)));
    }
}
