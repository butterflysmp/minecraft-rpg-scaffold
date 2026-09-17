package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hub's level gate: which stations open when, and what a locked one says.
 *
 * <p>{@code NexusMenu} needs a live {@code Player} and {@code Bukkit.createInventory}, so the
 * WIRING is boot-gate-only -- {@code GATE-nexus.md}'s SLICE 9 rows. The RULE is pure and is here.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class NexusStationGateTest {

    @Test
    void theTHRESHOLDSAreBENSLiterals_notDerivedFromAnything() {
        // LITERALS, for NexusMenuLayoutTest's reason: a constant only ever named symbolically has
        // no guard, because a mutation moves the code and the expectation together. There is no
        // formula behind 3/10/13 and nothing here should suggest one.
        assertEquals(3, NexusStationGate.Station.CRAFTING.unlockLevel(), "crafting at 3");
        assertEquals(10, NexusStationGate.Station.ENCHANTING.unlockLevel(), "enchanting at 10");
        assertEquals(13, NexusStationGate.Station.GRINDSTONE.unlockLevel(), "the grindstone at 13");

        // ALL THREE DIFFER, so no two can be transposed without a row seeing it -- and they are
        // STRICTLY INCREASING in the order the stations sit on the row, left to right.
        assertEquals(3, new HashSet<>(List.of(3, 10, 13)).size());
        assertTrue(NexusStationGate.Station.CRAFTING.unlockLevel()
                < NexusStationGate.Station.ENCHANTING.unlockLevel());
        assertTrue(NexusStationGate.Station.ENCHANTING.unlockLevel()
                < NexusStationGate.Station.GRINDSTONE.unlockLevel());
    }

    @Test
    void unlockIsINCLUSIVE_andTheLevelBELOWIsLocked() {
        // THE OFF-BY-ONE THAT NOBODY WOULD NOTICE IN PLAY. Every station is checked at its own
        // threshold, one below, and one above.
        for (NexusStationGate.Station station : NexusStationGate.Station.values()) {
            int at = station.unlockLevel();
            assertTrue(NexusStationGate.unlocked(station, at),
                    station + " opens AT level " + at + ", not after it");
            assertFalse(NexusStationGate.unlocked(station, at - 1),
                    station + " is locked one level short");
            assertTrue(NexusStationGate.unlocked(station, at + 1), station + " stays open above");
            assertTrue(NexusStationGate.unlocked(station, PlayerLevel.MAX_LEVEL),
                    station + " is open at the cap");
        }
        // Mutation MUTGATE-STRICT: `>=` -> `>` -> kill set RECORDED in the PR body.
    }

    @Test
    void aNEWPlayerHasCRAFTINGLockedToo_whichIsTheWholePointOfGatingTheFIRSTStation() {
        // Level 1 is where everyone starts and crafting opens at 3, so the hub a brand new player
        // opens has THREE locked stations. Stated because "the first station is effectively open"
        // is the assumption someone will make when trimming this.
        for (NexusStationGate.Station station : NexusStationGate.Station.values()) {
            assertFalse(NexusStationGate.unlocked(station, 1), station + " is locked at level 1");
        }
        assertEquals(1, PlayerLevel.levelFor(0L), "and level 1 is what zero lifetime XP buys");
    }

    @Test
    void everySTATIONSlotResolves_andNothingElseDoes() {
        assertEquals(Optional.of(NexusStationGate.Station.CRAFTING),
                NexusStationGate.at(NexusMenuLayout.CRAFTING_SLOT));
        assertEquals(Optional.of(NexusStationGate.Station.ENCHANTING),
                NexusStationGate.at(NexusMenuLayout.ENCHANT_SLOT));
        assertEquals(Optional.of(NexusStationGate.Station.GRINDSTONE),
                NexusStationGate.at(NexusMenuLayout.GRINDSTONE_SLOT));

        // THE CHROME MUST NOT RESOLVE. A gate that claimed Close was a station would swallow the
        // one button a player cannot do without.
        assertEquals(Optional.empty(), NexusStationGate.at(NexusMenuLayout.CLOSE_SLOT));
        assertEquals(Optional.empty(), NexusStationGate.at(NexusMenuLayout.SETTINGS_SLOT),
                "settings is NOT gated -- a locked-out player must still be able to move the star");
        assertEquals(Optional.empty(), NexusStationGate.at(NexusMenuLayout.STATS_SLOT),
                "nor the stats head, which is how they see the level they need");
        assertEquals(Optional.empty(), NexusStationGate.at(-1));

        // AND EVERY OTHER SLOT ON THE SCREEN IS EMPTY -- without this, the four above are equally
        // consistent with at() returning a station for half the filler.
        Set<Integer> stationSlots = Set.of(NexusMenuLayout.CRAFTING_SLOT,
                NexusMenuLayout.ENCHANT_SLOT, NexusMenuLayout.GRINDSTONE_SLOT);
        int resolved = 0;
        for (int slot = 0; slot < NexusMenuLayout.SIZE; slot++) {
            boolean isStation = NexusStationGate.at(slot).isPresent();
            assertEquals(stationSlots.contains(slot), isStation, "slot " + slot + " station-ness");
            if (isStation) resolved++;
        }
        assertEquals(3, resolved, "exactly three stations, and the sweep actually ran");
        // Mutation MUTAT-ALWAYS: return CRAFTING for any slot -> kill set RECORDED in the PR body.
    }

    @Test
    void theLOCKEDLoreSaysBOTHFacts_theLevelANDThatTheBlockStillWorks() {
        // *** THE HALF THAT IS EASY TO DROP. *** A player told only "unlocks at level 13" concludes
        // the grindstone is unavailable to them, when it is twenty blocks away in their base --
        // and the world blocks are NOT gated, by Ben's ruling. Both sentences or neither is useful.
        List<String> lore = NexusStationGate.lockedLore(NexusStationGate.Station.GRINDSTONE, 7);
        assertEquals(3, lore.size(), "three lines: the rule, where you stand, the way round it");
        assertEquals("Locked -- unlocks at level 13", lore.get(0));
        assertEquals("You are level 7.", lore.get(1));
        assertEquals("A grindstone in the world still works.", lore.get(2));

        // EVERY STATION NAMES ITS OWN BLOCK, so the third line cannot become a generic sentence
        // that is true of all three and specific to none.
        assertEquals("A crafting table in the world still works.",
                NexusStationGate.lockedLore(NexusStationGate.Station.CRAFTING, 1).get(2));
        assertEquals("An enchanting table in the world still works.",
                NexusStationGate.lockedLore(NexusStationGate.Station.ENCHANTING, 1).get(2));

        // AND EVERY STATION'S LORE CARRIES ITS OWN THRESHOLD AND THE PLAYER'S OWN LEVEL.
        for (NexusStationGate.Station station : NexusStationGate.Station.values()) {
            List<String> lines = NexusStationGate.lockedLore(station, 2);
            assertTrue(lines.get(0).endsWith(" " + station.unlockLevel()),
                    station + " names its own unlock level: " + lines.get(0));
            assertTrue(lines.get(1).contains("2"), station + " names the player's level");
        }
        // Mutation MUTLORE-ONEFACT: drop the third line -> kill set RECORDED in the PR body.
    }

    @Test
    void aLockedStationKeepsITSOWNNAME_soThePlayerKnowsWhatTheyAreWaitingFor() {
        // Not "Locked" and not a blank -- the name is the cue that says WHICH feature this is.
        assertEquals("Crafting", NexusStationGate.lockedName(NexusStationGate.Station.CRAFTING));
        assertEquals("Enchanting", NexusStationGate.lockedName(NexusStationGate.Station.ENCHANTING));
        assertEquals("Grindstone", NexusStationGate.lockedName(NexusStationGate.Station.GRINDSTONE));
        for (NexusStationGate.Station station : NexusStationGate.Station.values()) {
            assertEquals(station.displayName(), NexusStationGate.lockedName(station),
                    "locked and open render the SAME name -- only the colour dims");
        }
    }

    @Test
    void theChatRefusalSaysBOTHFactsToo_andIsSENTBecauseTheIconKeepsTheMaterial() {
        // *** SENT, NOT DRAFTED. RULED 2026-09-17, AND THE REASON IS THE ICON. ***
        // A dimmed name lives in the hover tooltip, so an un-hovered locked station is
        // PIXEL-IDENTICAL to an open one. Silence there is a crafting table that does nothing --
        // and it lands on the player least able to interpret it, someone at level 1 opening the
        // hub for the first time. The grindstone's silence ruling does not transfer: what THAT
        // button says unasked is COLOUR, at a glance, on the cell being clicked.
        String refusal = NexusStationGate.refusal(NexusStationGate.Station.GRINDSTONE, 7);
        assertEquals("Grindstone unlocks at level 13. You are level 7. "
                + "A grindstone in the world still works.", refusal);

        // CAPITAL "A", AND THE DRAFT HAD A LOWER-CASE ONE. It lower-cased blockPhrase(), opening a
        // sentence in lower case mid-message. It was never sent, so only reading the asserted
        // string could have caught it -- which is the argument for asserting a draft at all.
        assertTrue(refusal.contains(". A grindstone"), "the third sentence opens in upper case");

        // BOTH FACTS, as a property, so the message cannot be edited down to one of them.
        for (NexusStationGate.Station station : NexusStationGate.Station.values()) {
            String text = NexusStationGate.refusal(station, 2);
            assertTrue(text.contains("level " + station.unlockLevel()), "names the unlock level");
            assertTrue(text.contains("still works"), "and names the world-block escape");
            assertTrue(text.contains("You are level 2."), "and where the player stands");
        }
        // Mutation MUTREFUSAL-ONEFACT: drop the "still works" clause -> kill set RECORDED in the
        // PR body.
    }
}
