package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.SplitBasis;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AIRBURST FLAMES THROUGH THE REAL CAST PATH (the seat's brief of 2026-11-23, PR E), on the Living Lantern's own shot
 * (copied from {@code living_lantern.yml}: speed 0.35, gravity 0, lifetime 80, homing 0.45 / 0 / 7, 45 fire) and the
 * enchant's numbers (Ben's 14 and 14 blocks, three bursts, 25 / 30 / 35; the seat's 0.7 a tick and 12 degrees).
 */
class AirburstTest {

    private static final EnchantBehaviour.Airburst BURST = new EnchantBehaviour.Airburst(14, 3, 12, 0.7, 14,
            List.of(25.0, 30.0, 35.0), "fire");
    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    private static AbilityDefinition lantern() {
        return new AbilityDefinition("living_lantern", "Lantern's Breath", "fire", 0, ResourceCost.FREE,
                new CastSpec.Projectile(0.35, 0.0, 80, null, null, new CastSpec.Homing(0.45, 0, 7)),
                List.of(new EffectSpec.Visual("lantern_hit"), new EffectSpec.Damage(45, "fire")));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    private static Stage stage(int level, FakeWorld.Dummy... mobs) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        if (level > 0) him.behaviours = List.of(new ActiveBehaviour("airburst_flames", level, BURST));
        world.entities.add(him);
        world.entities.addAll(List.of(mobs));
        return new Stage(world, him);
    }

    private static void fire(Stage s) {
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(lantern(), s.him().snapshot(), EAST, true));
        s.world().advanceTicks(120);
    }

    private static List<Double> sent(Stage s) {
        return s.world().traced.stream().map(TracedHit::sent).toList();
    }

    /**
     * 14 blocks at 0.35 is 40 ticks, and 14 at 0.7 is 20 (both quotients exact in doubles, EXECUTED). The epsilon's own
     * row: 2.1 / 0.7 is 3.0000000000000004 in doubles (EXECUTED), so a bare ceil would say 4.
     */
    @Test
    void theTravelTicksAreFortyAndTwenty() {
        assertEquals(40, EnchantBehaviour.Airburst.ticksToTravel(14, 0.35));
        assertEquals(20, EnchantBehaviour.Airburst.ticksToTravel(14, 0.7));
        assertEquals(80, EnchantBehaviour.Airburst.ticksToTravel(28, 0.35), "the lantern's own reach, as a control");
        assertEquals(3, EnchantBehaviour.Airburst.ticksToTravel(2.1, 0.7), "2.1 / 0.7 is a hair above 3 in doubles");
    }

    /** The gate's row: a mob 20 out. No 45; one split at 14 blocks, 39 ticks after the press (the 40th step); the centre burst lands 30 (level II). */
    @Test
    void aMobTwentyOutTakesTheCentreBurstAndNeverTheShot() {
        var mob = new FakeWorld.Dummy(new Vec3(20, 0, 0));
        Stage s = stage(2, mob);
        fire(s);
        assertEquals(1, s.world().splits.size(), "one split");
        var split = s.world().splits.get(0);
        assertEquals(14.0, split.at().x(), 1e-9, "at 14 blocks of travel");
        assertEquals(0.0, split.at().z(), 1e-9);
        assertEquals(3, split.bursts());
        assertEquals(39, split.tick(), "the 40th step: the first runs on the press's own tick, so 39 ticks after it");
        assertEquals(List.of(30.0), sent(s), "the centre burst's 30, never the 45; the side bursts pass it");
        assertEquals(70.0, mob.health, 1e-9);
    }

    /** "Doesn't split if it hits something first": a mob 6 out takes the 45 and nothing splits. */
    @Test
    void aMobSixOutTakesTheFortyFiveAndNothingSplits() {
        var mob = new FakeWorld.Dummy(new Vec3(6, 0, 0));
        Stage s = stage(2, mob);
        fire(s);
        assertEquals(List.of(45.0), sent(s));
        assertEquals(0, s.world().splits.size());
    }

    /** A BLOCK hit before 14 blocks is "something" too: no split. */
    @Test
    void aWallBeforeFourteenBlocksMeansNoSplit() {
        Stage s = stage(2);
        s.world().wallX = 10;
        fire(s);
        assertEquals(0, s.world().splits.size());
    }

    /** Level I against level III: 25 against 35 on the centre burst. */
    @Test
    void levelOneAgainstLevelThree() {
        var one = new FakeWorld.Dummy(new Vec3(20, 0, 0));
        var three = new FakeWorld.Dummy(new Vec3(20, 0, 0));
        Stage a = stage(1, one);
        Stage b = stage(3, three);
        fire(a);
        fire(b);
        assertEquals(List.of(25.0), sent(a));
        assertEquals(List.of(35.0), sent(b));
    }

    /** The side bursts fly 12 degrees off the centre, sideways, and each lands its own hit on a mob in its path. */
    @Test
    void theThreeBurstsFanTwelveDegreesSideways() {
        double r = Math.toRadians(12);
        var centre = new FakeWorld.Dummy(new Vec3(14 + 7, 0, 0));
        var right = new FakeWorld.Dummy(new Vec3(14 + 7 * Math.cos(r), 0, 7 * Math.sin(r)));
        var left = new FakeWorld.Dummy(new Vec3(14 + 7 * Math.cos(r), 0, -7 * Math.sin(r)));
        Stage s = stage(2, centre, right, left);
        fire(s);
        assertEquals(1, s.world().splits.size(), "the shot reached 14: none of the three is within 7 of its path before");
        assertEquals(70.0, centre.health, 1e-9);
        assertEquals(70.0, right.health, 1e-9, "12 degrees to the right (south, travelling east)");
        assertEquals(70.0, left.health, 1e-9);
    }

    /** A burst travels a further 14 blocks and no more: a mob 28.5 along takes nothing, and nothing splits twice. */
    @Test
    void aBurstTravelsFourteenMoreAndNeverSplitsAgain() {
        var past = new FakeWorld.Dummy(new Vec3(28.5, 0, 0));
        Stage s = stage(2, past);
        fire(s);
        assertEquals(1, s.world().splits.size(), "the bursts never split");
        assertEquals(100.0, past.health, 1e-9, "28.5 is past 14 + 14");
    }

    /** The control: without the enchant the lantern flies its 28 blocks and lands its 45 on the mob 20 out. */
    @Test
    void withoutTheEnchantTheShotIsTheLanterns() {
        var mob = new FakeWorld.Dummy(new Vec3(20, 0, 0));
        Stage s = stage(0, mob);
        fire(s);
        assertEquals(List.of(45.0), sent(s));
        assertEquals(0, s.world().splits.size());
    }

    /** "Sideways" is horizontal; straight up or down it is the right of the aim the shot was fired with. */
    @Test
    void sidewaysIsHorizontalAndFallsBackToTheAimsRightWhenVertical() {
        assertEquals(new Vec3(0, 0, 1), SplitBasis.sidewaysOf(new Vec3(1, 0, 0), new Vec3(9, 9, 9)), "east: south");
        assertEquals(new Vec3(0, 0, 1), SplitBasis.sidewaysOf(new Vec3(1, 1, 0).normalize(), new Vec3(9, 9, 9)),
                "a climb keeps the horizontal answer");
        assertEquals(new Vec3(-1, 0, 0), SplitBasis.sidewaysOf(new Vec3(0, 1, 0), new Vec3(-1, 0, 0)),
                "straight up: the aim's right");
        assertEquals(new Vec3(-1, 0, 0), SplitBasis.sidewaysOf(new Vec3(0, -1, 0), new Vec3(-2, 0, 0)), "and straight down");
    }
}
