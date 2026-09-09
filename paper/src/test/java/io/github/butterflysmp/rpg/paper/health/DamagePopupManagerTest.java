package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pure show-gate: a number is drawn only for player-dealt DAMAGE with a known dealer. Everything
 * else -- heals, max changes, unattributed or non-player dealers -- draws nothing this pass. The wire
 * format is boot-witnessed; this gate is the one piece of the manager that is unit-testable.
 */
class DamagePopupManagerTest {

    private static HealthChange change(HealthChange.Kind kind, UUID dealer, boolean dealerIsPlayer) {
        return new HealthChange(UUID.randomUUID(), false, kind, 12.0, dealer, dealerIsPlayer, 88, 100, false);
    }

    @Test
    void showsForPlayerDealtDamage() {
        assertTrue(DamagePopupManager.shouldShow(change(HealthChange.Kind.DAMAGE, UUID.randomUUID(), true)),
                "player-dealt damage with a known dealer -> show");
    }

    @Test
    void hidesHealAndMaxChange() {
        UUID dealer = UUID.randomUUID();
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.HEAL, dealer, true)),
                "no heal numbers this pass");
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.MAX_CHANGE, dealer, true)),
                "a max change is not a hit");
        // Mutation: drop the kind == DAMAGE clause -> a heal pops a number -> reddens.
    }

    @Test
    void hidesNonPlayerAndUnattributedDealers() {
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.DAMAGE, UUID.randomUUID(), false)),
                "a mob dealer shows nothing until the mob->player pass");
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.DAMAGE, null, true)),
                "no dealer UUID -> no screen to draw on (null-safe)");
        // Mutation: drop the dealerIsPlayer or dealer != null clause -> reddens (and the null case NPEs
        // the manager at Bukkit.getPlayer(null) in the field).
    }

    @Test
    void jitterMapsUnitRandomToACenteredHorizontalOffset() {
        assertEquals(-0.3, DamagePopupManager.jitter(0.0), 1e-9, "0.0 -> the left edge, -HORIZONTAL_JITTER");
        assertEquals(0.0, DamagePopupManager.jitter(0.5), 1e-9, "0.5 -> dead centre, no offset");
        assertEquals(0.3, DamagePopupManager.jitter(1.0), 1e-9,
                "1.0 -> the right edge (nextDouble() never returns 1.0, but the map is symmetric)");
        double near = DamagePopupManager.jitter(0.999999);
        assertTrue(near > -0.3 && near < 0.3, "every real draw lands within [-0.3, 0.3), centred on the target");
        // Mutation: drop the -0.5 centering -> [0, 0.6) (off-centre); drop the *2 -> range halves -> reddens.
    }

    // --- symbolFor: the element's mark, or nothing, and NEVER a throw ---------------------------

    private static ElementRegistry registryWith(ElementDefinition... defs) {
        var registry = new ElementRegistry();
        for (ElementDefinition d : defs) registry.register(d);
        return registry;
    }

    @Test
    void anElementsGlyphIsTheONEHeldByTheRegistryRatherThanACopyOfIt() {
        // Read straight through -- no cached map. The registry already holds it PARSED
        // (ElementDefinition.damageSymbol is a Component, not a string), so a map would be a second
        // copy of the same data defended by a cost an earlier commit in this slice removed. Asserting
        // IDENTITY, not equality, is what pins that: an equal-but-distinct Component would pass an
        // equals() check and prove nothing about where it came from.
        Component mark = Component.text("*", NamedTextColor.GOLD);
        var elements = registryWith(new ElementDefinition("fire", "fire", mark, "scorch"));

        assertSame(mark, DamagePopupManager.symbolFor(elements, "fire"),
                "the registry's own Component, not a copy");
        // Mutation: rebuild the Component here (or re-deserialize the display name) -> a different
        // instance -> reddens, and the popup would be parsing on a per-hit region-thread path.
    }

    @Test
    void everyUNKNOWNShapeYieldsNoGlyphAndNoThrow() {
        // Three ways to have no mark, and all three must fail SOFT: a cosmetic lookup may never cost
        // the number itself. Boot validation has already NAMED a dangling element and a missing
        // damage_symbol, so a hit is not where either is discovered.
        var elements = registryWith(
                new ElementDefinition("fire", "fire", Component.text("*"), "scorch"),
                new ElementDefinition("kinetic", "kinetic"));   // loaded, but declares no glyph

        assertNull(DamagePopupManager.symbolFor(elements, null),
                "no element at all -- a thorns reflect, fall damage, scorch's own burn tick");
        assertNull(DamagePopupManager.symbolFor(elements, "plazma"), "an element nothing defines");
        assertNull(DamagePopupManager.symbolFor(elements, "kinetic"),
                "an element that loaded but declares no glyph -- six of the seven shipped ones today");
        // There is no null-id guard to mutate: find(null) falls out through the registry MISS, so a
        // check would be indistinguishable from it -- the same dead guard declined in ElementAccrual.
        // Mutation: throw on a registry miss instead of returning null -> all three redden, and one
        // typo in one yml would stop every damage number in the game.
    }
}
