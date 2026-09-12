package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Telling a player their quiver is empty, or still reloading, without spamming them.
 *
 * <p>{@link BrokenNotice}'s mechanism, reused wholesale: a throttle keyed in the existing
 * {@link CooldownTracker} under a {@code __}-prefixed id that no content filename could produce.
 * That class's javadoc gives the reason and it holds unchanged here -- the tracker is <i>"already
 * concurrent (Folia has two players on two region threads), already keyed per player, and already
 * cleared on quit by RpgListeners.onQuit, so this adds no state that can leak."</i>
 *
 * <h2>WHY NOT JUST CALL BrokenNotice</h2>
 *
 * <p>Because it would lie. {@code BrokenNotice} says <i>"Your weapon is broken -- repair it before
 * using it."</i> A full-durability Boltor with an empty magazine is neither broken nor repairable,
 * and a player has no way to check a message against the code the way a developer checks a comment.
 * The brief asked to reuse the throttle rather than invent messaging, and that is exactly what this
 * does: <b>same mechanism, same 40-tick window, different sentence.</b>
 *
 * <h2>SEPARATE THROTTLE KEYS, AND THAT IS NOT TIDINESS</h2>
 *
 * <p>Empty and reloading are the two halves of one loop -- you run dry, you reload, you run dry --
 * so sharing a bucket with each other or with {@code BrokenNotice} would let one state's message
 * suppress the other's. A player who empties a magazine and immediately presses fire would see
 * "empty" and then silence, rather than the reload countdown that tells them what to do about it.
 */
public final class QuiverNotice {

    private QuiverNotice() {}

    /** Leading underscores so no content filename can collide -- {@code BrokenNotice}'s convention. */
    private static final String EMPTY_KEY = "__quiver_empty_notice";
    private static final String RELOADING_KEY = "__quiver_reloading_notice";

    /** {@code BrokenNotice}'s window, deliberately: held fire quiet, still responsive. */
    private static final int THROTTLE_TICKS = 40;

    private static final String EMPTY_SOUND = "block.dispenser.fail";
    private static final String RELOAD_SOUND = "item.crossbow.loading_start";
    private static final float SOUND_VOLUME = 1.0f;
    private static final float SOUND_PITCH = 1.0f;

    /** The magazine is spent. Says what to DO, because a refusal with no remedy reads as a bug. */
    public static void empty(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, EMPTY_KEY)) return;
        player.sendMessage(Component.text(
                "Your quiver is empty -- left-click to reload.", NamedTextColor.GRAY));
        // Past the throttle check so message and sound are one notification and cannot drift apart,
        // exactly as BrokenNotice pairs them. Player#playSound, not World#playSound: the holder
        // hears it, nobody else does.
        player.playSound(player.getLocation(), EMPTY_SOUND, SOUND_VOLUME, SOUND_PITCH);
    }

    /**
     * A reload has just STARTED -- the only one of the three that reports a success.
     *
     * <p>Throttled on the same key as {@link #reloading}, deliberately: they are one event seen from
     * two sides ("it began" / "it is still going"), so sharing a bucket means a player who starts a
     * reload and immediately presses fire gets one message rather than two in the same tick.
     */
    public static void reloadStarted(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, RELOADING_KEY)) return;
        player.sendMessage(Component.text("Reloading...", NamedTextColor.GRAY));
        player.playSound(player.getLocation(), RELOAD_SOUND, SOUND_VOLUME, SOUND_PITCH);
    }

    /**
     * A reload is already running. Carries the remaining time, so the refusal has an end in sight.
     *
     * <p>No sound: this fires while the player is holding fire through a reload they started
     * deliberately, so it is the most repeated of the three notices and the least informative. A
     * click every two seconds for something the player already knows is noise.
     */
    public static void reloading(Player player, CooldownTracker cooldowns, long ticksRemaining) {
        if (!throttled(player, cooldowns, RELOADING_KEY)) return;
        player.sendMessage(Component.text(
                "Reloading -- %.1fs".formatted(ticksRemaining / 20.0), NamedTextColor.GRAY));
    }

    /** True if this notice may be sent now; stamps the window as a side effect when it may. */
    private static boolean throttled(Player player, CooldownTracker cooldowns, String key) {
        if (!cooldowns.isReady(player.getUniqueId(), key)) return false;
        cooldowns.trigger(player.getUniqueId(), key, THROTTLE_TICKS);
        return true;
    }
}
