package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Optional;

/**
 * THE VOID IMPLOSION: A REUSABLE SHAPE THAT PULLS (PLAN-dragon-mobs.md, Ben 2026-11-17: "A void explosion that pulls
 * things toward the origin ... I also want to be able to use this shape elsewhere"). Pure: given an ORIGIN, a REACH, a
 * FALLOFF for its damage, a PULL and a LIFT, and the RESISTANCE attribute the use names, it answers per victim the
 * share of the damage and an impulse: horizontal TOWARD the origin, vertical upward.
 *
 * <p><b>WHO it affects is never this class's:</b> the predicate is part of each USE. The Phage's blast
 * ({@code MobBlast#implosion}) pulls the combatants it is hostile to; an ability names the shape from content as
 * {@code type: implosion} ({@code EffectSpec.Implosion}) with an {@code affects:} of its own.
 *
 * <p><b>NAME THE QUANTITY (the seat's ruling 2 on #204: "the pull gets the push's lift").</b> The shape starts from the
 * VANILLA EXPLOSION PUSH the blast used to give (READ, PLAN section 7 F-B; {@code MobBlast.pushVector} on master
 * {@code f6a596ea}): strength {@code k = (1 - d / reach) x (1 - resistance)}, along the victim's EYE minus the origin,
 * normalised. Split into its HORIZONTAL part {@code k x h / len} and its VERTICAL part {@code k x (eyeY - originY) /
 * len}, {@code h} the horizontal distance and {@code len} the eye's distance from the origin:
 * <ul>
 *   <li>{@code reach}: BLOCKS, origin to the victim's FEET, inclusive, a sphere.</li>
 *   <li>{@code falloff}: the share of the use's zero-distance damage at distance d: vanilla's curve normalised
 *       ({@link Falloff#VANILLA_EXPLOSION}) or all of it ({@link Falloff#FLAT}).</li>
 *   <li>{@code pull}: a MULTIPLIER of the push's HORIZONTAL part, pointed AT the origin. The Phage's 1.25 (the seat's
 *       number for Ben's "A little bit stronger than the push today").</li>
 *   <li>{@code lift}: a MULTIPLIER of the push's VERTICAL part, its own named value: 1 is the same lift the push gave
 *       at that distance (the Phage's), 0 none (the test-only fixture's, which stays on the ground).</li>
 *   <li>{@code resistance}: WHICH attribute cuts both parts, content on the use.</li>
 * </ul>
 *
 * <p><b>THE CAP IS ON WHERE THE VICTIM COMES TO REST</b> (the seat's rule: never past the origin). The whole motion is
 * modelled from the jar, READ ({@code javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar <class>}):
 * {@code LivingEntity#travelInAir}: each tick the body MOVES by its velocity, then its horizontal velocity is
 * multiplied by {@code (onGround ? friction : 1.0f) x 0.91f}, the on-ground state being the one at the tick's START,
 * and its vertical becomes {@code (vy - gravity) x 0.98f}; {@code Attributes.GRAVITY} defaults to {@code 0.08};
 * {@code Block#updateEntityMovementAfterFallOn} multiplies the velocity by {@code (1, 0, 1)} on landing; a block's
 * friction defaults to {@code 0.6f} ({@code BlockBehaviour$Properties}), read 0.500001 below the feet
 * ({@code Entity#getBlockPosBelowThatAffectsMyMovement}). So a body given horizontal speed v and vertical speed w
 * comes to rest {@code v x travelFactor(w)} away: LINEAR in v. The HORIZONTAL part is scaled DOWN, never up, so that
 * rest point is at most the horizontal distance less {@link #MARGIN}; <b>the lift is never scaled.</b>
 *
 * <p><b>WHAT THE MODEL ASSUMES, STATED:</b> a level floor (the victim lands at the height it left); no input of the
 * victim's own (a player's keys, a mob's AI); and for a victim ALREADY IN THE AIR, whose height above any floor is
 * unknown, the slowest decay any flight can have, air's {@code 0.91} forever, so its cap is the tightest. <b>That a
 * player's CLIENT runs the same travel is INFERRED</b> (the client's {@code LivingEntity} is the same class).
 */
public record Implosion(double reach, Falloff falloff, double pull, double lift, Resistance resistance) {

    public enum Falloff { VANILLA_EXPLOSION, FLAT }

    public enum Resistance { EXPLOSION_KNOCKBACK, KNOCKBACK, NONE }

    /** BLOCKS short of the origin the modelled rest point stops. CHOSEN (2026-11-17). */
    public static final double MARGIN = 0.5;

    /** READ, {@code LivingEntity#travelInAir}: {@code ldc_w 0.91f}. */
    public static final float FRICTION_SCALE = 0.91f;

    /** READ, {@code LivingEntity#travelInAir}: {@code fconst_1}, the "friction" off the ground. */
    public static final float AIRBORNE_FRICTION = 1.0f;

    /** READ, {@code BlockBehaviour$Properties.<init>}: {@code ldc 0.6f}. */
    public static final float DEFAULT_BLOCK_FRICTION = 0.6f;

    /** READ, {@code Attributes.<clinit>}: the {@code gravity} attribute's default, {@code ldc2_w 0.08d}. */
    public static final double GRAVITY = 0.08;

    /** READ, {@code LivingEntity#travelInAir}: {@code ldc_w 0.98f}, the vertical drag, a float widened. */
    public static final double VERTICAL_DRAG = (double) 0.98f;

    /** READ, {@code Attributes.<clinit>}: the {@code safe_fall_distance} attribute's default, {@code ldc2_w 3.0d}. */
    public static final double SAFE_FALL = 3.0;

    /**
     * BLOCKS under {@link #SAFE_FALL} at which a lifted victim is given the safe-landing mark (the dash's, ruling 31).
     * CHOSEN (2026-11-17): the lift is never scaled (the seat's ruling 2), and near the origin it carries a player
     * above 3 blocks (d = 1: 3.20), so the fall is covered by the mark instead.
     */
    public static final double SAFE_FALL_MARGIN = 0.5;

    public Implosion {
        if (!(reach > 0 && Double.isFinite(reach))) {
            throw new IllegalArgumentException("implosion reach must be a positive number of blocks, was " + reach);
        }
        if (!(pull >= 0 && Double.isFinite(pull))) {
            throw new IllegalArgumentException("implosion pull must be 0 or more (a multiplier of vanilla's push), was "
                    + pull);
        }
        if (!(lift >= 0 && Double.isFinite(lift))) {
            throw new IllegalArgumentException("implosion lift must be 0 or more (a multiplier of the push's lift), was "
                    + lift);
        }
        if (falloff == null) throw new IllegalArgumentException("implosion falloff is required");
        if (resistance == null) throw new IllegalArgumentException("implosion resistance is required");
    }

    /**
     * What the world says about one victim, read on its own thread: its FEET, its EYE HEIGHT above them, whether it is
     * ON THE GROUND, the FRICTION of the block that moves it, and the VALUE of the use's resistance attribute.
     */
    public record Footing(Vec3 feet, double eyeHeight, boolean onGround, float blockFriction, double resistance) {}

    /**
     * One victim's pull. {@code distance} feet to feet, {@code horizontal} the cap's; the damage {@code share}; the old
     * push's strength {@code push} and its two parts; the pull's uncapped HORIZONTAL {@code strength} and its
     * {@code vertical}; the {@code resistance} used; {@code onGround}; the model's {@code travelFactor} (blocks of rest
     * per unit of horizontal speed); the {@code capSpeed}; the horizontal speed {@code applied}; whether the cap
     * {@code capped} it; the {@code apex} the vertical reaches; and the {@code impulse} to ADD to the velocity.
     */
    public record Pull(double distance, double horizontal, double share, double push, double pushHorizontal,
                       double pushVertical, double strength, double vertical, double resistance, boolean onGround,
                       double travelFactor, double capSpeed, double applied, boolean capped, double apex,
                       Vec3 impulse) {

        /** BLOCKS from where the victim stood to where the model brings it to rest, horizontally. */
        public double travel() {
            return applied * travelFactor;
        }

        /** The lift carries the feet within {@link #SAFE_FALL_MARGIN} of the safe fall, or above it. */
        public boolean safeLanding() {
            return apex >= SAFE_FALL - SAFE_FALL_MARGIN;
        }
    }

    /** VANILLA'S DAMAGE CURVE, NORMALISED: {@code (x*x + x) / 2}, {@code x = 1 - d / reach}; 0 beyond. */
    public static double vanillaShare(double distance, double reach) {
        if (distance > reach) return 0.0;
        double x = 1.0 - distance / reach;
        return (x * x + x) / 2.0;
    }

    /** VANILLA'S PUSH FACTOR: {@code 1 - d / reach} inside the reach, 0 beyond. LINEAR. */
    public static double factor(double distance, double reach) {
        if (distance > reach) return 0.0;
        return 1.0 - distance / reach;
    }

    public double share(double distance) {
        if (distance > reach) return 0.0;
        return falloff == Falloff.FLAT ? 1.0 : vanillaShare(distance, reach);
    }

    /** The per-tick horizontal multiplier: {@code (onGround ? blockFriction : 1.0f) x 0.91f}, in FLOAT, as vanilla's. */
    public static double tickMultiplier(boolean onGround, float blockFriction) {
        return (onGround ? blockFriction : AIRBORNE_FRICTION) * FRICTION_SCALE;
    }

    /** Ticks the flight is followed before the model gives up (none of ours lasts 60; a NaN past it is loud). */
    private static final int FLIGHT_TICKS = 1000;

    /**
     * BLOCKS OF REST PER UNIT OF HORIZONTAL SPEED: how far a body given horizontal speed 1 (and vertical
     * {@code verticalSpeed}) travels before it stops, in vanilla's order (class javadoc). For a body that starts ON THE
     * GROUND: each tick it moves, its horizontal is multiplied by the friction of the state it STARTED the tick in, and
     * its vertical becomes {@code (vy - 0.08) x 0.98f}; it lands when it comes back to its own height falling (a level
     * floor), its vertical zeroed; from then on it slides, {@code 1 / (1 - m)} more. For a body ALREADY IN THE AIR,
     * whose height above any floor is unknown: {@code 1 / (1 - 0.91)}, the slowest decay any flight can have, so the
     * cap built on it is never too loose.
     */
    public static double travelFactor(double verticalSpeed, boolean onGround, float blockFriction) {
        double air = tickMultiplier(false, blockFriction);
        if (!onGround) return 1.0 / (1.0 - air);
        double ground = tickMultiplier(true, blockFriction);
        double y = 0.0;
        double vy = verticalSpeed;
        double speed = 1.0;   // the horizontal speed, as a share of the first tick's
        double sum = 0.0;
        boolean onFloor = true;
        for (int t = 0; t < FLIGHT_TICKS; t++) {
            if (onFloor && vy <= 0) return sum + speed / (1.0 - ground);   // at rest vertically: the slide
            double m = onFloor ? ground : air;                              // the state at the tick's START
            sum += speed;
            y += vy;
            boolean landed = y <= 0 && vy <= 0;
            if (y < 0) y = 0;
            if (landed) vy = 0;                                             // updateEntityMovementAfterFallOn
            speed *= m;
            vy = (vy - GRAVITY) * VERTICAL_DRAG;
            onFloor = landed;
        }
        return Double.NaN;
    }

    /** BLOCKS: the highest the feet rise above where they started, under vertical speed {@code verticalSpeed}. */
    public static double apex(double verticalSpeed) {
        double y = 0.0;
        double vy = verticalSpeed;
        double top = 0.0;
        for (int t = 0; t < FLIGHT_TICKS && vy > 0; t++) {
            y += vy;
            top = Math.max(top, y);
            vy = (vy - GRAVITY) * VERTICAL_DRAG;
        }
        return top;
    }

    public boolean inReach(Vec3 origin, Vec3 feet) {
        return origin.distanceSquared(feet) <= reach * reach;
    }

    /**
     * THE RULE for one victim: empty beyond the reach; otherwise its two parts and the cap. A direction shorter than
     * {@code 1.0E-5F} (vanilla's {@code Vec3#normalize} cut-off) is none: no NaN.
     */
    public Optional<Pull> pull(Vec3 origin, Footing f) {
        Vec3 feet = f.feet();
        if (!inReach(origin, feet)) return Optional.empty();
        double distance = Math.sqrt(origin.distanceSquared(feet));
        double dx = origin.x() - feet.x();
        double dz = origin.z() - feet.z();
        double horizontal = Math.hypot(dx, dz);
        double up = feet.y() + f.eyeHeight() - origin.y();
        double len = Math.hypot(horizontal, up);
        double k = factor(distance, reach) * (1.0 - f.resistance());
        double pushH = len < 1.0E-5F ? 0.0 : k * horizontal / len;
        double pushV = len < 1.0E-5F ? 0.0 : k * up / len;
        double strength = pull * pushH;
        double vertical = lift * pushV;
        double tf = travelFactor(vertical, f.onGround(), f.blockFriction());
        double cap = Math.max(0.0, horizontal - MARGIN) / tf;
        boolean capped = strength > cap;
        double applied = Math.max(0.0, Math.min(strength, cap));
        double hx = horizontal < 1.0E-5F ? 0.0 : dx / horizontal * applied;
        double hz = horizontal < 1.0E-5F ? 0.0 : dz / horizontal * applied;
        return Optional.of(new Pull(distance, horizontal, share(distance), k, pushH, pushV, strength, vertical,
                f.resistance(), f.onGround(), tf, cap, applied, capped, apex(vertical), new Vec3(hx, vertical, hz)));
    }
}
