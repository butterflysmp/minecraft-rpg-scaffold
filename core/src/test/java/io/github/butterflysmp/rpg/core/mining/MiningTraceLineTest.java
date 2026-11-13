package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The dev trace's lines, pinned exactly (PLAN-mining.md 5.2). The break line carries each spot's
 * roll and the bonus, so its {@code count} is checkable by addition on the line itself, and
 * {@code kept + dropped = count} for a full inventory.
 */
class MiningTraceLineTest {

    @Test
    void aClickLine() {
        assertEquals("MINING click player=Ben slot=s1 outcome=HIT spot=1/3 cooldown=1.0000 aim=10.50,12.25 tick=117",
                MiningTraceLine.click("Ben", "s1", "HIT", 0, 3, 1.0f,
                        Optional.of(new PatchPoint(10.5, 12.25)), 117));
    }

    @Test
    void absentFieldsAreDashes() {
        assertEquals("MINING click player=Ben slot=s1 outcome=NOT_LIVE spot=- cooldown=0.0300 aim=- tick=9",
                MiningTraceLine.click("Ben", "s1", "NOT_LIVE", -1, 0, 0.03f, Optional.empty(), 9));
    }

    @Test
    void aBreakLine() {
        assertEquals("MINING break player=Ben slot=s1 payout=raw_ironx14 rolls=2+3+4 bonus=5 kept=10 dropped=4 regrow_at=1363",
                MiningTraceLine.breakLine("Ben", "s1", "raw_iron",
                        new Payout(List.of(2, 3, 4), 5), 10, 4, 1363));
    }

    @Test
    void aRegrowLine() {
        assertEquals("MINING regrow player=Ben slot=s1 spots=2 tick=1363",
                MiningTraceLine.regrow("Ben", "s1", 2, 1363));
    }

    @Test
    void theDecimalPointIsADotWhateverTheDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("MINING click player=Ben slot=s1 outcome=MISS spot=1/2 cooldown=0.5000 aim=1.00,2.00 tick=1",
                    MiningTraceLine.click("Ben", "s1", "MISS", 0, 2, 0.5f,
                            Optional.of(new PatchPoint(1, 2)), 1));
        } finally {
            Locale.setDefault(saved);
        }
        // Mutation: drop Locale.ROOT -> "0,5000" -> reddens.
    }
}
