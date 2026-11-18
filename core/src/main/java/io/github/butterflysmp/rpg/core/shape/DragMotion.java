package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * THE ONE TALLY OF EVERY DRAG ON A MOB, PER TICK (the implosion's drag, 2026-11-18). One instance per server, owned by
 * the plugin and handed to every {@link ImplosionDrag}: two drags on one MOB in one tick must add to ONE velocity,
 * because the server SETS a mob's velocity and the second {@code setVelocity} would otherwise replace the first.
 *
 * <p><b>WHAT REMAINS OF IT, AND WHY (feat/drag-additive, 2026-11-18).</b> Until then it also ESTIMATED a player's
 * momentum from his last two positions, so the server could send his momentum plus the drag as one velocity that
 * replaced his. That needed one move packet a server tick and a send landing the next tick; on a remote client
 * neither holds, and the same build dragged a still player to 0.491 in one run and 1.347 in the next. The seat ruled
 * that no design may need the server to know his momentum. A player's drag is now an ADD his own client adds
 * ({@link Implosion#addStep}, sent as one packet per drag), so the estimate is gone, and so is any tally for a player:
 * two adds simply both arrive. What remains is the mob's tally, unchanged.
 *
 * <p>Thread-safe by {@code synchronized}: each drag runs on its origin's region thread. Entries older than two ticks
 * are pruned by the drags themselves, so a body nobody drags is forgotten.
 */
public final class DragMotion {

    private static final class Track {
        long tallyTick = Long.MIN_VALUE;
        Vec3 base = Vec3.ZERO;
        Vec3 sum = Vec3.ZERO;
    }

    private final Map<UUID, Track> tracks = new HashMap<>();

    /**
     * ADD one drag's {@code add} to {@code id}'s tally for {@code tick} and return the velocity to SET now: the
     * {@code base} the FIRST drag of the tick named (the server's velocity for the mob, the same for every drag of one
     * tick), plus every add of the tick so far. The last {@code setVelocity} of the tick therefore carries them all.
     */
    public synchronized Vec3 tally(UUID id, long tick, Vec3 base, Vec3 add) {
        Track t = tracks.computeIfAbsent(id, k -> new Track());
        if (t.tallyTick != tick) {
            t.tallyTick = tick;
            t.base = base;
            t.sum = Vec3.ZERO;
        }
        t.sum = t.sum.add(add);
        return t.base.add(t.sum);
    }

    /** Forget every body last tallied before {@code tick - 2}. */
    public synchronized void prune(long tick) {
        tracks.values().removeIf(t -> t.tallyTick < tick - 2);
    }

    /** How many bodies are remembered: a test's window on {@link #prune}. */
    public synchronized int size() {
        return tracks.size();
    }
}
