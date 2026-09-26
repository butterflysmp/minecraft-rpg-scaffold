package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * Why an Ability Stone input did not cast: an ACTION-BAR line, never chat (PLAN-build-system.md section
 * 2.5, and the seat's slice-1 rule 3). {@code QuiverNotice}'s shape.
 *
 * <p><b>THROTTLED, AND IT HAS TO BE.</b> Ruling 19 makes every held input try the cast, and a held left
 * click on a block swings EVERY tick (measured, section 3.1.0.1) -- so an unthrottled refusal would
 * rewrite the action bar twenty times a second. {@link #THROTTLE_TICKS} is short enough that the
 * seconds-left figure still visibly counts down.
 *
 * <p><b>Known collision, stated:</b> {@code StatsBarSystem} also writes the action bar periodically and
 * will overwrite this line. {@code QuiverNotice} lives with the same thing; gate rows read that the line
 * is VISIBLE, not how long it lasts.
 */
public final class StoneNotice {

    private StoneNotice() {}

    // Three keys, each its own bucket (NoticeThrottleKeysTest): a player who is both short of mana and
    // then on cooldown must see the second line, not have it swallowed by the first one's window.
    private static final String COOLDOWN_KEY = "__stone_cooldown_notice";
    private static final String MANA_KEY = "__stone_mana_notice";
    private static final String NO_LOADOUT_KEY = "__stone_no_loadout_notice";
    private static final String EMPTY_SLOT_KEY = "__stone_empty_slot_notice";

    static final int THROTTLE_TICKS = 10;

    public static void onCooldown(Player player, CooldownTracker cooldowns, String abilityDisplayName,
                                  long ticksRemaining) {
        if (!throttled(player, cooldowns, COOLDOWN_KEY)) return;
        player.sendActionBar(MiniMessage.miniMessage().deserialize(abilityDisplayName)
                .append(Component.text(" -- ready in %.1fs".formatted(ticksRemaining / 20.0), NamedTextColor.GRAY)));
    }

    public static void notEnoughMana(Player player, CooldownTracker cooldowns, String abilityDisplayName,
                                     double required, double available) {
        if (!throttled(player, cooldowns, MANA_KEY)) return;
        player.sendActionBar(MiniMessage.miniMessage().deserialize(abilityDisplayName)
                .append(Component.text(" -- needs %.0f mana, you have %.0f".formatted(required, available),
                        NamedTextColor.GRAY)));
    }

    /**
     * No loadout: class or element is {@code none}, or the cell has no pool. It points at the Build screen,
     * the only place a class and an element are chosen (slice 3).
     */
    public static void noLoadout(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, NO_LOADOUT_KEY)) return;
        player.sendActionBar(Component.text(
                "Choose a class and an element first: open the Nexus, then Build.", NamedTextColor.YELLOW));
    }

    /**
     * The input's slot is empty: a SAVED loadout names an ability its pool no longer offers there
     * ({@code LoadoutResolution}). The Build screen is where a slot is filled
     * (slice 3).
     */
    public static void emptySlot(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, EMPTY_SLOT_KEY)) return;
        player.sendActionBar(Component.text("Nothing is equipped in that slot.", NamedTextColor.GRAY));
    }

    private static boolean throttled(Player player, CooldownTracker cooldowns, String key) {
        if (!cooldowns.isReady(player.getUniqueId(), key)) return false;
        cooldowns.trigger(player.getUniqueId(), key, THROTTLE_TICKS);
        return true;
    }
}
