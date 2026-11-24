package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WILDFIRE THROUGH THE REAL DAMAGE ARM (the seat's brief of 2026-11-24, PR E). The Living Lantern's 45 fire, cast as a ray
 * here so each press lands on its own tick (the chain reads the hit, not the cast shape). Every expected number EXECUTED in
 * jshell, 2026-11-24: 45 x 1.15 = 51.74999999999999, 45 x 1.3 = 58.5, 45 x 1.45 = 65.25, 45 x 1.6 = 72.0.
 */
class WildfireTest {

    private static final EnchantBehaviour.HitChain WILDFIRE = new EnchantBehaviour.HitChain("living_lantern", 60, 60,
            List.of(5.0, 10.0, 15.0));
    private static final double X0 = 4, Z = 8, EYE = 1.62;

    private static AbilityDefinition lantern(String weapon) {
        return new AbilityDefinition(weapon + "/right_click", "Lantern's Breath", "fire", 16, new ResourceCost("mana", 12),
                new CastSpec.Ray(28, "beam"), List.of(new EffectSpec.Damage(45, "fire")), List.of());
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    private static FakeWorld.Dummy mob(double out, double z) {
        var m = new FakeWorld.Dummy(new Vec3(X0 + out, EYE, z));
        m.health = 10_000;
        return m;
    }

    private static Stage stage(int level, FakeWorld.Dummy... mobs) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(new Vec3(X0, 0, Z));
        him.player = true;
        if (level > 0) him.behaviours = List.of(new ActiveBehaviour("wildfire", level, WILDFIRE));
        world.entities.add(him);
        world.entities.addAll(List.of(mobs));
        return new Stage(world, him);
    }

    private static void press(Stage s, AbilityDefinition a) {
        Aim aim = s.world().aimOf(s.him().id()).orElseThrow();
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(a, s.him().snapshot(), aim, true));
    }

    private static List<Double> sent(Stage s, FakeWorld.Dummy target) {
        return s.world().traced.stream().filter(h -> h.targetId().equals(target.id())).map(TracedHit::sent).toList();
    }

    private static void assertSent(List<Double> want, List<Double> got) {
        assertEquals(want.size(), got.size(), "hits: " + got);
        for (int i = 0; i < want.size(); i++) assertEquals(want.get(i), got.get(i), 1e-9, "hit " + (i + 1) + ": " + got);
    }

    /** Wildfire III, six hits at the lantern's 16-tick cadence; then a gap of 61 ticks and the chain starts again. */
    @Test
    void sixHitsClimbToTheCapAndAGapStartsAgain() {
        var m = mob(10, Z);
        Stage s = stage(3, m);
        for (int i = 0; i < 6; i++) {
            press(s, lantern("living_lantern"));
            s.world().advanceTicks(16);
        }
        s.world().advanceTicks(61 - 16);
        press(s, lantern("living_lantern"));
        assertSent(List.of(45.0, 51.74999999999999, 58.5, 65.25, 72.0, 72.0, 45.0), sent(s, m));
    }

    /** No enchant: seven hits of 45, the control. */
    @Test
    void withoutItEveryHitIsTheLanterns() {
        var m = mob(10, Z);
        Stage s = stage(0, m);
        for (int i = 0; i < 4; i++) {
            press(s, lantern("living_lantern"));
            s.world().advanceTicks(16);
        }
        assertSent(List.of(45.0, 45.0, 45.0, 45.0), sent(s, m));
    }

    /**
     * SAME TARGET: a second mob struck between two hits on the first starts its own chain and does not break the first's.
     * He turns by being moved: the second mob stands on his aim line only while he faces it (a mob 10 out at z 8, another
     * 10 out at z 8 behind a re-aim is not needed: the fake's ray stops at the first body, so the second mob is placed and
     * the first REMOVED from the line, then restored).
     */
    @Test
    void aSecondMobHasItsOwnChain() {
        var a = mob(10, Z);
        var b = mob(10, Z + 40);   // off the line until moved onto it
        Stage s = stage(3, a, b);
        press(s, lantern("living_lantern"));
        s.world().advanceTicks(16);
        press(s, lantern("living_lantern"));
        s.world().advanceTicks(16);
        a.moveTo(new Vec3(X0 + 10, EYE, Z + 40));
        b.moveTo(new Vec3(X0 + 10, EYE, Z));
        press(s, lantern("living_lantern"));
        s.world().advanceTicks(16);
        a.moveTo(new Vec3(X0 + 10, EYE, Z));
        b.moveTo(new Vec3(X0 + 10, EYE, Z + 40));
        press(s, lantern("living_lantern"));
        assertSent(List.of(45.0, 51.74999999999999, 58.5), sent(s, a));
        assertSent(List.of(45.0), sent(s, b));
    }

    /** Another weapon's cast carrying the same state (the dev command can stage it) is never multiplied. */
    @Test
    void onlyTheLanternsOwnCast() {
        var m = mob(10, Z);
        Stage s = stage(3, m);
        for (int i = 0; i < 3; i++) {
            press(s, lantern("flint_staff"));
            s.world().advanceTicks(16);
        }
        assertSent(List.of(45.0, 45.0, 45.0), sent(s, m));
    }

    /** A damage-over-time tick and every secondary damage neither extend a chain nor are multiplied. */
    @Test
    void secondaryDamageNeverMeetsAChain() {
        var m = mob(10, Z);
        Stage s = stage(3, m);
        press(s, lantern("living_lantern"));
        s.world().advanceTicks(16);
        var secondary = io.github.butterflysmp.rpg.core.combat.Caster.of(s.him().snapshot())
                .withSource("living_lantern/right_click")
                .withBehaviours(List.of(new ActiveBehaviour("wildfire", 3, WILDFIRE)));
        new io.github.butterflysmp.rpg.core.ability.effect.EffectApplier(s.world())
                .applyAll(List.of(new EffectSpec.Damage(10, "fire")), secondary.asSecondary("x/secondary"), s.world().combatant(m.id()).orElseThrow(), m.position());
        new io.github.butterflysmp.rpg.core.ability.effect.EffectApplier(s.world())
                .applyAll(List.of(new EffectSpec.Damage(10, "fire")), secondary.asNotAHit(), s.world().combatant(m.id()).orElseThrow(), m.position());
        press(s, lantern("living_lantern"));
        assertSent(List.of(45.0, 10.0, 10.0, 51.74999999999999), sent(s, m));
    }
}
