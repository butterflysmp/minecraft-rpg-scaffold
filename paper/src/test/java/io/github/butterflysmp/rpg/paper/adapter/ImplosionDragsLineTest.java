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

    /** With the instrument on, the summary also names the fewest and most move packets any tick of it saw. */
    @Test
    void aTracedSummaryAlsoCarriesTheSmallestAndLargestMoves() {
        var s = new ImplosionDrag.Summary(BEN, 3.0, 0.5003, 20, 16, ImplosionDrag.Outcome.HELD);
        assertEquals("DRAG source=phage id=" + PHAGE + " victim=player " + BEN
                        + " start=3.000 end=0.500 ticks=20 sent=16 outcome=held movesMin=0 movesMax=3",
                ImplosionDrags.line("phage", PHAGE, "player", s, 0, 3));
    }

    /**
     * THE DRAGTICK LINE (the seat's Part D): every field distinct, so a transposition is red. The add is x then z; the
     * cap the add's bound; the keys the ones held, in the brief's order.
     */
    @Test
    void theDragTickLineCarriesEveryFieldInTheBriefsOrder() {
        assertEquals("DRAGTICK drag=" + PHAGE + " t=7 victim=" + BEN + " at=-104.123 63.000 7.456 d=2.345 moves=2"
                        + " add=0.081234 -0.004321 cap=0.074944 ping=13 keys=back,sprint",
                ImplosionDrags.tickLine(PHAGE, 7, BEN, -104.1234, 63.0, 7.4561, 2.3454, "2",
                        new io.github.butterflysmp.rpg.core.Vec3(0.0812341, 0.0, -0.0043211), 0.0749441, 13,
                        "back,sprint"));
    }

    /** Not pulled (tick 0, or outside the reach): no add and no cap; nothing added: a cap and no add. */
    @Test
    void aTickWithNoAddPrintsDashes() {
        assertEquals("DRAGTICK drag=" + PHAGE + " t=0 victim=" + BEN + " at=1.000 2.000 3.000 d=4.000 moves=-"
                        + " add=- cap=- ping=0 keys=-",
                ImplosionDrags.tickLine(PHAGE, 0, BEN, 1, 2, 3, 4, "-", null, null, 0, "-"));
        assertEquals("DRAGTICK drag=" + PHAGE + " t=3 victim=" + BEN + " at=1.000 2.000 3.000 d=0.400 moves=1"
                        + " add=- cap=0.000000 ping=5 keys=forward",
                ImplosionDrags.tickLine(PHAGE, 3, BEN, 1, 2, 3, 0.4, "1", null, 0.0, 5, "forward"));
    }

    @Test
    void theKeysAreNamedInTheBriefsOrderAndADashWhenNoneIsHeld() {
        assertEquals("forward,back,left,right,jump,sneak,sprint",
                ImplosionDrags.keys(true, true, true, true, true, true, true));
        assertEquals("left,sneak", ImplosionDrags.keys(false, false, true, false, false, true, false));
        assertEquals("-", ImplosionDrags.keys(false, false, false, false, false, false, false));
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
