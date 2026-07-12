package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central home for every NamespacedKey. Never construct them inline.
 *
 * Instance-based rather than static: NamespacedKey(String, String) is
 * deprecated, and the supported constructor needs the owning Plugin, which a
 * static initialiser cannot reach. Build one of these once in RpgPlugin and
 * pass it down. The namespace it produces is the plugin name, lowercased.
 */
public final class Keys {

    public final NamespacedKey weaponId;
    public final NamespacedKey abilityId;

    /** Identity of the attack-damage modifier that cancels a weapon's vanilla melee. */
    public final NamespacedKey meleeSuppressor;

    /** Identity of Soaked's movement-speed modifier, so it can be removed by key on expiry. */
    public final NamespacedKey soaked;

    /** Identity of Rooted's movement-speed=0 modifier, the immobilize's AI-drive kill. */
    public final NamespacedKey rooted;

    /** Identity of Freeze's movement-speed=0 modifier -- distinct from rooted so both coexist. */
    public final NamespacedKey freeze;

    public Keys(Plugin plugin) {
        this.weaponId = new NamespacedKey(plugin, "weapon_id");
        this.abilityId = new NamespacedKey(plugin, "ability_id");
        this.meleeSuppressor = new NamespacedKey(plugin, "vanilla_melee_suppressor");
        this.soaked = new NamespacedKey(plugin, "soaked_slow");
        this.rooted = new NamespacedKey(plugin, "rooted_immobilize");
        this.freeze = new NamespacedKey(plugin, "freeze_immobilize");
    }
}
