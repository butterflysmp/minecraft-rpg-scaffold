package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The world, as far as core cares. This is the seam between game logic and
 * the server. In production it is backed by Paper. In tests it is a HashMap.
 *
 * schedule() is the ONLY way core defers work. It never touches a scheduler
 * directly, which is what lets the Paper adapter route the callback onto the
 * correct region thread (or, one day, a Folia region thread) without core
 * knowing threads exist.
 */
public interface CombatWorld {

    /**
     * Every combatant within {@code radius} of {@code center}, each paired with a snapshot
     * read here, on this thread. Only legal on the thread owning {@code center}'s region.
     */
    Collection<Combatant> combatantsNear(Vec3 center, double radius);

    /**
     * The combatant with this id, if it is here.
     *
     * Exists for Self casts: CastResult.Success carries only an immutable snapshot of the
     * caster, so acting on the caster needs its handle fetched again on the thread that
     * owns it. The alternative -- carrying a live handle across the region hop -- is the
     * bug this port was split to prevent.
     *
     * Only legal on the thread owning that combatant.
     */
    Optional<Combatant> combatant(UUID id);

    /**
     * The first combatant or block struck by the segment from {@code from} to
     * {@code to}, ignoring {@code ignoreId} (the caster, who is standing at the
     * origin of their own ray).
     *
     * One method serves both shapes that need it: a Ray cast walks the whole
     * range at once, and a Projectile casts the short segment it travelled this
     * tick. Empty means the segment reached {@code to} unobstructed.
     *
     * Like combatantsNear, this reads the world and is only legal on the thread
     * that owns the region containing the segment.
     */
    Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId);

    /**
     * Whether the segment from {@code from} to {@code to} is clear of BLOCKS. Nothing else
     * stops it: an entity standing in the way does not.
     *
     * That is the whole reason this is not {@link #castRay}. castRay reports the first
     * block OR combatant it meets, so asking it for a sight line reads a mob standing
     * behind another mob as blocked. Melee wants block line of sight, the way vanilla
     * does -- you may hit a mob through a mob, but not through a wall.
     *
     * Like combatantsNear and castRay this reads the world, and is only legal on the
     * thread that owns the region containing the segment. Unlike castRay it can honour
     * that today: its callers trace at most a melee reach -- 3 to 3.5 blocks in shipped
     * content -- so the segment straddles at most one chunk plane and stays inside the
     * region the caster's eye already put us on. A Ray's 30 blocks do not, which is the
     * Folia defect the adapter documents on combatantsNear.
     *
     * A zero-length segment is clear: there is nothing between a point and itself.
     */
    boolean lineOfSightClear(Vec3 from, Vec3 to);

    /**
     * Defer {@code task} by at least {@code delayTicks}, on the thread owning
     * {@code near}'s region.
     *
     * {@code delayTicks} must be >= 1. There is no "schedule this for the current
     * frame": the Paper adapter clamps a delay of 0 up to 1 tick, so asking for 0
     * would quietly get you 1. To act on the current frame, act inline.
     */
    void schedule(Vec3 near, int delayTicks, Runnable task);

    /** Fire-and-forget presentation hook. Particles, sounds, damage numbers. */
    void present(Vec3 at, String visualId);

    /**
     * Draw a named visual ALONG the segment from {@code from} to {@code to}, rather than at a
     * point. A beam.
     *
     * <p><b>ONE REGION HOP FOR THE WHOLE SEGMENT, AND THE CHUNK-COLUMN WALK IS WHAT MAKES THAT
     * LEGAL.</b> {@link #present} does its own hop per call, so core walking a segment and calling
     * it N times would be N region hops per tick -- roughly a hundred, for a 26-block beam at four
     * samples per block. This is one. It is sound precisely because the only caller hands it a
     * segment bounded by chunk planes ({@link ChunkTraversal}), so the segment lies inside one
     * chunk column by construction, and a column belongs to exactly one region. <b>The architecture
     * that made the beam awkward to draw is the same one that makes drawing it this way safe.</b>
     *
     * <p>So the contract is narrower than the signature: DO NOT call this with an arbitrary
     * segment. A caller that has not confined its segment to a column must walk columns first.
     *
     * <p>Fire-and-forget like {@link #present}, and an unknown id is a content mistake rather than
     * a programming error.
     */
    void presentAlong(Vec3 from, Vec3 to, String visualId);

    /**
     * Throw a real item of material {@code itemId} from {@code origin} moving at {@code velocity},
     * and return its id. The item flies and lands under ordinary physics -- it IS the marker for
     * a thrown detonator, so no separate display entity is planted. The id, never the entity, is
     * what core keeps -- the same discipline as a caster's UUID: it outlives the frame that threw
     * it, and holding the entity would pin it.
     *
     * Only legal on the thread owning {@code origin}'s region, like every other world write.
     */
    UUID throwMarker(Vec3 origin, Vec3 velocity, String itemId);

    /**
     * Plant a marker of material {@code itemId} AT {@code at} and return its id -- inert, going
     * nowhere on its own. The caller owns its motion from here and supplies it with
     * {@link #driveMarker}.
     *
     * <p>The opposite of {@link #throwMarker} in the one way that matters: that one hands the item
     * to physics and reads back where physics took it, this one is a body rendered at a position
     * something else computed. A projectile that resolves on a traced segment cannot let physics
     * own the position, or the thing you see and the thing you hit are two different objects.
     *
     * <p><b>{@code expectedLifetimeTicks} is not decoration.</b> It is how long the caller expects
     * to need the marker, and the adapter uses it to arm a death that does NOT depend on the caller
     * ever coming back. That matters because the caller is a chain of scheduled callbacks with no
     * {@code finally}: a task scheduled into a region that unloads, or a server that stops, simply
     * does not fire, and what is left behind is a real entity that only our code removes. Every
     * other exit is the caller's job; this parameter covers the exit where there is no caller left.
     *
     * <p>Only legal on the thread owning {@code at}'s region, like every other world write.
     */
    UUID spawnMarker(Vec3 at, String itemId, int expectedLifetimeTicks);

    /**
     * Drive a marker: give it {@code stepVelocity} as this tick's motion and let the platform carry
     * it. Named for what it does -- it does NOT set a position.
     *
     * <p><b>This is the only marker movement mechanism that has been WITNESSED reaching a player.</b>
     * Its predecessor repositioned the entity outright, which was verified to work server-side (23
     * repositions, zero target/actual mismatches) and verified NOT to reach the client's entity
     * tracker (a straight-up shot, where the body hangs nearly still around 20 blocks, showed
     * nothing there at all). Driving hands the movement to the platform's own mover, which is the
     * path every ordinary thrown item already uses and which is observed to render.
     *
     * <p><b>The caller must apply gravity itself, per tick.</b> The velocity is one tick's motion,
     * not a launch impulse: a value set once at spawn would fly straight while the computed path
     * arcs. {@code ProjectileFlight} already integrates the ability's own gravity each step and
     * hands the resulting step vector here, and the adapter suppresses the platform's own gravity so
     * the two do not both apply.
     *
     * <p><b>Only legal on the thread owning WHERE THE MARKER IS -- not where it is heading.</b> It
     * is an entity write, exactly like {@link #removeMarker}, and carries the identical unhopped
     * contract. A caller stepping a projectile is scheduled onto the region of the point it has
     * flown TO, while the marker still sits at the point it flew FROM, so the call belongs at the
     * END of a step, while still on the region the marker is actually in.
     *
     * <p>Contrast {@link #present}, which the adapter hops onto the right region by itself. This one
     * cannot: an entity write has to happen where the entity is, and only the caller knows that.
     *
     * <p>A no-op if the marker is already gone -- which is a reachable state rather than a
     * defensive one, because the platform may destroy a marker mid-flight (see the adapter).
     */
    void driveMarker(UUID markerId, Vec3 stepVelocity);

    /**
     * Remove a marker from {@link #throwMarker} or {@link #spawnMarker}. A no-op if it is already
     * gone, so the fuse task can call it unconditionally. Only legal on the thread owning the marker.
     */
    void removeMarker(UUID markerId);

    /**
     * Where the marker with this id currently is, or empty if it is gone (removed, or unloaded
     * with its chunk). A read, like {@link #combatantsNear} -- only legal on the thread owning
     * the marker.
     *
     * Lets a fuse detonate where the thrown item actually IS at fuse-end, wherever physics has
     * carried it, rather than where it was thrown.
     */
    Optional<Vec3> markerLocation(UUID markerId);
}
