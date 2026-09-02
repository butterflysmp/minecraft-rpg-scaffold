package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolKind;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

/**
 * Minting a tool item, and recognising one. An item is one of ours IFF it carries the
 * {@code tool_id} PDC tag -- no tag, not a tool, and the system leaves it untouched. The same
 * contract the other three kinds have, with its own key for the reason {@code GearItems} argues:
 * one merged {@code gear_id} would let an item answer to two registries, and the boot-time
 * collision warning is what currently keeps ids unique across them.
 *
 * <h2>IT PINS NOTHING AND HIDES NOTHING, and that is the whole design</h2>
 *
 * {@link WeaponItems#mint} pins an attack-damage modifier and adds {@code HIDE_ATTRIBUTES} because
 * its custom lore block IS the stat display. {@link ArmorItems#mint} adds the flag for the same
 * reason. This does neither, following {@link ShieldItems} -- whose javadoc states the cost of
 * adding the flag anyway: <i>"there is nothing to hide, and adding the flag anyway would suppress
 * the tooltip lines of any vanilla attributes the material carries."</i> A pickaxe's vanilla
 * tooltip is exactly that case.
 *
 * <p>So a minted iron pickaxe mines at iron speed, harvests what iron harvests, takes iron
 * durability, and still shows vanilla's own lines. Nothing in this project ever calls
 * {@code setAttributeModifiers}, and {@code ArmorItems}' javadoc records the measured consequence
 * one kind over: <i>"a minted diamond helmet and a plain one contribute the identical 3."</i>
 *
 * <p><b>The failure this avoids has a name.</b> {@code ShieldDefinition} records the shape: a shield
 * authored onto the wrong material <i>"would mint and render fine and then never block anything,
 * which is the quietest possible way to ship a broken item."</i> A tool that mints, renders a
 * correct footer and then digs like a fist is that failure, one kind over -- and it is why the boot
 * gate mines with one rather than only reading its tooltip.
 */
public final class ToolItems {

    private ToolItems() {}

    /**
     * Mint a fresh tool from its definition.
     *
     * <p>Four statements, like {@link ShieldItems#mint}: name, tag, stack size, lore. There is no
     * fifth because there is no stat to pin -- see the class javadoc.
     *
     * <p>{@code setMaxStackSize(1)} is not inherited from the material. Every vanilla tool already
     * stacks to one, so this is belt-and-braces rather than a correction -- but it is stated for the
     * sibling records' reason: a minted item that could stack would let two different instances'
     * enchant states merge into one, and the enchant container is per-item.
     */
    public static ItemStack mint(ToolDefinition tool, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack item = new ItemStack(materialOf(tool.material(), tool.kind()));

        item.editMeta(meta -> {
            meta.displayName(WeaponItems.displayName(tool.displayName(), tool.rarity()));
            meta.getPersistentDataContainer().set(keys.toolId, PersistentDataType.STRING, tool.id());
            meta.setMaxStackSize(1);
            applyLore(meta, tool, adapters);
        });
        return item;
    }

    /**
     * Write this item's whole lore: current content, plus the enchant block THIS ITEM's own state
     * calls for. Rebuilds rather than appends, so calling it twice produces the same lore and no
     * edit path can double the block.
     *
     * <p>Simpler than its three siblings by exactly one thing: there is no {@code EnchantValues}
     * call, because a tool has no stat for an enchant to compose onto. The shield passes its
     * Bulwark-adjusted DR into its lore so the tooltip and the rider cannot disagree; a tool has no
     * such number, so there is nothing to keep in step.
     *
     * <p>The glint is driven by the same {@code state.effective()} list the lore renders, so an
     * enchanted tool shimmers and an unenchanted one does not, and the two can never disagree.
     */
    private static void applyLore(ItemMeta meta, ToolDefinition tool, AdapterContext adapters) {
        EnchantState state = EnchantItems.read(meta, adapters.keys());
        List<Component> base = ToolLore.build(tool);
        meta.lore(EnchantLore.applied(base, EnchantLore.lines(state, adapters.enchants())));
        meta.setEnchantmentGlintOverride(!state.effective().isEmpty());
    }

    /**
     * Resolve a tool's material string to a Bukkit Material, falling back to the vanilla iron item
     * of its own KIND.
     *
     * <p>Per-kind rather than one constant, because "a tool" names no single vanilla item -- the
     * same problem {@link ArmorItems} has with four slots, and it gets the same answer: the fallback
     * is chosen by the axis value the definition already carries.
     *
     * <p>The fallback exists for {@link ShieldItems#materialOf}'s reason. A material token that
     * passes {@link ToolDefinition}'s kind check but names no real Bukkit item -- {@code
     * mythril_pickaxe} -- would otherwise mint AIR. Falling back to the iron item of the right kind
     * keeps a typo'd material a COSMETIC bug rather than a mechanical one: the player gets a
     * pickaxe, and it mines.
     *
     * <p>Exhaustive over {@link ToolKind} with no default arm, so a new kind must be given a vanilla
     * item to fall back to before the build passes -- the third of the three switches a new constant
     * has to answer.
     */
    private static Material materialOf(String material, ToolKind kind) {
        Material resolved = Material.matchMaterial(material);
        if (resolved != null) return resolved;
        return switch (kind) {
            case PICKAXE -> Material.IRON_PICKAXE;
            case AXE     -> Material.IRON_AXE;
            case SHOVEL  -> Material.IRON_SHOVEL;
            case HOE     -> Material.IRON_HOE;
            // Shears has no tier, so there is no "iron" one to fall back to -- the vanilla item IS
            // the fallback. The first of the untiered tools to arrive, and it will not be the last.
            case SHEARS  -> Material.SHEARS;
        };
    }

    /**
     * The tool id of ANY item, if it is one of ours. The one place the tag is read.
     *
     * <p>Null-guarded like its three siblings, because {@code Inventory#getContents()} is mostly
     * nulls and this is called against arbitrary slots.
     */
    public static Optional<String> toolId(ItemStack item, Keys keys) {
        return GearItems.idOf(item, keys.toolId);
    }

    /** The tool id of the player's main-hand item, if it is one of ours. */
    public static Optional<String> heldToolId(Player player, Keys keys) {
        return toolId(player.getInventory().getItemInMainHand(), keys);
    }

    /**
     * Rebuild an existing tool item's DISPLAY from the definition loaded NOW, keeping everything the
     * item itself earned -- its wear and its enchant state.
     *
     * <p>A full re-mint rather than a lore patch, for the reason {@code WeaponItems.remint} records:
     * material is baked at mint, so a content edit that changes it cannot be applied by rewriting
     * meta on the old stack.
     *
     * <p>Order is load-bearing and identical to the other three: carry the instance data across
     * FIRST, then rebuild lore, so the enchant block is rendered from state that has actually
     * arrived rather than from the empty container mint left behind.
     */
    public static ItemStack remint(ItemStack old, ToolDefinition current, AdapterContext adapters) {
        ItemStack fresh = mint(current, adapters);
        ItemMeta oldMeta = old.getItemMeta();
        if (oldMeta == null) return fresh;   // no meta means no tag; the caller would not have got here
        fresh.editMeta(meta -> {
            GearItems.carryInstanceData(oldMeta, meta, adapters.keys().toolId,
                    adapters.keys(), fresh.getType());
            applyLore(meta, current, adapters);
        });
        return fresh;
    }
}
