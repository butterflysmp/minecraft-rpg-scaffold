package io.github.butterflysmp.rpg.paper.packet;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one piece of new logic 1b adds that is pure: the left-click signal. A main-hand arm
 * swing is a swing; an off-hand swing or any other packet is not. Everything downstream
 * (item lookup, aim, firing) is server-coupled and lives inside the hop, so this predicate
 * is what the suite can hold; the hop's threading is proven separately.
 */
class WeaponSwingListenerTest {

    @Test
    void aMainHandArmSwingIsASwing() {
        assertTrue(WeaponSwingListener.isMainHandSwing(
                PacketType.Play.Client.ANIMATION, InteractionHand.MAIN_HAND));
    }

    @Test
    void anOffHandSwingIsNotASwing() {
        assertFalse(WeaponSwingListener.isMainHandSwing(
                PacketType.Play.Client.ANIMATION, InteractionHand.OFF_HAND));
    }

    @Test
    void aNonAnimationPacketIsNotASwing() {
        assertFalse(WeaponSwingListener.isMainHandSwing(
                PacketType.Play.Client.INTERACT_ENTITY, InteractionHand.MAIN_HAND));
    }
}
