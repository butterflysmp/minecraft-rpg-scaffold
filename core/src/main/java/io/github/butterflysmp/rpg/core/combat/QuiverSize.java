package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.QuiverState;

/**
 * Quiver size: whole arrows gear adds to a weapon's authored magazine, and the one conversion
 * between the {@code double} a {@code Stat} sums and the {@code int} a magazine is counted in.
 *
 * <h2>WHOLE ARROWS, AND THE UNIT IS A TYPE RATHER THAN A CONVENTION</h2>
 *
 * <p>{@link #boosts} and {@link #contribution} take {@code int}, unlike every other stat helper in
 * this package, which take {@code double}. That is deliberate: <b>a fractional arrow is
 * unrepresentable rather than forbidden.</b> Half an arrow has no meaning in a magazine -- a quiver
 * holds 9 or it holds 10 -- and the alternative is a {@code double} parameter with a comment asking
 * callers to pass whole numbers, which is the shape this slice has spent six commits removing.
 *
 * <p><b>Percentages were considered and declined, and the number is why.</b> At
 * {@code quiver_stone}'s 9 rounds, ONE ARROW IS 11%. Any percentage under that floors to zero, so a
 * tooltip would advertise a buff that grants nothing -- {@link Quiver#applyPercent} exists and stays
 * tested, but it is the rule for a percentage REACHING an integer stat, not this stat's design.
 * Flat summands are also what {@code classDamage} uses, for the same reason.
 *
 * <h2>ZERO IS NOT A LEGAL RESOLVED CAPACITY, BY ANY ROUTE</h2>
 *
 * <p>{@link #MIN_CAPACITY} is enforced in {@link #resolve}, and the argument is a mechanism rather
 * than a balance number. {@code QuiverStateTest.aCapacityOfZeroWouldBeARefusalOnBothInputs}
 * measures it: at {@code loaded 0, capacity 0}, {@link QuiverState#fireVerdict} is {@code EMPTY} and
 * {@link QuiverState#reloadVerdict} is {@code ALREADY_FULL}. <b>Both inputs refused, no third input,
 * no recovery.</b> Capacity 0 does not make a very small quiver, it makes a permanently dead item --
 * which is why this is enforced rather than documented.
 *
 * <p><b>THE FLOOR IS NOT A CLAMP AT THE AUTHORED BASE, AND THE DIFFERENCE IS THE WHOLE POINT.</b>
 * Refusing to go below {@code authoredCapacity} would silently discard a reduction -- a defect in
 * its own right, and one this repository already names. {@code MIN_CAPACITY} honours a reduction as
 * far as a reduction can legally go and stops at the last value the item still works at:
 * {@code resolve(9, -20)} is <b>1</b>, not 9 and not -11.
 *
 * <h3>Why this is written now rather than promised, and the previous version of this section was
 * wrong</h3>
 *
 * <p>It read: <i>"every stat helper in this repository is increase-only, so a negative amount never
 * becomes a modifier at all ... there is no floor because there is no way down."</i> <b>That claim
 * was wider than what was true, in two independent ways.</b>
 *
 * <ol>
 *   <li><b>Its support was a filter with no call sites.</b> {@link #boosts} is what makes the content
 *       pipeline increase-only, and at the commit that wrote the sentence its only callers were test
 *       rows -- the scanner that applies it did not exist yet. A guard with no instances is not a
 *       guard that cannot fire, and by exactly the same token <b>a filter with no call sites is not a
 *       filter that is being applied.</b> The dead-guard principle was invoked in one direction only.
 *   <li><b>{@link #resolve} never consulted it, and still does not.</b> The filter sits at the
 *       scanner; the arithmetic is public core API. {@code resolve(9, -5.0)} returned 4. <b>The API
 *       was the way down</b>; only the content pipeline was not.
 * </ol>
 *
 * <p>The file already knew this two paragraphs apart -- {@link #arrows} reasons correctly about
 * "a value that arrived from somewhere other than {@code contribution}", and
 * {@code QuiverSizeTest} has a row asserting {@code arrows(-2.1) == -3}. The code was right; the
 * sentence above it was not.
 *
 * <p><b>And the trigger was named wrong too.</b> It said a floor lands "the day a reducing modifier
 * is wanted". The hazard is <b>the resolved capacity reaching 0 by ANY route</b>, and the feature
 * already ships one that involves no reducing modifier: {@code Quiver.applyPercent(8, -100)} is
 * {@code 0}, with a green test row. {@link Quiver#applyPercent} has no production callers today, so
 * nothing is broken -- but wiring it would be such a route, and the old trigger would not have
 * fired. Stating the condition in terms of the resolved capacity rather than the authoring shape is
 * what makes it cover routes nobody has thought of.
 *
 * <p><b>{@code applyPercent} and this class now agree about whether 0 is legal.</b> That one answers
 * "what does this percentage evaluate to", which is arithmetic and has no opinion; this one answers
 * "what capacity governs", which is the decision. A percentage that ever reaches a capacity passes
 * through {@link #resolve}, and its javadoc says so at that end too.
 *
 * <h2>WHY {@link #boosts} STILL EXISTS, GIVEN THE FLOOR</h2>
 *
 * <p>They answer different questions and neither subsumes the other. {@code boosts} decides whether
 * an item DECLARES a modifier at all -- a 0-valued fixture must write no source rather than a no-op
 * one every scan. {@code MIN_CAPACITY} decides what a resolved capacity may be once the modifiers
 * are summed. Keeping the filter is what stops the reconciler churning; keeping the floor is what
 * stops an item becoming unusable. Its caller is {@code QuiverSizeModifierItems.desiredModifiers}.
 */
public final class QuiverSize {

    private QuiverSize() {}

    /** No bonus. The {@code 0-is-absent} convention of every stat helper beside this one. */
    public static final int NONE = 0;

    /**
     * The smallest capacity an item may resolve to. See the class javadoc: at 0 a quiver weapon is
     * refused on fire AND on reload, with no third input and no way back.
     *
     * <p><b>Unlike {@code Quiver.MIN_RELOAD_TICKS}, this is not a named placeholder at the no-op
     * value.</b> That one sits at 1 because no mechanism argument exists for any reload floor, so it
     * marks the decision rather than making one. This one is the value the mechanism produces: 0 is
     * the broken state, so 1 is the smallest working one.
     */
    public static final int MIN_CAPACITY = 1;

    /** Does this bonus grant anything at all? Strictly {@code >}, so 0 declares nothing. */
    public static boolean boosts(int bonusArrows) {
        return bonusArrows > NONE;
    }

    /**
     * A piece's contribution to quiver size, in whole arrows: the bonus itself.
     *
     * <p>Named rather than inlined for the reason {@code Growth.contribution} gives -- it is the ONE
     * place a quiver-size bonus becomes a stat modifier, so a future rule (a cap, diminishing
     * returns past a threshold) has somewhere to live that is not a scan loop.
     */
    public static int contribution(int bonusArrows) {
        return bonusArrows;
    }

    /**
     * The resolved capacity: a weapon's authored magazine plus whatever the wielder's gear adds,
     * never below {@link #MIN_CAPACITY}.
     *
     * <p><b>THIS ADDITION IS IN {@code core} ON PURPOSE.</b> Its natural home looks like
     * {@code QuiverItems.resolveCapacity}, which is the single site that reads the stat -- and that
     * file needs an {@code ItemStack}, so a decision placed there is permanently boot-only. This
     * slice has already paid that twice. The decision is two lines; the READ stays in {@code paper}.
     *
     * <p><b>This is the only place a bonus becomes a capacity</b>, which is why the floor lives here
     * and not at the call site: a floor at the call site would be a rule the next call site does not
     * inherit, and this slice exists because two enforcement sites for one capacity is how a tooltip
     * and a refusal come to disagree.
     *
     * @param authoredCapacity the weapon's own {@code quiver_size}, already validated {@code > 0}
     * @param bonusStatValue   {@code HealthState}'s summed modifiers, in arrows
     */
    public static int resolve(int authoredCapacity, double bonusStatValue) {
        return Math.max(MIN_CAPACITY, authoredCapacity + arrows(bonusStatValue));
    }

    /**
     * The one conversion from the {@code double} a {@code Stat} sums to whole arrows. FLOOR.
     *
     * <p><b>Exact for everything the content pipeline can produce, and the floor is for everything
     * else.</b> {@link #contribution} takes an {@code int}, so every modifier arriving that way is
     * integral and a sum of integral {@code double}s is exact far below {@code 2^53} -- flooring
     * such a value returns it unchanged. The rule therefore only bites on a value that reached here
     * some other way, and there it matches {@link Quiver#applyPercent}: <b>flooring rounds against
     * the player</b>, for buffs and debuffs alike, so no rounding rule has to be chosen twice in one
     * weapon.
     *
     * <p><b>This returns a raw count and is NOT where legality is decided</b> -- {@code arrows(-2.1)}
     * is {@code -3}, and that is correct for what this method is. {@link #resolve} is what turns a
     * bonus into a capacity, and the floor is there.
     */
    public static int arrows(double bonusStatValue) {
        return (int) Math.floor(bonusStatValue);
    }
}
