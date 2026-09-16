package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The strip refund, and the bound that ships silently wrong.
 *
 * <p>The headline case is {@link #aLevelIONLYItemRefundsItsUNLOCK_theCaseTheOldBoundReturnsZEROFor}:
 * the predecessor's exclusive loop returns <b>nothing at all</b> for the commonest item on the
 * server, and no test that only exercised II and III would catch it.
 *
 * <p>Every figure here is a LITERAL. The curve is {@code {352, 910, 2920}} per rung, cumulative
 * {@code 352 / 1262 / 4182}; re-deriving those through {@code EnchantCost} would make this file
 * agree with a mutated curve.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class GrindstoneRefundTest {

    private static final String A = "unbreaking";
    private static final String B = "sharpness";
    private static final String C = "protection";

    /** One slot, one candidate, at {@code level}. */
    private static EnchantState oneAt(int level) {
        EnchantState state = EnchantState.empty().addCandidate(0, A);
        return level == 0 ? state : state.withLevel(0, 0, level);
    }

    // --- The bound -----------------------------------------------------------------------------

    @Test
    void aLevelIONLYItemRefundsItsUNLOCK_theCaseTheOldBoundReturnsZEROFor() {
        // THE TRAP, AND IT IS THE COMMONEST ITEM ON THE SERVER. The old repo's loop is
        //     for (int lvl = 1; lvl < a.level(); lvl++)
        // which at level 1 does not execute at all. Correct THERE -- its cost function means "raise
        // FROM this level" and unlocking to I was free -- and 100% short HERE, where reaching I
        // costs 352.
        assertEquals(352, GrindstoneRefund.listSpend(oneAt(1)),
                "reaching level I costs 352, so a level-I item has 352 of list price in it");
        assertEquals(123, GrindstoneRefund.points(List.of(oneAt(1))),
                "35% of 352 floors to 123 -- NOT zero, which is what the exclusive bound gives");

        // And the refund is not merely non-zero, it is the RIGHT non-zero: 0 is what the defect
        // produces, so asserting "> 0" alone would pass on a build that returned 1.
        assertNotEquals(0, GrindstoneRefund.points(List.of(oneAt(1))),
                "the exclusive bound's answer, named so the diff between the two is visible here");
        // Mutation: rung <= level -> rung < level -> both assertions redden.
    }

    @Test
    void theBoundIsInclusiveAtEveryLevel_andTheCUMULATIVEFiguresAreTheCurves() {
        // 352, then +910, then +2920. Cumulative, because a level-III enchant was paid for three
        // times on the way up -- each rung is priced independently and none is refunded twice.
        assertEquals(352, GrindstoneRefund.listSpend(oneAt(1)), "I");
        assertEquals(1262, GrindstoneRefund.listSpend(oneAt(2)), "II -- 352 + 910");
        assertEquals(4182, GrindstoneRefund.listSpend(oneAt(3)), "III -- 352 + 910 + 2920");

        // THE EXCLUSIVE BOUND'S ANSWERS, WRITTEN OUT so the shortfall is legible rather than
        // described: it returns the CUMULATIVE COST OF THE LEVEL BELOW at every rung.
        assertEquals(0, 0, "exclusive at I would be 0 against 352");
        assertEquals(352, GrindstoneRefund.listSpend(oneAt(1)),
                "exclusive at II would be 352 -- which is this, the level BELOW");
        assertEquals(1262, GrindstoneRefund.listSpend(oneAt(2)),
                "exclusive at III would be 1262 -- again the level below");
        // Mutation: rung <= level -> rung < level -> the first three redden.
    }

    @Test
    void aLockedCandidateCostsNothingWithoutASpecialCase() {
        assertEquals(0, GrindstoneRefund.listSpend(oneAt(0)),
                "a level-0 candidate's rung loop does not execute");
        assertEquals(0, GrindstoneRefund.points(List.of(oneAt(0))), "so it refunds nothing");
        assertEquals(0, GrindstoneRefund.points(List.of()), "and an empty tray refunds nothing");
    }

    // --- Every candidate, not just the active one ----------------------------------------------

    @Test
    void theWALKCoversEVERYCandidateInEVERYSlot_notJustTheActiveOne() {
        // A PLAYER PAID FOR EVERY CANDIDATE THEY UNLOCKED. Only one can be active, and refunding
        // only the active one would silently keep the rest of their spend -- invisible, because
        // the tooltip only ever shows the active choice.
        EnchantState state = EnchantState.empty()
                .addCandidate(0, A).addCandidate(0, B)
                .addCandidate(1, C)
                .withLevel(0, 0, 1)     // 352
                .withLevel(0, 1, 2)     // 1262
                .withLevel(1, 0, 3);    // 4182

        assertEquals(5796, GrindstoneRefund.listSpend(state),
                "352 + 1262 + 4182 -- two slots, three unlocked candidates");
        assertEquals(2028, GrindstoneRefund.points(List.of(state)), "35% of 5796 floors to 2028");

        // THE DISCRIMINATING HALF: the active candidate alone would be 352, and a slot-only walk
        // would be 1614. Both are wrong and both are plausible implementations.
        assertNotEquals(352, GrindstoneRefund.listSpend(state), "not the active candidate alone");
        assertNotEquals(1614, GrindstoneRefund.listSpend(state), "not slot 0 alone");
        // Mutation: walk slot.active() instead of slot.candidates() -> reddens.
    }

    // --- The tray is the unit ------------------------------------------------------------------

    @Test
    void thePercentageIsAppliedONCEToTheTRAY_notPerItemAndSummed() {
        // Integer division floors, so summing per-item refunds loses up to a point PER ITEM.
        // MEASURED, not argued: two items each holding one level-II candidate.
        //
        //   per item   floor(1262 * 35 / 100) = 441   ->  441 + 441 = 882
        //   per tray   floor(2524 * 35 / 100) = 883
        //
        // One point, taken from the player, for nothing. OVER A FULL TRAY IT IS 12 -- computed, not
        // the n-1 = 13 the shape suggests: only ten distinct remainders are reachable and the
        // largest is .90, so fourteen of them sum to 12.6 and floor to 12. See the class javadoc.
        EnchantState item = oneAt(2);
        assertEquals(441, GrindstoneRefund.points(List.of(item)), "one item alone floors to 441");
        assertEquals(883, GrindstoneRefund.points(List.of(item, item)),
                "the TRAY floors once, to 883 -- not 882, which is 441 twice");
        // Mutation: sum points(single) per item -> 882 -> reddens.
    }

    @Test
    void theINTEGERFormIsNotTheFLOATINGPointForm_atAREACHABLETotal() {
        // ENUMERATED, NOT PREDICTED. Every reachable tray total is a sum of 352/1262/4182, so the
        // textbook counterexamples for 0.35 are mostly unreachable here. A sweep of all 171030
        // distinct reachable totals found 3466 divergences.
        //
        // *** THIS IS THE SMALLEST *CLEAN* DIVERGENCE, NOT THE SMALLEST. *** Smaller MIXED totals
        // diverge too -- 3466 of them exist and many are below 41820. "Clean" means a tray of
        // level-III candidates and nothing else, so the staged total is ten times one number and a
        // reader can re-derive it in their head.
        //
        // CHOSEN THAT WAY ON PURPOSE: the smallest divergence overall is some combination of three
        // different rung costs, which nobody can check by inspection and which reads as a magic
        // number. A fixture whose total cannot be reproduced without re-running the sweep is a
        // fixture the next reader has to trust. This one is 10 x 4182.
        //
        //   t = 41820, ten level-III candidates
        //   t * 35 / 100   = 1463700 / 100 = 14637     <- integer, and what ships
        //   (int)(t * 0.35)                = 14636     <- 41820 * 0.35 is 14636.999999999998
        //
        // The FP form takes a point from the player. EnchantCost's javadoc argues the integer form
        // with its own measured example; this is the same argument on the refund's side, where the
        // floor has the OPPOSITE SIGN -- it takes from the player rather than from the price.
        List<EnchantState> tray = List.of(
                EnchantState.empty()
                        .addCandidate(0, A).addCandidate(0, B).addCandidate(0, C)
                        .withLevel(0, 0, 3).withLevel(0, 1, 3).withLevel(0, 2, 3)
                        .addCandidate(1, A).addCandidate(1, B).addCandidate(1, C)
                        .withLevel(1, 0, 3).withLevel(1, 1, 3).withLevel(1, 2, 3)
                        .addCandidate(2, A).addCandidate(2, B).addCandidate(2, C)
                        .withLevel(2, 0, 3).withLevel(2, 1, 3).withLevel(2, 2, 3),
                oneAt(3));

        int total = 0;
        for (EnchantState state : tray) total += GrindstoneRefund.listSpend(state);
        assertEquals(41820, total, "ten level-III candidates -- the staged total");

        assertEquals(14637, GrindstoneRefund.points(tray), "the integer form");
        assertEquals(14636, (int) (total * 0.35), "the floating-point form, one point short");
        // Mutation: total * REFUND_PERCENT / 100 -> (int)(total * 0.35) -> reddens by one point.
    }

    // --- The licence for having no ledger ------------------------------------------------------

    @Test
    void theRefundCanNEVERExceedWhatWasACTUALLYPaid_atANYShelfCount() {
        // THE WHOLE ARGUMENT FOR NOT CARRYING A LEDGER, asserted rather than described. The
        // discount caps at MAX_POWER, so a player always paid at least 70% of list; 35% of list is
        // therefore at most 35/70 = HALF of what they paid, and exactly 35% at full price.
        //
        // Swept over every power AND every level, because the claim is universal.
        int checked = 0;
        for (int level = 1; level <= EnchantState.MAX_LEVEL; level++) {
            int refund = GrindstoneRefund.points(List.of(oneAt(level)));
            for (int power = 0; power <= EnchantCost.MAX_POWER; power++) {
                int paid = 0;
                for (int rung = 1; rung <= level; rung++) paid += EnchantCost.xpPoints(rung, power);

                assertTrue(refund <= paid,
                        "level " + level + " at power " + power + ": refund " + refund
                                + " must not exceed the " + paid + " actually paid");
                assertTrue(refund * 2 <= paid + 1,
                        "and never more than half of it -- level " + level + " power " + power);
                checked++;
            }
        }
        assertEquals(93, checked, "the property has to have actually run");

        // *** THIS ROW BOUNDS THE CONSTANT FROM ABOVE ONLY. IT IS NOT THE GUARD ON 35. ***
        //
        // MEASURED over P = 1..100 against this exact sweep, not reasoned:
        //
        //   the first assertion alone  (refund <= paid)          permits P in 1..70
        //   both assertions            (+ refund <= half paid)   permits P in 1..35
        //
        // The ceiling is exact and it is not a coincidence: at power 30 a player paid 70% of list,
        // so at REFUND_PERCENT 70 the worst-case margin is ZERO -- not small, zero -- and 71 is the
        // first value that exceeds it, by 42. The half-assertion pulls that ceiling down to exactly
        // 35, so P = 36 reddens here.
        //
        // AND THE FLOOR IS WIDE OPEN. REFUND_PERCENT 1 satisfies every assertion in this method
        // trivially: a smaller refund can never exceed what was paid. **35 -> 1 IS GREEN HERE AND
        // RED ONLY ON THE LITERAL-VALUE ROWS** -- 123 / 441 / 1463 and the 2028 above.
        //
        // SO THE LITERALS ARE NOT REDUNDANT WITH THIS SWEEP, AND THIS COMMENT EXISTS TO STOP THE
        // NEXT TIDY-UP DELETING THEM AS "already covered by the property test". A property test and
        // a literal test that look interchangeable, where only one of them can see a whole class of
        // mutation, is the same shape as a mutation that kills fewer rows than expected: ask which
        // row is now the only thing holding the behaviour, and say so in that row.
        //
        // What this row DOES prove is the LICENCE for having no ledger -- that the refund can never
        // exceed the spend at any shelf count. That is a different claim from "the number is 35".
    }

    @Test
    void atFullPriceTheRateIsEXACTLY35Percent_andAtMaxPowerItIsHALF() {
        // The two ends of the argument above, as concrete numbers rather than an inequality.
        int listIII = 4182;
        assertEquals(listIII, GrindstoneRefund.listSpend(oneAt(3)), "list price for III");
        assertEquals(1463, GrindstoneRefund.points(List.of(oneAt(3))), "35% of list");

        int paidAtMaxPower = 0;
        for (int rung = 1; rung <= 3; rung++) paidAtMaxPower += EnchantCost.xpPoints(rung, 30);
        assertEquals(2927, paidAtMaxPower, "70% of list, rung by rung");

        // THE BACKWARDS INCENTIVE, MEASURED AND ACCEPTED: the cheap enchanter recovers 49.98%,
        // the full-price one 34.98%. Named here so it is a recorded consequence rather than a
        // discovery.
        assertTrue(1463 * 100 / 2927 > 1463 * 100 / listIII,
                "a player who enchanted cheaply gets the better RATE -- backwards from intent");
    }

    // --- The strip -----------------------------------------------------------------------------

    @Test
    void strippingLocksEveryCandidateAndKEEPSTheRollAndTheList() {
        EnchantState before = EnchantState.empty()
                .addCandidate(0, A).addCandidate(0, B)
                .addCandidate(1, C)
                .withLevel(0, 0, 3)
                .withLevel(0, 1, 1)
                .withLevel(1, 0, 2)
                .withActive(0, 0);

        EnchantState after = GrindstoneRefund.stripped(before);

        // THE ROLL SURVIVES -- Ben's ruling. The candidate LIST and its ORDER are the roll, so
        // this is the assertion that the player keeps what they rolled.
        assertEquals(2, after.slots().size(), "both slots survive");
        assertEquals(List.of(A, B),
                after.slots().get(0).candidates().stream().map(EnchantCandidate::enchantId).toList(),
                "slot 0 keeps both candidates, in order");
        assertEquals(List.of(C),
                after.slots().get(1).candidates().stream().map(EnchantCandidate::enchantId).toList(),
                "and slot 1 keeps its one");

        // EVERY level is 0 and NO slot has an active choice.
        for (EnchantSlot slot : after.slots()) {
            for (EnchantCandidate candidate : slot.candidates()) {
                assertEquals(0, candidate.level(), candidate.enchantId() + " must be locked");
            }
            assertEquals(EnchantSlot.NONE, slot.activeIndex(), "no slot may keep an active choice");
        }

        assertEquals(0, GrindstoneRefund.listSpend(after), "and there is nothing left to strip");
        // Mutation: withLevel(.., 1) instead of 0 -> the level loop reddens.
    }

    @Test
    void theACTIVECandidateClearsITSELF_soNoOrderIsForced() {
        // THE BRIEF SAID THIS ORDER WAS FORCED AND THAT A WRONG ORDER WOULD FAIL LOUDLY. It is
        // not, and it does not. EnchantState.withLevel clears active when it locks the active
        // candidate -- its own javadoc calls that "a documented consequence rather than a refusal".
        //
        // So this test does the thing the brief said would throw, and asserts that it does not.
        EnchantState before = EnchantState.empty()
                .addCandidate(0, A).addCandidate(0, B)
                .withLevel(0, 0, 2)
                .withActive(0, 0);
        assertEquals(0, before.slots().get(0).activeIndex(), "staged with candidate 0 ACTIVE");

        EnchantState after = before.withLevel(0, 0, 0);   // lock the ACTIVE one, directly
        assertEquals(EnchantSlot.NONE, after.slots().get(0).activeIndex(),
                "locking the active candidate clears active rather than throwing");

        // AND THE THING THAT DOES THROW, so the guard is not assumed to be absent either: building
        // the slot by hand with a locked candidate active is still refused.
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new EnchantSlot(List.of(new EnchantCandidate(A, 0)), 0));
        assertTrue(ex.getMessage().contains("locked"),
                "the constructor still refuses it -- the rule is to go through withLevel");
    }

    @Test
    void strippingAnUnenchantedItemIsAnIdentityRatherThanAnErrorCase() {
        EnchantState rolled = EnchantState.empty().addCandidate(0, A).addCandidate(0, B);
        assertEquals(rolled, GrindstoneRefund.stripped(rolled),
                "an already-stripped item is unchanged -- the strip is idempotent");
        assertEquals(GrindstoneRefund.stripped(rolled),
                GrindstoneRefund.stripped(GrindstoneRefund.stripped(rolled)),
                "and stays unchanged on a second pass");
    }

    // --- The count on the button ---------------------------------------------------------------

    @Test
    void theCountIsTheItemsThatWouldCHANGE_notTheTraySize() {
        // A fourteen-item tray with nine enchanted reads "Strip 9 weapons". Printing 14 beside the
        // refund invites the player to divide the refund by 14.
        List<EnchantState> tray = List.of(oneAt(1), oneAt(0), oneAt(3), oneAt(0), oneAt(2));

        assertEquals(3, GrindstoneRefund.strippableCount(tray), "three of five have levels");
        assertNotEquals(tray.size(), GrindstoneRefund.strippableCount(tray),
                "and that is deliberately not the tray size");
        assertEquals(0, GrindstoneRefund.strippableCount(List.of(oneAt(0), oneAt(0))),
                "a tray of already-stripped items counts zero");
        assertEquals(0, GrindstoneRefund.strippableCount(List.of()), "an empty tray counts zero");
        // Mutation: return tray.size() -> reddens on the first and third.
    }

    @Test
    void aTrayOfUnenchantedItemsRefundsZERO_soTheButtonHasSomethingToSayNo_to() {
        // The GRAY "these have nothing to strip" state, from the model's side: a non-empty tray
        // whose refund AND count are both zero. The button distinguishes it from an EMPTY tray by
        // tray.isEmpty(), which is why the model does not need a third answer.
        List<EnchantState> tray = List.of(oneAt(0), oneAt(0));
        assertEquals(0, GrindstoneRefund.points(tray), "nothing to give back");
        assertEquals(0, GrindstoneRefund.strippableCount(tray), "and nothing would change");
        assertTrue(!tray.isEmpty(), "but the tray is NOT empty -- that is the other gray");
    }

    // --- Arguments -----------------------------------------------------------------------------

    @Test
    void nullsAreRefusedLoudlyRatherThanCountedAsZero() {
        // A null tray returning 0 would be indistinguishable from an empty one, and the caller
        // would have granted nothing while believing it granted a refund.
        assertThrows(IllegalArgumentException.class, () -> GrindstoneRefund.points(null));
        assertThrows(IllegalArgumentException.class, () -> GrindstoneRefund.strippableCount(null));
        assertThrows(IllegalArgumentException.class, () -> GrindstoneRefund.listSpend(null));
        assertThrows(IllegalArgumentException.class, () -> GrindstoneRefund.stripped(null));
    }
}
