package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.vault.VaultPageGate;
import io.github.butterflysmp.rpg.core.vault.VaultShape;

import java.util.ArrayList;
import java.util.List;

/**
 * What reaching a level UNLOCKS, for the level screen: the Nexus stations and the vault pages -- today,
 * the only two things in the game gated on the player level.
 *
 * <p><b>READ FROM THE GATES, NEVER COPIED.</b> Each threshold lives once, in
 * {@link NexusStationGate.Station} and {@link VaultPageGate}; this walks them. A list of "3, 7, 10, 13,
 * 20, 25 ... 50" typed here would be a second copy of Ben's numbers, and the first retune would make the
 * screen advertise an unlock at a level that no longer grants it.
 *
 * <p>The wording is a default and Ben's to change.
 */
final class LevelUnlocks {

    private LevelUnlocks() {}

    /** Stations first, in the gate's own order, then vault pages in page order. Empty when none. */
    static List<String> at(int level) {
        List<String> lines = new ArrayList<>();
        for (NexusStationGate.Station station : NexusStationGate.Station.values()) {
            if (station.unlockLevel() == level) lines.add("Unlocks the " + station.displayName() + " station");
        }
        // Page 0 is FREE -- no level buys it, and "unlocks at level 0" is the sentence VaultPageGate.FREE
        // exists to stop anyone writing. Pages are shown 1-based, converted here at the edge.
        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            if (VaultPageGate.isFree(page)) continue;
            if (VaultPageGate.unlockLevel(page) == level) lines.add("Unlocks Vault page " + (page + 1));
        }
        return List.copyOf(lines);
    }
}
