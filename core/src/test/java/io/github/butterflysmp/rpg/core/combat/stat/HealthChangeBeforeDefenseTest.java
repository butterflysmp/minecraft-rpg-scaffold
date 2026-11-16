package io.github.butterflysmp.rpg.core.combat.stat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A DAMAGE change carries the amount BEFORE the victim's defence as well as the amount that landed, so the selftest's
 * damage line can print both (PLAN-dragon-mobs.md step R3, the instrument; GATE-dragon-fix.md D5 stayed UNVERIFIED for
 * want of it). The store is the only place both numbers exist; recomputing the curve elsewhere would be a second site.
 */
class HealthChangeBeforeDefenseTest {

    private static final class Recorder implements HealthListener {
        final List<HealthChange> seen = new ArrayList<>();
        @Override public void onChange(HealthChange change) { seen.add(change); }
    }

    /** 30 against 20 defence lands for 25 (CombatantStatsTest's executed figure); the change says 30 AND 25. */
    @Test
    void aDamageChangeCarriesBothTheOfferedAndTheLandedAmount() {
        Recorder recorder = new Recorder();
        CombatantStats stats = new CombatantStats(recorder);
        UUID victim = UUID.randomUUID();
        stats.register(victim, CombatantStats.DEFAULT_PLAYER_BASE, true);
        stats.reconcileDefenseModifiers(victim, Map.of("HEAD", 3.0, "CHEST", 8.0, "LEGS", 6.0, "FEET", 3.0));
        recorder.seen.clear();

        stats.damage(victim, 30, null, false);

        HealthChange change = recorder.seen.getLast();
        assertEquals(HealthChange.Kind.DAMAGE, change.kind());
        assertEquals(25.0, change.amount(), 1e-9, "landed, after defence");
        assertEquals(30.0, change.beforeDefense(), 0.0, "offered, before defence");
    }

    /** Every older constructor means "nothing mitigated it": before equals the amount. */
    @Test
    void theOlderConstructorsCarryTheAmountAsBefore() {
        HealthChange heal = new HealthChange(UUID.randomUUID(), true, HealthChange.Kind.HEAL, 7.0, null, false,
                50, 100, false, false, null);
        assertEquals(7.0, heal.beforeDefense(), 0.0);
        HealthChange nine = new HealthChange(UUID.randomUUID(), true, HealthChange.Kind.DAMAGE, 4.0, null, false,
                50, 100, false);
        assertEquals(4.0, nine.beforeDefense(), 0.0);
    }
}
