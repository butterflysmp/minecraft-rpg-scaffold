package io.github.yourname.rpg.paper.adapter;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.element.Element;
import io.github.yourname.rpg.paper.scheduler.Scheduler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;

/** Wraps a Bukkit LivingEntity so core can hit it without knowing what it is. */
public final class BukkitCombatant implements Combatant {
    private final LivingEntity entity;
    private final Scheduler scheduler;
    private final Keys keys;

    public BukkitCombatant(LivingEntity entity, Scheduler scheduler, Keys keys) {
        this.entity = entity;
        this.scheduler = scheduler;
        this.keys = keys;
    }

    public LivingEntity handle() { return entity; }

    @Override public UUID id() { return entity.getUniqueId(); }

    @Override public Vec3 position() {
        var l = entity.getLocation();
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }

    @Override public boolean isAlive() { return !entity.isDead(); }

    @Override public Element shieldElement() {
        // Shield element stored in the entity's PDC -- no NBT reflection.
        String raw = entity.getPersistentDataContainer()
                .get(keys.shieldElement, PersistentDataType.STRING);
        return raw == null ? null : Element.valueOf(raw);
    }

    @Override public void applyDamage(double amount, Element element) {
        scheduler.onEntity(entity, () -> entity.damage(amount));
    }

    @Override public void applyHeal(double amount) {
        scheduler.onEntity(entity, () -> {
            var attr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double max = attr == null ? 20.0 : attr.getValue();
            entity.setHealth(Math.min(max, entity.getHealth() + amount));
        });
    }

    @Override public void applyKnockback(Vec3 direction, double strength) {
        scheduler.onEntity(entity, () -> {
            Vector v = new Vector(direction.x(), direction.y(), direction.z());
            if (v.lengthSquared() > 0) v.normalize().multiply(strength);
            entity.setVelocity(entity.getVelocity().add(v));
        });
    }

    @Override public void applyStatus(String statusId, int durationTicks, int amplifier) {
        scheduler.onEntity(entity, () -> {
            // TODO: route through your StatusRegistry. Placeholder no-op.
        });
    }
}
