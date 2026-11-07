package io.github.butterflysmp.rpg.core.combat;

/**
 * Wither's arithmetic: OUR damage-over-time, shaped like {@link Scorch} (WITHER-STATUS,
 * {@code PLAN-wither.md}). Every rate is here, as Scorch's are in its file. The STATE lives in
 * {@code paper/adapter/DotStatus}, the same store Scorch uses, created a second time.
 *
 * <h2>BEN'S WORDS, AND THE RULED NUMBERS</h2>
 *
 * <p>Ben, 2026-09-30: <i>"let's make our Wither effect work. It should function fairly similar to
 * Vanilla Wither does and how our scorch already does. Damage over time based off of the damage of the
 * weapon used to inflict. Difference mobs Don't explode when killed by wither"</i>. His answers to
 * Q-W1..Q-W10, verbatim: <i>"8, yes everything else is fine"</i>, which accepted every PROPOSED value:
 *
 * <pre>
 *   Q-W1  a tick every 40 ticks (2 s)              PERIOD_TICKS         vanilla Wither I's 40 >> 0
 *   Q-W2  min(5% of max, cap) per tick              RATE_PER_TICK        on a 20-max mob, 1.0 per 2 s:
 *         the cap HALF the applying hit             CAP_FRACTION         exactly vanilla Wither I
 *   Q-W3  lasts 200 ticks (10 s), so 5 ticks        DEFAULT_DURATION_TICKS  a wither skeleton's hit
 *   Q-W4  a re-hit RESETS the timer, no stacking    the shared store's refresh, as Scorch's
 * </pre>
 *
 * <h2>WHAT IS NOT HERE, AND WHERE IT IS</h2>
 *
 * <ul>
 *   <li><b>No Ignite.</b> A never-scorched mob killed by Wither does not explode (WS2). Ignite reads
 *       the SCORCH store only; Wither lives in a second instance a wither application never shares.
 *       A mob that is scorched AND withered and dies to a Wither tick still explodes, because Ignite
 *       is "died while scorched" (Q-W8, Ben: "yes").</li>
 *   <li><b>Immunity</b> is content: {@code statuses/withering.yml}'s {@code immune} list (Q-W6).</li>
 *   <li><b>The look</b> is the vanilla WITHER potion with its damage suppressed (Q-W7 a), in
 *       {@code BukkitCombatant} and {@code RpgListeners.onEnvironmentalDamage}.</li>
 *   <li><b>The undeclared cap</b> (a dev {@code /rpg apply}, which declares no damage) is
 *       {@link Scorch#UNDECLARED_CAP}, shared: it belongs to the apply path, not to a status.</li>
 * </ul>
 */
public final class Wither {

    private Wither() {}

    /** Q-W1: a tick every 40 ticks, vanilla Wither I's cadence ({@code 40 >> amplifier 0}). */
    public static final int PERIOD_TICKS = 40;

    /**
     * Q-W2: the fraction of the victim's MAX health one tick takes, before the cap. Per TICK, and a
     * tick is two seconds, so this is half Scorch's per-second rate in time.
     */
    public static final double RATE_PER_TICK = 0.05;

    /** Q-W2: the cap is HALF the applying hit, as Scorch's ({@code Scorch.CAP_FRACTION}). */
    public static final double CAP_FRACTION = 0.5;

    /** Q-W3: 200 ticks, a wither skeleton's hit; {@code ceil(200 / 40)} = five ticks. */
    public static final int DEFAULT_DURATION_TICKS = 200;

    /** The four, as the shared store reads them. */
    public static final DotRates RATES =
            new DotRates(PERIOD_TICKS, RATE_PER_TICK, CAP_FRACTION, DEFAULT_DURATION_TICKS);
}
