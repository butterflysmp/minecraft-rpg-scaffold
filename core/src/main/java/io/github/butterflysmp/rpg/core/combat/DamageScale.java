package io.github.butterflysmp.rpg.core.combat;

/**
 * Vanilla damage numbers, converted into the custom store's scale.
 *
 * <h2>The defect this closes</h2>
 *
 * The environmental rider spent {@code event.getDamage()} — a VANILLA-scale number, lava = 4 on a
 * 20-point bar — directly against a custom store where a player bases at 100. Measured 2026-09-06:
 * a {@code FALL raw=25} left a player alive where vanilla kills outright, and drowning took ~100 s
 * against vanilla's ~10 s.
 *
 * <p>An audit of every {@code applyDamage} call site found <b>exactly one vanilla-denominated
 * entry</b> — the environmental rider. Sweep, mob-melee and thorns price from the custom attack stat;
 * ability and weapon damage from content; the dev commands from an operator-typed integer. So this
 * conversion has one call site by construction, and adding a second would be the bug.
 *
 * <p><b>Since mob scaling slice 2 that call site is {@link RerouteDamagePrice#of}</b>, which the rider
 * calls once. A mob-sourced hit on a player takes M16's flat price INSTEAD of this conversion, never
 * after it; every other rerouted hit still comes here.
 *
 * <h2>THE DENOMINATOR IS 20, AND IT IS NOT {@code heartCount*2}</h2>
 *
 * The obvious factor is {@code customMax / (heartCount(customMax)*2)} — the player's rendered bar.
 * <b>It is wrong, because {@code heartCount} is a DISPLAY function:</b> above 100 HP it compresses one
 * heart to 100 points so the bar fits on screen. Keying damage to it would give a cosmetic decision
 * authority over every environmental damage number in the game.
 *
 * <p><b>That would be display-becomes-truth for the FOURTH time in this slice</b> — after the display
 * floor undoing a death, {@code ArmorBarOverride} leaking our DR into vanilla's own mitigation curve,
 * and the original bar-moves-number-doesn't defect. The first three were accidents; this one would
 * have been deliberate.
 *
 * <pre>
 *                    heartCount*2      20
 *   fall  @ max 100        5.00      5.00
 *   fall  @ max 400       15.38     20.00
 *   drown @ max 100       10.0%     10.0%
 *   drown @ max 400        7.7%     10.0%
 * </pre>
 *
 * <p>With 20, the operator's rule — <i>"10% of max health regardless of how much defense or health
 * they have"</i> — is <b>exactly true at every max</b>. That is the only statement he has made about
 * non-base maximums, made unprompted a week before the question arose, and it settles the choice.
 *
 * <h2>Fail soft to k = 1, on BOTH sides of the divide</h2>
 *
 * A missing denominator, and equally a missing or non-positive {@code customMax}, returns the vanilla
 * amount <b>unscaled</b>. Never zero, and never larger than the input.
 *
 * <p><b>Guarding only the denominator would leave the worse hole open:</b> {@code customMax = 0} makes
 * k zero, every hit converts to nothing, and the combatant is <b>unkillable</b>. That is precisely the
 * outcome {@code MobSeeding}'s javadoc names as the worst one — <i>"an unkillable mob (0 max, never
 * draining) … [is] worse than a mob that quietly reverts to its vanilla numbers"</i> — and it is the
 * same sentence this class cites for the denominator's fallback. The rule applies to both operands or
 * it is not the rule.
 */
public final class DamageScale {

    /**
     * Vanilla's own player bar, in health points: 20, ten hearts.
     *
     * <p>The denominator for any combatant whose vanilla max is a PUPPET. Deliberately a constant and
     * not a read of the rendered bar — see the class javadoc for why reading the display back would be
     * this slice's fourth display-becomes-truth defect. It also removes a hazard rather than measuring
     * one: a puppeted entity's attribute is stale between registration and the first render, and this
     * never consults it.
     */
    public static final double VANILLA_BAR_POINTS = 20.0;

    private DamageScale() {}

    /**
     * Convert a vanilla-scale damage amount into custom-store units.
     *
     * <p>{@code k = customMax / denominator}, where the denominator is {@link #VANILLA_BAR_POINTS} for
     * a puppeted bar and the entity's REAL max-health attribute otherwise:
     *
     * <pre>
     *   player              100 / 20   -> k = 5
     *   spider, GS 100       80 / 16   -> k = 5   (custom max = attribute x 5 x GS/100, seeded)
     *   zombie, GS 300      300 / 20   -> k = 15
     *   the Knell, GS 100   360 / 20   -> k = 18
     * </pre>
     *
     * <p><b>For a mob, k is Ben's M13/M14 PROPORTION, word for word</b> -- vanilla amount / vanilla
     * max x the mob's max, so a fall that kills a vanilla zombie kills any zombie at any gear score.
     * <b>It is right only while the denominator stays VANILLA</b>: mob scaling seeds the scaled max
     * into the custom store and never into the attribute (M9). Write it into the attribute and k
     * collapses to 1 for every mob, silently. (Until mob scaling, an untagged mob's k was 1, because
     * its custom max WAS its attribute.)
     *
     * @param vanillaAmount       the raw vanilla number. Non-positive returns 0.
     * @param customMax           the victim's custom max. Non-positive or NaN FAILS SOFT to k = 1 —
     *                            zero here would make the combatant unkillable, not merely mis-scaled.
     * @param vanillaMaxAttribute the entity's real MAX_HEALTH attribute, or NaN when it has none.
     *                            Ignored entirely when {@code barIsPuppeted}. Non-positive or NaN
     *                            FAILS SOFT to k = 1.
     * @param barIsPuppeted       whether this entity's vanilla max is written from the custom numbers.
     *                            <b>Not "is it a player"</b> — see {@code CombatantStats.isBarPuppeted}
     *                            for why the distinction is kept even though the two agree today.
     */
    public static double toCustom(double vanillaAmount, double customMax,
                                  double vanillaMaxAttribute, boolean barIsPuppeted) {
        if (!(vanillaAmount > 0)) return 0.0;
        // NaN-safe by construction: every comparison against NaN is false, so `!(x > 0)` catches both
        // the absent attribute and the non-positive one without a separate isNaN test.
        if (!(customMax > 0)) return vanillaAmount;

        double denominator = barIsPuppeted ? VANILLA_BAR_POINTS : vanillaMaxAttribute;
        if (!(denominator > 0)) return vanillaAmount;

        return vanillaAmount * customMax / denominator;
    }
}
