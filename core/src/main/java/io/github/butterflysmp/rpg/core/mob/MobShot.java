package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * THE PHALANX'S SHOT (PLAN-dragon-mobs.md step R7a; the plan's N7). Authored in the mob file's {@code shot:} block.
 * Ben, 2026-11-17: "change the duration of the shot to 12s and it fires a shot every 6s"; every other number is the
 * old game's (cfde822, section 3.4), a starting number he left unchanged. This class is the PURE half: the flight,
 * its endings and the price. Who is aimed at, and what a block or a body is, are paper's.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code periodTicks}: TICKS from the Phalanx appearing, or from its last attempt (fired or not), to its next
 *       attempt (X7: 120).</li>
 *   <li>{@code acquireRange}: BLOCKS, the Phalanx's EYE to a candidate's AIM POINT, inclusive: who it may fire at
 *       (X8: 30).</li>
 *   <li>{@code reacquireRange}: BLOCKS, the SHOT'S position to a candidate's aim point, inclusive: who a shot whose
 *       target is lost turns to (X8's re-acquire: 35).</li>
 *   <li>{@code speed}: BLOCKS PER TICK, the length of every step (X9: 0.2; cfde822 moved 0.4 every 2 ticks).</li>
 *   <li>{@code homing}: a FRACTION, 0 to 1. Each tick the shot's unit DIRECTION OF FLIGHT and the unit direction
 *       FROM THE SHOT TO ITS TARGET'S AIM POINT are blended, {@code (1 - homing) x flight + homing x toward}, and the
 *       sum is renormalised; the speed never changes. 0 flies straight; 1 points at the target every tick (X10: 0.1,
 *       cfde822's 10% per STEP, and its step was 2 ticks).</li>
 *   <li>{@code lifeTicks}: TICKS of flight after which it expires (Ben: 12 s = 240).</li>
 *   <li>{@code maxDistance}: BLOCKS, the straight line from its ORIGIN (where it was fired) to where it is: past
 *       this it expires. cfde822: "past 35 blocks from its start", so a DISTANCE FROM THE ORIGIN, not the path
 *       flown (35).</li>
 *   <li>{@code tolerance}: BLOCKS a combatant's box is grown by on EVERY side: the shot hits when its position is
 *       inside the grown box, faces included. Every aim point is a body's EYE (cfde822: "aimed at the target's eye"). cfde822: the first body within a 1.2 half-extent box around the swirl,
 *       which is the same test turned round (X12: 1.2).</li>
 *   <li>{@code damage}: the AUTHORED damage of a hit, before gear score and before the victim's defence (X13: 40).</li>
 * * </ul>
 */
public record MobShot(int periodTicks, double acquireRange, double reacquireRange, double speed, double homing,
                      int lifeTicks, double maxDistance, double tolerance, double damage,
                      ShotLook look, List<BeamSound> fireSounds, List<BeamSound> destroySounds) {

    public MobShot {
        if (periodTicks < 1) throw new IllegalArgumentException("shot.period_ticks must be at least 1, was " + periodTicks);
        positive("shot.acquire_range", acquireRange);
        positive("shot.reacquire_range", reacquireRange);
        positive("shot.speed", speed);
        if (!(homing >= 0 && homing <= 1)) {
            throw new IllegalArgumentException("shot.homing must be a fraction, 0 to 1, was " + homing);
        }
        if (lifeTicks < 1) throw new IllegalArgumentException("shot.life_ticks must be at least 1, was " + lifeTicks);
        positive("shot.max_distance", maxDistance);
        if (!(tolerance >= 0 && Double.isFinite(tolerance))) {
            throw new IllegalArgumentException("shot.tolerance must be 0 or more blocks, was " + tolerance);
        }
        if (!(damage >= 0 && Double.isFinite(damage))) {
            throw new IllegalArgumentException("shot.damage must be 0 or more, was " + damage);
        }
        if (look == null) throw new IllegalArgumentException("shot.look is required");
        fireSounds = List.copyOf(fireSounds);
        destroySounds = List.copyOf(destroySounds);
    }

    private static void positive(String name, double v) {
        if (!(v > 0 && Double.isFinite(v))) throw new IllegalArgumentException(name + " must be above 0, was " + v);
    }

    /** What a hit deals BEFORE the victim's defence: the authored damage, priced as every custom mob's hit is. */
    public double price(boolean hostile, int gearScore) {
        return MobScaling.attackDamage(damage, true, hostile, gearScore);
    }

    /** May the Phalanx fire at a body whose aim point is {@code aim}? Eye to aim point, inclusive, straight line. */
    public boolean inAcquireRange(Vec3 eye, Vec3 aim) {
        return eye.distanceSquared(aim) <= acquireRange * acquireRange;
    }

    /** May a shot at {@code position} whose target is lost turn to a body whose aim point is {@code aim}? */
    public boolean inReacquireRange(Vec3 position, Vec3 aim) {
        return position.distanceSquared(aim) <= reacquireRange * reacquireRange;
    }

    /** One live shot: where it was fired from, where it is, its velocity (BLOCKS PER TICK) and its age (TICKS). */
    public record State(Vec3 origin, Vec3 position, Vec3 velocity, int age) {}

    /** The shot as fired: at {@code origin} (the Phalanx's eye), aimed straight at {@code aim}, age 0. */
    public State launch(Vec3 origin, Vec3 aim) {
        Vec3 d = aim.subtract(origin);
        if (d.lengthSquared() == 0) throw new IllegalArgumentException("a shot cannot be aimed at its own origin " + origin);
        return new State(origin, origin, d.normalize().scale(speed), 0);
    }

    /**
     * ONE TICK OF FLIGHT: turn, then move. {@code aim} is the target's aim point now, or empty when it has none (it
     * then flies straight on). A zero-length "toward" (the shot exactly at the aim point) or a zero-length blend (the
     * target exactly behind at {@code homing} 0.5) keeps the old direction.
     */
    public State step(State s, Optional<Vec3> aim) {
        Vec3 flight = s.velocity().normalize();
        if (aim.isPresent()) {
            Vec3 toward = aim.get().subtract(s.position());
            if (toward.lengthSquared() > 0) {
                Vec3 blend = flight.scale(1.0 - homing).add(toward.normalize().scale(homing));
                if (blend.lengthSquared() > 0) flight = blend.normalize();
            }
        }
        Vec3 velocity = flight.scale(speed);
        return new State(s.origin(), s.position().add(velocity), velocity, s.age() + 1);
    }

    /** How a shot's flight ended. */
    public enum Ending {
        /** Its path this tick met a block with a collision box. */
        BLOCKED,
        /** It came within the tolerance of a combatant mobs are hostile to: that one is dealt the damage. */
        HIT,
        /** It reached {@code lifeTicks} of flight. */
        EXPIRED_LIFE,
        /** It is more than {@code maxDistance} from its origin. */
        EXPIRED_RANGE
    }

    /**
     * THE ENDINGS, checked after each tick's move, IN THIS ORDER: a block first (cfde822's order is not in the record;
     * chosen so a wall between the shot and a player always saves him), then a hit, then the life, then the range.
     *
     * @param blocked the path this tick, from the last position to this one, met a block
     * @param hit     the new position is inside a hostile combatant's box grown by the tolerance ({@link #meets})
     */
    public Optional<Ending> ending(State s, boolean blocked, boolean hit) {
        if (blocked) return Optional.of(Ending.BLOCKED);
        if (hit) return Optional.of(Ending.HIT);
        if (s.age() >= lifeTicks) return Optional.of(Ending.EXPIRED_LIFE);
        if (s.origin().distanceSquared(s.position()) > maxDistance * maxDistance) return Optional.of(Ending.EXPIRED_RANGE);
        return Optional.empty();
    }

    /** Is {@code position} inside {@code box} grown by the tolerance on every side? Inclusive. */
    public boolean meets(BeamBox box, Vec3 position) {
        BeamBox g = box.grow(tolerance);
        return position.x() >= g.min().x() && position.x() <= g.max().x()
                && position.y() >= g.min().y() && position.y() <= g.max().y()
                && position.z() >= g.min().z() && position.z() <= g.max().z();
    }

    /**
     * WHICH COMBATANT A SHOT AT {@code position} HITS, of those near it: the nearest (box centre to the position) whose
     * grown box contains the position and whom {@link MobHostility#hostileTo} names. cfde822 hit "the first living
     * entity" in the box, mobs included; Ben's ruling (seat ruling 5 of R7a) is that it never hurts a mob, so a mob in
     * the box is passed over and the shot flies on. Empty when none.
     */
    public Optional<Candidate> victim(Vec3 position, String world, List<Candidate> candidates) {
        Candidate best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (Candidate c : candidates) {
            if (!c.world().equals(world) || !MobHostility.hostileTo(c.side(), c.exemptMode())) continue;
            if (!meets(c.box(), position)) continue;
            double d = c.box().center().distanceSquared(position);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return Optional.ofNullable(best);
    }

    /** A live body near a shot: its id, side, whether it is an exempt player, its world and its box. */
    public record Candidate(java.util.UUID id, MobHostility.Side side, boolean exemptMode, String world, BeamBox box) {}

    /**
     * THE SHOT'S LOOK, as content (seat ruling 8 of R7a; CHOSEN, for Ben). {@code block} is the BLOCK key a small
     * display shows; {@code size} its edge in BLOCKS; {@code glow} the display's glow outline colour; {@code trail} a
     * dust particle (its colour and SIZE, the particle's own scale) drawn at the shot every tick; {@code flash} a
     * second dust drawn every {@code flashEveryTicks} ticks. {@code hitBox}: the edge, in BLOCKS, of the cube a player
     * can HIT (the carrier's box), centred on the shape and at least as big as it (the seat's ruling 2 on #201: what
     * he sees is what he can hit). It changes nothing about whom the SHOT hits: that is its centre against a body's box
     * grown by the tolerance. It is what is swept for blocks, so a bigger box meets a wall sooner, never later.
     */
    public record ShotLook(String block, double size, double hitBox, int glowRgb, int trailRgb, double trailSize, int flashRgb,
                           double flashSize, int flashEveryTicks) {
        public ShotLook {
            if (block == null || block.isBlank()) throw new IllegalArgumentException("shot.look.block is required");
            if (!(size > 0 && size <= 4)) throw new IllegalArgumentException("shot.look.size must be above 0 and at most 4 blocks, was " + size);
            if (!(hitBox >= size && hitBox <= 4)) {
                throw new IllegalArgumentException("shot.hit_box must be at least shot.look.size (" + size
                        + ") and at most 4 blocks, was " + hitBox);
            }
            rgb("shot.look.glow", glowRgb);
            rgb("shot.look.trail.rgb", trailRgb);
            rgb("shot.look.flash.rgb", flashRgb);
            if (!(trailSize > 0 && trailSize <= 4)) throw new IllegalArgumentException("shot.look.trail.size must be above 0 and at most 4, was " + trailSize);
            if (!(flashSize > 0 && flashSize <= 4)) throw new IllegalArgumentException("shot.look.flash.size must be above 0 and at most 4, was " + flashSize);
            if (flashEveryTicks < 1) throw new IllegalArgumentException("shot.look.flash.every_ticks must be at least 1, was " + flashEveryTicks);
        }

        private static void rgb(String name, int v) {
            if (v < 0 || v > 0xFFFFFF) throw new IllegalArgumentException(name + " must be an RGB colour, was " + v);
        }

        /** Is {@code age} (0 at the fire) a flash tick? */
        public boolean flashes(int age) {
            return age % flashEveryTicks == 0;
        }
    }

    /**
     * THE PHALANX'S CLOCK (X7): an attempt every {@code periodTicks}, the first that long after it appears; the count
     * starts again at every attempt, fired or not (cfde822: "No target: the counter resets and it tries again in
     * 100 t"). A FORCED attempt (the instrument's) is due on the next tick and resets the count the same way. No
     * randomness: the record has none.
     */
    public static final class Clock {
        private final int period;
        private int elapsed;
        private boolean forced;

        public Clock(int period) {
            if (period < 1) throw new IllegalArgumentException("period must be at least 1, was " + period);
            this.period = period;
        }

        public enum Step { WAIT, ATTEMPT, FORCED }

        /** One tick. ATTEMPT or FORCED resets the count. */
        public Step tick() {
            elapsed++;
            if (forced) {
                forced = false;
                elapsed = 0;
                return Step.FORCED;
            }
            if (elapsed >= period) {
                elapsed = 0;
                return Step.ATTEMPT;
            }
            return Step.WAIT;
        }

        /** The instrument: the next tick is a forced attempt. */
        public void force() {
            forced = true;
        }

        /** Ticks since the last attempt (or the start). */
        public int elapsed() {
            return elapsed;
        }
    }
}
