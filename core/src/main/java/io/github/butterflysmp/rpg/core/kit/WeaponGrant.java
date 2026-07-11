package io.github.butterflysmp.rpg.core.kit;

/**
 * A weapon a kit hands out, and whether to put it in the player's hand on selection.
 *
 * {@code equip} closes the milestone-1 feedback failure: a fresh Ranger who has to dig a
 * bow out of a random inventory slot swings nothing and thinks the class is broken.
 * Auto-equipping the primary weapon makes the class playable the instant it is chosen.
 */
public record WeaponGrant(String weaponId, boolean equip) {
    public WeaponGrant {
        if (weaponId == null || weaponId.isBlank()) {
            throw new IllegalArgumentException("weapon grant needs a weapon id");
        }
    }
}
