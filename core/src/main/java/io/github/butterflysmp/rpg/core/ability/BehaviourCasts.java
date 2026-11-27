package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * WHICH BEHAVIOUR CHANGES A CAST, AND WHAT ITS HIT CARRIES: the pure resolvers {@link CastExecutor} and the tooltip both
 * call (true tooltips, the seat's T1, 2026-11-26). Extracted from the executor word for word, so the cast a press makes
 * and the cast the tooltip describes are decided by ONE function each; no second formula is written for the lore.
 *
 * <p>Every method here is pure: an ability id, a cast and the behaviours in, an answer out. The executor's tests stayed
 * green, untouched, across the extraction.
 */
public final class BehaviourCasts {

    private BehaviourCasts() {}

    /**
     * The weapon's on_hit with every literal {@link EffectSpec.Damage} replaced by {@code amount} in {@code element}, every
     * other effect (knockback, the hit visual) kept as authored. What Shatter Caps' stone, an Airburst burst and Focused
     * Beam's one beam each land.
     */
    public static List<EffectSpec> replaceDamage(List<EffectSpec> onHit, double amount, String element) {
        List<EffectSpec> out = new ArrayList<>(onHit.size());
        for (EffectSpec effect : onHit) {
            out.add(effect instanceof EffectSpec.Damage ? new EffectSpec.Damage(amount, element) : effect);
        }
        return out;
    }

    /**
     * Focused Beam: the active behaviour that replaces a volley's FIRST shot with one beam, when {@code inner} (the
     * volley's own inner cast) is a ray and the ability is a cast of the behaviour's weapon ({@code <weapon>/...}).
     */
    public static Optional<ActiveBehaviour> focusedBeam(String abilityId, CastSpec inner, List<ActiveBehaviour> behaviours) {
        if (!(inner instanceof CastSpec.Ray) || behaviours == null) return Optional.empty();
        for (ActiveBehaviour active : behaviours) {
            if (active.behaviour() instanceof EnchantBehaviour.FocusedBeam beam && abilityId.startsWith(beam.weaponId() + "/")) {
                return Optional.of(active);
            }
        }
        return Optional.empty();
    }

    /** Shatter Caps: the active behaviour that replaces a SPREAD projectile with one stone, on its weapon's cast. */
    public static Optional<ActiveBehaviour> shatter(String abilityId, CastSpec.Projectile spec, List<ActiveBehaviour> behaviours) {
        if (spec.spread() == null || behaviours == null) return Optional.empty();
        for (ActiveBehaviour active : behaviours) {
            if (active.behaviour() instanceof EnchantBehaviour.Shatter caps && abilityId.startsWith(caps.weaponId() + "/")) {
                return Optional.of(active);
            }
        }
        return Optional.empty();
    }

    /**
     * Airburst: the active behaviour that splits ONE projectile body, when the split's travel is reached inside the
     * authored lifetime (otherwise the shot flies as authored and never splits). As the executor, no ability-id check: the
     * weapon filter ({@code BehaviourItems.activeOf}) already holds it to its weapon.
     */
    public static Optional<ActiveBehaviour> airburst(CastSpec.Projectile spec, List<ActiveBehaviour> behaviours) {
        if (behaviours == null) return Optional.empty();
        for (ActiveBehaviour active : behaviours) {
            if (active.behaviour() instanceof EnchantBehaviour.Airburst burst
                    && EnchantBehaviour.Airburst.ticksToTravel(burst.travelBlocks(), spec.speed()) < spec.maxLifetimeTicks()) {
                return Optional.of(active);
            }
        }
        return Optional.empty();
    }

    /**
     * Cluster Bombs (F9, 2026-11-26): the active behaviour that replaces a FUSED projectile's throw with one big chunk, on
     * its weapon's cast ({@code <weapon>/...}, as Shatter's: a volley never fires a fused projectile, FusedPayload refuses
     * one, so here the check is a belt).
     */
    public static Optional<ActiveBehaviour> clusterBombs(String abilityId, CastSpec.Projectile spec,
                                                         List<ActiveBehaviour> behaviours) {
        if (!spec.fused() || behaviours == null) return Optional.empty();
        for (ActiveBehaviour active : behaviours) {
            if (active.behaviour() instanceof EnchantBehaviour.ClusterBombs bombs
                    && abilityId.startsWith(bombs.weaponId() + "/")) {
                return Optional.of(active);
            }
        }
        return Optional.empty();
    }

    /**
     * The BIG chunk's detonation (F9): the weapon's own on_hit with every burst's radius replaced by {@code radius} and
     * every damage inside it replaced by {@code amount} in {@code element}; every other effect (the blast visual) kept as
     * authored. What the executor sets off and what the tooltip's first Cluster Bombs row reads.
     */
    public static List<EffectSpec> clusterBig(List<EffectSpec> onHit, double amount, String element, double radius) {
        List<EffectSpec> out = new ArrayList<>(onHit.size());
        for (EffectSpec effect : onHit) {
            if (effect instanceof EffectSpec.Burst b) {
                List<EffectSpec.Targeted> inner = new ArrayList<>(b.effects().size());
                for (EffectSpec.Targeted t : b.effects()) {
                    inner.add(t instanceof EffectSpec.Damage ? new EffectSpec.Damage(amount, element) : t);
                }
                out.add(new EffectSpec.Burst(radius, inner));
            } else {
                out.add(effect);
            }
        }
        return out;
    }

    /**
     * The Lapis Crystal's shot BEFORE its hit terms: the beam's FIRST literal Damage amount x the level's percent / 100, or
     * empty when the beam carries no literal damage (then no shot fires). The Damage arm then prices it as any amount.
     */
    public static OptionalDouble orbShotAmount(List<EffectSpec> beamOnHit, CastSpec.OrbShot shot, int level) {
        Optional<Double> beamAmount = beamOnHit.stream()
                .filter(e -> e instanceof EffectSpec.Damage)
                .map(e -> ((EffectSpec.Damage) e).amount())
                .findFirst();
        if (beamAmount.isEmpty() || shot == null) return OptionalDouble.empty();
        List<Double> percents = shot.percentByLevel();
        double percent = percents.get(Math.max(1, Math.min(level, percents.size())) - 1);
        return OptionalDouble.of(beamAmount.get() * percent / 100.0);
    }
}
