package io.github.butterflysmp.rpg.paper.mining;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Packet-only ITEM_DISPLAYs, shown to ONE player (PLAN-mining.md 5.4, F5). Re-implemented from the
 * spike's {@code MineSpikeVisuals}, following {@code PacketDamagePopupSender}: a fresh wrapper per
 * send. The server never knows these entities exist.
 *
 * <p>Called on the main thread only (the BlockDamageEvent handler, the chunk events, the commands
 * and the regrowth task). Nothing here runs on a Netty thread, and there is no packet listener.
 *
 * <p>Metadata indices (F5, read from the pinned server jar): 11 translation, 12 scale, 13 left
 * rotation, 15 billboard, 16 brightness override, 23 the item stack, 24 the display type.
 */
final class MiningVisuals {

    static final int DISPLAY_TRANSLATION = 11;
    static final int DISPLAY_SCALE = 12;
    static final int DISPLAY_LEFT_ROTATION = 13;
    static final int DISPLAY_BILLBOARD = 15;
    static final int DISPLAY_BRIGHTNESS = 16;
    static final int ITEM_DISPLAY_ITEM = 23;
    static final int ITEM_DISPLAY_TYPE = 24;

    static final byte BILLBOARD_FIXED = 0;
    /** {@code ItemDisplayContext.FIXED}. */
    static final byte DISPLAY_TYPE_FIXED = 8;
    /** Block light 15 and sky light 15, packed as {@code block << 4 | sky << 20}. */
    static final int FULL_BRIGHT = (15 << 4) | (15 << 20);

    void spawn(Player viewer, int entityId, double[] at, ItemStack item, float[] rotation,
               float sx, float sy, float sz) {
        send(viewer, new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(UUID.randomUUID()), EntityTypes.ITEM_DISPLAY,
                new Vector3d(at[0], at[1], at[2]), 0f, 0f, 0f, 0, Optional.empty()));
        List<EntityData<?>> data = new ArrayList<>(7);
        data.add(new EntityData<>(DISPLAY_TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(0f, 0f, 0f)));
        data.add(new EntityData<>(DISPLAY_SCALE, EntityDataTypes.VECTOR3F, new Vector3f(sx, sy, sz)));
        data.add(new EntityData<>(DISPLAY_LEFT_ROTATION, EntityDataTypes.QUATERNION,
                new Quaternion4f(rotation[0], rotation[1], rotation[2], rotation[3])));
        data.add(new EntityData<>(DISPLAY_BILLBOARD, EntityDataTypes.BYTE, BILLBOARD_FIXED));
        data.add(new EntityData<>(DISPLAY_BRIGHTNESS, EntityDataTypes.INT, FULL_BRIGHT));
        data.add(new EntityData<>(ITEM_DISPLAY_ITEM, EntityDataTypes.ITEMSTACK,
                SpigotConversionUtil.fromBukkitItemStack(item)));
        data.add(new EntityData<>(ITEM_DISPLAY_TYPE, EntityDataTypes.BYTE, DISPLAY_TYPE_FIXED));
        send(viewer, new WrapperPlayServerEntityMetadata(entityId, data));
    }

    /** Swaps a display's item, which is how a live spot becomes a crack. */
    void setItem(Player viewer, int entityId, ItemStack item) {
        List<EntityData<?>> data = new ArrayList<>(1);
        data.add(new EntityData<>(ITEM_DISPLAY_ITEM, EntityDataTypes.ITEMSTACK,
                SpigotConversionUtil.fromBukkitItemStack(item)));
        send(viewer, new WrapperPlayServerEntityMetadata(entityId, data));
    }

    void destroy(Player viewer, int... entityIds) {
        if (entityIds.length == 0) return;
        send(viewer, new WrapperPlayServerDestroyEntities(entityIds));
    }

    private static void send(Player viewer, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
