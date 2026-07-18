package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pure kill-gate: a mob dies only on the DAMAGE hit that zeroed its custom HP. A player target is
 * skipped (player death is a follow-up pass); a non-zeroing hit leaves it alive. The setHealth(0) /
 * setKiller / entity resolution is boot-witnessed; this gate is the unit-testable piece.
 */
class MobDeathSystemTest {

    private static HealthChange change(boolean reachedZero, boolean targetIsPlayer, HealthChange.Kind kind) {
        return new HealthChange(UUID.randomUUID(), targetIsPlayer, kind, 12.0,
                UUID.randomUUID(), true, reachedZero ? 0 : 88, 100, reachedZero);
    }

    @Test
    void killsMobThatReachedZero() {
        assertTrue(MobDeathSystem.shouldKill(change(true, false, HealthChange.Kind.DAMAGE)),
                "a mob whose custom HP crossed to 0 -> kill");
    }

    @Test
    void doesNotKillOnANonZeroingHit() {
        assertFalse(MobDeathSystem.shouldKill(change(false, false, HealthChange.Kind.DAMAGE)),
                "a hit that did not zero custom HP leaves the mob alive (token floor holds)");
        // Mutation: drop the reachedZero() clause -> every hit kills -> reddens.
    }

    @Test
    void doesNotKillPlayers() {
        assertFalse(MobDeathSystem.shouldKill(change(true, true, HealthChange.Kind.DAMAGE)),
                "a player at 0 sits at the floor, alive -- player death is the follow-up pass");
        // Mutation: drop the !targetIsPlayer() clause -> players die this pass -> reddens.
    }
}
