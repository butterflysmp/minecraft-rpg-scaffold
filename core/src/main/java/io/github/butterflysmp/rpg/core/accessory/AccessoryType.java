package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.weapon.WeaponClass;

import java.util.Locale;

/**
 * The kind of a CLASS accessory, and the one class each kind belongs to: melee wears Gauntlets, a
 * ranger a Quiver, a mage a Scroll. Universal accessories have no type.
 *
 * <p><b>{@link #QUIVER} is a player-facing word and a content token, never an internal key.</b>
 * Ruling A2: the Ranger accessory is called "Quiver" and the magazine keeps its name too, so the
 * word means two things to a player -- accepted. What A2 forbids is a COLLISION of internal
 * identifiers, so no PDC key, stat source prefix or notice id built for accessories contains
 * {@code quiver}. This enum constant is none of those.
 *
 * <p>The pairing is ONE table, here, rather than a rule restated in the loader: a type names its
 * class, and the loader refuses a file whose {@code class:} disagrees with it.
 */
public enum AccessoryType {
    GAUNTLET(WeaponClass.MELEE),
    QUIVER(WeaponClass.RANGER),
    SCROLL(WeaponClass.MAGE),
    /**
     * The Healing Tome's type (the seat's H2, 2026-11-26). READ: this word is never shown to a player -- it is read only
     * by {@code AccessoryLoader} (a content token, and its own refusal message) and by {@code AccessoryDefinition}'s
     * class-pairing check. So a second MAGE type is a content token and a pairing, nothing more: two types may name one
     * class, and the one table still refuses a class/type mismatch ({@code AccessoryDefinitionTest}).
     */
    TOME(WeaponClass.MAGE);

    private final WeaponClass weaponClass;

    AccessoryType(WeaponClass weaponClass) {
        this.weaponClass = weaponClass;
    }

    /** The one class that wears this type. */
    public WeaponClass weaponClass() {
        return weaponClass;
    }

    /** Case-insensitive lookup for the loader. Null on a miss, so the caller names the file. */
    public static AccessoryType fromName(String name) {
        if (name == null) return null;
        for (AccessoryType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    /** The content token, as authored. */
    public String token() {
        return name().toLowerCase(Locale.ROOT);
    }
}
