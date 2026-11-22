package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE AMMUNITION IS PER WEAPON (the seat's ruling 1, 2026-11-22, the Breacher's brief): one key, {@code ammo:}, DEFAULT
 * arrow. Core holds the token; paper resolves it ({@code QuiverAmmo.ammoOf}) and plans the reload with it.
 */
class WeaponAmmoTest {

    private static List<TriggerBinding> fire() {
        return List.of(new TriggerBinding("right_click", new AbilityDefinition("w/right_click", "Fire", "kinetic", 0,
                null, null, List.of(), List.of())));
    }

    private static WeaponDefinition quiverWeapon(String ammo) {
        return new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.RANGER, "crossbow", 0.0, 0.0, 0.0,
                2, 60, fire(), List.of(), Optional.empty(), false, ammo);
    }

    /** Absent is the arrow, so every file written before the key reloads exactly as it did. */
    @Test
    void anAbsentAmmoIsTheArrow() {
        assertEquals("arrow", WeaponDefinition.DEFAULT_AMMO);
        assertEquals(WeaponDefinition.DEFAULT_AMMO, quiverWeapon(null).ammo());
        // Every constructor that predates the key, quiver or not:
        assertEquals("arrow", new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.RANGER, "crossbow",
                0.0, 0.0, 0.0, 2, 60, fire(), List.of()).ammo());
        assertEquals("arrow", new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.RANGER, "crossbow",
                0.0, 0.0, 0.0, 2, 60, fire(), List.of(), Optional.empty(), false).ammo());
        assertEquals("arrow", new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, fire()).ammo());
    }

    /** A named ammo is kept, normalised as a material token is: lower case, the vanilla namespace dropped. */
    @Test
    void aNamedAmmoIsKeptAsAToken() {
        assertEquals("cobblestone", quiverWeapon("cobblestone").ammo());
        assertEquals("cobblestone", quiverWeapon(" minecraft:Cobblestone ").ammo());
    }

    /** An item to load and nothing to load it into: refused by name, as reload_ticks without a quiver is. */
    @Test
    void ammoWithNoQuiverIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new WeaponDefinition("w", "W",
                "kinetic", Rarity.COMMON, WeaponClass.RANGER, "crossbow", 0.0, 0.0, 0.0, 0, 0, fire(), List.of(),
                Optional.empty(), false, "cobblestone"));
        assertTrue(e.getMessage().contains("declares ammo 'cobblestone' but no quiver_size"), e.getMessage());
    }

    @Test
    void aBlankAmmoIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> quiverWeapon("  "));
        assertTrue(e.getMessage().contains("blank ammo"), e.getMessage());
    }
}
