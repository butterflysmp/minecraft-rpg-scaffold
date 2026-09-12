package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.QuiverSize;
import io.github.butterflysmp.rpg.core.combat.ReloadTime;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of the reload-time scan a unit test can reach: its source keys, its number, and
 * <b>the gate</b>.
 *
 * <p>{@code desiredModifiers} needs a live {@code Player} and is boot-witnessed, like every scanner
 * in this package. {@code PLAN-quiver-a2.md}'s boot-only table carries the row.
 */
class ReloadTimeModifierItemsTest {

    /** {@code quiver_stone.yml}'s authored reload duration, in ticks. */
    private static final int AUTHORED = 34;

    @Test
    void everyReloadTimeSourceKeyIsDisjointFromABareSlotNameAndFromEveryOtherScannersPrefix() {
        assertFalse(ReloadTimeModifierItems.SOURCE_PREFIX.isEmpty(),
                "an empty prefix would collide with four other all-slot scanners outright");

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String key = ReloadTimeModifierItems.SOURCE_PREFIX + slot.name();
            assertNotEquals(slot.name(), key, "the key for " + slot + " must not be the bare name");
            assertNotEquals(QuiverSizeModifierItems.SOURCE_PREFIX + slot.name(), key,
                    "AND ABOVE ALL NOT ITS TWIN'S. These two scanners were written from one "
                            + "template, walk the same slots, and a player holds both at once in "
                            + "every gate row -- a shared prefix would make each wipe the other's "
                            + "sources on alternate ticks.");
            assertNotEquals(ManaRegenModifierItems.SOURCE_PREFIX + slot.name(), key);
            assertNotEquals(HealthRegenModifierItems.SOURCE_PREFIX + slot.name(), key);
            assertNotEquals(GrowthModifierItems.SOURCE_PREFIX + slot.name(), key);
            assertNotEquals(ManaBankModifierItems.SOURCE_PREFIX + slot.name(), key);
            assertTrue(key.startsWith(ReloadTimeModifierItems.SOURCE_PREFIX));
        }
    }

    /**
     * THE GATE ADMITS A REDUCTION, WHICH IS THE ONLY DIRECTION THE STAT IS FOR.
     *
     * <p>This row exists because the sibling's gate was copied here first and it forbade exactly
     * that: {@code boosts} is {@code > NONE}, a reload-speed item carries a NEGATIVE, and the item
     * would have declared nothing and done nothing, silently, on every scan. The scanner now calls
     * {@code ReloadTime.declares}.
     *
     * <p>Forces red: the scanner's gate narrowed back to {@code > NONE}, or
     * {@code ReloadTime.declares} reverted to it. This is the one change a later reader makes to
     * bring this file into line with its four neighbours, so it is asserted first and by name.
     */
    @Test
    void aReducingInstrumentDECLARESAModifierRatherThanBeingSilentlyDropped() {
        assertTrue(ReloadTime.declares(ReloadTime.ticks(-5.0)),
                "-5 ticks is reload-SPEED gear, the whole point of the stat");
        assertTrue(ReloadTime.declares(ReloadTime.ticks(ReloadTimeModifierItems.DEFAULT_BOOST)),
                "and the adding default declares too");
        assertFalse(ReloadTime.declares(ReloadTime.ticks(0.0)),
                "only 0 declares nothing -- a no-op source written four times a second is churn");

        assertFalse(QuiverSize.boosts(-5),
                "THE TWIN IS DELIBERATELY THE OTHER WAY. Quiver size is increase-only because that "
                        + "is a real content ruling; reload time is not, because its sign is "
                        + "inverted. Do not unify them.");
    }

    /**
     * THE INSTRUMENT'S NUMBER, AND ALL THREE QUANTITIES DIFFER.
     *
     * <p>A gate row reads the RESOLVED duration, so 34, 14 and 48 must be pairwise distinct or a
     * reading is consistent with more than one hypothesis.
     */
    @Test
    void theInstrumentResolvesToFortyEightAndNoTwoOfItsNumbersAreEqual() {
        int bonus = ReloadTime.ticks(ReloadTimeModifierItems.DEFAULT_BOOST);
        int resolved = ReloadTime.resolve(AUTHORED, ReloadTimeModifierItems.DEFAULT_BOOST);

        assertEquals(14, bonus);
        assertEquals(48, resolved, "34 + 14, the number a gate row times");
        assertNotEquals(AUTHORED, bonus);
        assertNotEquals(AUTHORED, resolved);
        assertNotEquals(bonus, resolved);
    }

    /**
     * THE TWO INSTRUMENTS CARRY DIFFERENT NUMBERS, AND THAT IS WHAT THE PAIR IS FOR.
     *
     * <p>+19 arrows and +14 ticks. If they matched, a boot row that read one value could not say
     * which instrument produced it, and the entanglement defect the pair exists to expose -- a
     * scanner wired to its twin's key -- would be invisible at exactly the moment it mattered.
     *
     * <p>Forces red: either DEFAULT_BOOST retuned to the other's value.
     */
    @Test
    void theTwoInstrumentsCannotBeConfusedForOneAnotherByTheirNumbers() {
        assertNotEquals(QuiverSizeModifierItems.DEFAULT_BOOST, ReloadTimeModifierItems.DEFAULT_BOOST,
                "+19 arrows vs +14 ticks -- a shared number would make a crossed-wire reading "
                        + "consistent with both hypotheses");
        assertNotEquals(
                QuiverSize.resolve(9, QuiverSizeModifierItems.DEFAULT_BOOST),
                ReloadTime.resolve(AUTHORED, ReloadTimeModifierItems.DEFAULT_BOOST),
                "and neither do the RESOLVED values, 28 vs 48, which are what a row actually reads");
    }
}
