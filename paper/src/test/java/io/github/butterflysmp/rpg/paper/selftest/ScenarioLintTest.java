package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
              - pad: ""
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
              - cmd: "tp @s ~ ~ ~ -90 0"
              - wait: 1
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
        // Both of the good row's turns removed (the stand and, since X6 (b), the view step), so the row turns him nowhere:
        // it is SELF_STAGED's finding, and VIEW_UNSET's too (its press has no view set before it).
        String bad = SETUP + GOOD_ROW.replace("  - cmd: \"execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0\"\n", "")
                .replace("  - cmd: \"tp @s ~ ~ ~ -90 0\"\n  - wait: 1\n", "");
        assertEquals(List.of(ScenarioLint.Rule.SELF_STAGED, ScenarioLint.Rule.VIEW_UNSET), rules(bad));
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
        // The same far summon with no pad: only the preflight box decides, and 50 is inside 0..60. Since P5 (the seat,
        // 2026-11-27) a setup with no pad step is itself a PAD_FIRST finding, and that is the only one.
        String noPad = "setup:\n  - preflight: \"0 -6 60 10\"\n" + GOOD_ROW.replace("~3 ~ ~0 {", "~50 ~ ~0 {")
                .replace("  - probe: a\n", "  - probe: \"a 52\"\n");
        assertEquals(List.of(ScenarioLint.Rule.PAD_FIRST), rules(noPad));
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

    // --- L1 (the seat's ruling, 2026-11-24): a scheduled held-item read, then a wait, then a change ---------------

    /** MG1 as committed: enchant show and rollstats, then clear_inventory and a give, with no wait between. */
    private static final String READ_THEN_CHANGE = """
            R1:
              - clear_inventory: ""
              - cmd: "rpg give flint_staff"
              - wait: 2
              - hold: flint_staff
              - cmd: "rpg enchant show"
              - cmd: "rpg enchant rollstats 500"
              - clear_inventory: ""
              - cmd: "rpg give lapis_staff"
              - wait: 2
              - hold: lapis_staff
              - cmd: "rpg enchant show"
              - wait: 2
            """;

    /**
     * X1 (the seat, 2026-11-25): a row reading a summon it did not cast. PLANTED: a probe_summon, and a selector naming
     * the wolf, each in a row with no cast; the CONTROL is the same rows with clear_state then use first (no finding).
     */
    @Test
    void aRowReadsOnlyASummonItCastItself() throws InvalidConfigurationException {
        String probeOnly = SETUP + """
                R1:
                  - probe_summon: "8"
                """;
        String selectorOnly = SETUP + """
                R1:
                  - cmd: "tp @e[type=minecraft:wolf,distance=..12,limit=1,sort=nearest] ~3 ~ ~"
                """;
        String castWithoutClear = SETUP + """
                R1:
                  - clear_inventory: ""
                  - cmd: "rpg give bone_whistle"
                  - wait: 2
                  - hold: bone_whistle
                  - cmd: "execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0"
                  - wait: 2
                  - use: ""
                  - probe_summon: "8"
                """;
        String own = SETUP + """
                R1:
                  - clear_inventory: ""
                  - clear_state: ""
                  - cmd: "rpg give bone_whistle"
                  - wait: 2
                  - hold: bone_whistle
                  - cmd: "execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0"
                  - wait: 2
                  - use: ""
                  - wait: 5
                  - probe_summon: "8"
                  - cmd: "tp @e[type=minecraft:wolf,distance=..12,limit=1,sort=nearest] ~3 ~ ~"
                """;
        assertTrue(rules(probeOnly).contains(ScenarioLint.Rule.SUMMON_UNSTAGED), "a probe with no cast");
        assertTrue(rules(selectorOnly).contains(ScenarioLint.Rule.SUMMON_UNSTAGED), "a wolf selector with no cast");
        assertTrue(rules(castWithoutClear).contains(ScenarioLint.Rule.SUMMON_UNSTAGED),
                "a cast with no clear_state first may have hit the cooldown and read a carried wolf");
        assertFalse(rules(own).contains(ScenarioLint.Rule.SUMMON_UNSTAGED), "CONTROL: its own cast");
    }

    /**
     * 4d: the two Summoner rules, each CAUSED (no shipped scenario summons a mob with its AI on, so only these rows
     * exercise MOB_ACT_UNSTAGED; READ: 0 such summons across the shipped scenarios at this commit).
     */
    @Test
    void aMobLeftItsAiNamesItsAttackAndASummonProbeNamesItsReach() {
        assertTrue(ScenarioLint.mobActsUnstaged("summon zombie ~ ~ ~ {Tags:[\"selftest\",\"s1\"]}"), "AI on, no attack");
        assertFalse(ScenarioLint.mobActsUnstaged(
                "summon zombie ~ ~ ~ {Tags:[\"s1\"],attributes:[{id:\"minecraft:attack_damage\",base:0.0}]}"), "staged");
        assertFalse(ScenarioLint.mobActsUnstaged("summon zombie ~ ~ ~ {NoAI:1b,Tags:[\"s1\"]}"), "frozen: it cannot act");
        assertFalse(ScenarioLint.mobActsUnstaged("rpg summon foo"), "not a vanilla summon");
        assertTrue(ScenarioLint.isPositiveNumber("8"));
        assertFalse(ScenarioLint.isPositiveNumber(""), "probe_summon with no reach");
        assertFalse(ScenarioLint.isPositiveNumber("0"));
    }

    @Test
    void aScheduledHeldReadFollowedByAnInventoryChangeWithNoWaitIsAFinding() throws InvalidConfigurationException {
        List<ScenarioLint.Finding> f = ScenarioLint.lint("g", parse("g", READ_THEN_CHANGE));
        assertEquals(List.of(ScenarioLint.Rule.HELD_READ_THEN_CHANGE), f.stream().map(ScenarioLint.Finding::rule).toList());
        assertEquals("LINT g#R1 HELD_READ_THEN_CHANGE clear_inventory after 'rpg enchant rollstats 500' with no wait:"
                + " the read runs a tick later (scheduler.onEntity) and would see the changed hand", f.get(0).line());
        // The fix: a wait after the read, before the change.
        String fixed = READ_THEN_CHANGE.replace("  - cmd: \"rpg enchant rollstats 500\"\n  - clear_inventory: \"\"\n",
                "  - cmd: \"rpg enchant rollstats 500\"\n  - wait: 2\n  - clear_inventory: \"\"\n");
        assertEquals(List.of(), rules(fixed));
    }

    /** Every reader and every changer, each by itself after the other: the one set, read from one place. */
    @Test
    void everyReaderAndEveryChangerIsSeen() throws InvalidConfigurationException {
        List<String> readers = List.of("rpg enchant show", "rpg enchant clear", "rpg durability damage 3", "rpg refresh");
        List<String> changers = List.of("  - hold: lapis_staff\n", "  - hold_only: lapis_staff\n",
                "  - clear_inventory: \"\"\n", "  - drop: \"\"\n", "  - click: \"Lapis\"\n",
                "  - click_material: \"STONE\"\n", "  - shift_click: \"bottom lapis_staff\"\n",
                "  - cmd: \"rpg give boltor\"\n", "  - cmd: \"give @s minecraft:stone\"\n",
                "  - cmd: \"clear @s minecraft:stone\"\n", "  - cmd: \"item replace entity @s weapon.mainhand with minecraft:air\"\n",
                "  - cmd: \"rpg gearscore set 100\"\n");
        for (String reader : readers) {
            assertTrue(ScenarioLint.readsHeldLater(reader), reader);
            for (String changer : changers) {
                String yaml = "R1:\n  - clear_inventory: \"\"\n  - cmd: \"" + reader + "\"\n" + changer + "  - wait: 2\n";
                // `drop:` is here as a CHANGER of the held item; it also reads his look (X6 (b)), which is VIEW_UNSET's to
                // judge (ViewUnsetLintTest), not this row's.
                assertEquals(List.of(ScenarioLint.Rule.HELD_READ_THEN_CHANGE), rules(yaml).stream()
                        .filter(r -> r != ScenarioLint.Rule.VIEW_UNSET).toList(), reader + " then " + changer);
            }
        }
        // Not readers: synchronous commands, and commands that touch no held item.
        for (String c : List.of("rpg gearscore set 100", "rpg give boltor", "rpg mana refill", "rpg playerxp set @s 1 levels")) {
            assertFalse(ScenarioLint.readsHeldLater(c), c);
        }
        // Not changers: a second scheduled command queues behind the first (first in, first out), and a probe reads.
        String queued = "R1:\n  - clear_inventory: \"\"\n  - cmd: \"rpg enchant clear\"\n  - cmd: \"rpg enchant candidate 0 x\"\n"
                + "  - cmd: \"rpg refresh\"\n  - dump: held\n  - wait: 2\n";
        assertEquals(List.of(), rules(queued));
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

    /**
     * THE LEVEL SEAM (the seat, 2026-11-25): AS13 set level 13, and AS14-AS19 ran at it with no step of their own. Each case
     * is CAUSED here, and the row that should be clean is beside the row that should not.
     */
    /**
     * LEVEL_GATED_MENU (the seat's follow-up, 2026-11-26, after AS15R SKIPPED at a locked /recipes): a command that opens a
     * level-gated screen runs at a staged level at least the screen's own, read from NexusScreens (recipes and craft 3, anvil
     * 7, enchanting 10, grindstone 13, vault 20). The level is the last `rpg playerxp set @s <n> levels` BEFORE the step, in
     * the row or carried from earlier rows, as the run carries it. CAUSED, with the clean rows beside the flagged ones.
     * Mutations: the rule dropped from lint(); the order check (a level set AFTER the open) dropped.
     */
    @Test
    void aRowThatOpensALevelGatedScreenStagesThatLevelFirst() throws InvalidConfigurationException {
        String yaml = """
                setup:
                  - pad: ""
                  - cmd: "rpg playerxp set @s 1 levels"
                A:
                  - cmd: "recipes"
                B:
                  - cmd: "rpg playerxp set @s 3 levels"
                  - cmd: "recipes"
                C:
                  - cmd: "enchanting"
                D:
                  - cmd: "rpg playerxp set @s 10 levels"
                  - cmd: "enchanting"
                  - cmd: "grindstone"
                E:
                  - cmd: "rpg playerxp set @s 1 levels"
                F:
                  - cmd: "recipes"
                  - cmd: "rpg playerxp set @s 3 levels"
                G:
                  - cmd: "level"
                  - cmd: "rpg stats"
                """;
        List<String> f = ScenarioLint.lint("g", parse("g", yaml)).stream()
                .filter(x -> x.rule() == ScenarioLint.Rule.LEVEL_GATED_MENU).map(ScenarioLint.Finding::line).toList();
        assertEquals(List.of(
                        "LINT g#A LEVEL_GATED_MENU 'recipes' needs level 3, and the staged level is 1",
                        "LINT g#C LEVEL_GATED_MENU 'enchanting' needs level 10, and the staged level is 3",
                        "LINT g#D LEVEL_GATED_MENU 'grindstone' needs level 13, and the staged level is 10",
                        "LINT g#F LEVEL_GATED_MENU 'recipes' needs level 3, and the staged level is 1"),
                f, "B and D's enchanting stage first; F sets 3 only AFTER it opened; G's commands are ungated");
        String none = """
                setup:
                  - pad: ""
                  - wait: 1
                A:
                  - cmd: "/enchanting"
                """;
        assertEquals(List.of("LINT g#A LEVEL_GATED_MENU 'enchanting' needs level 10, and no level is staged"),
                ScenarioLint.lint("g", parse("g", none)).stream().filter(x -> x.rule() == ScenarioLint.Rule.LEVEL_GATED_MENU)
                        .map(ScenarioLint.Finding::line).toList(), "with no level staged anywhere, and a leading slash");
    }

    @Test
    void aRowAfterALevelChangeStagesItsOwnLevel() throws InvalidConfigurationException {
        String yaml = """
                setup:
                  - pad: ""
                  - cmd: "rpg playerxp set @s 1 levels"
                A:
                  - wait: 1
                B:
                  - cmd: "rpg playerxp set @s 13 levels"
                  - wait: 40
                C:
                  - wait: 1
                D:
                  - after: C
                  - wait: 1
                E:
                  - wait: 2
                  - cmd: "rpg playerxp set @s 1 levels"
                F:
                  - wait: 1
                """;
        List<ScenarioLint.Finding> f = ScenarioLint.lint("g", parse("g", yaml)).stream()
                .filter(x -> x.rule() == ScenarioLint.Rule.LEVEL_UNSTAGED).toList();
        assertEquals(List.of("LINT g#C LEVEL_UNSTAGED starts at level '13 levels', left by B, not the setup's '1 levels',"
                        + " and stages none of its own"),
                f.stream().map(ScenarioLint.Finding::line).toList(),
                "A runs at the setup's level; B sets its own; C is the seam; D is chained (SELF_STAGED names it); E sets its"
                        + " own (anywhere in the row); F is back at the setup's level");
        // A setup that stages no level: a row that sets one makes every later row stage its own too.
        String none = """
                setup:
                  - pad: ""
                  - wait: 1
                A:
                  - cmd: "rpg playerxp set @s 1 levels"
                B:
                  - wait: 1
                """;
        assertEquals(List.of("B"), ScenarioLint.lint("g", parse("g", none)).stream()
                .filter(x -> x.rule() == ScenarioLint.Rule.LEVEL_UNSTAGED).map(ScenarioLint.Finding::row).toList());
        assertEquals(java.util.Optional.of("13 levels"), ScenarioLint.levelSet(parse("g", yaml).get(2)), "the CONTROL: B's");
        // Mutation: drop levelSeam's call from lint() -> both lists empty -> reddens.
    }
}
