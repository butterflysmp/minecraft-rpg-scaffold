package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.archetype.Archetype;
import io.github.butterflysmp.rpg.core.archetype.ArchetypeRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchetypeRegistryTest {

    private static Archetype hunter() {
        return new Archetype("hunter", "Hunter", List.of("solar_grenade", "solar_lance"));
    }

    @Test
    void registeredArchetypeIsFoundById() {
        var registry = new ArchetypeRegistry();
        registry.register(hunter());

        assertEquals(1, registry.size());
        assertEquals(List.of("solar_grenade", "solar_lance"),
                registry.find("hunter").orElseThrow().abilityIds());
        assertTrue(registry.find("mage").isEmpty());
    }

    @Test
    void duplicateIdIsRejected() {
        var registry = new ArchetypeRegistry();
        registry.register(hunter());

        assertThrows(IllegalStateException.class, () -> registry.register(hunter()));
    }

    @Test
    void abilityIdsAreImmutableAfterConstruction() {
        var mutable = new ArrayList<>(List.of("solar_grenade"));
        var archetype = new Archetype("hunter", "Hunter", mutable);

        mutable.add("sneaked_in"); // mutating the source list must not leak in
        assertEquals(List.of("solar_grenade"), archetype.abilityIds());
        assertThrows(UnsupportedOperationException.class,
                () -> archetype.abilityIds().add("nope"));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Archetype(" ", "Hunter", List.of("x")));
    }
}
