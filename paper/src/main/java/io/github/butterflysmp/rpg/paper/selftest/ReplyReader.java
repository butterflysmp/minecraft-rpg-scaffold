package io.github.butterflysmp.rpg.paper.selftest;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import java.util.Queue;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * The selftest's reply capture: a COPY of every system-chat message the server sends the test player.
 *
 * <p><b>WHY A PACKET LISTENER.</b> {@code Player#performCommand} returns only a boolean, and nothing in Bukkit
 * observes {@code sendMessage}. The packet is what actually reached the client, so a copy of it is the reply as sent
 * -- the one witness CP2, BN1, CM2 and WS2c's reply half need (the seat's ruling a, 2026-10-05).
 *
 * <p><b>COPY-ONLY, AND PINNED.</b> It never cancels, modifies, re-encodes or delays a packet. It runs on a Netty
 * thread, so it touches nothing but the queue (CLAUDE.md's threading rule: read the packet, compute, touch nothing
 * else); the run drains the queue on the player's own thread. {@code ReplyReaderCopyOnlyTest} scans this class's
 * compiled references for every mutating call, with a positive control.
 *
 * <p><b>Registered only while a run is active, and only for the test player</b>: {@link SelfTestRun} registers it
 * at start and unregisters it at finish, stop and quit; packets to anyone else are ignored here by UUID.
 * {@code isOverlay()} separates the action bar (a stone's refusal) from chat.
 */
final class ReplyReader extends PacketListenerAbstract {

    /** One captured message. Adventure components are immutable, so handing one across threads is safe. */
    record Reply(Component message, boolean overlay) {}

    private final UUID player;
    private final Queue<Reply> sink;

    ReplyReader(UUID player, Queue<Reply> sink) {
        // MONITOR: after every listener that might cancel or rewrite it, so the copy is what was sent.
        super(PacketListenerPriority.MONITOR);
        this.player = player;
        this.sink = sink;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) return;
        if (event.isCancelled()) return;   // not sent, so not a reply
        if (event.getUser() == null || !player.equals(event.getUser().getUUID())) return;
        WrapperPlayServerSystemChatMessage chat = new WrapperPlayServerSystemChatMessage(event);
        sink.add(new Reply(chat.getMessage(), chat.isOverlay()));
    }
}
