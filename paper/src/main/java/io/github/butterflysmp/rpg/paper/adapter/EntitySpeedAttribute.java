package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * Binds a real LivingEntity's MOVEMENT_SPEED attribute to the Bukkit-free {@link SpeedAttribute}
 * seam. 1:1 with the AttributeInstance API, no logic -- the replace/countdown/cleanup decisions
 * all live in the tested {@link SoakedStatus}, so all that is boot-witnessed here is that the
 * add/remove actually move the mob's speed and that removeModifier truly restores the base.
 *
 * MULTIPLY_SCALAR_1 with amount (factor - 1) makes the single modifier multiply base by factor
 * (0.9 -> -0.1 -> base*0.9). getAttribute is nullable (some entities lack the attribute), so
 * every op null-checks -- which also means an op against a removed entity is a no-op, not a throw.
 */
public final class EntitySpeedAttribute implements SpeedAttribute {

    private final LivingEntity entity;
    private final NamespacedKey key;

    public EntitySpeedAttribute(LivingEntity entity, NamespacedKey key) {
        this.entity = entity;
        this.key = key;
    }

    @Override public boolean hasSpeedModifier() {
        AttributeInstance attr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        return attr != null && attr.getModifier(key) != null;
    }

    @Override public void addSpeedModifier(double factor) {
        AttributeInstance attr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.addModifier(new AttributeModifier(
                key, factor - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    @Override public void removeSpeedModifier() {
        AttributeInstance attr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null && attr.getModifier(key) != null) attr.removeModifier(key);
    }
}
