package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.List;

/**
 * Fans one {@link HealthChange} out to several listeners. This is how the store stays
 * single-listener while more than one display rides the seam: the player heart bar and the mob
 * nameplate are both {@link HealthListener}s wrapped here, so a single emit reaches both. No second
 * observability path.
 *
 * Delivery is ISOLATED: if one listener throws, the others still receive the change. A display glitch
 * must not break the sibling display, nor propagate back into the store mutation that emitted the
 * event (which may be a live combat tick). Displays own their own error handling; this guarantees the
 * event reaches every one of them regardless. Insertion order is preserved.
 */
public final class CompositeHealthListener implements HealthListener {

    private final List<HealthListener> listeners;

    public CompositeHealthListener(HealthListener... listeners) {
        this.listeners = List.of(listeners);
    }

    @Override
    public void onChange(HealthChange change) {
        for (HealthListener listener : listeners) {
            try {
                listener.onChange(change);
            } catch (RuntimeException ignored) {
                // Isolate: a throwing display must not stop delivery to the others or break the
                // store mutation that emitted this. Displays log their own failures.
            }
        }
    }

    /** How many listeners are wired. For tests. */
    public int size() {
        return listeners.size();
    }
}
