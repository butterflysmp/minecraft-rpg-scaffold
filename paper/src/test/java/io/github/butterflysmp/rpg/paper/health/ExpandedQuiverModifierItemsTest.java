package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>THESE ROWS EXIST BECAUSE TWO MUTATIONS WERE RUN AND BOTH REDDENED NOTHING.</b>
 *
 * <p>Measured 2026-09-13, against the full 1592-row suite:
 *
 * <pre>
 *   MUT-MERGE    delete the putAll merging the two quiver scans   -> 0 failures
 *   MUT-PREFIX   alias this scanner's SOURCE_PREFIX to the
 *                instrument's, "quiversize:"                      -> 0 failures
 * </pre>
 *
 * <p>Both are the defects {@link QuiverSizeModifierItems}' own javadoc predicted this slice would
 * meet, and both were <i>asserted</i> in {@code ExpandedQuiverTest} without being <i>guarded</i>. The
 * gap is the same in each case: that file models the consequence against {@code Stat} directly, with
 * the key strings written out as literals, so it never reads either constant and never reaches the
 * wiring. <b>Understanding a trap is not evidence that anything catches it.</b>
 *
 * <p>These rows read the REAL constants and the REAL merge, which is the only reason a mutation to
 * either can reach them.
 *
 * <h2>WHY THIS CAN BE A UNIT TEST WHEN THE THING IT GUARDS LIVES IN PlayerHealthSystem</h2>
 *
 * <p>It cannot test {@code PlayerHealthSystem} -- that needs a live {@code Player} and a running
 * server. So the merge was EXTRACTED into
 * {@link ExpandedQuiverModifierItems#mergedSources(Map, Map)}, which takes two plain maps. That move
 * is what makes the behaviour reachable from here, and it also makes the original mutation
 * impossible: <b>dropping a source now means dropping an argument, which does not compile.</b>
 */
class ExpandedQuiverModifierItemsTest {

    /** The Boltor's authored magazine is 8; these are the bonuses, chosen pairwise distinct. */
    private static final double ENCHANT_ARROWS = 2.0;
    private static final double INSTRUMENT_ARROWS = 19.0;

    // --- MUT-PREFIX -------------------------------------------------------------------------------

    /**
     * THE TWO SCANNERS' KEY SPACES MUST BE DISJOINT, AND BOTH CAN KEY THE MAIN HAND.
     *
     * <p>{@link QuiverSizeModifierItems} walks ALL slots on BARE {@code EquipmentSlot} names, so it
     * produces {@code "HAND"}. This scanner reads the hand too. A player holding a
     * {@code quiver_size_boost} instrument in the same hand as an Expanded Quiver weapon is ONE SLOT
     * producing TWO sources -- one {@code /rpg give} away, not hypothetical -- and
     * {@code Stat.putModifier} is put-or-REPLACE, so a shared key silently erases one of them.
     *
     * <p>Reads the constants rather than the strings, which is the entire point: the literal-string
     * version of this assertion lives in {@code ExpandedQuiverTest} and survived MUT-PREFIX untouched.
     */
    @Test
    void thePrefixIsDisjointFromTheInstrumentScannersBareSlotKeys() {
        assertFalse(ExpandedQuiverModifierItems.SOURCE_PREFIX.isEmpty(),
                "an empty prefix is no prefix -- the keys would be bare slot names and collide");
        assertNotEquals(QuiverSizeModifierItems.SOURCE_PREFIX,
                ExpandedQuiverModifierItems.SOURCE_PREFIX,
                "the enchant scanner and the instrument scanner must not share a prefix: both can "
                        + "produce a source for the MAIN HAND, and Stat.putModifier would keep only one");

        // And the composed keys differ, which is what actually reaches the Stat.
        String instrumentKey = QuiverSizeModifierItems.SOURCE_PREFIX + "HAND";
        String enchantKey = ExpandedQuiverModifierItems.SOURCE_PREFIX + "HAND";
        assertNotEquals(instrumentKey, enchantKey,
                "same slot, two sources, two keys -- or one of them is lost");
        // Mutation MUT-PREFIX: set SOURCE_PREFIX to "quiversize:" -> reddens here. It did NOT redden
        // anywhere before this row existed.
    }

    /**
     * The prefix must not be a PREFIX OF the other one either, which equality alone does not catch.
     *
     * <p>{@code "quiversize:"} and {@code "quiversize:x"} are unequal and still collide the moment a
     * slot name makes them agree. Disjointness of the composed key space is the real requirement;
     * inequality of the constants is only the cheapest half of it.
     */
    @Test
    void neitherPrefixIsAPrefixOfTheOther() {
        String instrument = QuiverSizeModifierItems.SOURCE_PREFIX;
        String enchant = ExpandedQuiverModifierItems.SOURCE_PREFIX;
        assertFalse(instrument.startsWith(enchant) || enchant.startsWith(instrument),
                "one prefix contains the other (" + instrument + " / " + enchant + "), so some slot "
                        + "name makes the composed keys collide even though the constants differ");
    }

    // --- MUT-MERGE --------------------------------------------------------------------------------

    /**
     * BOTH SOURCES SURVIVE THE MERGE. This is the row MUT-MERGE had nothing to redden.
     *
     * <p>{@code ModifierReconciler.reconcile} removes every applied source ABSENT from the map it is
     * handed, so a merge that drops one is not a smaller bonus -- it is that source being CLEARED
     * from the stat on the next scan, silently and forever.
     */
    @Test
    void theMergeKeepsBothSourcesAndTheirValues() {
        Map<String, Double> instrument = new LinkedHashMap<>();
        instrument.put("HAND", INSTRUMENT_ARROWS);
        Map<String, Double> enchant = new LinkedHashMap<>();
        enchant.put(ExpandedQuiverModifierItems.SOURCE_PREFIX + "HAND", ENCHANT_ARROWS);

        Map<String, Double> merged = ExpandedQuiverModifierItems.mergedSources(instrument, enchant);

        assertEquals(2, merged.size(), "both sources present -- a size of 1 is a source being cleared");
        assertEquals(INSTRUMENT_ARROWS, merged.get("HAND"),
                "the instrument's 19 survived");
        assertEquals(ENCHANT_ARROWS, merged.get(ExpandedQuiverModifierItems.SOURCE_PREFIX + "HAND"),
                "the enchant's 2 survived");
        assertEquals(21.0, merged.values().stream().mapToDouble(Double::doubleValue).sum(),
                "8 authored + 21 resolves to 29, and 21 differs from both inputs");
        // Mutation MUT-MERGE: return the instrument map alone, or drop either argument -> reddens.
        // Dropping an argument no longer even compiles, which is why the merge was extracted.
    }

    /**
     * The merge must not MUTATE either input, because the caller passes freshly-built scan maps and a
     * future caller may not.
     *
     * <p>Cheap, and it pins the {@code new HashMap<>(a)} rather than {@code a.putAll(b)} -- two
     * spellings that are identical from the return value and differ entirely for the caller.
     */
    @Test
    void theMergeLeavesBothInputsUntouched() {
        Map<String, Double> instrument = new LinkedHashMap<>();
        instrument.put("HAND", INSTRUMENT_ARROWS);
        Map<String, Double> enchant = new LinkedHashMap<>();
        enchant.put(ExpandedQuiverModifierItems.SOURCE_PREFIX + "HAND", ENCHANT_ARROWS);

        ExpandedQuiverModifierItems.mergedSources(instrument, enchant);

        assertEquals(1, instrument.size(), "the instrument scan's map was written into");
        assertEquals(1, enchant.size(), "the enchant scan's map was written into");
        // Mutation: `instrument.putAll(enchant); return instrument;` -> reddens.
    }

    /**
     * AND THE COLLISION THE PREFIX EXISTS TO PREVENT, DEMONSTRATED THROUGH THE REAL MERGE.
     *
     * <p>Not a claim about correct behaviour -- it is what WOULD happen if the prefixes ever agreed,
     * which is why {@link #thePrefixIsDisjointFromTheInstrumentScannersBareSlotKeys} is the row that
     * matters and this one is its explanation.
     */
    @Test
    void aSHAREDKeyLosesOneSourceEntirely() {
        Map<String, Double> instrument = new LinkedHashMap<>();
        instrument.put("HAND", INSTRUMENT_ARROWS);
        Map<String, Double> collidingEnchant = new LinkedHashMap<>();
        collidingEnchant.put("HAND", ENCHANT_ARROWS);           // the un-prefixed world

        Map<String, Double> merged =
                ExpandedQuiverModifierItems.mergedSources(instrument, collidingEnchant);

        assertEquals(1, merged.size(), "one key, so one source -- the other is simply gone");
        assertEquals(ENCHANT_ARROWS, merged.get("HAND"),
                "putAll is put-or-REPLACE, so the enchant wins and the instrument's 19 vanished");
        assertTrue(merged.values().stream().mapToDouble(Double::doubleValue).sum() < INSTRUMENT_ARROWS,
                "and the player's total bonus DROPPED by holding a second source, which is the "
                        + "shape of the defect: gear that removes gear");
    }
}
