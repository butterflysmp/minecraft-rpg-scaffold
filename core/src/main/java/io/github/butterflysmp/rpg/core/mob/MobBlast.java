package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A staged mob's SELF-DESTRUCT: the Phage's blast (PLAN-dragon-mobs.md step R3). Authored in the mob file's
 * {@code blast:} block. Ben: "for the explosion keep vanilla creeper numbers for everything except the damage". The
 * damage numbers are the seat's provisional P-a and P-b, until Ben rules.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code radius}: BLOCKS, the straight-line distance between the Phage's FEET and the victim's FEET,
 *       INCLUSIVE, same world. A sphere. The Phage's is 3, vanilla's creeper number (but see PLAN section 8 F2:
 *       vanilla's 3 is an explosion POWER whose damage reaches twice as far, with falloff; Q33).</li>
 *   <li>{@code firstDamage} / {@code lastDamage}: the AUTHORED damage at the first and last stage, linear between
 *       (G3), before gear score. The Phage's are 20 and 50 (P-a).</li>
 *   <li>{@code fuseTicks}: TICKS from the swell's start to the blast, vanilla's {@code maxSwell}. The Phage's is 30
 *       (1.5 seconds). Paper applies it with {@code Creeper#setMaxFuseTicks}.</li>
 * </ul>
 * Every victim takes the FULL priced damage (P-c): no falloff, no cover check, no knockback.
 */
public record MobBlast(double radius, double firstDamage, double lastDamage, int fuseTicks) {

    public MobBlast {
        if (!(radius > 0 && Double.isFinite(radius))) {
            throw new IllegalArgumentException("blast.radius must be a positive number of blocks, was " + radius);
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

    /** Inside the radius, feet to feet, inclusive. */
    public boolean inRadius(Vec3 at, Vec3 other) {
        return at.distanceSquared(other) <= radius * radius;
    }

    /** A living creature near the blast: its id, its side, whether it is an exempt player, its world and feet. */
    public record Bystander(UUID id, MobHostility.Side side, boolean exemptMode, String world, Vec3 at) {}

    /**
     * Who the blast reaches. {@code victims} are hurt; {@code spared} were inside the radius and are NOT hurt (a mob,
     * a creative player), named so a gate can see the blast knew they were there. Both keep the order given.
     */
    public record Reach(List<Bystander> victims, List<Bystander> spared) {}

    /**
     * THE RULE for one blast at {@code at} in {@code world}. The exploding mob ({@code self}) is never in either
     * list; another world is in neither. The caller's search may be any shape that contains the sphere.
     */
    public Reach reach(UUID self, String world, Vec3 at, List<Bystander> bystanders) {
        List<Bystander> victims = new ArrayList<>();
        List<Bystander> spared = new ArrayList<>();
        for (Bystander b : bystanders) {
            if (b.id().equals(self) || !b.world().equals(world) || !inRadius(at, b.at())) continue;
            if (MobHostility.hostileTo(b.side(), b.exemptMode())) victims.add(b);
            else spared.add(b);
        }
        return new Reach(victims, spared);
    }
}
