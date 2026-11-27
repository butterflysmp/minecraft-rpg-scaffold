package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE SEAT'S P5 (2026-11-27): a gate's SETUP BEGINS WITH {@code pad:}, so no new gate can ship that runs on Ben's ground.
 * The gates that predate ruling A (2026-11-23) have no pad step at all; they are REPORTED as KNOWN, by name, the count
 * pinned, and are not changed in this stack (to be refused or moved to the pad in a later PR the seat will rule).
 *
 * <p>CORRECTED COUNT: CC's F5 report said 40 and the seat's ruling repeated it; READ over the shipped files (55 gates,
 * 13 with a pad step, all as their setup's first step), it is 42. None of the 42 has a pad step anywhere.
 */
class PadFirstLintTest {

    /** KNOWN: the gates whose setup does not begin with pad (none has a pad step at all). Edited by NOBODY but a ruling. */
    static final List<String> KNOWN = List.of("boltor-recipe", "breacher", "breacher-rework", "crystal-shard-staff",
            "deft-hands", "drag-additive", "dragon-beam", "dragon-blast", "dragon-blink", "dragon-bodies", "dragon-growth",
            "dragon-shot", "dragon-skreel", "dragon-split", "dragon-teleport", "fight-arena", "fight-down", "fight-dragon",
            "fight-end", "fight-revive", "fight-rules", "fight-site", "fight-template", "knell-beam", "legacy-a", "legacy-b",
            "level", "living-lantern", "loot-roll", "melee-cell", "mining-auto", "nexus-back", "nexus-polish",
            "remove-dev-weapons", "sunder", "sword-convert", "sword-smithing", "sword-tiers", "unique-enchant-slot",
            "vanilla-bows", "wither-status", "withered-shortbow");

    private static List<ScenarioLint.Rule> rules(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("g", file, refusals);
        assertEquals(List.of(), refusals);
        return ScenarioLint.lint("g", scenarios).stream().map(ScenarioLint.Finding::rule).toList();
    }

    /** Mutation: the rule disabled, or any gate added to or dropped from the known set. */
    @Test
    void everyShippedGateBeginsWithPad_butTheKnownOnes() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path -> PadFirstLintTest.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), loaded.refusals());
        TreeSet<String> flagged = new TreeSet<>();
        for (var gate : loaded.gates().entrySet()) {
            for (ScenarioLint.Finding f : ScenarioLint.lint(gate.getKey(), gate.getValue())) {
                if (f.rule() == ScenarioLint.Rule.PAD_FIRST) flagged.add(f.gate());
            }
        }
        assertEquals(42, KNOWN.size(), "the count, pinned");
        assertEquals(new TreeSet<>(KNOWN), flagged, "exactly the known gates, and no new one");
    }

    @Test
    void aSetupThatDoesNotBeginWithPad_isAFinding() throws InvalidConfigurationException {
        assertEquals(List.of(ScenarioLint.Rule.PAD_FIRST), rules("""
                setup:
                  - cmd: "time set 18000"
                  - pad: ""
                  - preflight: "0 -13 36 13"
                """), "a pad step that is not FIRST: whatever runs before it runs on his ground");
        assertEquals(List.of(ScenarioLint.Rule.PAD_FIRST), rules("""
                setup:
                  - cmd: "time set 18000"
                """), "no pad step at all");
        assertEquals(List.of(), rules("""
                setup:
                  - pad: ""
                  - preflight: "0 -13 36 13"
                  - cmd: "time set 18000"
                """), "the control: pad first");
    }
}
