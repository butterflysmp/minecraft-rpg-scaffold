package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * THE PHALANX'S BLINK (PLAN-dragon-mobs.md step R7b; the plan's MB4, reduced). Authored in the mob file's
 * {@code blink:} block. Ben, K: the rest of the Phalanx stays, "Yes"; its numbers are cfde822's (section 3.4), starting
 * numbers he left unchanged on 2026-11-17. This class is the PURE half: who triggers it, which points it tries, and which
 * one it lands on, through the Knell's ONE landing check ({@link BeamTeleport#foot}).
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code triggerRange}: BLOCKS, the HALF-EXTENT of the box round the Phalanx's FEET that a combatant's box must
 *       meet (cfde822's D4: "a 5-block half-extent box", READ). A box, not a distance: a corner 5 off on both axes is
 *       inside (5).</li>
 *   <li>{@code near}, {@code far}: BLOCKS, the horizontal distance of each tried point from the Phalanx's feet, drawn
 *       uniformly between them, both included (X5: 6 and 16).</li>
 *   <li>{@code attempts}: how many points one blink tries before it gives up and stays (X6: 8).</li>
 *   <li>{@code levelTolerance}: whole BLOCKS above or below the Phalanx's feet a landing may stand, nearest first (the
 *       Knell's rule). cfde822 stood it on "the column's highest block + 1"; the seat's ruling 3 of R7b sends the blink
 *       through the Knell's check instead, so a blink never lands on a roof. NOT IN THE RECORD: 3, the Knell's.</li>
 *   <li>{@code sounds}, {@code particle}, {@code particleCount}: played and drawn at both ends of a blink (cfde822:
 *       "squid-ink puffs at both ends, an enderman-teleport sound"; the counts NOT IN THE RECORD).</li>
 * </ul>
 */
public record MobBlink(double triggerRange, double near, double far, int attempts, int levelTolerance,
                       List<BeamSound> sounds, String particle, int particleCount) {

    public MobBlink {
        if (!(triggerRange > 0 && Double.isFinite(triggerRange))) {
            throw new IllegalArgumentException("blink.trigger_range must be above 0 blocks, was " + triggerRange);
        }
        if (!(near >= 0 && Double.isFinite(near))) throw new IllegalArgumentException("blink.near must be 0 or more blocks, was " + near);
        if (!(far >= near && Double.isFinite(far))) {
            throw new IllegalArgumentException("blink.far (" + far + ") must be at least blink.near (" + near + ")");
        }
        if (attempts < 1) throw new IllegalArgumentException("blink.attempts must be at least 1, was " + attempts);
        if (levelTolerance < 0) throw new IllegalArgumentException("blink.level_tolerance must be 0 or more, was " + levelTolerance);
        if (particle == null || particle.isBlank()) throw new IllegalArgumentException("blink.particle is required");
        if (particleCount < 0) throw new IllegalArgumentException("blink.particle_count must be 0 or more, was " + particleCount);
        sounds = List.copyOf(sounds);
    }

    /**
     * Does this combatant trigger a blink? {@link MobHostility#hostileTo} must name it (a player today, never a mob; a
     * creative or spectator player never), and its box must meet the cube of half-extent {@code triggerRange} round the
     * Phalanx's feet, faces included: its box grown by the range contains the feet.
     */
    public boolean triggered(Vec3 mobFeet, BeamBox body, MobHostility.Side side, boolean exemptMode) {
        if (!MobHostility.hostileTo(side, exemptMode)) return false;
        BeamBox g = body.grow(triggerRange);
        return mobFeet.x() >= g.min().x() && mobFeet.x() <= g.max().x()
                && mobFeet.y() >= g.min().y() && mobFeet.y() <= g.max().y()
                && mobFeet.z() >= g.min().z() && mobFeet.z() <= g.max().z();
    }

    /** One tried point: an angle in DEGREES (0 is +x, 90 is +z) and a horizontal distance in BLOCKS. */
    public record Draw(double angleDegrees, double distance) {}

    /** The blink's randomness, injected: production draws uniformly; the instrument fixes it. */
    public interface Source {
        Draw next(MobBlink b);
    }

    /** Uniform: the angle in [0, 360), the distance in [near, far]. */
    public static Source uniform(RandomGenerator random) {
        return b -> new Draw(random.nextDouble() * 360.0,
                b.far() == b.near() ? b.near() : b.near() + random.nextDouble() * (b.far() - b.near()));
    }

    /** {@code attempts} draws from the source, in order. */
    public List<Draw> draws(Source source) {
        List<Draw> out = new ArrayList<>();
        for (int i = 0; i < attempts; i++) out.add(source.next(this));
        return out;
    }

    /** One tried point and what the landing check made of it. */
    public record Verdict(Draw draw, Vec3 column, BeamTeleport.Outcome outcome, Optional<Vec3> landing, int blocked,
                          int ungrounded) {}

    /** The decision: every point tried in order, and the landing, or none (it stays). */
    public record Decision(List<Verdict> tried, Optional<Vec3> landing) {}

    /**
     * THE DECISION. Each draw in order: its column is the Phalanx's feet moved {@code distance} along {@code angle}
     * (feet.y kept, the height the check searches round); the Knell's check ({@link BeamTeleport#foot}, sight never
     * asked: cfde822's blink had none). The first OK wins and nothing after it is asked; none OK, no landing.
     */
    public Decision decide(Vec3 mobFeet, List<Draw> draws, BeamTeleport.Footing footing) {
        List<Verdict> tried = new ArrayList<>();
        for (Draw d : draws) {
            double a = Math.toRadians(d.angleDegrees());
            Vec3 column = new Vec3(mobFeet.x() + Math.cos(a) * d.distance(), mobFeet.y(),
                    mobFeet.z() + Math.sin(a) * d.distance());
            BeamTeleport.Foothold f = BeamTeleport.foot(column, levelTolerance, false, footing);
            Verdict v = new Verdict(d, column, f.outcome(), f.landing(), f.blocked(), f.ungrounded());
            tried.add(v);
            if (f.outcome() == BeamTeleport.Outcome.OK) return new Decision(List.copyOf(tried), f.landing());
        }
        return new Decision(List.copyOf(tried), Optional.empty());
    }
}
