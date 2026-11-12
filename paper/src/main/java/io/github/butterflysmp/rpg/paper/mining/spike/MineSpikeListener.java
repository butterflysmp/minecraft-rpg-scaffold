package io.github.butterflysmp.rpg.paper.mining.spike;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.github.butterflysmp.rpg.paper.packet.PacketListenerBase;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * The spike's packet witness: SWING, DIG_START, DIG_ABORT, DIG_STOP and ATTACK. SPIKE CODE.
 *
 * <p><b>THREADING (CLAUDE.md).</b> On the Netty thread this only reads the packet and computes: the
 * type, the wrapper fields, the user's UUID (PacketEvents data, not Bukkit) and a seq. The one
 * statement that reaches Bukkit is the {@code bukkit(...)} hop. MONITOR priority, and it never
 * cancels.
 *
 * <p><b>ATTACK, NOT INTERACT_ENTITY</b> (PLAN-mining.md F3): on 26.1 a left-click on an entity is
 * {@code ServerboundAttackPacket}, which is {@code PacketType.Play.Client.ATTACK} here.
 */
public final class MineSpikeListener extends PacketListenerBase {

    private final MineSpike spike;

    public MineSpikeListener(Scheduler scheduler, MineSpike spike) {
        super(scheduler, PacketListenerPriority.MONITOR);
        this.spike = spike;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.ANIMATION && type != PacketType.Play.Client.PLAYER_DIGGING
                && type != PacketType.Play.Client.ATTACK) return;
        UUID playerId = event.getUser().getUUID();
        if (!spike.isActive(playerId)) return;

        InteractionHand hand = type == PacketType.Play.Client.ANIMATION
                ? new WrapperPlayClientAnimation(event).getHand() : null;
        DiggingAction dig = type == PacketType.Play.Client.PLAYER_DIGGING
                ? new WrapperPlayClientPlayerDigging(event).getAction() : null;
        String name = classify(type, hand, dig);
        if (name == null) return;
        Integer entityId = type == PacketType.Play.Client.ATTACK
                ? new WrapperPlayClientAttack(event).getEntityId() : null;
        long seq = spike.nextSeq();
        if (!(event.getPlayer() instanceof Player player)) return;

        bukkit(player, () -> spike.logPacket(player, seq, name, entityId));
    }

    /** The line type for a packet, or null for one the spike does not log. Pure. */
    static String classify(PacketTypeCommon type, InteractionHand hand, DiggingAction dig) {
        if (type == PacketType.Play.Client.ANIMATION) {
            return hand == InteractionHand.OFF_HAND ? "SWING_OFFHAND" : "SWING";
        }
        if (type == PacketType.Play.Client.ATTACK) return "ATTACK";
        if (type == PacketType.Play.Client.PLAYER_DIGGING && dig != null) {
            return switch (dig) {
                case START_DIGGING -> "DIG_START";
                case CANCELLED_DIGGING -> "DIG_ABORT";
                case FINISHED_DIGGING -> "DIG_STOP";
                default -> null;
            };
        }
        return null;
    }
}
