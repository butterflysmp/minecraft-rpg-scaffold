package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Has;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Requires;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Rollable;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE UNIQUE SLOT (the seat's rulings U1-U6 and V1-V2, 2026-11-23). Ben, verbatim: <i>"the first enchantment is a
 * "Unique" enchantment in that the enchantments in that slot are tied to the element more"</i>, <i>"make it possible
 * that an item has none if there are none in the pool"</i>, and (the amendment) <i>"Instead of 2 normal enchantment
 * slots, I want 3 in addition to the unique enchantment."</i>
 *
 * <p>Slot 0 draws ONLY from the unique pool (element, then class, then requires); slots 1, 2 and 3 ONLY from the normal
 * pool. No element, or an empty unique pool, gives slot 0 NO candidates (Ben's "A").
 */
class EnchantRollUniqueTest {

    // A unique enchant per filter, and normals beside them.
    private static final Rollable KINETIC_ANY = new Rollable("k_any", null, Requires.NONE, "kinetic");
    private static final Rollable KINETIC_MELEE = new Rollable("k_melee", GearClass.MELEE, Requires.NONE, "kinetic");
    private static final Rollable KINETIC_QUIVER = new Rollable("k_quiver", GearClass.RANGER, Requires.QUIVER, "kinetic");
    private static final Rollable FIRE_ANY = new Rollable("f_any", null, Requires.NONE, "fire");
    private static final Rollable SHARPNESS = new Rollable("sharpness", GearClass.MELEE);
    private static final Rollable POWER = new Rollable("power", GearClass.RANGER);
    private static final Rollable UNBREAKING = new Rollable("unbreaking", null);
    private static final List<Rollable> ROSTER =
            List.of(FIRE_ANY, KINETIC_ANY, KINETIC_MELEE, KINETIC_QUIVER, POWER, SHARPNESS, UNBREAKING);

    private static final CandidateCascade ALWAYS_FOUR = new CandidateCascade(List.of(new Tier(4, 1.0)));
    private static final CandidateCascade BENS = new CandidateCascade(List.of(
            new Tier(4, 0.10), new Tier(3, 0.40), new Tier(2, 0.70), new Tier(1, 1.0)));

    private static List<String> ids(List<Rollable> pool) {
        return pool.stream().map(Rollable::enchantId).toList();
    }

    private static List<String> ids(EnchantSlot slot) {
        return slot.candidates().stream().map(EnchantCandidate::enchantId).toList();
    }

    // ---- U2: each filter of the unique pool, IN ISOLATION -----------------------------------------------------------

    /** ELEMENT: on a fire weapon only the fire unique; on a kinetic one only kinetic ones. Class and requires open. */
    @Test
    void theElementFilterAlone() {
        assertEquals(List.of("f_any"), ids(EnchantRoll.uniquePoolFor(Optional.of("fire"), Has.EVERY, GearClass.RANGER, ROSTER)));
        assertEquals(List.of("k_any", "k_quiver"),
                ids(EnchantRoll.uniquePoolFor(Optional.of("kinetic"), Has.EVERY, GearClass.RANGER, ROSTER)));
    }

    /** CLASS: same element, same quantities; only the class differs, and k_melee leaves / k_quiver leaves. */
    @Test
    void theClassFilterAlone() {
        assertEquals(List.of("k_any", "k_melee"),
                ids(EnchantRoll.uniquePoolFor(Optional.of("kinetic"), Has.EVERY, GearClass.MELEE, ROSTER)));
        assertEquals(List.of("k_any", "k_quiver"),
                ids(EnchantRoll.uniquePoolFor(Optional.of("kinetic"), Has.EVERY, GearClass.RANGER, ROSTER)));
    }

    /** REQUIRES: a ranger with no quiver loses k_quiver; element and class unchanged. */
    @Test
    void theRequiresFilterAlone() {
        assertEquals(List.of("k_any"),
                ids(EnchantRoll.uniquePoolFor(Optional.of("kinetic"), Has.NOTHING, GearClass.RANGER, ROSTER)));
        assertEquals(List.of("k_any", "k_quiver"),
                ids(EnchantRoll.uniquePoolFor(Optional.of("kinetic"), new Has(false, true), GearClass.RANGER, ROSTER)));
    }

    /** An accessory rolls nothing, unique included (ruling A4). */
    @Test
    void anAccessoryHasNoUniquePool() {
        assertEquals(List.of(), EnchantRoll.uniquePoolFor(Optional.of("kinetic"), Has.EVERY, GearClass.ACCESSORY, ROSTER));
    }

    // ---- the two pools are disjoint -------------------------------------------------------------------------------

    /** The normal pool never holds a unique enchant, whatever the class. */
    @Test
    void theNormalPoolNeverHoldsAUniqueEnchant() {
        for (GearClass c : GearClass.values()) {
            for (Rollable r : EnchantRoll.normalPoolFor(Has.EVERY, c, ROSTER)) {
                assertEquals(null, r.element(), c + " normal pool holds unique " + r.enchantId());
            }
        }
        assertEquals(List.of("power", "unbreaking"), ids(EnchantRoll.normalPoolFor(Has.EVERY, GearClass.RANGER, ROSTER)),
                "the control: the normal pool is not simply empty");
    }

    // ---- U3 / V1: slot 0 is unique, slots 1-3 normal, on every roll -----------------------------------------------

    @Test
    void fourSlotsSlotZeroUniqueAndTheOtherThreeNormal() {
        EnchantState state = EnchantRoll.roll(Optional.of("kinetic"), Has.EVERY, GearClass.MELEE, ROSTER, ALWAYS_FOUR,
                () -> 0.0);
        assertEquals(4, state.slots().size());
        assertEquals(List.of("k_any", "k_melee"), ids(state.slots().get(0)));
        for (int s = 1; s < 4; s++) assertEquals(List.of("sharpness", "unbreaking"), ids(state.slots().get(s)), "slot " + s);
    }

    /** Over 2,000 seeded rolls of every class and both elements: no unique id outside slot 0, no normal id in it. */
    @Test
    void overManyRollsSlotZeroNeverHoldsANormalAndTheOthersNeverAUnique() {
        Random random = new Random(20261123L);
        int slotZeroFilled = 0;
        for (int i = 0; i < 2000; i++) {
            GearClass c = List.of(GearClass.MELEE, GearClass.RANGER, GearClass.MAGE).get(i % 3);
            Optional<String> element = Optional.of(i % 2 == 0 ? "kinetic" : "fire");
            EnchantState state = EnchantRoll.roll(element, Has.EVERY, c, ROSTER, BENS, random::nextDouble);
            for (EnchantCandidate cand : state.slots().get(0).candidates()) {
                assertTrue(cand.enchantId().startsWith("k_") || cand.enchantId().startsWith("f_"), cand.enchantId());
                slotZeroFilled++;
            }
            for (int s = 1; s < 4; s++) {
                for (EnchantCandidate cand : state.slots().get(s).candidates()) {
                    assertTrue(!cand.enchantId().startsWith("k_") && !cand.enchantId().startsWith("f_"),
                            "slot " + s + " holds unique " + cand.enchantId());
                }
            }
        }
        assertTrue(slotZeroFilled > 0, "the control: slot 0 was filled at all");
    }

    // ---- Ben's "A": no element, or an empty unique pool, gives slot 0 nothing -------------------------------------

    @Test
    void noElementGivesSlotZeroNoCandidates() {
        EnchantState state = EnchantRoll.roll(Optional.empty(), Has.EVERY, GearClass.ARMOR, ROSTER, ALWAYS_FOUR, () -> 0.0);
        assertEquals(4, state.slots().size(), "no gear loses a slot");
        assertEquals(List.of(), state.slots().get(0).candidates());
        assertEquals(List.of("unbreaking"), ids(state.slots().get(1)), "the control: the normal slots still roll");
    }

    @Test
    void anEmptyUniquePoolGivesSlotZeroNoCandidates() {
        EnchantState state = EnchantRoll.roll(Optional.of("water"), Has.EVERY, GearClass.MAGE, ROSTER, ALWAYS_FOUR, () -> 0.0);
        assertEquals(List.of(), state.slots().get(0).candidates());
        assertEquals(EnchantSlot.NONE, state.slots().get(0).activeIndex());
    }

    /** With one unique in the pool, slot 0 offers that one (U4), however many the cascade rolled. */
    @Test
    void aUniquePoolOfOneOffersThatOne() {
        EnchantState state = EnchantRoll.roll(Optional.of("fire"), Has.EVERY, GearClass.MAGE, ROSTER, ALWAYS_FOUR, () -> 0.0);
        assertEquals(List.of("f_any"), ids(state.slots().get(0)));
    }

    // ---- U6: the element cannot be skipped -------------------------------------------------------------------------

    @Test
    void aNullElementIsRefused_theCallerMustSayEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> EnchantRoll.roll(null, Has.EVERY, GearClass.MELEE, ROSTER, BENS, () -> 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> EnchantRoll.uniquePoolFor(null, Has.EVERY, GearClass.MELEE, ROSTER));
    }

    @Test
    void aBlankElementOnARollableIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Rollable("x", null, Requires.NONE, " "));
    }

    // ---- the cascade drives every slot, unique and normal alike (V2, V3) ------------------------------------------

    /**
     * The one draw-order row. Kinetic MELEE: unique pool [k_any, k_melee], normal [sharpness, unbreaking].
     * slot 0: 0.05 lands the 10% -> 4, capped 2; picks 0.0 -> k_any, 0.0 -> k_melee.
     * slot 1: 0.5 misses 10% and 40%, 0.6 lands 70% -> 2; picks 0.9 -> unbreaking, 0.0 -> sharpness.
     * slot 2: 0.2 misses 10%, 0.3 lands 40% -> 3, capped 2; picks 0.0 -> sharpness, 0.0 -> unbreaking.
     * slot 3: 0.9, 0.9, 0.9 miss all three -> 1 (no draw for the guaranteed tier); pick 0.0 -> sharpness.
     */
    @Test
    void aFullRollReadsSlotBySlot() {
        double[] d = {0.05, 0.0, 0.0,   0.5, 0.5, 0.6, 0.9, 0.0,   0.2, 0.3, 0.0, 0.0,   0.9, 0.9, 0.9, 0.0};
        int[] cursor = {0};
        EnchantState state = EnchantRoll.roll(Optional.of("kinetic"), Has.EVERY, GearClass.MELEE, ROSTER, BENS,
                () -> d[cursor[0]++]);
        assertEquals(List.of("k_any", "k_melee"), ids(state.slots().get(0)));
        assertEquals(List.of("unbreaking", "sharpness"), ids(state.slots().get(1)));
        assertEquals(List.of("sharpness", "unbreaking"), ids(state.slots().get(2)));
        assertEquals(List.of("sharpness"), ids(state.slots().get(3)));
        assertEquals(d.length, cursor[0], "every draw consumed, none extra");
    }

    // ---- the dev tally (/rpg enchant rollstats) --------------------------------------------------------------------

    @Test
    void theTallyCountsEachSlotsCandidates() {
        EnchantState a = new EnchantState(List.of(EnchantSlot.empty(),
                new EnchantSlot(List.of(new EnchantCandidate("x", 0)), EnchantSlot.NONE)));
        EnchantState b = new EnchantState(List.of(new EnchantSlot(List.of(new EnchantCandidate("y", 0)), EnchantSlot.NONE),
                new EnchantSlot(List.of(new EnchantCandidate("x", 0), new EnchantCandidate("z", 0)), EnchantSlot.NONE)));
        int[][] t = EnchantRoll.tally(List.of(a, b));
        assertEquals(EnchantRoll.SLOTS, t.length);
        assertEquals(EnchantRoll.MAX_CANDIDATES + 1, t[0].length);
        assertEquals(1, t[0][0]);
        assertEquals(1, t[0][1]);
        assertEquals(1, t[1][1]);
        assertEquals(1, t[1][2]);
        assertEquals(2, t[2][0], "a slot the state lacks counts as zero candidates");
    }
}
