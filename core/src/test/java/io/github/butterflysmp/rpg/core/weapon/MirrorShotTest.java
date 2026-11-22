package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The seat's rulings B and C, 2026-11-22: no mirror arrow ever leaves as a retrievable or unpaid shot. */
class MirrorShotTest {

    @Test
    void aQuiverPaidShotLeavesNoPickup() {
        assertEquals(MirrorShot.Act.NO_PICKUP, MirrorShot.of(true, true), "the mirror spent by its own weapon");
        assertEquals(MirrorShot.Act.NO_PICKUP, MirrorShot.of(false, true), "a backstop shot: his arrow given back, a round spent");
    }

    @Test
    void aMirrorArrowSpentByABowThatPaysNothingIsCancelled() {
        assertEquals(MirrorShot.Act.CANCEL, MirrorShot.of(true, false), "a plain bow in the off hand drawing on the mirror");
    }

    @Test
    void anOrdinaryShotIsLeftToVanilla() {
        assertEquals(MirrorShot.Act.LEAVE, MirrorShot.of(false, false), "the Crossbow's own arrow: pickup as vanilla set it");
    }
}
