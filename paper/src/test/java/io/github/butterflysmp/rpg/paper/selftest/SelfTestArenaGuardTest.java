package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE RUN'S GUARD AND THE FIGHT'S OWN WORLD (GATE-fight-arena.md; the seat's ruling 2, 2026-11-18). The arena run
 * aborted inside AE1 three times: {@code SELFTEST RUN ABORTED the player left the dev world}. Every arena row takes
 * him into the fight's world, and the guard refused any world but the run's.
 * <ul>
 *   <li>The guard, as a pure rule: the run's world AND a fight world are accepted; any third world (the nether) is
 *       still refused.</li>
 *   <li>A WALK OF THE SHIPPED STEPS (the seat's 2c): AE1R2's and AE4R2's real step lists, read from the jar's own
 *       scenario file, driven through the guard. Entering moves him into the fight world, leaving and his respawn
 *       move him back, and AE4R2's await is where he dies. No step boundary may abort.</li>
 *   <li>The control: the old guard on the same walk aborts right after the enter, so the walk is not blind.</li>
 * </ul>
 */
class SelfTestArenaGuardTest {

    @Test
    void theGuardAcceptsTheRunsWorldAndAFightWorldAndStillRefusesAnyOther() {
        assertEquals(Optional.empty(), SelfTestStaging.invalid(true, false, true, false, 1), "the run's world");
        assertEquals(Optional.empty(), SelfTestStaging.invalid(true, false, false, true, 1), "a fight world");
        assertEquals(Optional.of("the player left the dev world"), SelfTestStaging.invalid(true, false, false, false, 1),
                "a third world (the nether) is still refused");
        assertEquals(Optional.of("the player died"), SelfTestStaging.invalid(true, true, false, true, 1));
        assertEquals(Optional.of("the player left"), SelfTestStaging.invalid(false, false, true, false, 1));
        assertEquals(Optional.of("2 players are online"), SelfTestStaging.invalid(true, false, false, true, 2));
    }

    /** Where he is during the walk. */
    private enum Place { RUN, FIGHT }

    /** The guard to walk with: the real one, or the control. */
    private interface Guard {
        Optional<String> check(boolean dead, Place place);
    }

    private static final Guard REAL = (dead, place) ->
            SelfTestStaging.invalid(true, dead, place == Place.RUN, place == Place.FIGHT, 1);

    /** The guard as it was at 7630e37c: the run's world only. */
    private static final Guard OLD = (dead, place) ->
            SelfTestStaging.invalid(true, dead, place == Place.RUN, false, 1);

    /**
     * Walks one shipped row's steps as the run does: after each step the run's advance asks the guard. A death_rules
     * row that dies goes to the respawn wait first ({@link SelfTestStaging#waitForRespawn}); he respawns at his return
     * location, in the run's world. Returns the first abort, or empty.
     */
    private static Optional<String> walk(String row, Guard guard, List<String> trace) {
        List<Scenario> arena = ScenarioLoader.load(path -> SelfTestArenaGuardTest.class.getClassLoader()
                .getResourceAsStream(path)).gates().get("fight-arena");
        Scenario s = arena.stream().filter(x -> x.row().equals(row)).findFirst().orElseThrow(() ->
                new AssertionError("no row " + row + " in the shipped fight-arena scenarios"));
        Place place = Place.RUN;
        boolean dead = false;
        boolean deathExpected = false;
        for (Scenario.Step step : s.steps()) {
            String arg = step.arg().trim();
            if (step.verb() == Scenario.Verb.DEATH_RULES) deathExpected = true;
            if (step.verb() == Scenario.Verb.CMD && arg.startsWith("rpg fight enter")) place = Place.FIGHT;
            if (step.verb() == Scenario.Verb.CMD && arg.equals("rpg fight leave")) place = Place.RUN;
            if (step.verb() == Scenario.Verb.AWAIT_FIGHT && deathExpected && place == Place.FIGHT) {
                dead = true;   // AE4R2: the closed ring's lightning kills him while the run waits for the fight to end
            }
            trace.add(step.verb() + " " + arg + " @" + place + (dead ? " dead" : ""));
            if (SelfTestStaging.waitForRespawn(deathExpected, dead)) {
                // awaitFight / advance: the respawn wait; his respawn puts him at his return location.
                dead = false;
                place = Place.RUN;
                trace.add("RESPAWNED @" + place);
            }
            Optional<String> abort = guard.check(dead, place);
            if (abort.isPresent()) return abort;
        }
        return Optional.empty();
    }

    @Test
    void ae1R2sRealStepsWalkThroughTheGuardIntoTheFightWorldAndBack() {
        List<String> trace = new ArrayList<>();
        assertEquals(Optional.empty(), walk("AE1R2", REAL, trace), "no abort: " + trace);
        assertTrue(trace.stream().anyMatch(t -> t.contains("@FIGHT")), "the walk did go into the fight world: " + trace);
    }

    @Test
    void ae4R2sRealStepsWalkThroughADeathInTheFightWorld() {
        List<String> trace = new ArrayList<>();
        assertEquals(Optional.empty(), walk("AE4R2", REAL, trace), "no abort: " + trace);
        assertTrue(trace.stream().anyMatch(t -> t.endsWith("dead")), "the walk did die: " + trace);
        assertTrue(trace.contains("RESPAWNED @RUN"), "and respawned home: " + trace);
    }

    private static String code(String file) throws java.io.IOException {
        return java.nio.file.Files.readString(java.nio.file.Path.of(
                        "src/main/java/io/github/butterflysmp/rpg/paper/selftest/" + file))
                .replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    /** The seat's 2b: save_blocks in a fight world would be restored into a deleted world, so it is refused first. */
    @Test
    void saveBlocksIsRefusedInAFightWorldBeforeItSavesAnything() throws java.io.IOException {
        String c = code("SelfTestDrivers.java");
        int from = c.indexOf("private Optional<String> saveBlocks(Player p, String arg)");
        String body = c.substring(from, c.indexOf("\n    private ", from + 10));
        int refuse = body.indexOf("FightWorlds.isFightWorld(p.getWorld())");
        int save = body.indexOf("saveBlock(");
        assertTrue(refuse > 0 && save > refuse, "the refusal comes before any block is saved: " + body);
    }

    /** The seat's 2b: both respawn waits poll at the run's start, never at a dead player in a world being unloaded. */
    @Test
    void bothRespawnWaitsPollAtTheRunsStart() throws java.io.IOException {
        String c = code("SelfTestRun.java");
        assertEquals(2, c.split("awaitRespawn\\(snapshot\\.start\\(\\), 0\\)", -1).length - 1, "advance and awaitFight");
        assertFalse(c.contains("awaitRespawn(player.getLocation()"), "never at the dead player's location");
    }

    @Test
    void theControlTheOldGuardAbortsTheSameWalkRightAfterTheEnter() {
        List<String> trace = new ArrayList<>();
        assertEquals(Optional.of("the player left the dev world"), walk("AE1R2", OLD, trace));
        assertFalse(trace.isEmpty());
        assertTrue(trace.getLast().startsWith("CMD rpg fight enter"), "aborted at the enter: " + trace.getLast());
    }
}
