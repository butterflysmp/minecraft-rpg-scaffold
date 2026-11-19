package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE ARENA'S LAYOUT, FROM EITHER SOURCE (PLAN-dragon-mobs.md C3; the seat's queue, item 2). The generated arena's
 * layout is EXACTLY what the arena read from its shape before, so no merged row moves. A template's layout is read from
 * its markers: one podium (the centre, on the floor), and one crystal marker per crystal.
 */
class ArenaLayoutTest {

    /** The shipped generated arena (content/fights/dragon.yml, arena:). */
    private static final ArenaShape SHIPPED = new ArenaShape(0, 0, 64, 56, 3, 10, 40.0, 3, 12);

    @Test
    void theGeneratedLayoutIsExactlyTheShapesNumbers() {
        ArenaLayout l = SHIPPED.layout();
        assertEquals(0, l.centreX());
        assertEquals(0, l.centreZ());
        assertEquals(64, l.floorY());
        assertEquals(65.0, l.groundY(), 0.0, "C's ground: the floor's top plus one, as before");
        assertEquals(10, l.crystalSpots().size());
        for (int n = 1; n <= 10; n++) {
            assertArrayEquals(SHIPPED.crystalSpot(n), l.crystalSpots().get(n - 1), 0.0, "spot " + n + " unchanged");
        }
        assertEquals(56, l.scanRadius(), "the probe's fire scan: the disc, as before");
        assertEquals(15, l.scanTopAboveFloor(), "y 65..79, as the ARENAPROBE line printed (pillar top 12 + 3)");
    }

    private static TemplateLayout.Marker crystal(int x, int y, int z) {
        return new TemplateLayout.Marker(x, y, z, TemplateLayout.Kind.CRYSTAL);
    }

    private static TemplateLayout.Marker podium(int x, int y, int z) {
        return new TemplateLayout.Marker(x, y, z, TemplateLayout.Kind.PODIUM);
    }

    /** A 21 x 10 x 21 file, its podium at (10, 3, 10): the floor's top at file y 2. Four crystals. */
    private static List<TemplateLayout.Marker> four() {
        List<TemplateLayout.Marker> m = new ArrayList<>();
        m.add(podium(10, 3, 10));
        m.add(crystal(10, 8, 18)); // +z: north-ish of the podium, angle 90
        m.add(crystal(18, 8, 10)); // +x: due east, angle 0
        m.add(crystal(2, 8, 10));  // -x: angle 180
        m.add(crystal(10, 8, 2));  // -z: angle 270
        return m;
    }

    /** The podium lands on the world's (0, 0); its floor on the content's floor_y; crystals ordered east, anticlockwise. */
    @Test
    void aTemplateIsPlacedSoItsPodiumIsTheCentreOnTheFloor() {
        TemplateLayout.Result r = TemplateLayout.read(21, 10, 21, four(), 64, 4);
        TemplateLayout.Ok ok = assertInstanceOf(TemplateLayout.Ok.class, r);
        assertEquals(-10, ok.originX(), "file x 10 -> world 0");
        assertEquals(-10, ok.originZ());
        assertEquals(62, ok.originY(), "file y 2 (the floor's top under the podium) -> world 64");
        ArenaLayout l = ok.layout();
        assertEquals(0, l.centreX());
        assertEquals(0, l.centreZ());
        assertEquals(64, l.floorY());
        // Each crystal stands AT its marker (the marker block is removed on load): world = origin + file, centred.
        assertArrayEquals(new double[] {8.5, 70.0, 0.5}, l.crystalSpots().get(0), 1e-9, "1: due east (+x)");
        assertArrayEquals(new double[] {0.5, 70.0, 8.5}, l.crystalSpots().get(1), 1e-9, "2: +z, anticlockwise");
        assertArrayEquals(new double[] {-7.5, 70.0, 0.5}, l.crystalSpots().get(2), 1e-9, "3: -x");
        assertArrayEquals(new double[] {0.5, 70.0, -7.5}, l.crystalSpots().get(3), 1e-9, "4: -z");
        assertEquals(10, l.scanRadius(), "half the file's wider side");
        assertEquals(7, l.scanTopAboveFloor(), "the file's top (y 9) above its floor (y 2)");
        assertEquals(List.of(new int[] {-10 + 10, 62 + 3, -10 + 10}).get(0)[1], ok.markersWorld().get(0)[1], "the podium's own block");
        assertEquals(5, ok.markersWorld().size(), "every marker's world block, to be removed on load");
    }

    @Test
    void aTemplateWithoutExactlyOnePodiumIsRefusedByName() {
        List<TemplateLayout.Marker> none = new ArrayList<>(four());
        none.remove(0);
        assertTrue(assertInstanceOf(TemplateLayout.Refused.class, TemplateLayout.read(21, 10, 21, none, 64, 4))
                .why().contains("0 podium markers"));
        List<TemplateLayout.Marker> two = new ArrayList<>(four());
        two.add(podium(5, 3, 5));
        assertTrue(assertInstanceOf(TemplateLayout.Refused.class, TemplateLayout.read(21, 10, 21, two, 64, 4))
                .why().contains("2 podium markers"));
    }

    @Test
    void aTemplateWithTheWrongCrystalCountIsRefusedByName() {
        TemplateLayout.Refused r = assertInstanceOf(TemplateLayout.Refused.class,
                TemplateLayout.read(21, 10, 21, four(), 64, 10));
        assertTrue(r.why().contains("4 crystal markers") && r.why().contains("10"), r.why());
    }

    @Test
    void aPodiumOnTheFilesBottomLayerHasNoFloorAndIsRefused() {
        List<TemplateLayout.Marker> m = new ArrayList<>(four());
        m.set(0, podium(10, 0, 10));
        assertTrue(assertInstanceOf(TemplateLayout.Refused.class, TemplateLayout.read(21, 10, 21, m, 64, 4))
                .why().contains("no floor under the podium"));
    }
}
