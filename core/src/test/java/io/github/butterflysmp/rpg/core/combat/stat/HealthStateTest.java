package io.github.butterflysmp.rpg.core.combat.stat;

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
}
