package io.github.butterflysmp.rpg.core.combat;

/**
 * Scorch's arithmetic: what one damage tick is worth, and how many stacks a hit is worth.
 *
 * Pure and dependency-free, beside {@link Defense} and for the same reason -- the numbers are the
 * design, and they are testable without a server. The STATE (stacks, the timer, the applier) lives in
 * {@code paper/adapter/ScorchStatus}; only the maths is here.
 *
 * <h2>THE RATE IS FLAT. STACKS DO NOT SCALE IT.</h2>
 *
 * The operator said both of these, in this order:
 *
 * <pre>
 *   FIRST:      "the amount of scorch stacks a target has determines the damage it takes per second"
 *   SUPERSEDED: "on second thought, let's change the numbers a bit"  -&gt;  a FLAT rate
 * </pre>
 *
 * <b>The revision is a decision, not a transcription slip, and the arithmetic is what proves it.</b>
 * The Flint Staff applies TEN stacks. At 5% per stack per second that is 50%/sec -- a two-second kill
 * on anything -- which is precisely the outcome the operator ruled out: <i>"Scorch/Ignite is NOT MEANT
 * TO BE AN EXECUTION, it's meant to be a crowd control that comes from sustained damage."</i>
 *
 * Both statements are recorded here so nobody re-derives the first one from the archive later. Stacks
 * are not decorative: they refresh the timer, they gate the fire-tick suppression in
 * {@code RpgListeners.onEnvironmentalDamage}, and they carry Ignite's threshold in slice 2.
 *
 * <h2>THE CAP IS WHAT MAKES THE DoT RESPOND TO THE APPLIER -- the job stacks would otherwise have</h2>
 *
 * Percent-max-health scales UP with the pool, so it runs away on a boss. The cap stops it, and a
 * stronger fire weapon buys more scorch damage:
 *
 * <pre>
 *   100 HP player, 20-dmg staff -&gt; 5% = 5    cap 20 -&gt; cap NEVER binds
 *   360 HP Knell,  20-dmg staff -&gt; 5% = 18   cap 20 -&gt; barely binds
 *   5000 HP boss,  20-dmg staff -&gt; 5% = 250  cap 20 -&gt; CAP HOLDS IT TO 20
 * </pre>
 *
 * <b>AT MAX 100 THE CAP NEVER BINDS, WHICH IS WHY EVERY TEST AT THE DEFAULT MAX IS BLIND TO IT.</b> A
 * working cap and a cap deleted entirely are the same number there. {@code ScorchTest} pins it at a
 * max where it binds; anything less would pass whether this {@code min} exists or not.
 *
 * <h2>ARMOUR DELAYS SCORCH RATHER THAN BLUNTING IT</h2>
 *
 * Armour reaches scorch by three channels and the operator ruled on all three: the DoT BYPASSES
 * Defense (see {@code CombatantStats.damage}'s {@code bypassesDefense}); stack accrual is POST
 * mitigation ({@link #stacksFor} is fed what actually landed); and the cap is the weapon's AUTHORED
 * number, not what it landed. So armour buys time to ignite and does not reduce the burn once it is on
 * you -- coherent with crowd control that comes from sustained damage, and the reason the DoT is not
 * "ordinary damage with extra arithmetic".
 */
public final class Scorch {

    private Scorch() {}

    /**
     * The burn, as a fraction of the victim's MAX health per second.
     *
     * Not a tuning guess: {@code GATE-vanilla-damage.md} D15 measured 391 {@code FIRE_TICK} events and
     * found vanilla's own fire tick already lands here -- a skeleton takes 1 HP/sec on a 20 HP pool,
     * a player {@code raw=1 -> applied=5} on a 100 max. Both are 5% of max per second. So this slice
     * changes ATTRIBUTION, PACING and the CAP; for a standard entity it barely changes magnitude.
     */
    public static final double RATE_PER_SECOND = 0.05;

    /**
     * Scorch's own clock, in ticks. TWENTY, and it is owned rather than inherited.
     *
     * {@code NEXT.md:833}: a rate implemented as "deal damage while the status is active" has no clock
     * of its own and is paced entirely by the invulnerability window -- correct-looking until anything
     * writes {@code lastHurt}, then 20 Hz. That finding cost a boot, a diagnosis and an unusable test
     * world (D3/D3a).
     *
     * <b>Vanilla fire is NOT in that class</b> -- D15's traces show it ticking steadily every 20 ticks
     * under an already-poisoned {@code lastHurt}, which puts it in DROWNING's row, not LAVA's. The
     * reason not to ride it is different and simpler: {@code fireTicks} is not ours. Rain, water, a
     * second fire source and {@code /extinguish} all write it, and it can carry neither the cap, nor
     * the applier, nor the Defense bypass.
     */
    public static final int PERIOD_TICKS = 20;

    /**
     * The default window for the WEAPON-DAMAGE entry point only -- eight seconds.
     *
     * Every existing content applier keeps its own {@code duration_ticks} (40, 60, 80), exactly as
     * {@code scorch.yml}'s own header rules: "The ability that applies it decides for how long." The
     * operator's original "10 seconds" was given without knowing eight authored values already
     * existed, then revised to 8 once they were on the table.
     *
     * At {@link #PERIOD_TICKS} this is EIGHT damage ticks over exactly 8.0 seconds -- see
     * {@code ScorchStatus}, where the ordering that makes the lifetime exact is the whole point.
     */
    public static final int DEFAULT_DURATION_TICKS = 160;

    /** Damage dealt, post-mitigation, that buys one stack. */
    public static final double DAMAGE_PER_STACK = 2.0;

    /**
     * The cap used when the thing applying scorch declares no damage number of its own.
     *
     * <b>A NON-BINDING FALLBACK IS NOT A FALLBACK.</b> Defaulting to the 5% figure makes
     * {@code min(5% of max, 5% of max)} -- an UNCAPPED percent-max-health DoT, 250/sec against a 5000
     * HP boss from an ability whose sibling weapon is held to 20. It hands the boss-melting tool to
     * exactly the appliers the cap exists to restrain, while a cap field sits in the store looking like
     * it does something. And it is invisible at max 100, where the real cap never binds either.
     *
     * <b>THE RULE THAT FIXES THIS VALUE, rather than a number chosen by taste:</b> an undeclared cap
     * must never exceed the SMALLEST DECLARED one, so an author who forgets cannot get a stronger
     * scorch than an author who declares. The smallest authored fire damage in shipped content is 2.0
     * ({@code solar_grenade}'s field tick), so that is the value.
     *
     * <b>A CONSTANT, NOT A DERIVED MINIMUM.</b> Deriving it at runtime would mean adding one weak
     * ability silently weakens every undeclared scorch -- surprising in the other direction, and
     * untestable at a fixed number.
     *
     * <b>The rule is ENFORCED, because a rule that lives only in a comment gets built past.</b>
     * {@code ScorchContentInvariantTest} walks the bundled content and fails the BUILD if any declared
     * cap is below this; {@code ContentValidator} names it at boot for content added after the build.
     * Author a fire ability at 1.5 without either and this constant silently violates its own
     * invariant, in exactly the direction the rule exists to prevent.
     */
    public static final double UNDECLARED_CAP = 2.0;

    /**
     * What one scorch tick takes off {@code victimMaxHealth}, held to {@code cap}.
     *
     * {@code cap} is the AUTHORED damage of whatever applied the stacks -- never what that hit actually
     * landed, or armour would lower the cap and re-enter through the back door after being ruled out
     * of the DoT.
     *
     * Worked: {@code (100, 20) -> 5} (cap idle); {@code (360, 20) -> 18} (cap idle, barely);
     * {@code (5000, 20) -> 20} (CAP BINDS -- the only shape that can see the min).
     */
    public static double damagePerTick(double victimMaxHealth, double cap) {
        return Math.min(RATE_PER_SECOND * victimMaxHealth, cap);
    }

    /**
     * How many stacks {@code dealtPostMitigation} buys: one per {@link #DAMAGE_PER_STACK}, FLOORED.
     *
     * Post-mitigation by the operator's ruling -- this is fed what actually landed, so armour slows the
     * climb toward ignite even though it does not touch the burn itself.
     *
     * Floored, not rounded: 1 damage must buy nothing rather than rounding up to a stack, or chip
     * damage scorches. Non-positive returns 0 -- a heal or a fully-absorbed hit accrues nothing.
     *
     * <b>THE REMAINDER IS DISCARDED, AND THAT IS THE CHOICE RATHER THAN AN OVERSIGHT.</b> A 3-damage
     * hit buys one stack and drops 1; ten such hits buy ten stacks, not fifteen. There is deliberately
     * NO per-victim accumulator carrying the leftover forward, for two reasons: it would be state that
     * outlives the burn it belongs to (what happens to a remainder when the scorch expires, or when a
     * different applier takes over?), and it would make the stack a target reaches depend on the
     * ORDER its hits arrived in rather than on their total. Chip damage advancing toward ignite more
     * slowly than its raw total suggests is the intended shape, not a rounding artefact.
     */
    public static int stacksFor(double dealtPostMitigation) {
        if (dealtPostMitigation <= 0) return 0;
        return (int) (dealtPostMitigation / DAMAGE_PER_STACK);
    }

    /**
     * The number of damage ticks a scorch of {@code durationTicks} deals, first one INCLUDED.
     *
     * One inline on application plus one per whole period after it, which is
     * {@code ceil(duration / period)} -- 160 ticks is 8, not 9. Stated here rather than left implicit
     * in the scheduler because the tick COUNT and the LIFETIME are two numbers and only asserting one
     * of them cannot see an off-by-one-period overrun. See {@code ScorchStatusTest}.
     *
     * A duration that is not a whole number of periods rounds UP: 50 ticks burns 3 times and lives 60.
     * Every authored value in content today (40, 60, 80, 160) is a multiple of the period.
     */
    public static int damageTicksFor(int durationTicks) {
        if (durationTicks <= 0) return 0;
        return (durationTicks + PERIOD_TICKS - 1) / PERIOD_TICKS;
    }
}
