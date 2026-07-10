package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.element.Element;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeaponRegistryTest {

    /** A trigger's ability, synthesized the way WeaponLoader will: id = weaponId/input. */
    static AbilityDefinition trigger(String weaponId, String input) {
        return new AbilityDefinition(weaponId + "/" + input, "Test", Element.KINETIC, "none",
                0, ResourceCost.FREE, new CastSpec.Melee(3.0, 90),
                List.of(new EffectSpec.Damage(8, Element.KINETIC)));
    }

    static WeaponDefinition ironblade() {
        return new WeaponDefinition("ironblade", "Ironblade", Element.KINETIC, Rarity.COMMON,
                List.of(new TriggerBinding("left_click", trigger("ironblade", "left_click"))));
    }

    @Test
    void registeredWeaponIsFoundById() {
        var registry = new WeaponRegistry();
        registry.register(ironblade());

        assertEquals(1, registry.size());
        assertEquals("Ironblade", registry.find("ironblade").orElseThrow().displayName());
        assertTrue(registry.find("emberblade").isEmpty());
    }

    @Test
    void duplicateIdIsRejected() {
        var registry = new WeaponRegistry();
        registry.register(ironblade());

        assertThrows(IllegalStateException.class, () -> registry.register(ironblade()));
    }

    @Test
    void triggerLookupFindsTheBoundInputAndMissesTheAbsentOne() {
        var weapon = ironblade();
        assertTrue(weapon.trigger("left_click").isPresent());
        assertTrue(weapon.trigger("right_click").isEmpty());
    }

    @Test
    void triggersAreImmutableAfterConstruction() {
        var mutable = new ArrayList<>(List.of(
                new TriggerBinding("left_click", trigger("ironblade", "left_click"))));
        var weapon = new WeaponDefinition("ironblade", "Ironblade", Element.KINETIC, Rarity.COMMON, mutable);

        mutable.add(new TriggerBinding("right_click", trigger("ironblade", "right_click")));
        assertEquals(1, weapon.triggers().size());
    }

    @Test
    void aWeaponWithNoTriggersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeaponDefinition("empty", "Empty", Element.KINETIC, Rarity.COMMON, List.of()));
    }

    @Test
    void aNullElementIsRejectedRatherThanTreatedAsUnflavored() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeaponDefinition("x", "X", null, Rarity.COMMON,
                        List.of(new TriggerBinding("left_click", trigger("x", "left_click")))));
    }
}
