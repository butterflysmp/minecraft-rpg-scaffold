package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.UUID;

/**
 * THE BEAM'S REPORT (PLAN-dragon-mobs.md step R5, the plan's E5): this mob's beam LANDED on this combatant at this
 * server tick, along the line from {@code from} (the mob's eye at the lock) to {@code to} (the point of the fired
 * segment nearest the target's body). Raised only on a hit, never on a miss.
 *
 * <p>Step R6's teleport will be its first listener: it needs the mob, the TARGET (a combatant, never specifically a
 * player, Ben's "Target"), and the tick its delay counts from. Nothing listens in step R5.
 */
public record BeamHit(UUID mob, String mobId, UUID target, long tick, Vec3 from, Vec3 to) {}
