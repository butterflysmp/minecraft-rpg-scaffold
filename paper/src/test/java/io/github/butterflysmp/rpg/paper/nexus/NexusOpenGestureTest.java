package io.github.butterflysmp.rpg.paper.nexus;

import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which already-refused click ALSO opens the hub.
 *
 * <p>{@code ClickType} is a plain enum that loads without a server, so the whole rule is decidable
 * here. What this file cannot see is whether the four booleans are computed from the right things
 * -- that is {@code GATE-nexus.md}'s slice 7.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class NexusOpenGestureTest {

    /** The one combination that opens, spelled out once so every other row is a departure from it. */
    private static boolean opens(ClickType click, boolean screen, boolean slot, boolean empty) {
        return NexusOpenGesture.opensHub(click, screen, slot, empty);
    }

    @Test
    void leftAndRightOnTheStarsSlotWithAnEmptyCursorOpenTheHub() {
        assertTrue(opens(ClickType.LEFT, true, true, true), "LEFT opens");
        assertTrue(opens(ClickType.RIGHT, true, true, true), "RIGHT opens");
        // Mutation: drop the RIGHT arm -> the second reddens.
    }

    @Test
    void EVERYOtherClickTypeRefuses_andTHATISHowCreativeIsExcluded() {
        // *** THE ROW THAT KEEPS GATE ROW 6.1 TRUE. ***
        //
        // In creative, a click on an own-inventory slot arrives as ClickType.CREATIVE, not LEFT or
        // RIGHT -- which is the basis of NexusLock's step 2b payload guard. So this arm excludes
        // creative BY CONSTRUCTION, with no branch to write and none to go stale.
        //
        // Row 6.1 read "pick up the star -> silent refusal" under CONDITIONS: CREATIVE mode. That
        // reading is STILL TRUE after this slice, and it is true only because CREATIVE is not in
        // the pair above. Widen this arm and 6.1 becomes false with nothing going red -- which is
        // why GATE slice 7 carries a creative CONTROL row rather than trusting this argument.
        assertFalse(opens(ClickType.CREATIVE, true, true, true),
                "CREATIVE must NOT open -- Row 6.1's reading depends on it");

        // AND THE WHOLE ENUM, not the three that came to mind. A click type added by a future
        // Paper version defaults to REFUSING, which is the safe direction for a lock.
        List<ClickType> opensFor = new ArrayList<>();
        for (ClickType click : ClickType.values()) {
            if (opens(click, true, true, true)) opensFor.add(click);
        }
        assertEquals(List.of(ClickType.LEFT, ClickType.RIGHT), opensFor,
                "exactly two click types open, out of the whole enum");

        // The gestures Ben's REFUSES list names by hand, asserted individually so a reader can see
        // them rather than infer them from the sweep above.
        assertFalse(opens(ClickType.SHIFT_LEFT, true, true, true), "shift-click refuses");
        assertFalse(opens(ClickType.NUMBER_KEY, true, true, true), "number-key refuses");
        assertFalse(opens(ClickType.SWAP_OFFHAND, true, true, true), "F refuses");
        assertFalse(opens(ClickType.DROP, true, true, true), "Q refuses");
        assertFalse(opens(ClickType.CONTROL_DROP, true, true, true), "and ctrl-Q");
        assertFalse(opens(ClickType.DOUBLE_CLICK, true, true, true), "and double-click");
        assertFalse(opens(ClickType.MIDDLE, true, true, true), "and middle-click");
        // Mutation: `click != LEFT && click != RIGHT` -> `click == CREATIVE` -> the creative row
        // AND the whole-enum row redden.
    }

    @Test
    void aLOADEDCursorRefuses_becauseThatIsAnAttemptedPLACE() {
        // A click with something on the cursor is the player trying to PUT IT DOWN, not a click on
        // the star. Opening a menu there drops them into the hub holding an item, which is
        // PLAN-enchant-table-ui row 10c's problem arriving somewhere it was never solved.
        assertFalse(opens(ClickType.LEFT, true, true, false), "LEFT with a loaded cursor refuses");
        assertFalse(opens(ClickType.RIGHT, true, true, false), "and RIGHT");
        // Mutation: drop the cursorEmpty conjunct -> both redden.
    }

    @Test
    void anotherSCREENRefuses_evenOnTheStarsOwnSlot() {
        // With a chest or one of our menus open, the click stays a silent refusal. Opening the hub
        // from inside another menu is a nested transition, and our menus with input slots have
        // returnEverything obligations on close.
        //
        // NOTE THIS IS THE *SCREEN*, NOT THE SLOT'S HALF. The star's slot is in the player's own
        // inventory in a chest view too -- that is exactly the near-miss predicate
        // NexusSlots.isOwnInventoryScreen exists to avoid.
        assertFalse(opens(ClickType.LEFT, false, true, true), "chest open -> refuses");
        assertFalse(opens(ClickType.RIGHT, false, true, true), "and on right-click");
        // Mutation: drop the ownInventoryScreen conjunct -> both redden.
    }

    @Test
    void anotherSLOTRefuses_evenOnTheOwnInventoryScreen() {
        assertFalse(opens(ClickType.LEFT, true, false, true), "a different slot refuses");
        assertFalse(opens(ClickType.RIGHT, true, false, true), "and on right-click");
        // Mutation: drop the starsOwnSlot conjunct -> both redden.
    }

    @Test
    void ALLFOURConditionsAreNECESSARY_oneAtATime() {
        // THE WHOLE TRUTH TABLE FOR THE OPENING CASE, not the cases that came to mind. Each row
        // flips exactly ONE input away from the opening combination and must refuse -- which is
        // what proves every conjunct is load-bearing rather than decorative.
        assertTrue(opens(ClickType.LEFT, true, true, true), "the baseline opens");

        assertFalse(opens(ClickType.SHIFT_LEFT, true, true, true), "click type alone");
        assertFalse(opens(ClickType.LEFT, false, true, true), "screen alone");
        assertFalse(opens(ClickType.LEFT, true, false, true), "slot alone");
        assertFalse(opens(ClickType.LEFT, true, true, false), "cursor alone");

        // AND NO COMBINATION OF THE THREE BOOLEANS OPENS WITHOUT ALL THREE. Eight rows, and only
        // one of them may be true.
        int opened = 0;
        for (boolean screen : new boolean[] {false, true}) {
            for (boolean slot : new boolean[] {false, true}) {
                for (boolean empty : new boolean[] {false, true}) {
                    if (opens(ClickType.LEFT, screen, slot, empty)) opened++;
                }
            }
        }
        assertEquals(1, opened, "exactly one of the eight boolean combinations opens");
        // Mutation: change any && to || -> the count row reddens with a number a reader can act on.
    }
}
