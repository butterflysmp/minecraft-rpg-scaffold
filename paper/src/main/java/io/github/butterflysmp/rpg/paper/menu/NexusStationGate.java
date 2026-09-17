package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.vault.VaultPageGate;

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
 * 13" concludes the feature is unavailable to them, when in fact the block still works. Saying only
 * "use the block" would hide that the hub route is coming. <b>Neither sentence is sufficient and the
 * omission is silent either way.</b>
 *
 * <h2>*** THE RATIONALE ABOVE USED TO SAY "IT IS TWENTY BLOCKS AWAY IN THEIR BASE", AND THE VAULT
 * FALSIFIED THAT HALF WITHOUT TOUCHING THE HEADING ***</h2>
 *
 * <b>The heading still holds, literally and for every station including the vault:</b> the ender
 * chest opens our screen at every level, exactly as the crafting table does. <b>What stopped being
 * true is the reason the third lore line gave</b> -- "the feature is twenty blocks away in your
 * base" -- because walking to an ender chest does not get a sub-20 player storage. It gets them this
 * same locked screen.
 *
 * <p>So the third line is no longer DERIVED from the block's name. Each station
 * {@linkplain Station#worldRoute() authors its own sentence}, and the vault's says what its block
 * actually does rather than promising vanilla behaviour it took away.
 *
 * <p><b>The heading is deliberately NOT amended</b>, per the 2026-09-17 ruling: amending a heading
 * that is still true, because a paragraph under it went stale, loses the ruling the heading records.
 *
 * <p><b>{@code CraftingMenu}, {@code EnchantMenu} and {@code GrindstoneMenu} are UNTOUCHED by this
 * slice.</b> Putting the check in them would gate the world blocks too -- the opposite of the
 * ruling -- and it would put one rule in three places.
 */
final class NexusStationGate {

    private NexusStationGate() {}

    /**
     * The stations, each with the level that opens it.
     *
     * <h2>THE THRESHOLDS ARE BEN'S, NOT DERIVED FROM THE CURVE</h2>
     *
     * {@code 3 / 10 / 13}. They are content decisions and there is no formula behind them --
     * do not "regularise" them into a progression, and do not re-derive them from the XP totals.
     * {@code PlayerLevelTest} pins the totals those levels correspond to
     * ({@code 2,090 / 12,940 / 19,980}) so a curve retune moves the XP and never the levels.
     *
     * <p><b>The VAULT's threshold is the exception and is READ rather than authored</b>: it is
     * {@code VaultPageGate}'s page-1 level, because the hub cell and the page button must open on
     * the same day. This javadoc said "the three stations" until the vault arrived -- a count in a
     * heading cannot be raised by appending.
     */
    enum Station {
        CRAFTING(3, "Crafting", "A crafting table in the world still works."),
        ENCHANTING(10, "Enchanting", "An enchanting table in the world still works."),
        GRINDSTONE(13, "Grindstone", "A grindstone in the world still works."),

        /**
         * The vault, and <b>the one station whose world block does not give the vanilla thing.</b>
         *
         * <p>{@code 20} is page 1's threshold and it is read from {@code VaultPageGate} rather than
         * written here, so the hub cell and the page button cannot disagree about when the vault
         * opens. The other three stations' levels are literals because nothing else holds them.
         *
         * <p><b>Its sentence is the whole reason this field stopped being derived.</b> "An ender
         * chest in the world still works" would be FALSE -- the hijack takes the vanilla chest away
         * at every level -- so it says what the block really does instead.
         */
        VAULT(VaultPageGate.unlockLevel(0), "Vault", "An ender chest opens this same vault.");

        private final int unlockLevel;
        private final String displayName;
        private final String worldRoute;

        Station(int unlockLevel, String displayName, String worldRoute) {
            this.unlockLevel = unlockLevel;
            this.displayName = displayName;
            this.worldRoute = worldRoute;
        }

        int unlockLevel() { return unlockLevel; }

        String displayName() { return displayName; }

        /**
         * The third lore line: what this station's world block does for a player who cannot reach
         * the hub route yet.
         *
         * <h2>*** A WHOLE SENTENCE, AUTHORED PER STATION. IT USED TO BE A NOUN PHRASE. ***</h2>
         *
         * <p>This was {@code blockPhrase()} -- {@code "A grindstone"} -- and both call sites
         * appended {@code " in the world still works."} to it. That derivation made the three
         * sentences consistent and made a FOURTH station impossible to word truthfully: the
         * vault's block does not leave vanilla behaviour in place, it replaces it.
         *
         * <p><b>The three original sentences are byte-identical to what the derivation produced</b>,
         * including the capital "A" that {@code refusal}'s own comment records a draft getting
         * wrong by lower-casing.
         */
        String worldRoute() { return worldRoute; }
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
        if (slot == NexusMenuLayout.VAULT_SLOT) return Optional.of(Station.VAULT);
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
                station.worldRoute());
    }

    /**
     * What a locked station says when it is clicked. <b>SENT. It is not optional.</b>
     *
     * <h2>*** THE MESSAGE IS MANDATORY **BECAUSE** THE ICON KEEPS THE STATION'S MATERIAL ***</h2>
     *
     * {@code NexusMenu.station} renders a locked station with <b>its own material</b> and dims only
     * the display NAME. That is a good call on its own terms -- the player sees what the station
     * will be, rather than a barrier standing where it will go -- and <b>it is exactly what makes
     * silence unacceptable</b>:
     *
     * <p><b>A dimmed name lives in the HOVER TOOLTIP. Without hovering, a locked crafting station
     * is PIXEL-IDENTICAL to an unlocked one.</b> So a player clicks a normal-looking crafting
     * table, nothing happens, and nothing explains why -- indistinguishable from a broken menu, and
     * <b>most likely to happen to the player least equipped to interpret it</b>: someone at level 1
     * opening the hub for the first time.
     *
     * <h2>THE TWO DECISIONS ARE COUPLED, AND ONLY THREE OF THE FOUR COMBINATIONS ARE COHERENT</h2>
     *
     * <pre>
     *   material changes + silent    you can SEE it is locked before you click
     *   material same    + SPEAKS    you find out by clicking, the only gesture you have   &lt;-- OURS
     *   material changes + speaks    coherent, mildly redundant
     *   material same    + silent    A CRAFTING TABLE THAT DOES NOTHING                    &lt;-- avoid
     * </pre>
     *
     * <p><b>If anyone ever changes the locked material to a barrier or a pane, this message becomes
     * merely redundant rather than wrong</b> -- so the coupling is safe in that direction. The
     * direction that breaks is deleting this call while keeping the material.
     * {@code ProgressionWiringSignatureTest} is what goes red.
     *
     * <h2>THE GRINDSTONE'S SILENCE RULING DOES NOT TRANSFER, AND THE PREMISE IS WHY</h2>
     *
     * That ruling reads <i>"a click on a button that is not LIME does nothing and says nothing,
     * because the button has already said it."</i> <b>What that button says without being asked is
     * COLOUR</b> -- lime, yellow, red, gray, visible at a glance, on the cell being clicked.
     * <b>A locked station says nothing without a hover.</b> Same shape of rule, different premise,
     * opposite answer.
     *
     * <p><b>BOTH FACTS, per Row 28</b> -- the unlock level AND that the world block still works.
     * <i>"Unlocks at level 10."</i> alone is the dead end that rule exists to name.
     */
    static String refusal(Station station, int level) {
        // blockPhrase() AS AUTHORED, not lower-cased. The draft lower-cased it and produced
        // "... You are level 7. a grindstone in the world still works." -- a sentence opening in
        // lower case, mid-message. It was never sent, so nothing could have caught it except
        // reading the asserted string, which is why the draft was asserted at all.
        return station.displayName() + " unlocks at level " + station.unlockLevel()
                + ". You are level " + level + ". "
                + station.worldRoute();
    }
}
