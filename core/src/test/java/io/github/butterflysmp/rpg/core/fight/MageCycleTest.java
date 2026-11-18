package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F3, ONE MAGE CYCLE (PLAN-dragon-fight.md section 6, step F3).
 *
 * <p>The staging, chosen so no two quantities a row reads are equal: C at (0, 0); the Mage lands at tick 100 at
 * M = (18, 0), so the fast close lasts (18 + 36) / 5 = 10.8 s = 216 ticks and the slow close starts at tick 316. With
 * no kills the slow close shuts 12 / 0.4 = 30 s = 600 ticks later, at 916. Crystals light every 80 ticks from 316.
 */
class MageCycleTest {

    private static final FightSettings S = FightTestSettings.placeholders();
    private static final long LANDED = 100L;
    private static final long SLOW_START = 316L;
    private static final double MX = 18.0;

    private static MageCycle landed() {
        MageCycle cycle = new MageCycle(S, 0.0, 0.0);
        assertEquals(MageCycle.LandingResult.STARTED, cycle.mageLanded(LANDED, MX, 0.0));
        return cycle;
    }

    @Test
    void idleIsTheBorderWithNoMage() {
        MageCycle cycle = new MageCycle(S, 0.0, 0.0);
        MageCycle.Snapshot s = cycle.tick(0L);
        assertEquals(MageCycle.Phase.IDLE, s.phase());
        assertEquals(48.0, s.ring().radiusBlocks(), 0.0);
        assertEquals(0.0, s.ring().centreXBlocks(), 0.0);
        assertFalse(s.mageDamageable());
        assertFalse(s.mageAlive(), "no Mage, so the dragon is not immune on his account");
        assertEquals(0, s.crystalsLit());
        assertEquals(MageCycle.Outcome.RUNNING, s.outcome());
    }

    @Test
    void aLandingStartsTheFastCloseAndTheMageCannotBeHurtInIt() {
        MageCycle cycle = landed();
        MageCycle.Snapshot s = cycle.tick(LANDED);
        assertEquals(MageCycle.Phase.FAST_CLOSE, s.phase());
        assertFalse(s.mageDamageable());
        assertTrue(s.mageAlive(), "the dragon is immune while he lives");
        assertEquals(48.0, s.ring().radiusBlocks(), 1e-9, "progress 0 on the landing tick");
    }

    @Test
    void theMageBecomesDamageableOnTheExactTickTheSlowCloseStarts() {
        MageCycle cycle = landed();
        MageCycle.Snapshot before = cycle.tick(SLOW_START - 1);
        assertEquals(MageCycle.Phase.FAST_CLOSE, before.phase());
        assertFalse(before.mageDamageable());
        MageCycle.Snapshot at = cycle.tick(SLOW_START);
        assertEquals(MageCycle.Phase.SLOW_CLOSE, at.phase());
        assertTrue(at.mageDamageable());
        assertEquals(12.0, at.ring().radiusBlocks(), 1e-9);
        assertEquals(MX, at.ring().centreXBlocks(), 1e-9);
    }

    @Test
    void theSlowCloseStartsOnTheSameTickWhenTicksAreSkipped() {
        MageCycle cycle = landed();
        MageCycle.Snapshot late = cycle.tick(SLOW_START + 80L);
        assertEquals(MageCycle.Phase.SLOW_CLOSE, late.phase());
        assertEquals(1, late.crystalsLit(), "counted from the real start tick, not from when we looked");
        assertEquals(12.0 - 0.4 * 4.0, late.ring().radiusBlocks(), 1e-9);
    }

    @Test
    void crystalsLightOneEvery80TicksFromTheSlowClose() {
        MageCycle cycle = landed();
        assertEquals(0, cycle.tick(SLOW_START).crystalsLit());
        assertEquals(0, cycle.tick(SLOW_START + 79L).crystalsLit());
        assertEquals(1, cycle.tick(SLOW_START + 80L).crystalsLit());
        assertEquals(3, cycle.tick(SLOW_START + 240L).crystalsLit());
    }

    /** Six kills inside keep the ring open past 800 ticks (each adds 1 block = 50 ticks of slow close). */
    private static MageCycle keptOpen() {
        MageCycle cycle = landed();
        for (long k = 100L; k <= 600L; k += 100L) {
            assertTrue(cycle.kill(SLOW_START + k, MX, 0.0), "kill at +" + k + " counted");
        }
        return cycle;
    }

    @Test
    void theLastCrystalPowersTheMageOnTheExactTick() {
        MageCycle cycle = keptOpen();
        MageCycle.Snapshot before = cycle.tick(SLOW_START + 799L);
        assertEquals(MageCycle.Phase.SLOW_CLOSE, before.phase());
        assertEquals(9, before.crystalsLit());
        MageCycle.Snapshot at = cycle.tick(SLOW_START + 800L);
        assertEquals(MageCycle.Phase.POWERED, at.phase());
        assertEquals(MageCycle.Outcome.FAILED_POWERED, at.outcome());
        assertEquals(10, at.crystalsLit());
        assertFalse(at.mageDamageable());
    }

    @Test
    void withNoKillsTheRingClosesBeforeTheCrystalsFinish() {
        MageCycle cycle = landed();
        MageCycle.Snapshot open = cycle.tick(SLOW_START + 599L);
        assertEquals(MageCycle.Phase.SLOW_CLOSE, open.phase());
        MageCycle.Snapshot shut = cycle.tick(SLOW_START + 600L);
        assertEquals(MageCycle.Phase.CLOSED, shut.phase());
        assertEquals(MageCycle.Outcome.FAILED_CLOSED, shut.outcome());
        assertEquals(0.0, shut.ring().radiusBlocks(), 0.0);
        assertFalse(shut.ring().contains(MX, 0.0), "nobody is inside a closed ring");
    }

    @Test
    void everyInputIsIgnoredOnceClosed() {
        MageCycle cycle = landed();
        cycle.tick(SLOW_START + 600L);
        assertFalse(cycle.kill(SLOW_START + 601L, MX, 0.0));
        cycle.mageDied(SLOW_START + 602L);
        assertEquals(MageCycle.LandingResult.IGNORED_ENDED, cycle.mageLanded(SLOW_START + 603L, 0.0, 0.0));
        MageCycle.Snapshot s = cycle.tick(SLOW_START + 2_000L);
        assertEquals(MageCycle.Phase.CLOSED, s.phase());
        assertEquals(0.0, s.ring().radiusBlocks(), 0.0, "no kill and no Mage death reopens it");
    }

    @Test
    void everyInputIsIgnoredOncePowered() {
        MageCycle cycle = keptOpen();
        cycle.tick(SLOW_START + 800L);
        assertFalse(cycle.kill(SLOW_START + 801L, MX, 0.0));
        cycle.mageDied(SLOW_START + 802L);
        assertEquals(MageCycle.LandingResult.IGNORED_ENDED, cycle.mageLanded(SLOW_START + 803L, 0.0, 0.0));
        assertEquals(MageCycle.Phase.POWERED, cycle.tick(SLOW_START + 900L).phase());
    }

    @Test
    void aKillCountsOnlyInTheSlowCloseAndOnlyFromInsideTheRing() {
        MageCycle idle = new MageCycle(S, 0.0, 0.0);
        assertFalse(idle.kill(5L, 0.0, 0.0), "IDLE: ignored");

        MageCycle cycle = landed();
        assertFalse(cycle.kill(LANDED + 10L, MX, 0.0), "FAST_CLOSE: ignored, even from the Mage's feet");

        double before = cycle.tick(SLOW_START + 100L).ring().radiusBlocks(); // 10
        assertFalse(cycle.kill(SLOW_START + 100L, MX + 10.5, 0.0), "10.5 from M, outside a radius-10 ring: ignored");
        assertEquals(before, cycle.tick(SLOW_START + 100L).ring().radiusBlocks(), 0.0, "and the ring did not grow");
        assertTrue(cycle.kill(SLOW_START + 100L, MX + 9.9, 0.0), "9.9 from M, inside: counted");
        assertEquals(before + 1.0, cycle.tick(SLOW_START + 100L).ring().radiusBlocks(), 1e-9);

        cycle.mageDied(SLOW_START + 200L);
        assertFalse(cycle.kill(SLOW_START + 210L, MX, 0.0), "RETURNING: ignored");
    }

    @Test
    void theMagesDeathPutsTheCrystalsOutAndReturnsTheRing() {
        MageCycle cycle = landed();
        long died = SLOW_START + 300L; // radius 12 - 6 = 6, 3 crystals lit
        assertEquals(3, cycle.tick(died - 1).crystalsLit());
        cycle.mageDied(died);
        MageCycle.Snapshot s = cycle.tick(died);
        assertEquals(MageCycle.Phase.RETURNING, s.phase());
        assertEquals(0, s.crystalsLit(), "the crystals go out");
        assertFalse(s.mageAlive(), "the dragon is no longer immune on his account");
        assertFalse(s.mageDamageable());
        assertEquals(6.0, s.ring().radiusBlocks(), 1e-9, "the return starts where the ring was");
        // Back up to 12 at v_slow (300 ticks), then the fast part reversed (216 ticks).
        long home = died + 300L + 216L;
        assertEquals(MageCycle.Phase.RETURNING, cycle.tick(home - 1).phase());
        MageCycle.Snapshot back = cycle.tick(home);
        assertEquals(MageCycle.Phase.IDLE, back.phase());
        assertEquals(48.0, back.ring().radiusBlocks(), 0.0);
        assertEquals(0.0, back.ring().centreXBlocks(), 0.0);
    }

    @Test
    void anAdminKillInTheFastCloseReturnsFromWhereTheRingIs() {
        MageCycle cycle = landed();
        long died = LANDED + 54L; // a quarter of the way: progress 0.25
        Ring where = cycle.tick(died).ring();
        cycle.mageDied(died);
        MageCycle.Snapshot s = cycle.tick(died);
        assertEquals(MageCycle.Phase.RETURNING, s.phase());
        assertEquals(where.radiusBlocks(), s.ring().radiusBlocks(), 1e-9);
        assertEquals(where.centreXBlocks(), s.ring().centreXBlocks(), 1e-9);
        assertEquals(39.0, s.ring().radiusBlocks(), 1e-9, "48 - 36 x 0.25");
        assertEquals(MageCycle.Phase.RETURNING, cycle.tick(died + 53L).phase());
        assertEquals(MageCycle.Phase.IDLE, cycle.tick(died + 54L).phase(), "home after the same 54 ticks, reversed");
    }

    @Test
    void aLandingWhileReturningIsHeldUntilTheRingIsHome() {
        MageCycle cycle = landed();
        long died = SLOW_START + 300L;
        cycle.mageDied(died);
        long home = died + 300L + 216L;
        assertEquals(MageCycle.LandingResult.HELD, cycle.mageLanded(died + 10L, -20.0, 0.0));
        MageCycle.Snapshot held = cycle.tick(died + 10L);
        assertEquals(MageCycle.Phase.RETURNING, held.phase(), "the ring keeps returning");
        assertEquals(MX, held.ring().centreXBlocks(), 1e-9, "around the old M, not the new one");
        assertTrue(held.mageAlive(), "the held Mage is alive, so the dragon is immune again");
        assertFalse(held.mageDamageable());
        MageCycle.Snapshot start = cycle.tick(home);
        assertEquals(MageCycle.Phase.FAST_CLOSE, start.phase(), "his fast close starts the tick the ring is home");
        assertEquals(48.0, start.ring().radiusBlocks(), 1e-9);
        assertEquals(0.0, start.ring().centreXBlocks(), 1e-9);
        // (20 + 36) / 5 s = 224 ticks to the new slow close, about the new M.
        MageCycle.Snapshot slow = cycle.tick(home + 224L);
        assertEquals(MageCycle.Phase.SLOW_CLOSE, slow.phase());
        assertEquals(-20.0, slow.ring().centreXBlocks(), 1e-9);
    }

    @Test
    void landingsThatCannotStartAreRefused() {
        MageCycle cycle = new MageCycle(S, 0.0, 0.0);
        assertEquals(MageCycle.LandingResult.REFUSED_TOO_FAR, cycle.mageLanded(10L, 36.01, 0.0));
        assertEquals(MageCycle.Phase.IDLE, cycle.tick(10L).phase(), "a refused landing changes nothing");
        assertEquals(MageCycle.LandingResult.STARTED, cycle.mageLanded(11L, 10.0, 0.0));
        assertEquals(MageCycle.LandingResult.REFUSED_BUSY, cycle.mageLanded(12L, 5.0, 0.0),
                "one Mage at a time");
        // Died 100 ticks into a (10 + 36) / 5 s = 184-tick fast close: the return takes 100 ticks, home at 211.
        cycle.mageDied(111L);
        assertEquals(MageCycle.LandingResult.REFUSED_TOO_FAR, cycle.mageLanded(112L, 0.0, 40.0),
                "a held landing is checked for distance too");
        assertEquals(MageCycle.LandingResult.HELD, cycle.mageLanded(113L, 0.0, 30.0));
        assertEquals(MageCycle.LandingResult.REFUSED_BUSY, cycle.mageLanded(114L, 0.0, 20.0),
                "and only one may be held");
        assertEquals(MageCycle.Phase.RETURNING, cycle.tick(210L).phase());
        assertEquals(MageCycle.Phase.FAST_CLOSE, cycle.tick(211L).phase(), "the held one starts at home");
    }

    @Test
    void aMageDeathWithNoMageIsIgnored() {
        MageCycle cycle = new MageCycle(S, 0.0, 0.0);
        cycle.mageDied(5L);
        assertEquals(MageCycle.Phase.IDLE, cycle.tick(6L).phase());
    }

    @Test
    void timeMayNotRunBackwards() {
        MageCycle cycle = landed();
        cycle.tick(200L);
        assertThrows(IllegalArgumentException.class, () -> cycle.tick(199L));
    }
}
