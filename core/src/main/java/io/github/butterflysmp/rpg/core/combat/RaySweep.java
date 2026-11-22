package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.BeamBox;
import io.github.butterflysmp.rpg.core.mob.BeamLine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * What one segment of a PIERCING or TOLERANT ray can strike: every live body near the segment, with its box, and where
 * the segment first meets a block, if it does (the seat's brief of 2026-11-21, the Knell Beam, rulings 1 and 2).
 *
 * <p><b>ONE GEOMETRY.</b> Whether the line strikes a body is {@link BeamLine#meets} on the body's box grown by the
 * tolerance: the very test the Knell mob's beam takes ({@code MobBeam.resolve}). It is reused, not copied, and
 * {@code BeamLine} is not changed.
 *
 * <p>A port answers {@link CombatWorld#sweepRay} with this; {@link #struck} decides, in core, who is struck. The default
 * ray (no pierce, no tolerance) never comes here: it stays on {@link CombatWorld#castRay}, byte for byte.
 *
 * @param bodies  every live body near the segment, the caster excluded, each with its box; NOT yet judged
 * @param blockAt where the segment first meets a block with a collision box, or empty if it reaches its end
 */
public record RaySweep(List<Body> bodies, Optional<Vec3> blockAt) {

    public RaySweep {
        bodies = List.copyOf(bodies);
    }

    /** A live body and its box (feet to head, its own width). */
    public record Body(Combatant combatant, BeamBox box) {}

    /**
     * A body the line strikes: where on the line it is struck (the line's point nearest the box's centre), and whether
     * the line meets the box ITSELF ({@code exact}) or only its tolerance margin. A margin hit is the one a wall can
     * hide, so the caller checks sight for it; an exact hit cannot be behind a wall the clipped line never reached.
     */
    public record Struck(Combatant combatant, Vec3 point, boolean exact) {}

    /**
     * Every body whose box, grown by {@code tolerance} on every side, the line meets, NEAREST FIRST (by where the box's
     * centre falls along the line). The line is already clipped at the first block by the caller.
     */
    public static List<Struck> struck(BeamLine line, double tolerance, List<Body> bodies) {
        if (!(tolerance >= 0 && Double.isFinite(tolerance))) {
            throw new IllegalArgumentException("a ray's tolerance must be 0 or more blocks, was " + tolerance);
        }
        List<Body> met = new ArrayList<>();
        for (Body body : bodies) {
            if (line.meets(body.box(), tolerance)) met.add(body);
        }
        met.sort(Comparator.comparingDouble(b -> along(line, b.box().center())));
        List<Struck> out = new ArrayList<>(met.size());
        for (Body b : met) {
            out.add(new Struck(b.combatant(), line.nearestTo(b.box().center()), line.meets(b.box(), 0.0)));
        }
        return out;
    }

    private static double along(BeamLine line, Vec3 p) {
        return p.subtract(line.origin()).dot(line.direction());
    }
}
