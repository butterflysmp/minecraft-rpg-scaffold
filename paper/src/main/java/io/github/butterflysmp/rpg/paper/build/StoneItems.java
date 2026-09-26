package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.build.Loadout;
import io.github.butterflysmp.rpg.core.build.LoadoutSlot;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Ability Stone item (PLAN-build-system.md section 2.5): mint, identify, and render its lore.
 *
 * <p><b>IDENTITY IS {@code Keys.buildStone}, NEVER THE MATERIAL.</b> The stone is an {@code ECHO_SHARD}
 * (ruling 12) -- the material of the three universal accessories and of {@code volley_stone} too
 * (section 2.5.2). {@link #isStone} reads only the PDC key, exactly as {@code NexusItems.isNexus} does.
 *
 * <p>It carries NO {@code weapon_id}, so {@code WeaponFire}, the durability code and every gear scanner
 * ignore it by construction; and no {@code accessory_id}, so {@code Accessories} decodes it as nothing.
 */
public final class StoneItems {

    private StoneItems() {}

    /** Ruling 12. */
    static final Material MATERIAL = Material.ECHO_SHARD;

    /**
     * A fresh stone. Stack size 1 at the source (the standing decision: no custom item stacks above 1),
     * and its lore names the loadout it will cast -- or says there is none yet.
     */
    public static ItemStack mint(Keys keys, Optional<Loadout> loadout, AbilityRegistry abilities) {
        ItemStack item = new ItemStack(MATERIAL);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<gradient:#f1fa8c:#ffb86c>Ability Stone</gradient>")
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(keys.buildStone, PersistentDataType.BYTE, (byte) 1);
            meta.setMaxStackSize(1);
            meta.lore(lore(loadout, abilities));
        });
        return item;
    }

    /** Null-safe; presence of the key, not its value -- {@code NexusItems.isNexus}'s rule. */
    public static boolean isStone(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.buildStone, PersistentDataType.BYTE);
    }

    /** Rewrite a stone's lore in place, for a loadout that changed under it. Not a stone: untouched. */
    public static void refreshLore(ItemStack item, Keys keys, Optional<Loadout> loadout, AbilityRegistry abilities) {
        if (!isStone(item, keys)) return;
        item.editMeta(meta -> meta.lore(lore(loadout, abilities)));
    }

    static List<Component> lore(Optional<Loadout> loadout, AbilityRegistry abilities) {
        List<Component> lines = new ArrayList<>();
        if (loadout.isEmpty()) {
            lines.add(line("Choose a class and an element to use it.", NamedTextColor.GRAY));
            return lines;
        }
        lines.add(binding("Left click", loadout.get(), LoadoutSlot.ACTIVE_1, abilities));
        lines.add(binding("Right click", loadout.get(), LoadoutSlot.ACTIVE_2, abilities));
        lines.add(binding("Q", loadout.get(), LoadoutSlot.ULTIMATE, abilities));
        return lines;
    }

    private static Component binding(String input, Loadout loadout, LoadoutSlot slot, AbilityRegistry abilities) {
        String id = loadout.idFor(slot);
        Component name = abilities.find(id)
                .map(def -> MiniMessage.miniMessage().deserialize(def.displayName()))
                .orElse(Component.text(id, NamedTextColor.RED));
        return line(input + ": ", NamedTextColor.GRAY).append(name.decoration(TextDecoration.ITALIC, false));
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
