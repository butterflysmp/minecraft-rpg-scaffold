package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE STACK'S ONE COMMAND (2026-11-27): the regen-tooltip rows (#336) by reference, GATE-smolder (#337, #338), then
 * GATE-solar-flare (#340). Two gates, so two asks at most: SM7 and SF9, the last step. Parsed against the shipped
 * scenarios, so the queue is the one Ben's command makes.
 */
class SolarFlareGateCommandTest {

    static final String TYPED = "armor-sets.AS1.AS2+mana-battery.MB1+smolder+solar-flare";

    private static ScenarioLoader.Loaded loaded() {
        ScenarioLoader.Loaded l = ScenarioLoader.load(path -> SolarFlareGateCommandTest.class.getClassLoader().getResourceAsStream(path));
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
    void theOneCommandQueuesTheWholeStack_andIsNotRefused() {
        List<Scenario> q = queue();
        assertEquals(List.of("armor-sets#setup", "armor-sets#AS1", "armor-sets#AS2", "mana-battery#setup",
                "mana-battery#MB1", "smolder#setup", "smolder#SM1", "smolder#SM2", "smolder#SM3", "smolder#SM4",
                "smolder#SM5", "smolder#SM6", "smolder#WA1", "smolder#WA2", "smolder#WA3", "smolder#WA6",
                "smolder#SM7", "solar-flare#setup", "solar-flare#SF1", "solar-flare#SF2", "solar-flare#SF3",
                "solar-flare#SF4", "solar-flare#SF5", "solar-flare#SF6", "solar-flare#SF7", "solar-flare#SF8",
                "solar-flare#SF9"), q.stream().map(s -> s.gate() + "#" + s.row()).toList());
        assertNull(SelfTestGateList.padRefusal(q), "every gate begins with pad: (V7 (e) does not refuse it)");
    }

    @Test
    void itLintsClean_asksTwiceWithSF9Last_andNoStepDamagesHim() {
        ScenarioLoader.Loaded l = loaded();
        assertEquals(List.of(), ScenarioLint.lint("solar-flare", l.gates().get("solar-flare")).stream().map(f -> f.line()).toList());
        List<Scenario.Step> steps = queue().stream().flatMap(s -> s.steps().stream()).toList();
        assertEquals(2, steps.stream().filter(st -> st.verb() == Scenario.Verb.ASK).count(), "SM7 and SF9");
        assertEquals(Scenario.Verb.ASK, steps.getLast().verb(), "SF9 is the command's last step");
        assertTrue(steps.getLast().arg().startsWith("1 3600 "), "3600 ticks: he watches first");
        assertEquals(0, steps.stream().filter(st -> st.verb() == Scenario.Verb.CMD && (st.arg().contains("rpg damage")
                || st.arg().trim().startsWith("damage ") || st.arg().contains("kill @s"))).count(),
                "no step deals him damage");
    }

    /** D3 here too: every cast from G, against the preflight SolarFlareGateArithmeticTest checks. */
    @Test
    void d3_everySolarFlareCastIsMadeFromG() {
        SmolderGateCommandTest.everyCastIsMadeFromG(loaded().gates().get("solar-flare"), "-2 -13 33 13");
    }

    @Test
    void everyMobThisGateSummonsIsFrozenAndTagged_andEveryRowAfterSF1NeedsTheCell() {
        var mine = loaded().gates().get("solar-flare");
        List<String> summons = mine.stream().flatMap(s -> s.steps().stream()).filter(st -> st.verb() == Scenario.Verb.CMD
                && st.arg().trim().startsWith("summon ")).map(Scenario.Step::arg).toList();
        assertEquals(8, summons.size(), "the control: z4, z7, s8, s13, b, t, u, w");
        assertTrue(summons.stream().allMatch(s -> s.startsWith("summon zombie ") && s.contains("NoAI:1b")
                && s.contains("\"selftest\"")), summons.toString());
        for (Scenario s : mine) {
            if (s.row().equals(Scenario.SETUP)) continue;
            assertTrue(s.steps().stream().anyMatch(st -> st.verb() == Scenario.Verb.CLEAR_STATE), s.row());
            if (s.row().equals("SF1")) continue;
            assertEquals(Scenario.Verb.EXPECT_CELL, s.steps().getFirst().verb(), s.row());
            assertEquals("ranger fire", s.steps().getFirst().arg(), s.row());
        }
    }
}
