package io.github.butterflysmp.rpg.core.progression;

import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The progression readout's text, pinned as literals.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class PlayerLevelLinesTest {

    private static final long LEVEL_13 = 19_980L;
    private static final long LEVEL_99 = 11_642_250L;

    @Test
    void theLABELSPadThroughTheSTATSHEETSOwnPadder_soThereIsOneColumnAndOneOwner() {
        // THESE LINES SIT DIRECTLY ABOVE THE EIGHT STAT LINES on the hub's head, so they share a
        // column. This class deliberately has NO padder of its own -- the width cannot drift
        // because there is only one of it, which beats a test asserting two copies agree.
        assertEquals("Level        ", StatsSheetLines.label(PlayerLevelLines.LEVEL_LABEL));
        assertEquals("Lifetime XP  ", StatsSheetLines.label(PlayerLevelLines.LIFETIME_LABEL));
        assertEquals("To Next      ", StatsSheetLines.label(PlayerLevelLines.TO_NEXT_LABEL));
        for (String raw : new String[] { PlayerLevelLines.LEVEL_LABEL,
                PlayerLevelLines.LIFETIME_LABEL, PlayerLevelLines.TO_NEXT_LABEL }) {
            assertEquals(13, StatsSheetLines.label(raw).length(),
                    "every label occupies the same column as the combat stats");
            assertTrue(raw.length() < 13,
                    "a label at or past the width is returned UNPADDED and silently breaks the "
                            + "column -- '" + raw + "' must stay shorter than it");
        }
    }

    @Test
    void amountsCarryThousandsSeparators_inLocaleROOTAndNotTheMachines() {
        // *** LOCALE.ROOT IS LOAD-BEARING. *** The default locale belongs to the SERVER'S MACHINE,
        // so a German-locale JVM renders this as 11.642.250 -- the same build printing different
        // numbers on two servers, and a gate reading nobody can reproduce.
        assertEquals("11,642,250", PlayerLevelLines.amount(LEVEL_99), "the cap total");
        assertEquals("19,980", PlayerLevelLines.amount(LEVEL_13), "the grindstone's gate");
        assertEquals("1,000", PlayerLevelLines.amount(1_000L), "exactly four digits gets a comma");
        assertEquals("999", PlayerLevelLines.amount(999L), "three does not");
        assertEquals("0", PlayerLevelLines.amount(0L));
        assertTrue(PlayerLevelLines.amount(LEVEL_99).contains(","),
                "if this ever reads 11642250 the locale argument has been lost");
        // Mutation MUTLOCALE-DEFAULT: Locale.ROOT -> Locale.getDefault() -> kill set RECORDED in
        // the PR body. NOTE it can only bite on a machine whose default groups differently.
    }

    @Test
    void theLEVELLineCarriesTheMAXMarkerONLYAtTheCap() {
        assertEquals("1", PlayerLevelLines.level(0L), "a new player");
        assertEquals("13", PlayerLevelLines.level(LEVEL_13), "the grindstone's gate");
        assertEquals("98", PlayerLevelLines.level(LEVEL_99 - 1L), "ONE XP SHORT is not maxed");
        assertEquals("99 (MAX)", PlayerLevelLines.level(LEVEL_99), "exactly at the cap");
        assertEquals("99 (MAX)", PlayerLevelLines.level(LEVEL_99 * 1000L),
                "and far past it -- lifetime XP does not stop at 99");
        // Mutation MUTMAX-ALWAYS: drop the isMaxed branch -> kill set RECORDED in the PR body.
    }

    @Test
    void lifetimeNeverRendersANegative_becauseThisReadsOffDisk() {
        assertEquals("19,980", PlayerLevelLines.lifetime(LEVEL_13));
        assertEquals("0", PlayerLevelLines.lifetime(0L));
        assertEquals("0", PlayerLevelLines.lifetime(-5_000L),
                "a hand-edited profile must not render a minus sign at a player");
        assertEquals("0", PlayerLevelLines.lifetime(Long.MIN_VALUE));
    }

    @Test
    void toNextTHROWSAtTheCapRatherThanRenderingAPlaceholder() {
        // *** THE PREDECESSOR RETURNED Long.MAX_VALUE AND IT RENDERED. *** The tempting fix is a
        // word in the same column -- "To Next  MAX" -- which is the same defect in better clothes:
        // the column's subject is an AMOUNT REMAINING and there is none. Loud, so a caller cannot
        // print the line by accident; the caller asks PlayerLevel.isMaxed and omits it.
        var error = assertThrows(IllegalStateException.class, () -> PlayerLevelLines.toNext(LEVEL_99));
        assertTrue(error.getMessage().contains("isMaxed"),
                "the message names the question the caller should have asked");
        assertThrows(IllegalStateException.class, () -> PlayerLevelLines.toNext(Long.MAX_VALUE));

        // AND BELOW THE CAP IT IS A REAL, SHRINKING NUMBER.
        assertEquals("1,000", PlayerLevelLines.toNext(0L), "a new player needs the first rung");
        assertEquals("1", PlayerLevelLines.toNext(999L), "one XP short of level 2");
        assertEquals("2,770", PlayerLevelLines.toNext(LEVEL_13),
                "rung 13 is 2,770 -- the whole of it remains when you land exactly on 13. "
                        + "(3,020 is rung 14; this literal was typed from memory and the row "
                        + "caught it, which is why these are literals and not derived)");
    }
}
