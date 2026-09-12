package io.github.butterflysmp.rpg.core.combat;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * How often a held input actually arrives, and how often it becomes a shot.
 *
 * <h2>WHY THIS EXISTS: A ROW DEFERRED TWICE BECAUSE NOBODY BUILT ITS INSTRUMENT</h2>
 *
 * <p><b>Q7 — the held-right-click repeat interval — has been owed since slice A1 and deferred
 * twice.</b> It is the one row in two gates with <b>no green state</b>: it produces a number that
 * exists nowhere else in this project, so "green" cannot select an outcome.
 *
 * <p>Its recipe asked an operator to hold a button for a timed ten seconds, <b>count the shots by
 * hand</b>, and divide. <b>A row that needs hand-counting is un-runnable, and an un-runnable row
 * gets deferred</b> — which is what happened, twice. This class is the missing instrument, and it is
 * built before the reading rather than after it.
 *
 * <h2>TWO COUNTERS, BECAUSE ONE NUMBER CANNOT SAY WHICH MECHANISM PRODUCED IT</h2>
 *
 * <p>A fires-only reading reports {@code min(input rate, cooldown)}. A reading of 15 on
 * {@code hunters_bow} is indistinguishable between <i>the input floor is 15</i> and <i>the cooldown
 * is 15 and the floor is lower</i> — two different facts with different consequences for slice C's
 * 7-tick dual-wield halving. <b>The remedy for one observation that cannot separate two mechanisms
 * is a second observation</b>, which is the same answer A2's gate reached for its two tooltip
 * mutations.
 *
 * <p>So {@link Kind#INPUT} counts every click that reached a bound weapon, before any gate, and
 * {@link Kind#FIRE} counts every one that became a shot. {@link #verdict} states their relation.
 *
 * <h2>NO CONTENT FILE IS EDITED TO TAKE THIS READING, AND THAT IS THE POINT</h2>
 *
 * <p>Q7's recipe also said to set {@code cooldown_ticks} to 1 and {@code quiver_size} to 60 so the
 * cooldown could not be the limiter. <b>Struck.</b> {@code quiver_stone}'s numbers were chosen by an
 * exhaustive collision sweep and eight other gate rows read bare values that depend on it; mutating
 * a swept fixture to take one reading spends a guarantee those rows stand on.
 *
 * <p><b>Counting inputs BEFORE the gate makes both edits unnecessary</b>, not just the cooldown one:
 * a magazine cannot limit an input either.
 *
 * <p><b>ONE CLAUSE OF THAT RECIPE SURVIVES, AND IT IS A REQUIREMENT ON WHERE THE COUNTER SITS:</b>
 * <i>run it on a CANCELLED BINDING, because that is the configuration that ships.</i> Held-use
 * cadence is not independent of whether the interaction is cancelled, so a reading taken on an
 * uncancelled binding is a number about a configuration nothing ships. The call site satisfies it by
 * construction — see {@code WeaponFire.attempt}, which records the exception and proves it is the
 * only one.
 */
public final class FireCadence {

    /** What is being counted. Kept apart because their RELATION is the reading. */
    public enum Kind {
        /** A click that reached a weapon binding this input — before durability, magazine, cooldown. */
        INPUT,
        /** A click that became a shot. */
        FIRE
    }

    /**
     * What the pair of samples means together. <b>Four arms, each a different FACT.</b>
     *
     * <p>The arms compare COUNTS, not means: if every input became a fire, nothing gated; if fewer
     * fires than inputs, something did. That is an exact integer comparison with no epsilon, and it
     * cannot drift the way a float equality would.
     */
    public enum Verdict {
        /**
         * Inputs and fires are equal. <b>Nothing gated, so the input repeat IS the limiter, and its
         * interval IS Q7's answer.</b>
         */
        INPUT_LIMITED,

        /**
         * Fewer fires than inputs — something gated them. <b>Q7's answer is still the INPUT number</b>;
         * the gap is the quantity slice C needs before pricing a halving.
         *
         * <p><b>THE NAME SAYS "COOLDOWN" AND THREE GATES CAN PRODUCE IT.</b> Measured in
         * {@code WeaponFire.attempt}'s own order: durability ({@code Broken}, :90), the magazine
         * ({@code Empty}/{@code Reloading}, :106-116) and the cooldown ({@code AbilityService:203}).
         * <b>Only a weapon with no magazine and undamaged durability isolates the cooldown</b> —
         * {@code hunters_bow} has no {@code quiver_size}, which is why the gate row reads it there
         * first and {@code quiver_stone} second.
         */
        COOLDOWN_LIMITED,

        /**
         * One input and then nothing while the button was held. <b>The client is not re-sending.</b>
         *
         * <p><b>NOT AN INSTRUMENT FAULT — the instrument worked and this is the answer.</b> An item
         * with a use action may latch into a use state instead of repeating, and the Boltor's
         * material is exactly such an item. If this arm is what the reading returns, <b>held-repeat
         * does not work on that material and slice B's ruling 1 is unimplementable as stated</b>,
         * which needs re-ruling before the slice proceeds. That would be the most valuable thing Q7
         * could produce, so it is a named outcome rather than a surprise in prose.
         */
        NO_REPEAT,

        /**
         * More fires than inputs. <b>Impossible: a gate cannot fire more often than it is asked
         * to.</b>
         *
         * <p><b>This arm is the CONTROL, and it is why the pair is worth more than the sum.</b> An
         * instrument with no reading that would indict it cannot tell you when it is broken — and
         * this slice is priced on trusting one number nobody has ever taken. Print these words, not
         * a verdict.
         */
        INSTRUMENT_FAULT
    }

    /**
     * One finished reading. <b>Absence is modelled, never rendered as zero.</b>
     *
     * <p>{@code count == 1} is a timestamp, not a cadence: there is no interval between one event
     * and nothing. Both interval figures are therefore {@code Optional}, so a caller cannot print a
     * {@code 0} that looks like an answer.
     *
     * <h2>{@code sinceLastEventTicks} IS THE ONLY FIELD MEASURED TO <i>NOW</i>, AND THAT IS WHY IT
     * IS A SEPARATE FIELD RATHER THAN A CHANGE TO {@code windowTicks}</h2>
     *
     * <p>{@code windowTicks} is first event to last and must stay that way — {@code MUTWINDOW} in
     * {@code FireCadenceTest} exists to keep it there, and measured {@code 400} where {@code 8}
     * belonged when it was made to run to the current tick. <b>Folding the elapsed figure into the
     * window would swallow the dead time into the cadence.</b> So the two sit side by side with
     * opposite endpoints, deliberately.
     *
     * <p><b>WHAT IT CAN AND CANNOT ESTABLISH, BECAUSE THE ASYMMETRY IS THE WHOLE POINT.</b> It exists
     * for {@link Verdict#NO_REPEAT}, the one arm with no control: the instrument cannot see the
     * button, only events, so <i>"held ten seconds and it never repeated"</i> and <i>"clicked once
     * and ran the command"</i> are otherwise the same reading. This figure separates them in ONE
     * direction. It is elapsed time, not held time — the operator may have released the button and
     * waited — so:
     *
     * <ul>
     *   <li><b>A small figure DISQUALIFIES the reading.</b> A handful of ticks means no repeat ever
     *       had the chance to arrive, and {@code NO_REPEAT} says nothing about the material.</li>
     *   <li><b>A large figure does NOT validate it.</b> ~200t is consistent with a ten-second hold
     *       and equally with a tap followed by a pause.</li>
     * </ul>
     *
     * <p>So it is a <b>necessary-not-sufficient</b> condition, and it makes the mis-take VISIBLE
     * rather than proving the take was good. That is what lets {@code GATE-q7.md}'s staging rule be
     * a checkable condition instead of an instruction nobody can verify was followed.
     *
     * @param count            events recorded
     * @param windowTicks      FIRST counted event to LAST — never "now minus first", which would
     *                         include the dead time before the operator started holding
     * @param meanIntervalTicks {@code windowTicks / (count - 1)}; the SUSTAINED rate
     * @param minIntervalTicks  the smallest gap observed; <b>the FLOOR</b>, which is what Q7 is named
     *                          for and what prices slice C's halving
     * @param sinceLastEventTicks LAST counted event to <b>now</b> — the reading moment. Absent only
     *                          on a synthetic zero-count sample, where there is no last event
     * @param weaponId          the weapon the sample was taken on
     * @param mixedWeapons      true if the weapon changed mid-sample — <b>then it is not a reading</b>
     */
    public record Sample(int count, long windowTicks, OptionalDouble meanIntervalTicks,
                         OptionalLong minIntervalTicks, OptionalLong sinceLastEventTicks,
                         String weaponId, boolean mixedWeapons) {

        /** Gaps between consecutive events: one fewer than the events themselves. */
        public int intervals() {
            return Math.max(0, count - 1);
        }
    }

    private record Key(String input, Kind kind) {}

    /**
     * Live state for one {@link Key}. Mutated only on the player's own thread — every call site is
     * inside {@code WeaponFire.attempt}, which runs pre-hop — and read by the command on whatever
     * thread it arrives on. The outer maps are concurrent; these fields are written once per event.
     */
    private static final class Running {
        private int count;
        private long firstTick;
        private long lastTick;
        private long minInterval = Long.MAX_VALUE;
        private String weaponId;
        private boolean mixedWeapons;
    }

    private final LongSupplier currentTick;
    private final Map<UUID, Map<Key, Running>> samples = new ConcurrentHashMap<>();

    /**
     * @param currentTick the server tick, injected so tests drive a fake clock — the same shape
     *                    {@code CooldownTracker} and {@code DamageWindow} use, and the reason this
     *                    class is in {@code core} where a decision can be unit-tested at all
     */
    public FireCadence(LongSupplier currentTick) {
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
    }

    /**
     * Stamp one event.
     *
     * <p>The interval recorded is the gap from the PREVIOUS event of the same kind, so the first
     * event of a sample contributes a timestamp and no interval.
     */
    public void record(UUID player, String input, Kind kind, String weaponId) {
        Map<Key, Running> perKey = samples.computeIfAbsent(player, id -> new ConcurrentHashMap<>());
        Running running = perKey.computeIfAbsent(new Key(input, kind), k -> new Running());
        long now = currentTick.getAsLong();

        if (running.count == 0) {
            running.firstTick = now;
            running.weaponId = weaponId;
        } else {
            long gap = now - running.lastTick;
            if (gap < running.minInterval) running.minInterval = gap;
            if (!Objects.equals(running.weaponId, weaponId)) running.mixedWeapons = true;
        }
        running.lastTick = now;
        running.count++;
    }

    /** The reading for one player, input and kind, or empty when nothing was recorded. */
    public Optional<Sample> sample(UUID player, String input, Kind kind) {
        Map<Key, Running> perKey = samples.get(player);
        if (perKey == null) return Optional.empty();
        Running running = perKey.get(new Key(input, kind));
        if (running == null || running.count == 0) return Optional.empty();

        long window = running.lastTick - running.firstTick;
        boolean hasInterval = running.count > 1;
        // TO THE LAST EVENT for the window, TO NOW for the elapsed figure. Two endpoints, one
        // expression each, so neither can be quietly rewritten into the other.
        long sinceLast = currentTick.getAsLong() - running.lastTick;
        return Optional.of(new Sample(
                running.count,
                window,
                hasInterval ? OptionalDouble.of((double) window / (running.count - 1))
                            : OptionalDouble.empty(),
                hasInterval ? OptionalLong.of(running.minInterval) : OptionalLong.empty(),
                OptionalLong.of(sinceLast),
                running.weaponId,
                running.mixedWeapons));
    }

    /** Drop everything recorded for a player — the command's "print and clear". */
    public void clear(UUID player) {
        samples.remove(player);
    }

    /** Players holding a sample. Bounds check for tests. */
    public int trackedPlayers() {
        return samples.size();
    }

    /**
     * What the pair means together. See {@link Verdict} — the arms are ordered so the impossible one
     * is answered first, because a broken instrument must not be reported as a measurement.
     */
    public static Verdict verdict(Sample inputs, Sample fires) {
        if (fires.count() > inputs.count()) return Verdict.INSTRUMENT_FAULT;
        if (inputs.count() <= 1) return Verdict.NO_REPEAT;
        if (fires.count() == inputs.count()) return Verdict.INPUT_LIMITED;
        return Verdict.COOLDOWN_LIMITED;
    }
}
