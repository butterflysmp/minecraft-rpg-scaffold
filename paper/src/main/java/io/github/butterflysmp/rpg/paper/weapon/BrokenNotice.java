package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Telling a player their weapon is broken, without spamming them.
 *
 * Private to the holder, like the dry-pool denial it sits beside -- a plain {@code sendMessage},
 * never a broadcast.
 *
 * <p><b>Why this is throttled at all.</b> Both paths it serves are deliberately silent by default,
 * and for good reasons that a per-action message would trample:
 *
 * <ul>
 *   <li>{@code WeaponSwingListener.onSwing} discards its result -- "a swing that lands nothing
 *       because you are mid-cooldown or out of energy does not deserve chat spam". Holding
 *       left-click is one swing per tick.</li>
 *   <li>{@code RpgListeners.onRightClick} silences refusals when the press IS a basic attack, which
 *       the hunters_bow's shot is. Holding fire is attacking, not repeatedly deciding to cast.</li>
 * </ul>
 *
 * Broken has to speak on both -- a weapon that does nothing with no explanation is
 * indistinguishable from a bug -- so it bypasses those silences and pays for it with a throttle
 * instead. First blocked action reports; holding the button stays quiet; a later attempt reminds.
 *
 * <p>The throttle reuses {@link CooldownTracker} rather than adding a second per-player map: it is
 * already concurrent (Folia has two players on two region threads), already keyed per player, and
 * already cleared on quit by {@code RpgListeners.onQuit}, so this adds no state that can leak.
 */
public final class BrokenNotice {

    private BrokenNotice() {}

    /**
     * The tracker key. Leading underscores so it can never collide with a real ability id -- those
     * come from content filenames, and the loader would have to accept a file called
     * {@code __broken_weapon_notice.yml} for this bucket to be shared with a genuine cooldown.
     */
    private static final String KEY = "__broken_weapon_notice";

    /** Roughly two seconds. Long enough that held input is quiet, short enough to feel responsive. */
    private static final int THROTTLE_TICKS = 40;

    /**
     * Tell {@code player} their weapon is broken, at most once per {@link #THROTTLE_TICKS}.
     *
     * Must run on the thread that owns the player -- every caller is already there (the swing
     * listener via its Netty hop, the interact and damage handlers by virtue of being events).
     */
    public static void notify(Player player, CooldownTracker cooldowns) {
        if (!cooldowns.isReady(player.getUniqueId(), KEY)) return;
        cooldowns.trigger(player.getUniqueId(), KEY, THROTTLE_TICKS);
        player.sendMessage(Component.text(
                "Your weapon is broken -- repair it before using it.", NamedTextColor.GRAY));
    }
}
