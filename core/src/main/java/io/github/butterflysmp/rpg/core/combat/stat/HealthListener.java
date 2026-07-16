package io.github.butterflysmp.rpg.core.combat.stat;

/**
 * The consumer side of the observability seam. A {@link CombatantStats} store emits a
 * {@link HealthChange} to its listener on every mutation; core stays pure, so the listener is
 * whatever the caller wires -- the Paper adapter routes each change onto the owning entity's thread
 * and updates the displays.
 *
 * Functional so a test can pass a lambda that records what it saw and assert on the payload, and so
 * a store with no display attached uses {@link #NONE} rather than a null check on every mutation.
 */
@FunctionalInterface
public interface HealthListener {

    void onChange(HealthChange change);

    /** A listener that does nothing -- for a store nobody is displaying yet, and for tests. */
    HealthListener NONE = change -> { };
}
