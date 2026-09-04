package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.recipe.RecipeDefinition;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers every {@link ShapedRecipe} this plugin owns, from content, at enable.
 *
 * <h2>THE REGISTERED RESULT IS THE PLAIN VANILLA MATERIAL, NEVER A MINTED ITEM</h2>
 *
 * A recipe's result is {@code new ItemStack(material)} for whatever the minted gear's material is
 * -- a Flint Staff registers a plain {@code stick}. Three reasons, and none of them is cosmetic:
 *
 * <ul>
 *   <li><b>The mint must ROLL.</b> A minted item baked in at registration would be one enchant roll
 *       handed to every player who ever crafts it, forever. The roll happens per craft, in
 *       {@code InventoryCraft.commitCraft}.
 *   <li><b>A registered result is a SHARED PROTOTYPE.</b> {@code CrafterCraftEvent.getResult},
 *       {@code MenuSafety.fits}, the cursor test and every suggestion icon read this stack; handing
 *       any of them a tagged item would leak minted gear onto surfaces that never minted it.
 *   <li><b>It is why the two guards downstream had to change.</b> A plain stick is not durable, so
 *       {@code RpgListeners.onCrafterCraft}'s durability test waves it through, and
 *       {@code MenuSafety.fits} counts a partial stick stack as room the minted staff cannot use.
 *       Both are handled at those sites; this is the sentence that explains why they had to be.
 * </ul>
 *
 * <h2>REMOVE-THEN-ADD, UNCONDITIONALLY -- so this need not know what /reload does</h2>
 *
 * Whether a second {@code onEnable} finds our key already present, whether {@code addRecipe} throws
 * or returns false on a duplicate, and whether {@code /reload} calls {@code resetRecipes()} are
 * three questions the pinned API does not answer. Rather than design around a guess, every path
 * collapses into one: remove the key (the result is IGNORED -- false simply means it was not there),
 * then add. Afterwards exactly one recipe exists under our key whatever the prior state was.
 *
 * <p>{@code removeRecipe}'s javadoc warns it "may cause permanent loss of data associated with that
 * recipe (eg whether it has been discovered by players)". <b>That cost is zero here, and the
 * argument holds only inside our own namespace:</b> we never call {@code discoverRecipe}, and
 * crafting tables are hijacked to our menu so the vanilla recipe book is unreachable. There is no
 * discovery state under our namespace to lose. Which is exactly why the key is built from the
 * plugin and the content-authored id, and <b>never</b> from a namespace content could choose --
 * and why {@code resetRecipes()} must never be called, since that would take datapacks with it.
 *
 * <h2>THE READBACK IS NOT BELT AND BRACES</h2>
 *
 * {@code addRecipe}'s boolean is the API's CLAIM that it worked. {@code getRecipe(key)} is the
 * WITNESS. A check that did not run looks exactly like a check that passed, and a roster that
 * silently declined our recipe would look exactly like one that took it.
 */
public final class RecipeRegistrar {

    private RecipeRegistrar() {}

    /**
     * What happened, in numbers the boot log can print.
     *
     * <p>{@code authored} comes from the REGISTRY and the rest from this walk, deliberately: two
     * sources for one line means a dead registrar reads wrong at a glance instead of reading
     * self-consistently and wrong. Same trade the mint-on-craft line already makes.
     *
     * @param replaced how many keys already held a recipe when we got there. On a fresh boot this
     *                 is 0; if it is not, something else registered under our namespace. It is also
     *                 the only observable answer to what {@code /reload} does to our roster.
     */
    public record Report(int authored, int registered, int replaced, int refused) {}

    public static Report registerAll(Plugin plugin, Collection<RecipeDefinition> recipes,
                                     CraftResultIndex claims, Logger log) {
        int registered = 0;
        int replaced = 0;
        int refused = 0;

        for (RecipeDefinition definition : recipes) {
            NamespacedKey key;
            try {
                // RecipeDefinition already refused any id NamespacedKey would reject, so this
                // cannot throw for a definition that loaded. Caught anyway: the alternative to
                // being wrong here is a dead server, and the cost of being right is one branch.
                key = new NamespacedKey(plugin, definition.id());
            } catch (IllegalArgumentException ex) {
                refused++;
                log.warning("Recipe '" + definition.id() + "' is not a legal key: " + ex.getMessage());
                continue;
            }

            ShapedRecipe built;
            try {
                built = build(plugin, key, definition, claims, log);
            } catch (IllegalArgumentException ex) {
                // Bukkit's shape/setIngredient Preconditions. Reaching here means a rule
                // RecipeDefinition is supposed to enforce has drifted from Bukkit's -- worth a
                // stack trace, and NEVER worth aborting the boot for.
                built = null;
                log.log(Level.WARNING, "Recipe '" + definition.id()
                        + "' was refused by Bukkit: " + ex.getMessage(), ex);
            }

            if (built == null) {
                refused++;
                // STILL REMOVE. A recipe registered by an earlier boot must not outlive the
                // definition that justified it: a stale one would keep matching and hand out plain
                // items with nothing left in content to explain it.
                if (plugin.getServer().removeRecipe(key)) {
                    log.warning("Removed a previously registered '" + key
                            + "'; its definition is no longer valid.");
                }
                continue;
            }

            if (plugin.getServer().removeRecipe(key)) replaced++;

            if (!plugin.getServer().addRecipe(built)) {
                refused++;
                log.severe("The server declined to add recipe '" + key + "'.");
                continue;
            }
            // THE WITNESS, not the claim. See the class javadoc.
            if (plugin.getServer().getRecipe(key) == null) {
                refused++;
                log.severe("Recipe '" + key + "' reported as added but is not on the roster.");
                continue;
            }

            registered++;

            if (definition.fitsInTwoByTwo()) {
                log.warning("Recipe '" + key + "' fits a 2x2 grid, so it can be crafted in a "
                        + "player's own inventory -- the one crafting surface that does not mint. "
                        + "Crafted there it hands over a plain '"
                        + built.getResult().getType().getKey().getKey()
                        + "' instead of the gear it claims. Make it 3 rows or 3 columns, or accept "
                        + "that the inventory grid is a way to waste the materials.");
            }
        }

        return new Report(recipes.size(), registered, replaced, refused);
    }

    /** Null when anything the recipe names cannot be resolved; every reason is logged by name. */
    private static ShapedRecipe build(Plugin plugin, NamespacedKey key, RecipeDefinition definition,
                                      CraftResultIndex claims, Logger log) {
        // The index has already resolved 'mints' against all four gear registries and warned about
        // an unresolvable one. An absent claim here therefore means a recipe that was DROPPED, and
        // registering it would hand a player a plain item for their materials forever.
        Optional<GearDefinition> minted = claims.forRecipe(definition.id());
        if (minted.isEmpty()) return null;

        Material result = Material.matchMaterial(minted.get().material());
        if (result == null) {
            log.warning("Recipe '" + key + "' mints '" + minted.get().id() + "', whose material '"
                    + minted.get().material() + "' is not a material. Not registered.");
            return null;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(result));
        recipe.shape(definition.shape().toArray(new String[0]));

        for (Map.Entry<Character, String> entry : definition.ingredients().entrySet()) {
            Material ingredient = Material.matchMaterial(entry.getValue());
            if (ingredient == null) {
                log.warning("Recipe '" + key + "' names ingredient '" + entry.getValue()
                        + "' for '" + entry.getKey() + "', which is not a material. Not registered.");
                return null;
            }
            recipe.setIngredient(entry.getKey(), ingredient);
        }

        return recipe;
    }
}
