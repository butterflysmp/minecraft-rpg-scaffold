package io.github.butterflysmp.rpg.core.progression;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static io.github.butterflysmp.rpg.core.progression.XpGrant.Op.ADD;
import static io.github.butterflysmp.rpg.core.progression.XpGrant.Op.SET;
import static io.github.butterflysmp.rpg.core.progression.XpGrant.Unit.LEVELS;
import static io.github.butterflysmp.rpg.core.progression.XpGrant.Unit.XP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four arms of {@code /rpg playerxp}, and the two places the port diverges from its source.
 *
 * <p>Literals, not symbols, for {@code PlayerLevelTest}'s reason: a threshold named symbolically
 * moves with the code and guards nothing.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class XpGrantTest {

    private static final long LEVEL_3 = 2_090L;
    private static final long LEVEL_10 = 12_940L;
    private static final long LEVEL_13 = 19_980L;
    // Rung 13 is 2,770, so a partial of 1,000 stays INSIDE level 13. Staged at 5,000 first, which
    // is past the rung -- the fixture was level 14 and the row's own control caught it.
    private static final long LEVEL_14 = 22_750L;
    private static final long PARTIAL = 1_000L;
    private static final long LEVEL_99 = 11_642_250L;

    private static long target(XpGrant.Op op, XpGrant.Unit unit, long amount, long current) {
        return XpGrant.targetLifetimeXp(op, unit, amount, current).orElseThrow();
    }

    @Test
    void setLEVELSLandsEXACTLYOnTheThreshold_whichIsTheWholePointForStaging() {
        // *** BEN'S RULING, AND THE DIVERGENCE FROM THE PREDECESSOR. *** It kept partial progress
        // because progress was a separate field; there is no separate field here, so the threshold
        // IS the answer. An operator staging "level 13 unlocks the grindstone" lands on 13 with
        // nothing into it, and the next XP they earn is the first XP of level 13.
        assertEquals(LEVEL_13, target(SET, LEVELS, 13, 0L), "set 13 levels is the level-13 total");
        assertEquals(LEVEL_3, target(SET, LEVELS, 3, 0L), "crafting's gate");
        assertEquals(LEVEL_10, target(SET, LEVELS, 10, 0L), "enchanting's gate");

        // AND IT IS ABSOLUTE, NOT RELATIVE -- the current total is discarded, including a larger one.
        assertEquals(LEVEL_3, target(SET, LEVELS, 3, LEVEL_99),
                "set is absolute: a level-99 player set to 3 LANDS on 3");
        assertEquals(LEVEL_13, target(SET, LEVELS, 13, LEVEL_13 + 5_000L),
                "and partial progress into 13 is discarded rather than kept");
        // Mutation MUTSET-RELATIVE: add `current` to the result -> kill set RECORDED in the PR body.
    }

    @Test
    void setLEVELSClampsToARealLevelRatherThanThrowing() {
        // totalForLevel THROWS outside 1..99 by design, so this arm must fold the input first --
        // an operator typo must not take out the command handler.
        assertEquals(0L, target(SET, LEVELS, 1, 5_000L), "level 1 is a total of zero");
        assertEquals(0L, target(SET, LEVELS, 0, 5_000L), "0 is not a level; fold to 1");
        assertEquals(0L, target(SET, LEVELS, -50, 5_000L), "nor is a negative one");
        assertEquals(LEVEL_99, target(SET, LEVELS, 99, 0L), "the cap");
        assertEquals(LEVEL_99, target(SET, LEVELS, 100, 0L), "and past it is still the cap");
        assertEquals(LEVEL_99, target(SET, LEVELS, Long.MAX_VALUE, 0L));
    }

    @Test
    void addLEVELSKeepsPartialProgress_whichIsTheARMThatReproducesThePredecessor() {
        // *** THE OTHER DIVERGENCE, AND THIS ONE IS A MATCH RATHER THAN A CHANGE. ***
        // There, `add 1 levels` raised the level field and left progress alone. In our units that
        // is exactly "add the rung", because T(L+1)+p minus T(L)+p IS the rung. Staged AT 5000
        // into level 13 so a "set the new threshold" implementation reddens -- it would return
        // LEVEL_14 flat and lose the 5000.
        long partway = LEVEL_13 + PARTIAL;
        assertEquals(LEVEL_14 + PARTIAL, target(ADD, LEVELS, 1, partway),
                "one level up from 1000 into 13 is 1000 into 14 -- the progress SURVIVES");
        assertEquals(13, PlayerLevel.levelFor(partway), "the fixture really is level 13");
        assertEquals(14, PlayerLevel.levelFor(target(ADD, LEVELS, 1, partway)), "and lands on 14");

        // FROM A CLEAN BOUNDARY IT IS THE PLAIN THRESHOLD, which is the case that cannot tell the
        // two implementations apart -- stated here so the row above is visibly the discriminating one.
        assertEquals(LEVEL_13, target(ADD, LEVELS, 3, LEVEL_10), "10 plus 3 levels is exactly 13");
        // Mutation MUTADD-THRESHOLD: return totalForLevel(to) instead of the delta ->
        // kill set RECORDED in the PR body.
    }

    @Test
    void addLEVELSAtTheCapIsANoOp_andDoesNotLoseProgressEither() {
        // The delta between two clamped thresholds is zero, so nothing moves -- including the
        // overshoot a level-99 player has accumulated past the cap total.
        long pastTheCap = LEVEL_99 + 1_000_000L;
        assertEquals(pastTheCap, target(ADD, LEVELS, 5, pastTheCap),
                "already capped: 5 more levels changes nothing, and does not TRUNCATE to the cap");
        assertEquals(LEVEL_99, target(ADD, LEVELS, 50, LEVEL_99), "and exactly at the cap it holds");

        // AND THE ARITHMETIC CANNOT OVERFLOW ON THE WAY: from + levels is done in long, so a
        // huge amount folds to 99 rather than wrapping negative and landing on level 1.
        assertEquals(LEVEL_99, target(ADD, LEVELS, Long.MAX_VALUE, 0L),
                "an absurd level count lands at the cap, NOT at level 1");
        assertEquals(99, PlayerLevel.levelFor(target(ADD, LEVELS, Long.MAX_VALUE, 0L)));
        // Mutation MUTCLAMP-INT: compute `from + levels` as an int -> kill set RECORDED in the PR body.
    }

    @Test
    void theXPArmsAreTheSimpleOnes_addAccumulatesAndSetReplaces() {
        assertEquals(5_500L, target(ADD, XP, 5_000L, 500L), "add is current plus amount");
        assertEquals(5_000L, target(SET, XP, 5_000L, 500L), "set discards current");
        assertEquals(0L, target(SET, XP, 0L, LEVEL_99), "set 0 wipes a capped player, deliberately");

        // SET FLOORS AT ZERO. Below zero is not a lower level -- levelFor already reads it as 1 --
        // it is a number that looks corrupt to the next person who opens the JSON.
        assertEquals(0L, target(SET, XP, -1L, 5_000L));
        assertEquals(0L, target(SET, XP, Long.MIN_VALUE, 5_000L));

        // AND ADD SATURATES rather than wrapping, through the one expression in PlayerLevel.
        assertEquals(Long.MAX_VALUE, target(ADD, XP, 1_000L, Long.MAX_VALUE - 5L));
    }

    @Test
    void addREFUSESANegativeAmount_inBOTHUnits_whichTightensThePredecessor() {
        // *** THE PORT IS TIGHTER THAN ITS SOURCE HERE, AND THAT IS A DEFECT FOUND, NOT A CHOICE. ***
        // The predecessor's check sat inside its `xp` branch only, so `add -3 levels` DEMOTED a
        // player through the very command that refuses `add -3 xp`. The message is about `add`, so
        // it binds `add`.
        assertTrue(XpGrant.targetLifetimeXp(ADD, XP, -1L, 5_000L).isEmpty(), "add -1 xp is refused");
        assertTrue(XpGrant.targetLifetimeXp(ADD, LEVELS, -1L, 5_000L).isEmpty(),
                "AND SO IS add -1 levels -- the half the predecessor left open");
        assertTrue(XpGrant.targetLifetimeXp(ADD, LEVELS, Long.MIN_VALUE, 5_000L).isEmpty());

        // ZERO IS NOT NEGATIVE AND IS ALLOWED. A no-op `add 0` is a legitimate thing to type, and
        // refusing it would make the boundary read as "positive" when the message says
        // "non-negative".
        assertEquals(5_000L, target(ADD, XP, 0L, 5_000L), "add 0 xp is a no-op, not a refusal");
        assertEquals(5_000L, target(ADD, LEVELS, 0L, 5_000L), "and so is add 0 levels");

        // SET IS NOT REFUSED AT ANY VALUE -- it is the arm the message points at.
        assertEquals(OptionalLong.of(0L), XpGrant.targetLifetimeXp(SET, XP, -5L, 5_000L));
        assertEquals(OptionalLong.of(0L), XpGrant.targetLifetimeXp(SET, LEVELS, -5L, 5_000L));
        // Mutation MUTREFUSE-XPONLY: scope the refusal to Unit.XP -> kill set RECORDED in the PR body.
    }

    @Test
    void theREFUSALMessageNamesTheOperationThatTakesAnAbsoluteValue() {
        // Asserted as a whole string rather than by a clause, per CLAUDE.md: a `contains` on one
        // word survives a mutation that removes a different half of the sentence.
        assertEquals("'add' requires a non-negative amount; use 'set' for absolute values.",
                XpGrant.ADD_REFUSES_NEGATIVE,
                "ported verbatim -- it tells the operator what to type next, which is the only "
                        + "thing a refusal has to do");
    }
}
