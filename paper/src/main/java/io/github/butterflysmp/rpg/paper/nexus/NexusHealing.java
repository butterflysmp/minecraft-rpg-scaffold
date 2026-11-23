package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.core.build.NexusHeal;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * THE NEXUS ITEM HEALS ON THE BEAT (the seat's ruling M, 2026-11-22): the paper half, which only EXECUTES core's
 * {@link NexusHeal}. Run on the stats bar's 10-tick beat, BEFORE {@link NexusMirror#redraw}, so the mirror is drawn on the
 * healed star in the same beat.
 *
 * <p>The call is the JOIN PATH's ({@code RpgListeners.onJoin}): the star is converged to his locked slot by
 * {@link NexusSlots#converge}. That call clears no star here (none was found) and mints one into the locked slot;
 * whatever already sat in that slot is handed to {@code MenuSafety.give} (READ): his inventory first, dropped at his
 * feet with a chat line only if it is full.
 *
 * <p><b>THE GUARD IS NOT THE JOIN PATH'S, AND THAT WAS THE SEAT'S ERROR (corrected 2026-11-22).</b> The join path reads
 * an absent profile as on, but only AFTER the load has settled ({@code profiles.whenSettled}), where absent means
 * "unreadable, permanently". This beat starts at join ({@code StatsBarSystem.onJoin}, its first beat 10 ticks later)
 * and nothing orders it after the load (READ, {@code RepeatingTask.start} arms one period and no more), so here absent
 * can mean "not yet". The heal therefore acts only on a LOADED profile that says the star is on.
 */
public final class NexusHealing {

    private NexusHealing() {}

    /** Heal when core says so; {@code log} receives one line per heal (the gate's witness). */
    public static void healIfMissing(Player player, Keys keys, ProfileService profiles, Consumer<String> log) {
        Optional<PlayerProfile> profile = profiles.profile(player.getUniqueId());
        boolean loaded = profile.isPresent();
        boolean starOn = profile.map(PlayerProfile::starEnabled).orElse(false);
        int found = countNexus(player.getInventory(), keys);
        if (!NexusHeal.heals(loaded, starOn, found, !player.isDead())) return;
        int slot = profile.map(PlayerProfile::nexusSlot).orElse(NexusLock.DEFAULT_LOCKED_SLOT);
        NexusSlots.converge(player, keys, slot);
        log.accept("NEXUSHEAL player=" + player.getName() + " slot=" + slot);
    }

    /** Every item carrying the Nexus tag, in any slot (armour and the off hand included). */
    public static int countNexus(PlayerInventory inventory, Keys keys) {
        int found = 0;
        for (int index = 0; index < inventory.getSize(); index++) {
            if (NexusItems.isNexus(inventory.getItem(index), keys)) found++;
        }
        return found;
    }
}
