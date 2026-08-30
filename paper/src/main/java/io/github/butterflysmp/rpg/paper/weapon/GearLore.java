package io.github.butterflysmp.rpg.paper.weapon;

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
