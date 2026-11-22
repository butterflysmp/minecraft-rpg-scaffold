package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WHERE {@code hold:} STAGES A WEAPON THAT IS NOT IN THE HOTBAR (the seat's brief of 2026-11-21, PR B item 3).
 *
 * <p>The defect it fixes: {@code SelfTestDrivers.hold} moved a weapon into an EMPTY hotbar slot only, so with a full
 * hotbar every row that gave a weapon and then held it skipped with "no hotbar slot holds ..." (GATE-sword-convert.md,
 * WHAT IT FOUND, 2: sword-tiers ran twice, STA5 skipped twice). Now, with no empty hotbar slot, the weapon is SWAPPED
 * with a hotbar slot that does not hold the Ability Stone. Nothing is dropped or destroyed: the displaced item takes the
 * weapon's old slot, and the run's inventory restore (at every gate boundary, and at the end) puts both back.
 *
 * <p>Every fixture puts the stone at a DIFFERENT slot from the empty or chosen one, so a planner that ignored the stone
 * or the empty slots could not pass by accident.
 */
class SelfTestHotbarStagingTest {

    private static boolean[] none() { return new boolean[9]; }

    private static boolean[] only(int... slots) {
        boolean[] b = new boolean[9];
        for (int s : slots) b[s] = true;
        return b;
    }

    @Test
    void anEmptyHotbarSlotIsUsedFirstAndNothingIsDisplaced() {
        // slots 0 and 1 hold items, 2 is the stone, 5 is the first empty one
        assertEquals(new SelfTestStaging.HotbarPlan(SelfTestStaging.HotbarPlan.Kind.MOVE, 20, 5),
                SelfTestStaging.hotbarPlan(20, only(5, 7), only(2)));
    }

    @Test
    void aFullHotbarSwapsWithTheFirstSlotThatIsNotTheStone() {
        assertEquals(new SelfTestStaging.HotbarPlan(SelfTestStaging.HotbarPlan.Kind.SWAP, 14, 1),
                SelfTestStaging.hotbarPlan(14, none(), only(0)), "the stone at 0 is passed over");
        assertEquals(new SelfTestStaging.HotbarPlan(SelfTestStaging.HotbarPlan.Kind.SWAP, 30, 0),
                SelfTestStaging.hotbarPlan(30, none(), only(4)), "the stone at 4: slot 0 is the first that is not");
        assertEquals(new SelfTestStaging.HotbarPlan(SelfTestStaging.HotbarPlan.Kind.SWAP, 9, 8),
                SelfTestStaging.hotbarPlan(9, none(), only(0, 1, 2, 3, 4, 5, 6, 7)), "every slot but 8 a stone");
    }

    @Test
    void nothingToStageIsNone() {
        assertEquals(SelfTestStaging.HotbarPlan.Kind.NONE,
                SelfTestStaging.hotbarPlan(-1, only(3), none()).kind(), "the weapon is nowhere in the inventory");
        assertEquals(SelfTestStaging.HotbarPlan.Kind.NONE,
                SelfTestStaging.hotbarPlan(12, none(), only(0, 1, 2, 3, 4, 5, 6, 7, 8)).kind(),
                "a hotbar of nine stones: no slot may be given up");
    }

    /**
     * THE WIRING: {@code hold} asks the planner, performs a SWAP as an exchange of the two slots (never a drop), and logs
     * a STAGE line for each kind. A source read, like {@code SelfTestStagingTest}'s, because {@code hold} needs a live
     * player.
     */
    @Test
    void holdTakesThePlanAndLogsAStageLine() throws IOException {
        // CR stripped first: the working tree is CRLF here, and a "\n    }\n" needle would otherwise find nothing.
        String drivers = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/"
                + "SelfTestDrivers.java"), StandardCharsets.UTF_8).replace("\r", "");
        int start = drivers.indexOf("private Optional<String> hold(Player p, String what)");
        assertTrue(start >= 0, "hold() not found: the scan is blind");
        int end = drivers.indexOf("\n    }\n", start);
        assertTrue(end > start, "hold()'s closing brace not found: the scan is blind");
        String hold = drivers.substring(start, end);
        assertTrue(hold.contains("SelfTestStaging.hotbarPlan("), "hold asks the planner");
        assertTrue(hold.contains("\"STAGE moved \""), "a MOVE logs a STAGE line");
        assertTrue(hold.contains("\"STAGE swapped \""), "a SWAP logs a STAGE line");
        assertTrue(hold.contains("inv.setItem(plan.from(), displaced)"),
                "a SWAP writes the displaced item into the weapon's old slot: nothing is dropped or destroyed");
        assertTrue(!hold.contains("dropItem"), "hold never drops an item");
    }

    private static boolean[] mainEmpty(int... indices) {
        boolean[] b = new boolean[27];
        for (int i : indices) b[i] = true;
        return b;
    }

    /**
     * HOLD_ONLY (2026-11-22, the seat's brief, PR 1): every occupied hotbar slot but the weapon's goes to the next EMPTY
     * main slot, in order, skipping the occupied ones. The fixture puts the weapon at 4 (not 0), occupied slots on both
     * sides of it, and an occupied main slot (index 1) between two empty ones, so a plan that moved the weapon, kept a
     * neighbour, or wrote into an occupied main slot could not pass.
     *
     * <p>Forces red: dropping {@code i == keep} (slot 4 gets moved); dropping the {@code !mainEmpty} skip (slot 2 lands
     * on inventory slot 10, which is full).
     */
    @Test
    void holdOnlyMovesEveryOtherHotbarItemIntoEmptyMainSlotsInOrder() {
        int[] dest = SelfTestStaging.hotbarOnlyPlan(only(0, 2, 4, 8), 4, mainEmpty(0, 2, 3, 20)).orElseThrow();
        assertEquals("[9, -1, 11, -1, -1, -1, -1, -1, 12]", java.util.Arrays.toString(dest));
    }

    /** A weapon from the main inventory ({@code keep -1}): slot 0 is emptied with the rest, so the caller can put it there. */
    @Test
    void holdOnlyWithTheWeaponInTheMainInventoryEmptiesSlotZeroToo() {
        int[] dest = SelfTestStaging.hotbarOnlyPlan(only(0, 3), -1, mainEmpty(5, 6)).orElseThrow();
        assertEquals("[14, -1, -1, 15, -1, -1, -1, -1, -1]", java.util.Arrays.toString(dest));
    }

    /**
     * Too few empty main slots: NOTHING is moved (empty plan), never a partial clear and never a drop. Three items, two
     * empty slots. Forces red: returning the partial plan.
     */
    @Test
    void holdOnlyRefusesWholeWhenTheMainInventoryCannotTakeEverything() {
        assertTrue(SelfTestStaging.hotbarOnlyPlan(only(1, 2, 3), -1, mainEmpty(0, 26)).isEmpty());
        assertTrue(SelfTestStaging.hotbarOnlyPlan(only(1, 2), -1, mainEmpty(0, 26)).isPresent(), "CONTROL: two fit");
    }
}
