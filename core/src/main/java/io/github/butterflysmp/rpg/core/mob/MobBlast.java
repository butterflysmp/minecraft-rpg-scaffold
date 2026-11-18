package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.shape.Implosion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A staged mob's SELF-DESTRUCT: the Phage's blast (PLAN-dragon-mobs.md step R3). Authored in the mob file's
 * {@code blast:} block. Ben: "for the explosion keep vanilla creeper numbers for everything except the damage", and
 * on its shape, "keep vanillas shap". The damage is 60 at stage 1 and 150 at stage 5 since 2026-11-17 (the seat's
 * reading of Ben's "increase 3x", CONFIRMED BY PLAY); the gear-score pricing is still the seat's provisional P-b.
 *
 * <p><b>SINCE 2026-11-17 (feat/void-implosion) IT PULLS, NEVER PUSHES.</b> Ben: "A void explosion that pulls things
 * toward the origin with black and dark purple swirling particles ... when the phage explodes", and "swap the push for
 * the pull". The blast is ONE USE of the reusable shape {@link Implosion} ({@link #implosion}); the push vector,
 * {@code knockbackAt} and {@code pushVector} it replaced are gone.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code reach}: BLOCKS, how far the blast hurts AND pulls: the straight-line distance between the Phage's FEET
 *       and the victim's FEET, INCLUSIVE, same world. A sphere. The Phage's is 6, vanilla's reach for a creeper: its
 *       power 3 doubled ({@code ServerExplosion.hurtEntities}, {@code radius * 2}).</li>
 *   <li>{@code firstDamage} / {@code lastDamage}: the AUTHORED damage at the first and last stage, linear between
 *       (G3), before gear score, at ZERO distance. The Phage's are 60 and 150.</li>
 *   <li>{@code fuseTicks}: TICKS from the swell's start to the blast, vanilla's {@code maxSwell}. The Phage's is 30
 *       (1.5 seconds). Paper applies it with {@code Creeper#setMaxFuseTicks}.</li>
 *   <li>{@code pull}: a MULTIPLIER of vanilla's explosion push for the same distance, directed TOWARD the Phage, never
 *       an absolute speed (see {@link Implosion}). The Phage's is 1.25: the seat's number for Ben's "A little bit
 *       stronger than the push today". 0 turns the pull off.</li>
 *   <li>{@code visual}: the content visual id played at the detonation in place of vanilla's explosion emitter and
 *       sound, drawn at the blast's reach ({@code void_implosion}).</li>
 * </ul>
 * A victim at distance d takes the priced damage x {@link #falloff}(d, reach): vanilla's curve, normalised. No cover
 * check (vanilla's exposure): left out knowingly, and Ben's question (Q34).
 */
public record MobBlast(double reach, double firstDamage, double lastDamage, int fuseTicks, double pull,
                       String visual) {

    public MobBlast {
        if (!(reach > 0 && Double.isFinite(reach))) {
            throw new IllegalArgumentException("blast.reach must be a positive number of blocks, was " + reach);
        }
        requireDamage("blast.damage.first", firstDamage);
        requireDamage("blast.damage.last", lastDamage);
        if (fuseTicks < 1) throw new IllegalArgumentException("blast.fuse_ticks must be at least 1, was " + fuseTicks);
        if (!(pull >= 0 && Double.isFinite(pull))) {
            throw new IllegalArgumentException("blast.pull must be 0 or more (a multiplier of vanilla's push), was "
                    + pull);
        }
        if (visual == null || visual.isBlank()) throw new IllegalArgumentException("blast.visual is required");
    }

    /**
     * THE BLAST'S IMPLOSION: this blast's reach, vanilla's explosion falloff, its pull, and the explosion knockback
     * resistance (the brief: "the Phage keeps the explosion one"). The victims are the blast's ({@link #reach(UUID,
     * String, Vec3, List)}); the shape says how hard each is pulled and how much damage share it takes.
     */
    public Implosion implosion() {
        return new Implosion(reach, Implosion.Falloff.VANILLA_EXPLOSION, pull, Implosion.Resistance.EXPLOSION_KNOCKBACK);
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
     * 1 at zero, 0 at the reach and beyond, never negative. Read from the server jar (build 74),
     * {@code ExplosionDamageCalculator#getEntityDamageAmount}; only the SHAPE is kept (seat's ruling 28). Its home is
     * the shape since 2026-11-17, {@link Implosion#vanillaShare}; this name stays for the BLAST line's readers.
     */
    public static double falloff(double distance, double reach) {
        return Implosion.vanillaShare(distance, reach);
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
