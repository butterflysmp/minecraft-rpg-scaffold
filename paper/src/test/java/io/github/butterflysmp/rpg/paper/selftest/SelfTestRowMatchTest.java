package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code /rpg selftest <gate> <row>} finds the row in ANY LETTER CASE (the seat's brief of 2026-11-23, PR 1).
 *
 * <p><b>Why:</b> at the boot of {@code ac46c838} (GATE-boltor-recipe.md, HIS COMMANDS) Ben typed
 * {@code /rpg selftest vanilla-bows vb3r2} and no run started; the row is {@code VB3R2}. The match is
 * {@link SelfTestService#matchRow}, pure, so these rows reach it without a player or a server.
 */
class SelfTestRowMatchTest {

    private static List<Scenario> gate(String... rows) {
        List<Scenario> out = new ArrayList<>();
        for (String row : rows) out.add(new Scenario("g", row, List.of()));
        return out;
    }

    @Test
    void lowerCaseFindsTheRowAsTheGateSpellsIt() {
        assertEquals(Optional.of("VB3R2"), SelfTestService.matchRow(gate("setup", "VB1", "VB3R2"), "vb3r2"));
    }

    @Test
    void mixedCaseFindsTheRowAsTheGateSpellsIt() {
        assertEquals(Optional.of("VB3R2"), SelfTestService.matchRow(gate("setup", "VB1", "VB3R2"), "Vb3r2"));
    }

    @Test
    void theExactSpellingStillFindsIt() {
        assertEquals(Optional.of("VB3R2"), SelfTestService.matchRow(gate("setup", "VB1", "VB3R2"), "VB3R2"));
    }

    @Test
    void anUnknownRowStillRefuses() {
        assertEquals(Optional.empty(), SelfTestService.matchRow(gate("setup", "VB1", "VB3R2"), "vb4"));
        // A prefix is not a match: "vb3" must not start VB3R2.
        assertEquals(Optional.empty(), SelfTestService.matchRow(gate("setup", "VB1", "VB3R2"), "vb3"));
    }

    /** The queue the run gets: the gate's setup and the ONE row, spelled as the gate spells it, whatever he typed. */
    @Test
    void theQueueIsTheSetupAndTheCanonicalRow() {
        List<Scenario> queue = SelfTestService.queueFor(gate("setup", "VB1", "VB3R2"), "VB3R2");
        assertEquals(List.of("setup", "VB3R2"), queue.stream().map(Scenario::row).toList());
    }

    /** What the player is told: the refusal names what he typed, unchanged, and the gate. */
    @Test
    void theRefusalNamesWhatHeTyped() {
        assertEquals("No row 'vb4' in selftest gate 'vanilla-bows'.", SelfTestService.noRow("vb4", "vanilla-bows"));
    }

    /**
     * NO SHIPPED GATE HAS TWO ROWS THAT DIFFER ONLY IN CASE, so a case-blind match can never pick between two. Over every
     * scenario the jar carries. The control: the same scan over a fixture that has such a pair finds it.
     */
    @Test
    void noShippedGateHasTwoRowsEqualIgnoringCase() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(
                path -> SelfTestRowMatchTest.class.getClassLoader().getResourceAsStream(path));
        assertTrue(loaded.rows() > 300, "the scan saw the shipped scenarios (" + loaded.rows() + ")");
        assertEquals(List.of(), caseTwins(loaded.gates()));
        assertEquals(List.of("g: AB/ab"), caseTwins(Map.of("g", gate("setup", "AB", "ab"))), "the control");
    }

    /** With a case-twin pair, the exact spelling wins and a spelling that matches neither exactly refuses. */
    @Test
    void aCaseTwinPairPrefersTheExactSpellingAndOtherwiseRefuses() {
        assertEquals(Optional.of("ab"), SelfTestService.matchRow(gate("setup", "AB", "ab"), "ab"));
        assertEquals(Optional.of("AB"), SelfTestService.matchRow(gate("setup", "AB", "ab"), "AB"));
        assertEquals(Optional.empty(), SelfTestService.matchRow(gate("setup", "AB", "ab"), "Ab"));
    }

    private static List<String> caseTwins(Map<String, List<Scenario>> gates) {
        List<String> out = new ArrayList<>();
        gates.forEach((g, rows) -> {
            for (int i = 0; i < rows.size(); i++) {
                for (int j = i + 1; j < rows.size(); j++) {
                    String a = rows.get(i).row(), b = rows.get(j).row();
                    if (!a.equals(b) && a.toLowerCase(Locale.ROOT).equals(b.toLowerCase(Locale.ROOT))) {
                        out.add(g + ": " + a + "/" + b);
                    }
                }
            }
        });
        return out;
    }
}
