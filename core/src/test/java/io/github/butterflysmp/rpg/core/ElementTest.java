package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.element.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Element carries no combat math -- it is pure identity now. All that remains to test is
 * how a name becomes an Element: the case-insensitive, null-on-miss resolution the loaders
 * lean on. (This whole enum becomes content in 2B; this file goes with it.)
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
}
