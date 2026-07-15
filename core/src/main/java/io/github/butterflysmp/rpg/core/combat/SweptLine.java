package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Who a dash passes through: the combatants within {@code radius} of the INTENDED line from
 * {@code origin} along {@code direction} for {@code distance} blocks.
 *
 * The line is intended, not actual. A velocity-impulse dash is ballistic -- it may arc -- so
 * hit detection uses the line the cast aimed down, computed once, rather than sampling the
 * caster's wobbly real path. Over a short dash the two are close enough that this reads right,
 * and it sidesteps the per-tick de-dup a moving hitbox would need.
 *
 * This is the hit-set RESOLVER, not just geometry: it also applies the two exclusions that
 * are rules rather than positions -- the caster never hits themselves, and players are left
 * out because the payload is mob-only. Both live here, beside the geometry, so a single core
 * test guards them: shrink the radius and a borderline enemy drops; delete the player skip and
 * an in-path player is wrongly returned. Either mutation reddens.
 */
public final class SweptLine {
    private SweptLine() {}

    /**
     * @param direction need not be unit length; it is normalised here.
     * @param excludeId the caster, never a target of their own dash.
     * @return the mob combatants the intended line passes through, order unspecified.
     */
    public static List<Combatant> enemiesAlong(Vec3 origin, Vec3 direction, double distance,
                                               double radius, Iterable<Combatant> candidates,
                                               UUID excludeId) {
        Vec3 unit = direction.normalize();
        double radiusSquared = radius * radius;

        List<Combatant> hits = new ArrayList<>();
        for (Combatant candidate : candidates) {
            CombatantSnapshot state = candidate.state();
            if (candidate.id().equals(excludeId)) continue;   // never the caster
            if (state.player()) continue;                     // mob-only: players are not hit

            Vec3 toCandidate = state.position().subtract(origin);
            double projected = toCandidate.dot(unit);         // distance along the line
            if (projected < 0 || projected > distance) continue;   // behind the start, or past the end

            double perpendicularSquared = toCandidate.lengthSquared() - projected * projected;
            if (perpendicularSquared > radiusSquared) continue;    // the line passes wide

            hits.add(candidate);
        }
        return hits;
    }
}
