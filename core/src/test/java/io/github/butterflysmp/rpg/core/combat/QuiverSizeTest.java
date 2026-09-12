package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quiver size: whole arrows added to a weapon's authored magazine.
 *
 * <p>The numbers here are {@code quiver_stone}'s shipped {@code quiver_size: 9} and the A2 fixture's
 * {@code +19}, which resolves to <b>28</b>. Both are swept free of every authored value in
 * {@code content/} -- see {@code PLAN-quiver-a2.md} -- <b>and the swept quantity is the RESOLVED
 * one</b>, because a gate row reads what is on screen, not what the modifier says.
 *
 * <p>Each row names what it forces red. <b>Three of those predictions were RUN, not left as
 * predictions</b> -- each spliced by line number, marker grepped both directions, byte delta
 * recomputed from the edit rather than quoted:
 *
 * <table><tr><th>mutation</th><th>edit</th><th>bytes</th><th>result</th></tr>
 * <tr><td>{@code MUTQSBOOST}</td><td>{@code boosts}: {@code >} -> {@code >=}</td>
 *     <td>+15 = 1 for {@code =} + 14 for the marker</td>
 *     <td><b>1 of 6 red</b>: {@link #zeroDeclaresNothingAndSoDoesANegative}</td></tr>
 * <tr><td>{@code MUTQSFLOOR}</td><td>{@code arrows}: {@code Math.floor} -> {@code Math.round}</td>
 *     <td>+14 = 0 for the same-length name + 14</td>
 *     <td><b>1 of 6 red</b>: {@link #flooringRoundsAgainstThePlayerForBuffsAndDebuffsAlike}, on
 *     {@code 2.9}</td></tr>
 * <tr><td>{@code MUTQSALIAS}</td><td>{@code HealthState.quiverSizeBonusValue} reads
 *     {@code manaRegenBonus}</td><td>-1 + 14 = +13</td>
 *     <td><b>3 red across two files</b>, including the entanglement row, which read {@code 1.0} --
 *     the mana value -- where it expected {@code 19.0}</td></tr>
 * </table>
 *
 * <p>All three restored from scratchpad copies; {@code HealthState} verified byte-identical with
 * {@code cmp}; {@code markers left: 0} in both files.
 */
class QuiverSizeTest {

    /** {@code quiver_stone.yml}'s authored magazine. */
    private static final int AUTHORED = 9;

    /** The A2 fixture's bonus. Resolves to 28; both numbers are free of everything shipped. */
    private static final int FIXTURE_BONUS = 19;

    /**
     * Zero declares nothing, and so does a NEGATIVE. <b>That makes the CONTENT PIPELINE
     * increase-only; it does NOT make the resolved capacity safe</b>, which is what this row's
     * first version claimed.
     *
     * <p>{@link QuiverSize#resolve} never consults this filter -- it is applied at the scanner
     * ({@code QuiverSizeModifierItems.desiredModifiers}), and the arithmetic is public core API that
     * anything may call with anything. What covers the API is {@link QuiverSize#MIN_CAPACITY}; see
     * {@link #theResolvedCapacityNeverFallsBelowOneHoweverFarTheBonusGoesDown}.
     *
     * <p>Both are kept because they answer different questions: this one decides whether an item
     * DECLARES a modifier (a 0-valued fixture must write no source rather than a no-op one every
     * scan), the floor decides what a resolved capacity may BE.
     *
     * <p>Forces red: {@code boosts} relaxed to {@code >=}, or to {@code != NONE}.
     */
    @Test
    void zeroDeclaresNothingAndSoDoesANegative() {
        assertFalse(QuiverSize.boosts(QuiverSize.NONE), "0 arrows is not a bonus");
        assertFalse(QuiverSize.boosts(-1), "a reducing amount must not become a modifier");
        assertFalse(QuiverSize.boosts(-FIXTURE_BONUS));
        assertTrue(QuiverSize.boosts(1), "one arrow is the smallest bonus that means anything");
        assertTrue(QuiverSize.boosts(FIXTURE_BONUS));
    }

    /** The contribution is the bonus. Forces red: any scaling or cap sneaking in un-tested. */
    @Test
    void theContributionIsTheBonusItself() {
        assertEquals(FIXTURE_BONUS, QuiverSize.contribution(FIXTURE_BONUS));
        assertEquals(1, QuiverSize.contribution(1));
        assertEquals(QuiverSize.NONE, QuiverSize.contribution(QuiverSize.NONE));
    }

    /**
     * WITH NO GEAR THE RESOLVED CAPACITY IS EXACTLY THE AUTHORED ONE -- the neutral that keeps every
     * existing weapon, tooltip, golden line and browser icon reading what it read before this stat
     * existed.
     *
     * <p>Forces red: a base other than 0, or an off-by-one in {@code resolve}.
     */
    @Test
    void withNoGearTheResolvedCapacityIsExactlyTheAuthoredOne() {
        assertEquals(AUTHORED, QuiverSize.resolve(AUTHORED, 0.0),
                "a player wearing nothing carries exactly the magazine the weapon declares");
        assertEquals(1, QuiverSize.resolve(1, 0.0));
    }

    /**
     * The fixture's own arithmetic, end to end: 9 + 19 = 28.
     *
     * <p><b>The three quantities are pairwise different on purpose.</b> 9, 19 and 28 share no value,
     * so a row that passed by reading the base, the bonus or the sum interchangeably cannot pass
     * here -- the "two independent quantities are equal" defect this repository records.
     *
     * <p>Forces red: {@code resolve} returning the base, the bonus, or a product.
     */
    @Test
    void theFixtureResolvesToTwentyEight() {
        assertEquals(28, QuiverSize.resolve(AUTHORED, FIXTURE_BONUS),
                "quiver_stone's 9 plus the fixture's +19");
        assertEquals(11, QuiverSize.resolve(AUTHORED, 2.0), "the transfer-case tooltip's 8/11");
    }

    /**
     * FLOORING ROUNDS AGAINST THE PLAYER, FOR BUFFS AND DEBUFFS ALIKE.
     *
     * <p><b>The buff values are ones where floor and round DISAGREE</b>, which is the only kind that
     * discriminates. {@code 2.9} floors to 2 and rounds to 3; {@code 2.5} floors to 2 and rounds to
     * 3. A row staged only on {@code 2.1} would stay green under a rounding mutation, which is
     * exactly how the A1 version of this rule survived {@code MUTROUND}.
     *
     * <p>The debuff side is unreachable today ({@link #zeroDeclaresNothingAndSoDoesANegative}) and is
     * staged anyway, because the rule is what makes a future reducing modifier safe to add.
     *
     * <p>Forces red: {@code Math.round}, {@code Math.ceil}, or a bare {@code (int)} cast -- the cast
     * truncates toward zero, so it agrees with floor on {@code +2.9} and disagrees on {@code -2.1}.
     */
    @Test
    void flooringRoundsAgainstThePlayerForBuffsAndDebuffsAlike() {
        assertEquals(2, QuiverSize.arrows(2.9), "floor 2.9 = 2; round would say 3");
        assertEquals(2, QuiverSize.arrows(2.5), "floor 2.5 = 2; round would say 3");
        assertEquals(-3, QuiverSize.arrows(-2.1), "floor -2.1 = -3; a (int) cast would say -2");
        assertEquals(-3, QuiverSize.arrows(-2.9));
        assertEquals(2, QuiverSize.arrows(2.0), "an exact value is unchanged");
    }

    /**
     * A SUM OF WHOLE-ARROW MODIFIERS ROUND-TRIPS EXACTLY, which is why the {@code double} storage is
     * not a hazard.
     *
     * <p>{@code QuiverSize.contribution} takes an {@code int}, so everything reaching {@code Stat} is
     * integral; integral doubles are exact far below {@code 2^53}. Three sources summed the way the
     * reconciler sums them, then converted back.
     *
     * <p>Forces red: any conversion that is not exact on integral input.
     */
    @Test
    void aSumOfWholeArrowModifiersRoundTripsExactly() {
        double summed = QuiverSize.contribution(FIXTURE_BONUS)
                + QuiverSize.contribution(3)
                + QuiverSize.contribution(7);
        assertEquals(29.0, summed, 0.0, "19 + 3 + 7, summed as doubles, is exact");
        assertEquals(29, QuiverSize.arrows(summed));
        assertEquals(AUTHORED + 29, QuiverSize.resolve(AUTHORED, summed));
    }

    /**
     * THE RESOLVED CAPACITY NEVER FALLS BELOW ONE, HOWEVER FAR THE BONUS GOES DOWN -- and this row
     * exists because the previous commit argued it could not happen and was wrong.
     *
     * <p>That argument was <i>"every stat helper is increase-only, so there is no way down."</i>
     * {@link QuiverSize#boosts} is what makes that true of the SCANNER, and {@code resolve} does not
     * consult it: {@code resolve(9, -5.0)} returned <b>4</b> at the commit that wrote the sentence.
     * The API was the way down all along.
     *
     * <p><b>The floor is not a clamp at the authored base, and the row asserts the difference.</b>
     * Refusing to go below 9 would silently discard the reduction; {@code MIN_CAPACITY} honours it as
     * far as it can legally go and stops at the last value the item still works at. {@code -20} on a
     * base of 9 is {@code 1}, not {@code 9} and not {@code -11} -- three outcomes, three different
     * numbers, so no two of them can be confused.
     *
     * <p>Why 1 and not 0: {@code QuiverStateTest.aCapacityOfZeroWouldBeARefusalOnBothInputs} measures
     * that at capacity 0 a quiver is refused on fire AND on reload, with no third input and no
     * recovery. A mechanism, not a balance number.
     *
     * <p>Forces red: dropping the {@code Math.max}; flooring at 0; clamping at
     * {@code authoredCapacity} instead.
     */
    @Test
    void theResolvedCapacityNeverFallsBelowOneHoweverFarTheBonusGoesDown() {
        assertEquals(QuiverSize.MIN_CAPACITY, QuiverSize.resolve(AUTHORED, -20.0),
                "a reduction is honoured as far as it can go and stops at the last working value -- "
                        + "NOT clamped back to the authored 9, which would discard it silently");
        assertEquals(1, QuiverSize.resolve(AUTHORED, -1000.0), "and no further, however large");
        assertEquals(1, QuiverSize.resolve(1, -1.0), "an authored 1 reduced by 1 stays at 1, not 0");

        assertEquals(4, QuiverSize.resolve(AUTHORED, -5.0),
                "ABOVE the floor the reduction passes through untouched -- the floor must not become "
                        + "a clamp that swallows every reduction. 9 - 5 = 4, which is what this "
                        + "expression returned BEFORE the floor existed, so the floor changed only "
                        + "the cases it was written for.");
    }

    /**
     * AND THE SIBLING HELPER AGREES ABOUT WHETHER 0 IS LEGAL, WHICH IT DID NOT BEFORE.
     *
     * <p>{@code Quiver.applyPercent(8, -100)} is {@code 0} and stays {@code 0} -- it answers "what
     * does this percentage evaluate to", which is arithmetic with no opinion about legality. This
     * class answers "what capacity governs". The two disagreed only because {@code QuiverTest}'s row
     * called {@code 0} a total debuff that "stops there", which reads as approval of an outcome
     * {@code QuiverSize} calls a permanently dead item.
     *
     * <p>The resolution is one enforcement site, not two opinions: <b>a percentage that ever reaches
     * a capacity passes through {@link QuiverSize#resolve}</b>, and this row is that composition.
     *
     * <p>Forces red: a floor added to {@code applyPercent} (the first assertion), or removed from
     * {@code resolve} (the second).
     */
    @Test
    void aTotalPercentageDebuffEvaluatesToZeroAndSTILLRESOLVESToAWorkingCapacity() {
        int arithmetic = Quiver.applyPercent(8, -100);
        assertEquals(0, arithmetic, "the arithmetic answer is unchanged and has no opinion");

        assertEquals(QuiverSize.MIN_CAPACITY, QuiverSize.resolve(arithmetic, 0.0),
                "but routed through resolve it is a working capacity, because resolve is the one "
                        + "place a number becomes a capacity");
    }
}
