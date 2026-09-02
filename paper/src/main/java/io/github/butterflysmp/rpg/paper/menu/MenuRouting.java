package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
 * cancels; it only ever UN-cancels, on one path, or performs a move ITSELF. That is what makes a
 * {@code return} added later by someone who has not read this safe.
 *
 * <p><b>What an input slot ACCEPTS is now per-slot</b>, via {@link Menu#slotPolicy}. The whitelist
 * property is unchanged: {@link GridClickIntent} names every permitted action for each policy and
 * everything else reaches {@code REFUSE}. The decision itself lives there rather than here, because
 * this class cannot be built in a unit test and that one can.
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
     * <p>{@code CLONE_STACK} is creative middle-click, which makes items out of nothing.
     *
     * <p><b>None of the three cross-inventory GESTURES is here, and their absence is not a
     * relaxation.</b> Shift-click, the number keys and F are what a player actually reaches for, so
     * all three are supported -- by being PERFORMED, in {@link #shiftMove}, {@link #hotbarMove} and
     * {@link #offhandMove}, never by being permitted. The objection that had them refused outright
     * still stands and is exactly why none is ever un-cancelled: the SERVER would pick the
     * destination -- across the whole other inventory for a shift-click, and as a two-way swap for
     * a number key or F -- which would be a slot chosen by someone other than us.
     *
     * <p>{@code HOTBAR_SWAP} and {@code HOTBAR_MOVE_AND_READD} are produced only by a number-key
     * press, and both that and F are now caught by TYPE before this set is consulted, so listing
     * them here would be dead weight that reads like a guard.
     */
    private static final Set<InventoryAction> ALWAYS_REFUSED = Set.of(
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.CLONE_STACK,
            InventoryAction.UNKNOWN);

    /**
     * What a player may do inside their OWN inventory while a menu is open. They have to be able
     * to put down the weapon they just took out of the input slot, so this cannot be "nothing".
     * Everything genuinely dangerous was already refused above.
     *
     * <p>{@code MOVE_TO_OTHER_INVENTORY} and the hotbar swaps must never be added here. All are
     * intercepted before this set is consulted, and adding any of them would hand the destination
     * back to the server.
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
        // 1. The two SWAP gestures, by TYPE, ahead of EVERYTHING -- including the action set.
        //    By type because neither resolves to a predictable action in the case that matters: a
        //    number-key press over an EMPTY slot does not resolve to HOTBAR_SWAP, so matching on
        //    the action alone would miss exactly the move we want to allow. Both are performed by
        //    us or not at all, and neither is ever un-cancelled -- vanilla's version of each is a
        //    two-way swap, which is two moves on rules that are not ours.
        ClickType click = event.getClick();
        if (click == ClickType.NUMBER_KEY) return hotbarMove(event, menu);
        if (click == ClickType.SWAP_OFFHAND) return offhandMove(event, menu);

        // 2. Cross-inventory actions. The ones that matter most are clicked in the player's own
        //    inventory and would sail past every later gate.
        if (ALWAYS_REFUSED.contains(event.getAction())) return null;

        // 2b. And by TYPE as well, so neither depends on which InventoryAction the server happened
        //     to resolve it to.
        if (click == ClickType.DOUBLE_CLICK || click == ClickType.CREATIVE) {
            return null;
        }

        Inventory clicked = event.getClickedInventory();

        // 3. Outside the window entirely. Left cancelled: the cursor keeps what it holds and the
        //    close handler returns it. Dropping into the world would also be safe, but ONE return
        //    path is worth more than a second correct one.
        if (clicked == null) return null;

        boolean inMenu = clicked.equals(event.getView().getTopInventory());

        // 4. Shift-click, BEFORE the own-inventory branch -- it is a cross-inventory move and has
        //    no business being judged as an ordinary click in whichever half it started from.
        //    Performed by us or not at all; never un-cancelled.
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return shiftMove(event, menu, inMenu);
        }

        // 5. The player's own inventory. Permitted for the plain moves, and never dispatched --
        //    a menu has no business reacting to a player tidying their own backpack.
        if (!inMenu) {
            if (OWN_INVENTORY_ACTIONS.contains(event.getAction())) event.setCancelled(false);
            return null;
        }

        // 6. The menu itself.
        int slot = event.getRawSlot();
        if (menu.inputSlots().contains(slot)) {
            return inputClick(event, menu, slot, click);
        }

        // Everything else in the menu -- including a RIGHT-click on that same input slot -- stays
        // cancelled and is handed over as a button press.
        return new MenuClick(slot, click, event.getAction(), false);
    }

    /**
     * A click on a declared input slot, decided by {@link GridClickIntent} and then carried out.
     *
     * <p>The decision is asked BEFORE anything moves, against the slot as it rests: an
     * {@code InventoryClickEvent} fires before the click applies, so the slot still holds its
     * occupant and only the incoming item's landing is pending.
     *
     * <p>{@code acceptsInput} is asked only when the answer depends on it, which
     * {@link GridClickIntent#consultsAcceptance} decides. That is not an optimisation: the method
     * is not a pure query -- {@code EnchantMenu}'s says a sentence in chat when it refuses -- so
     * asking it on a pickup would tell a player their own weapon is not one of their weapons as
     * they took it back out.
     */
    private static MenuClick inputClick(InventoryClickEvent event, Menu menu, int slot,
                                        ClickType click) {
        InventoryAction action = event.getAction();
        SlotPolicy policy = menu.slotPolicy(slot);

        ItemStack cursor = event.getCursor();
        ItemStack resting = menu.getInventory().getItem(slot);
        boolean restingEmpty = isEmpty(resting);
        boolean similar = !restingEmpty && !isEmpty(cursor) && resting.isSimilar(cursor);

        boolean accepted = GridClickIntent.consultsAcceptance(action, click, policy)
                && menu.acceptsInput(cursor);

        // Exhaustive switch EXPRESSION, no default arm: a sixth intent is a compile error here
        // until someone says what the router should DO about it.
        return switch (GridClickIntent.of(action, click, policy, restingEmpty, similar, accepted)) {
            case REFUSE -> null;                    // stays cancelled: the move never happened
            case PERMIT -> {
                event.setCancelled(false);          // vanilla applies it; both endpoints are fixed
                yield new MenuClick(slot, click, action, true);
            }
            case MERGE_ALL -> merge(event, menu, slot, Integer.MAX_VALUE);
            case MERGE_ONE -> merge(event, menu, slot, 1);
            case SWAP -> swapCursor(event, menu, slot);
        };
    }

    /**
     * Top a resting stack up from the cursor, by at most {@code limit} items.
     *
     * <p>Performed rather than permitted so the arithmetic is ours and can be asserted. The two
     * writes are computed from CLONES taken before either lands, so there is no window in which
     * the total is wrong: what leaves the cursor is exactly what arrives in the slot.
     *
     * <p>{@code getMaxStackSize()} is read from the RESTING item, not from its Material. A weapon
     * minted with {@code setMaxStackSize(1)} therefore has zero room and this refuses, which is the
     * same answer the enchant slot gives for its own reasons.
     */
    private static MenuClick merge(InventoryClickEvent event, Menu menu, int slot, int limit) {
        if (!(event.getWhoClicked() instanceof Player player)) return null;

        ItemStack cursor = event.getCursor();
        ItemStack resting = menu.getInventory().getItem(slot);
        if (isEmpty(cursor) || isEmpty(resting)) return null;

        int room = resting.getMaxStackSize() - resting.getAmount();
        int moved = Math.min(Math.min(limit, cursor.getAmount()), room);
        if (moved <= 0) return null;

        ItemStack topped = resting.clone();
        topped.setAmount(resting.getAmount() + moved);

        ItemStack remaining = cursor.clone();
        remaining.setAmount(cursor.getAmount() - moved);

        menu.getInventory().setItem(slot, topped);
        player.setItemOnCursor(remaining.getAmount() <= 0 ? null : remaining);
        player.updateInventory();
        return new MenuClick(slot, event.getClick(), event.getAction(), true);
    }

    /**
     * Exchange the cursor and a resting stack.
     *
     * <p><b>This is the one two-way move the router performs, and the reason it is safe is not the
     * reason the number key is refused.</b> {@link #swapWithInput} refuses a both-full number key
     * because that gesture names a SLOT and not a direction: both endpoints are resting storage and
     * there is no intent to infer. A cursor click has a designated incoming side -- the player is
     * holding one thing and clicking one destination -- and crosses no inventory boundary. The
     * server picks nothing here either.
     *
     * <p>Both clones are taken before either write, so the pair cannot be observed half-applied.
     */
    private static MenuClick swapCursor(InventoryClickEvent event, Menu menu, int slot) {
        if (!(event.getWhoClicked() instanceof Player player)) return null;

        ItemStack cursor = event.getCursor();
        ItemStack resting = menu.getInventory().getItem(slot);
        if (isEmpty(cursor) || isEmpty(resting)) return null;

        ItemStack incoming = cursor.clone();
        ItemStack outgoing = resting.clone();

        menu.getInventory().setItem(slot, incoming);
        player.setItemOnCursor(outgoing);
        player.updateInventory();
        return new MenuClick(slot, event.getClick(), event.getAction(), true);
    }

    /**
     * Move a weapon between an input slot and ONE other slot -- a hotbar slot, or the offhand --
     * in whichever direction is unambiguous.
     *
     * <p>The shared body of {@link #hotbarMove} and {@link #offhandMove}. Those two differ only in
     * WHICH slot is "the other one", so the rules live here once. Two copies of an in/out decision
     * is how the number key and the F key drift into disagreeing about what an input slot accepts
     * -- the same reason every inbound path shares {@link #placeAllowed}.
     *
     * <p><b>Exactly one side must hold something.</b> That single test gives both directions and
     * refuses vanilla's swap in the same breath: an empty input slot and a full other slot moves
     * IN, a full input slot and an empty other slot moves OUT, and the two remaining cases are
     * refused -- both full is the two-way swap vanilla would do, and both empty is nothing to move.
     *
     * <p><b>This rule does NOT change for a STACKING slot, and that is a decision rather than an
     * oversight.</b> A cursor click on an occupied grid slot merges or swaps because the cursor is
     * a designated incoming side. A number key or F names a SLOT, not a direction: both endpoints
     * are resting storage, so "exactly one side full" is how intent is inferred, and when both are
     * full there is no intent to infer. Vanilla guesses "swap", and that guess crosses an inventory
     * boundary in two directions at once.
     *
     * <p><b>Latent hazard if that is ever relaxed:</b> the IN branch below writes the slot BLINDLY,
     * which is safe only because {@code restingEmpty} has already been established. Under
     * {@code STACKING}, {@link #placeAllowed} now returns true for an occupied-but-similar slot, so
     * relaxing the one-side-full test without also teaching that write to merge would turn it into
     * a destructive overwrite of whatever was resting there.
     *
     * @param read  the other slot's current contents. May be air; never assumed non-null.
     * @param write sets the other slot. {@code null} clears it.
     */
    private static MenuClick swapWithInput(InventoryClickEvent event, Menu menu, Player player,
                                           int hovered, Supplier<ItemStack> read,
                                           Consumer<ItemStack> write) {
        ItemStack other = read.get();
        ItemStack resting = menu.getInventory().getItem(hovered);
        boolean otherEmpty = isEmpty(other);
        boolean restingEmpty = isEmpty(resting);

        // Both full, or both empty. Neither is a one-way move.
        if (otherEmpty == restingEmpty) return null;

        if (restingEmpty) {
            // IN: the SAME gate the click-place and the shift-click use, so every entry path agrees
            // about what an input slot takes.
            if (!placeAllowed(menu, hovered, other)) return null;
            // Cloned before the source is cleared, and the source cleared before the place: a live
            // view of a slot we are about to empty is how an item becomes air in transit, and
            // clearing first cannot leave two.
            ItemStack moving = other.clone();
            write.accept(null);
            menu.getInventory().setItem(hovered, moving);
        } else {
            // OUT. placeAllowed is deliberately NOT consulted: it asks what may come IN, and the
            // only rule going the other way is that the destination is empty -- which the
            // one-side-full test above has already established.
            ItemStack moving = resting.clone();
            menu.getInventory().setItem(hovered, null);
            write.accept(moving);
        }

        player.updateInventory();
        return new MenuClick(hovered, event.getClick(), event.getAction(), true);
    }

    /**
     * A number-key press over an input slot, carried out by us. Both directions.
     *
     * <p>Performed, never permitted. Vanilla's number key is a BIDIRECTIONAL swap -- it puts the
     * hotbar item into the hovered slot AND the hovered slot's item into the hotbar, in one press
     * -- so un-cancelling it would move two items in two inventories on rules that are not ours.
     * Cancelled throughout; the one-way move in {@link #swapWithInput} is the whole of what happens.
     *
     * <p><b>The hovered-slot check is the load-bearing one</b>, here and in {@link #offhandMove}.
     * It is what stops a filler pane leaking into the hotbar and a hotbar item vanishing into a
     * menu slot, which is the entire reason number keys were blanket-refused before. A slot in the
     * player's own inventory fails it for free: its raw slot is past the end of the menu, so it is
     * not one of the input slots.
     */
    private static MenuClick hotbarMove(InventoryClickEvent event, Menu menu) {
        if (!(event.getWhoClicked() instanceof Player player)) return null;

        int hovered = event.getRawSlot();
        if (!menu.inputSlots().contains(hovered)) return null;

        // -1 when the swap did not come from a hotbar button at all. Reading the inventory at a
        // negative index would throw from inside a click handler.
        int button = event.getHotbarButton();
        if (button < 0 || button > 8) return null;

        return swapWithInput(event, menu, player, hovered,
                () -> player.getInventory().getItem(button),
                item -> player.getInventory().setItem(button, item));
    }

    /**
     * F over an input slot, carried out by us. Both directions.
     *
     * <p>Structurally {@link #hotbarMove} with the offhand as the other slot, and it shares that
     * method's whole body through {@link #swapWithInput} rather than restating it. Vanilla's F is
     * the same bidirectional swap the number key is, and is refused for the same reason.
     *
     * <p>{@code getItemInOffHand} returns AIR rather than null when the hand is empty, which the
     * shared emptiness test already handles; {@code setItemInOffHand(null)} clears it.
     */
    private static MenuClick offhandMove(InventoryClickEvent event, Menu menu) {
        if (!(event.getWhoClicked() instanceof Player player)) return null;

        int hovered = event.getRawSlot();
        if (!menu.inputSlots().contains(hovered)) return null;

        return swapWithInput(event, menu, player, hovered,
                () -> player.getInventory().getItemInOffHand(),
                item -> player.getInventory().setItemInOffHand(item));
    }

    /**
     * A shift-click, carried out by us.
     *
     * <p><b>Supported by being PERFORMED, never by being permitted.</b> The objection that had it
     * refused outright still stands: vanilla picks the destination across the whole other
     * inventory, so un-cancelling would be accepting a slot chosen by someone other than us. Here
     * the destination is chosen here, and vetted through the SAME {@link #placeAllowed} the
     * click-place uses -- one copy of the validity rules, so the two gestures cannot drift into
     * disagreeing about what the input slot accepts.
     *
     * <p>Everything not explicitly moved below falls through to {@code null} and stays cancelled.
     * That is load-bearing rather than incidental: because the whitelist means a move we do not
     * perform simply does not happen, a filler pane cannot leak into a player's inventory even
     * though shift-clicking one is now a gesture that reaches this method.
     */
    private static MenuClick shiftMove(InventoryClickEvent event, Menu menu, boolean fromMenu) {
        if (!(event.getWhoClicked() instanceof Player player)) return null;

        ItemStack clickedItem = event.getCurrentItem();
        if (isEmpty(clickedItem)) return null;
        // Cloned because getCurrentItem() can hand back a view backed by the slot we are about to
        // clear. Moving a reference to a slot we then empty is how an item becomes air in transit.
        ItemStack moving = clickedItem.clone();

        if (fromMenu) {
            int slot = event.getRawSlot();
            // ONLY an input slot leaves the menu. A filler pane, a candidate icon and the close
            // button are display items the menu owns, and none of them is a thing to own.
            if (!menu.inputSlots().contains(slot)) {
                // ...but the menu may still want the GESTURE, as a button press. A crafting result
                // reads shift-click as "do it repeatedly" rather than "move this item".
                //
                // DISPATCH ONLY, and the early return here is the whole point: falling through to
                // the clear-and-give below would hand the player the DISPLAY item and then whatever
                // onClick produces on top of it -- one free item per shift-click, from a preview
                // nobody paid for. itemMoved is false because nothing moved.
                if (menu.shiftClickDispatches(slot)) {
                    return new MenuClick(slot, event.getClick(), event.getAction(), false);
                }
                return null;
            }

            menu.getInventory().setItem(slot, null);      // clear FIRST, so a failed give cannot
            MenuSafety.give(player, moving);              // leave a second copy behind
            player.updateInventory();
            return new MenuClick(slot, event.getClick(), event.getAction(), true);
        }

        // From the player's inventory. Sorted so a multi-input menu fills left to right rather than
        // in Set.of's unspecified order.
        //
        // TOP UP FIRST, THEN FILL AN EMPTY SLOT -- in that order, because that is what vanilla
        // does and the difference is invisible when it is wrong. Shift-clicking 64 cobblestone into
        // a grid that already holds cobblestone must land it ON that stack; a first-empty-slot
        // search puts it in the next cell along, throws no error, reddens nothing, and reads to a
        // player as the menu simply being weird.
        Integer target = topUpTarget(menu, moving);
        if (target == null) target = firstEmptyInput(menu);

        // Every input slot occupied and none of them able to take this. Refused silently: the
        // player can see the slots are full, and acceptsInput has not been asked, so nothing has
        // claimed the ITEM was the problem.
        if (target == null) return null;
        if (!placeAllowed(menu, target, moving)) return null;

        ItemStack resting = menu.getInventory().getItem(target);
        if (isEmpty(resting)) {
            event.setCurrentItem(null);                   // clear the source FIRST, same reason
            menu.getInventory().setItem(target, moving);
        } else {
            // A top-up. Only what fits moves; the remainder stays in the source slot, which is
            // vanilla's behaviour and is why this cannot simply overwrite.
            int moved = Math.min(resting.getMaxStackSize() - resting.getAmount(), moving.getAmount());
            if (moved <= 0) return null;

            ItemStack topped = resting.clone();
            topped.setAmount(resting.getAmount() + moved);

            ItemStack leftover = moving.clone();
            leftover.setAmount(moving.getAmount() - moved);

            event.setCurrentItem(leftover.getAmount() <= 0 ? null : leftover);
            menu.getInventory().setItem(target, topped);
        }

        player.updateInventory();
        return new MenuClick(target, event.getClick(), event.getAction(), true);
    }

    /**
     * The first STACKING input slot already holding a matching stack with room in it, or
     * {@code null}.
     *
     * <p>Only stacking slots, because an EXCLUSIVE slot holds one whole item and has no notion of
     * room: topping one up is exactly the merge its policy exists to refuse.
     */
    private static Integer topUpTarget(Menu menu, ItemStack moving) {
        for (int slot : new TreeSet<>(menu.inputSlots())) {
            boolean stacking = switch (menu.slotPolicy(slot)) {
                case STACKING -> true;
                case EXCLUSIVE -> false;
            };
            if (!stacking) continue;

            ItemStack resting = menu.getInventory().getItem(slot);
            if (isEmpty(resting)) continue;
            if (!resting.isSimilar(moving)) continue;
            if (resting.getAmount() >= resting.getMaxStackSize()) continue;
            return slot;
        }
        return null;
    }

    /** The first input slot holding nothing at all, or {@code null}. Policy-independent. */
    private static Integer firstEmptyInput(Menu menu) {
        for (int slot : new TreeSet<>(menu.inputSlots())) {
            if (isEmpty(menu.getInventory().getItem(slot))) return slot;
        }
        return null;
    }

    /**
     * May this item go into this input slot? Two questions, and they are deliberately different.
     *
     * <p><b>Occupancy, which the menu is not asked about, and which the POLICY decides.</b> For an
     * EXCLUSIVE slot the target must be EMPTY: vanilla MERGES a place onto a matching stack rather
     * than swapping, so without this a cursor of one item passes every validity check the menu
     * could make and the slot still ends up holding two. For a STACKING slot an occupied target is
     * fine when the resting item would stack with the incoming one and has room. Reading the slot
     * is reliable here: {@code InventoryClickEvent} fires BEFORE the place applies, so the slot
     * still holds its resting occupant and only the incoming item's landing is pending.
     *
     * <p>The DISSIMILAR case is absent on purpose. This answers "may it go IN", and a dissimilar
     * cursor onto an occupied stacking slot is a swap rather than a place -- decided by
     * {@link GridClickIntent} and performed by {@link #swapCursor}, on a path that asks
     * {@code acceptsInput} exactly as this does. A shift-click has no cursor to swap with, so
     * answering false here is the right answer for every caller that reaches it.
     *
     * <p><b>Validity, which is the menu's own.</b> Asked with the item still in the player's
     * hands -- on the cursor for a click-place, in its source slot for a shift-click -- which is
     * the only moment a refusal is free: the item never moves, so there is nothing to hand back
     * and no window in which the menu holds something it has already decided it does not want.
     * That is also why none of this needs a scheduler hop.
     *
     * <p>Takes the incoming stack as a parameter rather than reading the cursor itself, so the
     * click-place and the shift-click share ONE copy of the rules.
     */
    private static boolean placeAllowed(Menu menu, int slot, ItemStack incoming) {
        ItemStack resting = menu.getInventory().getItem(slot);
        boolean restingEmpty = isEmpty(resting);

        // Exhaustive switch EXPRESSION, no default arm: a third policy is a compile error rather
        // than a silent adoption of whichever occupancy rule happened to be written last.
        boolean occupancyOk = switch (menu.slotPolicy(slot)) {
            case EXCLUSIVE -> restingEmpty;
            case STACKING -> restingEmpty
                    || (resting.isSimilar(incoming) && resting.getAmount() < resting.getMaxStackSize());
        };
        if (!occupancyOk) return false;

        return menu.acceptsInput(incoming);
    }

    /**
     * Air, null and a zero stack are all "nothing here".
     *
     * <p>One copy because the three-part test was written out at six call sites and a fourth
     * condition added to only five of them is exactly the kind of drift that ends in an item
     * becoming air in transit.
     */
    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
