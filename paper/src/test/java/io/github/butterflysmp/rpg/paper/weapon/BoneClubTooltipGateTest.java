package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GATE-bone-club BC1 and DB1's tooltips, WHOLE, EXECUTED through the shipped assembly (WeaponItems.applyLore's own three
 * calls: WeaponLore.build over the item's enchant terms, then EnchantLore.applied with EnchantLore.lines) at score 100.
 * The gate quotes these lists line for line (2026-11-26).
 */
class BoneClubTooltipGateTest {

    private static List<String> lore(EnchantState state) {
        TrueTooltipsConsistencyTest.Content c = TrueTooltipsConsistencyTest.load();
        WeaponDefinition club = c.weapons().stream().filter(w -> w.id().equals("bone_club")).findFirst().orElseThrow();
        var base = WeaponLore.build(club, c.elements(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(100),
                ItemEnchantTermsReader.of(state, club, c.enchants()));
        return TrueTooltipsConsistencyTest.plain(EnchantLore.applied(base, EnchantLore.lines(state, c.enchants())));
    }

    /** BC1: the bare club at score 100. No crit line, no tier line, no flavor. */
    @Test
    void theBareClub() {
        assertEquals(List.of("Kinetic", "Gear Score: 100", "", "Melee Damage: 44", "Attack Speed: 0.8", "",
                "Common Melee Weapon"), lore(EnchantState.empty()));
    }

    /** DB1: with Drumming Beat III active, the enchant line on top and NOT ONE stat line moved (T8). */
    @Test
    void withDrummingBeatThree() {
        List<String> three = lore(TrueTooltipsConsistencyTest.activeAt("drumming_beat", 3));
        assertEquals(List.of("Drumming Beat III", "", "Kinetic", "Gear Score: 100", "", "Melee Damage: 44",
                "Attack Speed: 0.8", "", "Common Melee Weapon"), three);
        assertEquals(lore(EnchantState.empty()), three.subList(2, three.size()), "the bare tooltip, unmoved, under it");
    }
}
