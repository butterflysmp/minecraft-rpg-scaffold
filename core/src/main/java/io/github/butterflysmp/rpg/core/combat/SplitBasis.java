package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * WHAT "SIDEWAYS" IS AT A SPLIT (Airburst Flames, 2026-11-23: the bursts are "fanned ... sideways about its direction of
 * travel at the split"). Sideways is HORIZONTAL and perpendicular to the direction of travel, on the same side as an
 * aim's right: travelling east (+x), it is south (+z), as a player facing east has south at his right hand.
 *
 * <p><b>Straight up or straight down there is no horizontal perpendicular</b> (the direction's horizontal part is zero),
 * so sideways is then the RIGHT OF THE AIM THE SHOT WAS FIRED WITH, frozen at the cast: the fan opens across his view as
 * he looked when he fired. The brief asked what sideways is when the shot travels straight up; this is the answer.
 */
public final class SplitBasis {

    private SplitBasis() {}

    /** Below this squared horizontal length the direction is treated as vertical. */
    static final double VERTICAL_EPSILON_SQUARED = 1e-12;

    public static Vec3 sidewaysOf(Vec3 forward, Vec3 aimRight) {
        Vec3 horizontal = new Vec3(0.0 - forward.z(), 0, forward.x());   // 0.0 - z, not -z: never a negative zero
        if (horizontal.lengthSquared() < VERTICAL_EPSILON_SQUARED) return aimRight.normalize();
        return horizontal.normalize();
    }
}
