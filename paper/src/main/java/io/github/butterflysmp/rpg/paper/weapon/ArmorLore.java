package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.Protection;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import net.kyori.adventure.text.Component;
import io.github.butterflysmp.rpg.paper.hud.StatsBarText;
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
 * <h2>TWO KINDS OF ENCHANT LINE, one uniform shape</h2>
 *
 * Every stat line reads {@code "Label: value"} -- Defense, Health, and Mana when Slice 2b adds it --
 * so the block reads as one column. What differs is what the value MEANS, and the {@code +} is what
 * says so:
 *
 * <pre>
 *   Defense: 17     Protection EDITED a total the piece already has
 *   Health:  +30    Growth ADDED a stat the piece has none of
 * </pre>
 *
 * <ul>
 *   <li><b>Protection composes into the number.</b> Defense is armor's own stat, so the bonus goes
 *       inside the total -- the same reason {@link ShieldLore} composes Bulwark into its
 *       damage-reduction line rather than listing it separately. A tooltip showing the material's
 *       bare 8 while the piece contributes 17 is a display contradicting truth.
 *   <li><b>Growth cannot.</b> A helmet carries no max health, so there is no total to edit. Its
 *       line is a bonus, and the {@code +} is what stops {@code "Health: 30"} being read as a pool
 *       the piece carries -- which would be the wrong claim for a piece of armor.
 * </ul>
 *
 * <p><b>An earlier draft wrote the bonus backwards</b> -- {@code "+30 Max Health"} -- to mark that
 * difference in the SHAPE. It marks it in the value instead, because two line formats in one stat
 * block reads as an inconsistency rather than as a distinction, and because the shared helper has
 * to serve Mana next.
 *
 * Bonus lines arrive as a LIST of {@link StatBonus}, not as an argument per enchant, so Slice 2b's
 * Mana Bank is one more list entry rather than a fourth overload.
 *
 * <p>What it deliberately omits, against the weapon shape it mirrors: no element line (armor deals
 * no damage), no class label ({@code WeaponClassLabel.of} is an exhaustive switch over a closed enum
 * and armor has no {@code WeaponClass}), no ability blocks (wearing armor is not a trigger).
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
        return build(armor, Protection.NONE);
    }

    /**
     * A flat stat an enchant ADDS to a piece that has none of its own.
     *
     * <p>Growth's {@code "Health: +30"} is the first; Slice 2b's Mana Bank is the second and needs
     * no change here beyond a second entry in the list its caller builds. Carrying label and colour
     * as data rather than branching per enchant is what makes that true -- and is why this record
     * exists at all rather than {@link #build} taking a {@code growthPoints} argument beside
     * {@code protectionPoints}, which is the shape that would have needed widening every slice.
     *
     * @param points the amount, already summed across the piece's active enchants for that stat
     * @param label  the stat's name, from {@code ArmorLoreLines}
     * @param color  the stat's HUD colour, from {@code StatsBarText} -- never picked at the call site
     */
    public record StatBonus(double points, String label, NamedTextColor color) {}

    /**
     * The same tooltip, showing the EFFECTIVE Defense after this piece's own {@code protectionPoints}
     * are added.
     *
     * <p><b>The lore must not disagree with the stat.</b> Once Protection composes onto a piece's
     * Defense in the reconcile scan, a tooltip rendering the material's bare 8 while the piece
     * actually contributes 17 is a display contradicting truth -- the defect this project keeps a
     * whole invariant about. It is also what lets the boot gate read the expected number off the
     * screen BEFORE taking a hit.
     *
     * <p>Composed through {@link Protection#effectiveDefense}, the same function
     * {@code DefenseModifierItems.scan} calls, so the two cannot drift.
     * {@code Protection.NONE} makes the unenchanted case an exact identity -- which is why the
     * one-argument overload above changes nothing and the golden stays green.
     */
    public static List<Component> build(ArmorDefinition armor, double protectionPoints) {
        return build(armor, protectionPoints, List.of());
    }

    /**
     * The full tooltip: the Defense stat with Protection composed in, then a BONUS LINE for every
     * flat stat an enchant has added to a piece that has none of its own.
     *
     * <p><b>Both kinds read "Label: value"; the + is what tells them apart.</b> Protection edits a
     * number the piece already has, so it stays inside the Defense line. Growth adds a stat armor
     * does not otherwise carry -- a helmet has no max health -- so its line is a bonus, and the sign
     * is what stops "Health: 30" being read as a pool the piece carries.
     *
     * <p>The bonuses arrive as a LIST rather than as one argument per enchant, which is what keeps
     * Slice 2b to a content file and one call-site entry: Mana Bank's {@code "Mana: +30"} falls out
     * of the same helper with a different noun and colour. See {@link StatBonus}.
     *
     * <p>Bonus lines sit under the stat block and above the flavour, so the mechanical numbers stay
     * together. An empty list adds nothing at all -- not even a blank -- which is what makes
     * {@link #build(ArmorDefinition, double)} an exact identity and the golden green.
     */
    public static List<Component> build(ArmorDefinition armor, double protectionPoints,
                                        List<StatBonus> bonuses) {
        List<Component> lore = new ArrayList<>();

        // The stat block. One line, unconditionally -- including for a piece that declares no
        // defense at all, which then honestly reads "Defense: 0". Hiding the line at zero would
        // make a mis-authored piece look like one with no stat rather than one with a zero stat.
        //
        // GRAY label, coloured number -- the split WeaponLore's damage line and ShieldLore's
        // reduction line both use. The colour is StatsBarText.DEFENSE_COLOR ITSELF, not a copy of
        // its value: this line and the action bar's field report the SAME STAT, so a player
        // glancing between them must not see two colours, and a compile-time reference is what
        // makes that true rather than merely true today.
        lore.add(GearLore.plain(ArmorLoreLines.DEFENSE_LABEL, NamedTextColor.GRAY)
                .append(GearLore.plain(ArmorLoreLines.defenseValue(
                        Protection.effectiveDefense(armor.defense(), protectionPoints)),
                        StatsBarText.DEFENSE_COLOR)));

        for (StatBonus bonus : bonuses) {
            GearLore.appendFlatBonus(lore, bonus.points(), bonus.label(), bonus.color());
        }

        GearLore.appendFlavor(lore, armor);

        // Rarity footer at the very bottom, coloured by tier: "Uncommon Helmet".
        //
        // The NOUN is the slot's, not the material's, so a Leather Cap reads "Common Helmet". The
        // footer says what KIND of gear this is -- the same job "Rare Melee Weapon" does -- rather
        // than repeating the item's own name two lines above it.
        GearLore.appendRarityFooter(lore, armor.rarity(), ArmorLoreLines.slotNoun(armor.slot()));

        return lore;
    }

}
