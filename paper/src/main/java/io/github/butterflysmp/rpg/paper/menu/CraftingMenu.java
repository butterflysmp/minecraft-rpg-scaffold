package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemCraftResult;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

import static io.github.butterflysmp.rpg.paper.menu.CraftingMenuLayout.CLOSE_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.CraftingMenuLayout.GRID_SLOTS;
import static io.github.butterflysmp.rpg.paper.menu.CraftingMenuLayout.MATRIX_LENGTH;
import static io.github.butterflysmp.rpg.paper.menu.CraftingMenuLayout.RESULT_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.CraftingMenuLayout.SIZE;

/**
 * The crafting table, replaced.
 *
 * <p>Slice 1 ships ZERO custom recipes on purpose: this is the SURFACE, and it has to be provably
 * safe before anything valuable rides on it. What it does is take items with vanilla ergonomics,
 * resolve vanilla recipes through the SERVER'S OWN matcher, and refuse absolutely to let a minted
 * item be consumed by one.
 *
 * <p><b>The matcher is the server's, and that is the most important line in this class.</b> The
 * previous project hand-rolled shaped/shapeless matching by walking {@code recipeIterator()}, which
 * skips {@code ComplexRecipe} -- so it then had to hand-implement firework rockets, firework stars
 * and dye tables purely to restore parity with the vanilla UI it had displaced. Delegating deletes
 * all of that.
 *
 * <p><b>Two different overloads, deliberately.</b> Verified from the Paper sources jar, not assumed
 * from the names:
 *
 * <ul>
 *   <li>PREVIEW uses {@code craftItemResult(matrix, world)}, which fires NOTHING. It runs on every
 *       grid change, several times a second.
 *   <li>COMMIT uses {@code craftItemResult(matrix, world, player)}, whose javadoc says it "Calls
 *       PrepareItemCraftEvent to imitate the Player initiating the crafting event". That is right
 *       for a real craft -- other plugins should see it -- and would be badly wrong for a preview,
 *       which would re-enter our own guard and every third-party listener from inside a click
 *       handler.
 * </ul>
 */
public final class CraftingMenu extends Menu {

    /**
     * How many crafts one shift-click may perform.
     *
     * <p>A bound rather than "until the grid runs dry" alone. The loop's exit already depends on the
     * matrix shrinking each pass, and a recipe that somehow did not consume anything would spin
     * forever inside a click handler, taking the server's main thread with it. This is the guard
     * that does not depend on the recipe behaving.
     */
    private static final int MAX_BULK_CRAFTS = 64;

    private final AdapterContext adapters;

    public CraftingMenu(Player viewer, AdapterContext adapters) {
        super(viewer, SIZE, MenuIcons.line("Crafting", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        render();
        refreshPreview();
    }

    /**
     * The nine grid cells, and ONLY those.
     *
     * <p><b>The result slot is deliberately absent.</b> {@link Menu#returnEverything} iterates this
     * set and hands the player everything in it on close, death, disconnect and shutdown. A preview
     * listed here would be a free item every single time the menu closed. The result is not the
     * player's until they take it, and taking it is performed by {@link #takeResult}.
     */
    @Override
    protected Set<Integer> inputSlots() {
        return GRID_SLOTS;
    }

    /** Every grid cell stacks. Nothing else is an input slot, so nothing else is asked. */
    @Override
    protected SlotPolicy slotPolicy(int slot) {
        return SlotPolicy.STACKING;
    }

    /**
     * Shift-clicking the RESULT means "craft repeatedly", not "move this item".
     *
     * <p>Dispatch only -- the router performs no move for it, which is what keeps the preview from
     * being handed over alongside what the craft produces.
     */
    @Override
    protected boolean shiftClickDispatches(int slot) {
        return slot == RESULT_SLOT;
    }

    /**
     * The grid takes ANYTHING, minted gear included, and that is not an oversight.
     *
     * <p>A player must be able to put a minted weapon into the grid -- otherwise the gear-tag screen
     * has nothing to screen and the safety property is untestable in game. What protects the item is
     * {@link CraftMatrixScreen}: a matrix holding it is never shown to the server's matcher, so it
     * cannot be consumed. Refusing it at the door would look safer and would actually hide the
     * mechanism that does the work.
     */
    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return true;
    }

    @Override
    protected void onClick(MenuClick click) {
        if (click.slot() == CLOSE_SLOT) {
            // Closes, and nothing else. The grid comes back through onClose -- the SAME path Esc
            // takes -- so the button and the escape key cannot drift apart.
            viewer.closeInventory();
            return;
        }

        if (click.slot() == RESULT_SLOT) {
            takeResult(click);
            return;
        }

        if (click.itemMoved()) {
            // The item has NOT landed yet for a PERMITTED move: InventoryClickEvent fires before
            // the place applies. Recompute next tick, when the grid holds what the player thinks it
            // holds. onEntityLater is the sanctioned route and clamps a zero delay to one tick.
            adapters.scheduler().onEntityLater(viewer, this::refreshPreview, 1);
        }
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        returnEverything();
    }

    // ------------------------------------------------------------------ taking

    /**
     * Take the result: once onto the cursor, or repeatedly into the inventory for a shift-click.
     *
     * <p>Performed, never permitted, and this is the strongest case for that rule in the whole menu:
     * the item does not exist until we mint it. There is nothing for the server to apply afterwards,
     * so setting a new preview inside this handler cannot hand out anything free.
     */
    private void takeResult(MenuClick click) {
        if (isEmpty(getInventory().getItem(RESULT_SLOT))) return;

        if (click.click().isShiftClick()) {
            craftRepeatedly();
        } else {
            craftOnceToCursor();
        }
        refreshPreview();
        viewer.updateInventory();
    }

    /**
     * One craft, onto the cursor.
     *
     * <p><b>The cursor is checked BEFORE anything is debited.</b> Crafting first and then
     * discovering the cursor is full would leave the ingredients spent with nowhere for the output
     * to go -- the item would have to be dropped or destroyed, and either is a loss the player did
     * not ask for. A refusal that happens before the debit costs nothing.
     */
    private void craftOnceToCursor() {
        ItemStack preview = getInventory().getItem(RESULT_SLOT);
        ItemStack cursor = viewer.getItemOnCursor();
        boolean cursorEmpty = isEmpty(cursor);

        if (!cursorEmpty) {
            if (!cursor.isSimilar(preview)) {
                say("Your cursor is holding something else.");
                return;
            }
            if (cursor.getAmount() + preview.getAmount() > cursor.getMaxStackSize()) {
                say("No room on your cursor for that.");
                return;
            }
        }

        ItemCraftResult result = commitCraft();
        if (result == null) return;

        ItemStack crafted = result.getResult();
        if (cursorEmpty) {
            viewer.setItemOnCursor(crafted);
        } else {
            ItemStack merged = cursor.clone();
            merged.setAmount(cursor.getAmount() + crafted.getAmount());
            viewer.setItemOnCursor(merged);
        }
    }

    /**
     * Craft until the grid runs dry, straight into the inventory.
     *
     * <p>Ingredients come from the GRID only. Pulling more from the player's inventory is Quick
     * Craft's job and needs a craftability model this slice does not have.
     *
     * <p>Each pass is atomic: {@link #commitCraft} either debits the matrix and yields a result, or
     * changes nothing and yields null. So an interrupted run leaves the grid consistent with the
     * items handed over, whatever pass it stopped on.
     */
    private void craftRepeatedly() {
        for (int crafted = 0; crafted < MAX_BULK_CRAFTS; crafted++) {
            ItemCraftResult result = commitCraft();
            if (result == null) return;
            MenuSafety.give(viewer, result.getResult());
        }
    }

    /**
     * Perform ONE craft against the grid, or nothing at all.
     *
     * <p><b>The grid afterwards is the server's resulting matrix, never the grid minus one per
     * slot.</b> Decrementing destroys container remainders: a cake leaves three EMPTY BUCKETS
     * behind, honey bottles leave glass bottles, and a hand-rolled debit silently deletes them.
     * {@code getResultingMatrix()} already holds them.
     *
     * <p>{@code getOverflowItems()} is the other half of that: remainders that could not fit back
     * into the matrix. Dropping them on the floor of this method would be a second silent loss, so
     * they go to the player through the one give path.
     *
     * <p>Everything written back is CLONED. {@code ItemCraftResult}'s own javadoc says it "makes no
     * guarantees about the nature or mutability of the returned values".
     *
     * @return the result, or {@code null} when nothing was crafted and nothing was changed.
     */
    private ItemCraftResult commitCraft() {
        ItemStack[] matrix = readMatrix();

        // The screen, BEFORE the server is ever asked. Exhaustive switch expression, no default.
        boolean eligible = switch (CraftMatrixScreen.verdict(matrix, adapters.keys())) {
            case VANILLA_ELIGIBLE -> true;
            case CONTAINS_GEAR -> false;
        };
        if (!eligible) return null;

        // The PLAYER overload: this is a real craft, and its javadoc says it calls
        // PrepareItemCraftEvent so other plugins observe it. Re-entering our own guard is harmless
        // -- the matrix was screened one line above and holds nothing of ours.
        ItemCraftResult result = Bukkit.craftItemResult(matrix, viewer.getWorld(), viewer);
        if (isEmpty(result.getResult())) return null;

        writeMatrix(result.getResultingMatrix());
        for (ItemStack overflow : result.getOverflowItems()) {
            MenuSafety.give(viewer, overflow);
        }
        return result;
    }

    // ----------------------------------------------------------------- preview

    /**
     * Recompute what the grid would make.
     *
     * <p>Uses the overload that fires NO events. The player-taking one calls
     * {@code PrepareItemCraftEvent} by contract, and running that on every grid change would
     * re-enter our own guard and every third-party listener several times a second from inside a
     * click handler.
     *
     * <p>The preview is a CLONE of the real result, so what a player sees is exactly what they get.
     */
    private void refreshPreview() {
        ItemStack[] matrix = readMatrix();

        ItemStack preview = switch (CraftMatrixScreen.verdict(matrix, adapters.keys())) {
            // A matrix holding any of our gear is invisible to the server's matcher. In slice 1
            // that is simply "no recipe"; slice 2 asks our own gear-id table here instead.
            case CONTAINS_GEAR -> null;
            case VANILLA_ELIGIBLE -> {
                ItemCraftResult result = Bukkit.craftItemResult(matrix, viewer.getWorld());
                yield isEmpty(result.getResult()) ? null : result.getResult().clone();
            }
        };

        getInventory().setItem(RESULT_SLOT, preview);
    }

    // ------------------------------------------------------------------ matrix

    /**
     * The grid as the server wants it: nine entries, row-major.
     *
     * <p>Live references rather than clones, because {@code getCraftingRecipe} and the preview
     * overload are documented not to modify the array they are given, and the commit overload's
     * output is written back wholesale rather than in place.
     */
    private ItemStack[] readMatrix() {
        ItemStack[] matrix = new ItemStack[MATRIX_LENGTH];
        for (int index = 0; index < MATRIX_LENGTH; index++) {
            matrix[index] = getInventory().getItem(CraftingMenuLayout.rawSlotForMatrix(index));
        }
        return matrix;
    }

    /** Write a resulting matrix back over the grid, cloning as it goes. */
    private void writeMatrix(ItemStack[] matrix) {
        for (int index = 0; index < MATRIX_LENGTH; index++) {
            ItemStack item = matrix != null && index < matrix.length ? matrix[index] : null;
            getInventory().setItem(CraftingMenuLayout.rawSlotForMatrix(index),
                    isEmpty(item) ? null : item.clone());
        }
    }

    // ------------------------------------------------------------------ chrome

    /**
     * Paint the chrome.
     *
     * <p><b>Never writes a grid slot or the result slot.</b> The grid holds the player's items and
     * the result is written by {@link #refreshPreview} alone; a repaint that touched either would
     * blank or duplicate something a player is looking at.
     */
    private void render() {
        for (int slot = 0; slot < SIZE; slot++) {
            if (GRID_SLOTS.contains(slot) || slot == RESULT_SLOT) continue;
            getInventory().setItem(slot, MenuIcons.filler());
        }
        getInventory().setItem(CLOSE_SLOT, MenuIcons.close());
    }

    private void say(String message) {
        viewer.sendMessage(Component.text(message, NamedTextColor.GRAY));
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
