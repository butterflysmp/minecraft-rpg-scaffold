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
     *
     * <p>{@code speed} is Bukkit's {@code extra} argument, and <b>its meaning is
     * particle-dependent</b>. For FLAME and SMOKE -- the only two shipped content uses it -- it is
     * how fast the particles drift outward from the spawn point, which is why it is NAMED speed
     * here. It is not speed for every particle: some read it as a scale, some as a colour or a
     * lifetime, and some ignore it. Check what the particle you are authoring does with it rather
     * than assuming this name is the whole story.
     *
     * <p><b>The default is 1.0, and that is not an arbitrary choice.</b> Before this field existed
     * {@code PaperCombatWorld.present} called the 6-argument {@code spawnParticle}, whose default
     * chain in the pinned Paper API ends at {@code dconst_1} -- so every visual ever authored in
     * this repo has been running at extra 1.0, chosen by nobody. Defaulting to 0.0, the reflexive
     * choice for a new numeric field, would silently restyle every one of them. Do not "tidy" it.
     */
    record Particles(Particle particle, int count, double spread, double speed) implements VisualSpec {}

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
