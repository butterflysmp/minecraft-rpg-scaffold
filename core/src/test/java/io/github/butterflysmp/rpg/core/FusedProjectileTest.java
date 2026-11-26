package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.FuseRules;
import io.github.butterflysmp.rpg.core.combat.FusedChunks;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.github.butterflysmp.rpg.core.combat.FuseRules.Outcome.DETONATE_FUSE;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Outcome.DETONATE_TOUCH;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Outcome.FLY_ON;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Outcome.REST;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Outcome.STAY;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Phase.FLYING;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Phase.RESTING;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Touch.BLOCK;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Touch.MOB;
import static io.github.butterflysmp.rpg.core.combat.FuseRules.Touch.NOTHING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A FUSED PROJECTILE (the seat's F1 to F6, 2026-11-26, the Blast Fungus; Ben: "Keep" the fuse, "Correct" to mobs only).
 * The table first, then the real cast path on a {@link FakeWorld}: his eye at the origin facing east (+x), his right
 * south. Every detonation tick is counted from the THROW (the executor's frame, tick 0 here; on the server the throw is
 * the press + 1, READ in {@code WeaponFire}). Damage figures are at score 100 with no enchant, class or level term, so a
 * 55 burst deals 55.
 */
class FusedProjectileTest {

    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(0, 0, 1));

    // ------------------------------------------------------------------------------------------------ THE TABLE (F1)

    @Test
    void theTableEveryRow() {
        assertEquals(FLY_ON, FuseRules.decide(FLYING, 3, 40, NOTHING), "in flight, fuse left, nothing met");
        assertEquals(DETONATE_TOUCH, FuseRules.decide(FLYING, 3, 40, MOB), "in flight, touches a mob");
        assertEquals(REST, FuseRules.decide(FLYING, 3, 40, BLOCK), "in flight, touches a block: it rests");
        assertEquals(STAY, FuseRules.decide(RESTING, 3, 40, NOTHING), "at rest, fuse left");
        assertEquals(DETONATE_FUSE, FuseRules.decide(FLYING, 40, 40, NOTHING), "fuse out in flight");
        assertEquals(DETONATE_FUSE, FuseRules.decide(RESTING, 40, 40, NOTHING), "fuse out at rest");
        // The fuse is asked FIRST: a mob met on the step the fuse runs out does not change where it goes off.
        assertEquals(DETONATE_FUSE, FuseRules.decide(FLYING, 40, 40, MOB));
        // A resting chunk is never traced, so a touch handed to it is ignored.
        assertEquals(STAY, FuseRules.decide(RESTING, 39, 40, MOB));
        assertTrue(FuseRules.traces(FLYING, 39, 40));
        assertFalse(FuseRules.traces(FLYING, 40, 40));
        assertFalse(FuseRules.traces(RESTING, 1, 40));
        assertThrows(IllegalArgumentException.class, () -> FuseRules.decide(FLYING, 0, 0, NOTHING));
    }

    // -------------------------------------------------------------------------------------------- THE STORE (F2)

    @Test
    void theStoreGivesEachChunkOutExactlyOnce() {
        var store = new FusedChunks();
        UUID him = UUID.randomUUID(), other = UUID.randomUUID(), body = UUID.randomUUID();
        long a = store.add(him, body, Vec3.ZERO), b = store.add(him, null, Vec3.ZERO), c = store.add(other, null, Vec3.ZERO);
        assertTrue(store.ownsMarker(body));
        assertTrue(store.take(a).isPresent());
        assertTrue(store.take(a).isEmpty(), "taken once: the second taker gets nothing");
        assertFalse(store.ownsMarker(body), "a body no live chunk owns is an orphan");
        assertEquals(List.of(b), store.takeAllOf(him).stream().map(FusedChunks.Chunk::id).toList(), "only his");
        assertTrue(store.live(c));
        assertEquals(1, store.takeAll().size());
        assertEquals(0, store.size());
    }

    // ---------------------------------------------------------------------------------------- THE CAST PATH (F1-F6)

    private static AbilityDefinition fused(int fuse, double speed, double gravity, CastSpec.Spread spread, double radius) {
        return new AbilityDefinition("blast_fungus/right_click", "Fungal Barrage", "nature", 140, ResourceCost.FREE,
                new CastSpec.Projectile(speed, gravity, 100, null, "nether_wart_block", null, null, spread, fuse),
                List.of(new EffectSpec.Visual("fungus_blast"),
                        new EffectSpec.Burst(radius, List.of(new EffectSpec.Damage(55, "nature")))));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    private static Stage stage(FakeWorld.Dummy... others) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        world.entities.add(him);
        world.entities.addAll(List.of(others));
        return new Stage(world, him);
    }

    private static void press(Stage s, AbilityDefinition ability) {
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(ability, s.him().snapshot(), EAST, true));
    }

    private static List<FakeWorld.FusedLine> lines(Stage s, String event) {
        return s.world().fusedLines.stream().filter(l -> l.event().equals(event)).toList();
    }

    private static FakeWorld.Dummy mob(double x, double y, double z) {
        return new FakeWorld.Dummy(new Vec3(x, y, z));
    }

    /** It meets a wall at x = 3, RESTS there (its body held still), and goes off on the step 40 after the throw. */
    @Test
    void aChunkThatMeetsABlockRestsAndGoesOffOnItsFuse() {
        var near = mob(3, 0, 2);   // 2 from the rest point: inside 3
        Stage s = stage(near);
        s.world().wallX = 3;
        press(s, fused(40, 1.0, 0.0, null, 3));
        s.world().advanceTicks(5);
        assertEquals(1, lines(s, "rest").size());
        assertEquals(new Vec3(3, 0, 0), lines(s, "rest").getFirst().at(), "at the point it met the block");
        UUID body = s.world().markersEverSpawned.getFirst();
        Vec3 held = s.world().markerPositions.get(body);
        assertEquals(new Vec3(3, 0, 0), held, "the body driven to the rest point");
        s.world().advanceTicks(30);
        assertEquals(held, s.world().markerPositions.get(body), "and held there: constant across ticks");
        assertTrue(lines(s, "detonate").isEmpty(), "not before its fuse");
        assertEquals(100.0, near.health, 1e-9);
        s.world().advanceTicks(5);   // tick 40
        assertEquals(1, lines(s, "detonate").size(), "once");
        assertEquals(40, lines(s, "detonate").getFirst().tick(), "40 ticks after the throw");
        assertEquals("fuse", lines(s, "detonate").getFirst().cause());
        assertEquals(new Vec3(3, 0, 0), lines(s, "detonate").getFirst().at(), "where it rests");
        assertEquals(45.0, near.health, 1e-9, "55 in the blast");
        assertFalse(s.world().markerPositions.containsKey(body), "the body is gone with it");
        assertEquals(0, s.world().fused.size(), "nothing left in the store");
        s.world().advanceTicks(5);
        assertEquals(0, s.world().pendingTasks(), "nothing left scheduled");
    }

    /** A mob in the path: it goes off AT ONCE, at the point it met the mob, before its fuse. */
    @Test
    void aMobInTheFlightPathSetsItOffEarly() {
        var inPath = mob(5, 0, 0);
        Stage s = stage(inPath);
        press(s, fused(40, 1.0, 0.0, null, 3));
        s.world().advanceTicks(40);
        assertEquals(1, lines(s, "detonate").size());
        assertEquals("touch", lines(s, "detonate").getFirst().cause());
        assertTrue(lines(s, "detonate").getFirst().tick() < 40, "before the fuse: tick " + lines(s, "detonate").getFirst().tick());
        assertEquals(45.0, inPath.health, 1e-9, "the burst, which it is inside: 55");
        assertTrue(lines(s, "rest").isEmpty());
    }

    /** No block, no mob: it goes off in the air where its fuse runs out (40 steps of 1.0, gravity 0). */
    @Test
    void theFuseRunsOutInFlight() {
        Stage s = stage();
        press(s, fused(40, 1.0, 0.0, null, 3));
        s.world().advanceTicks(41);
        assertEquals(1, lines(s, "detonate").size());
        assertEquals(new Vec3(40, 0, 0), lines(s, "detonate").getFirst().at());
        assertEquals(40, lines(s, "detonate").getFirst().tick());
    }

    /** F2: a chunk a removal took (his quit, the selftest's clear) never goes off, and leaves nothing scheduled. */
    @Test
    void aRemovedChunkNeverGoesOff() {
        var near = mob(3, 0, 1);
        Stage s = stage(near);
        s.world().wallX = 3;
        press(s, fused(40, 1.0, 0.0, null, 3));
        s.world().advanceTicks(10);
        assertEquals(1, s.world().fused.takeAllOf(s.him().id()).size());
        s.world().advanceTicks(40);
        assertTrue(lines(s, "detonate").isEmpty());
        assertEquals(100.0, near.health, 1e-9);
        assertEquals(0, s.world().pendingTasks());
    }

    /**
     * F4, SAFETY: the caster inside his own blast takes nothing (excluded by id, and spared as a player), another player
     * takes nothing (FUSED_BURST is spared), a summon takes nothing (the table's SUMMON cell); the mob beside them is hit.
     */
    @Test
    void onlyMobsAreHurt() {
        var other = mob(1, 0, 1);
        other.player = true;
        var wolf = mob(1, 0, -1);
        wolf.summon = true;
        var zombie = mob(2, 0, 0);
        Stage s = stage(other, wolf, zombie);
        s.world().wallX = 1;   // it rests 1 block out: he stands inside his own blast
        press(s, fused(40, 1.0, 0.0, null, 3));
        s.world().advanceTicks(41);
        assertEquals(1, lines(s, "detonate").size());
        assertEquals(100.0, s.him().health, 1e-9, "the caster: nothing");
        assertEquals(100.0, other.health, 1e-9, "another player: nothing");
        assertEquals(100.0, wolf.health, 1e-9, "a summon: nothing");
        assertEquals(45.0, zombie.health, 1e-9, "the mob: 55");
        assertFalse(MobHostility.PvpPath.FUSED_BURST.landsOnAPlayer(), "the verdict, pinned: SPARED");
    }

    /** Another player in the path is no touch: the chunk flies on through him and goes off on its fuse. */
    @Test
    void anotherPlayerInThePathIsNoTouch() {
        var other = mob(3, 0, 0);
        other.player = true;
        Stage s = stage(other);
        press(s, fused(40, 1.0, 0.0, null, 3));
        s.world().advanceTicks(41);
        assertEquals("fuse", lines(s, "detonate").getFirst().cause());
    }

    /**
     * F5: three bodies of a spread are three chunks, three fuses, three detonations. A mob inside all three blasts takes
     * each (165); each blast's damage is one primary hit, as a burst's is; one outside every blast takes nothing.
     */
    @Test
    void aSpreadOfThreeIsThreeChunksAndAMobInsideAllThreeTakesEach() {
        var inAll = mob(2.5, 0, 0.5);   // off every path (centre z 0, sides z = 0.466 x), within 3 of all three rest points
        inAll.health = 1000;
        var outside = mob(3, 0, 5);
        Stage s = stage(inAll, outside);
        s.world().wallX = 3;
        press(s, fused(40, 1.0, 0.0, new CastSpec.Spread(3, 25, 0), 3));
        assertEquals(3, s.world().markersEverSpawned.size(), "three bodies at the throw");
        s.world().advanceTicks(41);
        assertEquals(3, lines(s, "detonate").size());
        for (var l : lines(s, "detonate")) assertEquals(40, l.tick());
        assertEquals(1000.0 - 165.0, inAll.health, 1e-9, "55 x 3");
        assertEquals(100.0, outside.health, 1e-9);
        assertEquals(List.of(55.0, 55.0, 55.0), s.world().traced.stream().map(TracedHit::sent).toList());
        assertEquals(3, s.world().primaryHits.size(), "each blast counts as a hit, as a burst's damage does (B4)");
    }

    // ----------------------------------------------------------------------------------------- WHAT LOAD REFUSES

    @Test
    void aFusedOnHitIsADetonationOnly() {
        var cast = new CastSpec.Projectile(0.7, 0.04, 100, null, "nether_wart_block", null, null, null, 40);
        var e = assertThrows(IllegalArgumentException.class, () -> new AbilityDefinition("x/right_click", "X", "nature",
                10, ResourceCost.FREE, cast, List.of(new EffectSpec.Damage(55, "nature"))));
        assertTrue(e.getMessage().contains("x/right_click"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new AbilityDefinition("x/right_click", "X", "nature",
                10, ResourceCost.FREE, cast, List.of(new EffectSpec.Visual("v"))), "a burst is required");
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(0.7, 0.04, 100, null, null,
                new CastSpec.Homing(0.5, 0, 5), null, null, 40), "no seeking fuse");
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(0.7, 0.04, 100, null, null,
                null, "arrow", null, 40), "an item body only");
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Projectile(0.7, 0.04, 100, null, null,
                null, null, null, -1));
    }
}
