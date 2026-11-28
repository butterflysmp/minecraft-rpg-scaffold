package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE SEAT'S X6 (b) (2026-11-28): HIS VIEW IS SET BEFORE EVERY ACT THAT READS IT. A press, the stone's {@code drop} and a
 * look-reading command ({@code rpg mobdamage|mobheal|mobinfo|apply|cast}) each have a view-setting step (a {@code tp}
 * with a rotation, an {@code aim}) at most two ticks before them in the row. The gates that broke it when it was written
 * are named in {@code ScenarioLint.VIEW_RULE_PREDATES} (frozen readings, not re-staged); every other gate is clean.
 */
class ViewUnsetLintTest {

    private static List<ScenarioLint.Rule> rules(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("g", file, refusals);
        assertEquals(List.of(), refusals);
        return ScenarioLint.lint("g", scenarios).stream().map(ScenarioLint.Finding::rule)
                .filter(r -> r == ScenarioLint.Rule.VIEW_UNSET).toList();
    }

    private static final String SETUP = "setup:\n  - pad: \"\"\n";

    /** The scan: every shipped gate the rule flags. Printed, then pinned against the named list. */
    @Test
    void theRuleFlagsExactlyTheGatesNamedAsPredatingIt() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path -> ViewUnsetLintTest.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), loaded.refusals());
        TreeSet<String> flagged = new TreeSet<>();
        for (var gate : loaded.gates().entrySet()) {
            if (ScenarioLint.VIEW_RULE_PREDATES.contains(gate.getKey())) {
                // Run the rule as if the gate were new, to show the named gate really breaks it.
                List<ScenarioLint.Finding> raw = new ArrayList<>();
                ScenarioLint.viewSetIgnoringPredates(gate.getKey(), gate.getValue(), raw);
                if (!raw.isEmpty()) flagged.add(gate.getKey());
                continue;
            }
            for (ScenarioLint.Finding f : ScenarioLint.lint(gate.getKey(), gate.getValue())) {
                if (f.rule() == ScenarioLint.Rule.VIEW_UNSET) flagged.add(f.gate() + " (NEW)");
            }
        }
        assertEquals(32, ScenarioLint.VIEW_RULE_PREDATES.size(), "the count, pinned (read by this scan, 2026-11-28)");
        assertEquals(new TreeSet<>(ScenarioLint.VIEW_RULE_PREDATES), flagged,
                "every named gate breaks it, and no other gate does");
    }

    /** A drop one tick after a tp with a rotation: clean. The control. */
    @Test
    void aCastOneTickAfterTheViewIsSet_isClean() throws InvalidConfigurationException {
        assertEquals(List.of(), rules(SETUP + "R:\n  - hold: stone\n  - cmd: \"tp @s ~ ~ ~ -90 0\"\n  - wait: 1\n  - drop: selected\n"));
        assertEquals(List.of(), rules(SETUP + "R:\n  - cmd: \"tp @s ~ ~ ~ -90 0\"\n  - wait: 2\n  - cmd: \"rpg mobdamage 20\"\n"));
    }

    /** No view in the row; a view three ticks before; a look-reading command with none: each a finding. */
    @Test
    void aCastOrALookReadingCommandWithNoViewInTheTwoTicksBefore_isAFinding() throws InvalidConfigurationException {
        assertEquals(List.of(ScenarioLint.Rule.VIEW_UNSET), rules(SETUP + "R:\n  - hold: stone\n  - drop: selected\n"));
        assertEquals(List.of(ScenarioLint.Rule.VIEW_UNSET),
                rules(SETUP + "R:\n  - cmd: \"tp @s ~ ~ ~ -90 0\"\n  - wait: 3\n  - drop: selected\n"));
        assertEquals(List.of(ScenarioLint.Rule.VIEW_UNSET), rules(SETUP + "R:\n  - cmd: \"rpg mobdamage 2000\"\n"));
        assertEquals(List.of(ScenarioLint.Rule.VIEW_UNSET), rules(SETUP + "R:\n  - cmd: \"rpg apply rooted 1\"\n"));
        assertEquals(List.of(), rules(SETUP + "R:\n  - cmd: \"rpg mana refill\"\n"), "CONTROL: a command that reads no look");
    }
}
