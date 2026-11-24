package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantSlot;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.weapon.BehaviourItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * KINETIC TREMORS ON THE PAPER SIDE (the seat's brief of 2026-11-23, PR C): its tooltip and table line at each level (B5,
 * the real numbers), and which of a weapon's enchants become its BEHAVIOURS (B2: the active candidate at level 1 or more,
 * and only an enchant whose file declares a behaviour). Read from the shipped file.
 */
class KineticTremorsContentTest {

    private static EnchantRegistry shipped() {
        Logger q = Logger.getLogger("KineticTremorsContentTest-" + System.nanoTime());
        q.setUseParentHandlers(false);
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        EnchantRegistry r = new EnchantLoader(q, Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither")
                ::contains, false).loadAll(dir);
        assertTrue(r.find("sharpness").isPresent(), "CONTROL: the shipped roster loaded");
        return r;
    }

    /** B5: the line shows the level's own numbers, and it reads the same on every class (universal). */
    @Test
    void itsLineShowsEachLevelsRealNumbers() {
        EnchantDefinition t = shipped().find("kinetic_tremors").orElseThrow();
        assertEquals("3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks", EnchantEffectLine.bare(t, 1, GearClass.MELEE));
        assertEquals("3 hits on one foe in 3s: a 15 damage shockwave, 4 blocks", EnchantEffectLine.bare(t, 2, GearClass.RANGER));
        assertEquals("3 hits on one foe in 3s: a 20 damage shockwave, 5 blocks", EnchantEffectLine.bare(t, 3, GearClass.MAGE));
        assertEquals(" (3 hits on one foe in 3s: a 15 damage shockwave, 4 blocks)", EnchantEffectLine.of(t, 2, GearClass.MELEE),
                "the dev command's bracketed form");
    }

    /** Its visual loads, so the boot's check is silent; a misspelt one is named (caused, not asserted present). */
    @Test
    void itsVisualLoadsAndAMissingOneIsNamedAtBoot() {
        Logger q = Logger.getLogger("KineticTremorsContentTest-v-" + System.nanoTime());
        q.setUseParentHandlers(false);
        var visuals = new VisualLoader(q).loadAll(Path.of("src", "main", "resources", "content", "visuals").toFile());
        assertTrue(visuals.find("tremor_wave").isPresent(), "tremor_wave.yml loads");
        EnchantRegistry r = shipped();
        assertEquals(List.of(), ContentValidator.validateBehaviourVisuals(r.all(), id -> visuals.find(id).isPresent()));
        assertEquals(List.of("enchant 'kinetic_tremors' names shockwave visual 'tremor_wave', which is not a loaded visual;"
                        + " the shockwave will land unseen"),
                ContentValidator.validateBehaviourVisuals(r.all(), id -> false), "the control: the check can speak");
    }

    /** B2: only the ACTIVE candidate at level 1 or more, and only an enchant that declares a behaviour. */
    @Test
    void onlyAnActiveBehaviouralEnchantBecomesABehaviour() {
        EnchantRegistry r = shipped();
        EnchantDefinition t = r.find("kinetic_tremors").orElseThrow();
        EnchantState active = new EnchantState(List.of(EnchantSlot.empty(),
                new EnchantSlot(List.of(new EnchantCandidate("kinetic_tremors", 2)), 0),
                new EnchantSlot(List.of(new EnchantCandidate("sharpness", 3)), 0),
                EnchantSlot.empty()));
        assertEquals(List.of(new ActiveBehaviour("kinetic_tremors", 2, t.behaviour())), BehaviourItems.activeOf(active, "boltor", r::find),
                "Tremors at II; Sharpness is a stat, not a behaviour");

        EnchantState notActive = new EnchantState(List.of(EnchantSlot.empty(),
                new EnchantSlot(List.of(new EnchantCandidate("kinetic_tremors", 2)), EnchantSlot.NONE),
                EnchantSlot.empty(), EnchantSlot.empty()));
        assertEquals(List.of(), BehaviourItems.activeOf(notActive, "boltor", r::find), "bought but not the active candidate");

        EnchantState locked = new EnchantState(List.of(EnchantSlot.empty(),
                new EnchantSlot(List.of(new EnchantCandidate("kinetic_tremors", 0)), EnchantSlot.NONE),
                EnchantSlot.empty(), EnchantSlot.empty()));
        assertEquals(List.of(), BehaviourItems.activeOf(locked, "boltor", r::find), "a locked candidate");

        EnchantState unknown = new EnchantState(List.of(EnchantSlot.empty(),
                new EnchantSlot(List.of(new EnchantCandidate("gone_enchant", 1)), 0), EnchantSlot.empty(), EnchantSlot.empty()));
        assertEquals(List.of(), BehaviourItems.activeOf(unknown, "boltor", id -> Optional.empty()), "an id no longer loaded");
    }
}
