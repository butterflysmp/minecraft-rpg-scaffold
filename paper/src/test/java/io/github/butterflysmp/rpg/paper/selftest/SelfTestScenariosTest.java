package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scenarios the jar carries, against the gates they claim to drive.
 *
 * <p><b>THE PAIRING IS THE GUARD.</b> A scenario keyed {@code WB2} must name a {@code ### WB2 } heading in
 * {@code GATE-withered-shortbow.md} at the repo root, or a renamed row would be driven under its old name and its
 * readings filed against a row that no longer says what was predicted.
 */
class SelfTestScenariosTest {

    private static ScenarioLoader.Loaded shipped() {
        return ScenarioLoader.load(path -> SelfTestScenariosTest.class.getClassLoader().getResourceAsStream(path));
    }

    @Test
    void everyShippedScenarioLoadsWithNoRefusal() {
        ScenarioLoader.Loaded loaded = shipped();
        assertEquals(List.of(), loaded.refusals());
        assertEquals(List.of("melee-cell", "level", "nexus-polish", "legacy-a", "legacy-b", "wither-status",
                "withered-shortbow", "sunder", "deft-hands", "mining-auto", "nexus-back", "dragon-bodies",
                "dragon-split", "dragon-growth", "dragon-blast", "dragon-beam", "dragon-teleport"),
                List.copyOf(loaded.gates().keySet()),
                "the seventeen gates, in boot order; nexus-back after mining-auto (2026-11-14), then dragon-bodies,"
                        + " dragon-split and dragon-growth (2026-11-15), then dragon-blast (step R3, 2026-11-15), then dragon-beam (step R5) and dragon-teleport LAST (step R6, 2026-11-17)");
    }

    /** The seat's CORE rows, one scenario each (2026-10-05). R0d is a boot log line and has none. */
    @Test
    void theScenariosAreExactlyTheCoreRows() {
        // Map.of takes at most ten pairs; the eleventh gate (nexus-back, 2026-11-14) is put after it.
        Map<String, List<String>> expected = new java.util.LinkedHashMap<>(Map.of(
                "melee-cell", List.of("MC1", "MC2", "MC3"),
                "level", List.of("CP2", "BN1", "WD1", "WD3", "LS1"),
                "nexus-polish", List.of("D1", "G1", "G3", "CM1", "CM2"),
                "legacy-a", List.of("SB2"),
                "legacy-b", List.of("LB2", "LB5"),
                "wither-status", List.of("WS-C", "WS1", "WS2b", "WS2c", "WS3"),
                "withered-shortbow", List.of("WB1", "WB2", "WB3"),
                "sunder", List.of("SU1", "SU2", "SU4", "SU5", "SU6", "SU7", "SU8", "SU9", "SU10", "SU11", "SU12"),
                "deft-hands", List.of("DH1", "DH3", "DH6"),
                "mining-auto", List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11", "A12")));
        expected.put("nexus-back", List.of("NB1", "NB2", "NB3", "NB4", "NB5", "NB6", "NB7"));
        // The three dragon gates' rows are GATE-dragon-fix.md's since the 2026-11-15 boot: each re-predicts the old
        // row it is named for (DB8 and DB9 read as predicted and are not re-run).
        // STEP R3 TOOK EIGHT OF THEM OUT (the seat's brief of 2026-11-16; GATE-dragon-fix.md's dated note): DB10R2 and
        // DQ1 retired (a live Phage now blows), and DS1R2, DS2R2, DS5R2, DS6R2, DG6R2 and DG9R2 re-predicted as R3
        // rows in GATE-dragon-blast.md, where their split children are born frozen (or, in DS1R3, thawed on purpose).
        expected.put("dragon-bodies", List.of("DB1R2", "DB2R2", "DB3R2", "DB4R2", "DB5R2", "DB6R2", "DB7R2",
                "DB11R2"));
        expected.put("dragon-split", List.of("DS3R2", "DS4R2"));
        expected.put("dragon-growth", List.of("DG1R2", "DG2R2", "DG3R2", "DG4R2", "DG5R2", "DG7R2", "DG8R2"));
        // Step R3's rows, RE-PREDICTED by the follow-up of 2026-11-17 (GATE-dragon-blast-2.md): each is a new row
        // named for the GATE-dragon-blast.md row it supersedes, with the next suffix. BQ1R2 is Ben's three questions.
        expected.put("dragon-blast", List.of("BL1R2", "BL2R2", "BL3R2", "BL4R2", "BL5R2", "BL6R2", "BL7R2", "BL8R2",
                "BL9R2", "DS1R4", "DS2R4", "DS5R4", "DS6R4", "DG6R4", "DG9R4", "BQ1R2"));
        // Step R5 (GATE-dragon-beam.md, 2026-11-17): eleven AUTO rows and Ben's three questions, in one.
        // Step R6 (GATE-dragon-teleport.md) moved KB10 and KB11 out, re-predicted there as KB10R2 and KB11R2.
        // The Knell follow-up (GATE-dragon-knell-2.md, 2026-11-17) re-predicted every row of both Knell files, each
        // renamed with the next suffix; KB2R2 is the root read on a WALKING Knell, and TP14 is new.
        // Ben's re-timing (GATE-dragon-knell-3.md, 2026-11-17) re-predicted every row of both Knell files again, each
        // renamed with the next suffix; WA1 (the Knell walks again after a beam) is new, and KQ1R3 asks two questions.
        expected.put("dragon-beam", List.of("KB1R3", "KB2R3", "WA1", "KB3R3", "KB4R3", "KB5R3", "KB6R3", "KB7R3",
                "KB8R3", "KB9R3", "KQ1R3"));
        // Step R6 (GATE-dragon-teleport.md, 2026-11-17, stacked on R5): twelve new AUTO rows, the two re-predictions,
        // and Ben's three questions in one.
        // Ben's one more number (GATE-dragon-knell-4.md, 2026-11-17): the teleport file's rows whose lines change at
        // a 28-tick delay are renamed with the next suffix; TP5R3 and TP13R3 log the same lines at any delay and keep
        // their names (carried in knell-4). The beam file is unchanged.
        expected.put("dragon-teleport", List.of("KB10R5", "TP2R4", "TP3R4", "TP4R4", "TP5R3", "TP6R4", "TP7R4", "TP8R4",
                "TP9R4", "TP10R4", "TP11R4", "TP12R4", "TP13R3", "TP14R3", "KB11R5", "TQ1R4"));
        ScenarioLoader.Loaded loaded = shipped();
        for (var gate : expected.entrySet()) {
            List<String> rows = new ArrayList<>();
            for (Scenario s : loaded.gates().get(gate.getKey())) if (!s.row().equals(Scenario.SETUP)) rows.add(s.row());
            assertEquals(gate.getValue(), rows, gate.getKey());
        }
        // 24 CORE rows on the seat's run sheet + WS-C (made CORE 2026-10-05) - R0d (a boot line, no scenario). Then +10, GATE-sunder.md, and +3, GATE-deft-hands.md (2026-11-06).
        // Then +1, SU8's death half (the seat's ruling, 2026-11-10). Then +12, GATE-mining-auto.md (2026-11-14). Then +7, GATE-nexus-back.md (2026-11-14).
        // Then GATE-dragon-bodies.md (2026-11-15). RECOUNTED from the files, not adjusted: in selftest/, the index's
        // gates (grep -cE '^  - ' index.yml) printed 12; their top-level keys (^[A-Za-z0-9-]+:) 80; their setups 12;
        // so 80 - 12 = 68. The same three commands at master 5b71e24b print 11, 68, 11: 57, this line's old value.
        // Then GATE-dragon-split.md (2026-11-15), RECOUNTED the same way: 13 gates, 87 keys, 13 setups, so 74. At
        // feat/dragon-bodies 974e4357 the same commands print 12, 80, 12: 68, this line's old value.
        // Then GATE-dragon-growth.md (2026-11-15), RECOUNTED the same way: 14 gates, 97 keys, 14 setups, so 83. At
        // feat/dragon-split 56a8bb00 the same commands print 13, 87, 13: 74, this line's old value.
        // Then GATE-dragon-fix.md (2026-11-15), RECOUNTED the same way: 14 gates, 96 keys, 14 setups, so 82. At
        // feat/dragon-growth 39bb2e90 the same commands print 14, 97, 14: 83, this line's old value.
        // Then GATE-dragon-blast.md (step R3, 2026-11-15), RECOUNTED the same way: 15 gates, 106 keys, 15 setups, so 91.
        // At master 94915476 the same commands print 14, 96, 14: 82, this line's old value.
        // Then BL9, vanilla's blast shape (the seat's brief of 2026-11-16), RECOUNTED the same way: 15 gates, 107 keys,
        // 15 setups, so 92. At feat/dragon-blast 58165392 the same commands print 15, 106, 15: 91, this line's old value.
        // Then the R3 audit of the older dragon rows (the seat's brief of 2026-11-16), RECOUNTED the same way: 15 gates,
        // 105 keys, 15 setups, so 90. At feat/dragon-blast ba74df59 the same commands print 15, 107, 15: 92, this
        // line's old value.
        // Then the follow-up of 2026-11-17 (GATE-dragon-blast-2.md), which RENAMES the blast gate's sixteen rows and adds
        // none, RECOUNTED the same way: 15 gates, 105 keys, 15 setups, so 90. At master ca0ae38a the same commands
        // print 15, 105, 15: 90, the same.
        // Then GATE-dragon-beam.md (step R5, 2026-11-17), RECOUNTED the same way: 16 gates, 118 keys, 16 setups, so 102.
        // At master 8671ab16 the same commands print 15, 105, 15: 90, this line's old value.
        // Then GATE-dragon-teleport.md (step R6, 2026-11-17), RECOUNTED the same way: 17 gates, 132 keys, 17 setups,
        // so 115. At feat/dragon-beam 1bfee281 the same commands print 16, 118, 16: 102, this line's old value.
        // Then GATE-dragon-knell-2.md (the Knell follow-up, 2026-11-17), RECOUNTED the same way: 17 gates, 133 keys, 17
        // setups, so 116. At master 7a3e9985 the same commands print 17, 132, 17: 115, this line's old value.
        // Then GATE-dragon-knell-3.md (Ben's re-timing, 2026-11-17), RECOUNTED the same way: 17 gates, 134 keys, 17
        // setups, so 117. At master 232f82b4 the same commands print 17, 133, 17: 116, this line's old value.
        // Then GATE-dragon-knell-4.md (Ben's one more number, 2026-11-17), RECOUNTED: 17 gates, 134 keys, 17 setups,
        // so 117, unchanged: it renames rows and adds none. At master 6b587faa the same commands print the same, so
        // this line cannot tell the two apart; the row lists above can.
        assertEquals(117, loaded.rows());
    }

    /**
     * NO DRAGON ROW OUTSIDE GATE-dragon-blast.yml LEAVES A LIVE PHAGE BESIDE THE PLAYER (the seat's brief of
     * 2026-11-16, item 4). From step R3 a live Phage within 3 blocks swells and BLOWS, so a row that never asked for
     * one would read a blast its prediction does not name. Stated on the FILES, in two halves:
     * <ol>
     *   <li>every SPAWN of a {@code phage} outside the blast gate is {@code frozen}, and no CMD there sets
     *       {@code NoAI} to anything but {@code 1b} (the only way a step could thaw one);</li>
     *   <li>a split child of a frozen tagged parent is born frozen ({@code SelfTestSnapshot.freezeSplitChild},
     *       {@code SelfTestSplitChildTest}), so every Phage those spawns can make is frozen too.</li>
     * </ol>
     * A frozen Phage never swells: NoAI stops its goals ({@code Mob.isEffectiveAi}) and only its swell goal (or an
     * ignite, which ours refuses) turns its swell up. <b>The positive control</b> is the blast gate itself: the same
     * scan must FIND its three live spawns and DS1R4's one THAW there (since 2026-11-17), or the scan is blind and its silence elsewhere
     * proves nothing.
     */
    @Test
    void noScenarioOutsideTheBlastGateLeavesALivePhage() {
        List<String> blastGate = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            List<String> found = new ArrayList<>();
            for (Scenario s : gate.getValue()) {
                for (Scenario.Step step : s.steps()) {
                    if (livePhage(step)) found.add(s.row() + " " + step.arg());
                }
            }
            if (gate.getKey().equals("dragon-blast")) blastGate.addAll(found);
            else assertEquals(List.of(), found, gate.getKey() + " has a live Phage; it belongs in the blast gate");
        }
        // Since 2026-11-17 (GATE-dragon-blast-2.md): BQ1R2's two Phages are FROZEN and blown, and DS1R4 thaws with the
        // instrument's THAW step instead of a data merge.
        assertEquals(List.of("BL1R2 bl1 live phage 100", "BL2R2 bl2 live phage stage 5 60", "BL5R2 bl5 live phage 100",
                        "DS1R4 ds1"),
                blastGate, "the control: the scan must find every live Phage the blast gate stages on purpose");
    }

    /**
     * A step that puts a live Phage in the world: a live phage SPAWN, a CMD that sets NoAI to anything but 1b, or a
     * THAW (which switches AI on for whatever carries its tag).
     */
    private static boolean livePhage(Scenario.Step step) {
        if (step.verb() == Scenario.Verb.THAW) return true;
        if (step.verb() == Scenario.Verb.SPAWN) {
            SelfTestSpawn.Spec spec = SelfTestSpawn.parse(step.arg()).orElseThrow();
            return spec.mob().equals("phage") && !spec.frozen();
        }
        if (step.verb() == Scenario.Verb.CMD) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("NoAI:([^,}\\]]*)").matcher(step.arg());
            while (m.find()) if (!m.group(1).equals("1b")) return true;
        }
        return false;
    }

    @Test
    void everyScenarioNamesARowOfItsGate() throws IOException {
        for (var gate : shipped().gates().entrySet()) {
            Path md = Path.of("..", "docs", "gates", "GATE-" + docOf(gate.getKey()) + ".md");
            Set<String> headings = ScenarioLoader.headings(Files.readString(md, StandardCharsets.UTF_8));
            assertTrue(headings.size() >= 7, md + " read as " + headings + " -- the heading pattern found too few rows");
            assertEquals(List.of(), ScenarioLoader.unpaired(gate.getValue(), headings), md.toString());
        }
    }

    /**
     * Which gate file a scenario file's rows are headings in. Its own, except the three dragon gates, whose rows since
     * the 2026-11-15 boot are re-predictions held in GATE-dragon-fix.md, the stack top (its brief: the old
     * predictions and readings stay as they are, and every re-run row is a new row named for the old one).
     * NAMED, not a prefix: {@code dragon-blast} (step R3) is a dragon gate with its OWN file, and a prefix test sent it
     * to GATE-dragon-fix.md.
     */
    static String docOf(String gate) {
        if (gate.equals("dragon-blast")) return "dragon-blast-2";   // re-predicted 2026-11-17, rows in the -2 gate
        // Ben's re-timing of 2026-11-17 (KNELL 3): both Knell files' rows are GATE-dragon-knell-3.md's re-predictions
        // (before it, GATE-dragon-knell-2.md's).
        // Ben's one more number (KNELL 4): the teleport file's rows are GATE-dragon-knell-4.md's; the beam file's stay
        // GATE-dragon-knell-3.md's (its lines do not change at a 28-tick delay).
        if (gate.equals("dragon-teleport")) return "dragon-knell-4";
        if (gate.equals("dragon-beam")) return "dragon-knell-3";
        return Set.of("dragon-bodies", "dragon-split", "dragon-growth").contains(gate) ? "dragon-fix" : gate;
    }

    /**
     * NO ROW OUTSIDE GATE-dragon-beam.yml LEAVES A KNELL WITH ITS AI ON (the R5 audit, GATE-dragon-beam.md, THE ROWS
     * ALREADY MERGED). Since step R5 a Knell with AI begins a beam of its own 100 to 200 ticks after it appears; a
     * frozen (NoAI) one never does (I23). So an older row's Knell must be spawned frozen, or by {@code rpg spawn knell}
     * followed at once by a {@code data merge} that sets {@code NoAI:1b}, or be a spawn the command refuses (DB11R2's
     * {@code stage 2}). The control: the beam gate's own two live Knells (KB2, KB11) ARE found.
     */
    @Test
    void noScenarioOutsideTheBeamGateLeavesALiveKnell() {
        List<String> live = new ArrayList<>();
        List<String> beamGate = new ArrayList<>();
        for (var gate : shipped().gates().entrySet()) {
            for (Scenario s : gate.getValue()) {
                List<Scenario.Step> steps = s.steps();
                for (int i = 0; i < steps.size(); i++) {
                    Scenario.Step st = steps.get(i);
                    String a = st.arg().trim();
                    boolean liveSpawn = st.verb() == Scenario.Verb.SPAWN && a.matches("\\S+ live knell\\b.*");
                    boolean bareCmd = st.verb() == Scenario.Verb.CMD && a.matches("rpg spawn knell( \\d+)?");
                    boolean frozenAtOnce = i + 1 < steps.size() && steps.get(i + 1).verb() == Scenario.Verb.CMD
                            && steps.get(i + 1).arg().contains("NoAI:1b");
                    if (!(liveSpawn || (bareCmd && !frozenAtOnce))) continue;
                    String where = gate.getKey() + "#" + s.row() + " " + st.verb() + " " + a;
                    if (Set.of("dragon-beam", "dragon-teleport").contains(gate.getKey())) beamGate.add(where); else live.add(where);
                }
            }
        }
        assertEquals(List.of(), live, "a live Knell outside the beam gate would beam the player at a time no row predicts");
        assertEquals(3, beamGate.size(), "the control: the beam file's KB2R3 and WA1 and the teleport file's KB11R5 are "
                + "seen: " + beamGate);
    }

    /** The dragon gates declare the difficulty their rows need, first thing in their setup (GATE-dragon-fix.md, C5). */
    @Test
    void theDragonGatesDeclareNormalFirstInTheirSetup() {
        for (String gate : List.of("dragon-bodies", "dragon-split", "dragon-growth", "dragon-blast", "dragon-beam",
                "dragon-teleport")) {
            Scenario setup = shipped().gates().get(gate).getFirst();
            assertEquals(Scenario.SETUP, setup.row(), gate);
            assertEquals(new Scenario.Step(Scenario.Verb.DIFFICULTY, "normal"), setup.steps().getFirst(), gate);
        }
    }

    @Test
    void controlAnUnpairedKeyAndAnUnknownVerbAreBothCaught() throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("""
                WB2:
                  - wait: 5
                WB9:
                  - wait: 5
                WB3:
                  - teleport: "~ ~ ~"
                """);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("withered-shortbow", file, refusals);
        assertEquals(1, refusals.size(), refusals.toString());
        assertTrue(refusals.getFirst().contains("unknown verb 'teleport'"), refusals.getFirst());
        assertEquals(List.of("WB2", "WB9"), scenarios.stream().map(Scenario::row).toList(), "WB3 is refused whole");
        assertEquals(List.of("WB9"), ScenarioLoader.unpaired(scenarios, Set.of("WB2", "WB3")));
    }

    @Test
    void setupRunsFirstWhereverItIsWritten() throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("""
                WB1:
                  - note: "row"
                setup:
                  - note: "setup"
                """);
        List<Scenario> scenarios = ScenarioLoader.parse("g", file, new ArrayList<>());
        assertEquals(List.of(Scenario.SETUP, "WB1"), scenarios.stream().map(Scenario::row).toList());
    }

    @Test
    void theHeadingPatternReadsRowIdsAndNotSectionTitles() {
        String md = "## R0\n### R0a — the build\n### WS-C — negative\n#### not a row\n### WB4b — cap\n";
        assertEquals(List.of("R0a", "WS-C", "WB4b"), List.copyOf(ScenarioLoader.headings(md)));
    }
}
