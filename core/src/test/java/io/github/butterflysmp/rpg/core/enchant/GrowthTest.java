package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.combat.stat.HealthState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Growth: flat points of MAX health, and the two transition rules it inherits.
 *
 * The headline is {@link #twoSourcesInOneSlotMustNotShareAKeyOrOneSILENTLYERASESTheOther} -- the
 * reason Growth's reconciler keys are namespaced and every other scanner's are not. It is a property
 * of {@code Stat}, so it is pinned here in core rather than left to a Bukkit scan no unit test can
 * construct.
 *
 * The other three pin what Growth does to a player who equips it, removes it, and removes it while
 * hurt. Those rules are HealthState's and predate this enchant; they are asserted here because
 * Growth is the first thing that makes them reachable in normal play.
 *
 * Each test names the mutation it forces red.
 */
class GrowthTest {

    private static final double EPS = 1e-9;

    // --- The arithmetic ---------------------------------------------------------------------------

    @Test
    void aPieceContributesItsBonusAndNothingElse() {
        assertEquals(10.0, Growth.contribution(10), EPS);
        assertEquals(30.0, Growth.contribution(30), EPS);
        assertFalse(Growth.boosts(Growth.NONE), "zero declares no bonus");
        assertTrue(Growth.boosts(0.5), "anything above zero does");
        // Mutation: make boosts() use >= -> an unenchanted piece claims a bonus and the scan writes
        // a zero-valued source instead of leaving it absent -> reddens.
    }

    // --- THE HEADLINE: why the keys are namespaced ------------------------------------------------

    @Test
    void twoSourcesInOneSlotMustNotShareAKeyOrOneSILENTLYERASESTheOther() {
        // Stat.putModifier is put-or-REPLACE. HealthModifierItems keys by a bare slot name and walks
        // ALL slots including the armor ones, so a health_boost_TEMP in the chest slot and a Growth
        // chestplate both want "CHEST". Merged into one map, the second write would erase the first
        // and the player would silently get whichever the merge happened to write last.
        var collided = new HealthState(100, true);
        collided.setMaxModifier("CHEST", 300);            // the fixture item
        collided.setMaxModifier("CHEST", 30);             // Growth, same key
        assertEquals(130, collided.max(), EPS,
                "same key REPLACES -- the fixture's 300 is gone and nothing said so");

        // Namespaced, both survive and sum, which is the only correct reading: the player really is
        // wearing two things that each raise max health.
        var namespaced = new HealthState(100, true);
        namespaced.setMaxModifier("CHEST", 300);          // the fixture item
        namespaced.setMaxModifier("growth:CHEST", 30);    // Growth, disjoint key
        assertEquals(430, namespaced.max(), EPS, "both sources contribute");
        // Mutation: drop the "growth:" prefix from GrowthModifierItems.SOURCE_PREFIX -> the live
        // scan produces the first case; this test is what says why that is wrong.
    }

    // --- The transition rules Growth makes reachable ----------------------------------------------

    @Test
    void equippingGrowthIsHeadroomAndNeverAHeal() {
        var state = new HealthState(100, true);                 // 100/100
        state.setMaxModifier("growth:CHEST", Growth.contribution(30));

        assertEquals(130, state.max(), EPS);
        assertEquals(100, state.current(), EPS,
                "current UNCHANGED -- the player now looks hurt and has gained nothing to spend");
        // Mutation: heal to the new max on equip -> equip/unequip cycling becomes a free heal
        // -> reddens.
    }

    @Test
    void removingGrowthAtFullHealthCLAMPSCurrentDown() {
        // THE ONE PLAYERS WILL NOTICE, and it is correct rather than a bug. Growth is the only
        // enchant in the game that can leave someone with fewer hearts than a moment ago.
        var state = new HealthState(100, true);
        state.setMaxModifier("growth:CHEST", 30);               // 100/130
        state.heal(30);                                          // 130/130
        assertEquals(130, state.current(), EPS, "topped up first, so the clamp has something to do");

        state.clearMaxModifier("growth:CHEST");
        assertEquals(100, state.max(), EPS);
        assertEquals(100, state.current(), EPS, "current clamped to the new max, not left above it");
        // Mutation: skip clampCurrentToMax on removal -> current stays 130 above a max of 100
        // -> reddens.
    }

    @Test
    void removingGrowthWhileAlreadyHurtLeavesCurrentAlone() {
        // The other branch of the same rule: the clamp only pulls DOWN, so a player below the new
        // max loses nothing. Without this, the test above would pass on an implementation that
        // simply set current = max on every removal.
        var state = new HealthState(100, true);
        state.setMaxModifier("growth:CHEST", 30);               // 100/130
        state.damage(60);                                        // 40/130

        state.clearMaxModifier("growth:CHEST");
        assertEquals(100, state.max(), EPS);
        assertEquals(40, state.current(), EPS, "already below the new max, so untouched");
        // Mutation: set current = max on removal -> 100 instead of 40, a free heal for taking armor
        // OFF -> reddens.
    }
}
