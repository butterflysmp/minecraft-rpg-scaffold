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

    /** TELEPORT_DRAW (dragon-mobs step R6): '<tag> <right|left> <degrees>', and its line. */
    @Test
    void theTeleportDrawStepsGrammarAndLine() {
        var d = SelfTestBeam.parseDraw("tp1 right 50").orElseThrow();
        assertEquals("tp1", d.tag());
        assertEquals(new io.github.butterflysmp.rpg.core.mob.BeamTeleport.Draw(
                io.github.butterflysmp.rpg.core.mob.BeamTeleport.Side.RIGHT, 50.0), d.draw());
        assertEquals(io.github.butterflysmp.rpg.core.mob.BeamTeleport.Side.LEFT,
                SelfTestBeam.parseDraw("tp2 left 30").orElseThrow().draw().side());
        for (String bad : new String[] {"", "tp1", "tp1 right", "tp1 up 50", "tp1 right fifty", "tp1 right NaN",
                "tp1 right 50 extra"}) {
            assertTrue(SelfTestBeam.parseDraw(bad).isEmpty(), bad);
            assertTrue(SelfTestStaging.refusal(Scenario.Verb.TELEPORT_DRAW, bad).isPresent(), bad);
        }
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.TELEPORT_DRAW, "tp1 right 50"));
        assertEquals("DRIVE teleport_draw tag=tp1 " + K + " side=right first=50.000", SelfTestBeam.drawLine("tp1", K, d.draw()));
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
