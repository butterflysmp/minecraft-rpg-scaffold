package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.Growth;
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
 * Reading the MAX-HEALTH bonus a player's worn armor grants through Growth.
 *
 * <h2>Its output MUST be merged with {@link HealthModifierItems}, never reconciled separately</h2>
 *
 * {@code ModifierReconciler.reconcile} removes every applied source absent from the map it is
 * handed. There is exactly ONE {@code reconcileMaxModifiers} call per tick and there must stay
 * exactly one: two calls against the same target -- one for the fixture items, one for Growth --
 * would have each wipe the other's sources, leaving only whichever ran last. The stat would then
 * silently hold half of what the player is wearing, forever, with nothing thrown and no event
 * missing.
 *
 * <h2>The keys are NAMESPACED, and this is the first place in the codebase that needed to be</h2>
 *
 * Every other scanner keys by a bare {@code EquipmentSlot.name()}. {@link HealthModifierItems} walks
 * ALL slots -- including the four armor ones -- so a {@code health_boost_TEMP} in the chest slot and
 * a Growth chestplate would both want the key {@code "CHEST"}. {@code Stat.putModifier} is
 * put-or-REPLACE, so one would silently erase the other and the player would get whichever the merge
 * happened to write second.
 *
 * <p>So Growth's sources are {@code "growth:CHEST"} and friends. The prefix is not decoration: it is
 * what makes the two scanners' key spaces disjoint, and it keeps working when the TEMP fixture is
 * eventually deleted.
 *
 * <p>Absent-not-zeroed, like every sibling: a slot whose piece carries no Growth contributes NO
 * ENTRY, so the reconciler's removal branch does the cleanup when a piece leaves by any route.
 *
 * <p>Reads the same four {@link DefenseModifierItems#ARMOR_SLOTS} the defense scan does -- Growth is
 * gated to armor, so a Growth-tagged item held in the hand grants nothing, which is what the gate
 * already promises.
 */
public final class GrowthModifierItems {

    private GrowthModifierItems() {}

    /** The prefix that keeps these sources out of {@link HealthModifierItems}' bare slot keys. */
    static final String SOURCE_PREFIX = "growth:";

    /**
     * The max-health modifiers the player's worn armor justifies right now.
     *
     * <p>Merge this into the fixture scan's map and reconcile ONCE. See the class javadoc.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys,
                                                       EnchantRegistry enchants) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        for (EquipmentSlot slot : DefenseModifierItems.ARMOR_SLOTS) {
            double bonus = EnchantValues.totalFor(
                    EnchantItems.read(equipment.getItem(slot), keys), enchants,
                    EnchantEffect.MAX_HEALTH);
            if (Growth.boosts(bonus)) {
                desired.put(SOURCE_PREFIX + slot.name(), Growth.contribution(bonus));
            }
        }
        return desired;
    }
}
