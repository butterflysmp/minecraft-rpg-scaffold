package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.QuiverSize;
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
 * Reading the QUIVER-SIZE bonus a player's equipped items grant, from the {@code quiver_size_boost}
 * instrument.
 *
 * <h2>PERMANENT, NOT {@code _TEMP}, AND IT IS THE ONLY THING THAT CAN MOVE THIS STAT</h2>
 *
 * <p>Every sibling scanner reads a {@code *_TEMP} fixture that a real enchant will retire. This one
 * does not. <b>A2 ships no quiver enchant</b> -- out of scope by ruling -- so after this slice this
 * item is the sole source of a quiver-size modifier, and deleting it would remove the ability to
 * re-gate the stat rather than tidying scaffolding away. {@code MEMORY} already tracks two
 * undischarged {@code _TEMP} debts; adding a third that is not actually temporary would be a label
 * that lies. Same call, same reasoning, as {@code quiver_stone}'s carrier in A1.
 *
 * <h2>WHY THIS IS A SEPARATE SCANNER FROM THE RELOAD-TIME ONE, AND WHAT THE PAIR BUYS</h2>
 *
 * <p>The reload-time instrument (commit 5) could have been folded in here as a second PDC value on
 * one item -- it is two more {@code Keys}, another dev-command arm and another scanner to keep them
 * apart, and that is the larger half of why merging is tempting.
 *
 * <p><b>The pair buys the ability to observe ONE STAT HOLDING STILL WHILE THE OTHER MOVES.</b> A
 * single item carrying both bonuses has no staging in which they diverge, so no gate row could ever
 * make the negative observation -- and the negative observation is the whole of the crit precedent
 * (<i>one item can raise how often you crit without touching how hard</i>). The defect one item is
 * blind to is the two stats being ENTANGLED: a size modifier that also moves reload, or a reload
 * scanner wired to the size key by copy-paste, which is exactly what happens when two scanners are
 * written minutes apart from the same template. The core half of that mutation is
 * {@code MUTQSALIAS}; this is what makes the item-level half stageable.
 *
 * <h2>The number</h2>
 *
 * <p>{@link #DEFAULT_BOOST} is <b>+19</b> on {@code quiver_stone}'s authored 9, resolving to
 * <b>28</b>. Both were swept against every authored value in {@code content/} and both are free --
 * <b>and the swept quantity is the RESOLVED one</b>, because a gate row reads what is on screen, not
 * what the modifier says. 9, 19 and 28 are pairwise different, so no reading can be confused for
 * another.
 *
 * <p>Keyed by EQUIPMENT SLOT and scanning ALL slots, like the crit, health-regen and mana-regen
 * fixtures: an instrument you can simply hold is faster to drive than one you must wear. Whatever
 * route the item leaves by (swap, drop, break, death, {@code /clear}) it is absent from the next scan
 * and the reconciler drops its source.
 *
 * <h2>It reconciles alone, for now</h2>
 *
 * <p>Quiver size has exactly one scanner. <b>The moment a second source lands it must be MERGED here
 * rather than reconciled separately</b>: {@code ModifierReconciler} removes every source absent from
 * the map it is handed, so two calls against one target would each wipe the other's. That is the trap
 * {@code PlayerHealthSystem} records for max health, where Growth and the health fixture already
 * share a target. The keys carry {@link #SOURCE_PREFIX} for that future, and it guards something real
 * already: this scanner walks every slot on bare names, and a player can hold several at once.
 *
 * <h2>AND THIS IS WHERE {@code QuiverSize.boosts} ACQUIRES ITS ONLY PRODUCTION CALLER</h2>
 *
 * <p>Worth stating because the previous commit argued from that filter while it had none -- its only
 * callers were test rows, which is not a filter being applied. It is applied here, and what it makes
 * true is narrow: <b>the CONTENT PIPELINE is increase-only.</b> The arithmetic is public core API and
 * is not, which is why {@code QuiverSize.resolve} floors at {@code MIN_CAPACITY} independently.
 */
public final class QuiverSizeModifierItems {

    private QuiverSizeModifierItems() {}

    /** The prefix that keeps these sources disjoint from every other all-slot scanner's. */
    static final String SOURCE_PREFIX = "quiversize:";

    /** +19 arrows on {@code quiver_stone}'s authored 9, resolving to 28. All three numbers differ. */
    public static final double DEFAULT_BOOST = 19.0;

    /** Mint a quiver_size_boost granting {@code amount} arrows while held or worn. */
    public static ItemStack mint(Keys keys, double amount) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<green>Quiver Size <gray>(+" + (int) amount + " arrows)")
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer()
                    .set(keys.quiverSizeBoost, PersistentDataType.DOUBLE, amount);
        });
        return item;
    }

    /**
     * The quiver-size modifiers the player's equipped items justify right now, keyed by slot.
     *
     * <p>Absent-not-zeroed: a slot holding nothing of ours contributes NO ENTRY rather than a 0, so
     * the reconciler's removal branch does the cleanup. {@link QuiverSize#boosts} is the gate, so an
     * instrument minted with 0 declares nothing rather than writing a no-op source every scan.
     *
     * <p><b>The bonus is narrowed to {@code int} HERE</b>, at the boundary between a PDC {@code
     * double} and a stat that means whole arrows -- so {@code boosts} and {@code contribution} never
     * see a fraction, and a hand-crafted {@code 2.5} is floored once, at the edge, by the same rule
     * {@code QuiverSize.arrows} applies everywhere else.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Double bonus = boostAmount(equipment.getItem(slot), keys.quiverSizeBoost);
            if (bonus == null) continue;
            int arrows = QuiverSize.arrows(bonus);
            if (QuiverSize.boosts(arrows)) {
                desired.put(SOURCE_PREFIX + slot.name(), (double) QuiverSize.contribution(arrows));
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
