package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Sends the packets for one dealer's floating damage number, isolated behind an interface -- the same
 * seam shape as {@link NameplateSender} -- so the wire format lives in one place ({@link
 * PacketDamagePopupSender}) and the popup's decision logic ({@link DamagePopupManager}) stays free of
 * PacketEvents in its signatures. The number is a fake client-side entity: spawned, given text, and
 * later destroyed, all sent to the viewer only.
 */
public interface DamagePopupSender {

    /** Spawn a client-only text display showing {@code text} at (x,y,z), visible to {@code viewer} only. */
    void spawn(Player viewer, int entityId, double x, double y, double z, Component text);

    /** Destroy the client-only display. Sent to {@code viewer} only; safe no-op if they already dropped it. */
    void destroy(Player viewer, int entityId);
}
