package io.github.butterflysmp.rpg.core.mob;

/**
 * How much a mob's seeded stats are multiplied by: the vanilla 5x (M1) and the gear-score multiplier
 * (M2). {@link MobSeeding} decides WHICH base a mob starts from; this decides HOW MUCH it is scaled.
 * Kept apart so {@code MobSeedingTest}'s separation property stays about the base alone.
 *
 * <pre>
 *                     vanilla factor      GS factor
 *   vanilla hostile        5              gs / 100      zombie GS 500: 20 x 5 x 5 = 500
 *   vanilla passive        5              1             cow: 10 x 5 = 50            (M7)
 *   custom hostile         1              gs / 100      Knell GS 100: 360           (M4)
 *   custom passive         1              1             none shipped                (M4 + M7)
 * </pre>
 *
 * <p>Health only, this slice. The attack half lands in slice 2, beside the code that reads it -- an
 * output nothing reads is a claim nothing tests.
 *
 * <p><b>There is no environmental factor, by ruling.</b> Environmental damage and healing on a mob are
 * PROPORTIONAL (M13-M15), which is {@code DamageScale.toCustom} against the mob's VANILLA max-health
 * attribute. That is only right while the scaled max never reaches that attribute (M9).
 */
public final class MobScaling {

    private MobScaling() {}

    /** M1: players are 5x vanilla health, so every vanilla mob is too. A custom mob is not (M4). */
    public static final double VANILLA_FACTOR = 5.0;

    /**
     * The gear-score divisor: a GS of exactly this scales nothing.
     *
     * <p><b>Not {@code core.weapon.GearScore.BASELINE}</b>, though both are 100. An item's divisor and a
     * mob's are different quantities; importing one into the other would couple item tuning to mob
     * tuning invisibly, and no test can tell two equal constants apart.
     */
    public static final int GS_BASELINE = 100;

    /**
     * The max HP to seed a mob at.
     *
     * @param base      {@link MobSeeding#maxHealth}'s answer: the content definition's, or vanilla's
     * @param isCustom  the mob carries a {@code mob_id} the registry knows ({@link MobSeeding#isCustom})
     * @param isHostile M21's line; a passive mob's {@code gs} is ignored entirely (M7)
     * @param gs        the mob's stored gear score; must be at least {@link MobGearScore#MIN} when hostile
     */
    public static double maxHealth(double base, boolean isCustom, boolean isHostile, int gs) {
        double vanilla = isCustom ? 1.0 : VANILLA_FACTOR;
        return base * vanilla * gsFactor(isHostile, gs);
    }

    private static double gsFactor(boolean isHostile, int gs) {
        if (!isHostile) return 1.0;
        // Loud, not soft: a hostile mob reaching here without a valid score is a seeding bug, and a
        // 0 factor would make it unkillable -- MobSeeding's "worst outcome".
        if (!MobGearScore.isValidStored(gs)) {
            throw new IllegalArgumentException("hostile mob gear score must be in "
                    + MobGearScore.MIN + ".." + MobGearScore.CAP + ", was " + gs);
        }
        return (double) gs / GS_BASELINE;
    }
}
