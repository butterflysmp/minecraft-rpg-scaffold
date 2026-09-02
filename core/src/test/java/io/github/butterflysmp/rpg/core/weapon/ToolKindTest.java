package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tool axis: its material tokens, and the suffix rule that keeps a kind honest about the item
 * it is claiming to be.
 *
 * <p>The headline is {@link #aPickaxeIsNotAnAxe}. It is the one pair a bare {@code endsWith} accepts
 * and is the entire reason {@link ToolKind#matchesMaterial} carries a separator.
 *
 * <p>Each test names the mutation it forces red.
 */
class ToolKindTest {

    // --- The material tokens --------------------------------------------------------------------

    @Test
    void everyKindHasItsOwnMaterialToken() {
        assertEquals("pickaxe", ToolKind.PICKAXE.materialToken());
        assertEquals("axe", ToolKind.AXE.materialToken());
        assertEquals("shovel", ToolKind.SHOVEL.materialToken());
        assertEquals("hoe", ToolKind.HOE.materialToken());
        assertEquals("shears", ToolKind.SHEARS.materialToken());
        // Mutation: point two arms at the same token -> reddens here and on the walk below.
    }

    @Test
    void noTwoKindsShareAMaterialTokenAndNoneIsBlank() {
        // Discovery-shaped, so a sixth kind is covered the day it is added rather than waiting for
        // someone to extend the five literals above. It asserts the walk is NON-EMPTY first, for
        // the reason CLAUDE.md records twice: a loop over nothing reads exactly like a loop that
        // passed.
        List<String> tokens = new ArrayList<>();
        for (ToolKind kind : ToolKind.values()) {
            String token = kind.materialToken();
            assertFalse(token == null || token.isBlank(), kind + " has no material token");
            assertFalse(tokens.contains(token), kind + " reuses the token '" + token + "'");
            assertEquals(token.toLowerCase(java.util.Locale.ROOT), token,
                    kind + "'s token is compared against a normalised material, so it must be lower case");
            tokens.add(token);
        }
        assertEquals(5, tokens.size(), "the walk must not be empty or short");
    }

    // --- The suffix rule ------------------------------------------------------------------------

    @Test
    void aTieredMaterialMatchesItsKind() {
        assertTrue(ToolKind.PICKAXE.matchesMaterial("iron_pickaxe"));
        assertTrue(ToolKind.PICKAXE.matchesMaterial("golden_pickaxe"));
        assertTrue(ToolKind.PICKAXE.matchesMaterial("netherite_pickaxe"));
        assertTrue(ToolKind.HOE.matchesMaterial("wooden_hoe"));
        // Mutation: drop the endsWith arm and keep only equals -> every tiered tool is refused ->
        // reddens, and no content file would load at all.
    }

    @Test
    void aPickaxeIsNotAnAxe() {
        // THE defect this rule exists for, and the reason the separator is not decoration:
        //
        //     "iron_pickaxe".endsWith("axe")  == true
        //     "iron_pickaxe".endsWith("_axe") == false     <- the four chars before the end are "kaxe"
        //
        // Without the underscore, `material: iron_pickaxe` + `kind: axe` loads clean and mints a
        // pickaxe whose footer reads "Common Axe". Nothing throws and nothing logs; the only
        // vantage point is a player reading the last line of a tooltip.
        assertTrue("iron_pickaxe".endsWith("axe"), "the trap itself, executed rather than argued");
        assertFalse(ToolKind.AXE.matchesMaterial("iron_pickaxe"));
        assertFalse(ToolKind.AXE.matchesMaterial("diamond_pickaxe"));
        // The direction that must still work, so the fix cannot be "refuse everything".
        assertTrue(ToolKind.AXE.matchesMaterial("iron_axe"));
        // Mutation: use endsWith(token) without the separator -> the two assertFalse lines redden.
    }

    @Test
    void anUntieredToolMatchesAsAWholeToken() {
        // Shears has no tier prefix at all, which is exactly why the loader reads a flat list rather
        // than a tier grid. The bare-equality arm is what lets it be an ordinary entry instead of a
        // special case -- and a future flint_and_steel or mace arrives the same way.
        assertTrue(ToolKind.SHEARS.matchesMaterial("shears"));

        // WHAT THIS CHECK DOES NOT DO, asserted so nobody reads more into it than it promises:
        // `iron_shears` also matches, because the kind and the token genuinely DO agree -- the
        // suffix names shears. That the token is not a real Bukkit material is a different question
        // with a different answer (ToolItems.materialOf falls back, keeping a typo'd material a
        // cosmetic bug rather than a mechanical one, exactly as ShieldItems does). This refusal is
        // about kind-versus-material DISAGREEMENT, not about material existence.
        assertTrue(ToolKind.SHEARS.matchesMaterial("iron_shears"));

        // The disagreement direction still works for the untiered kind, which is the half that
        // matters: shears cannot be authored as some other family's item.
        assertFalse(ToolKind.SHEARS.matchesMaterial("iron_pickaxe"));
        assertFalse(ToolKind.PICKAXE.matchesMaterial("shears"));
        // Mutation: drop the equals arm -> the bare token `shears` can never be authored, so the one
        // untiered tool in the shipped file stops loading -> reddens on the first assertion.
    }

    @Test
    void aMaterialFromAnotherFamilyMatchesNothing() {
        assertFalse(ToolKind.PICKAXE.matchesMaterial("iron_sword"));
        assertFalse(ToolKind.PICKAXE.matchesMaterial("iron_chestplate"));
        assertFalse(ToolKind.SHOVEL.matchesMaterial("shield"));
        // Null is no-match rather than a throw, so ToolDefinition's own blank-material refusal
        // reports first and names the real problem instead of an NPE from here.
        assertFalse(ToolKind.PICKAXE.matchesMaterial(null));
        // Mutation: return true when the token is unrecognised -> reddens, and every material would
        // be accepted for every kind.
    }

    // --- fromName -------------------------------------------------------------------------------

    @Test
    void fromNameReadsTheContentTokenCaseInsensitively() {
        assertEquals(ToolKind.PICKAXE, ToolKind.fromName("pickaxe"));
        assertEquals(ToolKind.PICKAXE, ToolKind.fromName("PICKAXE"));
        assertEquals(ToolKind.SHEARS, ToolKind.fromName("  shears  "));
        // Mutation: drop the toUpperCase -> lower-case content tokens stop resolving -> reddens.
    }

    @Test
    void fromNameReturnsNullOnAMissSoTheLoaderCanNameTheFile() {
        // Same contract as Rarity.fromName and ArmorSlot.fromName: null, so the CALLER decides what
        // a bad name means. The loader throws, which becomes a named, skipped entry in the boot log.
        assertNull(ToolKind.fromName("pick"));
        assertNull(ToolKind.fromName("spade"), "the token is 'shovel', matching the enum");
        assertNull(ToolKind.fromName(null));
        assertNull(ToolKind.fromName(""));
        // Mutation: throw instead of returning null -> the loader's message loses the entry name.
    }
}
