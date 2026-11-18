package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * WHAT A DRAGGED BODY IS ALREADY DOING, AND WHAT EVERY DRAG ADDS TO IT THIS TICK (the implosion's drag, 2026-11-18).
 * One instance per server, owned by the plugin and handed to every {@link ImplosionDrag}: two drags on one victim in
 * one tick must add to ONE velocity, because the second {@code setVelocity} would otherwise replace the first.
 *
 * <p><b>WHY A PLAYER'S MOMENTUM IS ESTIMATED HERE AND NEVER READ FROM THE SERVER</b> (D1, D2 and D3 of the brief of
 * 2026-11-18, READ in the pinned jars, {@code javap -c -p}):
 * <ul>
 *   <li>A velocity the server sends REPLACES the client's: {@code ClientPacketListener#handleSetEntityMotion} calls
 *       {@code Entity#lerpMotion}, which is {@code setDeltaMovement} (client jar {@code 26.1.2.jar}).</li>
 *   <li>The client applies queued packets BEFORE its tick ({@code Minecraft#runTick}: {@code processQueuedPackets},
 *       then {@code tick}), and its tick then ADDS that tick's key input ({@code Entity#moveRelative}) before it moves:
 *       a send wipes the momentum he carried, never the input of the tick it lands in.</li>
 *   <li>The server's own copy of a player's velocity ({@code Player#getVelocity}, {@code getDeltaMovement}) is NOT his:
 *       the client never reports it (INFERRED from {@code ServerGamePacketListenerImpl#handleMovePlayer}, which reads
 *       positions; the server keeps the position delta apart, {@code ServerPlayer#lastKnownClientMovement}). It holds
 *       the server's own residue: the resting {@code (0 - 0.08) x 0.98f} downward, and a push out of an overlapping mob
 *       ({@code Entity#push} sets {@code needsSync}, never {@code hurtMarked}, so it is never sent).</li>
 * </ul>
 * So a drag that sent {@code getVelocity() + pull} each tick would wipe his walking every tick (his fight against it
 * shrinks to one tick's input) and send the server's residue as though it were his. Instead the momentum he carries
 * into the next tick is ESTIMATED from his last tick's own motion, as his client computed it: the displacement between
 * the last two positions the server saw, its horizontal part times the friction of the state it STARTED that tick in
 * ({@link Implosion#tickMultiplier}), its vertical part {@code (dy - 0.08) x 0.98f}. The send is that estimate plus the
 * drag, so his own momentum survives it and the drag ADDS, as a force.
 *
 * <p><b>WHAT THE ESTIMATE ASSUMES, STATED:</b> one move packet a server tick, and a packet that reaches him a tick or
 * less after it was sent (a local server); no wall or step in his last tick's path (a collision zeroes that axis on
 * the client, which a zero displacement also gives). <b>INFERRED, not read in game.</b> A displacement longer than
 * {@link #MAX_STEP} is a teleport, not motion: no estimate that tick.
 *
 * <p>Thread-safe by {@code synchronized}: each drag runs on its origin's region thread. Entries older than two ticks
 * are pruned by the drags themselves, so a body nobody drags is forgotten.
 */
public final class DragMotion {

    /** BLOCKS: a displacement longer than this in one tick is a teleport. CHOSEN (2026-11-18): over three sprints. */
    public static final double MAX_STEP = 1.0;

    private record Seen(long tick, Vec3 feet, boolean onGround, float friction) {}

    private static final class Track {
        Seen prev;
        Seen last;
        long tallyTick = Long.MIN_VALUE;
        Vec3 base = Vec3.ZERO;
        Vec3 sum = Vec3.ZERO;
    }

    private final Map<UUID, Track> tracks = new HashMap<>();

    /** Where {@code id} stands at {@code tick}. A second sighting in the same tick (a second drag) changes nothing. */
    public synchronized void observe(UUID id, long tick, Implosion.Footing f) {
        Track t = tracks.computeIfAbsent(id, k -> new Track());
        if (t.last != null && t.last.tick() == tick) return;
        t.prev = t.last;
        t.last = new Seen(tick, f.feet(), f.onGround(), f.blockFriction());
    }

    /**
     * The momentum {@code id} carries into the tick after {@code tick}, estimated from its motion between the last tick
     * and this one (class javadoc); empty when it was not seen in BOTH, or it moved farther than {@link #MAX_STEP}.
     */
    public synchronized Optional<Vec3> momentum(UUID id, long tick) {
        Track t = tracks.get(id);
        if (t == null || t.last == null || t.prev == null || t.last.tick() != tick || t.prev.tick() != tick - 1) {
            return Optional.empty();
        }
        Vec3 d = t.last.feet().subtract(t.prev.feet());
        if (d.lengthSquared() > MAX_STEP * MAX_STEP) return Optional.empty();
        double m = Implosion.tickMultiplier(t.prev.onGround(), t.prev.friction());
        return Optional.of(new Vec3(d.x() * m, (d.y() - Implosion.GRAVITY) * Implosion.VERTICAL_DRAG, d.z() * m));
    }

    /**
     * ADD one drag's {@code add} to {@code id}'s tally for {@code tick} and return the velocity to SET now: the
     * {@code base} the FIRST drag of the tick named (every drag of one tick estimates the same base), plus every add of
     * the tick so far. The last {@code setVelocity} of the tick therefore carries them all.
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

    /** Forget every body last seen before {@code tick - 2}. */
    public synchronized void prune(long tick) {
        tracks.values().removeIf(t -> t.last == null || t.last.tick() < tick - 2);
    }

    /** How many bodies are remembered: a test's window on {@link #prune}. */
    public synchronized int size() {
        return tracks.size();
    }
}
