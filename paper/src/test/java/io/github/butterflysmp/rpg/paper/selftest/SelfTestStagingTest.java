package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The four staging decisions between rows (SelfTestStaging), each pinned with its control, and the wiring that makes
 * the run actually take them. The live witnesses are GATE-selftest.md ST8-ST11, read at the next boot.
 */
class SelfTestStagingTest {

    private static Scenario.Step step(Scenario.Verb verb, String arg) {
        return new Scenario.Step(verb, arg);
    }

    private static Scenario scenario(Scenario.Step... steps) {
        return new Scenario("gate", "ROW", List.of(steps));
    }

    // --- 1. a chained row keeps what the row before it left ---

    @Test
    void aRowWhoseFirstStepIsAfterIsChainedAndNoOtherIs() {
        assertTrue(SelfTestStaging.chained(scenario(step(Scenario.Verb.AFTER, "WB2"), step(Scenario.Verb.PROBE, "t"))),
                "WB3 reads WB2's zombie: a row-start clean would remove it");
        assertFalse(SelfTestStaging.chained(scenario(step(Scenario.Verb.PROBE, "t"), step(Scenario.Verb.AFTER, "WB2"))),
                "only a FIRST-step after: chains");
        assertFalse(SelfTestStaging.chained(scenario()), "an empty row chains to nothing");
    }

    // --- 2. row start removes every selftest-tagged entity ---

    @Test
    void rowStartRemovesEveryTaggedEntityAndNeverAPlayer() {
        assertTrue(SelfTestStaging.removedAtRowStart(false, true), "WS3's tag-n neighbour took a hit in SU2");
        assertFalse(SelfTestStaging.removedAtRowStart(false, false), "an untagged mob is the world's, not the instrument's");
        assertFalse(SelfTestStaging.removedAtRowStart(true, true), "never a player, tagged or not");
    }

    // --- 3. after a kill, wait for the dying ---

    @Test
    void aDyingBodyIsDeadAndStillInTheWorld() {
        assertTrue(SelfTestStaging.dying(true, true, true), "killed, not yet removed: the death animation");
        assertFalse(SelfTestStaging.dying(true, false, true), "THE CONTROL: a live mob is not dying");
        assertFalse(SelfTestStaging.dying(true, true, false), "a removed body is gone, not dying: nothing to wait for");
        assertFalse(SelfTestStaging.dying(false, true, true), "only a living entity has a death animation");
    }

    @Test
    void onlyAKillCommandStartsTheWait() {
        assertTrue(SelfTestStaging.isKill(step(Scenario.Verb.CMD, "kill @e[tag=t]")));
        assertTrue(SelfTestStaging.isKill(step(Scenario.Verb.CMD, "  kill @e[tag=n]")), "the arg is trimmed, as every driver trims it");
        assertFalse(SelfTestStaging.isKill(step(Scenario.Verb.CMD, "rpg mobdamage 1000000")), "a kill by damage dies by our own path");
        assertFalse(SelfTestStaging.isKill(step(Scenario.Verb.NOTE, "kill @e[tag=t]")), "a note is not a command");
        assertFalse(SelfTestStaging.isKill(step(Scenario.Verb.CMD, "killzone")), "the command word, not a prefix of another");
    }

    @Test
    void theWaitIsCappedAboveTheDeathAnimation() {
        assertTrue(SelfTestStaging.SETTLE_CAP_TICKS > 20, "vanilla's death animation is 20 ticks; a cap below it cannot settle");
    }

    // --- 4. the inventory goes back at every gate boundary ---

    @Test
    void aGateBoundaryIsAChangeOfGateAndTheFirstGateIsNot() {
        assertTrue(SelfTestStaging.gateChanged("sunder", "deft-hands"), "DH1 found no hotbar slot after eight gates' gives");
        assertFalse(SelfTestStaging.gateChanged("sunder", "sunder"), "inside a gate the rows share what its setup gave");
        assertFalse(SelfTestStaging.gateChanged(null, "melee-cell"), "the first gate starts from the run's own snapshot");
    }

    // --- 5. blocks go back per chain, newest save first, first save wins ---

    @Test
    void rowBlocksRestoreNewestFirstKeepTheFirstSaveAndEmpty() {
        SelfTestStaging.RowBlocks<String, String> blocks = new SelfTestStaging.RowBlocks<>();
        blocks.save("a", "a-original");
        blocks.save("b", "b-original");
        blocks.save("a", "a-after-the-first-fill");   // a second save of a block keeps its FIRST state

        assertEquals(2, blocks.pending());
        assertEquals(List.of("b-original", "a-original"), blocks.drainNewestFirst());
        assertEquals(0, blocks.pending(), "a drained chain leaves nothing for the next chain to restore twice");
        assertEquals(List.of(), blocks.drainNewestFirst());

        blocks.save("a", "a-next-chain");
        assertEquals(List.of("a-next-chain"), blocks.drainNewestFirst(), "a new chain saves afresh");
        assertEquals(3, blocks.total(), "the RESTORED line's blocks= counts every save in the run");
    }

    // --- the wiring: the run takes each decision where the boot needs it ---

    private static final Path RUN = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java");

    private static String between(String source, String from, String to) {
        int a = source.indexOf(from);
        assertTrue(a >= 0, "'" + from + "' moved; this scan needs its new name");
        int b = source.indexOf(to, a + from.length());
        assertTrue(b > a, "'" + to + "' no longer follows '" + from + "'");
        return source.substring(a, b);
    }

    @Test
    void theRunCleansAtEveryUnchainedRowAndSettlesAfterEveryKill() throws IOException {
        String run = Files.readString(RUN, StandardCharsets.UTF_8);
        String next = between(run, "private void nextScenario()", "private void advance()");
        for (String call : List.of("SelfTestStaging.chained(s)", "SelfTestStaging.gateChanged(", "snapshot.restoreRowBlocks()",
                "snapshot.restoreInventory(player)", "snapshot.removeTagged(", "player.teleportAsync(start)")) {
            assertTrue(next.contains(call), "nextScenario must call " + call);
        }
        assertTrue(next.indexOf("SelfTestStaging.chained(s)") < next.indexOf("snapshot.restoreRowBlocks()"),
                "a chained row returns BEFORE the clean: WB3 needs WB2's zombie");
        assertTrue(next.indexOf("player.teleportAsync(start)") < next.indexOf("snapshot.restoreRowBlocks()"),
                "the teleport goes FIRST, so the player is never inside the restored blocks");

        String advance = between(run, "private void advance()", "private void settle(");
        assertTrue(advance.contains("SelfTestStaging.isKill(step)") && advance.contains("settle(0)"),
                "every kill step hands over to settle before the next step");
    }
}
