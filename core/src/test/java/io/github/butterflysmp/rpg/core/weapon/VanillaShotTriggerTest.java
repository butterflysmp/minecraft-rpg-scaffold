package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE {@code shot} TRIGGER: how a weapon that vanilla draws and shoots (the seat's Part 2, 2026-11-22) declares its hit.
 * It binds no input of ours; its {@code cast: type: vanilla} is never cast by us, and its on_hit lands at the arrow's hit.
 * The loader refuses every shape that would bind it to nothing or to an input that would defeat vanilla's own use.
 */
class VanillaShotTriggerTest {

    private static AbilityDefinition ability(String id, CastSpec cast) {
        return new AbilityDefinition(id, "T", "kinetic", 0, ResourceCost.FREE, cast,
                List.of(new EffectSpec.WeaponDamage("kinetic")));
    }

    /** attack_speed 1.6 so a melee-cast fixture is refused for the shot rule, not for the melee speed rule. */
    private static WeaponDefinition weapon(String material, int quiver, int reload, TriggerBinding... bindings) {
        return new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.RANGER, material,
                14.0, 1.6, SweepShare.NONE, quiver, reload, List.of(bindings), List.of());
    }

    private static TriggerBinding shot() {
        return new TriggerBinding(WeaponDefinition.SHOT_TRIGGER, ability("w/shot", new CastSpec.Vanilla()));
    }

    @Test
    void aBowAndACrossbowMayDeclareTheShotAndItIsFound() {
        WeaponDefinition bow = weapon("bow", 8, 60, shot());
        assertEquals("w/shot", bow.vanillaShotTrigger().orElseThrow().id());
        WeaponDefinition crossbow = weapon("crossbow", 0, 0, shot());
        assertTrue(crossbow.vanillaShotTrigger().isPresent());
    }

    @Test
    void aWeaponWithoutTheShotHasNone() {
        WeaponDefinition plain = weapon("bow", 0, 0, new TriggerBinding("right_click",
                ability("w/rc", new CastSpec.Projectile(2.5, 0.05, 60))));
        assertTrue(plain.vanillaShotTrigger().isEmpty());
    }

    @Test
    void aVanillaCastUnderAnyOtherTriggerIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> weapon("bow", 0, 0,
                new TriggerBinding("right_click", ability("w/rc", new CastSpec.Vanilla()))));
        assertTrue(thrown.getMessage().contains("type: vanilla"), thrown.getMessage());
    }

    @Test
    void theShotTriggerWithAnyOtherCastIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> weapon("bow", 0, 0,
                new TriggerBinding(WeaponDefinition.SHOT_TRIGGER, ability("w/shot", new CastSpec.Melee(3, 90)))));
        assertTrue(thrown.getMessage().contains("cast type vanilla"), thrown.getMessage());
    }

    @Test
    void theShotOnAMaterialVanillaDoesNotShootIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> weapon("iron_sword", 0, 0, shot()));
        assertTrue(thrown.getMessage().contains("bow or crossbow"), thrown.getMessage());
    }

    /** It binds no right_click of ours (Ben's words): a right_click binding cancels vanilla's own use. */
    @Test
    void theShotBesideARightClickIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> weapon("bow", 0, 0, shot(),
                new TriggerBinding("right_click", ability("w/rc", new CastSpec.Projectile(2.5, 0.05, 60)))));
        assertTrue(thrown.getMessage().contains("right_click"), thrown.getMessage());
    }

    /** Nor the Plume's draw: PlumeDraw would take the draw over. */
    @Test
    void theShotBesideADrawIsRefused() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> weapon("bow", 0, 0, shot(),
                new TriggerBinding("draw", ability("w/draw", new CastSpec.Projectile(2.5, 0.05, 60)))));
        assertTrue(thrown.getMessage().contains("draw"), thrown.getMessage());
    }
}
