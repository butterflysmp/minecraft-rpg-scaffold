package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * A creature's BODY for the Knell's beam: an axis-aligned box in world coordinates, BLOCKS, from its {@code min}
 * corner to its {@code max} corner (PLAN-dragon-mobs.md step R5). Paper fills it from the entity's own bounding box at
 * the fire, so a scaled body is its real, scaled size.
 *
 * <p>(A {@code record} is a class whose fields are its constructor's arguments, each with a same-named getter.)
 */
public record BeamBox(Vec3 min, Vec3 max) {

    public BeamBox {
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("a box's min corner " + min + " must not exceed its max " + max);
        }
    }

    /**
     * The box of a body standing at {@code feet}: {@code width} BLOCKS across in x and in z, centred on the feet, and
     * {@code height} BLOCKS from the feet up. A vanilla entity's box has exactly this shape.
     */
    public static BeamBox standing(Vec3 feet, double width, double height) {
        double h = width / 2.0;
        return new BeamBox(new Vec3(feet.x() - h, feet.y(), feet.z() - h),
                new Vec3(feet.x() + h, feet.y() + height, feet.z() + h));
    }

    /** This box grown by {@code by} BLOCKS on EVERY side (six faces), as cfde822 grew the target's box by 1.0. */
    public BeamBox grow(double by) {
        Vec3 d = new Vec3(by, by, by);
        return new BeamBox(min.subtract(d), max.add(d));
    }

    public Vec3 center() {
        return min.add(max).scale(0.5);
    }
}
