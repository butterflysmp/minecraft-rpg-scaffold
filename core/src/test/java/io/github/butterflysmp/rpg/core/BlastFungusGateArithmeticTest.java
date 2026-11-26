package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.ClusterSplit;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.progression.LevelBonus;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY FIGURE GATE-blast-fungus.md PREDICTS, EXECUTED (2026-11-26). Coordinates are relative to his FEET, block-centred on
 * the pad (the rows' {@code execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0}): +x east (his aim at yaw -90),
 * +z south (his right, READ: {@code ViewAim}'s right vector at yaw -90), the pad's floor at y 0, his eye at 1.62. The shot
 * is blast_fungus.yml's and the enchant cluster_bombs.yml's, copied (the content tests pin the files). Ticks count from the
 * THROW; the press is one tick earlier (READ, {@code WeaponFire}'s {@code onRegion} hop).
 *
 * <p><b>The blast is a BOX (READ):</b> {@code PaperCombatWorld.combatantsNear} is {@code getNearbyEntities(c, r, r, r)}, so
 * a zombie (half-width 0.3, height 1.95, READ in the rows' own probes' {@code d}) is inside a blast of radius r when its
 * box meets the cube {@code c +/- r} on every axis. {@link #inBlast} is that test; the FakeWorld's sphere is not used for
 * membership here.
 */
class BlastFungusGateArithmeticTest {

    static final Vec3 EYE = new Vec3(0, 1.62, 0);
    static final Vec3 EAST = new Vec3(1, 0, 0);
    static final Vec3 SOUTH = new Vec3(0, 0, 1);
    static final EnchantBehaviour.ClusterBombs BOMBS = new EnchantBehaviour.ClusterBombs("blast_fungus",
            List.of(55.0, 80.0, 105.0), "nature", 5, "red_mushroom_block", 3, 2.5, 20, 0.25);

    static AbilityDefinition fungus() {
        return new AbilityDefinition("blast_fungus/right_click", "Fungal Barrage", "nature", 140, new ResourceCost("mana", 30),
                new CastSpec.Projectile(0.7, 0.04, 100, null, "nether_wart_block", null, null,
                        new CastSpec.Spread(3, 25, 0), 40),
                List.of(new EffectSpec.Visual("fungus_blast"),
                        new EffectSpec.Burst(3, List.of(new EffectSpec.Damage(55, "nature")))));
    }

    record Run(FakeWorld world, FakeWorld.Dummy him) {}

    static Run throwAt(Vec3 direction, int clusterLevel, int score, FakeWorld.Dummy... others) {
        var world = new FakeWorld();
        world.floorY = 0;
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.triggerScore = score;
        if (clusterLevel > 0) him.behaviours = List.of(new ActiveBehaviour("cluster_bombs", clusterLevel, BOMBS));
        world.entities.add(him);
        world.entities.addAll(List.of(others));
        new CastExecutor(world).execute(new AbilityService.CastResult.Success(fungus(), him.snapshot(),
                new Aim(EYE, direction, SOUTH), true));
        return new Run(world, him);
    }

    static List<FakeWorld.FusedLine> lines(Run r, String event) {
        return r.world().fusedLines.stream().filter(l -> l.event().equals(event)).toList();
    }

    /** Server box semantics: a zombie standing at {@code feet} is inside a blast of {@code radius} centred at {@code c}. */
    static boolean inBlast(Vec3 feet, Vec3 c, double radius) {
        return Math.abs(feet.x() - c.x()) <= radius + 0.3 && Math.abs(feet.z() - c.z()) <= radius + 0.3
                && feet.y() <= c.y() + radius && feet.y() + 1.95 >= c.y() - radius;
    }

    static String fmt(Vec3 v) {
        return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", v.x(), v.y(), v.z());
    }

    /** BF3: the three rest points, the rest tick, and where each staged zombie stands against them. */
    @Test
    void theThrowRestsAtThreePointsAndTheZombiesAreWhereTheRowsSay() {
        Run r = throwAt(EAST, 0, 100);
        r.world().advanceTicks(39);
        List<FakeWorld.FusedLine> rests = lines(r, "rest");
        assertEquals(3, rests.size());
        List<Vec3> at = new ArrayList<>();
        for (var l : rests) {
            at.add(l.at());
            System.out.println("BF GATE rest " + fmt(l.at()) + " tick=" + l.tick());
            assertEquals(9, l.tick(), "each rests on the step that runs 9 after the throw");
        }
        Vec3 centre = at.stream().filter(v -> Math.abs(v.z()) < 1e-9).findFirst().orElseThrow();
        assertEquals(6.65, centre.x(), 1e-9, "x = 0.7 x 9.5: the floor crossed halfway through the 10th step");
        Vec3 all = new Vec3(7.5, 0, 0), one = new Vec3(6.0, 0, 5.5), none = new Vec3(6.0, 0, -6.5);
        for (Vec3 c : at) assertTrue(inBlast(all, c, 3), "ALL is inside " + fmt(c));
        assertEquals(1, at.stream().filter(c -> inBlast(one, c, 3)).count(), "ONE is inside exactly one");
        assertEquals(0, at.stream().filter(c -> inBlast(none, c, 3)).count(), "NONE is inside none");
        double nearestNone = at.stream().mapToDouble(c -> Math.abs(none.z() - c.z())).min().orElseThrow();
        System.out.println("BF GATE none: |dz| to the nearest centre " + String.format(Locale.ROOT, "%.3f", nearestNone));
        r.world().advanceTicks(1);
        assertEquals(3, lines(r, "detonate").size());
        for (var l : lines(r, "detonate")) assertEquals(40, l.tick());
    }

    /** BF4: a zombie at x 4 in the centre chunk's path; its box's front face (3.7) is met on the 6th step (runs at +5). */
    @Test
    void theEarlyDetonationIsAtTheZombiesFrontFaceOnTheSixthStep() {
        double x = 0, y = 1.62, vy = 0;
        int step = -1;
        double metY = Double.NaN;
        for (int n = 0; n < 40; n++) {
            double nx = x + 0.7, ny = y + vy;
            if (nx >= 3.7 && x < 3.7) {
                metY = y + (ny - y) * (3.7 - x) / 0.7;
                step = n;
                break;
            }
            x = nx;
            y = ny;
            vy -= 0.04;
        }
        System.out.println("BF GATE touch step=" + step + " y=" + String.format(Locale.ROOT, "%.3f", metY));
        assertEquals(5, step, "the step that runs 5 ticks after the throw");
        assertTrue(metY > 0 && metY < 1.95, "inside the box's height");
        // Its side chunks still rest at the same two points and the zombie at x 4 is inside both: 55 x 3 in all.
        Vec3 inPath = new Vec3(4, 0, 0);
        Run r = throwAt(EAST, 0, 100);
        r.world().advanceTicks(39);
        long sidesIn = lines(r, "rest").stream().filter(l -> Math.abs(l.at().z()) > 1e-9)
                .filter(l -> inBlast(inPath, l.at(), 3)).count();
        assertEquals(2, sidesIn);
    }

    /** BF5 and BF6: straight down, the three rest at his feet's ring; SAFETY executed: a blast there deals him NOTHING. */
    @Test
    void straightDownTheyRestAtHisFeetAndHeTakesNothing() {
        var other = new FakeWorld.Dummy(new Vec3(0.5, 0, 0));
        other.player = true;
        var wolf = new FakeWorld.Dummy(new Vec3(-1, 0, 0));
        wolf.summon = true;
        var zombie = new FakeWorld.Dummy(new Vec3(1.5, 0, 0));
        zombie.health = 1000;
        Run r = throwAt(new Vec3(0, -1, 0), 0, 100, other, wolf, zombie);
        r.world().advanceTicks(41);
        for (var l : lines(r, "rest")) {
            System.out.println("BF GATE down rest " + fmt(l.at()) + " tick=" + l.tick());
            assertTrue(inBlast(Vec3.ZERO, l.at(), 3) && inBlast(new Vec3(1.5, 0, 0), l.at(), 3)
                    && inBlast(new Vec3(-1, 0, 0), l.at(), 3), "his feet, the zombie and the wolf are inside each");
        }
        assertEquals(3, lines(r, "detonate").size());
        assertEquals(100.0, r.him().health, 1e-9, "S-1, EXECUTED: three blasts at distance <1 from him deal him 0.000");
        assertEquals(100.0, other.health, 1e-9, "another player: 0.000");
        assertEquals(100.0, wolf.health, 1e-9, "a summon: untouched");
        assertEquals(1000 - 165.0, zombie.health, 1e-9, "the mob beside them: 55 x 3");
    }

    /** CB rows: the big chunk's rest, the triangle, and each zombie against the big blast (5) and the splits (3). */
    @Test
    void clusterBombsRestPointsAndMemberships() {
        Run r = throwAt(EAST, 2, 100);
        r.world().advanceTicks(59);
        List<FakeWorld.FusedLine> rests = lines(r, "rest");
        assertEquals(4, rests.size(), "the big one, then three");
        Vec3 big = rests.getFirst().at();
        assertEquals(6.65, big.x(), 1e-9);
        assertEquals(9, rests.getFirst().tick());
        List<Vec3> splits = new ArrayList<>();
        for (var l : rests.subList(1, 4)) {
            splits.add(l.at());
            System.out.println("BF GATE split rest " + fmt(l.at()) + " tick=" + l.tick());
        }
        List<Vec3> dirs = ClusterSplit.directions(EAST, 3);
        for (int i = 0; i < 3; i++) {
            assertTrue(splits.get(i).distanceSquared(big.add(dirs.get(i).scale(2.5))) < 1e-18, "split " + i);
        }
        Vec3 in45 = new Vec3(6.65, 0, 4.5), out55 = new Vec3(6.65, 0, -5.5), beyond = new Vec3(10.15, 0, 0);
        assertTrue(inBlast(in45, big, 5), "4.5: inside 5");
        assertFalse(inBlast(out55, big, 5), "5.5: outside 5");
        assertTrue(inBlast(beyond, big, 5), "10.15 (3.5 east of the centre): inside 5");
        int beyondSplits = 0, in45Splits = 0, out55Splits = 0;
        for (Vec3 s : splits) {
            if (inBlast(beyond, s, 3)) beyondSplits++;
            if (inBlast(in45, s, 3)) in45Splits++;
            if (inBlast(out55, s, 3)) out55Splits++;
        }
        System.out.println("BF GATE splits containing: beyond=" + beyondSplits + " in45=" + in45Splits + " out55=" + out55Splits);
        assertEquals(1, beyondSplits, "the zombie 1.0 past the heading's point: inside that split only");
        assertEquals(1, in45Splits);
        assertEquals(0, out55Splits);
        r.world().advanceTicks(1);
        assertEquals(4, lines(r, "detonate").size(), "the big one at 40, three at 60");
        assertEquals(60, lines(r, "detonate").getLast().tick());
    }

    /** Every damage figure the rows predict, through HitDamage with the rows' staging (level 1: 0 bonus, EXECUTED). */
    @Test
    void theSums() {
        double level = LevelBonus.at(1).weaponDamage();
        assertEquals(0.0, level, "level 1 adds nothing to a weapon hit");
        assertEquals(55.0, sent(55, 100, level));
        assertEquals(80.0, sent(80, 100, level));
        assertEquals(105.0, sent(105, 100, level));
        assertEquals(160.0, sent(80, 200, level));
        assertEquals(110.0, sent(55, 200, level));
        assertEquals(110.0, HitDamage.dealt(sent(55, 100, level), 1.0, 2.0), "a crit doubles");
    }

    static double sent(double amount, int score, double level) {
        return HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(amount, score), 0.0, 0.0 + level), 1.0, 1.0);
    }
}
