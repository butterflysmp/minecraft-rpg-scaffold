package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plain-text armor tooltip lines.
 *
 * The headline is {@link #everySlotHasItsOwnFooterNoun} -- the switch has no default arm, so a fifth
 * slot is a compile error rather than a piece footered as something it is not.
 *
 * Note what is deliberately NOT tested here, because it deliberately does not happen: there is no
 * multiply-by-100. {@code ShieldLoreLines} needs a rounding pass because it turns an authored
 * FRACTION into a displayed PERCENT, and that multiply is where binary floating point stops agreeing
 * with the author (measured there: {@code 0.29 * 100 == 28.999999999999996}). Armor points are
 * already the stat's own unit, so the value passes through untouched and the only formatting is the
 * whole-number trim.
 *
 * Every expected string below was produced by EXECUTING the expression, not derived from reading the
 * code. Each test names the mutation it forces red.
 */
class ArmorLoreLinesTest {

    // --- The defense line -----------------------------------------------------------------------

    @Test
    void aWholeDefenseDropsItsTrailingZero() {
        // Content may legally author `defense: 3` or `defense: 3.0`; both are the same double and
        // both must print as "3". "Defense: 3.0" on a piece worth 3 is noise on every item.
        assertEquals("3", ArmorLoreLines.defenseValue(3));
        assertEquals("3", ArmorLoreLines.defenseValue(3.0));
        assertEquals("8", ArmorLoreLines.defenseValue(8));
        assertEquals("0", ArmorLoreLines.defenseValue(0));
        // Mutation: return String.valueOf(n) unconditionally -> "3.0" -> reddens.
    }

    @Test
    void aFractionalDefenseKeepsItsFractionRatherThanBeingRoundedAway() {
        // Nothing shipped authors one, but a future piece might, and silently truncating 2.5 to 2
        // would be a tooltip that disagrees with the stat by half a point.
        assertEquals("2.5", ArmorLoreLines.defenseValue(2.5));
        // Mutation: cast to (long) unconditionally -> "2" -> reddens.
    }

    @Test
    void theLabelAndValueConcatenateWithoutAStrayOrMissingSpace() {
        assertEquals("Defense: 8", ArmorLoreLines.defenseLabel(8));
        assertEquals("Defense: ", ArmorLoreLines.DEFENSE_LABEL);
        assertTrue(ArmorLoreLines.DEFENSE_LABEL.endsWith(" "),
                "the label carries its own trailing space so callers concatenate blindly");
        // Mutation: drop the trailing space from DEFENSE_LABEL -> "Defense:8" -> reddens.
    }

    @Test
    void theLabelSaysDefenseAndNotArmorSoItMatchesTheStatItReports() {
        // "Armor" is vanilla's word for the raw points, and vanilla's own mitigation from 20 points
        // is about 80%. This project's curve turns the same 20 into about 17%. A tooltip labelled
        // "Armor: 20" invites the player to read vanilla's number; "Defense: 20" points at the
        // stat and the action bar's field, which are what actually govern the hit.
        assertTrue(ArmorLoreLines.defenseLabel(20).startsWith("Defense"));
        assertFalse(ArmorLoreLines.defenseLabel(20).contains("Armor"));
        // Mutation: rename the label to "Armor: " -> reddens.
    }

    @Test
    void aFlatBonusValueLeadsWithItsSignAndDropsTheTrailingZero() {
        assertEquals("+30", ArmorLoreLines.bonusValue(30));
        assertEquals("+30", ArmorLoreLines.bonusValue(30.0));
        assertEquals("+10", ArmorLoreLines.bonusValue(10));
        assertEquals("+2.5", ArmorLoreLines.bonusValue(2.5), "a fractional bonus keeps its fraction");
        // Mutation: drop the "+" -> "30 Max Health" reads as a total rather than a bonus -> reddens.
    }

    @Test
    void everyStatLabelLeadsAndOnlyDefenseCarriesItsOwnSeparator() {
        // ALL stat lines read "Label: value" -- Defense, Health, and Mana when 2b adds it -- so the
        // block reads as one column. A bonus is told apart by the + on its VALUE, not by being
        // written backwards, which an earlier draft did ("+30 Max Health") and which reads as two
        // competing formats rather than as a distinction.
        assertEquals("Health", ArmorLoreLines.MAX_HEALTH_LABEL);
        assertFalse(ArmorLoreLines.MAX_HEALTH_LABEL.contains(":"),
                "the shared bonus helper supplies the separator, so the constants must not"
                        + " -- or Health and Mana could punctuate differently");

        // Defense carries its own ": " ONLY because its caller concatenates it directly rather than
        // going through the shared bonus helper. One place owns the punctuation for the rest.
        assertTrue(ArmorLoreLines.DEFENSE_LABEL.endsWith(": "));
        // Mutation: give MAX_HEALTH_LABEL a colon -> "Health:: +30" -> reddens.
    }

    // --- The footer noun ------------------------------------------------------------------------

    @Test
    void everySlotHasItsOwnFooterNoun() {
        assertEquals("Helmet", ArmorLoreLines.slotNoun(ArmorSlot.HEAD));
        assertEquals("Chestplate", ArmorLoreLines.slotNoun(ArmorSlot.CHEST));
        assertEquals("Leggings", ArmorLoreLines.slotNoun(ArmorSlot.LEGS));
        assertEquals("Boots", ArmorLoreLines.slotNoun(ArmorSlot.FEET));
        // Mutation: return the same noun for two arms -> reddens.
    }

    @Test
    void noTwoSlotsShareAFooterNounAndNoneIsBlank() {
        // A discovery-shaped check so a fifth slot is covered the day it is added, rather than
        // waiting for someone to remember to extend the four literals above. It asserts the walk is
        // non-empty first, for the reason CLAUDE.md records twice: a loop over nothing reads exactly
        // like a loop that passed.
        List<String> nouns = new ArrayList<>();
        for (ArmorSlot slot : ArmorSlot.values()) {
            String noun = ArmorLoreLines.slotNoun(slot);
            assertFalse(noun == null || noun.isBlank(), slot + " has no footer noun");
            assertFalse(nouns.contains(noun), slot + " reuses the noun '" + noun + "'");
            nouns.add(noun);
        }
        assertEquals(4, nouns.size(), "the walk must not be empty or short");
        // Mutation: point two arms at the same string -> reddens on the contains() check.
    }

    @Test
    void theFooterNounIsTheGenericSlotWordNotTheVanillaItemName() {
        // Leather's vanilla pieces are Cap, Tunic, Pants and Boots -- irregular, and authored per
        // piece as display_name. The FOOTER says what kind of gear the item is, the way a weapon's
        // reads "Rare Melee Weapon" rather than repeating its name. So a Leather Cap footers as
        // "Common Helmet", and that is correct rather than a mismatch to be fixed.
        assertEquals("Helmet", ArmorLoreLines.slotNoun(ArmorSlot.HEAD));
        assertFalse(ArmorLoreLines.slotNoun(ArmorSlot.HEAD).equals("Cap"));
        // Mutation: return "Cap" for HEAD to match leather -> every non-leather helmet footers
        // wrongly -> reddens.
    }
}
