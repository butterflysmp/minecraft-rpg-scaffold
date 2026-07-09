package io.github.yourname.rpg.core.ability;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.ability.effect.EffectApplier;
import io.github.yourname.rpg.core.combat.Aim;
import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.combat.RayHit;

import java.util.Optional;
import java.util.UUID;

/**
 * Turns an aim into an impact, then applies the ability's effects there.
 *
 * This is the half of a cast that reads the world, so every entry point MUST
 * already be on the thread that owns the region containing the aim's origin.
 * On Paper that means inside Scheduler.onRegion(...). AbilityService.cast()
 * deliberately does none of this.
 */
public final class CastExecutor {

    private final CombatWorld world;
    private final EffectApplier effects;

    public CastExecutor(CombatWorld world) {
        this.world = world;
        this.effects = new EffectApplier(world);
    }

    public void execute(AbilityService.CastResult.Success success) {
        AbilityDefinition ability = success.ability();
        Combatant caster = success.caster();
        Aim aim = success.aim();

        switch (ability.cast()) {
            // The caster is their own target: heals, buffs, self-detonations.
            case CastSpec.Self ignored -> detonate(ability, caster, caster, caster.position());

            case CastSpec.Melee melee -> {
                Combatant target = meleeTarget(caster, aim, melee);
                Vec3 impact = target != null ? target.position() : aim.pointAt(melee.reach());
                detonate(ability, caster, target, impact);
            }

            case CastSpec.Ray ray -> resolveAlongAim(ability, caster, aim, ray.range());

            case CastSpec.Projectile projectile -> launch(ability, caster.id(), aim, projectile);
        }
    }

    /**
     * Throw it. The caster is captured by UUID and never dereferenced again: a
     * grenade with a 100-tick fuse outlives its thrower's logout, and holding the
     * Combatant would pin a Bukkit entity for five seconds. Same rule as an Area.
     */
    private void launch(AbilityDefinition ability, UUID casterId, Aim aim, CastSpec.Projectile spec) {
        step(ability, casterId, aim.origin(), aim.direction().scale(spec.speed()), 0, spec);
    }

    /**
     * One tick of flight. Trace the segment actually travelled rather than
     * sampling the endpoint, or a fast projectile tunnels straight through a
     * target thinner than its per-tick step.
     */
    private void step(AbilityDefinition ability, UUID casterId, Vec3 position,
                      Vec3 velocity, int elapsed, CastSpec.Projectile spec) {
        Vec3 next = position.add(velocity);

        Optional<RayHit> hit = world.castRay(position, next, casterId);
        if (hit.isPresent()) {
            detonate(ability, casterId, hit.get().combatant(), hit.get().point());
            return;
        }

        int nextElapsed = elapsed + 1;
        if (nextElapsed >= spec.maxLifetimeTicks()) {
            // The fuse ran out mid-air. It still goes off -- a grenade that
            // quietly vanishes because it hit nothing would be a bug, not a miss.
            detonate(ability, casterId, null, next);
            return;
        }

        Vec3 nextVelocity = velocity.add(new Vec3(0, -spec.gravity(), 0));
        world.schedule(next, 1, () -> step(ability, casterId, next, nextVelocity, nextElapsed, spec));
    }

    /** Walk the aim to its first obstruction, or to its full range if there is none. */
    private void resolveAlongAim(AbilityDefinition ability, Combatant caster, Aim aim, double range) {
        Vec3 end = aim.pointAt(range);
        Optional<RayHit> hit = world.castRay(aim.origin(), end, caster.id());

        Combatant target = hit.map(RayHit::combatant).orElse(null);
        Vec3 impact = hit.map(RayHit::point).orElse(end);

        detonate(ability, caster, target, impact);
    }

    /**
     * The nearest living thing inside the swing. arcDegrees is the full width of
     * the cone, so a 90-degree swing reaches 45 degrees either side of the aim.
     */
    private Combatant meleeTarget(Combatant caster, Aim aim, CastSpec.Melee melee) {
        double minimumDot = Math.cos(Math.toRadians(melee.arcDegrees() / 2.0));
        UUID casterId = caster.id();

        Combatant nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;

        for (Combatant candidate : world.combatantsNear(aim.origin(), melee.reach())) {
            if (candidate.id().equals(casterId)) continue;

            Vec3 toCandidate = candidate.position().subtract(aim.origin());
            // Both are unit vectors, so the dot product is the cosine of the
            // angle between them: larger means closer to straight ahead.
            if (toCandidate.normalize().dot(aim.direction()) < minimumDot) continue;

            double distanceSquared = toCandidate.lengthSquared();
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private void detonate(AbilityDefinition ability, Combatant caster, Combatant target, Vec3 impact) {
        effects.applyAll(ability.onHit(), caster, target, impact);
    }

    private void detonate(AbilityDefinition ability, UUID casterId, Combatant target, Vec3 impact) {
        effects.applyAllFromCaster(ability.onHit(), casterId, target, impact);
    }
}
