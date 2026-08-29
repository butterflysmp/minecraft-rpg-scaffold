package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

/**
 * Minting a shield item, and recognising one. An item is one of ours IFF it carries the
 * {@code shield_id} PDC tag -- no tag, not a shield, and the system leaves it untouched. That
 * boundary is the same one {@link WeaponItems} draws with {@code weapon_id}, and it is what lets a
 * plain vanilla shield keep behaving exactly like a plain vanilla shield.
 *
 * <p>This is the only place that reads or writes the {@code shield_id} tag.
 *
 * <p><b>Deliberate duplication of {@link WeaponItems}.</b> mint / remint / carry-forward repeat that
 * class's shape. Factoring a shared {@code GearItems} now would mean designing the abstraction from
 * a single example; when armor lands there will be three call sites to check it against. What is
 * NOT duplicated is anything with real logic in it -- {@link WeaponItems#displayName} is called
 * directly, and the durability helpers on {@link WeaponDurability} are reused because they ask pure
 * ITEM questions, not weapon questions.
 *
 * <p>What a shield does not have, and what is therefore absent here: no attribute modifiers (a
 * shield pins no attack stats), and so no {@code HIDE_ATTRIBUTES} either -- there is nothing to
 * hide, and adding the flag anyway would suppress the tooltip lines of any vanilla attributes the
 * material carries.
 */
public final class ShieldItems {

    private ShieldItems() {}

    /**
     * Mint a fresh shield from its definition.
     *
     * The block DR is deliberately NOT written onto the item -- not as a PDC value, and not as a
     * {@code blocks_attacks} component. The rider reads it from the loaded {@link ShieldDefinition}
     * every time it resolves a block, so a content edit plus {@code /rpg refresh} changes what
     * every existing shield does. Baking it would make the number an instance fact and give every
     * shield in every chest its own private copy of a value the content file is supposed to own.
     */
    public static ItemStack mint(ShieldDefinition shield, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack item = new ItemStack(materialOf(shield.material()));

        item.editMeta(meta -> {
            meta.displayName(WeaponItems.displayName(shield.displayName(), shield.rarity()));
            meta.getPersistentDataContainer().set(keys.shieldId, PersistentDataType.STRING, shield.id());
            meta.setMaxStackSize(1);   // a shield is always a single item
            applyLore(meta, shield, adapters);
        });
        return item;
    }

    /**
     * Write this item's whole lore: current content, plus the enchant block THIS ITEM's own state
     * calls for. Rebuilds rather than appends, for the reasons {@code WeaponItems.applyLore}
     * records -- calling it twice produces the same lore, so no edit path can double the block.
     *
     * The glint is driven by the same {@code state.effective()} list the lore renders, so an
     * enchanted shield shimmers and an unenchanted one does not, and the two can never disagree.
     */
    private static void applyLore(ItemMeta meta, ShieldDefinition shield, AdapterContext adapters) {
        List<Component> base = ShieldLore.build(shield);
        EnchantState state = EnchantItems.read(meta, adapters.keys());
        meta.lore(EnchantLore.applied(base, EnchantLore.lines(state, adapters.enchants())));
        meta.setEnchantmentGlintOverride(!state.effective().isEmpty());
    }

    /**
     * Resolve a shield's material string to a Bukkit Material, falling back to a vanilla shield.
     *
     * The fallback is load-bearing in a way {@code WeaponItems}' sword fallback is not. A weapon
     * that falls back to the wrong material still swings; a shield that falls back to something
     * without vanilla's block behaviour would mint and render perfectly and then never block
     * anything. Falling back to SHIELD keeps a typo'd material a COSMETIC bug rather than a
     * mechanical one.
     */
    private static Material materialOf(String material) {
        Material resolved = Material.matchMaterial(material);
        return resolved != null ? resolved : Material.SHIELD;
    }

    /**
     * The shield id of ANY item, if it is one of ours. The one place the tag is read.
     *
     * Null-guarded like {@code WeaponItems.weaponId}, because {@code Inventory#getContents()} is
     * mostly nulls and this is called against arbitrary slots.
     */
    public static Optional<String> shieldId(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer()
                .get(keys.shieldId, PersistentDataType.STRING));
    }

    /** The shield id of the player's main-hand item, if it is one of ours. */
    public static Optional<String> heldShieldId(Player player, Keys keys) {
        return shieldId(player.getInventory().getItemInMainHand(), keys);
    }

    /**
     * Which hand, if either, is holding one of our shields.
     *
     * <p><b>Both hands, and offhand FIRST.</b> A shield is legal in either hand and vanilla will
     * block with whichever one is raised, so a main-hand-only read would silently refuse to block
     * for a player holding it the unusual way. Offhand wins a tie because that is where a shield
     * actually lives in play -- a player with a shield in each hand is holding a weapon slot they
     * are not using, and the offhand is the one they raised.
     *
     * <p>This answers "is a shield HERE", not "is a shield RAISED". Whether the block was valid is
     * vanilla's call, read off the damage event; see {@code ShieldBlock}.
     */
    public static Optional<EquipmentSlot> shieldHand(LivingEntity entity, Keys keys) {
        if (entity.getEquipment() == null) return Optional.empty();
        if (shieldId(entity.getEquipment().getItemInOffHand(), keys).isPresent()) {
            return Optional.of(EquipmentSlot.OFF_HAND);
        }
        if (shieldId(entity.getEquipment().getItemInMainHand(), keys).isPresent()) {
            return Optional.of(EquipmentSlot.HAND);
        }
        return Optional.empty();
    }

    /**
     * Rebuild an existing shield item's DISPLAY from the definition loaded NOW, keeping everything
     * the item itself earned -- its wear and its enchant state.
     *
     * A full re-mint rather than a lore patch, for the reason {@code WeaponItems.remint} records:
     * material is baked at mint, so a content edit that changes it cannot be applied by rewriting
     * meta on the old stack.
     *
     * <p>Order is load-bearing and identical to the weapon path: carry the instance data across
     * FIRST, then rebuild lore, so the enchant block is rendered from state that has actually
     * arrived rather than from the empty container mint left behind.
     */
    public static ItemStack remint(ItemStack old, ShieldDefinition current, AdapterContext adapters) {
        ItemStack fresh = mint(current, adapters);
        ItemMeta oldMeta = old.getItemMeta();
        if (oldMeta == null) return fresh;   // no meta means no tag; the caller would not have got here
        fresh.editMeta(meta -> {
            carryInstanceData(oldMeta, meta, adapters.keys(), fresh.getType());
            applyLore(meta, current, adapters);
        });
        return fresh;
    }

    /**
     * Copy the old shield's INSTANCE data onto the freshly minted one. Everything not copied here
     * is DISPLAY and is rebuilt from current content.
     *
     * Three things, the same three the weapon path carries and for the same reasons: the id; the
     * accumulated wear, whose loss would be a relog-to-repair exploit; and the enchant blob, whose
     * loss would cost a player an unlock they earned.
     */
    private static void carryInstanceData(ItemMeta from, ItemMeta to, Keys keys, Material material) {
        String id = from.getPersistentDataContainer().get(keys.shieldId, PersistentDataType.STRING);
        if (id != null) {
            to.getPersistentDataContainer().set(keys.shieldId, PersistentDataType.STRING, id);
        }
        carryWear(from, to, material);
        carryEnchants(from, to, keys);
    }

    /**
     * Carry the item's enchant state across the re-mint.
     *
     * <p>The RAW STRING moves -- deliberately not decoded and re-encoded, so a blob written by a
     * future build in a grammar this one cannot parse arrives byte for byte. Only readers parse.
     * The two keys move INDEPENDENTLY: state without the flag is an item enchanted by hand, the
     * flag without state is a roll that came up empty, and both are legal. Verbatim the contract
     * {@code WeaponItems.carryEnchants} documents, because it is the same container.
     */
    private static void carryEnchants(ItemMeta from, ItemMeta to, Keys keys) {
        String data = from.getPersistentDataContainer().get(keys.enchantData, PersistentDataType.STRING);
        if (data != null) {
            to.getPersistentDataContainer().set(keys.enchantData, PersistentDataType.STRING, data);
        }
        Byte rolled = from.getPersistentDataContainer().get(keys.enchantRolled, PersistentDataType.BYTE);
        if (rolled != null) {
            to.getPersistentDataContainer().set(keys.enchantRolled, PersistentDataType.BYTE, rolled);
        }
    }

    /**
     * Carry accumulated durability damage across the re-mint, clamped so a material change can
     * never turn a display refresh into a destroyed item. {@link Durability#clamp} is the same
     * floor the dev commands and the break gate apply.
     */
    private static void carryWear(ItemMeta from, ItemMeta to, Material material) {
        if (!(from instanceof Damageable worn) || !(to instanceof Damageable fresh)) return;
        short maxDurability = material.getMaxDurability();
        if (maxDurability <= 0) return;   // not a damageable material -- nothing to carry
        fresh.setDamage(Durability.clamp(worn.getDamage(), maxDurability));
    }
}
