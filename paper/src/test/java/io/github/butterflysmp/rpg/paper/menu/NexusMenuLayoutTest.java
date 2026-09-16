package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Nexus hub's layout: two buttons and filler, and the three things that can go wrong with that.
 *
 * <p>{@code NexusMenu} itself needs a live {@code Player} and {@code Bukkit.createInventory}, so it
 * is boot-gate-only -- {@code GATE-nexus.md}'s slice 2 rows. The layout is pure and is tested here,
 * the same trade {@code RecipeBrowserLayout} and {@code CraftingMenuLayout} make.
 *
 * <p>Each test names the mutation it forces red.
 */
class NexusMenuLayoutTest {

    @Test
    void theButtonSlotsArePINNEDToTheirLITERALS_notNamedSymbolically() {
        // THE LITERALS, ASSERTED AS LITERALS. THIS FILE HAS BEEN BITTEN BY THE OTHER CHOICE TWICE
        // AND THIS IS THE ROW THAT STOPS A THIRD.
        //
        // NexusLock.LOCKED_SLOT 8 -> 7 and NexusLock.OFFHAND_SLOT 40 -> 39 both APPLIED CLEANLY and
        // KILLED NOTHING, because every row referring to them named them symbolically -- so the
        // mutation moved the code and the expectation together and bit nothing. A constant that is
        // only ever named symbolically is a constant with no guard at all.
        assertEquals(49, NexusMenuLayout.CLOSE_SLOT,
                "Close is slot 49 -- the same slot the crafting menu uses, so the button does not "
                        + "move between our screens");
        assertEquals(50, NexusMenuLayout.SETTINGS_SLOT, "Settings is slot 50");
        assertEquals(54, NexusMenuLayout.SIZE, "six rows");
        // Mutation MUTCLOSE: CLOSE_SLOT 49 -> 48   -> reddens HERE, and only here.
        // Mutation MUTSETTINGS: SETTINGS_SLOT 50 -> 51 -> reddens HERE, and only here.
        // Both APPLIED AND MEASURED, not expected -- see the PR body.
    }

    @Test
    void theTwoButtonsAreADJACENTInTheBottomRow() {
        // THIS ROW HAS NO UNIQUE KILL, AND IT IS NOT THEREBY REMOVABLE. MEASURED, NOT ASSUMED.
        //
        //   MUTCLOSE      CLOSE_SLOT    49 -> 48   -> reddens THIS row AND the literal row
        //   MUTSETTINGS   SETTINGS_SLOT 50 -> 51   -> reddens THIS row AND the literal row
        //   MUTFILLER     drop remove(CLOSE_SLOT)  -> reddens the filler row ONLY
        //
        // Every mutation that reddens this row also reddens the literal row, so a coverage sweep
        // would mark this one redundant. IT IS NOT, and the reason is that no mutation CAN kill it
        // alone: the literal row pins both constants exactly, so any change to either necessarily
        // fails there first. Emptiness of the unique-kill set is a property of the pair, not
        // evidence about this row.
        //
        // WHAT IS LOST IF IT GOES: the literal row records WHICH SLOTS. This one records that they
        // must stay TOGETHER -- the part that survives the next re-ruling of the numbers. Move the
        // pair to 48/49 for a third button and the literal row is simply rewritten; without this
        // row, nothing would then object to 48/52.
        //
        // The operator predicted each constant mutation would redden exactly one row. It is two,
        // and it cannot be one: an assertion over a RELATION between two constants cannot be blind
        // to either of them moving. Asserting adjacency and "exactly one kill each" are not jointly
        // satisfiable, and adjacency is the one that was asked for.
        //
        // The operator's layout ruling is "49 and 50", which is two literals that HAPPEN to sit
        // beside each other. This asserts the RELATION, so moving the pair together later -- to
        // make room for a third button -- cannot silently split them across a row boundary.
        assertEquals(1, NexusMenuLayout.SETTINGS_SLOT - NexusMenuLayout.CLOSE_SLOT,
                "Settings sits immediately right of Close");

        assertTrue(NexusMenuLayout.CLOSE_SLOT >= NexusMenuLayout.BOTTOM_ROW_START
                        && NexusMenuLayout.SETTINGS_SLOT < NexusMenuLayout.SIZE,
                "both buttons are in the bottom row");

        // SAME ROW, not merely consecutive. 53 and 54 differ by one and are in different rows --
        // and 44/45 is the pair that would actually be reached by sliding the buttons left.
        assertEquals(NexusMenuLayout.CLOSE_SLOT / 9, NexusMenuLayout.SETTINGS_SLOT / 9,
                "adjacent IN A ROW -- consecutive slot numbers can straddle a row boundary");
        // Mutation: move either constant to the far end of the row -> reddens.
    }

    @Test
    void theFillerEXCLUDESBothButtons_soNeitherCanBePaintedOver() {
        // A FILLER PANE PAINTED OVER A LIVE BUTTON IS INVISIBLE UNTIL SOMEONE CLICKS IT, and over
        // CLOSE_SLOT it makes the menu unclosable except with Esc. CraftingMenuLayout.STATUS_SLOTS
        // carries this same rule for the same reason; it is not left to a render loop remembering
        // to skip two indices.
        assertFalse(NexusMenuLayout.FILLER_SLOTS.contains(NexusMenuLayout.CLOSE_SLOT),
                "filler must never cover Close");
        assertFalse(NexusMenuLayout.FILLER_SLOTS.contains(NexusMenuLayout.SETTINGS_SLOT),
                "filler must never cover Settings");

        // AND IT COVERS EVERYTHING ELSE -- the other half, without which the assertions above are
        // equally consistent with FILLER_SLOTS being empty and the whole screen rendering blank.
        assertEquals(NexusMenuLayout.SIZE - 2, NexusMenuLayout.FILLER_SLOTS.size(),
                "every slot except the two buttons is filler");
        for (int slot = 0; slot < NexusMenuLayout.SIZE; slot++) {
            boolean isButton = slot == NexusMenuLayout.CLOSE_SLOT
                    || slot == NexusMenuLayout.SETTINGS_SLOT;
            assertEquals(!isButton, NexusMenuLayout.FILLER_SLOTS.contains(slot),
                    "slot " + slot + " filler membership");
        }
        // Mutation MUTFILLER: drop the slots.remove(CLOSE_SLOT) line -> reddens on the first
        // assertion AND on the size. APPLIED AND MEASURED.
    }
}
