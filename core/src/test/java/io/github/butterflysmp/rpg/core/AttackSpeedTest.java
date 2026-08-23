package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AttackSpeed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The effective-cooldown maths, which is the whole of the attack-speed stat's mechanical effect.
 *
 * Every case here is a guard that exists because its absence is a real, shippable bug: a divisor
 * that silently gates an ungated trigger, a modifier pile that loops a debuff around into an
 * infinite buff, or a fast weapon whose cooldown reaches zero and stops throttling at all. Redden by
 * flipping the divide, deleting the floor, or dropping either guard.
 */
class AttackSpeedTest {

    @Test
    void neutralSpeedLeavesTheAuthoredCadenceExactlyAlone() {
        // The identity case, and the one that matters most: nothing in content grants attack speed
        // yet, so EVERY weapon in the game runs through this branch. If it is not an identity, the
        // whole game's cadence silently shifts.
        assertEquals(10, AttackSpeed.effectiveCooldownTicks(10, AttackSpeed.BASE));
        assertEquals(60, AttackSpeed.effectiveCooldownTicks(60, AttackSpeed.BASE));
    }

    @Test
    void doubleSpeedHalvesTheCooldown() {
        // Ironblade's swing: 10 ticks at 2.0 -> 5. This is what the boot gate should feel.
        assertEquals(5, AttackSpeed.effectiveCooldownTicks(10, 2.0));
    }

    @Test
    void halfSpeedDoublesTheCooldown() {
        // A slow debuff must lengthen, not shorten -- a sign error here reads as a buff.
        assertEquals(20, AttackSpeed.effectiveCooldownTicks(10, 0.5));
    }

    @Test
    void aDeclaredZeroCooldownStaysUngated() {
        // Content saying "no cooldown" must MEAN no cooldown. A naive max(1, ...) would floor this
        // to 1 and silently gate a trigger that was deliberately left free-firing.
        assertEquals(0, AttackSpeed.effectiveCooldownTicks(0, 2.0));
        assertEquals(0, AttackSpeed.effectiveCooldownTicks(0, AttackSpeed.BASE));
    }

    @Test
    void aLargeBuffFloorsAtOneTickRatherThanReachingZero() {
        // Reaching 0 would not be "very fast" -- it would remove the cooldown entirely and turn a
        // throttled weapon into an unthrottled one. The floor is what keeps a buff a buff.
        assertEquals(1, AttackSpeed.effectiveCooldownTicks(1, 5.0));
        assertEquals(1, AttackSpeed.effectiveCooldownTicks(4, 20.0));
    }

    @Test
    void aPathologicalNonPositiveSpeedIsClampedRatherThanDividingByZero() {
        // Modifiers are open-ended: enough negative sources could stack to 0 or below. Without the
        // clamp, 0 is an infinite cooldown (never swing again) and a negative is a NEGATIVE cooldown
        // -- a debuff that loops around into the strongest buff in the game.
        int atZero = AttackSpeed.effectiveCooldownTicks(10, 0.0);
        int atNegative = AttackSpeed.effectiveCooldownTicks(10, -3.0);

        assertEquals(AttackSpeed.effectiveCooldownTicks(10, AttackSpeed.MIN_SPEED), atZero);
        assertEquals(atZero, atNegative, "both clamp to the same floor speed");
        assertEquals(100, atZero, "10 ticks at the 0.1 floor is 100 -- slow, but finite and positive");
    }

    @Test
    void roundsToTheNearestTickRatherThanTruncating() {
        // 15 / 2.0 = 7.5. Truncation would drift every odd cadence a half-tick fast, forever.
        assertEquals(8, AttackSpeed.effectiveCooldownTicks(15, 2.0));
        // 10 / 3.0 = 3.33 -> 3
        assertEquals(3, AttackSpeed.effectiveCooldownTicks(10, 3.0));
    }
}
