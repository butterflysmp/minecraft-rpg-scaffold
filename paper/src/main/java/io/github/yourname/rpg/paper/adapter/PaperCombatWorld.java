package io.github.yourname.rpg.paper.adapter;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.paper.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;

public final class PaperCombatWorld implements CombatWorld {
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
