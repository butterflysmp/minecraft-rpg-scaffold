package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The armor registry: lookup, duplicate refusal and deterministic order.
 *
 * The headline is {@link #twoPiecesSharingAnIdAreRefusedAtRegistrationRatherThanOverwriting}. One
 * tier file emits FOUR definitions, so a copy-paste slip inside a single file -- the same material
 * token pasted into two slots -- is a realistic authoring error here in a way it is not for the
 * one-file-one-id loaders. Refusing at registration turns it into a named, skipped file.
 *
 * Each test names the mutation it forces red.
 */
class ArmorRegistryTest {

    private static ArmorDefinition piece(String id, ArmorSlot slot) {
        return new ArmorDefinition(id, id, Rarity.COMMON, id, slot, 1, List.of());
    }

    // --- Lookup ---------------------------------------------------------------------------------

    @Test
    void aRegisteredPieceIsFoundByItsId() {
        ArmorRegistry registry = new ArmorRegistry();
        registry.register(piece("iron_helmet", ArmorSlot.HEAD));
        assertTrue(registry.find("iron_helmet").isPresent());
        assertEquals("iron_helmet", registry.find("iron_helmet").orElseThrow().id());
        assertEquals(1, registry.size());
        // Mutation: key the map by displayName instead of id -> reddens.
    }

    @Test
    void anUnknownIdComesBackEmptyRatherThanNull() {
        ArmorRegistry registry = new ArmorRegistry();
        assertTrue(registry.find("no_such_piece").isEmpty());
        assertEquals(0, registry.size());
        // Mutation: return the map value directly rather than Optional.ofNullable -> /rpg give
        // NPEs on a typo instead of reporting an unknown id -> reddens.
    }

    // --- Refusal --------------------------------------------------------------------------------

    @Test
    void twoPiecesSharingAnIdAreRefusedAtRegistrationRatherThanOverwriting() {
        ArmorRegistry registry = new ArmorRegistry();
        registry.register(piece("iron_helmet", ArmorSlot.HEAD));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> registry.register(piece("iron_helmet", ArmorSlot.CHEST)));
        assertTrue(ex.getMessage().contains("iron_helmet"), "the message names the id: " + ex.getMessage());
        assertEquals(ArmorSlot.HEAD, registry.find("iron_helmet").orElseThrow().slot(),
                "the first registration survived; the duplicate did not overwrite it");
        // Mutation: use put instead of putIfAbsent -> the second silently replaces the first, one
        // tier file loses a slot, and the boot log says nothing -> reddens.
    }

    // --- Order and exposure ---------------------------------------------------------------------

    @Test
    void insertionOrderIsPreservedSoTwoBootsAgreeOnSuggestionOrder() {
        // The loader sorts its files, and each file emits its four pieces in a fixed slot order, so
        // /rpg give's suggestion list is stable across restarts. A HashMap here would scramble it.
        ArmorRegistry registry = new ArmorRegistry();
        registry.register(piece("diamond_helmet", ArmorSlot.HEAD));
        registry.register(piece("diamond_chestplate", ArmorSlot.CHEST));
        registry.register(piece("diamond_leggings", ArmorSlot.LEGS));
        registry.register(piece("diamond_boots", ArmorSlot.FEET));
        List<String> ids = new ArrayList<>();
        registry.all().forEach(a -> ids.add(a.id()));
        assertEquals(List.of("diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots"), ids);
        // Mutation: swap LinkedHashMap for HashMap -> reddens (these four ids do not hash in order).
    }

    @Test
    void theExposedCollectionCannotBeEditedByItsCaller() {
        ArmorRegistry registry = new ArmorRegistry();
        registry.register(piece("iron_boots", ArmorSlot.FEET));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.all().add(piece("smuggled_in", ArmorSlot.HEAD)));
        // Mutation: return byId.values() unwrapped -> a caller can add a piece no loader validated
        // -> reddens.
    }
}
