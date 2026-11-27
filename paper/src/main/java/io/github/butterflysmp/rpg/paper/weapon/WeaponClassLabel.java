package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.WeaponClass;

/**
 * The class -> display label mapping, the one place a {@link WeaponClass} becomes tooltip words.
 * One exhaustive switch with no default arm (like {@code RarityColors}), so when SUMMONER lands it
 * is a compile error here until it is given a label. Used for both the per-ability damage label
 * ("Magic Damage") and the weapon-type footer ("... Magic Weapon").
 */
public final class WeaponClassLabel {

    private WeaponClassLabel() {}

    public static String of(WeaponClass weaponClass) {
        return switch (weaponClass) {
            case MELEE  -> "Melee";
            case RANGER -> "Ranged";
            case MAGE   -> "Magic";
            case SUMMONER -> "Summoner";   // 2026-11-24: "Common Summoner Weapon"
        };
    }

    /**
     * THE ONE LABEL OF EVERY DAMAGE LINE ON A WEAPON TOOLTIP: {@code "Magic Damage: "}, by the weapon's class (Ben,
     * 2026-11-26, TT19: "I want the damage type in the tool tip to match the class not the element. Ie Ranged Damage,
     * Magic Damage, Melee Damage etc"). The basic attack's stat line, every ability's damage line, every behaviour-block
     * row and every granted trigger's line ask this, and nothing else. It replaced the ELEMENT label on ability lines
     * ("Fire Damage: "); the element line at the top of the tooltip is unchanged. No shipped summoner weapon renders a
     * damage line (READ, 2026-11-26: the Bone Whistle's two blocks carry none), so "Summoner Damage: " is unreached.
     */
    public static String damageLabel(WeaponClass weaponClass) {
        return of(weaponClass) + " Damage: ";
    }
}
