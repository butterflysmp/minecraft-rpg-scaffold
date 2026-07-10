package io.github.yourname.rpg.core.combat;

import io.github.yourname.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a ray crosses from one chunk column into the next.
 *
 * A chunk belongs to exactly one region, so a trace confined to a single chunk column
 * reads exactly one region. A trace of any FIXED length does not have that property: it
 * straddles a plane sooner or later, whatever length you pick. Ending each segment ON a
 * plane is the only thing that guarantees it.
 *
 * Only X and Z matter. Regions own columns of chunks; height is irrelevant to ownership,
 * so a ray straight up never leaves its column however far it goes.
 *
 * This is the one place in core that knows Minecraft's grid is 16 blocks wide. It is not a
 * method on Aim: an aim is an origin and a unit direction, a general thing, and it must not
 * learn about chunks any more than core must learn about org.bukkit.
 */
public final class ChunkTraversal {

    /** Blocks per chunk, on X and on Z. */
    public static final double CHUNK_SIZE = 16.0;

    /** Guards against a crossing landing a hair before its predecessor, or on `range`. */
    private static final double EPSILON = 1e-9;

    private ChunkTraversal() {}

    /**
     * The points at which the aim crosses an {@code x = 16k} or {@code z = 16k} plane,
     * in increasing distance, followed by the point at {@code range}. The origin itself is
     * not included, and no two returned points coincide.
     *
     * Walk them in order and each consecutive pair bounds a segment lying inside one chunk
     * column. A ray that never leaves its column returns a single point: the far end.
     *
     * @param direction must be a unit vector, which Aim's constructor guarantees. Because
     *                  it is, the parameter along the ray IS distance in blocks.
     */
    public static List<Vec3> segmentEndpoints(Vec3 origin, Vec3 direction, double range) {
        List<Double> crossings = new ArrayList<>();
        addPlaneCrossings(crossings, origin.x(), direction.x(), range);
        addPlaneCrossings(crossings, origin.z(), direction.z(), range);
        crossings.sort(Double::compare);

        List<Vec3> endpoints = new ArrayList<>();
        double previous = 0.0;
        for (double t : crossings) {
            // Drop anything at or behind the origin, anything at or beyond the far end, and
            // a corner -- where an x-plane and a z-plane are crossed at the same instant.
            if (t > previous + EPSILON && t < range - EPSILON) {
                endpoints.add(origin.add(direction.scale(t)));
                previous = t;
            }
        }
        endpoints.add(origin.add(direction.scale(range)));
        return endpoints;
    }

    /**
     * Distances at which one axis crosses its planes.
     *
     * A zero component means the ray runs parallel to those planes and never crosses one:
     * return immediately rather than dividing by it. That is what keeps an axis-aligned ray
     * from producing Infinity, and what keeps this loop from never advancing -- the step is
     * {@code 16/|d|}, strictly positive whenever d is not zero.
     *
     * An origin sitting exactly on a plane yields a first crossing strictly greater than
     * zero in both directions, so there is never a zero-length leading segment: travelling
     * +x from x=32 the next plane is 48, and travelling -x from x=32 it is 16.
     */
    private static void addPlaneCrossings(List<Double> out, double origin, double direction, double range) {
        if (direction == 0.0) return;

        double step = CHUNK_SIZE / Math.abs(direction);
        double firstPlane = direction > 0
                ? Math.floor(origin / CHUNK_SIZE) * CHUNK_SIZE + CHUNK_SIZE
                : Math.ceil(origin / CHUNK_SIZE) * CHUNK_SIZE - CHUNK_SIZE;

        for (double t = (firstPlane - origin) / direction; t < range; t += step) {
            if (t > 0) out.add(t);
        }
    }

    /** The chunk column containing a world x (or z) coordinate. Negative-safe. */
    public static int columnOf(double coordinate) {
        return (int) Math.floor(coordinate / CHUNK_SIZE);
    }
}
