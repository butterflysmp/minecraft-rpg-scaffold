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

    public final NamespacedKey shieldElement;
    public final NamespacedKey weaponId;
    public final NamespacedKey abilityId;

    public Keys(Plugin plugin) {
        this.shieldElement = new NamespacedKey(plugin, "shield_element");
        this.weaponId = new NamespacedKey(plugin, "weapon_id");
        this.abilityId = new NamespacedKey(plugin, "ability_id");
    }
}
