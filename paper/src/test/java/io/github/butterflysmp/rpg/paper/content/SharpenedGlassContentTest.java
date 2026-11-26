package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade;
import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * K9 and K10 (the seat's brief, 2026-11-25): Sharpened Glass as shipped, and the Broken Bottle's three pools. Ben: "6.
 * Sharpened Glass - +5/10/15% Crit Chance". Mutations named per test.
 */
class SharpenedGlassContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("SharpenedGlassContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static WeaponRegistry weapons() {
        var weapons = new WeaponLoader(capturing(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        assertTrue(weapons.find("broken_bottle").isPresent(), "CONTROL: the bottle loaded");
        return weapons;
    }

    private static EnchantRegistry shipped(List<String> warnings) {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        return EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false).loadAll(dir);
    }

    /** Ben's name and numbers; weapon-specific to the bottle; guaranteed; the new effect; no warning. */
    @Test
    void itLoadsAsBenGaveIt() {
        List<String> warnings = new ArrayList<>();
        EnchantDefinition d = shipped(warnings).find("sharpened_glass")
                .orElseThrow(() -> new AssertionError("sharpened_glass did not load: " + warnings));
        assertEquals(List.of(), warnings.stream().filter(w -> w.contains("sharpened_glass")).toList());
        assertEquals("Sharpened Glass", d.displayName());
        assertEquals(EnchantEffect.CRIT_CHANCE, d.effect());
        assertEquals("broken_bottle", d.weapon());
        assertTrue(d.guaranteed());
        assertEquals(3, d.maxLevel());
        assertEquals(List.of(5, 10, 15), d.valueByLevel());
        assertNull(d.behaviour(), "a stat effect: no behaviour block");
    }

    /** B5: the line carries the REAL number. Mutation: the arm drops the value or prints the probability. */
    @Test
    void itsLineIsTheRealNumber() {
        EnchantDefinition d = shipped(new ArrayList<>()).find("sharpened_glass").orElseThrow();
        assertEquals("+5% Crit Chance", EnchantEffectLine.bare(d, 1, GearClass.MELEE));
        assertEquals("+10% Crit Chance", EnchantEffectLine.bare(d, 2, GearClass.MELEE));
        assertEquals("+15% Crit Chance", EnchantEffectLine.bare(d, 3, GearClass.MELEE));
    }

    private static List<String> ids(List<EnchantRoll.Rollable> pool) {
        return pool.stream().map(EnchantRoll.Rollable::enchantId).sorted().toList();
    }

    /**
     * K10, THE POOLS: slot 0 is Sharpened Glass, slot 1 the kinetic unique (Kinetic Tremors), the normal pool a melee
     * weapon's (Sharpness, Unbreaking: see the plan, K6). An iron sword's slot 0 is empty and holds it in no pool.
     */
    @Test
    void theBottlesPoolsAndTheSwordsControl() {
        EnchantRegistry r = shipped(new ArrayList<>());
        var roster = EnchantRollItems.roster(r.all());
        assertTrue(roster.size() >= 20, "CONTROL: the roster loaded, " + roster.size());
        WeaponRegistry w = weapons();
        WeaponDefinition bottle = w.find("broken_bottle").orElseThrow();
        WeaponDefinition sword = w.find("iron_sword").orElseThrow();
        GearClass melee = GearClass.of(bottle.weaponClass());
        assertEquals(List.of("sharpened_glass"),
                ids(EnchantRoll.weaponPoolFor(bottle.id(), EnchantRollItems.has(bottle), melee, roster)));
        assertEquals(List.of("kinetic_tremors"),
                ids(EnchantRoll.uniquePoolFor(bottle.gearElement(), EnchantRollItems.has(bottle), melee, roster)));
        assertEquals(List.of("sharpness", "unbreaking"),
                ids(EnchantRoll.normalPoolFor(EnchantRollItems.has(bottle), melee, roster)));

        assertEquals(List.of(), ids(EnchantRoll.weaponPoolFor(sword.id(), EnchantRollItems.has(sword), melee, roster)));
        assertEquals(List.of("kinetic_tremors"),
                ids(EnchantRoll.uniquePoolFor(sword.gearElement(), EnchantRollItems.has(sword), melee, roster)));
        assertFalse(ids(EnchantRoll.normalPoolFor(EnchantRollItems.has(sword), melee, roster)).contains("sharpened_glass"));
    }

    /** G1 over many rolls: every fresh bottle offers it in slot 0, FIRST, locked; no sword ever does. */
    @Test
    void slotZeroOverManyRolls() {
        var roster = EnchantRollItems.roster(shipped(new ArrayList<>()).all());
        var cascade = new CandidateCascade(List.of(new CandidateCascade.Tier(4, 0.10), new CandidateCascade.Tier(3, 0.40),
                new CandidateCascade.Tier(2, 0.70), new CandidateCascade.Tier(1, 1.0)));
        WeaponRegistry w = weapons();
        Random random = new Random(20261125L);
        for (String id : List.of("broken_bottle", "iron_sword")) {
            WeaponDefinition d = w.find(id).orElseThrow();
            for (int n = 0; n < 500; n++) {
                EnchantState s = EnchantRoll.roll(d.id(), d.gearElement(), EnchantRollItems.has(d),
                        GearClass.of(d.weaponClass()), roster, cascade, random::nextDouble);
                var slot0 = s.slots().get(0).candidates().stream().map(EnchantCandidate::enchantId).toList();
                assertEquals(id.equals("broken_bottle") ? List.of("sharpened_glass") : List.of(), slot0, id + " roll " + n);
                assertTrue(s.slots().get(0).candidates().stream().allMatch(c -> c.level() == 0), "locked, not pre-applied");
                for (var slot : s.slots()) {
                    if (id.equals("iron_sword")) {
                        assertTrue(slot.candidates().stream().noneMatch(c -> c.enchantId().equals("sharpened_glass")));
                    }
                }
            }
        }
    }
}
