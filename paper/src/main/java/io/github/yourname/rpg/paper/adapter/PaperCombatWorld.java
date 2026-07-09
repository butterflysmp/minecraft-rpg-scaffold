package io.github.yourname.rpg.paper.adapter;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.combat.RayHit;
import io.github.yourname.rpg.paper.scheduler.Scheduler;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class PaperCombatWorld implements CombatWorld {

    /** How much to inflate entity hitboxes when tracing. 0 = exact bounding box. */
    private static final double RAY_SIZE = 0.0;

    private final World world;
    private final Scheduler scheduler;
    private final Keys keys;

    public PaperCombatWorld(World world, Scheduler scheduler, Keys keys) {
        this.world = world;
        this.scheduler = scheduler;
        this.keys = keys;
    }

    private Location toLocation(Vec3 v) {
        return new Location(world, v.x(), v.y(), v.z());
    }

    /**
     * MUST run on the thread that owns {@code center}'s region.
     * World#getNearbyEntities is illegal anywhere else.
     *
     * Two entry points reach here, and both satisfy that:
     *   - EffectApplier's first application, which the caller is required to
     *     wrap in Scheduler.onRegion(impactPoint, ...) -- see AbilityService.cast.
     *   - Rescheduled area pulses, which arrive via schedule() -> onRegionLater.
     */
    @Override
    public Collection<Combatant> combatantsNear(Vec3 center, double radius) {
        return world.getNearbyEntities(toLocation(center), radius, radius, radius).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(e -> (Combatant) new BukkitCombatant(e, scheduler, keys))
                .toList();
    }

    /**
     * MUST run on the thread that owns the segment's region -- World#rayTrace
     * reads blocks and entities. See combatantsNear for who calls in here.
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
            return Optional.of(RayHit.ofCombatant(point, new BukkitCombatant(living, scheduler, keys)));
        }
        return Optional.of(RayHit.ofBlock(point));
    }

    @Override
    public void schedule(Vec3 near, int delayTicks, Runnable task) {
        scheduler.onRegionLater(toLocation(near), task, delayTicks);
    }

    @Override
    public void present(Vec3 at, String visualId) {
        scheduler.onRegion(toLocation(at), () -> {
            // TODO: look visualId up in a VisualRegistry.
            // Vanilla particles first. Reach for PacketEvents only when the
            // Bukkit API genuinely cannot express the effect.
        });
    }
}
