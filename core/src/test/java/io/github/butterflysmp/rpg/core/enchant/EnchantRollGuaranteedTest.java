package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Has;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Requires;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Rollable;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE GUARANTEED CANDIDATE (the seat's G1 and G2, 2026-11-24). Ben: "What we're going to do is add one that comes with
 * every weapon guaranteed." Slot 0 of a weapon ALWAYS offers every guaranteed enchant that names it, listed FIRST; the
 * cascade then rolls the count for the REST of the weapon's slot-0 pool, as today, capped so the slot never holds more
 * than {@link EnchantRoll#MAX_CANDIDATES}. With an empty rest no draw is consumed for it. A weapon no guaranteed enchant
 * names rolls exactly as today.
 */
class EnchantRollGuaranteedTest {

    // Listed AFTER the behavioural one in the roster, so "listed first" cannot pass by roster order.
    private static final Rollable STAFF_BEHAVIOUR = Rollable.forWeapon("staff_behaviour", null, Requires.NONE, "staff");
    private static final Rollable STAFF_TUNE = Rollable.forWeapon("staff_tune", null, Requires.NONE, "staff", true);
    private static final Rollable WAND_TUNE = Rollable.forWeapon("wand_tune", null, Requires.NONE, "wand", true);
    private static final Rollable ATTUNEMENT = new Rollable("attunement", GearClass.MAGE);
    private static final List<Rollable> ROSTER = List.of(ATTUNEMENT, STAFF_BEHAVIOUR, STAFF_TUNE, WAND_TUNE);

    /**
     * Ben's cascade. READ ({@code CandidateCascade.count}): it takes ONE DRAW PER TIER IT TRIES, rarest first, and none for
     * the last (guaranteed) tier; a pool of 0 takes none. So a draw of 0.99 misses three tiers and lands the fourth: three
     * draws for the count, then one per candidate picked.
     */
    private static final CandidateCascade BENS = new CandidateCascade(List.of(
            new Tier(4, 0.10), new Tier(3, 0.40), new Tier(2, 0.70), new Tier(1, 1.0)));
    private static final CandidateCascade ALWAYS_FOUR = new CandidateCascade(List.of(new Tier(4, 1.0)));

    private static Double[] repeat(double v, int n) {
        Double[] out = new Double[n];
        java.util.Arrays.fill(out, v);
        return out;
    }

    /** A draw source that records how many draws were taken and answers from a fixed list. */
    private static final class Draws implements DoubleSupplier {
        final List<Double> values;
        int taken;

        Draws(Double... values) {
            this.values = new ArrayList<>(List.of(values));
        }

        @Override public double getAsDouble() {
            return values.get(taken++);
        }
    }

    private static List<String> ids(EnchantSlot slot) {
        return slot.candidates().stream().map(EnchantCandidate::enchantId).toList();
    }

    @Test
    void theGuaranteedOneIsListedFirstAndTheRestRollsBesideIt() {
        // Slot 0: the REST is [staff_behaviour] (size 1): 3 tier draws + 1 pick = 4. Slot 1: no element, 0.
        // Slots 2 and 3: [attunement] (size 1): 4 each. Total 12. This row pins the ORDER and the locks; the next one
        // pins that the guaranteed one is kept out of the cascade's pool.
        Draws draws = new Draws(repeat(0.99, 12));
        EnchantState state = EnchantRoll.roll("staff", Optional.empty(), Has.EVERY, GearClass.MAGE, ROSTER, BENS, draws);
        assertEquals(List.of("staff_tune", "staff_behaviour"), ids(state.slots().get(0)),
                "the guaranteed one FIRST, though the roster lists it after the behavioural one");
        assertEquals(EnchantSlot.NONE, state.slots().get(0).activeIndex(), "nothing active: the player buys");
        assertTrue(state.slots().get(0).candidates().stream().allMatch(c -> c.level() == 0), "every candidate locked");
        assertEquals(12, draws.taken, "the guaranteed one consumed no draw");
    }

    @Test
    void anEmptyRestConsumesNoDrawForTheSlot() {
        // The wand's pool is its guaranteed one alone, so the REST is empty: slot 0 takes NO draw. Slots 2 and 3 take 4
        // each (3 tier draws + 1 pick), 8 in all; only 8 are supplied, so one draw spent on slot 0 runs the source dry.
        Draws draws = new Draws(repeat(0.99, 8));
        EnchantState state = EnchantRoll.roll("wand", Optional.empty(), Has.EVERY, GearClass.MAGE, ROSTER, BENS, draws);
        assertEquals(List.of("wand_tune"), ids(state.slots().get(0)));
        assertEquals(List.of("attunement"), ids(state.slots().get(2)));
        assertEquals(8, draws.taken, "slots 2 and 3 only");
    }

    @Test
    void theSlotNeverHoldsMoreThanTheTable() {
        List<Rollable> roster = new ArrayList<>();
        roster.add(Rollable.forWeapon("g1", null, Requires.NONE, "staff", true));
        roster.add(Rollable.forWeapon("g2", null, Requires.NONE, "staff", true));
        for (int i = 0; i < 5; i++) roster.add(Rollable.forWeapon("b" + i, null, Requires.NONE, "staff"));
        // ALWAYS_FOUR offers 4 with no draw; with two guaranteed there is room for 2 more: 2 picks of 0.0 (each the first
        // of what remains). No normal enchant in this roster, so slots 1-3 draw nothing.
        Draws draws = new Draws(repeat(0.0, 2));
        EnchantState state = EnchantRoll.roll("staff", Optional.empty(), Has.EVERY, GearClass.MAGE, roster, ALWAYS_FOUR,
                draws);
        assertEquals(EnchantRoll.MAX_CANDIDATES, state.slots().get(0).candidates().size());
        assertEquals(List.of("g1", "g2", "b0", "b1"), ids(state.slots().get(0)));
    }

    @Test
    void aWeaponNoGuaranteedEnchantNamesRollsAsToday() {
        Rollable plain = Rollable.forWeapon("plain", null, Requires.NONE, "rod");
        Draws a = new Draws(repeat(0.99, 12));
        EnchantState state = EnchantRoll.roll("rod", Optional.empty(), Has.EVERY, GearClass.MAGE,
                List.of(ATTUNEMENT, plain, WAND_TUNE), BENS, a);
        assertEquals(List.of("plain"), ids(state.slots().get(0)), "another weapon's guaranteed one is not offered");
        assertEquals(12, a.taken, "3 tier draws and a pick for slot 0, as before the guaranteed candidate existed");
    }

    @Test
    void aGuaranteedEnchantMustNameAWeapon() {
        assertThrows(IllegalArgumentException.class,
                () -> new Rollable("loose", null, Requires.NONE, null, null, true));
    }
}
