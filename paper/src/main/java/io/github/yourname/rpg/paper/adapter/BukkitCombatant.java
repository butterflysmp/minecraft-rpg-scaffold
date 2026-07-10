package io.github.yourname.rpg.paper.adapter;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.combat.CombatantHandle;
import io.github.yourname.rpg.core.combat.CombatantSnapshot;
import io.github.yourname.rpg.core.element.Element;
import io.github.yourname.rpg.paper.content.StatusDefinition;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Wraps a Bukkit LivingEntity as the two halves of the core port.
 *
 * The snapshot is read HERE, on the thread that owns the entity, and frozen. The handle
 * only dispatches: every mutator hops onto the entity's own thread and returns nothing.
 * That asymmetry is why the port is two types -- a reader must return a value, and you
 * cannot hop a thread and still return synchronously.
 */
public final class BukkitCombatant {

    private BukkitCombatant() {}

    /** State to read plus a handle to act on. MUST be called on the thread owning {@code entity}. */
    public static Combatant of(LivingEntity entity, AdapterContext ctx) {
        return new Combatant(snapshot(entity, ctx), new Handle(entity, ctx));
    }

    /**
     * Freeze everything core needs to know. MUST be called on the thread owning
     * {@code entity} -- enforced, not merely asked for.
     */
    public static CombatantSnapshot snapshot(LivingEntity entity, AdapterContext ctx) {
        Regions.requireOwned(entity);
        var location = entity.getLocation();
        return new CombatantSnapshot(
                entity.getUniqueId(),
                new Vec3(location.getX(), location.getY(), location.getZ()),
                !entity.isDead(),
                shieldElement(entity, ctx));
    }

    /**
     * Shield element stored in the entity's PDC -- no NBT reflection.
     *
     * Nothing in this plugin writes that key, so every value in it came from somewhere
     * else: another plugin, a datapack, an operator's /data. It is untrusted input, so an
     * unrecognised value warns once and reads as unshielded rather than throwing.
     */
    private static Element shieldElement(LivingEntity entity, AdapterContext ctx) {
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

    /** Dispatches onto the entity's own thread. Never reads the world, never returns state. */
    private record Handle(LivingEntity entity, AdapterContext ctx) implements CombatantHandle {

        @Override public UUID id() { return entity.getUniqueId(); }

        /**
         * The amount arrives already multiplied by the elemental matrix; EffectApplier did
         * that against the snapshot's shield. All this port carries is a number and a culprit.
         */
        @Override public void applyDamage(double amount, UUID sourceId) {
            ctx.scheduler().onEntity(entity, () -> {
                Entity source = Attribution.attributableSource(
                        sourceId, entity.getWorld()::getEntity, Bukkit::isOwnedByCurrentRegion);
                if (source != null) {
                    entity.damage(amount, source); // mobs aggro the caster; kills are credited
                } else {
                    entity.damage(amount);         // unresolvable or another region's: no lie
                }
            });
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
}
