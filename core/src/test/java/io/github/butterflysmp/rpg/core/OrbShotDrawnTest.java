package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Orbs;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * BEN'S CRYSTAL, 2026-11-23: "Draw a line with particles from the crystal to the target with the same particles the right
 * click uses" and "Increase the damage of the crystal to 30/50/70%". The line takes its visual from the RAY that met the
 * orb, at the moment it fires (the seat's ruling: nothing copied into the enchant file); it runs from the orb's rest point
 * to each STRUCK mob's sight point, once per struck mob, and to no other. OrbBeamTest's staging: him at the origin facing
 * east, his orb resting 6 east; N 5 from the orb (struck), F 9 from it (outside 8), P a player 3 from it, L behind a wall.
 */
class OrbShotDrawnTest {

    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));
    private static final Vec3 REST = new Vec3(6, 0, 0);
    private static final Orbs.Shot SHOT = new Orbs.Shot(8, List.of(30.0, 50.0, 70.0), "kinetic", null);

    private static AbilityDefinition beam(String visual) {
        return new AbilityDefinition("lapis_staff/right_click", "Lapis Beam", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Ray(26, visual, false, 0), List.of(new EffectSpec.Damage(21, "kinetic")));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him, FakeWorld.Dummy n) {}

    private static Stage stage(int level) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        var n = new FakeWorld.Dummy(new Vec3(6, 0, 5));
        var f = new FakeWorld.Dummy(new Vec3(6, 0, 9));
        var p = new FakeWorld.Dummy(new Vec3(6, 0, 3));
        p.player = true;
        var l = new FakeWorld.Dummy(new Vec3(6, 0, -4));
        world.entities.addAll(List.of(him, n, f, p, l));
        world.sightBlocked = (from, to) -> to.z() < -1;   // a wall between the orb and L
        var placed = world.orbs.place(him.id(), UUID.randomUUID(), REST, 0.6, 300, "lapis_staff", "lapis_crystal", level,
                SHOT);
        world.orbs.arrive(him.id(), placed.placed().generation(), 0);
        return new Stage(world, him, n);
    }

    private static void fire(Stage s, AbilityDefinition ability) {
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(ability, s.him().snapshot(), EAST, true));
        s.world().advanceTicks(5);
    }

    private static List<FakeWorld.Beam> fromOrb(Stage s) {
        return s.world().presentedAlong.stream().filter(b -> b.from().equals(REST)).toList();
    }

    @Test
    void oneLineFromTheOrbToTheStruckMobWithTheBeamsOwnVisualAndNoneToAnyOther() {
        Stage s = stage(2);
        fire(s, beam("lapis_beam"));
        Vec3 sight = s.n().snapshot().sightPoint();
        assertEquals(List.of(new FakeWorld.Beam(REST, sight, "lapis_beam")), fromOrb(s),
                "exactly one line, the orb to N's sight point, in the ray's visual; none to F, P or L");
        assertEquals(List.of(new FakeWorld.OrbShot(s.him().id(), REST, sight, "lapis_beam")), s.world().orbShots,
                "and its trace line");
    }

    @Test
    void aRayThatNamesNoBeamDrawsNothingAndTracesNothing() {
        Stage s = stage(2);
        fire(s, beam(null));
        assertEquals(List.of(), fromOrb(s), "no visual to draw with: no line");
        assertEquals(List.of(), s.world().orbShots);
        assertEquals(1, s.world().traced.stream().filter(h -> "lapis_crystal/crystal".equals(h.source())).count(),
                "the shot itself still lands");
    }

    /** 21 x 30 / 100, 21 x 50 / 100 and 21 x 70 / 100 are exactly 6.3, 10.5 and 14.7 in doubles (EXECUTED, jshell). */
    @Test
    void theThreeLevelsStrikeSixPointThreeTenPointFiveAndFourteenPointSeven() {
        double[] want = {6.3, 10.5, 14.7};
        for (int level = 1; level <= 3; level++) {
            Stage s = stage(level);
            fire(s, beam("lapis_beam"));
            List<Double> sent = s.world().traced.stream().filter(h -> "lapis_crystal/crystal".equals(h.source()))
                    .map(TracedHit::sent).toList();
            assertEquals(List.of(want[level - 1]), sent, "level " + level);
        }
    }
}
