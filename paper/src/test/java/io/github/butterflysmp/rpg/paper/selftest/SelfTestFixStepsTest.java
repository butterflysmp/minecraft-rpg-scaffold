package io.github.butterflysmp.rpg.paper.selftest;

import org.bukkit.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The instrument steps added after the 2026-11-15 boot of the dragon stack (GATE-dragon-fix.md): SPAWN tags a row's
 * mob by identity at spawn, ASK puts a question to the player and logs his answer verbatim, the row-start LEFTOVER
 * line says what a clean left behind, and DIFFICULTY declares the difficulty a gate's rows need. Each grammar is
 * refused at LOAD, by name, the way an unknown verb is.
 */
class SelfTestFixStepsTest {

    // --- SPAWN: "<tag> <frozen|live> <mob> [stage <n>] [<gs>]" ---

    @Test
    void aSpawnNamesItsTagItsAiItsMobItsStageAndItsGearScore() {
        SelfTestSpawn.Spec s = SelfTestSpawn.parse("ds1 frozen phage stage 5 300").orElseThrow();
        assertEquals("ds1", s.tag());
        assertTrue(s.frozen());
        assertEquals("phage", s.mob());
        assertEquals(5, s.stage());
        assertEquals(300, s.gearScore());

        SelfTestSpawn.Spec live = SelfTestSpawn.parse("db10 live phage 100").orElseThrow();
        assertFalse(live.frozen(), "live: the AI stays on");
        assertNull(live.stage(), "no stage given: the command's own default (stage 1)");
        assertEquals(100, live.gearScore());

        SelfTestSpawn.Spec bare = SelfTestSpawn.parse("x frozen knell").orElseThrow();
        assertNull(bare.stage());
        assertNull(bare.gearScore(), "no gs given: rolled, as the command does");
    }

    @Test
    void aMalformedSpawnIsRefusedNotGuessed() {
        for (String bad : List.of("", "ds1", "ds1 frozen", "ds1 asleep phage", "ds1 frozen phage stage",
                "ds1 frozen phage stage five", "ds1 frozen phage 300 7", "ds1 frozen phage stage 5 300 9",
                "ds1 frozen phage lots")) {
            assertTrue(SelfTestSpawn.parse(bad).isEmpty(), "refused: '" + bad + "'");
        }
    }

    // --- ASK: "<n> <timeout ticks> <question>" ---

    @Test
    void anAskCarriesItsNumberItsTimeoutAndItsQuestionVerbatim() {
        SelfTestAsk.Ask a = SelfTestAsk.parse("2 1200 Is the small Phage fast, but possible to back away from?")
                .orElseThrow();
        assertEquals(2, a.number());
        assertEquals(1200, a.timeoutTicks());
        assertEquals("Is the small Phage fast, but possible to back away from?", a.question());
    }

    @Test
    void aMalformedAskIsRefused() {
        for (String bad : List.of("", "1", "1 1200", "one 1200 Q?", "1 soon Q?", "0 1200 Q?", "1 0 Q?")) {
            assertTrue(SelfTestAsk.parse(bad).isEmpty(), "refused: '" + bad + "'");
        }
    }

    /** The answer is logged as he typed it, whatever it says: the instrument never interprets and never judges. */
    @Test
    void theAnswerLineIsVerbatimAndJudgesNothing() {
        SelfTestAsk.Ask a = SelfTestAsk.parse("1 1200 Is the big Phage very slow?").orElseThrow();
        assertEquals("ASK 1 question=\"Is the big Phage very slow?\" answer=\"yeah, crawling\" ticks=37",
                SelfTestAsk.answered(a, "yeah, crawling", 37));
        assertEquals("ASK 1 question=\"Is the big Phage very slow?\" answer=NOT STATED after 1200 ticks",
                SelfTestAsk.notStated(a));
        assertEquals("Selftest question 1: Is the big Phage very slow? Answer yes or no in chat. You have 60 seconds.",
                SelfTestAsk.prompt(a));
    }

    /** The scenario names the prompt (GATE-fight-mage.md, 2026-11-18): an open question is not told to answer yes or no. */
    @Test
    void theScenarioNamesWhichPromptAQuestionGets() {
        SelfTestAsk.Ask open = SelfTestAsk.parse("3 2400 words The wall now: what would you change?").orElseThrow();
        assertEquals("The wall now: what would you change?", open.question(), "the mode word is not part of it");
        assertEquals("Selftest question 3: The wall now: what would you change? Answer in your own words in chat. You have 120 seconds.",
                SelfTestAsk.prompt(open));
        SelfTestAsk.Ask yesNo = SelfTestAsk.parse("4 2400 yesno Is 3.8 right?").orElseThrow();
        assertEquals("Selftest question 4: Is 3.8 right? Answer yes or no in chat. You have 120 seconds.", SelfTestAsk.prompt(yesNo));
        assertEquals("ASK 3 question=\"The wall now: what would you change?\" answer=\"taller\" ticks=9",
                SelfTestAsk.answered(open, "taller", 9), "the answer line carries the question alone");
        assertTrue(SelfTestAsk.parse("5 2400 words").isEmpty(), "a mode word with no question is refused");
    }

    // --- LEFTOVER: what the row-start clean left in the row's radius ---

    @Test
    void aLeftoverIsALiveBodyThatIsNotThePlayerNorAnArmourStand() {
        assertTrue(SelfTestStaging.leftover(true, false, false, false), "a live mob");
        assertFalse(SelfTestStaging.leftover(true, true, false, false), "the player");
        assertFalse(SelfTestStaging.leftover(true, false, true, false), "an armour stand is scenery");
        assertFalse(SelfTestStaging.leftover(true, false, false, true), "a body already dead");
        assertFalse(SelfTestStaging.leftover(false, false, false, false), "an item, an orb, a marker");
    }

    @Test
    void theLeftoverLineCountsAndNamesWhatIsLeft() {
        assertEquals("LEFTOVER living=0 radius=16", SelfTestStaging.leftoverLine(16, List.of()));
        assertEquals("LEFTOVER living=2 radius=16 [creeper 5a4b, wither_skeleton 77c1]",
                SelfTestStaging.leftoverLine(16, List.of("creeper 5a4b", "wither_skeleton 77c1")));
    }

    // --- DIFFICULTY ---

    @Test
    void aDeclaredDifficultyIsOneOfTheFour() {
        assertEquals(Optional.of(Difficulty.NORMAL), SelfTestStaging.difficulty("normal"));
        assertEquals(Optional.of(Difficulty.HARD), SelfTestStaging.difficulty(" HARD "));
        assertTrue(SelfTestStaging.difficulty("nightmare").isEmpty());
        assertTrue(SelfTestStaging.difficulty("").isEmpty());
    }

    // --- refused at LOAD, by name, so a bad step never drives a different row ---

    @Test
    void theLoaderRefusesABadSpawnAskOrDifficultyByName() {
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("GOOD", List.of(java.util.Map.of("spawn", "g frozen phage stage 2 300"),
                java.util.Map.of("ask", "1 600 Q?"), java.util.Map.of("difficulty", "normal")));
        yaml.set("BADSPAWN", List.of(java.util.Map.of("spawn", "g asleep phage")));
        yaml.set("BADASK", List.of(java.util.Map.of("ask", "one 600 Q?")));
        yaml.set("BADDIFF", List.of(java.util.Map.of("difficulty", "nightmare")));
        List<String> refusals = new ArrayList<>();
        List<Scenario> parsed = ScenarioLoader.parse("g", yaml, refusals);
        assertEquals(List.of("GOOD"), parsed.stream().map(Scenario::row).toList());
        assertEquals(3, refusals.size(), refusals.toString());
        assertTrue(refusals.get(0).startsWith("Refusing selftest/GATE-g.yml#BADSPAWN: "), refusals.get(0));
        assertTrue(refusals.get(1).startsWith("Refusing selftest/GATE-g.yml#BADASK: "), refusals.get(1));
        assertTrue(refusals.get(2).startsWith("Refusing selftest/GATE-g.yml#BADDIFF: "), refusals.get(2));
    }
}
