package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobHostility.Side;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Candidate;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Decision;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Why;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D4, THE GUARD, as tables (the seat, 2026-11-24). guard 8, trigger 30, chase 32 (from HIM), teleport 48, damaged window
 * 100. He stands at the origin; tick 1000. Every candidate passes harms(SUMMON, side); "he hit it" is no input at all.
 */
class SummonGuardTest {

    private static final SummonDefinition WOLF = new SummonDefinition("test_wolf", "wolf", "{owner}'s Wolf", 100, 20,
            8, 30, 32, 48, 20, 100);
    private static final Vec3 HIM = Vec3.ZERO;
    private static final long NOW = 1000;

    private static final UUID A = new UUID(9, 1), B = new UUID(9, 2), C = new UUID(9, 3), D = new UUID(9, 4);

    private static Candidate mob(UUID id, double x, OptionalLong damagedAt, boolean targetsHim) {
        return new Candidate(id, Side.MOB, new Vec3(x, 0, 0), true, damagedAt, targetsHim);
    }

    private static Decision decide(Vec3 summonAt, Optional<UUID> directed, List<Candidate> candidates) {
        return SummonGuard.decide(WOLF, HIM, summonAt, directed, Optional.empty(), candidates, NOW);
    }

    private static Decision decide(Optional<UUID> directed, Optional<UUID> current, List<Candidate> candidates) {
        return SummonGuard.decide(WOLF, HIM, Vec3.ZERO, directed, current, candidates, NOW);
    }

    @Test
    void thePriorityTable() {
        Candidate damaged = mob(A, 20, OptionalLong.of(950), false);   // damaged him 50 ticks ago
        Candidate targeting = mob(B, 10, OptionalLong.empty(), true);  // nearer, targets him
        Candidate directed = mob(C, 25, OptionalLong.empty(), false);
        List<Candidate> all = List.of(damaged, targeting, directed);
        assertEquals(new Decision(Optional.of(C), Why.DIRECTED, Optional.empty(), false, false),
                decide(new Vec3(2, 0, 0), Optional.of(C), all), "directed first");
        assertEquals(new Decision(Optional.of(A), Why.DAMAGED, Optional.empty(), false, false),
                decide(new Vec3(2, 0, 0), Optional.empty(), all), "then a mob that damaged him, though farther");
        assertEquals(new Decision(Optional.of(B), Why.TARGETING, Optional.empty(), false, false),
                decide(new Vec3(2, 0, 0), Optional.empty(), List.of(targeting, directed)), "then one targeting him");
        assertEquals(new Decision(Optional.empty(), Why.NONE, Optional.empty(), false, false),
                decide(new Vec3(2, 0, 0), Optional.empty(), List.of(directed)), "a mob that did neither: left alone");
    }

    @Test
    void theWindowTriggerAndTiesTable() {
        assertEquals(Optional.of(A), decide(Vec3.ZERO, Optional.empty(),
                List.of(mob(A, 20, OptionalLong.of(NOW - 100), false))).target(), "at the window's edge: still");
        assertEquals(Optional.empty(), decide(Vec3.ZERO, Optional.empty(),
                List.of(mob(A, 20, OptionalLong.of(NOW - 101), false))).target(), "past the window: forgotten");
        assertEquals(Optional.of(B), decide(Vec3.ZERO, Optional.empty(),
                List.of(mob(B, 30, OptionalLong.empty(), true))).target(), "targeting him AT 30: taken");
        assertEquals(Optional.empty(), decide(Vec3.ZERO, Optional.empty(),
                List.of(mob(B, 30.01, OptionalLong.empty(), true))).target(), "beyond 30 of him: not a trigger");
        assertEquals(Optional.of(A), decide(new Vec3(-20, 0, 0), Optional.empty(), List.of(
                mob(B, 12, OptionalLong.empty(), true), mob(A, 6, OptionalLong.empty(), true))).target(),
                "ties go to the nearest to HIM, not to the summon");
    }

    @Test
    void theSidesTable() {
        for (Side side : new Side[] {Side.PLAYER, Side.SUMMON}) {
            Candidate c = new Candidate(D, side, new Vec3(5, 0, 0), true, OptionalLong.of(NOW), true);
            assertEquals(Optional.empty(), decide(Vec3.ZERO, Optional.of(D), List.of(c)).target(),
                    side + ": never a target, directed, damaging or targeting");
        }
    }

    @Test
    void theDropAndTeleportTable() {
        Candidate far = mob(A, 33, OptionalLong.of(NOW), true);
        Decision d = decide(new Vec3(30, 0, 0), Optional.of(A), List.of(far));
        assertEquals(Optional.empty(), d.target(), "beyond the chase limit FROM HIM: dropped");
        assertEquals(Optional.of(SummonGuard.Dropped.LIMIT), d.dropped(), "directed, but beyond the limit");
        Candidate dead = new Candidate(A, Side.MOB, new Vec3(5, 0, 0), false, OptionalLong.of(NOW), true);
        assertEquals(Optional.of(SummonGuard.Dropped.DEAD), decide(Vec3.ZERO, Optional.of(A), List.of(dead)).dropped());
        assertEquals(Optional.of(SummonGuard.Dropped.GONE), decide(Vec3.ZERO, Optional.of(A), List.of()).dropped(),
                "a directed target no longer among the candidates");
        assertTrue(decide(new Vec3(48.01, 0, 0), Optional.empty(), List.of()).teleport(), "beyond 48 of him: teleport");
        assertFalse(decide(new Vec3(48, 0, 0), Optional.empty(), List.of()).teleport(), "at 48: not yet");
    }

    /**
     * F1-F4 (the seat, 2026-11-24): DIRECTED and CURRENT are separate inputs; the tier is re-decided every tick; a higher
     * tier pre-empts the current target; within its tier the current target is KEPT while it still qualifies; Why is the
     * tier that chose THIS tick's target; a lost current target's drop is reported by the tick that loses it.
     */
    @Test
    void theTierTable() {
        Candidate targeting = mob(B, 10, OptionalLong.empty(), true);
        Candidate damagedNear = mob(A, 6, OptionalLong.of(990), false);
        Candidate damagedFar = mob(D, 20, OptionalLong.of(990), false);
        // A current TARGETING target is not "directed" next tick: a mob that DAMAGES him pre-empts it.
        Decision d1 = decide(Optional.empty(), Optional.of(B), List.of(targeting, damagedFar));
        assertEquals(Optional.of(D), d1.target(), "current TARGETING, a DAMAGED mob appears: switches");
        assertEquals(Why.DAMAGED, d1.why(), "why=damaged, the tier that chose it");
        assertEquals(Why.TARGETING, decide(Optional.empty(), Optional.of(B), List.of(targeting)).why(),
                "kept, and still called TARGETING, never DIRECTED: he never clicked");
        // Within its tier the current target is KEPT while it still qualifies.
        assertEquals(Optional.of(D), decide(Optional.empty(), Optional.of(D), List.of(damagedFar, damagedNear)).target(),
                "current DAMAGED, a NEARER DAMAGED mob appears: stays");
        // Its window expires while another still qualifies: nearest-to-him chooses again.
        Candidate expired = mob(D, 20, OptionalLong.of(NOW - 101), false);
        assertEquals(Optional.of(A), decide(Optional.empty(), Optional.of(D), List.of(expired, damagedNear)).target(),
                "current DAMAGED whose window expires: switches to the one still qualifying");
        // Directed set while attacking another: switches, why=directed.
        Decision d4 = decide(Optional.of(C), Optional.of(A), List.of(damagedNear, mob(C, 25, OptionalLong.empty(), false)));
        assertEquals(Optional.of(C), d4.target());
        assertEquals(Why.DIRECTED, d4.why());
        // Directed dies: cleared, dropped=dead, and the next tier chooses THE SAME TICK.
        Candidate deadC = new Candidate(C, Side.MOB, new Vec3(25, 0, 0), false, OptionalLong.empty(), false);
        Decision d5 = decide(Optional.of(C), Optional.of(C), List.of(deadC, damagedNear));
        assertTrue(d5.clearDirected(), "the directed order is cleared");
        assertEquals(Optional.of(SummonGuard.Dropped.DEAD), d5.dropped());
        assertEquals(Optional.of(A), d5.target(), "falls to the next tier the same tick");
        assertEquals(Why.DAMAGED, d5.why());
        // A current non-directed target lost: dropped reported by the tick that loses it; the next tick, with the new
        // target current, reports nothing (the caller carries the new current: "once, not every tick").
        Decision lost = decide(Optional.empty(), Optional.of(B), List.of(damagedNear));
        assertEquals(Optional.of(SummonGuard.Dropped.GONE), lost.dropped());
        assertFalse(lost.clearDirected(), "a current target is not a directed order");
        assertEquals(Optional.empty(), decide(Optional.empty(), lost.target(), List.of(damagedNear)).dropped(),
                "the next tick: nothing to report");
    }

    @Test
    void idleKeepsWithinTheGuardRadius() {
        assertFalse(SummonGuard.needsReturn(WOLF, HIM, new Vec3(8, 0, 0)), "at 8: within");
        assertTrue(SummonGuard.needsReturn(WOLF, HIM, new Vec3(8.01, 0, 0)), "beyond 8: walk back toward him");
    }
}
