package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@code docs/gates/GATE-regen-tooltips.md}: the ONE command Ben types on the next stack's boot, which re-reads the three
 * rows that already dump these tooltips (the seat's 2026-11-27 form: by gate.ROW reference, no rows of its own). The
 * queue, scenario by scenario; not refused by V7 (e); no question; no damage; no summon.
 */
class RegenTooltipsGateCommandTest {

    static final String TYPED = "armor-sets.AS1.AS2+mana-battery.MB1";

    private static ScenarioLoader.Loaded loaded() {
        ScenarioLoader.Loaded l = ScenarioLoader.load(path ->
                RegenTooltipsGateCommandTest.class.getClassLoader().getResourceAsStream(path));
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
    void theOneCommandQueuesTheThreeTooltipRows_andIsNotRefused() {
        List<Scenario> q = queue();
        assertEquals(List.of("armor-sets#setup", "armor-sets#AS1", "armor-sets#AS2", "mana-battery#setup",
                "mana-battery#MB1"), q.stream().map(s -> s.gate() + "#" + s.row()).toList());
        assertNull(SelfTestGateList.padRefusal(q), "both gates begin with pad: (V7 (e) does not refuse it)");
    }

    @Test
    void itAsksNothing_woundsNoOne_andSummonsNothing() {
        List<Scenario.Step> steps = queue().stream().flatMap(s -> s.steps().stream()).toList();
        assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.ASK).count(), "no question");
        assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.CMD && (st.arg().contains("rpg damage")
                || st.arg().trim().startsWith("damage "))).count(), "no command deals him damage");
        assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.CMD
                && st.arg().trim().startsWith("summon ")).count(), "no mob is summoned");
        // Counted from the two scenario files by awk over each row's lines (2026-11-27): AS1 4, AS2 4, MB1 4, setups 0.
        assertEquals(12, steps.stream().filter(st -> st.verb() == Scenario.Verb.DUMP).count(),
                "the control: the twelve tooltip dumps the gate re-predicts");
        assertEquals(1, steps.stream().filter(st -> st.verb() == Scenario.Verb.CMD
                && st.arg().equals("rpg enchant show")).count(), "MB1's one enchant show, on the helmet");
    }
}
