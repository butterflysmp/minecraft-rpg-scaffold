package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The enchant-gating axis, and specifically the two properties that keep it from drifting away from
 * {@link WeaponClass}: {@link GearClass#of} is TOTAL over every weapon class, and it can never
 * produce {@link GearClass#SHIELD}.
 *
 * <p>{@code WeaponClassTest} is the sibling and stays as it is -- it guards that {@code WeaponClass}
 * has NOT grown a SHIELD constant, which is the other half of the same decision.
 */
class GearClassTest {

    @Test
    void everyWeaponClassMapsToAGearClassSoTheAxesCannotDriftApart() {
        // TOTAL, by construction rather than by listing three cases: the day SUMMONER lands, `of`
        // stops compiling until it is given an arm, and this loop covers it the moment it does.
        for (WeaponClass weaponClass : WeaponClass.values()) {
            assertNotNull(GearClass.of(weaponClass),
                    weaponClass + " has no gear class -- the enchant gate cannot see this weapon");
        }
    }

    @Test
    void theMappingIsNameForNameSoAContentTokenMeansTheSameThingOnBothAxes() {
        // `class: melee` on a weapon file and `class: melee` on an enchant file are parsed by two
        // different enums now. If those ever disagreed about what "melee" is, a Sharpness would
        // silently stop matching the swords it was written for.
        for (WeaponClass weaponClass : WeaponClass.values()) {
            assertEquals(weaponClass.name(), GearClass.of(weaponClass).name(),
                    "the two axes must agree on the spelling of " + weaponClass);
        }
    }

    @Test
    void noWeaponEverMapsToShield() {
        // The load-bearing direction. A weapon that presented SHIELD would be offered Bulwark in its
        // roll and would read a block-DR enchant off a stack that cannot block.
        for (WeaponClass weaponClass : WeaponClass.values()) {
            assertNotEquals(GearClass.SHIELD, GearClass.of(weaponClass),
                    weaponClass + " mapped to SHIELD -- only a shield may present that");
        }
    }

    @Test
    void anEmptyHandHasNoGearClassRatherThanADefaultOne() {
        // Null in, null out. This null IS the universal gate, so it flows into DamageEnchants'
        // existing null arm unchanged. A default here -- MELEE, say -- would grant every melee
        // enchant's percent to an empty hand.
        assertNull(GearClass.of(null));
    }

    @Test
    void fromNameIsCaseInsensitiveAndKnowsTheShieldToken() {
        assertEquals(GearClass.MELEE, GearClass.fromName("melee"));
        assertEquals(GearClass.RANGER, GearClass.fromName("RANGER"));
        assertEquals(GearClass.MAGE, GearClass.fromName("Mage"));
        // The one the content schema gains this slice.
        assertEquals(GearClass.SHIELD, GearClass.fromName("shield"));
        assertEquals(GearClass.SHIELD, GearClass.fromName("SHIELD"));
    }

    @Test
    void fromNameIsNullOnAMissSoTheLoaderDecidesWhatABadTokenMeans() {
        assertNull(GearClass.fromName("bogus"));
        assertNull(GearClass.fromName(null));
        assertNull(GearClass.fromName("summoner"), "not a class until it has mechanics");
        // "ranged" is the tooltip LABEL, never the token -- the exact typo the weapon schema records.
        assertNull(GearClass.fromName("ranged"), "the token is 'ranger'; 'Ranged' is only the label");
        // `universal` is NOT a constant here. The loader tests that token before it gets this far,
        // and a null return is how "no gate" and "bad token" both arrive -- which is why the loader
        // must check universal FIRST rather than treating every null alike.
        assertNull(GearClass.fromName("universal"));
    }

    @Test
    void theAxisIsExactlyTheThreeFightingClassesPlusShieldAndArmor() {
        // A count, so adding a constant is a visible decision here rather than a silent widening of
        // every enchant gate in the game.
        //
        // IT IS ALSO NEARLY THE ONLY THING THAT NOTICES. Adding ARMOR produced exactly TWO compile
        // errors -- GearClassLabel.of and describe -- and this one runtime failure. Five further
        // sites changed SILENTLY and had to be found by hand: HeldGear.gearClass, HeldGear's effect
        // tail, the /rpg enchant SHOW refusal, EnchantMenu.PlacedGear.gearClass (a two-way ternary
        // that would have minted a helmet as a shield), and EnchantDefinition's ANY_BUT_SHIELD gate.
        // The repo's own comments claimed the compiler covered this. It does not; it covers three.
        assertEquals(5, GearClass.values().length);
    }

    @Test
    void noWeaponEverMapsToArmorEither() {
        // The twin of noWeaponEverMapsToShield, and it guards the same direction: a weapon that
        // presented ARMOR would be offered Protection and Growth in its roll, and would read a
        // defense enchant off a stack nobody is wearing.
        for (WeaponClass weaponClass : WeaponClass.values()) {
            assertNotEquals(GearClass.ARMOR, GearClass.of(weaponClass),
                    weaponClass + " mapped to ARMOR -- only a piece of armor may present that");
        }
    }

    @Test
    void fromNameKnowsTheArmorTokenSoContentCanGateOnIt() {
        assertEquals(GearClass.ARMOR, GearClass.fromName("armor"));
        assertEquals(GearClass.ARMOR, GearClass.fromName("ARMOR"));
        // fromName is a values() loop, so it began accepting this token the instant the constant
        // existed -- no code change and no compiler prompt. Pinned so that is a decision, not a
        // side effect somebody discovers from a content file that unexpectedly loaded.
        assertNull(GearClass.fromName("armour"), "the token is US-spelled, matching the enum");
    }
}
