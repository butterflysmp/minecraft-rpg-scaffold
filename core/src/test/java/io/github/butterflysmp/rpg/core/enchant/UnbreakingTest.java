package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Unbreaking curve, which is the whole of the enchant's mechanical effect.
 *
 * Every case here is a guard whose absence is a real, shippable bug: an unenchanted weapon that
 * stops wearing, a corrupt level that makes a weapon indestructible, a boundary off by one point so
 * III consumes at the wrong rate, or a level past the maximum that approaches never wearing at all.
 * Redden by flipping the comparison, dropping either guard, or moving the {@code +1}.
 *
 * The boundary doubles are fed directly rather than through a seeded random, the same way
 * {@code DamagePopupManagerTest} pins {@code jitter} -- the whole reason the draw was left at the
 * call site is so these numbers can be exact.
 */
class UnbreakingTest {

    private static final double EPS = 1e-9;

    @Test
    void levelZeroAlwaysConsumesSoAnUnenchantedWeaponWearsExactlyAsBefore() {
        // The identity case, and by far the most common one: every weapon in the game that is not
        // enchanted runs through this branch on every basic attack. "Nearly always consumes" would
        // be a silent, game-wide durability buff.
        assertTrue(Unbreaking.consumes(0, 0.0));
        assertTrue(Unbreaking.consumes(0, 0.5));
        assertTrue(Unbreaking.consumes(0, 0.9999999));
        // Mutation: flip `<` to `>` -> consumes(0, 0.5) is still true via the guard, but
        // consumes(3, 0.0) below turns false -> reddens there.
        // Mutation: return false from the level<=0 guard -> an unenchanted weapon NEVER wears -> reddens.
    }

    @Test
    void aNegativeLevelIsTreatedAsUnenchantedRatherThanAsFreeDurability() {
        // A corrupt or hand-edited level must still wear. Without a guard the threshold is
        // 1.0/(-2+1) == -1.0, and `roll < -1.0` is false for EVERY roll in [0,1) -- so the weapon
        // becomes INDESTRUCTIBLE, permanently and silently.
        assertTrue(Unbreaking.consumes(-1, 0.0), "a corrupt level must still wear");
        assertTrue(Unbreaking.consumes(-2, 0.0));
        assertTrue(Unbreaking.consumes(-2, 0.9999999));
        assertTrue(Unbreaking.consumes(Integer.MIN_VALUE, 0.5));

        // Asserted on consumeChance DIRECTLY, and this is the point of the assertion rather than a
        // restatement of the ones above. There are TWO `level <= 0` guards -- one in consumes, one
        // in consumeChance -- and the calls above are satisfied by EITHER of them, so deleting
        // either one on its own leaves every line above green. A mutation run proved exactly that:
        // removing the guard in consumes reddened nothing at all. This line is what pins
        // consumeChance's guard on its own; theRollBoundaryIsExactAtLevelZero pins the other.
        assertEquals(1.0, Unbreaking.consumeChance(-1), EPS, "a negative level is never a threshold");
        assertEquals(1.0, Unbreaking.consumeChance(-2), EPS);
        assertEquals(1.0, Unbreaking.consumeChance(Integer.MIN_VALUE), EPS);
        // Mutation: delete the `level <= 0` guard in consumeChance -> -1.0 -> reddens.
    }

    @Test
    void theRollBoundaryIsExactAtLevelZeroToo() {
        // The second of the two guards, pinned on its own. At level 0 the threshold is 1.0, and the
        // comparison is STRICT -- so `roll < 1.0` is false at exactly 1.0, and without the early
        // return in consumes an unenchanted weapon would skip that one roll. Unreachable from
        // ThreadLocalRandom.nextDouble(), which never returns 1.0, and pinned anyway: "always
        // consumes" is a promise the whole game's wear rate rests on, not an approximation.
        assertTrue(Unbreaking.consumes(0, 1.0), "level 0 consumes even at the excluded boundary");
        assertTrue(Unbreaking.consumes(-2, 1.0));
        // Mutation: delete the `level <= 0` guard in consumes -> 1.0 < 1.0 is false -> reddens.
    }

    @Test
    void levelThreeConsumesOneSwingInFourAtTheBoundary() {
        // The headline number the boot gate measures: III skips three uses in four. The boundary
        // point is the whole test -- 0.25 must SKIP, because the roll source is half-open [0,1) and
        // strict < is what makes the consuming set exactly one quarter wide.
        assertTrue(Unbreaking.consumes(3, 0.0));
        assertTrue(Unbreaking.consumes(3, 0.2499999));
        assertFalse(Unbreaking.consumes(3, 0.25), "0.25 is the first roll that skips");
        assertFalse(Unbreaking.consumes(3, 0.9999999));
        // Mutation: `<` -> `<=` -> 0.25 consumes -> reddens.
    }

    @Test
    void levelOneConsumesHalfTheTimeAtTheBoundary() {
        // The other end of the shipped range, and what boot gate step 4 compares III against. If I
        // and III cannot be told apart, the level is not reaching the curve at all.
        assertTrue(Unbreaking.consumes(1, 0.4999999));
        assertFalse(Unbreaking.consumes(1, 0.5), "0.5 is the first roll that skips at level I");
        // Mutation: `level + 1` -> `level` -> threshold 1.0 -> 0.5 consumes -> reddens.
    }

    @Test
    void aLevelPastTheMaximumIsClampedRatherThanApproachingUnbreakable() {
        // The same indestructibility failure from the other end. At level 99 the unclamped
        // threshold is 0.01: a weapon that wears once in a hundred swings, from an item nobody
        // could legitimately produce but a blob could carry.
        assertTrue(Unbreaking.consumes(99, 0.24), "clamped to III, so 0.24 still consumes");
        assertFalse(Unbreaking.consumes(99, 0.26));
        assertEquals(Unbreaking.consumeChance(3), Unbreaking.consumeChance(99), EPS,
                "past the maximum, the curve stops moving");
        assertEquals(Unbreaking.consumeChance(3), Unbreaking.consumeChance(Integer.MAX_VALUE), EPS);
        // Mutation: drop the Math.min(level, MAX_LEVEL) -> threshold 0.01 -> 0.24 skips -> reddens.
    }

    @Test
    void consumeChanceIsTheCurveTheTooltipAndTheSeamShare() {
        // The dev command prints this number back as a percentage, so the boot gate reads the
        // EXPECTED rate off the screen before swinging. If the printed number and the applied one
        // came from different expressions, the gate would be checking the wrong thing against
        // itself.
        assertEquals(1.0, Unbreaking.consumeChance(0), EPS, "unenchanted: always consumes");
        assertEquals(0.5, Unbreaking.consumeChance(1), EPS);
        assertEquals(1.0 / 3.0, Unbreaking.consumeChance(2), EPS);
        assertEquals(0.25, Unbreaking.consumeChance(3), EPS);
        // Mutation: 1.0/(level+1) -> 1.0 - level/4.0 -> level II gives 0.5 -> reddens.
    }

    @Test
    void aRollOutsideTheUnitIntervalFailsTowardsWearingRatherThanTowardsFree() {
        // Not reachable from ThreadLocalRandom.nextDouble(), which is [0,1). Pinned anyway so the
        // function is total: a malformed roll must cost durability, never grant it.
        assertTrue(Unbreaking.consumes(3, -0.1), "a negative roll wears");
        assertTrue(Unbreaking.consumes(3, Double.NEGATIVE_INFINITY));
        assertFalse(Unbreaking.consumes(3, 1.0));
        assertFalse(Unbreaking.consumes(3, Double.NaN), "NaN compares false, so it skips -- documented, not relied on");
    }

    @Test
    void theIdIsTheOneTheContentFileIsNamedAfter() {
        // The seam compares this id against the item's state and never consults the registry, so a
        // rename here silently stops every enchanted weapon from skipping wear while the tooltip
        // carries on rendering. Paired with EnchantLoaderTest, which asserts the yml resolves to
        // this same id -- together they make a rename impossible to do halfway.
        assertEquals("unbreaking", Unbreaking.ID);
    }
}
