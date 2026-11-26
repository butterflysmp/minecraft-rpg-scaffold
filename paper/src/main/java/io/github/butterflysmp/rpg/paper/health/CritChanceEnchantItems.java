package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.enchant.CritChanceEnchant;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantValues;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * K9 (the seat's brief, 2026-11-25): the crit chance the ACTIVE {@code effect: crit_chance} enchants on the weapon in the
 * MAIN HAND grant (Sharpened Glass on the Broken Bottle). One source, {@link #SOURCE}, merged into the ONE crit-chance
 * reconcile beside the held weapon's own ({@code HeldWeaponCrit}); the two keys differ, so the two ADD.
 *
 * <p>Active exactly while the enchant is active on the weapon in hand: {@code EnchantValues.totalFor} sums the item's
 * EFFECTIVE enchants only (a locked or inactive candidate adds nothing), and the loop re-reads the main hand every pass,
 * so a weapon switched away is absent from the next pass's map. Off hand, armour and storage are never read. FLAT.
 */
public final class CritChanceEnchantItems {

    private CritChanceEnchantItems() {}

    /** Distinct from the held weapon's {@code weapon:MAIN_HAND} and from every slot-named fixture key. */
    static final String SOURCE = "critenchant:MAIN_HAND";

    public static Map<String, Double> desiredModifiers(Player player, Keys keys, EnchantRegistry enchants) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (WeaponItems.weaponId(held, keys).isEmpty()) return Map.of();   // only our weapons carry our enchants
        double percent = EnchantValues.totalFor(EnchantItems.read(held, keys), enchants, EnchantEffect.CRIT_CHANCE);
        return CritChanceEnchant.boosts(percent) ? Map.of(SOURCE, CritChanceEnchant.contribution(percent)) : Map.of();
    }
}
