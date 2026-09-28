package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The health multiplier: vanilla 5x (M1), gear score / 100 for hostile mobs (M2), no 5x for custom
 * mobs (M4), no GS for passive mobs (M7). The whole grid is asserted, because a multiplier that is
 * wrong in one cell is right in the other fifteen.
 *
 * <p>Bases are chosen so no two quantities in a row collide: a zombie's 20 x 5 = 100 equals the GS
 * baseline, which is why the spider (16) and the Warden (500) rows exist.
 */
class MobScalingTest {

    private static final double EPS = 1e-9;

    // --- the M2 examples, verbatim from the ruling -------------------------------------------

    @Test
    void aGs500ZombieHas500Hp() {
        assertEquals(500, MobScaling.maxHealth(20, false, true, 500), EPS, "20 x 5 x 5.00 (M2)");
    }

    @Test
    void aGs500WardenHas12500Hp() {
        assertEquals(12_500, MobScaling.maxHealth(500, false, true, 500), EPS,
                "500 x 5 x 5.00 (M2) -- far past vanilla's attribute cap, which is why it never goes there (M9)");
    }

    // --- the grid ------------------------------------------------------------------------------

    @Test
    void vanillaHostileIsFiveTimesTimesGsOverOneHundred() {
        // spider, base 16: no cell collides with its own GS
        assertEquals(80, MobScaling.maxHealth(16, false, true, 100), EPS);
        assertEquals(160, MobScaling.maxHealth(16, false, true, 200), EPS);
        assertEquals(240, MobScaling.maxHealth(16, false, true, 300), EPS);
        assertEquals(400, MobScaling.maxHealth(16, false, true, 500), EPS);
        // Mutations: VANILLA_FACTOR 5 -> 1 reddens every row; / GS_BASELINE -> / 50 doubles them.
    }

    @Test
    void vanillaPassiveIsFiveTimesAndIgnoresTheGs() {
        // M7. A cow is 10 x 5 = 50 whatever score is passed -- the score must not leak in.
        assertEquals(50, MobScaling.maxHealth(10, false, false, 100), EPS);
        assertEquals(50, MobScaling.maxHealth(10, false, false, 500), EPS,
                "a passive mob takes NO gear score (M7). Mutation: ignore isHostile -> 250 -> reddens");
    }

    @Test
    void customHostileTakesOnlyTheGs() {
        // M4. The Knell is 360 at GS 100, NOT 1,800.
        assertEquals(360, MobScaling.maxHealth(360, true, true, 100), EPS,
                "no vanilla 5x on a custom mob (M4). Mutation: ignore isCustom -> 1800 -> reddens");
        assertEquals(720, MobScaling.maxHealth(360, true, true, 200), EPS, "but the GS still applies (M4, M18)");
        assertEquals(1_800, MobScaling.maxHealth(360, true, true, 500), EPS);
    }

    @Test
    void customPassiveIsUnscaled() {
        // M4 + M7: neither multiplier. None is shipped; stated so the arm is decided, not discovered.
        assertEquals(30, MobScaling.maxHealth(30, true, false, 500), EPS);
    }

    // --- the guard -----------------------------------------------------------------------------

    @Test
    void aHostileMobWithNoValidScoreIsASeedingBugAndThrows() {
        assertThrows(IllegalArgumentException.class, () -> MobScaling.maxHealth(20, false, true, 0),
                "GS 0 would be a zero-health mob -- never seeded silently");
        assertThrows(IllegalArgumentException.class, () -> MobScaling.maxHealth(20, false, true, 501),
                "above the M20 cap is not a score we ever roll");
    }

    @Test
    void aPassiveMobIgnoresEvenAnInvalidScore() {
        // The passive path never reads gs, so the seed can pass anything for it.
        assertEquals(50, MobScaling.maxHealth(10, false, false, 0), EPS);
    }

    // --- the attack half (slice 2): the same grid, because M1 and M2 say "health AND damage" ----------

    @Test
    void aGs100ZombieHitsForFiveTimesItsAttribute() {
        // A zombie's vanilla ATTACK_DAMAGE is 3: GX-MELEE's expectation, now x5 rather than x1.
        assertEquals(15, MobScaling.attackDamage(3, false, true, 100), EPS, "3 x 5 x 1.00 (M1)");
    }

    @Test
    void vanillaHostileAttackIsFiveTimesTimesGsOverOneHundred() {
        // base 3: no cell collides with its own GS, and 3 x 5 = 15 is not the baseline either
        assertEquals(15, MobScaling.attackDamage(3, false, true, 100), EPS);
        assertEquals(30, MobScaling.attackDamage(3, false, true, 200), EPS);
        assertEquals(45, MobScaling.attackDamage(3, false, true, 300), EPS);
        assertEquals(75, MobScaling.attackDamage(3, false, true, 500), EPS);
        // Mutation: attackDamage's vanilla factor -> 1 reddens every row (3, 6, 9, 15).
    }

    @Test
    void vanillaPassiveAttackIsFiveTimesAndIgnoresTheGs() {
        // M21's neutral-outside-Enemy row: a wolf or an iron golem is x5 damage with no GS. Base 4 (a wolf).
        assertEquals(20, MobScaling.attackDamage(4, false, false, 100), EPS);
        assertEquals(20, MobScaling.attackDamage(4, false, false, 500), EPS,
                "a passive mob takes NO gear score on its damage either (M7, M21)");
    }

    @Test
    void customHostileAttackTakesOnlyTheGs() {
        // M4, and the seat's reading of M17 (F15): the Knell's melee is its base's attribute x GS/100,
        // with no vanilla 5x. At GS 100 it is unchanged; at GS 200 it doubles. Base 8 is illustrative, not
        // a measured wither-skeleton attribute.
        assertEquals(8, MobScaling.attackDamage(8, true, true, 100), EPS,
                "no vanilla 5x on a custom mob (M4). Mutation: ignore isCustom -> 40 -> reddens");
        assertEquals(16, MobScaling.attackDamage(8, true, true, 200), EPS, "the GS still applies (M4)");
    }

    @Test
    void customPassiveAttackIsUnscaled() {
        assertEquals(6, MobScaling.attackDamage(6, true, false, 500), EPS);
    }

    @Test
    void aZeroAttackStaysZero() {
        // F4's pufferfish: a mob with no ATTACK_DAMAGE attribute seeds 0, and scaling cannot invent damage.
        assertEquals(0, MobScaling.attackDamage(0, false, true, 500), EPS);
    }

    @Test
    void theAttackGuardMatchesTheHealthGuard() {
        assertThrows(IllegalArgumentException.class, () -> MobScaling.attackDamage(3, false, true, 0));
        assertThrows(IllegalArgumentException.class, () -> MobScaling.attackDamage(3, false, true, 501));
    }
}
