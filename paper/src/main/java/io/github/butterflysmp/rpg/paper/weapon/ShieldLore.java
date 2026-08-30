package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.Bulwark;
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
        return build(shield, Bulwark.NONE);
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
     */
    public static List<Component> build(ShieldDefinition shield, double bulwarkPercent) {
        List<Component> lore = new ArrayList<>();

        // The stat block. One line, unconditionally -- including for a shield that declares no
        // reduction at all, which then honestly reads "Damage Reduction: 0%". Hiding the line at
        // zero would make a mis-authored shield look like a shield with no stat rather than one
        // with a zero stat, and those want telling apart.
        //
        // GRAY label, GREEN number -- the split WeaponLore's damage line already uses. The colour is
        // NamedTextColor.GREEN because that is exactly what StatsBarText.DEFENSE_COLOR is, read off
        // the HUD rather than picked: a shield's Damage Reduction and armor's Defense are the same
        // kind of number and compose with each other, so they read in the same colour. (Adventure
        // has no LIME; GREEN is the bright one Minecraft renders as lime.)
        lore.add(plain(ShieldLoreLines.DAMAGE_REDUCTION_LABEL, NamedTextColor.GRAY)
                .append(plain(ShieldLoreLines.damageReductionValue(
                        Bulwark.effectiveDr(shield.blockDr(), bulwarkPercent)), NamedTextColor.GREEN)));

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
