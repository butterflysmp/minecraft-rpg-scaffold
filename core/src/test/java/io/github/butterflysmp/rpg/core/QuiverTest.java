package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The quiver arithmetic: the magazine that brakes the Ranger, and the reload that refills it.
 *
 * <h2>EVERY ROW IS STAGED AT NUMBERS NO SHIPPED WEAPON USES, AND THAT IS THE POINT</h2>
 *
 * <p>Capacity {@link #CAPACITY} and reload {@link #RELOAD_TICKS} are deliberately <b>not</b> any
 * content file's values. This is one of the two guards that make slice A2's confinement
 * <b>observable rather than intended</b>: A2 promotes capacity and reload ticks to real stats, and
 * was budgeted as a change to the two call sites that SUPPLY them. That holds only while
 * {@link Quiver} never reads them itself -- and a {@code Quiver} that silently referenced a shipped
 * constant would fail every row below on its first run. {@code QuiverSignatureTest} is the other
 * guard, and it fails the build mechanically rather than behaviourally.
 *
 * <p>Redden any row by deleting the clamp it rests on, flipping {@link Quiver#isEmpty}'s comparison,
 * dropping the backwards-clock guard, or "simplifying" the percentage expression.
 */
class QuiverTest {

    /** NOT any weapon's capacity. See the class javadoc -- that is the whole reason for the value. */
    private static final int CAPACITY = 3;
    /** NOT any weapon's reload. Same reason. */
    private static final int RELOAD_TICKS = 7;

    // ---------------------------------------------------------------- spending to empty

    /**
     * THE CAPACITY RULE, AS A COUNT RATHER THAN A STOP.
     *
     * <p><b>"Fires N shots then refuses" is not a test of anything.</b> Capacity N and shots-to-empty
     * are necessarily the same number -- that is arithmetic, not an observation -- so a row asserting
     * only "something stopped at N" passes whether the rule exists or not. Two equal quantities in
     * one row, and at least one of them is measuring the fixture.
     *
     * <p>So this counts the rounds that actually leave the weapon and compares the COUNT against the
     * authored capacity, and then pins the boundary from both sides separately below.
     */
    @Test
    void aQuiverFiresExactlyItsCapacityAndTheCountIsWhatIsAsserted() {
        int loaded = Quiver.reload(CAPACITY);
        int fired = 0;
        while (!Quiver.isEmpty(loaded)) {
            loaded = Quiver.spend(loaded);
            fired++;
            if (fired > 100) break;   // a non-terminating spend is a bug, not a hang
        }
        assertEquals(CAPACITY, fired, "rounds that left the weapon, counted, against the capacity");
        assertTrue(Quiver.isEmpty(loaded));
    }

    /**
     * THE BOUNDARY'S LOWER HALF: round N still fires.
     *
     * <p>An off-by-one that refuses at N-1 leaves a quiver one short forever, and the counting row
     * above cannot see it -- it would simply count N-1 and compare against N... which is exactly why
     * BOTH halves are written as positive assertions with the capacity named before the run.
     */
    @Test
    void theLastRoundInTheMagazineStillFires() {
        assertFalse(Quiver.isEmpty(1), "one round left is not empty -- the round is there to be spent");
        assertEquals(0, Quiver.spend(1), "and spending it empties the quiver exactly");
    }

    /** THE BOUNDARY'S UPPER HALF: round N+1 does not. */
    @Test
    void theRoundAfterTheLastOneIsRefused() {
        assertTrue(Quiver.isEmpty(0));
        // The floor, so a caller that misses the gate no-ops instead of corrupting the item. The
        // refusal a player SEES is minted in paper, where the held item can be read.
        assertEquals(0, Quiver.spend(0), "a spend past empty may never drive the count negative");
    }

    /** A malformed negative count must read as empty, not fire forever. */
    @Test
    void aNegativeCountReadsAsEmptyRatherThanFiring() {
        assertTrue(Quiver.isEmpty(-1), "<= 0, not == 0 -- Durability.isBroken's precedent");
        assertEquals(0, Quiver.spend(-4));
    }

    // ---------------------------------------------------------------- reloading

    @Test
    void aReloadRestoresExactlyTheCapacity() {
        assertEquals(CAPACITY, Quiver.reload(CAPACITY), "not capacity + 1, not the pre-spend count");
    }

    @Test
    void aReloadRoutesThroughTheClampSoAnAbsurdCapacityCannotBeWritten() {
        // The one path that sets a count to its maximum is also the one most likely to be handed a
        // nonsense maximum from content. Delegating the bound means it cannot skip it.
        assertEquals(0, Quiver.reload(-5));
    }

    // ---------------------------------------------------------------- the clamp, and capacity change

    @Test
    void clampNeverExceedsCapacityAndNeverGoesNegative() {
        assertEquals(CAPACITY, Quiver.clamp(9999, CAPACITY));
        assertEquals(0, Quiver.clamp(-5, CAPACITY));
        assertEquals(2, Quiver.clamp(2, CAPACITY), "the identity case, which runs on every carry");
    }

    /**
     * THE CAPACITY-CHANGE SEMANTICS, reused verbatim from {@code DESIGN-stat-engine.md}'s max-health
     * rule: an INCREASE is headroom (the count does not move -- gaining capacity is not a free
     * reload, exactly as equipping a +HP item is not a free heal), a DECREASE clamps.
     *
     * <p>Exercised in A1 even though nothing can move capacity until A2, so A2 inherits it proven
     * rather than arriving with the transition untested. The transition is the hazard; the steady
     * state never was.
     */
    @Test
    void aCapacityIncreaseIsHeadroomAndADecreaseClamps() {
        assertEquals(2, Quiver.clamp(2, 8), "capacity 3 -> 8 with 2 loaded: still 2, NOT 8");
        assertEquals(1, Quiver.clamp(3, 1), "capacity 3 -> 1 with 3 loaded: clamped to 1");
        assertEquals(1, Quiver.clamp(1, 8), "below the new capacity either way: unchanged");
    }

    // ---------------------------------------------------------------- the reload clock

    @Test
    void aReloadFinishesOnItsDeadlineAndNotBefore() {
        long start = 100L;
        long deadline = Quiver.reloadCompletesAt(start, RELOAD_TICKS);
        assertEquals(107L, deadline);

        assertFalse(Quiver.reloadComplete(start, start, deadline), "at the start, not done");
        assertFalse(Quiver.reloadComplete(106L, start, deadline), "one tick short, not done");
        assertTrue(Quiver.reloadComplete(107L, start, deadline), "on the deadline, done");
        assertTrue(Quiver.reloadComplete(108L, start, deadline), "past it, done");

        assertEquals(7L, Quiver.reloadTicksRemaining(start, start, deadline));
        assertEquals(1L, Quiver.reloadTicksRemaining(106L, start, deadline));
        assertEquals(0L, Quiver.reloadTicksRemaining(107L, start, deadline));
    }

    /**
     * SHORTENING THE RELOAD MID-FLIGHT MUST NOT COMPLETE IT EARLY.
     *
     * <p>THE ROW THE OLD FIXTURE COULD NOT WRITE. {@link Quiver#reloadComplete} first bounded its
     * anomaly check by the live reload duration, which is sound only while that duration cannot move
     * between the stamp and the read -- and it is precisely what slice A2 makes movable. Executed
     * against that version: stamped at 100 with a 60-tick reload, then read at 105 with the stat
     * dropped to 20, it returned <b>complete</b> and reported <b>0 ticks remaining</b>. Equipping
     * reload-speed gear during a reload was free ammunition, and it looked like the item working.
     *
     * <p><b>Every clock row passed the SAME duration for the stamp and the read</b>, so the fixture
     * held fixed the one condition under which the guard was correct and the suite was green for a
     * reason unrelated to the guard being right. This row varies it: the deadline is committed from a
     * 60-tick duration and then read while the stat says 20.
     *
     * <p>The committed reload keeps the length it was committed at, on
     * {@code AbilityService.resolve}'s existing ruling for cooldowns -- <i>"the swing you have
     * already committed to keeps the cadence it was committed at."</i>
     */
    @Test
    void shorteningTheReloadDurationMidFlightDoesNotFinishItEarly() {
        long startedAt = 100L;
        long deadline = Quiver.reloadCompletesAt(startedAt, 60);
        assertEquals(160L, deadline);

        // Tick 105: the player equips reload-speed gear. Whatever the stat now reads, the committed
        // reload is untouched -- and the live duration cannot reach this method to be compared against.
        assertFalse(Quiver.reloadComplete(105L, startedAt, deadline),
                "55 ticks still to run; a shrunk reload stat must not read as a finished reload");
        assertEquals(55L, Quiver.reloadTicksRemaining(105L, startedAt, deadline));

        // And lengthening it mid-flight is the same story from the other side.
        assertFalse(Quiver.reloadComplete(105L, startedAt, deadline));
    }

    /**
     * THE BACKWARDS CLOCK -- a server restart between stamping the deadline and reading it.
     *
     * <p>The quiver is the first thing in this repository to write a tick value to DISK, and
     * {@code Bukkit.getCurrentTick()} resets on restart. Without the guard the item holds a deadline
     * in a future that never arrives: a weapon permanently mid-reload, with a tooltip that reads
     * perfectly correctly and a player with no way to diagnose it.
     *
     * <p>Unit-testable with no server because the tick is already supplied rather than called --
     * {@code MeleeHits} and {@code DamageWindow} both take {@code Bukkit::getCurrentTick} as a
     * {@code LongSupplier}. Here the same freedom is exercised by simply passing a smaller "now".
     */
    @Test
    void aDeadlineFurtherAwayThanTheReloadItselfMeansTheClockRestarted() {
        // Stamped at tick 895 on the old clock: deadline 902. The server restarts; now is 5, which
        // is BELOW the stamped start -- a reload cannot legitimately be read before it began.
        assertTrue(Quiver.reloadComplete(5L, 895L, 902L),
                "now < startedAt is proof the counter is not the one that wrote the stamp");
        assertEquals(0L, Quiver.reloadTicksRemaining(5L, 895L, 902L),
                "and the countdown agrees with the gate -- one source of truth, not two comparisons");

        // The boundary: standing exactly ON the stamped start is the normal first read of a reload.
        assertFalse(Quiver.reloadComplete(100L, 100L, 107L),
                "now == startedAt is the ordinary start, and must NOT be read as a restart");

        // THE RESIDUAL, PINNED AS A KNOWN LIMIT RATHER THAN LEFT TO BE FOUND. A counter that
        // restarts INSIDE the window is indistinguishable from an ordinary reload in progress, and
        // is deliberately not caught: the cost is bounded by one reload duration and then the
        // weapon reloads normally. The unbounded case -- a weapon stranded forever -- is the one
        // above, and that is the one the guard exists for.
        assertFalse(Quiver.reloadComplete(898L, 895L, 902L),
                "a restart landing mid-window reads as ordinary; bounded wrongness, by design");
    }

    /**
     * The reload deadline derives from NOW, and there is nowhere to gate it.
     *
     * <p>THE FENCEPOST, in the only form core can hold it. The operator's ruling is that a reload
     * started while the fire cooldown is still running <b>begins immediately</b> rather than waiting
     * the cooldown out -- which matters because slice B quotes its shots-per-second off the answer,
     * and A1 decides it by implementation whether or not anyone rules it.
     *
     * <p>Here that is structural: {@link Quiver#reloadCompletesAt} takes {@code now} and a duration,
     * and <b>no cooldown parameter exists for a gate to be threaded through</b>. The behavioural half
     * -- that the left-click path does not consult the fire timer before calling this -- is a
     * property of a {@code paper} call site and is tested in commit 2, not here. Said plainly because
     * a guard that quietly did not land is the kind that produces no symptom.
     */
    @Test
    void aReloadsDeadlineIsMeasuredFromNowWithNoCooldownInTheExpression() {
        assertEquals(107L, Quiver.reloadCompletesAt(100L, RELOAD_TICKS));
        assertEquals(7L, Quiver.reloadCompletesAt(0L, RELOAD_TICKS),
                "the same duration from any starting tick -- nothing else enters it");
    }

    /**
     * The deadline is STAMPED, not recomputed per read -- so a committed reload keeps its length.
     *
     * <p>The other half of the mid-flight rule above, and the reason the deadline is stored rather
     * than derived from the live stat each time it is read. Existing ruling, not a new one:
     * {@code AbilityService.resolve} decided it for cooldowns and the words carry over --
     * <i>"gaining or losing attack speed mid-cooldown does not retroactively lengthen or shorten a
     * timer already running."</i>
     */
    @Test
    void theCommittedDeadlineDoesNotMoveWhenTheReloadDurationDoes() {
        assertEquals(160L, Quiver.reloadCompletesAt(100L, 60), "committed at 60 ticks");
        // A later stamp with a different duration produces a different deadline -- which is correct,
        // and is exactly why the OLD deadline must not be re-derived from the NEW duration.
        assertEquals(120L, Quiver.reloadCompletesAt(100L, 20), "a LATER reload commits at 20");
    }

    // ---------------------------------------------------------------- the percentage rule

    /**
     * THE FLOATING-POINT CASE THE SHIPPED BASE CANNOT SEE.
     *
     * <p>Brute-forced base 1..64 x percent 1..200 over cases whose exact product is a whole number:
     * {@code base * (1 + p/100)} loses a round in <b>13</b> of them; {@code base * (100 + p) / 100}
     * and {@code base + base * (p/100)} lose none. {@code 25 +16%} is the smallest failure --
     * {@code 28.999999999999996}, floored to 28 where the exact answer is 29.
     *
     * <p><b>A base of 8 is a power of two and never fails</b>, so a test staged only at the shipped
     * capacity would pass under the broken expression forever. This row exists specifically to be
     * unfoolable by that, which is why it is staged away from both 8 and this file's own capacity 3.
     */
    @Test
    void aPercentageThatLandsOnAWholeNumberIsNotLostToFloatingPoint() {
        assertEquals(29, Quiver.applyPercent(25, 16), "25 +16% is exactly 29, not 28");
        assertEquals(58, Quiver.applyPercent(50, 16));
        assertEquals(63, Quiver.applyPercent(45, 40));
        // Mutation: applyPercent -> (int) Math.floor(base * (1 + percent / 100.0)) reddens all three
        // and leaves every row below green, which is the whole reason the three are here.
    }

    /**
     * FLOOR ROUNDS AGAINST THE PLAYER IN BOTH DIRECTIONS, AND THAT IS THE CHOICE.
     *
     * <p>This row was first written as <i>"a modifier never delivers more than it claims"</i>, which
     * is <b>true of buffs and false of debuffs</b> -- and the name of a test is the durable record,
     * so the next reader would have taken the over-broad form as the rule. Measured: at a base of 8,
     * {@code -10%} is {@code floor(7.2) = 7}, so the penalty DELIVERED is a whole round where 0.8 was
     * claimed. The debuff over-delivers by exactly the amount the buff under-delivers.
     *
     * <p>What is actually true, and is what the mode was chosen for: <b>a positive modifier never
     * delivers more than it claims, and a negative one never delivers less.</b> The bias is always in
     * the same direction, which is consistency rather than generosity, and it is asserted from both
     * sides here so the claim cannot quietly widen again.
     */
    @Test
    void flooringRoundsAgainstThePlayerForBuffsAndDebuffsAlike() {
        // The buff side: under-delivers.
        assertEquals(9, Quiver.applyPercent(8, 15), "9.2 -> 9, claiming 1.2 and granting 1");
        assertEquals(10, Quiver.applyPercent(8, 25), "exactly 10, no rounding to reach it");
        assertEquals(8, Quiver.applyPercent(8, 0), "the identity, which must not drift");

        // The debuff side: over-delivers, by the same mechanism and in the player's disfavour.
        assertEquals(7, Quiver.applyPercent(8, -10), "7.2 -> 7: 0.8 claimed, a whole round taken");
        assertEquals(6, Quiver.applyPercent(8, -25), "exactly 6, no rounding to reach it");
        // 0 IS THE ARITHMETIC ANSWER, AND THIS METHOD HAS NO OPINION ABOUT WHETHER IT IS LEGAL.
        // The comment here used to read "a total debuff empties it and stops there", which reads as
        // APPROVAL -- while QuiverSize argues, from a measured mechanism, that a resolved capacity of
        // 0 is a permanently dead item (refused on fire AND on reload, no third input, no recovery).
        // Two files in one feature disagreeing about whether 0 is a legal capacity. Closed by one
        // enforcement site rather than two opinions: legality is QuiverSize.resolve's, this is
        // arithmetic, and a percentage that ever reaches a capacity passes through resolve. The
        // composition is witnessed by QuiverSizeTest
        // .aTotalPercentageDebuffEvaluatesToZeroAndSTILLRESOLVESToAWorkingCapacity.
        assertEquals(0, Quiver.applyPercent(8, -100), "the arithmetic answer; legality is resolve's");

        // AND THE DEBUFF CASE THAT ACTUALLY DISCRIMINATES THE MODE. The five above are all values
        // where floor and Math.round AGREE, so a mutation to round leaves every one of them green --
        // measured, not assumed: MUTROUND reddened only the dead-zone row below. A row that cannot
        // fail is worth nothing however green, so the debuff half needs a fractional part >= 0.5.
        // -5% of 8 is 7.6: floor takes a WHOLE round for a 0.4-round claim; round would take none.
        assertEquals(7, Quiver.applyPercent(8, -5),
                "7.6 -> 7 under floor, 8 under round -- this is the assertion that pins the MODE");
    }

    /**
     * THE DEAD ZONE, PINNED AS A FACT RATHER THAN LEFT TO BE DISCOVERED IN PLAY.
     *
     * <p>At a base of 8 one round is 12.5%, so under floor a "+5%" and a "+10%" both grant NOTHING.
     * That is not a defect in the rounding mode -- no mode fixes it -- but it IS a tooltip that can
     * advertise a buff and deliver none, which is a falsified line shown to a player who cannot check
     * it. Asserted here so the day someone closes the dead zone this row fails and makes them say so,
     * rather than the behaviour changing quietly under a display that still reads the same.
     */
    @Test
    void asmallPercentageOnASmallBaseGrantsNothingAndThatIsARecordedConsequence() {
        assertEquals(8, Quiver.applyPercent(8, 5));
        assertEquals(8, Quiver.applyPercent(8, 10));
        assertEquals(9, Quiver.applyPercent(8, 12.5), "one whole round is 12.5% at this base");
    }
}
