package io.github.butterflysmp.rpg.core.weapon;

/**
 * The roll band: a drop rolls uniformly over {@code average - 5} to {@code average + 15}.
 *
 * <h2>*** RULED BY BEN. THIS WAS OWED AND IS NOW DISCHARGED. ***</h2>
 *
 * <p>Ben ruled the band <b>{@code average - 5} to {@code average + 15}</b>. Expressed in this class's
 * own parameterisation -- {@code [average + SKEW - SPREAD, average + SKEW + SPREAD]} -- that is
 * {@link #SPREAD} 10 and {@link #SKEW} 5, and both endpoints land exactly:
 *
 * <pre>
 *   low   = skew - spread =  5 - 10 =  -5
 *   high  = skew + spread =  5 + 10 = +15
 *   width = 2 * spread + 1 = 21 integer values, inclusive at both ends
 * </pre>
 *
 * <h2>THE BAND SKEWS UPWARD, SO IT CLIMBS RATHER THAN CONVERGING</h2>
 *
 * A band centred exactly on the average converges: the expected roll equals what you already have,
 * and progression comes only from keeping the better roll and discarding the worse. <b>A positive
 * {@link #SKEW} is what makes the ladder climb on its own</b> -- the expected drop is 5 points above
 * the wearer's current average -- and the question of whether it should was put to Ben rather than
 * answered by whoever picked the spread. It skews.
 *
 * <h2>*** HOW LONG THE CLIMB TAKES -- SIMULATED, NOT DERIVED ***</h2>
 *
 * <b>MEDIAN 182 SCORED DROPS</b> from the floor to the {@link GearScore#SOFT_CAP}, over <b>400
 * simulated players</b>. Min <b>150</b>, max <b>216</b>.
 *
 * <p><b>Instrument: a simulation of this exact band, 400 players, run by the operator.</b> Named
 * because the figure cannot be reproduced by reading this file -- the walk is a random process over a
 * six-slot average that feeds its own next draw, and there is no closed form here to check it
 * against.
 *
 * <p><b>SO A LATER TUNING PASS RE-RUNS IT RATHER THAN REASONING ABOUT IT.</b> Change either constant
 * below and this figure is stale in a way no test will catch: the suite asserts the band's
 * ARITHMETIC, and nothing in it measures how many drops the climb takes. Adjusting 182 by hand to
 * match a new spread would be a figure maintained by delta, in the file that names the band.
 *
 * <p>These numbers are PERISHABLE and their invalidator is an event, not a date: <b>any change to
 * {@link #SPREAD} or {@link #SKEW}, and any change to {@link GearScore#averageOf}'s denominator or
 * floor.</b> All three are inputs to the walk.
 *
 * <h2>WHAT THE ZERO USED TO BE FOR, KEPT BECAUSE THE ARGUMENT WAS DISCHARGED RATHER THAN WRONG</h2>
 *
 * <p>Both constants shipped as <b>0</b> in the slice that introduced them, under the name
 * {@code SPREAD_OWED} / {@code SKEW_OWED}, because a plausible-looking spread <b>would have been
 * indistinguishable from a ruling</b> -- and the next author would have derived from it rather than
 * re-deciding it. That is CLAUDE.md's descent rule, whose instance in this project is the Boltor's
 * {@code attack_damage}: {@code 19} was ruled outright precisely so a dev weapon's arbitrariness
 * would not outlive the dev weapon.
 *
 * <p><b>The condition that argument named was "until Ben rules", and he has.</b> So the zero is
 * discharged, not deleted -- recorded here so the next person to meet an owed number in this codebase
 * can see what the placeholder was for and that it was retired by a ruling rather than by someone
 * getting tired of it. <b>The mechanism it bought is still load-bearing:</b> both numbers are
 * PARAMETERS to {@code GearScore.roll}, never read from here by the arithmetic, so the band is
 * reddened across widths this file does not ship and Ben's next revision stays a one-line change.
 */
public final class GearScoreBand {

    private GearScoreBand() {}

    /**
     * Half-width of the band, in score points. A roll is uniform over
     * {@code [average + SKEW - SPREAD, average + SKEW + SPREAD]} -- 21 integer values.
     *
     * <p><b>RULED: 10.</b> With {@link #SKEW} 5 this is Ben's {@code -5 / +15}. Moving it invalidates
     * the simulated 182-drop climb in the class javadoc, which must then be re-run rather than
     * adjusted.
     */
    public static final int SPREAD = 10;

    /**
     * How far the band's centre sits ABOVE the player's average.
     *
     * <p><b>RULED: 5.</b> Strictly positive, and that is the design half rather than the tuning half:
     * a zero here would make the band converge on what the player already has, and progression would
     * come only from selection. <b>The expected drop is 5 points above the wearer's current
     * average</b>, which is the climb.
     */
    public static final int SKEW = 5;
}
