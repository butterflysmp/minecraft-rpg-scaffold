package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Telling a player that the Nexus star shadowed the block they were trying to use.
 *
 * <p>The star's branch in {@code RpgListeners.onRightClick} runs ahead of {@code hijackedBlocks},
 * so right-clicking a crafting table with the star in hand opens the <b>hub</b> and the crafting
 * menu never appears. That precedence is Ben's ruling and is correct. <b>What is not acceptable is
 * that it happens silently</b>: this file already argues twice -- {@code BrokenNotice} and
 * {@code QuiverNotice} -- that a thing which does nothing without an explanation is
 * indistinguishable from a bug.
 *
 * <h2>WHY THIS SPEAKS WHEN THE NEXUS LOCK DOES NOT, AND THE BOUNDARY IS NOT A CONTRADICTION</h2>
 *
 * <b>The Nexus refuses gestures SILENTLY -- Ben's standing ruling, recorded at {@code GATE-nexus.md}
 * Row 6 -- and this notice does not overturn it, because a shadowed block is a different event from
 * a refused gesture.</b>
 *
 * <ul>
 *   <li><b>The LOCK refuses a GESTURE, and the screen answers the question.</b> The player watches
 *       the star not move. Chat repeating what they can already see is noise, which is why
 *       {@code NexusLock}'s refusals are silent and every refusal path calls
 *       {@code player.updateInventory()} instead of saying anything.
 *   <li><b>This SHADOWS A DIFFERENT THING ENTIRELY.</b> Nothing on screen says the crafting table
 *       was suppressed -- the evidence of what went wrong is <i>the thing that did not happen</i>.
 *       <b>There is nothing to see, so there is something to say.</b>
 * </ul>
 *
 * <p><b>Do not harmonise the two.</b> Making the lock speak floods chat with what the player is
 * already looking at; making this silent leaves a player convinced the crafting table is broken.
 * Both directions break something that was ruled.
 *
 * <h2>NO SOUND, AND THAT IS A DELIBERATE DEPARTURE FROM {@code BrokenNotice}</h2>
 *
 * {@code BrokenNotice} plays one, immediately after its message and inside the same throttle. This
 * does not, on Ben's instruction. The Nexus is being kept quiet on purpose -- the same call that
 * took the lore line off {@code MenuIcons.close()} for a calmer screen -- and <b>a sound here would
 * be the loudest thing the Nexus does, attached to its least important event.</b>
 *
 * <p>The throttle reuses {@link CooldownTracker} for the reasons {@code BrokenNotice} records: it is
 * already concurrent, already keyed per player, and already cleared on quit by
 * {@code RpgListeners.onQuit}, so this adds no state that can leak.
 */
public final class NexusCollisionNotice {

    private NexusCollisionNotice() {}

    /**
     * The tracker key. Leading underscores so it can never collide with a real ability id, and its
     * own bucket so it cannot silence -- or be silenced by -- any other notice.
     *
     * <p>{@code NoticeThrottleKeysTest} counts the keys in the project and is the thing that forces
     * a new notice to be deliberate about this. Adding this class bumped its {@code KEYS_TODAY}
     * from 7 to 8, which is the edit that asks the question.
     */
    private static final String KEY = "__nexus_block_shadowed_notice";

    /** Two seconds, matching {@code BrokenNotice}. Held right-click must not become a rattle. */
    private static final int THROTTLE_TICKS = 40;

    /**
     * Say that the star took the click, at most once per {@link #THROTTLE_TICKS}.
     *
     * <p><b>It names the REMEDY, not the symptom.</b> "The Nexus opened instead" describes what the
     * player just watched; <i>switch off the star's slot</i> is the part they cannot work out from
     * the screen, because nothing on it connects the held item to the missing crafting menu.
     *
     * <p><b>Called ONLY for the collision, never for an ordinary open.</b> The usual way to open the
     * hub is right-clicking air, and that must stay silent -- a line of chat every time a player
     * opens their menu is {@code MenuSafety}'s <i>"no message repeated sixty-four times helps"</i>
     * objection in a place it would be earned every session. The caller's condition is
     * {@code RIGHT_CLICK_BLOCK} <b>and</b> {@code hijackedBlocks.containsKey(...)}, and the gate row
     * for this has a right-click-air control precisely because a notice that fired on every open
     * would pass the collision half perfectly.
     *
     * <p>Must run on the thread that owns the player. The caller is an event handler, so it already
     * is.
     */
    public static void shadowedBlock(Player player, CooldownTracker cooldowns) {
        if (!cooldowns.isReady(player.getUniqueId(), KEY)) return;
        cooldowns.trigger(player.getUniqueId(), KEY, THROTTLE_TICKS);
        player.sendMessage(Component.text(
                "The Nexus took that click -- switch to another hotbar slot to use the block.",
                NamedTextColor.GRAY));
        // NO SOUND. See the class javadoc: deliberate, and Ben's ruling, not an omission.
    }
}
