package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.UUID;

/**
 * Everything worth knowing about a combatant, read once, on the thread that owns it.
 *
 * This exists because a read cannot hop a thread. Every mutator on CombatantHandle
 * dispatches onto the owning entity's thread and returns nothing; a reader has to return
 * a value, and blocking on a future from a region thread deadlocks. So the reads happen
 * exactly where they are legal -- at the moment the world hands the combatant over -- and
 * are frozen into this record.
 *
 * A record is an immutable data carrier, so this may safely cross a tick or a thread
 * boundary where a live entity may not. It may of course be STALE by then: a snapshot is a
 * photograph, not a window.
 *
 * {@code player} is the one piece of faction the engine currently needs: mob-only effects
 * (statuses, and a dash's payload) read it to leave players out of the target set. It is a
 * read like any other -- captured on the owning thread, frozen here -- so the rule that
 * excludes players can be exercised by a core unit test rather than living in adapter wiring
 * no test reaches.
 */
public record CombatantSnapshot(UUID id, Vec3 position, boolean alive, boolean player) {}
