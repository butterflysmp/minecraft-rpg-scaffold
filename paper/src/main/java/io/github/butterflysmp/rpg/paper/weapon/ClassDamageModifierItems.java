package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ClassDamageModifiers;
import io.github.butterflysmp.rpg.core.weapon.ClassDamageModifiers.ClassGrant;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * The class_damage_boost_TEMP dev item, and reading a player's ACTIVE class-damage modifiers off it.
 *
 * A FIXTURE, like {@code health_boost_TEMP} and {@code attack_speed_boost_TEMP}, and for the same
 * reason: the class-damage stat bases at 0 and no real content grants it yet, so without a source
 * the whole feature is invisible at boot and provable only by unit test. It comes out when real
 * content grants class damage (an enchant, a passive, a build aspect). See NEXT.md, with the other
 * _TEMP fixtures owing removal.
 *
 * <p>It is the FIRST fixture needing two PDC values -- an amount AND a class -- so it takes two
 * {@link Keys} entries where the others take one. An item missing either, or naming a class
 * {@link WeaponClass#fromName} does not recognise, is treated as NOT ONE OF OURS and contributes
 * nothing: the untagged-reject-first discipline, extended to a half-tagged item.
 *
 * <p>Two-stage read, and the split is deliberate. Scanning equipment needs a live Bukkit
 * {@code Player}, which no unit test can construct -- which is exactly why neither sibling fixture
 * has a test. So this class does only the Bukkit read, and hands the decision to
 * {@link ClassDamageModifiers#matching}, which is pure, lives in core, and IS tested. Reading the
 * held weapon's class mirrors {@link WeaponAttackItems}, which already takes the registry for the
 * same purpose.
 *
 * <p>Keyed by EQUIPMENT SLOT, like both siblings, so whatever route an item leaves by -- swap, drop,
 * break, death, {@code /clear} -- it is simply absent from the desired map on the next scan and the
 * reconciler drops its source. No departure event to miss. A weapon SWAP needs no special handling
 * either: the held class is re-read every scan, so the same worn gear selects a different grant.
 */
public final class ClassDamageModifierItems {

    private ClassDamageModifierItems() {}

    /** +5, matching the "+5 Melee Damage" framing. Big enough to read off a nameplate at a glance. */
    public static final double DEFAULT_BOOST = 5.0;

    /** Mint a class_damage_boost_TEMP granting {@code amount} damage to {@code weaponClass} weapons. */
    public static ItemStack mint(Keys keys, WeaponClass weaponClass, double amount) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        item.editMeta(meta -> {
            // The label comes from WeaponClassLabel, the same mapping the weapon tooltip's footer
            // uses, so the gear and the weapon it boosts can never disagree about what a class is
            // called ("Ranged", not "RANGER").
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<gold>+" + amount + " " + WeaponClassLabel.of(weaponClass)
                            + " Damage <dark_gray>[TEMP]")
                    .decoration(TextDecoration.ITALIC, false));
            var pdc = meta.getPersistentDataContainer();
            pdc.set(keys.classDamageBoost, PersistentDataType.DOUBLE, amount);
            pdc.set(keys.classDamageBoostClass, PersistentDataType.STRING, weaponClass.name());
        });
        return item;
    }

    /**
     * The class-damage modifiers ACTIVE for this player right now, keyed by slot: their equipped
     * grants, filtered to the ones matching the class of the weapon in their main hand. This is the
     * "desired" set {@code CombatantStats.reconcileClassDamageModifiers} converges to.
     *
     * An empty hand -- or a hand holding something that is not one of our weapons -- yields a null
     * held class, and {@link ClassDamageModifiers#matching} turns that into an empty map. That is
     * what keeps weapon-only melee intact: no weapon, no class, no bonus.
     */
    public static Map<String, Double> desiredModifiers(Player player, Keys keys, WeaponRegistry weapons) {
        WeaponClass held = WeaponItems.heldWeaponId(player, keys)
                .flatMap(weapons::find)
                .map(WeaponDefinition::weaponClass)
                .orElse(null);
        return ClassDamageModifiers.matching(held, equippedGrants(player, keys));
    }

    /** Every class-damage grant the player is currently wearing or holding, keyed by slot. */
    private static Map<String, ClassGrant> equippedGrants(Player player, Keys keys) {
        Map<String, ClassGrant> equipped = new HashMap<>();
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return equipped;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ClassGrant grant = grantOf(equipment.getItem(slot), keys);
            if (grant != null) equipped.put(slot.name(), grant);
        }
        return equipped;
    }

    /**
     * The grant this item makes if it is a class_damage_boost_TEMP, else null. Untagged reject
     * first, then a half-tagged reject: an amount with no class, or a class name no longer in the
     * enum (a SUMMONER item minted by a future build, say), names no class we can gate on, so it
     * grants nothing rather than guessing one.
     */
    private static ClassGrant grantOf(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        Double amount = pdc.get(keys.classDamageBoost, PersistentDataType.DOUBLE);
        if (amount == null) return null;
        WeaponClass weaponClass = WeaponClass.fromName(
                pdc.get(keys.classDamageBoostClass, PersistentDataType.STRING));
        if (weaponClass == null) return null;
        return new ClassGrant(weaponClass, amount);
    }
}
