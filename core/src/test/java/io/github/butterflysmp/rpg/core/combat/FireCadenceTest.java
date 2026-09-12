package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The instrument for Q7 — a row deferred twice because nothing measured it.
 *
 * <p>Every row here drives a FAKE CLOCK, which is the whole reason this class lives in {@code core}:
 * the two call sites need a live {@code Player} and are boot-only, but the arithmetic that turns
 * stamps into a reading is not, and it is the half that can be wrong silently.
 *
 * <p>Each row names what it forces red, and <b>all four were RUN</b> — spliced by line number, the
 * marker grepped both directions, the byte delta recomputed from the edit rather than quoted, and
 * the file restored byte-identical from a scratchpad copy:
 *
 * <table><tr><th>mutation</th><th>edit</th><th>bytes</th><th>result</th></tr>
 * <tr><td>{@code MUTMEAN}</td><td>divide by {@code count} instead of {@code count - 1}</td>
 *     <td>+5 = −6 + 11</td><td><b>3 of 9 red</b> — every row that reads a mean</td></tr>
 * <tr><td>{@code MUTWINDOW}</td><td>window measured to {@code currentTick} instead of the last event</td>
 *     <td>+20 = +7 + 13</td><td><b>1 of 9 red</b>, reading {@code 400} where {@code 8} belongs —
 *     the dead time before the command, swallowed</td></tr>
 * <tr><td>{@code MUTMIN}</td><td>report the mean where the minimum belongs</td>
 *     <td>+28 = +18 + 10</td><td><b>1 of 9 red</b>, {@code 3} for a floor of {@code 1}</td></tr>
 * <tr><td>{@code MUTFAULT}</td><td>collapse {@code INSTRUMENT_FAULT} into {@code COOLDOWN_LIMITED}</td>
 *     <td>+12 = 0 + 12</td><td><b>1 of 9 red</b> — <b>the control reporting a broken instrument as a
 *     measurement is exactly the failure the arm exists to prevent</b></td></tr>
 * </table>
 *
 * <p>{@code markers left: 0} after each, grepped unfiltered.
 */
class FireCadenceTest {

    private static final String RIGHT = "right_click";
    private static final String BOW = "hunters_bow";

    private final AtomicLong clock = new AtomicLong(1_000L);
    private final FireCadence cadence = new FireCadence(clock::get);
    private final UUID player = UUID.randomUUID();

    private void at(long tick) {
        clock.set(tick);
    }

    private void input(long tick) {
        at(tick);
        cadence.record(player, RIGHT, FireCadence.Kind.INPUT, BOW);
    }

    private void fire(long tick) {
        at(tick);
        cadence.record(player, RIGHT, FireCadence.Kind.FIRE, BOW);
    }

    private FireCadence.Sample inputs() {
        return cadence.sample(player, RIGHT, FireCadence.Kind.INPUT).orElseThrow();
    }

    /**
     * THE MEAN IS THE WINDOW OVER THE GAPS, NOT OVER THE EVENTS.
     *
     * <p>Four events at a steady 4 ticks span 12 ticks and have THREE gaps, so the mean is 4. A
     * divide-by-count would say 3 — which is a plausible-looking number that is simply wrong, and
     * the kind a reader would believe.
     *
     * <p>Forces red: dividing by {@code count} instead of {@code count - 1}.
     */
    @Test
    void theMeanDividesByTheGapsAndNotByTheEvents() {
        input(100);
        input(104);
        input(108);
        input(112);

        FireCadence.Sample s = inputs();
        assertEquals(4, s.count());
        assertEquals(3, s.intervals(), "four events, three gaps");
        assertEquals(12L, s.windowTicks(), "100 to 112");
        assertEquals(4.0, s.meanIntervalTicks().orElseThrow(), 1e-9,
                "12 ticks over 3 gaps. Dividing by 4 events would say 3.0, which is wrong and "
                        + "looks right.");
    }

    /**
     * THE MINIMUM IS THE SMALLEST GAP, NOT THE MEAN — and Q7 is named for the FLOOR.
     *
     * <p>Staged jittery on purpose: gaps of 4, 4, 1, 4. The mean is 3.25 and the floor is 1, and
     * they answer different questions — the floor prices slice C's 7-tick halving, the mean prices
     * ruling 1's "eight rounds is a few seconds" brake. A steady sample cannot tell them apart, so
     * this one is not steady.
     *
     * <p>Forces red: reporting the mean where the minimum belongs, or taking the LAST gap.
     */
    @Test
    void theMinimumIsTheSmallestGapAndNotTheMean() {
        input(100);
        input(104);
        input(108);
        input(109);
        input(113);

        FireCadence.Sample s = inputs();
        assertEquals(13L, s.windowTicks());
        assertEquals(3.25, s.meanIntervalTicks().orElseThrow(), 1e-9, "13 over 4 gaps");
        assertEquals(1L, s.minIntervalTicks().orElseThrow(),
                "the FLOOR is the smallest gap observed -- 1, not the 3.25 mean and not the 4 that "
                        + "three of the four gaps happen to be");
    }

    /**
     * THE WINDOW IS FIRST EVENT TO LAST, NEVER "NOW MINUS FIRST".
     *
     * <p>The clock is advanced well past the last event before reading, exactly as it would be while
     * the operator stops holding the button and types the command. A window measured to {@code now}
     * would swallow that dead time and drag the mean down over nothing.
     *
     * <p>Forces red: measuring the window to {@code currentTick} instead of to the last event.
     */
    @Test
    void theWindowEndsAtTheLastEventAndNotAtTheReading() {
        input(100);
        input(104);
        input(108);

        at(500);   // the operator released the button and walked to the keyboard

        FireCadence.Sample s = inputs();
        assertEquals(8L, s.windowTicks(), "100 to 108, not 100 to 500");
        assertEquals(4.0, s.meanIntervalTicks().orElseThrow(), 1e-9,
                "and the mean is unaffected by when the reading was taken");
    }

    /**
     * ONE EVENT IS A TIMESTAMP, NOT A CADENCE, AND IT SAYS SO.
     *
     * <p>Both interval figures are absent rather than zero. <b>This is the one instrument where a
     * plausible zero would be believed</b> — "min 0t" reads as an infinitely fast weapon.
     *
     * <p>Forces red: defaulting either interval to 0 when there is only one event.
     */
    @Test
    void oneEventHasNoIntervalAtAllRatherThanAZeroOne() {
        input(100);

        FireCadence.Sample s = inputs();
        assertEquals(1, s.count());
        assertEquals(0, s.intervals());
        assertEquals(0L, s.windowTicks(), "a single event spans no time, which IS zero");
        assertTrue(s.meanIntervalTicks().isEmpty(), "but there is no mean interval to report");
        assertTrue(s.minIntervalTicks().isEmpty(), "and no minimum either");
    }

    /**
     * AN UNRECORDED SAMPLE IS ABSENT, NOT AN EMPTY ONE.
     *
     * <p>Run the command twice and the second call has nothing. Returning a zero-count sample would
     * let a caller print figures for a measurement that never happened.
     *
     * <p>Forces red: returning a zero sample instead of {@code Optional.empty()}; {@code clear}
     * leaving state behind.
     */
    @Test
    void nothingRecordedIsEmptyAndClearMakesItEmptyAgain() {
        assertTrue(cadence.sample(player, RIGHT, FireCadence.Kind.INPUT).isEmpty(),
                "before anything is recorded");
        assertEquals(0, cadence.trackedPlayers());

        input(100);
        input(104);
        assertTrue(cadence.sample(player, RIGHT, FireCadence.Kind.INPUT).isPresent());
        assertEquals(1, cadence.trackedPlayers());

        cadence.clear(player);
        assertTrue(cadence.sample(player, RIGHT, FireCadence.Kind.INPUT).isEmpty(),
                "print and clear means the next call has nothing, and says so");
        assertEquals(0, cadence.trackedPlayers());
    }

    /**
     * THE TWO KINDS AND THE TWO INPUTS ARE SEPARATE SAMPLES.
     *
     * <p>Inputs and fires must not pool, or the verdict compares a number with itself. And keying by
     * input is what keeps the string {@code "right_click"} out of {@code WeaponFire} — the command
     * asks for it, which is where the question is being asked.
     *
     * <p>Forces red: keying by player alone; dropping {@code Kind} or {@code input} from the key.
     */
    @Test
    void theKindsAndTheInputsDoNotPoolIntoOneSample() {
        input(100);
        input(104);
        fire(104);
        cadence.record(player, "left_click", FireCadence.Kind.INPUT, BOW);

        assertEquals(2, inputs().count(), "two right-click inputs");
        assertEquals(1, cadence.sample(player, RIGHT, FireCadence.Kind.FIRE).orElseThrow().count());
        assertEquals(1, cadence.sample(player, "left_click", FireCadence.Kind.INPUT)
                .orElseThrow().count(), "and left-click is its own sample");
    }

    /**
     * A SAMPLE SPANNING TWO WEAPONS IS NOT A READING.
     *
     * <p>"Two weapons disagreeing about the input floor is a finding" cannot BE a finding if one
     * sample silently averages both. The flag is what lets the readout refuse to print a number.
     *
     * <p>Forces red: dropping the weapon comparison; comparing with {@code ==} on the id.
     */
    @Test
    void aWeaponChangeMidSampleIsFlaggedRatherThanAveraged() {
        input(100);
        assertFalse(inputs().mixedWeapons(), "one weapon so far");
        assertEquals(BOW, inputs().weaponId());

        at(104);
        cadence.record(player, RIGHT, FireCadence.Kind.INPUT, "quiver_stone");

        assertTrue(inputs().mixedWeapons(),
                "the sample now spans two weapons and must not be printed as a reading");
    }

    /**
     * THE FOUR VERDICT ARMS, AND THE IMPOSSIBLE ONE IS ANSWERED FIRST.
     *
     * <p>They compare COUNTS, not means: an exact integer comparison with no epsilon. If every input
     * became a fire, nothing gated.
     *
     * <p><b>{@code INSTRUMENT_FAULT} is the control.</b> An instrument with no reading that would
     * indict it cannot tell you when it is broken, and this slice is priced on trusting one number
     * nobody has ever taken. <b>{@code NO_REPEAT} is not a failure either</b> — it is the answer that
     * held-repeat does not work on that material, which would make slice B's ruling 1
     * unimplementable as stated.
     *
     * <p>Forces red: collapsing {@code INSTRUMENT_FAULT} into {@code COOLDOWN_LIMITED}; ordering the
     * arms so a fault is reported as a measurement; treating one input as {@code INPUT_LIMITED}.
     */
    @Test
    void theVerdictHasFourArmsAndTheImpossibleOneIsCheckedBeforeTheOthers() {
        assertEquals(FireCadence.Verdict.INPUT_LIMITED, verdictOf(47, 47),
                "every input became a fire, so nothing gated and the input repeat IS the limiter");
        assertEquals(FireCadence.Verdict.COOLDOWN_LIMITED, verdictOf(47, 17),
                "fewer fires than inputs -- something gated them, and Q7's answer is still the "
                        + "INPUT number");
        assertEquals(FireCadence.Verdict.NO_REPEAT, verdictOf(1, 1),
                "one input and nothing after it: the client is not re-sending, which is a READING "
                        + "and not a fault");
        assertEquals(FireCadence.Verdict.NO_REPEAT, verdictOf(1, 0));

        assertEquals(FireCadence.Verdict.INSTRUMENT_FAULT, verdictOf(17, 47),
                "a gate cannot fire more often than it is asked to");
        assertEquals(FireCadence.Verdict.INSTRUMENT_FAULT, verdictOf(1, 2),
                "AND THE FAULT IS ANSWERED BEFORE NO_REPEAT -- one input with two fires is "
                        + "impossible, and reporting it as a reading would hide a broken counter");
    }

    private static FireCadence.Verdict verdictOf(int inputCount, int fireCount) {
        return FireCadence.verdict(countOnly(inputCount), countOnly(fireCount));
    }

    /** A sample with only its count set -- the verdict reads nothing else, deliberately. */
    private static FireCadence.Sample countOnly(int count) {
        return new FireCadence.Sample(count, 0L, java.util.OptionalDouble.empty(),
                java.util.OptionalLong.empty(), BOW, false);
    }

    /** Absent samples never reach the verdict: the caller has both or prints neither. */
    @Test
    void anAbsentSampleNeverReachesTheVerdict() {
        Optional<FireCadence.Sample> none = cadence.sample(player, RIGHT, FireCadence.Kind.FIRE);
        assertTrue(none.isEmpty(),
                "the command must check presence and say so in words -- there is no verdict over a "
                        + "measurement that did not happen");
    }
}
