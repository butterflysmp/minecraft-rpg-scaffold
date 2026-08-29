package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Bukkit half of shield durability: charging a shield one use when it blocks.
 *
 * The block mirror of {@link WeaponDurability#applyWearOnUse}, and it exists for one reason --
 * <b>our Unbreaking is not vanilla's</b>. {@link Unbreaking} is a custom enchant whose curve is
 * written out in core, because the no-vanilla-enchants policy means a player-held item never
 * carries a vanilla enchant to delegate to. Vanilla wearing a shield on a block would never consult
 * it, so Unbreaking would sit on a shield's tooltip doing nothing. Wear has to run through our path
 * or the enchant is a lie.
 *
 * <p><b>Which is why vanilla's own shield wear is SUPPRESSED</b> -- {@code RpgListeners} cancels
 * {@code PlayerItemDamageEvent} for any item carrying our {@code shield_id}. Without that the
 * shield would be charged twice for one block: once by vanilla, once by this. The witness for that
 * is a positive count, not a comparison: with the cancel in place, N blocks move the bar by exactly
 * N. Measuring against an un-suppressed run would prove nothing, because vanilla's shield wear can
 * scale with the damage blocked rather than being a flat one per block.
 *
 * <p>The DECISION is core ({@link Unbreaking#consumes}, {@link Durability#wear}); this only moves
 * values in and out of item meta. Same split as {@link WeaponDurability}, and for the same reason:
 * {@code new ItemStack(...)} throws without a running server, so nothing here can be unit-tested
 * and every line of arithmetic that lived here would be a line no test could reach.
 */
public final class ShieldDurability {

    private ShieldDurability() {}

    /**
     * What one block costs. Flat, and one, which is what vanilla charges for a blocked hit -- so a
     * shield wears at a familiar rate rather than at a rate this slice invented.
     *
     * A per-shield {@code wear:} content field is the obvious next step and is deferred for the
     * reason {@link WeaponDurability#WEAR_PER_USE} gives: shipping it now would mean picking a
     * number for a shield nobody has held yet.
     */
    public static final int WEAR_PER_BLOCK = 1;

    /**
     * Charge the shield in {@code slot} one block. THE ONE PLACE SHIELD WEAR IS APPLIED IN PLAY.
     *
     * <p><b>The exemptions, in order, and the order is the same one the weapon path uses.</b> A
     * non-Damageable material leaves with nothing done. An already-worn-out shield leaves too --
     * and note what that does NOT do: there is no break gate for shields in this slice, so a
     * spent shield keeps blocking at full strength and simply stops wearing. Whether a broken
     * shield should stop blocking is a deferred decision, recorded in NEXT.md; this method is
     * where it would be enforced, and deliberately is not yet. {@link Durability#wear} floors at
     * one remaining use, so the item can never be destroyed by blocking.
     *
     * <p>No broken-notice is sent, unlike the weapon path. {@code BrokenNotice} says "your weapon
     * is broken -- repair it before using it", which would be two lies at once for a shield: it is
     * not a weapon, and nothing stops it being used.
     *
     * <p>THE UNBREAKING SEAM. The level is read off THE BLOCKING STACK's own enchant state, never
     * off a definition, and the DRAW happens here at the impure call site while the DECISION stays
     * in core where it is reddening-testable against exact boundary doubles. Level 0 or absent is
     * threshold 1.0 -- always consume -- so an unenchanted shield wears exactly once per block.
     *
     * <p>Runs on the thread that owns the player: the caller is the {@code EntityDamageByEntityEvent}
     * rider, and the victim is the event's entity. {@code ThreadLocalRandom} rather than
     * {@code Math.random()} for the reason {@link WeaponDurability} records -- many players wear
     * shields at once, and {@code Math.random()} is a synchronized global.
     */
    public static void applyWearOnBlock(Player player, EquipmentSlot slot, Keys keys) {
        ItemStack shield = player.getInventory().getItem(slot);

        OptionalInt max = WeaponDurability.maxOf(shield);
        if (max.isEmpty()) return;
        int maximum = max.getAsInt();

        if (Durability.isBroken(WeaponDurability.damageOf(shield), maximum)) return;

        int unbreaking = EnchantItems.activeLevel(shield, keys, Unbreaking.ID);
        if (!Unbreaking.consumes(unbreaking, ThreadLocalRandom.current().nextDouble())) return;

        WeaponDurability.wear(shield, WEAR_PER_BLOCK);

        // Write the stack back explicitly rather than trusting the slot read to be a live mirror,
        // and updateInventory so the bar moves on this block rather than at the client's next sync
        // -- the same pair, for the same reasons, as WeaponDurability and /rpg durability.
        player.getInventory().setItem(slot, shield);
        player.updateInventory();
    }
}
