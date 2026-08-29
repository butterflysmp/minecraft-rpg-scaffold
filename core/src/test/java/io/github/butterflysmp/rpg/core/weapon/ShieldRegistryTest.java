package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shield lookup. Small, and the two things worth pinning are the duplicate refusal and the
 * load ORDER -- the loader sorts its files so a boot is reproducible, and a registry backed by a
 * HashMap would throw that away silently.
 *
 * Each test names the mutation it forces red.
 */
class ShieldRegistryTest {

    private static ShieldDefinition shield(String id) {
        return new ShieldDefinition(id, "Name of " + id, Rarity.COMMON, "shield", 0.5, List.of());
    }

    @Test
    void aRegisteredShieldIsFoundByItsId() {
        ShieldRegistry registry = new ShieldRegistry();
        registry.register(shield("shield"));
        assertTrue(registry.find("shield").isPresent());
        assertEquals("Name of shield", registry.find("shield").orElseThrow().displayName());
        assertEquals(1, registry.size());
        // Mutation: key the map on displayName instead of id -> find("shield") is empty and
        // /rpg give can never mint it -> reddens.
    }

    @Test
    void anUnknownIdIsAnEmptyOptionalRatherThanNull() {
        // /rpg give and the enchant dispatch both branch on this. A null would be an NPE in paper,
        // where no test would catch it.
        assertTrue(new ShieldRegistry().find("nothing").isEmpty());
        // Mutation: return the raw map get -> null reaches the command and NPEs -> reddens.
    }

    @Test
    void aDuplicateIdIsRefusedRatherThanSilentlyOverwriting() {
        // Two files claiming one id means one of them is invisible, and which one would depend on
        // readdir order. Throwing turns it into a named, skipped file in the boot log.
        ShieldRegistry registry = new ShieldRegistry();
        registry.register(shield("shield"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> registry.register(shield("shield")));
        assertTrue(ex.getMessage().contains("shield"),
                "the refusal must name the id, got: " + ex.getMessage());
        assertEquals(1, registry.size(), "and the first registration must survive intact");
        // Mutation: use put instead of putIfAbsent -> the second file silently wins and the first
        // shield vanishes with no log line -> reddens.
    }

    @Test
    void iterationKeepsRegistrationOrderSoABootIsReproducible() {
        // The loader sorts its files precisely so two boots load in the same order. A HashMap here
        // would discard that, and the /rpg give suggestion list would shuffle between boots.
        ShieldRegistry registry = new ShieldRegistry();
        for (String id : new String[] {"zzz", "aaa", "mmm"}) registry.register(shield(id));
        assertEquals(List.of("zzz", "aaa", "mmm"),
                registry.all().stream().map(ShieldDefinition::id).toList());
        // Mutation: back it with a HashMap -> iteration comes back hash-ordered ("aaa","zzz","mmm")
        // -> reddens.
    }

    @Test
    void theViewIsUnmodifiableSoNothingCanEditTheRegistryThroughIt() {
        ShieldRegistry registry = new ShieldRegistry();
        registry.register(shield("shield"));
        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
        // Mutation: return byId.values() directly -> a caller can clear the registry at runtime
        // -> reddens.
    }
}
