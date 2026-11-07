package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.ReloadTime;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantValues;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * DEFT HANDS (PLAN-deft-hands.md): the reload-time contribution of the enchant on the weapon in the MAIN HAND, as a
 * flat tick modifier for {@code HealthState.reloadTimeBonus}. {@link ExpandedQuiverModifierItems}'s shape.
 *
 * <p><b>The percent becomes ticks HERE, ONCE, against the held weapon's AUTHORED {@code reload_ticks}</b>
 * ({@code ReloadTime.percentCut}, Ben's ruling (a)), so it does not compound with the dev instrument's flat ticks:
 * the two ADD in {@link #mergedSources}. A held weapon with no reload contributes nothing.
 *
 * <p><b>THE GATE IS {@code ReloadTime.declares}, {@code != NONE}.</b> Every contribution this produces is NEGATIVE,
 * so {@code QuiverSize.boosts}' {@code > NONE} would declare nothing and the enchant would do nothing, silently --
 * the defect {@code ReloadTime.declares}' javadoc records. {@code DeftHandsModifierItemsTest} pins it, and a
 * mutation to {@code > NONE} reddens it.
 */
public final class DeftHandsModifierItems {

    private DeftHandsModifierItems() {}

    /** Namespaced, like {@code expandedquiver:}, so it never collides with the instrument's bare slot keys. */
    static final String SOURCE_PREFIX = "defthands:";

    /** The main hand's contribution, read off a live player. */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys, WeaponRegistry weapons,
                                                       EnchantRegistry enchants) {
        ItemStack held = player.getInventory().getItemInMainHand();
        int authored = WeaponItems.weaponId(held, keys).flatMap(weapons::find)
                .map(WeaponDefinition::reloadTicks).orElse(0);
        double percent = EnchantValues.totalFor(EnchantItems.read(held, keys), enchants, EnchantEffect.RELOAD_TIME);
        return desired(authored, percent);
    }

    /**
     * THE DECISION, Player-free so it is a unit row: no reload or no enchant, nothing; otherwise the floored cut,
     * declared through {@code ReloadTime.declares}.
     *
     * @param authoredReload the held weapon's {@code reload_ticks}, 0 for a weapon without one
     * @param percent        the summed {@code value_by_level} of the held weapon's active Deft Hands, 0 if none
     */
    static Map<String, Double> desired(int authoredReload, double percent) {
        Map<String, Double> desired = new HashMap<>();
        if (authoredReload <= 0) return desired;
        int cut = ReloadTime.percentCut(authoredReload, (int) Math.floor(percent));
        if (ReloadTime.declares(cut)) {
            desired.put(SOURCE_PREFIX + "HAND", (double) ReloadTime.contribution(cut));
        }
        return desired;
    }

    /**
     * The instrument's sources and the enchant's, in ONE map for ONE reconcile. A named two-argument merge, not an
     * inline {@code putAll}, for the reason {@link ExpandedQuiverModifierItems#mergedSources} gives: reconciling the
     * two separately would have each wipe the other's, and dropping a source here means dropping an argument.
     */
    public static Map<String, Double> mergedSources(Map<String, Double> instrumentSources,
                                                    Map<String, Double> enchantSources) {
        Map<String, Double> merged = new HashMap<>(instrumentSources);
        merged.putAll(enchantSources);
        return merged;
    }
}
