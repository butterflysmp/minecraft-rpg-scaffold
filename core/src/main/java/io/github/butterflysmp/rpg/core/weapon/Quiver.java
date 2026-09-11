package io.github.butterflysmp.rpg.core.weapon;

/**
 * The quiver arithmetic: a magazine that empties as a weapon fires and refills on a timed reload.
 *
 * <p>Modelled on {@link Durability}, and that is not an analogy -- it is the same mechanism with a
 * different verb. Durability is already per-item mutable state, mutated in play on every use, with
 * its DECISIONS here in core as pure arithmetic and its item I/O in a thin {@code paper} class that
 * no test can reach. This is the second of those, and it deliberately looks like the first:
 *
 * <pre>
 *   Durability                         Quiver
 *   ----------------------------       ----------------------------
 *   maxDurability (the material's)     capacity   (a PARAMETER)
 *   damage counts UP to a floor        loaded counts DOWN to zero
 *   wear(current, amount, max)         spend(loaded)
 *   repair(current, amount)            reload(capacity)
 *   isBroken(current, max)             isEmpty(loaded)
 *   clamp(proposed, max)               clamp(proposed, capacity)
 * </pre>
 *
 * <h2>CAPACITY AND RELOAD TICKS ARE PARAMETERS, AND THIS CLASS MUST NEVER LEARN THEIR VALUES</h2>
 *
 * <p><b>This is a load-bearing constraint with a cost attached, not a style preference.</b> The
 * quiver slice was split in two on the strength of it: A1 (this) ships the per-item count with
 * capacity and reload ticks supplied by the caller, and A2 promotes both to real stats. A2 was
 * budgeted as <b>a change to the two call sites that SUPPLY them</b> -- which is true only while
 * nothing in here reads them itself. One constant, one default, one convenience overload that fills
 * in "the usual" capacity, and A2 becomes a rewrite of this class instead. It would still compile,
 * still pass, and still look like the plan.
 *
 * <p>So the constraint is enforced from outside rather than trusted:
 *
 * <ul>
 *   <li><b>{@code QuiverSignatureTest}</b> asserts every public method here takes primitives only --
 *       no {@link WeaponDefinition}, no item, no stat -- and that this class declares no {@code int}
 *       constant at all. {@link Durability} already satisfies both ({@code wear(int, int, int)}), so
 *       this pins an existing house shape rather than inventing one.
 *   <li><b>Every row of {@code QuiverTest} constructs at capacity 3 and reload 7 ticks</b>, which are
 *       NOT the shipped numbers. A version that secretly read a constant fails immediately.
 * </ul>
 *
 * <p>Together those make A2's confinement <b>observable rather than intended</b>. "Only two call
 * sites change" is otherwise a prediction about a future diff, and a prediction with no guard is how
 * a split gets paid for and not delivered.
 */
public final class Quiver {

    private Quiver() {}

    /**
     * The count actually safe to store, for a proposed one: never negative, never above capacity.
     *
     * <p><b>AND THIS IS ALSO THE CAPACITY-CHANGE RULE, which is why there is no second method for
     * it.</b> {@code DESIGN-stat-engine.md} settled the transition semantics for max health and they
     * are reused here verbatim: <b>a capacity INCREASE leaves the count unchanged</b> (you gain
     * headroom, not arrows -- the same reason equipping a +HP item is not a free heal), and <b>a
     * capacity DECREASE clamps</b> (nothing may hold more than it can hold).
     *
     * <p>Both fall straight out of {@code min}: on an increase the count is already below the new
     * capacity, so this is an identity; on a decrease it is the clamp. Writing {@code resize} as a
     * separate method would be two names for one expression, and two names drift.
     *
     * <p>The lower guard against negatives is not decoration either -- a negative count is not "extra
     * arrows", it is a malformed item, and {@link #spend} floors for the same reason.
     *
     * <p>Worked: {@code (9999, 3) -> 3}; {@code (2, 3) -> 2}; {@code (-5, 3) -> 0};
     * {@code (3, 1) -> 1} (the capacity-decrease case); {@code (0, 0) -> 0}.
     */
    public static int clamp(int proposedLoaded, int capacity) {
        return Math.min(Math.max(proposedLoaded, 0), Math.max(capacity, 0));
    }

    /**
     * Is this quiver out of ammunition?
     *
     * <p>{@code <= 0} rather than {@code == 0} deliberately, on {@link Durability#isBroken}'s
     * precedent: the comparison must hold for any malformed value that somehow reaches it, not only
     * the one the arithmetic above can produce. A quiver at -1 that reports "not empty" would fire
     * forever.
     */
    public static boolean isEmpty(int loaded) {
        return loaded <= 0;
    }

    /**
     * Spend one round, floored at zero so a spend can never drive the count negative.
     *
     * <p>Takes no capacity: spending only ever moves the count DOWN, so no ceiling can be exceeded --
     * the mirror of {@link Durability#repair}, which takes no maximum for the same reason.
     *
     * <p><b>Callers must gate on {@link #isEmpty} first.</b> This flooring exists so a missed gate is
     * a no-op rather than a corruption; it is not the gate. The refusal a player sees is minted in
     * {@code paper}, where the held item can actually be read.
     *
     * <p>Worked: {@code 3 -> 2}; {@code 1 -> 0}; {@code 0 -> 0}; {@code -4 -> 0}.
     */
    public static int spend(int loaded) {
        return Math.max(loaded - 1, 0);
    }

    /**
     * A completed reload: the quiver comes back exactly full.
     *
     * <p>Routed through {@link #clamp} rather than returning {@code capacity} raw, so a negative or
     * absurd authored capacity cannot be written onto an item by the one path that sets the count to
     * its maximum. Same discipline as {@link Durability#wear} delegating its floor to
     * {@code Durability.clamp}: the bound lives in exactly one place.
     */
    public static int reload(int capacity) {
        return clamp(capacity, capacity);
    }

    /** The tick a reload started now will finish on. */
    public static long reloadCompletesAt(long now, int reloadTicks) {
        return now + Math.max(reloadTicks, 0);
    }

    /**
     * Has a reload finished -- and, THE SECOND HALF, has the clock moved out from under it?
     *
     * <p><b>THE QUIVER IS THE FIRST THING IN THIS REPOSITORY TO WRITE A TICK VALUE TO DISK, AND THAT
     * IS WHY THIS GUARD EXISTS.</b> The reload deadline is stamped into the item's own data so it
     * travels with the item -- which is the ruling, and which is also what makes the reload leak-proof
     * (there is no expiry event to miss when the player swaps, drops, dies or logs out; the state is
     * simply read again). The cost is that the deadline outlives the counter it was measured against.
     *
     * <p>{@code Bukkit.getCurrentTick()} <b>resets on server restart</b>. A reload spanning a restart
     * therefore leaves a deadline in a future that never arrives: a weapon permanently mid-reload,
     * with a tooltip that reads perfectly correctly. So a remaining time GREATER than the reload's own
     * duration is not a long wait, it is proof the clock is no longer the one that set the deadline,
     * and the only safe reading is "finished".
     *
     * <p><b>The supplier pattern is precedent here; the persistence is NOT.</b>
     * {@code Bukkit::getCurrentTick} is already injected as a {@code LongSupplier} into
     * {@code MeleeHits} and {@code DamageWindow} -- which is what lets this be a unit test with no
     * server, by handing it a supplier that jumps backwards. But <b>both of those compare ticks
     * within a single session and hold only in-memory state that a restart clears</b>
     * ({@code MeleeHits} asks {@code hit.tick() != now}, a same-tick test). Seeing
     * {@code getCurrentTick} used in three places is therefore NOT evidence that the restart question
     * was considered before; it is being considered here, first.
     *
     * <p>Same family as {@link Durability#wear}'s widening to {@code long}, whose javadoc names the
     * shape: a counter trusted past the range over which it is valid, turning silently into its own
     * opposite.
     *
     * <p>Worked, at {@code reloadTicks = 7}: {@code (100, 105) -> false} (5 left);
     * {@code (100, 100) -> true}; {@code (100, 99) -> true}; {@code (5, 900) -> true} (restart).
     */
    public static boolean reloadComplete(long now, long completesAt, int reloadTicks) {
        long remaining = completesAt - now;
        return remaining <= 0 || remaining > Math.max(reloadTicks, 0);
    }

    /**
     * Ticks left on a reload, or 0 once it is finished. What the refusal message counts down.
     *
     * <p>Reads "finished" through {@link #reloadComplete}, so the backwards-clock case cannot report
     * a nonsense countdown while the gate above it says the reload is done -- one source of truth for
     * the two questions, rather than two comparisons that can disagree.
     */
    public static long reloadTicksRemaining(long now, long completesAt, int reloadTicks) {
        return reloadComplete(now, completesAt, reloadTicks) ? 0L : completesAt - now;
    }

    /**
     * A percentage modifier applied to an integer capacity, rounded DOWN.
     *
     * <h2>Why floor</h2>
     *
     * <p>It is the only mode in which a modifier never delivers MORE than it claims. {@code ceil}
     * makes "+1%" worth a whole round; {@code Math.round} puts a threshold at half a round, so on a
     * base of 8 a "+6.25%" silently becomes +1. Floor only ever UNDER-delivers -- and an
     * under-delivery can be named on a tooltip, where a silent inflation cannot be.
     *
     * <h2>THE MODE IS THE EASY HALF. THE DEFECT LIVES IN THE EXPRESSION.</h2>
     *
     * <p>Brute-forced base 1..64 x percent 1..200, keeping only cases whose exact product is a whole
     * number, and counting where a floor loses one:
     *
     * <pre>
     *   base * (1 + p/100)        13 defects   &lt;- the natural-reading form, and the broken one
     *   base + base * (p/100)      0
     *   base * (100 + p) / 100     0           &lt;- this one
     * </pre>
     *
     * <p>The failures are real and ordinary: {@code 25 +16%} evaluates to {@code 28.999999999999996}
     * under the first form, so a floor yields <b>28 where the exact answer is 29</b>. Also
     * {@code 50 +16% -> 57.99999999999999} (exact 58) and {@code 45 +40% -> 62.99999999999999}
     * (exact 63).
     *
     * <p>This form defers every rounding to a single division of an exactly-represented product, so
     * there is no intermediate {@code p/100} to be inexact before it is multiplied. <b>Do not
     * "simplify" it to {@code base * (1 + percent / 100.0)}.</b> That reads better, compiles, passes
     * every test whose base is a power of two, and is the version measured above as wrong.
     *
     * <p><b>AND THE SHIPPED BASE HIDES ALL OF IT.</b> 8 is a power of two and never fails in the
     * searched range ({@code 8 +16% = 9.28}, {@code 8 +25% = 10.0}, both exact). A test at 8 proves
     * nothing about 25, and 25 is an ordinary capacity the moment flat modifiers stack before a
     * percentage -- which is why {@code QuiverTest} pins {@code 25 +16% -> 29} by name.
     *
     * <h2>What this does NOT decide</h2>
     *
     * <p><b>Whether quiver modifiers are percentages at all is the operator's call and is UNRULED.</b>
     * This repository already carries both conventions on one {@code Stat} class -- attack speed is a
     * multiplier neutral at 1.0, class damage is "a SUMMAND in points, so 0" -- and an integer
     * capacity fits the summand convention exactly: under "+2 Quiver" there is no rounding mode, no
     * expression hazard, and no dead zone. This ships regardless, because a percentage eventually
     * arrives from a set bonus or a global buff even if quiver enchants are flat, and "we decided not
     * to have percentages" is a premise that outlives itself silently.
     *
     * <p><b>THE DEAD ZONE IS NOT A ROUNDING PROBLEM AND NO MODE FIXES IT.</b> At a base of 8 one round
     * is 12.5%, so under floor a "+5%" and a "+10%" both deliver ZERO. An enchant whose tooltip
     * advertises a buff and grants nothing is a falsified line shown to a player, who -- unlike a
     * developer reading a stale comment -- has no way to check it. The remedy is a display rule (the
     * tooltip renders what is STAMPED, never what was authored) or a floor on the modifier, and
     * <b>neither is decided here.</b> Escalated in {@code PLAN-quiver.md} rather than closed by the
     * choice of rounding mode.
     */
    public static int applyPercent(int base, double percent) {
        return (int) Math.floor(base * (100.0 + percent) / 100.0);
    }
}
