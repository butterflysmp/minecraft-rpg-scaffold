package io.github.butterflysmp.rpg.core.enchant;

/**
 * {@code effect: crit_chance} (K9, the seat's brief of 2026-11-25; Sharpened Glass, the Broken Bottle's guaranteed
 * enchant). Ben: "Sharpened Glass - +5/10/15% Crit Chance".
 *
 * <p>The file authors WHOLE PERCENT POINTS, as every other percent enchant here does; the stat it reaches,
 * {@code critChance}, is a probability in [0, 1]. So the contribution is the percent over 100. FLAT: the gear score is
 * never a term. It is a sibling of {@link ManaBank} ({@code boosts}, {@code contribution}), and like it, it names a stat
 * that already has a reconcile: the ONE crit-chance reconcile, as a source of its own ({@code CritChanceEnchantItems},
 * keyed apart from the held weapon's own, so the two ADD: 15% base + 10% + 15% = 40%).
 */
public final class CritChanceEnchant {

    private CritChanceEnchant() {}

    /** No enchant, no source: absent, never a zero entry. */
    public static final double NONE = 0.0;

    public static boolean boosts(double percentPoints) {
        return percentPoints > NONE;
    }

    /** Percent points (15) as the probability the stat holds (0.15). */
    public static double contribution(double percentPoints) {
        return percentPoints / 100.0;
    }
}
