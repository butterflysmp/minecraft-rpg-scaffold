package io.github.butterflysmp.rpg.core.accessory;

import java.util.Locale;

/**
 * WHAT AN ACCESSORY DOES BESIDE ITS MODIFIERS (the seat's H1, 2026-11-26): at most ONE behaviour per accessory, an effect
 * type the engine knows with its parameters, all declared in the accessory's YAML under {@code behaviour:}. No accessory
 * id is written in Java; {@code AccessoryLoader} builds one of these from a file's block and refuses a malformed one by
 * name. {@code EnchantBehaviour} is the same shape for enchants.
 *
 * <p>A {@code sealed} interface: only the records listed in {@code permits} may implement it, so a {@code switch} over
 * it must answer for every kind (one line for a reader rusty at Java: the compiler checks the list is complete).
 */
public sealed interface AccessoryBehaviour permits AccessoryBehaviour.HealingPulse {

    /** The name a player reads on the tooltip and the stats sheet ("Radiant Pulse"). */
    String name();

    /** Who a behaviour may touch. One member today: Ben, "It heals you and any players near you". */
    enum Targets {
        PLAYERS;

        /** Case-insensitive lookup for the loader. Null on a miss, so the caller names the file. */
        public static Targets fromName(String name) {
            if (name == null) return null;
            for (Targets t : values()) if (t.name().equalsIgnoreCase(name.trim())) return t;
            return null;
        }

        public String token() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * {@code type: healing_pulse} (the Healing Tome's Radiant Pulse; Ben, 2026-11-26: "Heals 1 health per the players level
     * that has it equipped. A level 50 player would heal 50. Pulse every 4 seconds. 8 block radius", "No mana cost", "It
     * heals you and any players near you", "Only heals when someone is hurt"). While the accessory CONTRIBUTES, every
     * {@code intervalTicks} the wearer heals each hurt {@code targets} candidate within {@code radius} blocks (feet to feet)
     * by {@code min(level x healPerLevel, missing)} ({@link HealingPulses#decide}).
     *
     * <p>{@code particle}, {@code particleCount}, {@code heartParticle} and {@code sound} are the feedback on each player
     * actually healed (H9, SEAT PLACEHOLDER 2026-11-26, the old game's): a Minecraft particle name, how many, a second
     * particle shown once (the heart), and a Minecraft sound key played to the healed player only. Each may be null for
     * none (a count of 0 with a particle is refused, as a typo would be).
     */
    record HealingPulse(String name, double healPerLevel, int intervalTicks, double radius, Targets targets,
                        String particle, int particleCount, String heartParticle, String sound)
            implements AccessoryBehaviour {
        public HealingPulse {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("healing_pulse needs a name");
            if (!(healPerLevel > 0) || Double.isInfinite(healPerLevel)) {
                throw new IllegalArgumentException("healing_pulse heal_per_level must be above 0, was " + healPerLevel);
            }
            if (intervalTicks < 1) {
                throw new IllegalArgumentException("healing_pulse interval_ticks must be 1 or more, was " + intervalTicks);
            }
            if (!(radius > 0) || Double.isInfinite(radius)) {
                throw new IllegalArgumentException("healing_pulse radius must be above 0, was " + radius);
            }
            if (targets == null) throw new IllegalArgumentException("healing_pulse needs targets (players)");
            if (particle != null && particle.isBlank()) particle = null;
            if (heartParticle != null && heartParticle.isBlank()) heartParticle = null;
            if (sound != null && sound.isBlank()) sound = null;
            if (particle != null && particleCount < 1) {
                throw new IllegalArgumentException("healing_pulse particle_count must be 1 or more with a particle, was "
                        + particleCount);
            }
        }
    }
}
