package io.github.yourname.rpg.core;

import io.github.yourname.rpg.core.ability.effect.EffectApplier;
import io.github.yourname.rpg.core.ability.effect.EffectSpec;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.element.Element;
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
                new EffectSpec.Damage(12, Element.SOLAR),
                new EffectSpec.Heal(5),
                new EffectSpec.Knockback(1.5),
                new EffectSpec.Status("scorch", 40, 0),
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.Area(4.0, 100, 20,
                        List.of(new EffectSpec.Damage(2, Element.SOLAR))));

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
                        new EffectSpec.Damage(6, Element.SOLAR),
                        new EffectSpec.Status("scorch", 40, 0)))),
                caster.id(), null, Vec3.ZERO);

        assertEquals(0, world.pendingTasks(), "a burst is inline; it must schedule nothing");
        assertEquals(94, victim.health, 0.001);
        assertEquals(List.of("scorch"), victim.statuses);

        assertEquals(100, caster.health, 0.001, "you do not splash yourself");
        assertTrue(caster.statuses.isEmpty(), "you do not scorch yourself");

        assertEquals(100, farAway.health, 0.001, "outside the radius");
    }

    @Test
    void burstRejectsANonPositiveRadius() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(6, Element.SOLAR));

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
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(2, Element.SOLAR));

        var zero = assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(4.0, 100, 0, effects));
        assertTrue(zero.getMessage().contains("tick_interval"), zero.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(4.0, 100, -20, effects));
    }

    @Test
    void areaRejectsANonPositiveRadius() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(2, Element.SOLAR));

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
                new EffectSpec.Damage(12, Element.SOLAR),
                new EffectSpec.Status("scorch", 40, 0)), caster.id(), pair(target), Vec3.ZERO);

        assertEquals(88, target.health, 0.001);
        assertEquals(List.of("scorch"), target.statuses);
    }

    /**
     * Damage names its culprit. The port carries the caster's id, never the caster, so a
     * grenade that outlives its thrower still knows whom to credit.
     *
     * And the elemental multiplier is resolved HERE, before the port is called: 12 solar
     * into a solar shield is 18, and the handle is told 18 -- not 12 and an element.
     */
    @Test
    void damageIsAttributedToTheCasterAndAlreadyMultiplied() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        target.shield = Element.SOLAR;

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(12, Element.SOLAR)),
                caster.id(), pair(target), Vec3.ZERO);

        assertEquals(caster.id(), target.lastDamageSource, "the caster must be blamed");
        assertEquals(82, target.health, 0.001, "12 solar x1.5 against a solar shield");
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
                List.of(new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(6, Element.SOLAR)))),
                caster.id(), null, Vec3.ZERO);

        assertEquals(caster.id(), victim.lastDamageSource);
        assertNull(caster.lastDamageSource, "a burst never splashes its own caster");
    }
}
