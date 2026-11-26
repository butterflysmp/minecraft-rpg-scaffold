package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.core.combat.AttackSpeedAttribute;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import io.github.butterflysmp.rpg.core.enchant.DrumBeats;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.progression.LevelBonus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-bone-club's figures, EXECUTED (2026-11-26): every number a row predicts that is not read straight off the server,
 * run through the shipped function with the row's staged inputs. The gate quotes this file.
 */
class BoneClubGateArithmeticTest {

    static final EnchantBehaviour.DrummingBeat BEAT = new EnchantBehaviour.DrummingBeat("bone_club", 100,
            List.of(3.0, 4.0, 5.0), "block.note_block.basedrum", "block.bone_block.break");

    /** BC4: every term. sent = ((44 x score/100) x 1.0 + 0 + 0) x 1.0 x (1 or 2). */
    @Test
    void theHitAtOneHundredAndTwoHundred() {
        double level = LevelBonus.at(1).weaponDamage();
        assertEquals(0.0, level);
        assertEquals(0.0, LevelBonus.at(1).critChance(), "level 1 adds no crit chance");
        assertEquals(44.0, HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(44, 100), 0.0, 0.0 + level), 1.0, 1.0));
        assertEquals(88.0, HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(44, 100), 0.0, 0.0 + level), 1.0,
                1.0 + Crit.BASE_DAMAGE), "the allowed crit reading at 100");
        assertEquals(88.0, HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(44, 200), 0.0, 0.0 + level), 1.0, 1.0));
        assertEquals(176.0, HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(44, 200), 0.0, 0.0 + level), 1.0,
                1.0 + Crit.BASE_DAMAGE), "the allowed crit reading at 200");
        assertEquals(1.0, AttackCharge.scale(AttackCharge.FULL_CHARGE), "a full charge scales by exactly 1");
    }

    /** The zombies' custom health: max_health 200 → 1000 (the hit rows), 400 → 2000 (the truth row). */
    @Test
    void theZombiesCustomMax() {
        assertEquals(1000.0, MobScaling.maxHealth(200.0, false, true, 100));
        assertEquals(2000.0, MobScaling.maxHealth(400.0, false, true, 100));
        // DB7, the truth row: twenty hits, every one a crit at the most (88 sent each, before its defence): 1760 < 2000.
        assertEquals(1760.0, 20 * 88.0);
        assertTrue(20 * 88.0 < MobScaling.maxHealth(400.0, false, true, 100), "it survives twenty crits");
    }

    /** BC2: the attribute, and the full-charge time at 0.8 (READ: delay = 1 / attack speed x 20, as the server computes). */
    @Test
    void theAttackSpeedAndTheChargeTime() {
        // EXECUTED (this test's first run, 2026-11-26): 4.0 + (0.8 - 4.0) is 0.7999999999999998, NOT 0.8. probe_attribute
        // prints it to three decimals, 0.800 (String.format rounds half-up at the third place).
        double speed = AttackSpeedAttribute.VANILLA_BASE + AttackSpeedAttribute.modifier(0.8, 1.0);
        assertEquals(0.7999999999999998, speed, 0.0);
        assertEquals("0.800", String.format(java.util.Locale.ROOT, "%.3f", speed));
        assertEquals(1.6, AttackSpeedAttribute.VANILLA_BASE + AttackSpeedAttribute.modifier(1.6, 1.0), "the iron sword");
        // Player.getCurrentItemAttackStrengthDelay, the same operations on that attribute: (float) (1.0 / speed * 20.0).
        float delay = (float) (1.0 / speed * 20.0);
        assertEquals(25.0f, delay, 0.0f);
        // getAttackCooldown = getAttackStrengthScale(0.5f) = clamp((ticker + 0.5) / delay, 0, 1) (the 0.5 INFERRED).
        assertEquals(1.0f, Math.min(1f, (40 + 0.5f) / delay), 0.0f, "40 ticks after a reset: full");
        assertEquals(1.0f, Math.min(1f, (25 + 0.5f) / delay), 0.0f, "full from the 25th tick (24.5 is the edge)");
        assertEquals(0.98f, Math.min(1f, (24 + 0.5f) / delay), 1e-6f, "the 24th is not full");
        assertEquals(0.62f, Math.min(1f, (15 + 0.5f) / delay), 1e-6f, "15 ticks: charged=false");
    }

    /**
     * DB6's uncharged hit: 15 ticks after the reset, the stashed charge is the float (15 + 0.5) / 25 widened to a double;
     * sent = 44 x scale(charge), and x 2 on a crit. EXECUTED here (never predicted): the figures the row quotes.
     */
    @Test
    void theUnchargedHit() {
        double charge = (double) Math.min(1f, (15 + 0.5f) / 25.0f);
        assertEquals(0.6200000047683716, charge, 0.0);
        double sent = HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(44, 100), 0.0, 0.0), AttackCharge.scale(charge), 1.0);
        assertEquals("22.331", String.format(java.util.Locale.ROOT, "%.3f", sent));
        assertEquals("44.662", String.format(java.util.Locale.ROOT, "%.3f",
                HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(44, 100), 0.0, 0.0), AttackCharge.scale(charge), 2.0)));
        assertTrue(charge < AttackCharge.FULL_CHARGE, "charged=false");
    }

    /** BC5: the iron sword's sweep is half the primary; the club's share is none. */
    @Test
    void theSweepShare() {
        assertEquals(0.0, SweepShare.NONE);
        assertEquals(11.5, SweepShare.of(23.0, 0.5));
    }

    /** The sheet's crit line at the base (the rows' baseline B, read in each row). */
    @Test
    void theSheetLine() {
        assertEquals("15%", StatsSheetLines.critChance(Crit.BASE_CHANCE));
    }

    private static List<String> streak(int level, int hits, double base, long spacing) {
        List<String> lines = new ArrayList<>();
        DrumBeats.Streak s = DrumBeats.Streak.NONE;
        long tick = 0;
        for (int i = 0; i < hits; i++) {
            DrumBeats.Decision d = DrumBeats.decide(s, tick, true, BEAT, level);
            lines.add(DrumBeats.line(d, BEAT.stepPercentFor(level), base + d.bonus(), true));
            s = d.after();
            tick += spacing;
        }
        return lines;
    }

    /** DB2 (III), DB3 (I), DB4 (II): the STREAK lines at the base 0.15, whole. */
    @Test
    void theStreakLinesByLevel() {
        assertEquals(List.of(
                "STREAK n=0 step=5 chance=0.150 charged=true result=counted",
                "STREAK n=1 step=5 chance=0.200 charged=true result=counted",
                "STREAK n=2 step=5 chance=0.250 charged=true result=counted",
                "STREAK n=3 step=5 chance=0.300 charged=true result=counted",
                "STREAK n=4 step=5 chance=0.350 charged=true result=counted"), streak(3, 5, Crit.BASE_CHANCE, 40));
        assertEquals(List.of(
                "STREAK n=0 step=3 chance=0.150 charged=true result=counted",
                "STREAK n=1 step=3 chance=0.180 charged=true result=counted",
                "STREAK n=2 step=3 chance=0.210 charged=true result=counted"), streak(1, 3, Crit.BASE_CHANCE, 40));
        assertEquals(List.of(
                "STREAK n=0 step=4 chance=0.150 charged=true result=counted",
                "STREAK n=1 step=4 chance=0.190 charged=true result=counted",
                "STREAK n=2 step=4 chance=0.230 charged=true result=counted"), streak(2, 3, Crit.BASE_CHANCE, 40));
    }

    /** DB7, THE TRUTH ROW: twenty hits at III, 30 ticks apart, from 0.15. The 18th, 19th and 20th roll at 1.000 or more. */
    @Test
    void theTruthRow() {
        List<String> lines = streak(3, 20, Crit.BASE_CHANCE, 30);
        assertEquals("STREAK n=16 step=5 chance=0.950 charged=true result=counted", lines.get(16), "the 17th");
        assertEquals("STREAK n=17 step=5 chance=1.000 charged=true result=counted", lines.get(17), "the 18th");
        assertEquals("STREAK n=18 step=5 chance=1.050 charged=true result=counted", lines.get(18), "the 19th");
        assertEquals("STREAK n=19 step=5 chance=1.100 charged=true result=counted", lines.get(19), "the 20th");
        assertEquals(1.0, Crit.BASE_CHANCE + DrumBeats.bonus(5, 17), 0.0, "the 18th's sum is exactly 1.0");
        assertTrue(Crit.crits(Crit.BASE_CHANCE + DrumBeats.bonus(5, 17), Math.nextDown(1.0)), "so it always crits");
        assertTrue(!Crit.crits(Crit.BASE_CHANCE + DrumBeats.bonus(5, 16), Math.nextDown(1.0)), "the 17th can miss");
    }

    /** DB5 (the gap) and DB6 (uncharged): the lines, at the base. */
    @Test
    void theResetLines() {
        DrumBeats.Decision two = DrumBeats.decide(new DrumBeats.Streak(1, 0), 100, true, BEAT, 3);
        assertEquals("STREAK n=1 step=5 chance=0.200 charged=true result=counted",
                DrumBeats.line(two, 5, Crit.BASE_CHANCE + two.bonus(), true), "a hit at exactly 100 continues");
        DrumBeats.Decision gap = DrumBeats.decide(new DrumBeats.Streak(2, 100), 221, true, BEAT, 3);
        assertEquals("STREAK n=0 step=5 chance=0.150 charged=true result=reset:gap",
                DrumBeats.line(gap, 5, Crit.BASE_CHANCE + gap.bonus(), true), "121 ticks later");
        DrumBeats.Decision un = DrumBeats.decide(new DrumBeats.Streak(2, 100), 115, false, BEAT, 3);
        assertEquals("STREAK n=2 step=5 chance=0.150 charged=false result=reset:uncharged",
                DrumBeats.line(un, 5, Crit.BASE_CHANCE + un.bonus(), false));
        DrumBeats.Decision next = DrumBeats.decide(un.after(), 155, true, BEAT, 3);
        assertEquals("STREAK n=0 step=5 chance=0.150 charged=true result=counted",
                DrumBeats.line(next, 5, Crit.BASE_CHANCE + next.bonus(), true), "the next reads n=0");
        DrumBeats.Decision plain = DrumBeats.decide(DrumBeats.Streak.NONE, 0, true, null, 0);
        assertEquals("STREAK n=0 step=0 chance=0.150 charged=true result=plain",
                DrumBeats.line(plain, 0, Crit.BASE_CHANCE + plain.bonus(), true), "the bare club");
    }
}
