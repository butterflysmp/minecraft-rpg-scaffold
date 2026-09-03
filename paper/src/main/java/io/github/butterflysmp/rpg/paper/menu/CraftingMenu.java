package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.CollectPlan;
import io.github.butterflysmp.rpg.core.weapon.CraftCount;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponDurability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemCraftResult;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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

    /**
     * What the player could craft, as of the last recompute.
     *
     * <p>ADVISORY, NEVER AUTHORITATIVE. It can be stale the instant it is computed -- a hopper, another
     * plugin, or the player staging a recipe between the recompute and the click. Every commit
     * re-verifies against the pin and the live inventory and refuses cleanly if it cannot deliver.
     */
    private RecipeProbe.Result suggestions = RecipeProbe.Result.empty();

    /**
     * A cheap fingerprint of the material pool the suggestions were computed from.
     *
     * <p>THE CADENCE GUARD. A full recipe walk costs more than a preview refresh, and the preview
     * fires on every permitted move -- several times a second while a player loads a grid. Comparing
     * this first means the walk runs when the POOL actually moved and not when the player is merely
     * rearranging what they already had.
     */
    private String poolSignature = "";

    private final AdapterContext adapters;

    public CraftingMenu(Player viewer, AdapterContext adapters) {
        super(viewer, SIZE, MenuIcons.line("Crafting", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        render();
        refreshPreview();
        refreshSuggestions(true);
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
        // The result means "craft repeatedly"; a suggestion means the same thing. Neither is an
        // input slot, so the router performs NO move for either -- it only lets the gesture be
        // heard. Widening this is what re-opens gate row 13.
        return slot == RESULT_SLOT || CraftingMenuLayout.suggestionIndexOf(slot).isPresent();
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

        OptionalInt suggestion = CraftingMenuLayout.suggestionIndexOf(click.slot());
        if (suggestion.isPresent()) {
            // No DOUBLE_CLICK arm is needed here, unlike the result slot. MenuRouting intercepts
            // that gesture by TYPE and collectToCursor refuses a non-input menu slot outright, so
            // it never reaches onClick at all. Verified rather than assumed.
            craftFromSuggestion(suggestion.getAsInt(), click.click().isShiftClick());
            return;
        }

        if (click.slot() == CraftingMenuLayout.BROWSER_SLOT) {
            say("The recipe browser is not built yet.");
            return;
        }

        if (click.itemMoved()) {
            // The item has NOT landed yet for a PERMITTED move: InventoryClickEvent fires before
            // the place applies. Recompute next tick, when the grid holds what the player thinks it
            // holds. onEntityLater is the sanctioned route and clamps a zero delay to one tick.
            adapters.scheduler().onEntityLater(viewer, this::afterGridChanged, 1);
        }
    }

    /**
     * The grid changed, so the preview must catch up -- and the SUGGESTIONS may need to as well.
     *
     * <p>Both, because moving an item between the inventory and the grid changes the material pool
     * Quick Craft counts. Staging six planks genuinely does reduce what the inventory can make.
     * {@link #refreshSuggestions} decides for itself whether the pool actually moved.
     */
    private void afterGridChanged() {
        refreshPreview();
        refreshSuggestions(false);
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
        // BOTH, because a grid craft changes the INVENTORY too: bulk output goes there through
        // MenuSafety.give, and so do overflow remainders. Refreshing only the preview would leave
        // the suggestion column counting materials the player no longer has -- or missing ones they
        // just gained. The signature comparison inside refreshSuggestions decides whether the walk
        // actually runs, so this is not a full recompute on every take.
        refreshPreview();
        refreshSuggestions(false);
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

        CraftOutcome outcome = commitCraft(readMatrix(), previewedRecipe);
        if (outcome == null) return;
        // The grid IS the matrix, so writing it back both debits the ingredients and returns the
        // remainders in one write. Quick Craft has no such luck -- see craftFromSuggestion.
        writeMatrix(outcome.resultingMatrix());
        ItemStack crafted = outcome.crafted();

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
            CraftOutcome outcome = commitCraft(readMatrix(), pinned);
            if (outcome == null) return;
            writeMatrix(outcome.resultingMatrix());
            MenuSafety.give(viewer, outcome.crafted());
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
     * <h2>IT TAKES A MATRIX, AND THAT IS THE SLICE 5 WIDENING</h2>
     *
     * It used to read {@code GRID_SLOTS} itself. Quick Craft's matrix is not the grid -- it is a
     * scratch copy assembled from inventory stacks -- so the matrix became a PARAMETER rather than
     * this method growing a second copy of itself. {@code Bukkit.craftItemResult} already takes a
     * bare {@code ItemStack[]}, so the engine never knew about the grid in the first place.
     *
     * <p><b>The two callers do NOT differ only in where leftovers land</b>, and an early draft of
     * the slice-5 plan said they did. For the GRID the matrix IS the grid, so writing the resulting
     * matrix back does two jobs in one write: it removes what was consumed AND returns the
     * remainders. For the INVENTORY nothing writes back at all -- {@code craftItemResult} never
     * touches an inventory -- so the caller must debit what it assembled and hand the remainders
     * over itself. Hence {@link CraftOutcome}: this method stops at the point the two genuinely
     * diverge and returns both halves rather than deciding for them.
     *
     * <p>The OVERFLOW give stays in here, because it does not diverge: on both surfaces a remainder
     * that would not fit back into the matrix goes to the player.
     *
     * @return the outcome, or {@code null} when nothing was crafted and nothing changed.
     */
    private CraftOutcome commitCraft(ItemStack[] matrix, Optional<NamespacedKey> pinned) {
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

        // The overflow, on BOTH surfaces. Dropping these on the floor of this method would be a
        // silent loss; they go to the player through the one give path.
        for (ItemStack overflow : result.getOverflowItems()) {
            MenuSafety.give(viewer, overflow);
        }

        ItemStack vanilla = result.getResult();
        Optional<GearDefinition> claimed = claimFor(vanilla);
        if (claimed.isEmpty()) return new CraftOutcome(vanilla, result.getResultingMatrix());

        // Minted, then rolled, in that order and never the other way: mint builds a FRESH meta with
        // an empty container, so a roll written first would be discarded by it.
        GearDefinition definition = claimed.get();
        ItemStack minted = GearItems.mint(definition, adapters);
        EnchantRollItems.rollOnAcquire(minted, GearItems.gearClassOf(definition), adapters);
        return new CraftOutcome(minted, result.getResultingMatrix());
    }

    /**
     * One craft's two outputs: the finished item, and what the matrix became.
     *
     * <p>Both are needed because the two surfaces send the matrix to different places -- the grid
     * writes it back over itself, Quick Craft hands it to the player. Returning both is what lets
     * {@link #commitCraft} stay one method: it does everything that is common and stops exactly
     * where the surfaces diverge.
     *
     * @param crafted         the item to hand over -- already minted and rolled where content claims
     *                        the result. Never null in a returned outcome.
     * @param resultingMatrix what the server says the matrix holds afterwards. <b>The remainders
     *                        live in here</b>: a cake leaves three EMPTY BUCKETS, honey bottles
     *                        leave glass bottles. Whichever surface receives it must not decrement
     *                        by hand instead -- that is what destroys them.
     */
    private record CraftOutcome(ItemStack crafted, ItemStack[] resultingMatrix) {}

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

    // ------------------------------------------------------------- quick craft

    /**
     * Recompute what the player could craft, and repaint the column.
     *
     * <p><b>THE CADENCE IS THE EXPENSIVE DECISION IN THIS SLICE.</b> A full roster walk is far more
     * work than a preview refresh, and the preview fires on every permitted move -- several times a
     * second while someone loads a grid. So the pool's fingerprint is compared FIRST and the walk
     * only runs when the material pool actually moved, not when a player is rearranging what they
     * already had.
     *
     * @param force recompute regardless of the fingerprint. True on open and after a craft, where
     *              the pool has certainly moved and the comparison would only be a wasted read.
     */
    private void refreshSuggestions(boolean force) {
        String signature = poolSignature();
        if (!force && signature.equals(poolSignature)) return;
        poolSignature = signature;

        suggestions = RecipeProbe.of(viewer.getInventory(), adapters.keys());
        renderSuggestions();
    }

    /**
     * A cheap fingerprint of what the player is carrying.
     *
     * <p>Material and amount per slot, which is enough to notice every change that could alter a
     * count. Deliberately NOT a full group-and-probe pass: the whole point is to be cheaper than the
     * thing it guards.
     *
     * <p>It over-fires rather than under-fires -- moving a stack between two inventory slots changes
     * this string without changing any count -- and that is the safe direction. A missed recompute
     * shows a stale number; a spare recompute costs one walk.
     */
    private String poolSignature() {
        StringBuilder signature = new StringBuilder();
        for (ItemStack item : viewer.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                signature.append('-');
            } else {
                signature.append(item.getType().getKey().getKey()).append('x').append(item.getAmount());
            }
            signature.append('|');
        }
        return signature.toString();
    }

    /** Paint the column: one icon per suggestion, filler for the rest. */
    private void renderSuggestions() {
        List<CraftCount.Craftable> ranked = suggestions.suggestions();
        for (int index = 0; index < CraftingMenuLayout.SUGGESTIONS; index++) {
            int slot = CraftingMenuLayout.rawSlotForSuggestion(index);
            if (index >= ranked.size()) {
                getInventory().setItem(slot, MenuIcons.filler());
                continue;
            }
            CraftCount.Craftable craftable = ranked.get(index);
            Recipe recipe = suggestions.recipes().get(craftable.key());
            getInventory().setItem(slot, suggestionIcon(recipe, craftable));
        }
    }

    /**
     * One suggestion icon: what it makes, and how many times.
     *
     * <p>The icon is the RESULT stack, so the player is looking at the thing they will receive --
     * the same "you receive what you were shown" rule the result slot follows. The count is lore
     * rather than the stack amount, because the stack amount is the recipe's own yield and
     * overloading it would make "8 sticks" and "8 crafts" indistinguishable.
     */
    private ItemStack suggestionIcon(Recipe recipe, CraftCount.Craftable craftable) {
        ItemStack result = recipe == null ? null : recipe.getResult();
        if (isEmpty(result)) return MenuIcons.filler();

        ItemStack icon = result.clone();
        icon.editMeta(meta -> meta.lore(List.of(
                MenuIcons.line("Craft " + craftable.count() + " more", NamedTextColor.GRAY),
                MenuIcons.line("Uses items from your inventory", NamedTextColor.DARK_GRAY))));
        return icon;
    }

    /**
     * Craft the suggestion in this cell, once or until the materials run out.
     *
     * <h2>THE COUNT IS ADVISORY; THIS IS AUTHORITATIVE</h2>
     *
     * Between the recompute and this click the inventory can have moved -- a hopper, another plugin,
     * the player staging a recipe. Every pass re-probes the live inventory and re-verifies the pin,
     * and a failure REFUSES AND SAYS SO rather than crafting something else or half-crafting. A
     * suggestion that silently does nothing reads as a broken button.
     *
     * <h2>CRAFT FIRST, THEN DEBIT, IN ONE SYNCHRONOUS PASS</h2>
     *
     * The order is the whole safety property. Debiting first would mean a pin mismatch or an
     * insufficient-materials refusal had ALREADY taken the ingredients -- theft, on the refusal
     * path, which is exactly the path least likely to be tested by hand. Crafting first is correct
     * only because nothing hops a scheduler in between: {@code commitCraft} and the debit run in
     * the same click handler, so nothing can move underneath them.
     *
     * <h2>THE BULK TRAP</h2>
     *
     * The loop re-probes ONE recipe per pass -- 36 slots and nine choices -- and walks the full
     * roster exactly ONCE, at the end. Recomputing the roster inside the loop reads almost
     * identically and is sixty-four times the work.
     */
    private void craftFromSuggestion(int index, boolean bulk) {
        List<CraftCount.Craftable> ranked = suggestions.suggestions();
        if (index >= ranked.size()) return;   // an empty cell is not a button

        String key = ranked.get(index).key();
        Recipe recipe = suggestions.recipes().get(key);
        if (recipe == null) {
            say("That recipe is no longer available.");
            refreshSuggestions(true);
            return;
        }

        NamespacedKey pin = RecipeProbe.keyOf(recipe);
        Optional<NamespacedKey> pinned = Optional.ofNullable(pin);

        int crafted = 0;
        int passes = bulk ? MAX_BULK_CRAFTS : 1;
        for (int pass = 0; pass < passes; pass++) {
            if (!craftOneFromInventory(recipe, pinned)) break;
            crafted++;
        }

        if (crafted == 0) {
            say("You no longer have the materials for that.");
        }

        // ONCE, after the loop. Never inside it.
        refreshSuggestions(true);
        viewer.updateInventory();
    }

    /**
     * One craft out of the inventory. Returns false without changing anything if it cannot.
     *
     * <p>Every early return here happens BEFORE {@code commitCraft}, so a refusal costs the player
     * nothing. The single debit is the last mutation, exactly as {@code EnchantMenu}'s XP deduction
     * is -- which is why there is no rollback anywhere in this method.
     */
    private boolean craftOneFromInventory(Recipe recipe, Optional<NamespacedKey> pinned) {
        List<RecipeProbe.Group> groups = RecipeProbe.groupsOf(viewer.getInventory(), adapters.keys());
        CraftCount.Candidate candidate = RecipeProbe.probeOne(recipe, groups);
        if (candidate == null) return false;

        List<CraftCount.Stock> stock = RecipeProbe.stockOf(groups);

        // THE COUNT, not the assignment. A non-empty assignment only says every slot can be filled
        // once; the count says whether the player can actually afford it -- see CraftCount.assign.
        if (CraftCount.rank(List.of(candidate), stock).isEmpty()) return false;

        RecipeProbe.Assembly assembly =
                RecipeProbe.assemble(recipe, groups, CraftCount.assign(candidate, stock));
        if (assembly == null) return false;

        CraftOutcome outcome = commitCraft(assembly.matrix(), pinned);
        if (outcome == null) return false;   // pin mismatch or the server declined: nothing taken

        // CRAFTED. Now, and only now, take the ingredients.
        debit(assembly.draws());

        // The matrix was a scratch copy, so nothing writes back -- whatever the server left in it is
        // a REMAINDER the player is owed. A cake's three empty buckets arrive here.
        //
        // Note this is the whole of the "input minus resulting" the plan describes: we debit exactly
        // what we assembled and hand back exactly what came out, which nets to the same thing with
        // no per-slot subtraction to get wrong when an ingredient CHANGES TYPE mid-craft.
        for (ItemStack remainder : outcome.resultingMatrix()) {
            if (!isEmpty(remainder)) MenuSafety.give(viewer, remainder.clone());
        }
        MenuSafety.give(viewer, outcome.crafted());
        return true;
    }

    /**
     * Remove exactly what the assembly took, from the slots it took it from.
     *
     * <p><b>By SLOT, never by similarity.</b> Re-finding the stacks by comparing items would be a
     * second search that can land on different slots than the probe counted -- which is how "the
     * player had 64 planks across three stacks" quietly goes wrong. The assembly recorded the slots;
     * this applies them. Same shape the collect gesture uses.
     */
    private void debit(List<CollectPlan.Source> draws) {
        for (CollectPlan.Source draw : draws) {
            ItemStack stack = viewer.getInventory().getItem(draw.slot());
            if (isEmpty(stack)) continue;
            int left = stack.getAmount() - draw.amount();
            if (left <= 0) {
                viewer.getInventory().setItem(draw.slot(), null);
            } else {
                ItemStack reduced = stack.clone();
                reduced.setAmount(left);
                viewer.getInventory().setItem(draw.slot(), reduced);
            }
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

        // The suggestion column IS painted over here, deliberately: this lays down filler as a base
        // and the constructor calls refreshSuggestions immediately afterwards. render() runs once,
        // so there is no repaint that could blank a live column.
        //
        // A PLACEHOLDER, not a dead button. MenuIcons.placeholder says "not implemented yet" out
        // loud rather than rendering something that looks clickable and does nothing -- its own
        // javadoc argues the case, and this is the consumer it was kept for.
        getInventory().setItem(CraftingMenuLayout.BROWSER_SLOT,
                MenuIcons.placeholder(Material.BOOK, "Recipe Browser",
                        "Everything you can make, paginated."));
    }

    private void say(String message) {
        viewer.sendMessage(Component.text(message, NamedTextColor.GRAY));
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
