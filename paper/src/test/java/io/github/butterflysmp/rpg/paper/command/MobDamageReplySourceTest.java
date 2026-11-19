package io.github.butterflysmp.rpg.paper.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code /rpg mobdamage}'s REPLY IS THE STORE'S READING, NOT AN ESTIMATE (the F8 brief, section 2;
 * GATE-fight-site.md). GATE-fight-mage.md's MG2 and MG4 read {@code "WITHER_SKELETON damaged: 1450/1500 custom HP"}
 * for a hit the Mage's gate REFUSED: the probe two ticks later read 1500. The reply was computed as
 * {@code current - amount} before {@code applyDamage}, which lands a tick later and knows nothing of a gate or a
 * defence. Now the reply is sent from a task scheduled after the hit lands, and reads the store. Pinned from the
 * source, comments stripped, bounded structurally from the method's signature to the next member.
 */
class MobDamageReplySourceTest {

    private static final Path SRC = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/command/RpgCommand.java");

    private static String mobMutate() throws IOException {
        String code = Files.readString(SRC).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        int from = code.indexOf("private static int mobMutate(");
        assertTrue(from >= 0);
        int to = code.indexOf("\n    private ", from + 10);
        return code.substring(from, to < 0 ? code.length() : to);
    }

    @Test
    void theDamageReplyIsReadFromTheStoreAfterTheHitLands() throws IOException {
        String body = mobMutate();
        assertFalse(body.contains("stats.current(id) - amount"), "no estimate: " + body);
        int apply = body.indexOf(".applyDamage(amount, player.getUniqueId())");
        int later = body.indexOf("onRegionLater(", apply);
        assertTrue(apply > 0 && later > apply, "the reply is scheduled AFTER the hit is dispatched: " + body);
        assertTrue(body.indexOf("stats.current(id)", later) > later, "and reads the store there: " + body);
    }
}
