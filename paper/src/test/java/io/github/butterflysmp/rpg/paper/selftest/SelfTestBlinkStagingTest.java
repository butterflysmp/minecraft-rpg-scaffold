package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Blink Tome's selftest pieces (K9 and the gate): the invisibility probe, the mob_target line's tick, and the three
 * places a run clears a blink (clear_state, the row-start CLEAN, the run's end on every exit).
 */
class SelfTestBlinkStagingTest {

    private static final String S = "src/main/java/io/github/butterflysmp/rpg/paper/selftest/";

    private static String source(String f) throws IOException {
        return Files.readString(Path.of(S + f), StandardCharsets.UTF_8).replace("\r", "");
    }

    @Test
    void theInvisibilityProbe_takesNothing_andItsLine() {
        assertEquals(Optional.empty(), SelfTestStaging.refusal(Scenario.Verb.PROBE_INVISIBILITY, ""));
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.PROBE_INVISIBILITY, "x").isPresent());
        assertEquals("INVIS present=true ticks=152 amp=0 particles=false blinked=true",
                SelfTestStaging.invisibilityLine(true, 152, 0, false, true));
        assertEquals("INVIS present=false ticks=0 amp=0 particles=false blinked=false",
                SelfTestStaging.invisibilityLine(false, 0, 0, false, false));
    }

    @Test
    void mobTargetGoesThroughTheSeam_andPrintsItsTick() throws IOException {
        String d = source("SelfTestDrivers.java");
        assertTrue(d.contains("run.log(\"DRIVE mob_target \" + best.getUniqueId() + \" tag=\" + tag + (set ? \"\" : \" refused\")"
                + " + \" tick=\" + Bukkit.getCurrentTick());"));
    }

    /** K9: clear_state, the row-start CLEAN and the run's end each end the blink and say whether one was live. */
    @Test
    void theThreeClears() throws IOException {
        String d = source("SelfTestDrivers.java");
        assertTrue(d.contains("adapters.attacks().attacked(p, io.github.butterflysmp.rpg.core.accessory.Blinks.EndReason.CLEARED);"));
        assertTrue(d.contains("+ \" drum=\" + drum + \" blink=\" + (blink ? \"ended\" : \"none\"));"), "clear_state's field");
        String r = source("SelfTestRun.java");
        assertEquals(2, count(r, "attacks().attacked(player, io.github.butterflysmp.rpg.core.accessory.Blinks.EndReason.CLEARED);"),
                "the CLEAN and the run's end");
        assertEquals(2, count(r, "cooldowns().reset(player.getUniqueId(), io.github.butterflysmp.rpg.paper.accessory.BlinkTomes.COOLDOWN_KEY);"),
                "each resets the blink's cooldown key, and only it");
        assertTrue(r.contains("+ \" drum=\" + drum + \" blink=\" + (blink ? \"ended\" : \"none\"));"), "the CLEAN line's field");
        assertTrue(r.contains("restored += \" blink=\" + (blinkLive ? \"ended\" : \"none\");"), "the RESTORED line's field");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }
}
