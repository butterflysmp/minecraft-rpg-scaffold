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

    /**
     * Enough arrows that the ammo rung never fires, so every row below still measures the rung it was
     * written for.
     *
     * <h2>THIS IS A DELIBERATE NON-CHANGE, AND THE ALTERNATIVE WAS TO SILENTLY RE-AIM ELEVEN ROWS</h2>
     *
     * <p>Slice E gave {@code reloadVerdict} a second parameter, which made all eleven call sites in
     * this file a compile error -- the point of changing the signature rather than overloading it.
     * Each was written before ammo existed and asserts something about a DIFFERENT rung: unstamped,
     * already-reloading, matured, already-full, the boosted-capacity cases.
     *
     * <p><b>Passing a value that cannot trip the new rung is what keeps those assertions about what
     * they were about.</b> Passing {@code 0} would have quietly converted several of them into
     * no-ammo rows that happen to still pass, which is a worse outcome than a red build: the file
     * would look unchanged and measure something else.
     *
     * <p>The no-ammo rung has its own rows, in {@code QuiverAmmoTest}, staged deliberately.
     */
    private static final int PLENTY = 64;
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
                QuiverState.unstamped(CAPACITY).reloadVerdict(START, PLENTY));
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

        assertEquals(QuiverState.Reload.ALREADY_RELOADING, mid.reloadVerdict(START + 1, PLENTY));
        assertEquals(QuiverState.Reload.ALREADY_RELOADING, mid.reloadVerdict(DEADLINE - 1, PLENTY));
        assertEquals(QuiverState.Reload.RELOAD_MATURED, mid.reloadVerdict(DEADLINE, PLENTY),
                "once it matures the press refills rather than starting a second one");
    }

    /** ORDERING 4 -- reloading a full magazine is refused, not three dead seconds. */
    @Test
    void reloadingAFullMagazineIsRefused() {
        assertEquals(QuiverState.Reload.ALREADY_FULL,
                QuiverState.loaded(CAPACITY, CAPACITY).reloadVerdict(START, PLENTY));
        assertEquals(QuiverState.Reload.BEGIN,
                QuiverState.loaded(CAPACITY - 1, CAPACITY).reloadVerdict(START, PLENTY),
                "one round short IS worth reloading");
        assertEquals(QuiverState.Reload.BEGIN,
                QuiverState.loaded(0, CAPACITY).reloadVerdict(START, PLENTY));
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
                QuiverState.loaded(CAPACITY + 6, CAPACITY).reloadVerdict(START, PLENTY));
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
        assertEquals(QuiverState.Reload.BEGIN, boosted.reloadVerdict(START, PLENTY),
                "9 of 11 is NOT full -- reading the authored 9 here strands the last two rounds");
        assertEquals(QuiverState.Fire.FIRE, boosted.fireVerdict(START));
    }

    /** And the fallback through the same factory, so the pair discriminates. */
    @Test
    void withNoStampTheFactoryFallsBackToTheAuthoredCapacity() {
        QuiverState plain = QuiverState.from(OptionalInt.of(9), OptionalInt.empty(), 9,
                OptionalLong.empty(), OptionalLong.empty());

        assertEquals(9, plain.capacity());
        assertEquals(QuiverState.Reload.ALREADY_FULL, plain.reloadVerdict(START, PLENTY),
                "9 of an authored 9 IS full -- the fallback must not invent headroom either");
    }

    /**
     * A CAPACITY OF ZERO WOULD BE A REFUSAL ON BOTH INPUTS -- the mechanism argument for a floor,
     * witnessed rather than asserted.
     *
     * <p><b>THIS ROW IS LOAD-BEARING NOW, NOT SPECULATIVE, AND THE FIRST VERSION OF THIS PARAGRAPH
     * SAID OTHERWISE.</b> It read: <i>"nothing can produce this state today ...
     * {@code QuiverSize.boosts} is strictly {@code >}, so no gear can reduce it."</i> That was
     * support from a filter with no production call sites -- and {@code QuiverSize.resolve} never
     * consulted it anyway, so {@code resolve(9, -5.0)} was {@code 4}. The API was the way down.
     *
     * <p>What this row measures is now the ARGUMENT FOR AN ENFORCED FLOOR:
     * {@code QuiverSize.MIN_CAPACITY}, applied in {@code resolve}, which cites this test by name.
     * The condition is stated on the RESOLVED CAPACITY rather than on which authoring shape produced
     * it -- because {@code Quiver.applyPercent(8, -100)} is a green, tested route to 0 that involves
     * no reducing modifier at all, and a trigger phrased as "the day a reducing modifier ships"
     * would not have fired for it.
     *
     * <p>Staged by CAUSING the condition rather than by asserting a guard exists, which is the only
     * way to test a case shipped content cannot reach.
     *
     * <p>The point: capacity 0 is not a very small quiver, it is <b>an item with no reachable
     * state</b>. Fire is refused as empty; reload is refused as already full; there is no third
     * input, so there is no way out. That is a MECHANISM argument for a floor of at least 1.
     *
     * <p><b>The contrast this paragraph used to draw was with a constant that does not exist.</b> It
     * read "as opposed to {@code Quiver.MIN_RELOAD_TICKS}, which is a named placeholder at the no-op
     * value" -- {@code Quiver} has no fields at all, and {@code QuiverSignatureTest} pins exactly
     * that. The real contrast is with the reload duration itself, and it survives the correction:
     * {@code Quiver.reloadCompletesAt} is {@code now + Math.max(reloadTicks, 0)}, so a resolved 0
     * stamps a deadline equal to its start and matures on the same tick. <b>No value of the reload
     * duration leaves the mechanic without a reachable state, so reload gets no floor at all</b> --
     * which is the same standard applied, not a weaker one.
     */
    @Test
    void aCapacityOfZeroWouldBeARefusalOnBothInputs() {
        QuiverState dead = QuiverState.loaded(0, 0);

        assertEquals(QuiverState.Fire.EMPTY, dead.fireVerdict(START),
                "firing a 0-capacity quiver is refused as empty");
        assertEquals(QuiverState.Reload.ALREADY_FULL, dead.reloadVerdict(START, PLENTY),
                "and reloading it is refused as already full -- 0 of 0 IS full. Both inputs "
                        + "refused, no third input, no recovery: the item is permanently dead.");
    }

    // ------------------------------------------------------- roundsRemaining, slice G

    /** The ordinary case, and the identity that admits the method to the pinned instance surface. */
    @Test
    void roundsRemainingIsTheLoadedCountAndCompletesTheCapacity() {
        QuiverState state = QuiverState.loaded(2, CAPACITY);

        assertEquals(2, state.roundsRemaining(), "two rounds loaded is two rounds remaining");
        assertEquals(CAPACITY, state.roundsNeeded() + state.roundsRemaining(),
                "needed + remaining == capacity. THIS is the sentence QuiversSignatureTest admits "
                        + "roundsRemaining() on: the pair recovers only capacity(), which is public "
                        + "already, so nothing new becomes derivable");
    }

    /**
     * UNSTAMPED IS ZERO, AND THIS IS THE ROW THAT CATCHES {@code MUT-REMAINING}.
     *
     * <p>An unstamped stamp is a DEFECT, not an empty magazine, and R3's cap must promise nothing
     * rather than something. <b>The wrong implementation -- {@code capacity() - roundsNeeded()} in
     * paper -- reports a FULL MAGAZINE here</b>, because {@code roundsNeeded()} returns 0 on an
     * unstamped item for a reason that belongs to that method and does not transfer.
     *
     * <p><b>No stamped fixture can see that mutation at all</b>, because the two forms agree
     * everywhere else. This row is the only one that does.
     */
    @Test
    void roundsRemainingIsZeroWhenUnstampedRatherThanAFullMagazine() {
        QuiverState unstamped = QuiverState.unstamped(CAPACITY);

        assertEquals(0, unstamped.roundsRemaining(),
                "an item with no count has no rounds. capacity() - roundsNeeded() would answer "
                        + CAPACITY + " here -- a full magazine, for an item that has never been "
                        + "stamped");
        assertEquals(QuiverState.Fire.UNSTAMPED, unstamped.fireVerdict(START),
                "and the zero does not lose the distinction: fireVerdict still separates an "
                        + "unstamped item from an empty one BY NAME, which is why the collision in "
                        + "value costs nothing");
    }

    /**
     * AN OVER-FULL MAGAZINE IS CLAMPED, AND THE ONE-ROUND GAP IS KNOWN AND CHOSEN.
     *
     * <p>Reachable in the window after a capacity modifier comes off and before the next write
     * re-clamps the stamp: {@code loaded} 11 against a capacity that now resolves to 8.
     *
     * <p><b>MEASURED: SUCH A MAGAZINE PAYS FOR NINE.</b> {@code Quivers.spendRound} is
     * {@code Quiver.spend(11) = 10} handed to {@code QuiverItems.setLoaded}, which clamps the value
     * it WRITES -- {@code Quiver.clamp(10, 8) = 8}. One over-full shot, then a magazine of eight.
     *
     * <pre>
     * unclamped  11   promises two arrows that are not coming
     * measured    9   exact, but it projects future WRITES rather than reading state
     * CHOSEN      8   under-promises by one, never over, and never couples to the write path
     * </pre>
     *
     * <p><b>So this row asserts 8 while recording 9, deliberately.</b> It is not an off-by-one. The
     * nine is written down here so the next reader meets it before deciding the eight is a bug --
     * closing the gap means modelling what the write funnel will do next, which is the coupling the
     * clamped form exists to avoid.
     *
     * <p><b>AND THE PAYOUT OF NINE IS ITSELF UNGUARDED.</b> {@code Quiver.clamp} and
     * {@code Quiver.spend} each have rows; their COMPOSITION has none, and it lives in paper --
     * {@code spendRound} plus {@code setLoaded}. Recorded as a second unguarded edge on
     * {@code GATE-expanded-quiver.md} row A-5, whose own trigger is the first shipped item granting
     * quiver size outside the dev instrument.
     */
    @Test
    void roundsRemainingClampsAnOverFullMagazineToItsResolvedCapacity() {
        QuiverState overFull = QuiverState.loaded(11, 8);

        assertEquals(8, overFull.roundsRemaining(),
                "clamped to the resolved capacity -- never promising rounds the tracker cannot be "
                        + "sure of. The magazine actually pays for NINE (one over-full shot, then "
                        + "the clamp at the write); 8 under-promises by one, on purpose");
        assertEquals(0, overFull.roundsNeeded(), "an over-full magazine needs nothing");
        assertEquals(8, overFull.roundsNeeded() + overFull.roundsRemaining(),
                "and the identity still holds BECAUSE of the clamp: 0 + 8 == capacity. Unclamped "
                        + "it would be 0 + 11, which recovers neither the capacity nor anything "
                        + "else -- and the signature test's admission argument would stop being true");
    }
}
