package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.CraftCount;
import io.github.butterflysmp.rpg.core.weapon.CraftOrder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Everything the player can craft RIGHT NOW, paged, straight out of their inventory.
 *
 * <h2>THE CONTRACT IS "WHAT YOU CAN CRAFT HERE, RIGHT NOW"</h2>
 *
 * <b>This reverses the premise the slice began under, deliberately.</b> The browser was built to
 * page through the whole 1214-recipe roster, on the argument that the three-cell suggestion column
 * squeezes out armor and vanilla (gate row Q16) and something had to make them reachable. The brief
 * changed: this is <i>an easy way to craft quickly</i>, not a recipe encyclopedia, and a
 * thousand-entry list is clutter against that purpose.
 *
 * <p>So the static {@link RecipeCatalogue} is FILTERED per player at open. What survives:
 *
 * <ul>
 *   <li>the catalogue itself — built lazily on first open, cached for the server lifetime, still the
 *       right shared structure because roster, key, tier, body slot and ingredients do not depend on
 *       any player;
 *   <li>the tier ordering, and the head-chest-legs-feet order within armor.
 * </ul>
 *
 * <h2>WHAT IT RESOLVES</h2>
 *
 * The inert-entry apparatus is gone. It existed because a browser claiming to show EVERYTHING could
 * not omit a grid-craftable recipe without that being a FALSE absence — Q10's mistake in UI form.
 * Under this contract a multi-star firework is absent <b>honestly</b>: it cannot be crafted here.
 *
 * <h2>WHAT IT COSTS</h2>
 *
 * <b>Armor the player cannot yet afford is invisible everywhere</b> — squeezed out of the column by
 * Q16, hidden here by the filter. No surface answers "what does a netherite helmet need?". A
 * consequence of the product decision, not a defect; see {@link RecipeCatalogue}.
 *
 * <h2>COST: THIS SCORES THE WHOLE ROSTER PER OPEN, NOT PER PAGE</h2>
 *
 * <b>The number that covers this walk is gate row Q2's 298µs</b> — the suggestion probe, which does
 * exactly this work: group the inventory, probe every recipe, rank. <b>NOT Q24</b>, which measures
 * the catalogue BUILD and is paid once per server. Two different walks, two different numbers, and
 * crossing them is precisely the confusion Q24's note was written to prevent.
 *
 * <h2>TWO FAILURE MODES THE FILTER CREATES</h2>
 *
 * <ol>
 *   <li><b>The list SHRINKS under the player.</b> Craft the last entry on page 3 and page 3 may
 *       cease to exist. So the page is re-clamped after EVERY recompute, not only on navigation —
 *       see {@link #recompute}.
 *   <li><b>An empty inventory means an empty browser</b>, which reads as broken. There is an
 *       explicit empty state; {@code MenuIcons.placeholder}'s argument applies exactly — a surface
 *       showing nothing because it measured nothing must be distinguishable from one that is broken.
 * </ol>
 */
public final class RecipeBrowserMenu extends Menu {

    private final AdapterContext adapters;
    private final RecipeCatalogue catalogue;
    private final InventoryCraft inventoryCraft;

    /** Zero-based. Every read goes through {@link PageMath#clampPage} first. */
    private int page;

    /** What the player can craft, as of the last {@link #recompute}. Ordered, never null. */
    private List<CraftCount.Craftable> visible = List.of();

    /**
     * Recipe key to its catalogue entry, so a click re-resolves without a second roster walk.
     *
     * <p>Deliberately holds the ENTRY and not the {@code Recipe}: the entry is the cache-safe
     * identity, and {@link RecipeCatalogue#resolve} against it is what re-checks the live roster.
     * Caching the {@code Recipe} object here and clicking that would be exactly the stale-cache trust
     * the catalogue's design exists to avoid.
     */
    private Map<String, RecipeCatalogue.Entry> shown = Map.of();

    public RecipeBrowserMenu(Player viewer, AdapterContext adapters, RecipeCatalogue catalogue) {
        super(viewer, RecipeBrowserLayout.SIZE, MenuIcons.line("Recipes", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.catalogue = catalogue;
        this.inventoryCraft = new InventoryCraft(viewer, adapters);
        recompute();
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
            adapters.scheduler().onEntity(viewer,
                    () -> new CraftingMenu(viewer, adapters, catalogue).open());
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

        int at = PageMath.startIndex(page, RecipeBrowserLayout.ENTRIES_PER_PAGE) + index.getAsInt();
        if (at >= visible.size()) return;               // an empty cell on a short final page

        CraftCount.Craftable entry = visible.get(at);

        // THE STALENESS PATH. Under the craftable-now contract a listed entry was affordable when
        // the list was built -- but the list can be seconds old, and the materials can be gone: a
        // hopper, another plugin, or the player crafting the same materials away in this very menu.
        // So the recipe is re-resolved against the LIVE roster and the craft re-verifies against the
        // LIVE inventory. Nothing here trusts that the list is still true.
        RecipeCatalogue.Entry catalogued = shown.get(entry.key());
        Recipe recipe = catalogued == null ? null : catalogue.resolve(catalogued);
        if (recipe == null) {
            say("That recipe is no longer available.");
            recompute();
            render();
            return;
        }

        InventoryCraft.Outcome outcome = inventoryCraft.craft(recipe, click.click().isShiftClick());
        if (outcome.inventoryFull()) {
            say("Your inventory is full -- made " + outcome.crafted() + ".");
        } else if (outcome.crafted() == 0) {
            // Reachable ONLY when the list is stale -- see above. On a fresh view every listed entry
            // is affordable by construction, which is why the gate row for this is a STALENESS row
            // and not a "click something you cannot afford" row: that state cannot be staged.
            say("You do not have the materials for that.");
        }

        // ONCE, after the run. Never inside it. recompute() re-clamps the page, which is what stops
        // a bulk craft that empties the last page from leaving the player on a page that no longer
        // exists.
        recompute();
        render();
        viewer.updateInventory();
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    private void turnTo(int requested) {
        int clamped = PageMath.clampPage(requested, visible.size(),
                RecipeBrowserLayout.ENTRIES_PER_PAGE);
        if (clamped == page) return;                    // already there; do not repaint for nothing
        page = clamped;
        render();
    }

    /**
     * Score the whole catalogue against the player's current inventory, and re-clamp the page.
     *
     * <h2>THE RE-CLAMP IS THE POINT, and it belongs HERE rather than in {@link #render}</h2>
     *
     * Crafting shrinks this list. Bulk-craft everything on the last page and that page ceases to
     * exist, leaving {@code page} pointing past the end — a blank grid that reads as a broken menu,
     * or an index past the end of the list. {@link PageMath#clampPage} exists for this; the defect
     * would have been calling it only on navigation, because navigation is the obvious moment and
     * <b>crafting is the one that actually changes the page count.</b>
     *
     * <p>Clamping here rather than in {@code render} means every path that changes the list — open,
     * craft, stale-recipe refusal — is covered by construction, and {@code render} can assume its
     * page is valid.
     *
     * <h2>Cost</h2>
     *
     * One inventory grouping plus one probe per catalogue entry. This is the walk gate row <b>Q2</b>
     * measured at 298µs against a 50000µs tick — NOT Q24, which times the catalogue build.
     */
    private void recompute() {
        List<RecipeProbe.Group> groups = RecipeProbe.groupsOf(viewer.getInventory(), adapters.keys());
        List<CraftCount.Stock> stock = RecipeProbe.stockOf(groups);

        List<CraftCount.Candidate> candidates = new ArrayList<>();
        Map<String, RecipeCatalogue.Entry> live = new HashMap<>();

        for (RecipeCatalogue.Entry entry : catalogue.entries()) {
            Recipe recipe = catalogue.resolve(entry);
            if (recipe == null) continue;               // left the roster since the catalogue built

            // The tier and body slot come from the CATALOGUE, already computed once at build. A
            // recipe that exposes no ingredients returns null here and is simply absent -- honestly,
            // under this contract.
            CraftCount.Candidate candidate =
                    RecipeProbe.probeOne(recipe, groups, entry.tier(), entry.armorSlot());
            if (candidate == null) continue;

            candidates.add(candidate);
            live.put(candidate.key(), entry);
        }

        // rank() applies the count invariant -- it never over-states, and it drops anything the
        // player cannot actually afford. THAT is the filter; there is no separate one to keep in
        // step with it.
        List<CraftCount.Craftable> ranked = new ArrayList<>(CraftCount.rank(candidates, stock));

        // ...then re-sorted into BROWSER order. rank() leads with count because the three-cell column
        // should spend its cells on what the player can make most of; a browser that did the same
        // would reshuffle the whole list every time the player crafted one item. Tier and the
        // within-tier tiebreak are shared -- see CraftOrder.
        ranked.sort(CraftOrder.TIER_FIRST);

        this.visible = List.copyOf(ranked);
        this.shown = Map.copyOf(live);
        this.page = PageMath.clampPage(page, visible.size(), RecipeBrowserLayout.ENTRIES_PER_PAGE);
    }

    private void render() {
        int size = RecipeBrowserLayout.ENTRIES_PER_PAGE;
        int from = PageMath.startIndex(page, size);
        int to = PageMath.endIndex(page, size, visible.size());

        for (int cell = 0; cell < size; cell++) {
            int at = from + cell;
            getInventory().setItem(RecipeBrowserLayout.ENTRY_SLOTS.get(cell),
                    at < to ? iconFor(visible.get(at)) : null);
        }

        // THE EMPTY STATE. An empty inventory means an empty browser, and a grid of nothing is
        // indistinguishable from a menu that failed to load, so it says which.
        //
        // NAME ONLY, NO LORE, AND NOT MenuIcons.placeholder. It WAS placeholder, and that rendered:
        //
        //     Nothing you can make right now
        //     Not implemented yet.
        //     materials for any recipe
        //
        // -- announcing the feature was MISSING while it was working correctly and had measured
        // zero. That is placeholder's own javadoc warning inverted: it exists because "0%" cannot be
        // told apart from a working readout that measured zero, and here the working readout was
        // built out of the placeholder. Reaching for placeholder is a claim that something is NOT
        // BUILT.
        //
        // STRUCTURE_VOID, not BARRIER, and verified on the pinned jar the same way KNOWLEDGE_BOOK
        // was. When the browser is empty the close button is on screen too -- two BARRIERs, same
        // material, distinguished only by name and position, so telling them apart needs a hover.
        // Clicking the wrong one is harmless (Menu cancels first), but a distinction that survives
        // without hovering is worth one constant.
        if (visible.isEmpty()) {
            getInventory().setItem(RecipeBrowserLayout.EMPTY_STATE_SLOT, MenuIcons.icon(
                    Material.STRUCTURE_VOID,
                    MenuIcons.line("Nothing you can make right now", NamedTextColor.GRAY),
                    List.of()));
        }

        for (int slot : RecipeBrowserLayout.FOOTER_FILLER) {
            getInventory().setItem(slot, MenuIcons.filler());
        }

        int pages = PageMath.pageCount(visible.size(), size);

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
                List.of(MenuIcons.line(visible.size() + " you can craft now",
                        NamedTextColor.DARK_GRAY))));

        getInventory().setItem(RecipeBrowserLayout.BACK_SLOT, MenuIcons.icon(
                Material.CRAFTING_TABLE,
                MenuIcons.line("Back to crafting", NamedTextColor.GRAY),
                List.of()));
    }

    /**
     * The icon for one entry: the item it makes, minted where content claims it, plus how many and
     * what it needs.
     *
     * <p>Minted, and NOT rolled -- the identical reasoning {@code CraftingMenu.suggestionIcon}
     * records. Rarity and stats are deterministic from the definition, so showing them is a promise
     * the craft keeps; enchant candidates are a random draw per item, so showing them is a promise
     * it breaks.
     */
    private ItemStack iconFor(CraftCount.Craftable entry) {
        RecipeCatalogue.Entry catalogued = shown.get(entry.key());
        Recipe recipe = catalogued == null ? null : catalogue.resolve(catalogued);
        ItemStack result = recipe == null ? null : recipe.getResult();
        if (MenuSafety.isEmpty(result)) {
            return MenuIcons.pane(MenuIcons.EMPTY_SUGGESTION);
        }

        Optional<GearDefinition> claimed = inventoryCraft.claimFor(result);
        ItemStack icon = claimed.isPresent()
                ? GearItems.mint(claimed.get(), adapters)
                : result.clone();

        List<Component> chrome = new ArrayList<>();
        chrome.add(MenuIcons.line("Craft " + entry.count() + " more", NamedTextColor.GRAY));
        chrome.addAll(IngredientLore.of(recipe));
        chrome.add(MenuIcons.line("Uses items from your inventory", NamedTextColor.DARK_GRAY));

        // Chrome on top, the item's own lore underneath, so the RARITY FOOTER stays last exactly as
        // it is on the real item. MenuIconsTest pins that ordering.
        icon.editMeta(meta -> meta.lore(MenuIcons.chromeOver(chrome, meta.lore())));
        return icon;
    }

    private void say(String message) {
        viewer.sendMessage(MenuIcons.line(message, NamedTextColor.GRAY));
    }
}
