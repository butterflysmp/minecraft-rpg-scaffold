package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.shape.Implosion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A staged mob's SELF-DESTRUCT: the Phage's blast (PLAN-dragon-mobs.md step R3). Authored in the mob file's
 * {@code blast:} block. Ben: "for the explosion keep vanilla creeper numbers for everything except the damage", and
 * on its shape, "keep vanillas shap". The damage is 60 at stage 1 and 150 at stage 5 (CONFIRMED BY PLAY 2026-11-17);
 * the gear-score pricing is still the seat's provisional P-b.
 *
 * <p><b>SINCE 2026-11-17 IT PULLS</b> (Ben: "swap the push for the pull"): the blast is ONE USE of the reusable shape
 * {@link Implosion} ({@link #implosion}). <b>AND ITS DISTANCES FOLLOW THE PHAGE'S SIZE</b> (Ben: "make the size of the
 * Phage affect the Fuse radius, explosion radius, etc. The Large phage being the current distances"; the seat's
 * ruling 1 on #204): the reach, the fuse's trigger and its cancel are {@link StagedDistance}s, the LAST stage's authored.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code reach}: BLOCKS, how far the blast hurts AND pulls, feet to feet, inclusive, a sphere; and the width the
 *       swirl is drawn at. Stage 5's is 6, vanilla's reach for a creeper (its power 3 doubled); stage 1's, 2.</li>
 *   <li>{@code trigger}: BLOCKS, feet to feet, STRICTLY inside which a target in sight starts the fuse. Stage 5's is 3,
 *       vanilla's {@code SwellGoal} {@code distanceToSqr < 9.0}; stage 1's, 1.</li>
 *   <li>{@code cancel}: BLOCKS, feet to feet, STRICTLY beyond which a fusing Phage resets. Stage 5's is 7, vanilla's
 *       {@code distanceToSqr > 49.0}; stage 1's, 7/3.</li>
 *   <li>{@code fuseTicks}: TICKS from the fuse's start to the blast, vanilla's {@code maxSwell}, 30, at every stage
 *       (the seat's reading: "etc." is distances only).</li>
 *   <li>{@code firstDamage} / {@code lastDamage}: the AUTHORED damage at the first and last stage, at ZERO distance,
 *       linear between, before gear score. 60 and 150.</li>
 *   <li>{@code pull}: a MULTIPLIER of the old push's HORIZONTAL part, toward the Phage: 1.25.</li>
 *   <li>{@code lift}: a MULTIPLIER of the old push's VERTICAL part: 1, the push's own lift (the seat's ruling 2).</li>
 *   <li>{@code visual}: the content visual drawn at the stage's reach in place of vanilla's puff ({@code void_implosion}).</li>
 * </ul>
 */
public record MobBlast(StagedDistance reach, StagedDistance trigger, StagedDistance cancel, int fuseTicks,
                       double firstDamage, double lastDamage, double pull, double lift, String visual) {

    public MobBlast {
        if (reach == null) throw new IllegalArgumentException("blast.reach is required");
        if (trigger == null) throw new IllegalArgumentException("blast.trigger is required");
        if (cancel == null) throw new IllegalArgumentException("blast.cancel is required");
        if (fuseTicks < 1) throw new IllegalArgumentException("blast.fuse_ticks must be at least 1, was " + fuseTicks);
        requireDamage("blast.damage.first", firstDamage);
        requireDamage("blast.damage.last", lastDamage);
        if (!(pull >= 0 && Double.isFinite(pull))) {
            throw new IllegalArgumentException("blast.pull must be 0 or more (a multiplier of vanilla's push), was "
                    + pull);
        }
        if (!(lift >= 0 && Double.isFinite(lift))) {
            throw new IllegalArgumentException("blast.lift must be 0 or more (a multiplier of the push's lift), was "
                    + lift);
        }
        if (visual == null || visual.isBlank()) throw new IllegalArgumentException("blast.visual is required");
    }

    private static void requireDamage(String name, double value) {
        if (!(value >= 0 && Double.isFinite(value))) {
            throw new IllegalArgumentException(name + " must be 0 or more, was " + value);
        }
    }

    /**
     * Refused at load when any stage's cancel is not beyond its trigger: such a fuse would reset the tick it starts.
     * Called by {@link MobDefinition}, which holds the stages.
     */
    public void checkStages(MobStages stages) {
        for (int s = 1; s <= stages.count(); s++) {
            if (!(cancelAt(s, stages) > triggerAt(s, stages))) {
                throw new IllegalArgumentException("blast.cancel must be beyond blast.trigger at every stage; at stage "
                        + s + " it is " + cancelAt(s, stages) + " against " + triggerAt(s, stages));
            }
        }
    }

    public double reachAt(int stage, MobStages stages) {
        return reach.at(stage, stages);
    }

    public double triggerAt(int stage, MobStages stages) {
        return trigger.at(stage, stages);
    }

    public double cancelAt(int stage, MobStages stages) {
        return cancel.at(stage, stages);
    }

    /**
     * THE BLAST'S IMPLOSION AT {@code stage}: the stage's reach, vanilla's explosion falloff, the pull and the lift,
     * and the explosion knockback resistance (the brief: "the Phage keeps the explosion one").
     */
    public Implosion implosion(int stage, MobStages stages) {
        return new Implosion(reachAt(stage, stages), Implosion.Falloff.VANILLA_EXPLOSION, pull, lift,
                Implosion.Resistance.EXPLOSION_KNOCKBACK);
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

    /** VANILLA'S DISTANCE CURVE, NORMALISED ({@link Implosion#vanillaShare}); this name stays for the line's readers. */
    public static double falloff(double distance, double reach) {
        return Implosion.vanillaShare(distance, reach);
    }

    /** Inside {@code reach}, feet to feet, INCLUSIVE, as vanilla's skip is ({@code d / f > 1} skips). */
    public static boolean inReach(Vec3 at, Vec3 other, double reach) {
        return at.distanceSquared(other) <= reach * reach;
    }

    /** A living creature near the blast: its id, its side, whether it is an exempt player, its world and feet. */
    public record Bystander(UUID id, MobHostility.Side side, boolean exemptMode, String world, Vec3 at) {}

    /** One creature inside the reach: who, how far (feet to feet), and the share of the damage {@link #falloff} gives. */
    public record Hit(Bystander who, double distance, double factor) {}

    /**
     * Who the blast reaches. {@code victims} are hurt, each by the priced damage x its factor; {@code spared} were
     * inside the reach and are NOT hurt (a mob, a creative player). A victim exactly at the reach is listed with factor
     * 0. Both keep the order given.
     */
    public record Reach(List<Hit> victims, List<Hit> spared) {}

    /**
     * THE RULE for one blast at {@code at} in {@code world}, with the stage's {@code reach}. The exploding mob
     * ({@code self}) is never in either list; another world is in neither.
     */
    public static Reach reach(UUID self, String world, Vec3 at, double reach, List<Bystander> bystanders) {
        List<Hit> victims = new ArrayList<>();
        List<Hit> spared = new ArrayList<>();
        for (Bystander b : bystanders) {
            if (b.id().equals(self) || !b.world().equals(world) || !inReach(at, b.at(), reach)) continue;
            double d = Math.sqrt(at.distanceSquared(b.at()));
            Hit hit = new Hit(b, d, falloff(d, reach));
            if (MobHostility.hostileTo(b.side(), b.exemptMode())) victims.add(hit);
            else spared.add(hit);
        }
        return new Reach(victims, spared);
    }
}
