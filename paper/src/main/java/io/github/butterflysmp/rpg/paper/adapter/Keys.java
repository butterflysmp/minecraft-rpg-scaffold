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

    /** Marks the crit_chance_boost_TEMP dev item and stores its +crit-chance amount (a DOUBLE) in the PDC. */
    public final NamespacedKey critChanceBoost;

    /** Marks the crit_damage_boost_TEMP dev item and stores its +crit-bonus amount (a DOUBLE) in the PDC. */
    public final NamespacedKey critDamageBoost;

    /**
     * Marks the class_damage_boost_TEMP dev item and stores its bonus (a DOUBLE) in the item's PDC.
     * Paired with {@link #classDamageBoostClass} -- this is the first fixture needing TWO values,
     * because a class-typed grant is meaningless without the class it grants to. An item carrying
     * one and not the other is treated as not ours.
     */
    public final NamespacedKey classDamageBoost;

    /** The {@code WeaponClass} name (a STRING) a class_damage_boost_TEMP boosts. See above. */
    public final NamespacedKey classDamageBoostClass;

    /**
     * A spawned entity's custom-mob id (a STRING), the mob mirror of {@link #weaponId}. An entity
     * carrying this IS one of ours and seeds its stats from content; an entity without it is vanilla
     * and is left entirely alone. Keyed per-ENTITY, never per-type: the Knell is a wither skeleton,
     * and ordinary wither skeletons must stay ordinary.
     */
    public final NamespacedKey mobId;

    /** Reserved opt-out: a mob carrying this (BYTE) PDC gets no health nameplate. For future NPCs/cosmetics. */
    public final NamespacedKey nameplateOptOut;

    /**
     * An item's whole custom-enchant state (a STRING): every slot, every candidate, every unlocked
     * level and every active choice, in one versioned blob. Grammar lives in {@code EnchantCodec}.
     * Per ITEM, never per player -- the enchant is on the weapon, not on whoever holds it.
     *
     * <p>Paired with {@link #enchantRolled}, and note this pair is deliberately NOT the
     * {@link #classDamageBoost} discipline. Those two keys are two halves of ONE value, so an item
     * carrying one and not the other is treated as not ours. These two are INDEPENDENT FACTS: state
     * without the flag is an item enchanted by hand before any roll existed, and the flag without
     * state is a roll that came up empty. Both are legal, and rejecting either would silently
     * discard a player's unlocks -- the exact loss this key exists to prevent.
     */
    public final NamespacedKey enchantData;

    /**
     * Set (BYTE 1) once an item's enchant slots have been ROLLED, even when the roll produced
     * nothing -- so an item can never re-roll on re-insertion and a bad roll cannot be laundered.
     *
     * <p>LOAD-BEARING since the rolls pass: {@code EnchantRollItems.rollOnAcquire} reads it to
     * refuse a second roll, and {@code /rpg enchant} still writes it. The forecast held -- it was
     * already carried across a re-mint, so the roll needed no change to the carry at all.
     *
     * <p>Which is what the whole invariant now rests on: {@code carryEnchants} moves this and
     * {@link #enchantData} as RAW BYTES on every re-mint, so a rolled item stays rolled through a
     * refresh, a rejoin and every enchant-table click, and a player never loses an unlock they paid
     * for.
     */
    public final NamespacedKey enchantRolled;
    /**
     * The armor-bar override: an entity-side {@code armor} modifier that cancels worn armor's
     * native contribution and refills the bar from damage reduction instead. A modifier IDENTITY,
     * like {@link #meleeSuppressor} and unlike the PDC keys around it -- and the first such key that
     * is recomputed on a loop rather than minted once onto an item.
     */
    public final NamespacedKey armorBarOverride;

    /**
     * Identity of the attack-SPEED modifier reconciled onto a PLAYER from their held weapon's
     * authored cadence and their attack-speed stat. Replaces the mint-time pin this key sat beside:
     * a weapon's speed is no longer fixed at mint, because a boost has to be able to move it.
     */
    public final NamespacedKey attackSpeedOverride;

    public Keys(Plugin plugin) {
        this.weaponId = new NamespacedKey(plugin, "weapon_id");
        this.abilityId = new NamespacedKey(plugin, "ability_id");
        this.meleeSuppressor = new NamespacedKey(plugin, "vanilla_melee_suppressor");
        this.soaked = new NamespacedKey(plugin, "soaked_slow");
        this.rooted = new NamespacedKey(plugin, "rooted_immobilize");
        this.freeze = new NamespacedKey(plugin, "freeze_immobilize");
        this.healthBoost = new NamespacedKey(plugin, "health_boost_temp");
        this.attackSpeedBoost = new NamespacedKey(plugin, "attack_speed_boost_temp");
        this.critChanceBoost = new NamespacedKey(plugin, "crit_chance_boost_temp");
        this.critDamageBoost = new NamespacedKey(plugin, "crit_damage_boost_temp");
        this.classDamageBoost = new NamespacedKey(plugin, "class_damage_boost_temp");
        this.classDamageBoostClass = new NamespacedKey(plugin, "class_damage_boost_temp_class");
        this.mobId = new NamespacedKey(plugin, "mob_id");
        this.nameplateOptOut = new NamespacedKey(plugin, "nameplate_opt_out");
        this.enchantData = new NamespacedKey(plugin, "enchant_data");
        this.enchantRolled = new NamespacedKey(plugin, "enchant_rolled");
        this.armorBarOverride = new NamespacedKey(plugin, "armor_bar_override");
        this.attackSpeedOverride = new NamespacedKey(plugin, "attack_speed_override");
    }
}
