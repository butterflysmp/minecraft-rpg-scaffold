package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CombatantHandle;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.paper.content.StatusDefinition;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
        return new Combatant(snapshot(entity), new Handle(entity, ctx));
    }

    /**
     * Freeze everything core needs to know. MUST be called on the thread owning
     * {@code entity} -- enforced, not merely asked for.
     */
    public static CombatantSnapshot snapshot(LivingEntity entity) {
        Regions.requireOwned(entity);
        var location = entity.getLocation();
        return new CombatantSnapshot(
                entity.getUniqueId(),
                new Vec3(location.getX(), location.getY(), location.getZ()),
                !entity.isDead());
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

                    // Mob-only for now. A player would fight the client's own movement
                    // prediction and get a broken half-immobilize; skip rather than half-work.
                    // MOVEMENT_SPEED=0 kills the AI drive; velocity-zero kills knockback/jumps.
                    // Rooted and Freeze are the two configurations: Freeze uses its own instance
                    // and key (so all three movement modifiers coexist) and adds attack
                    // suppression -- the RpgListeners cancel a frozen mob's attacks while
                    // ctx.freeze().isImmobilized(id) holds.
                    case StatusDefinition.Immobilize immobilize -> {
                        if (entity instanceof Player) return;
                        RepeatingTaskTarget target = new EntityTaskTarget(entity, ctx.scheduler());
                        if (immobilize.suppressAttacks()) {
                            SpeedAttribute speed = new EntitySpeedAttribute(entity, ctx.keys().freeze);
                            ctx.freeze().apply(entity.getUniqueId(), target, speed, durationTicks, () -> {
                                entity.setVelocity(new Vector(0, 0, 0));
                                // A frozen creeper does not explode: pause its swell each tick so it
                                // never sits primed and detonates the instant it unfreezes. The
                                // ExplosionPrimeEvent cancel in RpgListeners is the guaranteed backstop.
                                if (entity instanceof Creeper creeper) {
                                    creeper.setIgnited(false);
                                    creeper.setFuseTicks(creeper.getMaxFuseTicks());
                                }
                            });
                        } else {
                            SpeedAttribute speed = new EntitySpeedAttribute(entity, ctx.keys().rooted);
                            ctx.immobilize().apply(entity.getUniqueId(), target, speed, durationTicks,
                                    () -> entity.setVelocity(new Vector(0, 0, 0)));
                        }
                    }

                    // Stacking slow. Mob-only for the same reason as Immobilize. The stack +
                    // cleanup logic lives in SoakedStatus; here we only bind the two seams.
                    case StatusDefinition.Soaked ignored -> {
                        if (entity instanceof Player) return;
                        RepeatingTaskTarget target = new EntityTaskTarget(entity, ctx.scheduler());
                        SpeedAttribute speed = new EntitySpeedAttribute(entity, ctx.keys().soaked);
                        ctx.soaked().apply(entity.getUniqueId(), target, speed, durationTicks);
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
