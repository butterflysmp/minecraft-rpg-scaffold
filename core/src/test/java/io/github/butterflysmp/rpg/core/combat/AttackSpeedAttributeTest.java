package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The arithmetic behind the vanilla attack-speed modifier -- the half of the reflection that can be
 * proven without a server, exactly as DefenseTest covers Defense#barModifier for the armor bar.
 *
 * Every expected value below was produced by EXECUTING the expression and pasting what it printed,
 * never by reasoning about it. That matters more than usual here: modifier(1.6, 2.0) is
 * -0.79999999999999980, NOT -0.8, so a test written from the algebra with an exact assertion would
 * have failed on a correct implementation. Where an exact assertion IS used below, execution
 * confirmed the value is exact first.
 *
 * Each test names the mutation it forces red.
 */
class AttackSpeedAttributeTest {

    private static final double EPS = 1e-9;

    @Test
    void anUnboostedWeaponSwingsAtExactlyItsAuthoredCadence() {
        // The identity case, and the one every player is in: nothing in shipped content grants
        // attack speed, so EVERY swing in the game runs through this branch. Exact, because
        // execution confirms 1.6 * 1.0 == 1.6 and the modifier lands on -2.4 exactly.
        assertEquals(1.6, AttackSpeedAttribute.desiredSpeed(1.6, 1.0));
        assertEquals(-2.4, AttackSpeedAttribute.modifier(1.6, 1.0));
        // Mutation: return weaponBaseSpeed + statMultiplier -> 2.6 -> reddens.
    }

    @Test
    void theStatMultipliesTheWeaponsCadenceRatherThanAddingToIt() {
        // A "+100%" source resolves to 2.0 and DOUBLES the weapon's own speed. Adding instead would
        // make the same boost worth the same on a fast weapon as on a slow one, which is not what a
        // percentage means -- and on a 1.6 sword the two happen to differ by only 0.4, which is
        // exactly the kind of near-miss that survives a careless eyeball.
        assertEquals(3.2, AttackSpeedAttribute.desiredSpeed(1.6, 2.0), EPS);
        assertEquals(-0.8, AttackSpeedAttribute.modifier(1.6, 2.0), EPS);
        // A slowing multiplier goes the other way, and further below the base.
        assertEquals(0.8, AttackSpeedAttribute.desiredSpeed(1.6, 0.5), EPS);
        assertEquals(-3.2, AttackSpeedAttribute.modifier(1.6, 0.5), EPS);
        // Mutation: weaponBaseSpeed + (multiplier - 1) -> 2.6 at m=2.0 -> reddens.
    }

    @Test
    void aBigEnoughBoostGoesPositiveWhichIsCorrectNotABug() {
        // Normally the modifier is NEGATIVE -- every real weapon is slower than a bare fist. But a
        // large enough boost legitimately swings faster than an empty hand, and a guard that
        // clamped the modifier at 0 "because it should be negative" would silently cap the stat.
        assertTrue(AttackSpeedAttribute.modifier(1.6, 3.0) > 0.0,
                "1.6 at 3.0x is 4.8, faster than the 4.0 bare-hand base");
        assertEquals(4.8, AttackSpeedAttribute.desiredSpeed(1.6, 3.0), EPS);
        // Mutation: clamp the modifier to <= 0 -> reddens.
    }

    // --- The absent case, which is the leak story ---

    @Test
    void aWeaponWithNoDeclaredCadenceResolvesToZeroSoTheCallerWritesNothing() {
        // Holding a bow, a staff, or nothing: there is no melee cadence to express. desiredSpeed
        // returns 0 as the caller's signal to write NO modifier and leave the vanilla base alone.
        assertEquals(0.0, AttackSpeedAttribute.desiredSpeed(0.0, 2.0), EPS);
        assertEquals(0.0, AttackSpeedAttribute.desiredSpeed(0.0, 1.0), EPS);
        assertEquals(0.0, AttackSpeedAttribute.desiredSpeed(-1.0, 1.0), EPS,
                "a negative is nonsense input and reads as absent, never as a reversed swing");
        // Mutation: drop the weaponBaseSpeed <= 0 guard -> a boosted empty hand resolves to 2.0 and
        // the player swings at half speed while holding nothing -> reddens.
    }

    @Test
    void theAbsentCaseIsWhyTheCallerBranchesOnSpeedRatherThanOnTheModifier() {
        // The trap this test exists to pin: modifier() of an absent weapon is -4.0, not 0 -- which
        // if written would drive the attribute to a flat ZERO and stop the player attacking at all.
        // So the caller MUST decide on desiredSpeed (or the raw base) and never on the modifier.
        assertEquals(-4.0, AttackSpeedAttribute.modifier(0.0, 1.0), EPS,
                "not a usable value -- the caller must not reach here for an absent weapon");
        assertEquals(0.0, AttackSpeedAttribute.VANILLA_BASE + AttackSpeedAttribute.modifier(0.0, 1.0), EPS,
                "writing it would zero the attribute outright");
        // Mutation: make the override's guard test the modifier instead of the speed -> a player
        // holding nothing gets a zeroed attack speed -> this is the arithmetic that says why.
    }

    @Test
    void theModifierIsExpressedRelativeToTheVanillaBase() {
        // The whole point of subtracting: base + modifier must equal the speed we asked for.
        for (double[] each : new double[][] {{1.6, 1.0}, {1.6, 2.0}, {2.0, 1.0}, {0.5, 1.5}}) {
            double base = each[0], multiplier = each[1];
            assertEquals(AttackSpeedAttribute.desiredSpeed(base, multiplier),
                    AttackSpeedAttribute.VANILLA_BASE + AttackSpeedAttribute.modifier(base, multiplier), EPS,
                    "base + modifier must land on the desired speed, at " + base + " x " + multiplier);
        }
        // Mutation: drop the "- VANILLA_BASE" -> the totals overshoot by 4.0 everywhere -> reddens.
    }
}
