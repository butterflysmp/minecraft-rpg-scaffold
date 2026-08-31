package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Reading the HEALTH-REGEN bonus a player's equipped items grant, from the
 * {@code health_regen_boost_TEMP} dev fixture.
 *
 * <p>A FIXTURE, exactly like {@code crit_chance_boost_TEMP} and {@code health_boost_TEMP}, and for
 * the same reason: the regen rate bases at 0.2 HP/s and no content grants a bonus yet, so without a
 * source "gear can modify it" would be provable only by unit test. This makes it something you can
 * hold and feel, and gives the boot gate a row that fails if the reconcile surface is not wired. It
 * comes out when a Health Regen enchant lands. See NEXT.md, with the other _TEMP fixtures owing
 * removal.
 *
 * <p>The amount is a BONUS in HP per second, not a resolved rate: the stat resolves
 * {@code 0.2 + Sum(modifiers)}, so {@link #DEFAULT_BOOST} of 0.8 yields a resolved 1.0 HP/s -- five
 * times base, which is the point. A rate you have to time with a stopwatch proves nothing on a boot
 * gate; 1 HP a second is countable at a glance.
 *
 * <p>Keyed by EQUIPMENT SLOT and scanning ALL slots, like the crit and attack-speed fixtures rather
 * than like the armor-only enchant scanners -- a dev fixture you can simply hold is faster to drive
 * than one you must wear. Whatever route the item leaves by (swap, drop, break, death, {@code /clear})
 * it is simply absent from the next scan and the reconciler drops its source. No departure event to
 * miss.
 *
 * <h2>It reconciles ALONE, for now</h2>
 *
 * Health regen has exactly one scanner, so this map goes to
 * {@code CombatantStats.reconcileHealthRegenModifiers} on its own. <b>The moment a second source
 * lands -- the Health Regen enchant that retires this fixture, an archetype bonus -- it must be
 * MERGED here rather than reconciled separately</b>: {@code ModifierReconciler} removes every source
 * absent from the map it is handed, so two calls against one target would each wipe the other's and
 * the stat would hold whichever ran last, silently and forever. That is the trap
 * {@code PlayerHealthSystem} records for max health, where Growth and the health fixture already
 * share a target.
 *
 * <p>The keys carry {@link #SOURCE_PREFIX} for that future, not for a collision that exists today --
 * the same forward-looking choice {@code ManaBankModifierItems} made, and cheaper now than a
 * debugging session later.
 */
public final class HealthRegenModifierItems {

    private HealthRegenModifierItems() {}

    /** The prefix that keeps these sources disjoint from any future health-regen scanner's. */
    static final String SOURCE_PREFIX = "regen:";

    /** +0.8 HP/s on a base of 0.2 -> a resolved 1.0 HP/s: five times base, countable in a short boot. */
    public static final double DEFAULT_BOOST = 0.8;

    /**
     * The health-regen modifiers the player's equipped items justify right now, keyed by slot. The
     * "desired" set {@code CombatantStats.reconcileHealthRegenModifiers} converges to.
     *
     * <p>Absent-not-zeroed: a slot holding nothing of ours contributes NO ENTRY rather than a 0, so
     * the reconciler's removal branch does the cleanup. And {@link HealthRegen#boosts} is the gate,
     * so a fixture minted with 0 declares nothing rather than writing a no-op source every scan.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Double bonus = boostAmount(equipment.getItem(slot), keys.healthRegenBoost);
            if (bonus != null && HealthRegen.boosts(bonus)) {
                desired.put(SOURCE_PREFIX + slot.name(), HealthRegen.contribution(bonus));
            }
        }
        return desired;
    }

    /** The bonus this item grants under {@code key}, else null. Untagged rejects first. */
    private static Double boostAmount(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
    }
}
