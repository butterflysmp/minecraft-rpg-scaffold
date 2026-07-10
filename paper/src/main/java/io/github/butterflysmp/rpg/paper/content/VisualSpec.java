package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;

/**
 * One step of a visual. A visual is a list of these, not a single particle:
 * a grenade needs a burst and a bang, and an ultimate needs layers.
 *
 * Sealed so that the switch in PaperCombatWorld.present is exhaustive: add a
 * Beam and the compiler names every place that must handle it. Same reason
 * EffectSpec is sealed.
 *
 * Lives in paper, not core, because Particle and NamespacedKey are Bukkit types.
 */
public sealed interface VisualSpec permits VisualSpec.Particles, VisualSpec.Sound {

    /**
     * Particle is a plain enum, so it is resolved here at load time. A typo
     * fails the file rather than the first cast.
     */
    record Particles(Particle particle, int count, double spread) implements VisualSpec {}

    /**
     * A vanilla sound, kept as a key rather than an org.bukkit.Sound: that type
     * is registry-backed and cannot be resolved without a running server.
     *
     * {@code key} is what World#playSound takes. {@code namespacedKey} is the
     * same value already parsed, so ContentValidator can look it up in
     * Registry.SOUND_EVENT at startup without re-parsing.
     *
     * Named Sound deliberately; org.bukkit.Sound is never imported here.
     */
    record Sound(String key, NamespacedKey namespacedKey, float volume, float pitch)
            implements VisualSpec {}
}
