package io.github.butterflysmp.rpg.core.mining;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/**
 * One player's Trail-style patch at one slot: spots revealed one at a time, each hit spot staying
 * as a crack, and the last charged hit breaking the patch (PLAN-mining.md 5.2).
 *
 * <pre>
 * FRESH (spot 1 live) --charged hit on the live spot--> spot 1 cracks, spot 2 is revealed and live
 *    ...
 * last spot live --charged hit--> BROKEN: the payout is rolled, and the caller starts regrowth
 * </pre>
 *
 * <p><b>The order of the checks is NotLive, Denied, Miss, Uncharged, then Hit or Broke.</b>
 * {@code PatchTest} has a row for each neighbouring pair whose input satisfies both, so a swap
 * reddens it.
 *
 * <p>Each spot's reveal tick and hit tick are recorded. Nothing reads them for payout in slice 1;
 * they exist so a gold window can be computed later without a state change (Ben deferred the gold
 * window: "later"), and the trace can print them. Not thread-safe: used on the main thread only.
 */
public final class Patch {

    private static final long NONE = -1L;

    private final SpotLayout layout;
    private final PayoutRule payoutRule;
    private final long[] revealTicks;
    private final long[] hitTicks;
    private int live;          // index of the live spot; == spot count once broken

    /**
     * @param layout     the rolled spots, in reveal order
     * @param payoutRule how the break pays
     * @param revealTick the tick the patch appeared, which reveals its first spot
     */
    public Patch(SpotLayout layout, PayoutRule payoutRule, long revealTick) {
        if (layout.spots().isEmpty()) throw new IllegalArgumentException("a patch needs at least one spot");
        this.layout = layout;
        this.payoutRule = payoutRule;
        int n = layout.spots().size();
        this.revealTicks = new long[n];
        this.hitTicks = new long[n];
        Arrays.fill(revealTicks, NONE);
        Arrays.fill(hitTicks, NONE);
        revealTicks[0] = revealTick;
        this.live = 0;
    }

    /** Judges one click and applies it. The payout, if the patch breaks, is rolled from {@code rng}. */
    public ClickOutcome click(Click click, RandomGenerator rng) {
        if (isBroken()) return ClickOutcome.NotLive.INSTANCE;
        if (!click.holdingPickaxe()) return ClickOutcome.Denied.INSTANCE;
        Spot liveSpot = layout.spots().get(live);
        if (click.aim().isEmpty() || !SpotBox.contains(liveSpot, click.aim().get())) {
            return ClickOutcome.Miss.INSTANCE;
        }
        if (!ChargeRule.isCharged(click.cooldown())) return ClickOutcome.Uncharged.INSTANCE;

        int cracked = live;
        hitTicks[cracked] = click.tick();
        live++;
        if (isBroken()) {
            return new ClickOutcome.Broke(cracked, payoutRule.roll(layout.spots().size(), rng));
        }
        revealTicks[live] = click.tick();
        return new ClickOutcome.Hit(cracked, live);
    }

    public SpotLayout layout() { return layout; }

    /** The live spot's index, 0-based. Equal to the spot count once the patch is broken. */
    public int liveIndex() { return live; }

    public boolean isBroken() { return live >= layout.spots().size(); }

    /** The tick spot {@code i} was revealed, or -1 if it has not been. */
    public long revealTick(int i) { return revealTicks[i]; }

    /** The tick spot {@code i} was hit, or -1 if it has not been. */
    public long hitTick(int i) { return hitTicks[i]; }
}
