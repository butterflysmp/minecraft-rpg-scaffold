package io.github.butterflysmp.rpg.core.combat.stat;

import io.github.butterflysmp.rpg.core.combat.Crit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The max-change semantics are the hazard, not the steady state -- the Soaked cleanup lesson, here
 * for health. These tests pin the two rules as decisions: headroom on a max increase (equip is not
 * a free heal), clamp on a decrease (current never left above max), and the below-max case left
 * alone. Each test names the mutation it forces red.
 */
class HealthStateTest {

    private static final double EPS = 1e-9;

    @Test
    void raisingMaxIsHeadroomNotHealing() {
        var state = new HealthState(100, true);           // 100/100
        boolean changed = state.setMaxModifier("mainhand", 300);

        assertEquals(400, state.max(), EPS, "max rose to base + modifier");
        assertEquals(100, state.current(), EPS, "current UNCHANGED -- headroom, not a free heal (now 25%)");
        assertTrue(changed, "a new modifier changed the resolved max");
        // Mutation: raise current to the new max on equip -> current == 400 -> reddens (equip = free heal).
    }

    @Test
    void loweringMaxClampsCurrentToTheNewMax() {
        var state = new HealthState(100, true);
        state.setMaxModifier("mainhand", 300);            // 100/400
        state.heal(1000);                                 // 400/400 (capped at max)
        assertEquals(400, state.current(), EPS, "healed to the ceiling");

        boolean removed = state.clearMaxModifier("mainhand"); // 400/100 -> clamp

        assertEquals(100, state.max(), EPS, "max fell back to base");
        assertEquals(100, state.current(), EPS, "current CLAMPED to the new max, never above it");
        assertTrue(removed, "the modifier was actually removed");
        // Mutation: skip the clamp on remove -> current stays 400 > max 100 -> reddens.
    }

    @Test
    void loweringMaxLeavesCurrentAloneWhenAlreadyBelow() {
        var state = new HealthState(100, true);
        state.setMaxModifier("mainhand", 300);            // 100/400
        state.damage(50);                                 // 50/400

        state.clearMaxModifier("mainhand");               // 50/100 -- current already <= new max

        assertEquals(100, state.max(), EPS, "max fell to base");
        assertEquals(50, state.current(), EPS, "current unchanged -- it was already below the new max");
        // Mutation: clamp to max unconditionally (set current = max on remove) -> 100, not 50 -> reddens.
    }

    @Test
    void loweringAModifiersAmountAlsoClamps() {
        var state = new HealthState(100, true);
        state.setMaxModifier("mainhand", 300);            // max 400
        state.heal(1000);                                 // 400/400
        state.setMaxModifier("mainhand", 50);             // replace: max 150, current clamped

        assertEquals(150, state.max(), EPS, "amount lowered from 300 to 50 -> max 150");
        assertEquals(150, state.current(), EPS, "current clamped to the lower max");
        // Mutation: putModifier appends instead of replacing -> max 100+300+50=450 -> reddens.
    }

    @Test
    void damageReducesCurrentAndFloorsAtZero() {
        var state = new HealthState(100, true);
        state.damage(30);
        assertEquals(70, state.current(), EPS, "damage came off current");
        state.damage(1000);
        assertEquals(0, state.current(), EPS, "current floors at 0, never negative");
        // Mutation: drop the Math.max(0, ...) -> current goes negative -> reddens.
    }

    @Test
    void healCapsAtMax() {
        var state = new HealthState(100, true);
        state.damage(40);                                 // 60/100
        state.heal(1000);
        assertEquals(100, state.current(), EPS, "heal cannot exceed max");
        // Mutation: drop the Math.min(max, ...) -> current overshoots to 1060 -> reddens.
    }

    @Test
    void exactlyOneModifierPerSourceNeverN() {
        var state = new HealthState(100, true);
        state.setMaxModifier("mainhand", 300);
        state.setMaxModifier("mainhand", 300);
        state.setMaxModifier("mainhand", 300);

        assertEquals(1, state.maxModifierCount(), "one modifier for the source after three applies, not three");
        assertEquals(400, state.max(), EPS, "and the max reflects one 300, not three");
        // Mutation: putModifier appends per call -> count 3, max 1000 -> reddens (the leak, generalized).
    }

    @Test
    void reapplyingTheSameAmountReportsNoChange() {
        var state = new HealthState(100, true);
        assertTrue(state.setMaxModifier("mainhand", 300), "first apply changed the max");
        assertFalse(state.setMaxModifier("mainhand", 300), "re-applying the same amount changed nothing");
        // Mutation: always return true -> a change fires every reconcile tick -> steady-state spam -> reddens.
    }

    @Test
    void attackDamageIsASecondStatIndependentOfHealth() {
        var state = new HealthState(100, 6, false);        // mob: 100 HP, attack base 6
        assertEquals(6, state.attackValue(), EPS, "attack resolves base + modifiers, like max");

        state.setAttackModifier("MAIN_HAND", 4);           // e.g. a wielded weapon
        assertEquals(10, state.attackValue(), EPS, "an attack modifier adds, keyed by source");

        // Health and attack do not bleed into each other.
        state.damage(50);                                  // 50/100
        assertEquals(10, state.attackValue(), EPS, "damaging HP leaves attack untouched");
        assertEquals(50, state.current(), EPS, "and setting an attack modifier never moved current");
        assertEquals(100, state.max(), EPS, "nor max");

        assertTrue(state.clearAttackModifier("MAIN_HAND"), "the attack modifier removes by its source");
        assertEquals(6, state.attackValue(), EPS, "back to base attack");
        // Mutation: back attackValue() by the max Stat (or share one Stat) -> attack tracks HP changes -> reddens.
    }

    // --- Class-typed damage: the fourth Stat ---------------------------------------------------

    /**
     * The class-damage stat bases at 0.0, not 1.0. It is a SUMMAND -- it is ADDED to a damage
     * number -- where attack speed is a DIVISOR that must base at neutral. Copying attack speed's
     * base here would silently add 1 damage to every hit in the game.
     */
    @Test
    void classDamageBasesAtZeroNotOne() {
        var state = new HealthState(100, true);
        assertEquals(0.0, state.classDamageValue(), EPS,
                "a combatant with no class gear adds nothing");
        // Mutation: base it at AttackSpeed.BASE -> 1.0 -> reddens.
    }

    /** Modifiers add, and two sources sum -- the equipped-gear case. */
    @Test
    void classDamageModifiersSumAcrossSources() {
        var state = new HealthState(100, true);
        state.setClassDamageModifier("OFF_HAND", 5.0);
        state.setClassDamageModifier("HEAD", 2.0);

        assertEquals(7.0, state.classDamageValue(), EPS);
        assertEquals(2, state.classDamageModifierCount());
    }

    /** Clean removal, the Soaked property: unequip and the base is exactly restored, no leak. */
    @Test
    void clearingAClassDamageModifierRestoresTheBaseExactly() {
        var state = new HealthState(100, true);
        state.setClassDamageModifier("OFF_HAND", 5.0);
        assertTrue(state.clearClassDamageModifier("OFF_HAND"), "a real removal reports true");

        assertEquals(0.0, state.classDamageValue(), EPS, "exactly back to base, not 5 and not -5");
        assertEquals(0, state.classDamageModifierCount(), "and no modifier left parked on the stat");
        assertFalse(state.clearClassDamageModifier("OFF_HAND"), "removing it twice is not a change");
    }

    /**
     * Re-applying the same source REPLACES rather than appending -- exactly one modifier per source,
     * never N. This is the leak the reconcile loop would otherwise create every 5 ticks.
     */
    @Test
    void reapplyingAClassDamageSourceReplacesItRatherThanStacking() {
        var state = new HealthState(100, true);
        state.setClassDamageModifier("OFF_HAND", 5.0);
        state.setClassDamageModifier("OFF_HAND", 5.0);
        state.setClassDamageModifier("OFF_HAND", 8.0);

        assertEquals(8.0, state.classDamageValue(), EPS, "the latest amount, not 5+5+8");
        assertEquals(1, state.classDamageModifierCount(), "one source, one modifier");
    }

    /**
     * The four stats are independent: class damage does not leak into attack damage, which is the
     * whole reason it is a separate Stat rather than class-gated ATTACK_DAMAGE modifiers. Folding it
     * in there would double-count against the weapon's own MAIN_HAND source.
     */
    @Test
    void classDamageIsSeparateFromAttackDamage() {
        var state = new HealthState(100, 8.0, true);
        state.setClassDamageModifier("OFF_HAND", 5.0);

        assertEquals(8.0, state.attackValue(), EPS, "the weapon's inherent damage is untouched");
        assertEquals(5.0, state.classDamageValue(), EPS, "and the gear bonus stands apart from it");
        assertEquals(100, state.max(), EPS);
        assertEquals(1.0, state.attackSpeedValue(), EPS);
        // Mutation: route setClassDamageModifier at the attack stat -> attackValue 13 -> reddens.
    }

    // --- Crit: two stats whose BASE depends on faction ---

    @Test
    void aPlayerBasesAtTheStartingCritChanceAndBonus() {
        var player = new HealthState(100, true);

        assertEquals(Crit.BASE_CHANCE, player.critChanceValue(), EPS, "15% to start");
        assertEquals(Crit.BASE_DAMAGE, player.critDamageValue(), EPS, "+100%, i.e. a 2.0x crit");
        // Mutation: base both at 0 -> a player never crits and the whole feature is invisible at
        // boot while every unit test about the multiplier still passes -> reddens.
    }

    /**
     * A MOB bases at zero, and this is the whole of "a mob's hits never crit".
     *
     * There is no second check at the roll site: Crit.crits compares roll &lt; 0, false for every roll
     * a half-open source can produce. Basing the stat on faction rather than gating the roll is what
     * keeps a future stat screen from reading 0.15 on a zombie while something elsewhere silently
     * contradicted it.
     */
    @Test
    void aMobBasesAtZeroWhichIsWhyItNeverCrits() {
        var mob = new HealthState(20, false);

        assertEquals(0.0, mob.critChanceValue(), EPS);
        assertEquals(0.0, mob.critDamageValue(), EPS);
        assertFalse(Crit.crits(mob.critChanceValue(), 0.0), "not even on the lowest possible roll");
        // Mutation: base the mob at Crit.BASE_CHANCE like the player -> every zombie crits 15% of the
        // time on a stat nobody meant to give it -> reddens.
    }

    @Test
    void gearStacksAdditivelyOnBothCritStatsLikeEveryOtherStat() {
        var player = new HealthState(100, true);

        player.setCritChanceModifier("MAIN_HAND", 0.35);
        player.setCritDamageModifier("OFF_HAND", 0.5);

        assertEquals(0.5, player.critChanceValue(), EPS, "0.15 + 0.35");
        assertEquals(1.5, player.critDamageValue(), EPS, "1.0 + 0.5 -> a 2.5x crit");
        assertEquals(2.5, Crit.multiplier(player.critChanceValue(), player.critDamageValue(), 0.0), EPS);
        // Mutation: make either stat replace rather than sum -> a boost REPLACES the base instead of
        // adding to it, so a +0.35 chance item would LOWER the rate from 0.15 to 0.35... or rather
        // set it flat, breaking the "stacks like the others" promise -> reddens.
    }

    @Test
    void droppingACritItemReturnsBothStatsToBase() {
        var player = new HealthState(100, true);
        player.setCritChanceModifier("MAIN_HAND", 0.35);
        player.setCritDamageModifier("MAIN_HAND", 1.0);

        player.clearCritChanceModifier("MAIN_HAND");
        player.clearCritDamageModifier("MAIN_HAND");

        assertEquals(Crit.BASE_CHANCE, player.critChanceValue(), EPS);
        assertEquals(Crit.BASE_DAMAGE, player.critDamageValue(), EPS);
        assertEquals(0, player.critChanceModifierCount());
        // Mutation: leave the source behind on clear -> the boost outlives the item and the boot's
        // "drop it and the rate returns" row silently passes forever -> reddens.
    }

    /** The stat is NOT clamped; Crit.chance clamps at the point of use. Stated so it stays that way. */
    @Test
    void theStatReportsWhatGearGrantsAndTheCapIsAppliedAtUse() {
        var player = new HealthState(100, true);
        player.setCritChanceModifier("MAIN_HAND", 2.0);

        assertEquals(2.15, player.critChanceValue(), EPS, "the raw grant, uncapped");
        assertEquals(1.0, Crit.chance(player.critChanceValue()), EPS, "capped where it is used");
        // Mutation: clamp inside the Stat -> the cap is stated in two places and a future stat screen
        // showing "215% crit" vs "100%" depends on which one it asked -> reddens on the first row.
    }
}
