package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.build.AspectLore;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.Defense;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.combat.Ignite;
import io.github.butterflysmp.rpg.core.combat.PulseFront;
import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.progression.LevelBonus;
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
 * GATE-smolder's predictions, EXECUTED through the real functions before any boot (S-1; "a damage prediction names every
 * term"). Every figure the gate file writes beside a row is asserted here, so the gate cannot quote a number this class
 * did not produce. F is his stand (block-centred, feet), east +x, south +z.
 *
 * <p><b>THE BURN'S PHASE, READ by javap on the pinned 26.1.2 server (GATE-smolder.md, THE CLOCK).</b> A ring's hit is a
 * region task, which on Paper is the GLOBAL region scheduler ({@code FallbackRegionScheduler} delegates to it), and
 * {@code MinecraftServer.tickChildren} runs that BEFORE every {@code EntityScheduler.executeTick}. A burn booked from that
 * phase ({@code onEntityLater(20)}) is keyed {@code tickCount + 20}, and {@code executeTick} increments {@code tickCount}
 * before it looks, so the FIRST burn lands 19 server ticks after the hit; every re-arm after it is booked inside
 * {@code executeTick} and lands 20 later. The fake clock below books every delay exactly, so the store is run with
 * the first hit at fake 0 and each later hit ONE TICK LATER than its real offset: a burn at fake {@code f} is a burn at
 * real {@code f - 1}, and a real hit at {@code +60} (after the real burn at {@code +59}) is the fake hit at {@code 61}.
 *
 * <p><b>WHAT THE MAPPING DOES AND DOES NOT CARRY, MEASURED.</b> The fake runs a due burn BEFORE an application in the same
 * tick, so a later hit at fake {@code +60} gives the same burns as at {@code +61}: mutating the {@code + 1} to
 * {@code + 0} (MUTPHASE) left every row green, an equivalent mutation. What the arithmetic DOES depend on is the burn
 * landing before the hit: moving each later hit one tick EARLY ({@code - 1}, MUTPHASE2, the hit-first order) reddens
 * the fifteen-, twelve- and nine-burn rows. That order is the READ above.
 */
class SmolderGateArithmeticTest {

    @TempDir Path dir;

    static final double ZOMBIE_3000 = MobScaling.maxHealth(600.0, false, true, 100);
    static final double ZOMBIE_1000 = MobScaling.maxHealth(200.0, false, true, 100);
    static final double ZOMBIE_10 = MobScaling.maxHealth(2.0, false, true, 100);

    /** Real burn offsets (from the first hit) and their amounts, for hits at these real offsets. */
    record Burns(List<Long> offsets, double each, double total) {}

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

    private static List<Long> every20(long from, long to) {
        List<Long> out = new ArrayList<>();
        for (long t = from; t <= to; t += 20) out.add(t);
        return out;
    }

    // ------------------------------------------------------------------ the hit

    @Test
    void aSmolderHitIsOneHundredSixtyUncritAndThreeHundredTwentyCritAndItsCapIsEightyEitherWay() {
        // His hand holds the stone: no score stamp, so GearScore.orAbsent's ABSENT (100). No enchant, no class damage,
        // no level term (a stone cast is not a weapon hit), charge 1.0.
        assertEquals(100, GearScore.ABSENT);
        double base = HitDamage.hitBase(GearScore.scaledDamage(160, GearScore.ABSENT), 0.0, 0.0);
        double normal = Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.99);
        double crit = Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.0);
        assertEquals(1.0, normal);
        assertEquals(2.0, crit);
        assertEquals(160.0, HitDamage.dealt(base, 1.0, normal), 1e-9);
        assertEquals(320.0, HitDamage.dealt(base, 1.0, crit), 1e-9);
        assertEquals(80.0, (HitDamage.dealt(base, 1.0, crit) / crit) * Scorch.CAP_FRACTION, 1e-9, "a crit does not raise the cap");
        assertEquals(160.0, Defense.applyDefense(160.0, 0.0), "a mob's defence is 0");
        assertEquals(0.15, Crit.BASE_CHANCE, "a crit CAN occur: 15% per cast, frozen across its four pulses");
    }

    @Test
    void aFlintStaffBoltIsTwentyUncritAndFortyCritAtLevelOneAndItsCapIsTen() {
        double level = LevelBonus.at(1).weaponDamage();
        assertEquals(0.0, level, "level 1's weapon damage bonus");
        double base = HitDamage.hitBase(GearScore.scaledDamage(20, 100), 0.0, 0.0 + level);
        assertEquals(20.0, HitDamage.dealt(base, 1.0, 1.0), 1e-9);
        assertEquals(40.0, HitDamage.dealt(base, 1.0, 2.0), 1e-9);
        assertEquals(10.0, 20.0 * Scorch.CAP_FRACTION, 1e-9);
    }

    @Test
    void theStagedZombiesAreThreeThousandOneThousandAndTen() {
        assertEquals(3000.0, ZOMBIE_3000, 1e-9);
        assertEquals(1000.0, ZOMBIE_1000, 1e-9);
        assertEquals(10.0, ZOMBIE_10, 1e-9);
    }

    // ------------------------------------------------------------------ when the ring reaches each body

    @Test
    void reachTicksForEveryStagedDistance() {
        Vec3 f = new Vec3(0, 0, 0);
        assertEquals(8, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(3, 0, 0)), 12.0, 30), "SM2 a3");
        assertEquals(15, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(6, 0, 0)), 12.0, 30), "a6, b6, d6");
        assertEquals(29, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(11.5, 0, 0)), 12.0, 30), "a11");
        assertEquals(-1, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(12.5, 0, 0)), 12.0, 30), "a12 never");
        assertEquals(10, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(0, 0, 4)), 12.0, 30), "the wolf's distance (refused by the table)");
        // SM4: c11 from F, and from his moved feet F+8; c19 from F, and from F+8.
        Vec3 moved = new Vec3(8, 0, 0);
        assertEquals(28, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(0, 0, -11)), 12.0, 30), "c11, pulse 1");
        assertEquals(-1, PulseFront.reachTick(PulseFront.horizontal(moved, new Vec3(0, 0, -11)), 12.0, 30), "c11 from F+8: 13.601");
        assertEquals(13.601, PulseFront.horizontal(moved, new Vec3(0, 0, -11)), 5e-4);
        assertEquals(-1, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(19, 0, 0)), 12.0, 30), "c19, pulse 1");
        assertEquals(28, PulseFront.reachTick(PulseFront.horizontal(moved, new Vec3(19, 0, 0)), 12.0, 30), "c19 from F+8");
        // WA3: u in the water cell at F+(6, 0, -3).
        assertEquals(6.708, PulseFront.horizontal(f, new Vec3(6, 0, -3)), 5e-4);
        assertEquals(17, PulseFront.reachTick(PulseFront.horizontal(f, new Vec3(6, 0, -3)), 12.0, 30), "u");
    }

    // ------------------------------------------------------------------ the burns

    @Test
    void fourHitsSixtyApartBurnFifteenTimesAtEightyFromPlusNineteenToPlusTwoNinetyNine() {
        Burns b = burns(ZOMBIE_3000, 80.0, 0, 60, 120, 180);
        assertEquals(every20(19, 299), b.offsets());
        assertEquals(15, b.offsets().size());
        assertEquals(80.0, b.each(), 1e-9, "min(5% of 3000 = 150, 80)");
        assertEquals(1200.0, b.total(), 1e-9);
        assertEquals(1160.0, ZOMBIE_3000 - 4 * 160 - b.total(), 1e-9, "SM2/SM3 end, uncrit");
        assertEquals(520.0, ZOMBIE_3000 - 4 * 320 - b.total(), 1e-9, "SM2/SM3 end, crit: still alive");
    }

    @Test
    void sm4_c19ThreeHitsBurnTwelveTimes_c11OneHitBurnsSix() {
        Burns c19 = burns(ZOMBIE_3000, 80.0, 0, 60, 120);
        assertEquals(every20(19, 239), c19.offsets());
        assertEquals(1560.0, ZOMBIE_3000 - 3 * 160 - c19.total(), 1e-9);
        assertEquals(1080.0, ZOMBIE_3000 - 3 * 320 - c19.total(), 1e-9);
        Burns c11 = burns(ZOMBIE_3000, 80.0, 0);
        assertEquals(every20(19, 119), c11.offsets());
        assertEquals(2360.0, ZOMBIE_3000 - 160 - c11.total(), 1e-9);
        assertEquals(2200.0, ZOMBIE_3000 - 320 - c11.total(), 1e-9);
    }

    @Test
    void sm5_d6TwoHitsBurnNineTimes() {
        Burns d6 = burns(ZOMBIE_3000, 80.0, 0, 60);
        assertEquals(every20(19, 179), d6.offsets());
        assertEquals(1960.0, ZOMBIE_3000 - 2 * 160 - d6.total(), 1e-9);
        assertEquals(1640.0, ZOMBIE_3000 - 2 * 320 - d6.total(), 1e-9);
    }

    @Test
    void wa1_theDryControlBurnsSixTimesAtTen() {
        Burns w2 = burns(ZOMBIE_1000, 10.0, 0);
        assertEquals(every20(19, 119), w2.offsets());
        assertEquals(10.0, w2.each(), 1e-9, "min(5% of 1000 = 50, 10)");
        assertEquals(920.0, ZOMBIE_1000 - 20 - w2.total(), 1e-9);
        assertEquals(900.0, ZOMBIE_1000 - 40 - w2.total(), 1e-9);
    }

    @Test
    void sm3_afterTheFirstHit_andWa2_oneBurnThenDoused() {
        assertEquals(2840.0, ZOMBIE_3000 - 160, 1e-9);
        assertEquals(2680.0, ZOMBIE_3000 - 320, 1e-9);
        Burns one = burns(ZOMBIE_1000, 10.0, 0);
        assertEquals(19L, one.offsets().getFirst(), "its first burn, before the move at P+30");
        assertEquals(970.0, ZOMBIE_1000 - 20 - one.offsets().stream().filter(t -> t < 39).count() * 10.0, 1e-9);
        assertEquals(950.0, ZOMBIE_1000 - 40 - one.offsets().stream().filter(t -> t < 39).count() * 10.0, 1e-9);
    }

    @Test
    void wa3_theWetZombieTakesOnlyTheFourHits() {
        assertEquals(2360.0, ZOMBIE_3000 - 4 * 160, 1e-9);
        assertEquals(1720.0, ZOMBIE_3000 - 4 * 320, 1e-9);
    }

    @Test
    void wa6_aWitnessInABlastTakesSixAndTheKillsAreOneHit() {
        assertEquals(6.0, Ignite.DAMAGE * (1 + 0.0 / 100.0), 1e-9, "no Mana Burn stamp");
        assertEquals(994.0, ZOMBIE_1000 - Defense.applyDefense(Ignite.DAMAGE, 0.0), 1e-9);
        assertEquals(20, Ignite.DELAY_TICKS);
        assertEquals(4.0, Ignite.RADIUS);
        assertEquals(1.5, PulseFront.horizontal(new Vec3(6, 0, -8), new Vec3(7.5, 0, -8)), 1e-9);
        assertEquals(2.5, PulseFront.horizontal(new Vec3(6, 0, 0), new Vec3(6, 0, 2.5)), 1e-9);
        assertEquals(10.5, PulseFront.horizontal(new Vec3(6, 0, -8), new Vec3(6, 0, 2.5)), 1e-9, "outside k1's blast");
        assertEquals(5.5, PulseFront.horizontal(new Vec3(6, 0, 2.5), new Vec3(6, 0, 8)), 1e-9, "outside k3's blast");
        assertFalse(20.0 < ZOMBIE_10, "one uncrit bolt kills a 10");
    }

    // ------------------------------------------------------------------ D3: every ring on the pad

    /** The pad's interior, from his stand F (block-centred): columns x -2..45, z -13..13, so these edges. */
    static final double WEST = -2.5, EAST = 45.5, SIDE = 13.5;
    /** The setup's preflight, "-2 -12 30 12", as edges from F. */
    static final double PF_WEST = -2.5, PF_EAST = 30.5, PF_SIDE = 12.5;

    /**
     * THE SEAT'S D3 (2026-11-27): rows cast from F reached x = -12, outside the pad. Every Smolder row now stands at
     * G = F + 10 east, and SM4 and SM7 move him to G + 8. Each origin's whole reach (origin +/- 12 on x and z) lies inside
     * the interior AND the preflight box. The figures are the gate's coordinate table.
     */
    @Test
    void d3_everyRingFromEveryOriginLiesInsideTheInteriorAndThePreflight() {
        double[] origins = {10, 18};   // G, and G + 8 (SM4, SM7)
        for (double ox : origins) {
            assertTrue(ox - 12 >= WEST && ox - 12 >= PF_WEST, "west " + (ox - 12));
            assertTrue(ox + 12 <= EAST && ox + 12 <= PF_EAST, "east " + (ox + 12));
            assertTrue(12 <= SIDE && 12 <= PF_SIDE, "sides 12");
        }
        assertEquals(-2.0, 10 - 12, 0.0, "G's west reach");
        assertEquals(30.0, 18 + 12, 0.0, "G + 8's east reach");
        // Every staged body, from F: inside the preflight box.
        double[][] bodies = {{13, 0}, {16, 0}, {21.5, 0}, {22.5, 0}, {10, 4}, {16, 0}, {10, -11}, {29, 0}, {16, -3}};
        for (double[] b : bodies) {
            assertTrue(b[0] >= PF_WEST && b[0] <= PF_EAST && Math.abs(b[1]) <= PF_SIDE, b[0] + "," + b[1]);
        }
    }

    // ------------------------------------------------------------------ SAFETY

    @Test
    void safety_neitherThePulseNorAnIgniteBlastCanHarmAPlayer() {
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.PLAYER, false,
                MobHostility.PvpPath.PULSE), "PULSE: spared");
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.PLAYER, false,
                MobHostility.PvpPath.IGNITE_SPLASH), "IGNITE_SPLASH: spared");
        assertFalse(MobHostility.harms(MobHostility.ENGINE_ATTACKER, MobHostility.Side.SUMMON, false,
                MobHostility.PvpPath.PULSE), "his wolf: refused");
    }

    // ------------------------------------------------------------------ SM1: the Build screen's cell

    @Test
    void sm1_theCellsNumberLinesAreDamageCostAndCooldown() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/abilities/smolder.yml")) {
            assertNotNull(in);
            Files.write(dir.resolve("smolder.yml"), in.readAllBytes());
        }
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        AbilityDefinition def = new AbilityLoader(log).loadAll(dir.toFile()).find("smolder").orElseThrow();
        assertEquals(List.of(new AspectLore.Line("Damage", "160", null), new AspectLore.Line("Cost", "80", null),
                new AspectLore.Line("Cooldown", "300.0 s", null)), AspectLore.lines(def, def));
        // The frozen gate predicted (and read) "Four pulses, 3s apart, ..."; Ben retuned the interval on 2026-11-28 (M1, N7),
        // so this row now holds the shipped prose. The cell's number lines above are unchanged.
        assertEquals(List.of("Four pulses, 1.5s apart, burst from your feet.",
                "Each deals 160 fire damage within 12 blocks and scorches."), def.description());
    }
}
