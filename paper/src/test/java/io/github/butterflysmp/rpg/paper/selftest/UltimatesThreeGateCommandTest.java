package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-smolder-3 and GATE-solar-flare-3 (the seat's brief of 2026-11-28, Ben's two look-and-sound changes): two tiny
 * gates, ONE ROW and ONE ASK each, typed as one command, parsed against the shipped scenarios. Each row picks its cell as
 * the -2 gate's pick row did, then stages the ask exactly as the -2 gate's ask row did (SM7R, SF9R): only the question
 * differs. No mob is summoned and no step damages him.
 */
class UltimatesThreeGateCommandTest {

    static final String TYPED = "smolder-3+solar-flare-3";

    private static ScenarioLoader.Loaded loaded() {
        ScenarioLoader.Loaded l = ScenarioLoader.load(path -> UltimatesThreeGateCommandTest.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), l.refusals());
        return l;
    }

    private static Scenario row(String gate, String row) {
        return loaded().gates().get(gate).stream().filter(s -> s.row().equals(row)).findFirst()
                .orElseThrow(() -> new AssertionError(gate + "#" + row));
    }

    @Test
    void theOneCommandQueuesBothGatesInOrder_andIsNotRefused() {
        ScenarioLoader.Loaded l = loaded();
        var parsed = SelfTestGateList.parse(TYPED, l.gates());
        assertNull(parsed.refusal());
        List<Scenario> q = SelfTestGateList.queue(parsed, l.gates());
        assertEquals(List.of("smolder-3#setup", "smolder-3#SM8", "solar-flare-3#setup", "solar-flare-3#SF12"),
                q.stream().map(s -> s.gate() + "#" + s.row()).toList());
        assertNull(SelfTestGateList.padRefusal(q), "both begin with pad: (V7 (e) refuses neither)");
        for (String g : List.of("smolder-3", "solar-flare-3")) {
            var alone = SelfTestGateList.parse(g, l.gates());
            assertNull(alone.refusal(), g + " can be typed on its own");
            assertNull(SelfTestGateList.padRefusal(SelfTestGateList.queue(alone, l.gates())), g);
        }
    }

    /** Both lint clean (VIEW_UNSET included), one ask each, its gate's last step, 3600 ticks; no damage, no mob. */
    @Test
    void bothLintClean_askOnceEachLast_noMobAndNoStepDamagesHim() {
        ScenarioLoader.Loaded l = loaded();
        for (String g : List.of("smolder-3", "solar-flare-3")) {
            assertTrue(!ScenarioLint.VIEW_RULE_PREDATES.contains(g), g + " is held to VIEW_UNSET");
            assertEquals(List.of(), ScenarioLint.lint(g, l.gates().get(g)).stream().map(f -> f.line()).toList(), g);
            List<Scenario> rows = l.gates().get(g);
            assertEquals(2, rows.size(), g + ": its setup and ONE row");
            List<Scenario.Step> steps = rows.stream().flatMap(s -> s.steps().stream()).toList();
            assertEquals(1, steps.stream().filter(st -> st.verb() == Scenario.Verb.ASK).count(), g + ": one ask");
            assertEquals(Scenario.Verb.ASK, steps.getLast().verb(), g + ": its last step");
            assertTrue(steps.getLast().arg().startsWith("1 3600 "), g + ": 3600 ticks, he watches first");
            assertTrue(steps.stream().anyMatch(st -> st.verb() == Scenario.Verb.TELL && st.arg().contains("Keep your mouse still")),
                    g + ": the tell asks him to keep his mouse still");
            assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.CMD && (st.arg().contains("rpg damage")
                    || st.arg().trim().startsWith("damage ") || st.arg().contains("kill @s")
                    || st.arg().trim().startsWith("summon "))).count(), g + ": no damage to him, no mob");
            assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.SPAWN).count(), g + ": no spawn");
        }
    }

    /** The SEAT PLACEHOLDER wording of each question, verbatim. */
    @Test
    void eachAskIsTheSeatsPlaceholderWording() {
        assertEquals("1 3600 Did nine spiral arms of fire wind out from your feet, and are there enough particles now?"
                + " (yes / no / not sure)", row("smolder-3", "SM8").steps().getLast().arg());
        assertEquals("1 3600 Was each burst all orange, with a deep boom in place of the firework sound?"
                + " (yes / no / not sure)", row("solar-flare-3", "SF12").steps().getLast().arg());
    }

    /** Each setup is its -2 gate's setup, step for step (the pad, the preflight, night, no spawns, empty armour). */
    @Test
    void eachSetupIsItsTwoGatesSetup() {
        assertEquals(row("smolder-2", Scenario.SETUP).steps(), row("smolder-3", Scenario.SETUP).steps());
        assertEquals(row("solar-flare-2", Scenario.SETUP).steps(), row("solar-flare-3", Scenario.SETUP).steps());
    }

    /**
     * "THE CELL PICKED AS SM1R DOES" (SF1R for Solar Flare): the row opens with the -2 pick row's steps up to its stone,
     * with every menu dump left out (nothing but what staging the ask needs); then expect_cell, so a pick that did not
     * take stops the row before the cast; then the -2 ask row's steps from its stand on, step for step, with only the
     * question changed: the cast with his view set, the move 8 east (Smolder) or the views at P + 10 and P + 20 (Solar
     * Flare), and the same waits.
     */
    @Test
    void eachRowIsTheTwoGatesPickWithoutItsDumpsThenItsAskRowWithANewQuestion() {
        for (var g : List.of(List.of("smolder-3", "SM8", "smolder-2", "SM1R", "SM7R", "mage fire"),
                List.of("solar-flare-3", "SF12", "solar-flare-2", "SF1R", "SF9R", "ranger fire"))) {
            List<Scenario.Step> now = row(g.get(0), g.get(1)).steps();
            List<Scenario.Step> pick = row(g.get(2), g.get(3)).steps().stream()
                    .filter(st -> st.verb() != Scenario.Verb.DUMP).toList();
            pick = pick.subList(0, pick.indexOf(pick.stream().filter(st -> st.verb() == Scenario.Verb.STONE).findFirst().orElseThrow()));
            assertEquals(pick, now.subList(0, pick.size()), g.get(1) + ": the pick");
            assertEquals(Scenario.Verb.EXPECT_CELL, now.get(pick.size()).verb(), g.get(1));
            assertEquals(g.get(5), now.get(pick.size()).arg(), g.get(1));
            List<Scenario.Step> ask = row(g.get(2), g.get(4)).steps();
            int stand = 0;
            while (!(ask.get(stand).verb() == Scenario.Verb.CMD && ask.get(stand).arg().startsWith("execute align xz"))) stand++;
            List<Scenario.Step> tail = now.subList(pick.size() + 1, now.size());
            assertEquals(ask.subList(stand, ask.size() - 1), tail.subList(0, tail.size() - 1), g.get(1) + ": staged as " + g.get(4));
            assertEquals(Scenario.Verb.ASK, tail.getLast().verb());
            assertTrue(pick.stream().anyMatch(st -> st.verb() == Scenario.Verb.CLEAR_STATE), g.get(1) + ": clears his state first");
        }
    }

    /** Solar Flare's three shots each have his view set one tick before (P - 1 by the lint; P + 10 and P + 20 here). */
    @Test
    void theSolarFlareRowSetsHisViewBeforeShotsTwoAndThree() {
        List<Scenario.Step> st = row("solar-flare-3", "SF12").steps();
        int drop = 0;
        while (st.get(drop).verb() != Scenario.Verb.DROP) drop++;
        long t = 0;
        boolean at10 = false, at20 = false;
        for (int i = drop + 1; i < st.size(); i++) {
            if (st.get(i).verb() == Scenario.Verb.WAIT) t += Long.parseLong(st.get(i).arg().trim());
            if (ScenarioLint.turns(st.get(i)) && t == 10) at10 = true;
            if (ScenarioLint.turns(st.get(i)) && t == 20) at20 = true;
        }
        assertTrue(at10 && at20, "views at P+10 and P+20");
    }
}
