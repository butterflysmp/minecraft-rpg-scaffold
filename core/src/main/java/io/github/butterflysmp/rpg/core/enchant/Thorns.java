package io.github.butterflysmp.rpg.core.enchant;

/**
 * The mechanism {@code EnchantEffect.REFLECT} names: a fraction of the incoming blow, sent back at
 * whoever threw it.
 *
 * <p>Lives in {@code core/enchant} beside {@link Unbreaking}, {@link DamageEnchants} and
 * {@link Bulwark}, by {@link EnchantEffect}'s rule that mechanisms share a package so there is one
 * place to look for what an enchant can DO.
 *
 * <h2>Named Thorns, and NOT Thorns</h2>
 *
 * {@code DESIGN-status-effects.md} reserves "Thorns" for a Nature PROPAGATION status -- damaging a
 * Thorns target damages NEARBY targets, depth-1, with four load-bearing anti-loop safety rules
 * attached to that name. This is a different mechanic: one attacker, no propagation, no safety
 * rules. The rule applied was to rename the mechanic with no load-bearing associations rather than
 * the one that has them, so the safety rules stay unambiguously attached to the name that needs them.
 *
 * <h2>A fraction of the PRE-MITIGATION blow</h2>
 *
 * The input is the attacker's raw attack stat -- before the shield's block fraction AND before the
 * victim's armor, which is applied a thread-hop later inside {@code CombatantStats.damage}. So a
 * heavily armored player reflects more than the hit actually did to them.
 *
 * <p><b>That is forced, not chosen.</b> The post-mitigation figure does not exist yet on the thread
 * where the reflect is computed -- the identical constraint {@code SweepShare} records for the swept
 * mob's share. It is also the reading that keeps Thorns and Bulwark independently tunable: off the
 * blocked amount, Bulwark would secretly boost Thorns; off the pass-through, Bulwark would reduce
 * it. Semantically it reflects the attacker's blow, not the player's absorption, so a common and a
 * legendary shield with equal Thorns reflect equally.
 *
 * <p><b>Call it pre-MITIGATION, never pre-block</b>, in every comment and content file. "Pre-block"
 * understates it by a whole armor pass and the number reads as a bug the first time it is measured.
 *
 * <p>Worked, executed rather than reasoned -- off a 15.0 mob at the shipped curve:
 * {@code reflected(15.0, 10) -> 1.5}; {@code (15.0, 20) -> 3.0}; {@code (15.0, 30) -> 4.5}.
 * The damage popup ROUNDS ({@code Math.round}), so those render as <b>2 / 3 / 5</b>.
 */
public final class Thorns {

    private Thorns() {}

    /**
     * No reflect. The neutral value, and what a shield without Thorns -- or any unblocked hit --
     * contributes. Same 0.0-is-absent convention {@link Bulwark#NONE} and {@code SweepShare.NONE} use.
     */
    public static final double NONE = 0.0;

    /**
     * Is there anything to send back?
     *
     * <p><b>This gates the OUTPUT, and that is deliberately NOT what {@link Bulwark#boosts} and
     * {@code SweepShare.sweeps} do.</b> Those gate the authored fraction before the arithmetic; this
     * gates the product after it. Do not describe it as "the same convention" -- it is not, and the
     * difference is the point.
     *
     * <p>Gating the output is what makes a negative reflect unreachable. A percent cannot be negative
     * (the content boundary refuses it), but the attack stat can: {@code Stat} is
     * {@code base + sum(modifiers)}, so a debuffed attacker can carry a negative attack value, and
     * {@code negative x positive} is negative. A negative amount handed to {@code applyDamage} reaches
     * {@code stats.damage} and <b>HEALS the attacking mob</b>. The strict {@code >} closes that; a
     * percent-side gate would not have.
     */
    public static boolean reflects(double amount) {
        return amount > NONE;
    }

    /**
     * The damage to send back: {@code preMitigation * percent / 100}.
     *
     * <p>THE one place the reflect formula lives, so the effect line, the content comment and the
     * rider cannot disagree about what Thorns does.
     *
     * <p>No input guards, deliberately. {@code EnchantDefinition} refuses a negative percent at the
     * content boundary -- the only surface an author can reach -- and {@link #reflects} bounds the
     * output at the one call site that deals it. A third check here would be a guard in the kernel
     * for an input the kernel cannot receive, and would hide rather than surface a loader that had
     * stopped validating. The explicit precedent is {@code SweepShare}, whose javadoc records the
     * same reasoning in as many words.
     */
    public static double reflected(double preMitigation, double percent) {
        return preMitigation * percent / 100.0;
    }
}
