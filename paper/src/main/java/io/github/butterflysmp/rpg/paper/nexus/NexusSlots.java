package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.menu.MenuSafety;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * The boundary between Bukkit's view space and {@link NexusLock}'s index space, plus the join-time
 * convergence that makes the lock's job possible.
 *
 * <p>Everything that needs a live {@code Player}, {@code InventoryView} or {@code Inventory} lives
 * here, so that {@link NexusLock} needs none of them and has real unit tests. This class is the
 * part that cannot be unit-tested and is therefore kept as thin as it can be made -- the same trade
 * {@code GridClickIntent} makes against {@code MenuRouting}.
 */
public final class NexusSlots {

    private NexusSlots() {}

    // ------------------------------------------------------------------ the verdicts

    /** Would this click move the Nexus star? Pure translation; it changes nothing. */
    public static boolean refuses(InventoryClickEvent event, Keys keys) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;
        return NexusLock.refusesClick(
                event.getClick(),
                event.getAction(),
                touchedOf(event.getView(), event.getRawSlot()),
                event.getHotbarButton(),
                NexusItems.isNexus(event.getCursor(), keys),
                starAt(player, keys));
    }

    /**
     * Would this drag move the Nexus star?
     *
     * <p>{@code getOldCursor()} rather than {@code getCursor()}: the old cursor is what is being
     * distributed, and it is the drag's SOURCE. {@code getRawSlots()} names only destinations.
     */
    public static boolean refuses(InventoryDragEvent event, Keys keys) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;

        Set<NexusLock.Touched> dragged = new HashSet<>();
        for (int raw : event.getRawSlots()) dragged.add(touchedOf(event.getView(), raw));

        return NexusLock.refusesDrag(dragged,
                NexusItems.isNexus(event.getOldCursor(), keys),
                starAt(player, keys));
    }

    // ------------------------------------------------------------------ the conversion

    /**
     * One raw slot, converted into {@link NexusLock}'s {@code PlayerInventory} index space.
     *
     * <p><b>THIS IS THE ONLY PLACE THE TWO SPACES MEET, AND IT USES THE SANCTIONED PAIR.</b>
     * {@code view.getInventory(raw)} says WHICH inventory a raw slot belongs to and
     * {@code view.convertSlot(raw)} says its index within that inventory. Hand-rolled arithmetic
     * gets the own-inventory screen wrong, where the top is a 5-slot CRAFTING inventory and the
     * armour and offhand slots sit between the crafting grid and the storage rows.
     *
     * <p>A negative raw slot, or one belonging to no inventory, is an outside-the-window click --
     * it names no slot at all. Returned as {@code (false, -1)}, which the lock treats as untouched.
     * The gesture that matters on that path is the cursor DROP, and {@code NexusLock} identifies it
     * by {@code InventoryAction} rather than by slot for exactly this reason.
     */
    public static NexusLock.Touched touchedOf(InventoryView view, int rawSlot) {
        if (rawSlot < 0) return new NexusLock.Touched(false, -1);

        Inventory inventory = view.getInventory(rawSlot);
        if (inventory == null) return new NexusLock.Touched(false, -1);

        // The bottom inventory of ANY view is the player's own. Compared with equals rather than
        // instanceof, matching MenuRouting's own `clicked.equals(getView().getTopInventory())`.
        boolean isPlayers = inventory.equals(view.getBottomInventory());
        return new NexusLock.Touched(isPlayers, view.convertSlot(rawSlot));
    }

    /**
     * Does the given player-inventory index hold a Nexus star?
     *
     * <p>LAZY BY CONSTRUCTION. The lock asks about at most two indices per click, so this is at
     * most two {@code getItem} calls and two PDC reads -- not the 41-slot scan a {@code Set}
     * parameter would have made the natural implementation, on every click by every player.
     */
    public static IntPredicate starAt(Player player, Keys keys) {
        PlayerInventory inventory = player.getInventory();
        return index -> index >= 0 && index < inventory.getSize()
                && NexusItems.isNexus(inventory.getItem(index), keys);
    }

    // ------------------------------------------------------------------ convergence

    /**
     * Assert the invariant: EXACTLY ONE Nexus star, and it is at {@link NexusLock#LOCKED_SLOT}.
     *
     * <h2>WHY THIS IS NOT "MINT IF ABSENT"</h2>
     *
     * Refusals keep a correct state correct; they cannot repair a wrong one. A star that reaches
     * any other slot -- by an admin {@code /give}, a creative edit, a direct server-side
     * {@code setItem} (which raises no event at all and so cannot be refused), or simply a build
     * that predates this code -- would be welded THERE by the same rule, while the locked slot
     * refuses everything into it. Both halves would then work against the player, and mint-if-absent
     * would never fire because the star is not absent.
     *
     * <p>So this converges on the desired state instead, which is the shape
     * {@code HealthModifierItems.desiredModifiers} already uses here: <i>"whatever an item's
     * departure route -- drop, swap, break, death, /clear -- the slot simply no longer yields an
     * amount next scan. No departure event to miss."</i>
     *
     * <h2>THE GUARANTEE IS: NEVER DESTROYS A PLAYER'S ITEM</h2>
     *
     * Surplus stars ARE deleted, and that is not an exception to it. A second Nexus star is not the
     * player's property -- it is ours, plugin-minted, worth nothing, and re-minted free on the next
     * join. The DISPLACED OCCUPANT of the locked slot is the player's, and it goes through
     * {@code MenuSafety.give}, which finds a free slot, and failing that drops it at their feet
     * <i>and tells them so</i>. Mirrors {@code RpgCommand.grantWeapons} -- "never overwrites a held
     * item".
     *
     * <p>Called on join AND on respawn. Respawn because {@code onQuit} does not run on death and a
     * keepInventory failure, a cursor drop at death, or any future death path that loses the star
     * would otherwise leave the player without one until their next reconnect.
     */
    public static void converge(Player player, Keys keys) {
        PlayerInventory inventory = player.getInventory();

        List<Integer> stars = new ArrayList<>();
        for (int index = 0; index < inventory.getSize(); index++) {
            if (NexusItems.isNexus(inventory.getItem(index), keys)) stars.add(index);
        }

        // Surplus first, so the "already correct" test below cannot pass while a duplicate sits in
        // the backpack. Keep the lowest index and delete the rest; which one survives is arbitrary
        // because they are identical.
        for (int i = 1; i < stars.size(); i++) inventory.setItem(stars.get(i), null);

        int held = stars.isEmpty() ? -1 : stars.get(0);
        if (held == NexusLock.LOCKED_SLOT && stars.size() == 1) return;   // nothing to do

        // Take the existing star rather than minting a second, so a star that has been renamed,
        // re-tagged or otherwise touched is preserved as the one the player has.
        ItemStack star;
        if (held >= 0) {
            star = inventory.getItem(held);
            inventory.setItem(held, null);      // clear FIRST, so the slot counts as free below
        } else {
            star = NexusItems.mint(keys);
        }

        // Captured BEFORE the write, or it is gone.
        ItemStack occupant = inventory.getItem(NexusLock.LOCKED_SLOT);
        inventory.setItem(NexusLock.LOCKED_SLOT, star);

        // MenuSafety.isEmpty is the canonical copy -- absent, AIR, and zero-count husks all mean
        // nothing here, and testing only one of them is how a slot ends up holding an invisible
        // unclickable item. Do not write a fourth copy of it.
        if (!MenuSafety.isEmpty(occupant)) MenuSafety.give(player, occupant);

        player.updateInventory();
    }
}
