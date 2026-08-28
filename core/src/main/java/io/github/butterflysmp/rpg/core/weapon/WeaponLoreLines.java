package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The plain-text half of the weapon tooltip: pure String/number formatters over the content model.
 * No Adventure, no Bukkit -- lives in core so it runs in the 2-second test loop, and the paper
 * {@code WeaponLore} builder only wraps these in colour/layout with the class label it owns.
 *
 * Every damage number the tooltip shows comes from the {@link WeaponDefinition} statically -- the
 * declared {@code attackDamage} for a basic (weapon_damage) hit, or an ability's literal
 * {@code Damage.amount} for a costed payload -- never the holder's resolved ATTACK_DAMAGE stat.
 * The tooltip describes the weapon, not whoever swings it, so it is mint-time only and cannot drift.
 *
 * {@link DamagePayload.DamageSource} is the load-bearing distinction here: it is what lets the
 * tooltip render a basic attack as a STAT BLOCK (class-labelled damage + attack speed, no prose) and
 * an ability as an ABILITY BLOCK (name, prose, element-labelled damage, cadence). A basic attack is a
 * stat, not an ability, and it should not need a whole section to say so. That distinction now lives
 * in {@link DamagePayload} rather than here, because the cooldown scaler asks the same question --
 * see its javadoc for why one shared answer matters.
 *
 * Note what this class does NOT do: the {@code Attack Speed} it formats is the weapon's BASE, never
 * the holder's resolved attack-speed stat. Lore describes the weapon, not whoever is holding it --
 * that is what makes it mint-time safe and non-drifting. A boosted player and an unboosted one
 * reading the same sword must see the same number.
 *
 * That base now comes from TWO sources, because the two kinds of basic attack no longer share a
 * cadence. A vanilla-driven melee hit is paced by the vanilla attack-speed attribute, authored
 * directly as {@code attack_speed:}; a ranged basic attack is still paced by its trigger's
 * {@code cooldown_ticks} through {@code AttackSpeed.effectiveCooldownTicks}. Each formatter reads
 * the number that actually governs its own weapon, so neither line can drift from what the weapon
 * really does -- which is why this is two methods and not one with a converted argument.
 */
public final class WeaponLoreLines {

    private WeaponLoreLines() {}

    /** Ticks to a one-decimal second label: 10 -> "0.5s", 60 -> "3.0s". */
    public static String cooldownLabel(int ticks) {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0);
    }

    /** An input id as a readable label: "left_click" -> "Left-Click". */
    public static String inputLabel(String input) {
        String[] parts = input.split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append('-');
            out.append(capitalize(parts[i]));
        }
        return out.toString();
    }

    /**
     * The trigger's cadence folded onto one line: "Cooldown: 0.5s", or with a cost
     * "Cooldown: 3.0s | Mana Cost: 40". The resource name comes from the cost's resourceId
     * ("mana" -> "Mana Cost"). A free, instant trigger yields "" -- the caller drops the line.
     */
    public static String cadenceLine(int cooldownTicks, ResourceCost cost) {
        List<String> parts = new ArrayList<>();
        if (cooldownTicks > 0) parts.add("Cooldown: " + cooldownLabel(cooldownTicks));
        if (!isFree(cost)) parts.add(capitalize(cost.resourceId()) + " Cost: " + trimNumber(cost.amount()));
        return String.join(" | ", parts);
    }

    /**
     * The damage a trigger deals. Delegates to {@link DamagePayload}, which owns the walk and the
     * basic-attack/ability distinction -- the tooltip and the cooldown scaler both ask it, so the
     * two cannot disagree about the same weapon. Kept here so the lore call sites read naturally.
     */
    public static Optional<DamagePayload.TriggerDamage> triggerDamage(List<EffectSpec> onHit,
                                                                      double weaponAttackDamage) {
        return DamagePayload.of(onHit, weaponAttackDamage);
    }

    /**
     * A RANGED basic attack's rate as ATTACKS PER SECOND: 15 ticks between shots -> "1.3". Vanilla
     * Minecraft states attack speed this way on its own item tooltips, so a player already knows
     * that higher is better -- which is why this is not just {@link #cooldownLabel} reused. An
     * ability's cadence still reads as a cooldown in seconds, where lower is better.
     *
     * Derived from the trigger's cooldown because for a ranged basic attack that IS the cadence:
     * the shot is gated by {@code CooldownTracker}, scaled by {@code AttackSpeed
     * .effectiveCooldownTicks}. Deriving it rather than authoring a second number is what stops the
     * two disagreeing the first time someone edits one and forgets the other.
     *
     * A non-positive cooldown yields "" and the caller drops the line: no shipped ranged basic
     * attack has one, and the guard is what stops a zero-cooldown weapon_damage trigger dividing by
     * zero and printing "Infinity" on someone's tooltip.
     */
    public static String rangedAttackSpeedLabel(int cooldownTicks) {
        if (cooldownTicks <= 0) return "";
        return String.format(Locale.ROOT, "%.1f", 20.0 / cooldownTicks);
    }

    /**
     * A MELEE basic attack's rate, read straight off the weapon's authored {@code attack_speed}.
     *
     * Not derived from anything: since vanilla's crosshair attack took over the melee hit, the
     * weapon's cadence IS this number -- it is written onto the wielder's vanilla attack-speed
     * attribute and paces the attack-strength meter the charge curve reads. Authoring it directly
     * rather than deriving it from a tick count is also what makes true vanilla values expressible:
     * every vanilla sword is 1.6, and {@code 20 / n} cannot produce 1.6 for any integer n.
     *
     * A non-positive speed yields "" and the caller drops the line. In practice unreachable for a
     * vanilla-driven melee weapon -- {@code WeaponDefinition} rejects one outright -- so this guard
     * covers the weapon that has no melee basic at all rather than a legal-but-odd number.
     */
    public static String meleeAttackSpeedLabel(double attackSpeed) {
        if (attackSpeed <= 0) return "";
        return String.format(Locale.ROOT, "%.1f", attackSpeed);
    }

    private static boolean isFree(ResourceCost cost) {
        return cost.amount() == 0 || cost.resourceId().equals("none");
    }

    /** A double with the trailing ".0" dropped: 12.0 -> "12", 12.5 -> "12.5". */
    private static String trimNumber(double n) {
        if (n == Math.floor(n) && !Double.isInfinite(n)) return String.valueOf((long) n);
        return String.valueOf(n);
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
