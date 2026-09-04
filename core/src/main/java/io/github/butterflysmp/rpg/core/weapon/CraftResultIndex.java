package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.recipe.RecipeDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Which gear definition a crafted vanilla item should become.
 *
 * <p>The reverse of everything else in this package: every other gear lookup is by id, and this one
 * answers "the player just crafted an {@code iron_chestplate} -- is that ours?". Built ONCE at boot
 * from all four registries, because the crafting preview runs several times a second and a scan per
 * craft would be a walk of every definition on every grid change.
 *
 * <h2>Keyed on {@code craft_result}, never on {@code material}</h2>
 *
 * A material is PRESENTATION. {@code WeaponDefinition.DEFAULT_MATERIAL} is {@code iron_sword} and
 * every sword-shaped weapon leaves it there, so materials are contested BY DESIGN -- shipped content
 * already has two weapons on {@code iron_sword} and always will. Keying on them would mean
 * {@code iron_sword} permanently contested, a boot warning firing forever about correct content, and
 * no sword ever minting. {@link GearDefinition#craftResult()} is an opt-in claim instead, so one
 * definition per result is a property of the data rather than a hope.
 *
 * <h2>A contested result is DROPPED, and that is the fail-safe direction</h2>
 *
 * When two definitions claim the same item, neither is indexed and crafting it yields plain vanilla.
 * Not first-wins: the loaders sort their files for determinism, so first-wins would be alphabetical
 * order making an economy decision -- and a player would receive whichever weapon sorted earlier,
 * silently, with a rename able to change it. A plain item costs a player nothing; they can craft
 * another. A wrong minted item is theirs forever.
 *
 * <p>Pure, and in {@code core}, deliberately. {@link GearDefinition#craftResult()} is a String token,
 * so nothing here needs Bukkit -- which means the collision policy, the normalisation and the
 * zero-guard all have real unit tests instead of joining the boot-gate-only list this project keeps
 * for everything on the crafting surface.
 *
 * <h2>TWO INDEXES, AND THE NAME NOW UNDER-DESCRIBES THE CLASS</h2>
 *
 * Since slice 7 this holds a second map, keyed on the id of a recipe WE registered
 * ({@link RecipeDefinition#mints()}), because:
 *
 * <blockquote><b>WE DO NOT OWN VANILLA RECIPES, SO THEY ARE IDENTIFIED BY THEIR RESULT.
 * WE OWN OURS, SO THEY ARE IDENTIFIED BY THEIR KEY.</b></blockquote>
 *
 * Both directions are permanent and neither is a migration target. A vanilla recipe has no
 * identifier we may rely on, so its result material is the only stable key -- with all the contest
 * problems argued above. A recipe of ours has a {@code NamespacedKey} we minted: unique, stable,
 * uncontested. The class was NOT renamed to match; a rename touches every consumer for no
 * behavioural change, and {@code NEXT.md} is where that gets sized.
 *
 * <h2>THE TWO AXES HAVE DIFFERENT COLLISION POLICIES, AND MUST KEEP THEM</h2>
 *
 * The result axis drops both claimants (above). <b>The recipe axis has no contest policy at all,
 * and must never grow one.</b> Two recipes minting one weapon is TWO WAYS TO CRAFT IT -- a feature,
 * not a collision. The collision that would matter, two recipes claiming one key, cannot be
 * authored: a recipe's id is its filename, and {@code RecipeRegistry} throws on a duplicate id.
 * "Make the two axes symmetric" is the obvious wrong refactor, and
 * {@code CraftResultIndexTest.twoRecipesMayMintTheSameGear} is what stops it.
 *
 * <p>The one thing the recipe axis DOES refuse is a {@code mints} naming no gear at all -- see
 * {@link #build}. <b>Do not add the mirror of that warning.</b> A gear definition that nothing
 * mints is not an error; most gear does not participate in mint-on-craft, exactly as
 * {@link CraftResultToken#normalise} already records for {@code craft_result}.
 */
public final class CraftResultIndex {

    private final Map<String, GearDefinition> byResult;
    private final Map<String, GearDefinition> byRecipe;
    private final int claimed;
    private final int contested;
    private final int recipesIndexed;
    private final int recipesDropped;

    private CraftResultIndex(Map<String, GearDefinition> byResult,
                             Map<String, GearDefinition> byRecipe,
                             int claimed, int contested, int recipesIndexed, int recipesDropped) {
        this.byResult = byResult;
        this.byRecipe = byRecipe;
        this.claimed = claimed;
        this.contested = contested;
        this.recipesIndexed = recipesIndexed;
        this.recipesDropped = recipesDropped;
    }

    /**
     * Walk every definition and index the ones that claim a craft result.
     *
     * <p>Then walk every custom recipe and index the gear it mints. That join is done HERE rather
     * than in paper so "a recipe names a gear id that does not exist" is a decision with a unit
     * test, not a boot-gate-only one.
     *
     * @param gear      all gear, from all four registries. A definition with no claim is skipped.
     * @param recipes   every custom recipe that loaded. Each names the gear it mints; one naming
     *                  gear that does not exist is reported and DROPPED, so it can never be
     *                  registered and can never hand a player a plain item in place of a weapon.
     * @param onProblem called ONCE per contested result, and once per unresolvable {@code mints},
     *                  naming the files to open. The seam that keeps this class free of a Logger
     *                  and therefore unit-testable -- the same trade {@code ContentValidator} makes
     *                  with its predicates.
     */
    public static CraftResultIndex build(Collection<? extends GearDefinition> gear,
                                         Collection<RecipeDefinition> recipes,
                                         Consumer<String> onProblem) {
        // LinkedHashMap so two boots agree on order, exactly as GearRegistry does and for the same
        // reason: the messages below are read in a log, and an order that shuffles per boot makes
        // two runs impossible to compare.
        Map<String, List<GearDefinition>> claimants = new LinkedHashMap<>();
        int claimed = 0;

        for (GearDefinition definition : gear) {
            Optional<String> claim = definition.craftResult();
            if (claim.isEmpty()) continue;
            claimed++;
            claimants.computeIfAbsent(claim.get(), key -> new ArrayList<>()).add(definition);
        }

        Map<String, GearDefinition> resolved = new LinkedHashMap<>();
        int contested = 0;

        for (Map.Entry<String, List<GearDefinition>> entry : claimants.entrySet()) {
            List<GearDefinition> claiming = entry.getValue();
            if (claiming.size() == 1) {
                resolved.put(entry.getKey(), claiming.get(0));
                continue;
            }

            contested++;
            StringBuilder ids = new StringBuilder();
            for (GearDefinition definition : claiming) {
                if (!ids.isEmpty()) ids.append(", ");
                ids.append('\'').append(definition.id()).append('\'');
            }
            onProblem.accept("Gear " + ids + " all claim craft_result '" + entry.getKey()
                    + "'. None of them will be minted on craft, because there is no way to tell "
                    + "which was meant. Give the key to exactly one of them.");
        }

        // ---- the recipe axis ------------------------------------------------------------------
        // A SEPARATE MAP, not a second population of the one above. Sharing them would mean a
        // recipe minting a stick also claiming EVERY vanilla stick craft in the world -- which is
        // the whole design error the recipe key exists to avoid, and
        // CraftResultIndexTest.aRecipeClaimDoesNotPopulateTheMaterialMap is what holds the line.
        Map<String, GearDefinition> byRecipe = new LinkedHashMap<>();
        int recipesDropped = 0;

        for (RecipeDefinition recipe : recipes == null ? List.<RecipeDefinition>of() : recipes) {
            GearDefinition minted = null;
            for (GearDefinition definition : gear) {
                if (definition.id().equals(recipe.mints())) {
                    minted = definition;
                    break;
                }
            }
            if (minted == null) {
                recipesDropped++;
                onProblem.accept("Recipe '" + recipe.id() + "' mints '" + recipe.mints()
                        + "', which is not the id of any weapon, shield, armor piece or tool. "
                        + "The recipe will NOT be registered -- registering it would hand players a "
                        + "plain item for their materials, forever, with nothing saying why.");
                continue;
            }
            // No contest check, deliberately: two recipes minting one weapon is two ways to craft
            // it. Two recipes sharing an ID cannot exist -- RecipeRegistry throws on that.
            byRecipe.put(recipe.id(), minted);
        }

        return new CraftResultIndex(resolved, byRecipe, claimed, contested, byRecipe.size(),
                recipesDropped);
    }

    /**
     * The definition a crafted item should become, or empty for an item nothing claims.
     *
     * <p>Normalises through {@link CraftResultToken#token} exactly as the build did, so a lookup
     * spelled {@code minecraft:IRON_SWORD} finds an entry authored as {@code iron_sword}. A lookup
     * that normalised differently from the build would miss every entry and the only symptom would
     * be that crafting mints nothing.
     */
    public Optional<GearDefinition> forResult(String materialToken) {
        if (materialToken == null) return Optional.empty();
        return Optional.ofNullable(byResult.get(CraftResultToken.token(materialToken)));
    }

    /** How many results are actually indexed. ZERO IS A DEFECT -- the boot says so out loud. */
    public int size() {
        return byResult.size();
    }

    /** How many definitions made a claim, contested ones included. The denominator in the boot log. */
    public int claimed() {
        return claimed;
    }

    /** How many results were dropped for being claimed twice. */
    public int contested() {
        return contested;
    }

    /**
     * The definition a craft of OUR recipe should hand over, or empty for a recipe we did not
     * register.
     *
     * <p><b>The id is BARE -- {@code flint_staff}, never {@code rpg:flint_staff}.</b> Nothing in
     * {@code core} knows our namespace, and it must not learn it: the namespace comes from the
     * plugin at runtime. Paper strips it before asking, and is also what checks the namespace is
     * OURS before asking at all -- without that check a third-party {@code otherplugin:flint_staff}
     * would resolve to our weapon.
     *
     * <p><b>There is no durability gate on this axis, and that is the point.</b> The result axis
     * carries one because a material is a weak key and durability narrows it. A recipe key is a
     * strong key and needs no narrowing -- and a gate here would refuse every weapon whose material
     * has no durability, which is the Flint Staff (a stick), the ember_staff (a blaze_rod) and the
     * ability_stone (an amethyst_shard). It would index them cleanly and then never mint them,
     * silently, with no error anywhere.
     */
    public Optional<GearDefinition> forRecipe(String recipeId) {
        if (recipeId == null) return Optional.empty();
        return Optional.ofNullable(byRecipe.get(recipeId));
    }

    /** How many custom recipes resolved to a definition. Part of the boot log's positive control. */
    public int recipesIndexed() {
        return recipesIndexed;
    }

    /**
     * How many custom recipes were dropped for minting gear that does not exist.
     *
     * <p>Counted separately from {@link #contested()} rather than folded into it. One shared
     * counter would let the boot line read self-consistent and wrong, which is the exact failure
     * the three-number positive control above exists to prevent.
     */
    public int recipesDropped() {
        return recipesDropped;
    }
}
