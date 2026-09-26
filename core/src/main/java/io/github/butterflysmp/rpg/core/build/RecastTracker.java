package io.github.butterflysmp.rpg.core.build;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * ONE PLAYER's recast state (PLAN-build-system.md section 7.4): a hold per stone button ({@link InputHold}) and
 * at most one open window. The paper side keeps one per online player and feeds it EVERY stone input -- the
 * ones the cast will refuse included, because a held click's refused inputs are what carry its hold.
 *
 * <p>Pure and not thread-safe: the paper side touches it only on the player's own thread.
 */
public final class RecastTracker {

    private record Open(String abilityId, AspectDefinition.Recast recast, long castTick, boolean used) {}

    private final Map<LoadoutSlot, InputHold> holds = new EnumMap<>(LoadoutSlot.class);
    private Open open;

    /**
     * Record an input, then say whether it is a recast.
     *
     * @param slot         the button (the loadout slot it casts)
     * @param tick         the tick the input arrived in
     * @param abilityId    the ability that slot holds
     * @param stillGranted the recast aspect is STILL active on that ability (a build change closes the window)
     * @return the follow-up to cast instead, or empty for an ordinary cast
     */
    public Optional<String> input(LoadoutSlot slot, long tick, String abilityId, boolean stillGranted) {
        InputHold hold = holds.getOrDefault(slot, InputHold.NONE).next(tick);
        holds.put(slot, hold);
        if (open == null || !open.abilityId().equals(abilityId) || !stillGranted) return Optional.empty();
        if (!RecastRule.accepts(open.castTick(), tick, open.recast().fromTicks(), open.recast().windowTicks(),
                open.used(), hold.holdStartTick())) {
            return Optional.empty();
        }
        open = new Open(open.abilityId(), open.recast(), open.castTick(), true);
        return Optional.of(open.recast().ability());
    }

    /**
     * An ordinary cast succeeded. A cast of the window's own ability replaces the window (with a new one, or
     * none); any other ability's cast leaves it alone.
     *
     * @param inputTick the tick of the INPUT that cast it, the same clock {@link #input} is fed
     */
    public void castSucceeded(String abilityId, long inputTick, Optional<AspectDefinition.Recast> recast) {
        if (recast.isPresent()) {
            open = new Open(abilityId, recast.get(), inputTick, false);
        } else if (open != null && open.abilityId().equals(abilityId)) {
            open = null;
        }
    }

    /** Death: the respawned player did not cast what the window was opened for. */
    public void clear() {
        open = null;
    }
}
