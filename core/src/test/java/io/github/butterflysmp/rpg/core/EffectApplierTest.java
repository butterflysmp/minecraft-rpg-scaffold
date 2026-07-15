package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EffectApplierTest {

    /** What the world hands out: a snapshot to read, a handle to act on. */
    private static Combatant pair(FakeWorld.Dummy dummy) {
        return new Combatant(dummy.snapshot(), dummy);
    }

    /**
     * Every effect variant, applied with no target. A projectile that lands in
     * an empty field resolves no target, and the bundled solar_grenade.yml
     * carries a status effect. None of these may throw.
     */
    @Test
    void noEffectThrowsWhenThereIsNoTarget() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var applier = new EffectApplier(world);

        List<EffectSpec> everyVariant = List.of(
                new EffectSpec.Damage(12, "fire"),
                new EffectSpec.Heal(5),
                new EffectSpec.Knockback(1.5),
                new EffectSpec.Status("scorch", 40, 0),
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.Area(4.0, 100, 20,
                        List.of(new EffectSpec.Damage(2, "fire"))));

        assertDoesNotThrow(() -> applier.applyAll(everyVariant, caster.id(), null, Vec3.ZERO));
    }

    /** The specific regression: a status effect with nobody to apply it to. */
    @Test
    void statusWithNullTargetIsSkippedNotThrown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var applier = new EffectApplier(world);

        assertDoesNotThrow(() -> applier.applyAll(
                List.of(new EffectSpec.Status("scorch", 40, 0)), caster.id(), null, Vec3.ZERO));
    }

    /** Untargeted effects still run when there is no target. */
    @Test
    void visualStillPresentsWithNoTarget() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var applier = new EffectApplier(world);

        applier.applyAll(List.of(new EffectSpec.Visual("solar_detonation")), caster.id(), null, Vec3.ZERO);

        assertEquals(List.of("solar_detonation"), world.presented);
    }

    /**
     * A burst is a blast, not a field: it lands on the frame it is applied, with no
     * scheduling at all. Anything scheduled would arrive at least one tick late, because
     * the Paper adapter clamps a zero delay up to one tick.
     *
     * It needs no target -- a grenade that detonates on bare ground still splashes.
     */
    @Test
    void burstAppliesNestedEffectsImmediatelyExcludingCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var farAway = new FakeWorld.Dummy(new Vec3(50, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);
        world.entities.add(farAway);

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Burst(4.0, List.of(
                        new EffectSpec.Damage(6, "fire"),
                        new EffectSpec.Status("scorch", 40, 0)))),
                caster.id(), null, Vec3.ZERO);

        assertEquals(0, world.pendingTasks(), "a burst is inline; it must schedule nothing");
        assertEquals(94, victim.health, 0.001);
        assertEquals(List.of("scorch"), victim.statuses);

        assertEquals(100, caster.health, 0.001, "you do not splash yourself");
        assertTrue(caster.statuses.isEmpty(), "you do not scorch yourself");

        assertEquals(100, farAway.health, 0.001, "outside the radius");
    }

    /**
     * A delayed burst plants a marker at once, waits out its fuse, then detonates mob-only and
     * removes the marker in the SAME task -- so display and detonation cannot diverge and no
     * display entity leaks. Three mutations redden here: drop the player skip and the player
     * burns; drop removeMarker and the marker leaks; slip the fuse and the timing shifts.
     */
    @Test
    void delayedBurstFiresAfterItsFuseMobOnlyAndRemovesItsMarker() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var mob = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var player = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        player.player = true;
        world.entities.add(caster);
        world.entities.add(mob);
        world.entities.add(player);

        var delayed = new EffectSpec.DelayedBurst("blaze_powder", 20,
                new EffectSpec.Burst(4.0, List.of(
                        new EffectSpec.Damage(8, "fire"),
                        new EffectSpec.Status("scorch", 60, 0))),
                "ember_burst");   // the boom/flash at detonation

        new EffectApplier(world).applyAll(List.of(delayed), caster.id(), null, Vec3.ZERO);

        assertEquals(1, world.markers.size(), "the marker is planted at once, showing where");
        assertEquals(100, mob.health, 1e-9, "nothing detonates before the fuse");
        assertFalse(world.presented.contains("ember_burst"), "the boom waits for the fuse");

        world.advanceTicks(19);
        assertEquals(100, mob.health, 1e-9, "the fuse is 20 ticks -- not at 19");
        assertEquals(1, world.markers.size(), "the marker lives exactly as long as the fuse");

        world.advanceTicks(1);
        assertEquals(92, mob.health, 1e-9, "detonates at 20 ticks: 8 fire damage");
        assertEquals(List.of("scorch"), mob.statuses);
        assertEquals(100, player.health, 1e-9, "mob-only: the burst spares players");
        assertTrue(player.statuses.isEmpty(), "mob-only: no scorch on players");
        assertEquals(100, caster.health, 1e-9, "you do not burn yourself");
        assertTrue(world.markers.isEmpty(), "marker removed on detonation -- no leak");
        assertTrue(world.presented.contains("ember_burst"), "the boom fires with the blast");
    }

    @Test
    void burstRejectsANonPositiveRadius() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(6, "fire"));

        var zero = assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Burst(0, effects));
        assertTrue(zero.getMessage().contains("radius"), zero.getMessage());
    }

    /**
     * tickArea computes next = elapsed + tickInterval and reschedules while
     * next <= durationTicks. With tickInterval 0 that condition never fails, so the
     * area reschedules itself forever at zero delay. Reject it at construction.
     */
    @Test
    void areaRejectsANonPositiveTickInterval() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(2, "fire"));

        var zero = assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(4.0, 100, 0, effects));
        assertTrue(zero.getMessage().contains("tick_interval"), zero.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(4.0, 100, -20, effects));
    }

    @Test
    void areaRejectsANonPositiveRadius() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(2, "fire"));

        var zero = assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(0, 100, 20, effects));
        assertTrue(zero.getMessage().contains("radius"), zero.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(-1.0, 100, 20, effects));
    }

    /** And a targeted effect still lands when there is one. */
    @Test
    void targetedEffectsStillApplyWhenTargetPresent() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var applier = new EffectApplier(world);

        applier.applyAll(List.of(
                new EffectSpec.Damage(12, "fire"),
                new EffectSpec.Status("scorch", 40, 0)), caster.id(), pair(target), Vec3.ZERO);

        assertEquals(88, target.health, 0.001);
        assertEquals(List.of("scorch"), target.statuses);
    }

    /**
     * Damage names its culprit. The port carries the caster's id, never the caster, so a
     * grenade that outlives its thrower still knows whom to credit.
     */
    @Test
    void damageIsAttributedToTheCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(12, "fire")),
                caster.id(), pair(target), Vec3.ZERO);

        assertEquals(caster.id(), target.lastDamageSource, "the caster must be blamed");
        assertEquals(88, target.health, 0.001, "12 damage, no element multiplier");
    }

    /**
     * Element is identity, not math. The same amount deals the same damage whatever the
     * element -- there is no multiplier, no shield, no triangle. This is the subtraction's
     * proof: re-introduce a multiplier and these two numbers diverge.
     */
    @Test
    void damageIsTheAmountRegardlessOfElement() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var solarTarget = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var voidTarget = new FakeWorld.Dummy(new Vec3(2, 0, 0));

        var applier = new EffectApplier(world);
        applier.applyAll(List.of(new EffectSpec.Damage(10, "fire")),
                caster.id(), pair(solarTarget), Vec3.ZERO);
        applier.applyAll(List.of(new EffectSpec.Damage(10, "void")),
                caster.id(), pair(voidTarget), Vec3.ZERO);

        assertEquals(90, solarTarget.health, 1e-9);
        assertEquals(voidTarget.health, solarTarget.health, 1e-9, "element must not touch the number");
    }

    /** Splash damage carries the same culprit, and still never splashes the caster. */
    @Test
    void burstDamageCarriesTheCasterIdToEveryVictim() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(6, "fire")))),
                caster.id(), null, Vec3.ZERO);

        assertEquals(caster.id(), victim.lastDamageSource);
        assertNull(caster.lastDamageSource, "a burst never splashes its own caster");
    }
}
