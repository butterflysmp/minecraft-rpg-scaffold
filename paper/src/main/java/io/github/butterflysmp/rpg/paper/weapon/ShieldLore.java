package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.Bulwark;
import io.github.butterflysmp.rpg.core.enchant.Riposte;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldLoreLines;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The shield tooltip: a stat block, optional flavour, and the "&lt;Rarity&gt; Shield" footer.
 *
 * The colour half of {@link ShieldLoreLines}, exactly as {@link WeaponLore} is the colour half of
 * {@code WeaponLoreLines}. Every string comes from core; nothing here formats a number.
 *
 * <p><b>Takes the definition and nothing else.</b> No item, no meta, no holder -- the same
 * structural guarantee {@code WeaponLore} makes, and for the same reason: lore that cannot see item
 * state cannot drift from it, and a tooltip that cannot see the holder reads the same for everyone.
 * The enchant block is NOT built here; {@code ShieldItems.applyLore} prepends it through
 * {@code EnchantLore.applied}, which is what keeps the rarity footer literally last.
 *
 * <p>What it deliberately omits, against the weapon shape it mirrors:
 *
 * <ul>
 *   <li><b>No element line.</b> A weapon's element types the damage it deals; a shield deals none,
 *       and {@link ShieldDefinition} carries no element to render.
 *   <li><b>No class label.</b> {@code WeaponClassLabel.of} is an exhaustive switch over a closed
 *       enum with no default arm, and a shield has no {@code WeaponClass} to hand it. The footer
 *       reads "Common Shield" rather than "Common Melee Shield" because there is no third word to
 *       be honest about.
 *   <li><b>No ability blocks.</b> Blocking is not a trigger.
 * </ul>
 *
 * <p><b>Deliberate duplication.</b> The blank/plain/titleCase helpers and the footer shape are
 * copied from {@link WeaponLore} rather than shared. That is the brief for this slice: keep shields
 * focused now, and factor a common gear lore builder when armor lands and there are three shapes to
 * check the abstraction against instead of two.
 */
public final class ShieldLore {

    private ShieldLore() {}

    /**
     * The shield's lore, top to bottom: the block stat, any flavour, then the rarity footer.
     *
     * The leading blank line that a weapon's stat block opens with is absent here on purpose: a
     * weapon's stat block sits UNDER an element line, and a shield has nothing above it to be
     * separated from. Starting the tooltip with an empty line would read as a rendering bug.
     */
    public static List<Component> build(ShieldDefinition shield) {
        return build(shield, Bulwark.NONE, Riposte.NONE);
    }

    /** Without a reflect. Kept so callers predating Riposte read unchanged. */
    public static List<Component> build(ShieldDefinition shield, double bulwarkPercent) {
        return build(shield, bulwarkPercent, Riposte.NONE);
    }

    /**
     * The same tooltip, showing the EFFECTIVE block after {@code bulwarkPercent} is composed on.
     *
     * <p><b>The lore must not disagree with the block.</b> Once Bulwark composes onto block_dr in
     * {@code ShieldBlock.resolve}, a tooltip rendering the shield's own 0.5 while the shield actually
     * stops 0.65 is a display contradicting truth -- the defect this project keeps a whole invariant
     * about. It also matters for the boot gate: {@code EnchantEffectLine} exists so the expected
     * number can be read off the screen BEFORE the hit lands, and this is that number for shields.
     *
     * <p>Composed through {@link Bulwark#effectiveDr}, the same function the rider calls, so the two
     * cannot drift. {@code Bulwark.NONE} makes the unenchanted case an exact identity.
     *
     * <p><b>The reflect line is CONDITIONAL where the block line is not, and that asymmetry is the
     * honest one.</b> Every shield has a block fraction -- {@code block_dr} is a required content
     * field -- so "Block: 0%" on a mis-authored shield is information. No shield has a base reflect:
     * it exists only when Riposte is on the item, so a permanent "Reflect: 0%" would advertise a stat
     * the gear does not have. Without this line Riposte would be the only shield mechanic with no
     * number anywhere on the item, readable only in the enchant menu -- and the boot gate would have
     * nothing to read BEFORE the hit, which is the whole reason the block line renders composed.
     */
    public static List<Component> build(ShieldDefinition shield, double bulwarkPercent,
                                        double reflectPercent) {
        List<Component> lore = new ArrayList<>();

        // The stat block. One line, unconditionally -- including for a shield that declares no
        // block at all, which then honestly reads "Block: 0%". Hiding the line at zero would make
        // a mis-authored shield look like a shield with no stat rather than one with a zero stat,
        // and those want telling apart.
        lore.add(plain(ShieldLoreLines.blockLabel(
                Bulwark.effectiveDr(shield.blockDr(), bulwarkPercent)), NamedTextColor.GRAY));

        // Only when there is one. See the javadoc: a base-less stat rendered at zero is a claim.
        if (Riposte.reflects(reflectPercent)) {
            lore.add(plain(ShieldLoreLines.reflectLabel(reflectPercent), NamedTextColor.GRAY));
        }

        if (!shield.flavor().isEmpty()) {
            lore.add(blank());
            for (String line : shield.flavor()) {
                lore.add(Component.text(line, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, true));
            }
        }

        // Rarity footer at the very bottom, coloured by tier: "Common Shield".
        lore.add(blank());
        lore.add(plain(titleCase(shield.rarity().name()) + " Shield",
                RarityColors.of(shield.rarity())));

        return lore;
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Component blank() {
        return Component.empty().decoration(TextDecoration.ITALIC, false);
    }

    /** "COMMON" -> "Common". */
    private static String titleCase(String raw) {
        if (raw.isEmpty()) return raw;
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase(Locale.ROOT);
    }
}
