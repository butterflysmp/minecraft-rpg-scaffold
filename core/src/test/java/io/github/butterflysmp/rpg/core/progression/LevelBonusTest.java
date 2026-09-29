package io.github.butterflysmp.rpg.core.progression;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ben's level bonuses (2026-09-29), every rung 1..50 (seat ruling L3).
 *
 * <p><b>The grid is COUNTED FROM BEN'S WORDS, never derived through {@link LevelBonus}.</b> Each expected
 * value below is a count of level-ups, or of milestone levels, written as a list and walked. A test that
 * re-derived the formula would agree with a mutated one. The rungs Ben named are also pinned as
 * literals, because the counting loop and the function could share a misreading.
 */
class LevelBonusTest {

    /** "every 5 levels ... starting at level 10": 10, 15, ..., 50. Nine. */
    private static final List<Integer> REGEN_LEVELS = List.of(10, 15, 20, 25, 30, 35, 40, 45, 50);
    /** "every 5 levels starting at level 5": 5, 10, ..., 50. Ten. */
    private static final List<Integer> DEFENSE_LEVELS = List.of(5, 10, 15, 20, 25, 30, 35, 40, 45, 50);
    /** "every 10 levels starting at level 10": 10, 20, ..., 50. Five. */
    private static final List<Integer> CRIT_LEVELS = List.of(10, 20, 30, 40, 50);

    private static long reached(List<Integer> milestones, int level) {
        return milestones.stream().filter(m -> m <= level).count();
    }

    /** Ben: "every level gives +5 and only the level 49 -> 50 gives +10". Level-ups are 2..level. */
    private static double healthCountedByLevelUp(int level) {
        double total = 0;
        for (int levelUp = 2; levelUp <= level; levelUp++) {
            total += levelUp == 50 ? 10 : 5;
        }
        return total;
    }

    @Test
    void theWholeGridOneToFiftyMatchesBensWords() {
        for (int level = 1; level <= PlayerLevel.ACTIVE_CAP; level++) {
            LevelBonus bonus = LevelBonus.at(level);
            String at = "level " + level;
            assertEquals(level, bonus.level(), at);
            assertEquals(healthCountedByLevelUp(level), bonus.maxHealth(), at + " max health");
            assertEquals(level - 1, bonus.weaponDamage(), at + " weapon damage: +1 per level-up");
            assertEquals(reached(REGEN_LEVELS, level) / 5.0, bonus.healthRegenPerSecond(), at + " regen");
            assertEquals(reached(DEFENSE_LEVELS, level), bonus.defense(), at + " defense");
            assertEquals(reached(CRIT_LEVELS, level) / 100.0, bonus.critChance(), at + " crit chance");
        }
    }

    @Test
    void theRungsBenNamedArePinnedAsLiterals() {
        assertEquals(0.0, LevelBonus.at(1).maxHealth(), "level 1 grants nothing");
        assertEquals(5.0, LevelBonus.at(2).maxHealth());
        assertEquals(240.0, LevelBonus.at(49).maxHealth(), "48 level-ups of +5");
        assertEquals(250.0, LevelBonus.at(50).maxHealth(), "Ben: 'for a total of +250'");
        assertEquals(10.0, LevelBonus.at(50).maxHealth() - LevelBonus.at(49).maxHealth(), "49 -> 50 pays +10");

        assertEquals(0.0, LevelBonus.at(1).weaponDamage());
        assertEquals(49.0, LevelBonus.at(50).weaponDamage(), "Ben: 'Yes' to +49 at 50");

        assertEquals(0.0, LevelBonus.at(9).healthRegenPerSecond());
        assertEquals(0.2, LevelBonus.at(10).healthRegenPerSecond(), "+1 HP per 5 seconds, from level 10");
        assertEquals(0.2, LevelBonus.at(14).healthRegenPerSecond());
        assertEquals(1.8, LevelBonus.at(50).healthRegenPerSecond(), "nine steps");

        assertEquals(0.0, LevelBonus.at(4).defense());
        assertEquals(1.0, LevelBonus.at(5).defense(), "from level 5");
        assertEquals(10.0, LevelBonus.at(50).defense());

        assertEquals(0.0, LevelBonus.at(9).critChance());
        assertEquals(0.01, LevelBonus.at(10).critChance(), "one percentage point");
        assertEquals(0.05, LevelBonus.at(50).critChance(), "Ben: '+5% at level 50'");
    }

    /**
     * Ben: "+1 per 5 seconds for a total of +10 health per 5 seconds". The base is 1 per 5 seconds, so the
     * sheet reads 10 at 50 only if the base and the nine steps sum to exactly 2.0 per second.
     */
    @Test
    void theSheetTotalsAtFiftyAreBensTotals() {
        LevelBonus top = LevelBonus.at(50);
        assertEquals(2.0, HealthRegen.BASE_PER_SECOND + top.healthRegenPerSecond(), "10 HP per 5 seconds");
        assertEquals(0.2, Crit.BASE_CHANCE + top.critChance(), "15% base + 5%");
    }

    @Test
    void aLevelOutsideOneToTheActiveCapIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LevelBonus.at(0));
        assertThrows(IllegalArgumentException.class, () -> LevelBonus.at(-1));
        assertThrows(IllegalArgumentException.class, () -> LevelBonus.at(PlayerLevel.ACTIVE_CAP + 1),
                "above the cap is Ben's call (Q-L15), so it is refused, not extrapolated");
    }

    /** Past the cap the XP keeps counting, and the bonus stays the level-50 bonus. */
    @Test
    void lifetimeXpPastTheCapGivesTheCapsBonus() {
        long level60 = PlayerLevel.totalForLevel(60);
        assertEquals(60, PlayerLevel.levelFor(level60), "the curve still reads 60");
        assertEquals(LevelBonus.at(50), LevelBonus.forLifetimeXp(level60));
        assertEquals(LevelBonus.at(1), LevelBonus.forLifetimeXp(0));
    }

    @Test
    void sourcesCarryOneLevelKeyPerStatAndNothingAtLevelOne() {
        LevelBonus top = LevelBonus.at(50);
        assertEquals(Map.of("level", 250.0), top.sources(AccessoryStat.MAX_HEALTH));
        assertEquals(Map.of("level", 1.8), top.sources(AccessoryStat.HEALTH_REGEN));
        assertEquals(Map.of("level", 10.0), top.sources(AccessoryStat.DEFENSE));
        assertEquals(Map.of("level", 0.05), top.sources(AccessoryStat.CRIT_CHANCE));
        for (AccessoryStat notGranted : List.of(AccessoryStat.MAX_MANA, AccessoryStat.MANA_REGEN,
                AccessoryStat.CRIT_DAMAGE, AccessoryStat.CLASS_DAMAGE)) {
            assertTrue(top.sources(notGranted).isEmpty(), notGranted + " is not a level bonus");
        }
        for (AccessoryStat stat : AccessoryStat.values()) {
            assertTrue(LevelBonus.at(1).sources(stat).isEmpty(), "level 1 carries no modifier for " + stat);
        }
        assertEquals(Map.of("level", 49.0), top.weaponDamageSources());
        assertTrue(LevelBonus.at(1).weaponDamageSources().isEmpty(), "level 1 carries no damage modifier");
    }
}
