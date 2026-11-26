package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.ClusterSplit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.weapon.BehaviourBlocksAccess;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLUSTER BOMBS THROUGH THE REAL CAST PATH (the seat's F9, 2026-11-26; Ben: "throws 1 big blast fungus that splits into 3
 * dealing 55/80/105", "the 3 that split from it deal the normal 55", "Split out in a triangle shape", "make the blast size
 * of the big bomb 5 blocks", "Yes it rises"). The Blast Fungus's shot as shipped (speed 0.7, gravity 0.04, fuse 40, three
 * at 25, a burst of 3 carrying 55) thrown from his eye 1.62 above a FLOOR at y 0, east; the enchant as shipped (radius 5,
 * three splits, 2.5 out, a 20-tick fuse, lift 0.25). Every tick counts from the throw (tick 0).
 */
class ClusterBombsTest {

    private static final EnchantBehaviour.ClusterBombs BOMBS = new EnchantBehaviour.ClusterBombs("blast_fungus",
            List.of(55.0, 80.0, 105.0), "nature", 5, "red_mushroom_block", 3, 2.5, 20, 0.25);
    private static final Vec3 RIGHT = new Vec3(0, 0, 1);
    private static final Aim EAST = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0), RIGHT);

    private static AbilityDefinition fungus(String weapon) {
        return new AbilityDefinition(weapon + "/right_click", "Fungal Barrage", "nature", 140, ResourceCost.FREE,
                new CastSpec.Projectile(0.7, 0.04, 100, null, "nether_wart_block", null, null,
                        new CastSpec.Spread(3, 25, 0), 40),
                List.of(new EffectSpec.Visual("fungus_blast"),
                        new EffectSpec.Burst(3, List.of(new EffectSpec.Damage(55, "nature")))));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    private static Stage stage(int level, int score, FakeWorld.Dummy... mobs) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        world.floorY = 0;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.triggerScore = score;
        if (level > 0) him.behaviours = List.of(new ActiveBehaviour("cluster_bombs", level, BOMBS));
        world.entities.add(him);
        world.entities.addAll(List.of(mobs));
        return new Stage(world, him);
    }

    private static void press(Stage s, AbilityDefinition ability) {
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(ability, s.him().snapshot(), EAST, true));
    }

    private static List<FakeWorld.FusedLine> lines(Stage s, String event) {
        return s.world().fusedLines.stream().filter(l -> l.event().equals(event)).toList();
    }

    /** Where the big chunk comes to rest on this floor: read from a dry run, never predicted by hand. */
    private static Vec3 bigRest() {
        Stage dry = stage(2, 100);
        press(dry, fungus("blast_fungus"));
        dry.world().advanceTicks(39);
        return lines(dry, "rest").getFirst().at();
    }

    private static FakeWorld.Dummy mobAt(Vec3 at) {
        var d = new FakeWorld.Dummy(at);
        d.health = 1000;
        return d;
    }

    @Test
    void whileActiveAPressThrowsOneBigChunkNotThree() {
        Stage on = stage(1, 100);
        press(on, fungus("blast_fungus"));
        assertEquals(List.of("red_mushroom_block"), items(on), "one big chunk, its own body");
        Stage off = stage(0, 100);
        press(off, fungus("blast_fungus"));
        assertEquals(List.of("nether_wart_block", "nether_wart_block", "nether_wart_block"), items(off), "not active: three");
        Stage other = stage(1, 100);
        press(other, fungus("flint_staff"));
        assertEquals(3, other.world().markersEverSpawned.size(), "the enchant staged on another weapon does nothing there");
    }

    private static List<String> items(Stage s) {
        List<String> out = new ArrayList<>();
        for (UUID id : s.world().markersEverSpawned) out.add(s.world().markerItemsEver.get(id));
        return out;
    }

    /** The big blast: the level's damage in radius 5, on its fuse at tick 40; a mob at 4.5 takes it, one at 5.5 does not. */
    @Test
    void theBigChunkLandsTheLevelsDamageInRadiusFive() {
        Vec3 c = bigRest();
        for (int level = 1; level <= 3; level++) {
            var in = mobAt(c.add(new Vec3(0, 0, 4.5)));
            var out = mobAt(c.add(new Vec3(0, 0, 5.5)));
            Stage s = stage(level, 100, in, out);
            s.world().boxSearch = true;   // as the server's getNearbyEntities
            press(s, fungus("blast_fungus"));
            s.world().advanceTicks(40);
            assertEquals(1, lines(s, "detonate").size(), "level " + level);
            assertEquals(40, lines(s, "detonate").getFirst().tick());
            double want = BOMBS.damageAt(level);
            assertEquals(List.of(55.0, 80.0, 105.0).get(level - 1), want);
            assertEquals(1000 - want, in.health, 1e-9, "level " + level + ": 4.5 out, inside 5");
            assertEquals(1000, out.health, 1e-9, "5.5 out: outside");
        }
    }

    /**
     * The split: three ordinary chunks at tick 40, on the triangle (the first on his heading, east), each coming to REST
     * 2.5 from the centre on the floor, each going off 20 ticks after the split for the weapon's 55.
     */
    @Test
    void threeChunksSplitOnATriangleRestAtTwoAndAHalfAndGoOffTwentyLater() {
        Vec3 c = bigRest();
        Stage s = stage(1, 100);
        press(s, fungus("blast_fungus"));
        s.world().advanceTicks(40);
        assertEquals(1, s.world().splits.size());
        assertEquals(3, s.world().splits.getFirst().bursts());
        assertEquals(c, s.world().splits.getFirst().at(), "from where the big one went off");
        s.world().advanceTicks(19);
        List<FakeWorld.FusedLine> rests = lines(s, "rest").subList(1, 4);
        List<Vec3> dirs = ClusterSplit.directions(new Vec3(1, 0, 0), 3);
        for (int i = 0; i < 3; i++) {
            Vec3 at = rests.get(i).at();
            Vec3 want = c.add(dirs.get(i).scale(2.5));
            assertTrue(at.distanceSquared(want) < 1e-18, "split " + i + " rests at " + at + ", the triangle's point " + want);
        }
        assertTrue(dirs.getFirst().distanceSquared(new Vec3(1, 0, 0)) < 1e-24, "one point along the throw's heading");
        assertEquals(1, lines(s, "detonate").size(), "only the big one so far");
        s.world().advanceTicks(1);   // tick 60
        assertEquals(4, lines(s, "detonate").size());
        for (var l : lines(s, "detonate").subList(1, 4)) assertEquals(60, l.tick(), "20 after the split");
    }

    /** A mob at one split's rest point: the big blast (it is 2.5 in) then that split's 55; the others are 4.33 away. */
    @Test
    void aMobAtOneRestPointTakesTheBigBlastThenOneSplit() {
        Vec3 c = bigRest();
        Vec3 point = c.add(ClusterSplit.directions(new Vec3(1, 0, 0), 3).get(1).scale(2.5));
        var mob = mobAt(point);
        Stage s = stage(2, 100, mob);
        press(s, fungus("blast_fungus"));
        s.world().advanceTicks(40);
        assertEquals(1000 - 80, mob.health, 1e-9);
        s.world().advanceTicks(20);
        assertEquals(1000 - 80 - 55, mob.health, 1e-9, "one split's 55, the weapon's normal blast");
    }

    /** Ben: "Yes it rises": at score 200 both double (the press's frozen score, through the same Damage arm). */
    @Test
    void theScoreScalesTheBigAndTheSmall() {
        Vec3 c = bigRest();
        Vec3 point = c.add(ClusterSplit.directions(new Vec3(1, 0, 0), 3).get(1).scale(2.5));
        var mob = mobAt(point);
        Stage s = stage(2, 200, mob);
        press(s, fungus("blast_fungus"));
        s.world().advanceTicks(40);
        assertEquals(1000 - 160, mob.health, 1e-9, "80 x 200/100");
        s.world().advanceTicks(20);
        assertEquals(1000 - 160 - 110, mob.health, 1e-9, "55 x 200/100");
    }

    /** F9: a split chunk born inside a block meets it on its first step and rests at the centre rather than vanishing. */
    @Test
    void aSplitBornInsideABlockRestsAtTheCentre() {
        Vec3 c = bigRest();
        Stage s = stage(1, 100);
        press(s, fungus("blast_fungus"));
        s.world().advanceTicks(39);
        s.world().blockDistance = 0;   // every segment from here meets a block at its start: "inside" (VoxelShape.clip, READ)
        s.world().advanceTicks(1);
        List<FakeWorld.FusedLine> rests = lines(s, "rest");
        assertEquals(4, rests.size(), "the big one, then three at once");
        for (var r : rests.subList(1, 4)) assertEquals(c, r.at(), "at the centre");
        s.world().advanceTicks(20);
        assertEquals(4, lines(s, "detonate").size(), "each goes off on its fuse");
    }

    @Test
    void theSplitGeometry() {
        assertEquals(new Vec3(1, 0, 0), ClusterSplit.heading(new Vec3(1, 0.5, 0).normalize(), RIGHT), "level part of the aim");
        Vec3 down = ClusterSplit.heading(new Vec3(0, -1, 0), RIGHT);
        assertTrue(down.distanceSquared(new Vec3(1, 0, 0)) < 1e-24, "straight down: forward from his right: " + down);
        assertThrows(IllegalArgumentException.class, () -> ClusterSplit.outwardSpeed(2.5, 0.25, 0));
    }

    /**
     * F10: the tooltip's rows from the same resolvers: the big one (no count), then the weapon's own, x the SPLIT count.
     * Staged with a split of FOUR against the weapon's spread of three: shipped, both are 3, and a row reading the spread's
     * count instead passed (MUTCBROWS, green on the first run). Two equal quantities cannot detect a transposition.
     */
    @Test
    void theTooltipRowsAreTheBigThenTheSplit() {
        var four = new EnchantBehaviour.ClusterBombs("blast_fungus", List.of(55.0, 80.0, 105.0), "nature", 5,
                "red_mushroom_block", 4, 2.5, 20, 0.25);
        var rows = BehaviourBlocksAccess.rows(fungus("blast_fungus"), new ActiveBehaviour("cluster_bombs", 2, four));
        assertEquals("[DamageRow[authored=80.0, element=nature, count=OptionalInt.empty], "
                + "DamageRow[authored=55.0, element=nature, count=OptionalInt[4]]]", rows.toString());
        assertEquals("[]", BehaviourBlocksAccess.rows(fungus("flint_staff"),
                new ActiveBehaviour("cluster_bombs", 2, BOMBS)).toString(), "another weapon's cast: no block");
    }
}
