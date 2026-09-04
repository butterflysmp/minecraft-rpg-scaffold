package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Optional;

/**
 * A body arcing under gravity until it hits something. Extracted from CastExecutor so the
 * Projectile CAST and the throw_embers EFFECT share ONE flight loop, not two that drift.
 *
 * NOTE (corrected): that shared-loop claim is no longer true. throw_embers now throws real Bukkit
 * items and runs its own per-tick loop in EffectApplier.trackEmber, so this class has exactly one
 * caller -- CastExecutor.launch. The sentence above is kept because the extraction it describes is
 * still why this file exists; the reuse it promised simply went away.
 *
 * The caster rides as a {@link Caster}: an identity plus the stats frozen at cast time, never a live
 * handle. A projectile outlives the frame that threw it (and, with a fuse on impact, longer still),
 * so nothing here may hold an entity -- see EffectApplier. The frozen STATS are the load-bearing
 * half: this loop hands the Caster to the impact callback, and the callback resolves the payload on
 * the TARGET'S region, which on Folia is not the caster's. An effect that needed the caster's attack
 * damage there could not legally read the store; it reads the value this carried instead. That is
 * why the whole Caster is threaded rather than just the id -- the freeze is visible at the boundary
 * where the region hop actually happens, not buried in one call site's closure.
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
    public static void launch(CombatWorld world, Caster caster, Vec3 origin, Vec3 velocity,
                              double gravity, int maxLifetimeTicks, String trail, Impact onImpact) {
        step(world, caster, origin, velocity, gravity, maxLifetimeTicks, 0, trail, onImpact);
    }

    /**
     * One tick of flight. Trace the segment actually travelled rather than sampling the
     * endpoint, or a fast projectile tunnels through a target thinner than its per-tick step.
     * The first step runs inline on the launch frame, exactly as before the extraction.
     */
    private static void step(CombatWorld world, Caster caster, Vec3 position, Vec3 velocity,
                             double gravity, int maxLifetimeTicks, int elapsed,
                             String trail, Impact onImpact) {
        // NOT on the launch frame. On elapsed == 0 the position IS the aim's origin, which for a
        // weapon is the caster's EYE -- so drawing here puts the first puff of flame inside the
        // shooter's own camera. The old repo never did: its tracker was runTaskTimer(plugin, 1L,
        // 1L), first draw one tick AFTER launch, at a position the bolt had already moved to.
        //
        // A per-tick COUNT assertion cannot see this defect -- the count is the same either way --
        // so the test that guards it asserts the first presented POSITION instead.
        //
        // This does NOT generalise to EffectApplier.trackEmber, which draws inline on its launch
        // frame and should keep doing so: throw_embers spawns a real item AT the origin, so its
        // frame-0 particle sits on a visible body rather than in a face.
        if (trail != null && elapsed > 0) world.present(position, trail);

        Vec3 next = position.add(velocity);

        Optional<RayHit> hit = world.castRay(position, next, caster.id());
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
                step(world, caster, next, nextVelocity, gravity, maxLifetimeTicks, nextElapsed, trail, onImpact));
    }
}
