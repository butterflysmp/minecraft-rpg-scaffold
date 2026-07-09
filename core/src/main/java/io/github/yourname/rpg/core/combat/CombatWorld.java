package io.github.yourname.rpg.core.combat;

import io.github.yourname.rpg.core.Vec3;
import java.util.Collection;

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

    void schedule(Vec3 near, int delayTicks, Runnable task);

    /** Fire-and-forget presentation hook. Particles, sounds, damage numbers. */
    void present(Vec3 at, String visualId);
}
