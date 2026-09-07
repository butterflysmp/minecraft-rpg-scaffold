package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
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
 *       exactly one per call, so ten calls would be ten map lookups for one hit.
 * </ol>
 *
 * <h2>THE INHERITED OFF-BY-ONE, WHICH IS A FULL SECOND HERE</h2>
 *
 * <b>A constant inherited from a precedent carries that precedent's tolerances, and nothing in the
 * copy says which ones came along.</b> {@code SoakedStatus} checks {@code remaining <= 0} BEFORE
 * decrementing, so its task acts {@code N/P} times and then stops one whole period later. At period 1
 * that overrun is a single tick -- invisible, which is why the precedent is fine as written. At period
 * 20 it would make an 8-second status live 9.0 seconds:
 *
 * <pre>
 *   check-then-decrement:  acts t=20..160 (8x), stops t=180   LIFETIME 180 = 9.0s   WRONG
 *   decrement-then-check:  acts t=20..140 (7x), stops t=160   LIFETIME 160 = 8.0s   + 1 inline = 8 ticks
 * </pre>
 *
 * <b>The tick COUNT and the LIFETIME are two numbers, and {@code ScorchStatusTest} asserts them
 * separately because a count alone cannot pin the lifetime.</b> Verified rather than argued, by
 * mutation: restore the check-then-decrement ordering AND drop the inline first burn, and the status
 * acts 8 times at t=20..160 -- so {@code eightDamageTicksForTheEightSecondDefault} stays GREEN at 8
 * while the status lives 180 ticks. Only {@code theEightSecondStatusLivesEXACTLYEightSeconds} reddens.
 *
 * <p>(The single-part mutation -- ordering alone, inline burn kept -- reddens both, because it yields
 * 9 burns. That is worth stating precisely: the count row is not useless here, it simply cannot be
 * relied on to catch a lifetime defect, and the two-part mutation above is the witness.)
 *
 * <h2>REFRESH MUST NOT RESTART THE TASK</h2>
 *
 * {@link #apply} rewrites {@code remaining} and leaves the running task alone -- copied from
 * {@code SoakedStatus.java:57}, where it is load-bearing for a different reason and matters far more
 * here. <b>Restarting the task on refresh would re-phase a 20-tick clock, so a weapon hitting every 15
 * ticks would reset it forever and scorch would NEVER TICK AT ALL.</b> Total silent failure: stacks
 * climb, the burn shows, and no damage is ever dealt. Guarded by
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
public final class ScorchStatus {

    /** A live scorch: its handle, stacks, ticks left, and the newest applier's cap and identity. */
    private static final class Active {
        RepeatingTask task;
        int stacks;
        int remaining;
        double cap;
        UUID applierId;

        Active(int stacks, int remaining, double cap, UUID applierId) {
            this.stacks = stacks;
            this.remaining = remaining;
            this.cap = cap;
            this.applierId = applierId;
        }
    }

    private final Map<UUID, Active> active = new ConcurrentHashMap<>();

    /**
     * Apply {@code stacks} scorch stacks to {@code id}, or add them and refresh the whole timer if
     * already scorched.
     *
     * <p>The first damage tick lands INLINE, here, not one period later. {@code RepeatingTask}'s first
     * tick is scheduled rather than run inline ({@code RepeatingTask.java:42}), which for a DoT would
     * mean a scorch that expires inside one period deals nothing at all. A hit should always burn at
     * least once.
     *
     * @param stacks         how many stacks this application is worth, from {@code Scorch.stacksFor}
     * @param cap            the AUTHORED damage of whatever applied them -- never what it landed
     * @param applierId      who gets the kill credit; overwrites any previous applier
     * @param durationTicks  the whole window, refreshed on every application
     */
    public void apply(UUID id, RepeatingTaskTarget target, ScorchSink sink,
                      int stacks, double cap, UUID applierId, int durationTicks) {
        if (stacks <= 0) return;   // a hit too small to buy a stack scorches nothing

        Active a = active.get(id);
        if (a != null && a.task.isRunning()) {
            a.stacks += stacks;
            a.remaining = durationTicks;   // refresh the whole timer, and DO NOT restart the task
            a.cap = cap;                   // most recent applier owns the cap...
            a.applierId = applierId;       // ...and the credit. One rule, not two.
            burnOnce(a, sink);
            return;
        }

        Active na = new Active(stacks, durationTicks, cap, applierId);
        burnOnce(na, sink);                // tick 1, inline -- see the javadoc above

        BooleanSupplier tick = () -> {
            // DECREMENT FIRST. Reversing these two lines restores SoakedStatus's ordering and adds a
            // full period to the lifetime -- an 8-second status that lives 9. See the class javadoc
            // for the mutation that proves the lifetime needs its own assertion.
            na.remaining -= Scorch.PERIOD_TICKS;
            if (na.remaining <= 0) return false;
            burnOnce(na, sink);
            return true;
        };
        na.task = RepeatingTask.start(target, Scorch.PERIOD_TICKS, tick, () -> active.remove(id, na));
        active.put(id, na);
    }

    /**
     * One tick of burn: {@code min(5% of the victim's max, the cap)}, credited to the applier.
     *
     * <b>{@code a.stacks} is deliberately not read here.</b> The rate is FLAT -- see {@code Scorch}'s
     * javadoc for both operator statements and which supersedes which. Multiplying by the stack count
     * is the one-line change that turns crowd control into a two-second execution, and
     * {@code ScorchStatusTest.stacksDoNOTScaleTheDamage} is the only thing that would catch it.
     */
    private static void burnOnce(Active a, ScorchSink sink) {
        sink.deal(Scorch.damagePerTick(sink.victimMaxHealth(), a.cap), a.applierId);
    }

    /**
     * True while {@code id} has live scorch stacks.
     *
     * <p>Read from {@code RpgListeners.onEnvironmentalDamage} to suppress the {@code FIRE_TICK} this
     * status caused: the burn stays visible, the damage comes from our clock. Without that gate a
     * scorched target takes our 5%/sec DoT PLUS vanilla's rerouted fire ticks -- a double-dip on a
     * schedule we do not own.
     */
    public boolean isScorched(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning();
    }

    /** Current stack count on {@code id}, or 0. Slice 2's ignite threshold reads this. */
    public int stacks(UUID id) {
        Active a = active.get(id);
        return a != null && a.task.isRunning() ? a.stacks : 0;
    }

    /**
     * Who currently owns {@code id}'s scorch -- the most recent applier -- or null if unscorched.
     * Slice 2's ignite credits this.
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
    }

    /** Number of victims holding scorch state. The bounds check for tests. */
    public int trackedVictims() {
        return active.size();
    }
}
