package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.mob.MobBlink;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step R7b's instrument and the two carried fixes it reads through: the blink's fixed draw, C3's wait for the shot to
 * EXIST, and C2's clean of what can outlive a row.
 */
class SelfTestBlinkTest {

    // --- blink: "<tag> <angle> <distance>" -------------------------------------------------------------------------

    @Test
    void theBlinkStepTakesATagAnAngleAndADistance() {
        assertEquals(Optional.of(new SelfTestBlink.Forced("p1", new MobBlink.Draw(90, 8))), SelfTestBlink.parse("p1 90 8"));
        assertEquals(Optional.of(new SelfTestBlink.Forced("p1", new MobBlink.Draw(0, 6.5))), SelfTestBlink.parse(" p1 0 6.5 "));
        assertEquals(Optional.empty(), SelfTestBlink.parse("p1 90"));
        assertEquals(Optional.empty(), SelfTestBlink.parse("p1 ninety 8"));
        assertEquals(Optional.empty(), SelfTestBlink.parse("p1 90 -1"), "a negative distance is refused");
        assertEquals(Optional.empty(), SelfTestBlink.parse("p1 360 8"), "the angle is [0, 360)");
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.BLINK, "p1 90").orElseThrow().contains("<tag> <angle> <distance>"));
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.BLINK, "p1 90 8"));
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PROBE_SHIELD, "p1 p2").isPresent(), "probe_shield takes one tag");
    }

    // --- C3: strike_at_age waits for the shot to exist -------------------------------------------------------------

    @Test
    void noShotYetIsAWaitUntilTheTimeoutAndThenAGiveUpThatSaysSo() {
        assertEquals(SelfTestStrike.Next.WAIT, SelfTestStrike.next(false, 0, 30, 0, 200), "the forced shot fires next tick");
        assertEquals(SelfTestStrike.Next.WAIT, SelfTestStrike.next(false, 0, 30, 199, 200));
        assertEquals(SelfTestStrike.Next.NO_SHOT_AT_TIMEOUT, SelfTestStrike.next(false, 0, 30, 200, 200));
    }

    @Test
    void aLiveShotIsStruckAtItsAgeOrGivenUpAtTheTimeout() {
        assertEquals(SelfTestStrike.Next.WAIT, SelfTestStrike.next(true, 29, 30, 40, 200));
        assertEquals(SelfTestStrike.Next.STRIKE, SelfTestStrike.next(true, 30, 30, 40, 200));
        assertEquals(SelfTestStrike.Next.STRIKE, SelfTestStrike.next(true, 31, 30, 200, 200), "old enough wins over the timeout");
        assertEquals(SelfTestStrike.Next.YOUNG_AT_TIMEOUT, SelfTestStrike.next(true, 29, 30, 200, 200));
    }

    // --- C2: what can outlive a row --------------------------------------------------------------------------------

    @Test
    void theCleanEndsProjectilesCloudsAndOurMarkersAndNothingElse() {
        assertTrue(SelfTestStaging.lingering(true, false, false, false), "a projectile (an arrow, a fireball)");
        assertTrue(SelfTestStaging.lingering(false, true, false, false), "an area-effect cloud");
        assertTrue(SelfTestStaging.lingering(false, false, true, false), "a marker of ours (an Item or a fireball body)");
        assertFalse(SelfTestStaging.lingering(false, false, false, false), "anything else is the LEFTOVER count's");
        assertFalse(SelfTestStaging.lingering(true, false, false, true), "never a player");
    }

    @Test
    void theCleanSaysWhatItEnded() {
        // P2 (2026-11-27): the bound is the pad's region, not a radius.
        assertEquals("LINGER work=3 entities=2 pad [arrow 1, item 2]",
                SelfTestStaging.lingerLine(3, java.util.List.of("arrow 1", "item 2")));
        assertEquals("LINGER work=0 entities=0 pad", SelfTestStaging.lingerLine(0, java.util.List.of()));
    }
}
