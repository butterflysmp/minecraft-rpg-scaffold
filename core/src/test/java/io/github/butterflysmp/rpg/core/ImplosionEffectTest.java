package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * THE SHAPE'S SECOND USE (the brief, Part D 1): an ability names the void implosion from content, {@code type:
 * implosion}, and the SAME shape the Phage's blast uses pulls whom the USE's predicate names. This is the test-only
 * fixture the brief asked for: it pulls MOBS toward a point and deals NO damage. No shipped ability or weapon uses it.
 *
 * <p>The pull's arithmetic is {@link Implosion}'s (ImplosionTest); this pins the WIRING: who is handed the pull, with
 * which origin and shape, that the visual is drawn at the shape's reach, and that no damage is dealt unless the use
 * authors some, which then falls off as the shape says.
 */
class ImplosionEffectTest {

    private static final Implosion SHAPE = new Implosion(6.0, Implosion.Falloff.VANILLA_EXPLOSION, 1.0, 0.0,
            Implosion.Resistance.KNOCKBACK);
    private static final Vec3 POINT = new Vec3(0, 64, 0);

    /** The fixture: pull MOBS, no damage, the void_implosion visual. */
    private static final EffectSpec.Implosion PULL_MOBS =
            new EffectSpec.Implosion(SHAPE, EffectSpec.Affects.MOBS, "void_implosion", null);

    private static FakeWorld.Dummy at(FakeWorld world, double x, boolean player) {
        FakeWorld.Dummy d = new FakeWorld.Dummy(new Vec3(x, 64, 0));
        d.player = player;
        world.entities.add(d);
        return d;
    }

    @Test
    void itPullsTheMobsInsideTheReachAndNoOneElse() {
        var world = new FakeWorld();
        var caster = at(world, 0.5, true);          // the caster, a player, inside the reach
        var near = at(world, 3, false);
        var edge = at(world, 6, false);              // exactly at the reach: handed the pull (the shape applies 0)
        var beyond = at(world, 6.5, false);
        var player = at(world, 2, true);

        new EffectApplier(world).applyAll(List.of(PULL_MOBS), caster.asCaster(), null, POINT);

        assertEquals(1, near.pullCalls);
        assertEquals(POINT, near.lastPullOrigin);
        assertSame(SHAPE, near.lastPullShape, "the use's own shape, not a copy with other numbers");
        assertEquals(1, edge.pullCalls);
        assertEquals(0, beyond.pullCalls, "beyond the reach");
        assertEquals(0, player.pullCalls, "a player is not a mob");
        assertEquals(0, caster.pullCalls, "never the caster");
    }

    @Test
    void itDealsNoDamageWhenTheUseAuthorsNone() {
        var world = new FakeWorld();
        var caster = at(world, -20, true);
        var mob = at(world, 1, false);
        new EffectApplier(world).applyAll(List.of(PULL_MOBS), caster.asCaster(), null, POINT);
        assertEquals(100.0, mob.health, 0.0);
        assertEquals(0, mob.damageCalls);
    }

    @Test
    void thePlayersPredicatePullsPlayersAndNotMobs() {
        var world = new FakeWorld();
        var caster = at(world, -20, false);
        var mob = at(world, 2, false);
        var player = at(world, 2.5, true);
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Implosion(SHAPE, EffectSpec.Affects.PLAYERS, null, null)),
                caster.asCaster(), null, POINT);
        assertEquals(0, mob.pullCalls);
        assertEquals(1, player.pullCalls);
    }

    @Test
    void theVisualIsDrawnOnceAtTheOriginAtTheShapesReach() {
        var world = new FakeWorld();
        var caster = at(world, -20, true);
        new EffectApplier(world).applyAll(List.of(PULL_MOBS), caster.asCaster(), null, POINT);
        assertEquals(List.of("void_implosion"), world.presented);
        assertEquals(List.of(POINT), world.presentedAt);
        assertEquals(List.of(6.0), world.presentedRadius, "drawn at the reach: a stage's swirl is its own width");
    }

    @Test
    void noVisualIsDrawnWhenTheUseNamesNone() {
        var world = new FakeWorld();
        var caster = at(world, -20, true);
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Implosion(SHAPE, EffectSpec.Affects.MOBS, null, null)),
                caster.asCaster(), null, POINT);
        assertEquals(List.of(), world.presented);
    }

    @Test
    void authoredDamageFallsOffAsTheShapeSays() {
        // Two mobs, at 0 and at 3 (half the reach): vanilla's curve gives the second three eighths of the first.
        var world = new FakeWorld();
        var caster = at(world, -20, true);
        var centre = at(world, 0, false);
        var half = at(world, 3, false);
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Implosion(SHAPE, EffectSpec.Affects.MOBS, null,
                        new EffectSpec.Damage(16, "void"))),
                caster.asCaster(), null, POINT);
        double atCentre = 100.0 - centre.health;
        double atHalf = 100.0 - half.health;
        assertEquals(16.0, atCentre, 1e-9, "a neutral caster deals the authored amount at the centre");
        assertEquals(atCentre * 0.375, atHalf, 1e-9);
        assertEquals("void", half.lastDamageElement);
        assertEquals(1, half.pullCalls, "damaged AND pulled");
    }

    @Test
    void aBadUseIsRefusedByName() {
        assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Implosion(null, EffectSpec.Affects.MOBS, null, null), "no shape");
        assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Implosion(SHAPE, null, null, null), "no predicate");
        assertNull(PULL_MOBS.damage());
    }
}
