package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The dev trace's lines, pinned exactly (PLAN-mining.md 5.2, 5.12). The break line carries each
 * spot's roll and the bonus, so its {@code count} is checkable by addition on the line itself, and
 * {@code kept + dropped = count} for a full inventory.
 *
 * <p>S2 adds: the patch's rolled {@code size=} on the show, break and regrow lines; the click line's
 * {@code miss_sound=} field (the seat's ruling 9: readable without an ear); the hold line's
 * {@code sound=} field (ruling 8); the {@code click-outside} line (ruling 4); and the {@code roll}
 * and {@code roll-refused} lines (ruling 7).
 */
class MiningTraceLineTest {

    @Test
    void aClickLine() {
        assertEquals("MINING click player=Ben slot=s1 outcome=HIT spot=1/3 cooldown=1.0000 aim=10.50,12.25 tick=117 miss_sound=-",
                MiningTraceLine.click("Ben", "s1", "HIT", 0, 3, 1.0f,
                        Optional.of(new PatchPoint(10.5, 12.25)), 117, MiningTraceLine.NO_MISS));
    }

    @Test
    void aMissLineSaysWhetherTheMissSoundPlayed() {
        assertEquals("MINING click player=Ben slot=s1 outcome=MISS spot=1/3 cooldown=1.0000 aim=3.00,4.00 tick=200 miss_sound=yes",
                MiningTraceLine.click("Ben", "s1", "MISS", 0, 3, 1.0f,
                        Optional.of(new PatchPoint(3, 4)), 200, MiningTraceLine.missSound(true)));
        assertEquals("MINING click player=Ben slot=s1 outcome=MISS spot=1/3 cooldown=0.4500 aim=3.00,4.00 tick=207 miss_sound=no",
                MiningTraceLine.click("Ben", "s1", "MISS", 0, 3, 0.45f,
                        Optional.of(new PatchPoint(3, 4)), 207, MiningTraceLine.missSound(false)));
        // Mutation: missSound(true) -> "no" -> reddens.
    }

    @Test
    void absentFieldsAreDashes() {
        assertEquals("MINING click player=Ben slot=s1 outcome=NOT_LIVE spot=- cooldown=0.0300 aim=- tick=9 miss_sound=-",
                MiningTraceLine.click("Ben", "s1", "NOT_LIVE", -1, 0, 0.03f, Optional.empty(), 9, MiningTraceLine.NO_MISS));
    }

    @Test
    void anOutsideClickLineNamesTheBlockAndThePatchSize() {
        assertEquals("MINING click-outside player=Ben slot=s1 size=small block=12,64,20 tick=310",
                MiningTraceLine.clickOutside("Ben", "s1", "small", new BlockPos(12, 64, 20), 310));
    }

    @Test
    void aHoldLineCarriesItsStartAndEndTickAndWhetherTheDeniedSoundPlayed() {
        assertEquals("MINING hold player=Ben reason=denied start=4410 until=4440 sound=yes",
                MiningTraceLine.hold("Ben", "denied", 4410, 4440, true));
        assertEquals("MINING hold player=Ben reason=denied start=4421 until=4451 sound=no",
                MiningTraceLine.hold("Ben", "denied", 4421, 4451, false));
    }

    @Test
    void aPlaceRefusalLineCarriesTheExactChatText() {
        assertEquals("MINING place-refused player=Ben reason=A patch goes on a wall (north, south, east or west face), not up.",
                MiningTraceLine.placeRefused("Ben", "A patch goes on a wall (north, south, east or west face), not up."));
    }

    @Test
    void aRollLineAndARollRefusal() {
        assertEquals("MINING roll player=Ben slot=s1 size=large spots=7",
                MiningTraceLine.roll("Ben", "s1", "large", 7));
        assertEquals("MINING roll-refused player=Ben reason=Ore 'iron' does not list size 'medium'.",
                MiningTraceLine.rollRefused("Ben", "Ore 'iron' does not list size 'medium'."));
    }

    @Test
    void aBreakLine() {
        assertEquals("MINING break player=Ben slot=s1 size=small payout=raw_ironx14 rolls=2+3+4 bonus=5 kept=10 dropped=4 regrow_at=1363",
                MiningTraceLine.breakLine("Ben", "s1", "small", "raw_iron",
                        new Payout(List.of(2, 3, 4), 5), 10, 4, 1363));
    }

    @Test
    void aShowLineListsTheSizeEverySpotCentreAndTheLiveIndex() {
        assertEquals("MINING show player=Ben slot=s1 size=medium spots=10,12;30,20;41,7 live=2/3 tick=99",
                MiningTraceLine.show("Ben", "s1", "medium",
                        List.of(new Spot(10, 12), new Spot(30, 20), new Spot(41, 7)), 1, 99));
    }

    @Test
    void aRegrowLine() {
        assertEquals("MINING regrow player=Ben slot=s1 size=large spots=6 tick=1363",
                MiningTraceLine.regrow("Ben", "s1", "large", 6, 1363));
    }

    @Test
    void theDecimalPointIsADotWhateverTheDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("MINING click player=Ben slot=s1 outcome=MISS spot=1/2 cooldown=0.5000 aim=1.00,2.00 tick=1 miss_sound=no",
                    MiningTraceLine.click("Ben", "s1", "MISS", 0, 2, 0.5f,
                            Optional.of(new PatchPoint(1, 2)), 1, MiningTraceLine.missSound(false)));
        } finally {
            Locale.setDefault(saved);
        }
        // Mutation: drop Locale.ROOT -> "0,5000" -> reddens.
    }
}
