package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

/**
 * One click on a menu, after the framework has decided what it is allowed to do.
 *
 * <p><b>It deliberately carries no {@code InventoryClickEvent}.</b> That is the point of the type:
 * a consumer cannot reach {@code setCancelled}, so it cannot un-cancel what
 * {@link Menu#handleClick} cancelled, and "forgot to cancel" stops being a mistake a menu is able
 * to make. A consumer that needs the event is a consumer about to introduce a duplication bug.
 *
 * @param slot       the RAW slot clicked. Always inside the menu -- clicks in the player's own
 *                   inventory are handled by the router and never dispatched.
 * @param click      the button and modifier, for a consumer that wants to tell left from right.
 * @param action     what the server resolved the click to.
 * @param itemMoved  true only for the one whitelisted case: a permitted put-in or take-out of an
 *                   input slot. False for every button press, which is every other click.
 */
public record MenuClick(int slot, ClickType click, InventoryAction action, boolean itemMoved) {}
