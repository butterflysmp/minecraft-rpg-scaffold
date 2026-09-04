package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Optional;
import java.util.UUID;

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
     * What a projectile LOOKS like. Two independent presentation concerns, grouped so the flight's
     * parameter list does not grow one slot per visual idea: a per-tick {@code trail} visual id,
     * and an {@code item} material rendered as the bolt's BODY. Either may be null.
     *
     * <p>They are independent on purpose. A trail with no body is what the Flint Staff shipped as
     * one slice earlier; a body with no trail is a silent thrown rock.
     */
    public record Look(String trail, String item) {
        /** Neither -- a bare grenade, and both dev weapons. */
        public static final Look NONE = new Look(null, null);
    }

    /**
     * Throw it. {@code velocity} is the full first-tick step (direction * speed, plus any
     * launch lift); {@code gravity} is subtracted from the vertical each tick. {@code look}
     * carries the optional trail and body; {@link Look#NONE} for a projectile that shows nothing.
     */
    public static void launch(CombatWorld world, Caster caster, Vec3 origin, Vec3 velocity,
                              double gravity, int maxLifetimeTicks, Look look, Impact onImpact) {
        // THE BODY IS SPAWNED ON THE LAUNCH FRAME, AT THE AIM ORIGIN -- which for a weapon is the
        // caster's eye, the very position step() below REFUSES to draw the trail at.
        //
        // That asymmetry is deliberate and the two comments point at each other, because it reads
        // as an inconsistency and the obvious "tidy" is to align them:
        //
        //   a PARTICLE at the eye is a flash inside your own camera;
        //   a rendered BODY at the eye is the bolt leaving the staff.
        //
        // The old repo dropped its flint item immediately, at the eye. A bolt that pops into
        // existence one tick downrange is a different weapon. Same distinction EffectApplier's
        // trackEmber relies on when it draws inline on its own launch frame: its particle sits on a
        // body that is already there.
        //
        // Legal here: we are on the region owning `origin`, which is where the entity is created.
        UUID markerId = look.item() == null ? null
                : world.spawnMarker(origin, look.item(), maxLifetimeTicks);
        step(world, caster, origin, velocity, gravity, maxLifetimeTicks, 0, look, markerId, onImpact);
    }

    /**
     * One tick of flight. Trace the segment actually travelled rather than sampling the
     * endpoint, or a fast projectile tunnels through a target thinner than its per-tick step.
     * The first step runs inline on the launch frame, exactly as before the extraction.
     */
    private static void step(CombatWorld world, Caster caster, Vec3 position, Vec3 velocity,
                             double gravity, int maxLifetimeTicks, int elapsed,
                             Look look, UUID markerId, Impact onImpact) {
        // NOT on the launch frame. On elapsed == 0 the position IS the aim's origin, which for a
        // weapon is the caster's EYE -- so drawing here puts the first puff of flame inside the
        // shooter's own camera. The old repo never did: its tracker was runTaskTimer(plugin, 1L,
        // 1L), first draw one tick AFTER launch, at a position the bolt had already moved to.
        //
        // THE BODY DOES THE OPPOSITE, ON PURPOSE -- see launch(). A particle at the eye is a flash
        // in your camera; a body at the eye is the bolt leaving the staff. Do not "tidy" these two
        // into agreement; aligning them reintroduces one defect or the other.
        //
        // A per-tick COUNT assertion cannot see this defect -- the count is the same either way --
        // so the test that guards it asserts the first presented POSITION instead.
        //
        // This does NOT generalise to EffectApplier.trackEmber, which draws inline on its launch
        // frame and should keep doing so: throw_embers spawns a real item AT the origin, so its
        // frame-0 particle sits on a visible body rather than in a face.
        //
        // present() is safe here where moveMarker below is not, and the difference is not arbitrary:
        // the adapter hops present() onto the region owning `position` itself. An ENTITY write
        // cannot be hopped that way -- it has to happen where the entity is, and only we know that.
        if (look.trail() != null && elapsed > 0) world.present(position, look.trail());

        Vec3 next = position.add(velocity);

        Optional<RayHit> hit = world.castRay(position, next, caster.id());
        if (hit.isPresent()) {
            resolve(world, markerId, hit.get().point());
            onImpact.at(hit.get().combatant(), hit.get().point());
            return;
        }

        int nextElapsed = elapsed + 1;
        if (nextElapsed >= maxLifetimeTicks) {
            // The fuse ran out mid-air. It still lands -- a projectile that quietly vanishes
            // because it hit nothing would be a bug, not a miss.
            resolve(world, markerId, next);
            onImpact.at(null, next);
            return;
        }

        // THE MARKER MOVES HERE, AT THE END OF THE STEP -- NOT AT THE TOP BESIDE THE TRAIL.
        //
        // This step was scheduled by the previous one at ITS `next`, which is our `position`, so we
        // are on region(position) -- and region(position) is where the marker actually IS, because
        // that is where the previous step's move put it. Moving it at the top of the NEXT step would
        // touch an entity in region(position) from region(next)'s thread: you would own the
        // destination while Folia requires you to own the source.
        //
        // The adapter's removeMarker and markerLocation do no region hop of their own -- they are
        // getEntity(uuid) and then touch it -- so this contract is the caller's to keep, and it is
        // kept here.
        //
        // EffectApplier.trackEmber avoids the problem entirely by scheduling at the item's LIVE
        // position; a flight that schedules at a COMPUTED position cannot, because the entity is one
        // step behind by construction. Hence the ordering rather than the trick.
        //
        // On Paper every region is one thread, so getting this wrong would pass every boot row and
        // fail only on Folia -- green rather than merely unverified.
        if (markerId != null) world.moveMarker(markerId, next);

        Vec3 nextVelocity = velocity.add(new Vec3(0, -gravity, 0));
        world.schedule(next, 1, () ->
                step(world, caster, next, nextVelocity, gravity, maxLifetimeTicks, nextElapsed,
                        look, markerId, onImpact));
    }

    /**
     * Retire the body AT the point the bolt resolves, then remove it.
     *
     * <p>The move is not decoration. The impact is at {@code hit.point()}, somewhere along the
     * segment just traced, while the marker is still back at the segment's START -- at speed 1.4
     * that is up to 1.4 blocks between where the flint disappears and where the fire appears. The
     * old repo had no such gap: the item flew by physics and the hit was found within 0.7 blocks of
     * the item's own location, so the two coincided by construction. {@code trackEmber} states the
     * same principle for its fuse -- "the boom lands with the blast: same tick, same place, so they
     * cannot diverge."
     *
     * <p>A stride between the two reads in game as "the bolt passed through it", which is a
     * hit-detection bug that is not happening, so this is cheaper than the misdiagnosis.
     *
     * <p>Called while still on the region owning the marker (before this step's own move), so both
     * calls are legal. The one ordering NOT guaranteed is on Folia, where a removal immediately
     * after a cross-region async teleport acts on an entity mid-handoff; the marker's armed
     * self-destruct (see {@link CombatWorld#spawnMarker}) is what bounds that rather than a leak.
     */
    private static void resolve(CombatWorld world, UUID markerId, Vec3 impact) {
        if (markerId == null) return;
        world.moveMarker(markerId, impact);
        world.removeMarker(markerId);
    }
}
