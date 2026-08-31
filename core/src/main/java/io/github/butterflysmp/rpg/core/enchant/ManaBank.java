package io.github.butterflysmp.rpg.core.enchant;

/**
 * Mana Bank: flat points added to the MAX MANA of whoever wears the piece it sits on.
 *
 * <p>The shape of {@link Growth}, and the arithmetic is the same one line. What differs is where
 * the number lands, and that difference is the whole of why this needed a slice.
 *
 * <h2>Max mana had none of the machinery max health already had</h2>
 *
 * Growth was wiring: {@code HealthState.max} already had a {@code Stat}, a {@code ModifierTarget} in
 * the reconciler, and {@code clampCurrentToMax}. Max mana had a single {@code final double} shared
 * by every player. So this enchant's first version built the per-player ceiling
 * ({@code ResourcePool.MaxResolver}), the ninth {@code Stat}, and the max-change transition, before
 * granting a single point.
 *
 * <h2>The transition rules it inherits, and the one it had to be given</h2>
 *
 * <ul>
 *   <li><b>Equipping is HEADROOM.</b> 100/100 with a +30 piece becomes 100/130 -- the ceiling moves
 *       and the amount does not, exactly as Growth's does.
 *   <li><b>Unequipping CLAMPS.</b> 130/130 losing that piece becomes 100/100.
 * </ul>
 *
 * <p>Neither was free. {@code ResourcePool} stores a spent amount and a tick rather than a current
 * value, and an owner with no entry reads as FULL -- so raising the ceiling of a player who had
 * never cast would have handed them the difference instantly, while a player who had cast once got
 * headroom. The same enchant, two behaviours, decided by state nobody can see.
 * {@code ResourcePool.setCurrent} pins the pre-change reading and makes both headroom; the clamp is
 * the same call in the other direction.
 *
 * <h2>Points, and stacking</h2>
 *
 * Flat points, summed per piece by the reconciler under its own key, so four pieces compose without
 * this method knowing a set exists. Full Mana Bank III is +120 on a base of 100.
 *
 * <p><b>Unlike Growth, mana REGENERATES</b>, and that is worth knowing before retuning either. +120
 * max health is a permanent doubling of a pool nothing refills on its own; +120 max mana is a
 * doubling of a pool that refills completely in sixty seconds, so it buys burst rather than
 * survival. The two read as siblings on a tooltip and are not the same kind of gift.
 *
 * <p>No {@code ManaBank.ID}: bound by {@code EnchantEffect.MAX_MANA}, so the second max-mana enchant
 * is a yml file rather than a recompile.
 */
public final class ManaBank {

    private ManaBank() {}

    /** No bonus. The same 0.0-is-absent convention {@link Growth#NONE} and its siblings use. */
    public static final double NONE = 0.0;

    /** Does this bonus grant anything at all? Strictly {@code >}, so 0 declares nothing. */
    public static boolean boosts(double bonusPoints) {
        return bonusPoints > NONE;
    }

    /**
     * A piece's contribution to max mana. The bonus itself -- a piece of armor has no mana pool of
     * its own to add it to, exactly as it has no max health.
     *
     * <p>Named rather than inlined for the reason {@link Growth#contribution} gives: it is the ONE
     * place a Mana Bank bonus becomes a stat modifier, and a future rule -- a per-set cap, a
     * diminishing curve -- needs somewhere to live that is not a scan loop.
     */
    public static double contribution(double bonusPoints) {
        return bonusPoints;
    }
}
