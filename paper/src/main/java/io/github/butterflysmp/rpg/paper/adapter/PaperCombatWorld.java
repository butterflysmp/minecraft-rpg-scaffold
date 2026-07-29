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

    /**
     * How far ABOVE the caster's feet to release a thrown marker item, so it leaves from about
     * hand/eye height rather than the ground -- the old Blast Fungus threw from eye level and
     * kept the item moving, which is why it never rested inside a block and never popped. A
     * Y-only lift: X/Z stay the throw origin, so the item keeps the caster's column.
     */
    private static final double THROW_ORIGIN_LIFT = 1.4;

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
     * The caster's resolved ATTACK_DAMAGE, off the same custom store the reconcile loop feeds. Runs on
     * the caster's own thread (a melee WeaponDamage lands with the caster within reach), so reading the
     * store here is safe -- see the port javadoc and the NEXT.md note about ranged reuse.
     */
    @Override
    public double attackDamage(UUID id) {
        return ctx.stats().attackValue(id);
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
     * The project's only spawned entity: a thrown ember. A real Item launched from the caster
     * with {@code velocity}; vanilla physics flies and lands it, so it arcs, bounces, and rolls
     * to rest like any thrown item. The item IS the marker -- the fuse detonates at its LIVE
     * position (see {@link #markerLocation}), so where or how it settles does not matter, and
     * there is no landing detection and no separate display entity.
     *
     * It is released {@link #THROW_ORIGIN_LIFT} above {@code origin} (the caster's feet) so it
     * leaves from about hand height, not the ground. This is the whole reason the earlier
     * resting-marker approach could be thrown out: that one PLACED an item at a computed landing
     * point on a block face, and vanilla ejected it upward to resolve the intersection -- the
     * pop that six rounds of velocity-zeroing and settle-fighting never cured. A thrown item is
     * never set down inside a block, so the pop cannot arise. We do NOT zero the velocity here:
     * the point is that it flies.
     *
     * setPickupDelay(MAX) keeps it un-collectible (and, at the 32767 clamp, non-mergable and
     * non-despawning); the short fuse makes vanilla item edge cases irrelevant anyway.
     * setPersistent(false) is the unload backstop. Its normal removal is the fuse task, which
     * calls removeMarker below -- a leaked real Item is the leak-on-death hazard one more time.
     *
     * A world write, so only legal on the thread owning {@code origin} -- the caller (a cast
     * resolving on the caster's region) already satisfies that.
     */
    @Override
    public UUID throwMarker(Vec3 origin, Vec3 velocity, String itemId) {
        Material material = Material.matchMaterial(itemId);
        if (material == null || !material.isItem()) {
            ctx.warnOnce("Unknown marker material '" + itemId + "'; using BLAZE_POWDER");
            material = Material.BLAZE_POWDER;
        }
        ItemStack stack = new ItemStack(material);
        Location spawnAt = toLocation(origin).add(0, THROW_ORIGIN_LIFT, 0);
        Item marker = world.spawn(spawnAt, Item.class, item -> {
            item.setItemStack(stack);
            item.setVelocity(new Vector(velocity.x(), velocity.y(), velocity.z())); // thrown -- it flies
            item.setPickupDelay(Integer.MAX_VALUE);  // never collectible
            item.setPersistent(false);               // unload backstop
        });
        return marker.getUniqueId();
    }

    @Override
    public void removeMarker(UUID markerId) {
        if (world.getEntity(markerId) instanceof Item marker) {
            marker.remove();
        }
    }

    /**
     * The marker's live location, so a fuse can detonate where the thrown item actually IS at
     * fuse-end -- wherever physics carried it -- rather than where it was thrown. Empty when the
     * item is gone (removed, or unloaded with its chunk), which sends the fuse back to its throw
     * origin. A read of the entity's own position; getEntity mirrors removeMarker above.
     */
    @Override
    public Optional<Vec3> markerLocation(UUID markerId) {
        if (world.getEntity(markerId) instanceof Item marker) {
            Location loc = marker.getLocation();
            return Optional.of(new Vec3(loc.getX(), loc.getY(), loc.getZ()));
        }
        return Optional.empty();
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
