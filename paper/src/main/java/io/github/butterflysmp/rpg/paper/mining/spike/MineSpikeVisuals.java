package io.github.butterflysmp.rpg.paper.mining.spike;

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
 * Packet-only ITEM_DISPLAY and INTERACTION entities, shown to ONE viewer. SPIKE CODE.
 *
 * <p>Follows {@code PacketDamagePopupSender}: a fresh wrapper per send, because PacketEvents frees
 * the buffer after encoding. Called on the viewer's own thread (the command, a quit, a disable).
 *
 * <p><b>The metadata indices are read from the pinned server jar</b>, from each class's
 * {@code static {}} defineId order (PLAN-mining.md F3 and F5). A wrong index fails SILENTLY: the
 * client applies the value to some other field or ignores it.
 * <ul>
 *   <li>Entity: 0-7. Display: 8-22. ItemDisplay: 23 (item), 24 (display type).</li>
 *   <li>Interaction: 8 (width), 9 (height), 10 (response).</li>
 * </ul>
 */
final class MineSpikeVisuals {

    static final int DISPLAY_TRANSLATION = 11;
    static final int DISPLAY_SCALE = 12;
    static final int DISPLAY_LEFT_ROTATION = 13;
    static final int DISPLAY_BILLBOARD = 15;
    static final int DISPLAY_BRIGHTNESS = 16;
    static final int ITEM_DISPLAY_ITEM = 23;
    static final int ITEM_DISPLAY_TYPE = 24;

    static final int INTERACTION_WIDTH = 8;
    static final int INTERACTION_HEIGHT = 9;
    static final int INTERACTION_RESPONSE = 10;

    static final byte BILLBOARD_FIXED = 0;
    /** {@code ItemDisplayContext.FIXED}, the item-frame context. */
    static final byte DISPLAY_TYPE_FIXED = 8;
    /** Full block and sky light: {@code Brightness.pack()} is {@code block << 4 | sky << 20}. */
    static final int FULL_BRIGHT = (15 << 4) | (15 << 20);

    void spawnItemDisplay(Player viewer, int entityId, double x, double y, double z,
                          ItemStack item, float[] rotation, float sx, float sy, float sz) {
        send(viewer, new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(UUID.randomUUID()), EntityTypes.ITEM_DISPLAY,
                new Vector3d(x, y, z), 0f, 0f, 0f, 0, Optional.empty()));

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

    /**
     * An INTERACTION is an axis-aligned box centred on its x and z, rising {@code height} from its y.
     * So a box centred on point (cx, cy, cz) is spawned at (cx, cy - height / 2, cz).
     */
    void spawnInteraction(Player viewer, int entityId, double cx, double cy, double cz,
                          float width, float height) {
        send(viewer, new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(UUID.randomUUID()), EntityTypes.INTERACTION,
                new Vector3d(cx, cy - height / 2.0, cz), 0f, 0f, 0f, 0, Optional.empty()));

        List<EntityData<?>> data = new ArrayList<>(3);
        data.add(new EntityData<>(INTERACTION_WIDTH, EntityDataTypes.FLOAT, width));
        data.add(new EntityData<>(INTERACTION_HEIGHT, EntityDataTypes.FLOAT, height));
        data.add(new EntityData<>(INTERACTION_RESPONSE, EntityDataTypes.BOOLEAN, false));
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
