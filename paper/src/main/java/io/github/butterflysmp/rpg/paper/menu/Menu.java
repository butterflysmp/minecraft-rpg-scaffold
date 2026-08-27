package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * A custom chest menu, and the project's one place where clicking a container is decided.
 *
 * <p><b>Identity is THIS OBJECT</b>, reached through {@code inventory.getHolder()} -- never the
 * title. A title is a string a resource pack can change, a second menu can duplicate, and a player
 * can be tricked by; a holder is the menu. That also means there is no registry to keep in step:
 * the router asks the inventory who owns it and the answer cannot be stale.
 *
 * <p><b>Cancellation is not a subclass's decision.</b> {@link #handleClick} is final and cancels
 * FIRST, unconditionally, before it looks at anything at all. A subclass never sees the event -- it
 * declares which slots hold player items via {@link #inputSlots()}, vets arrivals in
 * {@link #acceptsInput}, and answers {@link #onClick} for everything else. So "forgot to cancel" is
 * not a mistake this class permits, which is the only way a rule survives its fifth consumer.
 *
 * <p>The whole reason the base exists: the anvil UI, the class-select screen and the stat screen
 * are all coming, and each one re-solving shift-click, drag and hotbar-swap is how the same
 * duplication bug gets written three times.
 */
public abstract class Menu implements InventoryHolder {

    private final Inventory inventory;
    protected final Player viewer;

    /**
     * @param size must be a multiple of nine, 9..54, as a chest inventory is.
     */
    protected Menu(Player viewer, int size, Component title) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    public final void open() {
        viewer.openInventory(inventory);
    }

    /**
     * The slots a player may put an item INTO or take one OUT OF. Empty by default, so a menu is
     * a display case until it says otherwise -- the safe direction to be wrong in.
     */
    protected Set<Integer> inputSlots() {
        return Set.of();
    }

    /**
     * May this item be placed in an input slot? Asked with the item still on the CURSOR, before the
     * place is permitted.
     *
     * <p>Returning false leaves the click cancelled, so the item never moves: "give it back
     * intact" is literally the place that never happened, and there is nothing to hand back.
     * Refusing here rather than accepting-then-ejecting is also why no scheduler hop is needed --
     * the alternative has a window in which the menu holds an item it has decided it does not want.
     *
     * <p>This is asked about the ITEM only. Whether the slot is already occupied is the router's
     * question, not the menu's.
     */
    protected boolean acceptsInput(ItemStack cursor) {
        return true;
    }

    /** A click the framework has already made safe. Never called for a click it fully handled. */
    protected abstract void onClick(MenuClick click);

    /**
     * The menu is closing, for ANY reason: Esc, a close button, death, a disconnect, or shutdown.
     * Return whatever the player owns.
     *
     * <p><b>Must be IDEMPOTENT.</b> That requirement is what lets the four abnormal exits be
     * handled independently -- none of them has to agree about which fires, in what order, or
     * whether the close event reaches us at all during a shutdown.
     */
    protected abstract void onClose(InventoryCloseEvent.Reason reason);

    public final void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);      // FIRST. Nothing below can make this not have happened.
        MenuClick click = MenuRouting.route(event, this);
        if (click != null) onClick(click);
    }

    /**
     * Any drag touching the menu is refused outright.
     *
     * <p>{@code getRawSlots()} spans BOTH inventories in one event, and a mixed drag has no safe
     * partial reading -- vanilla would spread the cursor stack across the filler panes. A drag
     * entirely inside the player's own inventory is left alone, because it is none of our business.
     */
    public final void handleDrag(InventoryDragEvent event) {
        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public final void handleClose(InventoryCloseEvent event) {
        onClose(event.getReason());
    }

    /**
     * Give back everything of the player's this menu is holding: the input slots, and the cursor.
     *
     * <p>IDEMPOTENT, by clearing each slot BEFORE handing its contents over. A second call finds
     * air. Clear-then-give rather than give-then-clear is the ordering that matters: the reverse
     * duplicates the item if the give throws or the close re-enters.
     *
     * <p>Public so shutdown can call it DIRECTLY rather than relying on a close event still being
     * routed while the plugin is disabling.
     */
    public final void returnEverything() {
        for (int slot : inputSlots()) {
            ItemStack held = inventory.getItem(slot);
            inventory.setItem(slot, null);
            MenuSafety.give(viewer, held);
        }

        ItemStack cursor = viewer.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            viewer.setItemOnCursor(null);
            MenuSafety.give(viewer, cursor);
        }
    }
}
