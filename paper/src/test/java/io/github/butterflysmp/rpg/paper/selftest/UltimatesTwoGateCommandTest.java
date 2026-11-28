package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-smolder-2 and GATE-solar-flare-2's ONE command (2026-11-28), parsed against the shipped scenarios, so the queue is
 * the one Ben's command makes. Two gates, ONE ASK EACH, each its gate's last row; the command's last step is the second.
 */
class UltimatesTwoGateCommandTest {

    static final String TYPED = "smolder-2+solar-flare-2";

    private static ScenarioLoader.Loaded loaded() {
        ScenarioLoader.Loaded l = ScenarioLoader.load(path -> UltimatesTwoGateCommandTest.class.getClassLoader().getResourceAsStream(path));
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
    void theOneCommandQueuesBothGatesInOrder_andIsNotRefused() {
        List<Scenario> q = queue();
        assertEquals(List.of("smolder-2#setup", "smolder-2#SM1R", "smolder-2#SM2R", "smolder-2#SM3R", "smolder-2#SM4R",
                "smolder-2#SM5R", "smolder-2#SM6R", "smolder-2#WA6R", "smolder-2#SM7R", "solar-flare-2#setup",
                "solar-flare-2#SF1R", "solar-flare-2#SF2R", "solar-flare-2#SF10", "solar-flare-2#SF3R", "solar-flare-2#SF4R",
                "solar-flare-2#SF6R", "solar-flare-2#SF7R", "solar-flare-2#SF8R", "solar-flare-2#SF11", "solar-flare-2#SF9R"),
                q.stream().map(s -> s.gate() + "#" + s.row()).toList());
        assertNull(SelfTestGateList.padRefusal(q), "both begin with pad: (V7 (e) refuses neither)");
    }

    /** Both lint clean (VIEW_UNSET included: neither is a predating gate); one ask each, its gate's last; no damage to him. */
    @Test
    void bothLintClean_askOnceEachLast_andNoStepDamagesHim() {
        ScenarioLoader.Loaded l = loaded();
        for (String g : List.of("smolder-2", "solar-flare-2")) {
            assertTrue(!ScenarioLint.VIEW_RULE_PREDATES.contains(g), g + " is held to VIEW_UNSET");
            assertEquals(List.of(), ScenarioLint.lint(g, l.gates().get(g)).stream().map(f -> f.line()).toList(), g);
            List<Scenario> rows = l.gates().get(g);
            List<Scenario.Step> steps = rows.stream().flatMap(s -> s.steps().stream()).toList();
            assertEquals(1, steps.stream().filter(st -> st.verb() == Scenario.Verb.ASK).count(), g + ": one ask");
            assertEquals(Scenario.Verb.ASK, rows.getLast().steps().getLast().verb(), g + ": its last row ends with it");
            assertTrue(rows.getLast().steps().getLast().arg().startsWith("1 3600 "), g + ": 3600 ticks, he watches first");
            assertTrue(rows.getLast().steps().stream().anyMatch(st -> st.verb() == Scenario.Verb.TELL
                    && st.arg().contains("Keep your mouse still")), g + ": the ask row's tell asks him to keep his mouse still");
        }
        List<Scenario.Step> all = queue().stream().flatMap(s -> s.steps().stream()).toList();
        assertEquals(Scenario.Verb.ASK, all.getLast().verb(), "the command's last step");
        assertEquals(0, all.stream().filter(st -> st.verb() == Scenario.Verb.CMD && (st.arg().contains("rpg damage")
                || st.arg().trim().startsWith("damage ") || st.arg().contains("kill @s"))).count(), "no step deals him damage");
    }

    @Test
    void everyMobIsFrozenAndTagged() {
        for (String g : List.of("smolder-2", "solar-flare-2")) {
            List<String> summons = loaded().gates().get(g).stream().flatMap(s -> s.steps().stream())
                    .filter(st -> st.verb() == Scenario.Verb.CMD && st.arg().trim().startsWith("summon "))
                    .map(Scenario.Step::arg).toList();
            assertEquals(g.equals("smolder-2") ? 10 : 7, summons.size(), g + ": every zombie the coordinate table names");
            assertTrue(summons.stream().allMatch(s -> s.startsWith("summon zombie ") && s.contains("NoAI:1b")
                    && s.contains("\"selftest\"")), summons.toString());
        }
    }

    static final String AT_G = "execute align xz positioned ~10.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0";
    static final String AT_H = "execute align xz positioned ~5.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0";

    /** Every casting row stands at its gate's stand first (G for Smolder, H for Solar Flare), under its preflight. */
    @Test
    void everyCastIsMadeFromItsStand() {
        assertEquals("-2 -12 30 12", preflight("smolder-2"));
        assertEquals("-2 -13 45 13", preflight("solar-flare-2"));
        for (var g : List.of(List.of("smolder-2", AT_G), List.of("solar-flare-2", AT_H))) {
            for (Scenario s : loaded().gates().get(g.get(0))) {
                int firstDrop = -1, stand = -1;
                for (int i = 0; i < s.steps().size(); i++) {
                    Scenario.Step st = s.steps().get(i);
                    if (st.verb() == Scenario.Verb.DROP && firstDrop < 0) firstDrop = i;
                    if (st.verb() == Scenario.Verb.CMD && st.arg().equals(g.get(1)) && stand < 0) stand = i;
                }
                if (firstDrop >= 0) assertTrue(stand >= 0 && stand < firstDrop, s.row() + ": casts before its stand");
            }
        }
    }

    private static String preflight(String gate) {
        return loaded().gates().get(gate).getFirst().steps().stream().filter(st -> st.verb() == Scenario.Verb.PREFLIGHT)
                .findFirst().orElseThrow().arg();
    }

    /**
     * X6 (b), FOR SOLAR FLARE BEFORE EACH OF THE THREE SHOTS: in every Solar Flare row, after its first drop at P, a
     * view-setting step runs at exactly P + 10 and at exactly P + 20 (one tick before shots 2 and 3, at P + 11 and P + 21),
     * unless the row ends the run first (a clear_state before that tick: SF8R). Ticks are the waits between the steps.
     */
    @Test
    void everySolarFlareCastHasItsViewSetOneTickBeforeShotsTwoAndThree() {
        int rows = 0;
        for (Scenario s : loaded().gates().get("solar-flare-2")) {
            List<Scenario.Step> st = s.steps();
            int drop = -1;
            for (int i = 0; i < st.size() && drop < 0; i++) if (st.get(i).verb() == Scenario.Verb.DROP) drop = i;
            if (drop < 0) continue;
            rows++;
            long t = 0;
            boolean at10 = false, at20 = false;
            long cleared = Long.MAX_VALUE;
            for (int i = drop + 1; i < st.size(); i++) {
                Scenario.Step x = st.get(i);
                if (x.verb() == Scenario.Verb.WAIT) t += Long.parseLong(x.arg().trim());
                if (x.verb() == Scenario.Verb.CLEAR_STATE && cleared == Long.MAX_VALUE) cleared = t;
                if (ScenarioLint.turns(x) && t == 10) at10 = true;
                if (ScenarioLint.turns(x) && t == 20) at20 = true;
            }
            assertTrue(at10 || cleared < 10, s.row() + ": a view at P+10");
            assertTrue(at20 || cleared < 20, s.row() + ": a view at P+20");
        }
        assertEquals(9, rows, "the casting rows: SF2R, SF10, SF3R, SF4R, SF6R, SF7R, SF8R, SF11, SF9R");
    }

    @Test
    void everyRowAfterTheFirstNeedsItsCell_andClearsHisState() {
        for (var g : List.of(List.of("smolder-2", "SM1R", "mage fire"), List.of("solar-flare-2", "SF1R", "ranger fire"))) {
            for (Scenario s : loaded().gates().get(g.get(0))) {
                if (s.row().equals(Scenario.SETUP)) continue;
                assertTrue(s.steps().stream().anyMatch(st -> st.verb() == Scenario.Verb.CLEAR_STATE), s.row());
                if (s.row().equals(g.get(1))) continue;
                assertEquals(Scenario.Verb.EXPECT_CELL, s.steps().getFirst().verb(), s.row());
                assertEquals(g.get(2), s.steps().getFirst().arg(), s.row());
            }
        }
    }
}
