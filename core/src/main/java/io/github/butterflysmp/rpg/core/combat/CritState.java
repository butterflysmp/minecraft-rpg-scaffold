package io.github.butterflysmp.rpg.core.combat;

/**
 * Whether a hit was a critical one, as a TYPE rather than as a boolean.
 *
 * <p>Distinct from {@link Crit}, which is the ARITHMETIC -- the chance, the multiplier, and the
 * already-drawn roll. This is the resulting FACT, carried to the displays. The multiplier is applied
 * to the amount long before this travels, so this never touches the maths: it decides a colour, a
 * particle and a popup, not a number.
 *
 * <h2>It is a type for {@link DefenseRule}'s reason, one seat over</h2>
 *
 * It sat immediately beside {@code bypassesDefense} in {@code CombatantHandle.applyDamage}, and
 * immediately beside BOTH {@code dealerIsPlayer} and {@code bypassesDefense} in
 * {@code CombatantStats.damage} -- <b>three adjacent booleans, six orderings, five of them wrong and
 * all six compiling.</b> Typing only the defense flag would have left this pair transposable with
 * each other, so the fix takes the pair rather than the single parameter that prompted it.
 *
 * <p>{@code dealerIsPlayer} deliberately stays a {@code boolean}: with these two lifted out it is the
 * ONLY boolean left in either signature, and a lone boolean has nothing to be transposed with. The
 * hazard was never "booleans are bad" -- it was ADJACENCY.
 *
 * <p>See {@link DefenseRule}'s javadoc for the full argument, including why named constants do not
 * solve this and a distinct type does.
 */
public enum CritState {

    /** An ordinary hit. */
    NORMAL,

    /**
     * A critical hit.
     *
     * <p>Carries a fact for the displays, never a factor for the arithmetic -- the crit multiplier
     * was already applied to the amount by {@code EffectApplier}, so multiplying again here would
     * double it.
     */
    CRIT;

    /**
     * Lift an already-drawn boolean into this type.
     *
     * <p>The one sanctioned boundary between the two representations, for call sites reading a
     * roll that is genuinely a boolean ({@code Caster.crit()}). A single named conversion is not the
     * hazard this type exists to remove: adjacency is, and there is nothing here to sit beside.
     */
    public static CritState of(boolean wasCrit) {
        return wasCrit ? CRIT : NORMAL;
    }

    /** True for {@link #CRIT}. For the seams that still carry the fact as a boolean. */
    public boolean isCrit() {
        return this == CRIT;
    }
}
