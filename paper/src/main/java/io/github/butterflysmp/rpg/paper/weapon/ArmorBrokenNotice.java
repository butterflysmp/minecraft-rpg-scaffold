package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * D1' (2026-11-25): the notice a piece of armor sends the moment it breaks, the shield's {@link ShieldBrokenNotice}
 * mirrored: the same throttle through the player's {@code CooldownTracker} (40 ticks, its own key), the same sound,
 * and a message that says what a broken piece stops doing. Wording SEAT PLACEHOLDER 2026-11-25, OPEN FOR BEN.
 */
public final class ArmorBrokenNotice {

    private ArmorBrokenNotice() {}

    private static final String KEY = "__broken_armor_notice";
    private static final int THROTTLE_TICKS = 40;
    private static final String BROKEN_SOUND = "entity.item.break";
    private static final float BROKEN_SOUND_VOLUME = 1.0f;
    private static final float BROKEN_SOUND_PITCH = 1.0f;

    /** The message, public so the gate predicts it from the one constant. SEAT PLACEHOLDER 2026-11-25. */
    public static final String MESSAGE = "A piece of your armor is broken -- it no longer protects you. Repair it.";

    public static void notify(Player player, CooldownTracker cooldowns) {
        if (!cooldowns.isReady(player.getUniqueId(), KEY)) return;
        cooldowns.trigger(player.getUniqueId(), KEY, THROTTLE_TICKS);
        player.sendMessage(Component.text(MESSAGE, NamedTextColor.GRAY));
        player.playSound(player.getLocation(), BROKEN_SOUND, BROKEN_SOUND_VOLUME, BROKEN_SOUND_PITCH);
    }
}
