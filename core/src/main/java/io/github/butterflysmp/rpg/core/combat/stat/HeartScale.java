package io.github.butterflysmp.rpg.core.combat.stat;

/**
 * The two-tier map from custom health to vanilla hearts. Pure math, so the whole scale is
 * unit-testable; the Paper renderer only writes the result onto the vanilla heart bar.
 *
 * The trick that dodges vanilla's 1024 cap: heart COUNTS stay small (~10-20) even when custom max is
 * huge, so they fit under vanilla's display range. The cap bites raw numbers; hearts are not raw
 * numbers. So a mob shows a raw nameplate (custom, uncapped) while a player rides the heart bar.
 *
 * Two independent axes:
 *  - Heart COUNT comes from MAX, non-linearly: the first 100 HP is 10 hearts (10 HP each), then
 *    every 100 HP above that adds one more heart (100 HP each). So 100 -> 10, 250 -> 12, 400 -> 13,
 *    1000 -> 19.
 *  - FILL comes from the PERCENTAGE current/max, applied to that count: 200/400 -> 0.5 * 13 = 6.5
 *    hearts filled. Fill is a fraction of the count, independent of the tier scale.
 */
public final class HeartScale {

    private HeartScale() {}

    /** Hearts below the tier boundary and the HP-per-heart in each tier. Named, not magic. */
    private static final double TIER_BOUNDARY = 100.0;
    private static final int BASE_HEARTS = 10;          // hearts covering the first 100 HP
    private static final double HP_PER_HEART_LOW = 10.0;   // first tier: 10 HP per heart
    private static final double HP_PER_HEART_HIGH = 100.0; // above the boundary: 100 HP per heart

    /**
     * How many hearts represent a combatant with {@code max} custom health.
     * {@code max <= 100 -> ceil(max/10)}; {@code max > 100 -> 10 + ceil((max-100)/100)}.
     */
    public static int heartCount(double max) {
        if (max <= 0) return 0;
        if (max <= TIER_BOUNDARY) {
            return (int) Math.ceil(max / HP_PER_HEART_LOW);
        }
        return BASE_HEARTS + (int) Math.ceil((max - TIER_BOUNDARY) / HP_PER_HEART_HIGH);
    }

    /**
     * How many of those hearts are filled, as a fraction: {@code (current/max) * heartCount(max)}.
     * Fractional on purpose -- 6.5 renders as six-and-a-half hearts on the vanilla bar. Zero max (a
     * combatant with no health at all) fills nothing rather than dividing by zero.
     */
    public static double filledHearts(double current, double max) {
        if (max <= 0) return 0.0;
        return (current / max) * heartCount(max);
    }

    /**
     * The INVERSE of {@link #filledHearts}, in vanilla HEALTH POINTS rather than hearts: how much
     * custom HP a vanilla heal of {@code healthPoints} is worth to a combatant whose ceiling is
     * {@code max}.
     *
     * <p>Every other method here maps custom health OUT to the vanilla bar, because that is the only
     * direction display needs. This one exists for the one place the traffic runs the other way: a
     * cancelled {@code EntityRegainHealthEvent} arrives holding a vanilla amount, and rerouting it
     * into {@code CombatantStats.heal} needs it in custom HP.
     *
     * <p>Points, not hearts, because that is the unit the event and {@code HeartBarRenderer} both
     * speak -- the renderer doubles {@link #filledHearts} on the way out, so this halves on the way
     * in. Hence the {@code * 2}: {@code custom = points * max / (heartCount(max) * 2)}.
     *
     * <p><b>Proportional, not one-to-one, and that is the decision.</b> A potion healing 4 points is
     * "two hearts" to a player, so it restores two hearts' worth of whatever their bar is scaled to --
     * 20% of max. A flat 1:1 would make every potion in the game worthless the moment Growth raised
     * someone's ceiling, which is exactly the failure this arc exists to fix.
     *
     * <p>Zero for a non-positive max or a non-positive amount: there is no bar to translate onto, and
     * a negative must not become a heal. {@link #heartCount} returns 0 there, so this also avoids
     * dividing by it.
     */
    public static double customFromHealthPoints(double healthPoints, double max) {
        if (max <= 0 || healthPoints <= 0) return 0.0;
        int hearts = heartCount(max);
        if (hearts <= 0) return 0.0;
        return healthPoints * max / (hearts * 2.0);
    }
}
