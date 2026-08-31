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
 * HP/s, because the x4 is a live mechanic that depends on a bar the player is already looking at.
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
 * second; saturated, four times it. At 99.9/100 the saturated amount is capped to the remaining 0.1.
 * At 100/100 it is 0.0, and that zero is load-bearing -- see {@link #healAmount}.
 */
public final class HealthRegen {

    private HealthRegen() {}

    /** A player's starting regeneration: 1 HP every 5 seconds. Mobs base at 0 -- see {@code HealthState}. */
    public static final double BASE_PER_SECOND = 0.2;

    /** What saturation multiplies the flat rate by while the player has any. */
    public static final double SATURATED_MULTIPLIER = 4.0;

    /** The multiplier outside the saturation window. Exactly 1.0, so the flat rate is untouched. */
    public static final double UNSATURATED_MULTIPLIER = 1.0;

    /** No bonus. The same 0.0-is-absent convention {@code ManaBank.NONE} and its siblings use. */
    public static final double NONE = 0.0;

    /** Ticks per second, the divisor that turns an HP/s rate into an HP-per-window amount. */
    private static final double TICKS_PER_SECOND = 20.0;

    /**
     * Exhaustion charged per HP healed inside the saturation window.
     *
     * <p><b>0.0 until the boot gate has witnessed the premise, and that ordering is the point.</b>
     * The charge is justified as RESTORATIVE, not additive: cancelling vanilla's {@code SATIATED}
     * regen also cancels the exhaustion vanilla charged for it, so without a replacement charge a fed
     * idle player holds the x4 nearly forever and food stops mattering out of combat. That is a claim
     * about vanilla's behaviour on THIS Paper build, and it has not been measured. The gate row
     * measures it -- suppression in, charge off -- and this constant is set from what it prints, in
     * its own commit, afterwards. If the suppressed drain turns out NOT to be slower than vanilla's,
     * the charge is additive and this constant stays at zero.
     *
     * <p>The derivation to check it against, from recall and therefore not to be trusted until the
     * gate confirms it: vanilla is believed to charge 6.0 exhaustion per vanilla health point and to
     * spend 4.0 exhaustion per saturation point; 100 custom HP renders as 20 vanilla points, so
     * 1 custom HP is about 0.2 vanilla points, about 1.2 exhaustion.
     */
    public static final double EXHAUSTION_PER_HP = 0.0;

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
     * <p>The result is capped at the remaining headroom, {@code max - current}, and that cap is not
     * duplicating {@code HealthState.heal}'s own clamp. It is what makes the exhaustion charge honest:
     * {@code CombatantStats.heal} reports the REQUESTED amount in its event, unlike {@code damage}
     * which reports what it actually dealt, so a caller charging for what it asked for would
     * overcharge on the window that tops a player off. Capping here makes requested == applied.
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

    /**
     * The exhaustion to charge for {@code healed} HP, or {@code 0.0} outside the saturation window.
     *
     * <p><b>Per HP healed, never per tick</b>, so the drain is CADENCE-INVARIANT: changing the regen
     * period changes how often this is called and how much each call heals, and those cancel. A
     * per-tick charge would silently retune the food economy the first time the period moved.
     *
     * <p><b>An unsaturated window is free; a saturated window charges the WHOLE heal</b> -- the flat
     * floor included, not merely the extra the multiplier added. Food-powered regeneration costs food,
     * all of it. The floor is free because it has no food gate at all, so charging for it would
     * slowly starve an idle player in exchange for nothing.
     *
     * <p><b>The ratio is a parameter, not a read of {@link #EXHAUSTION_PER_HP}.</b> That constant
     * ships at 0 until the gate authorizes it, and a version of this method that read it internally
     * would return 0 on BOTH branches until then -- making the "charges when unsaturated" mutation
     * impossible to redden, which is a test that cannot fail. Passing the ratio in keeps every test
     * discriminating whatever the shipped constant happens to be.
     */
    public static double exhaustionFor(double healed, boolean saturated, double exhaustionPerHp) {
        if (!saturated) return 0.0;
        if (healed <= 0 || exhaustionPerHp <= 0) return 0.0;
        return healed * exhaustionPerHp;
    }
}
