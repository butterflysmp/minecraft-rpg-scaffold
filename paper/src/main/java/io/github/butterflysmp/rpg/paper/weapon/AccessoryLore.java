package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.accessory.AccessoryLoreLines;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An accessory's tooltip, top to bottom: one line per modifier (bonuses green, drawbacks red, each
 * with its sign), the slot line, any flavour, then the rarity footer.
 *
 * <p>The TEXT is {@link AccessoryLoreLines}' -- this only colours it, the split every lore class in
 * this package keeps. No gear-score line and no enchant block: ruling A4, accessories take part in
 * neither.
 */
public final class AccessoryLore {

    private AccessoryLore() {}

    public static List<Component> build(AccessoryDefinition accessory) {
        List<Component> lore = new ArrayList<>();
        String classLabel = classLabel(accessory);

        List<Map.Entry<AccessoryStat, Double>> ordered = new ArrayList<>(accessory.modifiers().entrySet());
        // Bonuses first, drawbacks last -- the same order AccessoryLoreLines.modifiers gives.
        ordered.sort((a, b) -> Boolean.compare(
                AccessoryLoreLines.isDrawback(a.getValue()), AccessoryLoreLines.isDrawback(b.getValue())));
        for (Map.Entry<AccessoryStat, Double> m : ordered) {
            NamedTextColor color = AccessoryLoreLines.isDrawback(m.getValue())
                    ? NamedTextColor.RED : NamedTextColor.GREEN;
            lore.add(GearLore.plain(AccessoryLoreLines.modifier(m.getKey(), m.getValue(), classLabel), color));
        }

        lore.add(GearLore.plain(AccessoryLoreLines.slotLine(accessory), NamedTextColor.GRAY));

        GearLore.appendFlavor(lore, accessory);
        GearLore.appendRarityFooter(lore, accessory.rarity(), "Accessory");
        return lore;
    }

    /**
     * The class-damage label for a class accessory: "Ranged" for a ranger's. {@link WeaponClassLabel}
     * owns the spelling, so a Quiver's "+3 Ranged Damage" and a weapon's class line cannot disagree.
     * A universal accessory has no class and carries no class damage, so its label is never read.
     */
    public static String classLabel(AccessoryDefinition accessory) {
        return accessory.accessoryClass() == null ? "" : WeaponClassLabel.of(accessory.accessoryClass());
    }
}
