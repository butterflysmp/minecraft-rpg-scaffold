package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * THE SKREEL'S POUNCE (PLAN-dragon-mobs.md step R8; the plan's MB5): the numbers and the pure rules of the pair's cycle.
 * Authored in the RIDER'S file ({@code skreel.yml}, under {@code pounce:}), because the Skreel is the mob the plan names
 * and its file names the mount it rides. The state machine is {@link PounceCycle}; this class is everything it and paper
 * compute from a number.
 *
 * <p><b>THE NUMBERS ARE THE SEAT'S, ACCEPTED BY BEN AS A STARTING POINT, 2026-11-17</b> ("I like your numbers. Let's start
 * there"); where they are silent, cfde822's (section 3.5, READ). <b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code acquireRange}: BLOCKS, feet to feet from the MOUNT, inclusive: who the pair may pick as its target (the
 *       nearest combatant {@link MobHostility} names). The seat's 32 (cfde822 50).</li>
 *   <li>{@code bandNear}, {@code bandFar}: BLOCKS, feet to feet, both included: the band it circles at, and in which a
 *       wind-up may begin. 8 and 12 (cfde822's).</li>
 *   <li>{@code pathSpeed}: the pathfinder's SPEED MULTIPLIER while it closes in on the band (cfde822's "speed arg 1.6").
 *       It multiplies the mount's own movement speed, the ridden 1.3x included.</li>
 *   <li>{@code windupTicks}: TICKS from a wind-up's first tick to its launch, rooted throughout. The seat's 1 second, 20
 *       (cfde822 15).</li>
 *   <li>{@code periodTicks}: TICKS from one wind-up's FIRST tick to the next wind-up's earliest first tick, and from the
 *       pair appearing to its first. The seat's 6 seconds, 120 (cfde822: none).</li>
 *   <li>{@code launchVertical}: BLOCKS PER TICK, the leap's upward speed at its launch (cfde822's 1.2). On level ground
 *       it rises 8.19 blocks and comes down 29 ticks later (jshell, 2026-11-17, under vanilla's 0.08 gravity and 0.98f
 *       drag).</li>
 *   <li>{@code timeoutTicks}: TICKS after the launch at which a leap that has not landed ends where it is, and its hit is
 *       tested there (cfde822's 40).</li>
 *   <li>{@code reach}: BLOCKS, feet to feet from the MOUNT'S feet where it lands, inclusive: who the landing hits. The
 *       seat's 3 (cfde822 2.5).</li>
 *   <li>{@code damage}: the AUTHORED damage of the landing hit, x the pair's gear score / 100 ({@link #price}), before the
 *       victim's defence. No element. The seat's 55 (cfde822 35).</li>
 *   <li>{@code knockbackHorizontal}, {@code knockbackVertical}: BLOCKS PER TICK added to a victim's velocity: horizontally
 *       away from the landing point, and straight up, each x (1 - its knockback resistance). cfde822's 0.7 and 0.4, the
 *       seat's "moderate" (PLAN RULINGS, measured against the blast's).</li>
 *   <li>{@code windupParticle}, {@code windupParticleCount}, {@code windupParticleEvery}: the wind-up's sight. cfde822: 15
 *       crit particles on entry (READ); repeated every {@code windupParticleEvery} ticks through the wind-up (CHOSEN, so
 *       the warning stays visible for its whole second).</li>
 *   <li>{@code windupSounds}, {@code launchSounds}, {@code landSounds}: cfde822's spider-ambient on the wind-up, spider-hurt
 *       at the launch, a crit at the landing (READ); their volumes and pitches CHOSEN.</li>
 * </ul>
 */
public record MobPounce(double acquireRange, double bandNear, double bandFar, double pathSpeed, int windupTicks,
                        int periodTicks, double launchVertical, int timeoutTicks, double reach, double damage,
                        double knockbackHorizontal, double knockbackVertical, String windupParticle,
                        int windupParticleCount, int windupParticleEvery, List<BeamSound> windupSounds,
                        List<BeamSound> launchSounds, List<BeamSound> landSounds) {

    /**
     * The drag a living body's vertical speed takes each tick in the air, READ in the pinned server jar:
     * {@code LivingEntity#travelInAir} multiplies (y - gravity) by {@code ldc 0.98f}, a FLOAT widened to double, so
     * exactly 0.9800000190734863 ({@code javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar
     * net.minecraft.world.entity.LivingEntity}).
     */
    public static final double AIR_DRAG = (double) 0.98f;

    public MobPounce {
        positive(acquireRange, "pounce.acquire_range");
        if (!(bandNear > 0 && Double.isFinite(bandNear))) {
            throw new IllegalArgumentException("pounce.band.near must be above 0 blocks, was " + bandNear);
        }
        if (!(bandFar >= bandNear && Double.isFinite(bandFar))) {
            throw new IllegalArgumentException("pounce.band.far (" + bandFar + ") must be at least pounce.band.near ("
                    + bandNear + ")");
        }
        positive(pathSpeed, "pounce.path_speed");
        if (windupTicks < 1) throw new IllegalArgumentException("pounce.windup_ticks must be at least 1, was " + windupTicks);
        if (periodTicks < 1) throw new IllegalArgumentException("pounce.period_ticks must be at least 1, was " + periodTicks);
        positive(launchVertical, "pounce.launch_vertical");
        if (timeoutTicks < 1) throw new IllegalArgumentException("pounce.timeout_ticks must be at least 1, was " + timeoutTicks);
        positive(reach, "pounce.reach");
        nonNegative(damage, "pounce.damage");
        nonNegative(knockbackHorizontal, "pounce.knockback.horizontal");
        nonNegative(knockbackVertical, "pounce.knockback.vertical");
        if (windupParticle == null || windupParticle.isBlank()) {
            throw new IllegalArgumentException("pounce.windup.particle is required");
        }
        if (windupParticleCount < 0) {
            throw new IllegalArgumentException("pounce.windup.particle_count must be 0 or more, was " + windupParticleCount);
        }
        if (windupParticleEvery < 1) {
            throw new IllegalArgumentException("pounce.windup.every_ticks must be at least 1, was " + windupParticleEvery);
        }
        windupSounds = List.copyOf(windupSounds);
        launchSounds = List.copyOf(launchSounds);
        landSounds = List.copyOf(landSounds);
    }

    private static void positive(double v, String key) {
        if (!(v > 0 && Double.isFinite(v))) throw new IllegalArgumentException(key + " must be above 0, was " + v);
    }

    private static void nonNegative(double v, String key) {
        if (!(v >= 0 && Double.isFinite(v))) throw new IllegalArgumentException(key + " must be 0 or more, was " + v);
    }

    /** Is a target at {@code distance} (feet to feet) in the band? Both edges included. */
    public boolean inBand(double distance) {
        return distance >= bandNear && distance <= bandFar;
    }

    /**
     * BLOCKS from the target's feet to the stand-off point: the band's MIDDLE, (near + far) / 2, 10 at the shipped 8 and
     * 12 (the seat's ruling after the boot of 45bc33ce, 2026-11-17). Never an edge: the pathfinder ends a path with the
     * mob only NEAR its goal (MobPounceTest's {@code theBandsHalfWidthExceedsTheMountsStopOnAnAxisAndNotOnADiagonal} has
     * the jar's rule), so a goal on an edge is met outside the band about half the time. That was THE BAND EDGE: cfde822's
     * goal was the far edge, 12 (READ: "the point 12 blocks from the player"), PS6's mount parked at 12.4459, and no
     * wind-up began.
     */
    public double standOffDistance() {
        return (bandNear + bandFar) / 2.0;
    }

    /**
     * The IDEAL point for a pair outside the band, nearer or farther: {@link #standOffDistance} from the target's feet,
     * horizontally, toward the mount, at the target's height. A mount straight above or below the target has no axis; +x
     * is taken. The pair is never sent here as it stands: {@link #goal} picks the block near it that the pathfinder's
     * stopping rule cannot leave outside the band.
     */
    public Vec3 standOff(Vec3 targetFeet, Vec3 mountFeet) {
        double r = standOffDistance();
        double dx = mountFeet.x() - targetFeet.x();
        double dz = mountFeet.z() - targetFeet.z();
        double h = Math.hypot(dx, dz);
        if (h == 0) return new Vec3(targetFeet.x() + r, targetFeet.y(), targetFeet.z());
        return new Vec3(targetFeet.x() + dx / h * r, targetFeet.y(), targetFeet.z() + dz / h * r);
    }

    /**
     * THE STOPPING RULE'S TOLERANCE, READ ({@code PathNavigation#followThePath}, {@code javap -c -p -cp
     * run/versions/26.1.2/paper-26.1.2.jar net.minecraft.world.entity.ai.navigation.PathNavigation}): BLOCKS, how near a
     * node's block centre a mob of this width must be, on x and on z, to have reached it. Width / 2 over 0.75 wide, else
     * 0.75 - width / 2. The mount (a spider, 1.4 wide, at scale 1.5): 1.05.
     */
    public static double waypointTolerance(double width) {
        return width > 0.75 ? width / 2 : 0.75 - width / 2;
    }

    /**
     * THE STOPPING RULE, READ: a path to {@code goal} is done for a mob at {@code at} when it is under {@code tolerance}
     * from the goal BLOCK's centre on x and on z, and under 1 from the block's floor on y. Paper's {@code moveTo(Location)}
     * builds the path with {@code createPath(x, y, z, 0)}, to the goal's own block, and the last node is never passed early
     * ({@code shouldTargetNextNodeInDirection} returns false on it).
     */
    public static boolean pathDone(Vec3 at, Vec3 goal, double tolerance) {
        return Math.abs(at.x() - (Math.floor(goal.x()) + 0.5)) < tolerance
                && Math.abs(at.z() - (Math.floor(goal.z()) + 0.5)) < tolerance
                && Math.abs(at.y() - Math.floor(goal.y())) < 1;
    }

    /** BLOCKS outside the band a feet-to-feet distance is: near - d below it, d - far above it, 0 inside. */
    public double bandGap(double distance) {
        if (distance < bandNear) return bandNear - distance;
        if (distance > bandFar) return distance - bandFar;
        return 0;
    }

    /**
     * BLOCKS nearer the band a pair must come for its walk to count as progress (the cannot-reach rule, {@link PounceCycle}).
     * CHOSEN: one block; a walking mount covers it in about two ticks (PS3 read 0.65 a tick), so only a pair that is not
     * getting there goes a whole period without it.
     */
    public static final double PROGRESS_BLOCKS = 1.0;

    /**
     * BLOCKS, CHOSEN: the band is required to hold the goal block's stopping box GROWN by this on every side, for what the
     * model does not see (a path's nodes are not a straight line; a mob slides on after its path ends).
     */
    public static final double GOAL_MARGIN = 0.25;

    /**
     * WHERE THE PAIR WALKS WHEN IT IS OUTSIDE ITS BAND (the seat's ruling after its second diff: the pair must come to rest
     * inside the band from ANY bearing). The ideal point is {@link #standOff}, the band's middle along the target-to-mount
     * line; but the pathfinder walks to a BLOCK and stops anywhere under {@code tolerance} from its centre on each axis
     * ({@link #pathDone}), so the middle's own block can leave the mount stopped outside the band on a diagonal (1.55 x
     * sqrt 2 = 2.19 against the band's half-width of 2). So this chooses, among the blocks within 3 of the ideal point's,
     * the one nearest the ideal point whose stopping box, grown by {@link #GOAL_MARGIN}, lies wholly inside the band
     * (horizontally, on open ground). Then the walk crosses into the band before its path can end, and the cycle stands it
     * there; and a path that did end would end inside the band. Returns that block's centre at the target's height; the
     * ideal point's block when no block qualifies (a band too narrow for the body: none at the shipped numbers, the sweep
     * in MobPounceTest).
     */
    public Vec3 goal(Vec3 targetFeet, Vec3 mountFeet, double tolerance) {
        Vec3 ideal = standOff(targetFeet, mountFeet);
        int ix = (int) Math.floor(ideal.x());
        int iz = (int) Math.floor(ideal.z());
        double h = tolerance + GOAL_MARGIN;
        double bestX = ix + 0.5;
        double bestZ = iz + 0.5;
        double best = Double.POSITIVE_INFINITY;
        for (int bx = ix - 3; bx <= ix + 3; bx++) {
            for (int bz = iz - 3; bz <= iz + 3; bz++) {
                double cx = bx + 0.5;
                double cz = bz + 0.5;
                double ax = Math.abs(cx - targetFeet.x());
                double az = Math.abs(cz - targetFeet.z());
                double farthest = Math.hypot(ax + h, az + h);
                double nearest = Math.hypot(Math.max(ax - h, 0), Math.max(az - h, 0));
                if (farthest > bandFar || nearest < bandNear) continue;
                double off = Math.hypot(cx - ideal.x(), cz - ideal.z());
                if (off < best) {
                    best = off;
                    bestX = cx;
                    bestZ = cz;
                }
            }
        }
        return new Vec3(bestX, targetFeet.y(), bestZ);
    }

    /** What the landing deals BEFORE the victim's defence: the authored damage, priced as every custom mob's hit is. */
    public double price(boolean hostile, int gearScore) {
        return MobScaling.attackDamage(damage, true, hostile, gearScore);
    }

    /**
     * The launch: {@code velocity} (BLOCKS PER TICK, set on the mount at the launch), the flight's {@code ticks}, and
     * the {@code horizontal} distance to the aim.
     */
    public record Leap(Vec3 velocity, int ticks, double horizontal) {}

    /**
     * THE LEAP'S VELOCITY, so the pair comes down on {@code aim}. The vertical is {@code launchVertical} (cfde822's); the
     * loop HOLDS the horizontal velocity at its launch value every tick of the flight (cfde822 re-set it every tick, READ;
     * here toward the FIXED aim, not the player), so the horizontal distance after n ticks is exactly n x the speed.
     *
     * <p>The flight's length T is the first tick n at which the height risen, y(n) = the sum of the first n vertical
     * speeds, with v(0) = {@code launchVertical} and v(i+1) = (v(i) - {@code gravity}) x {@link #AIR_DRAG}, is at or
     * below the aim's height while FALLING (v(n-1) &lt; 0); and at most {@code timeoutTicks}. Then the horizontal speed is
     * the horizontal distance / T, along the line to the aim. An aim above the arc's top is met by its first falling
     * tick, and the pair lands short, where it lands.
     *
     * @param gravity the mount's own {@code GRAVITY} attribute value, which paper reads off the mob (vanilla's 0.08)
     */
    public Leap leap(Vec3 from, Vec3 aim, double gravity) {
        double dy = aim.y() - from.y();
        double v = launchVertical;
        double y = 0;
        int ticks = timeoutTicks;
        for (int n = 1; n <= timeoutTicks; n++) {
            y += v;
            boolean falling = v < 0;
            v = (v - gravity) * AIR_DRAG;
            if (falling && y <= dy) {
                ticks = n;
                break;
            }
        }
        double dx = aim.x() - from.x();
        double dz = aim.z() - from.z();
        double h = Math.hypot(dx, dz);
        double speed = h / ticks;
        Vec3 velocity = h == 0 ? new Vec3(0, launchVertical, 0)
                : new Vec3(dx / h * speed, launchVertical, dz / h * speed);
        return new Leap(velocity, ticks, h);
    }

    /** One creature within the reach of the landing, and its distance, feet to feet. */
    public record Struck(MobBlast.Bystander who, double distance) {}

    /** Who the landing reaches: {@code victims} are hit, nearest first; {@code spared} are within reach and never hit. */
    public record Landing(List<Struck> victims, List<Struck> spared) {}

    /**
     * THE LANDING'S RULE, at the mount's FEET where it came down. Every creature in the same world whose feet are within
     * {@code reach} of them (a sphere, inclusive) is a VICTIM when {@link MobHostility#hostileTo} names it (a survival or
     * adventure player today), and is SPARED otherwise (any mob, the pair's own two included if paper passes them; a
     * creative or spectator player). Each list nearest first. Beyond the reach, or in another world: in neither.
     */
    public Landing landing(Vec3 at, String world, List<MobBlast.Bystander> bystanders) {
        List<Struck> victims = new ArrayList<>();
        List<Struck> spared = new ArrayList<>();
        for (MobBlast.Bystander b : bystanders) {
            if (!world.equals(b.world())) continue;
            double d = Math.sqrt(at.distanceSquared(b.at()));
            if (d > reach) continue;
            (MobHostility.hostileTo(b.side(), b.exemptMode()) ? victims : spared).add(new Struck(b, d));
        }
        victims.sort(Comparator.comparingDouble(Struck::distance));
        spared.sort(Comparator.comparingDouble(Struck::distance));
        return new Landing(List.copyOf(victims), List.copyOf(spared));
    }

    /**
     * THE PUSH, added to a victim's velocity (cfde822, READ: "knockback 0.7 horizontal away from the spider, 0.4
     * vertical"): {@code knockbackHorizontal} along the horizontal line from the landing point to the victim's feet, and
     * {@code knockbackVertical} up, both x (1 - {@code resistance}), the resistance clamped to 0..1. A victim exactly over
     * the landing point has no horizontal direction and is pushed straight up.
     */
    public Vec3 push(Vec3 landing, Vec3 victimFeet, double resistance) {
        double keep = 1.0 - Math.max(0.0, Math.min(1.0, resistance));
        double dx = victimFeet.x() - landing.x();
        double dz = victimFeet.z() - landing.z();
        double h = Math.hypot(dx, dz);
        double hx = h == 0 ? 0 : dx / h * knockbackHorizontal * keep;
        double hz = h == 0 ? 0 : dz / h * knockbackHorizontal * keep;
        return new Vec3(hx, knockbackVertical * keep, hz);
    }
}
