package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
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
 * The armor tooltip's colour and layout.
 *
 * The headline is {@link #theRarityFooterIsLiterallyLast} -- the footer is the tier badge, and the
 * enchant block is PREPENDED by {@code EnchantLore.applied}, so anything appended after the footer
 * would push the badge into the middle of the tooltip where it reads as a stray line.
 *
 * Pure Adventure and a plain record -- no ItemStack, so no running server needed. That is also the
 * boundary of what this can cover: {@code ArmorItems.mint} needs a live server and is boot-witnessed
 * instead, which is where {@code HIDE_ATTRIBUTES} gets checked.
 *
 * Each test names the mutation it forces red.
 */
class ArmorLoreTest {

    private static ArmorDefinition armor(Rarity rarity, ArmorSlot slot, double defense,
                                         List<String> flavor) {
        return new ArmorDefinition("diamond_helmet", "Diamond Helmet", rarity,
                "diamond_helmet", slot, defense, flavor);
    }

    private static ArmorDefinition helmet() {
        return armor(Rarity.UNCOMMON, ArmorSlot.HEAD, 3, List.of());
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    // --- The stat block -------------------------------------------------------------------------

    @Test
    void theDefenseLineOpensTheTooltipWithNoLeadingBlank() {
        // A weapon's stat block sits under an element line and opens with a blank; armor has
        // nothing above it, so starting with an empty line would read as a rendering bug.
        List<Component> lore = ArmorLore.build(helmet());
        assertEquals("Defense: 3", plain(lore.get(0)));
        // Mutation: add a leading blank() -> reddens.
    }

    @Test
    void theStatLineSplitsAGrayLabelFromAGreenNumber() {
        // GREEN because that is exactly StatsBarText.DEFENSE_COLOR: this line and the action bar's
        // field report the SAME STAT, so a player glancing between them must not see two colours.
        Component line = ArmorLore.build(helmet()).get(0);
        assertEquals(NamedTextColor.GRAY, line.color(), "the label is gray");
        assertEquals(1, line.children().size(), "the value is a single appended child");
        assertEquals(NamedTextColor.GREEN, line.children().get(0).color(), "the number is green");
        assertEquals("3", plain(line.children().get(0)));
        // Mutation: colour the value DARK_GREEN -> reddens. (Adventure has no LIME; GREEN is the
        // bright one Minecraft renders as lime, and picking the wrong one ships the wrong colour
        // with nothing failing.)
    }

    @Test
    void aPieceDeclaringNoDefenseStillShowsTheLineRatherThanHidingIt() {
        // Hiding the line at zero would make a mis-authored piece look like one with no stat rather
        // than one with a zero stat, and those want telling apart.
        List<Component> lore = ArmorLore.build(armor(Rarity.COMMON, ArmorSlot.FEET, 0, List.of()));
        assertEquals("Defense: 0", plain(lore.get(0)));
        // Mutation: gate the stat line on defense > 0 -> reddens.
    }

    @Test
    void nothingInTheTooltipIsItalicExceptFlavour() {
        List<Component> lore = ArmorLore.build(
                armor(Rarity.UNCOMMON, ArmorSlot.HEAD, 3, List.of("Cut from a single lattice.")));
        assertEquals(TextDecoration.State.FALSE, lore.get(0).decoration(TextDecoration.ITALIC),
                "the stat line is not italic");
        assertEquals(TextDecoration.State.TRUE, lore.get(2).decoration(TextDecoration.ITALIC),
                "flavour is the only italic block");
        assertEquals(TextDecoration.State.FALSE,
                lore.get(lore.size() - 1).decoration(TextDecoration.ITALIC),
                "the footer is not italic");
        // Mutation: drop the explicit ITALIC=false -> Minecraft italicises custom lore by default
        // -> reddens.
    }

    @Test
    void theDefenseLineShowsTheEffectiveValueOnceProtectionIsOnThePiece() {
        // The lore must not disagree with the stat. A Protection III diamond chestplate contributes
        // 17 to Defense, so its own tooltip must say 17 -- not the material's bare 8.
        ArmorDefinition chest = armor(Rarity.UNCOMMON, ArmorSlot.CHEST, 8, List.of());
        assertEquals("Defense: 8", plain(ArmorLore.build(chest).get(0)), "unenchanted is unchanged");
        assertEquals("Defense: 11", plain(ArmorLore.build(chest, 3).get(0)), "Protection I");
        assertEquals("Defense: 14", plain(ArmorLore.build(chest, 6).get(0)), "Protection II");
        assertEquals("Defense: 17", plain(ArmorLore.build(chest, 9).get(0)), "Protection III");
        // Mutation: have the overload ignore protectionPoints -> every level reads 8 -> reddens.
    }

    @Test
    void theUnenchantedOverloadIsAnExactIdentitySoTheGoldenCannotMove() {
        // build(armor) delegates to build(armor, Protection.NONE). If that were not an exact
        // identity, adding the overload would have moved every shipped armor tooltip -- and the
        // golden dump is what would have caught it. Asserted here too, so the reason is stated
        // rather than only observed.
        for (ArmorSlot slot : ArmorSlot.values()) {
            ArmorDefinition piece = armor(Rarity.COMMON, slot, 5, List.of("f"));
            assertEquals(plain(ArmorLore.build(piece).get(0)),
                    plain(ArmorLore.build(piece, 0).get(0)),
                    "the one-arg overload must equal a zero bonus, at " + slot);
        }
        // Mutation: make build(armor) pass anything but NONE -> reddens here AND in the golden.
    }

    // --- The footer -----------------------------------------------------------------------------

    @Test
    void theRarityFooterIsLiterallyLast() {
        List<Component> lore = ArmorLore.build(
                armor(Rarity.UNCOMMON, ArmorSlot.HEAD, 3, List.of("a line", "another")));
        assertEquals("Uncommon Helmet", plain(lore.get(lore.size() - 1)));
        // Mutation: append anything after the footer -> reddens.
    }

    @Test
    void theFooterWearsItsRarityColour() {
        Component footer = last(ArmorLore.build(helmet()));
        assertEquals(NamedTextColor.GREEN, footer.color(), "UNCOMMON is green");

        Component common = last(ArmorLore.build(armor(Rarity.COMMON, ArmorSlot.CHEST, 6, List.of())));
        assertEquals(NamedTextColor.WHITE, common.color(), "COMMON is white");
        // Mutation: hardcode one colour -> the two disagree -> reddens. This is also the first
        // shipped gear to use the UNCOMMON tier at scale.
    }

    @Test
    void everySlotFootersWithItsOwnNoun() {
        assertEquals("Uncommon Helmet", plain(last(ArmorLore.build(
                armor(Rarity.UNCOMMON, ArmorSlot.HEAD, 3, List.of())))));
        assertEquals("Uncommon Chestplate", plain(last(ArmorLore.build(
                armor(Rarity.UNCOMMON, ArmorSlot.CHEST, 8, List.of())))));
        assertEquals("Uncommon Leggings", plain(last(ArmorLore.build(
                armor(Rarity.UNCOMMON, ArmorSlot.LEGS, 6, List.of())))));
        assertEquals("Uncommon Boots", plain(last(ArmorLore.build(
                armor(Rarity.UNCOMMON, ArmorSlot.FEET, 3, List.of())))));
        // Mutation: footer every piece as "Helmet" -> reddens.
    }

    @Test
    void everyRarityTitleCasesIntoItsOwnFooter() {
        // The enum names are SHOUTED; the footer must not be. Looped so a new tier cannot be added
        // with a footer nobody checked.
        for (Rarity rarity : Rarity.values()) {
            List<Component> lore = ArmorLore.build(armor(rarity, ArmorSlot.HEAD, 3, List.of()));
            String footer = plain(last(lore));
            assertTrue(footer.endsWith(" Helmet"), "footer must name the slot, got: " + footer);
            assertFalse(footer.contains(rarity.name()),
                    "the SHOUTED enum name must not survive into the tooltip, got: " + footer);
        }
        assertEquals(6, Rarity.values().length, "the walk must not be empty or short");
        // Mutation: drop the titleCase call -> "LEGENDARY Helmet" -> reddens.
    }

    @Test
    void theFooterSaysTheSlotNounNotTheItemName() {
        // A Leather Cap footers as "Common Helmet", deliberately: the footer says what KIND of gear
        // this is -- the job "Rare Melee Weapon" does on a weapon -- rather than repeating the name.
        ArmorDefinition cap = new ArmorDefinition("leather_helmet", "Leather Cap", Rarity.COMMON,
                "leather_helmet", ArmorSlot.HEAD, 1, List.of());
        assertEquals("Common Helmet", plain(last(ArmorLore.build(cap))));
        // Mutation: footer with the display name -> "Common Leather Cap" -> reddens.
    }

    // --- Flavour --------------------------------------------------------------------------------

    @Test
    void flavourIsSeparatedFromTheStatBlockAndOmittedEntirelyWhenAbsent() {
        List<Component> without = ArmorLore.build(helmet());
        assertEquals(3, without.size(), "stat, blank, footer -- and no flavour gap");

        List<Component> with = ArmorLore.build(
                armor(Rarity.UNCOMMON, ArmorSlot.HEAD, 3, List.of("one", "two")));
        assertEquals(6, with.size(), "stat, blank, two flavour lines, blank, footer");
        assertEquals("one", plain(with.get(2)));
        assertEquals("two", plain(with.get(3)));
        // Mutation: emit the flavour separator unconditionally -> the no-flavour tooltip grows a
        // stray blank -> reddens.
    }

    private static Component last(List<Component> lore) {
        return lore.get(lore.size() - 1);
    }
}
