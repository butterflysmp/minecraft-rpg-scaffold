package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE INFO ITEM AT THE TOP RIGHT ON EVERY SCREEN THAT HAS ONE (Ben, 2026-11-23, verbatim: "Move the Enchantment Table info
 * item to the top right corner and move the bookshelf to the left of that. Make the rest of the nexus menu sub menus and
 * our custom UIs consistent with that too"). The seat's ruling: move each screen's info item to row 1, column 9 (index
 * 8); where something else lives there, a screen has no info item, or it is not nine wide, leave it and list it for Ben.
 *
 * <p>MOVED: the enchanting table (PR 2), the anvil, the grindstone and the crafting screen. THIS FILE IS THE SOLE GUARD of crafting's index: CraftingMenuLayoutTest never pinned INDICATOR_SLOT's value (MUTCRAFT, 2026-11-23, reddened only this file). Each was at 4 (top-centre);
 * index 8 was filler on all four (READ: no layout constant held 8, and no menu draws a literal 8).
 *
 * <p>LEFT FOR BEN, unmoved (the seat's "DO NOT GUESS"): the Level screen's summary at 4 (a readout of his level, not an
 * info item); the Build picker's title at 4 (names what is being picked); the vault (no info item; 8 is its NEXT page
 * arrow); the recipe book (no info item; 8 is a recipe entry); the hub, Build, Equipment, the slot picker and Settings
 * (no info item). Every screen is 54 slots, nine wide.
 */
class InfoTopRightTest {

    private static final int TOP_RIGHT = 8;

    @Test
    void everyMovedScreensInfoItemIsTheTopRightCorner() {
        Map<String, Integer> moved = Map.of(
                "enchanting table", EnchantMenuLayout.INFO_SLOT,
                "anvil", AnvilMenuLayout.INFO_SLOT,
                "grindstone", GrindstoneMenuLayout.INFO_SLOT,
                "crafting", CraftingMenuLayout.INDICATOR_SLOT);
        moved.forEach((screen, slot) -> assertEquals(TOP_RIGHT, slot, screen));
        // Row 1, column 9, by the javadoc's own formula, so the literal is checked against the prose.
        assertEquals((1 - 1) * 9 + (9 - 1), TOP_RIGHT);
    }

    /** THE CONTROL: a screen listed for Ben stays where it was, so this file can tell a move from a sweep. */
    @Test
    void anUnmovedScreenIsLeftWhereItWas() {
        assertEquals(4, LevelMenuLayout.SUMMARY_SLOT, "the Level summary is listed for Ben, not moved");
        assertEquals(4, BuildMenuLayout.PICKER_TITLE_SLOT, "the Build picker's title is listed for Ben, not moved");
        assertEquals(8, NexusVaultLayout.NEXT_SLOT, "the vault's NEXT arrow keeps the top right corner");
    }

    /** Every screen's Back and Close stay where they are. */
    @Test
    void backAndCloseDoNotMove() {
        for (int close : new int[] {AnvilMenuLayout.CLOSE_SLOT, GrindstoneMenuLayout.CLOSE_SLOT, CraftingMenuLayout.CLOSE_SLOT,
                EnchantMenuLayout.CLOSE_SLOT}) {
            assertEquals(49, close);
        }
        for (int back : new int[] {AnvilMenuLayout.BACK_SLOT, GrindstoneMenuLayout.BACK_SLOT, CraftingMenuLayout.BACK_SLOT,
                EnchantMenuLayout.BACK_SLOT}) {
            assertEquals(48, back);
        }
    }
}
