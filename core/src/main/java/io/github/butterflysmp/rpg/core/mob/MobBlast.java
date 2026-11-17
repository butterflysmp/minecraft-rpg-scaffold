package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A staged mob's SELF-DESTRUCT: the Phage's blast (PLAN-dragon-mobs.md step R3). Authored in the mob file's
 * {@code blast:} block. Ben: "for the explosion keep vanilla creeper numbers for everything except the damage", and
 * on its shape, "keep vanillas shap". The damage is 60 at stage 1 and 150 at stage 5 since 2026-11-17 (the seat's
 * reading of Ben's "increase 3x", replacing P-a); the gear-score pricing is still the seat's provisional P-b. Since
 * the same day it PUSHES its victims as vanilla's does (Ben: "knockback sounds like a good addition").
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code reach}: BLOCKS, how far the blast hurts: the straight-line distance between the Phage's FEET and the
 *       victim's FEET, INCLUSIVE, same world. A sphere. The Phage's is 6, vanilla's reach for a creeper: its power 3
 *       doubled ({@code ServerExplosion.hurtEntities}, {@code radius * 2}), measured between the same two points
 *       ({@code Entity.distanceToSqr(center)} is the entity's position, and the creeper explodes at its own).</li>
 *   <li>{@code firstDamage} / {@code lastDamage}: the AUTHORED damage at the first and last stage, linear between
 *       (G3), before gear score, at ZERO distance. The Phage's are 60 and 150.</li>
 *   <li>{@code fuseTicks}: TICKS from the swell's start to the blast, vanilla's {@code maxSwell}. The Phage's is 30
 *       (1.5 seconds). Paper applies it with {@code Creeper#setMaxFuseTicks}.</li>
 *   <li>{@code knockback}: a MULTIPLIER of vanilla's push for the same distance, never an absolute push. 1 is
 *       vanilla's own; 0 turns the push off. The Phage's is 1. See {@link #knockbackStrength}.</li>
 * </ul>
 * A victim at distance d takes the priced damage x {@link #falloff}(d, reach): vanilla's curve, normalised, and is
 * pushed by {@link #knockbackStrength}: vanilla's linear share. No cover check (vanilla's exposure): left out
 * knowingly, and Ben's question (Q34). Exposure is 1 in both.
 */
public record MobBlast(double reach, double firstDamage, double lastDamage, int fuseTicks, double knockback) {

    public MobBlast {
        if (!(reach > 0 && Double.isFinite(reach))) {
            throw new IllegalArgumentException("blast.reach must be a positive number of blocks, was " + reach);
        }
        requireDamage("blast.damage.first", firstDamage);
        requireDamage("blast.damage.last", lastDamage);
        if (fuseTicks < 1) throw new IllegalArgumentException("blast.fuse_ticks must be at least 1, was " + fuseTicks);
        if (!(knockback >= 0 && Double.isFinite(knockback))) {
            throw new IllegalArgumentException("blast.knockback must be 0 or more (a multiplier of vanilla's push), was "
                    + knockback);
        }
    }

    private static void requireDamage(String name, double value) {
        if (!(value >= 0 && Double.isFinite(value))) {
            throw new IllegalArgumentException(name + " must be 0 or more, was " + value);
        }
    }

    /** The AUTHORED damage at {@code stage}, before gear score: linear from the first stage's to the last's. */
    public double damageAt(int stage, MobStages stages) {
        if (!stages.isValid(stage)) {
            throw new IllegalArgumentException("stage " + stage + " is outside 1.." + stages.count());
        }
        double t = (double) (stage - 1) / (stages.count() - 1);
        return firstDamage + (lastDamage - firstDamage) * t;
    }

    /**
     * What the blast deals each victim BEFORE the victim's defence: the authored damage, priced as every custom mob's
     * hit on a player is ({@link MobScaling#attackDamage}: x1 for a custom mob, x GS/100 when hostile). P-b.
     */
    public double price(int stage, MobStages stages, boolean hostile, int gearScore) {
        return MobScaling.attackDamage(damageAt(stage, stages), true, hostile, gearScore);
    }

    /**
     * VANILLA'S DISTANCE CURVE, NORMALISED: the share of the zero-distance damage a victim takes at {@code distance}.
     * 1 at zero, 0 at the reach and beyond, never negative.
     *
     * <p>Read from the server jar (build 74), {@code ExplosionDamageCalculator#getEntityDamageAmount}: vanilla deals
     * {@code (x*x + x) / 2 * 7 * f + 1} with {@code x = (1 - d / f) * exposure} and {@code f} the reach. Only the SHAPE
     * is kept (seat's ruling 28): exposure is 1 (no cover), and the {@code * 7 * f} and the {@code + 1} are vanilla's
     * damage constants, which our authored damage replaces. What is left, {@code (x*x + x) / 2}, is already 1 at
     * {@code x = 1} and 0 at {@code x = 0}. Half the reach is {@code x = 0.5}: three eighths.
     */
    public static double falloff(double distance, double reach) {
        // BEYOND the reach x is negative and (x*x + x) / 2 would be too: this guard is what keeps it 0. Written ">", as
        // vanilla's skip is; ">=" is an EQUIVALENT mutant (at exactly the reach x = 0, so the share is 0 either way),
        // measured 2026-11-16: it survived the suite. Its real job is pinned by deleting the line, which reddens.
        if (distance > reach) return 0.0;
        double x = 1.0 - distance / reach;
        return (x * x + x) / 2.0;
    }

    /**
     * VANILLA'S PUSH FACTOR: {@code 1 - d / reach} inside the reach, 0 beyond. LINEAR, unlike the damage's
     * {@link #falloff}: 1 at the blast, a half at half the reach, 0 at the reach. Read from the server jar,
     * {@code ServerExplosion#hurtEntities}: {@code (1 - r)} with {@code r = d / f}, the damage's own {@code r}.
     */
    public static double knockbackFactor(double distance, double reach) {
        if (distance > reach) return 0.0;
        return 1.0 - distance / reach;
    }

    /**
     * THE PUSH STRENGTH, vanilla's expression with exposure fixed at 1 (no cover, Q34):
     * {@code (1 - d / reach) * multiplier * (1 - resistance)}. In the jar it is
     * {@code (1 - r) * exposure * knockbackMultiplier * (1 - resistance)}, where {@code knockbackMultiplier} is the
     * default calculator's 1 and {@code resistance} the victim's {@code EXPLOSION_KNOCKBACK_RESISTANCE}
     * (PLAN-dragon-mobs.md section 7 F-B, READ). Our {@code multiplier} is content's {@link #knockback}. No clamp on
     * the resistance, as vanilla has none. 0 beyond the reach, never negative.
     */
    public static double knockbackStrength(double distance, double reach, double multiplier, double resistance) {
        return knockbackFactor(distance, reach) * multiplier * (1.0 - resistance);
    }

    /** This blast's push at {@code distance} on a victim with {@code resistance}: its own reach and multiplier. */
    public double knockbackAt(double distance, double resistance) {
        return knockbackStrength(distance, reach, knockback, resistance);
    }

    /**
     * THE PUSH VECTOR, vanilla's: from the blast's {@code center} to the victim's {@code eye}, normalised, times
     * {@code strength}. ZERO when the two are closer than {@code 1.0E-5F}, as {@code net.minecraft.world.phys.Vec3
     * #normalize} returns (it compares against that float), never a NaN.
     */
    public static Vec3 pushVector(Vec3 center, Vec3 eye, double strength) {
        Vec3 d = eye.subtract(center);
        double len = d.length();
        if (len < 1.0E-5F) return Vec3.ZERO;
        return d.scale(strength / len);
    }

    /** Inside the reach, feet to feet, INCLUSIVE, as vanilla's skip is ({@code d / f > 1} skips). */
    public boolean inReach(Vec3 at, Vec3 other) {
        return at.distanceSquared(other) <= reach * reach;
    }

    /** A living creature near the blast: its id, its side, whether it is an exempt player, its world and feet. */
    public record Bystander(UUID id, MobHostility.Side side, boolean exemptMode, String world, Vec3 at) {}

    /** One creature inside the reach: who, how far (feet to feet), and the share of the damage {@link #falloff} gives. */
    public record Hit(Bystander who, double distance, double factor) {}

    /**
     * Who the blast reaches. {@code victims} are hurt, each by the priced damage x its factor; {@code spared} were
     * inside the reach and are NOT hurt (a mob, a creative player), named so a gate can see the blast knew they were
     * there. A victim exactly at the reach is listed with factor 0. Both keep the order given.
     */
    public record Reach(List<Hit> victims, List<Hit> spared) {}

    /**
     * THE RULE for one blast at {@code at} in {@code world}. The exploding mob ({@code self}) is never in either
     * list; another world is in neither. The caller's search may be any shape that contains the sphere.
     */
    public Reach reach(UUID self, String world, Vec3 at, List<Bystander> bystanders) {
        List<Hit> victims = new ArrayList<>();
        List<Hit> spared = new ArrayList<>();
        for (Bystander b : bystanders) {
            if (b.id().equals(self) || !b.world().equals(world) || !inReach(at, b.at())) continue;
            double d = Math.sqrt(at.distanceSquared(b.at()));
            Hit hit = new Hit(b, d, falloff(d, reach));
            if (MobHostility.hostileTo(b.side(), b.exemptMode())) victims.add(hit);
            else spared.add(hit);
        }
        return new Reach(victims, spared);
    }
}
