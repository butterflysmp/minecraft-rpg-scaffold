package io.github.butterflysmp.rpg.core.fight;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * F9b, fix A: ONE CONTACT HIT PER WINDOW (the seat's brief of 2026-11-19). A player takes at most one priced contact
 * hit (bite or wing, together) from the fight's dragon per window of {@code intervalTicks}, counted from the hit that
 * landed. A hit inside the window lands nothing; the suppressed ones are counted and told once, with the next landing
 * hit (or at the fight's end). The breath is not in it: it keeps its cloud's own cadence.
 *
 * <p>Why it is needed, READ from the pinned server jar: the dragon calls its head's and its neck's hurt every tick it is
 * not flinching, and our melee rider's token damage sets the player's {@code lastHurt} to 0.01, so vanilla's own
 * invulnerability window lets every later hit through. The window holds whichever way the hits arrive.
 */
public final class ContactHitWindow {

    private final int intervalTicks;
    private final Map<UUID, long[]> open = new HashMap<>(); // {window's first tick, suppressed count}

    public ContactHitWindow(int intervalTicks) {
        if (intervalTicks < 1) throw new IllegalArgumentException("a contact window must be at least 1 tick: " + intervalTicks);
        this.intervalTicks = intervalTicks;
    }

    /** One contact hit on {@code player} at the fight's {@code tick}: whether it lands, and the last window's count. */
    public Result hit(UUID player, long tick) {
        long[] w = open.get(player);
        if (w != null && tick - w[0] < intervalTicks) {
            w[1]++;
            return new Result(false, 0, w[0]);
        }
        Result landed = new Result(true, w == null ? 0 : (int) w[1], w == null ? tick : w[0]);
        open.put(player, new long[] {tick, 0});
        return landed;
    }

    /** At the fight's end: the open window's suppressed count, told once. */
    public Result flush(UUID player) {
        long[] w = open.get(player);
        if (w == null || w[1] == 0) return new Result(false, 0, w == null ? -1 : w[0]);
        Result r = new Result(false, (int) w[1], w[0]);
        w[1] = 0;
        return r;
    }

    public int intervalTicks() { return intervalTicks; }

    /** Every player a window was opened for (to flush at the fight's end). */
    public java.util.Set<UUID> players() { return java.util.Set.copyOf(open.keySet()); }

    /**
     * @param lands            whether this hit is priced
     * @param suppressedBefore how many hits the window this one closes had suppressed (0 for a suppressed hit)
     * @param windowFrom       that window's first tick
     */
    public record Result(boolean lands, int suppressedBefore, long windowFrom) {}
}
