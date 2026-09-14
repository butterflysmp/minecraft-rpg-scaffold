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
     * How a projectile CHASES. {@code lerp} is how far the flight turns toward its target each
     * tick; {@code activationBlocks} is how far from the SPAWN POINT it flies ballistically first;
     * {@code searchRadius} is how far it looks, re-chosen every tick.
     *
     * <p><b>THE FLIGHT'S OWN PARAMETER RECORD, EXACTLY AS {@link Look} IS</b> -- and for the same
     * reason. {@code core.combat} imports nothing from {@code core.ability} (measured: zero such
     * imports before this slice), so the flight takes its parameters in its own vocabulary and
     * {@code CastExecutor} maps {@code CastSpec.Homing} into it at the call site, the way it already
     * maps {@code trail} and {@code item} into a {@code Look}. The numbers live in one place --
     * the content -- and this is the shape they arrive in.
     *
     * <p><b>NULL means no chasing</b> -- the projectile that flies where it was aimed, which is
     * every bolt in this repo before the Dragon's Plume.
     *
     * <p><b>There is deliberately no {@code NONE} constant to pair with {@link Look#NONE}.</b> A
     * {@code Look} with two nulls is a real, usable value -- the flight asks it for a trail and
     * gets none. There is no such thing for a seek: every field would have to be a lie, and a
     * {@code NONE} that is itself null is a constant nobody can call a method on. <b>Two ways to
     * say "no homing" is one more than the code can keep straight</b>, so there is one.
     */
    public record Seek(double lerp, double activationBlocks, double searchRadius) {}

    /**
     * Throw it. {@code velocity} is the full first-tick step (direction * speed, plus any
     * launch lift); {@code gravity} is subtracted from the vertical each tick. {@code look}
     * carries the optional trail and body; {@link Look#NONE} for a projectile that shows nothing.
     * {@code seek} carries the chase, or NULL for a bolt that flies where it was aimed.
     *
     * <p>{@code origin} was already a parameter and now does a second job: it is the SPAWN POINT
     * the activation distance is measured from, threaded down the recursion so every tick can ask
     * how far the bolt has come from where it started. That is a straight-line distance, not a path
     * length -- a bolt that curves has travelled further than it has got.
     */
    public static void launch(CombatWorld world, Caster caster, Vec3 origin, Vec3 velocity,
                              double gravity, int maxLifetimeTicks, Look look, Seek seek,
                              Impact onImpact) {
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
        step(world, caster, origin, origin, velocity, gravity, maxLifetimeTicks, 0, look, seek,
                markerId, onImpact);
    }

    /**
     * One tick of flight. Trace the segment actually travelled rather than sampling the
     * endpoint, or a fast projectile tunnels through a target thinner than its per-tick step.
     * The first step runs inline on the launch frame, exactly as before the extraction.
     */
    private static void step(CombatWorld world, Caster caster, Vec3 spawn, Vec3 position,
                             Vec3 velocity, double gravity, int maxLifetimeTicks, int elapsed,
                             Look look, Seek seek, UUID markerId, Impact onImpact) {
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
        // present() is safe here where driveMarker below is not, and the difference is not arbitrary:
        // the adapter hops present() onto the region owning `position` itself. An ENTITY write
        // cannot be hopped that way -- it has to happen where the entity is, and only we know that.
        if (look.trail() != null && elapsed > 0) world.present(position, look.trail());

        Vec3 next = position.add(velocity);

        Optional<RayHit> hit = world.castRay(position, next, caster.id());
        if (hit.isPresent()) {
            resolve(world, markerId);
            onImpact.at(hit.get().combatant(), hit.get().point());
            return;
        }

        int nextElapsed = elapsed + 1;
        if (nextElapsed >= maxLifetimeTicks) {
            // The fuse ran out mid-air. It still lands -- a projectile that quietly vanishes
            // because it hit nothing would be a bug, not a miss.
            resolve(world, markerId);
            onImpact.at(null, next);
            return;
        }

        // THE MARKER IS DRIVEN HERE, AT THE END OF THE STEP -- NOT AT THE TOP BESIDE THE TRAIL.
        //
        // This step was scheduled by the previous one at ITS `next`, which is our `position`, so we
        // are on region(position) -- and region(position) is where the marker actually IS. Driving
        // it at the top of the NEXT step would touch an entity in region(position) from
        // region(next)'s thread: you would own the destination while Folia requires you to own the
        // source. Setting a velocity is an entity write like any other, so the rule is unchanged
        // from when this line repositioned the marker instead.
        //
        // The adapter's removeMarker and markerLocation do no region hop of their own -- they are
        // getEntity(uuid) and then touch it -- so this contract is the caller's to keep.
        //
        // On Paper every region is one thread, so getting this wrong would pass every boot row and
        // fail only on Folia -- green rather than merely unverified.
        //
        // WHY A VELOCITY AND NOT A POSITION. Repositioning was verified to work server-side and
        // verified NOT to reach the client: the entity was where we put it, 23 times out of 23, and
        // a player watching a near-stationary bolt at 20 blocks saw nothing there. Driving hands the
        // motion to the platform's own mover -- the path every thrown item already uses, and the
        // only one observed to render.
        //
        // `velocity` is exactly one tick's displacement, already carrying this ability's gravity
        // (see nextVelocity below), and the adapter suppresses the platform's own gravity so the two
        // cannot both apply. That is what makes the driven body land ON the computed path rather
        // than near it.
        if (markerId != null) world.driveMarker(markerId, velocity);

        // THE STEER GOES HERE, INTO nextVelocity, AND NEVER INTO `velocity`. THIS IS THE WHOLE
        // TRAP AND IT IS INVISIBLE ONCE WRITTEN WRONG.
        //
        // `velocity` is the displacement ALREADY TRAVELLED this tick. Three things read it and they
        // must agree: `next` is position + velocity, castRay traced exactly that segment, and
        // driveMarker one line above handed the platform's mover the same vector -- which is what
        // makes "the driven body land ON the computed path rather than near it" true.
        //
        // STEER `velocity` AND THE BODY RENDERS ON A PATH THE RAY NEVER TRACED. The hit is computed
        // along one line and drawn along another, and on a fast bolt nobody can see which of the two
        // was wrong. A COUNT cannot see it either -- the same number of drives, the same number of
        // rays -- which is why the guard against it compares the DRIVEN VECTOR to the segment
        // between consecutive ray origins, in ProjectileHomingTest.
        //
        // So the steer belongs exactly where gravity already lives: in the velocity handed to the
        // NEXT tick, computed after everything that reads this one.
        //
        // GRAVITY FIRST, THEN THE STEER, AND THE ORDER IS A DECISION.
        //
        //   this order    the bolt is re-aimed AFTER falling, so a chasing bolt points AT its
        //                 target and does not shoot low.
        //   the other     the bolt is aimed, then gravity droops it every tick, so it chronically
        //                 undershoots a target it is looking straight at -- a homing arrow that
        //                 always lands at the feet.
        Vec3 nextVelocity = steer(world, caster, spawn, next,
                velocity.add(new Vec3(0, -gravity, 0)), seek);
        world.schedule(next, 1, () ->
                step(world, caster, spawn, next, nextVelocity, gravity, maxLifetimeTicks, nextElapsed,
                        look, seek, markerId, onImpact));
    }

    /**
     * Turn the bolt toward something worth chasing, or hand back what it was already doing.
     *
     * <p><b>A TARGETLESS BOLT KEEPS ITS BALLISTIC BEHAVIOUR</b> -- operator's ruling,
     * {@code PLAN-dragons-plume.md} §3.3, and it is the common case because it is every shot that
     * misses. There is no "fly flat" branch here: the velocity comes back untouched, gravity has
     * already been applied to it by the caller, and the bolt falls like an arrow until something
     * appears to chase. Under the ruled {@code gravity 0.05} a flat stray lands about 22.5 blocks
     * out, which is what keeps the 300-block leash a reach for SEEKING rather than a licence for
     * stray bolts to cross the map.
     *
     * <h2>THE MAGNITUDE IS THE CURRENT ONE, NOT THE LAUNCH SPEED -- A DECISION, NOT A COPY</h2>
     *
     * <p>The inherited algorithm says <i>"renormalised to the ORIGINAL speed"</i>. That fights R12,
     * which rules that the arrow drops like a normal arrow: pinning the magnitude to the launch
     * speed every tick means <b>gravity could only ever bend the direction and never accelerate the
     * fall</b> -- a constant-speed thing that curves, which is not an arrow.
     *
     * <pre>
     * IMPLEMENTED   scale by velocity.length()  -- the speed the bolt actually has, gravity included
     * NOT TAKEN     scale by the launch speed   -- magnitude frozen for the whole flight
     * </pre>
     *
     * <p><b>This applies R12 rather than making a new ruling</b>, and the inherited constant is not
     * being followed off a cliff: a number tuned in another game must not silently decide whether
     * this weapon's arrows accelerate. <b>The alternative is written down so the choice can be
     * reversed with one line</b> if it reads wrong on a boot.
     *
     * <p>It does NOT run away. Gravity adds {@code (0,-g,0)} to a roughly horizontal velocity, so
     * the magnitude grows by about {@code g^2/(2s)} per tick -- at {@code s = 2.5, g = 0.05} that is
     * {@code 0.0005}, about {@code +0.06} over a whole 120-tick leash. What it DOES preserve is real
     * speed a bolt has built up in a long fall, which the launch-speed form would throw away.
     */
    private static Vec3 steer(CombatWorld world, Caster caster, Vec3 spawn, Vec3 from,
                              Vec3 velocity, Seek seek) {
        if (seek == null) return velocity;                       // not a homing bolt at all

        // BALLISTIC UNTIL activationBlocks FROM THE SPAWN POINT. Straight-line, not path length.
        double activation = seek.activationBlocks();
        if (from.distanceSquared(spawn) < activation * activation) return velocity;

        Combatant target = nearestMob(world, caster, from, seek.searchRadius());
        if (target == null) return velocity;                     // nothing to chase: §3.3, it falls

        Vec3 toTarget = target.state().position().subtract(from).normalize();
        if (toTarget.equals(Vec3.ZERO)) return velocity;         // sitting on top of it; no direction

        double speed = velocity.length();
        if (speed == 0) return velocity;                         // normalize() below would be ZERO
        Vec3 turned = velocity.normalize().scale(1 - seek.lerp())
                .add(toTarget.scale(seek.lerp()))
                .normalize();
        return turned.scale(speed);
    }

    /**
     * The nearest MOB within {@code radius} of the bolt, or null.
     *
     * <p><b>R2 IS MOBS ONLY, AND THE SKIP GATES THE ASSIGNMENT RATHER THAN FOLLOWING IT.</b> A
     * filter applied after {@code best} has already been taken is a filter that cannot see the
     * defect: with a player standing nearer than the mob it would still have been chosen and then
     * discarded, leaving the bolt chasing nothing while a mob stood in range. So the {@code
     * continue} comes BEFORE the distance comparison, and the test stages the player NEARER than
     * the mob and registers it FIRST, because a fixture where the wrong answer happens to lose on
     * distance proves nothing.
     *
     * <p>The skip itself is the form already in the tree three times --
     * {@code EffectApplier.applyToNearbyMobs}, {@code Ignite}, {@code SweptLine} -- read off the
     * FROZEN SNAPSHOT so a core test can reach it. No new port method: {@code combatantsNear}
     * already existed, and it is a SPHERE where the inherited constant described a box
     * (see {@code CastSpec.Homing}).
     *
     * <p><b>LINE OF SIGHT IS NOT CONSULTED, AND THAT IS UNRULED RATHER THAN EXCLUDED.</b> A bolt
     * will turn toward a mob through a wall and then bury itself in the wall, because {@code
     * castRay} stops at the block. Nobody has decided whether a chase should require sight; the
     * question was never put, and {@code lineOfSightClear} is a port method that already exists if
     * the answer is yes.
     */
    private static Combatant nearestMob(CombatWorld world, Caster caster, Vec3 from, double radius) {
        Combatant best = null;
        double bestDistanceSquared = Double.POSITIVE_INFINITY;
        for (Combatant candidate : world.combatantsNear(from, radius)) {
            if (candidate.id().equals(caster.id())) continue;
            if (candidate.state().player()) continue;            // R2, and it gates what follows
            double distanceSquared = candidate.state().position().distanceSquared(from);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Retire the body when the bolt resolves.
     *
     * <p><b>IT IS NOT REPOSITIONED TO THE IMPACT POINT FIRST, AND THAT ABSENCE IS A DECISION.</b>
     * Do not read it as an oversight and "fix" it back.
     *
     * <p>There IS a gap. The impact resolves at {@code hit.point()}, somewhere along the segment
     * just traced, while the body sits at the segment's START -- so the flint vanishes between 0
     * and one full step short of the burst, which at the staff's speed 1.4 means a mean of about
     * 0.7 blocks. The old repo had no such gap, because its hit was found within 0.7 blocks of the
     * item's own location and the two coincided by construction.
     *
     * <p>An earlier version closed it with a reposition immediately before the removal. That
     * version <b>compiled, read well, carried an accurate javadoc, and did nothing a player could
     * see</b>: repositioning is verified to move the entity server-side and verified not to reach
     * the client's entity tracker. Keeping it would have left an inert call in the code with a
     * convincing comment attached, which is worse than the gap it pretended to close.
     *
     * <p>So the gap is RECORDED rather than fixed -- measured at the boot, written down in
     * {@code NEXT.md}. If it reads badly, the candidate that stays inside the witnessed mechanism is
     * to drive the marker by {@code (impact - position)} on the resolving tick and delay the removal
     * one tick so the platform's mover actually carries it there. That is sized and deliberately not
     * adopted: it reintroduces a scheduled removal that can fail to run, and it would be the SECOND
     * attempt at closing this gap. It gets a witness before it gets believed.
     *
     * <p>Removing without repositioning also closed a race by deletion. The reposition was
     * asynchronous, and its callback was observed completing AFTER this removal had run -- six
     * refusals in one session, every one of them the last move of a flight. With no reposition there
     * is no in-flight callback to lose to.
     *
     * <p>Called while still on the region owning the marker, so the write is legal.
     */
    private static void resolve(CombatWorld world, UUID markerId) {
        if (markerId == null) return;
        world.removeMarker(markerId);
    }
}
