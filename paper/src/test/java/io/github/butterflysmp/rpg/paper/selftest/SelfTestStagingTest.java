package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

    // --- 6. SU8's death_rules: a row may lose its player, and the rules go back ---

    @Test
    void onlyADeathRulesRowWaitsForTheRespawn() {
        assertTrue(SelfTestStaging.waitForRespawn(true, true), "SU8: the death is the row's subject");
        assertFalse(SelfTestStaging.waitForRespawn(false, true), "any other death still aborts the run");
        assertFalse(SelfTestStaging.waitForRespawn(true, false), "THE CONTROL: alive, nothing to wait for");
        assertTrue(SelfTestStaging.RESPAWN_CAP_TICKS >= 100, "an immediate respawn is one client round trip; the cap must outlast a lag spike");
    }

    @Test
    void rowRulesPutBackWhatTheRowFoundAndOnlyOnce() {
        SelfTestStaging.RowRules<String> rules = new SelfTestStaging.RowRules<>();
        assertFalse(rules.active());
        rules.record("keep_inventory", false);
        rules.record("immediate_respawn", null);   // a rule with no value is real, and must not be overwritten
        rules.record("keep_inventory", true);       // a second record in the row cannot replace what the world had
        rules.record("immediate_respawn", true);

        assertTrue(rules.active());
        Map<String, Boolean> back = rules.drain();
        assertEquals(Boolean.FALSE, back.get("keep_inventory"));
        assertTrue(back.containsKey("immediate_respawn"));
        assertNull(back.get("immediate_respawn"));
        assertFalse(rules.active(), "drained: the next row starts with nothing to put back");
    }

    private static final Path SNAPSHOT = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestSnapshot.java");

    @Test
    void bothDeathRulesAreInTheRunsRestoreSet() throws IOException {
        String snapshot = Files.readString(SNAPSHOT, StandardCharsets.UTF_8);
        for (String rule : List.of("KEEP_INVENTORY", "IMMEDIATE_RESPAWN")) {
            assertTrue(snapshot.contains("world.getGameRuleValue(GameRules." + rule + ")"), rule + " is captured at RUN START");
            assertTrue(snapshot.contains("world.setGameRule(GameRules." + rule + ","), rule + " is put back by the restore");
        }
        String run = Files.readString(RUN, StandardCharsets.UTF_8);
        String wait = between(run, "private void awaitRespawn(", "void deathRules()");
        assertTrue(wait.contains("onRegionLater(") && !wait.contains("onEntityLater("),
                "the respawn poll must not ride the player's own scheduler, which a death retires (run 2's stall)");
        assertTrue(between(run, "private void rowDone()", "nextScenario();").contains("restoreRowRules()"),
                "a death_rules row puts its rules back when it ends");
        assertTrue(between(run, "void end(String why)", "service.ended(this)").contains("restoreRowRules()"),
                "and when the run ends mid-row");
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
