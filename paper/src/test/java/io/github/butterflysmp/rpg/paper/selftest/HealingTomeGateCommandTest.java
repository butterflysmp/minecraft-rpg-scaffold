package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * GATE-healing-tome's ONE typed command for the whole stack, parsed against the SHIPPED scenarios (2026-11-26): what Ben
 * types, and the queue it makes, step for step. The Healing Tome's gate runs LAST, after both asks, so its staging (his
 * class, his slots, his armour) touches no row of the gates below it; the run's restore puts all of it back.
 * Mutation: a gate dropped from the command, or the order typed changed.
 */
class HealingTomeGateCommandTest {

    static final String TYPED =
            "bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27+healing-tome";

    @Test
    void theOneCommandQueuesTheWholeStack_theTomeLast() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path ->
                HealingTomeGateCommandTest.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), loaded.refusals());
        var parsed = SelfTestGateList.parse(TYPED, loaded.gates());
        assertNull(parsed.refusal());
        assertEquals(List.of("bone-club", "blast-fungus", "true-tooltips", "healing-tome"), parsed.gates());
        List<String> queue = SelfTestGateList.queue(parsed, loaded.gates()).stream().map(s -> s.gate() + "#" + s.row()).toList();
        List<String> expected = new ArrayList<>(List.of(
                "bone-club#setup", "bone-club#BC1", "bone-club#BC2", "bone-club#BC3", "bone-club#BC4", "bone-club#BC5",
                "bone-club#DB1", "bone-club#DB2", "bone-club#DB3", "bone-club#DB4", "bone-club#DB5", "bone-club#DB6",
                "bone-club#DB7", "bone-club#DB8", "bone-club#BC6",
                "blast-fungus#setup", "blast-fungus#BF1", "blast-fungus#BF2", "blast-fungus#BF3", "blast-fungus#BF4",
                "blast-fungus#BF5", "blast-fungus#BF6", "blast-fungus#BF7", "blast-fungus#FF1", "blast-fungus#FF2",
                "blast-fungus#FF3", "blast-fungus#CB1", "blast-fungus#CB2", "blast-fungus#CB3", "blast-fungus#CB4",
                "blast-fungus#BF8", "blast-fungus#BF9",
                "true-tooltips#setup", "true-tooltips#TT20", "true-tooltips#TT21", "true-tooltips#TT22", "true-tooltips#TT23",
                "true-tooltips#TT24", "true-tooltips#TT25", "true-tooltips#TT26", "true-tooltips#TT27"));
        expected.addAll(List.of("healing-tome#setup", "healing-tome#HT1", "healing-tome#HT2", "healing-tome#HT3",
                "healing-tome#HT4", "healing-tome#HT5", "healing-tome#HT6", "healing-tome#HT7", "healing-tome#HT8"));
        assertEquals(expected, queue);
    }

    /** The gate's lint is clean, every rule, and it asks nothing. */
    @Test
    void theGateLintsClean_andAsksNothing() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path ->
                HealingTomeGateCommandTest.class.getClassLoader().getResourceAsStream(path));
        List<Scenario> gate = loaded.gates().get("healing-tome");
        assertEquals(9, gate.size(), "the control: its setup and eight rows are loaded");
        assertEquals(List.of(), ScenarioLint.lint("healing-tome", gate).stream().map(ScenarioLint.Finding::line).toList());
        assertEquals(0L, gate.stream().flatMap(s -> s.steps().stream()).filter(st -> st.verb() == Scenario.Verb.ASK).count());
    }
}
