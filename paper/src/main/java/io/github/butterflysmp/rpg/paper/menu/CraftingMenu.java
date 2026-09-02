package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponDurability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemCraftResult;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
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
     *
     * <p><b>It is ALSO an ordinary per-gesture batch size, and reaching it is not a defect signal.</b>
     * 64 planks in each plank slot is 64 shields, so a stack-loaded grid hits this bound in normal
     * play and stops with material still loaded -- a second shift-click simply continues. That is
     * indistinguishable, to a player and to a reader, from stopping because nothing more could be
     * made. Said out loud because the runaway-guard framing alone invites someone to treat arrival
     * here as a bug, or to "fix" it by removing the bound.
     */
    private static final int MAX_BULK_CRAFTS = 64;

    /**
     * The recipe the RESULT SLOT is currently showing, or empty when it shows nothing.
     *
     * <p>Written by {@link #refreshPreview} and read by the two commit paths, so that what a player
     * receives is what they were looking at. {@code Recipe} itself carries no key -- verified
     * against the pinned API, where the interface declares only {@code getResult()} -- so the
     * identity is narrowed through {@code instanceof Keyed}, which covers shaped, shapeless AND
     * complex recipes. {@code NamespacedKey} implements {@code equals}, so comparing them is sound.
     */
    private Optional<NamespacedKey> previewedRecipe = Optional.empty();

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
            // A DOUBLE-CLICK ON THE RESULT IS REFUSED BY NAME, and this arm must not become a
            // fall-through from "it is not a STACKING slot" -- a fall-through is silent about why,
            // and the next person to touch the collect reads it as an oversight and helpfully
            // "fixes" it.
            //
            // A double-click fires TWO events: LEFT, then DOUBLE_CLICK. Slice 1 declined to port the
            // old project's MenuThrottle -- which guarded exactly that pair on its output slot --
            // because MenuRouting refused DOUBLE_CLICK by TYPE before dispatch. Slice 3 WITHDREW
            // that guarantee by performing the gesture instead. So treating this as a take would
            // give one gesture two crafts: the LEFT takes one, the DOUBLE_CLICK takes another,
            // ingredients paid twice -- and with the pin in place the second one SUCCEEDS. Not a
            // duplication, both crafts pay, but a craft the player did not ask for and will not
            // notice.
            //
            // The LEFT half has already crafted by the time this fires, so the observable is ONE
            // craft, never "nothing happened".
            if (click.click() == ClickType.DOUBLE_CLICK) return;

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

    /**
     * A drag just changed the grid, so the preview must catch up.
     *
     * <p>The SAME one-tick hop {@link #onClick} uses, for the SAME reason: a permitted drag has not
     * landed when the event returns, exactly as a permitted place has not. Without this the grid
     * changes behind a stale preview and the result slot advertises the previous recipe.
     *
     * <p>With the commit pinned to the preview, a stale preview no longer produces the wrong item --
     * it makes the craft REFUSE. Safer, and it reads as a broken table rather than as theft, which
     * is why the pin and this hook shipped together.
     */
    @Override
    protected void onDragPermitted() {
        adapters.scheduler().onEntityLater(viewer, this::refreshPreview, 1);
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
            // Read ONCE, here, before anything can move it. The field is live -- every commit
            // recomputes the preview -- so this read is the pin, and craftRepeatedly is handed a
            // value rather than a place to look.
            craftRepeatedly(previewedRecipe);
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

        ItemStack crafted = commitCraft(previewedRecipe);
        if (crafted == null) return;

        if (cursorEmpty) {
            viewer.setItemOnCursor(crafted);
            return;
        }

        // RE-CHECK THE CRAFTED ITEM, not the preview it was authorised against.
        //
        // The checks above ran against the PREVIEW, computed by the event-free overload. The commit
        // overload fires PrepareItemCraftEvent, and a listener may CHANGE a result rather than null
        // it -- the empty-result abort in commitCraft does not see that, because a substituted
        // result is not empty. Merging on the strength of the earlier check would then build the
        // stack from a CLONE OF THE CURSOR carrying the crafted amount, and hand the player more of
        // what they were already holding instead of what they actually made. Silent substitution.
        //
        // No gate row can catch this: nothing on the dev server mutates a craft result, so row 1e
        // passes on a build that has the bug. This branch is the only protection there is.
        //
        // Two callers agreeing TODAY is not two callers sharing an input. Same shape NEXT.md's
        // Stats Slice 3 section names, and slice 2 meets it again when recipes come from content.
        if (!cursor.isSimilar(crafted)
                || cursor.getAmount() + crafted.getAmount() > cursor.getMaxStackSize()) {
            MenuSafety.give(viewer, crafted);
            return;
        }

        ItemStack merged = cursor.clone();
        merged.setAmount(cursor.getAmount() + crafted.getAmount());
        viewer.setItemOnCursor(merged);
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
     *
     * @param pinned the recipe the preview matched, <b>captured by the CALLER before this runs</b>
     *               and never re-read from {@link #previewedRecipe} in here. That is the whole
     *               difference between the fix and a fix-shaped bug: each pass commits and then
     *               recomputes the preview, so the FIELD MOVES during the loop -- it tracks whatever
     *               the shrinking grid now makes. Re-pinning to that mid-loop is the exact defect
     *               the pin exists to close: a grid loaded with planks and 50 iron crafts its
     *               shields, runs out of planks, re-matches to the iron nugget recipe and converts
     *               the remaining ingots. With a pin apparently in place.
     */
    private void craftRepeatedly(Optional<NamespacedKey> pinned) {
        // A PARAMETER RATHER THAN A FIELD READ, AND THAT IS THE ONLY THING PROTECTING THIS.
        //
        // Taking it as an argument does not make the bug impossible -- previewedRecipe is still a
        // field and still in scope. What it does is make violating the rule something a reader has
        // to WRITE IN, against a parameter that is already correct, rather than the natural thing to
        // reach for. "The pin is a field, read it" is the version someone writes next year, and it
        // now has to be written next to a parameter that already holds the answer.
        //
        // THIS IS CONSPICUOUS, NOT WITNESSED, and the distinction is deliberate. The property has NO
        // check: no test can see it (the field's movement needs a live menu and a live grid), and
        // gate mutation M6 -- the one build that could have shown it -- was declined twice and is
        // recorded in GATE-crafting.md as WILL NOT BE RUN. Re-read the field here and every test
        // stays green, every gate row still passes, and players lose ingots.

        for (int pass = 0; pass < MAX_BULK_CRAFTS; pass++) {
            // The SAME call the single-click path makes, returning the SAME finished item. This is
            // the third caller of the craft output, and the reason commitCraft returns an ItemStack
            // rather than an ItemCraftResult: with the mint applied by the callers, a bulk craft
            // would have shipped every item plain and unrolled while a single click minted -- and a
            // gate row that counts output rather than opening it would have passed.
            ItemStack crafted = commitCraft(pinned);
            if (crafted == null) return;
            MenuSafety.give(viewer, crafted);
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
     * <p><b>Returns the FINISHED item -- minted and rolled -- not the raw {@code ItemCraftResult}.</b>
     * That is deliberate and it is the difference between one place to forget and three. Three
     * callers read this output: {@link #craftOnceToCursor}, {@link #craftRepeatedly} and (through
     * its own path) the preview. With the mint applied by each caller, the bulk path would have
     * shipped plain unrolled items while a single click minted, and a gate row that counts output
     * rather than inspecting it would never have noticed.
     *
     * <p><b>The roll happens HERE, and only here.</b> Crafting is an acquisition path, so it rolls
     * exactly as {@code /rpg give} and the kit grant do -- see
     * {@link EnchantRollItems#rollOnAcquire}. The preview never enters this method, which is how it
     * mints without rolling: a structural guarantee rather than a rule someone has to remember.
     *
     * @return the item to hand over, or {@code null} when nothing was crafted and nothing changed.
     */
    private ItemStack commitCraft(Optional<NamespacedKey> pinned) {
        ItemStack[] matrix = readMatrix();

        // The screen, BEFORE the server is ever asked. Exhaustive switch expression, no default.
        boolean eligible = switch (CraftMatrixScreen.verdict(matrix, adapters.keys())) {
            case VANILLA_ELIGIBLE -> true;
            case CONTAINS_GEAR -> false;
        };
        if (!eligible) return null;

        // YOU RECEIVE WHAT YOU WERE SHOWN. Match first, compare to the pin, and refuse WITHOUT
        // COMMITTING if the grid now makes something else.
        //
        // This closes by construction the divergence this arc has met three times -- NEXT.md's
        // Stats Slice 3 "SHARING A FORMULA IS NOT SHARING ITS INPUTS", the substitution re-check in
        // craftOnceToCursor, and row N8's caveat are all the same defect. It is the first time it is
        // made UNREACHABLE rather than guarded: two callers cannot disagree about which recipe this
        // is, because only one of them decides and the other is handed the answer.
        //
        // An UNKEYED match cannot be pinned. Recipe declares no key -- only CraftingRecipe and
        // ComplexRecipe do -- so a hypothetical unkeyed crafting recipe would be unpinnable, and
        // BULK refuses rather than proceeding blind. A single click still commits: it matches once
        // and commits once, so re-matching cannot diverge, and craftOnceToCursor's own re-check
        // still covers a substituted result. Weaker, and said so.
        if (!matches(Bukkit.getCraftingRecipe(matrix, viewer.getWorld()), pinned)) return null;

        // The PLAYER overload: this is a real craft, and its javadoc says it calls
        // PrepareItemCraftEvent so other plugins observe it. Re-entering our own guard is harmless
        // -- the matrix was screened one line above and holds nothing of ours.
        ItemCraftResult result = Bukkit.craftItemResult(matrix, viewer.getWorld(), viewer);
        if (isEmpty(result.getResult())) return null;

        writeMatrix(result.getResultingMatrix());
        for (ItemStack overflow : result.getOverflowItems()) {
            MenuSafety.give(viewer, overflow);
        }

        ItemStack vanilla = result.getResult();
        Optional<GearDefinition> claimed = claimFor(vanilla);
        if (claimed.isEmpty()) return vanilla;

        // Minted, then rolled, in that order and never the other way: mint builds a FRESH meta with
        // an empty container, so a roll written first would be discarded by it.
        GearDefinition definition = claimed.get();
        ItemStack minted = GearItems.mint(definition, adapters);
        EnchantRollItems.rollOnAcquire(minted, GearItems.gearClassOf(definition), adapters);
        return minted;
    }

    /**
     * Which gear definition, if any, this vanilla result should be replaced by.
     *
     * <p>Shared by the commit and the preview so the slot cannot advertise one item and hand over
     * another -- the third time this arc has met two callers that agree today, and the reason they
     * share a function rather than an intention.
     *
     * <p>The durability test here is BELT-AND-BRACES, not the real check. Boot already refuses a
     * {@code craft_result} naming a material with no durability
     * ({@code ContentValidator.validateCraftResults}), so nothing in the index can fail it. It stays
     * because it costs nothing and because the index is reachable from a future caller that has not
     * been through that validation.
     *
     * <p>Note this is a DIFFERENT test from the Crafter block's durability guard, which is the whole
     * policy there and applies to items no definition has ever claimed. They must not be merged.
     */
    /**
     * The stable identity of a matched recipe, or empty when it has none.
     *
     * <p>{@code Recipe} declares only {@code getResult()} -- it does NOT extend {@code Keyed}, which
     * was verified against the pinned jar rather than assumed. The narrowing below is therefore
     * mandatory, and it covers everything a crafting grid can return: {@code CraftingRecipe} (shaped
     * and shapeless) and {@code ComplexRecipe} (firework rockets, dye tables, book cloning) both
     * implement {@code Keyed}. The only unkeyed recipe in the API is {@code MerchantRecipe}, which
     * no crafting grid produces.
     *
     * <p>That completeness matters: slice 1 delegated matching to the server SPECIFICALLY because it
     * handles {@code ComplexRecipe}, so an identity that could not represent one would have
     * re-introduced the hand-rolled matcher this arc deleted.
     */
    static Optional<NamespacedKey> identityOf(Recipe recipe) {
        return recipe instanceof Keyed keyed ? Optional.of(keyed.getKey()) : Optional.empty();
    }

    /**
     * Is this freshly matched recipe the one the player was shown?
     *
     * <p>Empty pin means the preview showed nothing, so nothing may be committed. An unkeyed match
     * can never equal a pin, which is what makes the bulk loop stop rather than proceed blind.
     */
    static boolean matches(Recipe matched, Optional<NamespacedKey> pinned) {
        if (pinned.isEmpty()) return false;
        return identityOf(matched).map(pinned.get()::equals).orElse(false);
    }

    private Optional<GearDefinition> claimFor(ItemStack vanillaResult) {
        if (isEmpty(vanillaResult)) return Optional.empty();
        if (WeaponDurability.maxOf(vanillaResult).isEmpty()) return Optional.empty();
        return adapters.craftResults().forResult(vanillaResult.getType().getKey().getKey());
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

        // THE PIN IS SET HERE AND NOWHERE ELSE, because this is the only place that decides what the
        // player is looking at. Recorded before the item, so the two cannot disagree about which
        // recipe the slot is showing.
        previewedRecipe = switch (CraftMatrixScreen.verdict(matrix, adapters.keys())) {
            case CONTAINS_GEAR -> Optional.empty();
            case VANILLA_ELIGIBLE ->
                    identityOf(Bukkit.getCraftingRecipe(matrix, viewer.getWorld()));
        };

        ItemStack preview = switch (CraftMatrixScreen.verdict(matrix, adapters.keys())) {
            // A matrix holding any of our gear is invisible to the server's matcher. In slice 1
            // that is simply "no recipe"; slice 2 asks our own gear-id table here instead.
            case CONTAINS_GEAR -> null;
            case VANILLA_ELIGIBLE -> {
                ItemCraftResult result = Bukkit.craftItemResult(matrix, viewer.getWorld());
                if (isEmpty(result.getResult())) yield null;

                // THE PREVIEW MINTS, so the slot shows what the player will actually receive rather
                // than the vanilla item it is about to replace. Same claimFor the commit uses.
                //
                // BUT IT DOES NOT ROLL, deliberately. The roll is a ThreadLocalRandom draw: if both
                // sides rolled they would draw INDEPENDENTLY, and the slot would advertise enchant
                // candidates the player is not going to get. Rolling only here would waste a draw on
                // a stack that is usually discarded. So the preview is the minted, UNROLLED item,
                // and the enchant lines are the one expected difference between what is shown and
                // what is received.
                Optional<GearDefinition> claimed = claimFor(result.getResult());
                yield claimed.isPresent()
                        ? GearItems.mint(claimed.get(), adapters)
                        : result.getResult().clone();
            }
        };

        getInventory().setItem(RESULT_SLOT, preview);
    }

    // ------------------------------------------------------------------ matrix

    /**
     * The grid as the server wants it: nine entries, row-major, every one a CLONE.
     *
     * <p><b>Cloned because the API says the craft overloads may modify the array they are given.</b>
     * {@code getCraftingRecipe}'s javadoc is the only one that disclaims it, and it does so by
     * pointing AT the craft call as the thing that does:
     *
     * <blockquote>"This method will not modify the provided ItemStack array, for that, use
     * {@code craftItem(ItemStack[], World, Player)}."</blockquote>
     *
     * <p>No {@code craftItemResult} overload disclaims it. {@code Inventory.getItem} hands back a
     * stack that mirrors the underlying slot, so passing live references would let the server write
     * THROUGH this array into the player's grid. Two paths were exposed, and both lose items
     * silently:
     *
     * <ul>
     *   <li>the PREVIEW, which runs on every grid change -- an in-place debit there would eat the
     *       grid while the player was still deciding what to build;
     *   <li>the COMMIT's empty-result abort, which deliberately writes nothing back, so a mutation
     *       would stand un-corrected: ingredients gone, no output.
     * </ul>
     *
     * <p>The commit's success path was already safe -- {@link #writeMatrix} overwrites all nine
     * slots from {@code getResultingMatrix()} -- but "safe on the path that happens to write
     * everything back" is not a property to rest an item on. Nine clones per grid change is a price
     * worth paying to make the server physically unable to touch the grid.
     */
    private ItemStack[] readMatrix() {
        ItemStack[] matrix = new ItemStack[MATRIX_LENGTH];
        for (int index = 0; index < MATRIX_LENGTH; index++) {
            ItemStack resting = getInventory().getItem(CraftingMenuLayout.rawSlotForMatrix(index));
            matrix[index] = isEmpty(resting) ? null : resting.clone();
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
