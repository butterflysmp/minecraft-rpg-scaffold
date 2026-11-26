package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * K2's tooltip half: a weapon that grants crit chance says so in its stat block, as a whole percent with its sign (SEAT
 * PLACEHOLDER 2026-11-25 wording, open for Ben). A weapon that grants none prints no line. Mutations named per test.
 */
class WeaponCritLoreLineTest {

    /** Mutation: the percent conversion dropped (prints "+0%" or "+0.1"), or the sign lost. */
    @Test
    void aChanceIsAWholeSignedPercent() {
        assertEquals("+10%", WeaponLoreLines.critChanceLabel(0.10));
        assertEquals("+5%", WeaponLoreLines.critChanceLabel(0.05));
        assertEquals("+100%", WeaponLoreLines.critChanceLabel(1.0));
    }

    /** Mutation: the absent guard dropped, so every sword grows a "+0%" line. */
    @Test
    void noChanceIsNoLine() {
        assertEquals("", WeaponLoreLines.critChanceLabel(WeaponDefinition.NO_CRIT));
    }
}
