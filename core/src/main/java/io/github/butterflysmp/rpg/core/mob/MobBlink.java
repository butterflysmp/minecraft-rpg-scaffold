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
 *   <li>{@code cooldownTicks} (step R8): TICKS after a blink that LANDS before the Phalanx may decide another. Ben,
 *       2026-11-17: "needs a larger cooldown. Say 12 seconds" (240). A decision that lands nowhere does not start it
 *       (the seat's reading, put to Ben, not objected to). 0 is no cooldown; a negative value is refused by name.</li>
 * </ul>
 */
public record MobBlink(double triggerRange, double near, double far, int attempts, int levelTolerance,
                       List<BeamSound> sounds, String particle, int particleCount, int cooldownTicks) {

    /** Step R7b's eight-number form: no cooldown. Kept so its call sites read unchanged. */
    public MobBlink(double triggerRange, double near, double far, int attempts, int levelTolerance,
                    List<BeamSound> sounds, String particle, int particleCount) {
        this(triggerRange, near, far, attempts, levelTolerance, sounds, particle, particleCount, 0);
    }

    public MobBlink {
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("blink.cooldown_ticks must be 0 or more ticks, was " + cooldownTicks);
        }
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
        return decide(mobFeet, mobFeet.y(), draws, footing);
    }

    /**
     * THE DECISION, its height search anchored at {@code anchorY} rather than at the Phalanx's own feet (step R8, R3).
     * Each column is the feet moved {@code distance} along {@code angle} horizontally, at {@code anchorY}; the Knell's
     * check then tries whole blocks above and below THAT. Paper passes {@link #anchor}.
     */
    public Decision decide(Vec3 mobFeet, double anchorY, List<Draw> draws, BeamTeleport.Footing footing) {
        List<Verdict> tried = new ArrayList<>();
        for (Draw d : draws) {
            double a = Math.toRadians(d.angleDegrees());
            Vec3 column = new Vec3(mobFeet.x() + Math.cos(a) * d.distance(), anchorY,
                    mobFeet.z() + Math.sin(a) * d.distance());
            BeamTeleport.Foothold f = BeamTeleport.foot(column, levelTolerance, false, footing);
            Verdict v = new Verdict(d, column, f.outcome(), f.landing(), f.blocked(), f.ungrounded());
            tried.add(v);
            if (f.outcome() == BeamTeleport.Outcome.OK) return new Decision(List.copyOf(tried), f.landing());
        }
        return new Decision(List.copyOf(tried), Optional.empty());
    }

    /**
     * R3 (step R8): THE HEIGHT A BLINK'S SEARCH IS ANCHORED AT: the WHOLE BLOCK its feet are in, floor(y). The Knell's
     * check tries whole blocks above and below the column's y, and a full block's top is a whole number, so a search
     * anchored at a FRACTIONAL height never stands a body on a floor: every height is (say) 0.305 into a block, so it
     * does not fit, or 0.305 above one, so it has no ground. BQ1 met it mid-jump at 63.305; a Phalanx standing on a slab
     * (63.5) meets it on the ground. From floor(y), every height tried is whole.
     *
     * <p>Why not the two leans the seat offered. The ground beneath the Phalanx: on a slab it IS on the ground, at 63.5,
     * and fails the same way. The triggering combatant's feet (the Knell's teleport searches from its target's): a forced
     * blink has no trigger, a combatant mid-jump is fractional too, and the blink's ring of 6 to 16 is measured from the
     * Phalanx. A cost, stated: a landing on a slab's top (x.5) is never tried, as it never was from a whole-block height.
     *
     * <p>{@link #ANCHOR_SNAP} lifts a height within a millionth below a whole number onto it, so floating-point drift on a
     * floor (62.9999999999) anchors on 63, its own block, not 62.
     */
    public static double anchor(double feetY) {
        return Math.floor(feetY + ANCHOR_SNAP);
    }

    /** BLOCKS: how far below a whole number a height may sit and still be anchored on it. NOT IN THE RECORD: chosen. */
    public static final double ANCHOR_SNAP = 1.0E-6;

    /** This blink's cooldown, a fresh counter for one live Phalanx (its loop holds it; it dies with the loop). */
    public Cooldown cooldown() {
        return new Cooldown(cooldownTicks);
    }

    /**
     * THE COOLDOWN ON ONE LIVE PHALANX (step R8). {@link #landed} starts it at {@code cooldownTicks}; each loop tick
     * {@link #tick} counts one down BEFORE the blink is considered. A blink that lands on loop tick L is followed by
     * {@code cooldownTicks - 1} ticks on which it is running, and the earliest next decision is on tick
     * L + {@code cooldownTicks}. Nothing else starts it: a decision that lands nowhere leaves it as it was.
     */
    public static final class Cooldown {
        private final int length;
        private int remaining;

        Cooldown(int length) {
            this.length = length;
        }

        public void tick() {
            if (remaining > 0) remaining--;
        }

        public void landed() {
            remaining = length;
        }

        public boolean running() {
            return remaining > 0;
        }

        public int remaining() {
            return remaining;
        }
    }
}
