package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.ClassDamageModifiers;
import io.github.butterflysmp.rpg.core.weapon.ClassDamageModifiers.ClassGrant;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The class-typed gate: which equipped "+N <Class> Damage" grants are ACTIVE, given what the caster
 * is holding. This is the pure half of the modifier, and it is in core precisely because its paper
 * counterpart cannot be unit-tested -- scanning equipment needs a live Bukkit Player, which is why
 * neither HealthModifierItems nor AttackSpeedModifierItems has a test either.
 *
 * Each test names the mutation it forces red.
 */
class ClassDamageModifiersTest {

    private static final double EPS = 1e-9;

    /**
     * The headline rule: a grant counts only while a weapon of ITS class is held. Both grants are
     * equipped here and only one survives, so the test cannot pass by returning everything OR by
     * returning nothing -- it pins the filter itself.
     */
    @Test
    void onlyGrantsMatchingTheHeldWeaponsClassAreActive() {
        Map<String, ClassGrant> equipped = Map.of(
                "OFF_HAND", new ClassGrant(WeaponClass.MAGE, 5.0),
                "HEAD", new ClassGrant(WeaponClass.MELEE, 3.0));

        Map<String, Double> active = ClassDamageModifiers.matching(WeaponClass.MAGE, equipped);

        assertEquals(1, active.size(), "the melee grant is inert while a mage weapon is held");
        assertEquals(5.0, active.get("OFF_HAND"), EPS);
        assertNull(active.get("HEAD"), "a +Melee source must not reach a staff");
        // Mutation: drop the class equality check -> both land, size 2 -> reddens.
    }

    /** The mirror: swap the held class and the OTHER grant is the one that lives. */
    @Test
    void swappingTheHeldWeaponSwapsWhichGrantIsActive() {
        Map<String, ClassGrant> equipped = Map.of(
                "OFF_HAND", new ClassGrant(WeaponClass.MAGE, 5.0),
                "HEAD", new ClassGrant(WeaponClass.MELEE, 3.0));

        Map<String, Double> onASword = ClassDamageModifiers.matching(WeaponClass.MELEE, equipped);

        assertEquals(Map.of("HEAD", 3.0), onASword,
                "the same gear selects a different grant once a sword is in hand");
    }

    /**
     * A RANGER weapon with only mage/melee gear equipped gets nothing. Not zero -- ABSENT, so the
     * reconciler REMOVES those sources rather than leaving 0-valued modifiers parked on the stat.
     */
    @Test
    void aHeldClassWithNoMatchingGearYieldsNoSourcesAtAll() {
        Map<String, ClassGrant> equipped = Map.of(
                "OFF_HAND", new ClassGrant(WeaponClass.MAGE, 5.0),
                "HEAD", new ClassGrant(WeaponClass.MELEE, 3.0));

        assertTrue(ClassDamageModifiers.matching(WeaponClass.RANGER, equipped).isEmpty(),
                "+Magic and +Melee are both inert on a bow, and contribute no source");
        // Mutation: map non-matching grants to 0.0 instead of dropping them -> the map is non-empty,
        // the reconciler keeps two dead sources alive -> reddens.
    }

    /**
     * THE UNARMED INVARIANT. A null held class -- empty hand, or an item that is not one of ours --
     * activates nothing. This is what makes "a class bonus cannot resurrect an unarmed hit"
     * STRUCTURAL rather than a convention someone has to remember downstream: weapon-only melee is
     * preserved here, before any damage arm ever sees a number.
     */
    @Test
    void anEmptyHandActivatesNothingHoweverMuchGearIsWorn() {
        Map<String, ClassGrant> equipped = Map.of(
                "OFF_HAND", new ClassGrant(WeaponClass.MAGE, 5.0),
                "HEAD", new ClassGrant(WeaponClass.MELEE, 3.0),
                "CHEST", new ClassGrant(WeaponClass.RANGER, 9.0));

        assertTrue(ClassDamageModifiers.matching(null, equipped).isEmpty(),
                "no weapon means no class means no bonus -- unarmed still deals nothing");
        // Mutation: treat null as "matches everything" -> a bare-handed player gains 17 -> reddens.
    }

    /**
     * Two slots granting the SAME class both survive, under their own source keys. They are not
     * merged here: Stat sums them, and keeping them separate is what lets the reconciler drop one
     * without disturbing the other.
     */
    @Test
    void twoSlotsGrantingTheSameClassBothSurviveAsDistinctSources() {
        Map<String, ClassGrant> equipped = Map.of(
                "OFF_HAND", new ClassGrant(WeaponClass.RANGER, 5.0),
                "FEET", new ClassGrant(WeaponClass.RANGER, 2.0));

        Map<String, Double> active = ClassDamageModifiers.matching(WeaponClass.RANGER, equipped);

        assertEquals(2, active.size(), "two sources, not one merged total");
        assertEquals(5.0, active.get("OFF_HAND"), EPS);
        assertEquals(2.0, active.get("FEET"), EPS);
    }

    /** No gear at all is simply no sources -- the common case, and it must not throw. */
    @Test
    void noGearYieldsNoSources() {
        assertTrue(ClassDamageModifiers.matching(WeaponClass.MELEE, Map.of()).isEmpty());
    }
}
