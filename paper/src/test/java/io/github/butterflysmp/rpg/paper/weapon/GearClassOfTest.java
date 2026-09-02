package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolKind;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which enchant-roll class each gear kind draws from.
 *
 * <p>This decision had FOUR copies before it was extracted -- {@code /rpg give}'s three arms and the
 * kit grant -- and mint-on-craft would have made five. Each copy independently decided that a shield
 * draws {@code SHIELD} and a piece of armor draws {@code ARMOR}; a copy that got it wrong would roll
 * Bulwark onto a helmet, and nothing would throw.
 *
 * <p>The method touches no Bukkit type, so it is testable even though it lives in {@code paper}.
 *
 * <p>Each test names the mutation it forces red.
 */
class GearClassOfTest {

    private static WeaponDefinition weapon(WeaponClass weaponClass) {
        return new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, weaponClass,
                WeaponDefinition.DEFAULT_MATERIAL, 0.0, 0.0, 0.0,
                List.of(new TriggerBinding("left_click",
                        new AbilityDefinition("w/left_click", "W", "kinetic", "none", 0,
                                null, null, List.of(), List.of()))),
                List.of());
    }

    @Test
    void eachKindDrawsFromItsOwnRoster() {
        assertEquals(GearClass.SHIELD, GearItems.gearClassOf(
                new ShieldDefinition("s", "S", Rarity.COMMON, "shield", 0.35, List.of())));

        assertEquals(GearClass.ARMOR, GearItems.gearClassOf(
                new ArmorDefinition("a", "A", Rarity.COMMON, "iron_helmet", ArmorSlot.HEAD, 2.0,
                        List.of())));

        assertEquals(GearClass.TOOL, GearItems.gearClassOf(
                new ToolDefinition("iron_pickaxe", "T", Rarity.COMMON, "iron_pickaxe",
                        ToolKind.PICKAXE, List.of())));
        // Mutation: return SHIELD for the armor arm -> reddens. That mutation rolls Bulwark and
        // Thorns onto a helmet, which is a real balance defect that throws nothing.
        //
        // Mutation: return ARMOR for the TOOL arm -> reddens. That is the exact silent failure the
        // HeldGear/PlacedGear collapse was made to delete -- their old `shield != null ? SHIELD :
        // ARMOR` tail would have returned ARMOR for a tool, offering a pickaxe Protection, Growth
        // and Mana Bank: enchants that can never fire on it, sold for XP.
    }

    @Test
    void everyGearKindResolvesToADISTINCTRoster() {
        // The axis, not the cases that came to mind. Two kinds sharing a roster is the defect shape
        // here -- it is what "pass SHIELD to make armor work" would have been -- so distinctness is
        // asserted rather than left implied by four equality checks.
        List<GearClass> rosters = List.of(
                GearItems.gearClassOf(weapon(WeaponClass.MELEE)),
                GearItems.gearClassOf(new ShieldDefinition("s", "S", Rarity.COMMON, "shield", 0.35,
                        List.of())),
                GearItems.gearClassOf(new ArmorDefinition("a", "A", Rarity.COMMON, "iron_helmet",
                        ArmorSlot.HEAD, 2.0, List.of())),
                GearItems.gearClassOf(new ToolDefinition("iron_pickaxe", "T", Rarity.COMMON,
                        "iron_pickaxe", ToolKind.PICKAXE, List.of())));

        assertEquals(4, rosters.size(), "the walk must not be empty or short");
        assertEquals(rosters.size(), Set.copyOf(rosters).size(),
                "two gear kinds collapsed to one roster: " + rosters);
        // Mutation: point the tool arm at ARMOR -> reddens on the distinctness check as well as
        // above, so the guard survives someone deleting the literal assertion.
    }

    @Test
    void aWeaponDrawsFromITSOWNClassNotAFixedOne() {
        // The arm that is not a constant. A weapon's roster follows its declared class, so a
        // hardcoded MELEE would draw melee enchants onto a bow and a staff.
        assertEquals(GearClass.of(WeaponClass.MELEE), GearItems.gearClassOf(weapon(WeaponClass.MELEE)));
        assertEquals(GearClass.of(WeaponClass.RANGER), GearItems.gearClassOf(weapon(WeaponClass.RANGER)));
        assertEquals(GearClass.of(WeaponClass.MAGE), GearItems.gearClassOf(weapon(WeaponClass.MAGE)));

        assertNotEquals(GearItems.gearClassOf(weapon(WeaponClass.MELEE)),
                GearItems.gearClassOf(weapon(WeaponClass.MAGE)),
                "two weapon classes must not collapse to one roster");
        // Mutation: return a fixed GearClass for the weapon arm -> the last assertion reddens.
    }

    @Test
    void everyWeaponClassResolvesToSomething() {
        // The axis, not the cases that came to mind. A new WeaponClass constant reaches this loop
        // the day it exists, and GearClass.of would have to answer for it.
        for (WeaponClass weaponClass : WeaponClass.values()) {
            assertNotNull(GearItems.gearClassOf(weapon(weaponClass)),
                    weaponClass + " resolved to no roster");
        }
        // Mutation: make GearClass.of return null for one constant -> reddens naming it.
    }
}
