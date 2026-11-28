package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.combat.Ignite;
import io.github.butterflysmp.rpg.core.combat.PulseFront;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-smolder-2's and GATE-solar-flare-2's predictions, EXECUTED through the real functions before any boot (S-1; "a
 * damage prediction names every term"). Every figure either gate writes beside a row is asserted here. F is his pad stand
 * (block-centred, feet), east +x, south +z; G = F + (10, 0) is Smolder's stand, H = F + (5, 0) Solar Flare's. P is a
 * row's press (the selftest's step tick), C = P + 1 the cast (the {@code onRegion} hop: X6 (a)).
 *
 * <p>THE BURNS run on {@link ScorchBurnsReadOrder}, the server's order as READ (first burn at exactly hit + 20; on a
 * shared tick the burn before the hit's refresh), whose control is the last boot's own SM2 and SM5 readings
 * ({@code SmolderRetunedBurnsTest}).
 */
class UltimatesTwoGateArithmeticTest {

    static final double ZOMBIE_3000 = MobScaling.maxHealth(600.0, false, true, 100);
    static final double ZOMBIE_1000 = MobScaling.maxHealth(200.0, false, true, 100);

    private static List<Long> every20(long from, long to) {
        List<Long> out = new ArrayList<>();
        for (long t = from; t <= to; t += 20) out.add(t);
        return out;
    }

    private static int reach15(double d) {
        return PulseFront.reachTick(d, 12.0, 15);
    }

    // ------------------------------------------------------------------ SAFETY (S-1): every term that could price him

    @Test
    void safety_thePulseAndIgniteSparePlayersAndThePulseRefusesHisWolf() {
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.PLAYER, false,
                MobHostility.PvpPath.PULSE), "PULSE (Smolder's rings, Solar Flare's bursts): players spared");
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.PLAYER, false,
                MobHostility.PvpPath.IGNITE_SPLASH), "IGNITE_SPLASH: spared");
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.SUMMON, false,
                MobHostility.PvpPath.PULSE), "his wolf: refused");
        assertTrue(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.MOB, false,
                MobHostility.PvpPath.PULSE), "CONTROL: a mob is struck");
    }

    // ------------------------------------------------------------------ every term of a hit

    @Test
    void theHits_SmolderOneSixtyOrThreeTwentyCapEighty_SolarFlareTwoFiftyOrFiveHundredCapOneTwentyFive() {
        double normal = Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.99);
        double crit = Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.0);
        assertEquals(100, GearScore.ABSENT);
        double smolder = HitDamage.hitBase(GearScore.scaledDamage(160, GearScore.ABSENT), 0.0, 0.0);
        double flare = HitDamage.hitBase(GearScore.scaledDamage(250, GearScore.ABSENT), 0.0, 0.0);
        assertEquals(160.0, HitDamage.dealt(smolder, 1.0, normal), 1e-9);
        assertEquals(320.0, HitDamage.dealt(smolder, 1.0, crit), 1e-9);
        assertEquals(250.0, HitDamage.dealt(flare, 1.0, normal), 1e-9);
        assertEquals(500.0, HitDamage.dealt(flare, 1.0, crit), 1e-9);
        assertEquals(80.0, 160 * Scorch.CAP_FRACTION, 1e-9);
        assertEquals(125.0, 250 * Scorch.CAP_FRACTION, 1e-9);
        assertEquals(3000.0, ZOMBIE_3000, 1e-9);
        assertEquals(1000.0, ZOMBIE_1000, 1e-9);
        assertEquals(150.0, ZOMBIE_3000 * 0.05, 1e-9, "5% of 3000: above both caps, so each burn is the cap");
    }

    // ------------------------------------------------------------------ Smolder: reach ticks at travel 15

    @Test
    void smolder_reachTicks() {
        assertEquals(4, reach15(3.0), "a3");
        assertEquals(8, reach15(6.0), "a6, b6, d6");
        assertEquals(15, reach15(11.5), "a11");
        assertEquals(-1, reach15(12.5), "a12");
        assertEquals(5, reach15(4.0), "his wolf's distance (refused by the table)");
        assertEquals(14, reach15(11.0), "c11 from G (pulse 1); c19 from G + 8 (pulses 2 to 4)");
        double c11FromMoved = PulseFront.horizontal(new Vec3(8, 0, 0), new Vec3(0, 0, -11));
        assertEquals(-1, reach15(c11FromMoved), "c11 from G + 8");
        assertEquals(13.601, c11FromMoved, 5e-4);
        assertEquals(-1, reach15(19.0), "c19 from G");
    }

    // ------------------------------------------------------------------ Smolder: burns and end healths

    @Test
    void smolder_burnsAndEndHealths() {
        var four = ScorchBurnsReadOrder.burns(3000, 80, 0, 30, 60, 90);
        assertEquals(every20(20, 200), four.offsets(), "SM2R a3, a6, a11; SM3R b6: ten burns");
        assertEquals(1560.0, 3000 - 4 * 160 - four.total(), 1e-9);
        var one = ScorchBurnsReadOrder.burns(3000, 80, 0);
        assertEquals(every20(20, 120), one.offsets(), "SM4R c11: six");
        assertEquals(2360.0, 3000 - 160 - one.total(), 1e-9);
        var three = ScorchBurnsReadOrder.burns(3000, 80, 0, 30, 60);
        assertEquals(every20(20, 180), three.offsets(), "SM4R c19: nine");
        assertEquals(1800.0, 3000 - 3 * 160 - three.total(), 1e-9);
        var two = ScorchBurnsReadOrder.burns(3000, 80, 0, 30);
        assertEquals(every20(20, 140), two.offsets(), "SM5R d6: seven");
        assertEquals(2120.0, 3000 - 2 * 160 - two.total(), 1e-9);
        assertEquals(2840.0, 3000 - 160.0, 1e-9, "SM3R b6 after its first hit");
    }

    // ------------------------------------------------------------------ Solar Flare: the flight, the bursts, the burns

    /** The direction a yaw and pitch give on the server (Bukkit's own conversion). */
    private static Vec3 dir(float yaw, float pitch) {
        var d = new Location(null, 0, 0, 0, yaw, pitch).getDirection();
        return new Vec3(d.getX(), d.getY(), d.getZ());
    }

    @Test
    void solarFlare_rangeAndTheTurnedShots_andEveryBurstDistance() {
        // SF2R: due east, 32.0 out from his eye (16 steps of 2.0). The z residue is the conversion's, under a nanometre.
        assertEquals(32.0, dir(-90, 0).scale(32).x(), 1e-12);
        assertEquals(0.0, dir(-90, 0).scale(32).z(), 1e-12);
        // SF10: turned 10 degrees right, then 10 left, between shots.
        assertEquals(31.514, dir(-80, 0).scale(32).x(), 5e-4);
        assertEquals(5.557, dir(-80, 0).scale(32).z(), 5e-4);
        assertEquals(31.514, dir(-100, 0).scale(32).x(), 5e-4);
        assertEquals(-5.557, dir(-100, 0).scale(32).z(), 5e-4);
        // The bursts' distances (horizontal, from the burst origin).
        assertEquals(5.9, PulseFront.horizontal(new Vec3(32, 0, 0), new Vec3(37.9, 0, 0)), 1e-9, "SF2R z59");
        assertEquals(6.1, PulseFront.horizontal(new Vec3(32, 0, 0), new Vec3(38.1, 0, 0)), 1e-9, "SF2R z61");
        assertEquals(0, PulseFront.reachTick(5.9, 6.0, 0), "N3: struck on the burst's tick");
        assertEquals(-1, PulseFront.reachTick(6.1, 6.0, 0), "never");
        assertEquals(2.121, PulseFront.horizontal(new Vec3(5.5, 0, 0), new Vec3(7, 0, -1.5)), 5e-4, "SF4R b");
        assertEquals(2.828, PulseFront.horizontal(new Vec3(32, 0, 0), new Vec3(30, 0, -2)), 5e-4, "SF6R his wolf");
        assertEquals(2.828, PulseFront.horizontal(new Vec3(32, 0, 0), new Vec3(34, 0, 2)), 5e-4, "SF6R w");
        assertEquals(2.121, PulseFront.horizontal(new Vec3(1.5, 0, 0), new Vec3(3, 0, 1.5)), 5e-4, "SF11 e");
        assertEquals(5.0, PulseFront.horizontal(new Vec3(8, 0, 0), new Vec3(13, 0, 0)), 1e-12, "SF3R s13");
    }

    /**
     * THE STEP THAT STOPS EACH SHOT (N2: the step at elapsed e traces 2e to 2e + 2 on the launch tick + e): s8's box
     * begins at 7.7 (a zombie is 0.6 wide), step 3; the stone's west face at 5.5, step 2; SF11's wall at 1.5, step 0.
     */
    @Test
    void solarFlare_theStepThatStopsEachShot() {
        assertEquals(3, (int) Math.floor(7.7 / 2.0));
        assertEquals(2, (int) Math.floor(5.5 / 2.0));
        assertEquals(0, (int) Math.floor(1.5 / 2.0));
        assertEquals(15, 16 - 1, "the range burst on the launch tick + 15");
    }

    /** Three bursts 10 apart on one body: seven burns of 125, +20 to +140, 875; the end 3000 - 3 x 250 - 875 = 1375. */
    @Test
    void solarFlare_threeBurstsBurnSevenTimesAndEndAtThirteenSeventyFive() {
        var b3 = ScorchBurnsReadOrder.burns(3000, 125, 0, 10, 20);
        assertEquals(every20(20, 140), b3.offsets());
        assertEquals(125.0, b3.each(), 1e-9);
        assertEquals(875.0, b3.total(), 1e-9);
        assertEquals(1375.0, 3000 - 3 * 250 - b3.total(), 1e-9);
    }

    // ------------------------------------------------------------------ the coordinate tables, against the pad

    /** A box in blocks from F: x from..to, z from..to. */
    record Box(double x1, double x2, double z1, double z2) {
        boolean holds(double x, double z) {
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }

        boolean holdsDisc(double x, double z, double r) {
            return holds(x - r, z - r) && holds(x + r, z + r);
        }
    }

    /** The pad's interior (columns -2..45, -13..13 from his block) and each gate's preflight, as block edges from F. */
    static final Box INTERIOR = new Box(-2.5, 45.5, -13.5, 13.5);
    static final Box SMOLDER_PREFLIGHT = new Box(-2.5, 30.5, -12.5, 12.5);   // "-2 -12 30 12"
    static final Box FLARE_PREFLIGHT = new Box(-2.5, 45.5, -13.5, 13.5);     // "-2 -13 45 13"

    @Test
    void everyStagedBodyAndEveryReachLiesInsideTheInteriorAndItsPreflight() {
        // Smolder-2, from F: G = (10, 0). Bodies, then each ring's whole 12 (from G and from G + 8), WA6R's Ignite (4).
        double[][] smolderBodies = {{13, 0}, {16, 0}, {21.5, 0}, {22.5, 0}, {10, 4}, {10, -11}, {29, 0}, {6, -8}, {7.5, -8}};
        for (double[] b : smolderBodies) {
            assertTrue(INTERIOR.holds(b[0], b[1]) && SMOLDER_PREFLIGHT.holds(b[0], b[1]), b[0] + "," + b[1]);
        }
        for (double[] o : new double[][] {{10, 0}, {18, 0}}) {
            assertTrue(INTERIOR.holdsDisc(o[0], o[1], 12) && SMOLDER_PREFLIGHT.holdsDisc(o[0], o[1], 12), "ring at " + o[0]);
        }
        assertTrue(SMOLDER_PREFLIGHT.holdsDisc(6, -8, Ignite.RADIUS), "WA6R's blast");
        // Solar-flare-2, from F: H = (5, 0). Bodies, then each burst's 6 (range, turned, mob, block, SF11's wall).
        double[][] flareBodies = {{42.9, 0}, {43.1, 0}, {13, 0}, {18, 0}, {12, -1.5}, {35, -2}, {39, 2}, {8, 1.5}};
        for (double[] b : flareBodies) {
            assertTrue(INTERIOR.holds(b[0], b[1]) && FLARE_PREFLIGHT.holds(b[0], b[1]), b[0] + "," + b[1]);
        }
        Vec3 turnedRight = dir(-80, 0).scale(32);
        Vec3 turnedLeft = dir(-100, 0).scale(32);
        double[][] bursts = {{37, 0}, {5 + turnedRight.x(), turnedRight.z()}, {5 + turnedLeft.x(), turnedLeft.z()},
                {13, 0}, {10.5, 0}, {6.5, 0}};
        for (double[] o : bursts) {
            assertTrue(INTERIOR.holdsDisc(o[0], o[1], 6) && FLARE_PREFLIGHT.holdsDisc(o[0], o[1], 6),
                    "burst at " + o[0] + "," + o[1]);
        }
        assertEquals(43.0, 37 + 6, 0.0, "the farthest reach east: 32 + 6 from H, F + 43, inside 45.5");
    }

    // ------------------------------------------------------------------ X6 (c): mana with the regen term named

    @Test
    void mana_theRegenTermIsFiveHundredthsATickAndTwoTicksGiveTwentyPointOne() throws IOException {
        String plugin = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/RpgPlugin.java"),
                StandardCharsets.UTF_8);
        assertTrue(plugin.contains("private static final double MAX_MANA = 100.0;"));
        assertTrue(plugin.contains("private static final int MANA_REFILL_SECONDS = 100;"));
        assertTrue(plugin.contains("private static final double MANA_PER_TICK = MAX_MANA / (MANA_REFILL_SECONDS * 20);"));
        double perTick = 100.0 / (100 * 20);
        long[] now = {1000};
        var pool = new ResourcePool(() -> now[0], 100.0, perTick);
        UUID him = UUID.randomUUID();
        assertTrue(pool.tryConsume(him, "mana", 80));
        assertEquals("20.000", String.format(java.util.Locale.ROOT, "%.3f", pool.current(him, "mana")), "P: the press");
        now[0] += 2;
        assertEquals("20.100", String.format(java.util.Locale.ROOT, "%.3f", pool.current(him, "mana")),
                "P + 2: the refused press reads 20.000 + 2 x 0.05");
        now[0] += 20;
        assertEquals("21.100", String.format(java.util.Locale.ROOT, "%.3f", pool.current(him, "mana")), "P + 22");
    }

    // ------------------------------------------------------------------ WA6R clause 1: the blast on k1w

    @Test
    void wa6r_igniteBlastIsSixOnAThousand() {
        assertEquals(6.0, Ignite.DAMAGE, 1e-12);
        assertEquals(4.0, Ignite.RADIUS, 1e-12);
        assertEquals(994.0, ZOMBIE_1000 - Ignite.DAMAGE * (1 + 0 / 100.0), 1e-9);
        assertEquals(1.5, PulseFront.horizontal(new Vec3(6, 0, -8), new Vec3(7.5, 0, -8)), 1e-12, "k1w 1.5 from k1");
    }
}
