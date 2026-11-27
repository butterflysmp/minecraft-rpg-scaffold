package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.paper.content.ArmorLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GATE-mana-battery's predictions, EXECUTED rather than written by hand: MB1's four tooltips, whole, as the piece's lore
 * is composed ({@code ArmorItems.applyLore}: the enchant block over the stat lore), and the ±3σ bands its four
 * {@code rollstats 500} readings are judged against (the memory rule: a ±1σ band reds a correct build one run in three).
 * The refill and the sheet's figures are core's {@code ManaBatteryRefillTest}.
 */
class ManaBatteryGateArithmeticTest {

    private static Logger quiet() {
        Logger log = Logger.getLogger("ManaBatteryGateArithmeticTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        return log;
    }

    private static EnchantRegistry enchants() {
        var weapons = new WeaponLoader(quiet()).loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        return EnchantLoader.withWeapons(quiet(), Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither")::contains,
                weapons::find, false).loadAll(Path.of("src", "main", "resources", "content", "enchants").toFile());
    }

    private static List<String> tooltip(String piece, EnchantState state, EnchantRegistry r) {
        ArmorDefinition a = new ArmorLoader(quiet()).loadAll(new File("src/main/resources/content/armor")).find(piece).orElseThrow();
        return TrueTooltipsConsistencyTest.plain(EnchantLore.applied(ArmorItems.statLore(a, state, r, OptionalInt.of(100)),
                EnchantLore.lines(state, r)));
    }

    /** MB1: Mana Battery III, active, at score 100, on each iron piece: the whole lore. */
    @Test
    void mb1_theFourTooltipsWhole() {
        EnchantRegistry r = enchants();
        EnchantState iii = TrueTooltipsConsistencyTest.activeAt("mana_battery", 3);
        List<String> out = new ArrayList<>();
        for (String piece : List.of("iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots")) {
            out.add(piece + " " + tooltip(piece, iii, r));
        }
        out.forEach(l -> System.out.println("MB1 " + l));
        assertEquals(EXPECTED_MB1, out);
    }

    /** EXECUTED by this test (2026-11-27), then pinned; GATE-mana-battery.md MB1 quoted the pre-U1 form ("Mana Regen: +3"); GATE-regen-tooltips.md re-predicts these. */
    static final List<String> EXPECTED_MB1 = List.of(
            "iron_helmet [Mana Battery III, , Gear Score: 100, Defense: 10, Mana Regen: +3/5s, , Set Bonus (4 pieces):,"
                    + " +100 Max Health, , Honest plate. It has no opinion about who wears it., , Common Helmet]",
            "iron_chestplate [Mana Battery III, , Gear Score: 100, Defense: 16, Mana Regen: +3/5s, , Set Bonus (4 pieces):,"
                    + " +100 Max Health, , Honest plate. It has no opinion about who wears it., , Common Chestplate]",
            "iron_leggings [Mana Battery III, , Gear Score: 100, Defense: 14, Mana Regen: +3/5s, , Set Bonus (4 pieces):,"
                    + " +100 Max Health, , Honest plate. It has no opinion about who wears it., , Common Leggings]",
            "iron_boots [Mana Battery III, , Gear Score: 100, Defense: 8, Mana Regen: +3/5s, , Set Bonus (4 pieces):,"
                    + " +100 Max Health, , Honest plate. It has no opinion about who wears it., , Common Boots]");

    /** MB1: each tier's count over 500 rolls of one slot: the cascade's true odds, mean ± 3σ, rounded inward. */
    @Test
    void mb1_theRollBands() {
        double[] p = {0.10, 0.40 * 0.90, 0.70 * 0.90 * 0.60, 1.0 * 0.90 * 0.60 * 0.30};   // 4, 3, 2, 1 candidates
        List<String> out = new ArrayList<>();
        int[] cand = {4, 3, 2, 1};
        for (int i = 0; i < 4; i++) {
            double mean = 500 * p[i];
            double sigma = Math.sqrt(500 * p[i] * (1 - p[i]));
            out.add(cand[i] + "=[" + (int) Math.ceil(mean - 3 * sigma) + "," + (int) Math.floor(mean + 3 * sigma) + "]");
        }
        System.out.println("MB1 BANDS " + out);
        assertEquals(EXPECTED_BANDS, out);
    }

    /** EXECUTED by this test, then pinned: true odds 10%, 36%, 37.8%, 16.2% (enchant_candidates.yml). */
    static final List<String> EXPECTED_BANDS = List.of("4=[30,70]", "3=[148,212]", "2=[157,221]", "1=[57,105]");
}
