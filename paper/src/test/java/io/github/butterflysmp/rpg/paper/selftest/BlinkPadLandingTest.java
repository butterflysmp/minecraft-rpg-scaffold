package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.AccessoryBehaviour;
import io.github.butterflysmp.rpg.core.accessory.Blinks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE LANDING SPOT IS RANDOM AND MUST STILL LIE INSIDE THE PREFLIGHTED BOX (the seat's gate ruling, 2026-11-26). The
 * destination decision ({@link Blinks#search}) EXECUTED over the pad's real cells ({@link SelfTestPadShape}) from each
 * blink row's stand point, with every cell OUTSIDE the pad taken at its WORST: a safe floor and a clear body, at once.
 * So the result holds whatever the world beside and beneath the sky pad holds: no terrain is assumed to be air.
 *
 * <p>Coordinates are the pad's: x east and z south of the stand column, y 0 the floor (Y 200), feet at y 1. Every blink
 * row stands him at (20, 1, 0) ({@code tp @s ~20 ~ ~} from the pad's stand, centre (20.5, 1, 0.5)) with four NoAI blocker
 * zombies at (17, 13), (23, 13), (17, -13), (23, -13) and the trigger at (23, 0), each summoned at the stand before he
 * moves; {@code GATE-blink-tome.yml} carries exactly these offsets ({@code BlinkTomeGateCommandTest} reads them back).
 */
class BlinkPadLandingTest {

    static final AccessoryBehaviour.Blink BLINK = new AccessoryBehaviour.Blink("Blink", 5, 16, 5, 160, 1200, "poof", 12,
            "entity.enderman.teleport");

    /** His feet at a blink row's stand point. */
    static final Vec3 FROM = new Vec3(20.5, 1, 0.5);

    /** The four blockers' summon offsets, (east, south). */
    static final int[][] BLOCKERS = {{17, 13}, {23, 13}, {17, -13}, {23, -13}};

    /** The trigger's offset in every row that blinks from the stand: three east of him. */
    static final int[] TRIGGER = {23, 0};

    /** The row's preflight box, east from, south from, east to, south to (the gate's setup declares it). */
    static final int[] BOX = {0, -13, 36, 13};

    /** The pad, with everything outside it at its worst. */
    static final Blinks.Ground WORST = new Blinks.Ground() {
        @Override public boolean loaded(int x, int z) {
            return true;
        }

        @Override public boolean insideBorder(int x, int z) {
            return true;
        }

        @Override public Blinks.Floor floor(int x, int y, int z) {
            SelfTestPadShape.Kind k = SelfTestPadShape.kindAt(x, y, z);
            if (k == null) return Blinks.Floor.SAFE;                  // outside the pad: assume the worst
            return k == SelfTestPadShape.Kind.AIR ? Blinks.Floor.NOT_SOLID : Blinks.Floor.SAFE;   // stone, and barrier
        }

        @Override public Blinks.Body body(int x, int y, int z) {
            SelfTestPadShape.Kind k = SelfTestPadShape.kindAt(x, y, z);
            if (k == null) return Blinks.Body.CLEAR;                  // outside the pad: assume the worst
            return k == SelfTestPadShape.Kind.AIR ? Blinks.Body.CLEAR : Blinks.Body.SOLID;
        }
    };

    static Vec3 at(int[] offset) {
        return new Vec3(offset[0] + 0.5, 1, offset[1] + 0.5);
    }

    static List<Vec3> hostiles(int[]... extra) {
        List<Vec3> out = new ArrayList<>();
        for (int[] b : BLOCKERS) out.add(at(b));
        for (int[] e : extra) out.add(at(e));
        return out;
    }

    private static void everySpotIsInsideTheInteriorAndTheBox(List<Blinks.Spot> safe, String why) {
        assertFalse(safe.isEmpty(), why + ": the row needs somewhere to land");
        for (Blinks.Spot s : safe) {
            assertTrue(SelfTestPadShape.inInterior(s.x(), s.z()), why + ": " + s + " is not in the pad's interior");
            assertEquals(1, s.y(), why + ": " + s + " is not on the pad's floor (a rim top, a wall, or below)");
            assertTrue(s.x() >= BOX[0] && s.x() <= BOX[2] && s.z() >= BOX[1] && s.z() <= BOX[3],
                    why + ": " + s + " is outside the preflight's box");
        }
    }

    /** THE ENUMERATION: every spot the decision can choose, in every blink row's staging, and how many there are. */
    @Test
    void everyChoosableSpot_isOnThePadsFloor_insideTheBox() {
        Blinks.Search bt = Blinks.search(BLINK, FROM, WORST, hostiles(TRIGGER));
        everySpotIsInsideTheInteriorAndTheBox(bt.safe(), "the trigger three east (BT2 to BT8, BT10's control)");
        assertEquals(797, bt.columns());
        assertEquals(797 * 5, bt.tried());
        assertEquals(EXPECTED_SAFE_TRIGGER, bt.safe().size(), "the BLINK line's safe= in those rows");

        // BT9: the zombie six east is a hostile near him too when the control's zombie (three east) fires it.
        Blinks.Search far = Blinks.search(BLINK, FROM, WORST, hostiles(TRIGGER, new int[] {26, 0}));
        everySpotIsInsideTheInteriorAndTheBox(far.safe(), "BT9's control (the trigger three east, the far one six east)");
        assertEquals(EXPECTED_SAFE_FAR, far.safe().size(), "BT9's BLINK line's safe=");

        // And each landing is at least 5 from every hostile (the decision's own rule, read back over the enumeration).
        for (Blinks.Spot s : bt.safe()) for (Vec3 h : hostiles(TRIGGER)) assertTrue(s.feet().distanceSquared(h) > 25);
    }

    /**
     * THE CONTROL: without the blockers the same decision DOES offer spots outside the rim (the worst-case ground can
     * express them), so the blockers are what closes the ring, not the model. And at a vertical reach of three it offers
     * spots BELOW the pad's floor, which is why the reach is two (Blinks.VERTICAL_REACH).
     */
    @Test
    void theControl_withoutBlockersTheRingIsOpen() {
        List<Blinks.Spot> open = Blinks.search(BLINK, FROM, WORST, List.of(at(TRIGGER))).safe();
        long outside = open.stream().filter(s -> !SelfTestPadShape.inInterior(s.x(), s.z())).count();
        assertTrue(outside > 0, "control: the ring outside the rim is reachable without the blockers");
        assertTrue(open.stream().filter(s -> SelfTestPadShape.kindAt(s.x(), 0, s.z()) != null).allMatch(s -> s.y() >= 1),
                "at reach two nothing below the floor is offered in the pad's own columns (interior and rim)");
        // The reach of three, recomputed by hand over the pad's interior: a spot with its feet at y -2 has its head at y -1,
        // under the floor, and the worst-case ground below the pad makes it safe.
        Blinks.Spot under = new Blinks.Spot(20, -2, 5);
        assertEquals(Blinks.Floor.SAFE, WORST.floor(under.x(), under.y() - 1, under.z()));
        assertEquals(Blinks.Body.CLEAR, WORST.body(under.x(), under.y(), under.z()));
        assertEquals(Blinks.Body.CLEAR, WORST.body(under.x(), under.y() + 1, under.z()));
        // At reach two the lowest feet are y -1, whose head is the floor (y 0): SOLID.
        assertEquals(Blinks.Body.SOLID, WORST.body(20, 0, 5));
    }

    /** Every summon of the rows stands in the interior and the box (the coordinate table). */
    @Test
    void everyStagedMob_isInTheInteriorAndTheBox() {
        List<int[]> all = new ArrayList<>(List.of(BLOCKERS));
        all.add(TRIGGER);
        all.add(new int[] {26, 0});
        for (int[] m : all) {
            assertTrue(SelfTestPadShape.inInterior(m[0], m[1]), m[0] + "," + m[1]);
            assertTrue(m[0] >= BOX[0] && m[0] <= BOX[2] && m[1] >= BOX[1] && m[1] <= BOX[3], m[0] + "," + m[1]);
        }
        assertTrue(SelfTestPadShape.inInterior(20, 0) && SelfTestPadShape.inInterior(BOX[0], BOX[1])
                && SelfTestPadShape.inInterior(BOX[2], BOX[3]), "his stand and the box's corners");
    }

    static final int EXPECTED_SAFE_TRIGGER = 508;   // EXECUTED by this test
    static final int EXPECTED_SAFE_FAR = 479;   // EXECUTED by this test
}
