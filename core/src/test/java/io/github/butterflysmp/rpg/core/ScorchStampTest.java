package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.BehaviourHits;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Caster;
import io.github.butterflysmp.rpg.core.combat.HitAccrual;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MANA BURN'S STAMP, F1 (the seat's ruling of 2026-11-24 on PR F): a primary hit by a weapon carrying the enchant hands the
 * damage port a {@code HitAccrual} stamped with the enchant's percent at its level; every other hit hands it 0. The port
 * (paper's {@code BukkitCombatant}) gives the stamp to the scorch it applies; F2 makes the tick and Ignite read it.
 *
 * <p>"The stamp is taken from the weapon that made the hit, at the level it had then (B2); a scorch applied by a status
 * tick, a secondary or an unenchanted hit carries none." Each of those has its row.
 */
class ScorchStampTest {

    private static final EnchantBehaviour.ScorchBoost MANA_BURN = new EnchantBehaviour.ScorchBoost(List.of(10.0, 20.0, 30.0));
    private static final double X0 = 4, Z = 8, EYE = 1.62;

    private static AbilityDefinition staff(EffectSpec hit) {
        return new AbilityDefinition("flint_staff/right_click", "Spark", "fire", 24, new ResourceCost("mana", 8),
                new CastSpec.Ray(28, "beam"), List.of(hit), List.of());
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him, FakeWorld.Dummy mob) {}

    private static Stage stage(int level, boolean mobIsPlayer) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(new Vec3(X0, 0, Z));
        him.player = true;
        him.attackDamage = 20;
        if (level > 0) him.behaviours = List.of(new ActiveBehaviour("mana_burn", level, MANA_BURN));
        var mob = new FakeWorld.Dummy(new Vec3(X0 + 10, EYE, Z));
        mob.health = 10_000;
        mob.player = mobIsPlayer;
        world.entities.add(him);
        world.entities.add(mob);
        return new Stage(world, him, mob);
    }

    private static io.github.butterflysmp.rpg.core.combat.Combatant target(Stage s) {
        return s.world().combatant(s.mob().id()).orElseThrow();
    }

    private static void press(Stage s, AbilityDefinition a) {
        Aim aim = s.world().aimOf(s.him().id()).orElseThrow();
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(a, s.him().snapshot(), aim, true));
    }

    /** Level III on the Damage arm: the port is handed 30, on an ordinary accruing, depth-0 hit. */
    @Test
    void aHitByTheEnchantedWeaponIsStampedWithItsLevelsPercent() {
        for (int level = 1; level <= 3; level++) {
            Stage s = stage(level, false);
            press(s, staff(new EffectSpec.Damage(20, "fire")));
            assertEquals(1, s.mob().damageCalls, "the hit landed");
            assertEquals(10.0 * level, s.mob().lastDamageStamp, 0.0, "level " + level);
            assertEquals(0, s.mob().lastDamageDepth, "a weapon hit, depth 0");
        }
    }

    /** The stat-reading arm (weapon_damage) stamps the same. */
    @Test
    void theWeaponDamageArmIsStampedToo() {
        Stage s = stage(3, false);
        press(s, staff(new EffectSpec.WeaponDamage("fire")));
        assertEquals(1, s.mob().damageCalls);
        assertEquals(30.0, s.mob().lastDamageStamp, 0.0);
    }

    /** The control: the same hit with no enchant hands the port 0. */
    @Test
    void anUnenchantedHitCarriesNone() {
        Stage s = stage(0, false);
        press(s, staff(new EffectSpec.Damage(20, "fire")));
        assertEquals(1, s.mob().damageCalls);
        assertEquals(0.0, s.mob().lastDamageStamp, 0.0);
    }

    /** A secondary damage never stamps, whatever its caster carries (it does not count as a hit). */
    @Test
    void aSecondaryCarriesNone() {
        Stage s = stage(3, false);
        Caster secondary = Caster.of(s.him().snapshot(), 1.0).withBehaviours(s.him().behaviours).asSecondary("mana_burn/test");
        assertEquals(0.0, BehaviourHits.scorchStamp(secondary, target(s)), 0.0);
        assertEquals(30.0, BehaviourHits.scorchStamp(Caster.of(s.him().snapshot(), 1.0).withBehaviours(s.him().behaviours), target(s)), 0.0,
                "the control: the same caster as a primary stamps");
    }

    /**
     * A damage-over-time tick's caster ({@code asNotAHit}) KEEPS the weapon's behaviours and is not a hit: it stamps
     * nothing. This is the row that reaches the {@code countsAsHit} check; the secondary row above cannot, because
     * {@code asSecondary} also clears the behaviours (MUTSTAMPHIT stayed green on that row alone).
     */
    @Test
    void aDamageOverTimeTickCarriesNone() {
        Stage s = stage(3, false);
        Caster tick = Caster.of(s.him().snapshot(), 1.0).withBehaviours(s.him().behaviours).asNotAHit();
        assertEquals(1, tick.behaviours().size(), "the tick's caster still carries Mana Burn");
        assertEquals(0.0, BehaviourHits.scorchStamp(tick, target(s)), 0.0);
    }

    /** A hit on a player is not a hit (B4), so it stamps nothing. */
    @Test
    void aHitOnAPlayerCarriesNone() {
        Stage s = stage(3, true);
        assertEquals(0.0, BehaviourHits.scorchStamp(Caster.of(s.him().snapshot(), 1.0).withBehaviours(s.him().behaviours), target(s)), 0.0);
    }

    /** The accrual's own shape: every factory carries 0; a stamp is a percent of 0 or more. */
    @Test
    void theAccrualCarriesTheStampAndEveryFactoryCarriesNone() {
        assertEquals(0.0, HitAccrual.weapon().scorchStampPercent(), 0.0);
        assertEquals(0.0, HitAccrual.inert().scorchStampPercent(), 0.0, "a status tick");
        assertEquals(0.0, HitAccrual.chained(2).scorchStampPercent(), 0.0, "an Ignite link");
        assertEquals(0.0, HitAccrual.terminal().scorchStampPercent(), 0.0);
        HitAccrual stamped = HitAccrual.weapon().withScorchStamp(30);
        assertEquals(30.0, stamped.scorchStampPercent(), 0.0);
        assertEquals(HitAccrual.weapon().rule(), stamped.rule());
        assertEquals(0, stamped.depth());
        assertThrows(IllegalArgumentException.class, () -> HitAccrual.weapon().withScorchStamp(-1));
    }

    /** The behaviour's levels, and its refusals. */
    @Test
    void theBehaviourGivesItsPercentPerLevelAndRefusesNone() {
        assertEquals(10.0, MANA_BURN.percentFor(1), 0.0);
        assertEquals(20.0, MANA_BURN.percentFor(2), 0.0);
        assertEquals(30.0, MANA_BURN.percentFor(3), 0.0);
        assertEquals(3, MANA_BURN.levels());
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.ScorchBoost(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.ScorchBoost(List.of(10.0, 0.0)));
    }
}
