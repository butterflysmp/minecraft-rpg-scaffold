package io.github.butterflysmp.rpg.core.enchant;

import java.util.List;

/**
 * Reading an authored {@code percent_by_level} curve at a level. The one lookup every curve-carrying
 * enchant shares, whatever its mechanism.
 *
 * <h2>Why this class exists, and it is a NAME problem rather than a duplication one</h2>
 *
 * This lived on {@link DamageEnchants} until Slice 2b, from when damage was the only mechanism with a
 * curve. Slice 2a's plan named the trigger for moving it -- "a third caller" -- and by the time 2b
 * started it was over-met: four external callers, and two of them read a BLOCK or REFLECT curve. A
 * block enchant asking {@code DamageEnchants} for its percentage implies a coupling that does not
 * exist and sends the next reader looking for damage arithmetic that is not there.
 *
 * <p>So this is a rename, not a refactor. The body is a verbatim lift, and
 * {@link DamageEnchants#percentAt} now delegates here keeping its exact signature -- which is what
 * lets {@code DamageEnchantsTest} pass with ZERO edits. <b>That zero-edit pass is the faithfulness
 * check</b>: if the test had needed touching, the move would not have been pure.
 *
 * <p>For the same reason there is deliberately <b>no {@code EnchantCurveTest}</b>. A mutation here
 * still reddens {@code DamageEnchantsTest} through the delegation, so a second suite pinning one
 * function would leave neither as the authority.
 */
public final class EnchantCurve {

    private EnchantCurve() {}

    /**
     * The percent this curve grants at {@code level}: {@code percentByLevel[level - 1]}.
     *
     * <p>Levels at or below 0 grant nothing -- a locked or absent enchant must not scale anything, and
     * this is the branch every unenchanted piece of gear in the game takes.
     *
     * <p><b>CLAMPED to the list, and that is a real guard rather than defensive habit.</b>
     * {@link EnchantState#effective()} clamps to {@link EnchantState#MAX_LEVEL} (the model's global
     * 3), NOT to the individual enchant's authored {@code max_level}. The loader holds those equal
     * for a file it accepted, but a hand-edited item or a blob written by a build whose content
     * differed can carry level 3 for a two-entry curve. Without the clamp that is an
     * {@code IndexOutOfBoundsException} thrown from inside a reconcile tick -- or, since 2b, from
     * inside a blocked hit -- on a path that must be total. It fails toward the enchant's own top
     * percent, never past it, the same direction {@code Unbreaking.consumeChance}'s clamp fails in.
     */
    public static double percentAt(List<Integer> percentByLevel, int level) {
        if (percentByLevel == null || percentByLevel.isEmpty() || level <= 0) return 0.0;
        int index = Math.min(level, percentByLevel.size()) - 1;
        Integer percent = percentByLevel.get(index);
        return percent == null ? 0.0 : percent;
    }
}
