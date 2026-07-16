package io.github.butterflysmp.rpg.core.combat.stat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The composite is how two displays ride one seam. It must reach EVERY listener on one emit, and one
 * listener throwing must not rob the others of the event (a display glitch can't break the sibling
 * display or the store mutation). Each test names the mutation it forces red.
 */
class CompositeHealthListenerTest {

    private static HealthChange sample() {
        return new HealthChange(UUID.randomUUID(), false, HealthChange.Kind.DAMAGE, 5,
                null, false, 15, 20);
    }

    @Test
    void oneEmitReachesEveryListener() {
        List<HealthChange> a = new ArrayList<>();
        List<HealthChange> b = new ArrayList<>();
        var composite = new CompositeHealthListener(a::add, b::add);

        HealthChange change = sample();
        composite.onChange(change);

        assertEquals(1, a.size(), "first listener got the change");
        assertEquals(1, b.size(), "second listener got the SAME single emit");
        assertSame(change, b.get(0), "and it is the very change emitted, not a copy");
        // Mutation: forward to only the first listener -> b stays empty -> reddens (mob nameplate never updates).
    }

    @Test
    void aThrowingListenerDoesNotRobTheOthers() {
        List<HealthChange> reached = new ArrayList<>();
        HealthListener boom = c -> { throw new IllegalStateException("display glitch"); };
        // boom is registered BEFORE the good one, so if delivery aborted on throw, `reached` stays empty.
        var composite = new CompositeHealthListener(boom, reached::add);

        assertDoesNotThrow(() -> composite.onChange(sample()),
                "a display exception must not propagate back into the store mutation");
        assertEquals(1, reached.size(), "the sibling display still received the change");
        // Mutation: drop the per-listener try/catch -> the throw aborts the loop AND propagates -> reddens.
    }
}
