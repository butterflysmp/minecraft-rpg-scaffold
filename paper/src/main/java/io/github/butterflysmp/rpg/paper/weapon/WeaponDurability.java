package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.OptionalInt;

/**
 * The Bukkit half of durability: reading a stack's damage and writing it back. Every DECISION is
 * {@link Durability}, in core, where it is unit-tested; this class only moves values in and out of
 * item meta.
 *
 * Same split as {@link WeaponItems} and for the same reason: {@code new ItemStack(...)} throws
 * without a running server and there is no MockBukkit, so nothing here can be unit-tested and it is
 * boot-witnessed instead. That is the argument for keeping it this thin -- every line of arithmetic
 * that lives here is a line no test can reach.
 *
 * <p><b>Vanilla durability, not a custom stat.</b> The maximum is the material's own
 * ({@code iron_sword} 250, {@code bow} 384) and the damage value is the one the item already
 * carries, so the durability bar renders for free. A material with no durability
 * ({@code blaze_rod}, {@code amethyst_shard} -- the ember_staff and the ability_stone) is exempt by
 * construction: {@link #maxOf} is empty, and core's guards make every operation a no-op regardless.
 */
public final class WeaponDurability {

    private WeaponDurability() {}

    /**
     * The material's maximum durability, or empty when the material has none.
     *
     * Empty is the "this weapon has no durability" answer the dev command reports and the gates
     * treat as never-broken. It is deliberately a property of the MATERIAL, not of the stack: a
     * blaze_rod has no durability whatever meta it carries.
     */
    public static OptionalInt maxOf(ItemStack item) {
        if (item == null) return OptionalInt.empty();
        short max = item.getType().getMaxDurability();
        return max <= 0 ? OptionalInt.empty() : OptionalInt.of(max);
    }

    /** The stack's current durability damage; 0 for a stack that cannot carry any. */
    public static int damageOf(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof Damageable damageable)) return 0;
        return damageable.getDamage();
    }

    /**
     * Is this stack worn to its floor and therefore inert?
     *
     * A pure item question -- it does NOT ask whether the item is one of ours. Callers that have
     * already resolved a {@code weapon_id} use this; the one caller that has not
     * ({@code RpgListeners.onPlayerMeleeAttack}) uses {@link #isHeldWeaponBroken} instead.
     */
    public static boolean isBroken(ItemStack item) {
        OptionalInt max = maxOf(item);
        return max.isPresent() && Durability.isBroken(damageOf(item), max.getAsInt());
    }

    /**
     * Is the player's main hand holding one of OUR weapons, broken?
     *
     * The scope gate in one place: an untagged vanilla sword answers false however worn it is, so
     * ordinary items keep breaking normally and never see a gate or a message. Broadening the
     * no-break promise to untagged tools is a future pass, and this is the line it would change.
     */
    public static boolean isHeldWeaponBroken(Player player, Keys keys) {
        ItemStack held = player.getInventory().getItemInMainHand();
        return WeaponItems.weaponId(held, keys).isPresent() && isBroken(held);
    }

    /**
     * Wear the stack by {@code amount} and return its resulting damage.
     *
     * Floors at one use via {@link Durability#wear}, so this can never destroy the item -- which is
     * the whole promise. A stack with no durability is left untouched and reports 0.
     */
    public static int wear(ItemStack item, int amount) {
        return write(item, max -> Durability.wear(damageOf(item), amount, max));
    }

    /** Repair the stack by {@code amount} and return its resulting damage; 0 is fully repaired. */
    public static int repair(ItemStack item, int amount) {
        return write(item, max -> Durability.repair(damageOf(item), amount));
    }

    /** Set the stack's damage outright, clamped so it can never be a destroyed or negative value. */
    public static int set(ItemStack item, int damage) {
        return write(item, max -> Durability.clamp(damage, max));
    }

    /**
     * Resolve the maximum, compute the new damage from it, and write it back.
     *
     * The one place item meta is mutated, so the "not damageable -- do nothing" branch cannot be
     * forgotten by one of the three operations above.
     */
    private static int write(ItemStack item, java.util.function.IntUnaryOperator newDamage) {
        OptionalInt max = maxOf(item);
        if (max.isEmpty()) return 0;
        int damage = newDamage.applyAsInt(max.getAsInt());
        item.editMeta(meta -> {
            if (meta instanceof Damageable damageable) damageable.setDamage(damage);
        });
        return damage;
    }
}
