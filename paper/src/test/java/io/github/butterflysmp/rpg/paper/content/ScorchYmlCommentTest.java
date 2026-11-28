package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCORCH.YML'S COMMENT STATES WHAT THE CODE DOES (the seat's A2, 2026-11-27, after its comment had told the seat the cap
 * was "the damage of whatever applied the stacks" while the code halves it). Each number the comment names is rebuilt
 * here FROM THE CONSTANT, so a retuned constant with a stale comment is a red row, and the old false sentence cannot
 * come back. What a string cannot pin (the refresh keeping the phase and the larger cap) is DotStatusTest's.
 */
class ScorchYmlCommentTest {

    private static String comment() throws IOException {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readString(Path.of("src/main/resources/content/statuses/scorch.yml"),
                StandardCharsets.UTF_8).split("\\R")) {
            if (line.startsWith("#")) out.append(line.substring(1).strip()).append(' ');
        }
        return out.toString().replaceAll("\\s+", " ");
    }

    @Test
    void everyRateTheCommentNamesIsTheConstants() throws IOException {
        String c = comment();
        assertTrue(c.contains("Scorch.RATE_PER_SECOND " + Scorch.RATE_PER_SECOND), "rate");
        assertTrue(c.contains("min(" + Math.round(Scorch.RATE_PER_SECOND * 100) + "% of the victim's MAX health"), "rate in %");
        assertTrue(c.contains("Scorch.PERIOD_TICKS " + Scorch.PERIOD_TICKS), "period");
        assertTrue(c.contains("every " + Scorch.PERIOD_TICKS + " ticks"), "period in words");
        assertTrue(c.contains("Scorch.CAP_FRACTION " + Scorch.CAP_FRACTION), "cap fraction");
        assertTrue(c.contains("Scorch.DEFAULT_DURATION_TICKS " + Scorch.DEFAULT_DURATION_TICKS), "window");
        assertTrue(c.contains("1 per " + Math.round(Scorch.DAMAGE_PER_STACK) + " damage dealt"), "stacks");
        assertEquals(80.0, 160 * Scorch.CAP_FRACTION, "the worked figure: Smolder's 160 caps at 80");
        assertTrue(c.contains("Smolder's 160 caps its burn at 80"), "the worked figure, stated");
    }

    /** The sentences A2 retired: the cap as the whole hit, and the ability deciding the duration. */
    @Test
    void theRetiredSentencesAreGone() throws IOException {
        String c = comment();
        assertTrue(!c.contains("the damage of whatever applied the stacks"), "the cap is HALF the hit");
        assertTrue(!c.contains("The ability that applies it still decides for how long"), "the element's window is 120");
    }

    /** Water puts out Scorch (Y1 to Y3, E1), stated in the file a content author reads, naming its classes. */
    @Test
    void theWaterLineNamesTheServersTwoReadingsAndTheAccount() throws IOException {
        String c = comment();
        assertTrue(c.contains("(the server's own isInWater and isInRain)"), "the two readings");
        assertTrue(c.contains("a splash water bottle ends it at once"), "the splash");
        assertTrue(c.contains("A wet body does not"), "E1");
        assertTrue(c.contains("Douse and ScorchStatus are the account"), "the account");
    }
}
