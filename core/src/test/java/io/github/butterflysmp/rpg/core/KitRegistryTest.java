package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.kit.KitRegistry;
import io.github.butterflysmp.rpg.core.kit.WeaponGrant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KitRegistryTest {

    private static KitDefinition rangerFire() {
        return new KitDefinition("ranger", "fire", "Fire Ranger",
                List.of(new WeaponGrant("hunters_bow", true)), List.of("arc_surge"));
    }

    @Test
    void registeredKitIsFoundByItsCompositeKey() {
        var registry = new KitRegistry();
        registry.register(rangerFire());

        assertEquals(1, registry.size());
        assertEquals("Fire Ranger", registry.find("ranger", "fire").orElseThrow().displayName());
        assertTrue(registry.find("ranger", "void").isEmpty(), "wrong element -> no kit");
        assertTrue(registry.find("mage", "fire").isEmpty(), "wrong class -> no kit");
    }

    @Test
    void duplicateKeyIsRejected() {
        var registry = new KitRegistry();
        registry.register(rangerFire());
        assertThrows(IllegalStateException.class, () -> registry.register(rangerFire()));
    }

    /**
     * The whole reason the key is a composite record, not a concatenated string:
     * ("ranger_f","ire") and ("ranger","fire") would collide as "ranger_fire". They must
     * be two distinct, independently-findable kits.
     */
    @Test
    void aCompositeKeyDoesNotCollideTheWayConcatenationWould() {
        var registry = new KitRegistry();
        registry.register(new KitDefinition("ranger_f", "ire", "A", List.of(), List.of("a")));
        registry.register(new KitDefinition("ranger", "fire", "B", List.of(), List.of("b")));

        assertEquals(2, registry.size());
        assertEquals("A", registry.find("ranger_f", "ire").orElseThrow().displayName());
        assertEquals("B", registry.find("ranger", "fire").orElseThrow().displayName());
    }

    @Test
    void classesListsTheDistinctClassesOffered() {
        var registry = new KitRegistry();
        registry.register(new KitDefinition("ranger", "fire", "A", List.of(), List.of("a")));
        registry.register(new KitDefinition("ranger", "void", "B", List.of(), List.of("b")));
        registry.register(new KitDefinition("mage", "fire", "C", List.of(), List.of("c")));

        assertEquals(java.util.Set.of("ranger", "mage"), registry.classes());
    }

    @Test
    void aKitThatGrantsNothingIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new KitDefinition("ghost", "fire", "Ghost", List.of(), List.of()));
    }
}
