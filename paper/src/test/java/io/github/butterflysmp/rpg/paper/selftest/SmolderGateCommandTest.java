package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-smolder's one command (2026-11-27): the regen-tooltip rows (#336) by reference, then this gate. Parsed against the
 * shipped scenarios, so the queue is the one Ben's command makes.
 */
class SmolderGateCommandTest {

    static final String TYPED = "armor-sets.AS1.AS2+mana-battery.MB1+smolder";

    private static ScenarioLoader.Loaded loaded() {
        ScenarioLoader.Loaded l = ScenarioLoader.load(path -> SmolderGateCommandTest.class.getClassLoader().getResourceAsStream(path));
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
    void theOneCommandQueuesTheRegenRowsThenThisGate_andIsNotRefused() {
        List<Scenario> q = queue();
        assertEquals(List.of("armor-sets#setup", "armor-sets#AS1", "armor-sets#AS2", "mana-battery#setup",
                "mana-battery#MB1", "smolder#setup", "smolder#SM1", "smolder#SM2", "smolder#SM3", "smolder#SM4",
                "smolder#SM5", "smolder#SM6", "smolder#WA1", "smolder#WA2", "smolder#WA3", "smolder#WA6",
                "smolder#SM7"), q.stream().map(s -> s.gate() + "#" + s.row()).toList());
        assertNull(SelfTestGateList.padRefusal(q), "every gate begins with pad: (V7 (e) does not refuse it)");
    }

    @Test
    void itLintsClean_asksOnceLast_andNoStepDamagesHim() {
        ScenarioLoader.Loaded l = loaded();
        assertEquals(List.of(), ScenarioLint.lint("smolder", l.gates().get("smolder")).stream().map(f -> f.line()).toList());
        List<Scenario.Step> steps = queue().stream().flatMap(s -> s.steps().stream()).toList();
        assertEquals(1, steps.stream().filter(st -> st.verb() == Scenario.Verb.ASK).count(), "the one ask");
        assertEquals(Scenario.Verb.ASK, steps.getLast().verb(), "and it is the command's last step");
        assertTrue(steps.getLast().arg().startsWith("1 3600 "), "3600 ticks: he watches first");
        assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.CMD && (st.arg().contains("rpg damage")
                || st.arg().trim().startsWith("damage ") || st.arg().contains("@s") && st.arg().contains("kill @s")))
                .count(), "no step deals him damage");
    }

    @Test
    void everyMobThisGateSummonsIsFrozenAndTagged() {
        List<Scenario.Step> mine = loaded().gates().get("smolder").stream().flatMap(s -> s.steps().stream()).toList();
        List<String> summons = mine.stream().filter(st -> st.verb() == Scenario.Verb.CMD
                && st.arg().trim().startsWith("summon ")).map(Scenario.Step::arg).toList();
        assertEquals(18, summons.size(), "the control: every zombie the gate file's coordinate table names");
        assertTrue(summons.stream().allMatch(s -> s.startsWith("summon zombie ") && s.contains("NoAI:1b")
                && s.contains("\"selftest\"")), summons.toString());
    }

    /** The stone verb takes nothing, as clear_inventory does, and every Smolder row converges it before it holds it. */
    @Test
    void theStoneVerbTakesNothing_andEveryStoneHoldFollowsIt() {
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.STONE, "").isEmpty());
        assertEquals(java.util.Optional.of("stone takes nothing, got 'x'"), SelfTestStaging.refusal(Scenario.Verb.STONE, "x"));
        for (Scenario s : loaded().gates().get("smolder")) {
            List<Scenario.Step> st = s.steps();
            for (int i = 0; i < st.size(); i++) {
                if (st.get(i).verb() == Scenario.Verb.HOLD && st.get(i).arg().equals("stone")) {
                    assertEquals(Scenario.Verb.STONE, st.get(i - 1).verb(), s.row() + ": the stone converged first");
                }
            }
        }
    }

    @Test
    void everyRowAfterTheFirstNeedsTheCellTheFirstPicks_andClearsHisState() {
        for (Scenario s : loaded().gates().get("smolder")) {
            if (s.row().equals(Scenario.SETUP)) continue;
            assertTrue(s.steps().stream().anyMatch(st -> st.verb() == Scenario.Verb.CLEAR_STATE), s.row());
            if (s.row().equals("SM1")) continue;
            assertEquals(Scenario.Verb.EXPECT_CELL, s.steps().getFirst().verb(), s.row());
            assertEquals("mage fire", s.steps().getFirst().arg(), s.row());
        }
    }
}
