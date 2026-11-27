package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY DAMAGE LINE IS LABELLED BY THE WEAPON'S CLASS (Ben, 2026-11-26, TT19: "I want the damage type in the tool tip to
 * match the class not the element. Ie Ranged Damage, Magic Damage, Melee Damage etc"). One function,
 * {@link WeaponClassLabel#damageLabel}, labels the basic attack's stat line, every ability's damage line, every
 * behaviour-block row and every granted trigger's line. The element line at the top of the tooltip is unchanged.
 */
class ClassDamageLabelTest {

    @Test
    void theLabelIsTheClassesWord() {
        assertEquals("Melee Damage: ", WeaponClassLabel.damageLabel(WeaponClass.MELEE));
        assertEquals("Ranged Damage: ", WeaponClassLabel.damageLabel(WeaponClass.RANGER));
        assertEquals("Magic Damage: ", WeaponClassLabel.damageLabel(WeaponClass.MAGE));
        assertEquals("Summoner Damage: ", WeaponClassLabel.damageLabel(WeaponClass.SUMMONER));
    }

    @Test
    void anAbilityABehaviourRowAndAGrantedLineAllTakeTheClassLabel() {
        TrueTooltipsConsistencyTest.Content c = TrueTooltipsConsistencyTest.load();
        assertEquals(List.of("Magic Damage: 32"), damageLines(c, "flint_staff", "large_sparks", 3));
        assertEquals(List.of("Magic Damage: 162"), damageLines(c, "cursed_emerald", "focused_beam", 2));
        assertEquals(List.of("Ranged Damage: 85", "Ranged Damage: 40 x 4"), damageLines(c, "breacher", "shatter_caps", 3));
        assertEquals(List.of("Magic Damage: 45", "Magic Damage: 35 x 3"), damageLines(c, "living_lantern", "airburst_flames", 3));
        assertEquals(List.of("Magic Damage: 21", "Magic Damage: 14.7"), damageLines(c, "lapis_staff", "lapis_crystal", 3));
        assertEquals(List.of("Melee Damage: 23"), damageLines(c, "iron_sword", null, 0));
    }

    @Test
    void theElementLineAtTheTopIsUnchanged() {
        TrueTooltipsConsistencyTest.Content c = TrueTooltipsConsistencyTest.load();
        WeaponDefinition flint = find(c, "flint_staff");
        List<String> lore = TrueTooltipsConsistencyTest.plain(WeaponLore.build(flint, c.elements()));
        assertEquals("Fire", lore.get(0));
        assertTrue(lore.stream().noneMatch(l -> l.startsWith("Fire Damage")), lore.toString());
    }

    static List<String> damageLines(TrueTooltipsConsistencyTest.Content c, String weapon, String enchant, int level) {
        WeaponDefinition w = find(c, weapon);
        var state = enchant == null ? io.github.butterflysmp.rpg.core.enchant.EnchantState.empty()
                : TrueTooltipsConsistencyTest.activeAt(enchant, level);
        return TrueTooltipsConsistencyTest.plain(WeaponLore.build(w, c.elements(), OptionalInt.empty(), OptionalInt.empty(),
                        OptionalInt.empty(), ItemEnchantTermsReader.of(state, w, c.enchants()))).stream()
                .filter(l -> l.contains(" Damage: ")).toList();
    }

    private static WeaponDefinition find(TrueTooltipsConsistencyTest.Content c, String id) {
        return c.weapons().stream().filter(w -> w.id().equals(id)).findFirst().orElseThrow();
    }
}
