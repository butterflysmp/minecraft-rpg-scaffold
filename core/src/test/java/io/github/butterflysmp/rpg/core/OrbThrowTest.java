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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE THROWN ORB (Lapis Crystal's D2, the seat's message of 2026-11-23), through the real {@link CastExecutor} on the fake
 * world: Ben's 12 blocks and 300 ticks (counted from arrival), the seat's 10-tick flight, one orb per player, and a solid
 * block stopping it short.
 */
class OrbThrowTest {

    private static final CastSpec.Orb ORB = new CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea");
    private static final AbilityDefinition THROW = new AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal",
            "kinetic", 600, new ResourceCost("mana", 30), ORB, List.of());
    private static final Aim EAST = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));

    private static FakeWorld.Dummy him(FakeWorld world, boolean granted) {
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        if (granted) {
            him.behaviours = List.of(new ActiveBehaviour("lapis_crystal", 2,
                    new EnchantBehaviour.GrantTrigger("lapis_staff", "left_click", THROW)));
        }
        world.entities.add(him);
        return him;
    }

    private static void throwOne(FakeWorld world, FakeWorld.Dummy him) {
        new CastExecutor(world).execute(new AbilityService.CastResult.Success(THROW, him.snapshot(), EAST, true));
    }

    @Test
    void itRestsTwelveOutAfterTenTicksAndIsGoneThreeHundredTicksAfterArriving() {
        var world = new FakeWorld();
        var him = him(world, true);
        throwOne(world, him);
        assertEquals(1, world.orbMarkers.size(), "one body, spawned at the muzzle");
        assertFalse(world.orbs.of(him.id()).orElseThrow().resting(), "in flight");
        world.advanceTicks(10);
        assertEquals(List.of(new FakeWorld.OrbLine(him.id(), "rest", new Vec3(12, 1.62, 0), "", 10)), world.orbLines);
        Orbs.Orb orb = world.orbs.of(him.id()).orElseThrow();
        assertTrue(orb.resting());
        assertEquals("lapis_staff", orb.weaponId(), "it remembers the grant that threw it");
        assertEquals("lapis_crystal", orb.enchantId());
        assertEquals(2, orb.level());
        world.advanceTicks(299);
        assertTrue(world.orbs.of(him.id()).isPresent(), "299 ticks after arriving it is still there");
        world.advanceTicks(1);
        assertTrue(world.orbs.of(him.id()).isEmpty(), "300 after arriving it is gone");
        assertEquals(new FakeWorld.OrbLine(him.id(), "removed", new Vec3(12, 1.62, 0), "expired", 310),
                world.orbLines.get(1));
        assertFalse(world.markers.containsKey(world.orbMarkers.get(0)), "its body removed");
    }

    /** "stopping short at a solid block": a wall 8 out rests the orb 0.5 before it. */
    @Test
    void aSolidBlockStopsItShort() {
        var world = new FakeWorld();
        world.wallX = 8;
        var him = him(world, true);
        throwOne(world, him);
        world.advanceTicks(10);
        assertEquals(new Vec3(7.5, 1.62, 0), world.orbLines.get(0).at());
    }

    /** "One orb at a time": a second throw removes the first (cause replaced), and the first's expiry then does nothing. */
    @Test
    void aNewThrowRemovesTheStandingOrb() {
        var world = new FakeWorld();
        var him = him(world, true);
        throwOne(world, him);
        world.advanceTicks(100);
        throwOne(world, him);
        assertEquals("replaced", world.orbLines.get(1).cause());
        assertFalse(world.markers.containsKey(world.orbMarkers.get(0)), "the first body removed");
        assertEquals(1, world.orbs.size());
        world.advanceTicks(10 + 300);
        long expiries = world.orbLines.stream().filter(l -> l.cause().equals("expired")).count();
        assertEquals(1, expiries, "only the second orb expires; the first's scheduled expiry found a newer generation");
        assertEquals(0, world.orbs.size());
    }

    /** An orb no grant threw (an `orb` cast on any ability) remembers no weapon, so no beam it meets is his staff's. */
    @Test
    void anOrbNoGrantThrewRemembersNoWeapon() {
        var world = new FakeWorld();
        var him = him(world, false);
        throwOne(world, him);
        assertEquals(null, world.orbs.of(him.id()).orElseThrow().weaponId());
    }

    /** The registry alone: a body is owned only while its orb stands; removing all empties it (server stop). */
    @Test
    void theRegistryOwnsOnlyLiveBodies() {
        Orbs orbs = new Orbs();
        UUID owner = new UUID(0, 1), body = new UUID(0, 2);
        var placed = orbs.place(owner, body, Vec3.ZERO, 0.6, 300, null, null, 0, null);
        assertTrue(orbs.ownsMarker(body));
        assertTrue(orbs.removeIf(owner, placed.placed().generation() + 1).isEmpty(), "a stale generation removes nothing");
        assertEquals(1, orbs.removeAll().size());
        assertFalse(orbs.ownsMarker(body), "an orphan now: the sweep may remove it");
    }
}
