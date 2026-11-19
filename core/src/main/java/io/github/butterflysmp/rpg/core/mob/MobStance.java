package io.github.butterflysmp.rpg.core.mob;

/**
 * HOW A MOB STANDS (the dragon fight's step F6, PLAN-dragon-fight.md): two content flags a mob file may set.
 *
 * @param glowing    it glows, seen through walls (cfde822's Mage did; Ben, 2026-11-18: "yes for now")
 * @param stationary it stays where it is put: no movement, no attacks, no targeting, and no knockback moves it.
 *                   Gravity still acts, so it stands on the floor (cfde822's Mage had no AI and no attacks)
 */
public record MobStance(boolean glowing, boolean stationary) {

    /** Neither: every mob file that sets no stance. */
    public static final MobStance NONE = new MobStance(false, false);
}
