package io.github.butterflysmp.rpg.core.mob;

/**
 * One stage's body (PLAN-dragon-mobs.md step R1). <b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code height} is the body's HEIGHT IN BLOCKS, which is the unit Ben ruled in (*"the Phage will range from
 *       1 block to 3 blocks in height"*). Paper turns it into the {@code SCALE} that produces it, from the base
 *       entity's own unscaled height ({@link MobBodies#scaleForHeight}); a creeper is 1.7 tall, so 1 block is scale
 *       1/1.7.</li>
 *   <li>{@code speedMultiplier} multiplies the base entity's DEFAULT movement speed, as {@link MobBody}'s does.</li>
 *   <li>{@code maxHealth} is what {@link MobDefinition#maxHealth} means: the AUTHORED health, which the seed then
 *       multiplies by GS/100 and never by 5 (PLAN-mob-scaling.md M4, with the GS from M25).</li>
 * </ul>
 */
public record StageBody(double height, double speedMultiplier, double maxHealth) {

    public StageBody {
        requirePositive("height", height);
        requirePositive("speed", speedMultiplier);
        requirePositive("max_health", maxHealth);
    }

    private static void requirePositive(String name, double value) {
        if (!(value > 0 && Double.isFinite(value))) {
            throw new IllegalArgumentException("stage " + name + " must be a positive number, was " + value);
        }
    }
}
