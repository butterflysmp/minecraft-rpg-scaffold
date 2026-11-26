package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.combat.AttackSpeedAttribute;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.progression.LevelBonus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EVERY NUMBER docs/gates/GATE-broken-bottle.md QUOTES, EXECUTED with the inputs its rows stage (the S-1 discipline applied to
 * every prediction, not only the SAFETY sum, and the floating-point rule: never predicted). If one of these moves, the gate's
 * prediction moves with it, and both are edited together.
 */
class BrokenBottleGateArithmeticTest {

    /** BB3: sent = (18 x score / 100) x 1.0 (no damage enchant) + 0 (class, BASE) + 0 (level 1) ; x 1.0 charge ; x 1 or 2. */
    @Test
    void theHitAtOneHundredAndTwoHundred() {
        double level = LevelBonus.at(1).weaponDamage();
        assertEquals(0.0, level);
        assertEquals(18.0, GearScore.scaledDamage(18, 100) * 1.0 + 0.0 + level);
        assertEquals(36.0, GearScore.scaledDamage(18, 100) * (1.0 + Crit.BASE_DAMAGE), "the allowed crit reading at 100");
        assertEquals(36.0, GearScore.scaledDamage(18, 200) * 1.0 + 0.0 + level);
        assertEquals(72.0, GearScore.scaledDamage(18, 200) * (1.0 + Crit.BASE_DAMAGE), "the allowed crit reading at 200");
    }

    /** BB3 and BB6: a zombie summoned with max_health 200, seeded at the pad's score 100: its custom max. */
    @Test
    void theZombiesCustomMax() {
        assertEquals(1000.0, MobScaling.maxHealth(200.0, false, true, 100));
    }

    /** BB2: the player's attack-speed attribute while each weapon is held, at an attack-speed stat of 1.0. */
    @Test
    void theAttackSpeedAttribute() {
        assertEquals(2.0, AttackSpeedAttribute.VANILLA_BASE + AttackSpeedAttribute.modifier(2.0, 1.0));
        assertEquals(1.6, AttackSpeedAttribute.VANILLA_BASE + AttackSpeedAttribute.modifier(1.6, 1.0));
    }

    /** BB6's control: the iron sword's bystander takes half of what its primary took (the sweep's share, 0.5). */
    @Test
    void theSweepShare() {
        assertEquals(0.0, SweepShare.NONE);
        assertEquals(11.5, SweepShare.of(23.0, 0.5));
    }

    /** BB2 and BB4: the sheet's crit line at each offset from a 15% baseline. */
    @Test
    void theSheetLines() {
        assertEquals("15%", StatsSheetLines.critChance(Crit.BASE_CHANCE));
        assertEquals("25%", StatsSheetLines.critChance(Crit.BASE_CHANCE + 0.10));
        assertEquals("30%", StatsSheetLines.critChance(Crit.BASE_CHANCE + 0.10 + 0.05));
        assertEquals("40%", StatsSheetLines.critChance(Crit.BASE_CHANCE + 0.10 + 0.15));
    }
}
