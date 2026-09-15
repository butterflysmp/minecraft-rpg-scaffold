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

    /**
     * The locked slot THIS FILE'S ROWS ARE STAGED AGAINST, as a literal.
     *
     * <p><b>A literal and not {@code NexusLock.DEFAULT_LOCKED_SLOT}, deliberately.</b> Naming the
     * production constant would make every row below move with it, which is the symbolic-reference
     * defect this file already carries two scars from -- {@code 8 -> 7} and {@code 40 -> 39} both
     * applied cleanly and killed nothing. The rows are staged against a number; the number is
     * written here once, and {@code theDEFAULTLockedSlotIsTheRIGHTMOSTHOTBARSLOT} is what ties it
     * to production.
     */
    private static final int LOCKED = 8;

    /** Nothing in the inventory holds a star. The common case, and the one most rows want. */
    private static final IntPredicate NO_STARS = index -> false;

    /** The invariant state: the one star is in the locked slot. */
    private static final IntPredicate STAR_IN_LOCKED_SLOT = index -> index == LOCKED;

    /**
     * Every row calls these rather than {@link NexusLock} directly, so that the locked slot is
     * supplied in ONE place for the rows that do not care which slot it is.
     *
     * <p><b>This is a convenience, and it is also a hazard worth naming.</b> A row routed through
     * here cannot see the {@code lockedSlot} argument at all, so it can neither vary it nor be
     * blind to it being wrong -- it is pinned to {@link #LOCKED} by the helper. The rows that
     * exercise the argument itself call {@code NexusLock.refusesClick} DIRECTLY and pass their own
     * value; they are the per-player rows at the foot of this file.
     */
    private static boolean refusesClick(ClickType click, InventoryAction action, Touched clicked,
                                        int hotbarButton, boolean cursorIsStar, IntPredicate starAt) {
        return NexusLock.refusesClick(click, action, clicked, hotbarButton, cursorIsStar,
                LOCKED, starAt);
    }

    private static boolean refusesDrag(Set<Touched> dragged, boolean cursorIsStar,
                                       IntPredicate starAt) {
        return NexusLock.refusesDrag(dragged, cursorIsStar, LOCKED, starAt);
    }

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
                            () -> refusesClick(click, action, player(A_STORAGE_SLOT),
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
                    refusesClick(click, InventoryAction.PICKUP_ALL,
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
        assertFalse(refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                new Touched(false, LOCKED), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: drop the !t.playerInventory() continue in touchesTheStar -> reddens, and every
        // menu whose layout uses slot 8 becomes unclickable.
    }

    @Test
    void theBootsSlotIsPERMITTED_theRegressionForRawEightMeaningTwoThings() {
        // In the own-inventory screen the player's boots are RAW 8. A lock that compared raw
        // numbers would refuse this click and permit the locked hotbar slot, in the same view.
        assertFalse(refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
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
        assertFalse(refusesClick(ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR,
                player(LOCKED), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: refuse every DOUBLE_CLICK outright -> reddens. That mutation is the plausible
        // over-broad version of this rule, and it would kill collect-to-cursor plugin-wide.
    }

    @Test
    void aCursorDropOutsideTheWindowIsPERMITTEDWhenTheCursorIsNotAStar() {
        assertFalse(refusesClick(ClickType.LEFT, InventoryAction.DROP_ALL_CURSOR,
                outside(), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: return true unconditionally from the DROP_*_CURSOR arm -> reddens, and players
        // could no longer throw anything away while a menu is open.
    }

    // ---------------------------------------------------------------- the refusals

    @Test
    void aPlainClickOnTheLockedSlotIsREFUSED() {
        assertTrue(refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(LOCKED), -1, false, STAR_IN_LOCKED_SLOT));
        // Mutation: LOCKED_SLOT 8 -> 7 -> does NOT redden. See the row below for why, and do not
        // trust this row to guard the constant's value.
    }

    @Test
    void theLockedSlotIsTheRIGHTMOSTHOTBARSLOT_theONLYRowThatPinsTheVALUE() {
        // MEASURED, AND IT IS THE REASON THIS ROW EXISTS. Every other row in this file names the
        // locked slot symbolically, so mutating the value moves the code AND the expectation
        // together and bites nothing: `8 -> 7` was applied -- marker present, original gone, 13-byte
        // delta -- and all twenty rows stayed GREEN.
        //
        // That is the "applied, no bite" failure. A symbolic reference cannot pin a value. Only a
        // literal can.
        //
        // WHAT THIS ROW PINS CHANGED WHEN THE SLOT BECAME PER-PLAYER, AND IT NEARLY BECAME HOLLOW.
        // The old form was `assertEquals(8, NexusLock.LOCKED_SLOT)`. With that constant gone and the
        // rows staged against this file's own LOCKED literal, the same line would read
        // `assertEquals(8, LOCKED)` -- 8 against 8, a tautology that survives any production
        // change whatsoever. It asserts the DEFAULT instead, which is the thing production still
        // owns.
        assertEquals(8, NexusLock.DEFAULT_LOCKED_SLOT,
                "the default locked slot is the rightmost hotbar slot; PlayerInventory indexes the "
                        + "hotbar 0-8, so it is 8. If this is being changed deliberately, "
                        + "GATE-nexus.md's rows are staged against 8 and need restaging.");
        assertEquals(LOCKED, NexusLock.DEFAULT_LOCKED_SLOT,
                "and this file's rows are staged against the default, so the two must agree -- "
                        + "otherwise every row below is testing a slot no player will ever have");

        // And the value expressed as behaviour, so the pin is not merely a restatement of itself:
        // literal 8 refuses, literal 7 -- an ordinary hotbar slot beside it -- does not.
        assertTrue(refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(8), -1, false, NO_STARS), "slot 8 must be locked");
        assertFalse(refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(7), -1, false, NO_STARS), "slot 7 is an ordinary hotbar slot");
        // Mutation MUTDEFAULTSLOT: PlayerProfile.DEFAULT_NEXUS_SLOT 8 -> 7 -> kill set RECORDED in
        // the PR body, not predicted here.
    }

    /**
     * THE SLOT IS PER-PLAYER, AND THIS IS THE ROW THAT SAYS SO.
     *
     * <p>Every other row in this file is staged against one slot and would pass identically if the
     * parameter were ignored and the old constant restored. <b>This one varies it</b>, which is the
     * only way to show the argument is read at all.
     */
    @Test
    void aDIFFERENTPlayersSlotIsLockedInsteadOfTheDefault() {
        // A player whose chosen slot is 3. Slot 3 is theirs and locked; slot 8 -- the DEFAULT, and
        // the slot every other row in this file exercises -- is an ordinary hotbar cell for them.
        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        player(3), -1, false, 3, NO_STARS),
                "slot 3 is locked for a player who chose 3");
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        player(8), -1, false, 3, NO_STARS),
                "AND SLOT 8 IS NOT. If this fails, the parameter is being ignored and the default "
                        + "is welded in -- the whole point of the change, and invisible to every "
                        + "other row here");

        // The drag path takes the same argument and must agree; they share touchesTheStar, and a
        // caller that threaded the slot into one and not the other would pass every click row.
        assertTrue(NexusLock.refusesDrag(Set.of(player(3)), false, 3, NO_STARS),
                "the drag path reads the same per-player slot");
        assertFalse(NexusLock.refusesDrag(Set.of(player(8)), false, 3, NO_STARS),
                "and is equally not staged on the default");
    }

    /**
     * THE PROFILE-LOAD WINDOW: the slot is not known yet, and the star is still protected.
     *
     * <p>This is the state between a player joining and their profile arriving from disk. The
     * locked-slot arm is inert -- deliberately, because guessing the default here would brick an
     * ordinary hotbar cell for anyone who chose a different slot -- and the star-follows-the-item
     * arm carries the whole guard alone.
     *
     * <p><b>It is the row that refutes the obvious objection to waiting</b>, namely that the star
     * is unguarded meanwhile. It is not.
     */
    @Test
    void whileTheSlotIsUNKNOWNTheStarIsStillREFUSEDWhereverItSits() {
        IntPredicate starInFive = index -> index == 5;

        assertTrue(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        player(5), -1, false, NexusLock.NO_LOCKED_SLOT, starInFive),
                "THE STAR IS STILL GUARDED with no slot known -- the second arm follows the item");

        // And nothing else is: no slot is locked by position while the answer is unknown.
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        player(8), -1, false, NexusLock.NO_LOCKED_SLOT, NO_STARS),
                "the DEFAULT slot must not be locked on a guess -- a player whose slot is 3 would "
                        + "find slot 8 inert for no reason they could ever discover");
        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        player(0), -1, false, NexusLock.NO_LOCKED_SLOT, NO_STARS),
                "nor any other slot");

        assertEquals(-1, NexusLock.NO_LOCKED_SLOT,
                "the sentinel is negative, which is what makes the >= 0 guard in touchesTheStar "
                        + "the thing that disables the arm");
    }

    /**
     * THE {@code >= 0} GUARD ON THE LOCKED-SLOT ARM, FED THE INPUT PRODUCTION CANNOT PRODUCE.
     *
     * <p><b>This row exists because the guard was measured DEAD.</b> {@code MUTARMGUARD} -- dropping
     * {@code lockedSlot >= 0 &&} -- left the entire suite green, because nothing in production
     * builds a player-inventory {@code Touched} at a negative index: {@code NexusSlots.touchedOf}
     * pairs {@code -1} with {@code playerInventory=false}, and the {@code NUMBER_KEY} arm
     * range-checks the hotbar button first.
     *
     * <p>A guard that cannot fire is indistinguishable from one that protects you. So this row
     * CAUSES the condition rather than asserting the guard exists -- a {@code Touched(true, -1)}
     * built by hand, which is legal for a test and unreachable for the server.
     *
     * <p><b>What it protects:</b> the day a caller does produce one -- a new {@code ClickType} arm,
     * a changed {@code convertSlot} contract -- an unknown locked slot would otherwise match it and
     * refuse the gesture, which at {@code LOWEST} means a silently dead menu.
     */
    @Test
    void anUNKNOWNSlotDoesNotMatchANegativeTouchedIndex_theGuardThatWasMeasuredDEAD() {
        Touched negativeInPlayerInventory = new Touched(true, -1);

        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                        negativeInPlayerInventory, -1, false, NexusLock.NO_LOCKED_SLOT, NO_STARS),
                "NO_LOCKED_SLOT is -1 and so is this index -- without the >= 0 guard they compare "
                        + "EQUAL and the gesture is refused by a slot that does not exist");

        assertFalse(NexusLock.refusesDrag(Set.of(negativeInPlayerInventory), false,
                        NexusLock.NO_LOCKED_SLOT, NO_STARS),
                "and the drag path shares touchesTheStar, so it shares the defect");
        // Mutation MUTARMGUARD: drop `lockedSlot >= 0 &&` -> kill set RECORDED in the PR body.
    }

    @Test
    void theOffhandIsSLOT40AndNOT39_theHELMET_theONLYRowThatPinsThatVALUE() {
        // THE SAME DEFECT AS THE ROW ABOVE, FOUND IN REVIEW RATHER THAN BY THE SWEEP THAT SHOULD
        // HAVE FOLLOWED IT. swapOffhandIsREFUSEDFromBothEnds was OFFHAND_SLOT's only appearance in
        // this file and it named the constant symbolically on BOTH sides -- the touched set built
        // Touched(true, OFFHAND_SLOT) and the predicate tested index == NexusLock.OFFHAND_SLOT --
        // so the two moved together under mutation.
        //
        // MEASURED: 40 -> 39 applied (marker present, original gone) and all 21 rows stayed GREEN.
        //
        // AND 39 IS NOT AN ARBITRARY WRONG NUMBER. In this class's stated index space -- "36-39
        // armour, 40 offhand" -- 39 is the HELMET. The mutant lock watches a player's helmet
        // instead of their offhand, every F-swap of the star out of the locked slot goes unrefused,
        // and nothing reddens.
        assertEquals(40, NexusLock.OFFHAND_SLOT,
                "PlayerInventory indexes armour 36-39 and the offhand 40; 39 is the HELMET");

        // The value expressed as behaviour, and as a DISCRIMINATING PAIR rather than one assertion:
        // a star in the offhand is reachable by F, a star in the helmet slot is not.
        assertTrue(refusesClick(ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP,
                        player(A_STORAGE_SLOT), -1, false, index -> index == 40),
                "a star at index 40 IS in the offhand and F must refuse");
        assertFalse(refusesClick(ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP,
                        player(A_STORAGE_SLOT), -1, false, index -> index == 39),
                "index 39 is the HELMET, not the offhand; F does not reach it");
        // Mutation: OFFHAND_SLOT 40 -> 39 -> reddens HERE, and only here.
    }

    @Test
    void theLockedSlotIsREFUSEDEvenWhenNoStarIsInIt() {
        // THE SECOND AXIS, and it is invisible to every row that stages a star. The locked slot is
        // refused because it IS the locked slot, not because of what it currently holds -- so
        // nothing can be placed into a momentarily empty one, and the slot cannot be colonised in
        // the window between a star being lost and convergence running.
        assertTrue(refusesClick(ClickType.LEFT, InventoryAction.PLACE_ALL,
                player(LOCKED), -1, false, NO_STARS));
        // Mutation MUTAXISLOCKED: drop the `lockedSlot >= 0 && t.index() == lockedSlot` arm and keep
        // only starAt -> reddens HERE
        // and nowhere else, because every other refusal row has a star staged.
    }

    @Test
    void aStrayStarIsREFUSEDWhereverItSits() {
        // The other half of the same pair: a star outside the locked slot is still not the
        // player's to move. Convergence puts it back on the next join; until then it does not
        // travel.
        assertTrue(refusesClick(ClickType.LEFT, InventoryAction.PICKUP_ALL,
                player(A_STORAGE_SLOT), -1, false, index -> index == A_STORAGE_SLOT));
        // Mutation MUTAXISSTAR: drop the starAt arm and keep only the locked-slot arm -> reddens HERE
        // and nowhere
        // else, because every other refusal row targets the locked slot directly.
    }

    @Test
    void aNumberKeyOverAMENUCellIsREFUSEDWhenTheHotbarButtonNamesTheLockedSlot() {
        // THE ROUTE THAT IS INVISIBLE TO THE CLICKED SLOT. Hover a crafting-grid cell, press 9:
        // MenuRouting.hotbarMove swaps hotbar index 8 into that cell, performing the write itself.
        // The clicked raw slot is in the MENU and the locked slot is never clicked at all.
        assertTrue(refusesClick(ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP,
                new Touched(false, 4), LOCKED, false, STAR_IN_LOCKED_SLOT));
        // Mutation: drop the hotbarButton member from the NUMBER_KEY arm -> reddens. This is the
        // defect the first draft of the decision signature could not even express.
    }

    @Test
    void aNumberKeyWithNoHotbarButtonIsPERMITTED() {
        // getHotbarButton() is -1 when the click was not a hotbar press. Reading the inventory at a
        // negative index would throw from inside an event handler; MenuRouting guards the same
        // value the same way.
        assertFalse(refusesClick(ClickType.NUMBER_KEY, InventoryAction.NOTHING,
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
                refusesClick(ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP,
                        player(LOCKED), LOCKED, false,
                        STAR_IN_LOCKED_SLOT)));
        // Mutation: Set.copyOf(List.of(..)) -> Set.of(..) -> reddens with IllegalArgumentException.
    }

    @Test
    void swapOffhandIsREFUSEDFromBothEnds() {
        // F is a two-way swap, so both the hovered slot and the offhand are destinations.
        assertTrue(refusesClick(ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP,
                player(LOCKED), -1, false, STAR_IN_LOCKED_SLOT),
                "the star swapped OUT of the locked slot");
        assertTrue(refusesClick(ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP,
                player(A_STORAGE_SLOT), -1, false, index -> index == NexusLock.OFFHAND_SLOT),
                "a star resting in the OFFHAND swapped into an ordinary slot");
        // Mutation: drop the OFFHAND_SLOT member -> the second assertion reddens.
    }

    @Test
    void aDoubleClickWithAStarONTheCursorIsREFUSED() {
        // The genuine second-star case, and the only way a collect can reach a star at all.
        assertTrue(refusesClick(ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR,
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
        assertTrue(refusesClick(ClickType.LEFT, InventoryAction.DROP_ALL_CURSOR,
                outside(), -1, true, NO_STARS), "DROP_ALL_CURSOR");
        assertTrue(refusesClick(ClickType.RIGHT, InventoryAction.DROP_ONE_CURSOR,
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
        assertFalse(refusesClick(ClickType.LEFT, InventoryAction.PLACE_ALL,
                player(A_STORAGE_SLOT), -1, true, NO_STARS));
        // Mutation: short-circuit refusesClick on cursorIsStar -> reddens, and a star that ever
        // reached the cursor could never be put down again.
    }

    // ---------------------------------------------------------------- per-view

    @Test
    void anOutsideTheWindowClickNamesNoSlotAndIsConvertedWithoutTouchingTheView() {
        // THIS ROW REPLACES A HOLLOW ONE, AND THE REPLACEMENT IS THE POINT.
        //
        // What was here asserted refusesClick(X) == refusesClick(X): two `Touched` records built
        // from identical arguments, compared through the same function. `Touched` is a record, so
        // they were equal, and the assertion COULD NOT FAIL for any implementation whatsoever. Two
        // comments -- "raw 44 converted", "raw 89 converted" -- described a conversion the test
        // never performed, because NexusSlots was never on the stack. Its mutation note named a
        // mutation that cannot be written: refusesClick has no view-shaped parameter to corrupt.
        //
        // THE CONVERSION ITSELF CANNOT BE UNIT-TESTED HERE, AND THAT IS MEASURED RATHER THAN
        // ASSUMED. Probed against the pinned paper-api: both InventoryView.convertSlot(int) and
        // InventoryView.getInventory(int) are ABSTRACT, not default --
        //     "abstract method convertSlot(int) in InventoryView cannot be accessed directly"
        // -- so the raw->index arithmetic lives in the server's CraftInventoryView, which is not on
        // the test classpath. A stub would have to implement convertSlot ITSELF, and the test would
        // then assert its own fake arithmetic: the same hollowness, one layer down.
        //
        // So the conversion is carried by GATE-nexus.md rows 4-5 and by nothing else, and this row
        // claims only what it can show: the ONE branch of touchedOf that returns before the view is
        // ever consulted.
        assertEquals(new Touched(false, -1), NexusSlots.touchedOf(null, -999),
                "an outside-the-window click names no slot, and must not dereference the view");
        assertEquals(new Touched(false, -1), NexusSlots.touchedOf(null, -1));
        // Mutation: move the `rawSlot < 0` guard below the getInventory call -> NullPointerException
        // here. That reordering is a plausible tidy-up and would NPE inside an event handler on
        // every click a player makes outside a window.
    }

    // ---------------------------------------------------------------- drags

    @Test
    void aDragTouchingTheLockedSlotIsREFUSED() {
        assertTrue(refusesDrag(
                Set.of(player(A_STORAGE_SLOT), player(LOCKED)),
                false, STAR_IN_LOCKED_SLOT));
        // Mutation: this row is helper-routed and staged on LOCKED, so it moves with the fixture, not
        // with the production default. Kill sets are RECORDED in the PR body, not predicted here.
    }

    @Test
    void aDragWithAStarONTheCursorIsREFUSEDEvenIntoUnrelatedSlots() {
        // getRawSlots() enumerates a drag's DESTINATIONS. Its SOURCE is the cursor and is never in
        // that set, so without cursorIsStar a star could be dragged anywhere that is neither the
        // locked slot nor already holding one.
        //
        // Refused rather than permitted -- unlike the click-place above -- because refusing traps
        // nobody: a plain left-click into a free slot is still available to put it down.
        assertTrue(refusesDrag(
                Set.of(player(A_STORAGE_SLOT), player(21)), true, NO_STARS));
        // Mutation: drop cursorIsStar from refusesDrag -> reddens. The click path cannot catch
        // this; a drag is a different event with a different source.
    }

    @Test
    void aDragTouchingNothingRelevantIsPERMITTED() {
        assertFalse(refusesDrag(
                Set.of(player(A_STORAGE_SLOT), player(21), new Touched(false, LOCKED)),
                false, STAR_IN_LOCKED_SLOT));
        // Mutation: make refusesDrag always-true -> reddens, and no drag anywhere would work while
        // a menu was open.
    }

    // ------------------------------------------------- THE CREATIVE PAYLOAD GUARD

    @Test
    void aCreativeWriteCARRYINGAStarIsREFUSEDWhereverItLands() {
        // THE ROW FOR THE DEFECT GATE-nexus.md ROW 8 RECORDS, AND IT IS ABOUT THE PAYLOAD, NOT THE
        // SLOT. Every other refusal in this file answers "which slots does this gesture touch?".
        // This one cannot: an InventoryCreativeEvent names one slot and the stack that slot is
        // ABOUT TO BECOME, so at the moment it fires the destination is EMPTY and the star is
        // somewhere else entirely. NO_STARS is the fixture, deliberately -- it is the state
        // `starAt` reports during the duplicating write, and under the old rule it meant PERMITTED.
        //
        // Swept over every player slot rather than one: the two gestures that produced residuals in
        // play landed in DIFFERENT places -- slot 1 from a number key, the OFFHAND from F -- so a
        // row staged at a single index could pass while the other arm stayed open. Two instances of
        // one shape, one gesture apart, is what OFFHAND_SLOT already cost this file once.
        for (int index = 0; index < 41; index++) {
            assertTrue(
                    NexusLock.refusesClick(ClickType.CREATIVE, InventoryAction.PLACE_ALL,
                            player(index), -1, true, NO_STARS),
                    "a creative write of a star into player slot " + index + " must be REFUSED");
        }
        // Mutation MUTCREATIVE: delete the `click == ClickType.CREATIVE && cursorIsStar` return
        // -> reddens here at index 0 and at every index except LOCKED_SLOT, which the slot arm
        // still catches on its own. APPLIED AND MEASURED, not expected -- see the PR body.
    }

    @Test
    void aCreativeWriteOfAnORDINARYItemIsUNCHANGEDByTheGuard() {
        // THE POSITIVE CONTROL, AND WITHOUT IT THE ROW ABOVE IS EQUALLY CONSISTENT WITH THE GUARD
        // HAVING REFUSED EVERY CREATIVE CLICK IN THE GAME -- which would break creative inventory
        // editing wholesale and redden nothing, the same shape as the dead-menu guard above.
        assertFalse(NexusLock.refusesClick(ClickType.CREATIVE, InventoryAction.PLACE_ALL,
                        player(A_STORAGE_SLOT), -1, false, NO_STARS),
                "a creative write of an ordinary item into an ordinary slot must be PERMITTED");

        // And the pre-existing slot behaviour is untouched on the same click type: the locked slot
        // still refuses a write whose payload is NOT a star. This is what refuses PICKING THE STAR
        // UP in creative -- that write is slot 8 <- AIR. GATE-nexus.md Row 6's 6.1 read PASS.
        assertTrue(NexusLock.refusesClick(ClickType.CREATIVE, InventoryAction.PLACE_ALL,
                        player(NexusLock.LOCKED_SLOT), -1, false, NO_STARS),
                "a creative write to the LOCKED slot must still be REFUSED on payload alone");
        // Mutation MUTCREATIVEWIDE: widen 2b to `click == ClickType.CREATIVE` -> reddens the first
        // assertion. APPLIED AND MEASURED.
    }

    @Test
    void theCreativeGuardAndTheCURSORPlaceDISAGREE_deliberately_doNotHarmoniseThem() {
        // TWO ARMS, ONE PAYLOAD, THE SAME SLOT, OPPOSITE ANSWERS -- ON PURPOSE. Written down here
        // because an asymmetry that is only inferable gets "tidied up" by the next reader, and the
        // tidy-up silently reopens one of the two.
        //
        //   LEFT     + star payload -> PERMITTED   the player is MOVING something they hold
        //   CREATIVE + star payload -> REFUSED     the client is MANUFACTURING one
        //
        // The reason is not squeamishness about creative. A cursor place relocates a star that is
        // ALREADY outside its slot, where convergence collects it on the next join, and refusing it
        // would wedge the star on the cursor with no legal destination at all. A creative set-slot
        // write CREATES a star that convergence would then have to delete. Permit the move, refuse
        // the mint.
        NexusLock.Touched sameSlot = player(A_STORAGE_SLOT);

        assertFalse(NexusLock.refusesClick(ClickType.LEFT, InventoryAction.PLACE_ALL,
                        sameSlot, -1, true, NO_STARS),
                "the cursor place must stay PERMITTED -- see "
                        + "placingAStarFromTheCursorIntoAnOrdinarySlotIsPERMITTED");
        assertTrue(NexusLock.refusesClick(ClickType.CREATIVE, InventoryAction.PLACE_ALL,
                        sameSlot, -1, true, NO_STARS),
                "the creative write of the same payload into the same slot must be REFUSED");
        // Mutation: harmonise the two, in either direction -> reddens. That is this row's whole
        // job; it guards a DECISION rather than a behaviour, and nothing else in the file would
        // notice the decision being reversed.
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
