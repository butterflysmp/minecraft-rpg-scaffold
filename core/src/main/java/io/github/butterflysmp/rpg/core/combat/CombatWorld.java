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
     * Remove a marker thrown by {@link #throwMarker}. A no-op if it is already gone, so the
     * fuse task can call it unconditionally. Only legal on the thread owning the marker.
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
