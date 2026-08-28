package io.github.butterflysmp.rpg.core.combat;

/**
 * What a SWEPT mob takes: a fraction of the number the primary target took on the same swing.
 *
 * <p>Vanilla's sweeping sword raises one damage event per neighbouring mob, and this is the whole of
 * what we do with them -- pick up the primary's figure and scale it. Nothing else about the swing is
 * re-derived here, which is the point of the design: because a swept mob's damage IS a fraction of
 * the primary's final figure, sweep inherits the enchant percentage, the class damage bonus and the
 * charge BY CONSTRUCTION. There is no second multiplier chain to keep in step, so there is nothing
 * for the two to disagree about.
 *
 * <p><b>A fraction of the PRIMARY, not a fraction of the weapon.</b> The rejected alternative --
 * re-running the weapon's payload at a reduced amount -- is what the old project did, and it is
 * strictly worse in two ways this class exists to avoid. It would re-apply the whole on_hit payload
 * (statuses, visuals) to every bystander, turning a sweep into a free area-of-effect ability; and it
 * would bill a durability use per body, which {@code CastExecutor}'s melee arm already warns about
 * in as many words. A number multiplied by a fraction does neither.
 *
 * <p><b>The number is PRE-mitigation, and that is forced rather than chosen.</b> "What the primary
 * got hit for" would ideally be its post-mitigation figure, but that figure does not exist yet when
 * a sweep event fires: {@code CombatantHandle.applyDamage} is deferred onto the victim's entity
 * scheduler and lands on the NEXT tick, while vanilla raises every sweep event inside the same
 * synchronous {@code Player#attack} as the primary. So the seam feeding this reports the swing's
 * pre-mitigation output, and each swept mob then mitigates its own Defense exactly once -- which is
 * also the reading that avoids double-counting armor. Today it is moot either way: mobs are never
 * reconciled, so every mob resolves to 0 defense and the two figures are the same number. It starts
 * to matter the day mobs are given defense.
 *
 * <p><b>What it does NOT inherit: the crit.</b> Vanilla's 1.5x crit lands on the TOKENED vanilla
 * damage and contributes nothing to the custom number, so there is no crit in the primary's figure
 * for a fraction of it to carry. Sweep will inherit it for free on the day a crit multiplier reaches
 * the custom amount, by the same construction that gives it the enchant and the class bonus.
 *
 * <p><b>No input guards, deliberately.</b> {@code WeaponDefinition} rejects a negative fraction at
 * the content boundary, which is the only surface an author can reach, and the damage seam only ever
 * emits a positive amount because it fires inside {@code EffectApplier}'s {@code amount > 0} gate.
 * Bounding it again here would be a guard in the kernel for an input the kernel cannot receive --
 * and would hide, rather than surface, a loader that had stopped validating.
 *
 * <p>Worked: {@code of(14.2, 0.5) -> 7.1}; {@code of(8, 0.5) -> 4.0}; {@code of(8, NONE) -> 0.0}.
 */
public final class SweepShare {

    private SweepShare() {}

    /**
     * A weapon that does not sweep. The absent value for a weapon-level {@code sweep:} field, and
     * the same 0.0-is-absent convention {@code attack_damage} and {@code attack_speed} already use.
     */
    public static final double NONE = 0.0;

    /**
     * Does this fraction declare a sweep at all?
     *
     * <p>ONE predicate, read from two opposite ends -- {@code WeaponDefinition} asks it to decide
     * whether a declared sweep needs a melee trigger to fire from, and the sweep rider asks it to
     * decide whether to deal anything. If those two ever disagreed about what "declares a sweep"
     * means, a weapon could validate as sweeping and then sweep nothing, which is exactly the silent
     * no-op the content guard exists to prevent.
     */
    public static boolean sweeps(double fraction) {
        return fraction > NONE;
    }

    /**
     * The damage one swept mob takes, given what the primary took and the weapon's declared fraction.
     *
     * <p>A plain multiply, and it should stay one. Every modifier that ought to reach a swept mob has
     * already been applied to {@code primaryDamage} by the time it arrives.
     */
    public static double of(double primaryDamage, double fraction) {
        return primaryDamage * fraction;
    }
}
