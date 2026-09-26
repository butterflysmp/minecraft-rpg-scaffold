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
        assertEquals(30, NexusMenuLayout.ANVIL_SLOT,
                "the anvil is row 4, column 3 -- the cell VAULT_SLOT's javadoc held open for it, "
                        + "and the literal is what proves nothing moved to make room");
        assertEquals(31, NexusMenuLayout.CRAFTING_SLOT, "crafting is row 4, column 4");
        assertEquals(32, NexusMenuLayout.ENCHANT_SLOT, "enchanting is row 4, column 5");
        assertEquals(22, NexusMenuLayout.EQUIPMENT_SLOT,
                "the Equipment screen is row 3, column 5 -- the \"other features\" band, centred "
                        + "under the stats head (accessories Slice B)");
        assertEquals(21, NexusMenuLayout.BUILD_SLOT,
                "the Build screen is row 3, column 4 -- directly left of Equipment (PLAN-build-system 3.3)");
        assertEquals(NexusMenuLayout.EQUIPMENT_SLOT - 1, NexusMenuLayout.BUILD_SLOT,
                "Build sits IMMEDIATELY left of Equipment, in the same row");
        assertEquals(13, NexusMenuLayout.STATS_SLOT,
                "the stats head is slot 13 -- the CENTRE of row 2 (slots 9-17), Ben's ruling. It "
                        + "was 20 under a withdrawn rule that made it the start of a left-to-right "
                        + "run; where a SECOND feature goes is now UNRULED, see the constant");
        // Mutation MUTCLOSE: CLOSE_SLOT 49 -> 48   -> reddens HERE, and only here.
        // Mutation MUTSETTINGS: SETTINGS_SLOT 50 -> 51 -> reddens HERE, and only here.
        //
        // *** THIS ROW IS THE SOLE GUARD OF STATS_SLOT'S VALUE. MEASURED, AND IT CORRECTS A
        // PREDICTION MADE IN THE PLAN. ***
        //
        //   MUTSTATS19      STATS_SLOT 20 -> 19   -> reddens HERE, AND NOWHERE ELSE
        //   MUTSTATS21      STATS_SLOT 20 -> 21   -> reddens HERE, AND NOWHERE ELSE
        //   MUTSTATSBOTTOM  STATS_SLOT 20 -> 51   -> reddens here AND the body-rule row
        //
        // The plan predicted 19 and 21 would also redden the filler row. THEY DO NOT. The filler
        // row names STATS_SLOT symbolically throughout -- contains(STATS_SLOT), and the isButton
        // disjunction -- so moving the constant moves the code and the expectation TOGETHER and
        // bites nothing. That is the exact defect this file's first comment records about
        // LOCKED_SLOT and OFFHAND_SLOT, arriving a third time on a third constant.
        //
        // So: delete this assertion and STATS_SLOT can be moved to any other body slot with the
        // whole suite green. Only 51 is caught elsewhere, and only because it leaves the body.
        // ALL APPLIED AND MEASURED, not expected -- see the PR body.
    }

    @Test
    void theStatsHeadIsInTheBODY_notInTheChromeRowWithTheTwoButtons() {
        // THE LAYOUT RULE, GUARDED RATHER THAN ONLY JAVADOC'D. Without it, "slot 13" is a number
        // someone picked and the next person adding a feature has nothing to object to at 51.
        //
        // REWRITTEN FOR BEN'S RULING. It used to assert only "not in the chrome row", which was the
        // whole of the withdrawn left-to-right rule's content. The live rule is stronger and says
        // something checkable: the head is the MIDDLE CELL OF ROW 2.
        assertTrue(NexusMenuLayout.STATS_SLOT < NexusMenuLayout.BOTTOM_ROW_START,
                "a FEATURE must not sit in the chrome row");
        assertFalse(NexusMenuLayout.STATS_SLOT == NexusMenuLayout.CLOSE_SLOT
                        || NexusMenuLayout.STATS_SLOT == NexusMenuLayout.SETTINGS_SLOT,
                "and it must not collide with either button");

        // THE HEADER BAND. Row 2 is indices 9-17, and the head is centred in it.
        assertEquals(1, NexusMenuLayout.STATS_SLOT / 9,
                "row 2 -- the HEADER band. The player head is not the first feature; it is the "
                        + "header, which is why it sits alone");
        assertEquals(4, NexusMenuLayout.STATS_SLOT % 9,
                "and centred in it: a nine-wide row's middle cell is column 4, counting from 0");

        // Mutation MUTSTATSBOTTOM: STATS_SLOT -> 51 -> kill set RECORDED in the PR body.
        //
        // THIS ROW STOPPED BEING A TRIPWIRE. It previously carried a note saying it would go red
        // the moment a second feature arrived, because the rule for that was unruled. IT IS RULED:
        // the hub is BANDS, each with a meaning, so a second feature goes in the band its KIND
        // names and this row is untouched by it. See the band table on STATS_SLOT.
    }

    @Test
    void theHUBISBANDS_andEachBandHasAMeaningRatherThanAFillOrder() {
        // THE RULE, GUARDED. Without it the band table is prose and the next person adding a
        // feature has nothing mechanical objecting when they drop it in the header row beside the
        // player head -- which is the one placement the bands exist to prevent.
        //
        //   row 2  ( 9-17)  HEADER -- the player head alone
        //   row 3  (18-26)  other features
        //   row 4  (27-35)  crafting-type menus
        //   row 5  (36-44)  unassigned
        //   row 6  (45-53)  chrome
        assertEquals(1, NexusMenuLayout.STATS_SLOT / 9, "the head is the HEADER band, row 2");
        assertEquals(3, NexusMenuLayout.CRAFTING_SLOT / 9,
                "crafting is a crafting-type menu, so row 4 -- its KIND picked its band");
        assertEquals(3, NexusMenuLayout.ENCHANT_SLOT / 9, "and so is enchanting");
        assertEquals(5, NexusMenuLayout.CLOSE_SLOT / 9, "Close is chrome, row 6");
        assertEquals(5, NexusMenuLayout.SETTINGS_SLOT / 9, "Settings is chrome, row 6");

        // THE STATIONS ARE A CONTIGUOUS RUN, so a later one cannot silently split it.
        // Same claim theTwoButtonsAreADJACENT makes about Close and Settings, and for the reason
        // that row records: literals that happen to sit together are not literals that must.
        //
        // THE THIRD STATION ARRIVED AND THIS ROW DID NOT REDDEN, which is why it is EXTENDED here
        // rather than left. 33 kept 31 and 32 adjacent, so the pair assertion stayed green while
        // its comment -- "so a third cannot silently split them" -- stopped describing anything
        // the row checked. A row whose comment outlives what it asserts is the stale-prose defect
        // with a green tick beside it.
        //
        // *** THE RUN IS NOW COMPLETE AT 29-33, AND THE ANVIL CLOSED THE LAST GAP. *** The vault
        // sat at 29 with a HOLE at 30 between it and crafting, held open by VAULT_SLOT's javadoc
        // against exactly this slice. The chain below is walked end to end rather than in pairs, so
        // a sixth station cannot be dropped on either end without reddening here.
        assertEquals(1, NexusMenuLayout.ANVIL_SLOT - NexusMenuLayout.VAULT_SLOT,
                "the anvil sits immediately right of the vault -- the gap at 30 is closed");
        assertEquals(1, NexusMenuLayout.CRAFTING_SLOT - NexusMenuLayout.ANVIL_SLOT,
                "and crafting immediately right of the anvil");
        assertEquals(1, NexusMenuLayout.ENCHANT_SLOT - NexusMenuLayout.CRAFTING_SLOT,
                "enchanting sits immediately right of crafting");
        assertEquals(1, NexusMenuLayout.GRINDSTONE_SLOT - NexusMenuLayout.ENCHANT_SLOT,
                "and the grindstone immediately right of enchanting");
        assertEquals(NexusMenuLayout.CRAFTING_SLOT / 9, NexusMenuLayout.ENCHANT_SLOT / 9,
                "adjacent IN A ROW -- consecutive indices can straddle a row boundary");
        assertEquals(NexusMenuLayout.CRAFTING_SLOT / 9, NexusMenuLayout.GRINDSTONE_SLOT / 9,
                "all of them in ONE row, for the same reason");
        assertEquals(NexusMenuLayout.CRAFTING_SLOT / 9, NexusMenuLayout.VAULT_SLOT / 9,
                "including the left-hand end of the run");
        assertEquals(NexusMenuLayout.CRAFTING_SLOT / 9, NexusMenuLayout.ANVIL_SLOT / 9,
                "and the anvil, which is the cell that completed it");
        assertEquals(3, NexusMenuLayout.GRINDSTONE_SLOT / 9,
                "and that row is the crafting-type band, row 4 -- their KIND picked it");

        // AND THE BANDS DO NOT OVERLAP, which is the half that would otherwise be vacuous: a table
        // of bands that all resolved to the same row would satisfy every assertion above.
        assertTrue(NexusMenuLayout.STATS_SLOT / 9 < NexusMenuLayout.CLOSE_SLOT / 9,
                "the HEADER band is above the CHROME band -- a player reads the screen downwards");
        assertEquals(45, NexusMenuLayout.BOTTOM_ROW_START,
                "and the chrome band starts at 45, which is row 6 index 0");
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
        assertFalse(NexusMenuLayout.FILLER_SLOTS.contains(NexusMenuLayout.STATS_SLOT),
                "filler must never cover the stats head -- a pane painted over it would hide a "
                        + "working readout behind a black square, and nothing would say so");

        assertFalse(NexusMenuLayout.FILLER_SLOTS.contains(NexusMenuLayout.ANVIL_SLOT),
                "nor the anvil");
        assertFalse(NexusMenuLayout.FILLER_SLOTS.contains(NexusMenuLayout.CRAFTING_SLOT),
                "nor the crafting station");
        assertFalse(NexusMenuLayout.FILLER_SLOTS.contains(NexusMenuLayout.ENCHANT_SLOT),
                "nor the enchanting station");
        assertFalse(NexusMenuLayout.FILLER_SLOTS.contains(NexusMenuLayout.GRINDSTONE_SLOT),
                "nor the grindstone");

        // AND IT COVERS EVERYTHING ELSE -- the other half, without which the assertions above are
        // equally consistent with FILLER_SLOTS being empty and the whole screen rendering blank.
        //
        // THE CARDINALITY CAUGHT THE TWO NEW STATIONS BEFORE ANY OTHER ROW DID, which is what it is
        // for: 51 against 49. The per-slot loop below would have caught it too, but the count is
        // what fails with a number a reader can act on.
        //
        // IT DID IT AGAIN FOR THE THIRD STATION -- 49 against 48 -- and that was the ONLY row that
        // reddened when the grindstone was added, exactly as designed.
        //
        // AND AGAIN FOR THE ANVIL -- 47 against 46. Three stations, three times this row was the
        // one that said so with a number.
        //
        // *** THE SUBTRAHEND STAYS A LITERAL, AND WRITING IT AS PAINTED_SLOTS.size() WAS TRIED AND
        // REVERTED IN THE SAME SLICE. *** That form spares an edit here every time a station lands,
        // and it does so by making this row assert
        //
        //     SIZE - PAINTED.size() == FILLER.size()
        //
        // which is the partition row below, rearranged. Two rows, one claim, and the cheap one
        // deleted: the literal is what forces somebody to look at a NUMBER when the screen's shape
        // changes, and it is the only row here that can disagree with PAINTED_SLOTS at all.
        //
        // The count and the noun move together or this comment lies, which is why the message says
        // neither. See the partition row for the paint list's own size.
        assertEquals(NexusMenuLayout.SIZE - 10, NexusMenuLayout.FILLER_SLOTS.size(),
                "every slot except the two buttons, the head, the stations, Equipment and Build is filler");
        for (int slot = 0; slot < NexusMenuLayout.SIZE; slot++) {
            boolean isButton = slot == NexusMenuLayout.CLOSE_SLOT
                    || slot == NexusMenuLayout.SETTINGS_SLOT
                    || slot == NexusMenuLayout.STATS_SLOT
                    || slot == NexusMenuLayout.VAULT_SLOT
                    || slot == NexusMenuLayout.ANVIL_SLOT
                    || slot == NexusMenuLayout.CRAFTING_SLOT
                    || slot == NexusMenuLayout.ENCHANT_SLOT
                    || slot == NexusMenuLayout.GRINDSTONE_SLOT
                    || slot == NexusMenuLayout.EQUIPMENT_SLOT
                    || slot == NexusMenuLayout.BUILD_SLOT;
            assertEquals(!isButton, NexusMenuLayout.FILLER_SLOTS.contains(slot),
                    "slot " + slot + " filler membership");
        }
        // Mutation MUTFILLER: drop the slots.remove(CLOSE_SLOT) line -> reddens on the first
        // assertion AND on the size. APPLIED AND MEASURED.
        // Mutation MUTFILLERSTATS: drop slots.remove(STATS_SLOT) -> reddens HERE, and only here.
        // APPLIED AND MEASURED. This row is the sole guard of that line.
        //
        // NOTE, because it is the other half of the literal row's finding: this row is BLIND to
        // STATS_SLOT's VALUE. It names the constant symbolically everywhere, so it passes for any
        // value the remove() line is given. It guards that the head is EXCLUDED from filler, never
        // WHERE the head is.
    }

    @Test
    void everySlotNotInTheFILLERIsDECLAREDAsPainted_theInvariantThatShippedAHole() {
        // *** ADDED BECAUSE A SUBTRACTED CELL WITH NO PAINTER REACHED A SCREENSHOT. ***
        //
        // GRINDSTONE_SLOT was removed from FILLER_SLOTS and then painted by nothing. Slot 33 was an
        // invisible, clickable hole whose click handler worked perfectly -- so every behavioural
        // check passed and the only symptom was a gap on screen.
        //
        // NO EXISTING ASSERTION COULD SEE IT. The per-slot loop above asserts filler membership
        // against a hand-written isButton expression, and that expression was updated in the same
        // edit that added the removal -- both halves moved together, and neither is the paint list.
        //
        // THE INVARIANT: FILLER_SLOTS and PAINTED_SLOTS partition the screen. Every cell is filler
        // or is declared as needing a painter; none is both and none is neither.
        for (int slot = 0; slot < NexusMenuLayout.SIZE; slot++) {
            boolean filler = NexusMenuLayout.FILLER_SLOTS.contains(slot);
            boolean painted = NexusMenuLayout.PAINTED_SLOTS.contains(slot);
            assertTrue(filler ^ painted,
                    "slot " + slot + " must be EXACTLY ONE of filler or painted -- it is "
                            + (filler ? "both" : "neither"));
        }
        assertEquals(NexusMenuLayout.SIZE,
                NexusMenuLayout.FILLER_SLOTS.size() + NexusMenuLayout.PAINTED_SLOTS.size(),
                "the two sets must cover the screen exactly once");

        // AND THE PAINT LIST IS NOT EMPTY, without which the partition is satisfied by "everything
        // is filler" -- the blank-screen reading the cardinality row above also guards against.
        assertEquals(10, NexusMenuLayout.PAINTED_SLOTS.size(),
                "Close, Settings, the head, the five stations -- vault, anvil, crafting, "
                        + "enchanting, grindstone -- Equipment and Build");
        // Mutation: drop GRINDSTONE_SLOT from PAINTED_SLOTS -> slot 33 is neither -> reddens.
        // THAT MUTATION IS THE SHIPPED DEFECT, and nothing in this file reddened on it before.
    }
}
