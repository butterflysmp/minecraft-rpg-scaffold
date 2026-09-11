package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * The quiver's live state on a held weapon: whether it may fire, spending a round, starting and
 * finishing a reload.
 *
 * <p>{@link QuiverItems} is the storage half -- keys in and out of item meta. This is the half that
 * knows what the stored values MEAN together, and it is where the lazy reload lives.
 *
 * <p>Every method here must run on the thread that owns the player: they read and write the main
 * hand. Both call sites ({@code WeaponFire.attempt}, {@code WeaponSwingListener.onSwing} past its
 * Netty hop) already are.
 *
 * <h2>THE RELOAD IS EVALUATED ON READ, NOT SCHEDULED</h2>
 *
 * <p>No task is queued when a reload starts. {@link #refusalFor} asks the item whether its deadline
 * has passed and completes it in place if so. <b>That is what makes the reload leak-proof</b>: a
 * scheduled task needs an expiry event, and an expiry event can be missed -- the player swaps the
 * weapon away, drops it, dies, logs out. {@code ModifierReconciler}'s javadoc makes the identical
 * argument for diffing over listening: <i>"a single missed event LEAKS."</i> There is nothing here
 * to miss, because the state is simply read again from whatever item is in hand.
 *
 * <p>It also settles, for free, the case a per-player timer gets wrong: cooldowns are keyed per
 * player per {@code weaponId/input} and could not tell two identical quivers apart, so a swap
 * mid-reload would fill the wrong weapon. On the item, it cannot.
 */
public final class Quivers {

    private Quivers() {}

    /**
     * Why this weapon may not fire right now, or empty if it may.
     *
     * <p>Completes a finished reload as a side effect, which is the whole lazy-evaluation design: the
     * read IS the tick. A caller that gets {@code Optional.empty()} back has a loaded weapon, and the
     * item has already been updated if a reload matured on this call.
     */
    public static Optional<CastResult> refusalFor(Player player, WeaponDefinition weapon,
                                                  AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack held = player.getInventory().getItemInMainHand();
        long now = Bukkit.getCurrentTick();

        Long startedAt = read(held, keys.quiverReloadStartedAt);
        Long completesAt = read(held, keys.quiverReloadCompletesAt);
        if (startedAt != null && completesAt != null) {
            if (!Quiver.reloadComplete(now, startedAt, completesAt)) {
                return Optional.of(new CastResult.Reloading(
                        Quiver.reloadTicksRemaining(now, startedAt, completesAt)));
            }
            finishReload(player, held, weapon, keys);
            return Optional.empty();   // it matured on this very read, so the shot goes through
        }

        OptionalInt loaded = QuiverItems.loadedIn(held, keys);
        if (loaded.isEmpty()) {
            // ABSENCE IS NOT EMPTINESS, and conflating them is the trap this whole design is built
            // around. A weapon whose definition declares a quiver but whose item carries no count
            // was never stamped -- a defect in a mint path, not a spent magazine. Refusing to fire
            // would hide it behind a message that reads perfectly reasonable; stamping it full says
            // so loudly and leaves the weapon usable.
            adapters.warnOnce("weapon '" + weapon.id() + "' in "
                    + player.getName() + "'s hand declares a quiver but carries NO count -- it was"
                    + " minted by a path that does not stamp one. Treating it as full; this is a"
                    + " defect in that mint path, not in the item.");
            held.editMeta(meta -> QuiverItems.stampFull(meta, weapon, keys));
            player.getInventory().setItemInMainHand(held);
            return Optional.empty();
        }

        return Quiver.isEmpty(loaded.getAsInt())
                ? Optional.of(new CastResult.Empty())
                : Optional.empty();
    }

    /** Spend one round off the held weapon. Called only after a Success. */
    public static void spendRound(Player player, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack held = player.getInventory().getItemInMainHand();
        OptionalInt loaded = QuiverItems.loadedIn(held, keys);
        if (loaded.isEmpty()) return;   // warned about in refusalFor; never silently invent a count

        int spent = Quiver.spend(loaded.getAsInt());
        held.editMeta(meta -> meta.getPersistentDataContainer()
                .set(keys.quiverLoaded, PersistentDataType.INTEGER, spent));
        // Write the stack back explicitly rather than trusting the main-hand read to be a live
        // mirror, and updateInventory so the tooltip moves on this shot -- the same pair, for the
        // same reasons, as WeaponDurability.applyWearOnUse.
        player.getInventory().setItemInMainHand(held);
        player.updateInventory();
    }

    /**
     * Begin a reload on the held weapon, if one is warranted.
     *
     * @return true if a reload actually started -- so the caller can speak only on a real transition
     *         rather than on every one of the ~20 left-click packets a held button produces each
     *         second.
     */
    public static boolean beginReload(Player player, WeaponDefinition weapon, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack held = player.getInventory().getItemInMainHand();
        long now = Bukkit.getCurrentTick();

        // Already reloading: the held-input case, and the one that must NOT restart the timer. A
        // player holding left-click would otherwise reload forever, each packet pushing the deadline
        // another reload_ticks away -- a weapon that never comes back, with no error anywhere.
        Long startedAt = read(held, keys.quiverReloadStartedAt);
        Long completesAt = read(held, keys.quiverReloadCompletesAt);
        if (startedAt != null && completesAt != null) {
            if (!Quiver.reloadComplete(now, startedAt, completesAt)) return false;
            finishReload(player, held, weapon, keys);
            return false;
        }

        // Already full: nothing to do, and saying so is better than a 3-second dead weapon for a
        // player who pressed reload out of habit.
        OptionalInt loaded = QuiverItems.loadedIn(held, keys);
        if (loaded.isPresent() && loaded.getAsInt() >= weapon.quiverSize()) return false;

        held.editMeta(meta -> {
            meta.getPersistentDataContainer().set(keys.quiverReloadStartedAt,
                    PersistentDataType.LONG, now);
            meta.getPersistentDataContainer().set(keys.quiverReloadCompletesAt,
                    PersistentDataType.LONG, Quiver.reloadCompletesAt(now, weapon.reloadTicks()));
        });
        player.getInventory().setItemInMainHand(held);
        player.updateInventory();
        return true;
    }

    /**
     * Start a reload if the player's held item is a quiver weapon that wants one.
     *
     * <p>The swing listener's entry point: it holds a {@code Player} and no definition, so the
     * held-weapon resolution lives here rather than being a second copy at the call site.
     *
     * @return true only on a real transition -- a reload that actually began.
     */
    public static boolean tryReloadHeldWeapon(Player player, WeaponRegistry weapons,
                                              AdapterContext adapters) {
        return WeaponItems.heldWeaponId(player, adapters.keys())
                .flatMap(weapons::find)
                .filter(WeaponDefinition::hasQuiver)
                .map(weapon -> beginReload(player, weapon, adapters))
                .orElse(false);
    }

    /** Stamp the magazine full and clear the reload pair. Both keys go together or neither does. */
    private static void finishReload(Player player, ItemStack held, WeaponDefinition weapon, Keys keys) {
        held.editMeta(meta -> {
            QuiverItems.stampFull(meta, weapon, keys);
            meta.getPersistentDataContainer().remove(keys.quiverReloadStartedAt);
            meta.getPersistentDataContainer().remove(keys.quiverReloadCompletesAt);
        });
        player.getInventory().setItemInMainHand(held);
        player.updateInventory();
    }

    private static Long read(ItemStack item, org.bukkit.NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.LONG);
    }
}
