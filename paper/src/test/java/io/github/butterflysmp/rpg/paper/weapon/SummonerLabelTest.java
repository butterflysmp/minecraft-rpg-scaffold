package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE SUMMONER'S LABEL (the seat's PR 1, 2026-11-24): "Summoner Weapon" in the form the others use. The tooltip footer is
 * {@code WeaponClassLabel.of(class) + " Weapon"} ({@code WeaponLore}), after the rarity: "Common Summoner Weapon".
 */
class SummonerLabelTest {

    @Test
    void theSummonerIsLabelledInTheFormTheOthersUse() {
        assertEquals("Summoner Weapon", WeaponClassLabel.of(WeaponClass.SUMMONER) + " Weapon");
        assertEquals("Magic Weapon", WeaponClassLabel.of(WeaponClass.MAGE) + " Weapon", "the form, from a sibling");
        assertEquals("Summoner", GearClassLabel.of(GearClass.SUMMONER));
        assertEquals("a Summoner weapon", GearClassLabel.describe(GearClass.SUMMONER));
        assertEquals("a Summoner enchant", GearClassLabel.describeEnchant(GearClass.SUMMONER));
    }
}
