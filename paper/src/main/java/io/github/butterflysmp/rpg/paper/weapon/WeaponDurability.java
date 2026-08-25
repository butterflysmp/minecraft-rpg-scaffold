package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
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
     * What one basic attack costs. Flat, deliberately: a per-weapon {@code wear:} content field is
     * the obvious next step and is deferred to NEXT.md, because shipping it now would mean picking
     * a number for five weapons before a single one has been felt in play.
     */
    public static final int WEAR_PER_USE = 1;

    /**
     * Charge the player's held weapon one use. THE ONE PLACE WEAR IS APPLIED IN PLAY.
     *
     * Both hooks -- the melee swing that connects and the shot that launches -- arrive here rather
     * than each calling {@link #wear}, and that is the entire point of the method existing. WHEN a
     * use happens is core's decision ({@code CastExecutor.execute}: basic attacks only, melee on
     * connect, everything else at commit); WHAT it costs is this, and there is one of it so the
     * enchant below has one seam to plug into instead of two sites to reopen.
     *
     * <p><b>The exemptions, in order.</b> A non-Damageable material leaves with nothing done --
     * ember_staff (blaze_rod) and ability_stone (amethyst_shard) are exactly the two weapons that
     * should never wear, and {@link #maxOf} being empty is what makes that structural rather than a
     * rule each caller remembers. An already-broken weapon leaves too: the Pass 1 gate in
     * {@code WeaponFire} returns Broken before a Success can exist, so this is unreachable in
     * practice, and it is kept because it is what makes the just-broke test below mean "crossed
     * into broken ON THIS USE" rather than the weaker "is broken now".
     *
     * <p>Runs on the thread that owns the player. Both call sites are already there -- see the
     * threading note on {@code CastExecutor}'s two-arg constructor, which is what guarantees it.
     */
    public static void applyWearOnUse(Player player, CooldownTracker cooldowns) {
        ItemStack held = player.getInventory().getItemInMainHand();

        OptionalInt max = maxOf(held);
        if (max.isEmpty()) return;
        int maximum = max.getAsInt();

        if (Durability.isBroken(damageOf(held), maximum)) return;

        // THE UNBREAKING SEAM. A future custom Unbreaking enchant (part of the enchant system, not
        // vanilla's -- player-held items can never carry a vanilla enchant here) rolls HERE and
        // returns without wearing on a skip: roughly a 1/(level+1) chance to consume durability,
        // mirroring vanilla's own curve. It goes BEFORE the wear and AFTER the exemptions, so a
        // staff does not roll for something it can never spend. Building the seam now is what makes
        // that enchant "add the roll" rather than "reopen the two wear sites".

        int damage = wear(held, WEAR_PER_USE);
        // Write the stack back explicitly rather than trusting the main-hand read to be a live
        // mirror, and updateInventory so the bar moves on this swing rather than at the client's
        // next sync -- the same pair, for the same reasons, as RpgCommand's /rpg durability.
        player.getInventory().setItemInMainHand(held);
        player.updateInventory();

        // THE JUST-BROKE SIGNAL. This use is what took the weapon from usable to inert, so say so
        // now instead of leaving the player to discover it on their next dead swing -- a weapon
        // that silently stops working is indistinguishable from a bug, which is the whole argument
        // BrokenNotice was written on. Comparing before against after is what makes this fire once,
        // on the crossing, rather than on every use thereafter; BrokenNotice's 40-tick throttle
        // then dedups it against the gate's own message on the following action.
        if (Durability.isBroken(damage, maximum)) BrokenNotice.notify(player, cooldowns);
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
