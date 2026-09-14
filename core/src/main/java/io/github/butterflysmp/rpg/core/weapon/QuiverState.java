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
 * {@code paper}'s {@code Quivers.resolveForShot} (then named {@code refusalFor}), which takes a {@code Player} and therefore <b>can
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
 * <h2>WHY OPTIONALS IN ONE TYPE RATHER THAN LOOSE PARAMETERS</h2>
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
 *
 * <h2>WHY THIS IS A FINAL CLASS AND NOT A RECORD, WHICH IT WAS UNTIL THE FOURTH INSTANCE</h2>
 *
 * <p>A public record's canonical constructor is public -- the JLS requires it to be at least as
 * accessible as the record, and {@code paper} needs this type public. So while this was a record, a
 * sixth file could write
 *
 * <pre>{@code new QuiverState(loaded, weapon.quiverSize(), from, to)}</pre>
 *
 * and obtain a state whose capacity <b>ignores the stamp</b>: no {@code capacityIn}, no
 * {@code capacityOf(}, no {@code QuiverState.from(} for the source scan to see, and no new public
 * static method for the surface pin to see. Both guards green. And it is not hypothetical -- it is
 * the route {@code Quivers.stateOf} itself used until the resolution moved in here.
 *
 * <p><b>Four times the named routes were closed and the next defect arrived through an unnamed
 * one</b> ({@code capacityOf} -> {@code capacityIn} -> the core module -> {@code QuiverState.from}).
 * The set of ways to obtain a capacity is not enumerable by grep; a scan can only ever list the
 * doors somebody already thought of. So the constructor is PRIVATE and the factories are the only
 * doors -- the same move as removing {@code reloadTicks} from {@code reloadComplete} and the capacity
 * parameter from {@code reloadVerdict}, where a defect stopped being a rule and became a thing that
 * cannot be expressed.
 *
 * <p><b>What this does not close, and must not:</b> {@code from(..., OptionalInt.empty(), authored,
 * ...)} is still callable, and still SHOULD be -- the unstamped fallback is a real case with three
 * real readers. A caller that passes empty where it had a stamp is passing a legitimate value
 * wrongly, and no type can prevent that. It is boot-only, and {@code PLAN-quiver-a2.md}'s commit
 * table carries the row.
 *
 * <h2>WHAT THE CONVERSION COST -- BOTH ITEMS, BECAUSE THE FIRST DRAFT LISTED ONE</h2>
 *
 * <p><b>1 &middot; Equality is now IDENTITY, and this is a WARNING and not a reassurance.</b> The
 * generated {@code equals}/{@code hashCode}/{@code toString} are gone with the record, so
 * {@code Object}'s are what remain:
 *
 * <pre>{@code assertEquals(QuiverState.loaded(9, 11), state)   // compares REFERENCES. Fails.}</pre>
 *
 * even when all four fields match. Measured, both modules: nothing compares two states -- no
 * {@code Set}, no {@code Map} key, and every {@code assertEquals} touching this type compares an
 * enum verdict or an {@code int}, never a state. So no caller is broken today. <b>The earlier
 * wording said states "are compared by the VERDICT they yield, never by identity", which reads as
 * an assurance that identity comparison does not happen -- when identity is now the only comparison
 * available.</b> Nobody compares them; if somebody starts, the compiler will not say so.
 *
 * <p>Restoring value equality is a real option and it has a price this repo has not paid before:
 * there are <b>zero</b> hand-written {@code equals(Object)} in {@code core} and {@code paper} main
 * source, because value semantics here have always come from {@code record}. A hand-written one can
 * silently omit a field; a generated one cannot. So if it is restored, the field list it covers
 * needs pinning in the same breath -- and {@code QuiversSignatureTest}'s instance-surface pin will
 * fail the moment {@code equals} is declared, which lands the author here.
 *
 * <p><b>2 &middot; Three public accessors went with it:</b> {@code loaded()},
 * {@code reloadStartedAt()} and {@code reloadCompletesAt()}. Measured: <b>zero callers anywhere in
 * either module</b>, so nothing broke -- and a narrower surface is an IMPROVEMENT, not a loss, since
 * a caller holding the raw stamp and the authored value separately could re-do the resolution
 * outside {@link #capacityOf}. It is named because a cost list that is short by one tells a
 * reader the conversion did less than it did. {@link #capacity} is the one accessor that survives,
 * and it returns the RESOLVED value, which is the only one anybody should be reading.
 *
 * <p><b>AND ONE THING THAT IS NOT A COST, BECAUSE IT WAS DRAFTED AS THE THIRD.</b> The draft said
 * the conversion lost a canonical argument ordering, leaving {@code reloadStartedAt} and
 * {@code reloadCompletesAt} adjacent and both {@code OptionalLong} -- the transposable-pair shape
 * {@code DamageSignatureTest} exists to forbid, cited two sections above. <b>That hazard is
 * unchanged.</b> The record's canonical constructor took the same four arguments in the same order,
 * and the same four factory bodies ({@code from}, {@code loaded}, {@code reloading},
 * {@code unstamped}) called it. Nothing about the ordering moved, so it is not a cost of this
 * change -- it is a standing property, held by the constructor's both-or-neither check and
 * {@code QuiverStateTest}'s deadline rows rather than by the type system. Recorded because a cost
 * list that pads is exactly as misleading as one that is short.
 */
public final class QuiverState {

    private final OptionalInt loaded;
    private final int capacity;
    private final OptionalLong reloadStartedAt;
    private final OptionalLong reloadCompletesAt;

    /**
     * PRIVATE, AND THAT IS THE WHOLE POINT -- see the class javadoc. This was a record until the
     * fourth instance of the same defect; a public record's canonical constructor is public, and
     * {@code new QuiverState(loaded, weapon.quiverSize(), from, to)} fabricated a capacity that
     * ignored the stamp while passing every guard. The factories below are now the only doors.
     */
    private QuiverState(OptionalInt loaded, int capacity, OptionalLong reloadStartedAt,
                        OptionalLong reloadCompletesAt) {
        if (reloadStartedAt.isPresent() != reloadCompletesAt.isPresent()) {
            throw new IllegalArgumentException(
                    "a reload's start and deadline are two halves of one fact: both or neither");
        }
        this.loaded = loaded;
        this.capacity = capacity;
        this.reloadStartedAt = reloadStartedAt;
        this.reloadCompletesAt = reloadCompletesAt;
    }

    /** The capacity this state resolved to -- the stamp when present, the authored value otherwise. */
    public int capacity() {
        return capacity;
    }

    /**
     * The capacity that actually governs, given what an item carries and what its weapon declares.
     *
     * <h2>THE THREE SOURCES ARE ORDERED, NOT COMPETING</h2>
     *
     * <p>Once capacity is a stat the number has three origins, answering three different questions.
     * <b>This method is the whole of the ordering between the last two</b>, so the fallback happens
     * in exactly one place rather than once per reader:
     *
     * <ul>
     *   <li><b>the stat</b> — <i>what does this wielder resolve?</i> Read <b>only at a write</b>, by
     *       {@code QuiverItems.setLoaded}, and never here.
     *   <li><b>the stamp</b> — <i>what does this item hold, and what does it enforce?</i> Everywhere
     *       else: the tooltip AND the refusal logic. That single readership is the point — a tooltip
     *       rendering the stamp while the refusal resolved the holder LIVE would lie by a new
     *       mechanism, which is the defect {@code ResourceCost} records as <i>"two literals cannot
     *       drift apart if there is only one."</i>
     *   <li><b>the authored value</b> — <i>what does this weapon hold when there is NO ITEM at
     *       all?</i> The fallback below.
     * </ul>
     *
     * <p><b>AN UNSTAMPED CAPACITY IS NOT A DEFECT, UNLIKE AN UNSTAMPED COUNT</b>, and the asymmetry
     * is deliberate. A count has no item-free meaning, so its absence means a mint path failed and
     * {@link Fire#UNSTAMPED} reports it. A capacity has a perfectly good item-free answer: the
     * weapon's own. A definitions-only renderer has no item to read — {@code GoldenLoreTest}, a
     * recipe-browser icon and a craft preview all describe a WEAPON rather than a held item — and the
     * authored number is true for all three. It is also what an item minted before this stamp existed
     * carries, so this doubles as the migration path.
     *
     * <p><b>It lives here and not on {@link Quiver} because {@code QuiverSignatureTest} refused it
     * there</b>, and the guard was right: {@code Quiver} is primitives-only arithmetic that must
     * never learn a capacity, and an {@code OptionalInt} argument is composition. That is the
     * distinction between the two classes, enforced rather than remembered.
     *
     * <p>Worked: {@code (of(11), 9) -> 11} (gear resolved 11 when it was last packed);
     * {@code (of(9), 9) -> 9}; {@code (empty, 9) -> 9} (no item, or an item from before the stamp).
     */
    public static int capacityOf(OptionalInt stampedCapacity, int authoredCapacity) {
        return stampedCapacity.orElse(authoredCapacity);
    }

    /**
     * Build a state from exactly what an item carries, resolving the capacity on the way in.
     *
     * <h2>WHY THE RESOLUTION LIVES IN THE FACTORY AND NOT AT THE CALL SITE</h2>
     *
     * <p><b>Because a resolution performed in {@code paper} cannot be tested, and one performed here
     * can.</b> {@code Quivers.stateOf} took the five stored values and called {@link #capacityOf}
     * itself — and a mutation replacing that call's stamp with {@code OptionalInt.empty()}
     * ({@code MUTSTATEDROP}) passed the ENTIRE suite. {@code stateOf} needs an {@code ItemStack}, so
     * it has no unit test and never will; the guard over its call site is a source scan, which
     * proves no SIXTH resolver exists and nothing whatever about what the five do.
     *
     * <p><b>And the consequence was worse than the tooltip's.</b> This capacity feeds
     * {@link #reloadVerdict}'s already-full comparison. Under that mutation, once capacity is a stat:
     * a Ranger boosted to 11 fires down to 9, presses reload, and is told the magazine is FULL —
     * <b>the last two rounds permanently unreachable</b>, with the tooltip correctly reading 9/11
     * beside it. The tooltip right and the mechanic wrong is the same disagreement the stamp exists
     * to prevent, arriving from the other side.
     *
     * <p>So the decision moved here, where {@code QuiverStateTest} can stage a stamp that differs
     * from the authored value and observe the verdict. What remains in {@code paper} is reading five
     * values off an item and passing them — plumbing with no decision in it, and the only part a
     * boot must still witness.
     */
    public static QuiverState from(OptionalInt loaded, OptionalInt stampedCapacity,
                                   int authoredCapacity, OptionalLong reloadStartedAt,
                                   OptionalLong reloadCompletesAt) {
        return new QuiverState(loaded, capacityOf(stampedCapacity, authoredCapacity),
                reloadStartedAt, reloadCompletesAt);
    }

    /** An item carrying a count and no reload -- the ordinary state of a quiver weapon. */
    public static QuiverState loaded(int rounds, int capacity) {
        return new QuiverState(OptionalInt.of(rounds), capacity, OptionalLong.empty(), OptionalLong.empty());
    }

    /** An item mid-reload, carrying whatever count it had when the reload began. */
    public static QuiverState reloading(int rounds, int capacity, long startedAt, long completesAt) {
        return new QuiverState(OptionalInt.of(rounds), capacity, OptionalLong.of(startedAt),
                OptionalLong.of(completesAt));
    }

    /** An item that carries no count at all: never stamped. A defect, not an empty magazine. */
    public static QuiverState unstamped(int capacity) {
        return new QuiverState(OptionalInt.empty(), capacity, OptionalLong.empty(), OptionalLong.empty());
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
     * How many rounds a reload of this quiver would have to supply — <b>the arrows it would cost.</b>
     *
     * <p>A method here rather than a {@code loaded()} accessor plus arithmetic at the call site, and
     * that is deliberate: the record conversion REMOVED {@code loaded()} on purpose, and this class's
     * javadoc records that the narrower surface is an improvement because <i>a caller holding the raw
     * stamp and the authored value separately could re-do the resolution outside
     * {@link #capacityOf}</i>. Re-adding the accessor to let {@code paper} do the subtraction would
     * walk straight back through that door.
     *
     * <p><b>Zero when unstamped</b>, which is the only honest answer: an item with no count has no
     * gap to fill, and {@link #reloadVerdict} refuses it on the {@link Reload#UNSTAMPED} rung before
     * the ammo rung is ever reached. A non-zero answer here would make the caller buy arrows for a
     * reload that is about to be refused.
     */
    public int roundsNeeded() {
        return loaded.isEmpty() ? 0 : Quiver.roundsNeeded(loaded.getAsInt(), capacity);
    }

    /**
     * How many rounds this magazine has, clamped to its resolved capacity.
     *
     * <p>Written for the Dragon's Plume's R3 -- <i>the charge tracker is capped at rounds remaining,
     * and the sound never promises an arrow that is not coming</i> -- and it is a method here rather
     * than {@code capacity() - roundsNeeded()} at the call site for two separate reasons. That
     * subtraction is the one {@link #roundsNeeded}'s javadoc names and refuses; and it is
     * <b>WRONG ON AN UNSTAMPED ITEM</b>, where {@code roundsNeeded()} returns 0 and the subtraction
     * therefore reports a FULL MAGAZINE for an item with no count at all.
     *
     * <h2>UNSTAMPED IS ZERO, AND THE REASON IS ITS OWN RATHER THAN INHERITED</h2>
     *
     * <p>{@code roundsNeeded()} returns 0 when unstamped <i>because {@link #reloadVerdict} refuses
     * such an item on the {@link Reload#UNSTAMPED} rung before the ammo rung is reached</i>. That
     * reason belongs to that method and does not transfer, so this one states its own: <b>an item
     * with no count has no rounds</b>, an unstamped stamp is a DEFECT and not an empty magazine
     * (see the class javadoc), and R3's cap must promise NOTHING rather than something.
     *
     * <p><b>Zero collides by VALUE with a genuinely empty magazine, and that is not a loss of
     * information</b>: {@link #fireVerdict} separates them by name -- {@link Fire#UNSTAMPED} against
     * {@link Fire#EMPTY} -- so a caller that must tell them apart has a method that does, and a
     * caller that only needs the cap gets the same correct answer from both.
     *
     * <h2>IT CLAMPS, AND THE ONE-ROUND GAP THAT BUYS IS KNOWN AND CHOSEN</h2>
     *
     * <p>An OVER-FULL magazine is representable: a stamp of 11 read against a capacity that now
     * resolves to 8, in the window after a capacity modifier comes off and before the next write
     * re-clamps it. <b>This returns 8.</b>
     *
     * <p><b>MEASURED, AND IT IS THE REASON: SUCH A MAGAZINE PAYS FOR NINE, NOT ELEVEN.</b>
     * {@code Quivers.spendRound} is {@code Quiver.spend(11) = 10} handed to
     * {@code QuiverItems.setLoaded}, which clamps the value it WRITES -- {@code clamp(10, 8) = 8}.
     * So the surplus is lost at the FIRST shot rather than carried: one over-full shot, then a
     * magazine of 8.
     *
     * <pre>
     * unclamped  11   promises two arrows that are not coming   -- the dishonesty R3 exists to stop
     * measured    9   one over-full shot, then the clamp        -- exact, but it projects future
     *                                                              WRITES rather than reading state
     * CHOSEN      8   under-promises by one, never over         -- and never couples to the write path
     * </pre>
     *
     * <p><b>SO THIS ACCESSOR SAYS 8 WHERE THE MAGAZINE YIELDS 9, ON PURPOSE.</b> It is not an
     * off-by-one and it must not be "fixed": closing that gap means modelling what the write funnel
     * will do next, which is exactly the coupling the clamped form avoids. The gap exists only in a
     * state that is <b>unreachable in play today</b> -- see {@code GATE-expanded-quiver.md} row A-5,
     * whose trigger is the first shipped item granting quiver size outside the dev instrument.
     *
     * <h2>AND THE CLAMP IS WHAT MAKES THE IDENTITY TRUE, WHICH IS WHAT ADMITS IT TO THE PINNED SET</h2>
     *
     * <p>For every STAMPED state, including the over-full one:
     * {@code roundsNeeded() + roundsRemaining() == capacity()}. Over-full works only because of the
     * clamp -- {@code 0 + 8} -- and unclamped it would be {@code 0 + 11}, which recovers a number
     * that is not the capacity and is not anything else either. <b>That identity is the argument
     * {@code QuiversSignatureTest} admits this method on:</b> the pair it recovers is already
     * public. Anyone tempted to unclamp has to answer that sentence first.
     *
     * <p><b>Unstamped is the one exception and it fails SAFE:</b> {@code 0 + 0} recovers 0 rather
     * than the capacity -- less than the public surface already gives, never more.
     */
    public int roundsRemaining() {
        return loaded.isEmpty() ? 0 : Quiver.clamp(loaded.getAsInt(), capacity);
    }

    /**
     * What pressing reload should do.
     *
     * <p>{@code ALREADY_FULL} is what stops a player who pressed reload out of habit from paying
     * three dead seconds for nothing, and {@code ALREADY_RELOADING} is what stops a HELD input from
     * restarting the timer twenty times a second -- which would push the deadline further away on
     * every packet and leave a weapon that never comes back, with nothing reporting a problem.
     *
     * <p><b>IT TAKES NO CAPACITY, AND THAT IS THE POINT.</b> The capacity is this class's own
     * component, resolved once by {@link #capacityOf} at the single site that builds a state. With
     * nowhere to pass a different one, <b>a verdict that disagrees with the tooltip is
     * unrepresentable</b> rather than merely forbidden -- the same move that removed
     * {@code reloadTicks} from {@link Quiver#reloadComplete} and made A1's free-instant-reload
     * defect impossible to express.
     *
     * <h2>WHERE THE AMMO RUNG SITS, AND THE POSITION IS THE WHOLE DECISION</h2>
     *
     * <p><b>AFTER the full/not-full test</b>, so a FULL magazine with no arrows still says
     * {@link Reload#ALREADY_FULL} -- it is full, and nothing was going to be spent.
     * <b>BEFORE {@link Reload#BEGIN}</b>, so a 7/8 magazine with no arrows is refused rather than
     * starting a reload that would load nothing.
     *
     * <p>Above the full test, "already full" would become unreachable for anyone out of arrows and a
     * player at 8/8 would be told to go and find ammo. Below {@code BEGIN} it would never be
     * consulted at all. <b>There is exactly one position that is right, and this is it</b> --
     * measured, not asserted: {@code MUT-ORDER} moves the rung above the full test and reddens
     * {@code QuiverAmmoTest.aFULLMagazineWithNoArrowsIsSTILLAlreadyFull}.
     *
     * <p><b>A running reload never re-examines the ammo</b>, because the
     * {@code reloadStartedAt} rung is consulted first. Ruling 3 took the arrows at the START, so a
     * player who runs dry mid-reload keeps the reload they paid for.
     *
     * @param now            the current tick
     * @param roundsAvailable how many rounds the player can actually supply, as decided by
     *                       {@code QuiverAmmo.supply} -- the READ is {@code paper}'s and the DECISION
     *                       is this method's. <b>In creative this is whatever the reload needs</b>,
     *                       so the rung is satisfied and this method never learns that creative
     *                       exists.
     */
    public Reload reloadVerdict(long now, int roundsAvailable) {
        if (loaded.isEmpty()) return Reload.UNSTAMPED;
        if (reloadStartedAt.isPresent()) {
            return isReloading(now) ? Reload.ALREADY_RELOADING : Reload.RELOAD_MATURED;
        }
        if (loaded.getAsInt() >= Quiver.clamp(capacity, capacity)) return Reload.ALREADY_FULL;
        if (roundsAvailable <= 0) return Reload.NO_AMMO;
        return Reload.BEGIN;
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
        /**
         * There is ROOM but no ARROWS. Distinct from {@link #ALREADY_FULL}, and the distinction is
         * the player-facing point of this constant rather than an internal nicety.
         *
         * <p><b>A refusal that reuses another refusal's message is a bug report waiting to be
         * filed.</b> "Already full" on a weapon showing 7/8 is exactly the kind of thing a player
         * screenshots -- so this has its own text and its own throttle key in {@code QuiverNotice},
         * never a reuse of the empty-magazine one. Two refusals sharing a throttle silence each
         * other, which only shows up when a player hits both in one fight.
         */
        NO_AMMO,
        /** No count at all. The caller warns and repairs, as for {@link Fire#UNSTAMPED}. */
        UNSTAMPED
    }
}
