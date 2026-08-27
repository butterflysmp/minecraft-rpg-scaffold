package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantCost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which clicks the economy gates, and which it leaves alone.
 *
 * This exists as a unit test rather than as a boot row because the property that matters most here
 * is invisible from inside the game except as an absence: an XP counter that does not move. A gate
 * runner can watch a swap and see nothing happen, which is also what a broken readout looks like.
 *
 * The whole enum is walked, not a sample, because the failure this guards is an arm being added or
 * moved rather than an arm computing wrongly.
 */
class EnchantChargeTest {

    @Test
    void unlockingBuysTheFirstLevel() {
        assertEquals(1, EnchantCharge.targetLevel(EnchantClickIntent.UNLOCK, 0));
        // Mutation, RUN: UNLOCK -> FREE -> 3 red, "only UNLOCK and LEVEL_UP are ever paid for ==>
        // expected: <2> but was: <1>".
    }

    @Test
    void levellingUpBuysTheLevelBeingREACHEDAndNotTheOneAlreadyHeld() {
        // The off-by-one that would charge II's price to reach III -- and it is the cheaper of the
        // two, so it would read as a discount rather than as a bug.
        assertEquals(2, EnchantCharge.targetLevel(EnchantClickIntent.LEVEL_UP, 1));
        assertEquals(3, EnchantCharge.targetLevel(EnchantClickIntent.LEVEL_UP, 2));
        // Mutation, RUN: currentLevel + 1 -> currentLevel -> 2 red, "expected: <2> but was: <1>".
        // Reaching II would have been charged 352 instead of 910.
    }

    @Test
    void swappingBetweenTwoUnlockedCandidatesIsFreeForEver() {
        // THE PROPERTY THE CANDIDATE MODEL EXISTS FOR. A level rides the candidate, not the choice,
        // so a player can move the glint back and forth to compare two enchants without paying for
        // it. Charge here and the level-you-paid-for guarantee is gone.
        assertEquals(EnchantCharge.FREE, EnchantCharge.targetLevel(EnchantClickIntent.ACTIVATE, 0));
        assertEquals(EnchantCharge.FREE, EnchantCharge.targetLevel(EnchantClickIntent.ACTIVATE, 1));
        assertEquals(EnchantCharge.FREE, EnchantCharge.targetLevel(EnchantClickIntent.ACTIVATE, 3),
                "even at III, re-activating buys nothing");
        // Mutation, RUN: move ACTIVATE out of the free arm and price it at 1 -> 3 red, "only UNLOCK
        // and LEVEL_UP are ever paid for ==> expected: <2> but was: <3>". Every swap would cost 352.
    }

    @Test
    void everyClickThatChangesNothingCostsNothing() {
        // AT_MAX says something and yields the state unchanged; EMPTY is a filler pane; the unknown
        // arm is a refusal. None of the three may take money for a click that did not land.
        assertEquals(EnchantCharge.FREE, EnchantCharge.targetLevel(EnchantClickIntent.AT_MAX, 3));
        assertEquals(EnchantCharge.FREE, EnchantCharge.targetLevel(EnchantClickIntent.EMPTY, 0));
        assertEquals(EnchantCharge.FREE, EnchantCharge.targetLevel(EnchantClickIntent.UNKNOWN_ENCHANT, 2));
    }

    @Test
    void exactlyTwoOfTheSixIntentsEverCostAnything() {
        // Walks the WHOLE enum. A seventh intent cannot compile without being priced -- the switch
        // has no default arm -- but an existing one being quietly moved into the free list would
        // compile fine, and this is what catches that.
        int charged = 0;
        for (EnchantClickIntent intent : EnchantClickIntent.values()) {
            if (EnchantCharge.targetLevel(intent, 1) != EnchantCharge.FREE) charged++;
        }
        assertEquals(2, charged, "only UNLOCK and LEVEL_UP are ever paid for");
        assertEquals(6, EnchantClickIntent.values().length,
                "a new intent has to be priced deliberately, not counted accidentally");
    }

    @Test
    void anythingItChargesForIsALevelEnchantCostWillActuallyPrice() {
        // The seam calls xpPoints with whatever comes out of here, and xpPoints THROWS outside 1..3.
        // A target level this produced but that could not be priced would be an exception on a click
        // rather than a refusal -- so the two are pinned against each other rather than trusted.
        int checked = 0;
        for (EnchantClickIntent intent : EnchantClickIntent.values()) {
            for (int level = 0; level < 3; level++) {
                int target = EnchantCharge.targetLevel(intent, level);
                if (target == EnchantCharge.FREE) continue;
                assertTrue(target >= 1 && target <= EnchantCost.maxPricedLevel(),
                        intent + " at level " + level + " asked to buy level " + target);
                EnchantCost.xpPoints(target, 0);   // must not throw
                checked++;
            }
        }
        // Six, not five: UNLOCK charges at all three starting levels and so does LEVEL_UP. The
        // counter caught that miscount, which is the reason it is here.
        assertEquals(6, checked, "the property has to have actually run");
    }
}
