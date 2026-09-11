package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.weapon.Quiver;

/**
 * Reload time: whole ticks gear adds to a weapon's authored reload duration, and the one conversion
 * between the {@code double} a {@code Stat} sums and the {@code int} a tick count is measured in.
 *
 * <p>The shape of {@link QuiverSize}, deliberately, down to the {@code int} parameters -- half a tick
 * is as meaningless as half an arrow, and the server cannot schedule one. What differs is the floor,
 * and that difference is the whole content of this file's second half.
 *
 * <h2>THIS STAT HAS NO FLOOR, AND IT IS THE SAME STANDARD {@link QuiverSize} PASSES</h2>
 *
 * <p>The test is <b>is there a resolved value at which the mechanic has no reachable state</b> --
 * not <i>did the sibling stat get a 1</i>. Applied to both, measured rather than reasoned:
 *
 * <table><tr><th>resolved value</th><th>what the mechanic does</th><th>floor?</th></tr>
 * <tr><td>capacity 0</td>
 *     <td>{@code fireVerdict} EMPTY, {@code reloadVerdict} ALREADY_FULL. Both inputs refused, no
 *     third input, no recovery -- the item is dead.</td>
 *     <td><b>YES</b>, {@link QuiverSize#MIN_CAPACITY}, at the last value it works at</td></tr>
 * <tr><td>reload 0</td>
 *     <td>{@code Quiver.reloadCompletesAt} is {@code now + Math.max(reloadTicks, 0)}, so the deadline
 *     equals the start; {@code reloadComplete} returns true on that same tick. The reload stamps,
 *     matures and clears. Nothing is unreachable and nothing is lost.</td>
 *     <td><b>NO</b></td></tr>
 * </table>
 *
 * <p><b>So {@link #resolve} floors nothing, and that is a conclusion rather than an omission.</b> An
 * instant reload deletes the BRAKE this feature is about -- that is a real objection, and it is a
 * BALANCE objection, not a mechanism one. It stays where it already sits: the duration that still
 * brakes depends on the shortest achievable fire interval, which is 7, which is priced on <b>Q7 --
 * unrun</b>. The first slice that ships reload-reduction gear rules it, because that is the first
 * time anyone can feel it.
 *
 * <h3>The constant that was ruled, retracted twice, never written, and then cited twice as real</h3>
 *
 * <p>{@code PLAN-quiver-a2.md} ruled {@code Quiver.MIN_RELOAD_TICKS = 1} and retracted its argument
 * twice -- first a balance number wearing a MECHANISM argument (10, "invisible below the fire
 * cooldown", false on both premises), then a balance number wearing a STRUCTURAL one (1,
 * "{@code Durability.MIN_USES}'s argument", false because a zero-tick reload is fully
 * representable). <b>It was never written into the code</b>: {@code Quiver} has no fields at all and
 * {@code QuiverSignatureTest} pins exactly that.
 *
 * <p>It was nevertheless cited by two javadocs in commits 2 and 3 as the contrast case for
 * {@code MIN_CAPACITY} -- <b>a claim that a thing is in the tree, made about a thing that is not.</b>
 * That is the same shape as the two boot rows once promised to a {@code GATE-quiver-a2.md} that did
 * not exist. This is the third retraction and the last: the placeholder is deleted rather than
 * re-justified, because <i>a named hook at the no-op value</i> was the third costume on the same
 * balance number.
 *
 * <h2>WHAT DOES CLAMP, AND WHY IT IS NOT A SECOND FLOOR</h2>
 *
 * <p>{@code Quiver.reloadCompletesAt}'s {@code Math.max(reloadTicks, 0)} is not a balance floor. It
 * keeps a stamped deadline from landing BEFORE its own start -- a representability rule about the
 * pair of longs, the same kind as {@code QuiverState}'s both-or-neither check. Adding a clamp here
 * as well would be two enforcement sites for one quantity, which is the defect the capacity half of
 * this slice was reorganised to make unrepresentable, so {@link #resolve} deliberately does not.
 *
 * <p><b>There are already two layers on the AUTHORED side and they are the same number</b>:
 * {@code WeaponDefinition} refuses {@code quiverSize > 0 && reloadTicks <= 0}, so an authored reload
 * is at least 1. That is the authoring rule; this is the resolved value; they do not compete.
 */
public final class ReloadTime {

    private ReloadTime() {}

    /** No bonus. The {@code 0-is-absent} convention every stat helper here shares. */
    public static final int NONE = 0;

    /** Does this bonus change anything at all? Strictly {@code >}, so 0 declares nothing. */
    public static boolean boosts(int bonusTicks) {
        return bonusTicks > NONE;
    }

    /**
     * A piece's contribution to the reload duration, in whole ticks: the bonus itself.
     *
     * <p>Named rather than inlined for the reason {@code Growth.contribution} gives -- it is the ONE
     * place a reload-time bonus becomes a stat modifier, so a future rule (a cap, a floor once the
     * balance slice rules one, diminishing returns) has somewhere to live that is not a scan loop.
     */
    public static int contribution(int bonusTicks) {
        return bonusTicks;
    }

    /**
     * The resolved reload duration: a weapon's authored ticks plus whatever the wielder's gear adds.
     *
     * <p><b>No floor and no clamp</b> -- see the class javadoc. A negative resolved value is not
     * refused here; {@code Quiver.reloadCompletesAt} takes it to a deadline equal to its start, which
     * is an instant reload and a fully reachable state.
     *
     * <p><b>In {@code core} for the reason {@code QuiverSize.resolve} is:</b> its natural home looks
     * like the single site in {@code paper} that reads the stat, and a decision placed there is
     * permanently boot-only.
     *
     * <p><b>AND THE DIRECTION THIS ARITHMETIC WILL ACTUALLY BE USED IN IS THE ONE NO INSTRUMENT CAN
     * STAGE.</b> In play, reload gear REDUCES the number -- that is what a player wants from it --
     * and A2 ships no instrument that moves it down, because at base 34 every collision-free
     * reducing value lands on an authored number or on the bonus itself. So the downward direction
     * has exactly one witness in this whole slice, and it is
     * {@code ReloadTimeTest.theResolvedDurationGoesDownFreelyAndZeroIsAnInstantReload}. Do not delete
     * it as redundant with the upward rows; nothing else covers that half.
     *
     * @param authoredTicks  the weapon's own {@code reload_ticks}, already validated {@code > 0}
     * @param bonusStatValue {@code HealthState}'s summed modifiers, in ticks
     */
    public static int resolve(int authoredTicks, double bonusStatValue) {
        return authoredTicks + ticks(bonusStatValue);
    }

    /**
     * The one conversion from the {@code double} a {@code Stat} sums to whole ticks. FLOOR.
     *
     * <p>Exact for everything the content pipeline can produce -- {@link #contribution} takes an
     * {@code int}, and a sum of integral {@code double}s is exact far below {@code 2^53}. The rule
     * bites only on a value that reached here some other way. There it matches
     * {@link Quiver#applyPercent} and {@link QuiverSize#arrows} in MODE -- floor, one rounding rule
     * per weapon -- and <b>NOT in the phrase those two use to justify it.</b>
     *
     * <p><b>"Flooring rounds against the player" is FALSE for this stat, and writing it here would
     * have been the slice's sixth-shape failure again: a phrase carried across because it is the
     * house rule, not because it is true of the thing it is attached to.</b> A bigger capacity helps
     * the player; a bigger reload hurts. So the same {@code Math.floor} lands on opposite sides:
     *
     * <pre>{@code
     * QuiverSize.arrows(2.9)  = 2   round would say 3   -> the player gets LESS. Against them.
     * ReloadTime.ticks(-2.1)  = -3  round would say -2  -> the reload is SHORTER. For them.
     * }</pre>
     *
     * <p>What actually carries over is <b>one rounding mode per weapon</b>. Choosing the mode per
     * stat by who it favours is how two rounding rules end up inside one weapon, and neither is ever
     * re-derivable afterwards. Witnessed by
     * {@code ReloadTimeTest.flooringFavoursThePlayerHereAndTheHouseRuleIsTheMODENotThePhrase}.
     */
    public static int ticks(double bonusStatValue) {
        return (int) Math.floor(bonusStatValue);
    }
}
