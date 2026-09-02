package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tool content model and the refusals its constructor makes.
 *
 * <p>The headline is {@link #aKindThatDisagreesWithItsMaterialIsRefused}. It is the same defect
 * shape as slice 2's {@code craft_result} claim not equalling its own {@code material} -- an item
 * that mints, renders, and describes itself as something it is not -- and it gets the same answer:
 * refused at boot, so the loader turns it into a named, skipped entry rather than a tooltip nobody
 * reads carefully.
 *
 * <p>These refusals live here rather than in {@code ContentValidator} because none of them needs a
 * Bukkit registry. That is what makes them a two-second test instead of a boot gate row.
 *
 * <p>Each test names the mutation it forces red.
 */
class ToolDefinitionTest {

    private static ToolDefinition tool(String material, ToolKind kind) {
        return new ToolDefinition(material, "Iron Pickaxe", Rarity.COMMON, material, kind, List.of());
    }

    // --- What it carries ------------------------------------------------------------------------

    @Test
    void aToolCarriesItsKindAndNoStat() {
        ToolDefinition t = tool("iron_pickaxe", ToolKind.PICKAXE);
        assertEquals("iron_pickaxe", t.id());
        assertEquals("iron_pickaxe", t.material());
        assertEquals(ToolKind.PICKAXE, t.kind());
        assertEquals(Rarity.COMMON, t.rarity());
        // The record has no stat accessor at all, deliberately: mining speed, harvest level and
        // durability are vanilla's, and ToolItems.mint pins nothing. There is no number here to
        // drift from the one the game actually uses -- which is the ArmorConsistency trap, absent
        // by construction rather than guarded.
    }

    @Test
    void aToolIsGearAndFlowsThroughTheSealedInterface() {
        // The point of the slice. It must BE a GearDefinition, or GearItems' three switches never
        // see it and the compiler never asks what a tool does.
        GearDefinition gear = tool("iron_pickaxe", ToolKind.PICKAXE);
        assertEquals("iron_pickaxe", gear.id());
        assertTrue(gear.craftResult().isEmpty());
        assertTrue(gear.flavor().isEmpty());
    }

    @Test
    void flavorIsNeverNullAndIsDefensivelyCopied() {
        // GearDefinition.flavor()'s contract: "Never null; may be empty."
        assertTrue(new ToolDefinition("iron_hoe", "Iron Hoe", Rarity.COMMON, "iron_hoe",
                ToolKind.HOE, null).flavor().isEmpty());

        List<String> mutable = new ArrayList<>();
        mutable.add("one");
        ToolDefinition t = new ToolDefinition("iron_hoe", "Iron Hoe", Rarity.COMMON, "iron_hoe",
                ToolKind.HOE, mutable);
        mutable.add("two");
        assertEquals(1, t.flavor().size(), "the record kept a live reference to the caller's list");
        // Mutation: assign flavor directly without List.copyOf -> reddens.
    }

    // --- The kind refusals ----------------------------------------------------------------------

    @Test
    void aToolWithNoKindIsRefused() {
        // It must NOT fall back to the generic word "Tool". A silent fallback is invisible in play
        // and permanent on every item minted before anyone notices -- and gear is never re-minted
        // retroactively.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tool("iron_pickaxe", null));
        assertTrue(ex.getMessage().contains("iron_pickaxe"), "the message must name the entry");
        assertTrue(ex.getMessage().contains("kind"));
        // Mutation: default a null kind to PICKAXE -> reddens, and every kindless entry mints as a
        // pickaxe whatever it actually is.
    }

    @Test
    void aKindThatDisagreesWithItsMaterialIsRefused() {
        // THE defect. material: iron_pickaxe with kind: shovel mints a pickaxe whose footer reads
        // "Common Shovel". Nothing throws, nothing logs, and the item is wrong forever.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tool("iron_pickaxe", ToolKind.SHOVEL));
        assertTrue(ex.getMessage().contains("iron_pickaxe"));
        assertTrue(ex.getMessage().contains("shovel"));

        // And the pair a bare endsWith would have admitted, asserted HERE as well as in
        // ToolKindTest, because this is the layer content actually reaches.
        assertThrows(IllegalArgumentException.class, () -> tool("iron_pickaxe", ToolKind.AXE));
        // Mutation: delete the matchesMaterial check -> both redden.
    }

    @Test
    void aClaimSpelledWithItsNamespaceOrInCapsStillAgrees() {
        // Compared on the NORMALISED token, so a spelling difference is not a false alarm. Bukkit's
        // own matchMaterial accepts all three of these.
        assertEquals(ToolKind.PICKAXE, tool("minecraft:iron_pickaxe", ToolKind.PICKAXE).kind());
        assertEquals(ToolKind.PICKAXE, tool("IRON_PICKAXE", ToolKind.PICKAXE).kind());
        // Mutation: compare against the raw material instead of CraftResultToken.token(..) ->
        // reddens, and every namespaced or upper-case content file is refused.
    }

    @Test
    void anUntieredToolIsAnOrdinaryEntry() {
        // Shears is the reason the loader reads a flat list rather than a tier grid. If it needed a
        // special case here, the flat list would not have bought anything.
        ToolDefinition shears = new ToolDefinition("shears", "Shears", Rarity.COMMON, "shears",
                ToolKind.SHEARS, List.of());
        assertEquals(ToolKind.SHEARS, shears.kind());
        assertEquals("shears", shears.id());
    }

    // --- The shared refusals --------------------------------------------------------------------

    @Test
    void theIdentityFieldsAreRefusedWhenBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition(" ", "Iron Pickaxe", Rarity.COMMON, "iron_pickaxe",
                        ToolKind.PICKAXE, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition("iron_pickaxe", " ", Rarity.COMMON, "iron_pickaxe",
                        ToolKind.PICKAXE, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition("iron_pickaxe", "Iron Pickaxe", null, "iron_pickaxe",
                        ToolKind.PICKAXE, List.of()));
    }

    @Test
    void aBlankMaterialIsRefusedAndSaysWhyItCannotBeDefaulted() {
        // Unlike a shield, "a tool" names no single vanilla item -- and the material is also the id,
        // so a default would give two entries the same id and the second would be refused as a
        // duplicate, which is a confusing way to report a missing key.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition("iron_pickaxe", "Iron Pickaxe", Rarity.COMMON, " ",
                        ToolKind.PICKAXE, List.of()));
        assertTrue(ex.getMessage().contains(ToolDefinition.MATERIAL_REQUIRED));
    }

    @Test
    void aBlankCraftResultThrowsRatherThanBeingReadAsAbsent() {
        // CraftResultToken.normalise's contract, reached with the kind noun "tool" so the message
        // names the right schema. A file that writes `craft_result:` with nothing after it stated an
        // intention it failed to finish.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition("iron_pickaxe", "Iron Pickaxe", Rarity.COMMON,
                        "iron_pickaxe", ToolKind.PICKAXE, List.of(), Optional.of(" ")));
        assertTrue(ex.getMessage().startsWith("tool "), "the message must name the KIND: " + ex.getMessage());
        // Mutation: pass "shield" as the noun -> reddens, and boot logs point at the wrong schema.
    }

    @Test
    void aCraftResultClaimIsNormalisedTheSameWayTheIndexKeysIt() {
        ToolDefinition t = new ToolDefinition("iron_pickaxe", "Iron Pickaxe", Rarity.COMMON,
                "iron_pickaxe", ToolKind.PICKAXE, List.of(), Optional.of("MINECRAFT:IRON_PICKAXE"));
        assertEquals(Optional.of("iron_pickaxe"), t.craftResult());
        // A lookup that normalised differently from the build would miss every entry, and the
        // symptom would be "crafting mints nothing" with no error anywhere.
    }
}
