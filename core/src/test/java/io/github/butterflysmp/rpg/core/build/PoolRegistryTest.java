package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolRegistryTest {

    private static PoolDefinition pool(String classId, String elementId) {
        return new PoolDefinition(new CellKey(classId, elementId), classId,
                List.of("sunfall"), List.of("rekindle", "solar_lance"),
                new Loadout("sunfall", "rekindle", "solar_lance"));
    }

    @Test
    void findsByTheCompositeKey() {
        var registry = new PoolRegistry();
        registry.register(pool("ranger", "fire"));
        registry.register(pool("mage", "fire"));
        assertEquals("ranger", registry.find("ranger", "fire").orElseThrow().cell().classId());
        assertTrue(registry.find("ranger", "void").isEmpty());
        assertEquals(2, registry.size());
    }

    @Test
    void theKeyIsTwoFieldsSoSplittingCannotCollide() {
        var registry = new PoolRegistry();
        registry.register(pool("ranger_f", "ire"));
        assertTrue(registry.find("ranger", "fire").isEmpty());
    }

    @Test
    void aDuplicateCellIsRefused() {
        var registry = new PoolRegistry();
        registry.register(pool("ranger", "fire"));
        assertThrows(IllegalStateException.class, () -> registry.register(pool("ranger", "fire")));
    }

    /** A profile's class or element can be "none" or null; neither is a cell. */
    @Test
    void noneAndNullFindNothing() {
        var registry = new PoolRegistry();
        registry.register(pool("ranger", "fire"));
        assertTrue(registry.find(null, "fire").isEmpty());
        assertTrue(registry.find("ranger", null).isEmpty());
        assertTrue(registry.find("none", "none").isEmpty());
    }

    @Test
    void theDefaultMustBeDrawnFromTheLists() {
        var cell = new CellKey("ranger", "fire");
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(cell, "x",
                List.of("sunfall"), List.of("rekindle", "solar_lance"),
                new Loadout("other_ultimate", "rekindle", "solar_lance")));
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(cell, "x",
                List.of("sunfall"), List.of("rekindle", "solar_lance"),
                new Loadout("sunfall", "rekindle", "not_in_the_pool")));
    }

    @Test
    void anAbilityCannotBeBothAnUltimateAndAnActive() {
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(new CellKey("ranger", "fire"), "x",
                List.of("rekindle"), List.of("rekindle", "solar_lance"),
                new Loadout("rekindle", "rekindle", "solar_lance")));
    }

    @Test
    void aPoolMustBeAbleToFillALoadout() {
        var cell = new CellKey("ranger", "fire");
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(cell, "x",
                List.of(), List.of("rekindle", "solar_lance"), new Loadout("sunfall", "rekindle", "solar_lance")));
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(cell, "x",
                List.of("sunfall"), List.of("rekindle"), new Loadout("sunfall", "rekindle", "solar_lance")));
    }

    @Test
    void aListedTwiceIdIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(new CellKey("ranger", "fire"), "x",
                List.of("sunfall"), List.of("rekindle", "rekindle", "solar_lance"),
                new Loadout("sunfall", "rekindle", "solar_lance")));
    }

    @Test
    void theTwoDefaultActivesMustDiffer() {
        assertThrows(IllegalArgumentException.class, () -> new Loadout("sunfall", "rekindle", "rekindle"));
    }

    @Test
    void aLoadoutMapsEachSlotToItsAbility() {
        var loadout = new Loadout("sunfall", "rekindle", "solar_lance");
        assertEquals("rekindle", loadout.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals("solar_lance", loadout.idFor(LoadoutSlot.ACTIVE_2));
        assertEquals("sunfall", loadout.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(List.of("sunfall", "rekindle", "solar_lance"), loadout.ids());
    }
}
