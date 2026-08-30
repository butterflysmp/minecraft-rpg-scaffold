package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The armor tooltip: a stat block, optional flavour, and the "&lt;Rarity&gt; &lt;SlotNoun&gt;" footer.
 *
 * The colour half of {@link ArmorLoreLines}, exactly as {@link ShieldLore} is the colour half of
 * {@code ShieldLoreLines}. Every string comes from core; nothing here formats a number.
 *
 * <p><b>Takes the definition and nothing else.</b> No item, no meta, no holder -- the same
 * structural guarantee {@code WeaponLore} and {@code ShieldLore} make. The enchant block is NOT
 * built here; {@code ArmorItems.applyLore} prepends it through {@code EnchantLore.applied}, which is
 * what keeps the rarity footer literally last.
 *
 * <p><b>No enchant-composed overload, unlike {@link ShieldLore}.</b> A shield needs one because
 * Bulwark changes the very number the stat line prints, so the tooltip would otherwise contradict
 * the rider. Nothing in this slice changes a piece's Defense -- Protection is Slice 2 -- so the
 * armor stat line is a pure function of the definition. When Protection lands, this gains the same
 * second overload for the same reason, and it must, or the item will advertise its base while
 * mitigating its boosted value.
 *
 * <p>What it deliberately omits, against the weapon shape it mirrors: no element line (armor deals
 * no damage), no class label ({@code WeaponClassLabel.of} is an exhaustive switch over a closed enum
 * and armor has no {@code WeaponClass}), no ability blocks (wearing armor is not a trigger).
 *
 * <p><b>Deliberate duplication.</b> The blank/plain/titleCase helpers and the footer shape are
 * copied from {@link ShieldLore} rather than shared -- the THIRD copy, and the one the project has
 * been waiting for. Factoring a common gear lore builder is the immediate follow-up PR, with three
 * shapes to check the abstraction against instead of two.
 */
public final class ArmorLore {

    private ArmorLore() {}

    /**
     * The piece's lore, top to bottom: the defense stat, any flavour, then the rarity footer.
     *
     * The leading blank line a weapon's stat block opens with is absent here for the reason
     * {@code ShieldLore} records: a weapon's stat block sits under an element line, and armor has
     * nothing above it to be separated from.
     */
    public static List<Component> build(ArmorDefinition armor) {
        List<Component> lore = new ArrayList<>();

        // The stat block. One line, unconditionally -- including for a piece that declares no
        // defense at all, which then honestly reads "Defense: 0". Hiding the line at zero would
        // make a mis-authored piece look like one with no stat rather than one with a zero stat.
        //
        // GRAY label, GREEN number -- the split WeaponLore's damage line and ShieldLore's reduction
        // line both use. GREEN because that is exactly StatsBarText.DEFENSE_COLOR, read off the HUD
        // rather than picked: this line and the action bar's field report the SAME STAT, so a
        // player glancing between them must not see two colours. (Adventure has no LIME; GREEN is
        // the bright one Minecraft renders as lime, and DARK_GREEN is the darker one.)
        lore.add(plain(ArmorLoreLines.DEFENSE_LABEL, NamedTextColor.GRAY)
                .append(plain(ArmorLoreLines.defenseValue(armor.defense()), NamedTextColor.GREEN)));

        if (!armor.flavor().isEmpty()) {
            lore.add(blank());
            for (String line : armor.flavor()) {
                lore.add(Component.text(line, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, true));
            }
        }

        // Rarity footer at the very bottom, coloured by tier: "Uncommon Helmet".
        //
        // The NOUN is the slot's, not the material's, so a Leather Cap reads "Common Helmet". The
        // footer says what KIND of gear this is -- the same job "Rare Melee Weapon" does -- rather
        // than repeating the item's own name two lines above it.
        lore.add(blank());
        lore.add(plain(titleCase(armor.rarity().name()) + " " + ArmorLoreLines.slotNoun(armor.slot()),
                RarityColors.of(armor.rarity())));

        return lore;
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Component blank() {
        return Component.empty().decoration(TextDecoration.ITALIC, false);
    }

    /** "UNCOMMON" -> "Uncommon". */
    private static String titleCase(String raw) {
        if (raw.isEmpty()) return raw;
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase(Locale.ROOT);
    }
}
