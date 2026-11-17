package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * THE KNELL'S BEAM (PLAN-dragon-mobs.md step R5; the plan's MB3 + E5). Authored in the mob file's {@code beam:} block.
 * Ben, 2026-11-17, to the old game's numbers: "yes". Every behaviour he did not re-rule is cfde822's, listed with its
 * mark in the plan's section 3.R.1.a; this class is the PURE half: who is aimed at is paper's search, but whether the
 * fire HITS, and what it deals, is decided here.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code cycleMinTicks} / {@code cycleMaxTicks}: TICKS from the Knell appearing, a fire, or an attempt that
 *       found no one, to its next attempt; drawn uniformly, both ends included (100 and 200: 5 to 10 seconds).</li>
 *   <li>{@code telegraphTicks}: TICKS from the telegraph's first tick (the line is locked) to the fire (12).</li>
 *   <li>{@code flashAt}: the telegraph TICKS, counted from 0 at the lock, on which its warning line is drawn
 *       (cfde822: 0 and 9). Each is inside the telegraph.</li>
 *   <li>{@code range}: BLOCKS from the Knell's EYE. A target is in range when its aim point is at most this far from
 *       the eye, inclusive; the fired segment runs this far along the locked line unless a block stops it (40).</li>
 *   <li>{@code aimHeight}: a FRACTION of the target's height above its feet: the point the line is locked through
 *       (0.6).</li>
 *   <li>{@code tolerance}: BLOCKS the target's box is grown by on EVERY side before it is tested against the segment
 *       (cfde822's 1.0, K9: not asked of Ben).</li>
 *   <li>{@code damage}: the AUTHORED damage, before gear score and before the victim's defence (35).</li>
 * </ul>
 */
public record MobBeam(int cycleMinTicks, int cycleMaxTicks, int telegraphTicks, List<Integer> flashAt,
                      double range, double aimHeight, double tolerance, double damage,
                      List<BeamSound> telegraphSounds, List<BeamSound> fireSounds) {

    public MobBeam {
        if (cycleMinTicks < 1) {
            throw new IllegalArgumentException("beam.cycle_ticks.min must be at least 1 tick, was " + cycleMinTicks);
        }
        if (cycleMaxTicks < cycleMinTicks) {
            throw new IllegalArgumentException("beam.cycle_ticks.max (" + cycleMaxTicks
                    + ") must be at least beam.cycle_ticks.min (" + cycleMinTicks + ")");
        }
        if (telegraphTicks < 1) {
            throw new IllegalArgumentException("beam.telegraph_ticks must be at least 1, was " + telegraphTicks);
        }
        flashAt = List.copyOf(flashAt);
        telegraphSounds = List.copyOf(telegraphSounds);
        fireSounds = List.copyOf(fireSounds);
        for (int t : flashAt) {
            if (t < 0 || t >= telegraphTicks) {
                throw new IllegalArgumentException("beam.flash_at " + t + " is not a telegraph tick (0.."
                        + (telegraphTicks - 1) + ")");
            }
        }
        if (!(range > 0 && Double.isFinite(range))) {
            throw new IllegalArgumentException("beam.range must be a positive number of blocks, was " + range);
        }
        if (!(aimHeight >= 0 && aimHeight <= 1)) {
            throw new IllegalArgumentException("beam.aim_height must be a fraction of a height, 0 to 1, was " + aimHeight);
        }
        if (!(tolerance >= 0 && Double.isFinite(tolerance))) {
            throw new IllegalArgumentException("beam.tolerance must be 0 or more blocks, was " + tolerance);
        }
        if (!(damage >= 0 && Double.isFinite(damage))) {
            throw new IllegalArgumentException("beam.damage must be 0 or more, was " + damage);
        }
    }

    /**
     * What the beam deals its target BEFORE the target's defence: the authored damage, priced as every custom mob's
     * hit on a player is ({@link MobScaling#attackDamage}: x1 for a custom mob, x GS/100 when hostile). The seat's
     * provisional pricing, as the Phage's blast's P-b.
     */
    /** The eight-number form with no sounds: step R5's call sites read unchanged. */
    public MobBeam(int cycleMinTicks, int cycleMaxTicks, int telegraphTicks, List<Integer> flashAt,
                   double range, double aimHeight, double tolerance, double damage) {
        this(cycleMinTicks, cycleMaxTicks, telegraphTicks, flashAt, range, aimHeight, tolerance, damage, List.of(),
                List.of());
    }

    public double price(boolean hostile, int gearScore) {
        return MobScaling.attackDamage(damage, true, hostile, gearScore);
    }

    /** The point the line is locked through: {@code aimHeight} of the way up a body {@code height} blocks tall. */
    public Vec3 aimPoint(Vec3 feet, double height) {
        return new Vec3(feet.x(), feet.y() + aimHeight * height, feet.z());
    }

    /** Is {@code aim} within {@code range} blocks of {@code eye}? Inclusive, straight line. */
    public boolean inRange(Vec3 eye, Vec3 aim) {
        return eye.distanceSquared(aim) <= range * range;
    }

    /** Is {@code tick} (0 at the lock) one of the telegraph's flash ticks? */
    public boolean flashes(int tick) {
        return flashAt.contains(tick);
    }

    /** What happened to one creature the fire tested. */
    public enum Outcome {
        /** The locked target, hostile to mobs, its grown box meets the segment: it is dealt the damage. */
        HIT,
        /** The locked target, hostile to mobs, its grown box misses the segment. */
        MISS,
        /** The locked target is no longer a live body in this world (it died, was removed, or left the world). */
        GONE,
        /** The locked target is no longer a creature mobs are hostile to (a creative or spectator player). */
        EXEMPT,
        /** NOT the target, and its grown box meets the segment: listed, never hurt (cfde822 hit the locked one only). */
        SPARED
    }

    /** A live creature near the line at the fire: its id, side, whether it is an exempt player, world and body. */
    public record Candidate(UUID id, MobHostility.Side side, boolean exemptMode, String world, BeamBox box) {}

    /** One creature the fire tested: who, its {@link BeamLine#gap} in blocks, and what happened. */
    public record Tested(UUID id, double gap, Outcome outcome) {}

    /** The fire: every creature tested, the locked target first; and the target's body when it was HIT. */
    public record Shot(List<Tested> tested, Optional<BeamBox> hitBody) {
        public boolean hit() {
            return hitBody.isPresent();
        }
    }

    /**
     * THE FIRE'S DECISION. {@code line} is the locked line already cut where a block stops it; {@code target} the
     * locked target's id; {@code world} the Knell's. A candidate in another world is never tested. The target is
     * HIT only when it is among the candidates, in this world, {@link MobHostility#hostileTo} names it, and its box
     * grown by {@code tolerance} meets the segment. No other creature is ever hit.
     */
    public Shot resolve(BeamLine line, UUID target, String world, List<Candidate> candidates) {
        Tested first = new Tested(target, Double.NaN, Outcome.GONE);
        Optional<BeamBox> hitBody = Optional.empty();
        List<Tested> spared = new ArrayList<>();
        for (Candidate c : candidates) {
            if (!c.world().equals(world)) continue;
            if (c.id().equals(target)) {
                double gap = line.gap(c.box());
                if (!MobHostility.hostileTo(c.side(), c.exemptMode())) {
                    first = new Tested(target, gap, Outcome.EXEMPT);
                } else if (line.meets(c.box(), tolerance)) {
                    first = new Tested(target, gap, Outcome.HIT);
                    hitBody = Optional.of(c.box());
                } else {
                    first = new Tested(target, gap, Outcome.MISS);
                }
            } else if (line.meets(c.box(), tolerance)) {
                spared.add(new Tested(c.id(), line.gap(c.box()), Outcome.SPARED));
            }
        }
        List<Tested> tested = new ArrayList<>();
        tested.add(first);
        tested.addAll(spared);
        return new Shot(List.copyOf(tested), hitBody);
    }

    /**
     * THE BEAM REPORTS ITS HIT (the plan's E5 seam): "this mob's beam landed on this combatant at this tick, from
     * here to there". Empty on a miss, so a listener can never act on one. {@code to} is the point of the segment
     * nearest the hit body's centre.
     */
    public static Optional<BeamHit> report(Shot shot, UUID mob, String mobId, UUID target, long tick, BeamLine line) {
        return shot.hitBody().map(body ->
                new BeamHit(mob, mobId, target, tick, line.origin(), line.nearestTo(body.center())));
    }
}
