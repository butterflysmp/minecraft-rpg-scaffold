package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.build.AspectLore;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.combat.PulseFront;
import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.paper.content.AbilityLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-solar-flare's predictions, EXECUTED through the real functions before any boot (S-1). F is his stand, block-centred,
 * feet at y 0; his eye at y 1.62; he faces due east (yaw -90, pitch 0). East +x, south +z.
 *
 * <p><b>WHEN A SHOT STOPS (READ, ProjectileFlight.step):</b> the step on the cast's tick + e traces 0.9375 e to 0.9375
 * (e + 1) blocks from his eye, so a face at distance f is met on tick + e with e the least whole number for which
 * 0.9375 (e + 1) >= f (and at range, e = 15). A zombie is 0.6 wide (READ, EntityType's ZOMBIE {@code sized(0.6, 1.95)}),
 * the trace inflates nothing ({@code RAY_SIZE} 0.0, READ), so a zombie centred d out on the line presents its face at d -
 * 0.3. Burns: as {@code SmolderGateArithmeticTest} (the first lands 19 after a hit, then every 20; executed through the
 * real store, its javadoc).
 */
class SolarFlareGateArithmeticTest {

    @TempDir Path dir;

    static final double SPEED = 0.9375;
    static final double EYE = 1.62;
    static final double HALF_ZOMBIE = 0.3;
    static final double Z3000 = MobScaling.maxHealth(600.0, false, true, 100);

    static int stopTick(double face) {
        for (int e = 0; e < 16; e++) if (SPEED * (e + 1) >= face) return e;
        return 15;
    }

    static int ring(Vec3 origin, Vec3 body) {
        return PulseFront.reachTick(PulseFront.horizontal(origin, body), 6.0, 15);
    }

    static Vec3 range(double yawOffsetDegrees) {
        double a = Math.toRadians(yawOffsetDegrees);
        return new Vec3(15 * Math.cos(a), EYE, 15 * Math.sin(a));
    }

    record Burns(List<Long> offsets, double each, double total) {}

    /** Real hit offsets (ticks after the first hit) through the real store; see SmolderGateArithmeticTest's javadoc. */
    static Burns burns(double maxHealth, double cap, int... realHitOffsets) {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, maxHealth);
        UUID id = UUID.randomUUID();
        int last = 0;
        for (int i = 0; i < realHitOffsets.length; i++) {
            int fake = i == 0 ? 0 : realHitOffsets[i] + 1;
            clock.advance(fake - last);
            scorch.apply(id, clock, sink, 80, cap, UUID.randomUUID(), Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
            last = fake;
        }
        clock.advance(1000);
        List<Long> offsets = new ArrayList<>();
        for (var b : sink.burns) offsets.add(b.atTick() - 1);
        return new Burns(offsets, sink.burns.isEmpty() ? 0 : sink.burns.get(0).amount(), sink.totalDealt());
    }

    // ---------------------------------------------------------------- the hit, and Scorch from it

    @Test
    void aRingHitIs250UncritAnd500CritAndItsCapIs125EitherWay() {
        double base = HitDamage.hitBase(GearScore.scaledDamage(250, GearScore.ABSENT), 0.0, 0.0);
        assertEquals(250.0, HitDamage.dealt(base, 1.0, Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.99)), 1e-9);
        assertEquals(500.0, HitDamage.dealt(base, 1.0, Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.0)), 1e-9);
        assertEquals(125.0, 250 * Scorch.CAP_FRACTION, 1e-9);
        assertEquals(3000.0, Z3000, 1e-9);
        assertEquals(125.0, Math.min(Z3000 * Scorch.RATE_PER_SECOND, 125.0), 1e-9, "min(5% of 3000 = 150, 125)");
    }

    // ---------------------------------------------------------------- SF2, SF3, SF4, SF5, SF6: ticks and distances

    @Test
    void sf2_theThreeRangeOriginsAndTheZombieFourPast() {
        assertEquals(15, stopTick(15.0));
        Vec3 o2 = range(0);
        assertEquals(new Vec3(15, EYE, 0), o2);
        assertEquals(12.990, range(-30).x(), 5e-4);
        assertEquals(-7.5, range(-30).z(), 1e-9);
        assertEquals(10, ring(o2, new Vec3(19, 0, 0)), "4 past: + 10, so C + 25");
        assertEquals(-1, ring(o2, new Vec3(22, 0, 0)), "7 past: never");
        assertTrue(PulseFront.horizontal(range(-30), new Vec3(19, 0, 0)) > 6 && PulseFront.horizontal(range(30), new Vec3(19, 0, 0)) > 6);
        Burns one = burns(Z3000, 125.0, 0);
        assertEquals(List.of(19L, 39L, 59L, 79L, 99L, 119L), one.offsets());
        assertEquals(2000.0, Z3000 - 250 - one.total(), 1e-9);
        assertEquals(1750.0, Z3000 - 500 - one.total(), 1e-9);
    }

    @Test
    void sf3_aZombieEightOutStopsShotTwoOnTickEight() {
        assertEquals(8, stopTick(8 - HALF_ZOMBIE), "its face at 7.7");
        Vec3 o2 = new Vec3(8, 0, 0);
        assertEquals(0, ring(o2, new Vec3(8, 0, 0)), "the struck zombie: distance 0, the burst's own tick");
        assertEquals(13, ring(o2, new Vec3(13, 0, 0)), "5 beyond: + 13, so C + 21");
        assertTrue(PulseFront.horizontal(range(30), new Vec3(13, 0, 0)) > 6, "no side ring reaches it");
    }

    @Test
    void sf4_aBlockSixOutStopsShotTwoOnTickFive() {
        double face = 5.5;   // the block at dx 6 spans 5.5..6.5 from his block-centred x
        assertEquals(5, stopTick(face));
        Vec3 origin = new Vec3(5.5, EYE, 0);
        Vec3 b = new Vec3(7, 0, -1.5);
        assertEquals(2.121, PulseFront.horizontal(origin, b), 5e-4);
        assertEquals(6, ring(origin, b), "+ 6, so C + 11");
        assertTrue(PulseFront.horizontal(range(-30), b) > 6, "the left ring at range does not reach it");
    }

    @Test
    void sf5_twoRingsAtRange_thenThreeBehindAWall() {
        Vec3 t = new Vec3(14, 0, 4);
        assertEquals(11, ring(range(0), t), "n=2 at C + 26");
        assertEquals(10, ring(range(30), t), "n=3 at C + 25");
        assertTrue(PulseFront.horizontal(range(-30), t) > 6, "n=1 does not reach it");
        Burns two = burns(Z3000, 125.0, 0, 1);
        assertEquals(Z3000 - 500 - two.total(), 2500 - two.total(), 1e-9);
        assertEquals(List.of(19L, 39L, 59L, 79L, 99L, 119L), two.offsets(), "two hits a tick apart: six burns from the first");
        assertEquals(1750.0, Z3000 - 500 - two.total(), 1e-9);
        assertEquals(1250.0, Z3000 - 1000 - two.total(), 1e-9);

        // The wall at dx 3, dz -2..2 (faces at 2.5): the centre meets it 2.5 out, each side 2.5 / cos 30 out.
        assertEquals(2, stopTick(2.5));
        double side = 2.5 / Math.cos(Math.toRadians(30));
        assertEquals(2.887, side, 5e-4);
        assertEquals(3, stopTick(side));
        double sideZ = 2.5 * Math.tan(Math.toRadians(30));
        assertEquals(1.443, sideZ, 5e-4);
        Vec3 u = new Vec3(4.5, 0, 0);
        assertEquals(5, ring(new Vec3(2.5, EYE, 0), u), "n=2 at C + 7");
        assertEquals(7, ring(new Vec3(2.5, EYE, -sideZ), u), "n=1 at C + 10");
        assertEquals(7, ring(new Vec3(2.5, EYE, sideZ), u), "n=3 at C + 10");
        Burns three = burns(Z3000, 125.0, 0, 3, 3);
        assertEquals(List.of(19L, 39L, 59L, 79L, 99L, 119L), three.offsets());
        assertEquals(1500.0, Z3000 - 750 - three.total(), 1e-9);
        assertEquals(750.0, Z3000 - 1500 - three.total(), 1e-9, "still alive on a crit");
        assertTrue(Math.abs(sideZ) + 0.5 < 2.5, "the side shots meet the wall, inside its z -2.5..2.5");
    }

    @Test
    void sf6_theControlZombieInRingTwo() {
        Vec3 w = new Vec3(16, 0, 2);
        assertEquals(2.236, PulseFront.horizontal(range(0), w), 5e-4);
        assertEquals(6, ring(range(0), w), "C + 21");
        assertTrue(PulseFront.horizontal(range(0), new Vec3(14, 0, -2)) <= 6, "his wolf is INSIDE ring 2");
        Burns one = burns(Z3000, 125.0, 0);
        long hit = 15 + 6;
        assertEquals(2, one.offsets().stream().filter(o -> hit + o < 70).count(), "burns at C+40 and C+60 before C+70");
        assertTrue(one.offsets().stream().noneMatch(o -> hit + o == 70), "no burn ON the probe's tick");
        assertEquals(2500.0, Z3000 - 250 - 2 * 125.0, 1e-9);
        assertEquals(2250.0, Z3000 - 500 - 2 * 125.0, 1e-9);
    }

    // ---------------------------------------------------------------- the box

    /**
     * THE SEAT'S D3 (2026-11-27), applied here too: EVERY origin every row uses, not only the range ones. Every cast stands
     * him at G = F + 10 east. The first version of this row checked the RANGE origins alone, from F, and passed while SF5's
     * wall bursts (2.5 from F) reached 3.5 WEST of F, outside the interior's 2.5: corrected before any boot. Origins below
     * are from G; reach is origin +/- 6; edges are from F (interior columns x -2..45, z -13..13; the preflight -2 -13 33 13).
     */
    @Test
    void everyRingFromEveryOriginLiesInsideTheInteriorAndThePreflight() {
        double g = 10;
        double sideZ = 2.5 * Math.tan(Math.toRadians(30));
        List<Vec3> origins = List.of(range(-30), range(0), range(30),            // SF2, SF5, SF6, SF9 at range
                new Vec3(8, EYE, 0),                                             // SF3: s8's feet
                new Vec3(5.5, EYE, 0),                                           // SF4: the stone's face
                new Vec3(2.5, EYE, -sideZ), new Vec3(2.5, EYE, 0), new Vec3(2.5, EYE, sideZ));   // SF5: the wall
        for (Vec3 o : origins) {
            double west = g + o.x() - 6, east = g + o.x() + 6, side = Math.abs(o.z()) + 6;
            assertTrue(west >= -2.5 && east <= 45.5 && side <= 13.5 + 1e-9, "interior: " + o + " " + west + ".." + east + " z " + side);
            assertTrue(west >= -2.5 && east <= 33.5 && side <= 13.5 + 1e-9, "preflight: " + o);
        }
        assertEquals(6.5, g + 2.5 - 6, 1e-9, "the wall's west reach, from F");
        assertEquals(-3.5, 2.5 - 6, 1e-9, "the same from F itself: the defect D3 names");
        assertEquals(31.0, g + 15 + 6, 1e-9, "the centre range ring's east reach, from F");
        assertEquals(13.5, Math.abs(range(30).z()) + 6, 1e-9, "the side rings reach EXACTLY the interior's edge");
        // Every staged body, from F: inside the preflight box.
        double[][] bodies = {{29, 0}, {32, 0}, {18, 0}, {23, 0}, {17, -1.5}, {24, 4}, {14.5, 0}, {26, 2}, {24, -2}};
        for (double[] b : bodies) {
            assertTrue(b[0] >= -2.5 && b[0] <= 33.5 && Math.abs(b[1]) <= 13.5, b[0] + "," + b[1]);
        }
    }

    // ---------------------------------------------------------------- SAFETY

    @Test
    void safety_theRingAndIgniteSpareHim_andTheRingRefusesHisWolf() {
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.PLAYER, false, MobHostility.PvpPath.PULSE));
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.PLAYER, false,
                MobHostility.PvpPath.IGNITE_SPLASH));
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.SUMMON, false, MobHostility.PvpPath.PULSE));
    }

    // ---------------------------------------------------------------- SF1

    @Test
    void sf1_theCellsNumberLines() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/abilities/solar_flare.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("solar_flare.yml"), in.readAllBytes());
        }
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        AbilityDefinition def = new AbilityLoader(log).loadAll(dir.toFile()).find("solar_flare").orElseThrow();
        assertEquals(List.of(new AspectLore.Line("Damage", "250", null), new AspectLore.Line("Cost", "80", null),
                new AspectLore.Line("Cooldown", "210.0 s", null)), AspectLore.lines(def, def));
        assertEquals(209.9, (4200 - 2) / 20.0, 1e-9, "SF7: the refusal 2 ticks after the cast reads 209.9");
    }
}
