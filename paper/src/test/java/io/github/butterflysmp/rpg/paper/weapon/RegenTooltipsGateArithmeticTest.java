package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.paper.content.ArmorLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code docs/gates/GATE-regen-tooltips.md}: armor-sets.AS1's and AS2's eight tooltips, WHOLE, as their DUMPs will read
 * them under the 2026-11-27 form (bare, score 100: the rows stamp {@code rpg gearscore set 100} before each dump).
 * EXECUTED here, then pinned; the gate quotes these. MB1's four are {@code ManaBatteryGateArithmeticTest.EXPECTED_MB1}.
 */
class RegenTooltipsGateArithmeticTest {

    private static Logger quiet() {
        Logger log = Logger.getLogger("RegenTooltipsGateArithmeticTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        return log;
    }

    private static EnchantRegistry enchants() {
        var weapons = new WeaponLoader(quiet()).loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        return EnchantLoader.withWeapons(quiet(), Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither")::contains,
                weapons::find, false).loadAll(Path.of("src", "main", "resources", "content", "enchants").toFile());
    }

    private static List<String> tooltip(String piece, EnchantRegistry r) {
        ArmorDefinition a = new ArmorLoader(quiet()).loadAll(Path.of("src", "main", "resources", "content", "armor").toFile())
                .find(piece).orElseThrow();
        return TrueTooltipsConsistencyTest.plain(EnchantLore.applied(
                ArmorItems.statLore(a, EnchantState.empty(), r, OptionalInt.of(100)), EnchantLore.lines(EnchantState.empty(), r)));
    }

    @Test
    void as1AndAs2_theEightTooltipsWhole() {
        EnchantRegistry r = enchants();
        List<String> out = new ArrayList<>();
        for (String tier : List.of("leather", "golden")) {
            for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                String piece = tier + "_" + slot;
                out.add(piece + " " + tooltip(piece, r));
            }
        }
        out.forEach(l -> System.out.println("RT " + l));
        assertEquals(EXPECTED, out);
    }

    /** EXECUTED by this test (2026-11-27), then pinned; GATE-regen-tooltips.md RT1 and RT2 quote these. */
    static final List<String> EXPECTED = List.of(
            "leather_helmet [Gear Score: 100, Defense: 5, , Set Bonus (4 pieces):, +2 Health Regen/5s, ,"
                    + " Better than nothing, and it knows it., , Common Helmet]",
            "leather_chestplate [Gear Score: 100, Defense: 8, , Set Bonus (4 pieces):, +2 Health Regen/5s, ,"
                    + " Better than nothing, and it knows it., , Common Chestplate]",
            "leather_leggings [Gear Score: 100, Defense: 7, , Set Bonus (4 pieces):, +2 Health Regen/5s, ,"
                    + " Better than nothing, and it knows it., , Common Leggings]",
            "leather_boots [Gear Score: 100, Defense: 4, , Set Bonus (4 pieces):, +2 Health Regen/5s, ,"
                    + " Better than nothing, and it knows it., , Common Boots]",
            "golden_helmet [Gear Score: 100, Defense: 5, , Set Bonus (4 pieces):, +10 Mana Regen/5s, ,"
                    + " It buys goodwill from things that cannot spend it., , Common Helmet]",
            "golden_chestplate [Gear Score: 100, Defense: 8, , Set Bonus (4 pieces):, +10 Mana Regen/5s, ,"
                    + " It buys goodwill from things that cannot spend it., , Common Chestplate]",
            "golden_leggings [Gear Score: 100, Defense: 7, , Set Bonus (4 pieces):, +10 Mana Regen/5s, ,"
                    + " It buys goodwill from things that cannot spend it., , Common Leggings]",
            "golden_boots [Gear Score: 100, Defense: 4, , Set Bonus (4 pieces):, +10 Mana Regen/5s, ,"
                    + " It buys goodwill from things that cannot spend it., , Common Boots]");
}
