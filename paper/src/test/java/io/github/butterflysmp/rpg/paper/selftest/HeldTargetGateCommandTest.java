package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-held-target's one command (2026-11-26): the owed rows R2 to R6 re-read by gate.ROW reference, then the summon fix's
 * S1 to S5. Parsed against the shipped scenarios, so the queue Ben's command makes is the one this test names.
 */
class HeldTargetGateCommandTest {

    static final String TYPED = "bone-club.DB6+blast-fungus.BF3.BF6.BF8+healing-tome.HT1"
            + "+blink-tome.BT1.BT3.BT4.BT5.BT6+held-target";

    private static ScenarioLoader.Loaded loaded() {
        return ScenarioLoader.load(path -> HeldTargetGateCommandTest.class.getClassLoader().getResourceAsStream(path));
    }

    @Test
    void theOneCommandQueuesTheOwedRowsThenThisGate() {
        ScenarioLoader.Loaded loaded = loaded();
        assertEquals(List.of(), loaded.refusals());
        var parsed = SelfTestGateList.parse(TYPED, loaded.gates());
        assertNull(parsed.refusal());
        List<String> queue = SelfTestGateList.queue(parsed, loaded.gates()).stream().map(s -> s.gate() + "#" + s.row()).toList();
        assertEquals(List.of("bone-club#setup", "bone-club#DB6",
                "blast-fungus#setup", "blast-fungus#BF3", "blast-fungus#BF6", "blast-fungus#BF8",
                "healing-tome#setup", "healing-tome#HT1",
                "blink-tome#setup", "blink-tome#BT1", "blink-tome#BT3", "blink-tome#BT4", "blink-tome#BT5", "blink-tome#BT6",
                "held-target#setup", "held-target#S1", "held-target#S2", "held-target#S3", "held-target#S4", "held-target#S5"),
                queue, "each referenced gate's setup, its rows; BT6's next row-start CLEAN is this gate's setup's");
    }

    @Test
    void theGateLintsClean_andTheWholeCommandAsksNothing() {
        ScenarioLoader.Loaded loaded = loaded();
        List<Scenario> gate = loaded.gates().get("held-target");
        assertEquals(6, gate.size(), "the control: its setup and five rows are loaded");
        assertEquals(List.of(), ScenarioLint.lint("held-target", gate).stream().map(f -> "LINT " + f.gate() + "#" + f.row()
                + " " + f.rule() + " " + f.detail()).toList());
        var parsed = SelfTestGateList.parse(TYPED, loaded.gates());
        long asks = SelfTestGateList.queue(parsed, loaded.gates()).stream()
                .flatMap(s -> s.steps().stream()).filter(st -> st.verb() == Scenario.Verb.ASK).count();
        assertEquals(0, asks, "no question in the whole command");
    }

    /** SAFETY: every mob a row of this gate summons is NoAI (it never acts); his wolf is the whistle's, refused on him. */
    @Test
    void everySummonedMobIsNoAi() {
        List<Scenario> gate = loaded().gates().get("held-target");
        List<String> summons = gate.stream().flatMap(s -> s.steps().stream())
                .filter(st -> st.verb() == Scenario.Verb.CMD && st.arg().trim().startsWith("summon ")).map(Scenario.Step::arg).toList();
        assertEquals(10, summons.size(), "control: S1 one, S2 one, S3 five, S4 two, S5 one");
        for (String s : summons) assertTrue(s.contains("NoAI:1b"), s);
    }

    /**
     * S3 stands where BT2 stands, with BT2's blockers and trigger, summoned in the same order before the same move, so
     * {@code BlinkPadLandingTest}'s enumeration (508 choosable spots, every one inside the box) is S3's. His wolf blocks no
     * spot: READ, {@code BlinkTomes.blocksALanding} returns false for any body whose side is not MOB, and a summon's is SUMMON.
     */
    @Test
    void s3StandsOnBt2sLayout_soItsLandingsAreTheEnumeratedOnes() {
        ScenarioLoader.Loaded loaded = loaded();
        assertEquals(layout(loaded.gates().get("blink-tome"), "BT2"), layout(loaded.gates().get("held-target"), "S3"));
    }

    private static List<String> layout(List<Scenario> gate, String row) {
        Scenario s = gate.stream().filter(x -> x.row().equals(row)).findFirst().orElseThrow();
        return s.steps().stream().filter(st -> st.verb() == Scenario.Verb.CMD)
                .map(st -> st.arg().trim())
                .filter(a -> a.startsWith("summon zombie") || a.equals("tp @s ~20 ~ ~"))
                .flatMap(a -> Stream.of(a)).toList();
    }
}
