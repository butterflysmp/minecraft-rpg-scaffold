package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.Entity;

/**
 * WHERE OUR OWN DAMAGE MEETS THE PHALANX'S SHOT (dragon-mobs step R7a, the seat's ruling 2). Every player attack that
 * our pipeline deals (a weapon's ability, a burst, an area pulse, a sweep, a dash, an Ignite blast) ends in one of
 * {@link BukkitCombatant}'s handle calls; each asks this FIRST, on the target's own thread, and stops if it says the
 * target was a shot. So "any attack destroys it" holds for every shape at one place, and the shot is never handed a
 * status, a knockback or a health change.
 *
 * <p>Vanilla's own damage (a melee swing, an arrow, a vanilla explosion, fire, {@code /kill}) never reaches these
 * handles; it is met by {@code RpgListeners.onShotDamaged}, at LOWEST, on the vanilla event.
 */
public interface ShotSink {

    /**
     * If {@code entity} is a live shot's carrier, destroy it and log what did ({@code by}); true. Else false and
     * nothing happens. On the entity's own thread.
     */
    boolean strike(Entity entity, String by);

    /**
     * The sink as the context is built, before the shooter exists (the shooter needs the context). Bound once at
     * enable; until then, and in a unit test that never binds it, nothing is a shot.
     */
    final class Late implements ShotSink {
        private volatile ShotSink to;

        public void bind(ShotSink sink) {
            this.to = sink;
        }

        @Override
        public boolean strike(Entity entity, String by) {
            ShotSink s = to;
            return s != null && s.strike(entity, by);
        }
    }
}
