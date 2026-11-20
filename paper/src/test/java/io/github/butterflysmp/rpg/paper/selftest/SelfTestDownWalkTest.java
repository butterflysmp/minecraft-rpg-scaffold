package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9d h: THE SELFTEST SURVIVES SPECTATOR MODE (the seat's go-ahead of 2026-11-19, as plan 6.7 did for a fight world).
 * <ul>
 *   <li><b>The audit:</b> the run's guard ({@link SelfTestStaging#invalid}) reads online, dead, which world, and how
 *       many are online. NO GAME MODE. A down player is alive (his death was cancelled) and in the fight's world, so
 *       the guard cannot tell him from a living one. The steps the rows use (cmd, wait, await_fight, death_rules,
 *       probe_entities) run through {@code performCommand} and the run's own pollers, none of which reads his mode.
 *       The run's end restores the mode it saved at its start ({@code SelfTestSnapshot}), which the selftest's own guard
 *       requires to be survival.</li>
 *   <li><b>The walk:</b> the shipped fight-down rows' real steps, read from the jar's scenario file, driven through the
 *       guard with the DOWN model: a death inside leaves him ALIVE, in the fight's world, spectating, until the fight
 *       ends and returns him; a teleport out (FD4) brings him back to the run's world at once. No step may abort.</li>
 *   <li><b>The control:</b> the same walk with the guard from before F8b's ruling (the run's world only) aborts right
 *       after the enter, so the walk does reach the fight's world.</li>
 * </ul>
 */
class SelfTestDownWalkTest {

    private enum Place { RUN, FIGHT }

    private interface Guard {
        Optional<String> check(boolean dead, Place place);
    }

    private static final Guard REAL = (dead, place) ->
            SelfTestStaging.invalid(true, dead, place == Place.RUN, place == Place.FIGHT, 1);

    private static final Guard RUN_WORLD_ONLY = (dead, place) ->
            SelfTestStaging.invalid(true, dead, place == Place.RUN, false, 1);

    private static Optional<String> walk(String row, Guard guard, List<String> trace) {
        List<Scenario> rows = ScenarioLoader.load(path -> SelfTestDownWalkTest.class.getClassLoader()
                .getResourceAsStream(path)).gates().get("fight-down");
        Scenario s = rows.stream().filter(x -> x.row().equals(row)).findFirst().orElseThrow(() ->
                new AssertionError("no row " + row + " in the shipped fight-down scenarios"));
        Place place = Place.RUN;
        boolean down = false;
        for (Scenario.Step step : s.steps()) {
            String arg = step.arg().trim();
            if (step.verb() == Scenario.Verb.CMD && arg.startsWith("rpg fight enter")) place = Place.FIGHT;
            if (step.verb() == Scenario.Verb.CMD && place == Place.FIGHT
                    && (arg.equals("kill @s") || arg.equals("tp @s ~ -130 ~"))) {
                down = true; // FD1, FD2R2, FD4: his death is cancelled; he spectates, alive, in the fight's world
            }
            if (step.verb() == Scenario.Verb.CMD && arg.startsWith("execute in minecraft:overworld run tp")) {
                place = Place.RUN; // FD4: a down spectator leaving the world is returned at once
                down = false;
            }
            if (step.verb() == Scenario.Verb.AWAIT_FIGHT && place == Place.FIGHT) {
                if (row.equals("FD3")) down = true; // the powered kill lands inside the await
                trace.add("AWAIT " + (down ? "down" : "alive") + " @" + place);
                place = Place.RUN; // ALL DOWN: five seconds later the fight ends and returns him
                down = false;
            }
            trace.add(step.verb() + " " + arg + " @" + place + (down ? " down" : ""));
            Optional<String> abort = guard.check(false, place); // DOWN IS NOT DEAD: the guard is asked with dead=false
            if (abort.isPresent()) return abort;
        }
        return Optional.empty();
    }

    @Test
    void everyFightDownRowWalksThroughTheGuardWhileHeIsDown() {
        for (String row : List.of("FD1", "FD2R2", "FD3", "FD4")) {
            List<String> trace = new ArrayList<>();
            assertEquals(Optional.empty(), walk(row, REAL, trace), row + " aborted: " + trace);
            assertTrue(trace.stream().anyMatch(t -> t.contains(" down")), row + " went down in the walk: " + trace);
            assertTrue(trace.get(trace.size() - 1).contains("@RUN"), row + " ends home: " + trace);
        }
    }

    @Test
    void theControlTheRunWorldOnlyGuardAbortsRightAfterTheEnter() {
        List<String> trace = new ArrayList<>();
        assertEquals(Optional.of("the player left the dev world"), walk("FD1", RUN_WORLD_ONLY, trace),
                "the walk reached the fight's world: " + trace);
    }

    private static String code(String file) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/" + file))
                .replace("\r\n", "\n").replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    /** The audit's two source facts: the guard takes no mode, and the run's end puts his saved mode back. */
    @Test
    void theGuardReadsNoGameModeAndTheRunsEndRestoresTheSavedOne() throws IOException {
        String staging = code("SelfTestStaging.java");
        assertTrue(staging.contains("static java.util.Optional<String> invalid(boolean online, boolean dead, boolean inRunWorld,"
                + " boolean inFightWorld,\n                                              int onlinePlayers) {"), "no mode in the guard");
        String run = code("SelfTestRun.java");
        int from = run.indexOf("private Optional<String> invalidated() {");
        String inv = run.substring(from, run.indexOf("\n    }", from));
        assertTrue(!inv.contains("GameMode") && !inv.contains("getGameMode"), inv);
        assertTrue(code("SelfTestSnapshot.java").contains("p.setGameMode(gameMode);"), "the run's end restores his mode");
    }
}
