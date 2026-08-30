package io.github.butterflysmp.rpg.core.enchant;

/**
 * Protection: flat armor POINTS added to the Defense of the piece it sits on.
 *
 * <p>The shape of {@link Bulwark} and {@link Thorns} -- a NONE constant, a predicate, and one line
 * of arithmetic -- and deliberately even smaller than they are, because there is less to decide.
 *
 * <h2>It ADDS, and there is nothing to clamp</h2>
 *
 * Bulwark needs {@code Shield.clamp} because a block fraction above 1.0 is a shield that heals you.
 * Defense has no such ceiling: it is a SUMMAND in points, and {@code Defense.applyDefense} converts
 * the sum through an asymptotic curve exactly once, at the point of use. That curve approaches 100%
 * reduction without ever arriving, so no quantity of Protection can reach immunity -- 56 points, a
 * full diamond set with Protection III in every slot, is about 35.9%.
 *
 * <p>Which is why points are the right unit and a percent would have been wrong. {@code HealthState}
 * records the rule this obeys: armor points add correctly across four slots (a helmet and boots are
 * 3 + 3) where damage-reduction fractions do not (two 50% sources are not 100%). Keeping the stat
 * linear is what makes stacking composable at all.
 *
 * <h2>No input guards, deliberately -- the same call Bulwark made</h2>
 *
 * A negative curve entry is refused at the content boundary by {@code EnchantDefinition}, and a
 * negative resolved Defense is already handled by {@code Defense.applyDefense}'s own
 * {@code defense <= 0} guard, which returns the damage untouched rather than amplifying it. A third
 * guard here would hide, rather than surface, a loader that had stopped validating.
 *
 * <p>There is deliberately <b>no {@code Protection.ID}</b>. Its curve is content, so the definition
 * must be resolved anyway, and binding on {@code EnchantEffect.DEFENSE} rather than on an id is what
 * makes the SECOND defense enchant a yml file instead of a recompile. Only Unbreaking is bound by
 * id, because only Unbreaking's curve is Java.
 */
public final class Protection {

    private Protection() {}

    /**
     * No bonus. The same 0.0-is-absent convention {@link Bulwark#NONE} and {@link Thorns#NONE} use,
     * and the value {@code EnchantValues.totalFor} returns for a piece carrying no Protection.
     */
    public static final double NONE = 0.0;

    /** Does this bonus grant anything at all? Strictly {@code >}, so 0 declares nothing. */
    public static boolean boosts(double bonusPoints) {
        return bonusPoints > NONE;
    }

    /**
     * A piece's effective Defense with its own Protection applied: {@code base + bonus}.
     *
     * <p>PER PIECE, not per set. Each piece carries its own enchant and contributes its own total,
     * and the four totals are summed by the reconciler into one Defense stat -- which is how
     * Protection stacks across a set without this method ever knowing a set exists.
     *
     * <p>Worked: a diamond chestplate is {@code 8 -> 11 / 14 / 17} at I / II / III; a full diamond
     * set at III is {@code 20 -> 56}.
     */
    public static double effectiveDefense(double basePoints, double bonusPoints) {
        return basePoints + bonusPoints;
    }
}
