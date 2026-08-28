package io.github.butterflysmp.rpg.core.combat;

/**
 * The critical hit: a chance to multiply a hit, and how much it multiplies by.
 *
 * <p>Two stats, not one. {@code critChance} is a PROBABILITY in {@code [0,1]} (base 0.15, so fifteen
 * swings in a hundred); {@code critDamage} is the BONUS the crit adds (base 1.0), so the multiplier
 * is {@code 1 + critDamage} and a base crit is exactly {@code 2.0x}. Splitting them is what lets gear
 * raise how OFTEN you crit and how HARD you crit independently -- one stat could not express "crits
 * half the time for a little extra" and "crits rarely for a lot".
 *
 * <p><b>The bonus convention, not the multiplier convention.</b> {@code critDamage} is a summand with
 * a base of 1.0 and gear adds to it, so a {@code +0.5} item yields {@code 1 + 1.5 = 2.5x}. Storing
 * the multiplier directly (base 2.0) would have made gear either multiplicative -- a different
 * composition rule from every other stat in the store -- or additive on a base that already contains
 * the 1, which reads as {@code +0.5} meaning {@code 2.5x} anyway with an extra mental step. Keeping
 * the stat a bonus is what makes it stack additively "exactly like the other stats".
 *
 * <p><b>The draw is not here.</b> The RNG call stays at the impure call site
 * ({@code BukkitCombatant.snapshot}, once per cast) and this takes the already-drawn double -- the
 * same split {@link io.github.butterflysmp.rpg.core.enchant.Unbreaking} and
 * {@code DamagePopupManager.jitter} use, and for the same payoff: the decision is reddening-testable
 * against exact boundary values with no random source and no seeded fake.
 *
 * <p><b>Rolled ONCE per cast, then frozen.</b> The multiplier rides {@code CombatantSnapshot} and
 * {@code Caster} beside {@code chargeScale}, so one swing is one roll however many effects its
 * payload lands. Rolling per damage arm would make a two-effect payload crit twice independently,
 * and a swept mob would get its own roll instead of inheriting the swing's -- sweep is a fraction of
 * the primary's already-multiplied number, so it inherits the crit for free and must not re-roll.
 *
 * <p>Worked: {@code multiplier(0.15, 1.0, 0.10) -> 2.0} (crit); {@code multiplier(0.15, 1.0, 0.20)
 * -> 1.0} (no crit); {@code multiplier(0.15, 1.5, 0.10) -> 2.5} (a crit-damage boost).
 */
public final class Crit {

    private Crit() {}

    /**
     * A player's starting crit chance: 15%. Mobs base at 0 -- see {@code HealthState}, which bases
     * this stat on the combatant's frozen faction rather than gating the roll at the call site, so
     * anything that later READS the stat (a stat screen) sees the truth rather than a number
     * contradicted by a check somewhere else.
     */
    public static final double BASE_CHANCE = 0.15;

    /** A player's starting crit bonus: +100%, i.e. a base crit is {@code 2.0x}. */
    public static final double BASE_DAMAGE = 1.0;

    /** The multiplier a hit that did not crit is scaled by. Exactly 1.0, so a non-crit is untouched. */
    public static final double NO_CRIT = 1.0;

    /**
     * {@code critChance} as a usable probability: clamped to {@code [0,1]}.
     *
     * <p>The UPPER clamp is a decision, not hygiene: at or above 100% every hit crits, rather than
     * overflowing into a second tier of "extra-critical". Gear that pushes past 1.0 is simply capped,
     * so stacking crit chance has a defined ceiling instead of an undefined behaviour.
     *
     * <p>The LOWER clamp guards the same shape {@code Unbreaking} guards: a negative chance from some
     * future debuff would otherwise meet {@code roll < negative}, which is false for every roll --
     * which happens to be correct here (never crit) but only by accident. Clamping says it on
     * purpose, and keeps {@link #crits} total for inputs the store could grow later.
     */
    public static double chance(double critChance) {
        return Math.max(0.0, Math.min(1.0, critChance));
    }

    /**
     * Did this hit crit? {@code roll} is a draw from {@code [0,1)}.
     *
     * <p><b>Strict {@code <}, not {@code <=}.</b> The source is {@code ThreadLocalRandom.nextDouble()},
     * which is half-open {@code [0,1)}, so with {@code <} the measure of the critting set is exactly
     * the chance: at 0.15 a roll of 0.1499 crits and 0.15 does not, and over many swings the observed
     * rate is 15%. {@code <=} would crit on one extra point -- immeasurable in play, and exactly the
     * kind of off-by-one that a boundary test exists to pin.
     *
     * <p>A chance of exactly 0 never crits, because no roll in {@code [0,1)} is below 0. That is what
     * makes a mob's hits never crit without a second check anywhere.
     */
    public static boolean crits(double critChance, double roll) {
        return roll < chance(critChance);
    }

    /**
     * What to multiply a hit by: {@code 1 + critDamage} on a crit, {@link #NO_CRIT} otherwise.
     *
     * <p>Applied in {@code EffectApplier} alongside {@code chargeScale}, so it reaches EVERY custom
     * damage effect uniformly -- a weapon swing, an ability literal, a projectile, an area -- with no
     * per-arm branch to keep in step. A non-crit returns exactly 1.0, so the multiply is an exact
     * identity and damage does not drift by a rounding error on the 85% of swings that do not crit.
     */
    public static double multiplier(double critChance, double critDamage, double roll) {
        return crits(critChance, roll) ? 1.0 + critDamage : NO_CRIT;
    }
}
