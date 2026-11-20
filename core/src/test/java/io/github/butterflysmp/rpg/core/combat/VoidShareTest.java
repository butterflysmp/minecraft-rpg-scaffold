package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ben's void ruling (2026-11-19, PLAN-dragon-fight.md section 1 j): "A player should lose 10% of their health each damage
 * tick (0.5s) to the void regardless of their max health." Each tick takes the share of the player's MAXIMUM, defence
 * bypassed, so the tenth tick takes the last of a full bar.
 *
 * <p>Every expected number here was EXECUTED before it was written (a scratch {@code java} run, 2026-11-19): 100, 350 and
 * 1000 step down by exactly 10, 35 and 100 and reach 0.0 on the tenth subtraction. The same run found that naive
 * subtraction reaches zero only on the ELEVENTH tick for 16,001 of the 39,999 half-step maxima from 1 to 20,000 (the
 * first is 1.0), which is why the rule snaps residue and why {@link #everyHalfStepMaximumGoesToZeroOnTheTenthTick}
 * walks the whole range rather than three points.
 */
class VoidShareTest {

    private static final double BEN = 0.10;

    private static UUID player(CombatantStats stats, double max, double defence) {
        UUID id = UUID.randomUUID();
        stats.register(id, max, true);
        if (defence > 0) stats.reconcileDefenseModifiers(id, Map.of("armour", defence));
        return id;
    }

    /** Ten ticks through the real store at one maximum, with defence on him: each takes the same share, the tenth the last. */
    private static void tenTicks(double max, double share) {
        CombatantStats stats = new CombatantStats();
        UUID id = player(stats, max, 50.0);
        assertTrue(stats.defenseValue(id) > 0, "the control: he has defence for the bypass to bypass");
        for (int tick = 1; tick <= 10; tick++) {
            VoidShare.Hit hit = VoidShare.hitFor(stats.max(id), stats.current(id), BEN);
            assertEquals(share, hit.amount(), 0.0, "tick " + tick + " at max " + max + ": a share of the MAXIMUM, every tick");
            assertSame(DefenseRule.BYPASSED, hit.defense());
            var outcome = stats.damage(id, hit.amount(), id, true, CritState.NORMAL, hit.defense());
            assertEquals(share, outcome.dealt(), 0.0, "the store lands it whole: defence bypassed");
            if (tick < 10) {
                assertEquals(max - tick * share, stats.current(id), 1e-9, "tick " + tick);
                assertTrue(stats.current(id) > 0, "alive after tick " + tick);
            } else {
                assertEquals(0.0, stats.current(id), 0.0, "the tenth takes the last of a full bar");
            }
        }
    }

    @Test
    void atMaximum100TenTicksOf10() { tenTicks(100.0, 10.0); }

    @Test
    void atMaximum350TenTicksOf35() { tenTicks(350.0, 35.0); }

    @Test
    void atMaximum1000TenTicksOf100() { tenTicks(1000.0, 100.0); }

    /**
     * THE PROPERTY, NOT THREE POINTS. Naive subtraction leaves floating residue on 16,001 of these maxima and a player
     * would survive the tenth tick with 1e-15 health; the rule's snap is what makes the ruling true across the range.
     */
    @Test
    void everyHalfStepMaximumGoesToZeroOnTheTenthTick() {
        int checked = 0;
        for (int half = 2; half <= 40000; half++) {
            double max = half / 2.0;
            double current = max;
            int ticks = 0;
            while (current > 0 && ticks < 20) {
                VoidShare.Hit hit = VoidShare.hitFor(max, current, BEN);
                current = Math.max(0.0, current - hit.amount());
                ticks++;
            }
            assertEquals(10, ticks, "max " + max);
            checked++;
        }
        assertEquals(39999, checked, "the control: the whole range was walked");
    }

    /** The share is the content's, not a constant: a quarter takes four ticks. */
    @Test
    void theShareIsTheArgument() {
        double current = 200.0;
        int ticks = 0;
        while (current > 0 && ticks < 20) {
            current = Math.max(0.0, current - VoidShare.hitFor(200.0, current, 0.25).amount());
            ticks++;
        }
        assertEquals(4, ticks);
        assertEquals(50.0, VoidShare.hitFor(200.0, 200.0, 0.25).amount(), 0.0);
    }

    /** Maximum, not current: a player at a quarter of his bar still loses a tenth of his MAXIMUM. */
    @Test
    void aWoundedPlayerLosesAShareOfHisMaximum() {
        assertEquals(35.0, VoidShare.hitFor(350.0, 87.5, BEN).amount(), 0.0);
    }

    /** Below one share left, the tick takes what is left: never more than he has. */
    @Test
    void theLastTickTakesOnlyWhatIsLeft() {
        assertEquals(20.0, VoidShare.hitFor(350.0, 20.0, BEN).amount(), 0.0);
        assertFalse(VoidShare.hitFor(350.0, 20.0, BEN).amount() > 20.0);
    }

    @Test
    void aShareOutsideZeroToOneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> VoidShare.hitFor(350.0, 350.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> VoidShare.hitFor(350.0, 350.0, -0.1));
        assertThrows(IllegalArgumentException.class, () -> VoidShare.hitFor(350.0, 350.0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> VoidShare.hitFor(350.0, 350.0, Double.NaN));
        assertEquals(350.0, VoidShare.hitFor(350.0, 350.0, 1.0).amount(), 0.0, "the control: a whole share is legal");
    }
}
