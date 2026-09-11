package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The quiver's VERDICTS -- the orderings that decide whether a shot happens.
 *
 * <h2>WHY THIS FILE EXISTS AT ALL, WHICH IS THE USEFUL PART</h2>
 *
 * <p>Every row here was previously unreachable by any test. The composition lived in
 * {@code paper}'s {@code Quivers.resolveForShot} (then named {@code refusalFor}), which takes a {@code Player}, and this project has no
 * MockBukkit and cannot construct an {@code ItemStack} without a running server. The ARITHMETIC was
 * unit-tested from the first commit; the DECISIONS built on it were not.
 *
 * <p><b>The signal was a suite total that did not move.</b> The commit that introduced those
 * decisions added 303 lines of production code across two new classes and zero test rows, and the
 * figure was re-read and quoted as routine. An unchanged total after two new classes is the loudest
 * line in a report.
 *
 * <p>Staged at capacity {@link #CAPACITY} and reload {@link #RELOAD_TICKS} -- not the shipped
 * numbers -- for the reason {@code QuiverTest} gives: it is half of what makes slice A2's
 * confinement observable rather than intended.
 */
class QuiverStateTest {

    private static final int CAPACITY = 3;
    private static final int RELOAD_TICKS = 7;
    private static final long START = 100L;
    private static final long DEADLINE = START + RELOAD_TICKS;   // 107

    // ---------------------------------------------------------------- the plain states

    @Test
    void aLoadedQuiverFires() {
        assertEquals(QuiverState.Fire.FIRE, QuiverState.loaded(2, CAPACITY).fireVerdict(START));
    }

    @Test
    void aSpentQuiverIsEmpty() {
        assertEquals(QuiverState.Fire.EMPTY, QuiverState.loaded(0, CAPACITY).fireVerdict(START));
    }

    /**
     * ABSENCE IS NOT EMPTINESS, and the verdict keeps them apart.
     *
     * <p>An item carrying no count was never stamped by a mint path -- a defect. Collapsing it into
     * EMPTY would turn a forgotten stamp into a weapon that silently never fires, behind a message
     * that reads perfectly reasonable. This is the one verdict whose correct handling is to REPAIR
     * the item and complain, not to refuse the player.
     */
    @Test
    void anUnstampedQuiverIsADefectAndNotAnEmptyMagazine() {
        assertEquals(QuiverState.Fire.UNSTAMPED, QuiverState.unstamped(CAPACITY).fireVerdict(START));
        assertEquals(QuiverState.Reload.UNSTAMPED,
                QuiverState.unstamped(CAPACITY).reloadVerdict(START));
    }

    // ---------------------------------------------------------------- the four orderings

    /**
     * ORDERING 1 -- A RUNNING RELOAD BEATS EMPTY.
     *
     * <p>An item mid-reload still carries the count it had when the reload began, which is almost
     * always 0. If the empty check ran first, a player who is visibly reloading would be told
     * "empty -- left-click to reload", which is both wrong and useless: they already did.
     */
    @Test
    void aRunningReloadBeatsEmptyEvenThoughTheCountIsZero() {
        QuiverState mid = QuiverState.reloading(0, CAPACITY, START, DEADLINE);

        assertEquals(QuiverState.Fire.RELOADING, mid.fireVerdict(START + 1));
        assertTrue(mid.isReloading(START + 1));
        assertEquals(6L, mid.reloadTicksRemaining(START + 1));
    }

    /**
     * ORDERING 2 -- THE SHOT THAT MATURES A RELOAD ALSO FIRES.
     *
     * <p>The read IS the tick: there is no scheduled task, so a reload finishes the first time
     * anything looks at the item. Reporting EMPTY or RELOADING here would drop that press, refill
     * only on the next one, and feel exactly like input lag -- a dropped shot with no error
     * anywhere, and invisible to any test of {@link Quiver}'s arithmetic alone.
     *
     * <p>RELOAD_MATURED is a distinct verdict rather than plain FIRE because the caller has WORK to
     * do -- refill the item and clear the reload keys -- before the shot goes through. Collapsing it
     * into FIRE would fire from a magazine that still reads 0.
     */
    @Test
    void theShotThatMaturesAReloadIsNotDropped() {
        QuiverState mid = QuiverState.reloading(0, CAPACITY, START, DEADLINE);

        assertFalse(mid.isReloading(DEADLINE), "on the deadline the reload is done");
        assertEquals(QuiverState.Fire.RELOAD_MATURED, mid.fireVerdict(DEADLINE),
                "NOT EMPTY -- the stored count is still 0, and reading it first would drop this shot");
        assertEquals(QuiverState.Fire.RELOAD_MATURED, mid.fireVerdict(DEADLINE + 50));
    }

    /**
     * ORDERING 3 -- PRESSING RELOAD WHILE ONE RUNS MUST NOT RESTART IT.
     *
     * <p>The arm-swing packet arrives roughly once per tick while left-click is held. An
     * unconditional start would push the deadline another {@code reload_ticks} away twenty times a
     * second, and the weapon would never come back -- with nothing anywhere reporting a problem.
     */
    @Test
    void pressingReloadDuringAReloadDoesNotRestartIt() {
        QuiverState mid = QuiverState.reloading(0, CAPACITY, START, DEADLINE);

        assertEquals(QuiverState.Reload.ALREADY_RELOADING, mid.reloadVerdict(START + 1));
        assertEquals(QuiverState.Reload.ALREADY_RELOADING, mid.reloadVerdict(DEADLINE - 1));
        assertEquals(QuiverState.Reload.RELOAD_MATURED, mid.reloadVerdict(DEADLINE),
                "once it matures the press refills rather than starting a second one");
    }

    /** ORDERING 4 -- reloading a full magazine is refused, not three dead seconds. */
    @Test
    void reloadingAFullMagazineIsRefused() {
        assertEquals(QuiverState.Reload.ALREADY_FULL,
                QuiverState.loaded(CAPACITY, CAPACITY).reloadVerdict(START));
        assertEquals(QuiverState.Reload.BEGIN,
                QuiverState.loaded(CAPACITY - 1, CAPACITY).reloadVerdict(START),
                "one round short IS worth reloading");
        assertEquals(QuiverState.Reload.BEGIN,
                QuiverState.loaded(0, CAPACITY).reloadVerdict(START));
    }

    /**
     * A count ABOVE capacity still reads as full -- {@code >=}, not {@code ==}.
     *
     * <p>Reachable once A2 lets capacity move: a magazine loaded to 9 on gear that is then removed
     * sits above a capacity of 3 until something clamps it. An {@code ==} here would offer a reload
     * that could only ever reduce it.
     */
    @Test
    void aCountAboveCapacityStillCountsAsFull() {
        assertEquals(QuiverState.Reload.ALREADY_FULL,
                QuiverState.loaded(CAPACITY + 6, CAPACITY).reloadVerdict(START));
    }

    // ---------------------------------------------------------------- the shape itself

    /**
     * A reload's two ticks are one fact, and a half-reload is unrepresentable.
     *
     * <p>The start is the restart guard's bound, so a deadline without one cannot be checked at all.
     * Refusing the pair in the constructor means no caller can express it by accident -- the
     * modelling this record exists for, rather than a loose parameter list carrying two adjacent
     * booleans the compiler could not tell apart.
     */
    @Test
    void aReloadsStartAndDeadlineCannotBeSeparated() {
        // THROUGH THE FACTORY, because the canonical constructor is now PRIVATE -- a public record's
        // was public, and `new QuiverState(loaded, weapon.quiverSize(), from, to)` was a door that
        // fabricated a capacity past every guard. The validation still lives in the constructor; the
        // factory is simply the only way to reach it.
        assertThrows(IllegalArgumentException.class, () -> QuiverState.from(
                OptionalInt.of(0), OptionalInt.empty(), CAPACITY,
                OptionalLong.of(START), OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> QuiverState.from(
                OptionalInt.of(0), OptionalInt.empty(), CAPACITY,
                OptionalLong.empty(), OptionalLong.of(DEADLINE)));
    }

    /**
     * THE RESTART GUARD REACHES THE VERDICT, not just the arithmetic.
     *
     * <p>{@code QuiverTest} proves {@link Quiver#reloadComplete} handles a counter that restarted
     * below the stamp. This proves the verdict built on it does too -- otherwise a server restart
     * mid-reload would leave a weapon reporting RELOADING forever, which is the stranded-weapon case
     * the guard exists for, arriving one layer up.
     */
    @Test
    void aRestartedClockMaturesTheReloadRatherThanStrandingTheWeapon() {
        QuiverState mid = QuiverState.reloading(0, CAPACITY, 895L, 902L);

        assertEquals(QuiverState.Fire.RELOAD_MATURED, mid.fireVerdict(5L));
        assertEquals(0L, mid.reloadTicksRemaining(5L));
    }

    // ---------------------------------------------------------------- the capacity's three sources

    /**
     * THE STAMP WINS OVER THE AUTHORED VALUE, BECAUSE THE STAMP IS WHAT GOVERNED WHEN IT WAS PACKED.
     *
     * <p>Once capacity is a stat, a boosted wielder genuinely fires more than the weapon declares.
     * {@code capacityOf} is the single place the stamp beats the definition, so the tooltip and the
     * refusal logic cannot disagree — a tooltip rendering the stamp while the refusal resolved the
     * holder live would lie by a new mechanism.
     */
    @Test
    void aStampedCapacityBeatsTheAuthoredOne() {
        assertEquals(11, QuiverState.capacityOf(OptionalInt.of(11), 9),
                "gear resolved 11 when this was last packed; the weapon still declares 9");
        assertEquals(9, QuiverState.capacityOf(OptionalInt.of(9), 9));
        assertEquals(2, QuiverState.capacityOf(OptionalInt.of(2), 9),
                "a REDUCED capacity is stamped and obeyed too -- the stamp is not a maximum");
    }

    /**
     * AN ABSENT CAPACITY FALLS BACK TO AUTHORED, AND IS NOT A DEFECT -- unlike an absent COUNT.
     *
     * <p>The asymmetry is the point. A count has no item-free meaning, so its absence means a mint
     * path failed and {@link QuiverState.Fire#UNSTAMPED} reports it. A capacity has a perfectly good
     * item-free answer: the weapon's own.
     *
     * <p>Three real readers depend on this and none of them is a bug: {@code GoldenLoreTest} renders
     * from definitions with no item at all, a recipe-browser icon previews a weapon rather than a
     * held one, and an item minted before this stamp existed carries no capacity — so this doubles
     * as the migration path.
     */
    @Test
    void anAbsentCapacityFallsBackToTheAuthoredValue() {
        assertEquals(9, QuiverState.capacityOf(OptionalInt.empty(), 9),
                "no item to read: the weapon's own number is the true answer, not a defect");
        assertEquals(0, QuiverState.capacityOf(OptionalInt.empty(), 0),
                "and a weapon with no quiver stays at no quiver");
    }

    /**
     * THE STAMP REACHES THE VERDICT, NOT JUST THE TOOLTIP -- and this is the worse of the two holes.
     *
     * <p>{@code MUTSTATEDROP} -- replacing the stamp with {@code OptionalInt.empty()} where
     * {@code Quivers.stateOf} resolved it -- passed the ENTIRE suite. {@code stateOf} needs an
     * {@code ItemStack}, so it has no unit test; the guard over its call site is a source scan, which
     * proves no sixth resolver exists and <b>nothing about what the five do.</b>
     *
     * <p><b>The consequence, once capacity is a stat:</b> a Ranger boosted to 11 fires down to 9,
     * presses reload, and {@link QuiverState.Reload#ALREADY_FULL} tells them the magazine is full.
     * <b>The last two rounds are permanently unreachable</b> — while the tooltip, resolved correctly
     * from the same stamp, reads 9/11 beside it. The tooltip right and the mechanic wrong.
     *
     * <p>Staged through {@link QuiverState#from}, which is where the resolution moved so that a row
     * could reach it: 9 loaded, stamp 11, weapon declaring 9.
     */
    @Test
    void aStampedCapacityAboveTheAuthoredOneLeavesRoomToReload() {
        QuiverState boosted = QuiverState.from(OptionalInt.of(9), OptionalInt.of(11), 9,
                OptionalLong.empty(), OptionalLong.empty());

        assertEquals(11, boosted.capacity(), "the stamp, not the weapon's authored 9");
        assertEquals(QuiverState.Reload.BEGIN, boosted.reloadVerdict(START),
                "9 of 11 is NOT full -- reading the authored 9 here strands the last two rounds");
        assertEquals(QuiverState.Fire.FIRE, boosted.fireVerdict(START));
    }

    /** And the fallback through the same factory, so the pair discriminates. */
    @Test
    void withNoStampTheFactoryFallsBackToTheAuthoredCapacity() {
        QuiverState plain = QuiverState.from(OptionalInt.of(9), OptionalInt.empty(), 9,
                OptionalLong.empty(), OptionalLong.empty());

        assertEquals(9, plain.capacity());
        assertEquals(QuiverState.Reload.ALREADY_FULL, plain.reloadVerdict(START),
                "9 of an authored 9 IS full -- the fallback must not invent headroom either");
    }
}
