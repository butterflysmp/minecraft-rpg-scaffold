package io.github.butterflysmp.rpg.core.combat;

/**
 * BEN'S VOID RULING (2026-11-19, PLAN-dragon-fight.md section 1 j; the game-wide account is PLAN-world-damage.md):
 * "A player should lose 10% of their health each damage tick (0.5s) to the void regardless of their max health."
 *
 * <p>One void tick takes {@code share} of the player's MAXIMUM health, with defence bypassed: a share of the bar, not a
 * hit, so armour, the shield and every other mitigation stay out of it ({@link DefenseRule#BYPASSED}). It wears no
 * element. Where the void begins and how often it ticks stay the server's (PLAN-world-damage.md section 2, READ).
 *
 * <p><b>THE SNAP, AND WHY IT IS HERE.</b> The ruling's consequence is that the tenth tick takes the last of a full bar.
 * Naive subtraction does not deliver it: {@code max * 0.10} subtracted ten times leaves floating residue for 16,001 of
 * the 39,999 half-step maxima from 1 to 20,000 (EXECUTED, 2026-11-19; the first is 1.0), so a player would survive the
 * tenth tick on {@code 1e-16} health and go down on the eleventh. So a tick that would leave less than
 * {@link #RESIDUE} of the maximum takes everything that is left. {@code VoidShareTest} walks the whole range.
 *
 * <p>Pure: no Bukkit, no store. The Paper handler asks this with the store's own maximum and current, and applies the
 * answer through {@code CombatantHandle.applyDamage(amount, self, NORMAL, defense)}.
 */
public final class VoidShare {

    /** Below this fraction of the maximum, what would remain is floating residue, not health. */
    static final double RESIDUE = 1e-9;

    private VoidShare() {}

    /** One void tick: how much it takes, and that defence does not touch it. */
    public record Hit(double amount, DefenseRule defense) {}

    /**
     * @param max     the player's maximum health in our store
     * @param current his current health in our store
     * @param share   the content's share of the maximum per tick, in (0, 1] (Ben's 0.10)
     */
    public static Hit hitFor(double max, double current, double share) {
        if (!(share > 0.0 && share <= 1.0)) {
            throw new IllegalArgumentException("void share must be in (0, 1], got " + share);
        }
        double amount = max * share;
        if (current - amount < max * RESIDUE) amount = current;
        return new Hit(amount, DefenseRule.BYPASSED);
    }
}
