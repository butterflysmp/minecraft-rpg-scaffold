package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.shape.ImplosionDrag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The DRAG line's exact text (2026-11-18): one per victim when a drag ends. A gate predicts it verbatim, so it is pinned
 * here. Every figure differs from every other, so a transposition between two fields is red. Pure: no server.
 */
class ImplosionDragsLineTest {

    private static final UUID PHAGE = UUID.fromString("4c30b659-a0dd-4c32-a998-e4e45e8c49c7");
    private static final UUID BEN = UUID.fromString("b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5");

    @Test
    void aVictimsLineCarriesItsStartItsEndItsTicksWhatWasSentAndHowItEnded() {
        var s = new ImplosionDrag.Summary(BEN, 3.0, 0.5003, 20, 16, ImplosionDrag.Outcome.HELD);
        assertEquals("DRAG source=phage id=" + PHAGE + " victim=player " + BEN
                        + " start=3.000 end=0.500 ticks=20 sent=16 outcome=held",
                ImplosionDrags.line("phage", PHAGE, "player", s));
    }

    @Test
    void eachOutcomeIsPrintedInLowerCase() {
        for (ImplosionDrag.Outcome o : ImplosionDrag.Outcome.values()) {
            var s = new ImplosionDrag.Summary(BEN, 4.0, 7.639, 13, 12, o);
            assertEquals("DRAG source=ability id=" + PHAGE + " victim=husk " + BEN
                            + " start=4.000 end=7.639 ticks=13 sent=12 outcome=" + o.name().toLowerCase(),
                    ImplosionDrags.line("ability", PHAGE, "husk", s));
        }
    }
}
