package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-mana-battery's one command (2026-11-27): GATE-held-target's S3 and S5 by reference (V7 (b), (c), (d)), then this
 * gate (AC1 for V7 (a), MB1 to MB6 for Mana Battery). Parsed against the shipped scenarios, so the queue is the one Ben's
 * command makes.
 */
class ManaBatteryGateCommandTest {

    static final String TYPED = "held-target.S3.S5+mana-battery";

    private static ScenarioLoader.Loaded loaded() {
        ScenarioLoader.Loaded l = ScenarioLoader.load(path -> ManaBatteryGateCommandTest.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), l.refusals());
        return l;
    }

    private static List<Scenario> queue() {
        ScenarioLoader.Loaded l = loaded();
        var parsed = SelfTestGateList.parse(TYPED, l.gates());
        assertNull(parsed.refusal());
        return SelfTestGateList.queue(parsed, l.gates());
    }

    @Test
    void theOneCommandQueuesTheOwedRowsThenThisGate_andIsNotRefused() {
        List<Scenario> q = queue();
        assertEquals(List.of("held-target#setup", "held-target#S3", "held-target#S5", "mana-battery#setup",
                "mana-battery#AC1", "mana-battery#MB1", "mana-battery#MB2", "mana-battery#MB3", "mana-battery#MB4",
                "mana-battery#MB5", "mana-battery#MB6"), q.stream().map(s -> s.gate() + "#" + s.row()).toList());
        assertNull(SelfTestGateList.padRefusal(q), "both gates begin with pad: (V7 (e) does not refuse it)");
    }

    @Test
    void bothGatesLintClean_andTheCommandAsksNothing_summonsNothingHere_andWoundsNoOne() {
        ScenarioLoader.Loaded l = loaded();
        for (String g : List.of("held-target", "mana-battery")) {
            assertEquals(List.of(), ScenarioLint.lint(g, l.gates().get(g)).stream().map(f -> f.line()).toList(), g);
        }
        List<Scenario.Step> steps = queue().stream().flatMap(s -> s.steps().stream()).toList();
        assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.ASK).count(), "no question");
        assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.CMD && (st.arg().contains("rpg damage")
                || st.arg().trim().startsWith("damage "))).count(), "no command deals him damage");
        List<Scenario> mine = l.gates().get("mana-battery");
        assertEquals(8, mine.size(), "the control: its setup and seven rows (AC1, MB1 to MB6)");
        assertEquals(0, mine.stream().flatMap(s -> s.steps().stream()).filter(st -> st.verb() == Scenario.Verb.CMD
                && st.arg().trim().startsWith("summon ")).count(), "this gate summons no mob");
        assertTrue(mine.stream().filter(s -> !s.row().equals(Scenario.SETUP)).allMatch(s -> s.steps().stream()
                .anyMatch(st -> st.verb() == Scenario.Verb.ACCESSORY && st.arg().trim().equals("clear"))
                && s.steps().stream().anyMatch(st -> st.verb() == Scenario.Verb.CLASS)),
                "every row stages its class and its accessory slots");
    }
}
