package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * WHERE CLUSTER BOMBS' SPLIT CHUNKS GO (the seat's F9, 2026-11-26; Ben: "Split out in a triangle shape"). Pure geometry:
 * no world, no clock. {@code CastExecutor} asks it when the big chunk goes off.
 *
 * <ul>
 *   <li><b>The heading</b> is the throw's, horizontal: the press's aim with its vertical part removed; straight up or down
 *       (no horizontal part), the shooter's forward from his right vector ({@code up x right}). SEAT PLACEHOLDER
 *       2026-11-26: "one point along the throw's heading".</li>
 *   <li><b>The directions</b> are {@code count} horizontal unit vectors, the first ON the heading and the rest
 *       {@code 360 / count} degrees apart about world up ({@link Vec3#rotateAboutY}): three make the triangle.</li>
 *   <li><b>The throw</b> of each is {@code direction x outwardSpeed + (0, lift, 0)}, under the weapon's gravity, so on
 *       LEVEL ground it comes back down to its start height {@code distance} out: {@link #outwardSpeed} steps the flight's
 *       own discrete arc (each tick: position += velocity, then velocity.y -= gravity, as {@code ProjectileFlight} does)
 *       to find the crossing, so the landing is the flight's and not a closed form's. CC'S CALL: an arc, so a player sees
 *       them thrown; the alternative, placing them at rest 2.5 out at once, shows no throw. On uneven ground each lands
 *       where its arc meets a block, "about" 2.5 out (BEN's word is the seat's: SEAT PLACEHOLDER 2026-11-26).</li>
 * </ul>
 */
public final class ClusterSplit {

    private ClusterSplit() {}

    /** The throw's horizontal heading, a unit vector (see the class note). */
    public static Vec3 heading(Vec3 aimDirection, Vec3 right) {
        Vec3 flat = new Vec3(aimDirection.x(), 0, aimDirection.z());
        if (flat.lengthSquared() > 1e-12) return flat.normalize();
        Vec3 fromRight = new Vec3(0, 1, 0).cross(right);
        Vec3 flatRight = new Vec3(fromRight.x(), 0, fromRight.z());
        if (flatRight.lengthSquared() > 1e-12) return flatRight.normalize();
        return new Vec3(1, 0, 0);   // no aim and no right: a fixed heading, so the split still happens
    }

    /** {@code count} horizontal directions, the first on {@code heading}, {@code 360 / count} degrees apart. */
    public static List<Vec3> directions(Vec3 heading, int count) {
        if (count < 1) throw new IllegalArgumentException("a split of " + count + " chunks");
        List<Vec3> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(heading.rotateAboutY(360.0 * i / count).normalize());
        return out;
    }

    /**
     * The horizontal speed that brings a chunk thrown with {@code lift} up, under {@code gravity}, back to its start height
     * {@code distance} out. Steps the flight's own arc: the crossing is on the step whose end first falls below the start,
     * at the fraction of that step where it crosses.
     */
    public static double outwardSpeed(double distance, double lift, double gravity) {
        if (!(lift > 0) || !(gravity > 0)) {
            throw new IllegalArgumentException("a split chunk needs lift and gravity above 0 to come down: lift " + lift
                    + ", gravity " + gravity);
        }
        double y = 0, vy = lift;
        for (int n = 1; n <= 100_000; n++) {
            double next = y + vy;
            if (next < 0) {
                double steps = (n - 1) + y / (y - next);
                return distance / steps;
            }
            y = next;
            vy -= gravity;
        }
        throw new IllegalStateException("a split chunk with lift " + lift + " and gravity " + gravity + " never came down");
    }

    /** The velocity each split chunk is thrown with, in {@link #directions}' order. */
    public static List<Vec3> velocities(Vec3 heading, int count, double distance, double lift, double gravity) {
        double speed = outwardSpeed(distance, lift, gravity);
        List<Vec3> out = new ArrayList<>(count);
        for (Vec3 d : directions(heading, count)) out.add(d.scale(speed).add(new Vec3(0, lift, 0)));
        return out;
    }
}
