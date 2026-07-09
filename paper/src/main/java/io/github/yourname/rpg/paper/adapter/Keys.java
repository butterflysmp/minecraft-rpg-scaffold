package io.github.yourname.rpg.paper.adapter;

import org.bukkit.NamespacedKey;

/** Central home for every NamespacedKey. Never construct them inline. */
public final class Keys {
    private Keys() {}

    private static final String NS = "rpg";

    public static final NamespacedKey SHIELD_ELEMENT = new NamespacedKey(NS, "shield_element");
    public static final NamespacedKey WEAPON_ID      = new NamespacedKey(NS, "weapon_id");
    public static final NamespacedKey ABILITY_ID     = new NamespacedKey(NS, "ability_id");
}
