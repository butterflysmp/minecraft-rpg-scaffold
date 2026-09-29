package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.core.combat.Caster;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The level's damage bonus reaches WEAPON hits only (Ben, 2026-09-29: <i>"Only weapons, we'll tackle the
 * ability damage pipeline later"</i>; seat ruling L1, the flag on {@code Caster}).
 *
 * <p>Every row runs the REAL {@link AbilityService} and {@link CastExecutor}; only the world is fake. The
 * same ability is fired both ways -- as a weapon trigger ({@code fireTrigger}) and as a stone cast
 * ({@code cast}) -- so the only difference between the two readings is the path, which is the thing
 * under test.
 *
 * <p><b>THE VALUES ARE STAGED SO NO TWO READINGS COLLIDE.</b> Level bonus 7 against a literal of 10 and an
 * attack of 12: a weapon hit reads 17 or 19, a stone reads 10 or 12. The enchant row stages a 50% enchant
 * so that the level bonus multiplied by it (28.5) and added after it (25) differ.
 *
 * <p>Each row names the mutation it forces red.
 */
class LevelDamageWeaponOnlyTest {

    private static final double EPS = 1e-9;
    private static final double LEVEL_BONUS = 7.0;

    private static AbilityDefinition rayOf(EffectSpec effect) {
        return new AbilityDefinition("test", "Test", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Ray(30), List.of(effect));
    }

    private static AbilityService serviceFor(AbilityDefinition def) {
        var registry = new AbilityRegistry();
        registry.register(def);
        return new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
    }

    private static Aim east() {
        return new Aim(Vec3.ZERO, new Vec3(1, 0, 0));
    }

    private static FakeWorld.Dummy casterIn(FakeWorld world) {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        caster.levelDamageBonus = LEVEL_BONUS;
        world.entities.add(caster);
        return caster;
    }

    private static FakeWorld.Dummy victimAt(FakeWorld world, Vec3 where) {
        var victim = new FakeWorld.Dummy(where);
        world.entities.add(victim);
        return victim;
    }

    /** Fire {@code def} as a WEAPON TRIGGER and return the victim's health after. */
    private static double asWeaponTrigger(AbilityDefinition def, FakeWorld world, FakeWorld.Dummy caster,
                                          FakeWorld.Dummy victim) {
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                serviceFor(def).fireTrigger(caster.snapshot(), def, east()));
        new CastExecutor(world).execute(success);
        return victim.health;
    }

    /** Fire {@code def} as a STONE CAST and return the victim's health after. */
    private static double asStoneCast(AbilityDefinition def, FakeWorld world, FakeWorld.Dummy caster,
                                      FakeWorld.Dummy victim) {
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                serviceFor(def).cast(caster.snapshot(), def.id(), east(), Set.of(def.id())));
        new CastExecutor(world).execute(success);
        return victim.health;
    }

    // --- The flag, where it is set ---

    /**
     * Seat ruling L1's named row: a stone cast carries the flag FALSE. The dev cast too; only a weapon
     * trigger carries it true.
     * Mutation MUTLVL-RESOLVE-TRUE: resolve's weaponTrigger argument hardcoded true -> reddens here.
     */
    @Test
    void aStoneCastCarriesWeaponTriggerFalseAndOnlyATriggerCarriesItTrue() {
        var def = rayOf(new EffectSpec.Damage(10, "kinetic"));
        var caster = new FakeWorld.Dummy(Vec3.ZERO);

        var stone = assertInstanceOf(AbilityService.CastResult.Success.class,
                serviceFor(def).cast(caster.snapshot(), def.id(), east(), Set.of(def.id())));
        assertFalse(stone.weaponTrigger(), "a stone cast is not a weapon hit");

        var dev = assertInstanceOf(AbilityService.CastResult.Success.class,
                serviceFor(def).castUnchecked(caster.snapshot(), def.id(), east()));
        assertFalse(dev.weaponTrigger(), "an operator's dev cast is not a weapon hit");

        var trigger = assertInstanceOf(AbilityService.CastResult.Success.class,
                serviceFor(def).fireTrigger(caster.snapshot(), def, east()));
        assertTrue(trigger.weaponTrigger(), "a weapon trigger is");
    }

    /** {@code Caster.of} starts every cast OFF; only {@code withWeaponHit} turns the bonus on. */
    @Test
    void theCasterCarriesTheBonusInertlyUntilMarkedAWeaponHit() {
        var dummy = new FakeWorld.Dummy(Vec3.ZERO);
        dummy.levelDamageBonus = LEVEL_BONUS;

        Caster plain = Caster.of(dummy.snapshot());
        assertFalse(plain.weaponHit());
        assertEquals(LEVEL_BONUS, plain.levelDamageBonus(), EPS, "carried...");
        assertEquals(0.0, plain.weaponLevelDamage(), EPS, "...and inert");
        assertEquals(LEVEL_BONUS, plain.withWeaponHit().weaponLevelDamage(), EPS);
    }

    // --- The two damage arms, both paths ---

    /**
     * The LITERAL arm -- every staff, every stone. The case the refused attack-stat route got wrong.
     * Mutation MUTLVL-DAMAGE-ARM: drop weaponLevelDamage from the Damage arm -> the trigger reads 90.
     */
    @Test
    void aLiteralDamageWeaponTriggerAddsTheBonusAndTheSameAbilityFromAStoneDoesNot() {
        var def = rayOf(new EffectSpec.Damage(10, "kinetic"));

        var world = new FakeWorld();
        assertEquals(100 - 17, asWeaponTrigger(def, world, casterIn(world), victimAt(world, new Vec3(5, 0, 0))), EPS,
                "10 + level 7");

        var world2 = new FakeWorld();
        assertEquals(100 - 10, asStoneCast(def, world2, casterIn(world2), victimAt(world2, new Vec3(5, 0, 0))), EPS,
                "the same ability from a stone: no level damage");
    }

    /** The WeaponDamage arm -- a bow's or a sword's basic attack. Mutation MUTLVL-WEAPON-ARM reddens here. */
    @Test
    void aWeaponDamageTriggerAddsTheBonusAndAStoneCastOfItDoesNot() {
        var def = rayOf(new EffectSpec.WeaponDamage("kinetic"));

        var world = new FakeWorld();
        var caster = casterIn(world);
        caster.attackDamage = 12.0;
        assertEquals(100 - 19, asWeaponTrigger(def, world, caster, victimAt(world, new Vec3(5, 0, 0))), EPS);

        var world2 = new FakeWorld();
        var caster2 = casterIn(world2);
        caster2.attackDamage = 12.0;
        assertEquals(100 - 12, asStoneCast(def, world2, caster2, victimAt(world2, new Vec3(5, 0, 0))), EPS);
    }

    /**
     * FLAT ON TOP, beside class damage: not multiplied by a damage enchant. 10 x 1.5 + 3 + 7 = 25; inside
     * the multiplier it would be (10 + 7) x 1.5 + 3 = 28.5.
     */
    @Test
    void theBonusAddsBesideClassDamageAfterTheEnchantMultiplier() {
        var def = rayOf(new EffectSpec.Damage(10, "kinetic"));
        var world = new FakeWorld();
        var caster = casterIn(world);
        caster.enchantDamagePercent = 50.0;
        caster.classDamageBonus = 3.0;

        assertEquals(100 - 25, asWeaponTrigger(def, world, caster, victimAt(world, new Vec3(5, 0, 0))), EPS);
    }

    // --- The other two build sites ---

    /** A basic melee swing is always a weapon hit. Mutation MUTLVL-MELEE: drop withWeaponHit there. */
    @Test
    void aBasicMeleeSwingCarriesTheBonus() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        caster.attackDamage = 8.0;
        var victim = victimAt(world, new Vec3(1, 0, 0));

        new CastExecutor(world).landBasicMelee(
                new AbilityDefinition("swing", "Swing", "kinetic", 0, ResourceCost.FREE,
                        new CastSpec.Melee(3, 120), List.of(new EffectSpec.WeaponDamage("kinetic"))),
                caster.snapshot(), new Combatant(victim.snapshot(), victim), AttackCharge.FULL_CHARGE);

        assertEquals(100 - 15, victim.health, EPS, "8 + level 7");
    }

    /**
     * A VOLLEY rebuilds its Caster every shot, from a fresh snapshot -- so the flag has to be carried
     * down, not read off the committed Caster. Both shots of a trigger's volley carry it; a stone's
     * carry neither. Mutation MUTLVL-VOLLEY: drop the withWeaponHit in volley() -> the trigger reads 80.
     */
    @Test
    void everyShotOfATriggersVolleyCarriesTheBonusAndAStonesDoesNot() {
        double eye = 1.62;
        var def = new AbilityDefinition("test", "Test", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Volley(0, 2, 1, new CastSpec.Ray(30)),
                List.of(new EffectSpec.Damage(10, "kinetic")));

        var world = new FakeWorld();
        var victim = victimAt(world, new Vec3(5, eye, 0));
        asWeaponTrigger(def, world, casterIn(world), victim);
        world.advanceTicks(1);
        assertEquals(2, victim.damageCalls, "both shots landed");
        assertEquals(100 - 2 * 17, victim.health, EPS);

        var world2 = new FakeWorld();
        var victim2 = victimAt(world2, new Vec3(5, eye, 0));
        asStoneCast(def, world2, casterIn(world2), victim2);
        world2.advanceTicks(1);
        assertEquals(2, victim2.damageCalls, "both shots landed");
        assertEquals(100 - 2 * 10, victim2.health, EPS);
    }
}
