package io.github.butterflysmp.rpg.paper.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import io.github.butterflysmp.rpg.paper.adapter.DragTrace;

/**
 * COUNTS A PLAYER'S POSITION-BEARING MOVE PACKETS for the drag's {@code DRAGTICK} line ({@link DragTrace}; the seat's
 * Part D, feat/drag-additive, 2026-11-18). Runs on NETTY I/O THREADS: it reads the packet's type and the user's id and
 * increments a counter. It reads no packet body, cancels nothing and touches no Bukkit object (CLAUDE.md THREADING).
 * {@code PLAYER_POSITION} and {@code PLAYER_POSITION_AND_ROTATION} are the client's {@code MovePlayer.Pos} and
 * {@code PosRot}; a rotation-only or status-only packet moves him nowhere and is not counted.
 */
public final class MoveCountListener extends PacketListenerAbstract {

    private final DragTrace trace;

    public MoveCountListener(DragTrace trace) {
        super(PacketListenerPriority.MONITOR);
        this.trace = trace;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            trace.moved(event.getUser().getUUID());
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        trace.forget(event.getUser().getUUID());
    }
}
