package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.ManaBank;
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
 * Reading the MAX-MANA bonus a player's worn armor grants through Mana Bank.
 *
 * <p>The sibling of {@link GrowthModifierItems}, scanning the same four
 * {@link DefenseModifierItems#ARMOR_SLOTS} for a different effect. Absent-not-zeroed, like every
 * scanner here: a slot whose piece carries no Mana Bank contributes NO ENTRY, so the reconciler's
 * removal branch does the cleanup when a piece leaves by any route.
 *
 * <h2>It reconciles ALONE, and that is the difference from Growth</h2>
 *
 * Growth's output has to be merged with {@code HealthModifierItems} before reconciling, because both
 * feed max HEALTH and {@code ModifierReconciler} removes every source absent from the map it is
 * handed -- two calls against one target would have each wipe the other's.
 *
 * <p>Max mana has exactly one scanner, so this map goes to
 * {@code reconcileMaxManaModifiers} on its own. <b>The moment a second max-mana source lands -- a
 * fixture item, a second enchant, an archetype bonus -- it must be merged here rather than
 * reconciled separately</b>, for the identical reason. That is why the keys carry a prefix even
 * though nothing collides with them today.
 *
 * <h2>The prefix is for the future, not for a collision that exists</h2>
 *
 * {@code GrowthModifierItems} needed {@code "growth:"} because {@code HealthModifierItems} already
 * walked all slots on bare names and would have erased it. Nothing shares the max-mana target yet,
 * so {@code "manabank:"} prevents no live collision -- it is carried so the first scanner to join
 * this target cannot silently overwrite it, which is a cheaper decision now than a debugging session
 * later.
 */
public final class ManaBankModifierItems {

    private ManaBankModifierItems() {}

    /** The prefix that keeps these sources disjoint from any future max-mana scanner's. */
    static final String SOURCE_PREFIX = "manabank:";

    /** The max-mana modifiers the player's worn armor justifies right now. */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys,
                                                       EnchantRegistry enchants) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        for (EquipmentSlot slot : DefenseModifierItems.ARMOR_SLOTS) {
            double bonus = EnchantValues.totalFor(
                    EnchantItems.read(equipment.getItem(slot), keys), enchants,
                    EnchantEffect.MAX_MANA);
            if (ManaBank.boosts(bonus)) {
                desired.put(SOURCE_PREFIX + slot.name(), ManaBank.contribution(bonus));
            }
        }
        return desired;
    }
}
