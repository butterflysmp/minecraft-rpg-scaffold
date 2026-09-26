package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.accessory.AccessoryLoreLines;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.build.FragmentDefinition;
import io.github.butterflysmp.rpg.paper.build.Stones;
import io.github.butterflysmp.rpg.paper.weapon.GearLore;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The stats sheet's "Fragments" block (PLAN-build-system.md section 2.3): the current cell's slotted
 * fragments, under the totals they feed, beside {@link AccessorySheet}'s block.
 *
 * <p>Empty when no fragment is slotted -- the sheet does not list four empty slots. An UNUSABLE build store
 * is said, never rendered as nothing, for {@code AccessorySheet}'s reason.
 */
public final class FragmentSheet {

    private FragmentSheet() {}

    public static final String HEADER = "Fragments";

    public static List<Component> lines(UUID playerId, Stones stones, Optional<PlayerProfile> profile) {
        if (stones.storeUnavailable(playerId)) {
            return List.of(GearLore.plain(HEADER, NamedTextColor.GOLD),
                    GearLore.plain("  unavailable this session -- see the server log", NamedTextColor.RED));
        }
        List<Component> lines = new ArrayList<>();
        for (String id : stones.equippedFragments(playerId, profile)) {
            if (id == null) continue;
            Optional<FragmentDefinition> fragment = stones.fragments().find(id);
            // A BEHAVIOUR fragment (section 7.3) moves no stat, so the stats sheet has nothing to say about it.
            if (fragment.isEmpty() || fragment.get().behavioural()) continue;
            if (lines.isEmpty()) lines.add(GearLore.plain(HEADER, NamedTextColor.GOLD));
            lines.add(GearLore.plain("  " + plainName(fragment.get()) + ": " + modifierText(fragment.get()),
                    NamedTextColor.GRAY));
        }
        return lines;
    }

    /** The accessory block with this block after it: the one list the Nexus stats head takes. */
    public static List<Component> appendedTo(List<Component> accessoryLines, UUID playerId, Stones stones,
                                             Optional<PlayerProfile> profile) {
        List<Component> all = new ArrayList<>(accessoryLines);
        all.addAll(lines(playerId, stones, profile));
        return all;
    }

    /** "+4 Max Health, +0.02 Crit Chance" -- each through the accessories' own modifier formatter. */
    static String modifierText(FragmentDefinition fragment) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<AccessoryStat, Double> m : fragment.modifiers().entrySet()) {
            parts.add(AccessoryLoreLines.modifier(m.getKey(), m.getValue(), null));
        }
        return String.join(", ", parts);
    }

    private static String plainName(FragmentDefinition fragment) {
        return PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(fragment.displayName()));
    }
}
