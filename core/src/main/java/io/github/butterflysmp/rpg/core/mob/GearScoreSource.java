package io.github.butterflysmp.rpg.core.mob;

/**
 * Where a hostile mob's gear score comes from, when it does not already carry one. The seam for M3's
 * exceptions (boss mobs, activity mobs), which are not built: {@link #BLANKET} is the only
 * implementation, and the seed holds one field of this type. There is deliberately no chooser, no
 * list and no per-type dispatch -- a chooser with one arm is an arm nothing exercises.
 *
 * <p><b>A stored score always wins over every source (M5).</b> This is consulted only for a mob with
 * no score, so a later boss source can never re-roll a mob that already has one.
 */
@FunctionalInterface
public interface GearScoreSource {

    int gearScoreFor(MobDimension dimension, double horizontalDistance);

    /**
     * The blanket rule, M25's dimension curve ({@link DimensionGearScore}). It was named {@code DISTANCE}
     * until M25 removed the distance term from the overworld and the Nether.
     */
    GearScoreSource BLANKET = DimensionGearScore::of;
}
