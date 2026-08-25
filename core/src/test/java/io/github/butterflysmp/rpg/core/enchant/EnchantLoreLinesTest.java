package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The text of an enchant's tooltip line.
 *
 * Cosmetic, but not therefore unimportant: the roman numeral is the ONE thing a player reads to
 * know what level their enchant is, and boot gate step 3 checks it by eye. It is also exactly the
 * kind of thing that is cheap to pin here and expensive to check by booting a server and hovering
 * an item, which is the whole reason it lives in core rather than in the renderer.
 */
class EnchantLoreLinesTest {

    @Test
    void theShippedLevelsRenderAsRomanNumerals() {
        // I..III is the whole shipped range. Arabic here is the difference between a tooltip that
        // reads like Minecraft and one that reads like a debug line.
        assertEquals("I", EnchantLoreLines.romanNumeral(1));
        assertEquals("II", EnchantLoreLines.romanNumeral(2));
        assertEquals("III", EnchantLoreLines.romanNumeral(3));
        // Mutation: return String.valueOf(level) -> "3" -> reddens.
    }

    @Test
    void levelsBeyondTheShippedRangeStillHaveNumerals() {
        // MAX_LEVEL is 3 today; the table runs to X so raising it is a one-line change rather than
        // a rendering bug discovered in-game.
        assertEquals("IV", EnchantLoreLines.romanNumeral(4));
        assertEquals("IX", EnchantLoreLines.romanNumeral(9));
        assertEquals("X", EnchantLoreLines.romanNumeral(10));
    }

    @Test
    void aLevelPastTheTableFallsBackToArabicRatherThanThrowing() {
        // A cosmetic line must never be the reason an item cannot render. Same instinct as
        // WeaponLore.elementLine falling back to a title-cased id for an unknown element.
        assertEquals("11", EnchantLoreLines.romanNumeral(11));
        assertEquals("100", EnchantLoreLines.romanNumeral(100));
        // Mutation: index the array unguarded -> ArrayIndexOutOfBounds -> reddens.
    }

    @Test
    void alevelOfZeroOrLessHasNoNumeralAtAll() {
        // Level 0 is LOCKED and never reaches a renderer -- but if it ever did, "Unbreaking" is a
        // survivable wrong answer and "Unbreaking " with a trailing space is not.
        assertEquals("", EnchantLoreLines.romanNumeral(0));
        assertEquals("", EnchantLoreLines.romanNumeral(-1));
    }

    @Test
    void anActiveEnchantReadsAsItsNameAndNumeral() {
        // What boot gate step 3 reads off the screen.
        assertEquals("Unbreaking III", EnchantLoreLines.label("Unbreaking", 3, 3));
        assertEquals("Unbreaking I", EnchantLoreLines.label("Unbreaking", 1, 3));
        // Mutation: drop the space -> "UnbreakingIII" -> reddens.
    }

    @Test
    void aSingleLevelEnchantOmitsTheNumeralTheWayVanillaDoes() {
        // Vanilla renders Mending as "Mending", never "Mending I" -- a numeral on a single-level
        // enchant implies a II that does not exist. Driven by the enchant's declared max_level, not
        // by the level being 1, which is why the second assertion matters: an enchant that CAN
        // reach III still shows its level at I.
        assertEquals("Mending", EnchantLoreLines.label("Mending", 1, 1));
        assertEquals("Unbreaking I", EnchantLoreLines.label("Unbreaking", 1, 3));
        // Mutation: omit the numeral whenever level == 1 -> "Unbreaking" at level I -> reddens.
    }
}
