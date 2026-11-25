package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.FakeWorld;
import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 3a: a summon cast is the summon service's, decided before the cast is charged; reaching the engine is named. */
class SummonCastEngineTest {

    @Test
    void aSummonCastThatReachesTheEngineIsNamedAsAWiringDefect() {
        var def = new AbilityDefinition("whistle_cast", "Test", "nature", 0, ResourceCost.FREE,
                new CastSpec.Summon("test_wolf"), List.of());
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var world = new FakeWorld();
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        world.entities.add(him);
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(him.snapshot(), def.id(), new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0)), Set.of(def.id())));
        var ex = assertThrows(IllegalStateException.class, () -> new CastExecutor(world).execute(success));
        assertTrue(ex.getMessage().contains("summon cast 'test_wolf' reached the engine"), ex.getMessage());
    }

    @Test
    void aSummonCastNamesItsSummon() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Summon(" "));
    }
}
