package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The numbers the Build screen shows on an equipped ability's icon: damage (the headline, the FIRST
 * damage-bearing effect), cost and cooldown, each FROM THE DERIVED DEFINITION -- the one
 * {@code AspectApplication.derive} output the cast also uses -- with the base value beside it only where an
 * aspect changed it (PLAN-build-system.md section 2.4.1: "Damage: 9  (12)"). Cooldown is seconds to one
 * decimal. Pure; paper colours the base gray.
 */
public final class AspectLore {

    private AspectLore() {}

    /** One line: a label, the resolved value, and the base value -- null when unchanged. */
    public record Line(String label, String value, String base) {}

    public static List<Line> lines(AbilityDefinition base, AbilityDefinition derived) {
        List<Line> lines = new ArrayList<>();
        double baseDamage = DamagePayload.headlineDamage(base.onHit(), 0);
        double derivedDamage = DamagePayload.headlineDamage(derived.onHit(), 0);
        if (derivedDamage > 0) lines.add(line("Damage", number(derivedDamage), number(baseDamage)));
        if (derived.cost() != null) {
            lines.add(line("Cost", number(derived.cost().amount()),
                    base.cost() == null ? null : number(base.cost().amount())));
        }
        lines.add(line("Cooldown", seconds(derived.cooldownTicks()), seconds(base.cooldownTicks())));
        return lines;
    }

    private static Line line(String label, String value, String base) {
        return new Line(label, value, value.equals(base) ? null : base);
    }

    /** "9", "10.2" -- no trailing zeros, no exponent. */
    static String number(double value) {
        return NumberResolution.plain(BigDecimal.valueOf(value));
    }

    /** Ticks as seconds to one decimal: 240 -> "12.0 s". */
    static String seconds(int ticks) {
        return BigDecimal.valueOf(ticks).divide(BigDecimal.valueOf(20), 1, RoundingMode.HALF_UP).toPlainString() + " s";
    }
}
