package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What changed between the decision on screen and the decision at the click.
 *
 * <h2>EVERY ROW ASSERTS THE NAMED ARM, NEVER MERELY "SOMETHING DIFFERED"</h2>
 *
 * A row that accepts any change cannot fail for its stated reason -- and here the arms carry
 * different sentences to the player, so collapsing them would tell someone whose donor was swapped
 * that the price moved.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilReconcileTest {

    private static final int RARE = 910;
    private static final int EPIC = 2920;

    private static AnvilDecision melee(int targetScore, int donorScore, Rarity rarity, int wallet) {
        return AnvilDecision.of(
                new Side.Scored(TransferKey.MELEE, targetScore, rarity),
                new Side.Scored(TransferKey.MELEE, donorScore, rarity),
                wallet);
    }

    @Test
    void anUNCHANGEDDecisionIsSAMEAndSaysNOTHING() {
        AnvilDecision shown = melee(140, 310, Rarity.RARE, 5000);
        AnvilReconcile same = AnvilReconcile.of(shown, melee(140, 310, Rarity.RARE, 5000));

        assertInstanceOf(AnvilReconcile.Same.class, same);
        assertEquals(Optional.empty(), same.sentence(),
                "SAY NOTHING, not say an empty string -- a blank line in chat is a message the "
                        + "player has to interpret");
    }

    /**
     * *** THE ROW BEN SAID TO WRITE FIRST: THE ONLY CASE WHERE THE WRONG ANSWER IS SILENT,
     * PLAUSIBLE AND PERMANENT. ***
     *
     * <p>Two donors with <b>DIFFERENT scores</b> and the <b>SAME cost</b> -- same target, so the
     * same rarity, so the same price. <b>A confirm button that printed only the price would render
     * these two decisions identically</b>, and a comparison over that text would call them
     * unchanged and apply the wrong score to the player's item.
     *
     * <p>It is silent (nothing looks wrong), plausible (the number is a real score) and permanent
     * (there is no undo and no output slot to take anything back from).
     */
    @Test
    void aDONORSwappedForOneOfADIFFERENTScoreAndTheSAMECostIsSCORECHANGED() {
        AnvilDecision shown = melee(140, 310, Rarity.RARE, 5000);
        AnvilDecision now = melee(140, 340, Rarity.RARE, 5000);

        assertEquals(RARE, ((AnvilVerdict.Ready) shown.verdict()).xpPoints());
        assertEquals(RARE, ((AnvilVerdict.Ready) now.verdict()).xpPoints());
        // THE PRICES ARE EQUAL. That is the fixture, not an accident of it: the two decisions are
        // indistinguishable to anything that reads only the cost.

        AnvilReconcile.ScoreChanged changed = assertInstanceOf(AnvilReconcile.ScoreChanged.class,
                AnvilReconcile.of(shown, now),
                "THE SCORE MOVED AND THE PRICE DID NOT. A comparison that reads the rendered price "
                        + "-- or any field that cannot tell 310 from 340 -- calls this unchanged "
                        + "and stamps the wrong number onto an item that cannot be un-stamped.");
        assertEquals(310, changed.was());
        assertEquals(340, changed.now());
        assertEquals(Optional.of("The result changed from 310 to 340. Check it and try again."),
                changed.sentence());
        // Mutation MUT13B-TEXTCOMPARE: reconcile on the cost alone, as a price-only button text
        // would -> reddens HERE. This row is the SOLE guard of that axis.
    }

    /**
     * The mirror: same score, different price. Swapping the TARGET for a same-score item of another
     * rarity moves the cost and nothing else.
     */
    @Test
    void aTARGETSwappedForOneOfTheSAMEScoreAndADIFFERENTRarityIsCOSTCHANGED() {
        AnvilDecision shown = melee(140, 310, Rarity.RARE, 5000);
        AnvilDecision now = melee(140, 310, Rarity.EPIC, 5000);

        AnvilReconcile.CostChanged changed = assertInstanceOf(AnvilReconcile.CostChanged.class,
                AnvilReconcile.of(shown, now));
        assertEquals(RARE, changed.was());
        assertEquals(EPIC, changed.now());
        assertEquals(Optional.of("The price changed from 910 to 2920 XP. Check it and try again."),
                changed.sentence());
    }

    /**
     * *** THE WALLET. THE ONE INPUT TO THE DECISION THAT IS NOT AN INPUT SLOT. ***
     *
     * <p>XP can move while the menu is open -- a kill, a command, another plugin -- and none of that
     * touches a slot or re-arms anything. <b>Re-evaluation is its only guard</b>, which is why this
     * row exists and why the splice that caches the decision must redden it.
     */
    @Test
    void theWALLETDroppingBelowTheCostIsAFFORDABILITYCHANGED_andReusesTheExistingSentence() {
        AnvilDecision shown = melee(140, 310, Rarity.RARE, 5000);
        AnvilDecision now = melee(140, 310, Rarity.RARE, 400);

        AnvilReconcile.AffordabilityChanged changed = assertInstanceOf(
                AnvilReconcile.AffordabilityChanged.class, AnvilReconcile.of(shown, now));
        assertInstanceOf(Affordability.Affordable.class, changed.was());
        assertInstanceOf(Affordability.CannotAfford.class, changed.now());
        assertEquals(Optional.of("This transfer costs 910 XP; you have 400."), changed.sentence(),
                "EnchantMenu's register, not a new sentence for one fact");
        // Mutation MUT13B-CACHED: use the displayed decision instead of recomputing -> the confirm
        // handler never sees this, and this row is what the paper-side splice is measured against.
    }

    @Test
    void aDIFFERENTVERDICTArmIsPAIRCHANGED_becauseThereIsNoScoreOrPriceToCompareAcross() {
        AnvilDecision shown = melee(140, 310, Rarity.RARE, 5000);
        AnvilDecision now = AnvilDecision.of(Side.EMPTY, Side.EMPTY, 5000);

        AnvilReconcile.PairChanged changed = assertInstanceOf(AnvilReconcile.PairChanged.class,
                AnvilReconcile.of(shown, now));
        assertInstanceOf(AnvilVerdict.Ready.class, changed.was());
        assertInstanceOf(AnvilVerdict.SlotEmpty.class, changed.now());
        assertEquals(Optional.of("These items changed. Check the result and try again."),
                changed.sentence());

        // AND THE OTHER WAY ROUND, so the arm is not accidentally about losing a Ready verdict.
        assertInstanceOf(AnvilReconcile.PairChanged.class, AnvilReconcile.of(now, shown));
    }

    /**
     * *** PRECEDENCE, AND THE ORDER IS THE RULING. ***
     *
     * <p>When several things moved at once, the player is told the most fundamental one. Someone
     * whose donor was swapped needs to hear that, not that the price moved as a consequence.
     */
    @Test
    void whenSEVERALThingsMoveTheMOSTFundamentalOneIsReported() {
        // Score AND cost AND affordability all differ. The arm must be SCORE.
        AnvilDecision shown = melee(140, 310, Rarity.RARE, 5000);
        AnvilDecision now = melee(140, 340, Rarity.EXOTIC, 10);
        assertInstanceOf(AnvilReconcile.ScoreChanged.class, AnvilReconcile.of(shown, now),
                "the result the player receives outranks what they pay for it");

        // Cost AND affordability differ, score does not. The arm must be COST.
        assertInstanceOf(AnvilReconcile.CostChanged.class,
                AnvilReconcile.of(shown, melee(140, 310, Rarity.EXOTIC, 10)),
                "and what they pay outranks whether they can");
    }

    /**
     * *** EVERY CHANGED ARM SAYS SOMETHING, AND NO TWO SAY THE SAME THING. ***
     *
     * <p>{@code QuiverState}'s rule: <i>"a refusal that reuses another refusal's message is a bug
     * report waiting to be filed."</i>
     */
    @Test
    void allFOURChangedArmsRenderADISTINCTSentence_andSAMERendersNone() {
        AnvilReconcile[] all = {
                new AnvilReconcile.Same(),
                new AnvilReconcile.PairChanged(new AnvilVerdict.SlotEmpty(),
                        new AnvilVerdict.Ready(310, 910)),
                new AnvilReconcile.ScoreChanged(310, 340),
                new AnvilReconcile.CostChanged(910, 2920),
                new AnvilReconcile.AffordabilityChanged(Affordability.AFFORDABLE,
                        new Affordability.CannotAfford(910, 400)),
        };

        Set<String> sentences = new HashSet<>();
        int checked = 0;
        int silent = 0;
        for (AnvilReconcile arm : all) {
            Optional<String> sentence = arm.sentence();
            if (sentence.isEmpty()) {
                silent++;
            } else {
                assertTrue(sentence.get().endsWith("."), arm + " reads as a sentence");
                sentences.add(sentence.get());
            }
            checked++;
        }
        assertEquals(5, checked, "the sweep has to have actually run");
        assertEquals(1, silent, "exactly one arm is silent, and it is Same");
        assertEquals(4, sentences.size(),
                "four changed arms, four sentences: " + sentences);
    }

    @Test
    void aNULLSideOfTheComparisonIsREFUSEDLoudly_becauseItWouldSilentlyAlwaysAgree() {
        AnvilDecision any = melee(140, 310, Rarity.RARE, 5000);
        assertThrows(IllegalArgumentException.class, () -> AnvilReconcile.of(null, any));
        assertThrows(IllegalArgumentException.class, () -> AnvilReconcile.of(any, null));
    }
}
