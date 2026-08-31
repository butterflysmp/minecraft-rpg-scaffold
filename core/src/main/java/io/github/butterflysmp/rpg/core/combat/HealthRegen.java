package io.github.butterflysmp.rpg.core.combat;

/**
 * Passive health regeneration: a flat rate in HP per second, and the saturation window that
 * multiplies it.
 *
 * <p>Growth let max health rise. Nothing filled it -- before this class the only ways custom HP went
 * up were {@code /rpg heal} and {@code /rpg mobheal}, so a bigger pool was strictly a bigger hole.
 *
 * <h2>Two systems, ONE knob</h2>
 *
 * The stat is a flat rate, always ticking, base {@link #BASE_PER_SECOND}. While the player has
 * saturation it is multiplied by {@link #SATURATED_MULTIPLIER}. That is deliberately a multiplier on
 * the same stat rather than a second stat: gear that boosts Health Regen boosts the saturated rate
 * too, so there is one number to tune, one number to display, and no way for the two to disagree.
 * Below saturation the flat rate is the FLOOR -- it is never zero, so a player out of food still
 * comes back, just slowly.
 *
 * <p>The rate is what a stat sheet shows; the multiplier is not. {@code /rpg stats} displays the flat
 * HP/s, because the multiplier is a live mechanic that depends on a bar the player is already
 * looking at.
 *
 * <h2>FOOD GATES THE RATE, and vanilla does the gating for us</h2>
 *
 * The design called for the saturated window to charge exhaustion, on the premise that cancelling
 * vanilla's {@code SATIATED} regen would also stop vanilla charging the exhaustion that goes with it
 * -- making a replacement charge RESTORATIVE rather than additive. <b>Boot gate row 4 measured that
 * premise on Paper 26.1.2 and it is FALSE:</b> with our heal cancelled and no charge of our own,
 * saturation still drained in roughly four to five seconds. Vanilla drains saturation on its own,
 * independently of whether its regen tick was allowed to heal.
 *
 * <p>Which means the two-tier design falls out FOR FREE. Fed, you regenerate at the saturated rate;
 * once vanilla has drained the saturation, you drop to the floor. Food gates the rate exactly as
 * intended, through vanilla's own drain -- and a custom charge on top would simply have doubled it.
 * So there is no exhaustion machinery here, and none shipped dormant. See {@code NEXT.md}.
 *
 * <h2>The saturation read is NOT here</h2>
 *
 * This takes an already-sampled {@code saturated} boolean, exactly as {@link Crit} takes an
 * already-drawn roll. {@code getSaturation()} is Bukkit and {@code core} has no dependencies -- but
 * the payoff is the one {@link Crit} records: the decision is reddening-testable against exact
 * boundary values with no fake and no random source.
 *
 * <h2>Worked</h2>
 *
 * At base and a 20-tick window: {@code healAmount(0.2, false, 20, 50, 100)} is the flat rate for one
 * second; saturated, five times it -- a round 1.0 HP/s while fed. At 99.9/100 the saturated amount is
 * capped to the remaining 0.1. At 100/100 it is 0.0, and that zero is load-bearing -- see
 * {@link #healAmount}.
 */
public final class HealthRegen {

    private HealthRegen() {}

    /** A player's starting regeneration: 1 HP every 5 seconds. Mobs base at 0 -- see {@code HealthState}. */
    public static final double BASE_PER_SECOND = 0.2;

    /**
     * What saturation multiplies the flat rate by while the player has any.
     *
     * <p>Five, so a player at the base rate regenerates a round <b>1.0 HP/s</b> while fed, dropping to
     * the 0.2 HP/s floor once vanilla has drained their saturation. Those two tiers are the whole food
     * economy of this system, and neither needs a custom cost -- see the class javadoc.
     */
    public static final double SATURATED_MULTIPLIER = 5.0;

    /** The multiplier outside the saturation window. Exactly 1.0, so the flat rate is untouched. */
    public static final double UNSATURATED_MULTIPLIER = 1.0;

    /** No bonus. The same 0.0-is-absent convention {@code ManaBank.NONE} and its siblings use. */
    public static final double NONE = 0.0;

    /** Ticks per second, the divisor that turns an HP/s rate into an HP-per-window amount. */
    private static final double TICKS_PER_SECOND = 20.0;

    /** Does this bonus grant anything at all? Strictly {@code >}, so 0 declares nothing. */
    public static boolean boosts(double bonusPerSecond) {
        return bonusPerSecond > NONE;
    }

    /**
     * A piece's contribution to the regeneration rate: the bonus itself.
     *
     * <p>Named rather than inlined for the reason {@code ManaBank.contribution} gives -- it is the ONE
     * place a regen bonus becomes a stat modifier, and a future rule (a cap, a diminishing curve)
     * needs somewhere to live that is not a scan loop.
     */
    public static double contribution(double bonusPerSecond) {
        return bonusPerSecond;
    }

    /**
     * How much custom HP to heal for one window of {@code periodTicks}, or {@code 0.0} for "nothing
     * happens".
     *
     * <p><b>The three zero cases are decisions, not hygiene.</b>
     *
     * <ul>
     *   <li><b>{@code current >= max}</b> -- a full player is not healed. {@code CombatantStats.heal}
     *       emits a {@code HealthChange} whenever the state exists, EVEN when the heal was entirely
     *       clamped away, and that change drives a {@code setHealth} write and a heart-bar render. So
     *       without this branch every full-health player on the server would take a render every
     *       single window, forever, for nothing.
     *   <li><b>{@code current <= 0}</b> -- a player at zero custom HP is dying or on the death screen
     *       ({@code HealthState.damage} floors at 0 and the kill is dispatched from the change), and
     *       {@code onQuit} does not run on death, so custom HP SITS at 0 until respawn. Regenerating
     *       there would heal a corpse back up mid-death-screen.
     *   <li><b>{@code ratePerSecond <= 0}</b> -- a mob, whose stat bases at 0, or a future debuff that
     *       pushed the rate negative. Neither should heal, and a negative would otherwise DAMAGE
     *       through a method named heal.
     * </ul>
     *
     * <p>The result is capped at the remaining headroom, {@code max - current}. That cap is not
     * duplicating {@code HealthState.heal}'s own clamp: it keeps the amount REQUESTED equal to the
     * amount applied, and {@code CombatantStats.heal} reports the requested amount in its event
     * (unlike {@code damage}, which reports what it actually dealt). Any future consumer of a HEAL
     * change -- a heal popup, a healing statistic -- reads that number, so the cap is what stops it
     * reporting a heal larger than the one that landed.
     */
    public static double healAmount(double ratePerSecond, boolean saturated, int periodTicks,
                                    double current, double max) {
        if (ratePerSecond <= 0 || periodTicks <= 0) return 0.0;
        if (current <= 0) return 0.0;
        if (current >= max) return 0.0;
        double multiplier = saturated ? SATURATED_MULTIPLIER : UNSATURATED_MULTIPLIER;
        double window = periodTicks / TICKS_PER_SECOND;
        return Math.min(ratePerSecond * multiplier * window, max - current);
    }
}
