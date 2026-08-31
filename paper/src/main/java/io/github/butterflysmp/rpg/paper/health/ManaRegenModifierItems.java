package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Reading the MANA-REGEN bonus a player's equipped items grant, from the
 * {@code mana_regen_boost_TEMP} dev fixture.
 *
 * <p>A FIXTURE, exactly like {@code health_regen_boost_TEMP} and the crit pair, and for the same
 * reason: no content grants mana regen yet, so without a source "gear can modify the rate" would be
 * provable only by unit test and the boot gate would have no row that fails when the reconcile
 * surface is unwired. It comes out when a real mana-regen enchant lands. See NEXT.md, with the other
 * {@code _TEMP} fixtures owing removal.
 *
 * <p>The amount is a BONUS in mana per SECOND, not a resolved rate: the pool's resolver adds
 * {@code ManaRegen.perTick(bonus)} to the per-tick base. {@link #DEFAULT_BOOST} of 1.0/s DOUBLES the
 * base 1.0/s, so a bare 100-mana bar fills in about 50 seconds instead of 100 -- a difference you can
 * watch without a stopwatch.
 *
 * <p>Keyed by EQUIPMENT SLOT and scanning ALL slots, like the crit and health-regen fixtures: a dev
 * fixture you can simply hold is faster to drive than one you must wear. Whatever route the item
 * leaves by (swap, drop, break, death, {@code /clear}) it is absent from the next scan and the
 * reconciler drops its source.
 *
 * <h2>It reconciles alone, for now</h2>
 *
 * Mana regen has exactly one scanner. <b>The moment a second source lands -- the enchant that retires
 * this fixture, an archetype bonus -- it must be MERGED here rather than reconciled separately</b>:
 * {@code ModifierReconciler} removes every source absent from the map it is handed, so two calls
 * against one target would each wipe the other's. That is the trap {@code PlayerHealthSystem} records
 * for max health, where Growth and the health fixture already share a target.
 *
 * <p>The keys carry {@link #SOURCE_PREFIX} for that future, and here it guards something real
 * already: this scanner walks every slot on bare names exactly as the crit and health-regen fixtures
 * do, and a player can hold all of them at once.
 */
public final class ManaRegenModifierItems {

    private ManaRegenModifierItems() {}

    /** The prefix that keeps these sources disjoint from every other all-slot scanner's. */
    static final String SOURCE_PREFIX = "manaregen:";

    /** +1.0 mana/s on a base of 1.0/s -- doubles it, so a bare bar fills in ~50s instead of 100. */
    public static final double DEFAULT_BOOST = 1.0;

    /** Mint a mana_regen_boost_TEMP granting {@code amount} mana/s while held or worn. */
    public static ItemStack mint(Keys keys, double amount) {
        ItemStack item = new ItemStack(Material.LAPIS_LAZULI);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<aqua>Mana Regen <gray>(+" + amount + "/s) <dark_gray>[TEMP]")
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer()
                    .set(keys.manaRegenBoost, PersistentDataType.DOUBLE, amount);
        });
        return item;
    }

    /**
     * The mana-regen modifiers the player's equipped items justify right now, keyed by slot.
     *
     * <p>Absent-not-zeroed: a slot holding nothing of ours contributes NO ENTRY rather than a 0, so
     * the reconciler's removal branch does the cleanup. {@link ManaRegen#boosts} is the gate, so a
     * fixture minted with 0 declares nothing rather than writing a no-op source every scan -- which
     * here would also mean pinning the pool every scan, and that stops mana regenerating entirely.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Double bonus = boostAmount(equipment.getItem(slot), keys.manaRegenBoost);
            if (bonus != null && ManaRegen.boosts(bonus)) {
                desired.put(SOURCE_PREFIX + slot.name(), ManaRegen.contribution(bonus));
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
