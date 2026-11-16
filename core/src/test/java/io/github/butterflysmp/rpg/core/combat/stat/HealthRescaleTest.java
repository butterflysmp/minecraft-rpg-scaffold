package io.github.butterflysmp.rpg.core.combat.stat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A grown Phage's health (PLAN-dragon-mobs.md, growth; Ben: "Same share"): the new stage's maximum, and current
 * health at the SAME FRACTION of maximum it had before.
 */
class HealthRescaleTest {

    @Test
    void theFractionRuleHalfStaysHalfAndFullStaysFull() {
        assertEquals(50.0, HealthState.sameShare(40.0, 80.0, 100.0), 0.0, "half before, half after");
        assertEquals(100.0, HealthState.sameShare(80.0, 80.0, 100.0), 0.0, "full stays full");
        assertEquals(0.0, HealthState.sameShare(0.0, 80.0, 100.0), 0.0, "zero stays zero");
    }

    /** Through the store: max moves, current keeps its share, and ONE MAX_CHANGE is emitted for the nameplate. */
    @Test
    void theStoreRescalesKeepingTheShareAndEmitsOneChange() {
        List<HealthChange> changes = new ArrayList<>();
        CombatantStats stats = new CombatantStats(changes::add);
        UUID id = UUID.randomUUID();
        stats.register(id, 240.0, false);              // a stage-1 Phage at GS 300
        stats.damage(id, 120.0, null, false);          // half
        changes.clear();

        stats.rescaleMaxKeepingShare(id, 300.0);        // stage 2 at GS 300

        assertEquals(300.0, stats.max(id), 0.0);
        assertEquals(150.0, stats.current(id), 0.0, "half of the new maximum");
        assertEquals(1, changes.size());
        assertEquals(HealthChange.Kind.MAX_CHANGE, changes.getFirst().kind());
        assertEquals(150.0, changes.getFirst().newCurrent(), 0.0);
        assertEquals(300.0, changes.getFirst().max(), 0.0);
    }

    /** Re-applied, it moves from where it is, never compounding: a second call to the same max changes nothing. */
    @Test
    void rescalingTwiceToTheSameMaxIsStable() {
        CombatantStats stats = new CombatantStats(change -> {});
        UUID id = UUID.randomUUID();
        stats.register(id, 240.0, false);
        stats.rescaleMaxKeepingShare(id, 300.0);
        stats.rescaleMaxKeepingShare(id, 300.0);
        assertEquals(300.0, stats.max(id), 0.0);
        assertEquals(300.0, stats.current(id), 0.0);
        stats.rescaleMaxKeepingShare(id, 360.0);
        assertEquals(360.0, stats.max(id), 0.0, "from 300 to 360, not 240 + two deltas");
    }
}
