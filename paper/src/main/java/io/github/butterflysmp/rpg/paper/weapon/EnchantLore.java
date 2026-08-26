package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveEnchant;
import io.github.butterflysmp.rpg.core.enchant.EnchantLoreLines;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The enchant block of the tooltip: one line per ACTIVE enchant, in vanilla style.
 *
 * <p><b>This is the only lore code that reads per-ITEM state, and it is a separate class for
 * exactly that reason.</b> {@link WeaponLore#build} takes only a definition and the element
 * registry, and its "mint-time only and cannot drift" promise is enforced by that SIGNATURE rather
 * than by discipline -- {@code WeaponLoreTest} says so in as many words. Widening it to accept an
 * {@link EnchantState} would put per-item state in reach of every line in it and turn a structural
 * guarantee back into a convention. So the two never meet: WeaponLore sees only static content,
 * EnchantLore sees only item state and takes no {@code WeaponDefinition} at all.
 *
 * <p>Pure Adventure, no {@code ItemStack} -- so it is unit-testable, like {@link WeaponLore} and
 * {@code NameplateText}. The text itself comes from {@code EnchantLoreLines} in core, so the roman
 * numerals run in the two-second loop.
 */
public final class EnchantLore {

    private EnchantLore() {}

    /**
     * One line per active enchant: {@code "Unbreaking III"}, grey and not italic.
     *
     * <p>Walks {@link EnchantState#effective()}, which is what makes the tooltip and the durability
     * seam agree by construction: the same enchant active in two slots renders ONCE, at the level
     * {@code effective()} resolved, which is literally the number the seam will act on. Rendering
     * per slot would let the tooltip promise two Unbreakings while one took effect.
     *
     * <p>An id the registry no longer knows is title-cased and rendered ANYWAY, rather than hidden.
     * That is the deliberate choice, and it follows from the seam: the seam compares ids and never
     * consults the registry, so a deleted content file leaves the enchant WORKING. An enchant that
     * silently works while showing nothing is a worse bug than one with an ugly name. Same
     * fail-soft instinct as {@code WeaponLore.elementLine} for an unknown element.
     */
    public static List<Component> lines(EnchantState state, EnchantRegistry enchants) {
        List<Component> lines = new ArrayList<>();
        for (ActiveEnchant active : state.effective()) {
            EnchantDefinition def = enchants == null ? null : enchants.find(active.enchantId()).orElse(null);
            String name = def != null ? def.displayName() : titleCase(active.enchantId());
            int maxLevel = def != null ? def.maxLevel() : EnchantState.MAX_LEVEL;

            // Grey, and explicitly NOT italic -- matching WeaponLore.plain and modern vanilla,
            // where the flavour block is the only italic thing on the tooltip. The explicit false
            // is load-bearing and must not be "simplified" to a deletion: lore renders italic by
            // DEFAULT, so dropping the call leaves the line italic via NOT_SET rather than making
            // it plain.
            lines.add(Component.text(EnchantLoreLines.label(name, active.level(), maxLevel),
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        return lines;
    }

    /**
     * {@code base} with the enchant block, and one blank after it, at the TOP.
     *
     * <p>Returns {@code base} UNCHANGED when there is nothing active -- not "base plus a blank".
     * Every unenchanted weapon in the game goes through here, and a stray leading empty line on all
     * of them is the kind of thing nobody notices until every tooltip looks wrong.
     *
     * <p>Index 0 rather than above the footer: it matches vanilla's order (name, enchantments, then
     * everything else), it keeps the rarity/class footer's "always last" promise literally true,
     * and it needs no index arithmetic -- so nothing here has to be updated when the footer changes.
     */
    public static List<Component> applied(List<Component> base, List<Component> enchantLines) {
        if (enchantLines.isEmpty()) return base;

        List<Component> out = new ArrayList<>(enchantLines.size() + 1 + base.size());
        out.addAll(enchantLines);
        out.add(Component.empty().decoration(TextDecoration.ITALIC, false));
        out.addAll(base);
        return out;
    }

    /** "unbreaking" -> "Unbreaking", for an id whose content file has gone missing. */
    private static String titleCase(String raw) {
        if (raw.isEmpty()) return raw;
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase(Locale.ROOT);
    }
}
