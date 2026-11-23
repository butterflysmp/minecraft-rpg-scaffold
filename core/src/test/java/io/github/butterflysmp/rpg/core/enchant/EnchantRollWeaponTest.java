package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Has;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Kind;
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
 * THE WEAPON SLOT (the seat's rulings W1, W2 and W4, 2026-11-23; Ben: "The first column will be weapon specific
 * enchantments"). Slot 0 offers only the enchants that name THIS gear's id; slot 1 only the unique ones; slots 2 and 3
 * only the normal ones. A weapon no enchant names rolls slot 0 empty.
 */
class EnchantRollWeaponTest {

    // A weapon-specific enchant per filter, a unique one, and normals beside them.
    private static final Rollable STAFF_ANY = Rollable.forWeapon("staff_any", null, Requires.NONE, "staff");
    private static final Rollable STAFF_MAGE = Rollable.forWeapon("staff_mage", GearClass.MAGE, Requires.NONE, "staff");
    private static final Rollable STAFF_QUIVER = Rollable.forWeapon("staff_quiver", null, Requires.QUIVER, "staff");
    private static final Rollable LANTERN_ANY = Rollable.forWeapon("lantern_any", null, Requires.NONE, "lantern");
    private static final Rollable KINETIC_ANY = new Rollable("k_any", null, Requires.NONE, "kinetic");
    private static final Rollable ATTUNEMENT = new Rollable("attunement", GearClass.MAGE);
    private static final Rollable UNBREAKING = new Rollable("unbreaking", null);
    private static final List<Rollable> ROSTER =
            List.of(ATTUNEMENT, KINETIC_ANY, LANTERN_ANY, STAFF_ANY, STAFF_MAGE, STAFF_QUIVER, UNBREAKING);

    private static final CandidateCascade ALWAYS_FOUR = new CandidateCascade(List.of(new Tier(4, 1.0)));
    private static final CandidateCascade BENS = new CandidateCascade(List.of(
            new Tier(4, 0.10), new Tier(3, 0.40), new Tier(2, 0.70), new Tier(1, 1.0)));

    private static List<String> ids(List<Rollable> pool) {
        return pool.stream().map(Rollable::enchantId).toList();
    }

    private static List<String> ids(EnchantSlot slot) {
        return slot.candidates().stream().map(EnchantCandidate::enchantId).toList();
    }

    @Test
    void eachIndexHasOneKind() {
        assertEquals(Kind.WEAPON, EnchantRoll.kindOf(0));
        assertEquals(Kind.UNIQUE, EnchantRoll.kindOf(1));
        assertEquals(Kind.NORMAL, EnchantRoll.kindOf(2));
        assertEquals(Kind.NORMAL, EnchantRoll.kindOf(3));
        assertEquals(Kind.WEAPON, STAFF_ANY.kind());
        assertEquals(Kind.UNIQUE, KINETIC_ANY.kind());
        assertEquals(Kind.NORMAL, UNBREAKING.kind());
    }

    // ---- W2: each filter of the weapon pool, IN ISOLATION -----------------------------------------------------------

    @Test
    void theWeaponIdFilterAlone() {
        assertEquals(List.of("lantern_any"), ids(EnchantRoll.weaponPoolFor("lantern", Has.EVERY, GearClass.MAGE, ROSTER)));
        assertEquals(List.of("staff_any", "staff_mage", "staff_quiver"),
                ids(EnchantRoll.weaponPoolFor("staff", Has.EVERY, GearClass.MAGE, ROSTER)));
        assertEquals(List.of(), ids(EnchantRoll.weaponPoolFor("iron_sword", Has.EVERY, GearClass.MELEE, ROSTER)),
                "a weapon no enchant names has an empty pool");
    }

    @Test
    void theClassFilterAlone() {
        assertEquals(List.of("staff_any", "staff_quiver"),
                ids(EnchantRoll.weaponPoolFor("staff", Has.EVERY, GearClass.MELEE, ROSTER)));
    }

    @Test
    void theRequiresFilterAlone() {
        assertEquals(List.of("staff_any", "staff_mage"),
                ids(EnchantRoll.weaponPoolFor("staff", Has.NOTHING, GearClass.MAGE, ROSTER)));
    }

    @Test
    void anAccessoryHasNoWeaponPool() {
        assertEquals(List.of(), ids(EnchantRoll.weaponPoolFor("staff", Has.EVERY, GearClass.ACCESSORY, ROSTER)));
    }

    // ---- the three pools are disjoint -------------------------------------------------------------------------------

    @Test
    void noPoolHoldsAnotherKind() {
        for (GearClass c : GearClass.values()) {
            for (Rollable r : EnchantRoll.normalPoolFor(Has.EVERY, c, ROSTER)) {
                assertEquals(Kind.NORMAL, r.kind(), c + " normal pool holds " + r.enchantId());
            }
            for (Rollable r : EnchantRoll.uniquePoolFor(Optional.of("kinetic"), Has.EVERY, c, ROSTER)) {
                assertEquals(Kind.UNIQUE, r.kind(), c + " unique pool holds " + r.enchantId());
            }
            for (Rollable r : EnchantRoll.weaponPoolFor("staff", Has.EVERY, c, ROSTER)) {
                assertEquals(Kind.WEAPON, r.kind(), c + " weapon pool holds " + r.enchantId());
            }
        }
        assertEquals(List.of("attunement", "unbreaking"), ids(EnchantRoll.normalPoolFor(Has.EVERY, GearClass.MAGE, ROSTER)),
                "the control: the normal pool is not simply empty");
    }

    // ---- W2 on whole rolls ------------------------------------------------------------------------------------------

    @Test
    void slotZeroIsTheWeaponsSlotOneUniqueTheLastTwoNormal() {
        EnchantState state = EnchantRoll.roll("staff", Optional.of("kinetic"), Has.EVERY, GearClass.MAGE, ROSTER,
                ALWAYS_FOUR, () -> 0.0);
        assertEquals(4, state.slots().size());
        assertEquals(List.of("staff_any", "staff_mage", "staff_quiver"), ids(state.slots().get(0)));
        assertEquals(List.of("k_any"), ids(state.slots().get(1)));
        assertEquals(List.of("attunement", "unbreaking"), ids(state.slots().get(2)));
        assertEquals(List.of("attunement", "unbreaking"), ids(state.slots().get(3)));
    }

    @Test
    void aWeaponNoEnchantNamesRollsSlotZeroEmpty() {
        EnchantState state = EnchantRoll.roll("iron_sword", Optional.of("kinetic"), Has.EVERY, GearClass.MELEE, ROSTER,
                ALWAYS_FOUR, () -> 0.0);
        assertEquals(List.of(), ids(state.slots().get(0)));
        assertEquals(EnchantSlot.NONE, state.slots().get(0).activeIndex());
        assertEquals(List.of("k_any"), ids(state.slots().get(1)), "the control: the other slots still roll");
    }

    @Test
    void gearThatIsNotAWeaponRollsSlotZeroEmpty() {
        EnchantState state = EnchantRoll.roll("iron_chestplate", Optional.empty(), Has.NOTHING, GearClass.ARMOR, ROSTER,
                ALWAYS_FOUR, () -> 0.0);
        assertEquals(List.of(), ids(state.slots().get(0)));
        assertEquals(List.of(), ids(state.slots().get(1)));
        assertEquals(List.of("unbreaking"), ids(state.slots().get(2)), "the control");
    }

    /** Over 2,000 seeded rolls: every candidate's kind is its slot's kind, and every kind was rolled at least once. */
    @Test
    void overManyRollsEveryCandidateSitsInItsOwnKindOfSlot() {
        Random random = new Random(20261123L);
        int[] filled = new int[3];
        for (int i = 0; i < 2000; i++) {
            String gear = List.of("staff", "lantern", "iron_sword").get(i % 3);
            GearClass c = i % 3 == 2 ? GearClass.MELEE : GearClass.MAGE;
            Optional<String> element = Optional.of(i % 2 == 0 ? "kinetic" : "fire");
            EnchantState state = EnchantRoll.roll(gear, element, Has.EVERY, c, ROSTER, BENS, random::nextDouble);
            for (int s = 0; s < 4; s++) {
                for (EnchantCandidate cand : state.slots().get(s).candidates()) {
                    Rollable r = ROSTER.stream().filter(x -> x.enchantId().equals(cand.enchantId())).findFirst().orElseThrow();
                    assertEquals(EnchantRoll.kindOf(s), r.kind(), gear + " slot " + s + " holds " + cand.enchantId());
                    if (r.isWeaponSpecific()) assertEquals(gear, r.weapon(), "a weapon enchant on another weapon");
                    filled[r.kind().ordinal()]++;
                }
            }
        }
        for (Kind k : Kind.values()) assertTrue(filled[k.ordinal()] > 0, "the control: " + k + " was rolled");
    }

    // ---- W4: the gear's id cannot be skipped -----------------------------------------------------------------------

    @Test
    void aNullOrBlankGearIdIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> EnchantRoll.roll(null, Optional.empty(), Has.EVERY, GearClass.MELEE, ROSTER, BENS, () -> 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> EnchantRoll.roll(" ", Optional.empty(), Has.EVERY, GearClass.MELEE, ROSTER, BENS, () -> 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> EnchantRoll.weaponPoolFor(null, Has.EVERY, GearClass.MELEE, ROSTER));
    }

    @Test
    void aRollableCannotBeBothUniqueAndWeaponSpecific() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Rollable("x", null, Requires.NONE, "kinetic", "staff"));
        assertTrue(e.getMessage().contains("both unique"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new Rollable("x", null, Requires.NONE, null, " "));
    }
}
