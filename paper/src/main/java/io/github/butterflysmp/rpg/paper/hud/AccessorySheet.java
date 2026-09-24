package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.accessory.AccessoryLoreLines;
import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.paper.accessory.Accessories;
import io.github.butterflysmp.rpg.paper.weapon.AccessoryLore;
import io.github.butterflysmp.rpg.paper.weapon.GearLore;
import io.github.butterflysmp.rpg.storage.PlayerAccessories;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The "Accessories" block under the stat lines, on {@code /rpg stats} and on the Nexus hub's stats
 * head -- ruling Q4: accessory stats show on the stats sheet as their own source lines, so a drawback
 * is visible where the total is. The TEXT is {@link AccessoryLoreLines#sheetBlock}; this colours it.
 *
 * <p><b>Unavailable is said, never rendered as nothing.</b> A store that failed to load reads
 * "unavailable" -- an empty block there would be indistinguishable from a player wearing nothing,
 * which is {@code StatsSheetLines.UNTRACKED}'s argument. Still LOADING is the one silent case: it
 * lasts a tick or two after join and the next render has the answer.
 */
public final class AccessorySheet {

    private AccessorySheet() {}

    /** Header plus one line per occupied slot, indented like the stat lines; empty when none. */
    public static List<Component> lines(UUID playerId, Accessories accessories, String profileClass) {
        Optional<PlayerAccessories> stored = accessories.service().accessories(playerId);
        if (stored.isEmpty()) {
            if (!accessories.service().unusable(playerId)) return List.of();
            return List.of(GearLore.plain(AccessoryLoreLines.SHEET_HEADER, NamedTextColor.GOLD),
                    GearLore.plain("  unavailable this session -- see the server log", NamedTextColor.RED));
        }
        List<AccessoryDefinition> worn = accessories.worn(playerId).orElse(null);
        if (worn == null) return List.of();
        List<Boolean> occupied = new ArrayList<>();
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            occupied.add(stored.get().item(slot).isPresent());
        }
        List<String> text = AccessoryLoreLines.sheetBlock(worn, occupied, profileClass,
                AccessorySheet::plainName, AccessoryLore::classLabel);

        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < text.size(); i++) {
            String line = text.get(i);
            if (i == 0) {
                lines.add(GearLore.plain(line, NamedTextColor.GOLD));
            } else {
                NamedTextColor color = line.contains(": inactive") || line.contains("unreadable")
                        ? NamedTextColor.DARK_GRAY : NamedTextColor.GRAY;
                lines.add(GearLore.plain("  " + line, color));
            }
        }
        return lines;
    }

    private static String plainName(AccessoryDefinition accessory) {
        return PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(accessory.displayName()));
    }
}
