package io.github.butterflysmp.rpg.paper.health;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sends a per-viewer entity-metadata packet to control a mob's nameplate. This is the only place that
 * knows the wire format, and the first packet-SEND in the project (everything else receives).
 *
 * The base-Entity metadata indices, confirmed against the current protocol (minecraft.wiki documents
 * Java Edition 26.2; our server is 26.1) rather than trusted from an old ported constant:
 *   - index 2 = custom name        (Optional Text Component)  -> EntityDataTypes.OPTIONAL_ADV_COMPONENT
 *   - index 3 = is custom name visible (Boolean)              -> EntityDataTypes.BOOLEAN
 * A wrong index would silently route the HP bar into another field, so this must be boot-witnessed
 * (the name lands in the right place, and the mob's REAL name is never touched).
 *
 * The mob's server-side name is deliberately never set: index 2 here is a per-VIEWER override, so the
 * real CustomName stays empty (death messages say "Zombie", /data is clean). Name is sent only when
 * present (first sight / health change); visibility rides every send. A fresh wrapper is built per
 * send -- PacketEvents frees a wrapper's buffer after encoding, so one must never be shared across
 * sends.
 */
public final class PacketNameplateSender implements NameplateSender {

    private static final int INDEX_CUSTOM_NAME = 2;
    private static final int INDEX_CUSTOM_NAME_VISIBLE = 3;

    @Override
    public void send(Player viewer, int entityId, Optional<Component> name, boolean visible) {
        List<EntityData<?>> metadata = new ArrayList<>(2);
        // Name only when present: a visibility-only cycle leaves index 2 untouched (its last value stands).
        name.ifPresent(component -> metadata.add(
                new EntityData<>(INDEX_CUSTOM_NAME, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(component))));
        metadata.add(new EntityData<>(INDEX_CUSTOM_NAME_VISIBLE, EntityDataTypes.BOOLEAN, visible));

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
