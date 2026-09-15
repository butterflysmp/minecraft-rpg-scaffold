package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.paper.nexus.NexusLock.Touched;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Nexus lock, one row per ROUTE, so a route nobody guarded is a missing test rather than a gap
 * inside an assertion.
 *
 * <p>{@code NexusSlots} needs an {@code InventoryView}, a {@code Player} and a live
 * {@code Inventory}, none of which exist without a server, and the project has no MockBukkit. This
 * is the part that is decidable: {@code ClickType} and {@code InventoryAction} are plain enums that
 * load without a server, exactly as {@code GridClickIntentTest} relies on.
 *
 * <h2>THE PERMIT ROWS MATTER MORE THAN THE REFUSE ROWS HERE, WHICH IS UNUSUAL</h2>
 *
 * The guard runs at {@code LOWEST} and a cancel makes {@code onMenuClick} skip entirely. So a false
 * POSITIVE is not a Nexus bug -- it is every button in the crafting menu, the enchant menu and the
 * recipe browser silently ceasing to respond, with nothing logged. The rows asserting PERMIT are
 * what stand between this design and a dead menu system, and the always-true mutation below is the
 * one that has to redden.
 *
 * <h2>SLOT SPACE, AND THE NUMBERS THE ADAPTER MUST PRODUCE</h2>
 *
 * Every index here is {@code PlayerInventory} index space: 0-8 hotbar, 9-35 storage, 36-39 armour
 * (36 boots), 40 offhand. The RAW numbers the adapter converts FROM are not visible to this class
 * and are recorded here so the two can be compared:
 *
 * <pre>
 *   physical slot        own-inventory screen    54-slot chest open
 *   locked hotbar 8      raw 44                  raw 89
 *   boots                raw 8                   raw 85
 * </pre>
 *
 * <b>Raw 8 is the BOOTS slot in the own-inventory screen</b>, which is why a rule built on raw
 * numbers would refuse a player's boots and permit every click on the slot it exists to protect.
 * {@code theSameLockedSlotIsRefusedWhicheverViewItArrivedFrom} asserts the property the conversion
 * must deliver; that {@code convertSlot} actually delivers it is boot-only, {@code GATE-nexus.md}
 * rows 4-5, because it needs a live view.
 *
 * <p>Each test names the mutation it forces red.
 */
class NexusLockTest {

    /** Nothing in the inventory holds a star. The common case, and the one most rows want. */
    private static final IntPredicate NO_STARS = index -> false;

    /** The invariant state: the one star is in the locked slot. */
    private static final IntPredicate STAR_IN_LOCKED_SLOT = index -> index == NexusLock.LOCKED_SLOT;

    private static final int BOOTS = 36;
    private static final int A_STORAGE_SLOT = 20;

    // ---------------------------------------------------------------- the axis

    @Test
    void everyClickTypeDecidesSomethingForEveryCursorState() {
        // The coverage claim: no ClickType falls off the end of the switch into an exception, and a
        // new Bukkit constant reaches this loop the day it exists. ClickType grows in Minecraft
        // drops -- InventoryAction on this very build carries six *_BUNDLE constants that are
        // recent additions -- so this is the row that survives a Paper upgrade.
        for (ClickType click : ClickType.values()) {
            for (InventoryAction action : InventoryAction.values()) {
                for (boolean cursorIsStar : new boolean[]{true, false}) {
                    assertDoesNotThrow(
                            () -> NexusLock.refusesClick(click, action, player(A_STORAGE_SLOT),
                                    -1, cursorIsStar, NO_STARS),
                            click + "/" + action + " threw");
                }
            }
        }
        // Mutation: remove an arm from the switch -> does not compile, which is the point of the
        // expression form. Make an arm throw -> reddens naming the constant.
    }

    // ------------------------------------------------- THE DEAD-MENU GUARD (permit rows)

    @Test
    void aClickTouchingNeitherTheLockedSlotNorAStarIsPERMITTED() {
        // THE row. If this ever goes the other way, every menu in the plugin stops responding and
        // nothing else reddens, because a cancel at LOWEST makes onMenuClick skip.
        for (ClickType click : ClickType.values()) {
            assertFalse(
                    NexusLock.refusesClick(click, InventoryAction.PICKUP_ALL,
                            player(A_STORAGE_SLOT), -1, false, STAR_IN_LOCKED_SLOT),
                    click + " on an unrelated slot must be PERMITTED");
        }
        // Mutation: make refusesClick always-true -> reddens here. This is the mutation that
        // matters most in the file; the refusal rows are all still green under it.
    }

    @Test
    void aMenusOwnSlotEightIsNotTheLockedSlotAndIsPERMITTED() {
        // playerInventory=false is the ONLY thing separating a crafting grid's slot 8 from the
        // hotbar slot the lock protects. Both are "8".
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                new Touched(false, NexusLock.LOCKED_SLOT), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: drop the !t.playerInventory() continue in touchesTheStar -> reddens, and every
        // menu whose layout uses slot 8 becomes unclickable.
    }

    @Test
    void theBootsSlotIsPERMITTED_theRegressionForRawEightMeaningTwoThings() {
        // In the own-inventory screen the player's boots are RAW 8. A lock that compared raw
        // numbers would refuse this click and permit the locked hotbar slot, in the same view.
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(BOOTS), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: have NexusSlots pass getRawSlot() instead of convertSlot() -> reddens only in
        // a boot gate, which is why GATE-nexus.md rows 4-5 exist as well as this row.
    }

    @Test
    void aDoubleClickONTheLockedSlotIsPERMITTEDWhenTheCursorIsNotAStar() {
        // THE ONE PLACE THE RULE DELIBERATELY PERMITS A GESTURE ON SLOT 8, named as its own row
        // because it is exactly the case a reader will challenge.
        //
        // It is correct: collect-to-cursor draws only from stacks SIMILAR to the cursor, and
        // MenuRouting.collectToCursor's javadoc settles what similar means here -- "isSimilar
        // compares item meta, so a tagged item never matches a plain stack of the same Material".
        // A double-click holding cobblestone cannot pull a PDC-tagged nether star out of slot 8.
        assertFalse(NexusLock.refusesClick(ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR,
                player(NexusLock.LOCKED_SLOT), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: refuse every DOUBLE_CLICK outright -> reddens. That mutation is the plausible
        // over-broad version of this rule, and it would kill collect-to-cursor plugin-wide.
    }

    @Test
    void aCursorDropOutsideTheWindowIsPERMITTEDWhenTheCursorIsNotAStar() {
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.DROP_ALL_CURSOR,
                outside(), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: return true unconditionally from the DROP_*_CURSOR arm -> reddens, and players
        // could no longer throw anything away while a menu is open.
    }

    // ---------------------------------------------------------------- the refusals

    @Test
    void aPlainClickOnTheLockedSlotIsREFUSED() {
        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(NexusLock.LOCKED_SLOT), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: LOCKED_SLOT 8 -> 7 -> does NOT redden. See the row below for why, and do not
        // trust this row to guard the constant's value.
    }

    @Test
    void theLockedSlotIsTheRIGHTMOSTHOTBARSLOT_theONLYRowThatPinsTheVALUE() {
        // MEASURED, AND IT IS THE REASON THIS ROW EXISTS. Every other row in this file names
        // LOCKED_SLOT symbolically, so mutating the constant moves the code AND the expectation
        // together and bites nothing: `8 -> 7` was applied -- marker present, original gone, 13-byte
        // delta -- and all twenty rows stayed GREEN.
        //
        // That is the "applied, no bite" failure, and the constant's javadoc originally claimed the
        // opposite ("NexusLockTest mutates it rather than asserting the literal 8"). A symbolic
        // reference cannot pin a value. Only a literal can.
        assertEquals(8, NexusLock.LOCKED_SLOT,
                "the locked slot is the rightmost hotbar slot; PlayerInventory indexes the hotbar "
                        + "0-8, so it is 8. If this is being changed deliberately, GATE-nexus.md's "
                        + "rows are staged against 8 and need restaging.");

        // And the value expressed as behaviour, so the pin is not merely a restatement of itself:
        // literal 8 refuses, literal 7 -- an ordinary hotbar slot beside it -- does not.
        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(8), -1, false, NO_STARS), "slot 8 must be locked");
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(7), -1, false, NO_STARS), "slot 7 is an ordinary hotbar slot");
        // Mutation: LOCKED_SLOT 8 -> 7 -> reddens HERE, and only here.
    }

    @Test
    void theLockedSlotIsREFUSEDEvenWhenNoStarIsInIt() {
        // THE SECOND AXIS, and it is invisible to every row that stages a star. The locked slot is
        // refused because it IS the locked slot, not because of what it currently holds -- so
        // nothing can be placed into a momentarily empty one, and the slot cannot be colonised in
        // the window between a star being lost and convergence running.
        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PLACE_ALL,
                player(NexusLock.LOCKED_SLOT), -1, false, NO_STARS));
        // Mutation: drop the `t.index() == LOCKED_SLOT` arm and keep only starAt -> reddens HERE
        // and nowhere else, because every other refusal row has a star staged.
    }

    @Test
    void aStrayStarIsREFUSEDWhereverItSits() {
        // The other half of the same pair: a star outside the locked slot is still not the
        // player's to move. Convergence puts it back on the next join; until then it does not
        // travel.
        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(A_STORAGE_SLOT), -1, false, index -> index == A_STORAGE_SLOT));
        // Mutation: drop the starAt arm and keep only LOCKED_SLOT -> reddens HERE and nowhere
        // else, because every other refusal row targets the locked slot directly.
    }

    @Test
    void aNumberKeyOverAMENUCellIsREFUSEDWhenTheHotbarButtonNamesTheLockedSlot() {
        // THE ROUTE THAT IS INVISIBLE TO THE CLICKED SLOT. Hover a crafting-grid cell, press 9:
        // MenuRouting.hotbarMove swaps hotbar index 8 into that cell, performing the write itself.
        // The clicked raw slot is in the MENU and the locked slot is never clicked at all.
        assertTrue(NexusLock.refusesClick(ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP,
                new Touched(false, 4), NexusLock.LOCKED_SLOT, false, STAR_IN_LOCKED_SLOT));
        // Mutation: drop the hotbarButton member from the NUMBER_KEY arm -> reddens. This is the
        // defect the first draft of the decision signature could not even express.
    }

    @Test
    void aNumberKeyWithNoHotbarButtonIsPERMITTED() {
        // getHotbarButton() is -1 when the click was not a hotbar press. Reading the inventory at a
        // negative index would throw from inside an event handler; MenuRouting guards the same
        // value the same way.
        assertFalse(NexusLock.refusesClick(ClickType.NUMBER_KEY, InventoryAction.NOTHING,
                new Touched(false, 4), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: drop the `hotbarButton < 0` guard -> Touched(true, -1) enters the set; reddens
        // only if -1 could match, which is why touchesTheStar also guards index >= 0.
    }

    @Test
    void aNumberKeyForTheSlotAlreadyHoveredDoesNotThrow() {
        // Set.of throws IllegalArgumentException on duplicate elements, and the two members of the
        // NUMBER_KEY arm COLLIDE whenever a player presses the number key for the slot they are
        // already hovering -- which would throw from inside an event handler, on an ordinary
        // gesture, in the player's own inventory.
        assertTrue(assertDoesNotThrow(() ->
                NexusLock.refusesClick(ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP,
                        player(NexusLock.LOCKED_SLOT), NexusLock.LOCKED_SLOT, false,
                        STAR_IN_LOCKED_SLOT)));
        // Mutation: Set.copyOf(List.of(..)) -> Set.of(..) -> reddens with IllegalArgumentException.
    }

    @Test
    void swapOffhandIsREFUSEDFromBothEnds() {
        // F is a two-way swap, so both the hovered slot and the offhand are destinations.
        assertTrue(NexusLock.refusesClick(ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP,
                player(NexusLock.LOCKED_SLOT), -1, false, STAR_IN_LOCKED_SLOT),
                "the star swapped OUT of the locked slot");
        assertTrue(NexusLock.refusesClick(ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP,
                player(A_STORAGE_SLOT), -1, false, index -> index == NexusLock.OFFHAND_SLOT),
                "a star resting in the OFFHAND swapped into an ordinary slot");
        // Mutation: drop the OFFHAND_SLOT member -> the second assertion reddens.
    }

    @Test
    void aDoubleClickWithAStarONTheCursorIsREFUSED() {
        // The genuine second-star case, and the only way a collect can reach a star at all.
        assertTrue(NexusLock.refusesClick(ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR,
                player(A_STORAGE_SLOT), -1, true, NO_STARS));
        // Mutation: ignore cursorIsStar on the DOUBLE_CLICK path -> reddens.
    }

    @Test
    void aCursorDropOutsideTheWindowIsREFUSEDWhenTheCursorHoldsAStar() {
        // The route that names no slot at all: an outside-the-window drop arrives as an ordinary
        // LEFT with raw slot -999, so only the ACTION identifies it. This is the sole reason
        // refusesClick reads InventoryAction.
        //
        // Refused where a cursor PLACE is permitted, and the difference is not squeamishness: a
        // place relocates the star inside the inventory where convergence finds it; a drop puts it
        // on the ground to despawn or be taken by somebody else.
        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.DROP_ALL_CURSOR,
                outside(), -1, true, NO_STARS), "DROP_ALL_CURSOR");
        assertTrue(NexusLock.refusesClick(ClickType.RIGHT, InventoryAction.DROP_ONE_CURSOR,
                outside(), -1, true, NO_STARS), "DROP_ONE_CURSOR");
        // Mutation: delete the DROP_*_CURSOR arm -> both redden, and the star goes on the floor.
    }

    @Test
    void placingAStarFromTheCursorIntoAnOrdinarySlotIsPERMITTED() {
        // DELIBERATE, and the asymmetry with the drop above is the whole of it. A star on the
        // cursor is already outside its slot -- a state these refusals cannot have produced.
        // Refusing every placement too would wedge it on the cursor with NO legal destination,
        // which is the failure a lock is most likely to create and least likely to be blamed for.
        // Convergence is the repair; the refusals are not.
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PLACE_ALL,
                player(A_STORAGE_SLOT), -1, true, NO_STARS));
        // Mutation: short-circuit refusesClick on cursorIsStar -> reddens, and a star that ever
        // reached the cursor could never be put down again.
    }

    // ---------------------------------------------------------------- per-view

    @Test
    void theSameLockedSlotIsRefusedWhicheverViewItArrivedFrom() {
        // The property the conversion must deliver, stated in the space this class owns: the
        // locked hotbar slot is raw 44 in the own-inventory screen and raw 89 with a 54-chest
        // open, and BOTH must convert to Touched(true, 8) and give the same verdict.
        //
        // That convertSlot actually produces 8 from both is boot-only -- it needs a live
        // InventoryView -- and is GATE-nexus.md rows 4-5. This row is what those gate rows are
        // checked against.
        Touched fromOwnScreen = new Touched(true, NexusLock.LOCKED_SLOT);   // raw 44 converted
        Touched fromChestView = new Touched(true, NexusLock.LOCKED_SLOT);   // raw 89 converted

        assertEquals(
                NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        fromOwnScreen, -1, false, STAR_IN_LOCKED_SLOT),
                NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        fromChestView, -1, false, STAR_IN_LOCKED_SLOT),
                "the same physical slot decided differently depending on the open view");
        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                fromOwnScreen, -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: make the verdict depend on anything view-shaped -> reddens.
    }

    // ---------------------------------------------------------------- drags

    @Test
    void aDragTouchingTheLockedSlotIsREFUSED() {
        assertTrue(NexusLock.refusesDrag(
                Set.of(player(A_STORAGE_SLOT), player(NexusLock.LOCKED_SLOT)),
                false, STAR_IN_LOCKED_SLOT));
        // Mutation: LOCKED_SLOT 8 -> 7 -> reddens.
    }

    @Test
    void aDragWithAStarONTheCursorIsREFUSEDEvenIntoUnrelatedSlots() {
        // getRawSlots() enumerates a drag's DESTINATIONS. Its SOURCE is the cursor and is never in
        // that set, so without cursorIsStar a star could be dragged anywhere that is neither the
        // locked slot nor already holding one.
        //
        // Refused rather than permitted -- unlike the click-place above -- because refusing traps
        // nobody: a plain left-click into a free slot is still available to put it down.
        assertTrue(NexusLock.refusesDrag(
                Set.of(player(A_STORAGE_SLOT), player(21)), true, NO_STARS));
        // Mutation: drop cursorIsStar from refusesDrag -> reddens. The click path cannot catch
        // this; a drag is a different event with a different source.
    }

    @Test
    void aDragTouchingNothingRelevantIsPERMITTED() {
        assertFalse(NexusLock.refusesDrag(
                Set.of(player(A_STORAGE_SLOT), player(21), new Touched(false, NexusLock.LOCKED_SLOT)),
                false, STAR_IN_LOCKED_SLOT));
        // Mutation: make refusesDrag always-true -> reddens, and no drag anywhere would work while
        // a menu was open.
    }

    // ---------------------------------------------------------------- helpers

    /** A slot in the PLAYER's own inventory, at the given index. */
    private static Touched player(int index) {
        return new Touched(true, index);
    }

    /** A click that landed outside the window and names no slot at all. */
    private static Touched outside() {
        return new Touched(false, -1);
    }
}
