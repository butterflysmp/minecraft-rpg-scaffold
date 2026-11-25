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
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FOCUSED BEAM THROUGH THE REAL CAST PATH (the seat's brief of 2026-11-24, PR 2). Ben: "Fires one stronger beam after the
 * wind up." / "1. 108/162/216 2. No piece 3. Everything the same except increase the range to 48 blocks. 4. Yes". The
 * Cursed Emerald's right click copied from {@code cursed_emerald.yml}: cooldown 30, mana 40, on_cast emerald_windup, a
 * volley of wind-up 20, 3 shots 2 apart (6 until Ben's 3, 2026-11-24), of a ray of range 32 drawing emerald_beam; on_hit emerald_impact and 27 kinetic.
 *
 * <p>THE FAKE'S RAY (READ, {@code FakeWorld.castRay}): a body is met when its POSITION lies within {@code hitRadius} of
 * the segment, in a chunk column the segment walks. So every mob here stands at his EYE height (1.62) on z = 8, inside
 * one column's width, and the range boundaries are placed mid-column (his feet at x = 4, so 48 ends at x = 52 and 32 at
 * x = 36, inside columns 3 and 2), so a miss there is the RANGE and not the column model.
 */
class FocusedBeamTest {

    private static final EnchantBehaviour.FocusedBeam BEAM = new EnchantBehaviour.FocusedBeam("cursed_emerald", 48,
            List.of(108.0, 162.0, 216.0), "kinetic");
    private static final double X0 = 4, Z = 8, EYE = 1.62;

    private static AbilityDefinition volley(String weapon) {
        return new AbilityDefinition(weapon + "/right_click", "Cursed Volley", "kinetic", 30, new ResourceCost("mana", 40),
                new CastSpec.Volley(20, 3, 2, new CastSpec.Ray(32, "emerald_beam")),
                List.of(new EffectSpec.Visual("emerald_impact"), new EffectSpec.Damage(27, "kinetic")),
                List.of(), List.of(new EffectSpec.Visual("emerald_windup")));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    /** A mob {@code out} blocks east of his feet, at his eye height, on his aim line; 1000 health, so nothing dies. */
    private static FakeWorld.Dummy mob(double out) {
        var m = new FakeWorld.Dummy(new Vec3(X0 + out, EYE, Z));
        m.health = 1000;
        return m;
    }

    private static Stage stage(int level, FakeWorld.Dummy... mobs) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(new Vec3(X0, 0, Z));
        him.player = true;
        if (level > 0) him.behaviours = List.of(new ActiveBehaviour("focused_beam", level, BEAM));
        world.entities.add(him);
        world.entities.addAll(List.of(mobs));
        return new Stage(world, him);
    }

    private static Aim eyeAim(Stage s) {
        return s.world().aimOf(s.him().id()).orElseThrow();
    }

    private static void press(Stage s, AbilityDefinition ability) {
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(ability, s.him().snapshot(), eyeAim(s), true));
    }

    private static List<Double> sent(Stage s, FakeWorld.Dummy target) {
        return s.world().traced.stream().filter(h -> h.targetId().equals(target.id())).map(TracedHit::sent).toList();
    }

    @Test
    void whileActiveOnePressFiresOneRayNotSix() {
        var m = mob(10);
        Stage s = stage(1, m);
        press(s, volley("cursed_emerald"));
        s.world().advanceTicks(40);
        assertEquals(List.of(108.0), sent(s, m), "ONE hit of level I's 108, in place of three of 27");
        assertEquals(1, s.world().focusedBeams.size(), "one Focused Beam traced");
        assertEquals(0, s.world().pendingTasks(), "no further shot left scheduled");
    }

    @Test
    void notActiveOrOnAnotherWeaponsVolleySixAsToday() {
        var m = mob(10);
        Stage off = stage(0, m);
        press(off, volley("cursed_emerald"));
        off.world().advanceTicks(40);
        assertEquals(List.of(27.0, 27.0, 27.0), sent(off, m), "not active: the three bolts of 27 (Ben's 3, 2026-11-24)");
        assertTrue(off.world().focusedBeams.isEmpty());

        var o = mob(10);
        Stage other = stage(1, o);
        press(other, volley("knell_beam"));
        other.world().advanceTicks(40);
        assertEquals(List.of(27.0, 27.0, 27.0), sent(other, o),
                "a volley another weapon pressed carries the Cursed Emerald's state (a swap mid-wind-up can) and fires as authored");
        assertTrue(other.world().focusedBeams.isEmpty());
    }

    /**
     * The ray leaves on the wind-up's last tick (20 after the press), not before. Pressed at tick 7, not 0, so the
     * traced press tick cannot pass by being a default zero.
     */
    @Test
    void theRayLeavesAtTheWindupsEndAndNotBefore() {
        var m = mob(10);
        Stage s = stage(1, m);
        s.world().advanceTicks(7);
        press(s, volley("cursed_emerald"));
        s.world().advanceTicks(19);
        assertEquals(List.of(), sent(s, m), "nothing by tick 26");
        assertTrue(s.world().focusedBeams.isEmpty(), "and no beam traced");
        s.world().advanceTicks(1);
        assertEquals(1, s.world().focusedBeams.size());
        assertEquals(27, s.world().focusedBeams.getFirst().tick(), "fired at tick 27");
        assertEquals(7, s.world().focusedBeams.getFirst().pressedTick(), "pressed at tick 7");
        assertEquals(List.of(108.0), sent(s, m), "the mob 10 out struck on that tick (one column, inline)");
    }

    /**
     * THE PRESS TICK IS THE PRESS'S, CARRIED (PLAN-enchant-columns.md section 19; the seat's ruling (c), 2026-11-24). On
     * the server the cast runs one tick after the press, past {@code WeaponFire}'s region hop, so the trace must carry the
     * tick stamped BEFORE the hop. Here the press is stamped at tick 5 and the cast run at tick 7: the trace reads 5, not
     * the cast's 7, and the beam still leaves 20 after the CAST in the fake (no phases), at 27. No two of 5, 7, 27 equal.
     */
    @Test
    void theTracedPressTickIsTheOneStampedAtThePressNotTheCasts() {
        var m = mob(10);
        Stage s = stage(1, m);
        s.world().advanceTicks(7);
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(volley("cursed_emerald"),
                s.him().snapshot(), eyeAim(s), true), 5);
        s.world().advanceTicks(20);
        assertEquals(1, s.world().focusedBeams.size());
        assertEquals(5, s.world().focusedBeams.getFirst().pressedTick(), "the press's own tick, stamped before the hop");
        assertEquals(27, s.world().focusedBeams.getFirst().tick(), "20 after the cast, in the fake");
    }

    /** Ben's 48, both ways at the boundary; and the plain volley's 32 stands (the gate's control). */
    @Test
    void itsRangeIsFortyEightAtTheBoundaryBothWays() {
        var in = mob(47.9);
        Stage reach = stage(1, in);
        press(reach, volley("cursed_emerald"));
        reach.world().advanceTicks(40);
        assertEquals(List.of(108.0), sent(reach, in), "47.9 out: inside 48");

        var out = mob(48.1);
        Stage beyond = stage(1, out);
        press(beyond, volley("cursed_emerald"));
        beyond.world().advanceTicks(40);
        assertEquals(List.of(), sent(beyond, out), "48.1 out: beyond 48");
        assertEquals(new Vec3(X0 + 48, EYE, Z), beyond.world().presentedAlong.getLast().to(),
                "the beam drawn to the 48-block end on a miss");

        var forty = mob(40);
        Stage plain = stage(0, forty);
        press(plain, volley("cursed_emerald"));
        plain.world().advanceTicks(40);
        assertEquals(List.of(), sent(plain, forty), "the plain volley's 32 stands: 40 out takes nothing from three bolts");
    }

    /** Ben: "No piece" (no pierce): the near mob takes the beam, the one behind it nothing. */
    @Test
    void itStopsAtTheFirstOfTwoMobsInLine() {
        var near = mob(10);
        var far = mob(14);
        Stage s = stage(1, near, far);
        press(s, volley("cursed_emerald"));
        s.world().advanceTicks(40);
        assertEquals(List.of(108.0), sent(s, near));
        assertEquals(List.of(), sent(s, far));
    }

    @Test
    void eachLevelsDamage() {
        double[] want = {108, 162, 216};
        for (int level = 1; level <= 3; level++) {
            var m = mob(10);
            Stage s = stage(level, m);
            press(s, volley("cursed_emerald"));
            s.world().advanceTicks(40);
            assertEquals(List.of(want[level - 1]), sent(s, m), "level " + level);
        }
    }

    /** Through AbilityService: 40 mana and the 30-tick cooldown, once a press, as the plain volley's. */
    @Test
    void manaAndCooldownAreChargedOnce() {
        var m = mob(10);
        Stage s = stage(1, m);
        var registry = new AbilityRegistry();
        AbilityDefinition ability = volley("cursed_emerald");
        registry.register(ability);
        var mana = new ResourcePool(s.world()::now, 100, 0.0);
        var service = new AbilityService(registry, new CooldownTracker(s.world()::now), mana);
        var first = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.fireTrigger(s.him().snapshot(), ability, eyeAim(s)));
        new CastExecutor(s.world()).execute(first);
        assertEquals(60.0, mana.current(s.him().id(), "mana"), 1e-9, "40 at the press");
        s.world().advanceTicks(25);
        assertEquals(List.of(108.0), sent(s, m), "the control: the beam fired");
        assertEquals(60.0, mana.current(s.him().id(), "mana"), 1e-9, "nothing more when it fires");
        assertInstanceOf(AbilityService.CastResult.OnCooldown.class, service.fireTrigger(s.him().snapshot(), ability, eyeAim(s)),
                "tick 25: still cooling");
        s.world().advanceTicks(5);
        assertInstanceOf(AbilityService.CastResult.Success.class, service.fireTrigger(s.him().snapshot(), ability, eyeAim(s)),
                "tick 30: the cooldown of 30 is over");
    }

    /** Ben: "Yes", it fires where he looks when the wind-up ends: the aim is read then, not at the press. */
    @Test
    void theAimIsReadAtTheWindupsEndNotAtThePress() {
        var east = mob(10);
        var south = new FakeWorld.Dummy(new Vec3(X0 + 0.5, EYE, Z + 10));   // x + 0.5: off the column border at x = 4.0
        south.health = 1000;
        Stage s = stage(1, east, south);
        press(s, volley("cursed_emerald"));
        s.world().advanceTicks(10);
        Vec3 toSouth = new Vec3(0.5, 0, 10).normalize();
        s.him().facing = toSouth;   // he turns during the wind-up
        s.world().advanceTicks(30);
        assertEquals(List.of(), sent(s, east), "the press's line (east) takes nothing");
        assertEquals(List.of(108.0), sent(s, south), "the line at the wind-up's end takes the beam");
    }

    /** One primary hit: with Kinetic Tremors I also active (3 hits needed), no shockwave. */
    @Test
    void withKineticTremorsOneHitFiresNoShockwave() {
        var tremors = new ActiveBehaviour("kinetic_tremors", 1, new EnchantBehaviour.Shockwave(3, 60, 5,
                List.of(10.0), List.of(3.0), "kinetic", null));
        var m = mob(10);
        Stage s = stage(1, m);
        s.him().behaviours = List.of(new ActiveBehaviour("focused_beam", 1, BEAM), tremors);
        press(s, volley("cursed_emerald"));
        s.world().advanceTicks(80);
        assertEquals(List.of(108.0), sent(s, m), "the control: the one hit landed");
        assertEquals(1, s.world().primaryHits.size(), "one primary hit");
        assertEquals(0, s.world().traced.stream().filter(h -> "kinetic_tremors/shockwave".equals(h.source())).count(),
                "no shockwave");

        var c = mob(10);
        Stage plain = stage(0, c);
        plain.him().behaviours = List.of(tremors);
        press(plain, volley("cursed_emerald"));
        plain.world().advanceTicks(80);
        assertTrue(plain.world().traced.stream().anyMatch(h -> "kinetic_tremors/shockwave".equals(h.source())),
                "the control: the plain three bolts DO fire one (so the fixture can see a shockwave)");
    }

    @Test
    void aMalformedFocusedBeamIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.FocusedBeam("cursed_emerald", 0,
                List.of(108.0), "kinetic"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.FocusedBeam("cursed_emerald", -1,
                List.of(108.0), "kinetic"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.FocusedBeam(" ", 48,
                List.of(108.0), "kinetic"));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.FocusedBeam("cursed_emerald", 48,
                List.of(), "kinetic"));
        assertEquals(162.0, BEAM.damageAt(2));
        assertEquals(3, BEAM.levels());
    }
}
