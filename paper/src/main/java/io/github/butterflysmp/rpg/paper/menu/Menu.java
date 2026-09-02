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
     * How one input slot accepts items. Asked only about slots in {@link #inputSlots()}.
     *
     * <p>Defaults to {@link SlotPolicy#EXCLUSIVE}, which is today's rule and the conservative one:
     * one whole stack, into an empty slot, LEFT-click only. A menu that wants a vanilla-feeling
     * stacking grid says so per slot, so a menu can have both kinds -- an enchant-style single slot
     * beside a grid -- without either rule leaking into the other.
     *
     * <p>{@link #inputSlots()} stays the UNION of every slot holding a player's items, whatever
     * their policies. That is what keeps {@link #returnEverything}, the shift-move, the hotbar move
     * and the offhand move covering all of them: a second parallel set would leave stacking slots
     * out of the return path, and every close would silently eat what rested in them.
     */
    protected SlotPolicy slotPolicy(int slot) {
        return SlotPolicy.EXCLUSIVE;
    }

    /**
     * May a shift-click on this NON-INPUT menu slot be dispatched to {@link #onClick} as a button
     * press?
     *
     * <p>Default false: a display slot swallows shift-clicks, exactly as it always has. This exists
     * for a slot the menu OWNS and hands over itself -- a crafting result -- where shift-click means
     * "do it repeatedly" rather than "move this item".
     *
     * <p><b>DISPATCH ONLY. This must PERFORM NO MOVE.</b> Returning true routes the gesture to
     * {@link #onClick} with {@code itemMoved=false} and nothing else happens. It must never fall
     * through to the router's clear-and-give for an input slot: that would hand the player the
     * DISPLAY item and then whatever {@code onClick} produces on top of it, which is one free item
     * per shift-click. The obvious implementation of this hook is the broken one.
     */
    protected boolean shiftClickDispatches(int slot) {
        return false;
    }

    /**
     * A drag was PERMITTED and will land after {@link #handleDrag} returns. Default: nothing.
     *
     * <p>Exists because a permitted drag changes the menu's contents and dispatches nothing else. A
     * menu that derives anything from its own slots -- a crafting preview, a cost readout -- would
     * otherwise show a stale answer until the next click, and the drag path is rare enough that
     * nobody would notice for a long time.
     *
     * <p><b>The contents have NOT changed yet when this is called.</b> That is the same situation
     * {@code InventoryClickEvent} creates for a permitted place, and it wants the same answer: a
     * one-tick hop, through the {@code Scheduler}, before reading the slots. A subclass that reads
     * the inventory synchronously here sees the grid as it was BEFORE the drag.
     *
     * <p>No slot is passed, deliberately. A drag has many destinations and no single one, which is
     * why it cannot be reported through {@link MenuClick} -- that record's javadoc pins it to one
     * raw slot with {@code itemMoved} meaning a permitted put-in or take-out.
     */
    protected void onDragPermitted() {}

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
     * A drag is refused unless EVERY slot it touches is a stacking input slot.
     *
     * <p><b>This is not the "performed, never permitted" rule being broken, and the difference is
     * worth reading before changing it.</b> The router refuses shift-click, the number key and F as
     * gestures and PERFORMS them instead, on the stated principle that the SERVER must never pick
     * the destination -- it would scan a whole inventory for a shift-click, and swap two ways for a
     * number key. A drag is different in the one way that matters: {@code getRawSlots()}
     * ENUMERATES its destinations, chosen by the player, and hands them over BEFORE we decide. So
     * un-cancelling a drag whose every raw slot we have verified is CONSISTENT with that principle
     * rather than an exception to it. Nothing here is a slot chosen by someone other than us; we
     * simply agree with a list the player already made.
     *
     * <p>Cancelled FIRST and un-cancelled only at the end, the same shape {@link #handleClick} uses,
     * so a {@code return} added later by someone who has not read this is safe. Every one of the
     * four refusals below is a bare {@code return} onto an already-cancelled event.
     *
     * <p>A drag entirely inside the player's own inventory is left alone, because it is none of our
     * business -- that is the one path that never cancels.
     */
    public final void handleDrag(InventoryDragEvent event) {
        int topSize = event.getView().getTopInventory().getSize();

        boolean touchesMenu = false;
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                touchesMenu = true;
                break;
            }
        }
        if (!touchesMenu) return;

        event.setCancelled(true);      // FIRST. Every refusal below is now just a return.

        for (int raw : event.getRawSlots()) {
            // Spanning both halves. A mixed drag has no safe partial reading, and un-cancelling it
            // would let vanilla spread the stack across whatever the bottom half holds.
            if (raw >= topSize) return;

            // Chrome, filler and the result slot are display items the menu owns.
            if (!inputSlots().contains(raw)) return;

            // Exhaustive switch EXPRESSION, no default arm: a third SlotPolicy constant is a
            // compile error here rather than silently joining the permitted arm.
            boolean stacking = switch (slotPolicy(raw)) {
                case STACKING -> true;
                case EXCLUSIVE -> false;
            };
            if (!stacking) return;
        }

        // Asked ONCE about the dragged item, before anything lands -- the same moment, and the same
        // question, that placeAllowed asks for a click-place.
        if (!acceptsInput(event.getOldCursor())) return;

        event.setCancelled(false);

        // The menu's contents are about to change with nothing else dispatched. Told here rather
        // than left to be noticed, because a stale derived value on the rare drag path is a defect
        // that survives every amount of clicking.
        //
        // REJECTED, so it is not re-proposed: InventoryDragEvent.getNewItems() would let a subclass
        // project the post-drag contents synchronously with no scheduler hop. Cleverer, and wrong --
        // it would be a SECOND way of answering "the contents changed, recompute" beside the
        // one-tick hop a permitted CLICK already uses for the identical reason. Two copies of that
        // rule will drift, and the drifting one will be this path, because it is the one nobody
        // exercises.
        onDragPermitted();
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
