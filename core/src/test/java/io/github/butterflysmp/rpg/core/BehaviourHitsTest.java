package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.BehaviourHits;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A SHOCKWAVE BEHAVIOUR THROUGH THE REAL CAST PATH (the seat's B2-B4 and the Kinetic Tremors rule, 2026-11-23): casts by
 * the real {@link CastExecutor}, hits reported by the real {@code EffectApplier}, the decision and the shockwave by the
 * fake world's one {@link BehaviourHits}. Ben's numbers (3 hits, 60 ticks, 5-tick cooldown); the levels' 10/15/20 and
 * 3/4/5 are the seat's placeholders.
 */
class BehaviourHitsTest {

    private static final EnchantBehaviour.Shockwave WAVE = new EnchantBehaviour.Shockwave(3, 60, 5,
            List.of(10.0, 15.0, 20.0), List.of(3.0, 4.0, 5.0), "kinetic", "tremor_wave");
    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));

    /** A melee strike of 8 kinetic at reach 4: one hit per cast on whatever stands at x = 2. */
    private static AbilityDefinition strike() {
        return new AbilityDefinition("probe_strike", "Strike", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Melee(4.0, 0), List.of(new EffectSpec.Damage(8, "kinetic")));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy him, AbilityDefinition ability, boolean weaponTrigger) {
        new CastExecutor(world).execute(new AbilityService.CastResult.Success(ability, him.snapshot(), EAST, weaponTrigger));
    }

    private static List<TracedHit> from(FakeWorld world, String source) {
        return world.traced.stream().filter(h -> source.equals(h.source())).toList();
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him, FakeWorld.Dummy target, FakeWorld.Dummy near,
                         FakeWorld.Dummy far, FakeWorld.Dummy player) {}

    /** Him at the origin; the target at 2; a mob 3.5 from the target (inside level 2's 4); one 4.5 from it; a player at 1 from it. */
    private static Stage stage(int level) {
        var world = new FakeWorld();
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.behaviours = List.of(new ActiveBehaviour("tremors", level, WAVE));
        var target = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        var near = new FakeWorld.Dummy(new Vec3(2, 0, 3.5));
        var far = new FakeWorld.Dummy(new Vec3(2, 0, 4.5));
        var player = new FakeWorld.Dummy(new Vec3(2, 0, -1));
        player.player = true;
        world.entities.addAll(List.of(him, target, near, far, player));
        return new Stage(world, him, target, near, far, player);
    }

    @Test
    void threeWeaponHitsOnOneTargetFireExactlyOneShockwaveAtLevelTwo() {
        Stage s = stage(2);
        for (int i = 0; i < 3; i++) {
            cast(s.world(), s.him(), strike(), true);
            s.world().advanceTicks(1);
        }
        List<TracedHit> waves = from(s.world(), "tremors/shockwave");
        assertEquals(2, waves.size(), "the struck mob and the near one: " + waves);
        assertTrue(waves.stream().allMatch(h -> h.sent() == 15.0 && "kinetic".equals(h.element())),
                "level 2's 15, at score 100 with no other term: " + waves);
        assertEquals(100 - 8 * 3 - 15, s.target().health, 1e-9, "the struck mob included");
        assertEquals(85.0, s.near().health, 1e-9, "3.5 from it, inside 4");
        assertEquals(100.0, s.far().health, 1e-9, "4.5 from it, outside 4");
        assertEquals(100.0, s.player().health, 1e-9, "never a player");
        assertEquals(100.0, s.him().health, 1e-9, "never himself");
        assertEquals(List.of("tremor_wave"), s.world().presented, "its visual once, at the struck mob");
        assertEquals(s.target().position(), s.world().presentedAt.get(0));
    }

    /** 2a, the one table: the shockwave never strikes a SUMMON (PLAYER -> SUMMON no); the near mob is the control. */
    @Test
    void theShockwaveSparesASummonStandingInIt() {
        Stage s = stage(2);
        s.player().player = false;
        s.player().summon = true;   // the stage's player slot, 1 from the struck mob, as a fake summon
        for (int i = 0; i < 3; i++) {
            cast(s.world(), s.him(), strike(), true);
            s.world().advanceTicks(1);
        }
        assertEquals(85.0, s.near().health, 1e-9, "the control: the wave fired and struck the near mob");
        assertEquals(100.0, s.player().health, 1e-9, "never a summon");
    }

    /** Level 1's 3 blocks and 10 against level 3's 5 blocks and 20: the far mob (4.5) is inside only level 3's. */
    @Test
    void levelOneAgainstLevelThreeRadiusAndDamage() {
        Stage one = stage(1);
        Stage three = stage(3);
        for (Stage s : List.of(one, three)) {
            for (int i = 0; i < 3; i++) { cast(s.world(), s.him(), strike(), true); s.world().advanceTicks(1); }
        }
        assertEquals(List.of(10.0), from(one.world(), "tremors/shockwave").stream().map(TracedHit::sent).toList(),
                "level 1: 3 blocks reaches the struck mob alone");
        assertEquals(List.of(20.0, 20.0, 20.0),
                from(three.world(), "tremors/shockwave").stream().map(TracedHit::sent).toList(),
                "level 3: 5 blocks reaches the struck mob, the near one and the far one");
    }

    /** Ben: "Damage will scale with the gear score". Score 150: 15 x 1.5 = 22.5; and a damage enchant's 10% on top. */
    @Test
    void theShockwaveCarriesEveryTermOfTheHitThatFiredIt() {
        Stage s = stage(2);
        s.him().triggerScore = 150;
        s.him().enchantDamagePercent = 10;
        for (int i = 0; i < 3; i++) { cast(s.world(), s.him(), strike(), true); s.world().advanceTicks(1); }
        List<TracedHit> waves = from(s.world(), "tremors/shockwave");
        assertEquals(2, waves.size());
        for (TracedHit h : waves) assertEquals(15.0 * 1.5 * 1.1, h.sent(), 1e-9, "15 x 150/100 x 1.10");
    }

    /** B4: a shockwave is never a hit, so three shockwaves' worth of damage on the near mob never makes a fourth. */
    @Test
    void aShockwaveNeverCountsAsAHitAndNeverTriggersAnother() {
        Stage s = stage(3);
        for (int i = 0; i < 3; i++) { cast(s.world(), s.him(), strike(), true); s.world().advanceTicks(10); }
        assertEquals(3, from(s.world(), "tremors/shockwave").size(), "one shockwave, three mobs in 5 blocks");
        assertEquals(0, s.world().behaviourHits.streaks().held(s.him().id(), "tremors", s.near().id()),
                "the near mob took a shockwave and holds no hit");
        assertTrue(s.world().primaryHits.stream().filter(p -> !p.caster().countsAsHit()).count() == 3,
                "the three shockwave damages were reported, and none counted");
    }

    /**
     * THE RADIUS IS A SPHERE ON THE SERVER TOO. With the fixture answering as the server does (a box), a mob 3 by 3 off
     * the struck one (4.24 away, inside the box of 4, outside the sphere of 4) takes nothing; one 2.5 by 2.5 off (3.54) does.
     */
    @Test
    void theRadiusIsCutToASphereWhenTheWorldAnswersWithABox() {
        Stage s = stage(2);
        s.world().boxSearch = true;
        var corner = new FakeWorld.Dummy(new Vec3(5, 0, 3));
        var inside = new FakeWorld.Dummy(new Vec3(4.5, 0, -2.5));
        s.world().entities.addAll(List.of(corner, inside));
        for (int i = 0; i < 3; i++) { cast(s.world(), s.him(), strike(), true); s.world().advanceTicks(1); }
        assertEquals(100.0, corner.health, 1e-9, "4.24 from the struck mob: inside the box, outside the sphere");
        assertEquals(85.0, inside.health, 1e-9, "3.54 from it: inside both");
    }

    /** The STAT-READING arm counts too: a sword's basic swing is a WeaponDamage (its attack damage), and three fire one. */
    @Test
    void threeStatReadingHitsFireOneToo() {
        Stage s = stage(2);
        s.him().attackDamage = 8;
        var swing = new AbilityDefinition("probe_swing", "Swing", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Melee(4.0, 0), List.of(new EffectSpec.WeaponDamage("kinetic")));
        for (int i = 0; i < 3; i++) { cast(s.world(), s.him(), swing, true); s.world().advanceTicks(1); }
        assertEquals(3, from(s.world(), "probe_swing").size(), "the control: three swings of 8 landed");
        assertEquals(2, from(s.world(), "tremors/shockwave").size());
    }

    /** B2: a stone cast (not a weapon trigger) carries no behaviour, so it never counts. */
    @Test
    void aCastNotMadeByTheWeaponCarriesNoBehaviour() {
        Stage s = stage(2);
        for (int i = 0; i < 3; i++) { cast(s.world(), s.him(), strike(), false); s.world().advanceTicks(1); }
        assertEquals(0, from(s.world(), "tremors/shockwave").size());
        assertEquals(3, from(s.world(), "probe_strike").size(), "the control: the three strikes landed");
    }

    /** B2: a slow body lands with the behaviours it was MADE with, not what the hand holds when it arrives. */
    @Test
    void aProjectileLandsWithTheBehavioursFrozenAtTheCast() {
        Stage s = stage(2);
        var bolt = new AbilityDefinition("probe_bolt", "Bolt", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Projectile(0.5, 0, 40), List.of(new EffectSpec.Damage(8, "kinetic")));
        for (int i = 0; i < 3; i++) cast(s.world(), s.him(), bolt, true);
        s.him().behaviours = List.of();     // he swaps to a weapon with none while the three bolts fly
        s.world().advanceTicks(20);
        assertEquals(3, from(s.world(), "probe_bolt").size(), "the control: all three landed");
        assertEquals(2, from(s.world(), "tremors/shockwave").size(), "fired: the bolts carried the cast's behaviour");
    }

    /** A lingering area's tick is damage over time: never a hit. */
    @Test
    void anAreasTickIsNotAHit() {
        Stage s = stage(2);
        var field = new AbilityDefinition("probe_field", "Field", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Melee(4.0, 0), List.of(new EffectSpec.Area(1.0, 20, 2, List.of(new EffectSpec.Damage(1, "kinetic")))));
        cast(s.world(), s.him(), field, true);
        s.world().advanceTicks(25);
        assertTrue(from(s.world(), "probe_field").size() >= 3, "the control: the area ticked at least three times on it");
        assertEquals(0, from(s.world(), "tremors/shockwave").size());
    }
}
