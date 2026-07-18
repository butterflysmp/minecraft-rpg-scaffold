package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

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
     * A thrown ember lands as a real item at once, waits out its single launch fuse, then
     * detonates mob-only and removes the item in the SAME task -- so item and detonation cannot
     * diverge and no item leaks. Three mutations redden here: drop the player skip and the
     * player burns; drop removeMarker and the item leaks; slip the fuse and the timing shifts.
     */
    @Test
    void throwEmbersFiresAfterItsFuseMobOnlyAndRemovesItsItem() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var mob = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var player = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        player.player = true;
        world.entities.add(caster);
        world.entities.add(mob);
        world.entities.add(player);

        var embers = new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 20,
                new EffectSpec.Burst(4.0, List.of(
                        new EffectSpec.Damage(8, "fire"),
                        new EffectSpec.Status("scorch", 60, 0))),
                "ember_burst", null);   // visual = boom at detonation; trail unused here

        new EffectApplier(world).applyAll(List.of(embers), caster.id(), null, Vec3.ZERO);

        assertEquals(1, world.markers.size(), "the item is thrown at once");
        assertEquals(100, mob.health, 1e-9, "nothing detonates before the fuse");
        assertFalse(world.presented.contains("ember_burst"), "the boom waits for the fuse");

        world.advanceTicks(19);
        assertEquals(100, mob.health, 1e-9, "the fuse is 20 ticks -- not at 19");
        assertEquals(1, world.markers.size(), "the item lives exactly as long as the fuse");

        world.advanceTicks(1);
        assertEquals(92, mob.health, 1e-9, "detonates at 20 ticks: 8 fire damage");
        assertEquals(List.of("scorch"), mob.statuses);
        assertEquals(100, player.health, 1e-9, "mob-only: the burst spares players");
        assertTrue(player.statuses.isEmpty(), "mob-only: no scorch on players");
        assertEquals(100, caster.health, 1e-9, "you do not burn yourself");
        assertTrue(world.markers.isEmpty(), "item removed on detonation -- no leak");
        assertTrue(world.presented.contains("ember_burst"), "the boom fires with the blast");
    }

    /**
     * The blast-fungus guarantee: the fuse detonates where the thrown item actually IS at
     * fuse-end, not where it was thrown. Throw at the origin, drift the item 10 blocks away
     * (standing in for its flight and landing), fire the fuse -- the mob under the LIVE item
     * burns, the mob still sitting at the throw origin does not. Revert the burst point to
     * {@code origin} and the two mobs swap fates: this test reddens.
     */
    @Test
    void throwEmbersDetonatesAtTheItemsLivePositionNotWhereItWasThrown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var mobAtThrow = new FakeWorld.Dummy(new Vec3(1, 0, 0));     // within 4 of the throw origin
        var mobAtLanding = new FakeWorld.Dummy(new Vec3(10, 0, 0));  // within 4 of where it lands
        world.entities.add(caster);
        world.entities.add(mobAtThrow);
        world.entities.add(mobAtLanding);

        var embers = new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 20,
                new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(8, "fire"))), null, null);

        new EffectApplier(world).applyAll(List.of(embers), caster.id(), null, Vec3.ZERO);

        // The item flew and landed 10 blocks away before the fuse fired.
        UUID itemId = world.markers.keySet().iterator().next();
        world.moveMarker(itemId, new Vec3(10, 0, 0));

        world.advanceTicks(20);

        assertEquals(92, mobAtLanding.health, 1e-9,
                "detonates at the item's LIVE position: the mob there burns");
        assertEquals(100, mobAtThrow.health, 1e-9,
                "not where it was thrown: the mob at the origin is 9 blocks from the blast");
        assertTrue(world.markers.isEmpty(), "item still removed on detonation -- no leak");
    }

    /**
     * The per-tick tracking loop draws the trail every tick the ember is alive, at its live
     * position -- so a clean particle LINE follows the arc, not a single puff. A trail declared
     * but never wired would only show (by its absence) at boot; this catches it in core. Drop
     * the per-tick present(trail) and the count falls to zero.
     */
    @Test
    void throwEmbersLeavesATrailEveryTickOfFlight() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        var embers = new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 5,
                new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(8, "fire"))), null, "ember_trail");

        new EffectApplier(world).applyAll(List.of(embers), caster.id(), null, Vec3.ZERO);
        world.advanceTicks(5);

        long trailCount = world.presented.stream().filter("ember_trail"::equals).count();
        assertTrue(trailCount > 1,
                "the trail is emitted every tick of flight, not just once; got " + trailCount);
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

    /**
     * Knockback is DECLARED, not default. An on-hit with no {@code Knockback} effect pushes the
     * target NOWHERE -- the common case (a quarter of weapons, Mage staves especially, deal none),
     * so absence must mean zero, never a sneaked-in fallback. Declared, it applies exactly that
     * strength, away from the origin.
     *
     * Mutation: a default knockback when none is declared -> knockbackCalls on the no-KB target
     * goes to 1 -> reddens.
     */
    @Test
    void knockbackIsAppliedOnlyWhenDeclared() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var noKb = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var withKb = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        var applier = new EffectApplier(world);

        // Damage only -- the Mage case: hurt, but not moved.
        applier.applyAll(List.of(new EffectSpec.Damage(6, "fire")), caster.id(), pair(noKb), Vec3.ZERO);
        assertEquals(0, noKb.knockbackCalls, "no Knockback effect declared -> no knockback (default is none)");

        // Damage + declared Knockback -- the Melee case: hurt AND shoved.
        applier.applyAll(List.of(new EffectSpec.Damage(6, "fire"), new EffectSpec.Knockback(1.5)),
                caster.id(), pair(withKb), Vec3.ZERO);
        assertEquals(1, withKb.knockbackCalls, "a declared Knockback applies exactly one push");
        assertEquals(1.5, withKb.lastKnockbackStrength, 1e-9, "at the declared strength");
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
