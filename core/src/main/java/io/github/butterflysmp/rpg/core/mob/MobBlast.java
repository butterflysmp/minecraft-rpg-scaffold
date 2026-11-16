package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A staged mob's SELF-DESTRUCT: the Phage's blast (PLAN-dragon-mobs.md step R3). Authored in the mob file's
 * {@code blast:} block. Ben: "for the explosion keep vanilla creeper numbers for everything except the damage", and
 * on its shape, "keep vanillas shap". The damage numbers are the seat's provisional P-a and P-b, until Ben rules.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code reach}: BLOCKS, how far the blast hurts: the straight-line distance between the Phage's FEET and the
 *       victim's FEET, INCLUSIVE, same world. A sphere. The Phage's is 6, vanilla's reach for a creeper: its power 3
 *       doubled ({@code ServerExplosion.hurtEntities}, {@code radius * 2}), measured between the same two points
 *       ({@code Entity.distanceToSqr(center)} is the entity's position, and the creeper explodes at its own).</li>
 *   <li>{@code firstDamage} / {@code lastDamage}: the AUTHORED damage at the first and last stage, linear between
 *       (G3), before gear score, at ZERO distance. The Phage's are 20 and 50 (P-a).</li>
 *   <li>{@code fuseTicks}: TICKS from the swell's start to the blast, vanilla's {@code maxSwell}. The Phage's is 30
 *       (1.5 seconds). Paper applies it with {@code Creeper#setMaxFuseTicks}.</li>
 * </ul>
 * A victim at distance d takes the priced damage x {@link #falloff}(d, reach): vanilla's curve, normalised. No cover
 * check (vanilla's exposure) and no knockback: both are left out knowingly, and both are questions for Ben.
 */
public record MobBlast(double reach, double firstDamage, double lastDamage, int fuseTicks) {

    public MobBlast {
        if (!(reach > 0 && Double.isFinite(reach))) {
            throw new IllegalArgumentException("blast.reach must be a positive number of blocks, was " + reach);
        }
        requireDamage("blast.damage.first", firstDamage);
        requireDamage("blast.damage.last", lastDamage);
        if (fuseTicks < 1) throw new IllegalArgumentException("blast.fuse_ticks must be at least 1, was " + fuseTicks);
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
