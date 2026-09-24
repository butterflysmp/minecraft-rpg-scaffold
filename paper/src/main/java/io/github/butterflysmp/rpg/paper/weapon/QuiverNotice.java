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

    // *** THE ACTION BAR, NOT CHAT, AND THE WORDS ARE UNCHANGED. *** Ben's pick, 2026-09-23. A
    // magazine state is transient, so it belongs where transient things go; four of these in chat
    // is four permanent lines about a condition that lasted two seconds.
    //
    // THE THROTTLE IS STILL 40 TICKS AND STILL LOAD-BEARING. An action bar overwrites rather than
    // accumulating, so the SPAM is invisible here in a way it was not in chat -- which makes it
    // tempting to drop the throttle. Do not: these fire from a right-click handler and from a swing
    // path that sees roughly twenty arm-swing packets a second, and an action bar rewritten every
    // tick is a flickering one. The throttle is what makes it hold still long enough to read.
    //
    // *** AND THERE IS DELIBERATELY NO LIVE PER-TICK COUNTDOWN. *** The hotbar cooldown sweep on the
    // weapon itself is the progress indicator -- see QuiverSweep -- so a second, textual countdown
    // would be a duplicate readout of one fact, with two chances to disagree. `reloading` below
    // still names a remaining time because it answers a PRESS ("how long?"), not because anything
    // polls; it fires only when the player asks and only once per 40 ticks.

    /** Leading underscores so no content filename can collide -- {@code BrokenNotice}'s convention. */
    private static final String EMPTY_KEY = "__quiver_empty_notice";
    private static final String RELOADING_KEY = "__quiver_reloading_notice";

    /**
     * ITS OWN KEY, and this is not tidiness.
     *
     * <p>{@link #noAmmo} and {@link #empty} are DIFFERENT refusals with different remedies — "the
     * magazine is spent, reload" versus "there is room but you have no arrows" — and a player can hit
     * both inside one fight: fire dry, get {@link #empty}, press reload, get this.
     *
     * <p><b>Sharing {@link #EMPTY_KEY} would make the second one silent</b>, and the player would see
     * "left-click to reload", do exactly that, and be told nothing at all. The bug would appear only
     * in the one sequence that matters and would look like the reload key not working.
     *
     * <p>Contrast {@link #RELOADING_KEY}, which IS shared between {@link #reloadStarted} and
     * {@link #reloading} — deliberately, because those two are one event seen from two sides. The
     * test is whether the two messages answer the same question, not whether they are adjacent.
     */
    private static final String NO_AMMO_KEY = "__quiver_no_ammo_notice";

    /** {@code BrokenNotice}'s window, deliberately: held fire quiet, still responsive. */
    private static final int THROTTLE_TICKS = 40;

    private static final String EMPTY_SOUND = "block.dispenser.fail";
    private static final String RELOAD_SOUND = "item.crossbow.loading_start";
    private static final float SOUND_VOLUME = 1.0f;
    private static final float SOUND_PITCH = 1.0f;

    /** The magazine is spent. Says what to DO, because a refusal with no remedy reads as a bug. */
    public static void empty(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, EMPTY_KEY)) return;
        player.sendActionBar(Component.text(
                "Your quiver is empty -- left-click to reload.", NamedTextColor.GRAY));
        // Past the throttle check so message and sound are one notification and cannot drift apart,
        // exactly as BrokenNotice pairs them. Player#playSound, not World#playSound: the holder
        // hears it, nobody else does.
        player.playSound(player.getLocation(), EMPTY_SOUND, SOUND_VOLUME, SOUND_PITCH);
    }

    /**
     * There is ROOM in the magazine but no arrows to put in it. Ruling 4: no arrows, no fire.
     *
     * <p><b>Its own words, never "already full" and never the empty-magazine line.</b> A refusal that
     * reuses another refusal's message is a bug report waiting to be filed — "already full" on a
     * weapon reading 7/8 is exactly the kind of thing a player screenshots, and "your quiver is empty,
     * left-click to reload" is worse, because the player has just done that.
     *
     * <p>Names the remedy, like its siblings: a refusal with no remedy reads as a bug. And it names
     * the item exactly, because <b>plain arrows only</b> — a player holding a stack of spectral arrows
     * and being told "you need arrows" would reasonably think the feature was broken.
     */
    public static void noAmmo(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, NO_AMMO_KEY)) return;
        player.sendActionBar(Component.text(
                "You have no arrows -- plain Arrows load a quiver.", NamedTextColor.GRAY));
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
        player.sendActionBar(Component.text("Reloading...", NamedTextColor.GRAY));
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
        player.sendActionBar(Component.text(
                "Reloading -- %.1fs".formatted(ticksRemaining / 20.0), NamedTextColor.GRAY));
    }

    /** True if this notice may be sent now; stamps the window as a side effect when it may. */
    private static boolean throttled(Player player, CooldownTracker cooldowns, String key) {
        if (!cooldowns.isReady(player.getUniqueId(), key)) return false;
        cooldowns.trigger(player.getUniqueId(), key, THROTTLE_TICKS);
        return true;
    }
}
