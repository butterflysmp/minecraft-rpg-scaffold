package io.github.butterflysmp.rpg.core.fight;

import io.github.butterflysmp.rpg.core.combat.CritState;
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
 * OWED FROM F1-F3 (PLAN-dragon-fight.md section 6.1): the ramp saturates at {@link Double#MAX_VALUE}, so the store must
 * take that hit with no infinity and no NaN anywhere. A strike reaches the store as {@code DefenseRule.BYPASSED} with
 * no element (the fixture's damage, step F5), so that is the path pinned here: the outcome, the change every listener
 * hears (the HURT line's and the death's source), and the health it leaves.
 */
class MaxValueStrikeTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Test
    void aMaxValueStrikeLeavesEveryFigureFiniteAndKills() {
        List<HealthChange> heard = new ArrayList<>();
        CombatantStats stats = new CombatantStats(heard::add);
        stats.register(PLAYER, CombatantStats.DEFAULT_PLAYER_BASE, true);
        // Armour on him, so a defence curve that ran would have something to work on.
        stats.reconcileDefenseModifiers(PLAYER, Map.of("armour", 40.0));
        heard.clear();

        double strike = StrikeRamp.damageForStrike(FightTestSettings.placeholders(), Integer.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, strike, 0.0);
        DamageOutcome outcome = stats.damage(PLAYER, strike, PLAYER, true, CritState.NORMAL, DefenseRule.BYPASSED, null);

        assertTrue(Double.isFinite(outcome.dealt()), "dealt " + outcome.dealt());
        assertTrue(Double.isFinite(outcome.newCurrent()), "current " + outcome.newCurrent());
        assertEquals(0.0, outcome.newCurrent(), 0.0, "floored at 0");
        assertEquals(0.0, stats.current(PLAYER), 0.0);
        assertEquals(1, heard.size());
        HealthChange c = heard.get(0);
        for (double v : new double[] {c.amount(), c.beforeDefense(), c.newCurrent(), c.max()}) {
            assertTrue(Double.isFinite(v), "a HealthChange figure is not finite: " + c);
        }
        assertEquals(Double.MAX_VALUE, c.amount(), 0.0, "BYPASSED: what landed is what was offered");
        assertEquals(Double.MAX_VALUE, c.beforeDefense(), 0.0);
        assertTrue(c.reachedZero(), "the death path is the reachedZero transition, which fires");
        assertEquals(HealthChange.Kind.DAMAGE, c.kind());
    }

    @Test
    void aSecondMaxValueStrikeOnADeadPlayerIsStillFinite() {
        List<HealthChange> heard = new ArrayList<>();
        CombatantStats stats = new CombatantStats(heard::add);
        stats.register(PLAYER, CombatantStats.DEFAULT_PLAYER_BASE, true);
        stats.damage(PLAYER, Double.MAX_VALUE, PLAYER, true, CritState.NORMAL, DefenseRule.BYPASSED, null);
        heard.clear();
        DamageOutcome again = stats.damage(PLAYER, Double.MAX_VALUE, PLAYER, true, CritState.NORMAL,
                DefenseRule.BYPASSED, null);
        assertTrue(Double.isFinite(again.newCurrent()));
        assertEquals(0.0, again.newCurrent(), 0.0);
        assertTrue(heard.isEmpty() || !heard.get(0).reachedZero(), "no second death transition");
    }
}
