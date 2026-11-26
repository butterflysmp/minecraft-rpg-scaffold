package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ONE COMMAND, SEVERAL GATES (the seat's ruling of 2026-11-24, G1-G6): {@code /rpg selftest <gate>+<gate>[+...]}. The parse
 * and the queue, pure: the gates in the order typed, each its own setup then its rows; refused before anything starts for a
 * gate named twice, an unknown gate, an empty part, or "all" in a combination. And G4: a setup that fails ends the run.
 */
class SelfTestGateListTest {

    private static Scenario sc(String gate, String row) {
        return new Scenario(gate, row, List.of());
    }

    private static Map<String, List<Scenario>> gates() {
        Map<String, List<Scenario>> m = new LinkedHashMap<>();
        m.put("alpha", List.of(sc("alpha", Scenario.SETUP), sc("alpha", "A1"), sc("alpha", "A2")));
        m.put("mage-guaranteed", List.of(sc("mage-guaranteed", Scenario.SETUP), sc("mage-guaranteed", "PT1"),
                sc("mage-guaranteed", "MG1R")));
        m.put("summoner", List.of(sc("summoner", Scenario.SETUP), sc("summoner", "SU1")));
        return m;
    }

    @Test
    void aPlusListIsSeveralGatesInTheOrderTyped() {
        var p = SelfTestGateList.parse("mage-guaranteed+summoner", gates());
        assertNull(p.refusal());
        assertEquals(List.of("mage-guaranteed", "summoner"), p.gates());
        var q = SelfTestGateList.queue(p, gates());
        assertEquals(List.of("mage-guaranteed#setup", "mage-guaranteed#PT1", "mage-guaranteed#MG1R", "summoner#setup",
                        "summoner#SU1"),
                q.stream().map(s -> s.gate() + "#" + s.row()).toList(), "each gate's setup, then its rows, in the order typed");
        var reversed = SelfTestGateList.parse("summoner+mage-guaranteed", gates());
        assertEquals(List.of("summoner", "mage-guaranteed"), reversed.gates(), "the order typed, not the file order");
        assertEquals("summoner#setup", SelfTestGateList.queue(reversed, gates()).getFirst().gate() + "#setup");
    }

    @Test
    void oneGateWithoutAPlusIsNotAList() {
        assertFalse(SelfTestGateList.isList("mage-guaranteed"));
        assertFalse(SelfTestGateList.isList("all"));
        assertTrue(SelfTestGateList.isList("a+b"));
    }

    @Test
    void aGateNamedTwiceIsRefused() {
        var p = SelfTestGateList.parse("summoner+mage-guaranteed+summoner", gates());
        assertEquals("Refusing: gate 'summoner' is named twice in 'summoner+mage-guaranteed+summoner'.", p.refusal());
        assertEquals(List.of(), p.gates());
    }

    @Test
    void anUnknownGateIsRefusedNamingTheGatesThereAre() {
        var p = SelfTestGateList.parse("mage-guaranteed+nope", gates());
        assertEquals("Refusing: no selftest gate 'nope' in 'mage-guaranteed+nope'. Gates: [alpha, mage-guaranteed, summoner]",
                p.refusal());
    }

    @Test
    void anEmptyPartIsRefused() {
        for (String typed : List.of("mage-guaranteed++summoner", "+summoner", "mage-guaranteed+")) {
            var p = SelfTestGateList.parse(typed, gates());
            assertEquals("Refusing: an empty gate name between two + in '" + typed + "'.", p.refusal(), typed);
        }
    }

    @Test
    void allIsNotCombinable() {
        var p = SelfTestGateList.parse("all+summoner", gates());
        assertEquals("Refusing: 'all' cannot be combined with other gates ('all+summoner').", p.refusal());
    }

    @Test
    void aRowNeedsOneGate() {
        assertEquals("Refusing: a row needs one gate, not 'a+b'.", SelfTestGateList.rowRefusal("a+b"));
        assertNull(SelfTestGateList.rowRefusal("mage-guaranteed"));
    }

    /**
     * The wiring, read from the source (it needs a server): the service parses a {@code +} list BEFORE "all" and before
     * one gate, refuses on any refusal, and builds ONE run from the queue; the run's skip branch ends the run for a setup
     * BEFORE it moves on to the next scenario.
     */
    @Test
    void theServiceAndTheRunAreWiredToTheList() throws java.io.IOException {
        String svc = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestService.java")).replace("\r", "");
        String start = svc.substring(svc.indexOf("public String start(Player player, String gate, String row) {"));
        int list = start.indexOf("if (SelfTestGateList.isList(gate)) {");
        int refused = start.indexOf("if (parsed.refusal() != null) return parsed.refusal();");
        int queued = start.indexOf("queue.addAll(SelfTestGateList.queue(parsed, loaded.gates()));");
        int all = start.indexOf("} else if (gate.equals(\"all\")) {");
        int run = start.indexOf("active = new SelfTestRun(this, player, queue);");
        assertTrue(list > 0 && refused > list && queued > refused && all > queued && run > all, "the order of the branch");
        String r = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java")).replace("\r", "");
        int skipped = r.indexOf("log(\"SKIPPED \" + skip.get());");
        int ends = r.indexOf("if (SelfTestGateList.skipEndsRun(current())) {", skipped);
        int abort = r.indexOf("end(SelfTestGateList.setupAbort(current().gate(), skip.get()));", ends);
        int next = r.indexOf("nextScenario();", skipped);
        assertTrue(skipped > 0 && ends > skipped && abort > ends && next > abort, "a setup's skip ends before moving on");
    }

    // --- NAMED ROWS OF OTHER GATES, BY REFERENCE (the seat's brief of 2026-11-25, the Broken Bottle) -----------------

    /**
     * {@code gate.ROW[.ROW]} in a + list queues that gate's setup, then only the rows named, in the GATE's order (CC'S CALL),
     * spelled as the gate spells them. A whole gate beside it is unchanged. Mutation: the rows filter dropped (the whole gate
     * runs), or the setup dropped from a reference.
     */
    @Test
    void aReferenceRunsItsGatesSetupAndOnlyTheRowsNamed() {
        var p = SelfTestGateList.parse("summoner+alpha.a2.A1+mage-guaranteed.MG1R", gates());
        assertNull(p.refusal(), p.refusal());
        assertEquals(List.of("summoner", "alpha", "mage-guaranteed"), p.gates());
        assertEquals(List.of("summoner#setup", "summoner#SU1", "alpha#setup", "alpha#A1", "alpha#A2",
                        "mage-guaranteed#setup", "mage-guaranteed#MG1R"),
                SelfTestGateList.queue(p, gates()).stream().map(s -> s.gate() + "#" + s.row()).toList());
    }

    /** Each refused BEFORE anything starts, by name. Mutation: any one refusal dropped. */
    @Test
    void aBadReferenceIsRefusedByName() {
        assertEquals("Refusing: no row 'Z9' in selftest gate 'alpha' ('summoner+alpha.Z9').",
                SelfTestGateList.parse("summoner+alpha.Z9", gates()).refusal());
        assertEquals("Refusing: an empty row name after 'alpha' in 'summoner+alpha.'.",
                SelfTestGateList.parse("summoner+alpha.", gates()).refusal());
        assertEquals("Refusing: an empty row name after 'alpha' in 'summoner+alpha..A1'.",
                SelfTestGateList.parse("summoner+alpha..A1", gates()).refusal());
        assertEquals("Refusing: row 'A1' of 'alpha' is named twice in 'summoner+alpha.A1.a1'.",
                SelfTestGateList.parse("summoner+alpha.A1.a1", gates()).refusal());
        assertEquals("Refusing: 'setup' runs with every row of 'alpha'; name a row ('summoner+alpha.setup').",
                SelfTestGateList.parse("summoner+alpha.setup", gates()).refusal());
        assertEquals("Refusing: gate 'alpha' is named twice in 'alpha+alpha.A1'.",
                SelfTestGateList.parse("alpha+alpha.A1", gates()).refusal(), "whole and by reference is still twice");
        assertTrue(SelfTestGateList.parse("summoner+nope.A1", gates()).refusal().startsWith("Refusing: no selftest gate 'nope'"));
    }

    /**
     * A CHAINED row (its first step is {@code after:}) is SKIPPED unless its chain ran just before it, so naming it alone
     * would only ever skip. Refused by name. Mutation: the check dropped.
     */
    @Test
    void aChainedRowCannotBeNamedAlone() {
        Map<String, List<Scenario>> m = new LinkedHashMap<>(gates());
        m.put("chain", List.of(sc("chain", Scenario.SETUP), sc("chain", "C1"),
                new Scenario("chain", "C2", List.of(new Scenario.Step(Scenario.Verb.AFTER, "C1")))));
        assertEquals("Refusing: row 'C2' of 'chain' runs only after its chain ('after: C1'); name its gate whole ('alpha+chain.C2').",
                SelfTestGateList.parse("alpha+chain.C2", m).refusal());
        assertNull(SelfTestGateList.parse("alpha+chain.C1", m).refusal(), "the head of a chain is not itself chained");
    }

    /** A reference is a + list item, so one reference alone still needs a +; the dot is what Brigadier's word() allows. */
    @Test
    void aReferenceIsAListItem() {
        assertTrue(SelfTestGateList.isList("a+b.C1"));
        assertFalse(SelfTestGateList.isList("alpha.A1"), "no +: the one-gate path, which takes its row as a second word");
    }

    /** G4: a step that SKIPS ends the whole run when it is a setup's; a row's skip moves on, as today. */
    @Test
    void aSetupFailureEndsTheRunAndARowFailureDoesNot() {
        assertTrue(SelfTestGateList.skipEndsRun(sc("summoner", Scenario.SETUP)));
        assertFalse(SelfTestGateList.skipEndsRun(sc("summoner", "SU1")));
        assertEquals("ABORTED setup of summoner: no hotbar slot holds x", SelfTestGateList.setupAbort("summoner",
                "no hotbar slot holds x"));
    }
}
