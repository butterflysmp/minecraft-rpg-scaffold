package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.paper.content.VisualDefinition;
import io.github.butterflysmp.rpg.paper.content.VisualSpec;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class PaperCombatWorld implements CombatWorld {

    /** How much to inflate entity hitboxes when tracing. 0 = exact bounding box. */
    private static final double RAY_SIZE = 0.0;

    private final World world;
    private final AdapterContext ctx;

    public PaperCombatWorld(World world, AdapterContext ctx) {
        this.world = world;
        this.ctx = ctx;
    }

    private Location toLocation(Vec3 v) {
        return new Location(world, v.x(), v.y(), v.z());
    }

    /**
     * MUST run on the thread that owns {@code center}'s region.
     * World#getNearbyEntities is illegal anywhere else.
     *
     * Three entry points reach here. Only one of them provably satisfies that:
     *
     *   - Rescheduled area pulses, via EffectApplier.tickArea -> schedule() ->
     *     onRegionLater(origin, ...). Correct: the hop names the area's own origin.
     *
     *   - EffectApplier's inline Burst, and CastExecutor.meleeTarget. Both run on
     *     whatever thread CastExecutor.execute was called on, which RpgCommand sets
     *     to the region owning the caster's EYE -- not the burst's origin.
     *
     * For Melee the eye and the target are within a few blocks, so they share a
     * region in practice. For a Burst at the far end of a 30-block Ray they need
     * not. This method is therefore called, today, on a thread that may not own
     * {@code center}.
     *
     * Do not read this as permission. It is a Folia-only defect: on Paper every
     * region scheduler runs on the main thread, so no test and no local server can
     * reproduce it. See NEXT.md, Commit C -- and the javadoc on present(), which
     * hops correctly and explains why.
     */
    @Override
    public Collection<Combatant> combatantsNear(Vec3 center, double radius) {
        return world.getNearbyEntities(toLocation(center), radius, radius, radius).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(e -> BukkitCombatant.of(e, ctx)) // snapshot taken here, on this thread
                .toList();
    }

    /**
     * Fetches the caster's handle for a Self cast, whose Success carries only a snapshot.
     * Null-safe by way of Optional: the caster may have died or logged out between deciding
     * the cast and resolving it.
     */
    @Override
    public Optional<Combatant> combatant(UUID id) {
        if (world.getEntity(id) instanceof LivingEntity living) {
            return Optional.of(BukkitCombatant.of(living, ctx));
        }
        return Optional.empty();
    }

    /**
     * MUST run on the thread owning every region the segment touches -- World#rayTrace
     * reads blocks and entities along its whole length, not just at its ends.
     *
     * A projectile's segment is one tick of flight, a block or two, so it lies inside
     * one region and CastExecutor.step re-enters the correct one each tick. A Ray's
     * segment is its entire range -- CastSpec.Ray defaults to 30 blocks -- and no
     * single thread owns all of it. That call is the Folia defect on combatantsNear.
     *
     * One trace covers blocks and entities together, so a grenade cannot pass
     * through a wall to reach someone standing behind it.
     */
    @Override
    public Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId) {
        Vec3 along = to.subtract(from);
        double distance = along.length();
        if (distance <= 0) return Optional.empty();

        Vector direction = new Vector(along.x(), along.y(), along.z()).normalize();
        RayTraceResult result = world.rayTrace(
                toLocation(from), direction, distance,
                FluidCollisionMode.NEVER, /* ignorePassableBlocks */ true, RAY_SIZE,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(ignoreId));

        if (result == null) return Optional.empty();

        Vector hit = result.getHitPosition();
        Vec3 point = new Vec3(hit.getX(), hit.getY(), hit.getZ());

        if (result.getHitEntity() instanceof LivingEntity living) {
            return Optional.of(RayHit.ofCombatant(point, BukkitCombatant.of(living, ctx)));
        }
        return Optional.of(RayHit.ofBlock(point));
    }

    @Override
    public void schedule(Vec3 near, int delayTicks, Runnable task) {
        ctx.scheduler().onRegionLater(toLocation(near), task, delayTicks);
    }

    /**
     * The project's only spawned entity: a marker showing where a delayed burst will go off.
     * A real dropped Item so it bobs and spins like any dropped item, spawned at the landing
     * point and made to STAY exactly there.
     *
     * The history matters. Freezing it (velocity 0 + gravity off) buzzed: with gravity off it
     * could never reach its onGround rest, so vanilla's per-tick block-collision settle kept
     * nudging it. Letting it settle naturally (gravity on) stopped the buzz but three embers
     * land in a tight fan, and clustered Item entities on the ground get shoved by vanilla
     * collision -- they popped up and pushed a block apart.
     *
     * The fix is setNoPhysics(true): the item ignores block AND entity collision, so nothing
     * ejects or shoves it. Per the Paper API, noPhysics and gravity are INDEPENDENT -- noPhysics
     * is pure noclip and does not disable gravity -- so on its own a noPhysics item would fall
     * THROUGH the world. Hence setGravity(false) is required alongside it. That gravity-off does
     * NOT bring the buzz back, because the buzz was the block-collision settle loop, and
     * setNoPhysics(true) turns that loop off entirely: there is no collision left to fight, so
     * nothing nudges it. velocity zeroed once (not per-tick) kills the drop's pop so noclip has
     * no residual to drift on. Net: it sits exactly where it landed.
     *
     * setPickupDelay(MAX) keeps it un-collectible (and, at the 32767 clamp, non-mergable and
     * non-despawning); the ~1s fuse makes vanilla item edge cases irrelevant anyway.
     * setPersistent(false) is the unload backstop. Its normal removal is the fuse task, which
     * calls removeMarker below -- a leaked real Item is the identical hazard to a leaked display,
     * unchanged.
     *
     * A world write, so like every other here it is only legal on the thread owning {@code at}
     * -- the caller (a projectile impact on its region thread) already satisfies that, the same
     * as an inline Burst.
     */
    @Override
    public UUID spawnMarker(Vec3 at, String markerId) {
        Material material = Material.matchMaterial(markerId);
        if (material == null || !material.isItem()) {
            ctx.warnOnce("Unknown marker material '" + markerId + "'; using BLAZE_POWDER");
            material = Material.BLAZE_POWDER;
        }
        Item marker = world.dropItem(toLocation(at), new ItemStack(material), item -> {
            item.setPickupDelay(Integer.MAX_VALUE);  // never collectible
            item.setPersistent(false);               // unload backstop
            item.setGravity(false);                  // don't fall -- REQUIRED, noPhysics is noclip
            item.setNoPhysics(true);                 // ignore block/entity collision: no eject, no shove
        });
        // Kill the drop's pop once so noclip has no residual velocity to drift on. Not per-tick.
        marker.setVelocity(new Vector(0, 0, 0));
        return marker.getUniqueId();
    }

    @Override
    public void removeMarker(UUID markerId) {
        if (world.getEntity(markerId) instanceof Item marker) {
            marker.remove();
        }
    }

    /**
     * Play a named visual at a point. An unknown id is a content mistake, not a
     * programming error: warn once and let the rest of the detonation land.
     *
     * The onRegion hop is not redundant. Callers reach here already on a region
     * thread, but not necessarily the one owning {@code at}: RpgCommand hops onto
     * the region of the caster's EYE, and a Ray can land its impact thirty blocks
     * away, in another region. spawnParticle and playSound are world writes and
     * are only legal on the thread owning this location.
     */
    @Override
    public void present(Vec3 at, String visualId) {
        VisualDefinition visual = ctx.visuals().find(visualId).orElse(null);
        if (visual == null) {
            ctx.warnOnce("Unknown visual_id '" + visualId + "'; nothing presented");
            return;
        }
        Location loc = toLocation(at);
        ctx.scheduler().onRegion(loc, () -> {
            for (VisualSpec step : visual.steps()) {
                switch (step) {
                    case VisualSpec.Particles p ->
                            world.spawnParticle(p.particle(), loc, p.count(),
                                    p.spread(), p.spread(), p.spread());
                    case VisualSpec.Sound s ->
                            world.playSound(loc, s.key(), s.volume(), s.pitch());
                }
            }
        });
    }
}
