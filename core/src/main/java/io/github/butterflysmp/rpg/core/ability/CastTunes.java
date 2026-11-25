package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * THE ONE SEAM FOR NUMERIC CHANGES TO A WEAPON'S OWN CAST (the seat's G4, 2026-11-24): the ability a weapon's right click
 * fires, with every ACTIVE {@link EnchantBehaviour.CastTune} of that weapon applied. Pure: an ability and the behaviours
 * in, an ability out ({@code CastExecutor} calls it at the commit and at each volley shot, ruling 1).
 *
 * <p><b>Which cast it touches:</b> only the ability whose id is {@code <weaponId>/right_click}, the id {@code WeaponLoader}
 * gives a weapon's right click. A stone cast, another gesture, another weapon's cast and a granted trigger
 * ({@code <enchant>/<input>}) are returned untouched, the SAME instance, so a caller can tell nothing applied.
 *
 * <p><b>The damage term (G5):</b> {@link EnchantBehaviour.CastTune.Quantity#DAMAGE} adds to the AUTHORED {@code amount}
 * of every literal {@link EffectSpec.Damage} in on_hit. The Damage arm then prices it as it prices any amount:
 * {@code ((amount + add) x score / 100) x enchantMultiplier + class bonus + level bonus, x charge x crit}. A
 * {@code weapon_damage} effect is not touched (no mage weapon authors one; the loader refuses a damage tune on a cast with
 * no literal damage).
 */
public final class CastTunes {

    private CastTunes() {}

    /** The ability's id a tune of {@code weaponId} acts on. */
    public static String castId(String weaponId) {
        return weaponId + "/right_click";
    }

    public static AbilityDefinition tune(AbilityDefinition ability, List<ActiveBehaviour> behaviours) {
        AbilityDefinition out = ability;
        if (behaviours == null) return out;
        for (ActiveBehaviour active : behaviours) {
            if (!(active.behaviour() instanceof EnchantBehaviour.CastTune t)) continue;
            if (!ability.id().equals(castId(t.weaponId()))) continue;
            out = apply(out, t.quantity(), t.valueAt(active.level()));
        }
        return out;
    }

    /** One quantity moved by {@code value}. A cast that does not have the quantity is returned unchanged. */
    static AbilityDefinition apply(AbilityDefinition a, EnchantBehaviour.CastTune.Quantity quantity, double value) {
        return switch (quantity) {
            case DAMAGE -> {
                List<EffectSpec> onHit = new ArrayList<>(a.onHit().size());
                for (EffectSpec e : a.onHit()) {
                    onHit.add(e instanceof EffectSpec.Damage d ? new EffectSpec.Damage(d.amount() + value, d.element()) : e);
                }
                yield with(a, a.cast(), onHit);
            }
            case RANGE -> switch (a.cast()) {
                case CastSpec.Ray r -> with(a, longer(r, value), a.onHit());
                case CastSpec.Volley v when v.of() instanceof CastSpec.Ray r -> with(a,
                        new CastSpec.Volley(v.windupTicks(), v.shots(), v.intervalTicks(), longer(r, value), v.lockedAim(),
                                v.warning()), a.onHit());
                default -> a;
            };
            case SHOTS -> a.cast() instanceof CastSpec.Volley v
                    ? with(a, new CastSpec.Volley(v.windupTicks(), (int) value, v.intervalTicks(), v.of(), v.lockedAim(),
                            v.warning()), a.onHit())
                    : a;
            // A cut below 0 throws in Volley's own constructor, by name: the loader builds every level once at load.
            case WINDUP -> a.cast() instanceof CastSpec.Volley v
                    ? with(a, new CastSpec.Volley(v.windupTicks() - (int) value, v.shots(), v.intervalTicks(), v.of(),
                            v.lockedAim(), v.warning()), a.onHit())
                    : a;
        };
    }

    private static CastSpec.Ray longer(CastSpec.Ray r, double add) {
        return new CastSpec.Ray(r.range() + add, r.beam(), r.pierce(), r.tolerance());
    }

    private static AbilityDefinition with(AbilityDefinition a, CastSpec cast, List<EffectSpec> onHit) {
        return new AbilityDefinition(a.id(), a.displayName(), a.element(), a.cooldownTicks(), a.cost(), cast, onHit,
                a.description(), a.onCast());
    }
}
