package io.github.butterflysmp.rpg.core.fight;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DOWN, AND ALL DOWN (step F9d; Ben, PLAN-dragon-fight.md section 1, 0d: "When a player dies, they are thrown into
 * spectator for the remainder of the fight or until they are respawned by another player"; 0e: "When all players are
 * defeated at once they are all in spectator mode for a few seconds and get sent back to the normal world").
 *
 * <p>Who is down, in the order they went down, and the fight's ALL-DOWN clock. Pure: the Paper side says who is alive
 * inside (a participant), and this says whether the fight has failed and when everyone goes home.
 *
 * <p>Not thread-safe; the fight's own thread owns it.
 */
public final class DownRoster {

    /** What {@link #check} found on this tick. */
    public enum Check {
        /** Not all down. */
        NONE,
        /** All down for the first time: the fight has FAILED on this tick. Reported once. */
        FAILED,
        /** Failed, and the return is not due yet. */
        WAITING,
        /** Failed, and {@code delay} ticks have passed: everyone goes home now. */
        RETURN_DUE
    }

    private final Set<UUID> down = new LinkedHashSet<>();
    /** F9e e: revives per player in this fight (read by {@code ReviveRules.mayRevive}). */
    private final java.util.Map<UUID, Integer> revives = new java.util.HashMap<>();
    private final int delayTicks;
    private long failedAt = -1;

    /** @param delayTicks ticks between the failure and the return ({@code all_down_seconds} x 20); 0 or more */
    public DownRoster(int delayTicks) {
        if (delayTicks < 0) throw new IllegalArgumentException("the all-down delay must be 0 or more: " + delayTicks);
        this.delayTicks = delayTicks;
    }

    /**
     * THE ALL-DOWN RULE: no participant alive inside, and at least one down. A solo player's death is the fight's
     * failure. Nobody down and nobody inside is not all down: that is "nobody inside" (everyone left or quit).
     */
    public static boolean allDown(int aliveInside, int downCount) {
        return aliveInside == 0 && downCount > 0;
    }

    /** He went down. True when he was not down already. */
    public boolean down(UUID id) {
        return down.add(id);
    }

    public boolean isDown(UUID id) {
        return down.contains(id);
    }

    /** He was revived (step F9e). True when he was down; his count of revives in this fight rises by one. */
    public boolean revive(UUID id) {
        if (!down.remove(id)) return false;
        revives.merge(id, 1, Integer::sum);
        return true;
    }

    /** How many times he has been revived in this fight. */
    public int revives(UUID id) {
        return revives.getOrDefault(id, 0);
    }

    /** He is out of the fight (quit, or left the fight world). */
    public void out(UUID id) {
        down.remove(id);
    }

    /** Who is down, in the order they went down. */
    public Set<UUID> downed() {
        return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(down));
    }

    public int downCount() {
        return down.size();
    }

    /** The tick the fight failed on, or -1 while it has not. */
    public long failedAt() {
        return failedAt;
    }

    /**
     * Once a tick. The first tick on which {@link #allDown} holds is FAILED; it then stays failed whatever happens
     * (nobody alive can revive, and a down player quitting does not undo it), and the return is due {@code delay}
     * ticks after the failure.
     */
    public Check check(long tick, int aliveInside) {
        if (failedAt < 0) {
            if (!allDown(aliveInside, down.size())) return Check.NONE;
            failedAt = tick;
            return Check.FAILED;
        }
        return tick - failedAt >= delayTicks ? Check.RETURN_DUE : Check.WAITING;
    }
}
