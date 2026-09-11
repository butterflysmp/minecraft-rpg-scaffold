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
 * <h2>WHY THERE IS NO FLOOR, AND WHAT MUST LAND WITH ONE IF A REDUCING MODIFIER EVER SHIPS</h2>
 *
 * <p>{@link #boosts} is strictly {@code >}, matching {@link ManaRegen}, {@link HealthRegen},
 * {@code Growth}, {@code ManaBank}, {@code Protection} and {@code Bulwark} -- <b>every stat helper
 * in this repository is increase-only</b>, so a negative amount never becomes a modifier at all.
 * {@code WeaponDefinition} already rejects {@code quiver_size < 0} and requires {@code > 0} for a
 * quiver weapon, so {@link #resolve}'s output cannot fall below the authored base. <b>There is no
 * floor because there is no way down.</b>
 *
 * <p>An unreachable guard is worse than an absent one -- it looks load-bearing, it is never
 * exercised by anything but a test asserting it exists, and this repository records the case
 * ({@code ContentValidator.validateElements}'s status arm) as a dead catch with a green suite around it.
 * So the floor is not written today.
 *
 * <p><b>But the argument FOR one is a mechanism and not a feel, so it is recorded rather than
 * rediscovered.</b> A capacity of 0 does not make a very small quiver, it makes a PERMANENTLY DEAD
 * weapon, and {@code QuiverStateTest} witnesses it directly: at {@code loaded 0, capacity 0},
 * {@link QuiverState#fireVerdict} is {@code EMPTY} and {@link QuiverState#reloadVerdict} is
 * {@code ALREADY_FULL} -- refused on both inputs, with no third input and no recovery path. So the
 * day a reducing modifier is wanted, <b>a floor of at least 1 lands in the same commit</b>, and it
 * is a mechanism argument (an item with no reachable state) rather than a balance number.
 */
public final class QuiverSize {

    private QuiverSize() {}

    /** No bonus. The {@code 0-is-absent} convention of every stat helper beside this one. */
    public static final int NONE = 0;

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
     * The resolved capacity: a weapon's authored magazine plus whatever the wielder's gear adds.
     *
     * <p><b>THIS ADDITION IS IN {@code core} ON PURPOSE.</b> Its natural home looks like
     * {@code QuiverItems.resolveCapacity}, which is the single site that reads the stat -- and that
     * file needs an {@code ItemStack}, so a decision placed there is permanently boot-only. This
     * slice has already paid that twice. The decision is one line; the READ stays in {@code paper}.
     *
     * @param authoredCapacity the weapon's own {@code quiver_size}, already validated {@code > 0}
     * @param bonusStatValue   {@code HealthState}'s summed modifiers, in arrows
     */
    public static int resolve(int authoredCapacity, double bonusStatValue) {
        return authoredCapacity + arrows(bonusStatValue);
    }

    /**
     * The one conversion from the {@code double} a {@code Stat} sums to whole arrows. FLOOR.
     *
     * <p><b>Exact for everything this path can produce, and the floor is for everything else.</b>
     * {@link #contribution} takes an {@code int}, so every modifier is integral and a sum of
     * integral {@code double}s is exact far below {@code 2^53} -- flooring such a value returns it
     * unchanged. The rule therefore only bites on a value that arrived from somewhere other than
     * {@link #contribution}, and there it matches {@link Quiver#applyPercent}: <b>flooring rounds
     * against the player</b>, for buffs and debuffs alike, so no rounding rule has to be chosen
     * twice in one weapon.
     */
    public static int arrows(double bonusStatValue) {
        return (int) Math.floor(bonusStatValue);
    }
}
