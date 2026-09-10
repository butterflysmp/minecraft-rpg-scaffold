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
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A volley: a wind-up, then N shots on a clock, each one re-read from a caster who may have moved,
 * turned, or changed what they are holding.
 *
 * <b>THE CLOCK IS WHY THIS SUITE IS IN {@code core}.</b> {@code FakeWorld}'s scheduler is a clock,
 * so every timing row is written {@code advanceTicks(N - 1)} - assert nothing - {@code advanceTicks(1)}
 * - assert exactly, the shape that cannot pass on an off-by-one. An adapter-side equivalent would
 * have no clock to advance and could only be boot-witnessed.
 *
 * <b>THE FIXTURES PUT BODIES ON THE EYE LINE, UNLIKE EVERY OTHER RAY TEST IN THIS PROJECT, AND THAT
 * IS NOT COSMETIC.</b> {@code CastExecutorTest} builds its own {@code Aim} from {@code Vec3.ZERO},
 * so its rays travel at y=0 and its dummies stand at y=0. A volley cannot do that: it gets its
 * origin from {@code CombatWorld.aimOf}, which is the caster's EYE. A victim left at y=0 sits 1.62
 * blocks off a ray fired from y=1.62 and is missed by {@code FakeWorld}'s 0.6 hit radius -- a row
 * staged that way fails for a reason that has nothing to do with volleys. Bodies go at {@link #EYE}.
 *
 * <b>WHAT THIS SUITE CANNOT SEE, stated so a green run is not over-read.</b> Nothing here proves a
 * REAL caster's crit is re-ROLLED: the roll happens in {@code BukkitCombatant.snapshot} against
 * {@code ThreadLocalRandom}, and {@code FakeWorld.Dummy} carries a fixed multiplier. What these rows
 * prove is that each shot RE-READS the dummy, which is the half core owns -- if the value it reads
 * changes, the shot changes. That the adapter draws a new number per read is the adapter's, and
 * {@code BukkitCombatant.snapshot} already documents it.
 *
 * Each row names the mutation it forces red.
 */
class CastExecutorVolleyTest {

    private static final double EPS = 1e-9;

    /** {@code FakeWorld.Dummy}'s default eye height, and therefore where a volley's rays travel. */
    private static final double EYE = 1.62;

    private static AbilityDefinition volleyOfRays(int windup, int shots, int interval, double damage) {
        return volleyOf(new CastSpec.Ray(30), windup, shots, interval, damage, ResourceCost.FREE);
    }

    private static AbilityDefinition volleyOf(CastSpec inner, int windup, int shots, int interval,
                                              double damage, ResourceCost cost) {
        return new AbilityDefinition("test", "Test", "kinetic", "none", 0, cost,
                new CastSpec.Volley(windup, shots, interval, inner),
                List.of(new EffectSpec.Damage(damage, "kinetic")));
    }

    /** Run the ability through the REAL service and executor; only the world is fake. */
    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def) {
        cast(world, caster, def, new ResourcePool(() -> 0L, 100, 1));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def,
                             ResourcePool resources) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), resources);
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), "test", new Aim(Vec3.ZERO, new Vec3(1, 0, 0)),
                        Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    private static FakeWorld.Dummy victimAt(FakeWorld world, double x) {
        var d = new FakeWorld.Dummy(new Vec3(x, EYE, 0));
        world.entities.add(d);
        return d;
    }

    private static FakeWorld.Dummy casterIn(FakeWorld world) {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);
        return caster;
    }

    // --- The clock ---

    @Test
    void theFirstShotLandsOnTheWINDUPTickAndNotOneBefore() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(20, 3, 2, 10));

        assertEquals(100, victim.health, EPS, "nothing fires on the cast frame -- that is the wind-up");
        world.advanceTicks(19);
        assertEquals(100, victim.health, EPS, "the wind-up is 20 ticks -- not 19");
        world.advanceTicks(1);
        assertEquals(90, victim.health, EPS, "shot 1 lands on tick 20 exactly");
        assertEquals(1, victim.damageCalls, "once, not twice");
        // Mutation: fire shot 1 inline regardless of windupTicks -> the cast-frame assertion reddens.
        // Mutation: schedule the wind-up at windup + interval -> the tick-20 assertion reddens.
        // Both sides are asserted, so this is insensitive to the direction of the error.
    }

    @Test
    void aVolleyWithNOWindupFiresItsFirstShotINLINEOnTheCastFrame() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(0, 3, 2, 10));

        assertEquals(90, victim.health, EPS,
                "windup 0 means NOW, and it has to be inline: the scheduler cannot defer by less "
                        + "than a tick, so a deferred 0 would quietly become 1");
        // Mutation: drop the windupTicks == 0 branch and always schedule -> this reddens, because
        // FakeWorld refuses a delay below 1 and the cast throws instead of firing.
    }

    @Test
    void eachShotLandsOnItsOwnINTERVALTickAndNotOneBefore() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(10, 3, 4, 10));

        world.advanceTicks(10);
        assertEquals(1, victim.damageCalls, "shot 1 at t=10");
        world.advanceTicks(3);
        assertEquals(1, victim.damageCalls, "the interval is 4 ticks -- shot 2 is not due at t=13");
        world.advanceTicks(1);
        assertEquals(2, victim.damageCalls, "shot 2 at t=14");
        world.advanceTicks(3);
        assertEquals(2, victim.damageCalls, "nor shot 3 at t=17");
        world.advanceTicks(1);
        assertEquals(3, victim.damageCalls, "shot 3 at t=18");
        assertEquals(70, victim.health, EPS, "three shots of 10");
        // WINDUP 10, INTERVAL 4, SHOTS 3: no two of the three are equal, and neither interval
        // boundary coincides with the wind-up. A fixture using the same number twice cannot tell a
        // volley that re-uses the wind-up as its interval from one that does not.
        // Mutation: schedule the next shot with windupTicks instead of intervalTicks -> shot 2
        // arrives at t=20 and the t=14 assertion reddens.
    }

    @Test
    void exactlyNShotsFireAndTheVolleyClockDoesNotREARM() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(10, 3, 4, 10));
        world.advanceTicks(500);

        assertEquals(3, victim.damageCalls, "three shots were authored and three were fired");
        int afterFirstSettle = victim.damageCalls;
        world.advanceTicks(500);
        assertEquals(afterFirstSettle, victim.damageCalls, "and nothing fires afterwards, ever");

        // DELIBERATELY NOT assertEquals(0, world.pendingTasks()). That would be true of this fake
        // and FALSE OF PRODUCTION: FakeWorld resolves a ray synchronously inside one chunk column,
        // but a real 64-block ray's chunk-column walk OUTLIVES the volley clock -- the last shot's
        // beam is still travelling after the volley is over. The claim this row actually holds is
        // "the volley did not re-arm", so it is measured by asking whether anything still FIRES,
        // which stays true whichever way the fake's ray behaves. A queue-empty assertion would
        // redden one day for a reason unrelated to the guard, and someone would loosen the
        // assertion that matters instead.
        //
        // Mutation: re-arm unconditionally (drop the shotIndex + 1 >= shots return) -> damageCalls
        // climbs without bound AND FakeWorld's MAX_TASKS_PER_ADVANCE trip-wire fires.
    }

    // --- The re-read, which is the whole ruling ---

    @Test
    void eachShotREREADSTheCastersStatsRatherThanTheCastFrameProjection() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(0, 3, 4, 10));
        assertEquals(90, victim.health, EPS, "shot 1: the authored 10, no gear bonus");

        caster.classDamageBonus = 5;                 // the player swaps to a stronger weapon
        world.advanceTicks(4);
        assertEquals(75, victim.health, EPS, "shot 2 is priced off the NEW projection: 15");
        world.advanceTicks(4);
        assertEquals(60, victim.health, EPS, "and so is shot 3");

        // THIS IS THE EXACT INVERSE OF ProjectileFlightTest's frozen-stat rows, and the pair is
        // load-bearing. There, changing a stat MID-FLIGHT must NOT change the impact, because a
        // projectile resolves on the target's region where the caster's store cannot legally be
        // read. Here, changing a stat MID-VOLLEY MUST change the next shot, because a volley is
        // scheduled ON the caster and each shot runs on the caster's own thread.
        //
        // A future reader who "unifies" the two by freezing the volley would pass ProjectileFlight's
        // rows and redden these. That is the point: the two suites disagree on purpose, and each
        // says so.
        //
        // Mutation: pass execute()'s cast-frame Caster into volley() instead of re-projecting ->
        // all three shots deal 10 and both later assertions redden.
    }

    @Test
    void eachShotREREADSTheCritRollToo() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(0, 2, 4, 10));
        assertFalse(victim.lastDamageWasCrit, "shot 1 did not crit");
        assertEquals(90, victim.health, EPS);

        caster.critMultiplier = 2.0;                 // what Crit.multiplier would have returned
        world.advanceTicks(4);
        assertTrue(victim.lastDamageWasCrit, "shot 2 crit, independently of shot 1");
        assertEquals(70, victim.health, EPS, "and dealt double");

        assertEquals(Crit.NO_CRIT, 1.0, EPS,
                "THE CONTROL: NO_CRIT is exactly 1.0, so shot 1's 10 is the un-multiplied authored "
                        + "amount and not a crit that happened to round to it");
        // The operator's Q1 ruling was two questions and this is the second: six shots off ONE
        // projection would share ONE crit roll, so a volley would be all-yellow or all-white. What
        // core can prove is that the value is RE-READ; that the adapter draws a fresh number per
        // read is BukkitCombatant.snapshot's, and its javadoc records it.
        //
        // Mutation: hoist the Caster projection above the shot loop -> shot 2 inherits shot 1's
        // NO_CRIT and both crit assertions redden.
    }

    @Test
    void eachShotREAIMSFromTheCastersLIVELook() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var ahead = victimAt(world, 5);                               // on the +X line
        var aside = new FakeWorld.Dummy(new Vec3(0, EYE, 5));         // on the +Z line
        world.entities.add(aside);

        cast(world, caster, volleyOfRays(0, 2, 4, 10));
        assertEquals(90, ahead.health, EPS, "shot 1 went where the caster was looking");
        assertEquals(100, aside.health, EPS, "and not where they were not");

        caster.facing = new Vec3(0, 0, 1);           // the player turns ninety degrees
        world.advanceTicks(4);
        assertEquals(90, ahead.health, EPS, "shot 2 did NOT follow the old line");
        assertEquals(90, aside.health, EPS, "it followed the new one");

        // TWO VICTIMS ON PERPENDICULAR LINES, so a volley that ignores the turn and one that honours
        // it produce DIFFERENT victims rather than the same victim twice. With a single body the row
        // cannot discriminate: both behaviours hit it.
        //
        // Mutation: read the aim once before the loop, or fall back to the cast-frame aim ->
        // `aside` stays at 100 and `ahead` drops to 80. Both assertions redden, in opposite
        // directions, which is what makes the pair readable.
    }

    @Test
    void aVolleyAlsoREAIMSItsORIGINAndNotOnlyItsDirection() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        victimAt(world, 5);

        cast(world, caster, volleyOfRays(0, 2, 4, 10));
        assertEquals(1, world.castRayFrom.size(), "one trace so far");
        assertEquals(0, world.castRayFrom.get(0).x(), EPS, "fired from where the caster stood");

        caster.moveTo(new Vec3(3, 0, 0));            // the player strafes
        world.advanceTicks(4);
        assertEquals(3, world.castRayFrom.get(1).x(), EPS,
                "shot 2 leaves the muzzle where the caster is NOW, not where they were");
        assertEquals(EYE, world.castRayFrom.get(1).y(), EPS,
                "and still from the eye rather than the feet");
        // The direction row above cannot see this: a caster who turns but does not move produces the
        // same origin either way. Mutation: build the Aim from a cached origin -> the x assertion
        // reddens. Drop the eyeHeight term in FakeWorld.aimOf -> the y assertion reddens.
    }

    // --- Stopping ---

    @Test
    void aVolleyWhoseCasterIsGONEStopsRatherThanFiringOn() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(0, 4, 4, 10));
        assertEquals(1, victim.damageCalls, "shot 1 landed while the caster was here");

        world.entities.remove(caster);               // logout, death removal, world change
        world.advanceTicks(200);

        assertEquals(1, victim.damageCalls,
                "a volley is scheduled ON its caster; with no caster there is nothing to re-read, "
                        + "no aim to fire down, and no shot");
        // Mutation: default the missing caster to the cast-frame projection instead of returning ->
        // all four shots land and this reddens. That fallback is exactly what the aim read must not
        // do either, and for the same reason.
    }

    @Test
    void aVolleyWhoseCasterIsDEADStopsToo() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        cast(world, caster, volleyOfRays(0, 4, 4, 10));
        assertEquals(1, victim.damageCalls);

        caster.health = 0;                           // present, readable, and not alive
        world.advanceTicks(200);

        assertEquals(1, victim.damageCalls, "a corpse does not finish its burst");
        // SEPARATE FROM THE ROW ABOVE, because the two states are different: gone means combatant()
        // is empty, dead means it returns a snapshot whose alive() is false. A guard written as a
        // null check alone passes the previous row and fails this one.
    }

    // --- The payload, and the cost ---

    @Test
    void aVolleyOfRaysDrawsItsBeamONCEPerShot() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        victimAt(world, 5);

        var def = new AbilityDefinition("test", "Test", "kinetic", "none", 0, ResourceCost.FREE,
                new CastSpec.Volley(0, 3, 4, new CastSpec.Ray(10, "test_beam")),
                List.of(new EffectSpec.Damage(10, "kinetic")));
        cast(world, caster, def);
        world.advanceTicks(20);

        assertEquals(3, world.presentedAlong.size(), "one beam per shot, three shots");
        assertTrue(world.presentedAlong.stream().allMatch(b -> b.visualId().equals("test_beam")));
        assertEquals(EYE, world.presentedAlong.get(0).from().y(), EPS,
                "drawn from the eye's HEIGHT -- see the row below for the origin itself, which this "
                        + "assertion cannot see");
        // The inner cast is dispatched through the SAME launchRay every standalone ray uses, so the
        // beam is not re-implemented here. This row is what says so. Mutation: draw the beam once
        // per volley instead of per shot -> the count reddens.
    }

    /**
     * <b>EACH SHOT'S BEAM STARTS ONE GAP ALONG THAT SHOT'S OWN RE-PROJECTED AIM.</b>
     *
     * <p><b>This row exists because the beam-origin gap turned the row above into a control that
     * succeeds for the wrong reason, and nothing else would have noticed.</b> That row asserts only
     * {@code from().y()}, and the gap moves only {@code x} on a horizontal aim -- so after the gap
     * landed it passed identically at gap 0, at gap 1.0, and under a per-segment gap. It was the
     * volley's ONLY beam-origin witness and it had stopped witnessing the origin.
     *
     * <p>The message moved with it: <i>"drawn from the eye, which is where the shot was fired
     * from"</i> is false the moment a gap exists.
     *
     * <p><b>AND THE FACING CHANGES MID-BURST, WHICH IS THE ONLY PLACE THE GAP MEETS THE
     * RE-PROJECTION.</b> A volley re-projects its caster before EVERY shot, so {@code beamStart} is
     * recomputed per shot from the NEW aim. Nothing tested that. A gap computed once for the whole
     * volley -- the obvious optimisation, since it looks like a constant -- passes every other row
     * in this file and fails only here.
     *
     * <p>Mutation: hoist the {@code aim.pointAt(BEAM_ORIGIN_GAP)} out of the per-shot projection so
     * it is computed once per volley -> <b>SHOT TWO</b> -- {@code presentedAlong.get(1)}, the first
     * shot after the turn, and the one this row asserts on -- still carries shot one's {@code +x}
     * gap, and the assertions below redden. (Named exactly, because a re-runner who sees shot two go
     * red should not have to work out whether that is the predicted failure or a different one.)
     * Measured: 855 run, <b>1</b> failed.
     */
    @Test
    void eachShotsBeamStartsOneGapAlongTHATShotsOwnAim() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var origin = caster.position().add(new Vec3(0, EYE, 0));

        var def = new AbilityDefinition("test", "Test", "kinetic", "none", 0, ResourceCost.FREE,
                new CastSpec.Volley(0, 3, 4, new CastSpec.Ray(10, "test_beam")),
                List.of(new EffectSpec.Damage(10, "kinetic")));
        cast(world, caster, def);

        // Shot one, on the default +x facing.
        assertEquals(1, world.presentedAlong.size(), "the first shot fires on the cast frame");
        assertEquals(1.0, origin.subtract(world.presentedAlong.get(0).from()).length(), EPS,
                "one gap from the eye, measured as a DISTANCE so it is not satisfied by an axis "
                        + "that the gap happens not to move");
        assertEquals(origin.x() + 1.0, world.presentedAlong.get(0).from().x(), EPS,
                "and it is one gap along +x, the direction this shot was aimed");

        // TURN THE CASTER AROUND MID-BURST. The volley re-reads its caster before every shot, so
        // shots two and three are aimed down -x and their beams must start one gap down -x.
        caster.facing = new Vec3(-1, 0, 0);
        world.advanceTicks(4);

        assertEquals(2, world.presentedAlong.size(), "the second shot lands four ticks later");
        var afterTurn = world.presentedAlong.get(1);
        assertEquals(1.0, origin.subtract(afterTurn.from()).length(), EPS,
                "still exactly one gap from the eye -- the distance is invariant to the turn");
        assertEquals(origin.x() - 1.0, afterTurn.from().x(), EPS,
                "THE PROPERTY: one gap along the NEW aim. A gap computed once for the whole volley "
                        + "would still be sitting at origin.x() + 1.0, on the aim the caster no "
                        + "longer has");
    }

    @Test
    void aVolleyOfPROJECTILESIsRepeatableToo() {
        var world = new FakeWorld();
        var caster = casterIn(world);

        var def = volleyOf(new CastSpec.Projectile(1.0, 0.0, 40, null, "flint"),
                0, 3, 4, 10, ResourceCost.FREE);
        cast(world, caster, def);
        world.advanceTicks(20);

        assertEquals(3, world.markersEverSpawned.size(),
                "three shots, three bodies -- the whitelist's second member, EXERCISED rather than "
                        + "asserted");
        // The content plan says a burst-fire projectile weapon is the second thing this grammar is
        // for. A wrapper proved only against rays would be widened blind the first time anyone
        // wanted one. Mutation: narrow fireInner to rays only -> this throws and reddens.
    }

    @Test
    void theCasterPaysTheCostONCEForAWholeVolley() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        victimAt(world, 5);
        var pool = new ResourcePool(() -> 0L, 100, 0);   // no regen, so the reading is unambiguous

        cast(world, caster, volleyOf(new CastSpec.Ray(10), 0, 4, 4, 10, new ResourceCost("mana", 40)),
                pool);
        world.advanceTicks(200);

        assertEquals(60, pool.current(caster.id(), "mana"), EPS,
                "one press, one charge -- the cost is spent at commit, not per shot");
        // FOUR SHOTS AT 40 IS 160 AGAINST A POOL OF 100, so a per-shot charge could not even
        // complete: the fixture is sized so the wrong behaviour is impossible rather than merely
        // wrong-looking. Mutation: charge inside volley() -> the pool empties and this reddens.
    }
}
