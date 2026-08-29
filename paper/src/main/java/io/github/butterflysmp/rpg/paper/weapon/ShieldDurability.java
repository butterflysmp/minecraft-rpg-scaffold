package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
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
 * <p><b>Vanilla's own shield wear is suppressed alongside this</b> -- {@code RpgListeners} cancels
 * {@code PlayerItemDamageEvent} for any item carrying our {@code shield_id}, so the shield cannot be
 * charged twice for one block.
 *
 * <p><b>Measured 2026-08-29, and weaker than it was first written:</b> across 20 blocks vanilla
 * fired {@code PlayerItemDamageEvent} ZERO times, so on this build there is no double-wear to
 * prevent and that cancel is a guard against something not currently happening. It stays, because
 * we own this item's durability outright and any future vanilla path charging it would be an
 * unaccounted second source -- but do not describe it as fixing an observed doubling.
 *
 * <p>The witness is a POSITIVE count, never a comparison against an un-suppressed run: vanilla's
 * shield wear can scale with the damage blocked rather than being a flat one per block, so "less
 * than 2N" would prove nothing. Observed: 20 blocks with Unbreaking III took the bar 336 -> 331,
 * against 5.00 expected at {@code consumeChance(3) = 0.25}. The rivals are far off -- never ran 0,
 * Unbreaking ignored 20, doubled ~25.
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
     * non-Damageable material leaves with nothing done; an already-worn-out shield leaves too.
     * {@link Durability#wear} floors at one remaining use, so the item can never be destroyed by
     * blocking -- it stops wearing and stays in the hand.
     *
     * <p><b>A broken shield now stops BLOCKING, and that gate is not here.</b> Slice 1 left the
     * question open and named this method as where it would be enforced. It went to
     * {@code ShieldBlock.resolve} instead, because that is where every other reason to grant no
     * mitigation already lives -- untagged, dangling, and now broken all return the one
     * {@code Outcome.NONE}, so base DR, Bulwark and the reflect fall off a single predicate rather
     * than three.
     *
     * <p>A consequence of that placement, and the right one: {@code resolve} runs BEFORE the wear
     * below, so the block that BREAKS the shield still mitigates in full, and only the next one
     * does nothing.
     *
     * <p><b>The already-broken early return below is therefore unreachable through the rider</b> --
     * {@code block.blocked()} is false for a broken shield, so this method is never entered. It
     * stays as defence in depth for any future caller, and no test asserts the broken case THROUGH
     * the rider, because such a test would pass without exercising anything.
     *
     * <p>A broken-notice IS sent now, and Slice 1's reason for omitting it is what changed rather
     * than the reasoning. Then, a spent shield still blocked at full strength, so there was nothing
     * to announce and {@code BrokenNotice}'s weapon wording would have been two lies at once. Now
     * the break has a real consequence, so {@link ShieldBrokenNotice} states it -- in shield words,
     * and on its own throttle key, so a broken sword cannot silence it.
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
    public static void applyWearOnBlock(Player player, EquipmentSlot slot, Keys keys,
                                        CooldownTracker cooldowns) {
        ItemStack shield = player.getInventory().getItem(slot);

        OptionalInt max = WeaponDurability.maxOf(shield);
        if (max.isEmpty()) return;
        int maximum = max.getAsInt();

        if (Durability.isBroken(WeaponDurability.damageOf(shield), maximum)) return;

        int unbreaking = EnchantItems.activeLevel(shield, keys, Unbreaking.ID);
        if (!Unbreaking.consumes(unbreaking, ThreadLocalRandom.current().nextDouble())) return;

        int damage = WeaponDurability.wear(shield, WEAR_PER_BLOCK);

        // Write the stack back explicitly rather than trusting the slot read to be a live mirror,
        // and updateInventory so the bar moves on this block rather than at the client's next sync
        // -- the same pair, for the same reasons, as WeaponDurability and /rpg durability.
        player.getInventory().setItem(slot, shield);
        player.updateInventory();

        // THE CROSSING, not the state: this is reached only on a block that actually wore the
        // shield, and the already-broken return above means the wear that crosses the threshold
        // happens exactly once. Mirrors WeaponDurability.applyWearOnUse's last line.
        if (Durability.isBroken(damage, maximum)) {
            ShieldBrokenNotice.notify(player, cooldowns);
        }
    }
}
