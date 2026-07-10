package io.github.yourname.rpg.core.combat;

import io.github.yourname.rpg.core.Vec3;

/**
 * What a ray ran into first.
 *
 * @param point     where it struck -- an entity's position, or the face of a block
 * @param combatant the thing struck, or null if the ray hit terrain
 */
public record RayHit(Vec3 point, Combatant combatant) {

    public static RayHit ofBlock(Vec3 point) {
        return new RayHit(point, null);
    }

    public static RayHit ofCombatant(Vec3 point, Combatant combatant) {
        return new RayHit(point, combatant);
    }
}
