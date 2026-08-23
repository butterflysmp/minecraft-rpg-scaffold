package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * The plain-text half of the weapon tooltip: pure String/number formatters over the content model.
 * No Adventure, no Bukkit -- lives in core so it runs in the 2-second test loop, and the paper
 * {@code WeaponLore} builder only wraps these in colour/layout with the class label it owns.
 *
 * Every damage number the tooltip shows comes from the {@link WeaponDefinition} statically -- the
 * declared {@code attackDamage} for a basic (weapon_damage) hit, or an ability's literal
 * {@code Damage.amount} for a costed payload -- never the holder's resolved ATTACK_DAMAGE stat.
 * The tooltip describes the weapon, not whoever swings it, so it is mint-time only and cannot drift.
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
     * "Cooldown: 3.0s | Energy Cost: 40". The resource name comes from the cost's resourceId
     * ("energy" -> "Energy Cost"). A free, instant trigger yields "" -- the caller drops the line.
     */
    public static String cadenceLine(int cooldownTicks, ResourceCost cost) {
        List<String> parts = new ArrayList<>();
        if (cooldownTicks > 0) parts.add("Cooldown: " + cooldownLabel(cooldownTicks));
        if (!isFree(cost)) parts.add(capitalize(cost.resourceId()) + " Cost: " + trimNumber(cost.amount()));
        return String.join(" | ", parts);
    }

    /**
     * The damage number a trigger deals, taken from the weapon's static content: the FIRST
     * damage-bearing effect found (recursing into a Burst/Area's inner effects). A
     * {@code WeaponDamage} reads the weapon's declared {@code attackDamage}; a literal
     * {@code Damage} uses its own amount. Empty when the trigger deals no direct damage
     * (a pure heal/status, or a cosmetic-only payload) -- the caller drops the line.
     */
    public static OptionalDouble triggerDamage(List<EffectSpec> onHit, double weaponAttackDamage) {
        return firstDamage(onHit, weaponAttackDamage);
    }

    private static OptionalDouble firstDamage(List<? extends EffectSpec> effects, double weaponAttackDamage) {
        for (EffectSpec effect : effects) {
            OptionalDouble d = damageOf(effect, weaponAttackDamage);
            if (d.isPresent()) return d;
        }
        return OptionalDouble.empty();
    }

    /** The direct damage a single effect contributes, if any. Exhaustive over the sealed EffectSpec. */
    private static OptionalDouble damageOf(EffectSpec effect, double weaponAttackDamage) {
        return switch (effect) {
            case EffectSpec.WeaponDamage w -> OptionalDouble.of(weaponAttackDamage);
            case EffectSpec.Damage d       -> OptionalDouble.of(d.amount());
            case EffectSpec.Burst b        -> firstDamage(b.effects(), weaponAttackDamage);
            case EffectSpec.Area a         -> firstDamage(a.effects(), weaponAttackDamage);
            case EffectSpec.Heal h         -> OptionalDouble.empty();
            case EffectSpec.Knockback k    -> OptionalDouble.empty();
            case EffectSpec.Status s       -> OptionalDouble.empty();
            case EffectSpec.Visual v       -> OptionalDouble.empty();
            case EffectSpec.ThrowEmbers t  -> OptionalDouble.empty();
        };
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
