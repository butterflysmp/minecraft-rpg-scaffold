package io.github.butterflysmp.rpg.core.fight;

import io.github.butterflysmp.rpg.core.fight.DragonAttacks.Attack;
import io.github.butterflysmp.rpg.core.fight.DragonAttacks.Box;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * F9b, section 4: WHICH OF THE DRAGON'S ATTACKS A HIT WAS, and what each costs (Ben: "Yes those numbers are fine to
 * start with": bite 80, wing 40, breath 30 a second).
 *
 * <p>READ, the pinned server jar, {@code EnderDragon.aiStep}: each tick the dragon is not flinching it pushes (and, not
 * sitting, hurts for 5) everything in each WING's box inflated by (4, 2, 4) and moved down 2, then hurts for 10
 * everything in the HEAD's and the NECK's boxes inflated by 1. Both hurts are a mob attack from the dragon, so Bukkit
 * cannot tell them apart by cause; the boxes can.
 */
class DragonAttacksTest {

    // A dragon facing +x at the origin: boxes as the server sizes them (head 1x1, neck 3x3, wings 4x2).
    private static final Box HEAD = Box.centred(10.0, 20.0, 0.0, 1.0, 1.0);
    private static final Box NECK = Box.centred(7.5, 20.0, 0.0, 3.0, 3.0);
    private static final Box WING_L = Box.centred(0.0, 20.0, 6.0, 4.0, 2.0);
    private static final Box WING_R = Box.centred(0.0, 20.0, -6.0, 4.0, 2.0);

    /** A player's box (0.6 wide, 1.8 tall) standing with his feet at (x, y, z). */
    private static Box player(double x, double y, double z) {
        return new Box(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
    }

    private static Optional<Attack> classify(Box victim) {
        return DragonAttacks.classify(victim, HEAD, NECK, List.of(WING_L, WING_R));
    }

    @Test
    void aPlayerAtTheHeadIsBitten() {
        assertEquals(Optional.of(Attack.BITE), classify(player(10.0, 19.0, 0.0)));
    }

    @Test
    void theHeadsBoxIsInflatedByOneSoAPlayerJustOutsideTheHeadIsStillBitten() {
        // The head spans x 9.5..10.5; inflated by 1 it reaches 11.5. A player whose box starts at 11.2 is inside it.
        assertEquals(Optional.of(Attack.BITE), classify(player(11.5, 19.5, 0.0)));
        // ...and one whose box starts at 11.7 is not (nor in any other box).
        assertEquals(Optional.empty(), classify(player(12.0, 19.5, 0.0)));
    }

    @Test
    void aPlayerAtTheNeckIsBittenToo() {
        assertEquals(Optional.of(Attack.BITE), classify(player(7.5, 18.0, 0.0)));
    }

    @Test
    void aPlayerUnderAWingIsStruckByTheWing() {
        // The wing at y 20..22, inflated 2 up and down then moved down 2: y 16..22. Feet at 17 are inside.
        assertEquals(Optional.of(Attack.WING), classify(player(0.0, 17.0, 6.0)));
        assertEquals(Optional.of(Attack.WING), classify(player(0.0, 17.0, -6.0)), "either wing");
    }

    @Test
    void theWingsBoxIsMovedDownSoAPlayerAboveTheWingIsNotStruck() {
        // Moved down 2 after inflating: the top is 22, so feet at 22.5 are above it.
        assertEquals(Optional.empty(), classify(player(0.0, 22.5, 6.0)));
        // Inflated 4 sideways: the wing's x 2.0 edge reaches 6.0, so a player at x 6.2 (box from 5.9) is struck.
        assertEquals(Optional.of(Attack.WING), classify(player(6.2, 20.0, 6.0)));
    }

    @Test
    void aPlayerInBothABiteBoxAndAWingBoxIsBitten() {
        Box wingByTheHead = Box.centred(10.0, 21.0, 0.0, 4.0, 2.0);
        assertEquals(Optional.of(Attack.BITE), DragonAttacks.classify(player(10.0, 19.0, 0.0), HEAD, NECK,
                List.of(wingByTheHead)), "the bigger of the two; INFERRED to be rare (the head is far from the wings)");
    }

    @Test
    void aPlayerNearNoPartIsNoAttack() {
        assertEquals(Optional.empty(), classify(player(30.0, 0.0, 30.0)));
    }

    @Test
    void theBreathCostsItsPerSecondFigureSpreadOverTheCloudsInterval() {
        // READ: the breath cloud's reapplication delay is the AreaEffectCloud default, 20 ticks: 30 a hit is 30 a second.
        assertEquals(30.0, DragonAttacks.breathPerApplication(30.0, 20), 0.0);
        assertEquals(15.0, DragonAttacks.breathPerApplication(30.0, 10), 0.0, "a faster cloud, a smaller hit");
        assertEquals(45.0, DragonAttacks.breathPerApplication(30.0, 30), 0.0);
    }

    @Test
    void aNonPositiveIntervalIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> DragonAttacks.breathPerApplication(30.0, 0));
    }

    @Test
    void eachAttackNamesItselfInTheLog() {
        assertEquals("bite", Attack.BITE.logName());
        assertEquals("wing", Attack.WING.logName());
        assertEquals("breath", Attack.BREATH.logName());
    }

    @Test
    void theContentNumbersAreHeldAsWrittenAndPickedByAttack() {
        DragonAttacks a = new DragonAttacks(80.0, 40.0, 30.0);
        assertEquals(80.0, a.base(Attack.BITE), 0.0);
        assertEquals(40.0, a.base(Attack.WING), 0.0);
        assertEquals(30.0, a.base(Attack.BREATH), 0.0, "the breath's base is its figure a second");
    }

    @Test
    void negativeOrMissingNumbersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new DragonAttacks(-1.0, 40.0, 30.0));
        assertThrows(IllegalArgumentException.class, () -> new DragonAttacks(80.0, Double.NaN, 30.0));
        assertThrows(IllegalArgumentException.class, () -> new DragonAttacks(80.0, 40.0, Double.POSITIVE_INFINITY));
    }
}
