package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SCENARIO LINT ({@link ScenarioLint}), rule by rule: each rule has a small FAILING fixture, the row the boot of
 * {@code e774d4b8} actually ran into, and its FIXED twin, which must lint clean. A rule that never fires on its failing
 * fixture is a rule that does not exist, so each failing assertion names the one rule it expects and nothing else.
 *
 * <p>The four defects (the seat's brief, 2026-11-23) and the fifth found while fixing them: (1) a row that relies on an
 * empty hotbar slot (nine rows SKIPPED); (2) a row that presses or probes on state another row staged (eight ran
 * VOID); (3) a press in the same pass as the turn before it (EC12); (4) a probe whose mob stands beyond its reach (EC8's
 * F at 30, a reach of 16); and (5) {@code rpg enchant candidate 1} straight after {@code rpg enchant clear}, which the
 * command REFUSES (a cleared item has no slots, and {@code EnchantState.addCandidate} adds only the next one). And the
 * ground: a summon outside what the gate's preflight measures.
 */
class ScenarioLintTest {

    private static List<Scenario> parse(String gate, String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse(gate, file, refusals);
        assertEquals(List.of(), refusals, "the fixture itself must load");
        return scenarios;
    }

    private static List<ScenarioLint.Rule> rules(String yaml) throws InvalidConfigurationException {
        return ScenarioLint.lint("g", parse("g", yaml)).stream().map(ScenarioLint.Finding::rule).toList();
    }

    /** A row that stages everything the lint asks for, on ground its preflight measures. The base every fixture edits. */
    private static final String SETUP = """
            setup:
              - preflight: "0 -6 32 10"
            """;

    private static final String GOOD_ROW = """
            R1:
              - clear_inventory: ""
              - clear_state: ""
              - cmd: "rpg give lapis_staff"
              - wait: 2
              - hold: lapis_staff
              - cmd: "rpg enchant clear"
              - cmd: "rpg enchant candidate 0 kinetic_tremors"
              - cmd: "execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0"
              - cmd: 'summon zombie ~3 ~ ~0 {NoAI:1b,Tags:["a","selftest"]}'
              - wait: 40
              - use: right_click
              - wait: 20
              - probe: a
            """;

    @Test
    void theGoodRowLintsClean() throws InvalidConfigurationException {
        assertEquals(List.of(), ScenarioLint.lint("g", parse("g", SETUP + GOOD_ROW)));
    }

    // --- rule 1: a gate asks nothing of his inventory -------------------------------------------------------------

    @Test
    void aHoldOnAnEmptySlotWithoutClearingTheInventoryIsAHotbarFinding() throws InvalidConfigurationException {
        String bad = SETUP + """
                R1:
                  - hold: empty
                  - note: "the inventory was never cleared"
                """;
        assertEquals(List.of(ScenarioLint.Rule.HOTBAR), rules(bad));
        String fixed = SETUP + """
                R1:
                  - clear_inventory: ""
                  - hold: empty
                """;
        assertEquals(List.of(), rules(fixed));
    }

    @Test
    void aGiveWithoutClearingTheInventoryIsAHotbarFinding() throws InvalidConfigurationException {
        String bad = SETUP + GOOD_ROW.replace("  - clear_inventory: \"\"\n", "");
        assertEquals(List.of(ScenarioLint.Rule.HOTBAR), rules(bad));
    }

    // --- rule 2: every row is self-contained ----------------------------------------------------------------------

    @Test
    void aPressWithNoWeaponHeldInTheRowIsASelfStagedFinding() throws InvalidConfigurationException {
        // EC10 at e774d4b8: no hold of its own, so it fired whatever EC2 had left in his hand.
        String bad = SETUP + GOOD_ROW.replace("  - hold: lapis_staff\n", "");
        assertEquals(List.of(ScenarioLint.Rule.SELF_STAGED), rules(bad));
    }

    @Test
    void aPressWithNoTurnInTheRowIsASelfStagedFinding() throws InvalidConfigurationException {
        // EC8 at e774d4b8: no turn of its own, so it fired at yaw 128.291, his own facing.
        String bad = SETUP + GOOD_ROW.replace("  - cmd: \"execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0\"\n", "");
        assertEquals(List.of(ScenarioLint.Rule.SELF_STAGED), rules(bad));
    }

    @Test
    void aProbeOfAMobTheRowDidNotSummonIsASelfStagedFinding() throws InvalidConfigurationException {
        // LC2 at e774d4b8: its N, F and T were LC1's.
        String bad = SETUP + GOOD_ROW.replace("  - cmd: 'summon zombie ~3 ~ ~0 {NoAI:1b,Tags:[\"a\",\"selftest\"]}'\n", "");
        assertEquals(List.of(ScenarioLint.Rule.SELF_STAGED), rules(bad));
    }

    @Test
    void aChainedRowIsASelfStagedFinding() throws InvalidConfigurationException {
        String bad = SETUP + GOOD_ROW + """
                R2:
                  - after: R1
                  - note: "reads what R1 left"
                """;
        assertEquals(List.of(ScenarioLint.Rule.SELF_STAGED), rules(bad));
    }

    // --- rule 3: a wait between a turn and the press after it -----------------------------------------------------

    @Test
    void anAimThenAUseWithNoWaitIsATurnThenPressFinding() throws InvalidConfigurationException {
        // EC12 at e774d4b8: each use read the facing of the aim before it.
        String bad = SETUP + GOOD_ROW.replace("  - use: right_click\n", "  - aim: a\n  - use: right_click\n");
        assertEquals(List.of(ScenarioLint.Rule.TURN_THEN_PRESS), rules(bad));
        String fixed = SETUP + GOOD_ROW.replace("  - use: right_click\n", "  - aim: a\n  - wait: 2\n  - use: right_click\n");
        assertEquals(List.of(), rules(fixed));
    }

    @Test
    void aTpWithRotationThenAReloadWithNoWaitIsATurnThenPressFinding() throws InvalidConfigurationException {
        String bad = SETUP + GOOD_ROW.replace("  - use: right_click\n",
                "  - cmd: \"execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0\"\n  - reload: \"\"\n");
        assertEquals(List.of(ScenarioLint.Rule.TURN_THEN_PRESS), rules(bad));
    }

    // --- rule 4: every probe's mob is inside the probe's reach ----------------------------------------------------

    @Test
    void aProbeOfAMobBeyondItsReachIsAProbeReachFinding() throws InvalidConfigurationException {
        // EC8 at e774d4b8: F 30 east, a probe that sees 16.
        String bad = SETUP + GOOD_ROW.replace("~3 ~ ~0 {", "~30 ~ ~0 {");
        assertEquals(List.of(ScenarioLint.Rule.PROBE_REACH), rules(bad));
        String fixed = SETUP + GOOD_ROW.replace("~3 ~ ~0 {", "~30 ~ ~0 {").replace("  - probe: a\n", "  - probe: \"a 32\"\n");
        assertEquals(List.of(), rules(fixed));
    }

    @Test
    void theReachLeavesOneBlockOfMargin() throws InvalidConfigurationException {
        // A mob 16 out is on the edge of a reach of 16 (its box half-width decides it): the lint wants 15.
        String edge = SETUP + GOOD_ROW.replace("~3 ~ ~0 {", "~16 ~ ~0 {");
        assertEquals(List.of(ScenarioLint.Rule.PROBE_REACH), rules(edge));
        String inside = SETUP + GOOD_ROW.replace("~3 ~ ~0 {", "~15 ~ ~0 {");
        assertEquals(List.of(), rules(inside));
    }

    // --- the ground: every summon stands on what the preflight measures --------------------------------------------

    @Test
    void aSummonOutsideThePreflightBoxIsAGroundFinding() throws InvalidConfigurationException {
        String bad = SETUP + GOOD_ROW.replace("~3 ~ ~0 {", "~3 ~ ~11 {");
        assertEquals(List.of(ScenarioLint.Rule.GROUND), rules(bad));
    }

    @Test
    void aGateWithSummonsAndNoPreflightIsOneGroundFinding() throws InvalidConfigurationException {
        assertEquals(List.of(ScenarioLint.Rule.GROUND), rules(GOOD_ROW));
    }

    // --- the pad (the seat's P9): a gate on the pad measures and summons only inside its interior ------------------

    private static final String PAD_SETUP = """
            setup:
              - pad: ""
              - preflight: "0 -6 32 10"
            """;

    @Test
    void aGateOnThePadWithEverythingInsideItsInteriorLintsClean() throws InvalidConfigurationException {
        assertEquals(List.of(), rules(PAD_SETUP + GOOD_ROW));
    }

    @Test
    void aPreflightBoxBeyondThePadsInteriorIsAGroundFinding() throws InvalidConfigurationException {
        String bad = PAD_SETUP.replace("0 -6 32 10", "0 -6 50 10") + GOOD_ROW;
        assertEquals(List.of(ScenarioLint.Rule.GROUND), rules(bad));
    }

    @Test
    void aSummonBeyondThePadsInteriorIsAGroundFindingEvenInsideAWiderPreflight() throws InvalidConfigurationException {
        // The preflight box reaches 60 east; the pad's interior ends at 45: the summon at 50 is caught by the pad's rule,
        // and the box by its own.
        String bad = PAD_SETUP.replace("0 -6 32 10", "0 -6 60 10") + GOOD_ROW.replace("~3 ~ ~0 {", "~50 ~ ~0 {")
                .replace("  - probe: a\n", "  - probe: \"a 52\"\n");
        assertEquals(List.of(ScenarioLint.Rule.GROUND, ScenarioLint.Rule.GROUND), rules(bad));
    }

    @Test
    void aGateThatDoesNotOptInIsLintedAsBefore() throws InvalidConfigurationException {
        // The same far summon with no pad: only the preflight box decides, and 50 is inside 0..60.
        String noPad = SETUP.replace("0 -6 32 10", "0 -6 60 10") + GOOD_ROW.replace("~3 ~ ~0 {", "~50 ~ ~0 {")
                .replace("  - probe: a\n", "  - probe: \"a 52\"\n");
        assertEquals(List.of(), rules(noPad));
    }

    // --- the fifth: a candidate slot the cleared item does not have yet -------------------------------------------

    @Test
    void aCandidateInSlotOneStraightAfterAClearIsAnEnchantOrderFinding() throws InvalidConfigurationException {
        // EC9 and EC11 as committed: clear, then candidate 1, which EnchantState.addCandidate refuses.
        String bad = SETUP + GOOD_ROW.replace("candidate 0 kinetic_tremors", "candidate 1 kinetic_tremors");
        assertEquals(List.of(ScenarioLint.Rule.ENCHANT_ORDER), rules(bad));
        String fixed = SETUP + GOOD_ROW.replace("  - cmd: \"rpg enchant candidate 0 kinetic_tremors\"\n",
                "  - cmd: \"rpg enchant candidate 0 lapis_crystal\"\n  - cmd: \"rpg enchant candidate 1 kinetic_tremors\"\n");
        assertEquals(List.of(), rules(fixed));
    }

    // --- the parsers the rules stand on ----------------------------------------------------------------------------

    @Test
    void aSummonsOffsetAndTagsAreReadFromItsCommand() {
        var s = ScenarioLint.summon("summon zombie ~12 ~ ~-5 {NoAI:1b,Tags:[\"n\",\"selftest\"],Health:200.0f}").orElseThrow();
        assertEquals(12.0, s.dx());
        assertEquals(0.0, s.dy());
        assertEquals(-5.0, s.dz());
        assertEquals(List.of("n", "selftest"), s.tags());
        assertTrue(ScenarioLint.summon("say summon zombie ~1 ~ ~1").isEmpty(), "only a summon command is a summon");
    }

    @Test
    void aTpTurnsOnlyWhenItCarriesARotation() {
        assertTrue(ScenarioLint.turns(new Scenario.Step(Scenario.Verb.CMD,
                "execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0")));
        assertTrue(!ScenarioLint.turns(new Scenario.Step(Scenario.Verb.CMD, "tp @s ~ ~ ~")), "no rotation, no turn");
        assertTrue(ScenarioLint.turns(new Scenario.Step(Scenario.Verb.AIM, "g")));
    }

    @Test
    void eachFindingPrintsAsOneLineNamingGateRowAndRule() {
        var f = new ScenarioLint.Finding("enchant-columns", "EC8", ScenarioLint.Rule.PROBE_REACH, "probe f: 30 out, reach 16");
        assertEquals("LINT enchant-columns#EC8 PROBE_REACH probe f: 30 out, reach 16", f.line());
    }
}
