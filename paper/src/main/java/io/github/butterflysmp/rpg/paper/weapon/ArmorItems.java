package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

/**
 * Minting an armor piece, and recognising one. An item is one of ours IFF it carries the
 * {@code armor_id} PDC tag -- no tag, not ours, and the system leaves it untouched. The same
 * boundary {@link WeaponItems} draws with {@code weapon_id} and {@link ShieldItems} with
 * {@code shield_id}.
 *
 * <p>This is the only place that reads or writes the {@code armor_id} tag.
 *
 * <h2>What minting does NOT do, and why that is the whole point of this slice</h2>
 *
 * It does not give the piece its Defense. It never did and it must not start.
 * {@code DefenseModifierItems.armorOf} sources Defense from
 * {@code ItemType.getDefaultAttributeModifiers(slot)} -- VANILLA's own armor points for the
 * material -- and that read is blind to anything on the stack. Since a minted piece's authored
 * Defense mirrors exactly those vanilla points, a minted diamond helmet and a plain one contribute
 * the identical 3, through the identical code path, with no change to the Defense source anywhere.
 * That is the claim this slice makes, and the boot gate witnesses it by reproducing the vanilla
 * ladder 3 to 11 to 17 to 20 on minted pieces.
 *
 * <h2>HIDE_ATTRIBUTES, and the one edit that would silently break the armor bar</h2>
 *
 * The flag is DISPLAY ONLY. The piece keeps granting its vanilla armor, which is required:
 * {@code ArmorBarOverride} cancels the native armor sum and refills the bar from damage reduction,
 * and {@code Defense.barModifier}'s input IS that native sum. Hiding a tooltip line does not touch
 * it.
 *
 * <p><b>Never call {@code setAttributeModifiers} to strip the armor instead.</b> {@code armorOf}
 * reads the MATERIAL's defaults, not the stack's, so stripping them leaves it still reporting 20 for
 * a full diamond set while the live attribute is 0. {@code barModifier} would then be off by the
 * whole set, the bar would be visibly wrong, the Defense stat would still look right, and nothing
 * anywhere would fail. It is the quietest way to break this slice.
 *
 * <p>The flag is present here and absent from {@link ShieldItems} for the reason
 * {@code WeaponItems.mint} states: the custom lore block IS the stat display, so vanilla's must be
 * hidden or the tooltip carries two sets of numbers saying different things. A shield has nothing to
 * hide; armor has two lines to hide, and the second of them -- Armor Toughness -- advertises a stat
 * this project does not implement at all.
 *
 * <p><b>Deliberate duplication of {@link ShieldItems} and {@link WeaponItems}.</b> mint / remint /
 * carry-forward repeat their shape. This is the THIRD copy, and the one the project has been waiting
 * for: factoring a shared {@code GearItems} is the immediate follow-up PR, with three call sites to
 * check the abstraction against rather than two. What is NOT duplicated is anything with logic in it
 * -- {@link WeaponItems#displayName}, {@link RarityColors}, {@code EnchantItems}, {@code EnchantLore}
 * and {@link WeaponDurability}'s pure-ITEM helpers are all called directly.
 */
public final class ArmorItems {

    private ArmorItems() {}

    /**
     * Mint a fresh piece from its definition.
     *
     * <p>The Defense number is deliberately NOT written onto the item -- not as a PDC value and not
     * as an attribute modifier. The tooltip renders it from the loaded {@link ArmorDefinition} at
     * mint time, and the STAT comes from vanilla independently. Baking it would create a third place
     * for the truth about a diamond helmet to live, and the two that already exist are one too many
     * -- which is why {@code ArmorConsistency} checks them against each other at boot.
     */
    public static ItemStack mint(ArmorDefinition armor, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack item = new ItemStack(materialOf(armor.material(), armor.slot()));

        item.editMeta(meta -> {
            meta.displayName(WeaponItems.displayName(armor.displayName(), armor.rarity()));
            meta.getPersistentDataContainer().set(keys.armorId, PersistentDataType.STRING, armor.id());
            meta.setMaxStackSize(1);   // a piece of armor is always a single item

            // Hide vanilla's "+8 Armor" and "+2 Armor Toughness" tooltip lines. DISPLAY ONLY -- the
            // modifiers stay, and must, because the armor bar's cancellation is computed from them.
            // See the class javadoc: stripping them instead is the quiet way to break this.
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            applyLore(meta, armor, adapters);
        });
        return item;
    }

    /**
     * Rebuild an existing piece against current content, carrying its instance data forward.
     *
     * <p>Order is load-bearing and identical to the weapon and shield paths: carry the instance data
     * across FIRST, then rebuild lore, so the enchant block is rendered from state that has actually
     * arrived rather than from the empty container mint left behind.
     */
    public static ItemStack remint(ItemStack old, ArmorDefinition current, AdapterContext adapters) {
        ItemStack fresh = mint(current, adapters);
        ItemMeta oldMeta = old.getItemMeta();
        if (oldMeta == null) return fresh;   // no meta means no tag; the caller would not have got here
        fresh.editMeta(meta -> {
            GearItems.carryInstanceData(oldMeta, meta, adapters.keys().armorId,
                    adapters.keys(), fresh.getType());
            applyLore(meta, current, adapters);
        });
        return fresh;
    }

    /**
     * Write this item's whole lore: current content, plus the enchant block THIS ITEM's own state
     * calls for. Rebuilds rather than appends, so no edit path can double the block.
     *
     * The glint is driven by the same {@code state.effective()} list the lore renders, so an
     * enchanted piece shimmers and an unenchanted one does not, and the two cannot disagree.
     */
    private static void applyLore(ItemMeta meta, ArmorDefinition armor, AdapterContext adapters) {
        EnchantState state = EnchantItems.read(meta, adapters.keys());
        // The tooltip shows the EFFECTIVE Defense, composed from the same state the enchant block
        // below is rendered from -- so the "Defense: 17" line and the "Protection III" line beneath
        // it can never disagree, and neither can disagree with the reconcile scan, which composes
        // through the same Protection.effectiveDefense.
        List<Component> base = ArmorLore.build(armor,
                EnchantValues.totalFor(state, adapters.enchants(), EnchantEffect.DEFENSE));
        meta.lore(EnchantLore.applied(base, EnchantLore.lines(state, adapters.enchants())));
        meta.setEnchantmentGlintOverride(!state.effective().isEmpty());
    }

    /**
     * Resolve a piece's material string to a Bukkit Material, falling back to the LEATHER item for
     * its slot.
     *
     * <p>The fallback is per-slot rather than one constant, and that is not tidiness. A typo'd
     * material must still mint something wearable IN THE SLOT THE DEFINITION DECLARES: falling back
     * to a single material would mint a chestplate for a definition that says {@code head}, which
     * then reconciles into the CHEST key and shows a Helmet footer -- wrong in the stat, wrong in
     * the tooltip, and consistent enough to look deliberate. Leather because it is the tier whose
     * points are smallest, so a fallback that escapes notice grants the least.
     */
    private static Material materialOf(String material, ArmorSlot slot) {
        Material resolved = Material.matchMaterial(material);
        if (resolved != null) return resolved;
        return switch (slot) {
            case HEAD -> Material.LEATHER_HELMET;
            case CHEST -> Material.LEATHER_CHESTPLATE;
            case LEGS -> Material.LEATHER_LEGGINGS;
            case FEET -> Material.LEATHER_BOOTS;
        };
    }

    /**
     * The armor id of ANY item, if it is one of ours. The one place the tag is read.
     *
     * Null-guarded like {@code WeaponItems.weaponId}, because {@code Inventory#getContents()} is
     * mostly nulls and this is called against arbitrary slots.
     */
    public static Optional<String> armorId(ItemStack item, Keys keys) {
        return GearItems.idOf(item, keys.armorId);
    }

    /**
     * The armor id of the player's MAIN-HAND item, if it is one of ours.
     *
     * <p>Main hand, not the worn slots, and deliberately: this exists for {@code /rpg enchant},
     * which edits the piece a player is HOLDING. A worn-slot reader is what a future refresher or a
     * set-bonus pass would want, and neither exists yet -- adding one now would be a method with no
     * caller to keep it honest.
     */
    public static Optional<String> heldArmorId(Player player, Keys keys) {
        return armorId(player.getInventory().getItemInMainHand(), keys);
    }

    /**
     * Copy the old piece's INSTANCE data onto the freshly minted one. Everything not copied here is
     * DISPLAY and is rebuilt from current content.
     *
     * The same three things the weapon and shield paths carry, for the same reasons: the id; the
     * accumulated wear, whose loss would be a relog-to-repair exploit; and the enchant blob, whose
     * loss would cost a player an unlock they earned.
     */

    /**
     * Carry the item's enchant state across the re-mint.
     *
     * <p>The RAW STRING moves -- deliberately not decoded and re-encoded, so a blob written by a
     * future build in a grammar this one cannot parse arrives byte for byte. The two keys move
     * INDEPENDENTLY: state without the flag is an item enchanted by hand, the flag without state is
     * a roll that came up empty, and both are legal.
     */

    /**
     * Carry accumulated durability damage across the re-mint, clamped so a material change can never
     * turn a display refresh into a destroyed item.
     */
}
