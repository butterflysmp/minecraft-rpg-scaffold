package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S V7 (a), (b) and (c), 2026-11-27, owed from the boot of {@code ffacaa76}. (b)'s landing proof is
 * {@code BlinkPadLandingTest}'s box of drift; (d) is {@code SummonLifecycleTest} and {@code SummonHoldsTargetWiringTest};
 * (e) is {@code SelfTestPadRefusalTest}.
 */
class OwedV7Test {

    private static List<String> code(String file) throws IOException {
        return Files.readAllLines(Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "selftest",
                file), StandardCharsets.UTF_8).stream().map(String::trim).toList();
    }

    private static Scenario row(String gate, String row) {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path -> OwedV7Test.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), loaded.refusals());
        return loaded.gates().get(gate).stream().filter(s -> s.row().equals(row)).findFirst().orElseThrow();
    }

    private static List<String> steps(Scenario s) {
        return s.steps().stream().map(st -> st.verb().name().toLowerCase() + " " + st.arg().trim()).toList();
    }

    // ---- (a) -----------------------------------------------------------------------------------------------------

    /** (a): the line, as the CAPTURED line names slots; and the store-not-loaded form. */
    @Test
    void a_theLineNamesWhatEachSlotHeld() {
        assertEquals("ACCESSORIES cleared=1 pad [healing_tome,-,-,-]",
                SelfTestStaging.accessoriesClearedLine(Arrays.asList("healing_tome", null, null, null)));
        assertEquals("ACCESSORIES cleared=0 pad [-,-,-,-]", SelfTestStaging.accessoriesClearedLine(Arrays.asList(null, null,
                null, null)));
        assertEquals("ACCESSORIES cleared=2 pad [-,blink_tome,-,unreadable]",
                SelfTestStaging.accessoriesClearedLine(Arrays.asList(null, "blink_tome", null, "unreadable")));
        assertEquals("ACCESSORIES cleared=0 pad not-loaded", SelfTestStaging.ACCESSORIES_NOT_LOADED);
    }

    /**
     * (a): in the pad step's own completion, once he stands on it (after {@code stagePoint} and the ITEMS clear), before the
     * setup goes on. So every pad gate's setup clears them, the command's first included, and a gate with no pad step never
     * reaches it. Mutation: the line deleted, or moved before {@code stagePoint = } -> reddens.
     */
    @Test
    void a_thePadStepClearsHisSlots_afterHeStandsOnIt() throws IOException {
        List<String> c = code("SelfTestRun.java");
        int stand = c.indexOf("stagePoint = stand.clone();");
        int items = c.indexOf("log(SelfTestStaging.itemsClearedLine(clearDroppedItems(padStand().orElseThrow())));");
        int clear = c.indexOf("log(drivers.clearAccessoriesOnPad(player));");
        assertTrue(stand > 0 && items > stand && clear == items + 3 && c.get(clear + 1).equals("advance();"),
                "stand " + stand + ", items " + items + ", clear " + clear);
        assertEquals(1, c.stream().filter(l -> l.contains("clearAccessoriesOnPad(")).count(), "the pad step only");
        // And the driver writes each occupied slot empty through the store, as accessory: clear does.
        List<String> d = code("SelfTestDrivers.java");
        int m = d.indexOf("String clearAccessoriesOnPad(Player p) {");
        int write = d.indexOf("service.write(id, slot, null, ok -> run.log(\"PAD accessory settled slot=\" + slot + \" ok=\" + ok));");
        assertTrue(m > 0 && write > m && write < m + 20, "m " + m + ", write " + write);
    }

    private static List<ScenarioLint.Rule> rules(String yaml) throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        List<String> refusals = new ArrayList<>();
        List<Scenario> scenarios = ScenarioLoader.parse("g", file, refusals);
        assertEquals(List.of(), refusals);
        return ScenarioLint.lint("g", scenarios).stream().map(ScenarioLint.Finding::rule).toList();
    }

    /** (a): SELF_STAGED: a row equips only an accessory it holds itself. Mutation: the new arm deleted -> reddens. */
    @Test
    void a_aRowThatEquipsWithoutHoldingItsOwn_isSelfStagedFinding() throws InvalidConfigurationException {
        String head = """
                setup:
                  - pad: ""
                  - preflight: "0 -13 36 13"
                R1:
                  - cmd: "rpg playerxp set @s 2 levels"
                  - clear_inventory: ""
                  - accessory: "clear"
                  - class: "mage"
                """;
        assertEquals(List.of(ScenarioLint.Rule.SELF_STAGED), rules(head + "  - accessory: \"equip 0\"\n"),
                "equips whatever his hand carried in");
        assertEquals(List.of(), rules(head + "  - cmd: \"rpg give blink_tome\"\n  - wait: 2\n  - hold: blink_tome\n"
                + "  - accessory: \"equip 0\"\n"), "the control: given, held, equipped in the row");
        assertEquals(List.of(ScenarioLint.Rule.SELF_STAGED), rules(head + "  - hold: empty\n  - accessory: \"equip 0\"\n"),
                "an empty hand is not an item of its own");
    }

    // ---- (b) and (c) ---------------------------------------------------------------------------------------------

    /**
     * (b): S3 aligns him in the tick before the trigger is staged: the align, one tick, then {@code mob_target bt}, after
     * his wolf was cast. Mutation: the align removed, or the wait made 0 -> reddens.
     */
    @Test
    void b_s3AlignsHimInTheTickBeforeTheTrigger() {
        List<String> s = steps(row("held-target", "S3"));
        int target = s.indexOf("mob_target bt");
        assertTrue(target > 2, s.toString());
        assertEquals("cmd execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0", s.get(target - 2));
        assertEquals("wait 1", s.get(target - 1));
        assertTrue(s.indexOf("use ") < target - 2 && s.indexOf("use ") > 0, "after his wolf is cast, which is what moves him");
    }

    /**
     * (c): S5 waits 120 after the order before probing and killing (so a SUMMON hit has landed), and ENDS with a clear,
     * so the run's RESTORED reads summons=0. Mutation: the wait back to 60, or the clear dropped -> reddens.
     */
    @Test
    void c_s5WaitsForAHit_andEndsWithAClear() {
        List<String> s = steps(row("held-target", "S5"));
        int order = s.indexOf("mob_target h5");
        assertEquals(List.of("wait 120", "probe_summon 24", "cmd kill @e[tag=h5]"), s.subList(order + 1, order + 4));
        assertEquals("clear_state ", s.getLast());
    }
}
