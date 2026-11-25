package io.github.butterflysmp.rpg.paper.summon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * THE REFUSED SUMMON'S ONE NOTICE, THROTTLED (O4, the seat, 2026-11-24). READ: the throttle this project already has for
 * a refusal shown while a button is held is {@code BrokenNotice}'s and {@code QuiverNotice}'s: the player's
 * {@link CooldownTracker} under a reserved key. This is THAT mechanism, not a second one. (READ too: the cooldown and
 * no-mana notices are not throttled at all; they send on every press.)
 */
public final class SummonNotice {

    private SummonNotice() {}

    /** A reserved key in the player's cooldown tracker; no ability id can collide (BrokenNotice's convention). */
    static final String KEY = "__summon_refused_notice";
    /** BrokenNotice's 40 ticks. SEAT PLACEHOLDER with the wording. */
    static final int THROTTLE_TICKS = 40;

    public static void notify(Player player, CooldownTracker cooldowns, String notice) {
        if (!cooldowns.isReady(player.getUniqueId(), KEY)) return;
        cooldowns.trigger(player.getUniqueId(), KEY, THROTTLE_TICKS);
        player.sendMessage(Component.text(notice, NamedTextColor.RED));
    }
}
