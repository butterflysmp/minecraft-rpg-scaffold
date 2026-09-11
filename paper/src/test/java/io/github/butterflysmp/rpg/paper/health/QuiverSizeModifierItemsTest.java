package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.QuiverSize;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of the quiver-size scan a unit test can reach: its source keys, and what the instrument
 * actually resolves to.
 *
 * <p>{@code desiredModifiers} needs a live {@code Player} and is boot-witnessed, like every scanner
 * in this package. {@code PLAN-quiver-a2.md}'s boot-only table carries the row rather than this file
 * pretending to cover it.
 */
class QuiverSizeModifierItemsTest {

    /** {@code quiver_stone.yml}'s authored magazine -- the base the instrument adds to. */
    private static final int AUTHORED = 9;

    @Test
    void everyQuiverSizeSourceKeyIsDisjointFromABareSlotNameAndFromEveryOtherScannersPrefix() {
        assertFalse(QuiverSizeModifierItems.SOURCE_PREFIX.isEmpty(),
                "an empty prefix would collide with the crit, health-regen and mana-regen scanners "
                        + "outright -- they walk the same slots on the same player");

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String key = QuiverSizeModifierItems.SOURCE_PREFIX + slot.name();
            assertNotEquals(slot.name(), key,
                    "the key for " + slot + " must not be the bare slot name");
            assertNotEquals(ManaRegenModifierItems.SOURCE_PREFIX + slot.name(), key,
                    "nor collide with the mana-regen scanner's, which walks the same slots");
            assertNotEquals(HealthRegenModifierItems.SOURCE_PREFIX + slot.name(), key,
                    "nor the health-regen one's");
            assertNotEquals(GrowthModifierItems.SOURCE_PREFIX + slot.name(), key, "nor Growth's");
            assertNotEquals(ManaBankModifierItems.SOURCE_PREFIX + slot.name(), key, "nor Mana Bank's");
            assertTrue(key.startsWith(QuiverSizeModifierItems.SOURCE_PREFIX));
        }
        // Mutation: SOURCE_PREFIX -> "" reddens the bare-slot row; -> "manaregen:" reddens the
        // mana-regen row. A player holding this AND a mana-regen fixture in two slots is an ordinary
        // dev session, so a collision here is not hypothetical.
    }

    /**
     * THE INSTRUMENT'S NUMBER IS BIG ENOUGH TO SEE, AND ALL THREE QUANTITIES DIFFER.
     *
     * <p>A gate row reads what is on screen -- the RESOLVED capacity -- so the base (9), the bonus
     * (19) and the resolved value (28) must be pairwise distinct, or a reading is consistent with
     * more than one hypothesis. That is the same "two independent quantities are equal" defect a
     * four-mob fixture against a chain limit of four already cost this repository.
     *
     * <p>The sweep against everything authored in {@code content/} is recorded in
     * {@code PLAN-quiver-a2.md}; what this row pins is that the three stay different from EACH OTHER,
     * which is the half a later retune is most likely to break.
     */
    @Test
    void theInstrumentResolvesToTwentyEightAndNoTwoOfItsNumbersAreEqual() {
        int bonus = (int) QuiverSizeModifierItems.DEFAULT_BOOST;
        int resolved = QuiverSize.resolve(AUTHORED, QuiverSizeModifierItems.DEFAULT_BOOST);

        assertTrue(QuiverSize.boosts(bonus),
                "an instrument that declared nothing would leave the reconcile surface unwitnessed");
        assertEquals(19, bonus);
        assertEquals(28, resolved, "9 + 19, the number a gate row actually reads off the tooltip");

        assertNotEquals(AUTHORED, bonus, "base and bonus must differ");
        assertNotEquals(AUTHORED, resolved, "base and resolved must differ");
        assertNotEquals(bonus, resolved, "bonus and resolved must differ -- otherwise a row that "
                + "read the modifier instead of the capacity would pass either way");
        // Mutation: DEFAULT_BOOST -> 9.0 makes bonus == base; -> 0.0 reddens the boosts row and the
        // resolved row at once.
    }

    /**
     * THE {@code double} PDC VALUE IS NARROWED TO WHOLE ARROWS AT THIS BOUNDARY.
     *
     * <p>{@code Keys.quiverSizeBoost} stores a {@code DOUBLE}, like every other boost key, because
     * {@code Stat} sums doubles. The stat means whole arrows. {@code desiredModifiers} floors once,
     * at the edge, so {@code boosts} and {@code contribution} never see a fraction -- their
     * {@code int} parameters make that unrepresentable rather than merely unwanted.
     *
     * <p>This row stages the conversion the scanner performs, since the scanner itself needs a
     * {@code Player}. Forces red: the scanner casting instead of flooring (they differ on negatives),
     * or passing the raw double through some future {@code double} overload.
     */
    @Test
    void aFractionalPdcValueIsFlooredOnceAtTheBoundaryRatherThanCarriedInwards() {
        assertEquals(2, QuiverSize.arrows(2.9), "floor, not round");
        assertTrue(QuiverSize.boosts(QuiverSize.arrows(2.9)), "2 arrows still declares something");
        assertFalse(QuiverSize.boosts(QuiverSize.arrows(0.9)),
                "but nine tenths of an arrow declares NOTHING -- it floors to 0, and a source "
                        + "written every scan for a no-op bonus is churn the reconciler does not need");
        assertEquals(AUTHORED, QuiverSize.resolve(AUTHORED, 0.9),
                "and the capacity is unmoved, which is what the player sees");
    }
}
