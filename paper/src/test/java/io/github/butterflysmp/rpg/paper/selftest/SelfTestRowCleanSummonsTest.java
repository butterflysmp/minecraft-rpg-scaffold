package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE LEFTOVER SUMMON (the seat's follow-up, 2026-11-26): on the boot of 37292713, SM16R2's wolf lived into armor-sets'
 * setup and AS15R (`LEFTOVER living=1 … [wolf …]`). A summon carries no `selftest` tag, so the row-start CLEAN's
 * removeTagged never took it, and those rows have no clear_state. Rule: the row-start CLEAN removes his summons (cause
 * clear) on EVERY unchained row, and the CLEAN line says how many. The run needs a server, so this reads the source.
 */
class SelfTestRowCleanSummonsTest {

    /** Mutation: the removal dropped, moved after the CLEAN line, or its count left off the line. */
    @Test
    void theRowStartCleanRemovesHisSummonsAndSaysHowMany() throws IOException {
        String r = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java"))
                .replace("\r", "");
        assertTrue(r.length() > 20_000, "control: SelfTestRun was read");
        int chained = r.indexOf("if (SelfTestStaging.chained(s)) {");
        int tagged = r.indexOf("int removed = snapshot.removeTagged(player, service.scheduler());", chained);
        int summons = r.indexOf("int summons = io.github.butterflysmp.rpg.paper.summon.SummonRemovals.removeAllOf(service.adapters(),", chained);
        int clear = r.indexOf("player.getUniqueId(), io.github.butterflysmp.rpg.core.summon.SummonRemoval.CLEAR);", summons);
        int line = r.indexOf("+ \" removed=\" + removed + \" summons=\" + summons + \" inventory=\"", chained);
        assertTrue(chained > 0 && tagged > chained, "the CLEAN follows the chained-row exemption: " + chained + " " + tagged);
        assertTrue(summons > tagged && clear > summons && line > clear,
                "removed with cause CLEAR, after the tagged mobs, before the CLEAN line, which carries the count");
        assertEquals(1, r.split("log\\(\"CLEAN blocks=", -1).length - 1, "one CLEAN line");
    }
}
