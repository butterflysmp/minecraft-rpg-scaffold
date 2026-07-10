package io.github.yourname.rpg.paper.adapter;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.element.Element;
import io.github.yourname.rpg.paper.content.StatusDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

/** Wraps a Bukkit LivingEntity so core can hit it without knowing what it is. */
public final class BukkitCombatant implements Combatant {
    private final LivingEntity entity;
    private final AdapterContext ctx;

    public BukkitCombatant(LivingEntity entity, AdapterContext ctx) {
        this.entity = entity;
        this.ctx = ctx;
    }

    public LivingEntity handle() { return entity; }

    @Override public UUID id() { return entity.getUniqueId(); }

    @Override public Vec3 position() {
        var l = entity.getLocation();
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }

    @Override public boolean isAlive() { return !entity.isDead(); }

    /**
     * Shield element stored in the entity's PDC -- no NBT reflection.
     *
     * Nothing in this plugin writes that key, so every value in it came from somewhere
     * else: another plugin, a datapack, an operator's /data. It is untrusted input, and
     * it is read on the damage path. Element.valueOf used to be called here directly,
     * which meant one badly tagged mob threw mid-detonation -- aborting the remaining
     * targets of the burst, and stopping the lingering area from ever rescheduling
     * itself, because tickArea applies its effects before it schedules the next pulse.
     *
     * So: warn once, treat it as unshielded, and let the detonation finish.
     */
    @Override public Element shieldElement() {
        String raw = entity.getPersistentDataContainer()
                .get(ctx.keys().shieldElement, PersistentDataType.STRING);
        if (raw == null) return null; // unshielded, and the overwhelmingly common case

        Element element = Element.fromName(raw);
        if (element == null) {
            ctx.warnOnce("Entity " + entity.getUniqueId() + " has an unrecognised "
                    + ctx.keys().shieldElement + " of '" + raw + "'; treating as unshielded");
        }
        return element;
    }

    @Override public void applyDamage(double amount, Element element) {
        ctx.scheduler().onEntity(entity, () -> entity.damage(amount));
    }

    @Override public void applyHeal(double amount) {
        ctx.scheduler().onEntity(entity, () -> {
            var attr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double max = attr == null ? 20.0 : attr.getValue();
            entity.setHealth(Math.min(max, entity.getHealth() + amount));
        });
    }

    @Override public void applyKnockback(Vec3 direction, double strength) {
        ctx.scheduler().onEntity(entity, () -> {
            Vector v = new Vector(direction.x(), direction.y(), direction.z());
            if (v.lengthSquared() > 0) v.normalize().multiply(strength);
            entity.setVelocity(entity.getVelocity().add(v));
        });
    }

    /**
     * duration and amplifier come from the ability; the status definition only says
     * what kind of thing a "scorch" is. An unknown id is a content mistake, not a
     * programming error: warn once and let the rest of the detonation land.
     */
    @Override public void applyStatus(String statusId, int durationTicks, int amplifier) {
        StatusDefinition def = ctx.statuses().find(statusId).orElse(null);
        if (def == null) {
            ctx.warnOnce("Unknown status_id '" + statusId + "'; no status applied");
            return;
        }
        ctx.scheduler().onEntity(entity, () -> {
            switch (def) {
                // Extend a burn, never shorten it. A 2s scorch pulse must not stomp
                // the 10s burn someone already took from flint and steel.
                case StatusDefinition.Fire ignored ->
                        entity.setFireTicks(Math.max(entity.getFireTicks(), durationTicks));

                case StatusDefinition.Potion potion -> {
                    PotionEffectType type = potionEffect(potion.potionType());
                    if (type == null) {
                        ctx.warnOnce("Unknown potion_type '" + potion.potionType()
                                + "' for status '" + statusId + "'; no status applied");
                        return;
                    }
                    // PotionEffect's amplifier is 0-based: 0 is level I.
                    entity.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
                }
            }
        });
    }

    /**
     * The key's syntax was validated at load, so this cannot throw. A null means the
     * key is well-formed but names no effect -- which ContentValidator already warned
     * about at startup. MOB_EFFECT, not the obsolete EFFECT alias.
     */
    private static PotionEffectType potionEffect(NamespacedKey key) {
        return Registry.MOB_EFFECT.get(key);
    }
}
