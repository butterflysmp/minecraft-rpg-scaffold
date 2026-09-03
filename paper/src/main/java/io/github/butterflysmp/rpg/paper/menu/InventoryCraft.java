package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.CollectPlan;
import io.github.butterflysmp.rpg.core.weapon.CraftCount;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponDurability;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemCraftResult;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;
import java.util.Optional;

/**
 * The one path from "a recipe and an inventory" to "the player is holding the thing".
 *
 * <h2>WHY THIS IS A CLASS AND NOT A METHOD ON A MENU</h2>
 *
 * Until slice 6 all of this lived in {@code CraftingMenu}, which was correct while there was exactly
 * one surface. The recipe browser is a SEPARATE menu, opened after the crafting menu has closed, so
 * there is no {@code CraftingMenu} instance for it to call into. The choice was a second copy or a
 * shared home, and a second copy of this particular code is not survivable: it is the most-gated
 * method in the arc, and the invariants below are ones a reader cannot re-derive from the code.
 *
 * <p>So every caller gets, BY CONSTRUCTION rather than by three implementations remembering:
 *
 * <ul>
 *   <li>the pin captured ONCE before the loop, so a bulk run cannot drift onto another recipe;
 *   <li>{@link MenuSafety#fits} checked BEFORE each pass, so a full inventory costs no ingredients;
 *   <li>the debit BY RECORDED SLOT, never by re-finding similar stacks;
 *   <li>the {@code CraftMatrixScreen} gear exclusion, before the server is ever asked;
 *   <li>mint THEN roll, in that order;
 *   <li>craft first, debit second, in one synchronous pass with no scheduler hop between.
 * </ul>
 *
 * <h2>THE MESSAGES ARE THE CALLER'S, DELIBERATELY</h2>
 *
 * {@link #craft} returns an {@link Outcome} and says nothing to the player. Two reasons, and the
 * second is the load-bearing one:
 *
 * <ol>
 *   <li>the surfaces word things differently -- the suggestion column says "You no longer have the
 *       materials", because it just showed the player a button claiming they did, while the browser
 *       lists recipes it never promised were affordable;
 *   <li><b>moving the messages in here would have made this a behaviour change rather than a pure
 *       relocation</b>, and a relocation is the only kind of move whose faithfulness can be PROVED
 *       by diffing the moved bodies. Keeping the wording at the call sites is what let this slice
 *       demonstrate the move changed nothing, instead of arguing it.
 * </ol>
 *
 * <p>The pin helpers {@code identityOf} and {@code matches} deliberately stay on
 * {@code CraftingMenu}: {@code RecipePinTest} pins them there, and a test file that does not move is
 * a test file that cannot be silently weakened by the move.
 */
final class InventoryCraft {

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
     *
     * <p>It lives here rather than on {@code CraftingMenu} because the inventory bulk loop is here
     * now. The GRID bulk loop still reads it from {@code CraftingMenu.craftRepeatedly}, which is the
     * honest state of things: one bound, two loops, and the constant sitting with whichever loop
     * could not reach it otherwise.
     */
    static final int MAX_BULK_CRAFTS = 64;

    private final Player viewer;
    private final AdapterContext adapters;

    InventoryCraft(Player viewer, AdapterContext adapters) {
        this.viewer = viewer;
        this.adapters = adapters;
    }

    /**
     * What a bulk run did, so the caller can say so in its own words.
     *
     * @param crafted        how many were actually made. Zero means nothing was taken either.
     * @param inventoryFull  the run stopped because there was no room, NOT because materials ran
     *                       out. The two need different messages and are indistinguishable from
     *                       {@code crafted} alone -- a run that makes 3 of 64 looks identical
     *                       either way.
     */
    record Outcome(int crafted, boolean inventoryFull) {}

    /**
     * Craft {@code recipe} out of the player's inventory, once or up to {@link #MAX_BULK_CRAFTS}.
     *
     * <p>Says nothing to the player and refreshes nothing -- see the class javadoc. The caller
     * decides the wording and does its own repaint ONCE, after this returns.
     */
    Outcome craft(Recipe recipe, boolean bulk) {
        NamespacedKey pin = RecipeProbe.keyOf(recipe);
        Optional<NamespacedKey> pinned = Optional.ofNullable(pin);

        int crafted = 0;
        int passes = bulk ? MAX_BULK_CRAFTS : 1;
        boolean full = false;
        for (int pass = 0; pass < passes; pass++) {
            // The same look-before-you-leap the grid loop does, and for the same reason -- see
            // craftRepeatedly. Checked BEFORE the craft so a full inventory costs no ingredients.
            if (!MenuSafety.fits(viewer, recipe.getResult())) {
                full = true;
                break;
            }
            if (!craftOneFromInventory(recipe, pinned)) break;
            crafted++;
        }
        return new Outcome(crafted, full);
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
        CraftCount.Candidate candidate = RecipeProbe.probeOne(recipe, groups, RecipeProbe.tierOf(recipe, adapters));
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
            if (!MenuSafety.isEmpty(remainder)) MenuSafety.give(viewer, remainder.clone());
        }
        MenuSafety.give(viewer, outcome.crafted());
        return true;
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
     * <h2>THE CONSERVATION RULE, and why "consumed = input MINUS resulting" is WRONG</h2>
     *
     * The obvious way to debit an inventory craft is to work out what was consumed by subtracting
     * the resulting matrix from the input matrix, per slot. <b>That formulation is incoherent for
     * exactly the case rows 12 and 12c exist for.</b> A milk bucket does not DECREASE when a cake is
     * made -- it BECOMES an empty bucket. There is no per-slot quantity to subtract, and any code
     * that tries lands on either "three buckets vanished" or "three buckets appeared from nowhere".
     *
     * <p>What holds instead, with no such question asked:
     *
     * <pre>
     * A       is exactly what left the inventory  (what the assembly took, BY SLOT)
     * R + O   is exactly what the engine says remains  (resulting matrix + overflow)
     *
     * the player ends at   inventory - A + R + O + result
     * </pre>
     *
     * <b>That is true WITHOUT EVER KNOWING which part of A was consumed and which was transformed.</b>
     * Any formulation that needs to know is wrong for cake. So the inventory caller debits the whole
     * assembly and hands back the whole resulting matrix, and the two net to the same thing a
     * per-slot diff would have produced for the easy cases and nothing sane for the hard one.
     *
     * <p>The GRID gets this for free -- writing the resulting matrix over the slots the input came
     * from IS {@code -A + R} in one operation -- which is precisely why the asymmetry between the
     * two callers is easy to miss.
     *
     * @return the outcome, or {@code null} when nothing was crafted and nothing changed.
     */
    CraftOutcome commitCraft(ItemStack[] matrix, Optional<NamespacedKey> pinned) {
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
        if (!CraftingMenu.matches(Bukkit.getCraftingRecipe(matrix, viewer.getWorld()), pinned)) return null;

        // The PLAYER overload: this is a real craft, and its javadoc says it calls
        // PrepareItemCraftEvent so other plugins observe it. Re-entering our own guard is harmless
        // -- the matrix was screened one line above and holds nothing of ours.
        ItemCraftResult result = Bukkit.craftItemResult(matrix, viewer.getWorld(), viewer);
        if (MenuSafety.isEmpty(result.getResult())) return null;

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
    record CraftOutcome(ItemStack crafted, ItemStack[] resultingMatrix) {}

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
    Optional<GearDefinition> claimFor(ItemStack vanillaResult) {
        if (MenuSafety.isEmpty(vanillaResult)) return Optional.empty();
        if (WeaponDurability.maxOf(vanillaResult).isEmpty()) return Optional.empty();
        return adapters.craftResults().forResult(vanillaResult.getType().getKey().getKey());
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
            if (MenuSafety.isEmpty(stack)) continue;
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

}
