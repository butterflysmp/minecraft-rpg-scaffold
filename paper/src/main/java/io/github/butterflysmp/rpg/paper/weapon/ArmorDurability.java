package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefenseSource;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.ArmorWear;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

/**
 * D1' (the seat, 2026-11-25): armor wears and breaks by the SAME code path weapons and shields use. The maximum is
 * read at runtime from the material ({@link WeaponDurability#maxOf}); the wear is the item's own damage, written by
 * OUR call ({@link WeaponDurability#set}); broken is {@link WeaponDurability#isBroken}. Nothing about durability is
 * authored in armor content. The decision of when and by how much is core's {@link ArmorWear}; this class applies it
 * to the four worn slots, as {@code ShieldDurability.applyWearOnBlock} applies a block's wear to the shield.
 *
 * <p>OURS is a piece that resolves through the ONE armor lookup ({@link ArmorDefenseSource#resolve}: the tag, then the
 * material): a minted piece and an untagged one of a content material both wear (A3). A material no content piece
 * covers (the turtle helmet) is left to vanilla, as before.
 */
public final class ArmorDurability {

    private ArmorDurability() {}

    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Is this item one of our armor pieces (tagged, or untagged of a content material), in any slot? */
    public static boolean isOurArmor(ItemStack item, Keys keys, ArmorRegistry armor) {
        if (item == null || item.getType().isAir() || armor == null) return false;
        String token = item.getType().name().toLowerCase(Locale.ROOT);
        return ArmorItems.armorId(item, keys).flatMap(armor::find).or(() -> armor.find(token)).isPresent();
    }

    /** Is the piece in this worn slot ours? The same lookup the Defense scan and the set decision use. */
    static boolean isOurs(ItemStack item, EquipmentSlot slot, Keys keys, ArmorRegistry armor) {
        if (item == null || item.getType().isAir() || armor == null) return false;
        return ArmorDefenseSource.resolve(armor, ArmorItems.armorId(item, keys),
                item.getType().name().toLowerCase(Locale.ROOT), ArmorSlot.valueOf(slot.name())).isPresent();
    }

    /** D3': is this piece broken, by the weapons' own predicate (damage at max - 1)? */
    public static boolean isBroken(ItemStack item) {
        return WeaponDurability.isBroken(item);
    }

    /**
     * D2': one qualifying hit on {@code player}. Each worn, unbroken piece of ours takes {@link ArmorWear#WEAR_PER_HIT},
     * Unbreaking applied; a piece that breaks on it sends the notice. Runs on the player's own thread (the caller hops).
     */
    public static void applyWearOnHit(Player player, Keys keys, ArmorRegistry armor, CooldownTracker cooldowns) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return;
        boolean broke = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack piece = equipment.getItem(slot);
            if (!isOurs(piece, slot, keys, armor)) continue;
            OptionalInt max = WeaponDurability.maxOf(piece);
            if (max.isEmpty()) continue;
            int unbreaking = EnchantItems.activeLevel(piece, keys, Unbreaking.ID);
            OptionalInt worn = ArmorWear.wearOne(WeaponDurability.damageOf(piece), max.getAsInt(), unbreaking,
                    ThreadLocalRandom.current().nextDouble());
            if (worn.isEmpty()) continue;
            WeaponDurability.set(piece, worn.getAsInt());
            equipment.setItem(slot, piece);
            if (isBroken(piece)) broke = true;
        }
        if (broke && cooldowns != null) ArmorBrokenNotice.notify(player, cooldowns);
    }
}
