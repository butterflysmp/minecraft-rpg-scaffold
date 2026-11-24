package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Orbs;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE ORB'S EXPIRY TASK, AND THE {@code LINGER work=1} (the seat's item 6, 2026-11-23). On the run of {@code efa38636} the
 * row-start clean after each row whose orb {@code OrbRemovals.remove} had removed ended ONE point-scheduled task: READ, it
 * is the expiry {@code throwOrb} schedules on arrival. Two things are pinned here: (1) that task CANNOT ACT on an orb that is
 * gone or replaced (the generation guard), and (2) it is now CANCELLED by every removal, so nothing is left scheduled.
 */
class OrbExpiryTest {

    private static final CastSpec.Orb ORB = new CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea");
    private static final AbilityDefinition THROW = new AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal",
            "kinetic", 600, new ResourceCost("mana", 30), ORB, List.of());
    private static final Aim EAST = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));

    private static FakeWorld.Dummy him(FakeWorld world) {
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.behaviours = List.of(new ActiveBehaviour("lapis_crystal", 2,
                new EnchantBehaviour.GrantTrigger("lapis_staff", "left_click", THROW)));
        world.entities.add(him);
        return him;
    }

    private static void throwOne(FakeWorld world, FakeWorld.Dummy him) {
        new CastExecutor(world).execute(new AbilityService.CastResult.Success(THROW, him.snapshot(), EAST, true));
    }

    @Test
    void anArrivedOrbCarriesItsExpiryTasksHandle() {
        var world = new FakeWorld();
        var him = him(world);
        throwOne(world, him);
        assertEquals(-1, world.orbs.of(him.id()).orElseThrow().expiryTask(), "no expiry while it flies");
        world.advanceTicks(10);
        long task = world.orbs.of(him.id()).orElseThrow().expiryTask();
        assertTrue(task >= 0, "resting: its expiry's handle");
        assertEquals(1, world.pendingTasks(), "the expiry, and nothing else, is pending");
        world.cancelScheduled(task);
        assertEquals(0, world.pendingTasks(), "cancelled by its handle: nothing left scheduled");
    }

    /** A removal by the store alone (what paper's removals do first) leaves the expiry harmless: it finds no orb. */
    @Test
    void anExpiryLeftScheduledAfterARemovalCannotAct() {
        var world = new FakeWorld();
        var him = him(world);
        throwOne(world, him);
        world.advanceTicks(10);
        world.orbs.remove(him.id());                 // removed, its task NOT cancelled: the old shape
        world.advanceTicks(400);
        assertEquals(List.of("rest"), world.orbLines.stream().map(FakeWorld.OrbLine::event).toList(),
                "the stale expiry wrote no line");
        assertTrue(world.orbs.of(him.id()).isEmpty());
    }

    /** A replacement CANCELS the old orb's expiry, and the stale one could not touch the new orb anyway. */
    @Test
    void aReplacementCancelsTheOldExpiryAndTheNewOrbExpiresOnItsOwnTime() {
        var world = new FakeWorld();
        var him = him(world);
        throwOne(world, him);
        world.advanceTicks(10);                      // the first rests at 10; its expiry is due at 310
        world.advanceTicks(50);
        throwOne(world, him);                        // at 60: replaces it, and cancels its expiry
        world.advanceTicks(10);                      // the second rests at 70; its expiry is due at 370
        assertEquals(1, world.pendingTasks(), "the second's expiry only: the first's is cancelled");
        world.advanceTicks(260);                     // 330: past the first's 310
        assertTrue(world.orbs.of(him.id()).isPresent(), "the new orb outlives the old one's expiry tick");
        world.advanceTicks(40);                      // 370
        assertTrue(world.orbs.of(him.id()).isEmpty());
        List<String> lines = world.orbLines.stream().map(l -> l.event() + ":" + l.cause()).toList();
        assertEquals(List.of("rest:", "removed:replaced", "rest:", "removed:expired"), lines);
    }

    /** The store's own half: an expiry handle is recorded only on the orb of its generation. */
    @Test
    void theStoreRecordsAnExpiryOnlyOnItsOwnGeneration() {
        var orbs = new Orbs();
        var owner = java.util.UUID.randomUUID();
        var first = orbs.place(owner, java.util.UUID.randomUUID(), Vec3.ZERO, 0.6, 300, null, null, 0, null).placed();
        orbs.arrive(owner, first.generation(), 5);
        var second = orbs.place(owner, java.util.UUID.randomUUID(), Vec3.ZERO, 0.6, 300, null, null, 0, null).placed();
        assertTrue(!orbs.expiring(owner, first.generation(), 7), "a stale generation records nothing");
        assertEquals(-1, orbs.of(owner).orElseThrow().expiryTask());
        orbs.arrive(owner, second.generation(), 9);
        assertTrue(orbs.expiring(owner, second.generation(), 8));
        assertEquals(8, orbs.of(owner).orElseThrow().expiryTask());
    }
}
