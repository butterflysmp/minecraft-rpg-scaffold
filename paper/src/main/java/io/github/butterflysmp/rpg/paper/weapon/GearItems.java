package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * The item plumbing every gear kind mints and re-mints through. One copy of what
 * {@link WeaponItems}, {@link ShieldItems} and {@link ArmorItems} each held privately.
 *
 * <p>Everything here was verified byte-identical across all three before it moved -- {@code md5sum}
 * on the extracted method bodies, not a reading of them. That matters because these are the methods
 * whose divergence would be silent: a carry-forward that dropped the enchant blob on one kind and
 * not the others costs a player an unlock they earned, and nothing throws.
 *
 * <h2>What stays per-kind, and why the abstraction stops here</h2>
 *
 * <ul>
 *   <li><b>{@code mint} itself.</b> A weapon pins an attack-damage modifier and hides it; a shield
 *       pins nothing and hides nothing; armor pins nothing and hides vanilla's armor lines. Three
 *       different answers to "what attributes does this item carry", and the shield's javadoc
 *       explicitly argues AGAINST the flag armor requires.
 *   <li><b>{@code materialOf}.</b> Three different fallbacks, each load-bearing for its own reason:
 *       a weapon falls back to a sword so a give never crashes, a shield to {@code SHIELD} because
 *       anything else would mint fine and then never block, and armor PER SLOT because a single
 *       fallback would put a chestplate in a {@code head} definition.
 *   <li><b>{@code applyLore}.</b> Different lore inputs -- the element registry, the Bulwark
 *       percent, nothing.
 * </ul>
 *
 * <p>So this is the mechanical half: the tag, the wear and the enchant container. The half with a
 * decision in it stays where the decision is documented.
 */
public final class GearItems {

    private GearItems() {}

    /**
     * The gear id on ANY item under {@code key}, if it carries one.
     *
     * <p>Null-guarded because {@code Inventory#getContents()} is mostly nulls and this is called
     * against arbitrary slots. The three tag keys stay SEPARATE rather than becoming one
     * {@code gear_id} plus a kind byte: a single key would let one item answer to two registries
     * during any future migration, and the boot-time collision warning is what currently keeps ids
     * unique ACROSS the three.
     */
    public static Optional<String> idOf(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.STRING));
    }

    /** Copy the gear's own id tag across a re-mint, if the old meta carried one. */
    public static void carryTag(ItemMeta from, ItemMeta to, NamespacedKey key) {
        String id = from.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id != null) {
            to.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        }
    }

    /**
     * Carry the item's enchant state across a re-mint.
     *
     * <p>The RAW STRING moves -- deliberately not decoded and re-encoded, so a blob written by a
     * future build in a grammar this one cannot parse arrives byte for byte. Only readers parse.
     *
     * <p>The two keys move INDEPENDENTLY: state without the flag is an item enchanted by hand, the
     * flag without state is a roll that came up empty, and both are legal. {@code Keys}' own javadoc
     * spells out why this is NOT the two-halves-of-one-value discipline {@code classDamageBoost}
     * uses.
     */
    public static void carryEnchants(ItemMeta from, ItemMeta to, Keys keys) {
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
     * Carry accumulated durability damage across a re-mint, clamped so a material change can never
     * turn a display refresh into a destroyed item.
     *
     * <p>Losing this would be a relog-to-repair exploit, since a re-mint happens on join and on
     * every {@code /rpg enchant} write.
     */
    public static void carryWear(ItemMeta from, ItemMeta to, Material material) {
        if (!(from instanceof Damageable worn) || !(to instanceof Damageable fresh)) return;
        short maxDurability = material.getMaxDurability();
        if (maxDurability <= 0) return;   // not a damageable material -- nothing to carry
        fresh.setDamage(Durability.clamp(worn.getDamage(), maxDurability));
    }

    /**
     * Re-mint any gear against its current definition, dispatched on the definition's own type.
     *
     * <p><b>An exhaustive switch over a sealed interface, with NO default arm -- and that is the
     * whole reason {@link GearDefinition} is sealed.</b> A fourth gear kind stops compiling here
     * until someone says how it re-mints, rather than falling through a catch-all that silently
     * returns the item unchanged. A caller that "just worked" for a new kind by leaving it alone
     * would be the quiet failure: the item keeps stale lore forever and nothing ever says so.
     *
     * <p>This replaces three separate dispatches that had each grown their own copy of the same
     * if-chain -- {@code RpgCommand.HeldGear.remint}, {@code EnchantMenu.PlacedGear.remint} and the
     * refresher -- which is the shape {@code NEXT.md} named when it said two aligned gear records
     * were "designing it from one and a half".
     */
    public static ItemStack remint(ItemStack item, GearDefinition definition, AdapterContext adapters) {
        return switch (definition) {
            case WeaponDefinition weapon -> WeaponItems.remint(item, weapon, adapters);
            case ShieldDefinition shield -> ShieldItems.remint(item, shield, adapters);
            case ArmorDefinition armor -> ArmorItems.remint(item, armor, adapters);
        };
    }

    /**
     * The three things a re-mint carries forward, in the order all three kinds carried them: the id
     * tag, then wear, then the enchant container. Everything NOT copied here is DISPLAY and is
     * rebuilt from current content.
     */
    public static void carryInstanceData(ItemMeta from, ItemMeta to, NamespacedKey idKey,
                                         Keys keys, Material material) {
        carryTag(from, to, idKey);
        carryWear(from, to, material);
        carryEnchants(from, to, keys);
    }
}
