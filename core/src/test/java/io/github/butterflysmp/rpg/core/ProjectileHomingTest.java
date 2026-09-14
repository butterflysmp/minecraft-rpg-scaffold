package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A bolt that CHASES. Slice F: the homing branch, the fields that feed it, and the mobs-only
 * filter -- all of it in {@code core}, none of it booted.
 *
 * <h2>EVERY ROW READS THE REAL CONSTANT</h2>
 *
 * <p>The five numbers below are the ones {@code PLAN-dragons-plume.md} carries, and they are
 * declared once here and used by every row rather than being retyped per test. Two of them are
 * <b>INHERITED AND UNJUDGED</b> (§5 of the plan) and two are RULED (R11, R12) -- and this file
 * judges none of them. It guards the MECHANISM: that the steer happens where it is supposed to, to
 * the thing it is supposed to, at the distance it is supposed to, and not at a player.
 *
 * <h2>WHAT IS OBSERVED, AND WHY IT IS THE RAY ORIGINS</h2>
 *
 * <p>{@code FakeWorld.castRayFrom} records the start of every segment the flight traced, so the
 * list IS the path, one entry per tick. That matters for the driven-body row: the difference
 * between consecutive origins is the displacement the flight actually committed to, and the vector
 * handed to {@code driveMarker} has to be the same one. A COUNT of either is blind to a divergence
 * between them.
 *
 * <h2>THE MUTATION MATRIX -- ALL RUN, NONE ASSERTED</h2>
 *
 * <pre>
 * MUT-STEER      the steer removed                      -> 3 rows   ballistic, player, retarget
 * MUT-PLAYER     the mobs-only skip deleted             -> 1 row    player        UNIQUE
 * MUT-ACTIVATE   the 15-block gate removed              -> 2 rows   ballistic, retarget
 * MUT-RETARGET   target cached at first acquisition     -> 1 row    retarget      UNIQUE
 * MUT-STRAY      targetless bolt flies flat             -> 2 rows   stray UNIQUE, speed
 * MUT-DRIVE      the steer applied to the DRIVEN vector -> 1 row    driven body   UNIQUE
 * MUT-RENORM     scaled by the LAUNCH speed             -> 2 rows   speed UNIQUE, retarget
 *
 * F2, the sight gate:
 * MUT-SIGHT       the sight check removed               -> 3 rows   all three sight rows
 * MUT-SIGHT-ORDER the check moved AFTER the comparison  -> 1 row    nearest-visible  UNIQUE
 * MUT-SIGHT-FROM  traced from the caster, not the bolt  -> 1 row    trace origin     UNIQUE
 * </pre>
 *
 * <p><b>{@code MUT-SIGHT-ORDER} PASSED ON ITS FIRST STAGING AND THE FIXTURE WAS MOVED, NOT THE
 * CODE.</b> With the blind mob nearer only at the moment of activation, the bolt flew PAST it, the
 * visible mob became the nearest a few ticks later, and the mutated loop picked it up after all --
 * <i>the defect healed itself before the row could see it</i>. The blind mob now sits closer to the
 * flight LINE, so it is nearer at every point of the path rather than at one moment of it. Same
 * hollowness {@code MUT-ACTIVATE} exposed in slice F, arriving through TIME instead of geometry.
 *
 * <p><b>{@code MUT-RENORM} IS NOT ON THE SLICE'S OWN LIST, AND IT IS WHY THE SPEED ROW EARNS ITS
 * KEEP.</b> The renormalisation decision is a SECOND AXIS of the same expression -- one splice moves
 * <i>whether the bolt steers</i>, another moves <i>what magnitude it keeps</i> -- and the six listed
 * mutations only reach the first. Count the axes before counting the mutations.
 *
 * <p><b>AND THE BALLISTIC ROW HAS NO UNIQUE KILL, WHICH DOES NOT MAKE IT REMOVABLE.</b> Everything
 * that reddens it also reddens another row. It is kept because it is the only row that states the
 * activation property DIRECTLY -- the others catch {@code MUT-ACTIVATE} through the accident of
 * their own fixture geometry, and a fixture moved for an unrelated reason would silently take that
 * coverage with it. A sweep that prunes rows by unique kills would delete exactly this one.
 */
class ProjectileHomingTest {

    /** R11: speed 2.5 and a 120-tick leash are the ruled reach. */
    private static final double SPEED = 2.5;
    private static final int LIFETIME = 120;
    /** R12: the arrow drops like an arrow. */
    private static final double GRAVITY = 0.05;
    /** INHERITED AND UNJUDGED -- the old repo's lerp, activation and search radius (plan §5). */
    private static final double LERP = 0.65;
    private static final double ACTIVATION = 15.0;
    private static final double RADIUS = 10.0;

    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    private static final CastSpec.Homing HOMING = new CastSpec.Homing(LERP, ACTIVATION, RADIUS);

    private static AbilityDefinition bolt(CastSpec.Homing homing, String item) {
        return new AbilityDefinition("plume", "Plume", "void", "ranger",
                0, ResourceCost.FREE,
                new CastSpec.Projectile(SPEED, GRAVITY, LIFETIME, null, item, homing),
                List.of(new EffectSpec.Damage(12, "void")));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "plume", FORWARD, Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    /** The path the flight actually flew: one entry per tick, the start of each traced segment. */
    private static List<Vec3> flyAndRecord(FakeWorld world, FakeWorld.Dummy caster,
                                           CastSpec.Homing homing) {
        cast(world, caster, bolt(homing, null));
        world.advanceTicks(LIFETIME + 5);
        return List.copyOf(world.castRayFrom);
    }

    private static FakeWorld.Dummy mobAt(Vec3 at) {
        return new FakeWorld.Dummy(at);
    }

    private static FakeWorld.Dummy playerAt(Vec3 at) {
        var dummy = new FakeWorld.Dummy(at);
        dummy.player = true;
        return dummy;
    }

    // ================================================================= MUT-STEER, MUT-ACTIVATE

    /**
     * BALLISTIC UNTIL THE ACTIVATION DISTANCE, THEN STEERING -- and the two halves are asserted
     * against each other rather than against numbers typed into this file.
     *
     * <p>The same cast is flown twice, once with a homing block and once without. <b>While the
     * plain bolt is inside {@code ACTIVATION} of its spawn the two paths must be
     * IDENTICAL</b> -- not close, identical, because before activation the homing branch must not
     * run at all. <b>And afterwards they must differ</b>, or nothing was being tested: that half is
     * what makes the row see {@code MUT-STEER}, and the first half is what makes it see
     * {@code MUT-ACTIVATE}.
     *
     * <p>Comparing two REAL flights is what keeps the row honest. A row asserting hand-computed
     * positions would be re-implementing the flight loop in the test, and would then agree with
     * itself about a defect they shared.
     *
     * <h2>IT TAKES TWO MOBS, AND THE FIRST VERSION OF THIS ROW WAS HOLLOW WITHOUT THE NEAR ONE</h2>
     *
     * <p>Written with only the far mob at {@code x = 30}, this row <b>passed under
     * {@code MUT-ACTIVATE}</b> -- the very mutation it was written for. The reason is a fixture
     * fact, not a code fact: <b>the search radius is 10 and the activation distance is 15</b>, so a
     * mob 30 blocks out is not a candidate until the bolt is 21 blocks downrange, <i>already past
     * activation</i>. Delete the gate and nothing changes, because the radius was the binding
     * constraint the whole time.
     *
     * <p>So a second mob sits <b>inside the search radius of the SPAWN POINT</b> ({@code 8.94}
     * blocks out), where only the activation gate keeps the bolt off it. <b>The mutation was run
     * again after the restaging and the row went red</b>, which is the only reason its javadoc is
     * allowed to say it guards activation.
     *
     * <p><i>A guard written against a trap someone described to you is the likeliest to be hollow.</i>
     */
    @Test
    void aHomingBoltIsBallisticUntilTheActivationDistanceAndThenIsNot() {
        var plainWorld = new FakeWorld();
        plainWorld.entities.add(mobAt(new Vec3(8, 0, 4)));      // inside RADIUS of the SPAWN
        plainWorld.entities.add(mobAt(new Vec3(30, 0, 5)));     // only reachable later
        List<Vec3> plain = flyAndRecord(plainWorld, new FakeWorld.Dummy(Vec3.ZERO), null);

        var homingWorld = new FakeWorld();
        homingWorld.entities.add(mobAt(new Vec3(8, 0, 4)));
        homingWorld.entities.add(mobAt(new Vec3(30, 0, 5)));
        List<Vec3> homing = flyAndRecord(homingWorld, new FakeWorld.Dummy(Vec3.ZERO), HOMING);

        int compared = 0;
        for (int i = 0; i < plain.size() && i < homing.size(); i++) {
            if (plain.get(i).distanceSquared(Vec3.ZERO) >= ACTIVATION * ACTIVATION) break;
            assertEquals(plain.get(i), homing.get(i),
                    "tick " + i + " is inside the activation distance, so the homing bolt must fly"
                            + " exactly where a plain one does -- the branch may not run at all yet");
            compared++;
        }
        assertTrue(compared >= 2,
                "the fixture must give at least two pre-activation ticks to compare, or this row"
                        + " passes without having looked at anything; it compared " + compared);

        // The paths are compared by FIRST DIVERGENCE rather than with assertNotEquals, which on
        // failure prints both hundred-entry lists and buries the one fact anybody needs.
        int divergedAt = -1;
        for (int i = 0; i < Math.min(plain.size(), homing.size()); i++) {
            if (!plain.get(i).equals(homing.get(i))) { divergedAt = i; break; }
        }
        assertTrue(divergedAt >= 0 || plain.size() != homing.size(),
                "past the activation distance the bolt must steer -- the two paths never diverged,"
                        + " over " + plain.size() + " ticks, so the steer never happened");
    }

    // ============================================================================== MUT-PLAYER

    /**
     * R2: MOBS ONLY -- and the fixture is staged so a broken filter LOSES rather than being
     * rescued by the distance comparison.
     *
     * <p><b>The player is NEARER than the mob and is registered FIRST.</b> Both halves matter. A
     * player standing further away would never win the nearest comparison, so the row would pass
     * with the skip deleted; and iteration order decides which candidate a broken loop happens to
     * hold when it reaches the end. This staging is the one where the wrong answer is reachable.
     *
     * <p>They sit on OPPOSITE SIDES of the flight line, so the assertion is a SIGN rather than a
     * distance: chase the mob and z goes positive, chase the player and z goes negative. No
     * floating-point prediction is involved in reading that.
     */
    @Test
    void aBoltNeverChasesAPlayerEvenWhenThePlayerIsNearer() {
        var world = new FakeWorld();
        world.entities.add(playerAt(new Vec3(25, 0, -3)));   // NEARER, and first in the list
        world.entities.add(mobAt(new Vec3(30, 0, 9)));

        List<Vec3> path = flyAndRecord(world, new FakeWorld.Dummy(Vec3.ZERO), HOMING);

        double finalZ = path.get(path.size() - 1).z();
        assertTrue(finalZ > 0,
                "the bolt must turn toward the MOB at +z, not the nearer player at -z; it ended at"
                        + " z=" + finalZ);
    }

    // ============================================================================ MUT-RETARGET

    /**
     * THE TARGET IS RE-CHOSEN EVERY TICK, so a mob that walks out of the chase loses it.
     *
     * <p>Two mobs on opposite sides of the flight line. The near one is acquired first, then moved
     * far away mid-flight; a bolt that re-chooses must swing to the other one, and one holding the
     * target it acquired must not. <b>The correct answer is -z and every wrong one is +z</b> --
     * whether a cached target is followed to its new position or chased at the frozen snapshot it
     * was picked with.
     *
     * <h2>THE STAGING WAS MEASURED, AND TWO EARLIER ONES FAILED FOR A REASON WORTH KNOWING</h2>
     *
     * <p>With the mobs at +/-6 blocks, and again at +/-5, <b>the bolt's own turn carried the second
     * mob out of the 10-block sphere</b>: by the time the first target was acquired and the lerp had
     * swung the bolt toward it, the other mob was 11-12 blocks away and there was nothing left to
     * re-choose. The row went red against correct code. <b>That is a fact about the inherited
     * constants -- a 0.65 lerp turns hard enough to abandon everything on the far side -- not about
     * the mechanism this row guards</b>, so the fixture was moved rather than the code.
     *
     * <p>So both mobs sit close to the line, the second one FURTHER DOWNRANGE, and the numbers were
     * read off a probe rather than reasoned about. <b>The probe also ran the control</b>: with no
     * move at all, the same fixture ends at <b>z = +1.60</b> on the first mob. So the wrong answer
     * really is the opposite sign, and this row is not measuring its own fixture.
     */
    @Test
    void theTargetIsRechosenEveryTickRatherThanHeldFromAcquisition() {
        var world = new FakeWorld();
        var walksAway = mobAt(new Vec3(20, 0, 2));
        world.entities.add(walksAway);
        world.entities.add(mobAt(new Vec3(26, 0, -2)));

        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        cast(world, caster, bolt(HOMING, null));

        world.advanceTicks(7);                       // past ACTIVATION (15 blocks at 2.5 a tick)
        Vec3 atMove = world.castRayFrom.get(world.castRayFrom.size() - 1);
        assertTrue(atMove.x() >= ACTIVATION,
                "the fixture must have passed the activation distance before the mob walks off,"
                        + " or nothing was ever acquired and this row proves nothing");
        assertTrue(atMove.z() > 0,
                "the bolt must ALREADY be turning toward the first mob at +z when it walks away --"
                        + " otherwise the row would pass on a bolt that never acquired anything;"
                        + " it was at z=" + atMove.z());

        walksAway.moveTo(new Vec3(20, 0, 200));
        world.advanceTicks(LIFETIME);

        double finalZ = world.castRayFrom.get(world.castRayFrom.size() - 1).z();
        assertTrue(finalZ < 0,
                "with its first target gone the bolt must re-choose the remaining mob at -z; it"
                        + " ended at z=" + finalZ);
    }

    // =============================================================================== MUT-STRAY

    /**
     * A TARGETLESS BOLT KEEPS ITS BALLISTIC BEHAVIOUR -- operator's ruling, plan §3.3, and the
     * common case, because it is every shot that misses.
     *
     * <p>The drop is asserted against the RECURRENCE rather than against a table of numbers: the
     * flight adds gravity to the velocity after each step, so the vertical displacement of tick
     * {@code k} is exactly {@code -g*k}. A bolt that sails flat gives zero for every one of them.
     *
     * <p>The world is EMPTY, so this is also the row that proves the homing branch does nothing
     * when there is nothing to chase -- the seek is present and finds no target.
     */
    @Test
    void aTargetlessBoltKeepsFallingRatherThanSailingFlat() {
        var world = new FakeWorld();
        List<Vec3> path = flyAndRecord(world, new FakeWorld.Dummy(Vec3.ZERO), HOMING);

        assertTrue(path.size() > 5, "need several ticks of flight to read a drop");
        for (int k = 0; k + 1 < path.size(); k++) {
            double fell = path.get(k + 1).y() - path.get(k).y();
            assertEquals(-(GRAVITY * k), fell, 1e-9,
                    "tick " + k + ": a targetless bolt falls under gravity like any other"
                            + " projectile -- it does not sail flat");
        }
    }

    // =============================================================================== MUT-DRIVE

    /**
     * THE BODY IS DRIVEN ALONG THE SEGMENT THE RAY TRACED, and this is the row for the trap.
     *
     * <p>{@code velocity} is the displacement already travelled this tick: {@code next} is computed
     * from it, {@code castRay} traced exactly that segment, and {@code driveMarker} is handed the
     * same vector. <b>Apply the steer to it and the hit is computed along one line while the body
     * is drawn along another</b> -- a divergence no count can see, on a bolt too fast for anyone to
     * watch.
     *
     * <p>So the assertion is a cross-check between two independent recordings: the vector the port
     * was handed, against the difference between the ray origins either side of it. They are the
     * same quantity recorded by two different methods of {@code FakeWorld}, which is exactly what
     * makes the row able to see them disagree.
     */
    @Test
    void theBodyIsDrivenAlongTheSegmentTheRayTraced() {
        var world = new FakeWorld();
        world.entities.add(mobAt(new Vec3(30, 0, 5)));

        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        cast(world, caster, bolt(HOMING, "arrow"));
        world.advanceTicks(LIFETIME + 5);

        assertEquals(1, world.markersEverSpawned.size(), "one body, one flight");
        UUID body = world.markersEverSpawned.get(0);
        List<Vec3> driven = world.markerVelocities.getOrDefault(body, List.of());
        List<Vec3> origins = world.castRayFrom;

        assertTrue(driven.size() >= 5,
                "need several driven ticks to compare; got " + driven.size());
        assertTrue(origins.size() > driven.size(),
                "every driven tick is followed by another traced segment");

        for (int i = 0; i < driven.size(); i++) {
            Vec3 traced = origins.get(i + 1).subtract(origins.get(i));
            assertEquals(traced.x(), driven.get(i).x(), 1e-9, "tick " + i + " x");
            assertEquals(traced.y(), driven.get(i).y(), 1e-9, "tick " + i + " y");
            assertEquals(traced.z(), driven.get(i).z(), 1e-9, "tick " + i + " z");
        }
    }

    // ======================================================================== THE SIGHT GATE, F2

    /**
     * A BOLT MUST BE ABLE TO SEE WHAT IT CHASES -- ruled, and behind a wall the bolt simply flies
     * on.
     *
     * <p>The comparison is against a REAL plain flight rather than against numbers: with the only
     * mob invisible, a bolt with a homing block must trace <b>exactly</b> the path of a bolt with
     * none. Not close -- identical, because §3.3's targetless case is what an unsightable mob falls
     * back to.
     */
    @Test
    void aBoltDoesNotChaseAMobItCannotSee() {
        var plainWorld = new FakeWorld();
        plainWorld.entities.add(mobAt(new Vec3(30, 0, 5)));
        List<Vec3> plain = flyAndRecord(plainWorld, new FakeWorld.Dummy(Vec3.ZERO), null);

        var blindWorld = new FakeWorld();
        blindWorld.entities.add(mobAt(new Vec3(30, 0, 5)));
        blindWorld.sightBlocked = (from, to) -> true;          // a wall in front of everything
        List<Vec3> blind = flyAndRecord(blindWorld, new FakeWorld.Dummy(Vec3.ZERO), HOMING);

        assertEquals(plain, blind,
                "a mob the bolt cannot see is not a candidate, so the bolt flies on ballistically"
                        + " -- the path must be the plain one, tick for tick");
        assertFalse(blindWorld.sightCheckFrom.isEmpty(),
                "and the gate must actually have been consulted -- an empty trace list means the"
                        + " paths matched because nothing ever looked, which is the same picture");
    }

    /**
     * THE NEAREST *VISIBLE* MOB IS TAKEN, NEVER THE NEAREST ONE THEN CHECKED.
     *
     * <p>The staging is the player row's, for the same reason and with the same two halves: <b>the
     * invisible mob is NEARER and registered FIRST</b>. A fixture where the wrong answer loses on
     * distance proves nothing.
     *
     * <p>The two failures are different and the row separates them by SIGN. Take the nearest and
     * then discard it for being blind, and the bolt chases <b>nothing</b> -- it flies straight and
     * ends at {@code z = 0}. Ignore sight altogether and it chases the hidden mob at <b>-z</b>. The
     * correct answer is the visible mob at <b>+z</b>.
     *
     * <h2>THE HIDDEN MOB MUST STAY NEARER, AND THE FIRST STAGING FAILED ON EXACTLY THAT</h2>
     *
     * <p>Staged with the hidden mob at {@code (18, -2)} and the visible one at {@code (20, 2)},
     * this row <b>passed under {@code MUT-SIGHT-ORDER}</b>. The hidden mob was nearer at the moment
     * the bolt activated -- and then the bolt flew PAST it, the visible mob became the nearest a few
     * ticks later, and the mutated loop picked it up and chased it after all. <b>The defect healed
     * itself before the row could see it.</b>
     *
     * <p>So both mobs now sit at the same {@code x} with the hidden one CLOSER TO THE FLIGHT LINE
     * ({@code |z| = 2} against {@code 3}), which makes it nearer at every point on the bolt's path
     * rather than at one moment of it. <b>The mutation was re-run against the new staging and the
     * row went red.</b>
     *
     * <p><i>It is the same hollowness {@code MUT-ACTIVATE} exposed in slice F, arriving through TIME
     * instead of through geometry: a fixture where the wrong answer stops being wrong proves nothing
     * either.</i>
     */
    @Test
    void theNearestVISIBLEMobIsTakenRatherThanTheNearestOneThenChecked() {
        var world = new FakeWorld();
        world.entities.add(mobAt(new Vec3(30, 0, -2)));        // NEARER at every point, blind, first
        world.entities.add(mobAt(new Vec3(30, 0, 3)));         // further off the line, in plain view
        world.sightBlocked = (from, to) -> to.z() < 0;         // the wall stands in front of -z only

        List<Vec3> path = flyAndRecord(world, new FakeWorld.Dummy(Vec3.ZERO), HOMING);

        double finalZ = path.get(path.size() - 1).z();
        assertTrue(finalZ > 0,
                "the bolt must take the nearest VISIBLE mob at +z. z = 0 means it took the blind"
                        + " nearest and then discarded it, chasing nothing while a valid target"
                        + " stood in range; z < 0 means it ignored sight entirely. It ended at z="
                        + finalZ);
    }

    /**
     * THE TRACE STARTS AT THE BOLT, NOT AT THE CASTER -- and the row asserts the ORIGIN the
     * instrument was handed rather than only the outcome.
     *
     * <p>{@code CastExecutor}'s melee sweep traces from the shooter's eye because it picks targets
     * at the moment of the cast. <b>A bolt is somewhere else entirely by tick 40</b>, and tracing
     * from the eye would make it refuse a mob it is staring straight at because a wall stands
     * between that mob and the player who fired.
     *
     * <p>So: every recorded trace origin must be a point on the bolt's own path, and none may be
     * the launch origin -- which is the caster's eye, and which the bolt leaves on tick one. This
     * is the same move that made the driven-body row able to see its mutation: <b>assert what the
     * instrument was handed, not just what came back.</b>
     */
    @Test
    void theSightTraceStartsAtTheBoltAndNeverAtTheCaster() {
        var world = new FakeWorld();
        world.entities.add(mobAt(new Vec3(30, 0, 5)));

        List<Vec3> path = flyAndRecord(world, new FakeWorld.Dummy(Vec3.ZERO), HOMING);

        assertFalse(world.sightCheckFrom.isEmpty(), "the gate must have been consulted at all");
        for (Vec3 origin : world.sightCheckFrom) {
            assertNotEquals(Vec3.ZERO, origin,
                    "no sight trace may start at the launch origin -- that is the CASTER'S eye,"
                            + " and the bolt left it on tick one");
            assertTrue(path.contains(origin),
                    "every trace must start at a position the bolt actually occupied; " + origin
                            + " is not on its path");
        }
    }

    // ===================================================== THE RENORMALISATION DECISION, PINNED

    /**
     * THE STEER KEEPS THE SPEED THE BOLT HAS, NOT THE SPEED IT LAUNCHED WITH.
     *
     * <p>The inherited algorithm says <i>"renormalised to the ORIGINAL speed"</i>, which fights R12:
     * a magnitude pinned to the launch speed means gravity can only ever bend the direction and
     * never accelerate the fall. The flight scales by {@code velocity.length()} instead -- the
     * speed the bolt actually has, gravity included.
     *
     * <p><b>This row is what makes that a decision rather than a sentence in a javadoc.</b> Fired
     * flat, the bolt's speed can only grow: gravity adds a downward component to a velocity that
     * has none at launch. So under the implemented form the steered segments are LONGER than the
     * launch speed, and under the form that was not taken every one of them would be exactly
     * {@code SPEED}. Reverse the decision and this row goes red, which is the point of it.
     */
    @Test
    void theSteerPreservesTheCurrentSpeedRatherThanTheLaunchSpeed() {
        var world = new FakeWorld();
        world.entities.add(mobAt(new Vec3(30, 0, 5)));

        List<Vec3> path = flyAndRecord(world, new FakeWorld.Dummy(Vec3.ZERO), HOMING);

        List<Double> steeredLengths = new ArrayList<>();
        for (int i = 0; i + 1 < path.size(); i++) {
            if (path.get(i).distanceSquared(Vec3.ZERO) < ACTIVATION * ACTIVATION) continue;
            steeredLengths.add(path.get(i + 1).subtract(path.get(i)).length());
        }

        assertFalse(steeredLengths.isEmpty(), "the fixture must produce at least one steered tick");
        assertTrue(steeredLengths.stream().allMatch(len -> len > SPEED),
                "every steered step must be longer than the launch speed -- pinning the magnitude"
                        + " to SPEED is the alternative this decision did not take; got "
                        + steeredLengths);
    }
}
