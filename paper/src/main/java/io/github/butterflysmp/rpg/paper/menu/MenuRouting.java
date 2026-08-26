package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * The click whitelist: the one page that decides whether anything is allowed to move.
 *
 * <p><b>A WHITELIST, not a blacklist, and that is the load-bearing decision here.</b> A blacklist
 * of dangerous actions is one Minecraft drop away from being incomplete -- a new
 * {@code InventoryAction} constant would fall through it as permitted. Everything below either
 * matches a named permitted shape or ends up cancelled, so a constant nobody has heard of yet lands
 * in the safe arm by construction.
 *
 * <p>{@link Menu#handleClick} has ALREADY cancelled the event before this is called. Nothing here
 * cancels; it only ever UN-cancels, on two paths. That is what makes a {@code return} added later
 * by someone who has not read this safe.
 */
final class MenuRouting {

    private MenuRouting() {}

    /**
     * Actions that reach across BOTH inventories at once, and are therefore refused wherever the
     * click landed -- including in the player's own inventory.
     *
     * <p>{@code COLLECT_TO_CURSOR} is the one that would actually be exploited, and it is the
     * reason this set is checked before anything else. Double-clicking an ordinary glass pane in
     * YOUR OWN inventory sweeps every matching stack out of the top inventory too, and this menu
     * paints some forty identical filler panes. A router that only cancelled top-inventory clicks
     * would leave that wide open, because the clicked inventory is the bottom one.
     *
     * <p>{@code MOVE_TO_OTHER_INVENTORY} is shift-click, whose destination the SERVER picks across
     * the whole other inventory -- so permitting it would be permitting a slot chosen by someone
     * else. The hotbar swaps move an item into a top slot with the cursor never involved.
     * {@code CLONE_STACK} is creative middle-click, which makes items out of nothing.
     */
    private static final Set<InventoryAction> ALWAYS_REFUSED = Set.of(
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.CLONE_STACK,
            InventoryAction.UNKNOWN);

    /**
     * The only two actions an input slot permits: put a whole stack in, take a whole stack out.
     *
     * <p>Whole-stack only. {@code PLACE_ONE} and {@code PICKUP_HALF} would let a player split a
     * stack across the boundary, and a slot holding "half a weapon" is a state nothing downstream
     * is written for.
     */
    private static final Set<InventoryAction> INPUT_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PLACE_ALL);

    /**
     * What a player may do inside their OWN inventory while a menu is open. They have to be able
     * to put down the weapon they just took out of the input slot, so this cannot be "nothing".
     * Everything genuinely dangerous was already refused above.
     */
    private static final Set<InventoryAction> OWN_INVENTORY_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_ONE,
            InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.DROP_ONE_SLOT, InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_CURSOR, InventoryAction.DROP_ALL_CURSOR,
            InventoryAction.NOTHING);

    /**
     * Route one click.
     *
     * @return the click the menu should act on, or {@code null} for "handled; nothing to dispatch".
     */
    static MenuClick route(InventoryClickEvent event, Menu menu) {
        // 1. Cross-inventory actions FIRST. The ones that matter most are clicked in the player's
        //    own inventory and would sail past every later gate.
        if (ALWAYS_REFUSED.contains(event.getAction())) return null;

        // 1b. And by TYPE as well, so none of these depends on which InventoryAction the server
        //     happened to resolve them to. A number-key press over an EMPTY slot, for instance,
        //     does not resolve to HOTBAR_SWAP, and refusing only the action would let it through.
        ClickType click = event.getClick();
        if (click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND
                || click == ClickType.DOUBLE_CLICK
                || click == ClickType.CREATIVE) {
            return null;
        }

        Inventory clicked = event.getClickedInventory();

        // 2. Outside the window entirely. Left cancelled: the cursor keeps what it holds and the
        //    close handler returns it. Dropping into the world would also be safe, but ONE return
        //    path is worth more than a second correct one.
        if (clicked == null) return null;

        // 3. The player's own inventory. Permitted for the plain moves, and never dispatched --
        //    a menu has no business reacting to a player tidying their own backpack.
        if (!clicked.equals(event.getView().getTopInventory())) {
            if (OWN_INVENTORY_ACTIONS.contains(event.getAction())) event.setCancelled(false);
            return null;
        }

        // 4. The menu itself.
        int slot = event.getRawSlot();
        if (menu.inputSlots().contains(slot) && click == ClickType.LEFT
                && INPUT_ACTIONS.contains(event.getAction())) {
            if (event.getAction() == InventoryAction.PLACE_ALL && !placeAllowed(event, menu, slot)) {
                return null;                       // stays cancelled: the place never happened
            }
            event.setCancelled(false);             // THE exception, and the only one
            return new MenuClick(slot, click, event.getAction(), true);
        }

        // Everything else in the menu -- including a RIGHT-click on that same input slot -- stays
        // cancelled and is handed over as a button press.
        return new MenuClick(slot, click, event.getAction(), false);
    }

    /**
     * May this place proceed? Two questions, and they are deliberately different questions.
     *
     * <p><b>Occupancy, which the menu is not asked about.</b> The target slot must be EMPTY.
     * Vanilla MERGES a place onto a matching stack rather than swapping, and two freshly minted
     * weapons of ours share identical meta -- so without this a cursor of one item passes every
     * validity check the menu could make and the slot still ends up holding two. Reading the slot
     * is reliable here: {@code InventoryClickEvent} fires BEFORE the place applies, so the slot
     * still holds its resting occupant and only the cursor's landing is pending.
     *
     * <p><b>Validity, which is the menu's own.</b> Asked with the item still on the CURSOR, which
     * is the only moment a refusal is free: the item never moves, so there is nothing to hand back
     * and no window in which the menu holds something it has already decided it does not want.
     * That is also why none of this needs a scheduler hop.
     */
    private static boolean placeAllowed(InventoryClickEvent event, Menu menu, int slot) {
        ItemStack resting = menu.getInventory().getItem(slot);
        if (resting != null && !resting.getType().isAir()) return false;
        return menu.acceptsInput(event.getCursor());
    }
}
