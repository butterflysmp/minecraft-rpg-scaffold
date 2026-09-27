package io.github.butterflysmp.rpg.core.mob;

/**
 * The bounds of a MOB's gear score, and nothing else.
 *
 * <h2>NOT {@code core.weapon.GearScore}, AND THE EQUAL 500s ARE A COINCIDENCE THE NAMES PROTECT</h2>
 *
 * {@link #CAP} is 500 and so is {@code GearScore.HARD_CAP}. They are different quantities -- a mob's
 * ceiling (Ben's ruling M20) and an item's ceiling -- and nothing forces them to move together. While
 * they are equal, swapping one for the other is invisible to every test, so the separate name is the
 * only protection there is. {@code GearScore}'s own javadoc takes the same position on its three 100s.
 */
public final class MobGearScore {

    private MobGearScore() {}

    /** M20: every mob's GS is capped at 500, in every dimension. */
    public static final int CAP = 500;

    /**
     * The lowest score a mob can carry. Every curve starts at 100 or above, so nothing of ours ever
     * stores less; the bound exists for a value someone else wrote (a {@code /summon} with
     * {@code BukkitValues}). Below it the multiplier would be zero or negative: an unkillable or
     * born-dead mob.
     */
    public static final int MIN = 1;

    /**
     * Whether a score read back off a mob can be trusted. Outside {@code MIN..CAP} it was not written
     * by us, and the caller re-rolls it from position -- which is not a re-roll in M5's sense, because
     * nothing of ours ever stored it.
     */
    public static boolean isValidStored(int score) {
        return score >= MIN && score <= CAP;
    }
}
