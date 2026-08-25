package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Minting a weapon item, and recognising one. An item is one of ours IFF it carries the
 * weapon_id PDC tag -- no tag, not a weapon, and the system leaves it untouched.
 *
 * This is the only place that reads or writes the weapon_id tag. The read is on the hot
 * path: the swing packet fires for every player on every click in 1b, so heldWeaponId's
 * FIRST act is the cheapest possible reject -- no meta means not ours -- before any
 * lookup or allocation.
 */
public final class WeaponItems {

    private WeaponItems() {}

    /**
     * The vanilla melee a held item deals is a SEPARATE damage path from a weapon's trigger.
     * A left-click swing (1b) would otherwise land both -- the iron sword's vanilla ~6 AND
     * the trigger's content damage -- double-hitting. We cancel the vanilla path so the
     * trigger's number stays authoritative.
     *
     * These two are the single source of truth for that decision, kept server-free so the
     * choice is unit-testable without constructing an ItemStack (which needs a running
     * server). mint() DERIVES the Bukkit Attribute from ATTACK_DAMAGE_ATTRIBUTE below, so
     * the constant the test asserts is the same one that reaches the item -- they cannot
     * drift. The load-bearing property: this touches ONLY vanilla attack damage; the
     * weapon's own damage flows through EffectSpec.Damage -> CombatantHandle.applyDamage,
     * which never goes through an attribute (see WeaponServiceTest, which damages with no
     * item at all).
     */
    public static final String ATTACK_DAMAGE_ATTRIBUTE = "attack_damage";

    /** A player's base attack_damage is 1.0, so -1.0 brings a held swing to a flat 0. */
    public static final double VANILLA_MELEE_SUPPRESSION = -1.0;

    /**
     * The item a weapon is carried in. Its display name is coloured by RARITY, unconditionally
     * (see {@link #displayName}), it carries weapon_id in its PDC -- the whole of its identity
     * -- and it carries the attack-damage suppressor above so the swing's vanilla melee is
     * zeroed. Everything else about the weapon lives in its content file.
     *
     * Takes the whole AdapterContext rather than just Keys because the lore needs the element
     * registry to colour a weapon's element from that element's own content.
     *
     * Phase 1 has no per-weapon material, so every weapon mints as a sword; that becomes a
     * weapon field when a non-melee weapon (the bow) needs a different item.
     */
    public static ItemStack mint(WeaponDefinition weapon, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack item = new ItemStack(materialOf(weapon.material()));
        Attribute attackDamage = Registry.ATTRIBUTE.getOrThrow(
                NamespacedKey.minecraft(ATTACK_DAMAGE_ATTRIBUTE));
        AttributeModifier suppressor = new AttributeModifier(
                keys.meleeSuppressor, VANILLA_MELEE_SUPPRESSION,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
        item.editMeta(meta -> {
            meta.displayName(displayName(weapon.displayName(), weapon.rarity()));
            meta.getPersistentDataContainer().set(keys.weaponId, PersistentDataType.STRING, weapon.id());
            // Setting an explicit attack_damage modifier suppresses the item's vanilla
            // default (+6 for iron), so the swing's melee is base 1.0 + (-1.0) = 0.
            meta.addAttributeModifier(attackDamage, suppressor);
            // Derived stats + authored flavour. Purely additive; the block above is untouched.
            meta.lore(WeaponLore.build(weapon, adapters.elements()));
        });
        return item;
    }

    /**
     * A weapon's item name: the authored text, in its RARITY's colour, always. Rarity owns this
     * colour outright -- an authored colour in the content no longer wins, because "the name is
     * the tier" is only readable if it is true every time. It was previously
     * {@code colorIfAbsent(...)}, under which Hunter's Bow (authored gold, rarity uncommon) and
     * Ember Staff (authored gold, rarity rare) both rendered gold, and the two weapons that DID
     * look right did so by coincidence.
     *
     * Extracted from mint() so it is unit-testable: {@code new ItemStack(...)} needs a running
     * server, but this is pure Adventure. Same reason the suppressor constants above are constants.
     *
     * The plain-text round trip is not a detour: it is what makes this hold for EVERY authored
     * string, not just the easy ones. Where a tag wraps the whole name ("<gold>Hunter's Bow</gold>")
     * MiniMessage compacts the colour onto the root, and a plain {@code .color(rarity)} would in
     * fact override it. But a PARTIAL tag ("Hunter's <gold>Bow</gold>"), a nested tag, or a gradient
     * puts colour on CHILD components that a root-level {@code .color} does not touch -- so that
     * approach would work on today's content and silently fail the first time someone authored a
     * name with a coloured word in it. Measured, not reasoned: reverting this to {@code .color()}
     * leaves the two whole-tag tests green and fails only the partial-tag one, with gold surviving
     * on a child. Flattening to text and rebuilding cannot be defeated that way.
     *
     * The cost, accepted deliberately: this also drops authored bold/underline/gradient. No shipped
     * weapon uses one, and styled names would need their own mechanism regardless.
     */
    public static Component displayName(String authoredName, Rarity rarity) {
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(authoredName));
        return Component.text(plain, RarityColors.of(rarity))
                // Item names render italic by default; a weapon name should read plainly.
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Resolve a weapon's material string ("bow", "iron_sword") to a Bukkit Material. An
     * unknown material falls back to a sword rather than crashing the give -- a wrong item
     * is visible in-game, where it can be fixed, and never blocks a boot.
     */
    private static Material materialOf(String material) {
        Material resolved = Material.matchMaterial(material);
        return resolved != null ? resolved : Material.IRON_SWORD;
    }

    /**
     * The weapon id of the player's main-hand item, if it is one of ours.
     *
     * Delegates to {@link #weaponId} so there is still exactly ONE place that reads the tag. The
     * untagged fast-path reject lives there and is unchanged: an empty hand, a dirt block, a
     * vanilla sword all lack item meta and return empty having cost nothing. This is the shape
     * 1b's packet listener calls, so it must stay allocation-free on a miss.
     */
    public static Optional<String> heldWeaponId(Player player, Keys keys) {
        return weaponId(player.getInventory().getItemInMainHand(), keys);
    }

    /**
     * The weapon id of ANY item, if it is one of ours -- the same read {@link #heldWeaponId} does,
     * widened from the main hand to an arbitrary stack so the Lore Refresher can walk every
     * inventory slot.
     *
     * The null guard is the one addition. {@code getItemInMainHand()} never returns null, but
     * {@code Inventory#getContents()} is full of nulls for empty slots, and an inventory is mostly
     * empty slots. Same shape as the sibling item readers ({@code ClassDamageModifierItems.grantOf},
     * {@code AttackSpeedModifierItems.boostAmount}).
     */
    public static Optional<String> weaponId(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer()
                .get(keys.weaponId, PersistentDataType.STRING));
    }

    /**
     * Rebuild an existing weapon item's DISPLAY from the definition loaded NOW, keeping everything
     * the item itself earned. This is the Lore Refresher's mechanism.
     *
     * A full re-mint rather than a lore patch, on purpose: MATERIAL is baked at mint too, so a
     * weapon that changed from iron_sword to diamond_sword in content cannot be brought up to date
     * by rewriting meta on the old stack -- it is a different item. Minting fresh and carrying the
     * instance data over is the only shape that covers every baked field, which is exactly why the
     * carry-forward has to be explicit rather than incidental.
     */
    public static ItemStack remint(ItemStack old, WeaponDefinition current, AdapterContext adapters) {
        ItemStack fresh = mint(current, adapters);
        ItemMeta oldMeta = old.getItemMeta();
        if (oldMeta == null) return fresh;   // no meta means no tag; the caller would not have got here
        fresh.editMeta(meta -> carryInstanceData(oldMeta, meta, adapters.keys(), fresh.getType()));
        return fresh;
    }

    /**
     * Copy the old item's INSTANCE data onto the freshly minted one -- the explicit half of the
     * refresh contract, and the half that decides whether this pass needs rewriting later.
     *
     * Everything NOT copied here is DISPLAY, and is deliberately rebuilt from current content. This
     * roster is what separates the two, so a future rarity or enchant roll is one more line in this
     * method and no change anywhere else.
     *
     * Today it is two things:
     *
     *  - the weapon id, which mint() already regenerated from the current definition's id -- the
     *    same value, since that is the id we looked the definition up BY. Copied anyway, so the
     *    step is a real mechanism rather than a comment describing one;
     *  - the item's accumulated wear, which mint() cannot know. This is what keeps the refresh
     *    strictly display-only. Resetting it would silently repair every weapon on every login:
     *    a gameplay change smuggled into a presentation pass, and a relog-to-repair exploit.
     *    Whether custom weapons should wear at all is a separate, deferred decision -- if they
     *    later mint unbreakable, this carry-forward quietly becomes a no-op.
     */
    private static void carryInstanceData(ItemMeta from, ItemMeta to, Keys keys, Material material) {
        String id = from.getPersistentDataContainer().get(keys.weaponId, PersistentDataType.STRING);
        if (id != null) {
            to.getPersistentDataContainer().set(keys.weaponId, PersistentDataType.STRING, id);
        }
        carryWear(from, to, material);
    }

    /**
     * Carry accumulated durability damage across the re-mint.
     *
     * The RAW damage value moves, not the wear fraction: if content also changed the material, 50
     * damage out of iron's 250 becomes 50 out of diamond's 1561, so the item reads as less worn
     * than it was. Accepted deliberately -- material changes are rare and the discrepancy is
     * cosmetic.
     *
     * The clamp is NOT cosmetic, and is why this is a method rather than a line. A material change
     * in the other direction -- iron (250) to gold (32) -- would copy a damage value past the new
     * maximum, and an item damaged beyond its maximum is a BROKEN item. Without the clamp, a
     * DISPLAY refresh could destroy a player's weapon outright: the exact class of failure this
     * pass exists to avoid, arriving through the fix rather than the bug.
     *
     * The clamp ITSELF is not here: it is {@link Durability#clamp}, the same floor the durability
     * pass's break gate and its dev commands apply. One definition of "a damage value that never
     * destroys the item", unit-tested in core, rather than a second copy of the arithmetic living
     * in a method no unit test can reach (an ItemStack needs a running server). This method keeps
     * only the item I/O. Worst case is a weapon one use from broken rather than one already gone.
     */
    private static void carryWear(ItemMeta from, ItemMeta to, Material material) {
        if (!(from instanceof Damageable worn) || !(to instanceof Damageable fresh)) return;
        short maxDurability = material.getMaxDurability();
        if (maxDurability <= 0) return;   // not a damageable material -- nothing to carry
        fresh.setDamage(Durability.clamp(worn.getDamage(), maxDurability));
    }
}
