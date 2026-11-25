package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefenseSource;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorSetContributions;
import io.github.butterflysmp.rpg.core.weapon.ArmorSetDecision;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.weapon.ArmorItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * B2 and B3 (the seat, 2026-11-25): the four armor slots, read into the pure set decision, and the active set's
 * contributions for the reconcile loop and the summon cap. Every slot resolves through the SAME lookup the Defense
 * scan uses ({@link ArmorDefenseSource#resolve}: the tag, then the material), so a minted piece and an untagged one of
 * the same material count identically (A3).
 *
 * <p><b>No piece is ever broken here yet</b>: armor has no broken state until D1-D3 are ruled (PLAN-armor-sets.md 4;
 * under vanilla wear a piece is destroyed at its maximum, not floored). The decision already ignores the flag, as Ben
 * ruled, so the day a broken state exists only this read changes.
 *
 * <p>Reads the live entity, so it runs on the thread that owns the player: the reconcile loop's entity task, and the
 * cast and command paths, which already run there.
 */
public final class ArmorSetItems {

    private ArmorSetItems() {}

    public static Optional<ArmorSetDecision.ActiveSet> active(Player player, Keys keys, ArmorRegistry armor) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null || armor == null) return Optional.empty();
        Map<ArmorSlot, ArmorSetDecision.Worn> worn = new EnumMap<>(ArmorSlot.class);
        for (ArmorSlot slot : ArmorSlot.values()) {
            ItemStack item = equipment.getItem(EquipmentSlot.valueOf(slot.name()));
            worn.put(slot, new ArmorSetDecision.Worn(ArmorDefenseSource.resolve(armor, ArmorItems.armorId(item, keys),
                    DefenseModifierItems.tokenOf(item), slot), false));
        }
        return ArmorSetDecision.active(worn);
    }

    public static ArmorSetContributions contributions(Player player, Keys keys, ArmorRegistry armor) {
        return ArmorSetContributions.of(active(player, keys, armor));
    }
}
