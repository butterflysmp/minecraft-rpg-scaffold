package io.github.butterflysmp.rpg.core.shape;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.Optional;

/**
 * THE VOID IMPLOSION: A REUSABLE SHAPE THAT PULLS (PLAN-dragon-mobs.md, Ben 2026-11-17: "A void explosion that pulls
 * things toward the origin ... I also want to be able to use this shape elsewhere (potential future abilities and
 * weapons)"). Pure: given an ORIGIN, a REACH, a FALLOFF for its damage, a PULL multiplier and the RESISTANCE attribute
 * the use names, it answers per victim the share of the damage and an impulse TOWARD the origin.
 *
 * <p><b>WHO it affects is never this class's:</b> the predicate is part of each USE. The Phage's blast
 * ({@code MobBlast#implosion}) pulls the combatants it is hostile to ({@code MobHostility}); an ability names the shape
 * from content as {@code type: implosion} ({@code EffectSpec.Implosion}) with an {@code affects:} of its own.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code reach}: BLOCKS, the straight-line distance between the ORIGIN and the victim's FEET, INCLUSIVE, a
 *       sphere. The Phage's origin is its own feet, as vanilla's explosion centre is.</li>
 *   <li>{@code falloff}: the share of the use's zero-distance damage a victim at distance d takes.
 *       {@link Falloff#VANILLA_EXPLOSION} is vanilla's curve normalised, {@code (x*x + x) / 2} with {@code x = 1 - d /
 *       reach} (PLAN section 7 F-B, READ); {@link Falloff#FLAT} is all of it anywhere inside.</li>
 *   <li>{@code pull}: a MULTIPLIER of vanilla's explosion PUSH for the same distance, never an absolute speed: the
 *       strength is {@code (1 - d / reach) x pull x (1 - resistance)} BLOCKS PER TICK (F-B's
 *       {@code (1 - r) x exposure x knockbackMultiplier x (1 - resistance)}, exposure 1), pointed the other way. The
 *       Phage's is 1.25 (the seat's number for Ben's "A little bit stronger than the push today").</li>
 *   <li>{@code resistance}: WHICH attribute cuts it, content on the use: {@link Resistance#EXPLOSION_KNOCKBACK}
 *       (vanilla explosion's own, the Phage's), {@link Resistance#KNOCKBACK} (a melee hit's), or
 *       {@link Resistance#NONE}. The VALUE is read off the victim by paper, on the victim's thread.</li>
 * </ul>
 *
 * <p><b>IT NEVER CARRIES A VICTIM PAST THE ORIGIN (the seat's rule).</b> The pull is HORIZONTAL (no vertical part: a
 * grounded victim stays grounded, so the model below holds, and nothing lifted by us can take fall damage). Its speed
 * is capped so that the travel a velocity gives on its own, {@link #travel}, is at most the HORIZONTAL distance to the
 * origin less {@link #MARGIN}. The model is READ in the pinned server jar, {@code LivingEntity#travelInAir}
 * ({@code javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar net.minecraft.world.entity.LivingEntity}): each tick
 * the body MOVES by its delta ({@code handleRelativeFrictionAndCalculateMovement} calls {@code move}) and THEN its
 * horizontal delta is multiplied by {@code f x 0.91f}, where {@code f} is the friction of the block that affects its
 * movement ({@code getBlockPosBelowThatAffectsMyMovement}, 0.500001 below the feet, READ in {@code Entity}) when it is
 * {@code onGround}, else {@code 1.0f}; a block's friction defaults to {@code 0.6f} ({@code BlockBehaviour$Properties},
 * READ). So with no input of its own a body given speed v travels {@code v / (1 - m)}. <b>What the cap does NOT
 * bound:</b> the victim's own walking or a player's keys, and momentum it already had. It bounds what OUR pull adds.
 * A victim in the air is modelled with the air's {@code 0.91} throughout, which over-states its travel (it lands into
 * ground friction), so its cap is the tighter one: safe, never past.
 */
public record Implosion(double reach, Falloff falloff, double pull, Resistance resistance) {

    /** How the use's damage falls off with distance. */
    public enum Falloff { VANILLA_EXPLOSION, FLAT }

    /** Which attribute of the victim cuts the pull. */
    public enum Resistance { EXPLOSION_KNOCKBACK, KNOCKBACK, NONE }

    /**
     * BLOCKS: how far short of the origin the modelled travel stops. CHOSEN (2026-11-17), not read: half a block, so a
     * victim stops beside the origin rather than in it, and one within half a block is not moved at all.
     */
    public static final double MARGIN = 0.5;

    /** READ, {@code LivingEntity#travelInAir}: {@code ldc_w 0.91f}, the scale on the friction. */
    public static final float FRICTION_SCALE = 0.91f;

    /** READ, {@code LivingEntity#travelInAir}: {@code fconst_1}, the "friction" used when not on the ground. */
    public static final float AIRBORNE_FRICTION = 1.0f;

    /** READ, {@code BlockBehaviour$Properties.<init>}: {@code ldc 0.6f}, every block's friction unless it says. */
    public static final float DEFAULT_BLOCK_FRICTION = 0.6f;

    public Implosion {
        if (!(reach > 0 && Double.isFinite(reach))) {
            throw new IllegalArgumentException("implosion reach must be a positive number of blocks, was " + reach);
        }
        if (!(pull >= 0 && Double.isFinite(pull))) {
            throw new IllegalArgumentException("implosion pull must be 0 or more (a multiplier of vanilla's push), was "
                    + pull);
        }
        if (falloff == null) throw new IllegalArgumentException("implosion falloff is required");
        if (resistance == null) throw new IllegalArgumentException("implosion resistance is required");
    }

    /**
     * What the world says about one victim, read on the victim's own thread: its FEET, whether it is ON THE GROUND,
     * the FRICTION of the block that affects its movement, and the VALUE of the use's resistance attribute (0 for
     * {@link Resistance#NONE}).
     */
    public record Footing(Vec3 feet, boolean onGround, float blockFriction, double resistance) {}

    /**
     * One victim's pull: {@code distance} feet to feet (the strength's), {@code horizontal} (the cap's), the damage
     * {@code share}, the uncapped {@code strength}, the {@code resistance} value it used, whether it stood
     * {@code onGround}, the per-tick multiplier the model used, the {@code capSpeed}, the
     * speed {@code applied} ({@code min(strength, capSpeed)}), whether the cap {@code capped} it, and the
     * {@code impulse}: {@code applied} blocks per tick, horizontal, toward the origin, to ADD to the victim's velocity.
     */
    public record Pull(double distance, double horizontal, double share, double strength, double resistance,
                       boolean onGround, double tickMultiplier, double capSpeed, double applied, boolean capped,
                       Vec3 impulse) {}

    /**
     * VANILLA'S DAMAGE CURVE, NORMALISED: {@code (x*x + x) / 2}, {@code x = 1 - d / reach}; 1 at zero, 0 at the reach
     * and beyond, never negative. The home of {@code MobBlast#falloff} since 2026-11-17.
     */
    public static double vanillaShare(double distance, double reach) {
        // BEYOND the reach x is negative and (x*x + x) / 2 would be too: this guard is what keeps it 0.
        if (distance > reach) return 0.0;
        double x = 1.0 - distance / reach;
        return (x * x + x) / 2.0;
    }

    /** VANILLA'S PUSH FACTOR: {@code 1 - d / reach} inside the reach, 0 beyond. LINEAR. */
    public static double factor(double distance, double reach) {
        if (distance > reach) return 0.0;
        return 1.0 - distance / reach;
    }

    /** This shape's damage share at {@code distance}: its falloff's. */
    public double share(double distance) {
        if (distance > reach) return 0.0;
        return falloff == Falloff.FLAT ? 1.0 : vanillaShare(distance, reach);
    }

    /** The UNCAPPED strength, blocks per tick: {@code (1 - d / reach) x pull x (1 - resistance)}. */
    public double strength(double distance, double resistance) {
        return factor(distance, reach) * pull * (1.0 - resistance);
    }

    /** The per-tick horizontal multiplier: {@code (onGround ? blockFriction : 1.0f) x 0.91f}, in FLOAT, as vanilla's. */
    public static double tickMultiplier(boolean onGround, float blockFriction) {
        return (onGround ? blockFriction : AIRBORNE_FRICTION) * FRICTION_SCALE;
    }

    /** BLOCKS a body travels with no input from speed {@code speed}: {@code speed / (1 - m)}. */
    public static double travel(double speed, double tickMultiplier) {
        return speed / (1.0 - tickMultiplier);
    }

    /** The largest speed whose {@link #travel} is {@code horizontal - MARGIN}; 0 within the margin, never negative. */
    public static double capSpeed(double horizontal, double tickMultiplier) {
        return Math.max(0.0, horizontal - MARGIN) * (1.0 - tickMultiplier);
    }

    /** Inside the reach, origin to feet, INCLUSIVE. */
    public boolean inReach(Vec3 origin, Vec3 feet) {
        return origin.distanceSquared(feet) <= reach * reach;
    }

    /**
     * THE RULE for one victim: empty beyond the reach; otherwise its share, its strength, the cap, and the impulse.
     * The direction is the horizontal from the feet to the origin; within {@code 1.0E-5F} of it (vanilla's
     * {@code Vec3#normalize} cut-off) there is none, and the impulse is ZERO, never a NaN.
     */
    public Optional<Pull> pull(Vec3 origin, Footing footing) {
        Vec3 feet = footing.feet();
        if (!inReach(origin, feet)) return Optional.empty();
        double distance = Math.sqrt(origin.distanceSquared(feet));
        double dx = origin.x() - feet.x();
        double dz = origin.z() - feet.z();
        double horizontal = Math.hypot(dx, dz);
        double strength = strength(distance, footing.resistance());
        double m = tickMultiplier(footing.onGround(), footing.blockFriction());
        double cap = capSpeed(horizontal, m);
        boolean capped = strength > cap;
        double applied = Math.max(0.0, Math.min(strength, cap));
        Vec3 impulse = horizontal < 1.0E-5F || applied == 0.0
                ? Vec3.ZERO
                : new Vec3(dx / horizontal * applied, 0.0, dz / horizontal * applied);
        return Optional.of(new Pull(distance, horizontal, share(distance), strength, footing.resistance(),
                footing.onGround(), m, cap, applied, capped, impulse));
    }
}
