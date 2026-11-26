package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.CastTunes;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FAST AND FUNGAL (the seat's F7, 2026-11-26; Ben: "reduces cooldown by 1/2/3s"): a COOLDOWN cast tune, 20 / 40 / 60 ticks
 * off the Blast Fungus's 140, resolved in ONE place ({@link CastTunes}) that the press's stamp and the tooltip both read,
 * applied at the press by the weapon in hand (B2), never to 0.
 */
class FastAndFungalTest {

    private static final EnchantBehaviour.CastTune FAST = new EnchantBehaviour.CastTune("blast_fungus",
            EnchantBehaviour.CastTune.Quantity.COOLDOWN, List.of(20.0, 40.0, 60.0));
    private static final Aim FORWARD = new Aim(Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(0, 0, 1));

    private static AbilityDefinition fungus(String weapon, int cooldown) {
        return new AbilityDefinition(weapon + "/right_click", "Fungal Barrage", "nature", cooldown,
                new ResourceCost("mana", 30),
                new CastSpec.Projectile(0.7, 0.04, 100, null, "nether_wart_block", null, null,
                        new CastSpec.Spread(3, 25, 0), 40),
                List.of(new EffectSpec.Burst(3, List.of(new EffectSpec.Damage(55, "nature")))));
    }

    private static List<ActiveBehaviour> at(int level) {
        return level == 0 ? List.of() : List.of(new ActiveBehaviour("fast_and_fungal", level, FAST));
    }

    @Test
    void theOneArmCutsTheCooldownByEachLevel() {
        assertEquals(140, CastTunes.tune(fungus("blast_fungus", 140), at(0)).cooldownTicks());
        assertEquals(120, CastTunes.tune(fungus("blast_fungus", 140), at(1)).cooldownTicks());
        assertEquals(100, CastTunes.tune(fungus("blast_fungus", 140), at(2)).cooldownTicks());
        assertEquals(80, CastTunes.tune(fungus("blast_fungus", 140), at(3)).cooldownTicks());
        AbilityDefinition other = fungus("flint_staff", 140);
        assertSame(other, CastTunes.tune(other, at(3)), "another weapon's cast: untouched, the same instance");
    }

    @Test
    void itNeverReachesZero() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> CastTunes.tune(fungus("blast_fungus", 60), at(3)));
        assertTrue(e.getMessage().contains("blast_fungus/right_click"), e.getMessage());
        assertEquals(1, CastTunes.tune(fungus("blast_fungus", 61), at(3)).cooldownTicks(), "1 tick is allowed");
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.CastTune("blast_fungus",
                EnchantBehaviour.CastTune.Quantity.COOLDOWN, List.of(0.0)), "a cut of 0 is no cut");
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.CastTune("blast_fungus",
                EnchantBehaviour.CastTune.Quantity.COOLDOWN, List.of(2.5)), "whole ticks only");
    }

    /** The press STAMPS the tuned length, reads the weapon in hand's behaviours, and carries the UNTUNED cast on. */
    @Test
    void thePressStampsTheTunedCooldown() {
        for (int level = 0; level <= 3; level++) {
            var caster = new FakeWorld.Dummy(Vec3.ZERO);
            var cooldowns = new CooldownTracker(() -> 0L);
            var service = new AbilityService(new AbilityRegistry(), cooldowns, new ResourcePool(() -> 0L, 100, 1.0));
            AbilityDefinition trigger = fungus("blast_fungus", 140);
            var result = assertInstanceOf(AbilityService.CastResult.Success.class,
                    service.fireTrigger(caster.snapshot(), trigger, FORWARD, at(level)));
            assertEquals(140 - 20 * level, cooldowns.ticksRemaining(caster.id(), "blast_fungus/right_click"),
                    "level " + level);
            assertSame(trigger, result.ability(), "the Success carries the untuned cast: the executor tunes it at commit");
            assertEquals(140 - 20 * level, AbilityService.stampedCooldownTicks(trigger, at(level), 1.0),
                    "the extracted function is the stamp's own figure");
        }
    }

    /** A press with no behaviours (the three-argument form every other caller uses) stamps the authored cooldown. */
    @Test
    void theOldFormStampsTheAuthoredCooldown() {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var cooldowns = new CooldownTracker(() -> 0L);
        var service = new AbilityService(new AbilityRegistry(), cooldowns, new ResourcePool(() -> 0L, 100, 1.0));
        service.fireTrigger(caster.snapshot(), fungus("blast_fungus", 140), FORWARD);
        assertEquals(140, cooldowns.ticksRemaining(caster.id(), "blast_fungus/right_click"));
    }
}
