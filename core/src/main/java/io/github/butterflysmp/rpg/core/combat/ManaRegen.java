package io.github.butterflysmp.rpg.core.combat;

/**
 * Mana regeneration: the bonus gear adds to the refill rate, and the one conversion between the unit
 * a player reads and the unit the pool counts in.
 *
 * <h2>Per SECOND for the stat, per TICK for the pool</h2>
 *
 * {@link ResourcePool} integrates over ticks, so {@link RegenResolver} is per tick. A player reads
 * "mana per second", and {@link HealthRegen} already states its rate that way, so the STAT is per
 * second and both regeneration stats share one unit. This class owns the only conversion between
 * them -- put it anywhere else and there will eventually be two.
 *
 * <h2>The bonus, not the total, exactly as ManaBank is</h2>
 *
 * Base 0.0: the whole value is gear-contributed. The base rate lives in {@code RpgPlugin} beside
 * {@code MAX_MANA} it is derived from, which {@code NEXT.md} records as becoming archetype CONTENT
 * later -- duplicating it here would make that content move a two-file change. Base 0.0 also keeps
 * the accessor TOTAL for an untracked combatant, which is what lets the resolver be called from
 * inside a cast without a {@code tracks()} guard.
 *
 * <h2>COMPOSE IN TICKS, and that is a floating-point decision</h2>
 *
 * The shipped base rate is {@code MAX_MANA / (60 * 20)}. Measured: that is
 * {@code 0x1.5555555555555p-4}, while the same quantity reached as {@code (MAX_MANA / 60.0) / 20.0}
 * is {@code 0x1.5555555555556p-4}. <b>They differ by one ULP and {@code ==} is false.</b> So a
 * resolver that added a per-second bonus to a per-second base and converted the sum would shift the
 * rate for every player on the server, including players wearing nothing at all -- silently, and by
 * an amount no boot gate could see.
 *
 * <p>The resolver therefore adds {@code perTick(bonus)} to the per-tick base. With no bonus,
 * {@link #perTick} returns exactly {@code 0.0} and {@code x + 0.0 == x}, so an unenchanted player's
 * rate is bit-for-bit what shipped before this slice.
 *
 * <p><b>{@link #perTick} and {@link #perSecond} are NOT exact inverses</b>, and no test should assert
 * that they are. Measured: {@code (x * 20) / 20} round-trips for every value tried, but
 * {@code (x / 20) * 20} does not -- it fails for {@code 1.6666666666666667}, which is exactly the
 * per-second figure someone would write by hand for this pool. Derive per-second FROM per-tick, never
 * the reverse.
 */
public final class ManaRegen {

    private ManaRegen() {}

    /** No bonus. The same 0.0-is-absent convention {@code ManaBank.NONE} and its siblings use. */
    public static final double NONE = 0.0;

    /** Ticks per second: the whole of the unit conversion, named so it is not a literal 20 anywhere. */
    private static final double TICKS_PER_SECOND = 20.0;

    /** Does this bonus grant anything at all? Strictly {@code >}, so 0 declares nothing. */
    public static boolean boosts(double bonusPerSecond) {
        return bonusPerSecond > NONE;
    }

    /**
     * A piece's contribution to the regeneration rate, in mana per second: the bonus itself.
     *
     * <p>Named rather than inlined for the reason {@code ManaBank.contribution} gives -- it is the ONE
     * place a mana-regen bonus becomes a stat modifier, and a future rule (a cap, a diminishing
     * curve) needs somewhere to live that is not a scan loop.
     */
    public static double contribution(double bonusPerSecond) {
        return bonusPerSecond;
    }

    /** A per-second rate as the per-tick rate {@link ResourcePool} counts in. */
    public static double perTick(double perSecond) {
        return perSecond / TICKS_PER_SECOND;
    }

    /**
     * A per-tick rate as the per-second rate a player reads.
     *
     * <p>The direction a display uses: {@code perSecond(pool.regen(owner, id))} composes base and
     * bonus once, in the pool, rather than a second time at the edge. See the class javadoc for why
     * this direction is the safe one.
     */
    public static double perSecond(double perTick) {
        return perTick * TICKS_PER_SECOND;
    }
}
