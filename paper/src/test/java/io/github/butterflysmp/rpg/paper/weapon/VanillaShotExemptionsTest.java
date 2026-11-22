package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S RULING 11, 2026-11-22: EXACTLY THREE EXEMPTIONS, and the seven shipped bows and crossbows unchanged.
 *
 * <ol>
 *   <li>The main-hand right-click already passes a weapon that binds none (READ, {@code WeaponFire.attempt} returns empty
 *       for an unbound input, so {@code onRightClick} cancels nothing). No change, so no row here.</li>
 *   <li>{@code onWeaponCrossbowLoad} skips a weapon that binds no right_click ({@link VanillaShots#crossbowLoadCancelled}).</li>
 *   <li>PlumeDraw's gate keys on the weapon's own draw trigger ({@link PlumeDraw#drawManaged}).</li>
 * </ol>
 *
 * <p>The seven are loaded from the SHIPPED content, and each gate is compared with the BASE's expression for it, written
 * out here: the Plume's gate was "material bow and no right_click", and every minted crossbow's load was cancelled.
 */
class VanillaShotExemptionsTest {

    private static final List<String> SEVEN = List.of(
            "short_bow", "withered_shortbow", "dragons_plume", "boltor", "locust", "dragons_breath", "breacher");

    private static WeaponRegistry shipped() {
        Logger quiet = Logger.getLogger("VanillaShotExemptionsTest-" + System.nanoTime());
        quiet.setUseParentHandlers(false);
        File dir = Path.of("src", "main", "resources", "content", "weapons").toFile();
        assertTrue(dir.isDirectory(), "no weapons directory: " + dir.getAbsolutePath());
        return new WeaponLoader(quiet).loadAll(dir);
    }

    /** The base's Plume gate, verbatim in meaning: a bow that binds no right_click. */
    private static boolean baseDrawManaged(WeaponDefinition w) {
        return "bow".equalsIgnoreCase(w.material()) && w.trigger("right_click").isEmpty();
    }

    @Test
    void theSevenAreLoadedAndNoneIsVanillaDriven() {
        WeaponRegistry weapons = shipped();
        for (String id : SEVEN) {
            WeaponDefinition w = weapons.find(id).orElseThrow(() -> new AssertionError(id + " is not loaded"));
            assertTrue(w.vanillaShotTrigger().isEmpty(), id + " binds no shot");
        }
    }

    @Test
    void thePlumeGateIsUnchangedForTheSevenAndTakesOnlyThePlume() {
        WeaponRegistry weapons = shipped();
        for (String id : SEVEN) {
            WeaponDefinition w = weapons.find(id).orElseThrow();
            assertEquals(baseDrawManaged(w), PlumeDraw.drawManaged(w), id + ": the draw gate moved");
            assertEquals(id.equals("dragons_plume"), PlumeDraw.drawManaged(w), id);
        }
    }

    @Test
    void theCrossbowLoadIsCancelledForEveryOneOfTheSevenAsOnTheBase() {
        WeaponRegistry weapons = shipped();
        for (String id : SEVEN) {
            assertTrue(VanillaShots.crossbowLoadCancelled(weapons.find(id)), id + ": the load would now run");
        }
        assertTrue(VanillaShots.crossbowLoadCancelled(Optional.empty()), "an id naming no weapon: cancelled, as on the base");
    }

    // ------------------------------------------------------------------ the two exemptions, on a vanilla-driven weapon

    private static WeaponDefinition vanillaDriven(String material) {
        AbilityDefinition shot = new AbilityDefinition(material + "/shot", "Shot", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Vanilla(), List.of(new EffectSpec.WeaponDamage("kinetic")));
        return new WeaponDefinition(material, "X", "kinetic", Rarity.COMMON, WeaponClass.RANGER, material,
                14.0, 0.0, SweepShare.NONE, 0, 0, List.of(new TriggerBinding(WeaponDefinition.SHOT_TRIGGER, shot)),
                List.of());
    }

    /** The base's gate WOULD have taken the vanilla Bow (a bow binding no right_click); the new one does not. */
    @Test
    void theVanillaBowIsNotTakenByThePlumeGate() {
        WeaponDefinition bow = vanillaDriven("bow");
        assertTrue(baseDrawManaged(bow), "CONTROL: the base gate captures it, which is why exemption 3 exists");
        assertFalse(PlumeDraw.drawManaged(bow));
    }

    @Test
    void theVanillaCrossbowIsLoadedByVanilla() {
        assertFalse(VanillaShots.crossbowLoadCancelled(Optional.of(vanillaDriven("crossbow"))));
    }
}
