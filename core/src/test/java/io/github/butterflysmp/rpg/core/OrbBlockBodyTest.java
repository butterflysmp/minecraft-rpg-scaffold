package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE BLOCK-DISPLAY ORB BODY (Ben's item 3, 2026-11-23: "I want to change the visual of the Lapis crystal to a Floating
 * Lapis block with glowing"; the seat's rulings (a) and (b), PR 2). (a) The flight stays: a block body walks the item
 * body's path, one step a tick, and both reach the same rest point at the same tick, with the same ORB rest line. (b) A
 * block body carries a BACKSTOP: a removal scheduled at the throw, at flight + life + 100, that removes the body by its id
 * whatever the store says; every proper removal cancels it.
 */
class OrbBlockBodyTest {

    private static final CastSpec.OrbBlock LAPIS = new CastSpec.OrbBlock("lapis_block", 0.5, "#2A5CFF");
    private static final CastSpec.Orb ITEM_ORB = new CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea");
    private static final CastSpec.Orb BLOCK_ORB = new CastSpec.Orb(12, 10, 300, 0.6, null, null, LAPIS);
    private static final Aim EAST = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));
    private static final int BACKSTOP_AT = 10 + 300 + 100;

    private static AbilityDefinition throwOf(CastSpec.Orb orb) {
        return new AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal", "kinetic", 600,
                new ResourceCost("mana", 30), orb, List.of());
    }

    private static FakeWorld.Dummy him(FakeWorld world, CastSpec.Orb orb) {
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.behaviours = List.of(new ActiveBehaviour("lapis_crystal", 2,
                new EnchantBehaviour.GrantTrigger("lapis_staff", "left_click", throwOf(orb))));
        world.entities.add(him);
        return him;
    }

    private static void throwOne(FakeWorld world, FakeWorld.Dummy him, CastSpec.Orb orb) {
        new CastExecutor(world).execute(new AbilityService.CastResult.Success(throwOf(orb), him.snapshot(), EAST, true));
    }

    private static UUID body(FakeWorld world) {
        return world.orbMarkers.getLast();
    }

    /** Where the body stands after the throw and after each tick to 12: index 0 is the throw's own tick. */
    private static List<Vec3> path(CastSpec.Orb orb, List<FakeWorld.OrbLine> rest) {
        var world = new FakeWorld();
        var him = him(world, orb);
        throwOne(world, him, orb);
        UUID body = body(world);
        List<Vec3> at = new ArrayList<>();
        at.add(world.markerPositions.get(body));
        for (int t = 1; t <= 12; t++) {
            world.advanceTicks(1);
            at.add(world.markerPositions.get(body));
        }
        rest.addAll(world.orbLines);
        return at;
    }

    @Test
    void theBlockBodyWalksTheItemBodysPathAndRestsAtTheSameTick() {
        List<FakeWorld.OrbLine> itemRest = new ArrayList<>(), blockRest = new ArrayList<>();
        List<Vec3> item = path(ITEM_ORB, itemRest);
        List<Vec3> block = path(BLOCK_ORB, blockRest);
        Vec3 rest = new Vec3(12, 1.62, 0);
        for (int t = 0; t <= 12; t++) {
            // after the step at tick t, both stand at origin + (t + 1) x 1.2 until the rest point at t = 9
            Vec3 want = t >= 9 ? rest : new Vec3(1.2 * (t + 1), 1.62, 0);
            assertTrue(item.get(t).distanceSquared(want) < 1e-18, "item at " + t + ": " + item.get(t));
            assertTrue(block.get(t).distanceSquared(want) < 1e-18, "block at " + t + ": " + block.get(t));
        }
        assertEquals(rest, block.get(9), "the block body's last placement is the rest point itself");
        // the owners are two dummies; everything else on the ORB rest line is the same
        assertEquals(restLines(itemRest), restLines(blockRest), "the same ORB rest line at the same tick");
        assertEquals(List.of("rest " + rest + " tick=10"), restLines(itemRest), "both rest at tick 10, as before");
    }

    private static List<String> restLines(List<FakeWorld.OrbLine> lines) {
        return lines.stream().map(l -> l.event() + " " + l.at() + (l.cause().isEmpty() ? "" : " " + l.cause())
                + " tick=" + l.tick()).toList();
    }

    @Test
    void theBlockBodyIsSpawnedWithItsLookAndPlacedOncePerStepThenAtRest() {
        var world = new FakeWorld();
        var him = him(world, BLOCK_ORB);
        throwOne(world, him, BLOCK_ORB);
        UUID body = body(world);
        assertEquals(LAPIS, world.orbBlocks.get(body));
        assertEquals(new Vec3(0, 1.62, 0), world.markerSpawnedAt.get(body), "spawned at the throw's origin");
        world.advanceTicks(10);
        assertEquals(11, world.orbBodyPlacements.get(body).size(), "ten steps (ticks 0 to 9) and the arrival's");
        assertFalse(world.markerVelocities.containsKey(body), "a display is never driven by velocity");
    }

    @Test
    void anItemBodyHasNoBackstop() {
        var world = new FakeWorld();
        var him = him(world, ITEM_ORB);
        throwOne(world, him, ITEM_ORB);
        assertEquals(-1, world.orbs.of(him.id()).orElseThrow().backstopTask());
        assertTrue(world.orbBlocks.isEmpty());
    }

    /** Every other removal skipped: the expiry cancelled and the store emptied without the body's removal. */
    @Test
    void theBackstopRemovesTheBodyWhenEveryOtherRemovalIsSkipped() {
        var world = new FakeWorld();
        var him = him(world, BLOCK_ORB);
        throwOne(world, him, BLOCK_ORB);
        UUID body = body(world);
        assertTrue(world.orbs.of(him.id()).orElseThrow().backstopTask() >= 0, "scheduled at the throw");
        world.advanceTicks(10);
        world.cancelScheduled(world.orbs.of(him.id()).orElseThrow().expiryTask());
        world.orbs.remove(him.id());
        world.advanceTicks(BACKSTOP_AT - 10 - 1);
        assertTrue(world.markers.containsKey(body), "still there one tick before flight + life + 100");
        world.advanceTicks(1);
        assertFalse(world.markers.containsKey(body), "removed by its id at flight + life + 100");
        assertEquals("removed:backstop", lastLine(world));
        assertEquals(0, world.pendingTasks());
    }

    /** Whatever the store says: the store still holds the orb (its expiry ended by something else), and the body goes. */
    @Test
    void theBackstopActsWhateverTheStoreSays() {
        var world = new FakeWorld();
        var him = him(world, BLOCK_ORB);
        throwOne(world, him, BLOCK_ORB);
        UUID body = body(world);
        world.advanceTicks(10);
        world.cancelScheduled(world.orbs.of(him.id()).orElseThrow().expiryTask());
        world.advanceTicks(BACKSTOP_AT - 10);
        assertTrue(world.orbs.of(him.id()).isPresent(), "the store still holds it");
        assertFalse(world.markers.containsKey(body), "and its body is gone");
    }

    @Test
    void theBackstopDoesNotFireAfterTheExpiry() {
        var world = new FakeWorld();
        var him = him(world, BLOCK_ORB);
        throwOne(world, him, BLOCK_ORB);
        world.advanceTicks(310);
        assertEquals("removed:expired", lastLine(world));
        assertEquals(0, world.pendingTasks(), "the expiry cancelled the backstop: nothing is left scheduled");
        world.advanceTicks(200);
        assertEquals(List.of("rest:", "removed:expired"), lines(world));
    }

    @Test
    void theBackstopDoesNotFireAfterAReplacement() {
        var world = new FakeWorld();
        var him = him(world, BLOCK_ORB);
        throwOne(world, him, BLOCK_ORB);
        UUID first = body(world);
        world.advanceTicks(60);
        throwOne(world, him, BLOCK_ORB);
        assertFalse(world.markers.containsKey(first), "the replacement removed the first body");
        world.advanceTicks(320);                     // 380: the second expired at 370
        assertEquals(0, world.pendingTasks(), "both backstops cancelled");
        world.advanceTicks(200);                     // past both backstops' ticks (410 and 470)
        assertEquals(List.of("rest:", "removed:replaced", "rest:", "removed:expired"), lines(world));
    }

    @Test
    void aBlockLookIsRefusedWithoutABlockAColourOrAScale() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.OrbBlock(" ", 0.5, "#2A5CFF"));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.OrbBlock("lapis_block", 0, "#2A5CFF"));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.OrbBlock("lapis_block", 0.5, "blue"));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Orb(12, 10, 300, 0.6, null, null, null));
        assertEquals(0x2A5CFF, LAPIS.glowRgb());
    }

    private static List<String> lines(FakeWorld world) {
        return world.orbLines.stream().map(l -> l.event() + ":" + l.cause()).toList();
    }

    private static String lastLine(FakeWorld world) {
        return lines(world).getLast();
    }
}
