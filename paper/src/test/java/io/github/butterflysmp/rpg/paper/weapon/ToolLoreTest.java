package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolKind;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tool tooltip's layout.
 *
 * <p>The headline is {@link #theRarityFooterIsLiterallyLast}, the same as its three siblings: the
 * footer is the tier badge, and the enchant block is PREPENDED by {@code EnchantLore.applied}, so
 * anything appended after the footer would push the badge into the middle of the tooltip.
 *
 * <p>The second headline is {@link #theFooterNounIsTheKindNotTheGenericWordTool}, which is this
 * kind's own risk: every other gear kind's footer noun is either a constant or comes from an axis
 * that already existed. A tool's comes from a brand-new enum, and the wrong answer -- "Common Tool"
 * on all five -- is what a default arm, a null kind or a fallback would each produce.
 *
 * <p>Pure Adventure and a plain record -- no ItemStack, so no running server needed. That is also
 * the boundary: {@code ToolItems.mint} needs a live server and is boot-witnessed instead, which is
 * where "does a minted pickaxe still mine" lives.
 *
 * <p>Each test names the mutation it forces red.
 */
class ToolLoreTest {

    private static ToolDefinition tool(Rarity rarity, ToolKind kind, List<String> flavor) {
        String material = kind == ToolKind.SHEARS ? "shears" : "iron_" + kind.materialToken();
        return new ToolDefinition(material, "Iron Thing", rarity, material, kind, flavor);
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    // --- The footer -----------------------------------------------------------------------------

    @Test
    void theRarityFooterIsLiterallyLast() {
        List<Component> lore = ToolLore.build(tool(Rarity.RARE, ToolKind.PICKAXE, List.of("a", "b")));
        assertEquals("Rare Pickaxe", plain(lore.get(lore.size() - 1)));
        // Mutation: append anything after appendRarityFooter -> the badge stops being last ->
        // reddens.
    }

    @Test
    void theFooterNounIsTheKindNotTheGenericWordTool() {
        // THE defect specific to this kind. "Common Tool" is what a default arm, a missing kind
        // falling back, or a noun derived from the wrong place would all produce -- and all three
        // are indistinguishable in play from each other and from a bug nobody has noticed.
        assertEquals("Common Pickaxe",
                plain(last(ToolLore.build(tool(Rarity.COMMON, ToolKind.PICKAXE, List.of())))));
        assertEquals("Common Shears",
                plain(last(ToolLore.build(tool(Rarity.COMMON, ToolKind.SHEARS, List.of())))));

        // And not the display NAME either. The record above is called "Iron Thing" on purpose: the
        // footer says what KIND of gear this is, exactly as a weapon's reads "Rare Melee Weapon"
        // rather than repeating the name three lines above it.
        for (ToolKind kind : ToolKind.values()) {
            String footer = plain(last(ToolLore.build(tool(Rarity.COMMON, kind, List.of()))));
            assertFalse(footer.contains("Tool"), kind + " footers with the generic word: " + footer);
            assertFalse(footer.contains("Iron Thing"), kind + " footer repeats the display name");
            assertTrue(footer.startsWith("Common "), kind + " footer lost its rarity: " + footer);
        }
        assertEquals(5, ToolKind.values().length, "the walk must not be empty or short");
        // Mutation: pass the literal "Tool" to appendRarityFooter -> every assertion above reddens.
        // Mutation: pass tool.displayName() -> the name check reddens.
    }

    // --- Flavour --------------------------------------------------------------------------------

    @Test
    void aToolWithNoFlavourIsFooterOnlyAndOpensOnNoBlankLine() {
        // A tool has NO STAT BLOCK -- mining speed and durability are vanilla's -- so an
        // unflavoured tool's whole tooltip is the blank-plus-footer. Nothing above it means a
        // leading blank would render as a gap at the top and read as a bug.
        List<Component> lore = ToolLore.build(tool(Rarity.COMMON, ToolKind.HOE, List.of()));
        assertEquals(2, lore.size(), "expected exactly blank + footer, got: " + lore.size());
        assertEquals("", plain(lore.get(0)), "the footer's own separator");
        assertEquals("Common Hoe", plain(lore.get(1)));
        // Mutation: add a stat line -> the size assertion reddens, which is the guard against a
        // number this project displays and does not own creeping in later.
    }

    @Test
    void flavourIsGrayItalicAndSeparatedFromTheFooter() {
        List<Component> lore = ToolLore.build(
                tool(Rarity.COMMON, ToolKind.AXE, List.of("one", "two")));

        // blank, one, two, blank, footer
        assertEquals(5, lore.size());
        assertEquals("one", plain(lore.get(1)));
        assertEquals("two", plain(lore.get(2)));
        assertEquals("Common Axe", plain(lore.get(4)));

        assertEquals(TextDecoration.State.TRUE, lore.get(1).decoration(TextDecoration.ITALIC),
                "flavour is the only italic block in any tooltip");
        // Mutation: drop the blank between flavour and footer -> the size and index assertions
        // redden.
    }

    private static Component last(List<Component> lore) {
        return lore.get(lore.size() - 1);
    }
}
