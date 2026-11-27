package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.enchant.Growth;
import io.github.butterflysmp.rpg.core.enchant.ManaBank;
import io.github.butterflysmp.rpg.core.enchant.Protection;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.progression.LevelBonus;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY FIGURE GATE-true-tooltips PREDICTS, EXECUTED with the inputs its rows stage (2026-11-26). A number in the gate
 * that is not asserted here is not a prediction. The PLAYERHIT figures go through the engine's own sum
 * ({@code HitDamage.hitBase}, {@code HitDamage.dealt}) and {@code TracedHit}'s format ({@code %.3f}); the tooltip figures
 * through the shipped renderer at the row's staged state.
 */
class TrueTooltipsGateArithmeticTest {

    private static String sent(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    /** The PLAYERHIT a row predicts: the item term, plus level 1's bonus and a class term of 0, at charge 1, crit 1 or 2. */
    private static List<String> hit(double itemTerm) {
        double level = LevelBonus.at(1).weaponDamage();
        double base = HitDamage.hitBase(itemTerm, 0.0, 0.0 + level);
        return List.of(sent(HitDamage.dealt(base, 1.0, 1.0)), sent(HitDamage.dealt(base, 1.0, 2.0)));
    }

    @Test
    void theLevelAndTheMobAreAsStaged() {
        assertEquals(0.0, LevelBonus.at(1).weaponDamage(), "level 1 adds no weapon damage (setup stages level 1)");
        assertEquals(1000.0, MobScaling.maxHealth(200, false, true, 100), "a max_health 200 zombie at score 100");
    }

    @Test
    void theTruthRowsPlayerHits() {
        // TT9 Large Sparks III, flint_staff, score 100: (20 + 12) x 100/100, no damage percent.
        assertEquals(List.of("32.000", "64.000"), hit(GearScore.scaledDamage(20 + 12, 100)));
        // TT10 Sharpness III, iron_sword, score 100: 23 x 100/100 x 1.15.
        assertEquals(List.of("26.450", "52.900"), hit(HitDamage.hitBase(GearScore.scaledDamage(23, 100), 15, 0)));
        // TT11 Echo Beam III, cursed_emerald, score 100: each of the six shots 27.
        assertEquals(List.of("27.000", "54.000"), hit(GearScore.scaledDamage(27, 100)));
        // TT12 Focused Beam II, cursed_emerald, score 100: one beam, 162.
        assertEquals(List.of("162.000", "324.000"), hit(GearScore.scaledDamage(162, 100)));
    }

    @Test
    void theTooltipFiguresTheRowsPredict() {
        TrueTooltipsConsistencyTest.Content c = TrueTooltipsConsistencyTest.load();
        // Every weapon line at the row's staged level and score, from the shipped renderer.
        assertEquals("Magic Damage: 32", line(c, "flint_staff", "large_sparks", 3, 100, "Magic Damage"));
        assertEquals("Magic Damage: 24", line(c, "flint_staff", "large_sparks", 1, 100, "Magic Damage"));
        assertEquals("Magic Damage: 64", line(c, "flint_staff", "large_sparks", 3, 200, "Magic Damage"));
        assertEquals("Magic Damage: 25", line(c, "crystal_shard_staff", "sharp_crystals", 3, 100, "Magic Damage"));
        assertEquals("Magic Damage: 19", line(c, "crystal_shard_staff", "sharp_crystals", 1, 100, "Magic Damage"));
        assertEquals("Melee Damage: 26.45", line(c, "iron_sword", "sharpness", 3, 100, "Melee Damage"));
        assertEquals("Melee Damage: 24.15", line(c, "iron_sword", "sharpness", 1, 100, "Melee Damage"));
        assertEquals("Melee Damage: 52.9", line(c, "iron_sword", "sharpness", 3, 200, "Melee Damage"));
        assertEquals("Magic Damage: 27 x 6", line(c, "cursed_emerald", "echo_beam", 3, 100, "Magic Damage"));
        assertEquals("Magic Damage: 27 x 4", line(c, "cursed_emerald", "echo_beam", 1, 100, "Magic Damage"));
        assertEquals("Crit Chance: +25%", line(c, "broken_bottle", "sharpened_glass", 3, 100, "Crit Chance"));
        assertEquals("Crit Chance: +15%", line(c, "broken_bottle", "sharpened_glass", 1, 100, "Crit Chance"));
        // The armor figures, by the reconcile's own functions.
        assertEquals(25.0, Protection.effectiveDefense(GearScore.scaledDefense(16, 100), 9));
        assertEquals(19.0, Protection.effectiveDefense(GearScore.scaledDefense(16, 100), 3));
        assertEquals(30.0, Growth.contribution(30));
        assertEquals(30.0, ManaBank.contribution(30));
        ArmorDefinition helmet = c.armor().stream().filter(a -> a.id().equals("iron_helmet")).findFirst().orElseThrow();
        List<String> grown = TrueTooltipsConsistencyTest.plain(ArmorItems.statLore(helmet,
                TrueTooltipsConsistencyTest.activeAt("growth", 3), c.enchants(), OptionalInt.of(100)));
        assertTrue(grown.contains("Health: +30"), grown.toString());
        List<String> banked = TrueTooltipsConsistencyTest.plain(ArmorItems.statLore(helmet,
                TrueTooltipsConsistencyTest.activeAt("mana_bank", 1), c.enchants(), OptionalInt.of(100)));
        assertTrue(banked.contains("Mana: +10"), banked.toString());
        assertEquals("14.7", WeaponLoreLines.damageNumber(21 * 70.0 / 100.0), "the crystal's shot at III");
        // TT5/TT13's dumps: an iron chestplate at score 100 with Protection III, its stat block's first two lines.
        ArmorDefinition chest = c.armor().stream().filter(a -> a.id().equals("iron_chestplate")).findFirst().orElseThrow();
        List<String> plate = TrueTooltipsConsistencyTest.plain(ArmorItems.statLore(chest,
                TrueTooltipsConsistencyTest.activeAt("protection", 3), c.enchants(), OptionalInt.of(100)));
        assertEquals(List.of("Gear Score: 100", "Defense: 25"), plate.subList(0, 2), plate.toString());
        List<String> helm = TrueTooltipsConsistencyTest.plain(ArmorItems.statLore(helmet,
                TrueTooltipsConsistencyTest.activeAt("growth", 3), c.enchants(), OptionalInt.of(100)));
        assertEquals(List.of("Gear Score: 100", "Defense: 10", "Health: +30"), helm.subList(0, 3), helm.toString());
    }

    private static String line(TrueTooltipsConsistencyTest.Content c, String weapon, String enchant, int level, int score,
                               String label) {
        WeaponDefinition w = c.weapons().stream().filter(x -> x.id().equals(weapon)).findFirst().orElseThrow();
        var state = TrueTooltipsConsistencyTest.activeAt(enchant, level);
        return TrueTooltipsConsistencyTest.plain(WeaponLore.build(w, c.elements(), OptionalInt.empty(), OptionalInt.empty(),
                        OptionalInt.of(score), ItemEnchantTermsReader.of(state, w, c.enchants()))).stream()
                .filter(l -> l.startsWith(label)).findFirst().orElse("(no " + label + " line)");
    }
}
