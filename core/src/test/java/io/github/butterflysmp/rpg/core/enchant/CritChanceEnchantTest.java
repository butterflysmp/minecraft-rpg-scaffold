package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K9 (the seat's brief, 2026-11-25): {@code effect: crit_chance}, Sharpened Glass's. Its file authors WHOLE PERCENT
 * points ({@code value_by_level: [5, 10, 15]}, Ben's "+5/10/15% Crit Chance"); the stat is a probability, so the
 * contribution is the percent over 100. FLAT. Mutations named per test.
 */
class CritChanceEnchantTest {

    /** Mutation: the /100 dropped (a +15 PROBABILITY, every hit crits), or boosts made >=. */
    @Test
    void aPercentIsAProbabilityAndZeroIsNoSource() {
        assertEquals(0.15, CritChanceEnchant.contribution(15));
        assertEquals(0.05, CritChanceEnchant.contribution(5));
        assertTrue(CritChanceEnchant.boosts(5));
        assertFalse(CritChanceEnchant.boosts(CritChanceEnchant.NONE));
    }

    /**
     * K9 and K11, EXECUTED rather than predicted (floating point): the base, the bottle's 0.10 and the enchant's, summed
     * in the store's order (base first, then the sources) and printed as the sheet prints them.
     */
    @Test
    void theSheetReadsFifteenTwentyFiveThirtyAndForty() {
        double base = Crit.BASE_CHANCE;
        assertEquals("15%", StatsSheetLines.critChance(base));
        assertEquals("25%", StatsSheetLines.critChance(base + 0.10));
        assertEquals("30%", StatsSheetLines.critChance(base + 0.10 + CritChanceEnchant.contribution(5)));
        assertEquals("35%", StatsSheetLines.critChance(base + 0.10 + CritChanceEnchant.contribution(10)));
        assertEquals("40%", StatsSheetLines.critChance(base + 0.10 + CritChanceEnchant.contribution(15)));
    }

    /**
     * The gate stages LEVEL 1. EXECUTED here, not read off an old gate: level 1 grants no crit chance and no weapon damage,
     * so neither is a term of the gate's sheet rows or its hit rows. Level 2's control shows the function is live.
     */
    @Test
    void levelOneAddsNoCritAndNoWeaponDamage() {
        var one = io.github.butterflysmp.rpg.core.progression.LevelBonus.at(1);
        assertEquals(0.0, one.critChance());
        assertEquals(0.0, one.weaponDamage());
        assertTrue(io.github.butterflysmp.rpg.core.progression.LevelBonus.at(50).critChance() > 0.0, "control: the level does grant it");
    }
}
