package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.core.enchant.EnchantCurve;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSetBonus;
import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import io.github.butterflysmp.rpg.paper.content.ArmorLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantLoader;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE SEAT'S U3 (2026-11-27): <b>NO STRENGTH CHANGES.</b> Bringing every regen tooltip into the stats sheet's unit (U1)
 * moves what a player READS and nothing the engine DOES. Each engine value below was EXECUTED on this branch and pinned,
 * from the shipped content, through the same seam the reconcile uses; the same figures held on {@code 16713667}, where
 * the tooltips still read in the authored unit.
 *
 * <p>Alongside each strength, the tooltip line it now shows, so the pair reads as one fact: what the engine does, and
 * that the line says exactly that in the sheet's unit.
 */
class RegenTooltipsSheetUnitTest {

    private static Logger quiet() {
        Logger log = Logger.getLogger("RegenTooltipsSheetUnitTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        return log;
    }

    private static ArmorSetBonus regenBonusOf(String tier) {
        List<ArmorDefinition> pieces = new ArmorLoader(quiet())
                .loadAll(Path.of("src", "main", "resources", "content", "armor").toFile()).all().stream()
                .filter(a -> a.id().startsWith(tier + "_")).toList();
        assertEquals(4, pieces.size(), tier + ": four pieces");
        List<ArmorSetBonus> regen = pieces.getFirst().setBonus().stream()
                .filter(b -> b.type() == ArmorSetBonus.Type.HEALTH_REGEN || b.type() == ArmorSetBonus.Type.MANA_REGEN)
                .toList();
        assertEquals(1, regen.size(), tier + ": one regen bonus");
        return regen.getFirst();
    }

    /** Golden: authored 2 per SECOND (Ben: "this is every second"); the engine adds 2.0 per second; it reads +10/5s. */
    @Test
    void golden_isStill2PerSecond_andReads10Per5s() {
        ArmorSetBonus b = regenBonusOf("golden");
        assertEquals(ArmorSetBonus.Type.MANA_REGEN, b.type());
        assertEquals(2.0, b.value(), 0.0, "authored");
        assertEquals(2.0, AccessoryContributions.sourceValue(AccessoryStat.MANA_REGEN, b.engineValue()), 0.0,
                "the reconcile's per-second modifier, unchanged");
        assertEquals("+10 Mana Regen/5s", ArmorLoreLines.setBonusLine(b));
        // Mutation: engineValue divides MANA_REGEN by the window too -> 0.4 -> reddens (and the line reads +2).
    }

    /** Leather: authored 2 per FIVE seconds ("the every 5s number"); the engine adds 0.4 per second; it reads +2/5s. */
    @Test
    void leather_isStill0Point4PerSecond_andReads2Per5s() {
        ArmorSetBonus b = regenBonusOf("leather");
        assertEquals(ArmorSetBonus.Type.HEALTH_REGEN, b.type());
        assertEquals(2.0, b.value(), 0.0, "authored");
        assertEquals(0.4, AccessoryContributions.sourceValue(AccessoryStat.HEALTH_REGEN, b.engineValue()), 0.0,
                "the reconcile's per-second modifier, unchanged (2 / 5.0, executed: 0.4)");
        assertEquals(5.0, ArmorSetBonus.HEALTH_REGEN_AUTHORED_SECONDS, 0.0, "the sheet's window, the same 5");
        assertEquals("+2 Health Regen/5s", ArmorLoreLines.setBonusLine(b));
        // Mutation: HEALTH_REGEN_AUTHORED_SECONDS = 1 -> 2.0 per second -> reddens.
    }

    /**
     * Mana Battery: authored 1 / 2 / 3 per FIVE seconds; the scan's modifier ({@code ManaBatteryModifierItems}:
     * contribution of perSecondFromPerFiveSeconds) is 0.2 / 0.4 / 0.6 per second; each reads +N/5s, stat block and suffix.
     */
    @Test
    void manaBattery_isStill1To3Per5s_andReadsSo() {
        var weapons = new WeaponLoader(quiet()).loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        EnchantDefinition d = EnchantLoader.withWeapons(quiet(),
                        Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither")::contains, weapons::find, false)
                .loadAll(Path.of("src", "main", "resources", "content", "enchants").toFile())
                .find("mana_battery").orElseThrow();
        double[] perSecond = {0.2, 0.4, 0.6};
        for (int level = 1; level <= 3; level++) {
            double authored = EnchantCurve.valueAt(d.valueByLevel(), level);
            assertEquals(level, authored, 0.0, "authored, level " + level);
            double engine = ManaRegen.contribution(ManaRegen.perSecondFromPerFiveSeconds(authored));
            assertEquals(perSecond[level - 1], engine, 0.0, "the scan's per-second modifier, level " + level);
            assertEquals("+" + level + "/5s", ArmorLoreLines.manaBatteryValue(authored), "stat block, level " + level);
            assertEquals(" (+" + level + " Mana Regen/5s)",
                    EnchantEffectLine.of(d, level, io.github.butterflysmp.rpg.core.weapon.GearClass.ARMOR),
                    "suffix, level " + level);
        }
        assertEquals(5, StatsSheetLines.RATE_WINDOW_SECONDS, "the window both directions share");
        // Mutation: perSecondFromPerFiveSeconds multiplies -> 5.0 per second at I -> reddens.
    }
}
