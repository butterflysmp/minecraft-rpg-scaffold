package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.QuiverState;
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
import java.util.OptionalLong;

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
        QuiverState state = stateOf(held, keys);

        // READ, DECIDE, DELIVER -- and the DECIDE is not here. The ordering of these cases is the
        // real content of this method, and every one of them is now a core row with a mutation
        // behind it (QuiverStateTest): a running reload beats empty, a MATURED reload fires rather
        // than dropping the press, an unstamped item is a defect and not an empty magazine.
        return switch (state.fireVerdict(now)) {
            case FIRE -> Optional.empty();
            case EMPTY -> Optional.of(new CastResult.Empty());
            case RELOADING -> Optional.of(new CastResult.Reloading(state.reloadTicksRemaining(now)));
            case RELOAD_MATURED -> {
                // The read IS the tick: the reload finished the moment anything looked. Refill and
                // let the shot through, so the press that matures a reload is not wasted.
                finishReload(player, held, weapon, keys);
                yield Optional.empty();
            }
            case UNSTAMPED -> {
                // A mint path failed to stamp a count. Refusing to fire would hide that behind a
                // message that reads perfectly reasonable; repairing it loudly leaves the weapon
                // usable and the defect visible. warnOnce because this runs on every shot.
                adapters.warnOnce("weapon '" + weapon.id() + "' declares a quiver but an item in"
                        + " play carries NO count -- it was minted by a path that does not stamp"
                        + " one. Treating it as full; the defect is in that mint path, not the item.");
                held.editMeta(meta -> QuiverItems.stampFull(meta, weapon, keys));
                player.getInventory().setItemInMainHand(held);
                yield Optional.empty();
            }
        };
    }

    /**
     * The three stored values as the one core type that knows what they mean together.
     *
     * <p>This is the whole of the READ half. The reload pair is taken together or not at all --
     * {@link QuiverState} refuses a half-reload outright, so a partially-written item surfaces as a
     * thrown exception here rather than as a weapon that behaves oddly.
     */
    private static QuiverState stateOf(ItemStack held, Keys keys) {
        OptionalInt loaded = QuiverItems.loadedIn(held, keys);
        Long startedAt = read(held, keys.quiverReloadStartedAt);
        Long completesAt = read(held, keys.quiverReloadCompletesAt);
        if (startedAt == null || completesAt == null) {
            return new QuiverState(loaded, OptionalLong.empty(), OptionalLong.empty());
        }
        return new QuiverState(loaded, OptionalLong.of(startedAt), OptionalLong.of(completesAt));
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

        // Same split as refusalFor: the verdict is core's, and each arm below is a core row.
        // ALREADY_RELOADING is the held-input case -- ~20 arm-swing packets a second, every one of
        // which would otherwise push the deadline another reload_ticks away and leave a weapon that
        // never comes back. ALREADY_FULL spares a habitual press three dead seconds.
        switch (stateOf(held, keys).reloadVerdict(now, weapon.quiverSize())) {
            case ALREADY_RELOADING, ALREADY_FULL -> { return false; }
            case RELOAD_MATURED -> {
                finishReload(player, held, weapon, keys);
                return false;
            }
            case UNSTAMPED -> {
                // Repaired on the firing path, which every quiver weapon reaches first; reloading a
                // never-stamped item is not the place to invent a count.
                return false;
            }
            case BEGIN -> { /* fall through to the write below */ }
        }

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
