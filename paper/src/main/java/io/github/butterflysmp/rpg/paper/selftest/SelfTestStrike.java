package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamBox;
import io.github.butterflysmp.rpg.core.mob.BeamLine;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * THE INSTRUMENT FOR A REAL SWING AT THE PHALANX'S SHOT (dragon-mobs step R7a; the seat's ruling 4 on #201), the pure
 * half. Pure, so {@code SelfTestStrikeTest} pins each line. It reads; it never judges.
 *
 * <ul>
 *   <li><b>{@link #along}</b>: how far along a look ray, from the eye, the ray first ENTERS a box, within a reach;
 *       empty when it never does. The look is the player's own, so a box that is not where his crosshair is reads as a
 *       miss.</li>
 *   <li><b>{@link #gap}</b>: on a miss, the smallest growth of the box on every side at which the reach-long ray
 *       would meet it ({@link BeamLine#gap}: a box's, so the largest axis distance, not a Euclidean one).</li>
 * </ul>
 */
final class SelfTestStrike {

    private SelfTestStrike() {}

    /** {@code strike_at_age}'s argument: strike the tagged Phalanx's shot the tick its age reaches {@code age}. */
    record AtAge(String tag, int age, int timeoutTicks) {}

    /** What a STRIKE_AT_AGE poll does this tick. */
    enum Next { WAIT, STRIKE, NO_SHOT_AT_TIMEOUT, YOUNG_AT_TIMEOUT }

    /**
     * C3, CLOSED HERE (step R7b). The boot of 3dc0e184 read SH15 as {@code no live shot after 0 ticks}: the row forced a
     * shot and polled in the same tick, before the Phalanx's loop had fired it, and the first build gave up on the first
     * empty poll. Now NO SHOT YET is a wait like any other; only the timeout ends it, and the line says which it was.
     */
    static Next next(boolean live, int age, int wanted, int waited, int timeoutTicks) {
        if (live && age >= wanted) return Next.STRIKE;
        if (waited >= timeoutTicks) return live ? Next.YOUNG_AT_TIMEOUT : Next.NO_SHOT_AT_TIMEOUT;
        return Next.WAIT;
    }

    static Optional<AtAge> parseAtAge(String arg) {
        String[] w = arg.trim().split("\\s+");
        if (w.length != 3 || w[0].isEmpty()) return Optional.empty();
        try {
            int age = Integer.parseInt(w[1]);
            int timeout = Integer.parseInt(w[2]);
            if (age < 0 || timeout < 1) return Optional.empty();
            return Optional.of(new AtAge(w[0], age, timeout));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** The entry distance of the ray {@code eye + t x look}, {@code 0 <= t <= reach}, into {@code box}; empty on a miss. */
    static OptionalDouble along(Vec3 eye, Vec3 look, double reach, BeamBox box) {
        BeamLine line = new BeamLine(eye, look.normalize(), reach);
        if (!line.meets(box, 0.0)) return OptionalDouble.empty();
        // The slab test's lower bound: the first parameter inside all three slabs.
        double lo = 0.0;
        double[] o = {eye.x(), eye.y(), eye.z()};
        Vec3 d3 = look.normalize();
        double[] d = {d3.x(), d3.y(), d3.z()};
        double[] min = {box.min().x(), box.min().y(), box.min().z()};
        double[] max = {box.max().x(), box.max().y(), box.max().z()};
        for (int i = 0; i < 3; i++) {
            if (d[i] == 0.0) continue;
            double t1 = (min[i] - o[i]) / d[i];
            double t2 = (max[i] - o[i]) / d[i];
            lo = Math.max(lo, Math.min(t1, t2));
        }
        return OptionalDouble.of(lo);
    }

    /** The growth of {@code box} at which the reach-long look ray would meet it: 0 when it already does. */
    static double gap(Vec3 eye, Vec3 look, double reach, BeamBox box) {
        return new BeamLine(eye, look.normalize(), reach).gap(box);
    }

    private static String v(Vec3 p) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", p.x(), p.y(), p.z());
    }

    /**
     * One line per arm swing, attack or interact of the run's player while a row watches: the nearest live shot, the
     * distance along his look into its box (or {@code miss} and the gap), his eye and his look.
     */
    static String swingLine(String kind, UUID shot, OptionalDouble along, double gap, Vec3 eye, Vec3 look) {
        if (shot == null) return "SWING kind=" + kind + " shot=none";
        return String.format(Locale.ROOT, "SWING kind=%s shot=%s along=%s gap=%.3f eye=%s look=%s", kind, shot,
                along.isPresent() ? String.format(Locale.ROOT, "%.3f", along.getAsDouble()) : "miss", gap, v(eye),
                v(look));
    }

    /** One tick after a watched swing: is that shot still live? (A destroyed one has its own SHOT destroyed line.) */
    static String afterLine(UUID shot, boolean live) {
        return "SWING after shot=" + shot + " live=" + live;
    }
}
