package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.*;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Resolving an aim into an impact: Self, Melee, Ray. */
class CastExecutorTest {

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    private static AbilityDefinition ability(CastSpec cast, EffectSpec... onHit) {
        return new AbilityDefinition("test", "Test", "fire", "none",
                0, ResourceCost.FREE, cast, List.of(onHit));
    }

    /** Run an ability with no cooldown and no cost, so only resolution is under test. */
    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def) {
        cast(world, caster, def, FORWARD);
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def, Aim aim) {
        cast(world, caster, def, aim, () -> {});
    }

    /** With the basic-attack use listener that durability wear rides in production. */
    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def,
                             Runnable onBasicAttackUse) {
        cast(world, caster, def, FORWARD, onBasicAttackUse);
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def,
                             Aim aim, Runnable onBasicAttackUse) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "test", aim, java.util.Set.of(def.id())));
        new CastExecutor(world, onBasicAttackUse).execute(success);
    }

    /**
     * A dash impulses the caster: direction * speed horizontally, plus lift straight up. The
     * up component is not decoration -- without it a flat-ground dash is friction-damped to
     * ~half a block. Guards the wiring (that lift reaches the impulse), not the feel (that the
     * velocity travels 12 blocks), which is inherently boot. Drop the `+ lift` in the executor
     * and the y assertion reddens.
     */
    @Test
    void dashImpulsesTheCasterByDirectionTimesSpeedPlusLift() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster); // so world.combatant(casterId) resolves the dasher

        cast(world, caster, ability(new CastSpec.Dash(12, 1.6, 0.4,
                CastSpec.DashDirection.MOVEMENT_ELSE_FORWARD))); // FORWARD aim = +X, unit

        assertNotNull(caster.lastImpulse, "the dash must move the caster");
        assertEquals(1.6, caster.lastImpulse.x(), 1e-9, "horizontal drive = direction * speed");
        assertEquals(0.4, caster.lastImpulse.y(), 1e-9, "the up component is lift");
        assertEquals(0.0, caster.lastImpulse.z(), 1e-9);
    }

    // --- Rekindle: the reverse-facing dash that throws a forward fan of embers. ---

    private static EffectSpec.ThrowEmbers embers(List<Double> angles) {
        return new EffectSpec.ThrowEmbers(angles, 1.2, 0.25, "blaze_powder", 20,
                new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(8, "fire"))), null, null);
    }

    /**
     * The three embers fly along the caster's facing, rotated horizontally by each angle.
     * Pure geometry: break the rotation and the wings are no longer 25 degrees off centre.
     */
    @Test
    void emberFanIsFacingRotatedHorizontallyByEachAngle() {
        Vec3 facing = new Vec3(1, 0, 0);
        var dirs = EffectSpec.ThrowEmbers.fan(facing, List.of(0.0, 25.0, -25.0));

        double cos = Math.cos(Math.toRadians(25));
        double sin = Math.sin(Math.toRadians(25));

        assertEquals(3, dirs.size());
        assertEquals(1.0, dirs.get(0).x(), 1e-9, "the centre ember flies straight ahead");
        assertEquals(0.0, dirs.get(0).z(), 1e-9);

        for (Vec3 wing : List.of(dirs.get(1), dirs.get(2))) {
            assertEquals(0.0, wing.y(), 1e-9, "the fan is horizontal");
            assertEquals(cos, wing.dot(facing), 1e-9, "each wing is 25 degrees off centre");
        }
        // Pin the exact wings, not just their symmetry -- +25 and -25 land on OPPOSITE sides.
        // A rotation that flips the z sign is a mirror image that preserves symmetry, so only
        // pinning z catches it.
        assertEquals(-sin, dirs.get(1).z(), 1e-9, "+25 fans to -z");
        assertEquals(+sin, dirs.get(2).z(), 1e-9, "-25 fans to +z, the opposite side");
    }

    /**
     * The embers launch from where the caster STOOD, not where the dash is carrying them. The
     * origin is the pre-dash snapshot, so it survives the live entity drifting after the
     * snapshot was taken (as it does between the player-thread snapshot and the executor, and
     * as the impulse itself will). Read the live position instead and this reddens.
     */
    @Test
    void embersLaunchFromThePreDashSnapshotOriginNotTheLivePosition() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        var def = ability(new CastSpec.Dash(12, 2.3, 0.3, CastSpec.DashDirection.REVERSE_FACING),
                embers(List.of(0.0)));

        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "test", FORWARD, java.util.Set.of(def.id())));

        // The live caster is elsewhere by the time the executor runs.
        caster.moveTo(new Vec3(-5, 0, 0));
        new CastExecutor(world).execute(success);

        // The thrown item is recorded at its launch origin (FakeWorld has no flight physics).
        assertEquals(1, world.markerPositions.size(), "the ember must have been thrown");
        Vec3 launchedFrom = world.markerPositions.values().iterator().next();
        assertEquals(0.0, launchedFrom.x(), 1e-9,
                "embers launch from the pre-dash feet, not the drifted live position");
        assertEquals(0.0, launchedFrom.z(), 1e-9);
    }

    /**
     * A Self cast detonates at the caster's FEET, not at the aim's origin -- which in
     * production is their eye, a metre and a half higher (RpgCommand builds the Aim from
     * getEyeLocation(); a Combatant's position is getLocation()).
     *
     * Nothing pinned this before. Every other Self test heals, and Heal never reads the
     * origin; and in this fake the dummy's position happens to equal the aim origin. So a
     * refactor that resolved the Self origin from the Aim would have moved the detonation
     * 1.62 blocks upward in silence.
     *
     * The bystander is 0.5 blocks from the feet (inside a radius-1 burst) and 1.69 blocks
     * from the eye (outside it). It is damaged if and only if we detonate at the feet.
     */
    @Test
    void selfCastDetonatesAtTheCastersFeetNotTheirEye() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var bystander = new FakeWorld.Dummy(new Vec3(0.5, 0, 0));
        world.entities.add(caster);
        world.entities.add(bystander);

        Aim fromTheEye = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));

        cast(world, caster, ability(new CastSpec.Self(),
                new EffectSpec.Burst(1.0, List.of(new EffectSpec.Damage(10, "fire")))),
                fromTheEye);

        assertEquals(90, bystander.health, 1e-9,
                "the burst must be centred on the caster's feet, not on their eye");
        assertEquals(100, caster.health, 1e-9, "a burst never splashes its own caster");
    }

    @Test
    void selfCastTargetsTheCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(5, 64, 5));
        caster.health = 50;
        var bystander = new FakeWorld.Dummy(new Vec3(6, 64, 5));
        world.entities.add(caster);
        world.entities.add(bystander);

        cast(world, caster, ability(new CastSpec.Self(), new EffectSpec.Heal(20)));

        assertEquals(70, caster.health, 1e-9);
        assertEquals(100, bystander.health, 1e-9, "a self cast must not touch anyone else");
    }

    @Test
    void rayHitsTheFirstCombatantAlongIt() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var near = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        var far = new FakeWorld.Dummy(new Vec3(10, 0, 0));
        world.entities.add(caster);
        world.entities.add(near);
        world.entities.add(far);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(88, near.health, 1e-9);
        assertEquals(100, far.health, 1e-9, "the ray must stop at the first body");
    }

    @Test
    void rayIgnoresTheCasterStandingAtItsOrigin() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(caster);
        world.entities.add(target);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, caster.health, 1e-9);
        assertEquals(88, target.health, 1e-9);
    }

    @Test
    void rayMissesWhenNothingIsInTheWay() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var offToTheSide = new FakeWorld.Dummy(new Vec3(5, 9, 0));
        world.entities.add(offToTheSide);

        cast(world, caster, ability(new CastSpec.Ray(30),
                new EffectSpec.Damage(12, "fire"), new EffectSpec.Visual("boom")));

        // The miss detonates at the END of the aim, which is now a segment away: the ray
        // crosses x=16 and the second segment resolves a tick later.
        assertEquals(List.of(), world.presented, "the far end has not been reached yet");
        world.advanceTicks(1);

        assertEquals(100, offToTheSide.health, 1e-9);
        assertEquals(List.of("boom"), world.presented, "an untargeted effect still fires on a miss");
    }

    @Test
    void rayStopsAtAWall() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var behindTheWall = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(behindTheWall);
        world.blockDistance = 3; // wall between caster and target

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, behindTheWall.health, 1e-9, "terrain must block the ray");
    }

    @Test
    void rayDoesNotReachBeyondItsRange() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var distant = new FakeWorld.Dummy(new Vec3(40, 0, 0));
        world.entities.add(distant);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, distant.health, 1e-9);
    }

    /**
     * A ray no longer resolves in one trace. Its first chunk column runs inline on the cast
     * frame; every column after that costs a tick, because crossing a chunk plane means
     * handing the trace to the region that owns the next chunk.
     */
    @Test
    void rayCrossingAChunkPlaneCostsATick() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var beyondThePlane = new FakeWorld.Dummy(new Vec3(20, 0, 0)); // x=20 is column 1
        world.entities.add(caster);
        world.entities.add(beyondThePlane);

        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, beyondThePlane.health, 1e-9,
                "the first segment stops at x=16 and cannot see into the next column");

        world.advanceTicks(1);
        assertEquals(88, beyondThePlane.health, 1e-9, "the second segment strikes it");
    }

    /** Two planes crossed, two ticks. The cost of a ray varies with how far it reaches. */
    @Test
    void rayCrossingTwoChunkPlanesCostsTwoTicks() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var far = new FakeWorld.Dummy(new Vec3(35, 0, 0)); // column 2
        world.entities.add(caster);
        world.entities.add(far);

        cast(world, caster, ability(new CastSpec.Ray(40), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, far.health, 1e-9);
        world.advanceTicks(1);
        assertEquals(100, far.health, 1e-9, "still only in column 1");
        world.advanceTicks(1);
        assertEquals(88, far.health, 1e-9, "column 2, on tick 2");
    }

    /** Stopping at the first body is what makes an already-hit set unnecessary. */
    @Test
    void rayStrikesExactlyOneBodyEvenAcrossColumns() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var first = new FakeWorld.Dummy(new Vec3(20, 0, 0));  // column 1
        var second = new FakeWorld.Dummy(new Vec3(35, 0, 0)); // column 2
        world.entities.add(caster);
        world.entities.add(first);
        world.entities.add(second);

        cast(world, caster, ability(new CastSpec.Ray(40), new EffectSpec.Damage(12, "fire")));
        world.advanceTicks(10);

        assertEquals(88, first.health, 1e-9);
        assertEquals(100, second.health, 1e-9, "the walk stops at the first body");
        assertEquals(0, world.pendingTasks(), "and schedules no further segments");
    }

    /**
     * A KNOWN, ACCEPTED DEFECT. This test asserts the bug, not the fix.
     *
     * >>> If you fix this, the assertion INVERTS: expect 88, not 100. <<<
     *
     * Confining a segment to one chunk column means the trace only sees entities that
     * column's region owns. This mob's CENTRE is at x=16.05, in column 1. The ray runs up
     * the z axis at x=15.7, entirely inside column 0, and passes 0.35 blocks from the mob
     * -- comfortably inside the 0.6 hitRadius, so it WOULD be struck if it were visible.
     * It is not: column 0's segment cannot see into column 1, and the ray never enters it.
     *
     * The 0.35 matters. An earlier version of this test put the mob at x=16.3, exactly
     * hitRadius away, where 16.3 - 15.7 = 0.6000000000000014 and the fake skipped it for
     * floating-point reasons. It asserted the right answer for the wrong reason, and passed
     * even with the column filter deleted.
     *
     * So a hitbox straddling a chunk plane can be missed. Fixing it needs a widened trace,
     * or a second query into the neighbouring column. Both are out of scope; what is in
     * scope is that FakeWorld can now SEE this, where before it scanned the whole world and
     * could not.
     */
    @Test
    void rayMissesAnEntityWhoseCentreLiesAcrossAChunkPlane() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(new Vec3(15.7, 0, 0));
        var straddler = new FakeWorld.Dummy(new Vec3(16.05, 0, 5)); // centre in column 1
        world.entities.add(caster);
        world.entities.add(straddler);

        // Straight up +z, staying at x=15.7: never leaves column 0.
        Aim upTheZAxis = new Aim(new Vec3(15.7, 0, 0), new Vec3(0, 0, 1));
        cast(world, caster, ability(new CastSpec.Ray(20),
                new EffectSpec.Damage(12, "fire")), upTheZAxis);
        world.advanceTicks(10);

        assertTrue(straddler.position().subtract(new Vec3(15.7, 0, 5)).length() < world.hitRadius,
                "it is well inside hitRadius, so only the column check can hide it");
        assertEquals(100, straddler.health, 1e-9,
                "MISSED: its centre is in a column the ray never traces. See the javadoc.");
    }

    @Test
    void meleeHitsTheNearestTargetInsideTheArc() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var near = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var alsoInArc = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        world.entities.add(caster);
        world.entities.add(near);
        world.entities.add(alsoInArc);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

        assertEquals(88, near.health, 1e-9);
        assertEquals(100, alsoInArc.health, 1e-9, "melee strikes one target, the nearest");
    }

    /**
     * The unified-path invariant: a BASIC ATTACK (a Melee cast) and an ABILITY payload (a Ray cast)
     * both land their damage through the SAME one route -- {@code CombatantHandle.applyDamage(amount,
     * sourceId)} -- with the same call shape: one call, the same amount off custom current, the same
     * caster blamed. There is NO second damage route that a later popup / death (reachedZero) / custom
     * knockback would silently miss for one of the two sources.
     *
     * Guards against reintroducing a bypass: route ability damage (or basic-attack damage) through
     * anything other than applyDamage and one of these `damageCalls == 1` / attribution assertions
     * reddens.
     */
    @Test
    void basicAttackAndAbilityDealDamageThroughTheOneApplyDamageRoute() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        // Melee target sits inside the 90-degree swing (~42 degrees off aim) but 0.9 off the +X
        // axis -- wider than the ray's 0.6 hitRadius, so the ray passes it and strikes only the
        // one on the axis. Each cast lands on a DISTINCT target, so a double-hit is unambiguous.
        var meleeTarget = new FakeWorld.Dummy(new Vec3(1, 0, 0.9));
        var abilityTarget = new FakeWorld.Dummy(new Vec3(5, 0, 0)); // first body down the 30-block ray
        world.entities.add(caster);
        world.entities.add(meleeTarget);
        world.entities.add(abilityTarget);

        // Basic attack: a melee swing carrying the same 12-damage payload.
        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));
        // Ability: a ray carrying the identical payload. (abilityTarget is the first body along it.)
        cast(world, caster, ability(new CastSpec.Ray(30), new EffectSpec.Damage(12, "fire")));

        for (var landed : List.of(meleeTarget, abilityTarget)) {
            assertEquals(1, landed.damageCalls, "damage arrived exactly once, through the one port");
            assertEquals(88, landed.health, 1e-9, "the same 12 came off custom current for both sources");
            assertEquals(caster.id(), landed.lastDamageSource, "and both blame the same caster -- one call shape");
        }
    }

    @Test
    void meleeMissesSomeoneBehindTheCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var behind = new FakeWorld.Dummy(new Vec3(-1, 0, 0));
        world.entities.add(caster);
        world.entities.add(behind);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, behind.health, 1e-9);
    }

    /** A 90-degree swing reaches 45 degrees either side, so (1,0,1) is on the edge. */
    @Test
    void meleeArcWidthIsTheFullConeNotTheHalfAngle() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var justInside = new FakeWorld.Dummy(new Vec3(1, 0, 0.9)); // ~42 degrees off aim
        var justOutside = new FakeWorld.Dummy(new Vec3(1, 0, 1.1)); // ~48 degrees off aim
        world.entities.add(justInside);
        world.entities.add(justOutside);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

        assertEquals(88, justInside.health, 1e-9);
        assertEquals(100, justOutside.health, 1e-9);
    }

    @Test
    void meleeMissesBeyondItsReach() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var tooFar = new FakeWorld.Dummy(new Vec3(5, 0, 0));
        world.entities.add(tooFar);

        cast(world, caster, ability(new CastSpec.Melee(3, 90), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, tooFar.health, 1e-9);
    }

    @Test
    void meleeIgnoresTheCasterItself() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        cast(world, caster, ability(new CastSpec.Melee(3, 360), new EffectSpec.Damage(12, "fire")));

        assertEquals(100, caster.health, 1e-9);
    }

    @Test
    void aMissStillDetonatesAtTheEndOfTheAim() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        // Nothing in the ray's path, but something sitting where it ends.
        var atTheEnd = new FakeWorld.Dummy(new Vec3(30, 5, 0));
        world.entities.add(atTheEnd);

        cast(world, caster, ability(new CastSpec.Ray(30),
                new EffectSpec.Area(6.0, 20, 20, List.of(new EffectSpec.Damage(2, "fire")))));

        // The ray crosses x=16, so its far end resolves on tick 1, not on the cast frame.
        // The area placed there pulses one tick_interval later: 1 + 20 = 21.
        world.advanceTicks(21);

        assertEquals(98, atTheEnd.health, 1e-9, "the area should land at the ray's end point");
    }

    // --- WHAT COSTS A USE, AND WHEN. The rule durability wear rides: a basic attack charges, an
    // ability never does; melee charges on CONNECT, every other shape at COMMIT. Core owns the
    // whole rule (CastExecutor.execute) so the wiring cannot get it wrong or forget it, and these
    // are what hold it -- the paper half is a one-line durability charge no test can reach.

    /** A basic attack: reads the wielder's ATTACK_DAMAGE stat, which is what isBasicAttack asks. */
    private static EffectSpec.WeaponDamage basicAttack() {
        return new EffectSpec.WeaponDamage("kinetic");
    }

    /** An ability payload: carries its own authored amount. The emberblade Fireball's shape. */
    private static EffectSpec.Damage abilityPayload() {
        return new EffectSpec.Damage(12, "fire");
    }

    @Test
    void aConnectingMeleeSwingChargesOneUse() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);
        world.entities.add(new FakeWorld.Dummy(new Vec3(1, 0, 0)));

        var uses = new int[1];
        cast(world, caster, ability(new CastSpec.Melee(3, 120), basicAttack()), () -> uses[0]++);

        assertEquals(1, uses[0], "a swing that lands costs exactly one use");
    }

    /**
     * Miss is free -- the rule that forced the connect signal into core in the first place. The
     * paper half cannot answer it: by the time WeaponFire holds a Success, whether the arc resolved
     * a target is not yet known, and it is decided past the region hop.
     */
    @Test
    void aMeleeSwingThatLandsOnNothingIsFree() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);
        world.entities.add(new FakeWorld.Dummy(new Vec3(-1, 0, 0))); // behind, outside the arc

        var uses = new int[1];
        cast(world, caster, ability(new CastSpec.Melee(3, 120), basicAttack()), () -> uses[0]++);

        assertEquals(0, uses[0], "a swing that touches nothing costs nothing");
    }

    /**
     * THE DEDUP. One swing is one use however many bodies its payload reaches -- vanilla charges a
     * sword once for a sweep, not once per mob caught.
     *
     * No SHIPPED weapon can produce this: CastExecutor.meleeTarget resolves the single nearest body
     * in the arc, so ironblade and emberblade damage at most one thing per swing and the case
     * cannot be witnessed in-game without inventing a throwaway weapon for a hypothetical. A Burst
     * payload IS the reachable multi-target melee shape (void_slash is exactly this, as an
     * ability), so this test is the guard -- and it is a real one: move the onBasicAttackUse call
     * inside EffectApplier's per-entity loop and it reddens with 2.
     */
    @Test
    void oneMeleeSwingChargesOneUseHoweverManyItSplashes() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);
        world.entities.add(new FakeWorld.Dummy(new Vec3(1, 0, 0)));
        world.entities.add(new FakeWorld.Dummy(new Vec3(1, 0, 0.5)));

        var uses = new int[1];
        cast(world, caster, ability(new CastSpec.Melee(3, 120),
                        basicAttack(),
                        new EffectSpec.Burst(3.0, List.of(new EffectSpec.Damage(5, "fire")))),
                () -> uses[0]++);

        assertEquals(1, uses[0], "one swing, one use -- not one per body it splashes");
    }

    /**
     * The bow: an arrow costs the bow at LAUNCH, hit or miss, like vanilla. Nothing is in the
     * projectile's path here and the use is still charged on the cast frame, before any flight.
     */
    @Test
    void aProjectileBasicAttackChargesAtLaunchHitOrMiss() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        var uses = new int[1];
        cast(world, caster, ability(new CastSpec.Projectile(2.5, 0.05, 60), basicAttack()),
                () -> uses[0]++);

        assertEquals(1, uses[0], "the shot costs the bow at launch, with nothing to hit");
    }

    /**
     * THE GATE, on the shape that needs it most. emberblade's Fireball and hunters_bow's shot are
     * BOTH `type: projectile` -- the cast shape cannot tell them apart, only the payload can. An
     * ability already spends mana; charging it durability as well bills one press twice.
     *
     * Delete the isBasicAttack gate and this reddens, together with the melee case below.
     */
    @Test
    void anAbilityPayloadNeverChargesAUse() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        var uses = new int[1];
        cast(world, caster, ability(new CastSpec.Projectile(1.6, 0.03, 100), abilityPayload()),
                () -> uses[0]++);

        assertEquals(0, uses[0], "an ability spends mana, not durability");
    }

    /**
     * The gate again, at the OTHER call site. Written separately because the two are separate
     * `charges &&` conditions: gate the commit point and forget the connect point and only this
     * one reddens. A melee ability (void_slash's shape) that connects must still cost no use.
     */
    @Test
    void aMeleeAbilityNeverChargesAUseEvenOnConnect() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);
        world.entities.add(new FakeWorld.Dummy(new Vec3(1, 0, 0)));

        var uses = new int[1];
        cast(world, caster, ability(new CastSpec.Melee(3, 120), abilityPayload()), () -> uses[0]++);

        assertEquals(0, uses[0], "a melee ability that connects still spends no durability");
    }

    // ---- Melee line of sight. In range and in arc is not enough; the caster must be able to
    // see the body. Block-only, so a mob behind a mob is still fair game -- that half is
    // boot-witnessed, since with a fake world it would be tautological.

    /**
     * The bound must not be lowered by a candidate that then fails the sight check.
     *
     * REGISTRATION ORDER IS LOAD-BEARING. combatantsNear walks entities in insertion order, and
     * with the clear-far dummy seen first a bound-poisoning bug still leaves it as the target and
     * this test passes green. Walled-near first is the only order that reddens it: the walled body
     * lowers the bound to 1, the clear body at 4 no longer improves on it, and nothing is hit.
     */
    @Test
    void meleeSkipsAWalledNearerTargetForAClearFartherOne() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var walledNear = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var clearFar = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        world.entities.add(caster);
        world.entities.add(walledNear);   // FIRST, deliberately
        world.entities.add(clearFar);

        world.sightBlocked = (from, to) -> to.x() < 1.5;

        cast(world, caster, ability(new CastSpec.Melee(3, 120), abilityPayload()));

        assertEquals(100, walledNear.health, 1e-9,
                "a wall between us means the nearer body is not a target at all");
        assertEquals(88, clearFar.health, 1e-9,
                "and the swing still finds the nearest body it CAN see, rather than whiffing");
    }

    @Test
    void meleeFindsNoTargetWhenEveryCandidateIsWalled() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var near = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var far = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        world.entities.add(caster);
        world.entities.add(near);
        world.entities.add(far);

        world.sightBlocked = (from, to) -> true;

        var uses = new int[1];
        cast(world, caster, ability(new CastSpec.Melee(3, 120), basicAttack()), () -> uses[0]++);

        assertEquals(0, near.damageCalls, "nothing behind a wall is struck");
        assertEquals(0, far.damageCalls, "nothing behind a wall is struck");
        assertEquals(0, uses[0], "a swing that can see nothing is a miss, and a miss is free");
    }

    /**
     * Traced from the AIM ORIGIN, which production sets to the player's eye -- not from the
     * caster's feet, which would see under a wall the player is looking over the top of. Same
     * class of bug as a self cast detonating at the feet rather than the eye, one method along.
     */
    @Test
    void meleeSightIsTracedFromTheAimOriginNotTheCastersFeet() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);
        world.entities.add(new FakeWorld.Dummy(new Vec3(1, 0, 0)));

        var eye = new Vec3(0, 1.62, 0);
        cast(world, caster, ability(new CastSpec.Melee(3, 360), abilityPayload()),
                new Aim(eye, new Vec3(1, 0, 0)));

        assertTrue(world.sightCheckFrom.contains(eye),
                "sight starts at the aim origin -- the eye WeaponFire hands in");
        assertFalse(world.sightCheckFrom.contains(Vec3.ZERO),
                "and never at the caster's feet");
    }

    /**
     * And traced TO the target's own eye. The eye height comes off the snapshot, per entity, so
     * this reddens both if the offset is dropped (back to a floor-hugging feet trace) and if it is
     * replaced by a constant baked into CastExecutor -- which is why the fixture picks 1.4 rather
     * than the 1.62 default or a round 1.0.
     */
    @Test
    void meleeSightIsTracedToTheTargetsEyeNotItsFeet() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0.5, 0));
        target.eyeHeight = 1.4;
        world.entities.add(caster);
        world.entities.add(target);

        cast(world, caster, ability(new CastSpec.Melee(3, 360), abilityPayload()));

        var traced = world.sightCheckTo.get(0);
        assertEquals(1.0, traced.x(), 1e-9);
        assertEquals(0.0, traced.z(), 1e-9);
        assertEquals(target.position().y() + 1.4, traced.y(), 1e-9,
                "the endpoint is this target's own eye height above its feet");
    }
}
