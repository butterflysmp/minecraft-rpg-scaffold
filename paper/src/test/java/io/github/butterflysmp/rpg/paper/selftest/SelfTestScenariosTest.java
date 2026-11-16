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
                "dragon-split", "dragon-growth"),
                List.copyOf(loaded.gates().keySet()),
                "the fourteen gates, in boot order; nexus-back after mining-auto (2026-11-14), then dragon-bodies,"
                        + " dragon-split and dragon-growth LAST (2026-11-15)");
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
        expected.put("dragon-bodies", List.of("DB1", "DB2", "DB3", "DB4", "DB5", "DB6", "DB7", "DB8", "DB9", "DB10",
                "DB11"));
        expected.put("dragon-split", List.of("DS1", "DS2", "DS3", "DS4", "DS5", "DS6"));
        expected.put("dragon-growth", List.of("DG1", "DG2", "DG3", "DG4", "DG5", "DG6", "DG7", "DG8", "DG9"));
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
        assertEquals(83, loaded.rows());
    }

    @Test
    void everyScenarioNamesARowOfItsGate() throws IOException {
        for (var gate : shipped().gates().entrySet()) {
            Path md = Path.of("..", "docs", "gates", "GATE-" + gate.getKey() + ".md");
            Set<String> headings = ScenarioLoader.headings(Files.readString(md, StandardCharsets.UTF_8));
            assertTrue(headings.size() >= 7, md + " read as " + headings + " -- the heading pattern found too few rows");
            assertEquals(List.of(), ScenarioLoader.unpaired(gate.getValue(), headings), md.toString());
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
