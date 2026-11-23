package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.ArrivalClaims;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.SmithingUpgrades;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE BOW AND THE CROSSBOW, AS BEN RULED THEM (2026-11-22, verbatim in each file's header and PLAN-weapon-tiers.md):
 * "The vanilla bow and cross bow are the wood/stone tier weapons"; "3. Bow at 14 and crossbow at 37"; "4. Both common
 * and kinetic"; "5. Vanilla crafting recipe and vanilla mob drops convert"; "I want to own the Quiver for the bow but
 * not the crossbow." Each SEAT PLACEHOLDER is pinned so that changing it is a visible edit.
 */
class VanillaBowsContentTest {

    private static Logger quiet() {
        Logger log = Logger.getLogger("VanillaBowsContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        return log;
    }

    private static File dir(String name) {
        File d = Path.of("src", "main", "resources", "content", name).toFile();
        assertTrue(d.isDirectory(), "no content directory " + d.getAbsolutePath());
        return d;
    }

    private static WeaponRegistry weapons() {
        return new WeaponLoader(quiet()).loadAll(dir("weapons"));
    }

    private static void assertVanillaShot(WeaponDefinition w) {
        var shot = w.vanillaShotTrigger().orElseThrow(() -> new AssertionError(w.id() + " binds no shot"));
        assertTrue(shot.cast() instanceof CastSpec.Vanilla, w.id() + ": cast type vanilla");
        assertEquals(List.of(new EffectSpec.WeaponDamage("kinetic")), shot.onHit(), w.id() + ": one kinetic weapon hit");
        assertTrue(w.trigger("right_click").isEmpty(), w.id() + ": binds no right_click of ours (Ben)");
        assertTrue(w.trigger("draw").isEmpty(), w.id() + ": the Plume's draw is not its");
        assertEquals(1, w.triggers().size(), w.id() + ": the shot is its only trigger");
    }

    @Test
    void theBowIsBensNumbers() {
        WeaponDefinition bow = weapons().find("bow").orElseThrow();
        assertEquals("Bow", bow.displayName());
        assertEquals(WeaponClass.RANGER, bow.weaponClass());
        assertEquals(Rarity.COMMON, bow.rarity());              // Ben: "Both common and kinetic"
        assertEquals("kinetic", bow.element());
        assertEquals("bow", bow.material());
        assertEquals(14.0, bow.attackDamage(), 0.0);            // Ben: "Bow at 14"; x force is SEAT PLACEHOLDER
        assertEquals(8, bow.quiverSize());                     // Ben: "own the Quiver for the bow"
        assertEquals(60, bow.reloadTicks());                   // SEAT PLACEHOLDER 2026-11-22 (short_bow's)
        assertEquals("arrow", bow.ammo());
        assertEquals(Optional.of("bow"), bow.craftResult());   // Ben: "Vanilla crafting recipe ... convert"
        assertVanillaShot(bow);
    }

    @Test
    void theCrossbowIsBensNumbers() {
        WeaponDefinition crossbow = weapons().find("crossbow").orElseThrow();
        assertEquals("Crossbow", crossbow.displayName());
        assertEquals(WeaponClass.RANGER, crossbow.weaponClass());
        assertEquals(Rarity.COMMON, crossbow.rarity());
        assertEquals("kinetic", crossbow.element());
        assertEquals("crossbow", crossbow.material());
        assertEquals(37.0, crossbow.attackDamage(), 0.0);      // Ben: "crossbow at 37"
        assertFalse(crossbow.hasQuiver(), "Ben: not the crossbow's quiver");
        assertFalse(crossbow.hasReload(), "Ben: 'The vanilla reload time'; vanilla's 25-tick charge, none of ours");
        assertEquals(Optional.of("crossbow"), crossbow.craftResult());
        assertVanillaShot(crossbow);
    }

    /** Both claims hold across ALL shipped gear, and both materials convert on arrival (the swords' route). */
    @Test
    void bothRecipesAreClaimedAndBothMaterialsConvertOnArrival() {
        WeaponRegistry weapons = weapons();
        List<GearDefinition> all = new ArrayList<>(weapons.all());
        all.addAll(new ShieldLoader(quiet()).loadAll(dir("shields")).all());
        all.addAll(new ArmorLoader(quiet()).loadAll(dir("armor")).all());
        all.addAll(new ToolLoader(quiet()).loadAll(dir("tools")).all());
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(all, List.of(), problems::add);
        assertEquals(List.of(), problems, "no contest anywhere in shipped gear");
        assertEquals("bow", index.forResult("bow").orElseThrow().id());
        assertEquals("crossbow", index.forResult("crossbow").orElseThrow().id());

        ArrivalClaims claims = ArrivalClaims.build(weapons.all(), index, new SmithingUpgrades(List.of()), problems::add);
        assertEquals(new ArrivalClaims.Decision(ArrivalClaims.Kind.MINT, "bow"), claims.decide(false, "bow"));
        assertEquals(new ArrivalClaims.Decision(ArrivalClaims.Kind.MINT, "crossbow"), claims.decide(false, "crossbow"));
        // CONTROL: a minted short_bow (tagged) is left alone; ruling 13, a mob's own bow is never asked (it is not an
        // arrival into a player's inventory).
        assertEquals(ArrivalClaims.Kind.LEAVE_TAGGED, claims.decide(true, "bow").kind());
        assertEquals(List.of(), problems);
    }

    /** Ruling H: the Bow keeps the full ranger pool; the Crossbow's is power and unbreaking. */
    @Test
    void eachPoolHoldsOnlyWhatTheWeaponHas() {
        RollPools roll = RollPools.load();
        assertEquals(List.of("deft_hands", "expanded_quiver", "power", "unbreaking"),
                roll.after(roll.weapons().find("bow").orElseThrow()));
        assertEquals(List.of("power", "unbreaking"), roll.after(roll.weapons().find("crossbow").orElseThrow()));
    }
}
