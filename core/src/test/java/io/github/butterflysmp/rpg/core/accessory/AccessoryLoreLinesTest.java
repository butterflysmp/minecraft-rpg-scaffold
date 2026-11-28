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
            // U1/U4 (the seat, 2026-11-27): the sheet's TOOLTIP form, perFiveSecondsBonus: the same x5 and suffix,
            // whole when whole. The shipped accessories are all non-whole, so their lines are byte-identical.
            assertEquals("+" + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSecondsBonus(0.1)
                            + " " + rate.label(),
                    AccessoryLoreLines.modifier(rate, 0.1, "x"), rate.token());
            assertEquals("-" + io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSecondsBonus(0.04)
                            + " " + rate.label(),
                    AccessoryLoreLines.modifier(rate, -0.04, "x"), rate.token());
            assertEquals("+1/5s " + rate.label(), AccessoryLoreLines.modifier(rate, 0.2, "x"),
                    "a whole amount prints whole, as every regen tooltip does; " + rate.token());
        }
        assertEquals("1.00/5s", io.github.butterflysmp.rpg.core.combat.StatsSheetLines.perFiveSeconds(0.2),
                "control: the sheet's own line keeps two decimals for the same rate");
        // Mutation: restore the per-second form (sign + plain(magnitude) + "/s ") -> reddens.
        // Mutation: back to perFiveSeconds -> the whole row reads "+1.00/5s" -> reddens.
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

    /**
     * H11 (SEAT PLACEHOLDER 2026-11-26 wording): the behaviour's lines, from the YAML's own numbers -- no number typed
     * twice. Predicted whole before the code was written.
     */
    @Test
    void theRadiantPulsesLines_fromItsOwnNumbers() {
        assertEquals(List.of("Radiant Pulse", "Every 4s, heals hurt players within 8 blocks", "for 1 health per your level"),
                AccessoryLoreLines.behaviour(AccessoryFixtures.radiantPulse()));
        AccessoryBehaviour.HealingPulse other = new AccessoryBehaviour.HealingPulse("Glow", 0.5, 30, 6.5,
                AccessoryBehaviour.Targets.PLAYERS, null, 0, null, null);
        assertEquals(List.of("Glow", "Every 1.5s, heals hurt players within 6.5 blocks", "for 0.5 health per your level"),
                AccessoryLoreLines.behaviour(other), "every number is read from the behaviour");
    }

    /** H11: the sheet line names the behaviour when active, and keeps the existing inactive wording. */
    @Test
    void theTomesSheetLine() {
        AccessoryDefinition tome = AccessoryFixtures.healingTome();
        assertEquals("Healing Tome: Radiant Pulse", AccessoryLoreLines.sheetLine("Healing Tome", tome, true, "Magic"));
        assertEquals("Healing Tome: inactive (requires Mage)", AccessoryLoreLines.sheetLine("Healing Tome", tome, false, "Magic"));
        assertEquals("Mage class accessory", AccessoryLoreLines.slotLine(tome));
        assertEquals(List.of(), AccessoryLoreLines.modifiers(tome, "Magic"), "it carries no modifier line");
    }

    /**
     * K13 (SEAT PLACEHOLDER 2026-11-26 wording): the Blink's lines, every number from the YAML. Predicted whole, from the
     * brief's quoted wording, before the code was written.
     */
    @Test
    void theBlinksLines_fromItsOwnNumbers() {
        assertEquals(List.of("Blink",
                        "When a hostile mob targeting you comes within 5 blocks,",
                        "you teleport to safety and turn invisible for 8s.",
                        "Attacking ends the invisibility.",
                        "Cooldown: 60s"),
                AccessoryLoreLines.behaviour(AccessoryFixtures.blink()));
        AccessoryBehaviour.Blink other = new AccessoryBehaviour.Blink("Shift", 3.5, 12, 4, 50, 90, null, 0, null);
        assertEquals(List.of("Shift",
                        "When a hostile mob targeting you comes within 3.5 blocks,",
                        "you teleport to safety and turn invisible for 2.5s.",
                        "Attacking ends the invisibility.",
                        "Cooldown: 4.5s"),
                AccessoryLoreLines.behaviour(other), "every number is read from the behaviour");
    }

    /** K13: the sheet line names the Blink when active. */
    @Test
    void theBlinkTomesSheetLine() {
        AccessoryDefinition tome = AccessoryFixtures.blinkTome();
        assertEquals("Blink Tome: Blink", AccessoryLoreLines.sheetLine("Blink Tome", tome, true, "Magic"));
        assertEquals("Blink Tome: inactive (requires Mage)", AccessoryLoreLines.sheetLine("Blink Tome", tome, false, "Magic"));
        assertEquals("Mage class accessory", AccessoryLoreLines.slotLine(tome));
    }
}
