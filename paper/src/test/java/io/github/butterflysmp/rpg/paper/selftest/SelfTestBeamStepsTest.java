package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The selftest's BEAM and PROBE_AT steps (dragon-mobs step R5; GATE-dragon-beam.md): their texts and their grammar. */
class SelfTestBeamStepsTest {

    private static final UUID K = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Test
    void theLinesAreExact() {
        assertEquals("DRIVE beam tag=kb1 " + K, SelfTestBeam.driveLine("kb1", K));
        assertEquals("DRIVE beam tag=kb1 " + K + " refused=no beam loop", SelfTestBeam.refusedLine("kb1", K, "no beam loop"));
        assertEquals("DRIVE beam tag=kb1 none within 48 blocks", SelfTestBeam.noneLine("kb1", "DRIVE beam", 48));
        assertEquals("AT tag=kb2 " + K + " x=10.500 y=64.000 z=-0.125", SelfTestBeam.atLine("kb2", K, 10.5, 64, -0.125));
    }

    /** The beam step reaches past the beam's own 40 blocks, so a row can force a Knell that is out of range. */
    @Test
    void theBeamStepReachesBeyondTheBeamsRange() {
        assertEquals(48.0, SelfTestBeam.REACH, 0.0);
    }

    @Test
    void eachTakesExactlyOneTag() {
        for (Scenario.Verb v : new Scenario.Verb[] {Scenario.Verb.BEAM, Scenario.Verb.PROBE_AT}) {
            assertEquals(Optional.empty(), SelfTestStaging.refusal(v, "kb1"));
            assertTrue(SelfTestStaging.refusal(v, "").isPresent(), v + " with no tag");
            assertTrue(SelfTestStaging.refusal(v, "kb1 kb2").isPresent(), v + " with two");
        }
    }
}
