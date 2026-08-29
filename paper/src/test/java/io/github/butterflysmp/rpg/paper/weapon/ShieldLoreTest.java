package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shield tooltip's colour and layout.
 *
 * The headline is {@link #theRarityFooterIsLiterallyLast} -- the footer is the tier badge, and the
 * enchant block is PREPENDED by {@code EnchantLore.applied}, so anything appended after the footer
 * would push the badge into the middle of the tooltip where it reads as a stray line.
 *
 * Pure Adventure and a plain record -- no ItemStack, so no running server needed. That is also the
 * boundary of what this can cover: {@code ShieldItems.mint} needs a live server and is
 * boot-witnessed instead.
 *
 * Each test names the mutation it forces red.
 */
class ShieldLoreTest {

    private static ShieldDefinition shield(Rarity rarity, double blockDr, List<String> flavor) {
        return new ShieldDefinition("roundshield", "Roundshield", rarity, "shield", blockDr, flavor);
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    // --- The stat block -------------------------------------------------------------------------

    @Test
    void theTooltipOpensOnTheBlockStatWithNoLeadingBlank() {
        // A weapon's stat block opens with a blank because it sits UNDER an element line. A shield
        // has nothing above it, so a leading blank would render as a gap at the top of the tooltip
        // and read as a bug rather than as spacing.
        List<Component> lore = ShieldLore.build(shield(Rarity.COMMON, 0.5, List.of()));
        assertEquals("Block: 50%", plain(lore.get(0)));
        assertEquals(NamedTextColor.GRAY, lore.get(0).color());
        // Mutation: add a blank() before the stat line -> the tooltip opens on an empty row
        // -> reddens.
    }

    @Test
    void aZeroBlockShieldStillShowsItsStatLine() {
        // Hiding the line at zero would make a mis-authored shield look like a shield with NO stat
        // rather than one with a zero stat, and those want telling apart -- especially since a
        // zero-block shield is exactly what a typo'd block_dr key produces.
        List<Component> lore = ShieldLore.build(shield(Rarity.COMMON, 0.0, List.of()));
        assertEquals("Block: 0%", plain(lore.get(0)));
        // Mutation: guard the stat line behind shield.blocks() -> a zero shield renders no stat and
        // looks identical to one whose loader dropped the key -> reddens.
    }

    @Test
    void everyLineButFlavorIsNonItalic() {
        // Item lore renders italic by default. Flavour is the ONE italic block, the same rule
        // WeaponLore follows, so italics mean "prose" and nothing else.
        List<Component> lore = ShieldLore.build(shield(Rarity.RARE, 0.5, List.of("Plain oak.")));
        assertFalse(lore.get(0).hasDecoration(TextDecoration.ITALIC), "the stat line is not italic");
        assertEquals(TextDecoration.State.TRUE,
                lore.get(2).decoration(TextDecoration.ITALIC), "flavour is italic");
        assertFalse(lore.get(lore.size() - 1).hasDecoration(TextDecoration.ITALIC),
                "the footer is not italic");
        // Mutation: drop the .decoration(ITALIC, false) from plain() -> every line renders italic
        // and the flavour block stops being distinguishable -> reddens.
    }

    // --- The footer, which is the tier badge ----------------------------------------------------

    @Test
    void theRarityFooterIsLiterallyLast() {
        // THE headline. EnchantLore.applied PREPENDS the enchant block, so the footer is only ever
        // last if nothing appends after it here.
        List<Component> withFlavor = ShieldLore.build(
                shield(Rarity.EPIC, 0.5, List.of("Plain oak.", "Banded in iron.")));
        assertEquals("Epic Shield", plain(withFlavor.get(withFlavor.size() - 1)));

        List<Component> without = ShieldLore.build(shield(Rarity.EPIC, 0.5, List.of()));
        assertEquals("Epic Shield", plain(without.get(without.size() - 1)));
        // Mutation: move the footer above the flavour block -> the tier badge lands mid-tooltip
        // -> reddens on the flavour case, and NOT on the no-flavour case, which is why both are
        // asserted here.
    }

    @Test
    void theFooterWearsTheRarityColourAndSaysShieldNotWeapon() {
        // The footer is the tier badge, so the colour is the tier's -- RarityColors, the closed
        // enum's exhaustive switch. And the noun is "Shield": a shield has no WeaponClass, so
        // there is no middle word, and calling it a Weapon would be the tooltip lying about what
        // the item is.
        List<Component> lore = ShieldLore.build(shield(Rarity.LEGENDARY, 0.5, List.of()));
        Component footer = lore.get(lore.size() - 1);
        assertEquals("Legendary Shield", plain(footer));
        assertEquals(RarityColors.of(Rarity.LEGENDARY), footer.color());
        assertFalse(plain(footer).contains("Weapon"), "a shield is not a weapon");
        // Mutation: colour the footer from a fixed NamedTextColor instead of RarityColors.of ->
        // every tier reads the same and the badge stops meaning anything -> reddens.
    }

    @Test
    void everyRarityTitleCasesIntoItsOwnFooter() {
        // The enum names are SHOUTED; the footer must not be. Looped so a new tier cannot be added
        // with a footer nobody checked.
        for (Rarity rarity : Rarity.values()) {
            List<Component> lore = ShieldLore.build(shield(rarity, 0.5, List.of()));
            String footer = plain(lore.get(lore.size() - 1));
            assertTrue(footer.endsWith(" Shield"), "footer must name the item type, got: " + footer);
            assertFalse(footer.contains(rarity.name()),
                    "the SHOUTED enum name must not survive into the tooltip, got: " + footer);
        }
        // Mutation: drop the titleCase call -> "LEGENDARY Shield" -> reddens.
    }

    // --- Flavour --------------------------------------------------------------------------------

    @Test
    void flavourIsSeparatedFromTheStatBlockAndOmittedEntirelyWhenAbsent() {
        List<Component> without = ShieldLore.build(shield(Rarity.COMMON, 0.5, List.of()));
        assertEquals(3, without.size(), "stat, blank, footer -- and no flavour gap");

        List<Component> with = ShieldLore.build(
                shield(Rarity.COMMON, 0.5, List.of("Plain oak.", "Banded in iron.")));
        assertEquals(6, with.size(), "stat, blank, two flavour lines, blank, footer");
        assertEquals("", plain(with.get(1)), "a blank separates the stat from the flavour");
        assertEquals("Plain oak.", plain(with.get(2)));
        assertEquals("Banded in iron.", plain(with.get(3)));
        // Mutation: emit the flavour blank unconditionally -> a shield with no flavour grows a
        // double gap above its footer -> reddens on the size assertion.
    }
}
