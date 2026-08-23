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

    /** Marks the health_boost_TEMP dev item and stores its +max-HP amount (a DOUBLE) in the item's PDC. */
    public final NamespacedKey healthBoost;

    /** Marks the attack_speed_boost_TEMP dev item and stores its attack-speed bonus (a DOUBLE) in the PDC. */
    public final NamespacedKey attackSpeedBoost;

    /**
     * A spawned entity's custom-mob id (a STRING), the mob mirror of {@link #weaponId}. An entity
     * carrying this IS one of ours and seeds its stats from content; an entity without it is vanilla
     * and is left entirely alone. Keyed per-ENTITY, never per-type: the Knell is a wither skeleton,
     * and ordinary wither skeletons must stay ordinary.
     */
    public final NamespacedKey mobId;

    /** Reserved opt-out: a mob carrying this (BYTE) PDC gets no health nameplate. For future NPCs/cosmetics. */
    public final NamespacedKey nameplateOptOut;

    public Keys(Plugin plugin) {
        this.weaponId = new NamespacedKey(plugin, "weapon_id");
        this.abilityId = new NamespacedKey(plugin, "ability_id");
        this.meleeSuppressor = new NamespacedKey(plugin, "vanilla_melee_suppressor");
        this.soaked = new NamespacedKey(plugin, "soaked_slow");
        this.rooted = new NamespacedKey(plugin, "rooted_immobilize");
        this.freeze = new NamespacedKey(plugin, "freeze_immobilize");
        this.healthBoost = new NamespacedKey(plugin, "health_boost_temp");
        this.attackSpeedBoost = new NamespacedKey(plugin, "attack_speed_boost_temp");
        this.mobId = new NamespacedKey(plugin, "mob_id");
        this.nameplateOptOut = new NamespacedKey(plugin, "nameplate_opt_out");
    }
}
