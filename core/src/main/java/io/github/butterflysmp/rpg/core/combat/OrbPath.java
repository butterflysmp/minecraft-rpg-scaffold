package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Optional;

/**
 * WHERE A THROWN ORB COMES TO REST (Lapis Crystal's D2, 2026-11-23), pure: {@code range} along the aim, or, when a solid
 * block is nearer, {@code standoff} short of it (never behind the thrower: a block closer than the standoff rests the orb
 * at the muzzle).
 */
public final class OrbPath {

    private OrbPath() {}

    public static Vec3 restPoint(Vec3 origin, Vec3 unitDirection, double range, Optional<Vec3> block, double standoff) {
        double reach = range;
        if (block.isPresent()) {
            double toBlock = block.get().subtract(origin).dot(unitDirection);
            if (toBlock < range) reach = Math.max(0.0, toBlock - standoff);
        }
        return origin.add(unitDirection.scale(reach));
    }
}
