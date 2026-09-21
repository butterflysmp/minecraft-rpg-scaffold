package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The anvil's geometry: two inputs, one preview, and chrome.
 *
 * <p>{@code AnvilMenu} needs a live {@code Player} and {@code Bukkit.createInventory}, so the
 * PAINTING is boot-gate-only. The geometry is pure and is here.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilMenuLayoutTest {

    @Test
    void theSlotsArePINNEDToTheirLITERALS_notNamedSymbolically() {
        // *** LITERALS, for NexusMenuLayoutTest's reason. *** A constant that is only ever named
        // symbolically is a constant with no guard at all: a mutation moves the code and every
        // expectation together and bites nothing.
        assertEquals(54, AnvilMenuLayout.SIZE, "six rows, like every other station screen");
        assertEquals(4, AnvilMenuLayout.INFO_SLOT,
                "the hint is top-centre -- slot 4 is INFORMATION on every screen that uses it, and "
                        + "reusing it for an action would make one cell mean two things");

        // BEN'S RULING: 20 / 22 / 24, centred on 22, the screen's midline, one gap between each.
        assertEquals(20, AnvilMenuLayout.TARGET_SLOT, "the item being upgraded");
        assertEquals(22, AnvilMenuLayout.DONOR_SLOT, "the item sacrificed");
        assertEquals(24, AnvilMenuLayout.OUTPUT_SLOT, "the preview");

        assertEquals(48, AnvilMenuLayout.BACK_SLOT, "Back is 48 on every screen");
        assertEquals(49, AnvilMenuLayout.CLOSE_SLOT, "and Close is 49 on every screen");
    }

    @Test
    void theTHREECellsAreEVENLYSpacedOnONERow_whichIsWhatMakesThemReadLeftToRight() {
        // The gaps are the point: three adjacent item icons read as a row of inventory, and the
        // player has to work out which is which. One empty cell between each is what makes the
        // screen say "this, then this, gives this".
        assertEquals(2, AnvilMenuLayout.DONOR_SLOT - AnvilMenuLayout.TARGET_SLOT,
                "one cell between the target and the donor");
        assertEquals(2, AnvilMenuLayout.OUTPUT_SLOT - AnvilMenuLayout.DONOR_SLOT,
                "and one between the donor and the preview");

        // ALL THREE IN ONE ROW -- consecutive-ish indices can straddle a row boundary, and a
        // preview that wrapped onto the next row would read as a separate thing entirely.
        assertEquals(AnvilMenuLayout.TARGET_SLOT / 9, AnvilMenuLayout.DONOR_SLOT / 9,
                "the target and the donor share a row");
        assertEquals(AnvilMenuLayout.TARGET_SLOT / 9, AnvilMenuLayout.OUTPUT_SLOT / 9,
                "and so does the preview");

        // AND THE DONOR IS THE MIDLINE. Row 2 is slots 18-26, whose centre is 22 -- Ben's ruling,
        // pinned as arithmetic rather than as a third literal so it says WHY 22.
        assertEquals(22, (18 + 26) / 2, "row 3 runs 18-26, so its centre is 22");
        assertEquals(22, AnvilMenuLayout.DONOR_SLOT, "and the donor sits on it");
    }

    /**
     * *** THE PREVIEW IS NOT AN INPUT SLOT, AND THAT IS THE WHOLE SAFETY ARGUMENT. ***
     *
     * <p>{@code Menu.returnEverything} iterates {@code returnedSlots()}, which defaults to
     * {@code inputSlots()}. A preview listed there would be handed to the player on <b>every close,
     * death, disconnect and shutdown</b> -- a duplicate of their own item, minted free, once per
     * screen close. {@code CraftingMenuLayout.RESULT_SLOT} records the same rule.
     */
    @Test
    void theOUTPUTSlotIsNOTAnInputSlot_soTheCloseCannotHandThePlayerAFreeCopy() {
        assertFalse(AnvilMenuLayout.INPUT_SLOTS.contains(AnvilMenuLayout.OUTPUT_SLOT),
                "THE PREVIEW MUST NEVER BE AN INPUT SLOT. Listing it here makes returnEverything "
                        + "give the player a free copy of their own item on every close.");

        // AND THE TWO REAL INPUTS ARE, so the row above is not satisfied by an empty set.
        assertTrue(AnvilMenuLayout.INPUT_SLOTS.contains(AnvilMenuLayout.TARGET_SLOT));
        assertTrue(AnvilMenuLayout.INPUT_SLOTS.contains(AnvilMenuLayout.DONOR_SLOT));
        assertEquals(2, AnvilMenuLayout.INPUT_SLOTS.size(),
                "exactly two cells take an item -- a third would be a slot nothing reads");

        // NOR IS IT FILLER, which is the other way it could go wrong: painted over by the render
        // loop, which would erase the readout every time the screen was drawn.
        assertFalse(AnvilMenuLayout.FILLER_SLOTS.contains(AnvilMenuLayout.OUTPUT_SLOT),
                "the preview is painted individually, so filler must not cover it");
    }

    /**
     * *** THE FOUR SETS PARTITION THE SCREEN, ON BOTH ORIGINS. ***
     *
     * <p>Copied wholesale from {@code GrindstoneMenuLayoutTest}, because it catches both ways a
     * screen of this shape goes wrong and neither is visible to an assertion that checks one set at
     * a time:
     *
     * <pre>
     *   AN UNPAINTED CELL            a slot in NONE of them -- the invisible clickable hole that
     *                                shipped on the hub at 33 and on the grindstone at 48.
     *   A RENDER THAT REACHES AN     a slot in TWO of them -- the repaint that paints filler over
     *   INPUT                        a player's gear and destroys it.
     * </pre>
     */
    @Test
    void theFOURSetsPARTITIONTheScreen_onBothOrigins() {
        for (boolean fromHub : new boolean[] {true, false}) {
            for (int slot = 0; slot < AnvilMenuLayout.SIZE; slot++) {
                int roles = 0;
                if (AnvilMenuLayout.FILLER_SLOTS.contains(slot)) roles++;
                if (AnvilMenuLayout.INPUT_SLOTS.contains(slot)) roles++;
                if (AnvilMenuLayout.statusSlots(fromHub).contains(slot)) roles++;
                if (AnvilMenuLayout.chromeSlots(fromHub).contains(slot)) roles++;
                assertEquals(1, roles, "slot " + slot + " (fromHub=" + fromHub
                        + ") must belong to EXACTLY ONE set, not " + roles);
            }
            assertEquals(AnvilMenuLayout.SIZE,
                    AnvilMenuLayout.FILLER_SLOTS.size()
                            + AnvilMenuLayout.INPUT_SLOTS.size()
                            + AnvilMenuLayout.statusSlots(fromHub).size()
                            + AnvilMenuLayout.chromeSlots(fromHub).size(),
                    "the four sets cover the screen exactly once, fromHub=" + fromHub);
        }

        // THE FILLER DOES NOT FORK: the whole bottom row is bar-or-chrome on both paths, nine cells
        // either way. 54 - 2 inputs - 1 preview - 9 bottom row - 1 info = 41.
        assertEquals(41, AnvilMenuLayout.FILLER_SLOTS.size(),
                "54 - 2 inputs - preview - 9 bottom row - info = 41, on both origins");
    }

    @Test
    void theBARForksWithTheCHROME_sevenCellsFromTheHubAndEIGHTFromABlock() {
        // From a block there is no Back button, so 48 is READOUT rather than chrome. Ben overturned
        // seven-on-both-origins for the grindstone after seeing a black pane sitting in the middle
        // of a row of coloured ones; this screen inherits that ruling rather than re-deriving it.
        assertEquals(7, AnvilMenuLayout.statusSlots(true).size(), "the bottom row less Back and Close");
        assertEquals(8, AnvilMenuLayout.statusSlots(false).size(), "the bottom row less Close only");

        assertFalse(AnvilMenuLayout.statusSlots(true).contains(AnvilMenuLayout.BACK_SLOT),
                "from the hub, 48 is a BUTTON and the bar must not paint over it");
        assertTrue(AnvilMenuLayout.statusSlots(false).contains(AnvilMenuLayout.BACK_SLOT),
                "from a block, 48 is BAR -- otherwise it is a hole in the readout");

        // CLOSE IS NEVER BAR, on either origin. The one button a player cannot do without.
        assertFalse(AnvilMenuLayout.statusSlots(true).contains(AnvilMenuLayout.CLOSE_SLOT));
        assertFalse(AnvilMenuLayout.statusSlots(false).contains(AnvilMenuLayout.CLOSE_SLOT));

        // AND THE CHROME IS THE COMPLEMENT: Back is chrome only where the bar does not take 48.
        assertTrue(AnvilMenuLayout.chromeSlots(true).contains(AnvilMenuLayout.BACK_SLOT));
        assertFalse(AnvilMenuLayout.chromeSlots(false).contains(AnvilMenuLayout.BACK_SLOT));
        assertTrue(AnvilMenuLayout.chromeSlots(true).contains(AnvilMenuLayout.OUTPUT_SLOT),
                "the preview is painted individually on both origins");
        assertTrue(AnvilMenuLayout.chromeSlots(false).contains(AnvilMenuLayout.OUTPUT_SLOT));
    }

    /**
     * *** SLOT 31 IS 13b's CONFIRM CELL AND IT MUST BE ORDINARY FILLER TODAY. ***
     *
     * <p>Ben has ruled where the confirm button goes. Reserving it in CODE -- a constant subtracted
     * from the filler with nothing painting it -- is the hub's own shipped defect: <b>an invisible,
     * clickable hole whose click handler worked perfectly</b>, at slot 33, which reached a
     * screenshot. This row is what stops the reservation being half-implemented.
     */
    @Test
    void theCellRESERVEDFor13bIsPlainFILLER_becauseAReservationWithNoPainterIsAHole() {
        assertTrue(AnvilMenuLayout.FILLER_SLOTS.contains(31),
                "slot 31 is 13b's confirm cell. Until 13b paints it, it must be FILLER -- a cell "
                        + "subtracted from the filler and painted by nothing is an invisible, "
                        + "clickable hole, which is exactly what shipped on the hub at slot 33.");
        assertFalse(AnvilMenuLayout.INPUT_SLOTS.contains(31), "and it takes no item today");
    }

    @Test
    void noTWOSlotConstantsCOLLIDE_soNoCellIsQuietlyDoingTwoJobs() {
        // Two constants sharing a value would make one cell mean two things, and every row above
        // that names them symbolically would stay green. Same shape as the distinctness row in
        // NexusStationGateTest, and for the same reason.
        Set<Integer> cells = new HashSet<>();
        int checked = 0;
        for (int cell : new int[] {AnvilMenuLayout.INFO_SLOT, AnvilMenuLayout.TARGET_SLOT,
                AnvilMenuLayout.DONOR_SLOT, AnvilMenuLayout.OUTPUT_SLOT,
                AnvilMenuLayout.BACK_SLOT, AnvilMenuLayout.CLOSE_SLOT}) {
            cells.add(cell);
            checked++;
        }
        assertEquals(6, checked, "the sweep has to have actually run");
        assertEquals(6, cells.size(), "six named cells, six distinct values: " + cells);

        // AND EVERY ONE IS ON THE SCREEN.
        for (int cell : cells) {
            assertTrue(cell >= 0 && cell < AnvilMenuLayout.SIZE, "slot " + cell + " is on screen");
        }
    }
}
