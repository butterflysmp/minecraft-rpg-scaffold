package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * THE ONE POINT A MOB'S ONE-HIT SHIELD IS DECIDED (dragon-mobs step R7b, the seat's ruling 5). Every path that takes
 * custom health from a mob ends in {@code BukkitCombatant}'s {@code applyDamage}: our weapons and abilities, the
 * player-melee rider and its sweep, a vanilla projectile, a fall, fire, an explosion and every other environmental
 * cause rerouted by {@code RpgListeners.onEnvironmentalDamage}, a mob's beam, blast or shot. It asks {@link #absorb}
 * first. {@code /kill} and the void are passed to vanilla before that point, so they ignore the shield.
 */
public interface ShieldSink {

    /**
     * If {@code entity}'s shield is up and {@code amount} spends it: spend it (once per life), play its look, log it,
     * and return true, so the hit takes nothing. Else false. On the entity's own thread.
     */
    boolean absorb(LivingEntity entity, double amount, String by);

    /**
     * NOTHING RIDES THROUGH AN ABSORBED HIT: a status, a push or an impulse on {@code entity} in the same tick as its
     * absorb is dropped and logged; true. Else false.
     */
    boolean suppressRider(LivingEntity entity, String what);

    /** Is {@code entity}'s shield up? (A vanilla push on a shielded mob is the push of a hit the shield will eat.) */
    boolean up(Entity entity);

    /** The sink as the context is built, bound once at enable; unbound, no mob has a shield. */
    final class Late implements ShieldSink {
        private volatile ShieldSink to;

        public void bind(ShieldSink sink) {
            this.to = sink;
        }

        @Override public boolean absorb(LivingEntity entity, double amount, String by) {
            ShieldSink s = to;
            return s != null && s.absorb(entity, amount, by);
        }

        @Override public boolean suppressRider(LivingEntity entity, String what) {
            ShieldSink s = to;
            return s != null && s.suppressRider(entity, what);
        }

        @Override public boolean up(Entity entity) {
            ShieldSink s = to;
            return s != null && s.up(entity);
        }
    }
}
