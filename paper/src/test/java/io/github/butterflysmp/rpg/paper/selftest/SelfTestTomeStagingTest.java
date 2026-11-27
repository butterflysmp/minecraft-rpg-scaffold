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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H15 (the seat, 2026-11-26): the verbs that stage a class and the accessory slots go through the REAL functions; the
 * run's snapshot captures his class and four slots and puts them back; a row that equips stages class, slots and level
 * itself (lint); and the HEALED line, which names his regeneration's every term.
 */
class SelfTestTomeStagingTest {

    private static final Path DIR = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "selftest");

    private static String code(String file) throws IOException {
        StringBuilder b = new StringBuilder();
        for (String l : Files.readAllLines(DIR.resolve(file), StandardCharsets.UTF_8)) {
            String t = l.trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            b.append(l).append('\n');
        }
        return b.toString();
    }

    private static String between(String src, String from, String to) {
        int a = src.indexOf(from);
        assertTrue(a >= 0, "'" + from + "' is not found -- the scan is blind");
        int z = src.indexOf(to, a + from.length());
        assertTrue(z > a, "no '" + to + "' after '" + from + "'");
        return src.substring(a, z);
    }

    @Test
    void theNewVerbsArgumentsAreCheckedAtLoad() {
        for (String ok : List.of("mage", "melee", "ranger", "none")) {
            assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.CLASS, ok), ok);
        }
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.CLASS, "Mage").isPresent(), "the profile token is lowercase");
        for (String ok : List.of("clear", "equip 0", "equip 3")) {
            assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.ACCESSORY, ok), ok);
        }
        for (String bad : List.of("equip 4", "equip", "unequip 0", "")) {
            assertTrue(SelfTestStaging.refusal(Scenario.Verb.ACCESSORY, bad).isPresent(), bad);
        }
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.SATURATION, "0"));
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.SATURATION, "21").isPresent());
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.SATURATION, "lots").isPresent());
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.PROBE_REGEN, ""));
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PROBE_REGEN, "x").isPresent());
    }

    @Test
    void theHealedLine() {
        assertEquals("HEALED amount=1.000 after=103.500/105.000 dealer=self",
                SelfTestRun.healedLine(1.0, 103.5, 105.0, "self"));
    }

    private static List<ScenarioLint.Finding> lint(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("g", file, refusals);
        assertEquals(List.of(), refusals, "the fixture itself must load");
        List<ScenarioLint.Finding> out = new ArrayList<>();
        ScenarioLint.accessoryStaged("g", scenarios, out);
        return out;
    }

    /** A row that equips stages class, slots and level BEFORE the equip, in itself. Each missing one is named. */
    @Test
    void aRowThatEquipsStagesClassSlotsAndLevelItself() throws InvalidConfigurationException {
        String full = """
                T1:
                  - cmd: "rpg playerxp set @s 2 levels"
                  - accessory: "clear"
                  - class: "mage"
                  - accessory: "equip 0"
                """;
        assertEquals(List.of(), lint(full));
        List<ScenarioLint.Finding> none = lint("""
                T1:
                  - accessory: "equip 0"
                """);
        assertEquals(1, none.size());
        assertEquals(ScenarioLint.Rule.ACCESSORY_UNSTAGED, none.get(0).rule());
        assertTrue(none.get(0).toString().contains("class") && none.get(0).toString().contains("accessory: clear")
                && none.get(0).toString().contains("a level"), none.get(0).toString());
        // AFTER the equip does not count.
        assertEquals(1, lint("""
                T1:
                  - accessory: "clear"
                  - class: "mage"
                  - accessory: "equip 0"
                  - cmd: "rpg playerxp set @s 2 levels"
                """).size());
        // Nor does a row before it: nothing is carried.
        assertEquals(1, lint("""
                T1:
                  - cmd: "rpg playerxp set @s 2 levels"
                  - accessory: "clear"
                  - class: "mage"
                T2:
                  - accessory: "equip 0"
                """).size());
        // Mutation: drop the class check -> the first finding's text loses "class" and the third fixture still reddens.
    }

    /** H15's paper wiring: captured in the constructor, on the CAPTURED line, restored after the cell, on the RESTORED line. */
    @Test
    void theSnapshotCapturesAndRestoresHisClassAndSlots() throws IOException {
        String s = code("SelfTestSnapshot.java");
        String ctor = between(s, "private SelfTestSnapshot(Player p,", "static SelfTestSnapshot take(");
        assertTrue(ctor.contains("this.accessorySlots = SelfTestAccessorySlots.capture(accessoryPort);"));
        assertTrue(ctor.contains("this.classId = profile == null ? null : profile.archetypeId();"), "his class, captured");
        assertTrue(between(s, "String capturedFields() {", "\n    }").contains(
                "\" class=\" + classId + \" accessories=\" + accessorySlots.fields(accessoryPort, accessoryName)"));
        String restore = between(s, "String restore(Player p, ProfileService profiles,", "\n    }\n");
        int cell = restore.indexOf("profiles.setCell(id, classId, elementId)");
        int slots = restore.indexOf("accessorySlots.restore(accessoryPort)");
        assertTrue(cell > 0 && slots > cell, "his slots go back AFTER his class, so the class gate reads his own class");
        assertTrue(restore.contains("+ accessoriesLine"), "and their fields are on the RESTORED line");
        assertTrue(restore.contains("p.setSaturation(saturation);"));
        String port = between(s, "private static SelfTestAccessorySlots.Port<", "private static String accessoryName(");
        assertTrue(port.contains("return service.accessories(id);"), "read through the service");
        assertTrue(port.contains("return service.replace(id, record, ok -> { });"), "written through the service");
        assertTrue(code("SelfTestRun.java").contains("service.stats(), service.adapters());"), "the run hands it the store");
        // Mutation: delete the accessorySlots.restore line -> reddens.
    }

    /** The verbs go through the real functions: the Build screen's class write, the Equipment screen's verdict and store. */
    @Test
    void theVerbsUseTheRealFunctions() throws IOException {
        String d = code("SelfTestDrivers.java");
        String cls = between(d, "private Optional<String> stageClass(Player p, String a) {", "\n    }\n");
        assertTrue(cls.contains("BuildRules.elementAfterClassPick("));
        assertTrue(cls.contains("run.profiles().setCell(id, classId, element)"));
        String acc = between(d, "private Optional<String> stageAccessory(Player p, String a) {", "\n    }\n");
        assertTrue(acc.contains("AccessorySlots.canEquip(slot, def.get(), profileClass)"));
        assertTrue(acc.contains("service.write(id, slot, text,"));
        assertTrue(acc.contains("service.write(id, slot, null,"));
        assertTrue(acc.contains("return Optional.of(\"slot \" + slot + \" is occupied; clear it first\");"),
                "an equip never overwrites a stored item");
    }
}
