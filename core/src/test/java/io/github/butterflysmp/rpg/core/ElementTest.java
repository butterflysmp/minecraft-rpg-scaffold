package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.element.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PROJECT.md keeps the elemental matrix in code rather than config so that it is
 * "unit-testable and typo-proof". It was neither until this class existed: nothing
 * tested multiplierAgainst directly, and nothing tested how a name becomes an Element.
 */
class ElementTest {

    /** Loop over values() so a newly added element cannot be quietly forgotten here. */
    @Test
    void everyElementResolvesFromItsOwnName() {
        for (Element element : Element.values()) {
            assertEquals(element, Element.fromName(element.name()));
        }
    }

    /**
     * The bug this method exists for. AbilityLoader upper-cases before resolving, so
     * `element: solar` has always worked in YAML. BukkitCombatant read the same enum
     * out of an entity's persistent data and did not, so `solar` threw.
     */
    @Test
    void resolutionIsCaseInsensitive() {
        assertEquals(Element.SOLAR, Element.fromName("solar"));
        assertEquals(Element.SOLAR, Element.fromName("SoLaR"));
        assertEquals(Element.VOID, Element.fromName("void"));
    }

    /**
     * Null, not a throw. This runs on the damage path, where an exception would abort
     * the remaining targets of a burst and stop a lingering area rescheduling itself.
     */
    @Test
    void anUnknownNameIsNullAndNeverThrows() {
        assertNull(assertDoesNotThrow(() -> Element.fromName("sloar")));
        assertNull(assertDoesNotThrow(() -> Element.fromName("")));
        assertNull(assertDoesNotThrow(() -> Element.fromName("   ")));
        assertNull(assertDoesNotThrow(() -> Element.fromName(null)));
    }

    /** A name that is close, but is not an element, is still not an element. */
    @Test
    void partialNamesDoNotResolve() {
        assertNull(Element.fromName("sol"));
        assertNull(Element.fromName("solar "));
        assertNull(Element.fromName("solarium"));
    }

    @Test
    void matchingShieldTakesAmplifiedDamage() {
        assertEquals(1.5, Element.SOLAR.multiplierAgainst(Element.SOLAR), 1e-9);
        assertEquals(1.5, Element.VOID.multiplierAgainst(Element.VOID), 1e-9);
    }

    @Test
    void differingShieldTakesNormalDamage() {
        assertEquals(1.0, Element.SOLAR.multiplierAgainst(Element.VOID), 1e-9);
        assertEquals(1.0, Element.ARC.multiplierAgainst(Element.STASIS), 1e-9);
    }

    /**
     * The path a garbage shield tag now takes: unrecognised resolves to null, and an
     * unshielded defender takes normal damage.
     */
    @Test
    void anUnshieldedDefenderTakesNormalDamage() {
        for (Element attacker : Element.values()) {
            assertEquals(1.0, attacker.multiplierAgainst(null), 1e-9);
        }
        assertEquals(1.0, Element.SOLAR.multiplierAgainst(Element.fromName("nonsense")), 1e-9);
    }
}
