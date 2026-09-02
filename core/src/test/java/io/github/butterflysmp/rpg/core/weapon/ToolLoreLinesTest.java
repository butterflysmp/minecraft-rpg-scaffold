package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * A tool's footer noun.
 *
 * <p>The whole of a tool's own tooltip text, because a tool has no stat and therefore no stat line.
 * That makes this class thin and the test short -- but the noun is the ONE thing on a tool's tooltip
 * that is neither vanilla's nor authored, so it is also the only thing here that can be wrong.
 *
 * <p>Each test names the mutation it forces red.
 */
class ToolLoreLinesTest {

    @Test
    void everyKindHasItsOwnFooterNoun() {
        assertEquals("Pickaxe", ToolLoreLines.kindNoun(ToolKind.PICKAXE));
        assertEquals("Axe", ToolLoreLines.kindNoun(ToolKind.AXE));
        assertEquals("Shovel", ToolLoreLines.kindNoun(ToolKind.SHOVEL));
        assertEquals("Hoe", ToolLoreLines.kindNoun(ToolKind.HOE));
        assertEquals("Shears", ToolLoreLines.kindNoun(ToolKind.SHEARS));
        // Mutation: return "Tool" from every arm -> reddens. That mutation is the exact shape of the
        // missing-kind fallback this design refuses, so this is the test that stands in for it.
    }

    @Test
    void noKindFootersAsTheGenericWordTool() {
        // Stated as its own assertion rather than left implied by the five literals above, because
        // "Common Tool" is the specific wrong answer: it is what a default arm, a null kind, or a
        // fallback would all produce, and all three are indistinguishable in play from each other.
        for (ToolKind kind : ToolKind.values()) {
            assertNotEquals("Tool", ToolLoreLines.kindNoun(kind),
                    kind + " footers as the generic word, which is what a fallback looks like");
        }
        assertEquals(5, ToolKind.values().length, "the walk must not be empty or short");
    }

    @Test
    void noTwoKindsShareAFooterNounAndNoneIsBlank() {
        // Discovery-shaped, so a sixth kind is covered the day it is added. Asserts the walk is
        // non-empty first: a loop over nothing reads exactly like a loop that passed.
        List<String> nouns = new ArrayList<>();
        for (ToolKind kind : ToolKind.values()) {
            String noun = ToolLoreLines.kindNoun(kind);
            assertFalse(noun == null || noun.isBlank(), kind + " has no footer noun");
            assertFalse(nouns.contains(noun), kind + " reuses the noun '" + noun + "'");
            nouns.add(noun);
        }
        assertEquals(5, nouns.size(), "the walk must not be empty or short");
        // Mutation: point two arms at the same string -> reddens on the contains() check.
    }

    @Test
    void theNounIsTitleCasedAndNotTheRawMaterialToken() {
        // The two are deliberately separate switches. They agree for every constant that exists
        // today, which is exactly what would tempt someone to derive one from the other by
        // case-folding -- and that derivation is the "template that assumed today's values" shape
        // this project already shipped once, as "a Armor enchant". It breaks at the first
        // irregular: flint_and_steel would fold to "Flint_and_steel".
        for (ToolKind kind : ToolKind.values()) {
            String noun = ToolLoreLines.kindNoun(kind);
            assertFalse(noun.contains("_"), kind + "'s noun carries a token separator");
            assertEquals(Character.toUpperCase(noun.charAt(0)), noun.charAt(0),
                    kind + "'s noun is not title-cased, so the footer would read 'Common pickaxe'");
        }
        // Mutation: return materialToken() instead -> "Common pickaxe" -> reddens on the case check.
    }
}
