package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE SEAT'S R2 (2026-11-26, the readings of the boot of {@code 1f447197}): BT3 and BT5 ended with {@code clear_state} while
 * the Blink Tome was still equipped and a NoAI zombie still targeted him, so the cleared cooldown let him blink again
 * before the row ended (ticks 34213 and 34373), from a spot {@code BlinkPadLandingTest} never enumerated. The rule
 * {@link ScenarioLint.Rule#ACCESSORY_ARMED_AT_CLEAR}: a row that has equipped an accessory AND staged a {@code mob_target}
 * takes the accessory off ({@code accessory: clear}, then waits at least {@link ScenarioLint#ACCESSORY_SETTLE_TICKS}, past
 * the 5-tick reconcile pass that stops its clock) before any {@code clear_state} and before the row ends.
 *
 * <p>The control: the OLD BT3 (its last steps as they shipped at {@code 6c9b1ff4}, copied whole below) fails it, and the
 * shipped gate passes it ({@code BlinkTomeGateCommandTest.theGateLintsClean_andAsksNothing}, every rule).
 */
class AccessoryArmedAtClearLintTest {

    private static List<ScenarioLint.Rule> rules(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("g", file, refusals);
        assertEquals(List.of(), refusals, "the fixture itself must load");
        return ScenarioLint.lint("g", scenarios).stream().map(ScenarioLint.Finding::rule).toList();
    }

    private static final String SETUP = """
            setup:
              - pad: ""
              - preflight: "0 -13 36 13"
            """;

    /** BT3 as it shipped at 6c9b1ff4 (the boot of 1f447197 ran it): equipped, two mob_targets, and its last clear_state. */
    private static final String OLD_BT3 = """
            BT3:
              - cmd: "rpg playerxp set @s 2 levels"
              - clear_inventory: ""
              - clear_state: ""
              - accessory: "clear"
              - class: "mage"
              - wait: 40
              - cmd: 'summon zombie ~23 ~ ~0 {NoAI:1b,PersistenceRequired:1b,IsBaby:0b,Silent:1b,Tags:["bt","selftest"]}'
              - cmd: "tp @s ~20 ~ ~"
              - cmd: "rpg give blink_tome"
              - wait: 2
              - hold: blink_tome
              - accessory: "equip 0"
              - wait: 10
              - mob_target: bt
              - wait: 8
              - cmd: 'summon zombie ~0 ~ ~0 {NoAI:1b,PersistenceRequired:1b,IsBaby:0b,Silent:1b,Tags:["bh","selftest"]}'
              - mob_target: bh
              - probe_target: bh
              - probe_invisibility: ""
              - wait: 160
              - probe_invisibility: ""
              - mob_target: bh
              - wait: 2
              - probe_target: bh
              - wait: 20
              - clear_state: ""
            """;

    private static String withTail(String tail) {
        return SETUP + OLD_BT3.replace("""
                  - wait: 20
                  - clear_state: ""
                """, tail);
    }

    @Test
    void theOldBt3Fails() throws InvalidConfigurationException {
        assertEquals(List.of(ScenarioLint.Rule.ACCESSORY_ARMED_AT_CLEAR), rules(SETUP + OLD_BT3));
    }

    @Test
    void takenOffAndSettledFirst_passes() throws InvalidConfigurationException {
        assertEquals(List.of(), rules(withTail("""
                  - wait: 20
                  - accessory: "clear"
                  - wait: 6
                  - clear_state: ""
                """)));
    }

    @Test
    void theSettleIsSixTicks_fiveIsNotEnough() throws InvalidConfigurationException {
        assertEquals(List.of(ScenarioLint.Rule.ACCESSORY_ARMED_AT_CLEAR), rules(withTail("""
                  - wait: 20
                  - accessory: "clear"
                  - wait: 5
                  - clear_state: ""
                """)), "five ticks can land inside the reconcile pass that stops the clock");
        assertEquals(List.of(), rules(withTail("""
                  - wait: 20
                  - accessory: "clear"
                  - wait: 3
                  - wait: 3
                  - clear_state: ""
                """)), "the waits after the clear add up");
    }

    @Test
    void theRowsEndCountsAsAClear() throws InvalidConfigurationException {
        assertEquals(List.of(ScenarioLint.Rule.ACCESSORY_ARMED_AT_CLEAR), rules(withTail("""
                  - wait: 20
                """)), "a row ending with the tome on hands the next row's CLEAN the same state");
    }

    @Test
    void reEquippingArmsItAgain() throws InvalidConfigurationException {
        assertEquals(List.of(ScenarioLint.Rule.ACCESSORY_ARMED_AT_CLEAR), rules(withTail("""
                  - wait: 20
                  - accessory: "clear"
                  - wait: 6
                  - cmd: "rpg give blink_tome"
                  - wait: 2
                  - hold: blink_tome
                  - accessory: "equip 0"
                  - wait: 30
                  - clear_state: ""
                """)), "BT7's shape: a re-equip after the clear");
    }

    @Test
    void noMobStagedAsTargetingHim_isNotAFinding() throws InvalidConfigurationException {
        // The Healing Tome's rows equip and clear_state with no mob_target: nothing can trigger, nothing to refuse.
        assertEquals(List.of(), rules(SETUP + """
                HT:
                  - cmd: "rpg playerxp set @s 2 levels"
                  - clear_inventory: ""
                  - clear_state: ""
                  - accessory: "clear"
                  - class: "mage"
                  - wait: 40
                  - cmd: "rpg give healing_tome"
                  - wait: 2
                  - hold: healing_tome
                  - accessory: "equip 0"
                  - wait: 10
                  - clear_state: ""
                """));
    }
}
