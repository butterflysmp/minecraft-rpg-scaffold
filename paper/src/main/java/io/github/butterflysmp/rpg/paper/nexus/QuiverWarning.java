package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.core.weapon.QuiverMirror;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.weapon.QuiverItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * THE WARNING WHILE THE STAR IS OFF (Ben, 2026-11-22: "Fall back and add a warning message that shows only to the
 * player. Only show the message when the player have a quiver-based weapon in their inventory and their nexus menu is
 * off"). The decision is core's ({@link QuiverMirror#warnNow}); this observes, on the stats-bar beat, and sends.
 *
 * <p>SEAT PLACEHOLDERS 2026-11-22, OPEN FOR BEN: the wording ({@link #TEXT}); that it is a CHAT line to him alone
 * ({@code Player#sendMessage}), not the action bar; and when it is sent: at join, when he turns the star off, and when a
 * quiver weapon first reaches his inventory, never more than once per such event.
 *
 * <p>Per-player state is the previous observation (did he carry a quiver weapon, was the star on), concurrent because
 * players beat on their own region threads; forgotten at quit, so a rejoin is a new JOIN.
 */
public final class QuiverWarning {

    /** SEAT PLACEHOLDER 2026-11-22: the words. */
    public static final String TEXT =
            "Your Nexus menu is off, so your quiver's rounds are not shown in its slot. Turn it back on in /menu, Settings.";

    private record Seen(boolean had, boolean starOn) {}

    private final Map<UUID, Seen> last = new ConcurrentHashMap<>();
    private final Keys keys;
    private final Predicate<UUID> starOn;

    public QuiverWarning(Keys keys, Predicate<UUID> starOn) {
        this.keys = keys;
        this.starOn = starOn;
    }

    /** One observation, on the player's own thread (the stats-bar beat). Sends the warning when the decision says so. */
    public void observe(Player player) {
        UUID id = player.getUniqueId();
        boolean has = hasQuiverWeapon(player, keys);
        boolean on = starOn.test(id);
        Seen previous = last.put(id, new Seen(has, on));
        if (QuiverMirror.warnNow(previous == null ? null : previous.had(), previous == null ? null : previous.starOn(),
                has, on)) {
            player.sendMessage(Component.text(TEXT, NamedTextColor.YELLOW));
        }
    }

    /** At quit: the next login is a new JOIN. */
    public void forget(UUID id) {
        last.remove(id);
    }

    /** A quiver weapon anywhere in his inventory: an item carrying a stamped quiver count (a minted quiver weapon). */
    static boolean hasQuiverWeapon(Player player, Keys keys) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && QuiverItems.loadedIn(item, keys).isPresent()) return true;
        }
        return false;
    }
}
