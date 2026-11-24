package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CRYSTAL'S DRAWN LINE NEEDS A BEAM TO DRAW WITH (the seat's ruling on Ben's item 4, 2026-11-23): the line takes its
 * visual from the ray that met the orb, so a weapon granted an orb with a shot, whose ray trigger names no beam, would
 * strike unseen. Warned at load, by the enchant, the weapon and the trigger.
 */
class OrbBeamCheckTest {

    private static AbilityDefinition ray(String id, String beam) {
        return new AbilityDefinition(id, "Beam", "kinetic", 20, ResourceCost.FREE, new CastSpec.Ray(26, beam, false, 0),
                List.of(new EffectSpec.Damage(21, "kinetic")));
    }

    private static WeaponDefinition staff(String id, String beam) {
        return new WeaponDefinition(id, "Staff", "kinetic", Rarity.COMMON, WeaponClass.MAGE, "breeze_rod", 0.0, 0.0, 0.0,
                0, 0, List.of(new TriggerBinding("right_click", ray(id + "/right_click", beam))), List.of(),
                Optional.empty(), false, null);
    }

    private static EnchantBehaviour.GrantTrigger crystal(String weaponId, boolean shot) {
        CastSpec.OrbShot s = shot ? new CastSpec.OrbShot(8, List.of(30.0, 50.0, 70.0), "kinetic", null) : null;
        AbilityDefinition thrown = new AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal", "kinetic", 600,
                new ResourceCost("mana", 30), new CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea", s), List.of());
        return new EnchantBehaviour.GrantTrigger(weaponId, "left_click", thrown);
    }

    @Test
    void aRayThatNamesNoBeamIsNamedAtLoad() {
        List<String> problems = ContentValidator.orbBeamProblems("lapis_crystal", crystal("plain_staff", true),
                id -> Optional.of(staff(id, null)));
        assertEquals(List.of("enchant 'lapis_crystal' grants an orb whose shots are drawn with the beam of the ray that"
                + " meets it, but weapon 'plain_staff' trigger 'right_click' is a ray that names no beam: its shots there"
                + " will land undrawn"), problems);
    }

    @Test
    void aRayThatNamesABeamIsFineAndSoIsAnOrbWithNoShot() {
        assertEquals(List.of(), ContentValidator.orbBeamProblems("lapis_crystal", crystal("lapis_staff", true),
                id -> Optional.of(staff(id, "lapis_beam"))), "the control: a beam to draw with");
        assertTrue(ContentValidator.orbBeamProblems("lapis_crystal", crystal("plain_staff", false),
                id -> Optional.of(staff(id, null))).isEmpty(), "no shot, nothing drawn, nothing to warn");
        assertTrue(ContentValidator.orbBeamProblems("lapis_crystal", crystal("gone", true), id -> Optional.empty()).isEmpty(),
                "an unknown weapon is the loader's refusal, not this check's");
    }
}
