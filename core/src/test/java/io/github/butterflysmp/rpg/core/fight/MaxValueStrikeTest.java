package io.github.butterflysmp.rpg.core.fight;

import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.Defense;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.DamageOutcome;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE RAMP'S HUGEST STRIKE THROUGH THE STORE. Since F6 + F7 a strike goes through ARMOUR (Ben, 2026-11-18: "yes",
 * {@code DefenseRule.APPLIES}, no element), so the ramp saturates at {@link StrikeRamp#CEILING_DAMAGE}, low enough that
 * the defence curve stays finite (PLAN-dragon-fight.md section 6.2). Pinned here on the path a strike now takes: the
 * outcome, the change every listener hears (the HURT line's and the death's source), and the health it leaves.
 */
class MaxValueStrikeTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static List<HealthChange> hitWith(double defence, double strike, DefenseRule rule, List<DamageOutcome> out) {
        List<HealthChange> heard = new ArrayList<>();
        CombatantStats stats = new CombatantStats(heard::add);
        stats.register(PLAYER, CombatantStats.DEFAULT_PLAYER_BASE, true);
        stats.reconcileDefenseModifiers(PLAYER, Map.of("armour", defence));
        heard.clear();
        out.add(stats.damage(PLAYER, strike, PLAYER, true, CritState.NORMAL, rule, null));
        return heard;
    }

    @Test
    void theHugestStrikeThroughArmourLeavesEveryFigureFiniteAndKills() {
        double strike = StrikeRamp.damageForStrike(FightTestSettings.placeholders(), Integer.MAX_VALUE);
        for (double defence : new double[] {0.0, 10.0, 40.0, 1e300}) {
            List<DamageOutcome> out = new ArrayList<>();
            List<HealthChange> heard = hitWith(defence, strike, DefenseRule.APPLIES, out);
            DamageOutcome outcome = out.get(0);
            assertTrue(Double.isFinite(outcome.dealt()), "defence " + defence + ": dealt " + outcome.dealt());
            assertEquals(0.0, outcome.newCurrent(), 0.0, "floored at 0");
            assertEquals(1, heard.size());
            HealthChange c = heard.get(0);
            for (double v : new double[] {c.amount(), c.beforeDefense(), c.newCurrent(), c.max()}) {
                assertTrue(Double.isFinite(v), "a HealthChange figure is not finite: " + c);
            }
            assertEquals(StrikeRamp.CEILING_DAMAGE, c.beforeDefense(), 0.0, "what was offered is the ceiling");
            assertTrue(c.reachedZero(), "the death path is the reachedZero transition, which fires");
        }
    }

    @Test
    void theFirstThreeStrikesThroughArmourAreTheRampsFiguresCutByTheCurve() {
        for (int k = 1; k <= 3; k++) {
            double strike = StrikeRamp.damageForStrike(FightTestSettings.placeholders(), k);
            List<DamageOutcome> out = new ArrayList<>();
            HealthChange c = hitWith(10.0, strike, DefenseRule.APPLIES, out).get(0);
            assertEquals(20.0 * Math.pow(2.0, k - 1), c.beforeDefense(), 0.0, "k=" + k + " before defence");
            assertEquals(Defense.applyDefense(c.beforeDefense(), 10.0), c.amount(), 0.0, "k=" + k + " after the curve");
        }
    }
}
