package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.Blinks.Body;
import io.github.butterflysmp.rpg.core.accessory.Blinks.Floor;
import io.github.butterflysmp.rpg.core.accessory.Blinks.Ground;
import io.github.butterflysmp.rpg.core.accessory.Blinks.Held;
import io.github.butterflysmp.rpg.core.accessory.Blinks.Near;
import io.github.butterflysmp.rpg.core.accessory.Blinks.Spot;
import io.github.butterflysmp.rpg.core.accessory.Blinks.Wearer;
import io.github.butterflysmp.rpg.core.mob.MobHostility.Side;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K2, K3 and K6 (the seat, 2026-11-26): the Blink's three pure decisions as TABLES. The blink is the shipped one
 * (trigger 5, range 16, clear 5, 160 ticks invisible, 1200 cooldown).
 */
class BlinksTest {

    private static final AccessoryBehaviour.Blink BLINK = AccessoryFixtures.blink();
    private static final Vec3 HERE = new Vec3(0.5, 65, 0.5);
    private static final Wearer READY = new Wearer("world", HERE, true, false, false, 0, false);

    private static Near mob(String name, Side side, boolean targetsHim, String world, double dx, boolean alive) {
        return new Near(UUID.nameUUIDFromBytes(name.getBytes()), "zombie", side, targetsHim, world,
                new Vec3(HERE.x() + dx, HERE.y(), HERE.z()), alive);
    }

    // --- K2: THE TRIGGER --------------------------------------------------------------------------------------------

    /** One entity near a ready wearer per row: does it trigger? */
    @Test
    void theTrigger_table() {
        record Row(String why, Near n, boolean fires) {}
        List<Row> rows = List.of(
                new Row("a hostile mob targeting him at 3", mob("a", Side.MOB, true, "world", 3, true), true),
                new Row("at 5.0 exactly (inclusive, CC'S CALL)", mob("b", Side.MOB, true, "world", 5.0, true), true),
                new Row("at 5.001", mob("c", Side.MOB, true, "world", 5.001, true), false),
                new Row("at 0, on his feet", mob("d", Side.MOB, true, "world", 0, true), true),
                new Row("within 5 but NOT targeting him", mob("e", Side.MOB, false, "world", 2, true), false),
                new Row("a player targeting him (Ben: not until a pvp pass)", mob("f", Side.PLAYER, true, "world", 2, true), false),
                new Row("a summon (never hostile to him)", mob("g", Side.SUMMON, true, "world", 2, true), false),
                new Row("dead", mob("h", Side.MOB, true, "world", 2, false), false),
                new Row("another world, same coordinates", mob("i", Side.MOB, true, "world_nether", 0, true), false));
        for (Row r : rows) {
            assertEquals(r.fires(), Blinks.triggers(BLINK, READY, r.n()), r.why());
            assertEquals(r.fires(), Blinks.trigger(BLINK, READY, List.of(r.n())).isPresent(), r.why());
        }
        // Vertical distance counts: feet to feet in three dimensions.
        Near above = new Near(UUID.randomUUID(), "zombie", Side.MOB, true, "world", new Vec3(0.5, 70.001, 0.5), true);
        assertFalse(Blinks.triggers(BLINK, READY, above), "5.001 straight up is out");
    }

    /** The wearer's own state holds it: each row is a reason it does not fire, with a mob that otherwise would. */
    @Test
    void theWearersState_holdsIt() {
        Near close = mob("z", Side.MOB, true, "world", 2, true);
        assertEquals(Held.NONE, Blinks.held(READY));
        record Row(Wearer w, Held why) {}
        for (Row r : List.of(
                new Row(new Wearer("world", HERE, false, false, false, 0, false), Held.DEAD),
                new Row(new Wearer("world", HERE, true, true, false, 0, false), Held.EXEMPT),
                new Row(new Wearer("world", HERE, true, false, true, 0, false), Held.BLINKED),
                new Row(new Wearer("world", HERE, true, false, false, 1, false), Held.COOLDOWN),
                new Row(new Wearer("world", HERE, true, false, false, 0, true), Held.RETRY))) {
            assertEquals(r.why(), Blinks.held(r.w()));
            assertTrue(Blinks.trigger(BLINK, r.w(), List.of(close)).isEmpty(), r.why().name());
        }
    }

    /** The nearest qualifying mob is the trigger; a tie keeps the first found; non-qualifying nearer ones are skipped. */
    @Test
    void theTriggerIsTheNearestQualifyingMob() {
        Near far = mob("far", Side.MOB, true, "world", 4, true);
        Near near = mob("near", Side.MOB, true, "world", 1.5, true);
        Near nearerButIdle = mob("idle", Side.MOB, false, "world", 0.5, true);
        assertEquals(near.id(), Blinks.trigger(BLINK, READY, List.of(far, nearerButIdle, near)).orElseThrow().mob().id());
        Near twin = mob("twin", Side.MOB, true, "world", -1.5, true);
        assertEquals(near.id(), Blinks.trigger(BLINK, READY, List.of(near, twin)).orElseThrow().mob().id());
        assertEquals(1.5, Blinks.trigger(BLINK, READY, List.of(near)).orElseThrow().distance(), 1e-9);
    }

    // --- K3: THE DESTINATION ----------------------------------------------------------------------------------------

    /**
     * A flat world: a stone floor at y 64 everywhere, air above, all loaded, all inside the border. The wearer's feet
     * stand on it at (0.5, 65, 0.5). Overrides make one cell anything else.
     */
    private static class Flat implements Ground {
        final java.util.Map<String, Floor> floors = new java.util.HashMap<>();
        final java.util.Map<String, Body> bodies = new java.util.HashMap<>();
        final Set<String> unloaded = new HashSet<>();
        final Set<String> outsideBorder = new HashSet<>();
        int reads;

        static String k(int x, int y, int z) {
            return x + "," + y + "," + z;
        }

        @Override public boolean loaded(int x, int z) {
            return !unloaded.contains(Math.floorDiv(x, 16) + "," + Math.floorDiv(z, 16));
        }

        @Override public boolean insideBorder(int x, int z) {
            return !outsideBorder.contains(x + "," + z);
        }

        @Override public Floor floor(int x, int y, int z) {
            reads++;
            Floor f = floors.get(k(x, y, z));
            if (f != null) return f;
            return y == 64 ? Floor.SAFE : Floor.NOT_SOLID;
        }

        @Override public Body body(int x, int y, int z) {
            reads++;
            Body b = bodies.get(k(x, y, z));
            if (b != null) return b;
            return y == 64 ? Body.SOLID : Body.CLEAR;
        }
    }

    private static boolean has(List<Spot> spots, int x, int y, int z) {
        return spots.contains(new Spot(x, y, z));
    }

    /** The search's bound: every column within 16 of his block, three blocks up and down; on flat ground one spot each. */
    @Test
    void theSearchsBound_andFlatGround() {
        Flat g = new Flat();
        Blinks.Search s = Blinks.search(BLINK, HERE, g, List.of());
        // Columns: integer (dx, dz) with dx^2 + dz^2 <= 16^2, counted by hand below, not trusted from the code.
        int columns = 0;
        for (int dx = -16; dx <= 16; dx++) for (int dz = -16; dz <= 16; dz++) if (dx * dx + dz * dz <= 256) columns++;
        assertEquals(797, columns, "the disc of radius 16 holds 797 integer columns (EXECUTED by the loop above; pi x 256 is 804)");
        assertEquals(797, s.columns());
        assertEquals(797 * 5, s.tried(), "five heights a column: his feet's block and two up and two down");
        // On flat ground the only safe height is y 65, and a spot is kept only if its feet are within 16 of his.
        int expected = 0;
        for (int dx = -16; dx <= 16; dx++) for (int dz = -16; dz <= 16; dz++) {
            if (dx * dx + dz * dz <= 256) expected++;   // feet (dx + .5, 65, dz + .5) vs his (.5, 65, .5): the same distance
        }
        assertEquals(expected, s.safe().size());
        for (Spot sp : s.safe()) assertEquals(65, sp.y());
        assertTrue(has(s.safe(), 16, 65, 0) && has(s.safe(), 0, 65, -16), "16 exactly is in (inclusive)");
        assertFalse(has(s.safe(), 12, 65, 12), "16.97 away is out");
    }

    /** One row per rule a spot must pass, each staged at (4, 65, 0) on flat ground with no mob near it. */
    @Test
    void theSafeSpot_table() {
        record Row(String why, java.util.function.Consumer<Flat> stage, boolean safe) {}
        List<Row> rows = List.of(
                new Row("flat stone, air above: safe", g -> { }, true),
                new Row("a harmful floor (magma, a campfire, cactus, dripstone)", g -> g.floors.put(Flat.k(4, 64, 0), Floor.HARMFUL), false),
                new Row("no solid floor (air, water, a slab of powder snow)", g -> g.floors.put(Flat.k(4, 64, 0), Floor.NOT_SOLID), false),
                new Row("a solid block at his feet", g -> g.bodies.put(Flat.k(4, 65, 0), Body.SOLID), false),
                new Row("a solid block at his head", g -> g.bodies.put(Flat.k(4, 66, 0), Body.SOLID), false),
                new Row("fluid at his feet", g -> g.bodies.put(Flat.k(4, 65, 0), Body.FLUID), false),
                new Row("fluid at his head", g -> g.bodies.put(Flat.k(4, 66, 0), Body.FLUID), false),
                new Row("fire (or a berry bush, a wither rose, a portal) at his feet", g -> g.bodies.put(Flat.k(4, 65, 0), Body.HARMFUL), false),
                new Row("harmful at his head", g -> g.bodies.put(Flat.k(4, 66, 0), Body.HARMFUL), false),
                new Row("outside the world border", g -> g.outsideBorder.add("4,0"), false),
                new Row("in a chunk not loaded", g -> g.unloaded.add("0,0"), false));
        for (Row r : rows) {
            Flat g = new Flat();
            r.stage().accept(g);
            assertEquals(r.safe(), has(Blinks.search(BLINK, HERE, g, List.of()).safe(), 4, 65, 0), r.why());
        }
    }

    /** An unloaded chunk is never READ: its blocks are not asked for at all (never load one). */
    @Test
    void anUnloadedChunkIsNeverRead() {
        Flat g = new Flat() {
            @Override public Floor floor(int x, int y, int z) {
                if (Math.floorDiv(x, 16) == -1) throw new AssertionError("read a block in an unloaded chunk at x " + x);
                return super.floor(x, y, z);
            }

            @Override public Body body(int x, int y, int z) {
                if (Math.floorDiv(x, 16) == -1) throw new AssertionError("read a block in an unloaded chunk at x " + x);
                return super.body(x, y, z);
            }
        };
        for (int cz = -2; cz <= 2; cz++) g.unloaded.add("-1," + cz);
        Blinks.Search s = Blinks.search(BLINK, HERE, g, List.of());
        for (Spot sp : s.safe()) assertTrue(sp.x() >= 0, "no spot west of x 0");
    }

    /** A hostile mob within clear_radius of a spot (inclusive) rules it out; at 5.001 it does not. */
    @Test
    void aMobNearASpot_rulesItOut() {
        Flat g = new Flat();
        Vec3 spotFeet = new Spot(10, 65, 0).feet();
        assertFalse(has(Blinks.search(BLINK, HERE, g, List.of(new Vec3(spotFeet.x() + 5.0, 65, spotFeet.z()))).safe(), 10, 65, 0),
                "a mob 5.0 away (inclusive, CC'S CALL)");
        assertTrue(has(Blinks.search(BLINK, HERE, g, List.of(new Vec3(spotFeet.x() + 5.001, 65, spotFeet.z()))).safe(), 10, 65, 0),
                "a mob 5.001 away");
        // The trigger itself is a mob near him: every spot within 5 of it is gone, his own included.
        Vec3 trigger = new Vec3(2.5, 65, 0.5);
        List<Spot> safe = Blinks.search(BLINK, HERE, g, List.of(trigger)).safe();
        assertFalse(has(safe, 0, 65, 0), "where he stands is 2 from the trigger");
        for (Spot sp : safe) assertTrue(sp.feet().distanceSquared(trigger) > 25, sp.toString());
    }

    /** A ledge two up and a pit two down are reached; three is not (CC'S CALL, 2: see Blinks.VERTICAL_REACH). */
    @Test
    void theVerticalReach_isTwo() {
        assertEquals(2, Blinks.VERTICAL_REACH);
        Flat g = new Flat();
        // A pillar of stone at (6, 65..66, 0): its top is a floor at y 66, standing at y 67 (two up).
        for (int y = 65; y <= 66; y++) {
            g.bodies.put(Flat.k(6, y, 0), Body.SOLID);
            g.floors.put(Flat.k(6, y, 0), Floor.SAFE);
        }
        assertTrue(has(Blinks.search(BLINK, HERE, g, List.of()).safe(), 6, 67, 0), "two up");
        g.bodies.put(Flat.k(6, 67, 0), Body.SOLID);
        g.floors.put(Flat.k(6, 67, 0), Floor.SAFE);
        assertFalse(has(Blinks.search(BLINK, HERE, g, List.of()).safe(), 6, 68, 0), "three up");
        // A pit at (8, ..., 0): floor at y 62, standing at y 63 (two down).
        Flat p = new Flat();
        p.floors.put(Flat.k(8, 64, 0), Floor.NOT_SOLID);
        for (int y = 63; y <= 64; y++) p.bodies.put(Flat.k(8, y, 0), Body.CLEAR);
        p.floors.put(Flat.k(8, 62, 0), Floor.SAFE);
        assertTrue(has(Blinks.search(BLINK, HERE, p, List.of()).safe(), 8, 63, 0), "two down");
        Flat q = new Flat();
        q.floors.put(Flat.k(8, 64, 0), Floor.NOT_SOLID);
        for (int y = 62; y <= 64; y++) q.bodies.put(Flat.k(8, y, 0), Body.CLEAR);
        q.floors.put(Flat.k(8, 62, 0), Floor.NOT_SOLID);
        q.floors.put(Flat.k(8, 61, 0), Floor.SAFE);
        assertFalse(has(Blinks.search(BLINK, HERE, q, List.of()).safe(), 8, 62, 0), "three down");
    }

    /** The choice is uniform over the safe spots by the random source passed in: a seed fixes it, every spot is reachable. */
    @Test
    void theChoice_isSeeded_andUniform() {
        List<Spot> spots = List.of(new Spot(1, 65, 0), new Spot(2, 65, 0), new Spot(3, 65, 0));
        assertEquals(Optional.empty(), Blinks.choose(List.of(), new Random(1)));
        assertEquals(Blinks.choose(spots, new Random(42)), Blinks.choose(spots, new Random(42)), "a seed fixes it");
        int[] counts = new int[3];
        Random r = new Random(7);
        for (int i = 0; i < 30000; i++) counts[spots.indexOf(Blinks.choose(spots, r).orElseThrow())]++;
        for (int c : counts) assertTrue(c > 9500 && c < 10500, "each about a third: " + java.util.Arrays.toString(counts));
    }

    /** The spot's feet are the block's centre on its floor. */
    @Test
    void aSpotsFeet() {
        assertEquals(new Vec3(-3.5, 65, 7.5), new Spot(-4, 65, 7).feet());
    }

    // --- K6: THE INVISIBILITY ---------------------------------------------------------------------------------------

    private static Blinks.Effect fx(int duration, int amp) {
        return new Blinks.Effect(duration, amp, false, true, true);
    }

    /** At the blink: apply ours unless he already has one that covers the whole state, which is never touched. */
    @Test
    void theInvisibility_atTheBlink() {
        assertTrue(Blinks.invisibilityAtBlink(BLINK, Optional.empty()).apply(), "none: ours");
        assertTrue(Blinks.invisibilityAtBlink(BLINK, Optional.of(fx(100, 0))).apply(), "a shorter one: ours, his remembered");
        assertEquals(Optional.of(fx(100, 0)), Blinks.invisibilityAtBlink(BLINK, Optional.of(fx(100, 0))).prior());
        assertFalse(Blinks.invisibilityAtBlink(BLINK, Optional.of(fx(160, 0))).apply(), "160 already covers it");
        assertFalse(Blinks.invisibilityAtBlink(BLINK, Optional.of(fx(2000, 1))).apply(), "longer: never shortened");
        assertFalse(Blinks.invisibilityAtBlink(BLINK, Optional.of(fx(Blinks.Effect.INFINITE, 0))).apply(), "infinite");
    }

    /** At the end: remove ONLY ours, and give back what remains of his own. */
    @Test
    void theInvisibility_atTheEnd() {
        Blinks.Effect ours = Blinks.ours(BLINK);
        assertEquals(new Blinks.Effect(160, 0, false, false, true), ours, "160 ticks, amplifier 0, no particles, its icon");
        record Row(String why, Blinks.InvisibilityAtBlink start, int elapsed, Optional<Blinks.Effect> now,
                   Blinks.EndAction action, Optional<Blinks.Effect> restore) {}
        Blinks.InvisibilityAtBlink none = new Blinks.InvisibilityAtBlink(true, Optional.empty());
        Blinks.InvisibilityAtBlink shorter = new Blinks.InvisibilityAtBlink(true, Optional.of(fx(100, 0)));
        Blinks.InvisibilityAtBlink his = new Blinks.InvisibilityAtBlink(false, Optional.of(fx(2000, 0)));
        for (Row r : List.of(
                new Row("ours, cancelled at 40: removed", none, 40, Optional.of(fx(120, 0)), Blinks.EndAction.REMOVE, Optional.empty()),
                new Row("ours, expired: already gone", none, 160, Optional.empty(), Blinks.EndAction.NOTHING, Optional.empty()),
                new Row("ours at 160, the last tick: removed", none, 160, Optional.of(fx(0, 0)), Blinks.EndAction.REMOVE, Optional.empty()),
                new Row("his shorter one, cancelled at 40: removed and 60 given back", shorter, 40, Optional.of(fx(120, 0)),
                        Blinks.EndAction.REMOVE_AND_RESTORE, Optional.of(fx(60, 0))),
                new Row("his shorter one had run out by the end: removed only", shorter, 120, Optional.of(fx(40, 0)),
                        Blinks.EndAction.REMOVE, Optional.empty()),
                new Row("his own covered it: never touched", his, 40, Optional.of(fx(1960, 0)), Blinks.EndAction.NOTHING, Optional.empty()),
                new Row("someone gave him a longer one meanwhile: left alone", none, 40, Optional.of(fx(3000, 0)),
                        Blinks.EndAction.NOTHING, Optional.empty()),
                new Row("a stronger one meanwhile: left alone", none, 40, Optional.of(fx(100, 1)), Blinks.EndAction.NOTHING, Optional.empty()),
                new Row("milk took it off: nothing to remove, nothing given back", shorter, 40, Optional.empty(),
                        Blinks.EndAction.NOTHING, Optional.empty()))) {
            Blinks.InvisibilityAtEnd e = Blinks.invisibilityAtEnd(BLINK.invisibilityTicks(), r.start(), r.elapsed(), r.now());
            assertEquals(r.action(), e.action(), r.why());
            assertEquals(r.restore(), e.restore(), r.why());
        }
    }

    // --- the record's own refusals ----------------------------------------------------------------------------------

    @Test
    void theBlinksRefusals() {
        assertThrows(IllegalArgumentException.class, () -> new AccessoryBehaviour.Blink(" ", 5, 16, 5, 160, 1200, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryBehaviour.Blink("B", 0, 16, 5, 160, 1200, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryBehaviour.Blink("B", 5, 0, 5, 160, 1200, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryBehaviour.Blink("B", 5, 16, -1, 160, 1200, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryBehaviour.Blink("B", 5, 16, 5, 0, 1200, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryBehaviour.Blink("B", 5, 16, 5, 160, -1, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new AccessoryBehaviour.Blink("B", 5, 16, 5, 160, 1200, "portal", 0, null));
        new AccessoryBehaviour.Blink("B", 5, 16, 0, 1, 0, null, 0, null);   // the least it accepts
    }

    /** The spots are listed in one fixed order (x, then z, then y), so a seeded choice is a fixed spot. */
    @Test
    void theListingOrderIsFixed() {
        List<Spot> safe = Blinks.search(BLINK, HERE, new Flat(), List.of()).safe();
        List<Spot> sorted = new ArrayList<>(safe);
        sorted.sort(java.util.Comparator.comparingInt(Spot::x).thenComparingInt(Spot::z).thenComparingInt(Spot::y));
        assertEquals(sorted, safe);
    }
}
