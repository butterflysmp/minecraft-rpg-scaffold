package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.DotRates;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * A damage-over-time status that owns its own clock, caps its own damage, and credits its applier:
 * the store SCORCH and WITHER share.
 *
 * <h2>ONE STORE, CREATED TWICE -- AND THE SECOND INSTANCE IS THE NO-EXPLOSION GUARANTEE</h2>
 *
 * <p>WITHER-STATUS (the seat's WS1 ruling, 2026-09-30): <i>"one parameterised paper store, created
 * twice (scorch(), wither())"</i>. This was {@code ScorchStatus}, and everything below was written for
 * scorch; it is kept as the account because the clock, the refresh, the credit and the cap are exactly
 * what the two share. The parameters are a {@link DotRates} (the period, the per-tick rate, the cap
 * fraction, the window) and the task's name. {@code AdapterContext} holds the two instances.
 *
 * <p><b>Two instances is what keeps a withered mob from exploding (WS2).</b> Ignite asks
 * {@code scorch().isScorched}, and a wither application only ever touches {@code wither()}. Handing one
 * instance to both would make every withered death an Ignite -- which looks exactly like the Ignite
 * ruling working. {@code DotStatusTest} holds that.
 *
 * <p><b>What is NOT here: {@code depth}</b>, Ignite's chain counter. It is scorch's alone (the seat's S2
 * ruling), so it lives on {@link ScorchStatus}, which extends this. A field the wither instance carried
 * and nothing read would be the write-only shape {@code Scorch}'s javadoc refuses.
 *
 * <h2>WHAT FOLLOWS WAS WRITTEN FOR SCORCH, AND READS FOR WITHER WITH ITS NUMBERS</h2>
 *
 * Scorch: a stacking burn that owns its own clock, caps its own damage, and credits its applier.
 *
 * <p>{@link SoakedStatus}'s shape, deliberately reused rather than invented beside -- same
 * {@code Map<UUID, Active>}, same "refresh the whole timer, not a per-stack clock", same
 * {@link RepeatingTask}. Four divergences, each with a reason:
 *
 * <ol>
 *   <li><b>Period 20, not 1</b>, and the effect is IN the tick body rather than on stack change,
 *       because this is a DoT and not an attribute.
 *   <li><b>{@code Active} carries {@code cap} and {@code applierId}</b>, both overwritten by the most
 *       recent applier. Two operator rules point the same way -- the newest applier gets ignite kill
 *       credit, and each new stack refreshes the whole timer -- so this is one rule, not two.
 *   <li><b>Players are NOT skipped.</b> Soaked and Immobilize both {@code return} for a Player
 *       ({@code BukkitCombatant}'s Soaked arm) because a movement modifier fights client-side
 *       prediction. A DoT has no such problem, and the operator confirmed players can be scorched.
 *   <li><b>Bulk stacks in one call.</b> A 20-damage hit is ten stacks; {@code SoakedStatus.apply} adds
 *       exactly one per call, so ten calls would be ten map lookups for one hit. (Since the
 *       2026-09-09 ruling the count is read only as a gate and nothing accumulates it -- the bulk
 *       parameter survives because {@code Scorch.stacksFor} still answers "did this hit buy any?".)
 * </ol>
 *
 * <h2>THE CLOCK IS THE ONLY THING THAT BURNS</h2>
 *
 * <b>One rule, not two.</b> Every burn comes from the repeating task; neither arm of {@link #apply}
 * deals damage. A first application starts the clock, a refresh extends it, and the
 * clock does the rest.
 *
 * <p><b>THIS DELETED A SPECIAL CASE RATHER THAN ADDING ONE, AND THAT IS THE ARGUMENT FOR IT.</b> The
 * first application used to burn INLINE while a refresh did not -- and that asymmetry is exactly what
 * produced the nine-burn defect in scorch slice 1, where the refresh arm called {@code burnOnce}
 * because the first arm did. With both arms silent there is no asymmetry left to copy.
 *
 * <h2>THE ORDERING IS COUPLED TO THAT, AND THE PAIR MOVES TOGETHER</h2>
 *
 * The tick body BURNS THEN DECREMENTS. It used to decrement first, and that was correct <i>only
 * because</i> the inline burn had already consumed tick one. <b>Delete the inline burn and keep
 * decrement-first and the last period goes silent</b> -- a 120-tick scorch burns five times and says
 * nothing in its sixth second:
 *
 * <pre>
 *   decrement-then-check, no inline burn:  t=20..100 (5x), stops t=120   FIVE burns   WRONG
 *   burn-then-decrement,  no inline burn:  t=20..120 (6x), stops t=120   SIX burns    lifetime 120
 * </pre>
 *
 * <p><b>The class javadoc here previously said the opposite</b> -- that reversing those two lines
 * "restores {@code SoakedStatus}'s ordering and adds a full period to the lifetime" -- and it was
 * right under the old premise, where the inline burn existed. {@code ScorchStatusTest}'s lifetime row
 * was written to catch that flip as a DEFECT and therefore <b>reddened on this correct change</b>.
 *
 * > A TEST WRITTEN TO GUARD AN ORDERING MUST BE RE-DERIVED WHEN THE ORDERING'S PREMISE CHANGES. Its
 * > red is evidence about the premise, not about the code.
 *
 * It was re-derived from the new premise rather than nudged to green, and the schedule was MEASURED
 * by printing it, not predicted: {@code [20, 40, 60, 80, 100, 120]}.
 *
 * <h2>THE TICK COUNT AND THE LIFETIME ARE STILL TWO NUMBERS</h2>
 *
 * {@code ScorchStatusTest} asserts them separately, because a count alone cannot pin a lifetime. What
 * changed is only where the first burn lands: t=20 rather than t=0. The general rule is unchanged and
 * is the one {@link io.github.butterflysmp.rpg.core.combat.Scorch#damageTicksFor} already stated -- a
 * scorch burns {@code ceil(duration / period)} times and lives that many whole periods.
 *
 * <p><b>There is therefore NO degenerate sub-period case to guard, which is worth stating because it
 * looks like there should be.</b> A duration under one period is not an exception: it is the
 * {@code n = 1} instance of the same rule, burning once at t=20 and living 20 ticks, exactly as a
 * 50-tick scorch burns three times and lives 60. Measured across 1, 19, 21, 41 and 50 ticks: burns
 * equals {@code damageTicksFor} at every one. <b>A refusal here would put back a special case this
 * change exists to remove.</b> (Sub-period durations are reachable only through the dev apply
 * command; no content authors one.)
 *
 *
 * <h2>REFRESH MUST NOT RESTART THE TASK</h2>
 *
 * {@link #apply} rewrites {@code remaining} and leaves the running task alone -- copied from
 * {@code SoakedStatus.java:57}, where it is load-bearing for a different reason and matters far more
 * here. <b>Restarting the task on refresh would re-phase a 20-tick clock, so a weapon hitting every 15
 * ticks would reset it forever and scorch would NEVER TICK AT ALL.</b> Total silent failure: the
 * window keeps refreshing, the burn shows, and no damage is ever dealt. Guarded by
 * {@code reApplyingFasterThanThePeriodStillTicks}.
 *
 * <h2>The map</h2>
 *
 * {@link ConcurrentHashMap} for {@code SoakedStatus}'s stated reason -- different entities apply from
 * different region threads -- and one of scorch's own: {@link #isScorched} is read by
 * {@code RpgListeners.onEnvironmentalDamage} to suppress the fire tick, as well as written by the
 * entity scheduler. A single victim's entry is still only touched on that victim's own thread, since
 * under Folia the region owning the entity runs both its damage events and its entity scheduler, so
 * the read-modify-write in {@link #apply} is uncontended in practice.
 */
public class DotStatus {

    /** A live DoT: its handle, ticks left, and the newest applier's cap and identity. */
    private static final class Active {
        RepeatingTask task;
        int remaining;
        double cap;
        UUID applierId;
        /** The element whose damage most recently applied or refreshed this burn, or null for a dev
         *  application. SAME rule as cap and applierId -- newest wins -- so the burn is marked with
         *  whatever last fed it rather than with whatever first lit it. */
        String element;

        Active(int remaining, double cap, UUID applierId, String element) {
            this.remaining = remaining;
            this.cap = cap;
            this.applierId = applierId;
            this.element = element;
        }
    }

    private final Map<UUID, Active> active = new ConcurrentHashMap<>();

    /** This instance's clock, rate, cap fraction and window: {@code Scorch.RATES} or {@code Wither.RATES}. */
    private final DotRates rates;

    /** The task's name in a failure's log line ({@code RepeatingTask.start}): {@code scorch}, {@code wither}. */
    private final String taskName;

    /** The content status id this instance runs, for {@code DOTTICK}: {@code scorch}, {@code withering}. */
    private final String statusId;

    /** Where every tick is reported ({@code DOTTICK}, the seat's S3 ruling). */
    private final DotTrace trace;

    /**
     * @param statusId the content id of the status this instance runs, resolved from the loaded
     *                 statuses by kind ({@code AdapterContext.statusIdOf}), so the trace names the id a
     *                 gate row quotes ({@code withering}), not the kind ({@code wither})
     */
    public DotStatus(DotRates rates, String taskName, String statusId, DotTrace trace) {
        this.rates = rates;
        this.taskName = taskName;
        this.statusId = statusId;
        this.trace = trace;
    }

    /** No trace, and the task name as the status id: a store a test builds when the trace is not its subject. */
    public DotStatus(DotRates rates, String taskName) {
        this(rates, taskName, taskName, DotTrace.NONE);
    }

    /**
     * Apply scorch to {@code id}, or refresh the whole timer if already scorched.
     *
     * <p>NOTHING BURNS HERE. Both arms are silent and the repeating task is the only source of
     * damage -- see the class javadoc for why deleting the old inline first burn removed a special
     * case rather than adding one, and why the tick body had to flip to burn-then-decrement in the
     * same change.
     *
     * @param stacks         what this application is worth, from {@code Scorch.stacksFor}. Read ONLY
     *                       as a gate: {@code <= 0} means the hit bought none and applies nothing.
     *                       Nothing accumulates it -- the count has no consumer, see {@code Scorch}
     * @param cap            the AUTHORED damage of whatever applied them -- never what it landed
     * @param applierId      who gets the kill credit; overwrites any previous applier
     * @param durationTicks  the whole window, refreshed on every application
     * @return false when the hit bought no stacks and nothing was applied or refreshed; true otherwise
     *         ({@link ScorchStatus} keeps its scorch-only {@code depth} only for an application that
     *         landed)
     */
    public boolean apply(UUID id, RepeatingTaskTarget target, ScorchSink sink,
                         int stacks, double cap, UUID applierId, int durationTicks,
                         String element) {
        // A hit too small to buy a stack scorches nothing -- AND DOES NOT REFRESH AN EXISTING BURN
        // either, since this returns before the refresh arm below. Correct by the letter of the
        // spec: stacks are what scorch is made of, and a hit that buys none has not applied it. But
        // it is the reading a player will report as a bug ("my weak hit didn't extend the burn"), so
        // it is written down here rather than left to be rediscovered from the symptom.
        if (stacks <= 0) return false;

        Active a = active.get(id);
        if (a != null && a.task.isRunning()) {
            // (Ignite's DEEPEST-WINS `depth` sat here and moved to ScorchStatus.apply with the S2
            // ruling. The note below still names it as this field's sibling, which it is.)
            a.remaining = durationTicks;   // refresh the whole timer, and DO NOT restart the task
            // THIS ASSIGNMENT CAN SHORTEN A LIVE BURN, AND NOTHING IN THE MECHANISM STOPS IT.
            // It is safe TODAY only because every content-driven application passes the same
            // Scorch.DEFAULT_DURATION_TICKS -- the nine authored durations were stripped in the
            // content pass. THAT IS A CONTENT-SHAPED INVARIANT, NOT A MECHANISM ONE. Contrast
            // WeaponFire.landVanillaMelee, whose one-slot sink genuinely enforces its first-wins rule
            // in code; this one is enforced by there being nothing else to pass.
            //
            // WHAT BREAKS IT: any SECOND source of durations. An element declaring one (the refused
            // shape (b) in PLAN-element-content-pass.md), or an ability regaining an authored
            // duration_ticks. On that day a short application silently truncates a long burn, and
            // this line becomes Math.max(a.remaining, durationTicks) -- "extend a burn, never shorten
            // it", the rule BukkitCombatant.java:235, :374 and :388 already apply three times.
            //
            // AND THE FOURTH IS SCORCH'S `depth` (ScorchStatus.apply; it sat directly above this line
            // until the S2 ruling moved it). `depth` is deepest-wins for the same reason under a
            // different name -- a safety counter must not be lowered by a later write -- so the fix
            // this comment predicts is already applied to a sibling field of the same burn. THAT IS
            // THE ARGUMENT FOR MAKING IT, not against: `remaining` and `depth` are the only two
            // fields where a later application could destroy information, and only one is guarded.
            //
            // (Updated 2026-09-10. The line numbers above were :272-273 and :288 and had gone stale --
            // both now point at Ignite's attribution comment and an aggro note. A citation that
            // drifts is worse than none: it sends the next reader to confidently wrong lines.)
            //
            // Reachable today only through the dev apply command, which takes an operator-chosen
            // duration. Deferred on THAT reason -- not on "no second duration exists", which was the
            // false one it was first deferred on.
            a.cap = cap;                   // most recent applier owns the cap...
            a.applierId = applierId;       // ...and the credit. One rule, not two.
            a.element = element;          // ...and the mark. One rule, not three.
            // AND IT DOES NOT BURN -- which is now the same rule the first-application arm follows
            // rather than an exception to it. A burn here would be damage outside the clock this
            // class exists to own, and it would scale with HIT RATE rather than with time: a weapon
            // hitting every 10 ticks would deal two unscheduled burns per period on top of the
            // scheduled one, three times the stated rate. "5% of max per second" has to keep
            // meaning that. A refresh refreshes.
            // Guarded by ScorchStatusTest.aRefreshDoesNotDealAnUNSCHEDULEDBurn.
            return true;
        }

        Active na = new Active(durationTicks, cap, applierId, element);

        BooleanSupplier tick = () -> {
            // BURN, THEN DECREMENT -- and this ordering is COUPLED to the inline burn being gone.
            // Decrement-first existed only because the inline burn had already consumed tick one;
            // keeping it after deleting that burn silently drops the LAST period (a 120-tick scorch
            // would burn five times and fall silent for its sixth second). The two move together.
            burnOnce(id, na, sink);
            na.remaining -= rates.periodTicks();
            return na.remaining > 0;
        };
        // THE MAP ENTRY IS THE ONLY THING THIS LOOP OWNS, so onStop is unchanged by the 2026-09-24
        // release rule -- there is no attribute to restore. Said rather than left, because the rule's
        // other two subjects (Immobilize, Soaked) both grew a release here and a reader checking the
        // third would otherwise wonder whether it was missed.
        //
        // WHAT A CRASHED LOOP USED TO LEAK HERE IS STILL REAL AND IS NOW FIXED BY THE ISOLATION
        // ALONE: apply() takes its refresh branch on `a.task.isRunning()`, and before the fix a
        // thrown body left running == true forever -- so the victim could never be scorched again,
        // and trackedVictims() counted them for the rest of the session. Now the loop stops, onStop
        // clears the entry, and the next apply() starts a fresh one.
        //
        // onEnded is the subclass's hook (ScorchStatus drops its depth there), called only when THIS
        // entry is the one removed: a stale task stopping after forget-and-reapply must not end the
        // new burn's state.
        na.task = RepeatingTask.start(target, rates.periodTicks(), taskName,
                tick, () -> { if (active.remove(id, na)) onEnded(id); });
        active.put(id, na);
        return true;
    }

    /**
     * Called when {@code id}'s DoT ends -- its window ran out, its loop stopped, or {@link #forget}
     * dropped it. A no-op here; {@link ScorchStatus} clears its scorch-only {@code depth}. May be called
     * twice for one ending (a forget that cancels a running task), so it must be idempotent.
     */
    protected void onEnded(UUID id) {}

    /**
     * One tick of burn: {@code min(5% of the victim's max, the cap)}, credited to the applier.
     *
     * <b>The rate is FLAT, and since 2026-09-09 there is no stack count here to read.</b> See
     * {@code Scorch}'s javadoc for both operator statements and which supersedes which, and for why
     * the accumulator was deleted rather than left unread. Multiplying by a stack count is the
     * one-line change that turns crowd control into a two-second execution; that mutation is now
     * <i>unexpressible</i> rather than merely tested against, which is the stronger guarantee -- the
     * count would have to be re-added first, and its re-add trigger requires naming a consumer.
     */
    private void burnOnce(UUID id, Active a, ScorchSink sink) {
        double amount = rates.damagePerTick(sink.victimMaxHealth(), a.cap);
        // DOTTICK BEFORE THE DAMAGE, so the tick that kills still has its line, ahead of the death it
        // causes (WS3-P reads "the killing tick names withering"). One line per tick, for every
        // instance of this store -- scorch and wither alike, which is the S3 ruling.
        trace.tick(new DotTick(id, sink.victimType(), statusId, a.applierId, amount, a.cap));
        sink.deal(amount, a.applierId, a.element);
    }

    /**
     * True while {@code id} carries this DoT -- the only question anything asks about its presence.
     * {@link ScorchStatus#isScorched} is this, under scorch's name.
     *
     * <p>For scorch it is read from {@code RpgListeners.onEnvironmentalDamage} to suppress the
     * {@code FIRE_TICK} the status caused, and by Ignite. For wither it is read by the same handler to
     * suppress the vanilla WITHER potion's own damage (Q-W7 a), and by nothing else.
     */
    public boolean isActive(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning();
    }

    /**
     * Who currently owns {@code id}'s DoT -- the most recent applier -- or null if it has none.
     * Ignite credits scorch's.
     */
    public UUID applier(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning() ? a.applierId : null;
    }

    /**
     * Drop {@code id}'s scorch outright.
     *
     * <p>Called on the same three occasions {@code DamageWindow.forget} is -- mob removal, player quit
     * and <b>player respawn</b> -- and the third is a correctness fix rather than a leak fix, for the
     * same reason: quit handling does not run on death and the entity-removal handler filters players
     * out, so without it a player who dies mid-burn respawns still scorched.
     */
    public void forget(UUID id) {
        Active a = active.remove(id);
        if (a != null && a.task != null) a.task.cancel();
        onEnded(id);
    }

    /** Number of victims holding scorch state. The bounds check for tests. */
    public int trackedVictims() {
        return active.size();
    }
}
