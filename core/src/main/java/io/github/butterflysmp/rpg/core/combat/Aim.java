package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * Where a cast starts and which way it points. In Paper this is the player's eye
 * location and look direction, but core neither knows nor cares.
 *
 * Replaces the already-resolved (target, impactPoint) pair that AbilityService
 * used to be handed. Resolving an aim into a target is the job of CastExecutor,
 * because doing so touches the world and must happen on the owning region thread.
 *
 * The direction is normalised on construction, so every consumer can treat it as
 * a unit vector. A zero direction stays zero rather than becoming NaN.
 */
public record Aim(Vec3 origin, Vec3 direction) {

    public Aim {
        direction = direction.normalize();
    }

    /** The point {@code distance} blocks along the aim. */
    public Vec3 pointAt(double distance) {
        return origin.add(direction.scale(distance));
    }
}
