package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Optional;

/**
 * WHERE A BEAM MEETS AN ORB (Lapis Crystal's D3, 2026-11-23), pure: the point of the segment {@code from}-{@code to}
 * nearest the orb's centre, when that nearest distance is within {@code radius}; otherwise nothing. The caller passes the
 * segment only as far as the beam REACHES (its first body or block), so an orb behind the beam's target is never met,
 * and the target in front of it is never shielded.
 */
public final class OrbIntercept {

    private OrbIntercept() {}

    public static Optional<Vec3> along(Vec3 from, Vec3 to, Vec3 centre, double radius) {
        Vec3 segment = to.subtract(from);
        double lengthSquared = segment.lengthSquared();
        double t = lengthSquared == 0 ? 0 : centre.subtract(from).dot(segment) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        Vec3 nearest = from.add(segment.scale(t));
        return nearest.distanceSquared(centre) <= radius * radius ? Optional.of(nearest) : Optional.empty();
    }
}
