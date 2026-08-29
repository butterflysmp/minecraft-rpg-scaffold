package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.Shield;

/**
 * The mechanism {@code EnchantEffect.BLOCK_DR} names: a bonus ADDED to the shield's own block
 * fraction, so a Bulwark III roundshield stops 65% of a hit instead of 50%.
 *
 * <p>Lives in {@code core/enchant} beside {@link Unbreaking} and {@link DamageEnchants}, not beside
 * {@code Shield}, for the reason {@link EnchantEffect}'s javadoc gives: adding a constant there
 * means adding a mechanism, and they live in one package so there is one place to look for what an
 * enchant can DO. It depends on {@code core/combat/Shield} for the clamp; that is a core-to-core
 * edge and costs nothing.
 *
 * <h2>ADDITIVE on the fraction, and that is a decision with two alternatives it beat</h2>
 *
 * "Increase the block DR by 5%" has three readings, and on the only shield we ship two of them are
 * <b>bit-identical</b>. Executed against the real {@code Shield.clamp} at {@code block_dr: 0.5}:
 *
 * <pre>
 *   additive   dr + p        ->  0.55   0.60   0.65
 *   multiply   dr * (1 + p)  ->  0.525  0.55   0.575
 *   cut-pass   1-(1-dr)(1-p) ->  0.525  0.55   0.575     &lt;- the same numbers
 * </pre>
 *
 * The two REJECTED readings are bit-identical to each other at 0.5; additive is distinguishable from
 * both, so a boot gate on the roundshield does catch a wrong implementation -- it just cannot say
 * which of the two wrong rules it followed. All three separate at {@code block_dr: 0.8}
 * ({@code 0.9500000000000001} / {@code 0.9199999999999999} / {@code 0.8300000000000001}), which is
 * why the test asserts there too: 0.5 pins the composition at one point, 0.8 pins the RULE.
 *
 * <p>Additive also keeps the tooltip honest: "+15%" adds fifteen points, where multiplicative would
 * add 7.5 and still be labelled 15.
 *
 * <h2>The clamp is inherited, not re-written, and this is its second consumer</h2>
 *
 * {@link #effectiveDr} routes through {@link Shield#clamp}, which Slice 1 wrote and which until now
 * had exactly one caller. So every guard already proven there holds here for free, and the enchant
 * stack is what validates that clamp rather than merely re-stating it. Executed:
 *
 * <pre>
 *   effectiveDr(0.5, 0)      -> 0.5                (an unenchanted shield is untouched)
 *   effectiveDr(0.5, 5)      -> 0.55               a 15.0 hit passes 6.749999999999999
 *   effectiveDr(0.5, 10)     -> 0.6                a 15.0 hit passes 6.0
 *   effectiveDr(0.5, 15)     -> 0.65               a 15.0 hit passes 5.25
 *   effectiveDr(0.9, 15)     -> 1.0                a 15.0 hit passes 0.0   -- TOTAL IMMUNITY
 *   effectiveDr(0.5, -1000)  -> 0.0
 *   effectiveDr(0.5, NaN)    -> 0.0
 *   effectiveDr(NaN, 15)     -> 0.0
 * </pre>
 *
 * Note {@code 6.749999999999999}: it is not {@code 6.75}, and the difference is why the composition
 * test carries an epsilon. This project has been caught by a hand-predicted float three times.
 *
 * <p><b>The immunity ceiling is a real constraint on future content, not a defensive branch.</b>
 * Additive means any shield authored at {@code block_dr >= 0.85} becomes untouchable with Bulwark
 * III. Nothing shipped reaches it -- the roundshield tops out at 0.65 -- and the clamp makes the
 * failure mode "invulnerable" rather than "negative damage", but a soft cap below 1.0 is the
 * decision the day a high-DR shield is authored. Recorded in NEXT.md.
 *
 * <p><b>A large negative bonus zeroes the SHIELD, not just the bonus</b> ({@code 0.5 + -10.0} is
 * {@code -9.5}, which clamps to {@code NONE}). That is the composition being honest rather than
 * clever, and it is unreachable from content: {@code EnchantDefinition} refuses a negative percent
 * at the boundary, which is the only surface an author can reach.
 */
public final class Bulwark {

    private Bulwark() {}

    /**
     * No bonus. The neutral value, and what an unenchanted shield -- or a shield whose Bulwark is
     * still locked -- contributes. The same 0.0-is-absent convention {@link Shield#NONE} and
     * {@code SweepShare.NONE} already use.
     */
    public static final double NONE = 0.0;

    /**
     * Does this percent declare a bonus at all? Strictly {@code >}, so a 0% curve entry is not a
     * boost -- matching {@code Shield.blocks} and {@code SweepShare.sweeps}, read the same way.
     *
     * <p>The rider uses it to skip the composition entirely for the overwhelmingly common
     * unenchanted block, so it is a fast path as well as a predicate.
     */
    public static boolean boosts(double bonusPercent) {
        return bonusPercent > NONE;
    }

    /**
     * The shield's effective block fraction with this bonus applied: {@code clamp(baseDr + p/100)}.
     *
     * <p>THE one place the composition rule lives, so the tooltip and the rider cannot disagree
     * about what Bulwark does -- the same reason {@code DamageEnchants.multiplier} is one function.
     * That matters here more than usual: the shield lore renders this number and the block rider
     * acts on it, and a tooltip reading 50% while the shield stops 65% is a display disagreeing
     * with truth.
     *
     * <p>No input guards, deliberately. {@code EnchantDefinition} refuses a negative percent at the
     * content boundary and {@link Shield#clamp} bounds both ends of the result, so a guard here
     * would be a third check on an input that has already passed two -- and would hide, rather than
     * surface, a loader that had stopped validating. The explicit precedent is {@code SweepShare},
     * whose javadoc records the same reasoning in as many words.
     */
    public static double effectiveDr(double baseDr, double bonusPercent) {
        return Shield.clamp(baseDr + bonusPercent / 100.0);
    }
}
