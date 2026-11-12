package io.github.butterflysmp.rpg.paper.mining.spike;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The Netty-side classification, which is pure. Same style as WeaponSwingListenerTest. */
class MineSpikeListenerTest {

    @Test
    void swingsAreSplitByHand() {
        assertEquals("SWING", MineSpikeListener.classify(PacketType.Play.Client.ANIMATION, InteractionHand.MAIN_HAND, null));
        assertEquals("SWING_OFFHAND", MineSpikeListener.classify(PacketType.Play.Client.ANIMATION, InteractionHand.OFF_HAND, null));
    }

    @Test
    void theThreeDigActionsAreNamedAndTheRestIgnored() {
        assertEquals("DIG_START", MineSpikeListener.classify(PacketType.Play.Client.PLAYER_DIGGING, null, DiggingAction.START_DIGGING));
        assertEquals("DIG_ABORT", MineSpikeListener.classify(PacketType.Play.Client.PLAYER_DIGGING, null, DiggingAction.CANCELLED_DIGGING));
        assertEquals("DIG_STOP", MineSpikeListener.classify(PacketType.Play.Client.PLAYER_DIGGING, null, DiggingAction.FINISHED_DIGGING));
        assertNull(MineSpikeListener.classify(PacketType.Play.Client.PLAYER_DIGGING, null, DiggingAction.DROP_ITEM));
    }

    @Test
    void anAttackIsTheAttackPacketNotInteractEntity() {
        // PLAN-mining.md F3: on 26.1 a left-click attack is ServerboundAttackPacket. INTERACT_ENTITY is a use.
        assertEquals("ATTACK", MineSpikeListener.classify(PacketType.Play.Client.ATTACK, null, null));
        assertNull(MineSpikeListener.classify(PacketType.Play.Client.INTERACT_ENTITY, InteractionHand.MAIN_HAND, null));
    }
}
