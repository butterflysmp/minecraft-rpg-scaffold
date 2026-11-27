package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S V7 (e), 2026-11-27: THE GATES WITH NO PAD STEP REFUSE TO RUN. When any gate a command names has no pad step as
 * its setup's first step, nothing of the command runs, no snapshot is taken, and one chat line says so. No override word.
 * The gates stay LOADED and counted; {@link PadFirstLintTest#KNOWN} stays the record of which they are.
 *
 * <p>Not typed by Ben: this is a unit and wiring test (the refusal is pure, and where it sits in {@code start} is read off
 * the source).
 */
class SelfTestPadRefusalTest {

    private static Map<String, List<Scenario>> shipped() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path -> SelfTestPadRefusalTest.class.getClassLoader()
                .getResourceAsStream(path));
        assertEquals(List.of(), loaded.refusals());
        return loaded.gates();
    }

    /** Every shipped gate, run whole: refused exactly when it is one of the KNOWN 42, by the placeholder line. */
    @Test
    void everyShippedGate_isRefusedExactlyWhenItPredatesThePad() {
        Map<String, List<Scenario>> gates = shipped();
        TreeSet<String> refused = new TreeSet<>();
        int ran = 0;
        for (var g : gates.entrySet()) {
            String r = SelfTestGateList.padRefusal(g.getValue());
            if (r == null) {
                ran++;
                continue;
            }
            assertEquals("Selftest: '" + g.getKey() + "' predates the pad and would run on your ground. Refused.", r);
            refused.add(g.getKey());
        }
        assertEquals(new TreeSet<>(PadFirstLintTest.KNOWN), refused, "the refused set IS the lint's known set");
        assertEquals(42, refused.size());
        assertEquals(gates.size(), refused.size() + ran, "every loaded gate was asked; they stay loaded (" + gates.size() + ")");
        assertTrue(ran > 0, "the control: a pad gate runs");
        // Mutation: padRefusal returns null always -> reddens; returns a refusal always -> the control reddens.
    }

    /** A list refuses as a whole, naming the first unpadded gate in the order it would run; a row by reference too. */
    @Test
    void aListThatNamesOne_isRefusedWhole_namingTheFirst() {
        Map<String, List<Scenario>> gates = shipped();
        SelfTestGateList.Parsed ok = SelfTestGateList.parse("held-target+blink-tome.BT1", gates);
        assertNull(ok.refusal());
        assertNull(SelfTestGateList.padRefusal(SelfTestGateList.queue(ok, gates)), "the control: two pad gates run");

        SelfTestGateList.Parsed mixed = SelfTestGateList.parse("held-target+sunder+boltor-recipe", gates);
        assertNull(mixed.refusal(), "the list itself parses");
        assertEquals(SelfTestGateList.padRefusalLine("sunder"), SelfTestGateList.padRefusal(SelfTestGateList.queue(mixed, gates)),
                "the first unpadded gate in the order typed");

        // One row of an unpadded gate: its setup comes with it, and it has no pad step.
        String row = gates.get("sunder").stream().map(Scenario::row).filter(r -> !r.equals(Scenario.SETUP)).findFirst()
                .orElseThrow();
        assertEquals(SelfTestGateList.padRefusalLine("sunder"),
                SelfTestGateList.padRefusal(SelfTestService.queueFor(gates.get("sunder"), row)));

        // `all`: every gate, so refused.
        List<Scenario> all = new ArrayList<>();
        gates.values().forEach(all::addAll);
        assertTrue(SelfTestGateList.padRefusal(all) != null, "all names the unpadded gates too");
    }

    /** A pad step that is not FIRST, or a gate with no setup, is refused: the predicate is the lint's own. */
    @Test
    void aPadStepThatIsNotFirst_orNoSetup_isRefused() {
        Scenario late = new Scenario("g", Scenario.SETUP, List.of(new Scenario.Step(Scenario.Verb.WAIT, "1"),
                new Scenario.Step(Scenario.Verb.PAD, "")));
        Scenario row = new Scenario("g", "R1", List.of(new Scenario.Step(Scenario.Verb.WAIT, "1")));
        assertEquals(SelfTestGateList.padRefusalLine("g"), SelfTestGateList.padRefusal(List.of(late, row)));
        assertEquals(SelfTestGateList.padRefusalLine("g"), SelfTestGateList.padRefusal(List.of(row)), "no setup at all");
        Scenario first = new Scenario("g", Scenario.SETUP, List.of(new Scenario.Step(Scenario.Verb.PAD, ""),
                new Scenario.Step(Scenario.Verb.WAIT, "1")));
        assertNull(SelfTestGateList.padRefusal(List.of(first, row)), "the control: pad first runs");
    }

    /** WIRING: in start(), the refusal is asked after every branch has filled the queue, and before the run is built. */
    @Test
    void startRefusesBeforeTheRunIsBuilt_soNoSnapshotIsTaken() throws IOException {
        List<String> c = Files.readAllLines(Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
                "selftest", "SelfTestService.java"), StandardCharsets.UTF_8);
        int lastFill = -1, ask = -1, refuse = -1, build = -1;
        for (int i = 0; i < c.size(); i++) {
            String l = c.get(i).trim();
            if (l.startsWith("queue.addAll(") || l.equals("loaded.gates().values().forEach(queue::addAll);")) lastFill = i;
            if (l.equals("String unpadded = SelfTestGateList.padRefusal(queue);")) ask = i;
            if (l.equals("if (unpadded != null) return unpadded;")) refuse = i;
            if (l.equals("active = new SelfTestRun(this, player, queue);")) build = i;
        }
        assertTrue(lastFill > 0 && ask > lastFill && refuse == ask + 1 && build == refuse + 1,
                "fill " + lastFill + ", ask " + ask + ", refuse " + refuse + ", build " + build);
        // And the snapshot is taken only in the run's start(), so a refusal before the run exists takes none.
        List<String> run = Files.readAllLines(Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
                "selftest", "SelfTestRun.java"), StandardCharsets.UTF_8);
        assertEquals(1, run.stream().filter(l -> l.contains("snapshot = SelfTestSnapshot.take(")).count());
    }
}
