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
        };
    }
}
