package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * A body arcing under gravity until it hits something. Extracted from CastExecutor so the
 * Projectile CAST and the throw_embers EFFECT share ONE flight loop, not two that drift --
 * the same reuse as AbilitySchema shared by weapon triggers and abilities.
 *
 * The caster is a UUID throughout: a projectile outlives the frame that threw it (and, with a
 * fuse on impact, longer still), so nothing here holds a live handle -- see EffectApplier.
 *
 * What happens on impact is the caller's, handed in as {@link Impact}. A grenade detonates
 * immediately; an ember schedules a fuse. The loop neither knows nor cares which -- it just
 * fires the callback at the point of impact, so any effect (immediate or scheduling) reuses
 * this same route.
 *
 * MUST be launched on the thread owning the origin's region; each tick re-enters the region
 * owning the point it has flown to, exactly as a Ray walks chunk columns.
 */
public final class ProjectileFlight {

    private ProjectileFlight() {}

    /** What to do where a projectile lands. {@code target} is null on a wall or a clean miss. */
    @FunctionalInterface
    public interface Impact {
        void at(Combatant target, Vec3 point);
    }

    /**
     * Throw it. {@code velocity} is the full first-tick step (direction * speed, plus any
     * launch lift); {@code gravity} is subtracted from the vertical each tick. {@code trail}
     * is a visual id presented at the projectile's position each tick, or null for none -- a
     * bare grenade leaves nothing, a thrown ember leaves flame.
     */
    public static void launch(CombatWorld world, UUID casterId, Vec3 origin, Vec3 velocity,
                              double gravity, int maxLifetimeTicks, String trail, Impact onImpact) {
        step(world, casterId, origin, velocity, gravity, maxLifetimeTicks, 0, trail, onImpact);
    }

    /**
     * One tick of flight. Trace the segment actually travelled rather than sampling the
     * endpoint, or a fast projectile tunnels through a target thinner than its per-tick step.
     * The first step runs inline on the launch frame, exactly as before the extraction.
     */
    private static void step(CombatWorld world, UUID casterId, Vec3 position, Vec3 velocity,
                             double gravity, int maxLifetimeTicks, int elapsed,
                             String trail, Impact onImpact) {
        if (trail != null) world.present(position, trail);

        Vec3 next = position.add(velocity);

        Optional<RayHit> hit = world.castRay(position, next, casterId);
        if (hit.isPresent()) {
            onImpact.at(hit.get().combatant(), hit.get().point());
            return;
        }

        int nextElapsed = elapsed + 1;
        if (nextElapsed >= maxLifetimeTicks) {
            // The fuse ran out mid-air. It still lands -- a projectile that quietly vanishes
            // because it hit nothing would be a bug, not a miss.
            onImpact.at(null, next);
            return;
        }

        Vec3 nextVelocity = velocity.add(new Vec3(0, -gravity, 0));
        world.schedule(next, 1, () ->
                step(world, casterId, next, nextVelocity, gravity, maxLifetimeTicks, nextElapsed, trail, onImpact));
    }
}
