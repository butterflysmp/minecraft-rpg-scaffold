package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The tooltip and stats-sheet text. Every shipped value is asserted as a string, because the numbers
 * are printed from the authored decimal and floating-point formatting is executed, never predicted.
 */
class AccessoryLoreLinesTest {

    /** The whole shipped roster's modifier text -- one cell per authored value. */
    @Test
    void everyShippedModifierPrintsAsAuthored() {
        assertEquals("+3 Defense", AccessoryLoreLines.modifier(AccessoryStat.DEFENSE, 3, "Ranged"));
        assertEquals("+5% Crit Chance", AccessoryLoreLines.modifier(AccessoryStat.CRIT_CHANCE, 0.05, "Ranged"));
        assertEquals("+0.50/5s Health Regen", AccessoryLoreLines.modifier(AccessoryStat.HEALTH_REGEN, 0.1, "Ranged"));
        assertEquals("+3 Melee Damage", AccessoryLoreLines.modifier(AccessoryStat.CLASS_DAMAGE, 3, "Melee"));
        assertEquals("+25% Crit Damage", AccessoryLoreLines.modifier(AccessoryStat.CRIT_DAMAGE, 0.25, "Melee"));
        assertEquals("-10 Max Mana", AccessoryLoreLines.modifier(AccessoryStat.MAX_MANA, -10, "Melee"));
        assertEquals("+3 Ranged Damage", AccessoryLoreLines.modifier(AccessoryStat.CLASS_DAMAGE, 3, "Ranged"));
        assertEquals("-0.20/5s Health Regen", AccessoryLoreLines.modifier(AccessoryStat.HEALTH_REGEN, -0.04, "Ranged"));
        assertEquals("+3 Magic Damage", AccessoryLoreLines.modifier(AccessoryStat.CLASS_DAMAGE, 3, "Magic"));
        assertEquals("+20 Max Mana", AccessoryLoreLines.modifier(AccessoryStat.MAX_MANA, 20, "Magic"));
        assertEquals("-3% Crit Chance", AccessoryLoreLines.modifier(AccessoryStat.CRIT_CHANCE, -0.03, "Magic"));
    }

    @Test
    void theRemainingStatsFormatToo() {
        assertEquals("+10 Max Health", AccessoryLoreLines.modifier(AccessoryStat.MAX_HEALTH, 10, "x"));
        assertEquals("+2.50/5s Mana Regen", AccessoryLoreLines.modifier(AccessoryStat.MANA_REGEN, 0.5, "x"));
        assertEquals("+2.5 Defense", AccessoryLoreLines.modifier(AccessoryStat.DEFENSE, 2.5, "x"));
        assertEquals("+12.5% Crit Chance", AccessoryLoreLines.modifier(AccessoryStat.CRIT_CHANCE, 0.125, "x"));
    }

    /**
     * *** ONE UNIT, ONE FORMATTER. The row the per-second-form mutation must redden. ***
     *
     * <p>An accessory's regen and the stats sheet's regen total are the same kind of number, so they
     * are printed by the same function in the same unit -- a player reading "-0.20/5s" on the Quiver
     * and "0.80/5s" on the sheet compares them without converting.
     */
    @Test
    void regenPrintsInTheStatsSheetsUnit_throughItsFormatter() {
        for (AccessoryStat rate : new AccessoryStat[] {AccessoryStat.HEALTH_REGEN, AccessoryStat.MANA_REGEN}) {
            assertEquals("+" + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSeconds(0.1)
                            + " " + rate.label(),
                    AccessoryLoreLines.modifier(rate, 0.1, "x"), rate.token());
            assertEquals("-" + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSeconds(0.04)
                            + " " + rate.label(),
                    AccessoryLoreLines.modifier(rate, -0.04, "x"), rate.token());
        }
        assertEquals("1.00/5s", io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSeconds(0.2),
                "control: the formatter is the one the sheet's '1.00/5s' base comes from");
        // Mutation: restore the per-second form (sign + plain(magnitude) + "/s ") -> reddens.
    }

    @Test
    void drawbacksComeLast() {
        AccessoryDefinition quiver = AccessoryFixtures.fletchersQuiver();
        assertEquals(List.of("+5% Crit Chance", "+3 Ranged Damage", "-0.20/5s Health Regen"),
                AccessoryLoreLines.modifiers(quiver, "Ranged"));
    }

    @Test
    void theSlotLineAndTheSheetLine() {
        assertEquals("Universal accessory", AccessoryLoreLines.slotLine(AccessoryFixtures.wardCharm()));
        assertEquals("Ranger class accessory", AccessoryLoreLines.slotLine(AccessoryFixtures.fletchersQuiver()));
        assertEquals("Ward Charm: +3 Defense", AccessoryLoreLines.sheetLine("Ward Charm",
                AccessoryFixtures.wardCharm(), true, "x"));
        assertEquals("Sage's Scroll: inactive (requires Mage)", AccessoryLoreLines.sheetLine("Sage's Scroll",
                AccessoryFixtures.sagesScroll(), false, "Magic"));
        assertEquals("Accessories", AccessoryLoreLines.SHEET_HEADER);
    }

    /** Ruling Q4: the sheet block, in the shape Ben approved -- header, then one line per slot. */
    @Test
    void theSheetBlock_activeInactiveUnreadableAndEmpty() {
        java.util.List<AccessoryDefinition> worn = java.util.Arrays.asList(
                AccessoryFixtures.sagesScroll(), AccessoryFixtures.wardCharm(), null, null);
        List<String> block = AccessoryLoreLines.sheetBlock(worn, List.of(true, true, true, false), "ranger",
                d -> d.id().equals("ward_charm") ? "Ward Charm" : "Sage's Scroll",
                d -> "Magic");
        assertEquals(List.of(
                "Accessories",
                "Sage's Scroll: inactive (requires Mage)",
                "Ward Charm: +3 Defense",
                "Slot 2: unreadable (kept, contributes nothing)"), block);

        assertEquals(List.of(), AccessoryLoreLines.sheetBlock(java.util.Arrays.asList(null, null, null, null),
                List.of(false, false, false, false), "ranger", d -> "", d -> ""),
                "nothing worn: no block at all, not a header over nothing");
    }

    @Test
    void theModifierOrderIsTheStatOrder_notTheAuthoredOrder() {
        AccessoryDefinition a = AccessoryFixtures.universal("a", Map.of(
                AccessoryStat.DEFENSE, 1.0, AccessoryStat.MAX_HEALTH, 2.0));
        assertEquals(List.of("+2 Max Health", "+1 Defense"), AccessoryLoreLines.modifiers(a, "x"),
                "stable across loads: a YAML map's order is the file's, the tooltip's is the enum's");
    }
}
