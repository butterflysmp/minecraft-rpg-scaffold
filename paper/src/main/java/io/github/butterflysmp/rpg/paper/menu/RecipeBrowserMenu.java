package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.PageMath;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Every recipe on the server, paged, craftable straight out of the inventory.
 *
 * <p>The second half of Quick Craft. The suggestion column is three cells ranked by tier, so armor
 * and every vanilla recipe are unreachable from the crafting screen entirely -- gate row Q16, which
 * passes BY DESIGN. This is what it hands off to.
 *
 * <h2>NO INPUT SLOTS AT ALL</h2>
 *
 * {@link #inputSlots} is empty, so the router permits no moves into this menu and
 * {@code returnEverything} has nothing to return. Every slot is chrome. That makes the whole
 * class a read-and-click surface, which is what lets it be much shorter than {@code CraftingMenu}
 * despite doing a comparable job.
 *
 * <h2>THREE CLICK OUTCOMES AND A FOURTH NON-CLICK STATE</h2>
 *
 * <table>
 *   <tr><td>craftable now</td><td>crafts -- left for one, shift-left for a bulk run</td></tr>
 *   <tr><td>materials missing</td><td>refuses: "You do not have the materials for that."</td></tr>
 *   <tr><td>gone from the roster</td><td>refuses: "That recipe is no longer available."</td></tr>
 *   <tr><td><b>inert</b></td><td><b>not clickable.</b> Its lore says to use the grid</td></tr>
 * </table>
 *
 * <p>The last two rows are the reason the dead-entry decision matters. A missing-materials refusal
 * is TEMPORARY and actionable; a refusal for a recipe this menu structurally cannot make is
 * PERMANENT. Routing both into one message means the player gathers materials, returns, and meets
 * the identical refusal for ever.
 */
public final class RecipeBrowserMenu extends Menu {

    private final AdapterContext adapters;
    private final RecipeCatalogue catalogue;
    private final InventoryCraft inventoryCraft;

    /** Zero-based. Every read goes through {@code PageMath.clampPage} first. */
    private int page;

    public RecipeBrowserMenu(Player viewer, AdapterContext adapters, RecipeCatalogue catalogue) {
        super(viewer, RecipeBrowserLayout.SIZE, MenuIcons.line("Recipes", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.catalogue = catalogue;
        this.inventoryCraft = new InventoryCraft(viewer, adapters);
        render();
    }

    /**
     * Nothing. Every slot is chrome.
     *
     * <p>This is what makes the browser safe by construction rather than by guarding: with no input
     * slots the router performs no moves, the drag handler permits nothing, and there is no state a
     * close could strand.
     */
    @Override
    protected Set<Integer> inputSlots() {
        return Set.of();
    }

    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return false;
    }

    /**
     * Shift-click on an entry means "craft as many as you can", exactly as it does in the column.
     *
     * <p>Dispatch only: the router performs no move, it merely lets the gesture be heard.
     */
    @Override
    protected boolean shiftClickDispatches(int slot) {
        return RecipeBrowserLayout.entryIndexOf(slot).isPresent();
    }

    @Override
    protected void onClick(MenuClick click) {
        int slot = click.slot();

        if (slot == RecipeBrowserLayout.BACK_SLOT) {
            adapters.scheduler().onEntity(viewer, () -> new CraftingMenu(viewer, adapters, catalogue).open());
            return;
        }
        if (slot == RecipeBrowserLayout.PREV_SLOT) {
            turnTo(page - 1);
            return;
        }
        if (slot == RecipeBrowserLayout.NEXT_SLOT) {
            turnTo(page + 1);
            return;
        }

        OptionalInt index = RecipeBrowserLayout.entryIndexOf(slot);
        if (index.isEmpty()) return;                    // the page readout and the filler are inert

        List<RecipeCatalogue.Entry> entries = catalogue.entries();
        int at = PageMath.startIndex(page, RecipeBrowserLayout.ENTRIES_PER_PAGE) + index.getAsInt();
        if (at >= entries.size()) return;               // an empty cell on a short final page

        RecipeCatalogue.Entry entry = entries.get(at);

        // INERT: not clickable, and it says so in its lore rather than in a message. A message here
        // would be indistinguishable from the missing-materials one at exactly the moment the player
        // needs to tell them apart.
        if (entry.inert()) return;

        // THE STALE-CACHE PATH, and the whole reason a cache built once per server lifetime is
        // survivable: the entry is re-resolved against the LIVE roster before anything is committed.
        Recipe recipe = catalogue.resolve(entry);
        if (recipe == null) {
            say("That recipe is no longer available.");
            render();
            return;
        }

        InventoryCraft.Outcome outcome = inventoryCraft.craft(recipe, click.click().isShiftClick());
        if (outcome.inventoryFull()) {
            say("Your inventory is full -- made " + outcome.crafted() + ".");
        } else if (outcome.crafted() == 0) {
            // Deliberately NOT "you no longer have" -- the column says that because it just showed a
            // button claiming the player could. The browser never promised affordability.
            say("You do not have the materials for that.");
        }

        // ONCE, after the run. Never inside it.
        render();
        viewer.updateInventory();
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    private void turnTo(int requested) {
        int clamped = PageMath.clampPage(requested, catalogue.entries().size(),
                RecipeBrowserLayout.ENTRIES_PER_PAGE);
        if (clamped == page) return;                    // already there; do not repaint for nothing
        page = clamped;
        render();
    }

    private void render() {
        List<RecipeCatalogue.Entry> entries = catalogue.entries();
        int size = RecipeBrowserLayout.ENTRIES_PER_PAGE;

        // CLAMP FIRST. PageMath's start/end pair is only sliceable for a clamped page -- that is its
        // stated contract, and this is the call site it exists for. The catalogue can also have
        // shrunk under a player who left the menu open.
        page = PageMath.clampPage(page, entries.size(), size);

        int from = PageMath.startIndex(page, size);
        int to = PageMath.endIndex(page, size, entries.size());

        for (int cell = 0; cell < size; cell++) {
            int at = from + cell;
            getInventory().setItem(RecipeBrowserLayout.ENTRY_SLOTS.get(cell),
                    at < to ? iconFor(entries.get(at)) : null);
        }

        for (int slot : RecipeBrowserLayout.FOOTER_FILLER) {
            getInventory().setItem(slot, MenuIcons.filler());
        }

        int pages = PageMath.pageCount(entries.size(), size);

        // HIDDEN, not disabled, at the ends. A greyed-out button a player can still click and get
        // nothing from is the same "did I break it" experience as one that does nothing silently.
        getInventory().setItem(RecipeBrowserLayout.PREV_SLOT, page > 0
                ? MenuIcons.icon(Material.ARROW, MenuIcons.line("Previous page", NamedTextColor.GRAY),
                        List.of(MenuIcons.line("Page " + PageMath.displayPage(page - 1),
                                NamedTextColor.DARK_GRAY)))
                : MenuIcons.filler());

        getInventory().setItem(RecipeBrowserLayout.NEXT_SLOT, page < pages - 1
                ? MenuIcons.icon(Material.ARROW, MenuIcons.line("Next page", NamedTextColor.GRAY),
                        List.of(MenuIcons.line("Page " + PageMath.displayPage(page + 1),
                                NamedTextColor.DARK_GRAY)))
                : MenuIcons.filler());

        getInventory().setItem(RecipeBrowserLayout.PAGE_SLOT, MenuIcons.icon(
                Material.PAPER,
                MenuIcons.line("Page " + PageMath.displayPage(page) + " of " + pages,
                        NamedTextColor.WHITE),
                List.of(MenuIcons.line(entries.size() + " recipes", NamedTextColor.DARK_GRAY))));

        getInventory().setItem(RecipeBrowserLayout.BACK_SLOT, MenuIcons.icon(
                Material.CRAFTING_TABLE,
                MenuIcons.line("Back to crafting", NamedTextColor.GRAY),
                List.of()));
    }

    /**
     * The icon for one entry: the item it makes, minted where content claims it, plus the
     * ingredient lines as chrome.
     *
     * <p><b>Resolved from the LIVE roster, not from the cache</b>, for the same reason the click is:
     * a recipe that has left the roster should look gone rather than craftable.
     *
     * <p>Minted, and NOT rolled -- the identical reasoning {@code CraftingMenu.suggestionIcon}
     * records. Rarity and stats are deterministic from the definition, so showing them is a promise
     * the craft keeps; enchant candidates are a random draw per item, so showing them is a promise
     * it breaks.
     */
    private ItemStack iconFor(RecipeCatalogue.Entry entry) {
        Recipe recipe = catalogue.resolve(entry);
        ItemStack result = recipe == null ? null : recipe.getResult();
        if (MenuSafety.isEmpty(result)) {
            return MenuIcons.pane(MenuIcons.EMPTY_SUGGESTION);
        }

        Optional<GearDefinition> claimed = inventoryCraft.claimFor(result);
        ItemStack icon = claimed.isPresent()
                ? GearItems.mint(claimed.get(), adapters)
                : result.clone();

        List<Component> chrome = new ArrayList<>();
        if (entry.inert()) {
            // ALL THREE TRUE THINGS: the recipe exists, this menu cannot make it, and the grid can.
            // An inert entry that does not say what to do instead is only two-thirds honest.
            chrome.add(MenuIcons.line("Cannot be crafted here", NamedTextColor.RED));
            chrome.add(MenuIcons.line("Use the crafting grid for this one", NamedTextColor.DARK_GRAY));
        } else {
            chrome.addAll(IngredientLore.of(recipe));
            chrome.add(MenuIcons.line("Click to craft from your inventory", NamedTextColor.DARK_GRAY));
        }

        // Chrome on top, the item's own lore underneath, so the RARITY FOOTER stays last exactly as
        // it is on the real item. MenuIconsTest pins that ordering.
        icon.editMeta(meta -> meta.lore(MenuIcons.chromeOver(chrome, meta.lore())));
        return icon;
    }

    private void say(String message) {
        viewer.sendMessage(MenuIcons.line(message, NamedTextColor.GRAY));
    }
}
