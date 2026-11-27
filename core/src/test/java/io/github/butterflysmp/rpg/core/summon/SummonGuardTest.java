package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobHostility.Side;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Candidate;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Change;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Decision;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Dropped;
import io.github.butterflysmp.rpg.core.summon.SummonGuard.Held;
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
 * THE GUARD AS TABLES. D4 (the seat, 2026-11-24): guard 8, trigger 30, teleport 48, damaged window 100. And G1 to G6 (the
 * seat, 2026-11-26, from Ben's words: "Summons should keep their target until it is defeated or their target switches"
 * and "A wolf (or any summon) should give up if it's target if it's target is outside of a 72 block range"): a HELD
 * target is KEPT until it is dead, gone or beyond {@code give_up_range} 72 FROM HIM, or a STRICTLY HIGHER tier switches
 * it; the reason it was acquired lapsing is not a drop; a summon holding a target is never teleported to him.
 *
 * <p>He stands at the origin; tick 1000. Every candidate passes harms(SUMMON, side); "he hit it" is no input at all. Each
 * row of the D4 table is RE-STATED with its new answer (marked NEW where it changed), then G5's rows.
 */
class SummonGuardTest {

    private static final SummonDefinition WOLF = new SummonDefinition("test_wolf", "wolf", "{owner}'s Wolf", 100, 20,
            8, 30, 72, 48, 20, 100);
    private static final Vec3 HIM = Vec3.ZERO;
    private static final long NOW = 1000;

    private static final UUID A = new UUID(9, 1), B = new UUID(9, 2), C = new UUID(9, 3), D = new UUID(9, 4);

    private static Candidate mob(UUID id, double x, OptionalLong damagedAt, boolean targetsHim) {
        return new Candidate(id, Side.MOB, new Vec3(x, 0, 0), true, damagedAt, targetsHim);
    }

    private static Candidate targeting(UUID id, double x) { return mob(id, x, OptionalLong.empty(), true); }
    private static Candidate damaged(UUID id, double x) { return mob(id, x, OptionalLong.of(NOW - 10), false); }
    private static Candidate idle(UUID id, double x) { return mob(id, x, OptionalLong.empty(), false); }

    private static Held held(UUID id, Why tier) { return new Held(id, tier, false); }

    private static Decision decide(Vec3 summonAt, Optional<UUID> directed, Optional<Held> held, List<Candidate> cs) {
        return SummonGuard.decide(WOLF, HIM, summonAt, directed, held, cs, NOW);
    }

    private static Decision decide(Optional<UUID> directed, Optional<Held> held, List<Candidate> cs) {
        return decide(Vec3.ZERO, directed, held, cs);
    }

    // --- D4's rows, re-stated ---------------------------------------------------------------------------------------

    @Test
    void thePriorityTable_nothingHeld() {
        Candidate dmg = mob(A, 20, OptionalLong.of(950), false);
        Candidate tgt = targeting(B, 10);
        Candidate dir = idle(C, 25);
        List<Candidate> all = List.of(dmg, tgt, dir);
        Decision d1 = decide(Optional.of(C), Optional.empty(), all);
        assertEquals(Optional.of(C), d1.target(), "directed first");
        assertEquals("acquired:directed", d1.reason());
        Decision d2 = decide(Optional.empty(), Optional.empty(), all);
        assertEquals(Optional.of(A), d2.target(), "then a mob that damaged him, though farther");
        assertEquals("acquired:damaged", d2.reason());
        Decision d3 = decide(Optional.empty(), Optional.empty(), List.of(tgt, dir));
        assertEquals(Optional.of(B), d3.target(), "then one targeting him");
        assertEquals("acquired:targeting", d3.reason());
        Decision d4 = decide(Optional.empty(), Optional.empty(), List.of(dir));
        assertEquals(Optional.empty(), d4.target(), "a mob that did neither: left alone");
        assertEquals(Change.NONE, d4.change());
        assertEquals("none", d4.reason());
    }

    @Test
    void theWindowTriggerAndTiesTable_unchanged() {
        assertEquals(Optional.of(A), decide(Optional.empty(), Optional.empty(),
                List.of(mob(A, 20, OptionalLong.of(NOW - 100), false))).target(), "at the window's edge: still");
        assertEquals(Optional.empty(), decide(Optional.empty(), Optional.empty(),
                List.of(mob(A, 20, OptionalLong.of(NOW - 101), false))).target(), "past the window: no reason to take it");
        assertEquals(Optional.of(B), decide(Optional.empty(), Optional.empty(), List.of(targeting(B, 30))).target(),
                "targeting him AT 30: taken");
        assertEquals(Optional.empty(), decide(Optional.empty(), Optional.empty(), List.of(targeting(B, 30.01))).target(),
                "beyond 30 of him: not a trigger");
        assertEquals(Optional.of(A), decide(new Vec3(-20, 0, 0), Optional.empty(), Optional.empty(),
                List.of(targeting(B, 12), targeting(A, 6))).target(), "ties go to the nearest to HIM, not to the summon");
    }

    @Test
    void theSidesTable_unchanged() {
        for (Side side : new Side[] {Side.PLAYER, Side.SUMMON}) {
            Candidate c = new Candidate(D, side, new Vec3(5, 0, 0), true, OptionalLong.of(NOW), true);
            assertEquals(Optional.empty(), decide(Optional.of(D), Optional.empty(), List.of(c)).target(),
                    side + ": never a target, directed, damaging or targeting");
        }
    }

    @Test
    void theDropAndTeleportTable() {
        // NEW: D4 dropped a directed mob 33 from him (the old chase limit 32). It is now within the give-up range.
        Decision at33 = decide(new Vec3(30, 0, 0), Optional.of(A), Optional.empty(), List.of(mob(A, 33, OptionalLong.of(NOW), true)));
        assertEquals(Optional.of(A), at33.target(), "NEW: 33 from him is within 72: taken");
        assertEquals(Optional.empty(), at33.dropped());
        Decision far = decide(new Vec3(30, 0, 0), Optional.of(A), Optional.empty(), List.of(mob(A, 72.001, OptionalLong.of(NOW), true)));
        assertEquals(Optional.empty(), far.target(), "beyond 72 FROM HIM: dropped");
        assertEquals(Optional.of(Dropped.RANGE), far.dropped(), "directed, but beyond the range");
        assertTrue(far.clearDirected());
        Candidate dead = new Candidate(A, Side.MOB, new Vec3(5, 0, 0), false, OptionalLong.of(NOW), true);
        assertEquals(Optional.of(Dropped.DEAD), decide(Optional.of(A), Optional.empty(), List.of(dead)).dropped());
        assertEquals(Optional.of(Dropped.GONE), decide(Optional.of(A), Optional.empty(), List.of()).dropped(),
                "a directed target no longer among the candidates");
        assertTrue(decide(new Vec3(48.01, 0, 0), Optional.empty(), Optional.empty(), List.of()).teleport(),
                "nothing held, beyond 48 of him: teleport");
        assertFalse(decide(new Vec3(48, 0, 0), Optional.empty(), Optional.empty(), List.of()).teleport(), "at 48: not yet");
    }

    @Test
    void theTierTable_reStated() {
        Candidate tgtB = targeting(B, 10);
        Candidate dmgNear = mob(A, 6, OptionalLong.of(990), false);
        Candidate dmgFar = mob(D, 20, OptionalLong.of(990), false);
        // Held TARGETING, a mob that DAMAGED him appears: a strictly higher tier, so it switches (unchanged answer).
        Decision d1 = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(tgtB, dmgFar));
        assertEquals(Optional.of(D), d1.target(), "held TARGETING, a DAMAGED mob appears: switches");
        assertEquals("switched:targeting->damaged", d1.reason());
        Decision kept = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(tgtB));
        assertEquals(Why.TARGETING, kept.why(), "kept, and still called TARGETING, never DIRECTED: he never clicked");
        assertEquals(Change.NONE, kept.change(), "nothing to report while it still qualifies");
        // Held DAMAGED, a NEARER DAMAGED mob appears: an equal tier, kept (unchanged answer).
        assertEquals(Optional.of(D), decide(Optional.empty(), Optional.of(held(D, Why.DAMAGED)), List.of(dmgFar, dmgNear)).target(),
                "held DAMAGED, a nearer DAMAGED mob: stays");
        // NEW: its window expires while another still qualifies. D4 switched to the one still qualifying; G1 keeps it.
        Decision expired = decide(Optional.empty(), Optional.of(held(D, Why.DAMAGED)),
                List.of(mob(D, 20, OptionalLong.of(NOW - 101), false), dmgNear));
        assertEquals(Optional.of(D), expired.target(), "NEW: held DAMAGED whose window expires: KEPT");
        assertEquals("kept", expired.reason());
        // A directed order while it holds another: switches, why=directed (unchanged answer).
        Decision d4 = decide(Optional.of(C), Optional.of(held(A, Why.DAMAGED)), List.of(dmgNear, idle(C, 25)));
        assertEquals(Optional.of(C), d4.target());
        assertEquals("switched:damaged->directed", d4.reason());
        // The directed mob dies: cleared, dropped=dead, and the next tier chooses THE SAME TICK (unchanged answer).
        Candidate deadC = new Candidate(C, Side.MOB, new Vec3(25, 0, 0), false, OptionalLong.empty(), false);
        Decision d5 = decide(Optional.of(C), Optional.of(held(C, Why.DIRECTED)), List.of(deadC, dmgNear));
        assertTrue(d5.clearDirected(), "the directed order is cleared");
        assertEquals(Optional.of(Dropped.DEAD), d5.dropped());
        assertEquals(Optional.of(A), d5.target(), "falls to a fresh acquisition the same tick");
        assertEquals("dropped:dead,acquired:damaged", d5.reason());
        // A held, non-directed target gone: dropped by the tick that loses it; nothing to report the tick after.
        Decision lost = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(dmgNear));
        assertEquals(Optional.of(Dropped.GONE), lost.dropped());
        assertFalse(lost.clearDirected(), "a held target is not a directed order");
        assertEquals("dropped:gone,acquired:damaged", lost.reason());
        assertEquals(Change.NONE, decide(Optional.empty(), lost.held(), List.of(dmgNear)).change(), "the next tick: nothing");
    }

    @Test
    void idleKeepsWithinTheGuardRadius_unchanged() {
        assertFalse(SummonGuard.needsReturn(WOLF, HIM, new Vec3(8, 0, 0)), "at 8: within");
        assertTrue(SummonGuard.needsReturn(WOLF, HIM, new Vec3(8.01, 0, 0)), "beyond 8: walk back toward him");
    }

    // --- G5's rows (the seat, 2026-11-26) ---------------------------------------------------------------------------

    /** G1. Mutation: the held target re-decided by its tier's reason (D4's rule). */
    @Test
    void theReasonLapsing_isKept_andSaidOnce() {
        Decision lapse = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(idle(B, 10)));
        assertEquals(Optional.of(B), lapse.target(), "it stopped targeting him: KEPT");
        assertEquals("kept", lapse.reason(), "said once, on the tick the reason lapses");
        assertTrue(lapse.held().orElseThrow().lapsed());
        Decision next = decide(Optional.empty(), lapse.held(), List.of(idle(B, 10)));
        assertEquals(Optional.of(B), next.target());
        assertEquals(Change.NONE, next.change(), "and not again while it stays lapsed");
    }

    /** G1: the owner blinks, every mob forgets him (the Blink's forget). Mutation: as above. */
    @Test
    void theOwnerBlinks_andEveryMobForgetsHim_isKept() {
        Decision d = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(idle(B, 3), idle(A, 4)));
        assertEquals(Optional.of(B), d.target());
        assertEquals("kept", d.reason());
        assertFalse(d.teleport());
    }

    /** G2. Mutation: an equal or a lower tier allowed to switch, or a higher one not. */
    @Test
    void switchingIsUpwardOnly() {
        assertEquals("switched:targeting->damaged",
                decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(targeting(B, 10), damaged(A, 20))).reason(),
                "a higher tier arrives: switch");
        assertEquals(Optional.of(B),
                decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(targeting(B, 10), targeting(A, 3))).target(),
                "an equal tier, nearer: kept");
        assertEquals(Optional.of(D),
                decide(Optional.empty(), Optional.of(held(D, Why.DAMAGED)), List.of(damaged(D, 20), targeting(A, 3))).target(),
                "a lower tier: kept");
        assertEquals("switched:damaged->directed",
                decide(Optional.of(C), Optional.of(held(D, Why.DAMAGED)), List.of(damaged(D, 20), idle(C, 25))).reason(),
                "a directed order replaces any other");
        assertEquals("switched:directed->directed",
                decide(Optional.of(C), Optional.of(held(D, Why.DIRECTED)), List.of(idle(D, 20), idle(C, 25))).reason(),
                "a NEW directed order replaces the old directed target");
        assertEquals(Change.NONE,
                decide(Optional.of(C), Optional.of(held(C, Why.DIRECTED)), List.of(idle(C, 25))).change(),
                "the same order on the mob it holds: nothing to report");
    }

    /** CC'S CALL: the held mob itself reaching a higher tier is promoted in place, no switch and no line. */
    @Test
    void theHeldMobItselfReachingAHigherTier_isPromotedInPlace() {
        Decision d = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(damaged(B, 10), damaged(A, 4)));
        assertEquals(Optional.of(B), d.target(), "it hit him itself: still B, not the other damaging mob");
        assertEquals(Why.DAMAGED, d.why(), "now held at the damaged tier");
        assertEquals(Change.NONE, d.change());
    }

    /** G3. Mutation: the range moved off 72, or made strict the other way, or measured from the summon. */
    @Test
    void theGiveUpRangeIs72FromHim_inclusive() {
        Decision at72 = decide(new Vec3(70, 0, 0), Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(idle(B, 72.0)));
        assertEquals(Optional.of(B), at72.target(), "exactly 72.0 from him: kept");
        Decision beyond = decide(new Vec3(70, 0, 0), Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(idle(B, 72.001)));
        assertEquals(Optional.empty(), beyond.target(), "72.001 from him: given up");
        assertEquals("dropped:range", beyond.reason());
        assertEquals(Optional.of(B), decide(new Vec3(-60, 0, 0), Optional.empty(), Optional.of(held(B, Why.TARGETING)),
                List.of(idle(B, 70))).target(), "130 from the SUMMON, 70 from him: kept (measured from the owner)");
        Decision past32 = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(targeting(B, 40)));
        assertEquals(Optional.of(B), past32.target(), "40 from him: D4's 32 would have dropped it (dropped=limit)");
        assertEquals("kept", past32.reason(), "its targeting reason lapsed past 30: kept, said once");
    }

    /** G4. Mutation: the teleport left unconditional (D4's). */
    @Test
    void aSummonHoldingATarget_isNeverTeleported() {
        Decision d = decide(new Vec3(60, 0, 0), Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(idle(B, 62)));
        assertEquals(Optional.of(B), d.target());
        assertFalse(d.teleport(), "60 from him, beyond 48, but holding a target: no teleport");
        assertTrue(decide(new Vec3(60, 0, 0), Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of()).teleport(),
                "its target gone the same tick: nothing held, so the teleport stays as it is");
    }

    /** G6: a target given up for range comes back only by a fresh acquisition under today's rules. */
    @Test
    void givenUpForRange_isTakenAgainOnlyFreshly() {
        Decision up = decide(Optional.empty(), Optional.of(held(B, Why.TARGETING)), List.of(targeting(B, 72.5)));
        assertEquals(Optional.empty(), up.target());
        assertEquals(Optional.empty(), decide(Optional.empty(), up.held(), List.of(targeting(B, 71))).target(),
                "back inside 72 and still targeting him, but beyond 30: no fresh reason, not taken");
        assertEquals("acquired:damaged", decide(Optional.empty(), up.held(),
                List.of(mob(B, 71, OptionalLong.of(NOW - 10), true))).reason(), "it damaged him within the window: taken freshly");
    }
}
