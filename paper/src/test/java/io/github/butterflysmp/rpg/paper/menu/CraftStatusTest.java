package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The status bar's three states and their colours.
 *
 * <p>Testable for the reason {@code GridClickIntentTest} is: {@code Material} is a plain Bukkit enum
 * that loads without a server. The BAR itself -- painting eight slots on every preview refresh --
 * needs a live menu and is boot-gate-only, but the DECISION does not have to be, and a three-arm
 * switch deserves better than three gate rows.
 *
 * <p>Each test names the mutation it forces red.
 */
class CraftStatusTest {

    @Test
    void anEmptyGridIsEMPTY_WhateverThePreviewSays() {
        // The empty arm wins first. A stale previewedRecipe against a cleared grid must read EMPTY,
        // not VALID -- otherwise clearing the grid would leave the bar green.
        assertEquals(CraftStatus.EMPTY, CraftStatus.of(true, false));
        assertEquals(CraftStatus.EMPTY, CraftStatus.of(true, true));
        // Mutation: test `matched` before `gridEmpty` -> the second assertion reddens, and the bar
        // stays LIME over an empty grid.
    }

    @Test
    void aLoadedGridIsVALIDOnlyWhenARecipeMatched() {
        assertEquals(CraftStatus.VALID, CraftStatus.of(false, true));
        assertEquals(CraftStatus.INVALID, CraftStatus.of(false, false));
        // Mutation: invert the matched arm -> both redden, and every junk grid reads green.
    }

    @Test
    void everyStateHasITSOWNColourAndNoneIsTheChromeColour() {
        // Three arms, three colours -- a bar that painted two states the same is a readout that
        // cannot distinguish them, which is worse than no readout.
        List<Material> seen = new ArrayList<>();
        for (CraftStatus status : CraftStatus.values()) {
            Material material = status.material();
            assertNotNull(material, status + " has no colour");
            assertFalse(seen.contains(material), status + " reuses " + material);

            // AND NOT THE CHROME COLOUR. The bar sits in a menu that is otherwise black panes; a
            // status colour that matched the filler would be invisible as a readout. This is also
            // the assertion that would have caught the slice-5 change if the bar had been left
            // gray while the chrome went black -- gray was the chrome colour until then.
            assertNotEquals(MenuIcons.FILLER, material,
                    status + " paints in the chrome colour and cannot be seen");
            seen.add(material);
        }
        assertEquals(3, seen.size(), "the walk must not be empty or short");
        // Mutation: point two arms at the same Material -> the contains() check reddens.
        // Mutation: revert FILLER to gray -> the EMPTY arm reddens, because EMPTY is gray.
    }

    @Test
    void theThreeStatesAreEXACTLYWhatTheGridCanBe() {
        // The axis, not the cases that came to mind. Two booleans, four combinations, three states
        // -- and the fourth combination (empty grid + a match) is deliberately folded into EMPTY
        // rather than being a state of its own.
        assertEquals(3, CraftStatus.values().length,
                "a fourth state is a decision -- see the RED-collapses-two-causes note");

        List<CraftStatus> reached = new ArrayList<>();
        for (boolean empty : new boolean[]{true, false}) {
            for (boolean matched : new boolean[]{true, false}) {
                CraftStatus status = CraftStatus.of(empty, matched);
                assertNotNull(status, empty + "/" + matched + " decided nothing");
                if (!reached.contains(status)) reached.add(status);
            }
        }
        assertEquals(3, reached.size(), "all three states must be reachable from the two inputs");
        // Mutation: collapse INVALID into EMPTY -> reached.size() becomes 2 -> reddens. That
        // mutation makes a grid full of junk look identical to an empty one.
    }
}
