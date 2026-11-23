package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.weapon.VanillaShot.Consumable;
import io.github.butterflysmp.rpg.core.weapon.VanillaShot.Plan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WHAT A VANILLA-DRIVEN BOW OR CROSSBOW SHOT DOES (the seat's rulings 7 to 9 of 2026-11-22, with B and C of the same day).
 * Vanilla has already taken one projectile when the shot event fires (READ, BowItem.releaseUsing), so every answer is
 * about what to give back and what to spend.
 */
class VanillaShotTest {

    // ------------------------------------------------------------------ the Bow (a quiver), main hand

    /** Ruling 7: the mirror paid; a round spent, nothing given back (the slot is re-drawn), the arrow unretrievable. */
    @Test
    void aMirrorShotSpendsARoundAndRefundsNothing() {
        Plan plan = VanillaShot.of(false, true, 8, Consumable.MIRROR);
        assertEquals(new Plan(false, false, true, true, false), plan);
    }

    /** Ruling 8: his own plain arrow is given back and the round spent; the arrow left cannot be picked up (B). */
    @Test
    void aPlainArrowOfHisOwnIsGivenBackAndTheRoundSpent() {
        assertEquals(new Plan(false, true, true, true, false), VanillaShot.of(false, true, 3, Consumable.PLAIN_ARROW));
    }

    /** Ruling 8 (SEAT PLACEHOLDER): tipped or spectral: no refund, the round still spent, vanilla's effect left alone. */
    @Test
    void aTippedOrSpectralArrowIsNotGivenBackButTheRoundIsSpent() {
        assertEquals(new Plan(false, false, true, true, false), VanillaShot.of(false, true, 3, Consumable.OTHER));
    }

    /** Ruling 8: an empty quiver cancels, gives back what vanilla took, and sends the empty notice. */
    @Test
    void anEmptyQuiverCancelsRefundsAndNotifies() {
        assertEquals(new Plan(true, true, false, false, true), VanillaShot.of(false, true, 0, Consumable.PLAIN_ARROW));
        assertEquals(new Plan(true, true, false, false, true), VanillaShot.of(false, true, 0, Consumable.OTHER));
        // a stale mirror at 0 rounds (the slot shows the star at 0): nothing to give back, the redraw puts the star
        assertEquals(new Plan(true, false, false, false, true), VanillaShot.of(false, true, 0, Consumable.MIRROR));
    }

    // ------------------------------------------------------------------ the Crossbow (no quiver), main hand

    /** The Crossbow shoots his own arrows: vanilla's shot, untouched, pickup as vanilla set it. */
    @Test
    void theCrossbowIsLeftToVanilla() {
        assertEquals(new Plan(false, false, false, false, false), VanillaShot.of(false, false, 0, Consumable.PLAIN_ARROW));
        assertEquals(new Plan(false, false, false, false, false), VanillaShot.of(false, false, 0, Consumable.OTHER));
    }

    /** A crossbow that somehow took the mirror (it loads from the same order) is cancelled, never left as a free shot. */
    @Test
    void aCrossbowShotOnTheMirrorIsCancelled() {
        Plan plan = VanillaShot.of(false, false, 0, Consumable.MIRROR);
        assertTrue(plan.cancel());
        assertFalse(plan.refund());
        assertFalse(plan.spendRound());
    }

    // ------------------------------------------------------------------ the off hand (ruling C)

    @Test
    void anOffHandShotIsCancelledAndWhateverVanillaTookIsGivenBack() {
        for (boolean quiver : new boolean[] {true, false}) {
            assertEquals(new Plan(true, true, false, false, false), VanillaShot.of(true, quiver, 5, Consumable.PLAIN_ARROW));
            assertEquals(new Plan(true, true, false, false, false), VanillaShot.of(true, quiver, 5, Consumable.OTHER));
            assertEquals(new Plan(true, false, false, false, false), VanillaShot.of(true, quiver, 5, Consumable.MIRROR));
        }
    }

    // ------------------------------------------------------------------ a broken weapon (ruling I, Ben's)

    /** A broken Bow or Crossbow refuses: cancelled, what vanilla took given back, nothing spent, never left to vanilla. */
    @Test
    void aBrokenWeaponRefusesAndGivesBackWhatVanillaTook() {
        for (boolean quiver : new boolean[] {true, false}) {
            assertEquals(new Plan(true, true, false, false, false), VanillaShot.of(true, false, quiver, 5, Consumable.PLAIN_ARROW));
            assertEquals(new Plan(true, true, false, false, false), VanillaShot.of(true, false, quiver, 5, Consumable.OTHER));
            assertEquals(new Plan(true, false, false, false, false), VanillaShot.of(true, false, quiver, 5, Consumable.MIRROR));
        }
        // CONTROL: the same shot, not broken, leaves the bow and spends a round
        assertEquals(new Plan(false, true, true, true, false), VanillaShot.of(false, false, true, 5, Consumable.PLAIN_ARROW));
        assertEquals(VanillaShot.of(false, true, 5, Consumable.OTHER), VanillaShot.of(false, false, true, 5, Consumable.OTHER));
    }

    // ------------------------------------------------------------------ the force

    /** READ: a bow's force is its draw power, 0.1 to 1 (BowItem.getPowerForTime); a crossbow's is the constant 1. */
    @Test
    void theChargeTermIsTheForceClampedToOne() {
        assertEquals(1.0, VanillaShot.chargeScale(1.0), 0.0);
        assertEquals(0.35, VanillaShot.chargeScale(0.35), 0.0);
        assertEquals(1.0, VanillaShot.chargeScale(3.0), 0.0, "never above the authored figure");
        assertEquals(0.0, VanillaShot.chargeScale(-1.0), 0.0);
        assertEquals(0.0, VanillaShot.chargeScale(Double.NaN), 0.0);
    }

    // ------------------------------------------------------------------ THE ARITHMETIC (ruling 8, as ruling B amends it)

    /**
     * EIGHT SHOTS ON THE MIRROR, THEN EIGHT ON HIS OWN ARROWS, THEN AN EMPTY QUIVER, THEN A RELOAD. A ROUND SPENT IS GONE:
     * every quiver-paid arrow is unretrievable (ruling B withdraws "eight shots, eight pickups"), so the only arrows that
     * ever leave his inventory are the ones a reload takes.
     */
    @Test
    void eightShotsLeaveHisRealArrowsWhereOnlyAReloadMovesThem() {
        Ledger l = new Ledger(8, 0);
        for (int i = 0; i < 8; i++) l.shoot(Consumable.MIRROR);           // the mirror shows 8..1, vanilla draws on it
        assertEquals(0, l.quiver);
        assertEquals(0, l.real, "no arrow of his was ever taken");
        assertEquals(0, l.pickups, "B: every one of the eight is unretrievable");

        l.real = 16;                                                      // 16 given, in a slot vanilla picks first
        l.quiver = 8;
        for (int i = 0; i < 8; i++) l.shoot(Consumable.PLAIN_ARROW);
        assertEquals(16, l.real, "each plain arrow taken was given back");
        assertEquals(0, l.quiver);
        assertEquals(0, l.pickups);

        l.shoot(Consumable.PLAIN_ARROW);                                  // the ninth: the quiver is empty
        assertEquals(16, l.real, "cancelled and given back");
        assertEquals(1, l.emptyNotices);
        assertEquals(16, l.shotsFired, "sixteen shots left the bow; the ninth of the second set did not");

        l.reload(8);
        assertEquals(8, l.real, "a reload of eight is the only thing that took his arrows");
        assertEquals(8, l.quiver);
    }

    /** The ledger the test drives: what vanilla takes, then what the plan gives back and spends. */
    private static final class Ledger {
        int quiver; int real; int pickups; int emptyNotices; int shotsFired;
        Ledger(int quiver, int real) { this.quiver = quiver; this.real = real; }

        void shoot(Consumable taken) {
            if (taken != Consumable.MIRROR) real--;                       // vanilla split one of his
            Plan plan = VanillaShot.of(false, true, quiver, taken);
            if (plan.refund()) real++;
            if (plan.spendRound()) quiver--;
            if (plan.emptyNotice()) emptyNotices++;
            if (!plan.cancel()) {
                shotsFired++;
                if (!plan.quiverPaid()) pickups++;                         // only an unpaid arrow can be picked up
            }
        }

        void reload(int size) {
            int missing = size - quiver;
            int taken = Math.min(missing, real);
            real -= taken;
            quiver += taken;
        }
    }
}
