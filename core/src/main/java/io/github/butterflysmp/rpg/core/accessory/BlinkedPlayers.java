package io.github.butterflysmp.rpg.core.accessory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE BLINKED STATE (the seat's K4, 2026-11-26): one per player, the engine's own record, never a reading of the potion
 * effect. It begins at the teleport and ends at the FIRST of: {@code invisibility_ticks} elapsed, his attack, his death,
 * his quit (and the selftest's clear). One instance per server, held in {@code AdapterContext} as {@code Orbs} is.
 *
 * <p>Thread-safe: it is written on the wearer's own thread and read on any mob's (a target event, a mob ability's
 * choice), so the map is concurrent and each state is an immutable value. {@link #end} removes and returns in one step, so
 * two ends racing for one state end it once.
 */
public final class BlinkedPlayers {

    /**
     * One blink: the tick it began, the tick it expires, and what it did to his invisibility (so its end removes only that).
     * {@code refusedLogged} holds the mobs whose refusal has been logged this blink (a goal re-asks every few ticks; one
     * line per mob per blink).
     */
    public record State(long startTick, long endTick, Blinks.InvisibilityAtBlink invisibility, Set<UUID> refusedLogged) {
        public State(long startTick, long endTick, Blinks.InvisibilityAtBlink invisibility) {
            this(startTick, endTick, invisibility, ConcurrentHashMap.newKeySet());
        }
    }

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    /** Begin his state. A second begin while one runs replaces it (it cannot happen: the trigger is held while blinked). */
    public void begin(UUID player, State state) {
        states.put(player, state);
    }

    public boolean isBlinked(UUID player) {
        return player != null && states.containsKey(player);
    }

    public Optional<State> state(UUID player) {
        return Optional.ofNullable(states.get(player));
    }

    /** End his state: removed and returned, once. Empty when none was running. */
    public Optional<State> end(UUID player) {
        return Optional.ofNullable(states.remove(player));
    }

    /** End his state only if it is still the one that began at {@code startTick} (the expiry's own check). */
    public Optional<State> endIfStarted(UUID player, long startTick) {
        State s = states.get(player);
        if (s == null || s.startTick() != startTick) return Optional.empty();
        return states.remove(player, s) ? Optional.of(s) : Optional.empty();
    }

    /** How many players are blinked now. For the tests. */
    public int size() {
        return states.size();
    }
}
