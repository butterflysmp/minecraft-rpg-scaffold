package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Rollable;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shippable bugs this guards, in the order they would hurt:
 *
 * <ul>
 *   <li>A roll offering an enchant the weapon can never use -- Power on a sword.
 *   <li>A roll producing a slot count the table cannot render, which {@code EnchantMenuLayout} refuses at the door.
 *   <li>A candidate arriving UNLOCKED, which hands out for free the thing the economy exists to charge for.
 *   <li>A slot offering the same enchant twice, which {@code EnchantSlot} throws on -- from inside a mint.
 * </ul>
 *
 * <p><b>REWRITTEN 2026-11-23 for the unique slot and Ben's cascade</b> (the seat's rulings U3 and V1-V3). The roll is
 * now four slots, slot 0 unique and slots 1-3 normal, and the count is {@link CandidateCascade}'s. The rows that pinned
 * {@code candidateCount}'s even 1..3 split were deleted with it; the cascade's boundaries are {@code CandidateCascadeTest}'s,
 * the unique slot's {@code EnchantRollUniqueTest}'s. The pool and {@code pick} rows are unchanged but for the rename
 * {@code poolFor} -> {@code normalPoolFor}. Every roll here passes NO element, so slot 0 is empty and the normal slots
 * are the ones read.
 */
class EnchantRollTest {

    // The shipped roster, IN THE ORDER THE LOADER PRODUCES (alphabetical by filename). `unbreaking` is universal. Nothing
    // pins this list against content/enchants/*.yml; the paper content tests read the real files.
    private static final Rollable SHARPNESS = new Rollable("sharpness", GearClass.MELEE);
    private static final Rollable POWER = new Rollable("power", GearClass.RANGER);
    private static final Rollable ATTUNEMENT = new Rollable("attunement", GearClass.MAGE);
    private static final Rollable BULWARK = new Rollable("bulwark", GearClass.SHIELD);
    private static final Rollable THORNS = new Rollable("thorns", GearClass.SHIELD);
    private static final Rollable PROTECTION = new Rollable("protection", GearClass.ARMOR);
    private static final Rollable GROWTH = new Rollable("growth", GearClass.ARMOR);
    private static final Rollable MANA_BANK = new Rollable("mana_bank", GearClass.ARMOR);
    private static final Rollable UNBREAKING = new Rollable("unbreaking", null);
    private static final List<Rollable> ROSTER =
            List.of(ATTUNEMENT, BULWARK, GROWTH, MANA_BANK, POWER, PROTECTION, SHARPNESS, THORNS,
                    UNBREAKING);

    /** Ben's cascade as a fixture (his numbers live in content; the paper test reads the file). */
    private static final CandidateCascade BENS = new CandidateCascade(List.of(
            new Tier(4, 0.10), new Tier(3, 0.40), new Tier(2, 0.70), new Tier(1, 1.0)));

    /** Literal draws in order; running off the end throws, so a change in arity is loud. */
    private static DoubleSupplier draws(double... values) {
        int[] cursor = {0};
        return () -> values[cursor[0]++];
    }

    /** Every draw the same value. Under BENS, 0.0 rolls FOUR (then capped) and 0.99 rolls ONE. */
    private static DoubleSupplier always(double value) {
        return () -> value;
    }

    private static EnchantState roll(GearClass c, List<Rollable> roster, DoubleSupplier d) {
        return EnchantRoll.roll(Optional.empty(), EnchantRoll.Has.EVERY, c, roster, BENS, d);
    }

    /** Slots 1..3: the normal ones. */
    private static List<EnchantSlot> normal(EnchantState state) {
        return state.slots().subList(1, state.slots().size());
    }

    private static int distinctCount(List<String> ids) {
        return (int) ids.stream().distinct().count();
    }

    private static List<String> idsIn(EnchantSlot slot) {
        List<String> ids = new ArrayList<>();
        for (EnchantCandidate candidate : slot.candidates()) ids.add(candidate.enchantId());
        return ids;
    }

    // ---- the shape ------------------------------------------------------------------------

    @Test
    void everyRolledPieceGetsExactlyFourSlots() {
        // V1: the unique slot and three normal ones. A fifth would be refused by EnchantMenuLayout.overflow.
        assertEquals(4, roll(GearClass.MELEE, ROSTER, always(0.0)).slots().size());
        assertEquals(4, roll(GearClass.MAGE, ROSTER, always(0.99)).slots().size());
        assertEquals(4, EnchantRoll.SLOTS);
        assertEquals(0, EnchantRoll.UNIQUE_SLOT);
        // Mutation: SLOTS 4 -> 3, or the loop bound -> reddens.
    }

    @Test
    void everyRolledCandidateIsLockedAndNothingIsActive() {
        EnchantState state = roll(GearClass.ARMOR, ROSTER, always(0.0));
        assertTrue(state.slots().stream().mapToInt(s -> s.candidates().size()).sum() > 0, "control: something rolled");
        for (EnchantSlot slot : state.slots()) {
            assertEquals(EnchantSlot.NONE, slot.activeIndex(), "a fresh roll activates nothing");
            for (EnchantCandidate candidate : slot.candidates()) {
                assertEquals(0, candidate.level());
                assertTrue(candidate.isLocked());
            }
        }
        assertTrue(state.effective().isEmpty(), "nothing takes effect until the player unlocks");
        // Mutation: `new EnchantCandidate(id, 0)` -> `1` -> reddens.
    }

    @Test
    void withinASlotTheCandidatesAreDistinct() {
        // always(0.0) asks for FOUR in every slot from a pool of two (melee) and four (armor): the case a non-shrinking
        // pool would fill with the same id twice.
        for (GearClass c : List.of(GearClass.MELEE, GearClass.ARMOR, GearClass.SHIELD)) {
            for (EnchantSlot slot : roll(c, ROSTER, always(0.0)).slots()) {
                List<String> ids = idsIn(slot);
                assertEquals(ids.size(), distinctCount(ids), "a slot offers an enchant twice: " + ids);
            }
        }
        // Mutation: drop `remaining.remove(picked)` -> EnchantSlot's constructor throws -> reddens by erroring.
    }

    @Test
    void theSameEnchantMayBeOfferedInMoreThanOneSlot() {
        // No mutual exclusion across slots. always(0.99) rolls ONE per slot and picks the LAST of the pool: unbreaking.
        for (EnchantSlot slot : normal(roll(GearClass.MELEE, ROSTER, always(0.99)))) {
            assertEquals(List.of("unbreaking"), idsIn(slot));
        }
        // Mutation: hoist `remaining` out of the slot loop -> slots 2 and 3 cannot repeat it -> reddens.
    }

    // ---- the class gate -------------------------------------------------------------------

    @Test
    void aMeleeWeaponIsNeverOfferedPowerOrAttunement() {
        for (EnchantSlot slot : roll(GearClass.MELEE, ROSTER, always(0.0)).slots()) {
            for (String id : idsIn(slot)) {
                assertTrue(id.equals("sharpness") || id.equals("unbreaking"), "a melee weapon was offered '" + id + "'");
            }
        }
        // Mutation: drop the class comparison in normalPoolFor -> reddens.
    }

    @Test
    void aShieldIsNeverOfferedAWeaponEnchantAndNoWeaponIsEverOfferedBulwark() {
        for (String id : idsIn(roll(GearClass.SHIELD, ROSTER, always(0.0)).slots().get(1))) {
            assertTrue(id.equals("bulwark") || id.equals("thorns") || id.equals("unbreaking"),
                    "a shield was offered '" + id + "'");
        }
        for (GearClass weapon : List.of(GearClass.MELEE, GearClass.RANGER, GearClass.MAGE)) {
            assertFalse(EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, weapon, ROSTER).contains(BULWARK),
                    weapon + " was offered Bulwark -- a block enchant on a weapon");
            assertFalse(EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, weapon, ROSTER).contains(THORNS),
                    weapon + " was offered Thorns -- a reflect read off a stack that cannot block");
        }
    }

    /**
     * The pool sizes the cap meets: shield THREE, armor FOUR, every weapon TWO. A slot rolled at four fills exactly the
     * pool, and armor is now the one shipped pool that fills all FOUR cells (the cap was three until 2026-11-23, and
     * armor's four was clamped to three).
     */
    @Test
    void aRollOfFourFillsExactlyThePool() {
        assertEquals(3, EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.SHIELD, ROSTER).size());
        assertEquals(4, EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.ARMOR, ROSTER).size());
        assertEquals(2, EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.MELEE, ROSTER).size());
        for (EnchantSlot slot : normal(roll(GearClass.SHIELD, ROSTER, always(0.0)))) assertEquals(3, slot.candidates().size());
        for (EnchantSlot slot : normal(roll(GearClass.ARMOR, ROSTER, always(0.0)))) {
            assertEquals(4, slot.candidates().size(), "armor's four now fit the four cells");
            assertEquals(4, distinctCount(idsIn(slot)));
        }
        for (EnchantSlot slot : normal(roll(GearClass.MELEE, ROSTER, always(0.0)))) assertEquals(2, slot.candidates().size());
        assertEquals(4, EnchantRoll.MAX_CANDIDATES);
        // Mutation: drop the cap in CandidateCascade.count -> a pool of two asks for four, pick runs dry -> the melee
        // row still reads 2 (the null-break), so the cap's own guard is CandidateCascadeTest.theCountIsCappedAtThePoolsSize.
    }

    @Test
    void thePoolIsTheClassEnchantPlusTheUniversalOne() {
        assertEquals(List.of(SHARPNESS, UNBREAKING), EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.MELEE, ROSTER));
        assertEquals(List.of(POWER, UNBREAKING), EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.RANGER, ROSTER));
        assertEquals(List.of(ATTUNEMENT, UNBREAKING), EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.MAGE, ROSTER));
        assertEquals(List.of(BULWARK, THORNS, UNBREAKING), EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.SHIELD, ROSTER));
        assertEquals(List.of(GROWTH, MANA_BANK, PROTECTION, UNBREAKING),
                EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.ARMOR, ROSTER));
        // Mutation: drop the `gearClass() != null` arm (universal stops matching) -> UNBREAKING disappears -> reddens.
    }

    @Test
    void aNullHeldClassLeavesOnlyTheUniversalEnchants() {
        assertEquals(List.of(UNBREAKING), EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, null, ROSTER));
    }

    // ---- pick: the boundaries --------------------------------------------------------------

    @Test
    void pickTakesTheFirstEntryAtZeroAndTheLastJustUnderOne() {
        List<Rollable> pool = List.of(SHARPNESS, UNBREAKING);
        assertEquals(SHARPNESS, EnchantRoll.pick(pool, 0.0));
        assertEquals(SHARPNESS, EnchantRoll.pick(pool, 0.4999999));
        assertEquals(UNBREAKING, EnchantRoll.pick(pool, 0.5), "0.5 is the first draw that reaches the second");
        assertEquals(UNBREAKING, EnchantRoll.pick(pool, 0.9999999));
        // Mutation: `roll * size` -> `roll * (size - 1)` -> 0.5 no longer reaches UNBREAKING -> reddens.
    }

    @Test
    void pickAddressesTheShrunkenPoolNotTheOriginal() {
        List<Rollable> shrunk = List.of(UNBREAKING);
        assertEquals(UNBREAKING, EnchantRoll.pick(shrunk, 0.0));
        assertEquals(UNBREAKING, EnchantRoll.pick(shrunk, 0.9999999), "a one-entry pool has one answer");
    }

    @Test
    void pickReturnsNullOnAnEmptyPoolRatherThanThrowing() {
        assertNull(EnchantRoll.pick(List.of(), 0.0));
        assertNull(EnchantRoll.pick(null, 0.0));
    }

    @Test
    void aDrawAtOrPastTheEndsStaysInsideThePool() {
        List<Rollable> pool = List.of(SHARPNESS, UNBREAKING);
        assertEquals(UNBREAKING, EnchantRoll.pick(pool, 1.0));
        assertEquals(SHARPNESS, EnchantRoll.pick(pool, -0.5));
    }

    // ---- the whole roll --------------------------------------------------------------------

    @Test
    void anEmptyRosterStillProducesFourSlotsOfferingNothing() {
        EnchantState state = roll(GearClass.MELEE, List.of(), draws());
        assertEquals(4, state.slots().size());
        for (EnchantSlot slot : state.slots()) {
            assertTrue(slot.candidates().isEmpty());
            assertEquals(EnchantSlot.NONE, slot.activeIndex());
        }
        assertFalse(state.isEmpty(), "four empty slots is not the same value as no slots");
    }

    @Test
    void aRolledStateSurvivesTheWireGrammar() {
        // Slot 0 empty, slots 1-3 filled: the empty slot round-trips as ":-1".
        EnchantState rolled = roll(GearClass.ARMOR, ROSTER, always(0.0));
        EnchantState decoded = EnchantCodec.decode(EnchantCodec.encode(rolled));
        assertEquals(rolled, decoded);
        assertTrue(EnchantCodec.encode(rolled).startsWith("v1;:-1;"), EnchantCodec.encode(rolled));
    }

    @Test
    void aRollableNeedsAnId() {
        assertThrows(IllegalArgumentException.class, () -> new Rollable(null, GearClass.MELEE));
        assertThrows(IllegalArgumentException.class, () -> new Rollable("  ", GearClass.MELEE));
    }

    /** Ruling A4: an accessory's pool is EMPTY -- not the universal enchants the null-gate rule would give any other kind. */
    @Test
    void anAccessoryRollsNothing_notEvenAUniversalEnchant() {
        assertTrue(EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.TOOL, ROSTER).contains(UNBREAKING),
                "control: the universal enchant IS in a non-accessory pool, so the row below can fail");
        assertTrue(EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.ACCESSORY, ROSTER).isEmpty(), "A4");
        roll(GearClass.ACCESSORY, ROSTER, always(0.0)).slots()
                .forEach(slot -> assertTrue(slot.candidates().isEmpty(), "no candidate on any slot"));
        // Mutation: delete the ACCESSORY return in EnchantRoll.normalPoolFor -> reddens.
    }

    /** DEFT HANDS (GATE-deft-hands.md DH5): a class: ranger enchant enters EVERY ranger pool and no other. */
    @Test
    void deftHandsEntersTheRangerPoolAndNoOther() {
        List<Rollable> roster = List.of(new Rollable("expanded_quiver", GearClass.RANGER),
                new Rollable("deft_hands", GearClass.RANGER), new Rollable("unbreaking", null),
                new Rollable("sharpness", GearClass.MELEE));
        assertEquals(List.of(new Rollable("expanded_quiver", GearClass.RANGER), new Rollable("deft_hands", GearClass.RANGER),
                new Rollable("unbreaking", null)), EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, GearClass.RANGER, roster));
        for (GearClass other : List.of(GearClass.MAGE, GearClass.MELEE, GearClass.SHIELD, GearClass.ARMOR)) {
            assertFalse(EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, other, roster).contains(new Rollable("deft_hands", GearClass.RANGER)),
                    other.name());
        }
    }

    /**
     * V4: AN ITEM ROLLED UNDER THE OLD THREE-SLOT SHAPE (three normal slots, up to three candidates) still decodes, and
     * every one of its slots is a legal state. Ben: "we don't care about existing items"; the duty is only that it never
     * throws. The table side is EnchantMenuLayoutTest's.
     */
    @Test
    void anOldThreeNormalSlotItemStillDecodes() {
        String old = "v1;sharpness=2,unbreaking=0:0;unbreaking=1:-1;sharpness=0,unbreaking=3:1";
        EnchantState state = EnchantCodec.decode(old);
        assertEquals(3, state.slots().size());
        assertEquals(List.of("sharpness", "unbreaking"), idsIn(state.slots().get(0)), "slot 0 keeps its NORMAL enchants");
        assertEquals(old, EnchantCodec.encode(state), "and writes back unchanged");
        assertEquals(2, state.activeLevel("sharpness"));
        assertEquals(3, state.activeLevel("unbreaking"));
    }
}
