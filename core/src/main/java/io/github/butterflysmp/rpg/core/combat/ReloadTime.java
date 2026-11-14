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
 * <h2>RULED 2026-11-06: THE RESOLVED RELOAD NEVER GOES BELOW {@link #MIN_TICKS} (1 TICK)</h2>
 *
 * <p><b>The seat's ruling, at Deft Hands (PLAN-deft-hands.md): "the resolved reload never goes below 1 tick (no
 * instant reload), whatever stacks."</b> This is the BALANCE ruling the section below deferred to "the first slice
 * that ships reload-reduction gear" -- Deft Hands is that slice. It is a balance floor, NOT a mechanism floor, and
 * the mechanism argument below still stands as written: a 0-tick reload IS reachable and representable. The ruling
 * removes it because an instant reload deletes the brake, not because the code cannot express one. Shipped content
 * alone never reaches it (Deft Hands III on the shortest authored reload, 34, resolves to 23); only stacking the
 * dev reload instrument does, and {@code ReloadTimeTest} drives exactly that.
 *
 * <h2>THE MECHANISM ARGUMENT, AS IT STOOD BEFORE THE RULING (KEPT: IT IS STILL TRUE)</h2>
 *
 * <h3>THIS STAT HAS NO MECHANISM FLOOR, AND IT IS THE SAME STANDARD {@link QuiverSize} PASSES</h3>
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
 * <p><b>So {@link #resolve} floored nothing -- a conclusion, not an omission, UNTIL 2026-11-06.</b> An
 * instant reload deletes the BRAKE this feature is about -- that is a real objection, and it is a
 * BALANCE objection, not a mechanism one. It stays where it already sits: the duration that still
 * brakes depends on the shortest achievable fire interval, which is 7, which is priced on <b>Q7 --
 * unrun</b>. The first slice that ships reload-reduction gear rules it (Deft Hands did: {@link #MIN_TICKS}), because that is the first
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
 * pair of longs, the same kind as {@code QuiverState}'s both-or-neither check. The ruled floor in
 * {@link #resolve} is NOT a second site for THAT rule: it is a different rule (balance, on the resolved
 * duration), and reloadCompletesAt's clamp still guards only the pair of longs. One site per rule.
 *
 * <p><b>There are already two layers on the AUTHORED side and they are the same number</b>:
 * {@code WeaponDefinition} refuses {@code quiverSize > 0 && reloadTicks <= 0}, so an authored reload
 * is at least 1. That is the authoring rule; this is the resolved value; they do not compete.
 */
public final class ReloadTime {

    private ReloadTime() {}

    /** No bonus. The {@code 0-is-absent} convention every stat helper here shares. */
    public static final int NONE = 0;

    /**
     * The shortest a resolved reload may be: ONE TICK, never an instant reload (the seat, 2026-11-06, at Deft Hands).
     * A balance floor on the resolved value only -- the authored side keeps {@code WeaponDefinition}'s own {@code >= 1}
     * rule, and {@code Quiver.reloadCompletesAt}'s {@code Math.max(…, 0)} stays the representability rule it was.
     */
    public static final int MIN_TICKS = 1;

    /**
     * Does this amount declare a modifier at all? <b>{@code != NONE}, and it is NOT called
     * {@code boosts}.</b>
     *
     * <p>Every other stat helper in this package gates on {@code > NONE} and is called
     * {@code boosts}. <b>Copying that here was a defect, caught in review before the scanner that
     * would have shipped it.</b> This stat's sign is INVERTED -- positive means slower -- so
     * {@code > NONE} does not mean "increase-only" the way it does for {@link QuiverSize}. It means:
     *
     * <pre>{@code
     * QuiverSize:  > NONE  ->  content cannot make a quiver SMALLER.   A coherent content ruling.
     * ReloadTime:  > NONE  ->  content can only make a reload SLOWER.  NO RELOAD-SPEED GEAR AT ALL.
     * }</pre>
     *
     * <p>A reload item that helps the player carries a NEGATIVE amount, so {@code boosts(-5)} would
     * have been {@code false} and <b>the item would have declared nothing and done nothing, silently,
     * on every scan.</b> Meanwhile {@link #resolve}'s own javadoc calls the downward direction the
     * real one, and {@code ReloadTimeTest} has the only row in the slice that covers it -- the
     * arithmetic was built and witnessed for a direction the gate forbade.
     *
     * <p><b>The javadoc also asked a different question than the body answered.</b> It read <i>"does
     * this bonus change anything at all?"</i> -- which is {@code != NONE} -- above a body that said
     * {@code > NONE}. For {@link QuiverSize} the two coincide in effect; for an inverted stat they
     * diverge on its primary direction. The question was right and the body was the copy.
     *
     * <p><b>RULED by the operator: the Ranger has reload-speed gear, in both directions.</b> The
     * consequence accepted with it: a large enough reduction once resolved to 0 --
     * an instant reload. Ruled out 2026-11-06: {@link #resolve} now stops at {@link #MIN_TICKS}.
     *
     * <p><b>{@link QuiverSize#boosts} keeps {@code > NONE} AND its name, deliberately</b>, and this
     * paragraph exists so nobody unifies them for consistency. Two sibling helpers with different
     * gates look like an oversight and are not: there, increase-only is a real content ruling and
     * "boosts" is the true verb; here neither is.
     */
    public static boolean declares(int bonusTicks) {
        return bonusTicks != NONE;
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
     * The resolved reload duration: a weapon's authored ticks plus whatever the wielder's gear adds, and NEVER BELOW
     * {@link #MIN_TICKS}.
     *
     * <p><b>One floor, here, and only here</b> (the seat's ruling, 2026-11-06; see the class javadoc). Before it this
     * method had none, and a resolved 0 or below was an instant reload -- reachable, and now ruled out on balance.
     * The clamp sits on the RESOLVED value, so any number of stacked sources can at most reach one tick.
     *
     * <p><b>In {@code core} for the reason {@code QuiverSize.resolve} is:</b> its natural home looks
     * like the single site in {@code paper} that reads the stat, and a decision placed there is
     * permanently boot-only.
     *
     * <p><b>AND THE DIRECTION THIS ARITHMETIC WILL ACTUALLY BE USED IN IS THE ONE NO <i>DEFAULT</i>
     * CAN STAGE.</b> In play, reload gear REDUCES the number -- that is what a player wants from it,
     * and {@link #declares} permits it. What A2 cannot supply is a reducing DEFAULT: at base 34
     * every collision-free reducing value lands on an authored number or on the bonus itself, so the
     * instrument's default adds (+14 -> 48) and {@code /rpg reloadtime <negative>} is how a
     * human-driven boot row drives the other way, reading the collision caveat at that command.
     *
     * <p>So the downward direction has exactly one UNATTENDED witness in this slice, and it is
     * {@code ReloadTimeTest.theResolvedDurationGoesDownToTheOneTickFloorAndNoFurther}. Do not delete
     * it as redundant with the upward rows; nothing else covers that half without a person driving it.
     *
     * @param authoredTicks  the weapon's own {@code reload_ticks}, already validated {@code > 0}
     * @param bonusStatValue {@code HealthState}'s summed modifiers, in ticks
     */
    public static int resolve(int authoredTicks, double bonusStatValue) {
        return Math.max(MIN_TICKS, authoredTicks + ticks(bonusStatValue));
    }

    /**
     * DEFT HANDS: a percent reduction of a weapon's AUTHORED reload, as whole ticks to ADD (so, negative).
     *
     * <p><b>Ben's ruling (a), 2026-11-06: reload TIME x0.9 / 0.8 / 0.7</b> for 10 / 20 / 30. So the contribution is
     * {@code -(authored x percent / 100)}, FLOORED -- {@link #ticks}'s mode, one rounding rule per weapon, and here
     * floor takes the larger cut (34 x 10% = 3.4 takes 4 ticks off), the side this stat's floor already lands on.
     *
     * <p><b>INTEGER ARITHMETIC, deliberately</b> ({@code Math.floorDiv}), not {@code percent / 100.0}: in a double
     * {@code 34 * 0.1} is {@code 3.4000000000000004}, and a product that lands on an integer would floor one tick too
     * far through it. Converted ONCE, against the AUTHORED value, so it does not compound with any other source --
     * every source is then a flat sum through {@link #resolve}. Every tier on 60, 40 and 34 is pinned in
     * {@code ReloadTimeTest}, executed, not characterised.
     *
     * @param authoredTicks the held weapon's {@code reload_ticks}, {@code > 0} for any weapon that has a reload
     * @param percent       the enchant's resolved {@code value_by_level}, 10 / 20 / 30
     */
    public static int percentCut(int authoredTicks, int percent) {
        return Math.floorDiv(-authoredTicks * percent, 100);
    }

    /**
     * The {@code /rpg mobtrace} line for a reload that just began (GATE-deft-hands.md DH2): {@code RELOAD
     * player=<uuid> weapon=<id> authored=<n> resolved=<n> start=<tick> completesAt=<tick>}. Formatted here, in core,
     * so its shape is a unit row.
     */
    public static String line(java.util.UUID playerId, String weaponId, int authoredTicks, int resolvedTicks,
                              long start, long completesAt) {
        return "RELOAD player=" + playerId + " weapon=" + weaponId + " authored=" + authoredTicks
                + " resolved=" + resolvedTicks + " start=" + start + " completesAt=" + completesAt;
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
