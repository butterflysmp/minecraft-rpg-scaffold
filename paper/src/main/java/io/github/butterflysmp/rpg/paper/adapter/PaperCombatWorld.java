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
     * A real Item, born AT REST at the landing point, so it just sits there bobbing and
     * spinning like any settled dropped item.
     *
     * The whole saga was one root cause: {@code dropItem} builds a vanilla ItemEntity, and that
     * constructor assigns a random "pop" velocity by design -- about (+/-0.1, +0.2, +/-0.1),
     * the loot-scatter behaviour. The +0.2 up was every round's "pop up"; each item's own random
     * +/-0.1 horizontal, integrated over the ~10 ticks before it settled, was the "drifts up to a
     * block away", and three embers each drifting their own way looked like they shoved each
     * other apart (they did not -- non-mergable items overlap, they do not push). The earlier
     * fixes fought that motion: horizontal-cancel kept the +0.2 up (still hopped); the freeze and
     * the noPhysics flags suppressed it after the fact.
     *
     * So do not fight it -- do not create it. spawn the Item directly and zero its velocity in the
     * pre-add function, BEFORE it ever ticks: it is born motionless, so there is no pop to hop, no
     * drift to cancel, nothing to settle. Gravity stays ON (default) and there are no noPhysics or
     * gravity-off flags: with zero initial velocity the item is already at rest on the ground, so
     * gravity has nothing to do and the block-collision jitter (which needed a gravity-less item
     * that could never reach rest) never arises. (world.spawn uses the same popping constructor as
     * dropItem; the fix is the pre-tick velocity zero, not the spawn method.)
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
        ItemStack stack = new ItemStack(material);
        Item marker = world.spawn(toLocation(at), Item.class, item -> {
            item.setItemStack(stack);
            item.setVelocity(new Vector(0, 0, 0));   // born at rest: kill the ItemEntity pop BEFORE it ticks
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
     * The marker's live location, so a fuse can detonate where it actually IS rather than
     * where it was planted -- an ember that popped or fell still bursts under itself. Empty
     * when the marker is gone (removed, or unloaded with its chunk), which sends the fuse
     * back to its planted origin. A read of the entity's own position; getEntity mirrors
     * removeMarker above.
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
