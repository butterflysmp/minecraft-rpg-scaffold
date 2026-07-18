package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.ChunkTraversal;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CombatantHandle;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import java.util.*;

/**
 * A whole Minecraft world, in a hundred lines, with no Minecraft.
 *
 * The scheduler is a clock, not a queue. An earlier version appended every task to a
 * FIFO deque and threw delayTicks away, which made an instantaneous effect and one
 * delayed by a second indistinguishable to every test in the suite. Timing bugs shipped
 * because nothing could see time.
 */
public final class FakeWorld implements CombatWorld {

    /** Trips on a task that reschedules itself at a rate the clock can never outrun. */
    private static final int MAX_TASKS_PER_ADVANCE = 100_000;

    public final List<Dummy> entities = new ArrayList<>();
    public final List<String> presented = new ArrayList<>();

    /** The start point of every castRay -- a projectile's launch origin is its first one. */
    public final List<Vec3> castRayFrom = new ArrayList<>();

    /** Live display markers, id -> marker material id. A marker leak shows up as a stale entry. */
    public final Map<UUID, String> markers = new HashMap<>();

    /** Where each live marker sits, id -> position. Parallel to {@link #markers}, so the leak
     *  assertions on that map stay untouched while a fuse can read a marker's live position. */
    public final Map<UUID, Vec3> markerPositions = new HashMap<>();

    private record Scheduled(long dueTick, long seq, Runnable task) {}

    /** Ordered by due time, then insertion, so a tie runs in the order it was queued. */
    private final PriorityQueue<Scheduled> queue = new PriorityQueue<>(
            Comparator.comparingLong(Scheduled::dueTick).thenComparingLong(Scheduled::seq));

    private long now = 0L;
    private long seq = 0L;

    /** How close a ray must pass to a combatant to strike it. A hitbox, roughly. */
    public double hitRadius = 0.6;

    /**
     * A wall this far along EVERY segment, measured from that segment's start.
     * Convenient for "there is a block right in front of you". Infinity is open sky.
     */
    public double blockDistance = Double.POSITIVE_INFINITY;

    /**
     * A wall standing at this x coordinate. Unlike blockDistance this is fixed in
     * the world, so a projectile can fly toward it for several ticks and then hit
     * it. Infinity is open sky.
     */
    public double wallX = Double.POSITIVE_INFINITY;

    /** The snapshot is taken HERE, when the entity is found -- as the real adapter does. */
    private static Combatant pair(Dummy dummy) {
        return new Combatant(dummy.snapshot(), dummy);
    }

    @Override public Collection<Combatant> combatantsNear(Vec3 center, double radius) {
        double r2 = radius * radius;
        return entities.stream()
                .filter(c -> c.position().distanceSquared(center) <= r2)
                .map(FakeWorld::pair)
                .toList();
    }

    @Override public Optional<Combatant> combatant(UUID id) {
        return entities.stream()
                .filter(d -> d.id().equals(id))
                .findFirst()
                .map(FakeWorld::pair);
    }

    /** Identifies a chunk column. Two coordinates packed into one key. */
    private static long columnKey(Vec3 at) {
        return ((long) ChunkTraversal.columnOf(at.x()) << 32)
                ^ (ChunkTraversal.columnOf(at.z()) & 0xffffffffL);
    }

    /**
     * Every chunk column this segment passes through, found by walking its own chunk-plane
     * crossings and taking the midpoint of each piece.
     */
    private static Set<Long> columnsAlong(Vec3 from, Vec3 direction, double length) {
        Set<Long> columns = new HashSet<>();
        Vec3 previous = from;
        for (Vec3 end : ChunkTraversal.segmentEndpoints(from, direction, length)) {
            Vec3 midpoint = previous.add(end.subtract(previous).scale(0.5));
            columns.add(columnKey(midpoint));
            previous = end;
        }
        return columns;
    }

    /**
     * Nearest combatant whose centre lies within hitRadius of the segment, or the
     * wall at blockDistance, whichever comes first.
     *
     * Only entities whose CENTRE lies in a chunk column this segment actually passes
     * through are visible -- because on a real server a trace only sees entities in the
     * chunks it reads, and those chunks belong to the region running the trace.
     *
     * This makes a real defect observable: an entity centred just across a plane, whose
     * hitbox reaches into a column the ray does walk, is missed. Before this the fake
     * scanned every entity in the world, which is more permissive than any server -- the
     * same shape of lie as the old scheduler discarding delayTicks, and the reason a
     * one-second bug once shipped past a green suite.
     */
    @Override public Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId) {
        castRayFrom.add(from);
        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length == 0) return Optional.empty();
        Vec3 direction = along.scale(1 / length);

        Set<Long> visibleColumns = columnsAlong(from, direction, length);

        Dummy nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;

        for (Dummy candidate : entities) {
            if (candidate.id().equals(ignoreId)) continue;
            if (!visibleColumns.contains(columnKey(candidate.position()))) continue;

            Vec3 toCandidate = candidate.position().subtract(from);
            double projected = toCandidate.dot(direction);       // distance along the ray
            if (projected < 0 || projected > length) continue;   // behind us, or past the end

            double perpendicularSquared = toCandidate.lengthSquared() - projected * projected;
            if (perpendicularSquared > hitRadius * hitRadius) continue; // ray passes wide

            if (projected < nearestDistance) {
                nearestDistance = projected;
                nearest = candidate;
            }
        }

        // Distance along this segment at which we meet a wall, if any.
        double wallAt = blockDistance;
        if (Double.isFinite(wallX) && direction.x() != 0) {
            double toPlane = (wallX - from.x()) / direction.x();
            if (toPlane >= 0) wallAt = Math.min(wallAt, toPlane);
        }

        if (wallAt <= Math.min(nearestDistance, length)) {
            return Optional.of(RayHit.ofBlock(from.add(direction.scale(wallAt))));
        }
        if (nearest == null) return Optional.empty();
        return Optional.of(RayHit.ofCombatant(nearest.position(), pair(nearest)));
    }

    /**
     * Stricter than production on purpose. PaperScheduler.onRegionLater clamps a delay of
     * 0 UP to 1 tick, so a fake that ran such a task on the current frame would be more
     * permissive than the server -- the one direction a fake must never be wrong in, since
     * the resulting bug exists only where no test can reach it. Refuse instead.
     */
    @Override public void schedule(Vec3 near, int delayTicks, Runnable task) {
        if (delayTicks < 1) {
            throw new IllegalArgumentException(
                    "schedule requires delayTicks >= 1, got " + delayTicks
                            + "; to act on the current frame, act inline");
        }
        queue.add(new Scheduled(now + delayTicks, seq++, task));
    }

    @Override public void present(Vec3 at, String visualId) { presented.add(visualId); }

    @Override public UUID throwMarker(Vec3 origin, Vec3 velocity, String itemId) {
        UUID id = UUID.randomUUID();
        markers.put(id, itemId);
        // No physics here: the item is recorded at its launch origin. A test that needs to
        // exercise where it LANDS drifts it with moveMarker, standing in for vanilla flight.
        markerPositions.put(id, origin);
        return id;
    }

    @Override public void removeMarker(UUID markerId) {
        markers.remove(markerId);
        markerPositions.remove(markerId);
    }

    @Override public Optional<Vec3> markerLocation(UUID markerId) {
        return Optional.ofNullable(markerPositions.get(markerId));
    }

    /** Drift a marker to a new spot, so a test can prove a fuse detonates at the LIVE
     *  position, not where the marker was planted. The production analogue is a marker
     *  that pops or falls before the fuse fires. */
    public void moveMarker(UUID markerId, Vec3 to) { markerPositions.put(markerId, to); }

    /**
     * Run the clock forward, firing every task that comes due. Deterministic: no sleeping,
     * no flakiness.
     *
     * A task scheduled with delay 1 while tick t is running lands at t+1, not t, because
     * `now` already equals the running task's own due tick. That is what makes a projectile
     * fly one block per tick instead of teleporting.
     */
    public void advanceTicks(int ticks) {
        long target = now + ticks;
        int ran = 0;
        while (!queue.isEmpty() && queue.peek().dueTick() <= target) {
            if (++ran > MAX_TASKS_PER_ADVANCE) {
                throw new IllegalStateException("runaway scheduling: more than "
                        + MAX_TASKS_PER_ADVANCE + " tasks in one advance");
            }
            Scheduled next = queue.poll();
            now = next.dueTick(); // never moves backward; the queue pops in due order
            next.task().run();
        }
        now = target;
    }

    public int pendingTasks() { return queue.size(); }

    public long now() { return now; }

    /**
     * A combatant that can be acted on. It is the HANDLE half of the port; its snapshot is
     * taken by the world at the moment it is found, exactly as the Paper adapter does.
     */
    public static final class Dummy implements CombatantHandle {
        private final UUID id = UUID.randomUUID();
        private Vec3 pos;
        public double health = 100;
        public final List<String> statuses = new ArrayList<>();

        /** A player is spared mob-only effects; a mob is not. Defaults to mob. */
        public boolean player = false;

        /** The last velocity a dash impulse set on this dummy, or null if never dashed. */
        public Vec3 lastImpulse;

        /** Who last damaged this dummy. Null means the damage was unattributed. */
        public UUID lastDamageSource;

        /** How many times damage arrived through the one port -- so a second, seam-skipping route shows up. */
        public int damageCalls;

        /** The last knockback pushed onto this dummy, and how many landed. Knockback is DECLARED, not
         *  default: with no {@code EffectSpec.Knockback} the count stays 0. */
        public Vec3 lastKnockbackDir;
        public double lastKnockbackStrength = Double.NaN;
        public int knockbackCalls;

        public Dummy(Vec3 pos) { this.pos = pos; }

        public void moveTo(Vec3 to) { this.pos = to; }

        public Vec3 position() { return pos; }

        public CombatantSnapshot snapshot() {
            return new CombatantSnapshot(id, pos, health > 0, player);
        }

        @Override public UUID id() { return id; }
        @Override public void applyDamage(double amount, UUID sourceId) {
            health -= amount;
            lastDamageSource = sourceId;
            damageCalls++;
        }
        @Override public void applyHeal(double a) { health += a; }
        @Override public void applyKnockback(Vec3 d, double s) {
            lastKnockbackDir = d;
            lastKnockbackStrength = s;
            knockbackCalls++;
        }
        @Override public void applyImpulse(Vec3 velocity) { this.lastImpulse = velocity; }
        @Override public void applyStatus(String id, int dur, int amp) { statuses.add(id); }
    }
}
