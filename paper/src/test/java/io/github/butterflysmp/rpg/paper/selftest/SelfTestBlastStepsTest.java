package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The selftest steps and lines added for dragon-mobs step R3 (GATE-dragon-blast.md): the player's damage line, the
 * fuse line, and the four new verbs' grammar. Each is a pure function, so its exact text is pinned here.
 */
class SelfTestBlastStepsTest {

    private static final UUID PHAGE = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    /** THE DAMAGE LINE: what was offered, what landed, the defence between them, and who dealt it. */
    @Test
    void theHurtLineCarriesBeforeAfterDefenceAndTheDealer() {
        assertEquals("HURT before=20.000 after=16.667 defense=20.000 dealer=" + PHAGE + " dealerType=creeper/phage",
                SelfTestBlast.hurtLine(20.0, 50.0 / 3.0, 20.0, PHAGE.toString(), "creeper/phage"));
        assertEquals("HURT before=4.000 after=4.000 defense=0.000 dealer=self dealerType=self",
                SelfTestBlast.hurtLine(4.0, 4.0, 0.0, "self", "self"));
    }

    @Test
    void theFuseLineNamesTheSwellAndTheIgnitedFlag() {
        assertEquals("FUSE tag=bl5 " + PHAGE + " fuse=10/30 ignited=false",
                SelfTestBlast.fuseLine("bl5", PHAGE, 10, 30, false));
    }

    @Test
    void anAwaitFuseNamesItsTagItsThresholdAndItsTimeout() {
        SelfTestBlast.Await a = SelfTestBlast.parseAwait("bl5 10 200").orElseThrow();
        assertEquals("bl5", a.tag());
        assertEquals(10, a.minFuse());
        assertEquals(200, a.timeoutTicks());
    }

    @Test
    void aMalformedAwaitFuseIsRefused() {
        for (String bad : new String[] {"", "bl5", "bl5 10", "bl5 ten 200", "bl5 0 200", "bl5 10 0", "bl5 10 200 x"}) {
            assertEquals(Optional.empty(), SelfTestBlast.parseAwait(bad), "'" + bad + "'");
        }
    }

    /** The loader refuses each new verb's bad argument by name, so it is a boot line, not a half-run gate. */
    @Test
    void theLoaderRefusesABadBlastStepByName() {
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.AWAIT_FUSE, "bl5 x").isPresent());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.DETONATE, "").isPresent(), "a tag is required");
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.IGNITE, "two words").isPresent(), "one tag only");
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PROBE_FUSE, "").isPresent());
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.AWAIT_FUSE, "bl5 10 200"));
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.DETONATE, "bl3"));
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.IGNITE, "bl8"));
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.PROBE_FUSE, "bl8"));
    }
}
