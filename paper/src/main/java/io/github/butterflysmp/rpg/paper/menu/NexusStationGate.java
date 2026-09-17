package io.github.butterflysmp.rpg.paper.menu;

import java.util.List;
import java.util.Optional;

/**
 * Which hub stations a player's level has opened, and what a locked one says.
 *
 * <p>Pure -- no Bukkit, no {@code Player}, no inventory -- so the thresholds and every string a
 * locked station renders are unit-testable at the 2-second loop. {@code GrindstoneButton} makes the
 * same split for the same reason: the slice before last shipped two zero-kill mutations because the
 * logic sat inside a class that needed a server to construct.
 *
 * <h2>*** THE GATE IS ON THE HUB ONLY. THE WORLD BLOCKS OPEN AT EVERY LEVEL. ***</h2>
 *
 * Ben's ruling, and it is the load-bearing half. A crafting table, an enchanting table and a
 * grindstone placed in the world are all hijacked by {@code RpgListeners.hijackedBlocks} and open
 * our screens directly, <b>never through the hub</b> -- so nothing here is reachable from that path
 * and no check was added to it.
 *
 * <p><b>That is why the locked lore must say BOTH facts.</b> A player told only "unlocks at level
 * 13" concludes the feature is unavailable to them, when in fact it is twenty blocks away in their
 * base. Saying only "use the block" would hide that the hub route is coming. <b>Neither sentence is
 * sufficient and the omission is silent either way.</b>
 *
 * <p><b>{@code CraftingMenu}, {@code EnchantMenu} and {@code GrindstoneMenu} are UNTOUCHED by this
 * slice.</b> Putting the check in them would gate the world blocks too -- the opposite of the
 * ruling -- and it would put one rule in three places.
 */
final class NexusStationGate {

    private NexusStationGate() {}

    /**
     * The three stations, each with the level that opens it.
     *
     * <h2>THE THRESHOLDS ARE BEN'S, NOT DERIVED FROM THE CURVE</h2>
     *
     * {@code 3 / 10 / 13}. They are content decisions and there is no formula behind them --
     * do not "regularise" them into a progression, and do not re-derive them from the XP totals.
     * {@code PlayerLevelTest} pins the totals those levels correspond to
     * ({@code 2,090 / 12,940 / 19,980}) so a curve retune moves the XP and never the levels.
     */
    enum Station {
        CRAFTING(3, "Crafting", "A crafting table"),
        ENCHANTING(10, "Enchanting", "An enchanting table"),
        GRINDSTONE(13, "Grindstone", "A grindstone");

        private final int unlockLevel;
        private final String displayName;
        private final String blockPhrase;

        Station(int unlockLevel, String displayName, String blockPhrase) {
            this.unlockLevel = unlockLevel;
            this.displayName = displayName;
            this.blockPhrase = blockPhrase;
        }

        int unlockLevel() { return unlockLevel; }

        String displayName() { return displayName; }

        /** How the second lore line names this station's world block. */
        String blockPhrase() { return blockPhrase; }
    }

    /**
     * The station at a hub slot, if that slot holds one.
     *
     * <p><b>The slot numbers live in {@code NexusMenuLayout} and are read from there</b>, not
     * copied -- the hub already paid for two hand-maintained lists checked against each other, and
     * a third copy of a slot number is how a gate comes to protect a cell nothing renders.
     */
    static Optional<Station> at(int slot) {
        if (slot == NexusMenuLayout.CRAFTING_SLOT) return Optional.of(Station.CRAFTING);
        if (slot == NexusMenuLayout.ENCHANT_SLOT) return Optional.of(Station.ENCHANTING);
        if (slot == NexusMenuLayout.GRINDSTONE_SLOT) return Optional.of(Station.GRINDSTONE);
        return Optional.empty();
    }

    /** Has this level opened this station? Inclusive: level 13 opens the level-13 station. */
    static boolean unlocked(Station station, int level) {
        return level >= station.unlockLevel();
    }

    /** A locked station's display name: the station's own name, so the player still knows what it is. */
    static String lockedName(Station station) {
        return station.displayName();
    }

    /**
     * A locked station's lore. <b>THREE LINES, AND THE SECOND AND THIRD ARE BOTH REQUIRED.</b>
     *
     * <pre>
     *   Locked -- unlocks at level 13
     *   You are level 7.
     *   A grindstone in the world still works.
     * </pre>
     *
     * <p><b>Line 1 is the rule, line 2 is where the player stands, line 3 is the way round it.</b>
     * Dropping line 2 makes the player count their own level; dropping line 3 tells them a feature
     * they own is unavailable.
     *
     * <p><b>DRAFTED, NOT SETTLED</b> -- Ben has the wording. Changing it is one method and one test
     * row.
     */
    static List<String> lockedLore(Station station, int level) {
        return List.of(
                "Locked -- unlocks at level " + station.unlockLevel(),
                "You are level " + level + ".",
                station.blockPhrase() + " in the world still works.");
    }

    /**
     * <b>THE DRAFTED CHAT REFUSAL, AND IT IS DELIBERATELY NOT SENT. FLAGGED FOR BEN.</b>
     *
     * <p>The brief said <i>"locked icon/lore/refusal"</i>. A chat line is one reading of "refusal"
     * and <b>the grindstone's ruling one slice ago says the opposite</b>, in those words: <i>"A
     * click on a button that is not LIME does nothing and says nothing, because the button has
     * already said it."</i> A locked station's lore has already said it, three ways.
     *
     * <p>So the click refuses <b>silently</b>, consistent with that ruling, and this string exists
     * so switching it on costs one line rather than a redesign. <b>It is not dead code being kept
     * for symmetry</b> -- it is a drafted decision with a named owner, and
     * {@code NexusStationGateTest} asserts its text so the draft cannot rot before it is ruled on.
     *
     * <p>If Ben rules for the chat line, the tension to resolve is whether the grindstone's silence
     * rule is hub-wide or was only ever about that button.
     */
    static String draftedChatRefusal(Station station, int level) {
        return station.displayName() + " unlocks at level " + station.unlockLevel()
                + ". You are level " + level + ". "
                + station.blockPhrase().toLowerCase(java.util.Locale.ROOT)
                + " in the world still works.";
    }
}
