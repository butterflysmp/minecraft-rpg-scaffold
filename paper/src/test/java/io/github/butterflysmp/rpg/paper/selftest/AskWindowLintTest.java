package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ASK_WINDOW_SHORT (the seat, 2026-11-26): an ask whose question has him hover, click, right-click or look first waits at
 * least {@link SelfTestAsk#DO_FIRST_TICKS}. TT19 waited 1200, timed out, and Ben ran the whole gate again to answer it.
 * The planted controls first; then every flag over the shipped gates, pinned EXACTLY. No old row is edited (they are the
 * record of what was run); a NEW ask must pass.
 */
class AskWindowLintTest {

    @Test
    void aDoFirstAskUnderTheWindowIsFlaggedAndNothingElse() {
        assertTrue(ScenarioLint.askWindowShort("1 1200 words Hover the Cursed Emerald and answer."), "hover, 1200");
        assertTrue(ScenarioLint.askWindowShort("1 2400 Right-click the water with the bottle. Did it fill?"), "right-click");
        assertTrue(ScenarioLint.askWindowShort("1 3599 Click the candidate. Did it unlock?"), "one tick under");
        assertTrue(ScenarioLint.askWindowShort("1 600 LOOK at the beam: is it blue?"), "any case");
        assertFalse(ScenarioLint.askWindowShort("1 3600 words Hover the Cursed Emerald and answer."), "at the window");
        assertFalse(ScenarioLint.askWindowShort("1 1200 Is the big Phage very slow?"), "no instruction verb");
        assertFalse(ScenarioLint.askWindowShort("not an ask"), "an unparseable ask is the parser's refusal, not this rule's");
        assertTrue(ScenarioLint.askWindowShort("1 1200 Two presses happened. Look at the wall: is it there?"),
                "a verb opening a LATER sentence is still an instruction");
        // The three false presences the bare word found in the shipped gates (2026-11-26): prose, not instructions.
        assertFalse(ScenarioLint.askWindowShort("1 2400 yesno Does the copy look like a player lying on the floor?"));
        assertFalse(ScenarioLint.askWindowShort("1 1200 words Did the flames reach as far and as wide as they looked?"));
        assertFalse(ScenarioLint.askWindowShort("1 1200 yesno Did the SECOND play no shot sound (only the empty-quiver click)?"));
    }

    @Test
    void thePromptTellsHimHowLongHeHas() {
        assertEquals("Selftest question 2: Hover it. Answer in your own words in chat. You have 180 seconds.",
                SelfTestAsk.prompt(SelfTestAsk.parse("2 3600 words Hover it.").orElseThrow()));
    }

    /** Every flag over the shipped gates, exactly: the old asks are the record and are not edited. */
    @Test
    void theShippedFlagsArePinned() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path ->
                AskWindowLintTest.class.getClassLoader().getResourceAsStream(path));
        List<String> flagged = new ArrayList<>();
        int asks = 0;
        for (var gate : loaded.gates().entrySet()) {
            for (Scenario s : gate.getValue()) {
                for (Scenario.Step st : s.steps()) if (st.verb() == Scenario.Verb.ASK) asks++;
            }
            for (ScenarioLint.Finding f : ScenarioLint.lint(gate.getKey(), gate.getValue())) {
                if (f.rule() == ScenarioLint.Rule.ASK_WINDOW_SHORT) flagged.add(gate.getKey() + "#" + f.row());
            }
        }
        System.out.println("ASK_WINDOW_SHORT over " + loaded.gates().size() + " gates, " + asks + " asks: " + flagged);
        assertTrue(asks > 20, "CONTROL: the shipped asks were read, " + asks);
        assertEquals(PINNED, flagged);
    }

    /**
     * PINNED 2026-11-26, read row by row: the three shipped asks that have him act first and wait 1200 ticks. Each is the
     * record of a boot already run, and is not edited. (The bare verb also matched fight-revive#RVB1 twice,
     * living-lantern#LL9 and unique-enchant-slot#BX2: prose, which the imperative position excludes.)
     */
    static final List<String> PINNED = List.of("armor-sets#AS16", "broken-bottle#BB8", "true-tooltips#TT19");
}
