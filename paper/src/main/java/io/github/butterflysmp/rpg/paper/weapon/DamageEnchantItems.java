package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveEnchant;
import io.github.butterflysmp.rpg.core.enchant.DamageEnchants;
import io.github.butterflysmp.rpg.core.enchant.DamageEnchants.Grant;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Reading a player's ACTIVE damage-enchant percentages off the weapon in their main hand.
 *
 * <p>NOT a fixture, unlike {@code ClassDamageModifierItems} beside it. The source here is real
 * content on a real item -- Sharpness on a sword, Power on a bow, Attunement on a staff -- so
 * nothing in this file owes removal with the {@code _TEMP} items. What is still a stand-in is how
 * an enchant GETS onto the item ({@code /rpg enchant}, until the roll exists), not this read.
 *
 * <p><b>MAIN HAND ONLY, and that is the whole difference from its sibling.</b>
 * {@code ClassDamageModifierItems} scans every equipment slot for grants and then asks whether each
 * matches the held weapon's class. A damage enchant is not worn somewhere else and pointed at your
 * weapon -- it IS on the weapon, so there is exactly one item to read and the gate compares the
 * enchant's class against the class of the very item carrying it. Sharpness on a bow does nothing
 * because the bow is a RANGER weapon, not because some other slot failed to match.
 *
 * <p>Two-stage read, the same split as every sibling: this class does only the Bukkit part, which
 * needs a live {@code Player} no unit test can construct, and hands the decision to
 * {@link DamageEnchants#matching}, which is pure, lives in core, and IS tested.
 *
 * <p>Keyed by ENCHANT ID rather than by equipment slot -- the sibling's slot keys exist because a
 * grant can come from any of several slots, and there is only one slot here. The id is what lets
 * {@code Stat} sum two damage enchants on one weapon, and it is what the reconciler drops when an
 * enchant is deactivated, swapped away from, or the whole weapon is sheathed.
 *
 * <p><b>The registry IS consulted here, unlike at the durability seam.</b> {@code EnchantItems
 * .activeLevel} deliberately compares an id and never looks an enchant up, so deleting
 * {@code unbreaking.yml} leaves Unbreaking working. That cannot hold for a damage enchant: its class
 * gate and its curve live in the definition, so an id with no definition has no percent to grant and
 * resolves to nothing. The asymmetry is a consequence of where each mechanism keeps its numbers, and
 * it means a dangling damage enchant renders on the tooltip (EnchantLore's fail-soft) while granting
 * 0 -- visible, and the safe direction.
 */
public final class DamageEnchantItems {

    private DamageEnchantItems() {}

    /**
     * The enchant-damage percentages ACTIVE for this player right now, keyed by enchant id: the
     * damage enchants active on their held weapon whose class matches that weapon's own. This is the
     * "desired" set {@code CombatantStats.reconcileEnchantDamageModifiers} converges to.
     *
     * <p>An empty hand, or a hand holding something that is not one of our weapons, yields an empty
     * map -- there is no item to read enchants off, so there is nothing to grant. A weapon SWAP
     * needs no event of its own: this is re-read every reconcile scan, so the previous weapon's
     * enchants are simply absent from the next desired set and the reconciler drops them.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys,
                                                       WeaponRegistry weapons,
                                                       EnchantRegistry enchants) {
        ItemStack held = player.getInventory().getItemInMainHand();

        // Untagged reject first, before any enchant decode -- the same cheapest-possible-reject
        // discipline heldWeaponId keeps on the swing path. Nothing of ours in hand means no class to
        // gate on and no enchants to read.
        WeaponClass heldClass = WeaponItems.weaponId(held, keys)
                .flatMap(weapons::find)
                .map(WeaponDefinition::weaponClass)
                .orElse(null);
        if (heldClass == null) return new HashMap<>();

        return DamageEnchants.matching(heldClass, damageGrantsOn(held, keys, enchants));
    }

    /**
     * The damage-enchant grants ACTIVE on this item, keyed by enchant id.
     *
     * <p>Reads {@code EnchantState.effective()}, so the level here is literally the level the
     * TOOLTIP rendered -- the shared-origin rule {@link ActiveEnchant} exists to enforce. An item
     * whose tooltip promises Sharpness III cannot be granting II.
     *
     * <p>A durability enchant in the list is skipped rather than treated as a 0% damage enchant:
     * absent from the map means the reconciler holds no source for it at all, which is what keeps
     * Unbreaking from appearing as a dead modifier on every enchanted weapon.
     */
    private static Map<String, Grant> damageGrantsOn(ItemStack item, Keys keys,
                                                     EnchantRegistry enchants) {
        Map<String, Grant> grants = new HashMap<>();
        for (ActiveEnchant active : EnchantItems.read(item, keys).effective()) {
            EnchantDefinition definition = enchants.find(active.enchantId()).orElse(null);
            // A dangling id has no class and no curve, so it cannot grant a percent. It still
            // RENDERS on the tooltip -- EnchantLore's deliberate fail-soft -- so the mismatch is
            // visible rather than silent, and it fails toward granting nothing.
            if (definition == null || definition.effect() != EnchantEffect.DAMAGE) continue;
            grants.put(active.enchantId(),
                    new Grant(definition.weaponClass(), definition.percentByLevel(), active.level()));
        }
        return grants;
    }
}
