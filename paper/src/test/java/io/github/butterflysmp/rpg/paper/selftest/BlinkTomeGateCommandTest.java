package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-blink-tome's ONE typed command for the whole stack, parsed against the SHIPPED scenarios (2026-11-26), and the
 * scenario's staging read back: the lint is clean, it asks nothing, and every summon a blinking row makes is the layout
 * {@link BlinkPadLandingTest} enumerated. Mutation: a blocker's offset changed in the YAML; the order typed changed.
 */
class BlinkTomeGateCommandTest {

    static final String TYPED = HealingTomeGateCommandTest.TYPED + "+blink-tome";

    private static ScenarioLoader.Loaded loaded() {
        return ScenarioLoader.load(path -> BlinkTomeGateCommandTest.class.getClassLoader().getResourceAsStream(path));
    }

    @Test
    void theOneCommandQueuesTheWholeStack_theBlinkTomeLast() {
        ScenarioLoader.Loaded loaded = loaded();
        assertEquals(List.of(), loaded.refusals());
        assertEquals("bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27+healing-tome+blink-tome", TYPED);
        var parsed = SelfTestGateList.parse(TYPED, loaded.gates());
        assertNull(parsed.refusal());
        assertEquals(List.of("bone-club", "blast-fungus", "true-tooltips", "healing-tome", "blink-tome"), parsed.gates());
        List<String> queue = SelfTestGateList.queue(parsed, loaded.gates()).stream().map(s -> s.gate() + "#" + s.row()).toList();
        List<String> tail = new ArrayList<>(List.of("healing-tome#HT8", "blink-tome#setup"));
        for (int i = 1; i <= 11; i++) tail.add("blink-tome#BT" + i);
        assertEquals(tail, queue.subList(queue.size() - 13, queue.size()), "the healing tome's last row, then this gate whole");
        assertEquals(41 + 9 + 12, queue.size(), "the lower gates' 41 steps of the queue, the tome's 9, this gate's 12");
    }

    @Test
    void theGateLintsClean_andAsksNothing() {
        List<Scenario> gate = loaded().gates().get("blink-tome");
        assertEquals(12, gate.size(), "the control: its setup and eleven rows are loaded");
        assertEquals(List.of(), ScenarioLint.lint("blink-tome", gate).stream().map(ScenarioLint.Finding::line).toList());
        assertEquals(0L, gate.stream().flatMap(s -> s.steps().stream()).filter(st -> st.verb() == Scenario.Verb.ASK).count());
    }

    private static final Pattern SUMMON = Pattern.compile("^summon zombie ~(-?\\d+) ~ ~(-?\\d+) \\{NoAI:1b,.*Tags:\\[\"(\\w+)\",\"selftest\"\\]\\}$");

    /**
     * Every row that can blink stages the enumerated layout: the four blockers, its trigger three east, then the move 20
     * east, in that order; every other zombie is at his feet after the move, or BT9's six east. Every zombie is NoAI.
     */
    @Test
    void everyBlinkingRowStagesTheEnumeratedLayout() {
        List<Scenario> gate = loaded().gates().get("blink-tome");
        int rows = 0;
        for (Scenario s : gate) {
            if (!s.row().startsWith("BT") || s.row().equals("BT1")) continue;
            rows++;
            List<int[]> blockers = new ArrayList<>();
            List<int[]> beforeMove = new ArrayList<>();
            boolean moved = false;
            for (Scenario.Step st : s.steps()) {
                if (st.verb() != Scenario.Verb.CMD) continue;
                String a = st.arg().trim();
                if (a.equals("tp @s ~20 ~ ~")) {
                    moved = true;
                    continue;
                }
                if (!a.startsWith("summon ")) continue;
                Matcher m = SUMMON.matcher(a);
                assertTrue(m.matches(), s.row() + ": every summon is a NoAI, selftest-tagged zombie: " + a);
                int[] at = {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
                if (moved) {
                    assertEquals(0, at[0] * at[0] + at[1] * at[1], s.row() + ": after the move, at his feet only: " + a);
                    continue;
                }
                beforeMove.add(at);
                if (m.group(3).equals("bkb")) blockers.add(at);
            }
            assertTrue(moved, s.row() + " moves him 20 east");
            assertEquals(List.of(BlinkPadLandingTest.BLOCKERS).stream().map(java.util.Arrays::toString).toList(),
                    blockers.stream().map(java.util.Arrays::toString).toList(), s.row() + ": the four blockers, as enumerated");
            for (int[] at : beforeMove) {
                boolean known = java.util.Arrays.equals(at, BlinkPadLandingTest.TRIGGER) || java.util.Arrays.equals(at, new int[] {26, 0})
                        || blockers.stream().anyMatch(b -> java.util.Arrays.equals(b, at));
                assertTrue(known, s.row() + ": a zombie the enumeration did not include, at " + java.util.Arrays.toString(at));
            }
        }
        assertEquals(10, rows, "the control: BT2 to BT11 were read");
    }
}
