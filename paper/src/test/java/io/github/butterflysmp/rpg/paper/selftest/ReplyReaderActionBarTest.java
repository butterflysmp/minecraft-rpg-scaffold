package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE ACTION BAR IS READ (the seat's ruling 3 on GATE-breacher's boot, 2026-11-22). That boot's three quiver notices
 * reached no REPLY line because {@code ReplyReader} returned on every packet but {@code SYSTEM_CHAT_MESSAGE}. Read
 * from the source, bounded at {@code onPacketSend}'s closing brace: no unit test can raise a {@code PacketSendEvent}.
 * The server half is the next gate's (a notice predicted as {@code REPLY actionbar …}).
 */
class ReplyReaderActionBarTest {

    @Test
    void theActionBarPacketIsCopiedAndMarkedAsActionBarText() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/ReplyReader.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int start = src.indexOf("public void onPacketSend(PacketSendEvent event) {");
        assertTrue(start > 0, "onPacketSend is not found: the scan is blind");
        int end = src.indexOf("\n    }\n", start);
        String body = src.substring(start, end);
        assertTrue(body.contains("PacketType.Play.Server.ACTION_BAR"), "the action-bar packet is let through: " + body);
        assertTrue(body.contains("PacketType.Play.Server.SYSTEM_CHAT_MESSAGE"), "CONTROL: system chat still is");
        assertTrue(body.contains("new Reply(new WrapperPlayServerActionBar(event).getActionBarText(), true)"),
                "copied as a Reply whose overlay flag is TRUE, which SelfTestRun prints as \"REPLY actionbar\"");
        assertTrue(body.indexOf("isCancelled()") < body.indexOf("ACTION_BAR) {") || body.indexOf("if (actionBar) {")
                        > body.indexOf("isCancelled()"),
                "a cancelled action bar was not sent, so it is not a reply: the cancel check comes first");
    }
}
