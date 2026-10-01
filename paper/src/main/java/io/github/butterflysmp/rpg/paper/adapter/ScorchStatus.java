package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTaskTarget;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scorch: the shared {@link DotStatus} at {@link Scorch#RATES}, plus the one thing only scorch has --
 * Ignite's chain {@code depth}.
 *
 * <p><b>Why a subclass (the seat's S2 ruling, 2026-09-30: {@code depth} stays SCORCH-ONLY).</b> The
 * store is shared with Wither, and Wither has no Ignite. On the shared store {@code depth} would be a
 * field the wither instance carried and nothing read -- the write-only shape {@code Scorch}'s javadoc
 * refuses (<i>"a write-only field is worse than a deleted one"</i>). Here it exists only on the instance
 * Ignite reads. The clock, the refresh, the cap and the credit are {@link DotStatus}'s, and its javadoc
 * is the account of them.
 *
 * <p><b>The depth's lifetime is the burn's.</b> It is set on a fresh application, raised on a refresh,
 * read only while the burn is live ({@link #depth}), and dropped in {@link #onEnded} -- the hook the
 * shared store calls when this victim's burn ends or is forgotten.
 */
public final class ScorchStatus extends DotStatus {

    /** Ignite-chain depth per scorched victim. DEEPEST wins -- NOT newest. See {@link #apply}. */
    private final Map<UUID, Integer> depths = new ConcurrentHashMap<>();

    public ScorchStatus() {
        super(Scorch.RATES, "scorch");
    }

    /**
     * Apply scorch to {@code id}, or refresh the whole timer if already scorched, recording the
     * ignite-chain {@code depth} of whatever applied it. Everything but the depth is
     * {@link DotStatus#apply}'s.
     *
     * @param depth the ignite-chain depth of the blast that scorched this mob, or 0 if no blast did
     */
    public void apply(UUID id, RepeatingTaskTarget target, ScorchSink sink,
                      int stacks, double cap, UUID applierId, int durationTicks,
                      String element, int depth) {
        boolean wasScorched = isScorched(id);
        if (!apply(id, target, sink, stacks, cap, applierId, durationTicks, element)) return;
        if (!wasScorched) {
            depths.put(id, depth);
            return;
        }
        // DEEPEST WINS, AND THIS IS THE ONE FIELD THAT IS NOT NEWEST-WINS.
        //
        // cap, applierId and element are PRESENTATION AND CREDIT facts, where the most recent
        // applier is the right answer. depth is a SAFETY COUNTER, where the deepest reading is --
        // and newest-wins here would silently defeat the chain limit, because THE BURN WINDOW
        // DECOUPLES DEPTH FROM ELAPSED TIME and a later blast is therefore not a deeper blast:
        //
        //   A scorched at depth 1, high HP, SURVIVES; burns out and dies at t=7 -> detonates t=8.
        //   Meanwhile a fast branch runs 2 -> 3, scorching D at DEPTH 3 by t=4.
        //   At t=8 A's depth-2 blast reaches D. Newest-wins drops D from 3 to 2, so D detonates
        //   at 3 instead of 4 and the frontier advances again.
        //
        // Repeat that with a pack of staggered survivors and the link count from the original
        // root is bounded by the MOB POPULATION rather than by MAX_CHAIN_DEPTH -- which is the
        // spawner scenario the recruitment ruling was bounded to avoid, arriving by the back door.
        //
        // "Extend, never shorten" is already this codebase's rule for the same reason: see
        // BukkitCombatant's two setFireTicks sites and the note on `remaining` in DotStatus.apply.
        //
        // THE COST, NAMED: a player re-lighting a depth-3 mob with a fire weapon no longer
        // restarts its chain, because a weapon hit is depth 0 and max ignores it. That is the
        // correct trade -- under newest-wins that same swing re-roots the wave AT WEAPON SPEED,
        // and no gate row could reach it.
        depths.merge(id, depth, Math::max);
    }

    /**
     * True while {@code id} is scorched -- the question Ignite asks, and the one
     * {@code RpgListeners.onEnvironmentalDamage} asks to suppress the {@code FIRE_TICK} this status
     * caused: the burn stays visible, the damage comes from our clock. Without that gate a scorched
     * target takes our 5%/sec DoT PLUS vanilla's rerouted fire ticks -- a double-dip on a schedule we
     * do not own.
     */
    public boolean isScorched(UUID id) {
        return isActive(id);
    }

    /**
     * The ignite-chain depth of the blast that scorched {@code id}, or 0 if no blast did.
     *
     * <p><b>READ ON THE DEATH FRAME, ALONGSIDE {@link #applier}, AND NEVER AT DETONATION.</b> The
     * once-ness guard forgets this entry the instant the death handler has what it needs, so a read
     * taken later returns 0 -- and 0 means "a player caused this", so <b>every link would detonate at
     * depth 1 and the chain limit would never engage.</b> That failure presents as an unbounded
     * cascade, i.e. exactly as "the limit doesn't work", with nothing pointing at the read order.
     *
     * <p>It has no unit witness: it is a paper-side ordering, the same class as the guard itself.
     * {@code GATE-ignite.md}'s cap row is the only thing that can see it.
     */
    public int depth(UUID id) {
        return isActive(id) ? depths.getOrDefault(id, 0) : 0;
    }

    /** The burn ended or was forgotten: its depth goes with it. */
    @Override protected void onEnded(UUID id) {
        depths.remove(id);
    }
}
