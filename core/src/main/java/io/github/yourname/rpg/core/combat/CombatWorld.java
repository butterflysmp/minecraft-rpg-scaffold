package io.github.yourname.rpg.core.combat;

import io.github.yourname.rpg.core.Vec3;
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
    Collection<Combatant> combatantsNear(Vec3 center, double radius);

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

    void schedule(Vec3 near, int delayTicks, Runnable task);

    /** Fire-and-forget presentation hook. Particles, sounds, damage numbers. */
    void present(Vec3 at, String visualId);
}
