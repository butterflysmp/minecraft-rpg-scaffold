package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AttackSpeed;
import io.github.butterflysmp.rpg.core.combat.AccrualRule;
import io.github.butterflysmp.rpg.core.combat.HitAccrual;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Caster;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;
import io.github.butterflysmp.rpg.core.combat.ChunkTraversal;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CombatantHandle;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.RayHit;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
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

    /** WHERE each of those was presented, parallel to {@link #presented}, one entry per call.
     *  It exists because a COUNT cannot see a visual drawn at the wrong PLACE: a projectile trail
     *  that draws on the launch frame puts its first puff inside the shooter's eye and still
     *  presents exactly once per tick. The count is identical; only the position tells them
     *  apart. */
    public final List<Vec3> presentedAt = new ArrayList<>();

    /** The start point of every castRay -- a projectile's launch origin is its first one. */
    public final List<Vec3> castRayFrom = new ArrayList<>();

    /** Both ends of every lineOfSightClear call, in order. Parallel lists, one entry per trace.
     *  sightCheckFrom proves melee traces from the AIM ORIGIN -- the eye -- and not the caster's
     *  feet. sightCheckTo proves it traces to a point ON the target and not to the ground under
     *  it, which is the entire ambiguity of the eye-height offset and the thing most likely to be
     *  quietly dropped later. Never assert on their SIZE: the number of traces depends on which
     *  candidates improve on the running nearest, and so on iteration order. */
    public final List<Vec3> sightCheckFrom = new ArrayList<>();
    public final List<Vec3> sightCheckTo = new ArrayList<>();

    /** Per-target occlusion on top of the wall model: sight to a (from, to) this accepts is
     *  blocked. A BiPredicate is a two-argument function returning a boolean. Matching on the
     *  endpoint rather than storing exact Vec3s keeps a test from having to reconstruct whatever
     *  eye offset core computes. */
    public java.util.function.BiPredicate<Vec3, Vec3> sightBlocked = (from, to) -> false;

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

    /**
     * When true, {@link #combatantsNear} answers as the SERVER does: a BOX of half-extent {@code radius} on every axis
     * (READ: {@code PaperCombatWorld.combatantsNear} is {@code World#getNearbyEntities(center, r, r, r)}), not the sphere
     * this fixture uses by default. A caller that needs a sphere must cut one itself, and only this mode can show it does.
     */
    public boolean boxSearch = false;

    @Override public Collection<Combatant> combatantsNear(Vec3 center, double radius) {
        double r2 = radius * radius;
        return entities.stream()
                .filter(c -> boxSearch
                        ? Math.abs(c.position().x() - center.x()) <= radius
                                && Math.abs(c.position().y() - center.y()) <= radius
                                && Math.abs(c.position().z() - center.z()) <= radius
                        : c.position().distanceSquared(center) <= r2)
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

        double wallAt = wallDistanceAlong(from, direction);

        if (wallAt <= Math.min(nearestDistance, length)) {
            return Optional.of(RayHit.ofBlock(from.add(direction.scale(wallAt))));
        }
        if (nearest == null) return Optional.empty();
        return Optional.of(RayHit.ofCombatant(nearest.position(), pair(nearest)));
    }

    /** The start of every sweepRay -- a sweeping ray's segments, in order. */
    public final List<Vec3> sweepFrom = new ArrayList<>();

    /**
     * The piercing / tolerant ray's port (2026-11-21, the Knell Beam). Every live dummy, the ignored one excluded, whose
     * standing box (its {@code width} x {@code height}) grown by {@code reach} plus a block of slack comes near the
     * segment -- deliberately WITHOUT the column filter castRay uses, so a body near a chunk plane is reported by BOTH
     * segments, which is what the struck-once set across a hop must survive. And where the segment meets the shared wall
     * model, if it does.
     */
    @Override public io.github.butterflysmp.rpg.core.combat.RaySweep sweepRay(Vec3 from, Vec3 to, UUID ignoreId,
                                                                             double reach) {
        sweepFrom.add(from);
        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length == 0) return new io.github.butterflysmp.rpg.core.combat.RaySweep(List.of(), Optional.empty());
        Vec3 direction = along.scale(1 / length);
        var line = new io.github.butterflysmp.rpg.core.mob.BeamLine(from, direction, length);
        List<io.github.butterflysmp.rpg.core.combat.RaySweep.Body> near = new ArrayList<>();
        for (Dummy d : entities) {
            if (d.id().equals(ignoreId) || d.health <= 0) continue;
            var box = io.github.butterflysmp.rpg.core.mob.BeamBox.standing(d.position(), d.width, d.height);
            if (!line.meets(box, reach + 1.0)) continue;
            near.add(new io.github.butterflysmp.rpg.core.combat.RaySweep.Body(pair(d), box));
        }
        double wallAt = wallDistanceAlong(from, direction);
        Optional<Vec3> blockAt = wallAt < length ? Optional.of(from.add(direction.scale(wallAt))) : Optional.empty();
        return new io.github.butterflysmp.rpg.core.combat.RaySweep(near, blockAt);
    }

    /**
     * How far along {@code direction} from {@code from} this segment meets a wall, or
     * infinity for open sky. The shared block model behind both castRay and
     * lineOfSightClear, so the two can never disagree about where the walls are.
     */
    private double wallDistanceAlong(Vec3 from, Vec3 direction) {
        double wallAt = blockDistance;
        if (Double.isFinite(wallX) && direction.x() != 0) {
            double toPlane = (wallX - from.x()) / direction.x();
            if (toPlane >= 0) wallAt = Math.min(wallAt, toPlane);
        }
        return wallAt;
    }

    /**
     * Blocks only, per the port: entities never occlude, so a dummy standing between two
     * others is not a wall.
     *
     * The default is NOT "always clear". It is the same wall model castRay uses, minus the
     * entities -- so setting blockDistance or wallX blocks sight exactly where it already
     * stops a ray. An always-clear default would let a test set wallX, watch castRay honour
     * it, and watch melee walk straight through the same wall: a fake more permissive than
     * the server, which is the one direction this file must never be wrong in.
     *
     * {@link #sightBlocked} is the per-TARGET knob on top of that, and it is needed because
     * neither existing knob can express one. blockDistance is uniform along every segment,
     * and wallX is an infinite plane -- it can only occlude the FARTHER of two candidates,
     * which is the direction that proves nothing.
     */
    @Override public boolean lineOfSightClear(Vec3 from, Vec3 to) {
        sightCheckFrom.add(from);
        sightCheckTo.add(to);
        if (sightBlocked.test(from, to)) return false;

        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length == 0) return true;
        return wallDistanceAlong(from, along.scale(1 / length)) > length;
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

    /** {@link #schedule} with a handle: its sequence number, which {@link #cancelScheduled} takes OUT of the queue. */
    @Override public long scheduleCancellable(Vec3 near, int delayTicks, Runnable task) {
        long handle = seq;
        schedule(near, delayTicks, task);
        return handle;
    }

    /** Every handle a cancel named, in order: what a removal ENDED, which a pending count cannot tell from a task that
     *  stopped itself (the crystal's look beat does both). */
    public final List<Long> cancelledHandles = new ArrayList<>();

    @Override public void cancelScheduled(long handle) {
        cancelledHandles.add(handle);
        queue.removeIf(s -> s.seq() == handle);
    }

    /**
     * The entity twin of {@link #schedule}, faked at the SAME fidelity and with one extra rule.
     *
     * <p>{@code PaperScheduler.onEntityLater} passes a null retired-runnable, so a task whose
     * entity is gone when the delay elapses simply NEVER RUNS. This reproduces that: the id is
     * captured, and the task is dropped at fire time if no dummy with that id is in the world.
     * Without it a volley whose caster logged out would keep firing in every test and never in
     * production -- a fake more permissive than the server, the one direction this file must
     * never be wrong in.
     */
    @Override public void scheduleOn(UUID combatantId, int delayTicks, Runnable task) {
        if (delayTicks < 1) {
            throw new IllegalArgumentException(
                    "scheduleOn requires delayTicks >= 1, got " + delayTicks
                            + "; to act on the current frame, act inline");
        }
        queue.add(new Scheduled(now + delayTicks, seq++, () -> {
            if (combatant(combatantId).isEmpty()) return;   // the entity went; the task does not run
            task.run();
        }));
    }

    @Override public Optional<Aim> aimOf(UUID combatantId) {
        return entities.stream()
                .filter(d -> d.id().equals(combatantId))
                .findFirst()
                .map(d -> new Aim(d.position().add(new Vec3(0, d.eyeHeight, 0)), d.facing));
    }

    /**
     * The held weapon's gear score, read LIVE off the dummy -- which is what lets a volley test move
     * it between shots and prove the later shots re-price.
     *
     * <p>An id this world does not know answers {@link GearScore#BASELINE}, which is the port's own
     * stated total-function answer rather than a convenience of this fixture.
     */
    @Override public int triggerScoreOf(UUID combatantId) {
        return entities.stream()
                .filter(d -> d.id().equals(combatantId))
                .findFirst()
                .map(d -> d.triggerScore)
                .orElse(GearScore.BASELINE);
    }

    /** The held weapon's active behaviours, read LIVE off the dummy, as {@link #triggerScoreOf} reads its score. */
    @Override public List<io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour> behavioursOf(UUID combatantId) {
        return entities.stream()
                .filter(d -> d.id().equals(combatantId))
                .findFirst()
                .map(d -> d.behaviours)
                .orElse(List.of());
    }

    /** This world's one long-lived behaviour executor, as paper keeps one per server. */
    public final io.github.butterflysmp.rpg.core.ability.effect.BehaviourHits behaviourHits =
            new io.github.butterflysmp.rpg.core.ability.effect.BehaviourHits();

    /** Every primary hit the applier reported, in order: who struck, and whether it counted. */
    public record PrimaryHit(io.github.butterflysmp.rpg.core.combat.Caster caster, UUID targetId) {}
    public final List<PrimaryHit> primaryHits = new ArrayList<>();

    @Override public void primaryHit(io.github.butterflysmp.rpg.core.combat.Caster caster, Combatant target) {
        primaryHits.add(new PrimaryHit(caster, target.id()));
        behaviourHits.onHit(this, caster, target);
    }

    @Override public long currentTick() {
        return now;
    }

    /** This world's one orb registry, as paper keeps one per server (D2). */
    public final io.github.butterflysmp.rpg.core.combat.Orbs orbs = new io.github.butterflysmp.rpg.core.combat.Orbs();

    @Override public io.github.butterflysmp.rpg.core.combat.Orbs orbs() {
        return orbs;
    }

    /** Orb bodies spawned, a subset of the markers. */
    public final List<UUID> orbMarkers = new ArrayList<>();

    @Override public UUID spawnOrbMarker(Vec3 at, String itemId, int expectedLifetimeTicks) {
        UUID id = spawnMarker(at, itemId, expectedLifetimeTicks);
        orbMarkers.add(id);
        entityAdded(id);
        return id;
    }

    /**
     * THE ENTITY-ADD MOMENT (the seat's fix of #265, 2026-11-23). On the server a spawned body is ADDED to the world inside
     * the spawn, before it returns, and the orphan sweep runs there (READ: {@code CraftRegionAccessor.addEntity} runs the
     * consumer, then {@code addEntityToWorld}, whose {@code onTrackingStart} raises {@code EntityAddToWorldEvent}). This is
     * the sweep's own test at that moment: a body carrying the orb tag that no orb owns is an orphan.
     */
    public final java.util.Set<UUID> orbTagged = new java.util.HashSet<>();
    public final List<UUID> sweptAtAdd = new ArrayList<>();
    /** Bodies claimed (tagged) while no orb owned them: what the run of 78826389 did, every throw. */
    public final List<UUID> claimedUnowned = new ArrayList<>();

    public void entityAdded(UUID id) {
        if (orbTagged.contains(id) && !orbs.ownsMarker(id)) sweptAtAdd.add(id);
    }

    /** Every pose handed to {@link #poseOrbBody}: the body, the tick, the pose, the interpolation. */
    public record OrbPose(UUID body, long tick, io.github.butterflysmp.rpg.core.ability.CastSpec.OrbBlock.Pose pose,
                          int interpolationTicks) {}
    public final List<OrbPose> orbPoses = new ArrayList<>();

    @Override public void poseOrbBody(UUID bodyId, io.github.butterflysmp.rpg.core.ability.CastSpec.OrbBlock.Pose pose,
                                      int interpolationTicks) {
        if (!markerPositions.containsKey(bodyId)) return;
        orbPoses.add(new OrbPose(bodyId, now, pose, interpolationTicks));
    }

    @Override public void claimOrbBody(UUID bodyId) {
        if (!orbs.ownsMarker(bodyId)) claimedUnowned.add(bodyId);
        orbTagged.add(bodyId);
    }

    /** Block-display orb bodies spawned (Ben's item 3), id -> the look it was given; also in {@link #orbMarkers}. */
    public final Map<UUID, io.github.butterflysmp.rpg.core.ability.CastSpec.OrbBlock> orbBlocks = new HashMap<>();

    @Override public UUID spawnOrbBlock(UUID ownerId, Vec3 at, io.github.butterflysmp.rpg.core.ability.CastSpec.OrbBlock look) {
        UUID id = spawnMarker(at, "block:" + look.block(), 0);
        orbMarkers.add(id);
        orbBlocks.put(id, look);
        entityAdded(id);
        return id;
    }

    /** Every placement handed to {@link #placeOrbBody}, id -> the points in order. */
    public final Map<UUID, List<Vec3>> orbBodyPlacements = new HashMap<>();

    @Override public void placeOrbBody(UUID bodyId, Vec3 at) {
        // Absent is a no-op, as in production (a removed display is not found by its id).
        if (!markerPositions.containsKey(bodyId)) return;
        orbBodyPlacements.computeIfAbsent(bodyId, k -> new ArrayList<>()).add(at);
        markerPositions.put(bodyId, at);
    }

    /** Every ORB trace line, in order. */
    public record OrbLine(UUID owner, String event, Vec3 at, String cause, long tick) {}
    public final List<OrbLine> orbLines = new ArrayList<>();

    @Override public void traceOrb(UUID ownerId, String event, Vec3 at, String cause) {
        orbLines.add(new OrbLine(ownerId, event, at, cause, now));
    }

    /** Every ORBSHOT line, in order: one per line drawn from an orb to a struck mob. */
    public record OrbShot(UUID owner, Vec3 from, Vec3 to, String visual) {}
    public final List<OrbShot> orbShots = new ArrayList<>();

    @Override public void traceOrbShot(UUID ownerId, Vec3 from, Vec3 to, String visual) {
        orbShots.add(new OrbShot(ownerId, from, to, visual));
    }

    /** Every split the cast path reported, in order. */
    public record Split(UUID casterId, String source, Vec3 at, int bursts, long tick) {}
    public final List<Split> splits = new ArrayList<>();

    /** Every Focused Beam fired (2026-11-24): who, what, from where, how far, when pressed and when fired. */
    public record FocusedBeamTrace(UUID casterId, String source, Vec3 from, double range, long pressedTick, long tick) {}
    public final List<FocusedBeamTrace> focusedBeams = new ArrayList<>();

    @Override public void traceFocusedBeam(UUID casterId, String source, Vec3 from, double range, long pressedTick) {
        focusedBeams.add(new FocusedBeamTrace(casterId, source, from, range, pressedTick, now));
    }

    /** Every volley shot fired (2026-11-24, Early Toll): who, what, which shot, when pressed and when fired. */
    public record VolleyShotTrace(UUID casterId, String source, int shotIndex, long pressedTick, long tick) {}
    public final List<VolleyShotTrace> volleyShots = new ArrayList<>();

    @Override public void traceVolleyShot(UUID casterId, String source, int shotIndex, long pressedTick) {
        volleyShots.add(new VolleyShotTrace(casterId, source, shotIndex, pressedTick, now));
    }

    @Override public void traceSplit(UUID casterId, String source, Vec3 at, int bursts) {
        splits.add(new Split(casterId, source, at, bursts, now));
    }

    /** Every hit the applier reported to the PLAYERHIT trace, in order. */
    public final List<io.github.butterflysmp.rpg.core.combat.TracedHit> traced = new ArrayList<>();

    @Override public void traceHit(io.github.butterflysmp.rpg.core.combat.TracedHit hit) {
        traced.add(hit);
    }

    /** One slamming leap's ending, as the LANDING trace would print it. {@code feet} null for a non-slam. */
    public record TracedLanding(UUID casterId, String source, io.github.butterflysmp.rpg.core.build.Touchdown how,
                                Vec3 feet) {}

    /** Every landing reported to the LANDING trace, in order. */
    public final List<TracedLanding> landings = new ArrayList<>();

    @Override public void traceLanding(UUID casterId, String source, io.github.butterflysmp.rpg.core.build.Touchdown how,
                                       Vec3 feet) {
        landings.add(new TracedLanding(casterId, source, how, feet));
    }

    /** Each drag handed to the world (the implosion's drag, 2026-11-18), as it was handed. */
    public record StartedDrag(Vec3 origin, io.github.butterflysmp.rpg.core.shape.Implosion shape,
                              io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Affects affects,
                              UUID casterId) {}

    public final List<StartedDrag> drags = new ArrayList<>();

    @Override public void startDrag(Vec3 origin, io.github.butterflysmp.rpg.core.shape.Implosion shape,
                                    io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Affects affects,
                                    UUID casterId) {
        drags.add(new StartedDrag(origin, shape, affects, casterId));
    }

    /** The radius each scaled presentation was handed (the void implosion's spiral); unscaled ones add nothing. */
    public final List<Double> presentedRadius = new ArrayList<>();

    @Override public void present(Vec3 at, String visualId, double radius) {
        presentedRadius.add(radius);
        present(at, visualId);
    }

    @Override public void present(Vec3 at, String visualId) {
        presented.add(visualId);
        presentedAt.add(at);
    }

    /** One beam segment: both ends and the visual, exactly as the port was handed them. */
    public record Beam(Vec3 from, Vec3 to, String visualId) {}

    /**
     * Every presentAlong call, in order.
     *
     * <p>Deliberately NOT merged into {@link #presented}. A beam and a point visual are different
     * geometry, and a test that could not tell them apart would pass on a beam drawn as a puff --
     * which is the exact mistake the mechanism exists to avoid.
     *
     * <p>And it records BOTH ENDS for the reason {@link #presentedAt} exists at all: a COUNT is
     * structurally blind to a beam that overshoots. A ray stopped by a wall halfway down its
     * segment draws exactly one beam whether it stops at the wall or carries on thirteen blocks
     * through it. Only the endpoint tells them apart.
     */
    public final List<Beam> presentedAlong = new ArrayList<>();

    @Override public void presentAlong(Vec3 from, Vec3 to, String visualId) {
        presentedAlong.add(new Beam(from, to, visualId));
    }

    @Override public UUID throwMarker(Vec3 origin, Vec3 velocity, String itemId) {
        UUID id = UUID.randomUUID();
        markers.put(id, itemId);
        // No physics here: the item is recorded at its launch origin. A test that needs to
        // exercise where it LANDS drifts it with moveMarker, standing in for vanilla flight.
        markerPositions.put(id, origin);
        return id;
    }

    /** Where each marker was when it was REMOVED, id -> position. The live position map is erased
     *  on removal, so without this a test cannot ask the question that matters for a projectile
     *  body: did the flint vanish AT the burst, or a stride short of it? */
    public final Map<UUID, Vec3> markerRemovedAt = new HashMap<>();

    @Override public void removeMarker(UUID markerId) {
        if (markerPositions.containsKey(markerId)) {
            markerRemovedAt.put(markerId, markerPositions.get(markerId));
        }
        markers.remove(markerId);
        markerPositions.remove(markerId);
    }

    @Override public Optional<Vec3> markerLocation(UUID markerId) {
        return Optional.ofNullable(markerPositions.get(markerId));
    }

    /** Where each marker was SPAWNED, id -> position, kept even after the marker is removed.
     *  Separate from {@link #markerPositions}, which is erased on removal: a test that asks "was
     *  the body created at the caster's eye on the launch frame" still needs an answer after the
     *  bolt has resolved and cleaned itself up. */
    public final Map<UUID, Vec3> markerSpawnedAt = new HashMap<>();

    /** Every marker id ever spawned, in order, INCLUDING removed ones -- so a test can tell
     *  "one marker, correctly cleaned up" from "no marker was ever created", which the live
     *  {@link #markers} map cannot: both leave it empty. */
    public final List<UUID> markersEverSpawned = new ArrayList<>();

    /** Every item marker's item, id -> item, KEPT after removal (Shatter Caps, 2026-11-24: a row asks which bodies a press
     *  made after they have landed, which the live {@link #markers} map cannot). */
    public final Map<UUID, String> markerItemsEver = new HashMap<>();

    @Override public UUID spawnMarker(Vec3 at, String itemId, int expectedLifetimeTicks) {
        UUID id = UUID.randomUUID();
        markerItemsEver.put(id, itemId);
        markers.put(id, itemId);
        markerPositions.put(id, at);
        markerSpawnedAt.put(id, at);
        markersEverSpawned.add(id);
        return id;
    }

    /** Every arrow body ever spawned, id -> the LAUNCH VELOCITY it was created with.
     *
     *  <p><b>The velocity is recorded because it is the whole reason this port method exists.</b>
     *  An arrow points along its own velocity, so a body spawned still has no direction on its
     *  first frame; a test that only counted bodies would pass under a three-argument sibling that
     *  threw the velocity away. Keyed separately from {@link #markers} so a row can assert WHICH
     *  kind of body was made, which a shared map could not. */
    public final Map<UUID, Vec3> boltMarkerLaunchVelocities = new HashMap<>();

    /** Every body ever spawned through {@link #spawnBoltMarker}, id -> the body id it was asked for
     *  ({@code arrow}, {@code fireball}). Kept after removal, like {@link #markerSpawnedAt}, so a row
     *  can ask which KIND reached the port after the bolt has resolved. */
    public final Map<UUID, String> boltMarkerBodies = new HashMap<>();

    @Override public UUID spawnBoltMarker(Vec3 at, Vec3 velocity, int expectedLifetimeTicks,
                                          String body) {
        UUID id = UUID.randomUUID();
        boltMarkerLaunchVelocities.put(id, velocity);
        boltMarkerBodies.put(id, body);
        markerPositions.put(id, at);
        markerSpawnedAt.put(id, at);
        markersEverSpawned.add(id);
        return id;
    }

    /** Drift a marker to a new spot -- a TEST HELPER again, not a port method. The flight drives
     *  its body by velocity now; this remains because throw_embers tests need to prove a fuse
     *  detonates at the LIVE position rather than where the marker was planted. */
    public void moveMarker(UUID markerId, Vec3 to) {
        if (markerPositions.containsKey(markerId)) markerPositions.put(markerId, to);
    }

    /** Every velocity handed to {@link #driveMarker}, id -> the vectors in order. The flight sets
     *  one per tick, so this is the computed path as the platform would have been asked to walk it. */
    public final Map<UUID, List<Vec3>> markerVelocities = new HashMap<>();

    @Override public void driveMarker(UUID markerId, Vec3 stepVelocity) {
        // Absent is a no-op, exactly as in production, where fire or lava may already have
        // destroyed the body mid-flight.
        if (!markerPositions.containsKey(markerId)) return;
        markerVelocities.computeIfAbsent(markerId, k -> new ArrayList<>()).add(stepVelocity);
        // The fake stands in for vanilla move(): the body is displaced by the velocity it was given.
        markerPositions.put(markerId, markerPositions.get(markerId).add(stepVelocity));
    }

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

        /**
         * The attack-speed multiplier this dummy's snapshot carries. Defaults to neutral, so every
         * existing test's cadence is unchanged; a cooldown-scaling test sets it.
         */
        public double attackSpeed = AttackSpeed.BASE;

        /**
         * The attack damage this dummy's snapshot carries -- what a WeaponDamage payload it casts
         * will deal, frozen at cast time. Defaults to 0 (untracked/unarmed), matching the real
         * store, so an existing test that never sets it still sees a caster who deals nothing.
         *
         * It lives on the DUMMY, not on the world, on purpose: the amount a payload uses is the one
         * captured when the cast began, so a test can change this MID-FLIGHT and prove a projectile
         * still lands the frozen value rather than a fresh read.
         */
        public double attackDamage = 0.0;

        /**
         * The class-damage bonus this dummy's snapshot carries -- the sum of its equipped
         * "+N <Class> Damage" gear matching the class of the weapon it holds, frozen at cast time.
         * Defaults to 0 (no gear, or gear for a class it is not currently wielding), so every test
         * written before class-typed modifiers existed sees the numbers it always saw.
         *
         * On the DUMMY rather than the world, for the same reason attackDamage is: a test can change
         * it MID-FLIGHT and prove a projectile still lands the value frozen at launch.
         */
        public double classDamageBonus = 0.0;

        /**
         * The LEVEL weapon-damage bonus this dummy's snapshot carries. Defaults to 0, so every test written
         * before the level slice sees the numbers it always saw. A weapon hit adds it; a stone cast does not
         * ({@code Caster.weaponLevelDamage}).
         */
        public double levelDamageBonus = 0.0;

        /**
         * The enchant-damage PERCENT this dummy's snapshot carries -- the sum of the percentages
         * granted by the damage enchants active on the weapon it holds, gated on that weapon's own
         * class, frozen at cast time. Sharpness III is {@code 15.0}, not {@code 1.15}.
         *
         * Defaults to 0, which {@code DamageEnchants.multiplier} turns into exactly 1.0, so every
         * test written before damage enchants existed sees the numbers it always saw. That is the
         * neutral this stat was made a percent to get: a multiplier-valued field defaulting to 0.0
         * would have zeroed the damage in every one of them.
         *
         * On the DUMMY rather than the world, for the same reason attackDamage is: a test can change
         * it MID-FLIGHT and prove a projectile still lands the value frozen at launch.
         */
        public double enchantDamagePercent = 0.0;

        /**
         * The CRIT MULTIPLIER this dummy's snapshot carries -- already rolled, already decided.
         * Defaults to {@code Crit.NO_CRIT} (exactly 1.0), so every test written before crit existed
         * sees the numbers it always saw: the multiply is an exact identity at this value.
         *
         * A test that wants a crit sets it to what {@code Crit.multiplier} would have returned, which
         * is why there is no chance/roll pair here -- the roll happens in paper, and the frozen
         * multiplier is the only thing core ever sees.
         */
        public double critMultiplier = Crit.NO_CRIT;

        /**
         * The GEAR SCORE of the weapon this dummy is deemed to be holding, as {@code Caster} carries
         * it. Defaults to {@link GearScore#BASELINE}, an exact identity in
         * {@link GearScore#scaledDamage}, so every test written before trigger scaling existed reads
         * the number it always read.
         *
         * <p><b>BASELINE and not 0, even though {@code clamp} would floor 0 back to the same value.</b>
         * That floor is honest only to {@code scaledDamage}; a field holding 0 while behaving as 100
         * lies to every other reader, and a fixture that lies is the one thing a fixture must not do.
         *
         * <p>On the DUMMY rather than the world, like {@code attackDamage} and
         * {@code enchantDamagePercent} above, so a test can change it MID-FLIGHT and prove a
         * projectile still lands the value frozen at launch.
         */
        public int triggerScore = GearScore.BASELINE;

        /** The held weapon's active behaviours (B2); on the dummy so a test can change them mid-flight. */
        public List<io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour> behaviours = List.of();

        /** Whether the last applyDamage was flagged a crit. The seam bit, as the port delivered it. */
        public boolean lastDamageWasCrit = false;

        /** The applier the last applyStatus named, or null. The credit half of the widened seam. */
        public UUID lastStatusApplier;

        /** The payload damage the last applyStatus carried -- a status's basis for a CAP. */
        public double lastStatusSourceDamage = 0.0;

        /**
         * Whether the last applyDamage asked to SKIP the Defense curve.
         *
         * Recorded rather than acted on: this fake carries no defense, so honouring the flag would be
         * a no-op and a test asserting "it bypassed" would pass against a port that dropped the
         * parameter on the floor. Capturing the bit as delivered is what lets a test see the seam.
         */
        public boolean lastDamageBypassedDefense = false;

        /**
         * The ELEMENT the last applyDamage wore, or null if it wore none.
         *
         * Captured as delivered, for the same reason as the bypass bit above: this fake resolves no
         * element and applies no status, so a test asserting "fire accrued" would pass against a port
         * that dropped the parameter. The NULL case is the one that matters most -- an elementless
         * call is what makes scorch unable to accrue from its own burn tick, so a test must be able
         * to see the difference between "no element" and "some element".
         */
        public String lastDamageElement;

        /**
         * The ignite-chain DEPTH the last applyDamage carried, unwrapped from its {@code HitAccrual}.
         *
         * Recorded separately from {@link #lastDamageAccrual} rather than storing the pair, because
         * the two are asserted independently: a row about recruitment reads the rule, a row about the
         * chain limit reads the depth, and a row that had to destructure a value object to assert
         * either would read worse than one that does not. The fake keeps them apart; the PORT keeps
         * them together, which is where it matters -- see {@code HitAccrual}.
         */
        public int lastDamageDepth;

        /**
         * Whether the last applyDamage said its element may ACCRUE, or wears it for display only.
         *
         * Captured as delivered, like the bypass bit and the element beside it. This is the half
         * that used to be unassertable: while the burn tick carried NO element, the loop guard could
         * only be witnessed behaviourally, and that witness could not tell the guard from an ordinary
         * registry miss. A named value can be read back.
         */
        public AccrualRule lastDamageAccrual;

        /** The last velocity a dash impulse set on this dummy, or null if never dashed. */
        public Vec3 lastImpulse;

        /** How many times a safe-landing dash armed this dummy (section 7.6). */
        public int safeLandingArms;

        /** The slam a Sunder-shaped dash armed on this dummy, or null. A test ends the leap by calling it. */
        public io.github.butterflysmp.rpg.core.build.LandingHook landingHook;

        /** How many times a slamming dash armed this dummy. */
        public int landingArms;

        /** Who last damaged this dummy. Null means the damage was unattributed. */
        public UUID lastDamageSource;

        /** How many times damage arrived through the one port -- so a second, seam-skipping route shows up. */
        public int damageCalls;

        /** The last knockback pushed onto this dummy, and how many landed. Knockback is DECLARED, not
         *  default: with no {@code EffectSpec.Knockback} the count stays 0. */
        public Vec3 lastKnockbackDir;
        public double lastKnockbackStrength = Double.NaN;
        public int knockbackCalls;

        /** Eye height above {@link #pos} -- the point a sight line traces TO. 1.62 is a player's,
         *  the value CastExecutorTest already uses for an eye. Non-zero on purpose: the snapshot's
         *  compact constructor rejects 0, since a 0 here would silently trace sight to the feet. */
        public double eyeHeight = 1.62;
        /** A zombie's box, for sweepRay (2026-11-21): 0.6 wide, 1.95 tall. castRay keeps its own hitRadius model. */
        public double width = 0.6;
        public double height = 1.95;

        /**
         * Which way this dummy is LOOKING -- the direction half of {@link FakeWorld#aimOf}.
         *
         * <p>Defaults to +X, the same direction {@code CastExecutorTest.FORWARD} aims, so every
         * test written before a cast could re-aim sees the line it always saw. A test that wants a
         * caster to TURN mid-cast sets it between ticks; that is the only way to tell a volley
         * that re-aims from one that fires six shots down the cast frame's line.
         *
         * <p>It is a mutable field rather than a snapshot value on purpose, and for the opposite
         * reason to {@link #attackDamage}'s: that one is here so a test can prove a value stays
         * FROZEN across a flight, this one so a test can prove a value is RE-READ across a volley.
         */
        public Vec3 facing = new Vec3(1, 0, 0);

        public Dummy(Vec3 pos) { this.pos = pos; }

        public void moveTo(Vec3 to) { this.pos = to; }

        public Vec3 position() { return pos; }

        public CombatantSnapshot snapshot() {
            return new CombatantSnapshot(id, pos, eyeHeight, health > 0, player, attackSpeed, attackDamage,
                    classDamageBonus, enchantDamagePercent, critMultiplier, levelDamageBonus);
        }

        /**
         * This dummy as the SOURCE of an effect: the frozen projection an EffectApplier or a
         * projectile carries. Deliberately taken through {@link #snapshot()}, so a test cannot
         * accidentally hand an effect a value the snapshot would not have carried.
         */
        public Caster asCaster() {
            return Caster.of(snapshot()).withTriggerScore(triggerScore);
        }

        /**
         * The same projection, as a BASIC MELEE source that earned only part of its swing. Kept
         * beside asCaster() rather than exposed as a mutable field, so a test states the charge at
         * the point it builds the source and cannot leave a stale one lying on the dummy.
         */
        public Caster asCaster(double chargeScale) {
            return Caster.of(snapshot(), chargeScale).withTriggerScore(triggerScore);
        }

        @Override public UUID id() { return id; }
        @Override public void applyDamage(double amount, UUID sourceId, CritState crit,
                                          DefenseRule defense, String element,
                                          HitAccrual accrual) {
            health -= amount;
            lastDamageSource = sourceId;
            lastDamageWasCrit = crit.isCrit();
            lastDamageBypassedDefense = defense == DefenseRule.BYPASSED;
            lastDamageElement = element;
            lastDamageAccrual = accrual.rule();
            lastDamageDepth = accrual.depth();
            damageCalls++;
        }
        @Override public void applyHeal(double a) { health += a; }
        @Override public void applyKnockback(Vec3 d, double s) {
            lastKnockbackDir = d;
            lastKnockbackStrength = s;
            knockbackCalls++;
        }
        @Override public void applyImpulse(Vec3 velocity) { this.lastImpulse = velocity; }
        /** The void implosion's pull as handed over: how many, from where, which shape. The arithmetic is core's. */
        public int pullCalls;
        public Vec3 lastPullOrigin;
        public io.github.butterflysmp.rpg.core.shape.Implosion lastPullShape;
        @Override public void applyPull(Vec3 origin, io.github.butterflysmp.rpg.core.shape.Implosion shape) {
            pullCalls++;
            lastPullOrigin = origin;
            lastPullShape = shape;
        }
        @Override public void armSafeLanding() { safeLandingArms++; }
        @Override public void armLanding(io.github.butterflysmp.rpg.core.build.LandingHook hook) {
            landingHook = hook;
            landingArms++;
        }
        @Override public void applyStatus(String id, int dur, int amp, UUID applierId, double srcDmg) {
            statuses.add(id);
            lastStatusApplier = applierId;
            lastStatusSourceDamage = srcDmg;
        }
    }
}
