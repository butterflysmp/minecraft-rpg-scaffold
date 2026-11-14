package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sunder's S1 machine (PLAN-melee-class.md section 8.2; the seat's ruling 4): the endings a real {@link SafeLanding}
 * produces, classified one step at a time. Every row drives {@link SafeLanding#step} itself rather than building
 * states by hand, so a row cannot present an ending the machine never reaches.
 */
class TouchdownTest {

    /** The ending of each step, for a fresh mark stepped through (onGround, fallDistance) pairs. */
    private static List<Touchdown> endings(Object... pairs) {
        List<Touchdown> out = new ArrayList<>();
        SafeLanding s = SafeLanding.armed();
        for (int i = 0; i < pairs.length; i += 2) {
            SafeLanding next = s.step((Boolean) pairs[i], ((Number) pairs[i + 1]).doubleValue());
            out.add(Touchdown.of(s, next));
            s = next;
        }
        return out;
    }

    /** Only the slamming endings, in order: a leap must produce exactly one or none. */
    private static List<Touchdown> slams(List<Touchdown> endings) {
        return endings.stream().filter(Touchdown::slams).toList();
    }

    @Test
    void aFlatLeapSlamsOnceOnTheFirstGroundContactAndNotOnTheClearingTick() {
        var e = endings(false, 0, false, 0, false, 1.2, true, 0, true, 0);
        assertEquals(List.of(Touchdown.NONE, Touchdown.NONE, Touchdown.NONE, Touchdown.LANDED, Touchdown.NONE), e);
    }

    /** Ruling 4: water (and a ladder, a cobweb) absorbing a started fall IS a landing. */
    @Test
    void aFallAbsorbedMidAirSlams() {
        var e = endings(false, 0, false, 1.5, false, 0);
        assertEquals(Touchdown.ABSORBED, e.getLast());
        assertEquals(List.of(Touchdown.ABSORBED), slams(e));
    }

    /** Ruling 4: a leap that never leaves the ground (a ceiling) slams IN PLACE, once, when TAKE_OFF_TICKS runs out. */
    @Test
    void aFailedTakeOffSlamsInPlace() {
        Object[] grounded = new Object[2 * (SafeLanding.TAKE_OFF_TICKS + 1)];
        for (int i = 0; i < grounded.length; i += 2) { grounded[i] = true; grounded[i + 1] = 0; }
        var e = endings(grounded);
        assertEquals(Touchdown.NO_TAKE_OFF, e.getLast());
        assertEquals(List.of(Touchdown.NO_TAKE_OFF), slams(e));
        for (int i = 0; i < e.size() - 1; i++) assertEquals(Touchdown.NONE, e.get(i), "tick " + (i + 1));
    }

    /**
     * THE BACKSTOP DOES NOT SLAM, AND THIS ROW IS THE ONE THAT CAN TELL: a mark still AIRBORNE when the backstop
     * clears it reaches CLEARED from AIRBORNE, exactly as an absorbed fall does. Classify by phase alone and it reads
     * ABSORBED and slams. The fall distance is kept rising so the absorbed arm never fires first.
     */
    @Test
    void theBackstopClearsAnAirborneMarkWithoutASlam() {
        Object[] airborne = new Object[2 * (SafeLanding.BACKSTOP_TICKS + 1)];
        for (int i = 0; i < airborne.length; i += 2) { airborne[i] = false; airborne[i + 1] = 1.0 + i; }
        var e = endings(airborne);
        assertEquals(Touchdown.BACKSTOP, e.getLast());
        assertEquals(List.of(), slams(e));
    }

    /** A leap started in the air goes straight to AIRBORNE and slams where it actually comes down. */
    @Test
    void aLeapStartedInTheAirSlamsAtItsRealLanding() {
        var e = endings(false, 0.4, false, 1.0, false, 2.0, true, 0);
        assertEquals(List.of(Touchdown.LANDED), slams(e));
        assertEquals(Touchdown.LANDED, e.getLast());
    }

    @Test
    void onlyTheThreeLandingEndingsSlam() {
        assertTrue(Touchdown.LANDED.slams());
        assertTrue(Touchdown.ABSORBED.slams());
        assertTrue(Touchdown.NO_TAKE_OFF.slams());
        for (Touchdown t : List.of(Touchdown.NONE, Touchdown.BACKSTOP, Touchdown.FORGOTTEN, Touchdown.SUPERSEDED)) {
            assertFalse(t.slams(), t.name());
        }
    }

    /** The LANDING trace line: the gate rows read it verbatim, so its shape is pinned here, not at a boot. */
    @Test
    void theTraceLineNamesTheEndingWhetherItSlamsAndWhere() {
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals("LANDING caster=00000000-0000-0000-0000-000000000001 source=sunder how=LANDED slam=true"
                        + " at=5.50 64.00 -3.25",
                Touchdown.LANDED.line(id, "sunder", new io.github.butterflysmp.rpg.core.Vec3(5.5, 64, -3.25)));
        assertEquals("LANDING caster=00000000-0000-0000-0000-000000000001 source=sunder how=FORGOTTEN slam=false at=-",
                Touchdown.FORGOTTEN.line(id, "sunder", null));
    }

    /** A cleared mark steps to itself, and that is never an ending again. */
    @Test
    void aClearedMarkNeverEndsTwice() {
        var e = endings(false, 0, true, 0, true, 0, true, 0, false, 0, true, 0);
        assertEquals(List.of(Touchdown.LANDED), slams(e));
    }
}
