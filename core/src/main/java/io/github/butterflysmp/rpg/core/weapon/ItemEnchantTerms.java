package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.CritChanceEnchant;
import io.github.butterflysmp.rpg.core.combat.HitDamage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WHAT THIS ITEM'S OWN ACTIVE ENCHANTS DO TO ITS NUMBERS (true tooltips, the seat's T1 to T3, 2026-11-26; Ben: "1. A",
 * only the item's own active enchants). A value read off ONE stack's enchant state by the same resolvers the engine's
 * stat reconcile and cast path use (paper {@code ItemEnchantTermsReader}), so the tooltip and the hit cannot disagree.
 *
 * <p>A {@code record} is a class whose fields are fixed at construction and which gets its accessors, equals and
 * toString written for it (one line for a reader rusty at Java).
 *
 * <p><b>What is NOT here, by T2:</b> the wielder's level bonus, class bonus, accessories, armor set bonuses, crit
 * multiplier and charge. Those are the PLAYER's terms of a hit, added after the item's term ({@link HitDamage#hitBase}'s
 * flat bonus, then {@link HitDamage#dealt}'s charge and crit), and the tooltip is the same whoever holds the item.
 *
 * @param behaviours        the item's ACTIVE behaviours, already weapon-filtered ({@code BehaviourItems.activeOf}): what
 *                          {@code CastTunes.tune} and {@code BehaviourCasts} read at a press
 * @param damagePercent     the summed percent of its active DAMAGE enchants that match the weapon's class
 *                          ({@code DamageEnchants.matching}), the {@code enchantDamagePercent} a hit by it carries
 * @param critPercentPoints the summed CRIT_CHANCE enchant points ({@code EnchantValues.totalFor}), as the stat takes them
 * @param prose             the replacement ability description each active behaviour enchant authors
 *                          ({@code ability_description:}, T7), by enchant id; absent when it authors none
 */
public record ItemEnchantTerms(List<ActiveBehaviour> behaviours, double damagePercent, double critPercentPoints,
                               Map<String, List<String>> prose) {

    /** An unenchanted item: every term neutral, so every line renders exactly as it did before this slice. */
    public static final ItemEnchantTerms NONE = new ItemEnchantTerms(List.of(), 0.0, 0.0, Map.of());

    public ItemEnchantTerms {
        behaviours = behaviours == null ? List.of() : List.copyOf(behaviours);
        prose = prose == null ? Map.of() : Map.copyOf(prose);
    }

    /**
     * The ITEM'S TERM of a hit whose authored-and-scored amount is {@code scored}: {@code HitDamage.hitBase} with the
     * player's flat bonus at 0, so it is exactly the part of the engine's sum this item owns.
     */
    public double itemDamage(double scored) {
        return HitDamage.hitBase(scored, damagePercent, 0.0);
    }

    /** The crit chance this weapon grants while held: its own {@code crit_chance} plus its crit enchants, as the stat. */
    public double critChance(WeaponDefinition weapon) {
        double own = HeldWeaponCrit.sources(Optional.of(weapon)).values().stream().mapToDouble(Double::doubleValue).sum();
        return own + (CritChanceEnchant.boosts(critPercentPoints) ? CritChanceEnchant.contribution(critPercentPoints) : 0.0);
    }

    /** The replacement prose an active enchant authored for the ability it changes, or empty. */
    public Optional<List<String>> proseOf(String enchantId) {
        List<String> lines = prose.get(enchantId);
        return lines == null || lines.isEmpty() ? Optional.empty() : Optional.of(lines);
    }
}
