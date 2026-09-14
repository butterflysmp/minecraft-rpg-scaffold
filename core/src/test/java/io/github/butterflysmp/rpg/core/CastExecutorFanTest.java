package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.*;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.DrawFan;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ONE PRESS, SEVERAL ARROWS: {@code CastExecutor.executeFan}.
 *
 * <h2>THIS FILE EXISTS BECAUSE THE OBVIOUS WIRING IS WRONG TWICE AND BOTH ARE QUIET</h2>
 *
 * <p>Firing five arrows by calling {@code execute} five times is what anyone would write first, and
 * it double-bills two things that belong to the PRESS rather than to the arrow:
 *
 * <pre>
 * on_cast effects     FIVE sounds for one release
 * onBasicAttackUse    FIVE durability uses for one release
 * </pre>
 *
 * <p><b>Neither would have failed a test</b> -- the arrows are correct, the damage is correct, and
 * the weapon simply wears out five times faster than it should while the release sounds like a
 * machine gun. {@code aFannedReleaseIsONESoundAndONEUse} is the row that makes the loop wrong
 * instead of merely inefficient.
 *
 * <h2>THE MUTATION MATRIX -- ALL RUN, NONE ASSERTED</h2>
 *
 * <pre>
 * MUT-PERSHOT   commit() moved INSIDE the loop -- the obvious wiring     -> 1 row   UNIQUE
 * MUT-NOROTATE  rotateAboutY(yaw) -> rotateAboutY(0), the fan collapses  -> 1 row   UNIQUE
 * MUT-EMPTY     the empty-array guard removed                           -> 1 row   UNIQUE
 * MUT-ONESHOT   the loop runs once instead of per offset                 -> 2 rows
 * </pre>
 *
 * <p><b>Three unique kills in four mutations is not over-coverage here -- it is four independent
 * decisions</b>, each of which could be got wrong without touching the others: what is per-press,
 * what the fan does to the aim, what an empty release costs, and how many shots leave.
 *
 * <blockquote><b>AND THE MEASUREMENT THAT MAKES THE POINT: UNDER {@code MUT-PERSHOT}, ALL 44 ROWS OF
 * {@code CastExecutorTest} AND ALL 14 OF {@code CastExecutorVolleyTest} STAY GREEN.</b> The obvious
 * wiring -- five {@code execute} calls -- breaks nothing that existed before this file. It fires the
 * right arrows, deals the right damage, and quietly bills the bow five times and plays the release
 * sound five times. <b>One row in this file is the entire difference between that shipping and not
 * shipping</b>, which is why it is named for what it asserts rather than for the method it
 * covers.</blockquote>
 */
class CastExecutorFanTest {

    /** An aim from an eye, pointing along +X, so a yaw rotation moves Z and leaves Y alone. */
    private static final Aim EYE_FORWARD = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));

    /**
     * A projectile carrying a weapon_damage payload, so it counts as a basic attack and CHARGES.
     *
     * <p><b>It authors an {@code item}, and that is load-bearing for this file rather than
     * decoration:</b> {@code ProjectileFlight} spawns a marker only for a bolt with a rendered body,
     * so a bodiless projectile leaves NOTHING for a test to count. The first draft used one and
     * every arrow-counting row read zero -- which looked like the fan failing to fire and was the
     * fixture failing to be observable.
     */
    private static AbilityDefinition bolt(String castVisual) {
        return new AbilityDefinition("plume", "Plume", "void", "ranger",
                0, ResourceCost.FREE, new CastSpec.Projectile(2.5, 0.05, 120, null, "arrow"),
                List.of(new EffectSpec.WeaponDamage("void")), List.of(),
                castVisual == null ? List.of() : List.of(new EffectSpec.Visual(castVisual)));
    }

    private record Fired(FakeWorld world, int uses) {}

    private static Fired fan(AbilityDefinition def, double[] offsets) {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), EYE_FORWARD, Set.of(def.id())));

        int[] uses = {0};
        new CastExecutor(world, () -> uses[0]++).executeFan(success, offsets);
        return new Fired(world, uses[0]);
    }

    /**
     * THE HEADLINE: five arrows leave, and the press is billed ONCE.
     *
     * <p>Five markers spawn -- one body per arrow -- while the cast visual is presented once and the
     * use listener runs once. <b>The two "once"s are the whole reason {@code executeFan} exists
     * rather than a five-iteration loop at the call site.</b>
     *
     * <p>Five and one are deliberately different numbers, so a row that counted the wrong thing
     * could not pass by coincidence.
     */
    @Test
    void aFannedReleaseIsONESoundAndONEUse() {
        Fired fired = fan(bolt("plume_cast"), DrawFan.offsetsFor(5));

        assertEquals(5, fired.world().markersEverSpawned.size(),
                "five arrows leave the bow -- one rendered body each");
        assertEquals(List.of("plume_cast"), fired.world().presented,
                "and you hear the release ONCE. Five copies here is the whole defect this method "
                        + "exists to prevent, and nothing else in the suite would have caught it");
        assertEquals(1, fired.uses(),
                "one press, one durability use -- an arrow costs the bow, and this was one press");
    }

    /**
     * THE FAN REACHES THE ARROWS: five distinct launch directions, matching the ruled angles.
     *
     * <p>Asserted through the marker VELOCITIES, which is where the direction actually shows up --
     * the offsets could be computed correctly and then dropped on the floor between
     * {@code DrawFan} and the flight, and every other row here would stay green.
     */
    @Test
    void eachArrowLeavesAlongItsOwnRotatedAim() {
        Fired fired = fan(bolt(null), DrawFan.offsetsFor(5));

        List<Vec3> launches = new ArrayList<>();
        for (var id : fired.world().markersEverSpawned) {
            launches.add(fired.world().markerVelocities.get(id).get(0));
        }
        assertEquals(5, launches.size());

        // The aim is +X. A yaw of 0 must stay on +X; the others must not, and must be symmetric
        // about it -- which is what "centred on the aim" means in vectors rather than in degrees.
        double[] offsets = DrawFan.offsetsFor(5);
        for (int i = 0; i < 5; i++) {
            Vec3 expected = EYE_FORWARD.direction().rotateAboutY(offsets[i]).scale(2.5);
            assertEquals(expected.x(), launches.get(i).x(), 1e-9, "arrow " + i + " x");
            assertEquals(expected.z(), launches.get(i).z(), 1e-9, "arrow " + i + " z");
        }

        assertEquals(5, launches.stream().map(v -> Math.round(v.z() * 1e6)).distinct().count(),
                "and the five are genuinely DIFFERENT directions -- a fan that computed the angles "
                        + "and then launched them all down the same line would pass every other row");
    }

    /**
     * AN EMPTY RELEASE COSTS NOTHING -- no arrow, no sound, no durability.
     *
     * <p>{@code DrawFan.offsetsFor(0)} is an empty array, and {@code DrawRelease} can produce a
     * release of nothing (below the floor, or an empty magazine). <b>Committing on an empty fan
     * would bill the bow for a press that produced no arrow</b>, which is the one case where the
     * per-press rule gives the wrong answer if applied blindly.
     */
    @Test
    void anEmptyFanFiresNothingAndCOMMITSNothing() {
        Fired fired = fan(bolt("plume_cast"), DrawFan.offsetsFor(0));

        assertEquals(0, fired.world().markersEverSpawned.size(), "no arrows");
        assertEquals(List.of(), fired.world().presented, "no sound -- nothing was released");
        assertEquals(0, fired.uses(), "and the bow is not billed for an arrow nobody got");
    }

    /**
     * A ONE-ARROW FAN IS THE TAP, AND IT IS AN ORDINARY SINGLE CAST.
     *
     * <p>R4' fires one arrow down the aim. {@code DrawFan.offsetsFor(1)} is {@code {0}}, and a zero
     * rotation must be the exact identity -- otherwise every tap in the game leaves along a
     * direction a hair off the crosshair, which no player would ever report as a bug and no other
     * row here would catch.
     */
    @Test
    void aSingleArrowFanLeavesExactlyAlongTheAim() {
        Fired fired = fan(bolt(null), DrawFan.offsetsFor(1));

        assertEquals(1, fired.world().markersEverSpawned.size());
        assertEquals(1, fired.uses());

        Vec3 launch = fired.world().markerVelocities.get(fired.world().markersEverSpawned.get(0)).get(0);
        assertEquals(2.5, launch.x(), 1e-12, "straight down the aim, to full precision");
        assertEquals(0.0, launch.z(), 1e-12, "and not a hair off it");
    }
}
