package io.github.butterflysmp.rpg.core.progression;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The player level curve, and the totals that gate the hub's stations.
 *
 * <p>The cumulative figures are LITERALS, computed from the source table and written out, because
 * re-deriving them here through {@code totalForLevel} would make this file agree with a mutated
 * table. The three gate thresholds in particular are content decisions and not arithmetic.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class PlayerLevelTest {

    // Verified against the predecessor's table by summing it, not by quoting its comments.
    private static final long LEVEL_3 = 2_090L;
    // The anvil's gate, slice 13a. Summed from XP_TO_NEXT[1..6] -- 1000 + 1090 + 1190 + 1290 +
    // 1400 + 1530 -- and NOT re-derived through totalForLevel, which would make this file agree
    // with a mutated table. Same rule the three around it were written under.
    private static final long LEVEL_7 = 7_500L;
    private static final long LEVEL_10 = 12_940L;
    private static final long LEVEL_13 = 19_980L;
    private static final long LEVEL_25 = 75_330L;
    private static final long LEVEL_50 = 712_580L;
    private static final long LEVEL_99 = 11_642_250L;

    @Test
    void theCUMULATIVETotalsAreTheCurveAsAuthored() {
        assertEquals(0L, PlayerLevel.totalForLevel(1), "level 1 is where everyone starts");
        assertEquals(1_000L, PlayerLevel.totalForLevel(2), "the first rung is 1000");
        assertEquals(LEVEL_3, PlayerLevel.totalForLevel(3), "crafting's gate");
        assertEquals(LEVEL_7, PlayerLevel.totalForLevel(7), "the anvil's gate");
        assertEquals(LEVEL_10, PlayerLevel.totalForLevel(10), "enchanting's gate");
        assertEquals(LEVEL_13, PlayerLevel.totalForLevel(13), "the grindstone's gate");
        assertEquals(LEVEL_25, PlayerLevel.totalForLevel(25));
        assertEquals(LEVEL_50, PlayerLevel.totalForLevel(50));
        assertEquals(LEVEL_99, PlayerLevel.totalForLevel(PlayerLevel.MAX_LEVEL), "the cap");
        // Mutation: change any table entry -> every total at or above it reddens.
    }

    @Test
    void theCURVEIsSTRICTLYIncreasing_atEveryRung() {
        // A flat or backward rung would make levelFor ambiguous and the "to next" figure negative.
        // Checked over the WHOLE curve rather than at the anchors, because a hand-authored table is
        // exactly where a transposed pair hides.
        long previous = -1;
        int checked = 0;
        for (int level = 1; level <= PlayerLevel.MAX_LEVEL; level++) {
            long total = PlayerLevel.totalForLevel(level);
            assertTrue(total > previous,
                    "total for level " + level + " (" + total + ") must exceed level "
                            + (level - 1) + " (" + previous + ")");
            previous = total;
            checked++;
        }
        assertEquals(99, checked, "the property has to have actually run");
    }

    @Test
    void levelForIsTheINVERSE_andEveryBoundaryIsExact() {
        // ONE XP SHORT IS THE PREVIOUS LEVEL, AND EXACTLY ON THE TOTAL IS THE NEW ONE. An off-by-one
        // here gates a station one XP early or late, which nobody would ever notice in play.
        for (int level = 2; level <= PlayerLevel.MAX_LEVEL; level++) {
            long total = PlayerLevel.totalForLevel(level);
            assertEquals(level, PlayerLevel.levelFor(total),
                    "exactly " + total + " is level " + level);
            assertEquals(level - 1, PlayerLevel.levelFor(total - 1),
                    "one XP short of " + total + " is still level " + (level - 1));
        }
        // Mutation: `>=` -> `>` in levelFor -> every exact-boundary row reddens.
    }

    @Test
    void theAUTHOREDStationGatesAreTheLEVELSBenNamed_craftingAnvilEnchantingGrindstone() {
        // *** NAMED, NOT COUNTED. THIS WAS theTHREEGateThresholds... AND THE ANVIL FALSIFIED IT. ***
        //
        // The name and its first comment both said THREE, so adding a fourth row made the method
        // name a claim about its own body that was wrong -- and an ordinal or a count in a name is
        // a line citation in different clothes: falsified by any insertion, silently, and invisible
        // to every test including this one. Naming the stations costs four words and cannot rot.
        //
        // THE VAULT IS ABSENT ON PURPOSE. Its threshold is not authored on Station at all; it is
        // read from VaultPageGate.HUB_SHORTCUT_LEVEL, so it is pinned where that constant lives and
        // not here. This method is about the levels typed as literals into the gate.
        //
        // THE CONTENT DECISION, pinned as levels AND as totals. Either alone is half the claim: the
        // level is what Ben ruled, and the total is what the listener actually compares against.
        assertEquals(3, PlayerLevel.levelFor(LEVEL_3), "crafting unlocks at 3");
        assertEquals(7, PlayerLevel.levelFor(LEVEL_7), "the anvil at 7");
        assertEquals(10, PlayerLevel.levelFor(LEVEL_10), "enchanting at 10");
        assertEquals(13, PlayerLevel.levelFor(LEVEL_13), "the grindstone at 13");

        // AND ONE XP SHORT OF EACH IS STILL LOCKED -- the half that catches an off-by-one gate.
        assertEquals(2, PlayerLevel.levelFor(LEVEL_3 - 1));
        assertEquals(6, PlayerLevel.levelFor(LEVEL_7 - 1));
        assertEquals(9, PlayerLevel.levelFor(LEVEL_10 - 1));
        assertEquals(12, PlayerLevel.levelFor(LEVEL_13 - 1));
    }

    @Test
    void badDataClampsRatherThanThrowing_becauseThisReadsOffDisk() {
        // levelFor takes a STORED number that may have been hand-edited. A progression display is
        // not a place to take out a join handler.
        assertEquals(1, PlayerLevel.levelFor(0), "a new player");
        assertEquals(1, PlayerLevel.levelFor(-1), "a negative total is not a level 0");
        assertEquals(1, PlayerLevel.levelFor(Long.MIN_VALUE), "nor is an absurd one");
        assertEquals(PlayerLevel.MAX_LEVEL, PlayerLevel.levelFor(LEVEL_99),
                "exactly the cap total is the cap");
        assertEquals(PlayerLevel.MAX_LEVEL, PlayerLevel.levelFor(LEVEL_99 * 1000),
                "and far beyond it is STILL the cap -- lifetime XP does not stop at 99");
        assertEquals(PlayerLevel.MAX_LEVEL, PlayerLevel.levelFor(Long.MAX_VALUE));
    }

    @Test
    void totalForLevelREFUSESAnOutOfRangeLevel_unlikeLevelFor() {
        // THE ASYMMETRY IS DELIBERATE AND IS EnchantCost's. levelFor takes DATA and clamps;
        // totalForLevel takes a LEVEL, which is always computed and never stored, so an
        // out-of-range value is a programming error and is refused loudly.
        var low = assertThrows(IllegalArgumentException.class, () -> PlayerLevel.totalForLevel(0));
        assertTrue(low.getMessage().contains("1..99"), "the message names the range");
        assertThrows(IllegalArgumentException.class, () -> PlayerLevel.totalForLevel(100));
        assertThrows(IllegalArgumentException.class, () -> PlayerLevel.totalForLevel(-1));
    }

    @Test
    void theCAPHasNoNextLevel_andItIsEMPTYRatherThanAHugeNumber() {
        // *** THE PREDECESSOR RETURNED Long.MAX_VALUE HERE AND IT RENDERS. ***
        // An empty optional cannot be printed by accident; the maxed case has to be handled to
        // compile. Same reasoning as SettingsMenuLayout.chooserFor returning OptionalInt over -1.
        assertEquals(OptionalLong.empty(), PlayerLevel.xpToNextLevel(LEVEL_99),
                "at the cap there is no next level");
        assertEquals(OptionalLong.empty(), PlayerLevel.xpToNextLevel(Long.MAX_VALUE));
        assertTrue(PlayerLevel.isMaxed(LEVEL_99));
        assertFalse(PlayerLevel.isMaxed(LEVEL_99 - 1), "one short is not maxed");

        // AND BELOW THE CAP IT IS A REAL, POSITIVE, SHRINKING NUMBER.
        assertEquals(OptionalLong.of(1_000L), PlayerLevel.xpToNextLevel(0),
                "a new player needs the first rung");
        assertEquals(OptionalLong.of(1L), PlayerLevel.xpToNextLevel(999L), "one XP short of level 2");
        assertEquals(OptionalLong.of(PlayerLevel.totalForLevel(11) - LEVEL_10),
                PlayerLevel.xpToNextLevel(LEVEL_10), "exactly at 10, the whole of rung 10 remains");
        // Mutation: return Long.MAX_VALUE at the cap instead of empty -> the two empty rows redden.
    }

    @Test
    void intoCurrentLevelIsZeroAtEveryBoundaryAndNeverNegative() {
        for (int level = 1; level <= PlayerLevel.MAX_LEVEL; level++) {
            assertEquals(0L, PlayerLevel.intoCurrentLevel(PlayerLevel.totalForLevel(level)),
                    "exactly at level " + level + " is zero into it");
        }
        assertEquals(1L, PlayerLevel.intoCurrentLevel(1L), "one XP in");
        assertEquals(0L, PlayerLevel.intoCurrentLevel(-5L), "bad data floors at zero, not negative");

        // THE TWO HALVES SUM TO THE RUNG, which is what makes a progress display honest.
        long staged = LEVEL_25 + 1_234L;
        assertEquals(PlayerLevel.totalForLevel(26) - PlayerLevel.totalForLevel(25),
                PlayerLevel.intoCurrentLevel(staged) + PlayerLevel.xpToNextLevel(staged).orElseThrow(),
                "progress into the level plus XP remaining is the whole rung");
    }

    @Test
    void plusSATURATESAtBothEnds_becauseAWrapReadsAsLevelONE() {
        // ORDINARY ADDITION FIRST -- without this the two clamps are satisfied by a method that
        // returns MAX_VALUE for everything.
        assertEquals(1_500L, PlayerLevel.plus(1_000L, 500L), "the normal case is just addition");
        assertEquals(LEVEL_13, PlayerLevel.plus(LEVEL_10, LEVEL_13 - LEVEL_10),
                "10 plus the gap to 13 is 13");
        assertEquals(0L, PlayerLevel.plus(0L, 0L));

        // THE TOP. A wrapped total is NEGATIVE, and levelFor reads negative as level 1 -- so an
        // overflow here is not a large number, it is a wiped player.
        assertEquals(Long.MAX_VALUE, PlayerLevel.plus(Long.MAX_VALUE - 5L, 1_000L));
        assertEquals(Long.MAX_VALUE, PlayerLevel.plus(Long.MAX_VALUE, 1L));
        assertEquals(1, PlayerLevel.levelFor(Long.MAX_VALUE - 5L + 1_000L),
                "THE FAILURE THIS PREVENTS, staged explicitly: the raw sum wraps and reads as "
                        + "level 1, which is indistinguishable from a brand new player");
        assertEquals(PlayerLevel.MAX_LEVEL, PlayerLevel.levelFor(PlayerLevel.plus(Long.MAX_VALUE - 5L, 1_000L)),
                "and the saturating form stays at the cap");

        // THE BOTTOM. Only /rpg playerxp set can reach a subtraction, and it clamps to 0 itself --
        // this is the arithmetic floor beneath that, not a second policy.
        assertEquals(Long.MIN_VALUE, PlayerLevel.plus(Long.MIN_VALUE + 5L, -1_000L));
        assertEquals(500L, PlayerLevel.plus(1_000L, -500L), "an ordinary subtraction is untouched");
        // Mutation MUTPLUS-RAW: return `lifetimeXp + amount` -> kill set RECORDED in the PR body.
    }

    @Test
    void theCURVEIsTHEAUTHOREDONE_notARegeneratedFormula() {
        // THE TABLE IS HAND-AUTHORED AND ROUNDED TO THE NEAREST 10, so no closed form reproduces it.
        // These three anchors are where the predecessor's two exponential segments were pinned, and
        // they are asserted so a "simplification" that swapped the table for the formula reddens.
        assertEquals(1_000L, PlayerLevel.totalForLevel(2) - PlayerLevel.totalForLevel(1),
                "rung 1 is the first anchor");
        assertEquals(150_000L, PlayerLevel.totalForLevel(61) - PlayerLevel.totalForLevel(60),
                "rung 60 is where the two segments meet");
        assertEquals(400_000L, PlayerLevel.totalForLevel(99) - PlayerLevel.totalForLevel(98),
                "rung 98 is the last anchor");

        // EVERY RUNG IS A MULTIPLE OF TEN -- the rounding is a property of the whole table, and a
        // regenerated formula would break it almost everywhere rather than at the anchors.
        int checked = 0;
        for (int level = 1; level < PlayerLevel.MAX_LEVEL; level++) {
            long rung = PlayerLevel.totalForLevel(level + 1) - PlayerLevel.totalForLevel(level);
            assertEquals(0L, rung % 10, "rung " + level + " (" + rung + ") is not a multiple of ten");
            checked++;
        }
        assertEquals(98, checked, "the property has to have actually run");
    }
}
