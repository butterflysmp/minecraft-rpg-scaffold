package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BEN, 2026-11-23 (the seat's brief, "THE BOLTOR'S SHOT SOUND AND THE FLINT STAFF'S TEXT"), verbatim:
 * <ol>
 *   <li>"Add a sound to the Boltor's shots. Tell me what the old repo uses and give me a recomendation" / told the old
 *       repo's ({@code Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.3f}, once per shot): "use the old one"</li>
 *   <li>"Replace the current description with this: "Prehistoric magicians learned you could create fire by slamming
 *       mana into flint.""</li>
 *   <li>"Change the Ability description to: "Hurls a fragment of burning mana""</li>
 * </ol>
 * Each row compares against his words character for character, so a reworded or re-punctuated line goes red.
 */
class BoltorSoundFlintTextTest {

    private static final String BENS_FLAVOR = "Prehistoric magicians learned you could create fire by slamming mana into flint.";
    private static final String BENS_DESCRIPTION = "Hurls a fragment of burning mana";

    private static Logger quiet() {
        Logger log = Logger.getLogger("BoltorSoundFlintTextTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        return log;
    }

    private static WeaponRegistry weapons() {
        return new WeaponLoader(quiet()).loadAll(new File("src/main/resources/content/weapons"));
    }

    private static AbilityDefinition rightClick(WeaponDefinition w) {
        return w.triggers().stream().filter(t -> t.input().equals("right_click")).map(TriggerBinding::ability)
                .findFirst().orElseThrow();
    }

    /** The Boltor's right_click casts ONE visual, boltor_cast, and it is ONE sound: item.crossbow.shoot at 1.0, 1.3. */
    @Test
    void theBoltorsShotPlaysTheOldReposSound() {
        AbilityDefinition shot = rightClick(weapons().find("boltor").orElseThrow());
        assertEquals(List.of(new EffectSpec.Visual("boltor_cast")), shot.onCast());
        VisualDefinition cast = new VisualLoader(quiet()).loadAll(new File("src/main/resources/content/visuals"))
                .find("boltor_cast").orElseThrow();
        assertEquals(1, cast.steps().size(), "a sound step only; no particle added");
        VisualSpec.Sound sound = (VisualSpec.Sound) cast.steps().getFirst();
        assertEquals("item.crossbow.shoot", sound.key());
        assertEquals(1.0f, sound.volume());
        assertEquals(1.3f, sound.pitch());
    }

    /** Nothing else on the Boltor's shot moved: cooldown 16, range 96, its beam, one weapon_damage. */
    @Test
    void nothingElseInTheBoltorsShotChanged() {
        AbilityDefinition shot = rightClick(weapons().find("boltor").orElseThrow());
        assertEquals(16, shot.cooldownTicks());
        assertEquals(1, shot.onHit().size());
        assertTrue(shot.onHit().getFirst() instanceof EffectSpec.WeaponDamage, shot.onHit().toString());
    }

    /** The flavor, joined by single spaces, IS Ben's sentence, every character and the full stop. */
    @Test
    void theFlintStaffsFlavorIsBensSentence() {
        WeaponDefinition staff = weapons().find("flint_staff").orElseThrow();
        assertEquals(BENS_FLAVOR, String.join(" ", staff.flavor()));
        assertEquals(List.of("Prehistoric magicians learned you could", "create fire by slamming mana into flint."),
                staff.flavor(), "the seat's break (SEAT PLACEHOLDER 2026-11-23)");
    }

    /** The ability description IS Ben's line, with NO trailing full stop; the name and the numbers stay. */
    @Test
    void theFlintStaffsAbilityDescriptionIsBensLine() {
        AbilityDefinition bolt = rightClick(weapons().find("flint_staff").orElseThrow());
        assertEquals(List.of(BENS_DESCRIPTION), bolt.description());
        assertFalse(bolt.description().getFirst().endsWith("."), "no full stop, as Ben typed it");
        assertEquals("Fire Bolt", bolt.displayName());
        assertEquals(24, bolt.cooldownTicks());
        assertEquals(5.0, bolt.cost().amount());
    }
}
