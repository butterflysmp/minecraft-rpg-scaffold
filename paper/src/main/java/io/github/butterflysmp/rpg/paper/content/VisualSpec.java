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
     * <p>{@code dust} is the DATA OBJECT a data-taking particle needs, and it is typed
     * {@code DustOptions} rather than {@code Object} ON PURPOSE. Eighteen particles on the pinned
     * API declare a non-Void data type, across ten different classes -- BLOCK wants BlockData, ITEM
     * an ItemStack, DUST_COLOR_TRANSITION a DustTransition, VIBRATION a Vibration. The schema can
     * supply exactly ONE of them, and the narrow type says so to the compiler rather than leaving
     * "any data object" as a bound nobody chose. Same move as typing
     * {@code AbilityDefinition.onCast} as {@code List<EffectSpec.Visual>} rather than
     * {@code Untargeted}. Null for every particle that takes no data, which is all five in shipped
     * content.
     *
     * <p>{@code speed} is Bukkit's {@code extra} argument, and <b>its meaning is
     * particle-dependent</b>. For FLAME and SMOKE -- the only two shipped content uses it -- it is
     * how fast the particles drift outward from the spawn point, which is why it is NAMED speed
     * here. It is not speed for every particle: some read it as a scale, some as a colour or a
     * lifetime, and some ignore it. Check what the particle you are authoring does with it rather
     * than assuming this name is the whole story.
     *
     * <p><b>{@code speed} IS INERT FOR DUST</b>, which is the first particle in this repo to
     * ignore it. Author it anyway: an omitted {@code speed} means 1.0, not 0.0, and the next
     * reader should not have to work out for themselves that it does not matter here.
     *
     * <p><b>The default is 1.0, and that is not an arbitrary choice.</b> Before this field existed
     * {@code PaperCombatWorld.present} called the 6-argument {@code spawnParticle}, whose default
     * chain in the pinned Paper API ends at {@code dconst_1} -- so every visual ever authored in
     * this repo has been running at extra 1.0, chosen by nobody. Defaulting to 0.0, the reflexive
     * choice for a new numeric field, would silently restyle every one of them. Do not "tidy" it.
     */
    record Particles(Particle particle, int count, double spread, double speed,
                     Particle.DustOptions dust, double samplesPerBlock) implements VisualSpec {}

    /**
     * How many times a beam is drawn per BLOCK of the segment it runs down. cfde822's value.
     *
     * <p><b>INERT FOR A POINT PRESENT.</b> It is read only by {@code presentAlong}; a visual
     * presented with plain {@code present} ignores it entirely. That is why it may sensibly carry
     * a default at all -- an absent value on a non-beam visual can never be observed.
     *
     * <p>4.0 rather than 0.0 for the same reason {@code speed} defaults to 1.0 and not 0.0: zero
     * is the reflexive default for a new numeric field, and here it would mean a beam that draws
     * NOTHING -- a silent no-op rather than a loud failure, which is the shape of defect this repo
     * has now recorded three times.
     */
    double DEFAULT_SAMPLES_PER_BLOCK = 4.0;

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
