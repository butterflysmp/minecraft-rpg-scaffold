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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // --- THE FIXTURE DRAGS (2026-11-18): duration 20, drag 0.28, no pull --------------------------------------------

    /** The test-only fixture since 2026-11-18 (void_pull_fixture.yml): reach 5, knockback resistance, a 20-tick drag. */
    private static final Implosion DRAG = new Implosion(5.0, Implosion.Falloff.VANILLA_EXPLOSION, 0.0, 0.0,
            Implosion.Resistance.KNOCKBACK, 20, 0.28);

    @Test
    void aDraggingUseIsHandedToTheWorldOnceAndPullsNobodyItself() {
        var world = new FakeWorld();
        var caster = at(world, 0.5, true);
        var mob = at(world, 3, false);
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Implosion(DRAG, EffectSpec.Affects.MOBS, "void_implosion", null)),
                caster.asCaster(), null, POINT);
        assertEquals(1, world.drags.size(), "one drag, started once");
        FakeWorld.StartedDrag d = world.drags.get(0);
        assertEquals(POINT, d.origin());
        assertSame(DRAG, d.shape(), "the use's own shape");
        assertEquals(EffectSpec.Affects.MOBS, d.affects());
        assertEquals(caster.id(), d.casterId(), "never the caster: named to the world");
        assertEquals(0, mob.pullCalls, "a drag is not a single pull");
        assertEquals(List.of(5.0), world.presentedRadius, "the swirl at the reach, as before");
    }

    @Test
    void aDraggingUsesDamageIsDealtOnceAtTheInstant() {
        var world = new FakeWorld();
        var caster = at(world, -20, true);
        var mob = at(world, 2.5, false);
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Implosion(DRAG, EffectSpec.Affects.MOBS, null, new EffectSpec.Damage(16, "void"))),
                caster.asCaster(), null, POINT);
        assertEquals(1, mob.damageCalls, "once, at the blast's instant, not every tick of the drag");
        assertEquals(16.0 * Implosion.vanillaShare(2.5, 5.0), 100.0 - mob.health, 1e-9);
    }

    /**
     * THE FIXTURE'S DRAG ON MOBS, run as the world runs it: each tick every mob the use names (never the caster, never
     * a player) is a sighting with its own velocity (the server owns a mob's motion), and each push is SET. The mobs
     * here move as a body on stone with no AI: move, then the horizontal x 0.546. Both are pulled in and come to rest
     * no nearer than the margin; the player beside them is never pushed.
     */
    @Test
    void theFixturesDragPullsTheMobsInAndNoFartherThanTheMargin() {
        var world = new FakeWorld();
        var caster = at(world, -20, true);
        var a = at(world, -3, false);
        var b = at(world, 4, false);
        var player = at(world, 2, true);
        new EffectApplier(world).applyAll(List.of(new EffectSpec.Implosion(DRAG, EffectSpec.Affects.MOBS, null, null)),
                caster.asCaster(), null, POINT);
        var drag = new io.github.butterflysmp.rpg.core.shape.ImplosionDrag(POINT, world.drags.get(0).shape(),
                new io.github.butterflysmp.rpg.core.shape.DragMotion());
        java.util.Map<java.util.UUID, Vec3> velocity = new java.util.HashMap<>();
        double m = Implosion.tickMultiplier(true, 0.6f);
        int pushedPlayer = 0;
        for (long t = 0; t < 21 + 40; t++) {
            if (!drag.finished()) {
                List<io.github.butterflysmp.rpg.core.shape.ImplosionDrag.Sighting> seen = new java.util.ArrayList<>();
                for (FakeWorld.Dummy d : List.of(a, b, player, caster)) {
                    if (d.player) continue;   // the use names MOBS: the world's predicate, as paper's is
                    seen.add(new io.github.butterflysmp.rpg.core.shape.ImplosionDrag.Sighting(d.id(),
                            new Implosion.Footing(d.position(), 1.0, true, 0.6f, 0.0),
                            velocity.getOrDefault(d.id(), Vec3.ZERO)));
                }
                for (var push : drag.tick(t, seen, java.util.Set.of())) {
                    if (push.id().equals(player.id())) pushedPlayer++;
                    velocity.put(push.id(), push.velocity());
                }
            }
            for (FakeWorld.Dummy d : List.of(a, b)) {
                Vec3 v = velocity.getOrDefault(d.id(), Vec3.ZERO);
                d.moveTo(d.position().add(new Vec3(v.x(), 0, v.z())));
                velocity.put(d.id(), new Vec3(v.x() * m, 0, v.z() * m));
            }
        }
        assertEquals(0, pushedPlayer, "the player is not a mob");
        assertEquals(2, drag.summaries().size(), "both mobs, and only them");
        for (FakeWorld.Dummy d : List.of(a, b)) {
            double rest = Math.abs(d.position().x());
            assertEquals(0.5, rest, 0.01, "dragged to the margin and held there: " + rest);
            assertTrue(rest >= 0.5 - 1e-9, "never past it");
        }
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
