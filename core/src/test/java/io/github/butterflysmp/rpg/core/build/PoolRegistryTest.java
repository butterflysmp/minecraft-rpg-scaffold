package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolRegistryTest {

    private static PoolDefinition pool(String classId, String elementId) {
        return new PoolDefinition(new CellKey(classId, elementId), classId,
                List.of("sunfall"), List.of("recall", "solar_lance"),
                new Loadout("sunfall", "recall", "solar_lance"));
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
                List.of("sunfall"), List.of("recall", "solar_lance"),
                new Loadout("other_ultimate", "recall", "solar_lance")));
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(cell, "x",
                List.of("sunfall"), List.of("recall", "solar_lance"),
                new Loadout("sunfall", "recall", "not_in_the_pool")));
    }

    @Test
    void anAbilityCannotBeBothAnUltimateAndAnActive() {
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(new CellKey("ranger", "fire"), "x",
                List.of("recall"), List.of("recall", "solar_lance"),
                new Loadout("recall", "recall", "solar_lance")));
    }

    @Test
    void aPoolMustBeAbleToFillALoadout() {
        var cell = new CellKey("ranger", "fire");
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(cell, "x",
                List.of(), List.of("recall", "solar_lance"), new Loadout("sunfall", "recall", "solar_lance")));
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(cell, "x",
                List.of("sunfall"), List.of("recall"), new Loadout("sunfall", "recall", "solar_lance")));
    }

    @Test
    void aListedTwiceIdIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(new CellKey("ranger", "fire"), "x",
                List.of("sunfall"), List.of("recall", "recall", "solar_lance"),
                new Loadout("sunfall", "recall", "solar_lance")));
    }

    @Test
    void theTwoDefaultActivesMustDiffer() {
        assertThrows(IllegalArgumentException.class, () -> new Loadout("sunfall", "recall", "recall"));
    }

    @Test
    void aLoadoutMapsEachSlotToItsAbility() {
        var loadout = new Loadout("sunfall", "recall", "solar_lance");
        assertEquals("recall", loadout.idFor(LoadoutSlot.ACTIVE_1));
        assertEquals("solar_lance", loadout.idFor(LoadoutSlot.ACTIVE_2));
        assertEquals("sunfall", loadout.idFor(LoadoutSlot.ULTIMATE));
        assertEquals(List.of("sunfall", "recall", "solar_lance"), loadout.ids());
    }
}
