package io.github.yourname.rpg.paper.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import io.github.yourname.rpg.paper.scheduler.Scheduler;
import org.bukkit.entity.Player;

/**
 * Reference implementation of the threading contract. Copy this shape.
 */
public final class ExampleTelegraphListener extends PacketListenerBase {

    public ExampleTelegraphListener(Scheduler scheduler) {
        super(scheduler, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        // SAFE: pure inspection on the Netty thread.
        Object player = event.getPlayer();
        if (!(player instanceof Player bukkitPlayer)) return;

        // UNSAFE, do not do this here:
        //   bukkitPlayer.sendMessage("hi");
        //   bukkitPlayer.getWorld().spawnParticle(...);

        // SAFE: hop onto the player's owning thread first.
        bukkit(bukkitPlayer, () -> {
            // Now the Bukkit API is legal.
        });
    }
}
