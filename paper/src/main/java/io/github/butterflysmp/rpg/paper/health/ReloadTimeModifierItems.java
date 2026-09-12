package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.ReloadTime;
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
 * Reading the RELOAD-TIME bonus a player's equipped items grant, from the {@code reload_time_boost}
 * instrument.
 *
 * <h2>THE GATE IS {@code declares}, NOT {@code boosts}, AND THAT IS THIS FILE'S WHOLE HAZARD</h2>
 *
 * <p>Every sibling scanner in this package gates its entry on {@code Xxx.boosts(amount)}, which is
 * {@code > NONE}. <b>Writing that here would have made reload-speed gear impossible.</b> Positive
 * ticks mean a SLOWER reload, so an item that helps the player carries a NEGATIVE amount; under
 * {@code > NONE} it would declare no modifier and do nothing, silently, on every scan -- a scanner
 * that runs four times a second and is correct-looking in every line.
 *
 * <p>It was caught in review one commit before this file existed, and the helper was renamed from
 * {@code boosts} to {@code ReloadTime.declares} so the wrong call cannot be written from muscle
 * memory. <b>If you are here to make this file consistent with its neighbours, read
 * {@code ReloadTime.declares}'s javadoc first</b> -- the difference is the sign, not an oversight.
 *
 * <h2>PERMANENT, NOT {@code _TEMP}</h2>
 *
 * <p>Same call as {@code QuiverSizeModifierItems}: A2 ships no quiver enchant, so after this slice
 * this instrument is the only thing that can move reload time, and deleting it would remove the
 * ability to re-gate the stat rather than tidying scaffolding away.
 *
 * <h2>WHY IT IS A SECOND SCANNER AND NOT A SECOND VALUE ON THE FIRST ONE</h2>
 *
 * <p>One item carrying both bonuses has no staging in which one stat moves and the other does not,
 * so no gate row could ever make the negative observation -- and the negative observation is the
 * whole of the crit precedent (<i>one item can raise how often you crit without touching how
 * hard</i>). <b>The pair buys the ability to observe one stat holding still while the other
 * moves</b>, at a price of two {@code Keys}, two dev-command arms and two scanners. The defect one
 * item is blind to is the two stats being ENTANGLED -- a reload scanner wired to the size key by
 * copy-paste, which is exactly what happens when two scanners are written from the same template.
 *
 * <h2>The number, and why the default ADDS when the useful direction SUBTRACTS</h2>
 *
 * <p>{@link #DEFAULT_BOOST} is <b>+14</b> on {@code quiver_stone}'s authored 34, resolving to
 * <b>48</b>; 14, 34 and 48 are pairwise different and none is authored anywhere in {@code content/}.
 *
 * <p><b>A reducing default has no collision-free value at base 34</b> -- exhaustively:
 * {@code -14→20}, {@code -18→16}, {@code -19→15}, {@code -22→12}, {@code -28→6}, {@code -29→5},
 * {@code -31→3}, {@code -33→1} all land on authored numbers, and the only survivor, {@code -17},
 * resolves to <b>17, the bonus itself</b> -- two independent quantities equal, which is the defect
 * that makes a row pass whether the rule exists or not. So the default adds, and a longer reload is
 * the easier thing to time at a boot anyway. The downward direction is driven by hand through
 * {@code /rpg reloadtime <negative>}, which carries that collision caveat at the command.
 *
 * <p>Keyed by EQUIPMENT SLOT and scanning ALL slots, like every scanner here. Whatever route the item
 * leaves by (swap, drop, break, death, {@code /clear}) it is absent from the next scan and the
 * reconciler drops its source.
 *
 * <h2>It reconciles alone, for now</h2>
 *
 * <p>Reload time has exactly one scanner. <b>The moment a second source lands it must be MERGED here
 * rather than reconciled separately</b>: {@code ModifierReconciler} removes every source absent from
 * the map it is handed, so two calls against one target would each wipe the other's.
 */
public final class ReloadTimeModifierItems {

    private ReloadTimeModifierItems() {}

    /** The prefix that keeps these sources disjoint from every other all-slot scanner's. */
    static final String SOURCE_PREFIX = "reloadtime:";

    /** +14 ticks on {@code quiver_stone}'s authored 34, resolving to 48. All three numbers differ. */
    public static final double DEFAULT_BOOST = 14.0;

    /** Mint a reload_time_boost granting {@code amount} ticks while held or worn. Signed. */
    public static ItemStack mint(Keys keys, double amount) {
        ItemStack item = new ItemStack(Material.CLOCK);
        int ticks = ReloadTime.ticks(amount);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<gold>Reload Time <gray>(" + (ticks >= 0 ? "+" : "") + ticks
                            + " ticks" + (ticks > 0 ? ", SLOWER" : ticks < 0 ? ", faster" : "") + ")")
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer()
                    .set(keys.reloadTimeBoost, PersistentDataType.DOUBLE, amount);
        });
        return item;
    }

    /**
     * The reload-time modifiers the player's equipped items justify right now, keyed by slot.
     *
     * <p>Absent-not-zeroed: a slot holding nothing of ours contributes NO ENTRY rather than a 0, so
     * the reconciler's removal branch does the cleanup.
     *
     * <p><b>{@link ReloadTime#declares} is the gate, NOT {@code boosts}</b> -- see the class javadoc.
     * It admits both signs and rejects only 0, so an instrument minted at 0 declares nothing rather
     * than writing a no-op source every scan.
     *
     * <p>The {@code double} is narrowed to {@code int} HERE, at the boundary, so {@code declares} and
     * {@code contribution} never see a fraction.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys) {
        Map<String, Double> desired = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return desired;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Double bonus = boostAmount(equipment.getItem(slot), keys.reloadTimeBoost);
            if (bonus == null) continue;
            int ticks = ReloadTime.ticks(bonus);
            if (ReloadTime.declares(ticks)) {
                desired.put(SOURCE_PREFIX + slot.name(), (double) ReloadTime.contribution(ticks));
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
