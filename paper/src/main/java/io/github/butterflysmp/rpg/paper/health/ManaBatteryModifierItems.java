package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantValues;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

/**
 * Reading the MANA-REGEN bonus a player's worn armor grants through Mana Battery (the seat's B2, 2026-11-27).
 *
 * <p>{@link ManaBankModifierItems} line for line, for a different effect: the same four
 * {@link DefenseModifierItems#ARMOR_SLOTS}, a BROKEN piece skipped (D3'), absent-not-zeroed, so a piece that leaves by
 * any route is absent from the next scan and the reconciler drops its source. A piece merely CARRIED is never read: only
 * the armor slots are walked.
 *
 * <h2>It does NOT reconcile on its own</h2>
 *
 * Mana regen already has sources (the {@code mana_regen_boost_TEMP} fixture, the armor set, accessories, fragments), and
 * {@code ModifierReconciler} removes every source absent from the map it is handed. So this map is MERGED into the
 * first argument of the one mana-regen reconcile, inside {@code ManaTransition.reconcile} (B3), never a call of its own.
 * Its keys carry {@link #SOURCE_PREFIX} so they cannot collide with the fixture's bare-slot keys ({@code manaregen:}).
 */
public final class ManaBatteryModifierItems {

    private ManaBatteryModifierItems() {}

    /** The prefix that keeps these sources disjoint from every other mana-regen source's. */
    static final String SOURCE_PREFIX = "manabattery:";

    /** The mana-regen modifiers (mana per SECOND) the player's worn armor justifies right now, keyed by slot. */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys, EnchantRegistry enchants) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        // D3' (2026-11-25): a broken piece's enchants are off, exactly as Mana Bank's are (the first line of the loop,
        // in the form ArmorDurabilityWiringTest reads off all three armor enchant scanners).
        for (EquipmentSlot slot : DefenseModifierItems.ARMOR_SLOTS) {
            if (io.github.butterflysmp.rpg.paper.weapon.ArmorDurability.isBroken(equipment.getItem(slot))) continue;
            double bonus = EnchantValues.totalFor(EnchantItems.read(equipment.getItem(slot), keys), enchants,
                    EnchantEffect.MANA_REGEN);
            // ManaRegen.boosts is strictly > 0: an absent enchant writes NO source, because a source written every pass
            // would pin the pool every pass and stop mana regenerating at all (ManaTransition's freeze guard).
            if (ManaRegen.boosts(bonus)) {
                desired.put(SOURCE_PREFIX + slot.name(), ManaRegen.contribution(bonus));
            }
        }
        return desired;
    }
}
