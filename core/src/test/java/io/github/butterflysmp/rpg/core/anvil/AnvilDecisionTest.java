package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one producer, and the affordability it layers on.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilDecisionTest {

    private static Side scored(int score, Rarity rarity) {
        return new Side.Scored(TransferKey.MELEE, score, rarity);
    }

    /** RARE is 25 levels, which is 910 points. Pinned in {@code AnvilCostTest}. */
    private static final int RARE_COST = 910;

    @Test
    void theEXACTCostIsAFFORDABLE_becauseSpendingYourLastPointIsStillSpendingIt() {
        // THE BOUNDARY, BOTH SIDES. An off-by-one here refuses the purchase a player has saved
        // exactly enough for, which is the one refusal they would be certain was a bug.
        assertInstanceOf(Affordability.Affordable.class,
                Affordability.of(RARE_COST, RARE_COST), "exact cost is affordable");
        assertInstanceOf(Affordability.CannotAfford.class,
                Affordability.of(RARE_COST, RARE_COST - 1), "one point short is not");
        assertInstanceOf(Affordability.Affordable.class,
                Affordability.of(RARE_COST, RARE_COST + 1), "and one over plainly is");
        // Mutation MUT13B-AFFORD: `walletPoints >= xpPoints` -> `true` -> reddens on the
        // one-point-short row.
    }

    @Test
    void theSHORTFALLCarriesBOTHNumbers_soTheSentenceCanSayHowFar() {
        Affordability.CannotAfford shortfall = assertInstanceOf(Affordability.CannotAfford.class,
                Affordability.of(910, 400));
        assertEquals(910, shortfall.xpPoints());
        assertEquals(400, shortfall.wallet());
        // THE REGISTER IS EnchantMenu'S, REUSED. That screen already says "<name> costs <cost> XP;
        // you have <wallet>." for exactly this fact, and a second phrasing for one fact is two
        // accounts of one rule.
        assertEquals("This transfer costs 910 XP; you have 400.", shortfall.sentence());
    }

    @Test
    void aDecisionIsBOTHHalves_andACTIONABLENeedsBothToSayYes() {
        AnvilDecision ready = AnvilDecision.of(scored(140, Rarity.RARE), scored(310, Rarity.RARE),
                RARE_COST);
        assertTrue(ready.actionable(), "a legal transfer the player can pay for");

        AnvilDecision broke = AnvilDecision.of(scored(140, Rarity.RARE), scored(310, Rarity.RARE),
                RARE_COST - 1);
        assertFalse(broke.actionable(),
                "ASKING ONLY THE VERDICT IS THE DEFECT. A Ready verdict the player cannot pay for "
                        + "would otherwise render as go on a button that does nothing.");

        AnvilDecision refused = AnvilDecision.of(Side.EMPTY, Side.EMPTY, 999_999);
        assertFalse(refused.actionable(), "and a bottomless wallet does not make a refusal legal");
    }

    /**
     * *** A REFUSED PAIR HAS NO PRICE, SO IT IS NOT REPORTED AS UNAFFORDABLE. ***
     *
     * <p>Otherwise the screen would name the wrong problem: a player with two helmets and no XP
     * would be told to go and earn some, when the items are what is wrong.
     */
    @Test
    void aNonREADYVerdictIsNeverReportedAsUnaffordable_evenAtAZeroWallet() {
        int checked = 0;
        for (Side target : new Side[] {Side.EMPTY, Side.UNSCOREABLE}) {
            AnvilDecision decision = AnvilDecision.of(target, scored(310, Rarity.EXOTIC), 0);
            assertInstanceOf(Affordability.Affordable.class, decision.affordability(),
                    "a refusal has no price to be short of: " + decision.verdict());
            assertFalse(decision.actionable());
            checked++;
        }
        // AND A KEY MISMATCH, which is the arm most likely to be expensive if it were priced.
        AnvilDecision mismatch = AnvilDecision.of(
                new Side.Scored(TransferKey.ARMOR_HEAD, 140, Rarity.EXOTIC),
                new Side.Scored(TransferKey.ARMOR_FEET, 310, Rarity.EXOTIC), 0);
        assertInstanceOf(Affordability.Affordable.class, mismatch.affordability());
        checked++;
        assertEquals(3, checked, "the sweep has to have actually run");
    }

    /**
     * *** THE WHOLE POINT OF THE TYPE: TWO DECISIONS ARE EQUAL EXACTLY WHEN NOTHING DECIDED
     * DIFFERENTLY. ***
     *
     * <p>If the record carried a rendered string, a tick count or any incidental field, this
     * equality would report differences that are not changes -- and the confirm handler would refuse
     * transfers that were never stale.
     */
    @Test
    void equalityIsSTRUCTURALOverDecisionBearingFieldsOnly() {
        AnvilDecision a = AnvilDecision.of(scored(140, Rarity.RARE), scored(310, Rarity.RARE), 5000);
        AnvilDecision b = AnvilDecision.of(scored(140, Rarity.RARE), scored(310, Rarity.RARE), 5000);
        assertEquals(a, b, "the same inputs must produce an EQUAL decision, or nothing reconciles");

        // A DIFFERENT DONOR SCORE IS A DIFFERENT DECISION.
        assertNotEquals(a,
                AnvilDecision.of(scored(140, Rarity.RARE), scored(340, Rarity.RARE), 5000));
        // A DIFFERENT TARGET RARITY IS TOO -- same score, different price.
        assertNotEquals(a,
                AnvilDecision.of(scored(140, Rarity.EPIC), scored(310, Rarity.EPIC), 5000));
        // AND SO IS A DIFFERENT WALLET, when it crosses the price.
        assertNotEquals(a,
                AnvilDecision.of(scored(140, Rarity.RARE), scored(310, Rarity.RARE), 10));

        // BUT A WALLET THAT MOVES WITHOUT CROSSING THE PRICE IS NOT A DECISION CHANGE, and that is
        // what stops every mob kill invalidating an armed transfer.
        assertEquals(a,
                AnvilDecision.of(scored(140, Rarity.RARE), scored(310, Rarity.RARE), 6000),
                "the wallet is not a field; only whether it covers the price is");
    }

    @Test
    void aHalfBuiltDecisionIsREFUSEDLoudly_becauseTheProducerWouldHaveBeenBypassed() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnvilDecision(null, Affordability.AFFORDABLE));
        assertThrows(IllegalArgumentException.class,
                () -> new AnvilDecision(new AnvilVerdict.SlotEmpty(), null));
    }
}
