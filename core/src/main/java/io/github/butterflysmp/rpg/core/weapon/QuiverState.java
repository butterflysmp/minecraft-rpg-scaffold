package io.github.butterflysmp.rpg.core.weapon;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * What a quiver's stored values MEAN together -- the verdicts, composed from {@link Quiver}'s
 * arithmetic.
 *
 * <h2>WHY THIS CLASS EXISTS, AND IT IS A CORRECTION</h2>
 *
 * <p>{@link Quiver} holds the arithmetic and was unit-tested from the start. The COMPOSITION of that
 * arithmetic into an answer -- <i>may this weapon fire right now?</i> -- first landed in
 * {@code paper}'s {@code Quivers.refusalFor}, which takes a {@code Player} and therefore <b>can
 * never be unit-tested in this project</b>: {@code new ItemStack(...)} throws without a running
 * server and there is no MockBukkit. The decision of the whole slice sat in the one layer no test
 * can reach.
 *
 * <p>The tell was a suite total that did not move. Commit 3 added 303 lines of production code in
 * two new classes and <b>zero test rows</b>, and the figure was re-read and quoted as routine. Re-
 * reading it after the last file lands is the rule; noticing when it has NOT moved is what the rule
 * is for.
 *
 * <p>The split this repository already argues for is {@code Durability} / {@code WeaponDurability}:
 * <i>"every DECISION is Durability, in core, where it is unit-tested; this class only moves values
 * in and out of item meta ... every line of arithmetic that lives here is a line no test can
 * reach."</i> <b>The verdict is core's; only the READ and the DELIVERY are paper's.</b>
 *
 * <h2>WHY A RECORD OF OPTIONALS RATHER THAN LOOSE PARAMETERS</h2>
 *
 * <p>The three stored values have exactly one legal shape between them, and a parameter list cannot
 * say so. {@code loaded} may be absent -- an item never stamped, which is a DEFECT and not an empty
 * magazine. The two reload ticks are present together or not at all, because the start is the
 * restart guard's bound and a deadline without one cannot be checked. Modelling that here means a
 * caller cannot express a half-reload by accident.
 *
 * <p>It also avoids the shape {@code DamageSignatureTest} exists to forbid: the loose form of this
 * would carry {@code boolean hasCount, boolean reloading} adjacent in one list -- two same-typed
 * flags the compiler cannot tell apart, transposable, and every ordering compiling.
 *
 * <p>{@link Quiver} keeps its primitives-only contract ({@code QuiverSignatureTest}) and knows
 * nothing about this type. The dependency runs one way: this composes {@code Quiver}, never the
 * reverse.
 */
public record QuiverState(OptionalInt loaded, OptionalLong reloadStartedAt,
                          OptionalLong reloadCompletesAt) {

    public QuiverState {
        if (reloadStartedAt.isPresent() != reloadCompletesAt.isPresent()) {
            throw new IllegalArgumentException(
                    "a reload's start and deadline are two halves of one fact: both or neither");
        }
    }

    /** An item carrying a count and no reload -- the ordinary state of a quiver weapon. */
    public static QuiverState loaded(int rounds) {
        return new QuiverState(OptionalInt.of(rounds), OptionalLong.empty(), OptionalLong.empty());
    }

    /** An item mid-reload, carrying whatever count it had when the reload began. */
    public static QuiverState reloading(int rounds, long startedAt, long completesAt) {
        return new QuiverState(OptionalInt.of(rounds), OptionalLong.of(startedAt),
                OptionalLong.of(completesAt));
    }

    /** An item that carries no count at all: never stamped. A defect, not an empty magazine. */
    public static QuiverState unstamped() {
        return new QuiverState(OptionalInt.empty(), OptionalLong.empty(), OptionalLong.empty());
    }

    /** Whether a reload is recorded AND still running at {@code now}. */
    public boolean isReloading(long now) {
        return reloadStartedAt.isPresent()
                && !Quiver.reloadComplete(now, reloadStartedAt.getAsLong(),
                                          reloadCompletesAt.getAsLong());
    }

    /** Ticks left on a running reload, or 0 when none is running. */
    public long reloadTicksRemaining(long now) {
        if (reloadStartedAt.isEmpty()) return 0L;
        return Quiver.reloadTicksRemaining(now, reloadStartedAt.getAsLong(),
                                           reloadCompletesAt.getAsLong());
    }

    /**
     * May this weapon fire right now, and if not, why not?
     *
     * <p><b>THE ORDER OF THESE FOUR CHECKS IS THE DECISION, and every one of them was previously
     * expressed only in a method no test could call.</b>
     *
     * <ol>
     *   <li><b>Unstamped first.</b> An item with no count was never stamped by a mint path -- a
     *       defect. It must be distinguishable from an empty magazine, or a forgotten stamp becomes
     *       a weapon that silently never fires with a message that reads perfectly reasonable.
     *   <li><b>A MATURED reload beats EMPTY, and this is the subtle one.</b> An item mid-reload
     *       still carries the count it had when the reload began, which is usually 0. If the empty
     *       check ran first, the shot that finishes a reload would be refused as empty and the
     *       magazine would refill only on the NEXT press -- a dropped shot that feels like input
     *       lag and is invisible to any test of the arithmetic alone.
     *   <li><b>A RUNNING reload beats EMPTY too</b>, for the player-facing reason: "reloading, 1.4s"
     *       tells them what is happening; "empty -- left-click to reload" during a reload they
     *       already started is actively wrong.
     *   <li><b>Then empty.</b>
     * </ol>
     */
    public Fire fireVerdict(long now) {
        if (loaded.isEmpty()) return Fire.UNSTAMPED;
        if (reloadStartedAt.isPresent()) {
            return isReloading(now) ? Fire.RELOADING : Fire.RELOAD_MATURED;
        }
        return Quiver.isEmpty(loaded.getAsInt()) ? Fire.EMPTY : Fire.FIRE;
    }

    /**
     * What pressing reload should do.
     *
     * <p>{@code ALREADY_FULL} is what stops a player who pressed reload out of habit from paying
     * three dead seconds for nothing, and {@code ALREADY_RELOADING} is what stops a HELD input from
     * restarting the timer twenty times a second -- which would push the deadline further away on
     * every packet and leave a weapon that never comes back, with nothing reporting a problem.
     *
     * @param capacity the wielder's resolved capacity -- a PARAMETER, on {@link Quiver}'s rule, so
     *                 A2 can move it without reopening this class.
     */
    public Reload reloadVerdict(long now, int capacity) {
        if (loaded.isEmpty()) return Reload.UNSTAMPED;
        if (reloadStartedAt.isPresent()) {
            return isReloading(now) ? Reload.ALREADY_RELOADING : Reload.RELOAD_MATURED;
        }
        return loaded.getAsInt() >= Quiver.clamp(capacity, capacity)
                ? Reload.ALREADY_FULL
                : Reload.BEGIN;
    }

    /** Why a shot may or may not happen. */
    public enum Fire {
        /** Loaded. The shot goes through. */
        FIRE,
        /** Spent. The player is told to reload. */
        EMPTY,
        /** A reload is in flight; the caller reports the remaining time. */
        RELOADING,
        /**
         * A reload finished on this very read. The caller refills the item and the shot GOES
         * THROUGH -- the read is the tick, so the press that matures a reload is not wasted.
         */
        RELOAD_MATURED,
        /** No count at all: a mint path failed to stamp one. The caller warns and repairs. */
        UNSTAMPED
    }

    /** What pressing reload does. */
    public enum Reload {
        /** Start one. */
        BEGIN,
        /** One is already running; do nothing and do not restart it. */
        ALREADY_RELOADING,
        /** One finished on this read; refill instead of starting another. */
        RELOAD_MATURED,
        /** The magazine is already full; do nothing. */
        ALREADY_FULL,
        /** No count at all. The caller warns and repairs, as for {@link Fire#UNSTAMPED}. */
        UNSTAMPED
    }
}
