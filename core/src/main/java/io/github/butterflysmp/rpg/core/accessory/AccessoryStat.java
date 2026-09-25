package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;

import java.util.Locale;

/**
 * The stats an accessory may modify -- the WHITELIST from {@code PLAN-accessories.md} §3.5, and
 * nothing else. The content token is the lowercase name ({@code crit_chance: 0.05}); an unknown
 * key refuses the file.
 *
 * <p>Each constant names the existing scanner it is MERGED with in {@code PlayerHealthSystem}'s
 * reconcile loop, because {@code ModifierReconciler.reconcile} removes every source absent from the
 * map it is handed: an accessory scan reconciled separately would wipe that scanner's sources, or be
 * wiped by them. The merge happens BEFORE the one reconcile call, never after.
 *
 * <h2>Excluded, and why -- stated here because an absent constant explains nothing</h2>
 *
 * <ul>
 *   <li><b>attack damage</b> -- a player's attack base is 0 BY DESIGN (weapon-only melee,
 *       {@code CombatantStats.register}). A flat accessory bonus would make an unarmed punch deal
 *       damage. {@link #CLASS_DAMAGE} is the route that respects this: it applies only while a
 *       weapon of the matching class is held.
 *   <li><b>attack speed</b> -- the 4-tick input grid swallows small bonuses (the arithmetic half of
 *       NAME THE QUANTITY).
 *   <li><b>enchant damage percent</b> -- enchant-owned, and accessories take no part in enchanting
 *       (ruling A4).
 *   <li><b>quiver size, reload time</b> -- ruling Q6: the eligibility half fails. The class slot is
 *       gated on the PROFILE class, so a ranger holding a weapon with no magazine would wear a
 *       "+2 arrows" that does nothing.
 * </ul>
 *
 * <h2>Negatives: {@link #negatives()}</h2>
 *
 * A drawback is allowed only where it is guaranteed to be a real reduction. Ruling Q7: max HP has no
 * floor, so a negative there is refused outright; other stats are either refused (their player base
 * is 0, or the composition below zero was never traced) or BOUNDED -- see
 * {@link AccessoryNegatives}.
 */
public enum AccessoryStat {

    /** Merged with {@code HealthModifierItems} + {@code GrowthModifierItems}. No floor exists. */
    MAX_HEALTH(StatsSheetLines.MAX_HEALTH_LABEL, Negatives.REFUSED),

    /** Merged with {@code ManaBankModifierItems}, into the first map {@code ManaTransition} takes. */
    MAX_MANA(StatsSheetLines.MAX_MANA_LABEL, Negatives.BOUNDED),

    /** Merged with {@code ManaRegenModifierItems}, into the second map of the same call. */
    MANA_REGEN(StatsSheetLines.MANA_REGEN_LABEL, Negatives.REFUSED),

    /** Merged with {@code CritModifierItems}' chance scan. */
    CRIT_CHANCE(StatsSheetLines.CRIT_CHANCE_LABEL, Negatives.BOUNDED),

    /** Merged with {@code CritModifierItems}' damage scan. */
    CRIT_DAMAGE(StatsSheetLines.CRIT_DAMAGE_LABEL, Negatives.BOUNDED),

    /** Merged with {@code HealthRegenModifierItems}. */
    HEALTH_REGEN(StatsSheetLines.HEALTH_REGEN_LABEL, Negatives.BOUNDED),

    /** Merged into {@code DefenseModifierItems.Worn.defense()}, before {@code ArmorBarOverride}. */
    DEFENSE(StatsSheetLines.DEFENSE_LABEL, Negatives.REFUSED),

    /**
     * Merged into {@code ClassDamageModifierItems}' equipped grants BEFORE
     * {@code ClassDamageModifiers.matching}, so the held-weapon-class gate applies to an accessory's
     * grant exactly as to a worn one (ruling Q3). CLASS accessories only; the grant's class is the
     * accessory's own class. The label is completed by the class ("Ranged Damage").
     */
    CLASS_DAMAGE("Damage", Negatives.REFUSED);

    /** How a negative amount on this stat is treated by the loader. */
    public enum Negatives { REFUSED, BOUNDED }

    private final String label;
    private final Negatives negatives;

    AccessoryStat(String label, Negatives negatives) {
        this.label = label;
        this.negatives = negatives;
    }

    /** The stat's display label, shared with the stats sheet so the two cannot spell it apart. */
    public String label() {
        return label;
    }

    public Negatives negatives() {
        return negatives;
    }

    /** The content token, as authored. */
    public String token() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive lookup for the loader. Null on a miss, so the caller names the file. */
    public static AccessoryStat fromName(String name) {
        if (name == null) return null;
        for (AccessoryStat stat : values()) {
            if (stat.name().equalsIgnoreCase(name)) return stat;
        }
        return null;
    }
}
