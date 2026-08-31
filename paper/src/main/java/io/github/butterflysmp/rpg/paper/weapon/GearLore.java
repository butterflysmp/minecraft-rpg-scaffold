package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Locale;

/**
 * The tooltip pieces every gear kind builds out of. One copy of what {@link WeaponLore},
 * {@link ShieldLore} and {@link ArmorLore} each held privately.
 *
 * <p>What moved here is the parts that were byte-identical across all three -- {@code plain},
 * {@code blank}, {@code titleCase} -- plus the two blocks that were identical except for one noun:
 * the flavour block and the rarity footer.
 *
 * <p><b>What did NOT move, and must not.</b> The stat line. A weapon's is a class-labelled damage
 * figure with an attack-speed line under it and a branch on whether the melee is vanilla-driven; a
 * shield's is one damage-reduction percent composed through Bulwark; armor's is a defense figure in
 * points. Three different shapes over three different units, and the shield's is the only one that
 * is enchant-derived. A "generic stat line" would have to take the label, the value, the colour and
 * a composition rule, which is every part of it -- an abstraction that costs more than it saves and
 * one this interface deliberately stops short of. {@link GearDefinition} has no stat accessor for
 * the same reason.
 *
 * <p>The ITALIC discipline is the reason {@link #plain} exists rather than bare
 * {@code Component.text}: Minecraft italicises custom lore by DEFAULT, so every line has to say
 * {@code ITALIC=false} explicitly, and flavour is the one block that says {@code true}. Getting that
 * wrong ships an entirely italic tooltip and nothing fails.
 */
public final class GearLore {

    private GearLore() {}

    /** A non-italic coloured line. The default state for everything but flavour. */
    public static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** A spacer that is also explicitly non-italic, so it cannot inherit a style. */
    public static Component blank() {
        return Component.empty().decoration(TextDecoration.ITALIC, false);
    }

    /** {@code "COMMON" -> "Common"}. The enum names are SHOUTED; tooltips must not be. */
    public static String titleCase(String raw) {
        if (raw.isEmpty()) return raw;
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * One FLAT-STAT BONUS line: {@code "Health: +30"} -- gray label, coloured value.
     *
     * <p><b>The same {@code "Label: value"} shape every stat line uses</b>, so an armor tooltip
     * reads as one column rather than as two competing formats. A bonus is told apart from a total
     * by the {@code +} on its value, not by being written backwards:
     *
     * <pre>
     *   Defense: 17     what the piece contributes -- a total Protection edited
     *   Health:  +30    what it ADDS to a pool it has none of
     * </pre>
     *
     * That is the whole distinction, and it lives in {@link ArmorLoreLines#bonusValue}. Dropping the
     * sign would make the second read as a total the piece carries, which is the wrong claim.
     *
     * <p><b>The separator is supplied HERE, not by the label constant</b>, so every stat rendered
     * through this method punctuates identically and Defense, Health and Mana cannot drift apart on
     * it. {@code DEFENSE_LABEL} carries its own {@code ": "} only because its caller concatenates it
     * directly.
     *
     * <p><b>Deliberately generic, because the next one is already known.</b> Slice 2b's Mana Bank
     * gets {@code "Mana: +30"} from this same call. Passing the label and the colour in is what
     * makes that a call rather than a copy -- the alternative was a {@code growthLine} that would
     * have needed a {@code manaBankLine} beside it one slice later, and then a third.
     *
     * <p>Emits NOTHING when the value is not positive, so an unenchanted piece grows no line and no
     * blank. That check lives here rather than in each caller for the same reason
     * {@link #appendFlavor}'s does.
     *
     * <p>Colour is the CALLER'S, and it should come from {@code StatsBarText} rather than being
     * picked: these lines report the same stats the action bar does, so a player glancing between an
     * item and their HUD must not see two colours for one number.
     */
    public static void appendFlatBonus(List<Component> lore, double points, String label,
                                       NamedTextColor color) {
        if (points <= 0) return;
        lore.add(plain(label + ": ", NamedTextColor.GRAY)
                .append(plain(ArmorLoreLines.bonusValue(points), color)));
    }

    /**
     * Append the authored flavour block, or nothing at all when there is none.
     *
     * <p>The separator is emitted INSIDE the empty check, which is the whole subtlety: a piece with
     * no flavour must not grow a stray blank line, and all three builders had that conditional
     * written out identically.
     *
     * <p>Gray and italic -- the only italic block in any tooltip.
     */
    public static void appendFlavor(List<Component> lore, GearDefinition gear) {
        if (gear.flavor().isEmpty()) return;
        lore.add(blank());
        for (String line : gear.flavor()) {
            lore.add(Component.text(line, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true));
        }
    }

    /**
     * Append the blank-then-footer that ends every gear tooltip: {@code "<Rarity> <noun>"} in the
     * tier's colour.
     *
     * <p>The {@code noun} is the caller's because the three disagree about what it is and each is
     * right: a weapon's is class-derived ("Melee Weapon"), a shield's is the literal word "Shield",
     * armor's is slot-derived ("Helmet"). Passing it in is what lets the LAST LINE be shared while
     * the word stays each kind's own business.
     *
     * <p>This must stay literally last. {@code EnchantLore.applied} PREPENDS the enchant block, so
     * anything appended after the footer pushes the tier badge into the middle of the tooltip where
     * it reads as a stray line -- which is the headline assertion in all three lore tests.
     */
    public static void appendRarityFooter(List<Component> lore, Rarity rarity, String noun) {
        lore.add(blank());
        lore.add(plain(titleCase(rarity.name()) + " " + noun, RarityColors.of(rarity)));
    }
}
