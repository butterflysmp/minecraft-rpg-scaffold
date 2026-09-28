package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.From;
import io.github.butterflysmp.rpg.core.mob.MobDamagePricing.Resolved;
import io.github.butterflysmp.rpg.core.mob.VanillaDifficulty.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M24: MOB DAMAGE IGNORES DIFFICULTY. The same hit prices identically on EASY, NORMAL and HARD.
 *
 * <p>The forward models below copy vanilla's bytecode (the pinned {@code paper-26.1.2.jar}, read with
 * {@code javap}) operation for operation, in FLOAT as vanilla computes them:
 * <ul>
 *   <li>{@code Player.hurtServer}: when {@code scalesWithDifficulty()}, EASY is
 *       {@code Math.min(a / 2f + 1f, a)}, HARD is {@code a * 3f / 2f}, and NORMAL is unchanged;
 *   <li>{@code Guardian$GuardianAttackGoal.tick}: the beam is {@code 1f}, {@code + 2f} on HARD, and
 *       {@code + 2f} for an elder;
 *   <li>{@code AbstractArrow.setBaseDamageFromMob}: {@code v * 2f + triangle(id * 0.11, 0.57425)}.
 * </ul>
 *
 * <p><b>The tolerance is float precision, and it is named, not tuned.</b> Vanilla rounds each step to a
 * float, and the reversal runs in double, so an amount that is not a short binary fraction comes back to
 * within about 1e-7 relative. Halves and whole numbers come back exactly.
 */
class VanillaDifficultyTest {

    private static final double REL = 1e-6;
    private static final Difficulty[] PLAYABLE = {Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};
    private static final float[] AMOUNTS = {0.5f, 1f, 1.5f, 1.99f, 2f, 2.6f, 3f, 4f, 5f, 6f, 7.3f, 8f, 10.5f, 15f, 22f, 49.7f};

    /** Player.hurtServer's scaling, copied from the bytecode. */
    private static float vanillaScale(float a, Difficulty d) {
        return switch (d) {
            case EASY -> Math.min(a / 2.0f + 1.0f, a);
            case HARD -> a * 3.0f / 2.0f;
            case NORMAL, PEACEFUL -> a;
        };
    }

    private static void assertClose(double expected, double actual, String msg) {
        assertEquals(expected, actual, Math.max(1e-12, Math.abs(expected) * REL), msg);
    }

    @Test
    void theSameHitPricesIdenticallyOnEasyNormalAndHard() {
        Resolved gs300 = new Resolved(From.DIRECT, false, true, 300);
        for (float a : AMOUNTS) {
            double expected = MobDamagePricing.price(a, gs300);    // the hit as if difficulty did not exist
            for (Difficulty d : PLAYABLE) {
                double eventAmount = vanillaScale(a, d);
                double priced = MobDamagePricing.price(
                        VanillaDifficulty.undifficulted(eventAmount, d, true, false), gs300);
                assertClose(expected, priced, "amount " + a + " on " + d
                        + ". Mutation: skip the reversal -> HARD prices x1.5 -> reddens");
            }
        }
    }

    @Test
    void wholeAndHalfAmountsComeBackExactly() {
        // A skeleton's arrow, a shulker bullet (4), a small fireball (5), a wither skull (8).
        for (float a : new float[] {3f, 4f, 5f, 7f, 8f}) {
            for (Difficulty d : PLAYABLE) {
                assertEquals(a, VanillaDifficulty.undifficulted(vanillaScale(a, d), d, true, false), 0.0,
                        a + " on " + d);
            }
        }
    }

    @Test
    void easysKneeAtTwoIsInvertibleOnBothSides() {
        // EASY is min(a/2 + 1, a): the identity below 2 and a/2 + 1 from 2 up. Both halves are
        // increasing and meet at 2, so every event amount has exactly one source amount.
        assertEquals(1.5, VanillaDifficulty.undifficulted(1.5, Difficulty.EASY, true, false), 0.0, "below the knee");
        assertEquals(2.0, VanillaDifficulty.undifficulted(2.0, Difficulty.EASY, true, false), 0.0, "at the knee");
        assertEquals(6.0, VanillaDifficulty.undifficulted(4.0, Difficulty.EASY, true, false), 0.0, "above: 6/2 + 1 = 4");
    }

    @Test
    void aSourceThatDidNotScaleIsLeftAlone() {
        // scalesWithDifficulty() false: vanilla never touched the amount, so reversing it would be the bug.
        for (Difficulty d : PLAYABLE) {
            assertEquals(4.0, VanillaDifficulty.undifficulted(4.0, d, false, false), 0.0, d.name());
        }
    }

    @Test
    void theGuardianBeamLosesItsHardBonus() {
        // A guardian's beam is 1 on every difficulty after M24; an elder's is 3 (its own + 2, not HARD's).
        for (boolean elder : new boolean[] {false, true}) {
            float base = elder ? 3.0f : 1.0f;
            for (Difficulty d : PLAYABLE) {
                float beam = base + (d == Difficulty.HARD ? 2.0f : 0.0f);
                assertEquals(base, VanillaDifficulty.undifficulted(vanillaScale(beam, d), d, true, true), 0.0,
                        (elder ? "elder " : "") + "guardian on " + d
                                + ". Mutation: keep the HARD bonus -> 3 / 5 on HARD -> reddens");
            }
        }
    }

    @Test
    void aMobArrowsBaseDamageLosesItsDifficultyShift() {
        // setBaseDamageFromMob(1.6): 3.2 + triangle(id * 0.11, 0.57425). The triangle's sample is the same
        // draw whatever the difficulty, so after the shift is removed the base is identical on all three.
        double sample = 0.37;                               // one fixed draw of (r1 - r2), in [-1, 1]
        double withoutDifficulty = 1.6f * 2.0f + 0.57425 * sample;
        for (Difficulty d : PLAYABLE) {
            double vanillaBase = 1.6f * 2.0f + (d.id() * 0.11 + 0.57425 * sample);
            assertClose(withoutDifficulty, VanillaDifficulty.mobArrowBaseDamage(vanillaBase, d),
                    d.name() + ". Mutation: no shift -> +0.11 per level -> reddens");
        }
    }

    @Test
    void theIdsAreVanillas() {
        // Difficulty's static initialiser in the pinned server jar: PEACEFUL 0, EASY 1, NORMAL 2, HARD 3.
        assertEquals(0, Difficulty.PEACEFUL.id());
        assertEquals(1, Difficulty.EASY.id());
        assertEquals(2, Difficulty.NORMAL.id());
        assertEquals(3, Difficulty.HARD.id());
    }
}
