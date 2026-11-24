package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.SpreadPattern;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SHATTER CAPS THROUGH THE REAL CAST PATH (the seat's brief of 2026-11-24). Ben: "Instead of shooting 4 cobble right away,
 * it shoots 1 stone that splits into 4 cobble after traveling 5 blocks. The damage of the Stone is 35/60/85" / "1. the
 * usual 40 2. their full reach 3. the same pattern." / the stone's damage "to follow with gearscore". The Breacher's own
 * shot, copied from {@code breacher.yml} after its reach change (speed 3.0, gravity 0, 6 ticks, cobblestone, the spread
 * 4 / 3 / 90, 40 kinetic and knockback 0.1); the stone the seat's 2.5 a tick and stone. His feet at the origin, facing
 * east, his right south: the stone splits on its 2nd step, at exactly 5.0 (EXECUTED: ceil(5 / 2.5) = 2, 2 x 2.5 = 5.0).
 */
class ShatterCapsTest {

    private static final EnchantBehaviour.Shatter CAPS = new EnchantBehaviour.Shatter("breacher", 5, 2.5, "stone",
            List.of(35.0, 60.0, 85.0), "kinetic");
    private static final Vec3 RIGHT = new Vec3(0, 0, 1);
    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0), RIGHT);
    private static final CastSpec.Spread SPREAD = new CastSpec.Spread(4, 3.0, 90.0);

    private static AbilityDefinition shot(String weapon) {
        return new AbilityDefinition(weapon + "/right_click", "Breacher", "kinetic", 20, ResourceCost.FREE,
                new CastSpec.Projectile(3.0, 0.0, 6, "breacher_trail", "cobblestone", null, null, SPREAD),
                List.of(new EffectSpec.Visual("breacher_hit"), new EffectSpec.Damage(40, "kinetic"),
                        new EffectSpec.Knockback(0.1)));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    private static Stage stage(int level, FakeWorld.Dummy... mobs) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        if (level > 0) him.behaviours = List.of(new ActiveBehaviour("shatter_caps", level, CAPS));
        world.entities.add(him);
        world.entities.addAll(List.of(mobs));
        return new Stage(world, him);
    }

    private static void press(Stage s, AbilityDefinition ability, Aim aim) {
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(ability, s.him().snapshot(), aim, true));
    }

    private static List<String> bodies(Stage s) {
        List<String> items = new ArrayList<>();
        for (UUID id : s.world().markersEverSpawned) items.add(s.world().markerItemsEver.get(id));
        return items;
    }

    private static List<Double> sent(Stage s) {
        return s.world().traced.stream().map(TracedHit::sent).toList();
    }

    @Test
    void whileActiveAPressLaunchesOneStoneNotFourChunks() {
        Stage s = stage(1);
        press(s, shot("breacher"), EAST);
        assertEquals(List.of("stone"), bodies(s), "Ben: \"1 stone\", at the press");
    }

    @Test
    void notActiveOrOnAnotherWeaponFourChunksAsToday() {
        Stage off = stage(0);
        press(off, shot("breacher"), EAST);
        assertEquals(List.of("cobblestone", "cobblestone", "cobblestone", "cobblestone"), bodies(off), "not active");
        Stage other = stage(1);
        press(other, shot("boltor"), EAST);
        assertEquals(List.of("cobblestone", "cobblestone", "cobblestone", "cobblestone"), bodies(other),
                "the enchant staged on another weapon (the dev command can) does nothing there");
        assertTrue(other.world().splits.isEmpty());
    }

    /** The split: the 2nd step, at exactly 5.0 along the aim, one tick after the press, four leaving. */
    @Test
    void theStoneSplitsOnItsSecondStepAtExactlyFive() {
        Stage s = stage(1);
        press(s, shot("breacher"), EAST);
        assertEquals(2, EnchantBehaviour.Airburst.ticksToTravel(CAPS.travelBlocks(), CAPS.speed()));
        s.world().advanceTicks(1);
        assertEquals(1, s.world().splits.size(), "one split");
        assertEquals(new Vec3(5, 0, 0), s.world().splits.getFirst().at(), "exactly 5.0 from his eye along the aim");
        assertEquals(4, s.world().splits.getFirst().bursts());
        assertEquals(1, s.world().splits.getFirst().tick(), "the 2nd step: one tick after the press");
        assertEquals(List.of("stone", "cobblestone", "cobblestone", "cobblestone", "cobblestone"), bodies(s));
    }

    /**
     * The four leave with the WEAPON's spread (its directions at a press, about the same aim and right), its speed 3.0,
     * its 40 and its full 6-tick reach: a mob 22 out (5 + 17) takes the centre chunk's 40; a mob 24 out (5 + 19) none.
     */
    @Test
    void theFourLeaveWithTheWeaponsSpreadSpeedDamageAndReach() {
        Stage s = stage(1);
        press(s, shot("breacher"), EAST);
        s.world().advanceTicks(1);
        List<Vec3> want = SpreadPattern.directionsFor(EAST.direction(), RIGHT, SPREAD);
        List<UUID> chunks = s.world().markersEverSpawned.subList(1, 5);
        for (int i = 0; i < 4; i++) {
            Vec3 v = s.world().markerVelocities.get(chunks.get(i)).getFirst();
            assertTrue(v.distanceSquared(want.get(i).scale(3.0)) < 1e-24, "chunk " + i + ": " + v + " vs " + want.get(i));
        }
        var near = new FakeWorld.Dummy(new Vec3(22, 0, 0));
        Stage reach = stage(1, near);
        press(reach, shot("breacher"), EAST);
        reach.world().advanceTicks(20);
        assertEquals(List.of(40.0), sent(reach), "the centre chunk's usual 40, at 22: within 5 + 18");
        var far = new FakeWorld.Dummy(new Vec3(24, 0, 0));
        Stage beyond = stage(1, far);
        press(beyond, shot("breacher"), EAST);
        beyond.world().advanceTicks(20);
        assertEquals(List.of(), sent(beyond), "at 24 nothing: beyond 5 + 18");
    }

    /** A hit before 5: the level's damage on the weapon's own on_hit, and no split. */
    @Test
    void aMobBeforeFiveTakesTheStoneAtEachLevelAndNothingSplits() {
        double[] want = {35, 60, 85};
        for (int level = 1; level <= 3; level++) {
            var mob = new FakeWorld.Dummy(new Vec3(3, 0, 0));
            Stage s = stage(level, mob);
            press(s, shot("breacher"), EAST);
            s.world().advanceTicks(20);
            assertEquals(List.of(want[level - 1]), sent(s), "level " + level);
            assertTrue(s.world().splits.isEmpty(), "no split at level " + level);
            assertEquals(List.of("stone"), bodies(s), "and no chunk");
        }
    }

    /** Fired straight up, the split's basis is the press's: his own right (Aim carries it), so the triangle keeps its point. */
    @Test
    void straightUpTheSplitUsesThePresssBasis() {
        Aim up = new Aim(Vec3.ZERO, new Vec3(0, 1, 0), RIGHT);
        Stage s = stage(1);
        press(s, shot("breacher"), up);
        s.world().advanceTicks(1);
        assertEquals(new Vec3(0, 5, 0), s.world().splits.getFirst().at());
        List<Vec3> want = SpreadPattern.directionsFor(up.direction(), RIGHT, SPREAD);
        List<UUID> chunks = s.world().markersEverSpawned.subList(1, 5);
        for (int i = 0; i < 4; i++) {
            Vec3 v = s.world().markerVelocities.get(chunks.get(i)).getFirst();
            assertTrue(v.distanceSquared(want.get(i).scale(3.0)) < 1e-24, "chunk " + i);
        }
    }

    /**
     * With Kinetic Tremors I also active (3 hits on one target within 60 ticks): the split's four chunks on one mob 8 out
     * (3 past the split, where the ring is tan 3 x 3 = 0.157 off the aim, inside the fake's 0.3) are four primary hits, so
     * exactly ONE shockwave (its 5-tick cooldown, per player); the stone's hit alone is one hit and fires none.
     */
    @Test
    void withKineticTremorsTheChunksFireOneShockwaveAndTheStoneAloneNone() {
        var tremors = new ActiveBehaviour("kinetic_tremors", 1, new EnchantBehaviour.Shockwave(3, 60, 5,
                List.of(10.0), List.of(3.0), "kinetic", null));
        // Off a chunk-column border: the fake sees only bodies centred in a column the segment walks (FakeWorld.castRay),
        // and a mob centred ON z = 0 is missed by the chunk flying toward negative z. The pad's aim line (z -3.5) is inside one.
        Aim eastAt8 = new Aim(new Vec3(0, 0, 8), new Vec3(1, 0, 0), RIGHT);
        var split = new FakeWorld.Dummy(new Vec3(8, 0, 8));
        split.health = 1000;   // the gate's zombies: a dummy's default 100 dies at the third 40
        Stage s = stage(1, split);
        s.him().behaviours = List.of(new ActiveBehaviour("shatter_caps", 1, CAPS), tremors);
        press(s, shot("breacher"), eastAt8);
        s.world().advanceTicks(20);
        assertEquals(4, s.world().traced.stream().filter(h -> "breacher/right_click".equals(h.source())).count(),
                "the control: four chunks landed on the mob 8 out");
        assertEquals(1, s.world().traced.stream().filter(h -> "kinetic_tremors/shockwave".equals(h.source())).count(),
                "exactly one shockwave");
        var near = new FakeWorld.Dummy(new Vec3(3, 0, 0));
        Stage alone = stage(1, near);
        alone.him().behaviours = List.of(new ActiveBehaviour("shatter_caps", 1, CAPS), tremors);
        press(alone, shot("breacher"), EAST);
        alone.world().advanceTicks(20);
        assertEquals(List.of(35.0), sent(alone), "the control: the stone's one hit");
        assertEquals(0, alone.world().traced.stream().filter(h -> "kinetic_tremors/shockwave".equals(h.source())).count(),
                "a stone hit alone fires none");
    }

    @Test
    void aMalformedShatterIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.Shatter("breacher", 0, 2.5, "stone",
                List.of(35.0), "kinetic"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.Shatter("breacher", 5, 0, "stone",
                List.of(35.0), "kinetic"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.Shatter(" ", 5, 2.5, "stone",
                List.of(35.0), "kinetic"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.Shatter("breacher", 5, 2.5, "stone",
                List.of(), "kinetic"));
        assertEquals(60.0, CAPS.damageAt(2));
        assertEquals(3, CAPS.levels());
    }
}
