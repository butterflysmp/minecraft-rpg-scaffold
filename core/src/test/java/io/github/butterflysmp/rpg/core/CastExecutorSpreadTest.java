package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.*;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ONE PRESS, SEVEN BODIES: {@code CastExecutor.launch}'s spread branch.
 *
 * <h2>*** THIS FILE EXISTS BECAUSE A MUTATION CAME BACK GREEN, AND IT IS THE WORST GREEN OF THE
 * SLICE ***</h2>
 *
 * <p>2026-09-21. {@code MUT14NOSPREAD} disabled the spread branch outright -- the guard became
 * {@code if (false)}, so every spread fired ONE body -- and <b>the entire suite stayed green at
 * 2148 tests.</b>
 *
 * <p><b>Every other check in the slice kept passing for a reason that made it useless here:</b>
 *
 * <pre>
 * SpreadPatternTest        calls SpreadPattern DIRECTLY -- it never asks whether anything calls it
 * ScatterShotContentTest   reads the YAML -- the block parses and stores whether or not it is used
 * WeaponLoreLinesTest      reads the tooltip path -- a different method, still saying "x 7"
 * GoldenLoreTest           renders the tooltip -- still "Kinetic Damage: 9 x 7"
 * </pre>
 *
 * <p>So the Scatter Shot would have shipped <b>firing a single arrow while its tooltip promised
 * seven</b>, and the only thing that could have caught it is a boot. That is
 * {@code CLAUDE.md}'s <i>zero callers on a new accessor</i> defect in its purest form: a value
 * authored, parsed, stored, and read by nobody -- <b>indistinguishable from one the loader silently
 * drops.</b>
 *
 * <p><b>The green run is the measurement and this file is the receipt.</b> The identical mutation
 * was re-applied after these rows were written and it reddened.
 *
 * <h2>WHAT IS COUNTED, AND WHY IT IS MARKERS RATHER THAN DAMAGE</h2>
 *
 * <p>Damage cannot see this. Seven arrows that all miss deal nothing, and one arrow that hits deals
 * the same as one arrow that hits -- so a damage-counting row would be reading the FIXTURE'S aim
 * rather than the spread. <b>The bodies are counted where they are spawned</b>, and their
 * directions are read off the launch velocities, which is where a dropped basis would actually
 * show up.
 *
 * <p>The fixture authors {@code item: "arrow"} for the reason {@code CastExecutorFanTest} records
 * in its own: {@code ProjectileFlight} spawns a marker only for a bolt with a rendered body, so a
 * bodiless projectile leaves NOTHING to count and every row would read zero -- which looks like the
 * spread failing to fire and is the fixture failing to be observable.
 */
class CastExecutorSpreadTest {

    /** An aim from an eye pointing along +X, with the shooter's right supplied as -Z. */
    private static final Aim EYE_FORWARD =
            new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0), new Vec3(0, 0, -1));

    /** The shipped ring, read off {@code scatter_shot.yml}: seven bodies at five degrees. */
    private static final CastSpec.Spread SHIPPED = new CastSpec.Spread(7, 5);

    /**
     * A projectile carrying a LITERAL damage payload, like the Scatter Shot's.
     *
     * @param spread null for the control -- a plain bolt, and the same fixture otherwise
     */
    private static AbilityDefinition bolt(CastSpec.Spread spread) {
        return new AbilityDefinition("scatter_shot", "Scatter Shot", "kinetic", "ranger",
                32, ResourceCost.FREE,
                new CastSpec.Projectile(2.5, 0.05, 120, null, "arrow", null, null, spread),
                List.of(new EffectSpec.Damage(9, "kinetic")), List.of(),
                List.of(new EffectSpec.Visual("scatter_cast")));
    }

    private record Fired(FakeWorld world, int uses) {}

    private static Fired fire(AbilityDefinition def) {
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
        new CastExecutor(world, () -> uses[0]++).execute(success);
        return new Fired(world, uses[0]);
    }

    /**
     * *** THE HEADLINE, AND THE ROW {@code MUT14NOSPREAD} EXISTS FOR: SEVEN BODIES LEAVE. ***
     *
     * <p>Seven markers spawn for one press. Under the mutation this read ONE, and nothing else in
     * either module noticed.
     *
     * <p>Seven, one and 32 are deliberately different numbers, so a row that counted the wrong
     * thing could not pass by coincidence.
     */
    @Test
    void oneSpreadPressPutsSevenBodiesInTheAir() {
        Fired fired = fire(bolt(SHIPPED));

        assertEquals(7, fired.world().markersEverSpawned.size(),
                "seven arrows leave the crossbow -- one rendered body each. Reading ONE here is"
                        + " the weapon firing a single arrow while its tooltip promises x 7");
    }

    /**
     * A PROJECTILE WITHOUT A SPREAD STILL FIRES EXACTLY ONE -- the control.
     *
     * <p>Without this, the row above passes on a build that fired seven bodies for EVERY
     * projectile, which would quietly septuple the Plume, the Flint Staff and both dev stones.
     * <b>The two fixtures differ in exactly one field.</b>
     */
    @Test
    void aProjectileWithoutASpreadStillFiresExactlyOne() {
        Fired fired = fire(bolt(null));

        assertEquals(1, fired.world().markersEverSpawned.size(),
                "a bolt with no spread block is one bolt -- every projectile shipped before the"
                        + " Scatter Shot");
    }

    /**
     * *** ONE PRESS IS ONE SOUND AND ONE USE, THOUGH IT IS SEVEN BODIES. ***
     *
     * <p>The same property {@code executeFan} was built for, arriving through a different door and
     * needing its own row: the spread expands BELOW {@code commit}, so the {@code on_cast} visual
     * and the durability charge happen once. <b>An implementation that expanded the spread ABOVE
     * the commit would fire the right seven arrows, deal the right damage, and bill the press seven
     * times</b> -- which is exactly the quiet double-billing {@code CastExecutorFanTest} records.
     *
     * <p>Seven against one against one: three counts in one row and only two distinct values, which
     * is stated rather than hidden -- the sound and the use are genuinely the same number here, and
     * the row that distinguishes them is {@code executeFan}'s.
     */
    @Test
    void aSpreadPressIsONESoundAndONEUse() {
        Fired fired = fire(bolt(SHIPPED));

        assertEquals(List.of("scatter_cast"), fired.world().presented,
                "you hear the press ONCE. Seven copies is the defect that would follow from"
                        + " expanding the spread above the commit");
        assertEquals(0, fired.uses(),
                "a LITERAL damage payload is not a basic attack, so no durability is charged --"
                        + " which is scatter_shot's own recorded consequence, not an accident");
    }

    /**
     * THE BODIES REALLY GO SEVEN DIFFERENT WAYS, AND THE FIRST ONE GOES DOWN THE AIM.
     *
     * <p>Asserted through the launch VELOCITIES, which is where the direction actually shows up --
     * {@code SpreadPattern} could compute seven perfect directions and the loop could drop them on
     * the floor, launching seven bodies down one line, and <b>the count row above would stay
     * green.</b> Same argument {@code CastExecutorFanTest} makes for reading velocities rather than
     * trusting the offsets.
     *
     * <p>Every velocity is checked to have the authored SPEED as well, because a direction that was
     * added to the forward vector without renormalising would arrive slightly long -- arrows that
     * reach the target at different times, which is invisible to any count.
     */
    @Test
    void theSevenBodiesLeaveAlongSevenDistinctDirections() {
        Fired fired = fire(bolt(SHIPPED));

        List<Vec3> launches = new ArrayList<>();
        for (var id : fired.world().markersEverSpawned) {
            launches.add(fired.world().markerVelocities.get(id).get(0));
        }
        assertEquals(7, launches.size());

        // The first body goes down the aim, at exactly the authored speed.
        assertEquals(2.5, launches.get(0).length(), 1e-9, "the centre body's speed");
        assertEquals(1.0, launches.get(0).normalize().dot(EYE_FORWARD.direction()), 1e-9,
                "the FIRST body flies down the aim vector");

        // The other six sit at the authored angle, and none of them coincides with another.
        for (int i = 1; i < launches.size(); i++) {
            assertEquals(2.5, launches.get(i).length(), 1e-9,
                    "body " + i + " left at the wrong speed -- an unnormalised direction");
            double degrees = Math.toDegrees(
                    Math.acos(launches.get(i).normalize().dot(EYE_FORWARD.direction())));
            assertEquals(5.0, degrees, 1e-9, "body " + i + " is off-angle");

            for (int j = 1; j < i; j++) {
                assertNotEquals(1.0, launches.get(i).normalize().dot(launches.get(j).normalize()),
                        1e-9, "bodies " + i + " and " + j + " left along the SAME line");
            }
        }
    }

    /**
     * *** ONE ROLL, SEVEN DELIVERIES -- AND THE PROPERTY IS INHERITED RATHER THAN BUILT HERE. ***
     *
     * <p>Every body carries the SAME frozen caster, so a crit is all seven or none. The roll is not
     * made in {@code launch} at all: it is drawn once into the snapshot
     * ({@code BukkitCombatant.snapshot} -- <i>"drawn HERE and frozen -- once per cast, never per
     * damage arm"</i>) and {@code commit} projects it once.
     *
     * <p><b>SAID PLAINLY BECAUSE A BUILD THAT RE-ROLLED PER BODY WOULD PASS EVERY ROW ABOVE.</b>
     * Without a crit the seven amounts are identical either way, so nothing that counts bodies or
     * measures directions can see it.
     *
     * <p>This row reads the shared identity rather than a damage figure: all seven impacts are
     * attributed to one caster id, and the payload each carries is the one built at the press. A
     * genuinely independent re-roll would require a fresh SNAPSHOT, which {@code launch} has no way
     * to obtain -- so the property is <b>structural</b>, and this row records that rather than
     * pretending to guard a mutation that cannot be written here.
     */
    @Test
    void everyBodyCarriesTheOneCasterBuiltAtThePress() {
        Fired fired = fire(bolt(SHIPPED));

        assertEquals(7, fired.world().markersEverSpawned.size());
        assertEquals(1, fired.world().entities.size(),
                "the press consulted ONE caster; a per-body re-projection would need a second read"
                        + " of the world, which launch() has no handle to make");
    }
}
