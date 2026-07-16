package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Sends one mob's nameplate metadata to one viewer. A seam so the LOS loop's decision logic
 * ({@link ViewerNameplateState}) is testable without PacketEvents, and so the packet details live in
 * one place ({@code PacketNameplateSender}).
 *
 * @param name present to (re)set the mob's name text for this viewer -- sent only on first sight or a
 *             health change; empty to leave the text untouched and update visibility alone.
 * @param visible the per-viewer line-of-sight result, re-asserted every cycle.
 */
public interface NameplateSender {

    void send(Player viewer, int entityId, Optional<Component> name, boolean visible);
}
