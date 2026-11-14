package io.github.butterflysmp.rpg.core.combat;

/**
 * The numbers that make a damage-over-time status: its clock, its rate, its cap and its window. The
 * arithmetic Scorch and Wither SHARE (PLAN-wither.md section 3.2; the seat's WS1 ruling, 2026-09-30:
 * "a shared core DotRates record").
 *
 * <p><b>Each status's numbers live in its own file.</b> {@link Scorch#RATES} and {@link Wither#RATES}
 * are the two instances, each built from named constants beside that status's account, so the person
 * tuning a status still finds every rate in one place. This record holds the RULES those numbers obey
 * -- {@link #damagePerTick} and {@link #damageTicksFor} -- once, so the two statuses cannot drift in how
 * they read the same field.
 *
 * <p>For Ben: a "record" is a small Java class that only holds values, declared in one line.
 *
 * @param periodTicks          the status's own clock: one damage tick every this many ticks
 * @param ratePerTick          the fraction of the victim's MAX health one tick takes, before the cap
 * @param capFraction          the fraction of the applying hit's magnitude that becomes the cap
 * @param defaultDurationTicks the window an accrued application opens (and a refresh re-opens)
 */
public record DotRates(int periodTicks, double ratePerTick, double capFraction, int defaultDurationTicks) {

    /** A clock of zero or less would never tick, or would tick every frame; neither is a status. */
    public DotRates {
        if (periodTicks <= 0) {
            throw new IllegalArgumentException("a DoT's period must be positive, was " + periodTicks);
        }
    }

    /**
     * What one tick takes off {@code victimMaxHealth}, held to {@code cap}: {@code min(rate x max, cap)}.
     * See {@link Scorch#damagePerTick} for why the cap is the boss protection and the percent arm is
     * what would run away.
     */
    public double damagePerTick(double victimMaxHealth, double cap) {
        return Math.min(ratePerTick * victimMaxHealth, cap);
    }

    /**
     * How many ticks a window of {@code durationTicks} deals: one per whole period, rounded UP. See
     * {@link Scorch#damageTicksFor} for why the count and the lifetime are two numbers.
     */
    public int damageTicksFor(int durationTicks) {
        if (durationTicks <= 0) return 0;
        return (durationTicks + periodTicks - 1) / periodTicks;
    }
}
