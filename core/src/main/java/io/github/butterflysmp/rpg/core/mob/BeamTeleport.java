package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * WHERE THE KNELL LANDS AFTER ITS BEAM HITS (PLAN-dragon-mobs.md step R6): a PURE decision. Paper supplies the
 * positions at the END of the delay and answers four questions through {@link Footing}; this class decides the order
 * of the candidates and which one wins.
 *
 * <p><b>THE GEOMETRY, every quantity named:</b>
 * <ul>
 *   <li>the CENTRE is the target's FEET;</li>
 *   <li>the RADIUS is the HORIZONTAL distance (x and z only, BLOCKS) from the target's feet to the Knell's feet;</li>
 *   <li>angle ZERO is the Knell's bearing as seen from the target (the horizontal direction from the target's feet to
 *       the Knell's);</li>
 *   <li>a candidate COLUMN is the point at that radius turned by an angle in DEGREES to one SIDE: {@link Side#RIGHT}
 *       is a right turn for someone at the target facing the Knell (seen from above, north up: clockwise, so from a
 *       Knell due east, right is toward south, +z), {@link Side#LEFT} the other;</li>
 *   <li>its STANDING HEIGHT is the target's feet height plus a whole number of blocks k, tried nearest first
 *       (0, +1, -1, +2, -2, ...) out to the level tolerance; the first that FITS and is GROUNDED is its footing.</li>
 * </ul>
 */
public final class BeamTeleport {

    private BeamTeleport() {}

    /** Which way round the target: as seen by someone at the target, facing the Knell. */
    public enum Side { RIGHT, LEFT }

    /** The injected randomness: the side tried first, and the first angle tried on it (degrees, in the arc). */
    public record Draw(Side side, double firstAngle) {}

    @FunctionalInterface
    public interface Source {
        Draw draw(double arcMinDegrees, double arcMaxDegrees);
    }

    /** The production source: a side by a fair coin, a first angle uniform over the arc. */
    public static Source uniform(RandomGenerator random) {
        // nextDouble() is in [0, 1), so the arc's max is approached and never drawn; the spread that follows always
        // tries the max itself (angles()), so both bounds are reached.
        return (min, max) -> new Draw(random.nextBoolean() ? Side.RIGHT : Side.LEFT,
                min + random.nextDouble() * (max - min));
    }

    /**
     * THE PORT: what the world says about one standing point (a Knell's FEET there). Paper answers each through the
     * collision API (PLAN-dragon-mobs.md section 7 F-T); this class never asks anything else.
     */
    public interface Footing {
        /** The chunk holding this point is loaded. */
        boolean loaded(Vec3 feet);
        /** The Knell's own scaled body, standing here, collides with nothing. */
        boolean fits(Vec3 feet);
        /** There is solid ground directly beneath these feet. */
        boolean grounded(Vec3 feet);
        /** From the Knell's eye, standing here, the target's aim point is not hidden by a block. */
        boolean sees(Vec3 feet);
    }

    /** One candidate: its side, its angle in degrees, and its column (x, z; y the target's feet height). */
    public record Candidate(Side side, double angle, Vec3 column) {}

    /** What one candidate came to. */
    public enum Outcome { OK, UNLOADED, NO_FOOTING, NO_SIGHT }

    /**
     * One candidate's verdict. {@code landing} is its footing (OK and NO_SIGHT carry it); {@code blocked} and
     * {@code ungrounded} count the heights tried that did not fit, and that fit but had no ground (NO_FOOTING's why).
     */
    public record Verdict(Candidate candidate, Outcome outcome, Optional<Vec3> landing, int blocked, int ungrounded) {}

    /** The decision: the radius, the side tried first, every candidate tried in order, and the landing, or none. */
    public record Decision(double radius, double bearingDegrees, Side firstSide, List<Verdict> tried,
                           Optional<Vec3> landing) {}

    /** The order of angles on ONE side: the drawn first angle, then the rest spread evenly across the arc. */
    /**
     * The order of angles on ONE side, {@code spotsPerSide} of them: the drawn first angle, then the remaining
     * {@code n - 1} spread evenly across the arc from its min to its max, both included (one remaining: the arc's
     * middle). For 5 over 30..70 drawn at 51.25: 51.25, 30, 43.33, 56.67, 70. A spread angle equal to the drawn one is
     * tried again, not skipped.
     */
    public static List<Double> angles(MobTeleport t, double firstAngle) {
        List<Double> out = new ArrayList<>();
        out.add(firstAngle);
        int m = t.spotsPerSide() - 1;
        double min = t.arcMinDegrees();
        double max = t.arcMaxDegrees();
        if (m == 1) out.add((min + max) / 2.0);
        for (int i = 0; m >= 2 && i < m; i++) out.add(min + (max - min) * i / (m - 1));
        return out;
    }

    /** The whole-block offsets from the target's feet height, nearest first, ups before downs: 0, +1, -1, +2, -2, ... */
    public static List<Integer> heights(int tolerance) {
        List<Integer> out = new ArrayList<>();
        out.add(0);
        for (int k = 1; k <= tolerance; k++) {
            out.add(k);
            out.add(-k);
        }
        return out;
    }

    /**
     * The candidate column for {@code side} and {@code angle}: the Knell's horizontal offset from the centre, turned
     * by the angle (RIGHT: {@code (x cos a - z sin a, x sin a + z cos a)}; LEFT: the angle negated), at the centre's
     * height. The radius is unchanged by construction. A Knell exactly above the centre (radius 0) has no bearing;
     * its offset is taken as (0, 0) and every column is the centre itself.
     */
    public static Vec3 column(Vec3 center, Vec3 mobFeet, Side side, double angleDegrees) {
        double hx = mobFeet.x() - center.x();
        double hz = mobFeet.z() - center.z();
        double a = Math.toRadians(side == Side.RIGHT ? angleDegrees : -angleDegrees);
        double cos = Math.cos(a);
        double sin = Math.sin(a);
        return new Vec3(center.x() + hx * cos - hz * sin, center.y(), center.z() + hx * sin + hz * cos);
    }

    /**
     * THE DECISION. Candidates in order: the drawn side's {@link #angles}, then the other side's, the same angles.
     * For each: {@code loaded}, or UNLOADED; then the nearest height ({@link #heights}) that {@code fits} and is
     * {@code grounded}, or NO_FOOTING with the counts; then, if sight is required, {@code sees} from that footing, or
     * NO_SIGHT (no other height is tried). The first OK wins and nothing after it is asked; none OK, no landing.
     */
    public static Decision decide(MobTeleport t, Vec3 targetFeet, Vec3 mobFeet, Draw draw, Footing footing) {
        double hx = mobFeet.x() - targetFeet.x();
        double hz = mobFeet.z() - targetFeet.z();
        double radius = Math.hypot(hx, hz);
        double bearing = Math.toDegrees(Math.atan2(hz, hx));
        Side other = draw.side() == Side.RIGHT ? Side.LEFT : Side.RIGHT;
        List<Double> angles = angles(t, draw.firstAngle());
        List<Verdict> tried = new ArrayList<>();
        for (Side side : List.of(draw.side(), other)) {
            for (double angle : angles) {
                Candidate c = new Candidate(side, angle, column(targetFeet, mobFeet, side, angle));
                Verdict v = judge(t, c, footing);
                tried.add(v);
                if (v.outcome() == Outcome.OK) {
                    return new Decision(radius, bearing, draw.side(), List.copyOf(tried), v.landing());
                }
            }
        }
        return new Decision(radius, bearing, draw.side(), List.copyOf(tried), Optional.empty());
    }

    private static Verdict judge(MobTeleport t, Candidate c, Footing footing) {
        Foothold f = foot(c.column(), t.levelTolerance(), t.requireSight(), footing);
        return new Verdict(c, f.outcome(), f.landing(), f.blocked(), f.ungrounded());
    }

    /** What one column came to: its outcome, its footing (OK and NO_SIGHT carry it), and NO_FOOTING's counts. */
    public record Foothold(Outcome outcome, Optional<Vec3> landing, int blocked, int ungrounded) {}

    /**
     * THE ONE LANDING CHECK, shared by the Knell's teleport and the Phalanx's blink (step R7b: "do not write a second
     * landing checker"). For a column whose y is the height to search around: {@code loaded}, or UNLOADED; then the
     * nearest height ({@link #heights} of {@code levelTolerance}) that {@code fits} and is {@code grounded}, or
     * NO_FOOTING with the counts; then, if {@code requireSight}, {@code sees} from that footing, or NO_SIGHT (no other
     * height is tried).
     */
    public static Foothold foot(Vec3 column, int levelTolerance, boolean requireSight, Footing footing) {
        if (!footing.loaded(column)) return new Foothold(Outcome.UNLOADED, Optional.empty(), 0, 0);
        double base = anchor(column.y());   // F19: the WHOLE block the column's y is in; see anchor()
        int blocked = 0;
        int ungrounded = 0;
        for (int k : heights(levelTolerance)) {
            Vec3 feet = new Vec3(column.x(), base + k, column.z());
            if (!footing.fits(feet)) { blocked++; continue; }
            if (!footing.grounded(feet)) { ungrounded++; continue; }
            if (requireSight && !footing.sees(feet)) {
                return new Foothold(Outcome.NO_SIGHT, Optional.of(feet), 0, 0);
            }
            return new Foothold(Outcome.OK, Optional.of(feet), 0, 0);
        }
        return new Foothold(Outcome.NO_FOOTING, Optional.empty(), blocked, ungrounded);
    }

    /**
     * THE HEIGHT EVERY LANDING SEARCH IS ANCHORED AT: the WHOLE block a height is in, floor(y). Moved here from
     * {@code MobBlink.anchor} (step R8's R3 fix) on 2026-11-17 for F19, so the Knell's teleport and the Phalanx's
     * blink share it inside {@link #foot}, the one landing check. A full block's top is a whole number, and
     * {@link #foot} tries whole blocks from its anchor, so a FRACTIONAL anchor never stands a body on a floor: every
     * height is (say) 0.5 into a block, so it does not fit, or 0.5 above one, so it has no ground. The Knell met that
     * whenever its TARGET stood on a slab (64.5) or was mid-jump when the move fell due (F19); the Phalanx met it
     * mid-jump (R3).
     *
     * <p><b>WHAT IT COSTS, stated:</b> a landing on a slab's top (x.5), or on any floor whose top is not a whole
     * number (a path block, farmland, a snow layer), is NEVER tried, because every height tried is whole. A target
     * standing on a slab is still reached (its anchor is the floor beside it); a Knell or a Phalanx in a field made only
     * of slab tops cannot land anywhere and does not move.
     *
     * <p>{@link #ANCHOR_SNAP} lifts a height within a millionth below a whole number onto it, so floating-point drift on
     * a floor (63.9999999999) anchors on 64, its own block, not 63.
     */
    public static double anchor(double y) {
        return Math.floor(y + ANCHOR_SNAP);
    }

    /** BLOCKS: how far below a whole number a height may sit and still be anchored on it. NOT IN THE RECORD: chosen. */
    public static final double ANCHOR_SNAP = 1.0E-6;

    /** Why a move due at the end of the delay does not happen, or empty when it goes ahead. */
    public enum Skip { KNELL_GONE, TARGET_GONE, TARGET_WORLD, TARGET_EXEMPT }

    /**
     * In this order: the Knell is not live (dead, removed, unloaded); the target is not live; the target is in another
     * world; the target is no longer a combatant the Knell is hostile to ({@link MobHostility}).
     */
    public static Optional<Skip> skip(boolean knellLive, boolean targetLive, boolean sameWorld,
                                      MobHostility.Side targetSide, boolean targetExemptMode) {
        if (!knellLive) return Optional.of(Skip.KNELL_GONE);
        if (!targetLive) return Optional.of(Skip.TARGET_GONE);
        if (!sameWorld) return Optional.of(Skip.TARGET_WORLD);
        if (!MobHostility.hostileTo(targetSide, targetExemptMode)) return Optional.of(Skip.TARGET_EXEMPT);
        return Optional.empty();
    }
}
