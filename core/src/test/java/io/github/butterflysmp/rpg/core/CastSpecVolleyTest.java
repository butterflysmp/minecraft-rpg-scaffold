package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The volley record's own refusals, and the cooldown floor derived from its three numbers.
 *
 * <b>WHY THE REFUSALS ARE IN THE RECORD AND NOT ONLY IN THE LOADER.</b> A compact constructor makes
 * a bad volley UNCONSTRUCTIBLE, so no call site -- loader, test, or something not written yet -- can
 * produce one. The loader's whitelist is a different question (WHICH cast may be repeated) and lives
 * there because it is content policy; these five are representability.
 *
 * <b>WHAT THIS SUITE CANNOT SEE:</b> whether the floor is APPLIED. That is AbilityServiceTest's job.
 * This file only pins the arithmetic and the refusals.
 *
 * Each row names the mutation it forces red.
 */
class CastSpecVolleyTest {

    private static CastSpec.Volley volley(int windup, int shots, int interval) {
        return new CastSpec.Volley(windup, shots, interval, new CastSpec.Ray(30));
    }

    @Test
    void theFloorIsTheTickOfTheLASTFiringDerivedFromAllTHREENumbers() {
        assertEquals(24, volley(12, 5, 3).minimumCooldownTicks(),
                "12 windup + 4 intervals of 3 -- shot 5 is fired at t=24");

        // EACH INPUT MOVED SEPARATELY, so a formula that ignores one of the three cannot pass.
        assertEquals(25, volley(13, 5, 3).minimumCooldownTicks(), "windup is a summand");
        assertEquals(30, volley(12, 7, 3).minimumCooldownTicks(), "shots multiplies the interval");
        assertEquals(28, volley(12, 5, 4).minimumCooldownTicks(), "interval multiplies the count");
        // NO TWO OF 12, 5, 3 ARE EQUAL AND NONE EQUALS 24. With a fixture where two of the four
        // quantities coincide, at least one of them is not being tested -- windup + (shots-1)*interval
        // and windup * interval would both give 36 at (12, 4, 3), for instance.
        //
        // Mutation: shots instead of shots - 1 -> 27, reddens. Drop windup -> 12, reddens. Use
        // shots * interval -> 15, reddens.
    }

    @Test
    void aSINGLEShotVolleyIsATelegraphedSingleCastAndItsFloorIsJustTheWindup() {
        assertEquals(20, volley(20, 1, 2).minimumCooldownTicks(),
                "one shot fires at the end of the wind-up and there is no interval to add");
        // This is the shape named in CastSpec.Volley's javadoc: a delayed strike with an audible
        // commitment, which nothing else in the schema can express. The row exists so that the
        // (shots - 1) term is pinned at its boundary rather than only in the middle of its range.
        // Mutation: shots instead of shots - 1 -> 22, reddens.
    }

    @Test
    void everyOTHERCastShapeHasNoFloorAndTheSwitchIsEXHAUSTIVE() {
        assertEquals(0, CastSpec.minimumCooldownTicks(new CastSpec.Self()));
        assertEquals(0, CastSpec.minimumCooldownTicks(new CastSpec.Melee(3, 90)));
        assertEquals(0, CastSpec.minimumCooldownTicks(new CastSpec.Ray(30)));
        assertEquals(0, CastSpec.minimumCooldownTicks(new CastSpec.Projectile(1, 0.03, 100)));
        assertEquals(0, CastSpec.minimumCooldownTicks(
                new CastSpec.Dash(12, 1.6, 0.4, CastSpec.DashDirection.REVERSE_FACING)));
        assertEquals(24, CastSpec.minimumCooldownTicks(volley(12, 5, 3)),
                "and the dispatcher reaches the volley's own derivation rather than shadowing it");
        // THE COMPILER IS THE REAL GUARD HERE, not this row: the switch is a pattern switch over a
        // sealed type, so a seventh kind fails the build until someone states its answer. This row
        // pins that the five existing answers are 0 and that the sixth is not, which the compiler
        // cannot say. It is why the method is a static switch rather than a `default` interface
        // method -- a default would let a seventh kind inherit "no floor" silently.
    }

    @Test
    void aVolleyCannotRepeatAVOLLEY() {
        var inner = volley(10, 3, 5);
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Volley(10, 3, 5, inner));
        assertTrue(thrown.getMessage().contains("cannot repeat a volley"), thrown.getMessage());
        // Refused in the RECORD, not only the loader, because it is a fork bomb: 3 shots each
        // starting a 3-shot burst is 9, and a third level is 27. Mutation: drop the instanceof
        // check -> this reddens, and CastExecutor.fireInner's Volley arm becomes reachable.
    }

    @Test
    void theThreeNUMERICRefusalsEachNameTheirOwnField() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Volley(-1, 3, 5, new CastSpec.Ray(30)))
                .getMessage().contains("windup_ticks"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Volley(10, 0, 5, new CastSpec.Ray(30)))
                .getMessage().contains("shots"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Volley(10, 3, 0, new CastSpec.Ray(30)))
                .getMessage().contains("interval_ticks"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Volley(10, 3, 5, null))
                .getMessage().contains("no 'of' cast"));

        // THE BOUNDARIES ON THE ACCEPTING SIDE, so the refusals cannot be over-broad. A windup of 0
        // is legal (the first shot fires on the cast frame) while an interval of 0 is not (the
        // scheduler cannot defer by less than a tick) -- two adjacent fields with DIFFERENT floors,
        // which is exactly the pair a single copied guard would get wrong.
        assertEquals(0, new CastSpec.Volley(0, 1, 1, new CastSpec.Ray(30)).minimumCooldownTicks(),
                "windup 0 with one shot is a cast that fires immediately, and it is legal");
        // Mutation: make windup's guard `< 1` like interval's -> the accepting assertion reddens.
        // Mutation: make interval's guard `< 0` like windup's -> the interval refusal reddens.
    }
}
