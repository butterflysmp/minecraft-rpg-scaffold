package io.github.butterflysmp.rpg.paper.health;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The one place that knows the damage-popup wire format. Sends a client-only TEXT_DISPLAY to a single
 * viewer: a spawn packet, a metadata packet (text + billboard + style), and later a destroy. Modelled on
 * {@link PacketNameplateSender}; same discipline -- a FRESH wrapper per send (PacketEvents frees a
 * wrapper's buffer after encoding, so one must never be shared).
 *
 * TextDisplay metadata indices, verified against minecraft.wiki's Entity-metadata / Display layout
 * (documented for Java 26.2; stable since 1.19.4, so it holds for our 26.1 server) and PacketEvents
 * 2.13.0's sources jar. A wrong index fails SILENTLY (invisible/garbage entity, no throw) -- so this is
 * the boot-witnessed surface, the whole reason the popup is a pass of its own.
 */
public final class PacketDamagePopupSender implements DamagePopupSender {

    /** Display block: billboard constraints (Byte). CENTER=3 makes the number face the camera. Index 15. */
    private static final int INDEX_BILLBOARD = 15;
    /** TextDisplay: the text (Component). Index 23. */
    private static final int INDEX_TEXT = 23;
    /** TextDisplay: style flags (Byte bitmask). Index 27. */
    private static final int INDEX_STYLE = 27;

    private static final byte BILLBOARD_CENTER = 3;
    /** shadow (0x01) | see-through (0x02) | default background (0x04). */
    private static final byte STYLE = 0x07;

    @Override
    public void spawn(Player viewer, int entityId, double x, double y, double z, Component text) {
        // Arg order in PE 2.13.0 is (id, uuid, type, position, pitch, yaw, headYaw, data, velocity) --
        // pitch BEFORE yaw. Both 0 for a camera-facing number.
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(UUID.randomUUID()), EntityTypes.TEXT_DISPLAY,
                new Vector3d(x, y, z), 0f, 0f, 0f, 0, Optional.empty());
        send(viewer, spawn);

        List<EntityData<?>> metadata = new ArrayList<>(3);
        metadata.add(new EntityData<>(INDEX_BILLBOARD, EntityDataTypes.BYTE, BILLBOARD_CENTER));
        metadata.add(new EntityData<>(INDEX_TEXT, EntityDataTypes.ADV_COMPONENT, text));
        metadata.add(new EntityData<>(INDEX_STYLE, EntityDataTypes.BYTE, STYLE));
        send(viewer, new WrapperPlayServerEntityMetadata(entityId, metadata));
    }

    @Override
    public void destroy(Player viewer, int entityId) {
        send(viewer, new WrapperPlayServerDestroyEntities(entityId));
    }

    private static void send(Player viewer, com.github.retrooper.packetevents.wrapper.PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
