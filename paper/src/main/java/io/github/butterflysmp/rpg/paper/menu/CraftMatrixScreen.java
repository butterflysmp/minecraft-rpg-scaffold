package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.weapon.ArmorItems;
import io.github.butterflysmp.rpg.paper.weapon.ShieldItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import org.bukkit.inventory.ItemStack;

/**
 * The one question asked before any crafting matcher is consulted: does this matrix contain
 * anything of ours?
 *
 * <p><b>A MINTED ITEM IS NOT ITS MATERIAL.</b> This is the load-bearing rule of the whole crafting
 * arc, and it is structural rather than a list of exceptions. Every minted weapon, shield and armor
 * piece is an ordinary vanilla Material carrying a PDC tag. The server's recipe matcher sees only
 * the Material, so a minted item whose base is Blaze Powder, a white dye, a tulip or an ingot will
 * be consumed by a vanilla recipe and turned into a plain output. That loss is silent and
 * unrecoverable, and no amount of care in the menu prevents it -- the matcher never asked us.
 *
 * <p>So: matching is by IDENTITY, and a minted item's identity is its gear id, not its Material.
 *
 * <ul>
 *   <li>A recipe slot asking for a Material matches ONLY an untagged item.
 *   <li>A minted item participates ONLY in a recipe slot that names its gear id.
 *   <li>Therefore a matrix holding ANY tagged item is INVISIBLE to the server's matcher.
 * </ul>
 *
 * <p>Screen the matrix BEFORE consulting the server. In slice 1 our own gear-id-aware table is
 * EMPTY, so {@link MatrixVerdict#CONTAINS_GEAR} simply means "no recipe" -- which is exactly the
 * fail-safe direction. A refusal costs a player nothing; a false match costs them an item.
 *
 * <p><b>All three tag keys are asked, and they stay separate.</b> {@code GearItems}' own javadoc
 * says why a single {@code gear_id} plus a kind byte would be worse. The armor asymmetry does not
 * weaken this: an UNTAGGED vanilla chestplate contributes its full Defense and is not ours, but a
 * MINTED piece carries rarity, lore and an enchant container, and eating one is the same loss as
 * eating a weapon.
 */
public final class CraftMatrixScreen {

    private CraftMatrixScreen() {}

    /**
     * What may be done with a matrix.
     *
     * <p><b>This is the seam the recipe arc grows into.</b> Slice 2 adds a gear-id-aware table, and
     * {@link #CONTAINS_GEAR} stops meaning "no recipe" and starts meaning "ask OUR table". The
     * sealed {@code Ingredient} type -- {@code Material | GearId} -- is created there, alongside its
     * first exhaustive switch, rather than here where it would have one permitted arm and no
     * consumer and so would enforce exhaustiveness on nothing.
     */
    public enum MatrixVerdict {

        /** Nothing of ours is present. The server's own matcher may be asked, and is authoritative. */
        VANILLA_ELIGIBLE,

        /**
         * At least one slot carries a gear tag. The server matcher is NEVER asked.
         *
         * <p>In slice 1 this resolves to "no recipe", full stop.
         */
        CONTAINS_GEAR
    }

    /**
     * Screen a whole matrix.
     *
     * <p>Null-tolerant throughout: a crafting matrix is mostly nulls, and
     * {@code GearItems.idOf} is documented as null-guarded for exactly this reason. A null array is
     * treated as containing nothing, which is the same answer an empty grid gives.
     */
    public static MatrixVerdict verdict(ItemStack[] matrix, Keys keys) {
        return verdict(matrix, item -> isGear(item, keys));
    }

    /**
     * The walk itself, with the tag read injected.
     *
     * <p><b>A seam, for the reason {@code ContentValidator} takes a {@code Predicate}:</b> reading a
     * PDC needs a real {@code ItemStack} and a real {@code Keys}, neither of which exists without a
     * running server, and this project has no MockBukkit. Split this way, the per-item tag read
     * stays boot-witnessed -- it is one delegation to {@code GearItems.idOf} -- while the part that
     * can actually be got wrong becomes a two-second test: does it check EVERY slot, or stop at the
     * first, or look at only the ones that happen to be non-null?
     *
     * <p>That distinction matters here more than most places. A walk that checked only slot 0 would
     * pass every hand-run trial where the tester put the weapon in the first cell, and would eat the
     * item for every player who did not.
     */
    static MatrixVerdict verdict(ItemStack[] matrix, java.util.function.Predicate<ItemStack> isGear) {
        if (matrix == null) return MatrixVerdict.VANILLA_ELIGIBLE;

        for (ItemStack item : matrix) {
            if (isGear.test(item)) return MatrixVerdict.CONTAINS_GEAR;
        }
        return MatrixVerdict.VANILLA_ELIGIBLE;
    }

    /**
     * Is this one item ours?
     *
     * <p>Asks all three keys through the same wrappers {@code EnchantMenu.acceptsInput} uses, so
     * the two places that decide "is this ours" cannot drift into disagreeing.
     */
    public static boolean isGear(ItemStack item, Keys keys) {
        if (item == null) return false;
        return WeaponItems.weaponId(item, keys).isPresent()
                || ShieldItems.shieldId(item, keys).isPresent()
                || ArmorItems.armorId(item, keys).isPresent();
    }
}
