package io.github.butterflysmp.rpg.core.enchant;

import java.util.List;

/**
 * What stripping gives back, in XP POINTS.
 *
 * <p>Beside {@link EnchantCost} because it is the same kind of thing: a pure mechanism the
 * grindstone drives, with the impure half -- reading a player's wallet, editing their items --
 * left at the call site.
 *
 * <h2>THE OLD REPO'S LOOP IS CORRECT THERE AND WRONG HERE, AND THE CALL LOOKS IDENTICAL</h2>
 *
 * The predecessor project summed a strip refund like this:
 *
 * <pre>
 *   for (int lvl = 1; lvl &lt; a.level(); lvl++) total += Formulas.enchantUpgradeXpCost(lvl, 0);
 * </pre>
 *
 * <b>An EXCLUSIVE bound, and it is right in that codebase.</b> Its function means <i>"what it
 * costs to raise an enchant FROM {@code currentLevel} to the next"</i> -- its own javadoc says so,
 * and its switch has no {@code case 0}, so <b>unlocking to level I was FREE there.</b> Summing
 * {@code 1..level-1} therefore walks exactly the rungs that were paid for.
 *
 * <p><b>{@link EnchantCost#xpPoints} means the other thing:</b> what it costs to take an enchant
 * <b>TO</b> {@code targetLevel}. Reaching I costs 352. Transplant the exclusive bound and it
 * underpays at every level, and by most at the commonest one:
 *
 * <pre>
 *   level I     0 of  352    100% short -- returns nothing at all
 *   level II  352 of 1262     72% short
 *   level III 1262 of 4182     70% short
 * </pre>
 *
 * <p><b>SO THE BOUND HERE IS INCLUSIVE: {@code for (rung = 1; rung &lt;= level; rung++)}.</b> Stated
 * with the reason rather than alone, because the two functions read identically at the call site
 * and the next person to compare them needs the semantic difference, not the fix.
 *
 * <h2>LIST PRICE, POWER ZERO, AND WHY THAT NEEDS NO LEDGER</h2>
 *
 * The refund is computed at <b>power 0</b> -- full list price -- and nothing records what a player
 * actually paid. The predecessor reached the same answer independently (<i>"no bookshelf discount
 * applied to the refund"</i>).
 *
 * <p><b>This is the entire licence for having no ledger, so it is stated rather than assumed.</b>
 * The discount caps at {@link EnchantCost#MAX_POWER}, so a player always paid <b>at least 70% of
 * list</b>. 35% of list is therefore at most {@code 35/70} = <b>50% of what they actually paid</b>,
 * and exactly 35% at full price. <b>It cannot exceed the spend at any shelf count</b>, so the
 * grindstone can never be an XP source.
 *
 * <p><b>THE ACCEPTED COST, SAID OUT LOUD: a player who enchanted cheaply gets the better RATE.</b>
 * Someone who paid 70% of list recovers half of it; someone who paid full price recovers 35%.
 * That is backwards from intent, and it is the price of not carrying a per-item ledger.
 *
 * <h2>THE TRAY IS THE UNIT, AND A PER-ITEM REFUND IS DELIBERATELY NOT PUBLIC</h2>
 *
 * {@link #points} takes the WHOLE TRAY and applies the percentage <b>once, to one total</b>.
 * {@link #listSpend} is per item and returns the RAW spend, never a refund.
 *
 * <p><b>That split is structural, not stylistic.</b> Integer division floors, so
 * {@code sum(floor(c * 35 / 100))} is up to one point short PER ITEM against
 * {@code floor(sum(c) * 35 / 100)} -- over a full tray that is <b>12 points</b> taken from the
 * player for nothing. <b>There is no public per-item refund to sum, so that mistake cannot be made
 * here.</b>
 *
 * <h2>AND THE CONSEQUENCE OF THAT: BATCHING PAYS, BY UP TO 12 POINTS. ACCEPTED.</h2>
 *
 * Stripping fourteen items in one press yields <b>up to twelve points more</b> than stripping them
 * one at a time, because the fractional remainders are kept once instead of discarded fourteen
 * times.
 *
 * <p><b>TWELVE IS MEASURED, AND THE OBVIOUS ANSWER OF THIRTEEN IS WRONG.</b> The arithmetic ceiling
 * for fourteen items is {@code n - 1} = 13, and it is <b>unreachable</b>: a per-item spend is a sum
 * of {@code 352 / 1262 / 4182}, which yields only <b>ten distinct remainders</b> under
 * {@code x * 35 mod 100}, the largest being {@code .90}. Fourteen items at {@code .90} is
 * {@code 12.6}, so the gain floors at <b>12</b>. Quoting the {@code n - 1} bound would be
 * characterising the class instead of computing it.
 *
 * <p><b>It is written down rather than left to be discovered, because an incentive nobody recorded
 * is one somebody later removes as a bug.</b> Two reasons it stands: thirteen points is noise
 * against a tray worth tens of thousands, and <b>it points the right way</b> -- it rewards using the
 * bulk feature rather than punishing it, which is the behaviour this screen exists to offer.
 *
 * <p>The alternative -- flooring per item so that one-at-a-time and all-at-once agree -- buys that
 * agreement by taking the thirteen points from the player instead. <b>Neither is exact; this one
 * fails towards the player.</b>
 */
public final class GrindstoneRefund {

    private GrindstoneRefund() {}

    /**
     * The share of list price handed back, as a percentage. Ben's ruling.
     *
     * <p>Public so the number shown and the number granted come out of one expression -- the same
     * reasoning as {@link EnchantCost#clampPower}: two expressions and the boot gate is checking
     * one against itself.
     */
    public static final int REFUND_PERCENT = 35;

    /**
     * What this item's enchants cost at LIST PRICE -- the raw spend, not the refund.
     *
     * <p>Sums <b>every slot, every candidate, and every rung {@code 1..level} INCLUSIVE</b>. A
     * player paid to unlock every candidate they unlocked, not only the one currently active, so
     * the walk is over {@link EnchantSlot#candidates()} rather than {@link EnchantSlot#active()}.
     *
     * <p>A locked candidate ({@code level == 0}) contributes nothing without a special case: its
     * rung loop does not execute. <b>So unenchanted gear costs zero and is not an edge case.</b>
     *
     * @return points, always {@code >= 0}; zero for an item with nothing unlocked.
     */
    public static int listSpend(EnchantState state) {
        if (state == null) throw new IllegalArgumentException("state required");

        int total = 0;
        for (EnchantSlot slot : state.slots()) {
            for (EnchantCandidate candidate : slot.candidates()) {
                // INCLUSIVE. See the class javadoc: the exclusive bound is the old repo's, correct
                // under its own semantics and 100% short at level I under ours.
                for (int rung = 1; rung <= candidate.level(); rung++) {
                    total += EnchantCost.xpPoints(rung, 0);
                }
            }
        }
        return total;
    }

    /**
     * The refund for stripping an ENTIRE TRAY, as one figure.
     *
     * <p>The percentage is applied <b>once, to the summed list price</b> -- see the class javadoc
     * for why there is no per-item refund to add up instead.
     *
     * <p><b>{@code total * REFUND_PERCENT / 100}, never {@code (int)(total * 0.35)}.</b>
     * {@link EnchantCost}'s javadoc already argues the integer form with a measured example, and
     * the same argument holds here. <b>The half it does not cover: this floor takes FROM the
     * player</b>, where {@code EnchantCost}'s floor takes from the price. Opposite signs, so that
     * comment must not be read as precedent for rounding the player's way here -- the rounding is
     * simply always downward, and at 35% of a four-figure spend it is worth at most 99 points.
     *
     * @param tray every item's state, in any order; an empty tray refunds 0.
     */
    public static int points(List<EnchantState> tray) {
        if (tray == null) throw new IllegalArgumentException("tray required");

        int total = 0;
        for (EnchantState state : tray) {
            total += listSpend(state);
        }
        // ONE expression, ONE flooring, over the whole tray.
        return total * REFUND_PERCENT / 100;
    }

    /**
     * How many of a tray's items would actually CHANGE -- the count the button prints.
     *
     * <p><b>Not the tray size.</b> A fourteen-item tray holding nine enchanted weapons reads
     * "Strip 9 weapons", because printing fourteen beside a refund invites the player to divide
     * the refund by fourteen. The other five are untouched no-ops and saying so is not the
     * button's job.
     *
     * <p>An item changes exactly when it has something to strip, which is exactly when its
     * {@link #listSpend} is non-zero -- the cheapest rung costs 352, so there is no item that
     * would change while spending nothing.
     */
    public static int strippableCount(List<EnchantState> tray) {
        if (tray == null) throw new IllegalArgumentException("tray required");

        int count = 0;
        for (EnchantState state : tray) {
            if (listSpend(state) > 0) count++;
        }
        return count;
    }

    /**
     * This item with every candidate locked -- the strip itself.
     *
     * <p><b>The roll and the candidate list SURVIVE.</b> Ben's ruling, and in this model it is
     * automatic rather than arranged: the roll IS the candidate list, so a fully stripped state is
     * byte-for-byte the shape {@link EnchantRoll#roll} produces for a fresh item.
     *
     * <p><b>NO ORDER IS FORCED HERE, AND THE OBVIOUS WORRY IS ALREADY HANDLED.</b>
     * {@link EnchantSlot}'s constructor refuses a locked active candidate, so it looks as though
     * active must be cleared first. It does not: {@link EnchantState#withLevel} clears active
     * itself when it locks the active candidate, and says so in its own javadoc. <b>The rule that
     * matters is to go through {@code withLevel} and never hand-build an {@code EnchantSlot}</b>
     * -- the invariant did not merely anticipate this operation, it already resolved it.
     */
    public static EnchantState stripped(EnchantState state) {
        if (state == null) throw new IllegalArgumentException("state required");

        EnchantState result = state;
        for (int slot = 0; slot < state.slots().size(); slot++) {
            int candidates = state.slots().get(slot).candidates().size();
            for (int candidate = 0; candidate < candidates; candidate++) {
                result = result.withLevel(slot, candidate, 0);
            }
        }
        return result;
    }
}
