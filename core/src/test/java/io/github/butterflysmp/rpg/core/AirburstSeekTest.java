package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BEN'S AIRBURST, 2026-11-23: "I want the Airburst Flames to seek as well" and, asked what his second change meant, "B":
 * "Split after 6 blocks into 3 pieces; pieces keep their 14." Through the real cast path on the Living Lantern's own shot
 * (copied from {@code living_lantern.yml}: 0.35 a tick, lifetime 80, homing 0.45 / 0 / 7, 45 fire) and the enchant's
 * numbers as committed: travel 6, three bursts 12 degrees apart at 0.7 a tick for 14 blocks, 25 / 30 / 35, and the bursts'
 * own homing block (SEAT PLACEHOLDER 2026-11-23: the lantern's 0.45 / 0 / 7).
 */
class AirburstSeekTest {

    private static final CastSpec.Homing SEEK = new CastSpec.Homing(0.45, 0, 7);
    private static final EnchantBehaviour.Airburst SEEKING = burst(SEEK);
    private static final EnchantBehaviour.Airburst STRAIGHT = burst(null);
    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    private static EnchantBehaviour.Airburst burst(CastSpec.Homing homing) {
        return new EnchantBehaviour.Airburst(6, 3, 12, 0.7, 14, List.of(25.0, 30.0, 35.0), "fire", homing);
    }

    private static AbilityDefinition lantern() {
        return new AbilityDefinition("living_lantern/right_click", "Lantern's Breath", "fire", 0, ResourceCost.FREE,
                new CastSpec.Projectile(0.35, 0.0, 80, null, null, new CastSpec.Homing(0.45, 0, 7)),
                List.of(new EffectSpec.Visual("lantern_hit"), new EffectSpec.Damage(45, "fire")));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    private static Stage stage(EnchantBehaviour.Airburst burst, int level, FakeWorld.Dummy... mobs) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.behaviours = List.of(new ActiveBehaviour("airburst_flames", level, burst));
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
     * 6 / 0.35 is 17.142857142857142 and 18 x 0.35 is 6.3 in doubles (both EXECUTED, jshell, 2026-11-23): the split is the
     * 18th step, 17 ticks after the press (the first step runs on the press's own tick), at 6.3 blocks of travel.
     */
    @Test
    void theSplitIsTheEighteenthStepAtSixPointThreeBlocks() {
        assertEquals(18, EnchantBehaviour.Airburst.ticksToTravel(6, 0.35));
        Stage s = stage(SEEKING, 2);
        fire(s);
        assertEquals(1, s.world().splits.size(), "one split, with no mob anywhere near");
        var split = s.world().splits.get(0);
        assertEquals(6.3, split.at().x(), 1e-9, "at 6.3 blocks: the first step at or beyond 6");
        assertEquals(0.0, split.at().z(), 1e-9);
        assertEquals(3, split.bursts());
        assertEquals(17, split.tick(), "the 18th step, 17 ticks after the press");
    }

    /**
     * THE BURSTS SEEK: a mob 14 east and 2.5 south. After the split at 6.3 the centre burst flies along z = 0 and the side
     * bursts at 12 degrees reach z = +-(14 - 6.3) x tan 12 = +-1.637 at x 14: every straight line passes it by more than
     * 0.86, well outside the 0.3 hit radius. It is never within 7 of the lantern's shot before the split (8.1 at the split
     * point), so the shot itself never turns for it. With the bursts' homing block, at least one lands its 30 (level II);
     * how many land is not this row's claim, only that each landing is exactly the level's 30.
     */
    @Test
    void aBurstSteersToAMobItsStraightLineMisses() {
        var mob = new FakeWorld.Dummy(new Vec3(14, 0, 2.5));
        Stage s = stage(SEEKING, 2, mob);
        fire(s);
        assertEquals(1, s.world().splits.size(), "the shot split: the mob never drew it");
        List<Double> landed = sent(s);
        assertTrue(!landed.isEmpty(), "a seeking burst reached the mob off every straight line");
        assertTrue(landed.stream().allMatch(v -> v == 30.0), "each landing is level II's 30, never the 45: " + landed);
    }

    /** THE CONTROL: the same mob, the same bursts with NO homing block, and every burst flies past it. */
    @Test
    void anUnhomingBurstPassesTheSameMob() {
        var mob = new FakeWorld.Dummy(new Vec3(14, 0, 2.5));
        Stage s = stage(STRAIGHT, 2, mob);
        fire(s);
        assertEquals(1, s.world().splits.size());
        assertEquals(List.of(), sent(s), "straight bursts miss it, as before Ben's change");
    }

    /** "Doesn't split if it hits something first": a mob 3 east takes the 45 and nothing splits. */
    @Test
    void aMobBeforeSixTakesTheFortyFiveAndNothingSplits() {
        var mob = new FakeWorld.Dummy(new Vec3(3, 0, 0));
        Stage s = stage(SEEKING, 2, mob);
        fire(s);
        assertEquals(List.of(), s.world().splits, "no split");
        assertEquals(List.of(45.0), sent(s));
    }

    /** Each level's burst damage, through the same path: 25, 30 and 35, each exact. */
    @Test
    void eachLevelLandsItsOwnBurstDamage() {
        for (int level = 1; level <= 3; level++) {
            var mob = new FakeWorld.Dummy(new Vec3(14, 0, 0));
            Stage s = stage(SEEKING, level, mob);
            fire(s);
            double want = List.of(25.0, 30.0, 35.0).get(level - 1);
            assertTrue(!sent(s).isEmpty(), "level " + level + " landed");
            assertTrue(sent(s).stream().allMatch(v -> v == want), "level " + level + ": " + sent(s));
        }
    }
}
