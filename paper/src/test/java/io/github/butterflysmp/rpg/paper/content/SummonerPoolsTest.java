package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SUMMONER'S POOLS (the seat's PR 1, 2026-11-24): a class with no class enchants yet gets what the existing rules give
 * it, which is the universal enchants a main-hand weapon with no magazine and no reload can hold. Measured from the shipped
 * enchants, then pinned: the pools are what the roll will offer the Bone Whistle (class summoner, element nature).
 */
class SummonerPoolsTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger quiet(List<String> into) {
        Logger log = Logger.getLogger("SummonerPoolsTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static List<EnchantRoll.Rollable> roster() {
        var weapons = new WeaponLoader(quiet(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(quiet(warnings), ELEMENTS::contains, weapons::find, false)
                .loadAll(new File(Path.of("src", "main", "resources", "content", "enchants").toString()));
        assertEquals(26, r.all().size(), "2026-11-26: + fast_and_fungal, + cluster_bombs, 26 (ls content/enchants/*.yml | wc -l; 24 at fcfbefbb); CONTROL: the 24 shipped enchants loaded (2026-11-25: + sharpened_glass, 23 at 9a6ed020): " + warnings);
        return EnchantRollItems.roster(r.all());
    }

    private static List<String> ids(List<EnchantRoll.Rollable> pool) {
        return pool.stream().map(EnchantRoll.Rollable::enchantId).sorted().toList();
    }

    @Test
    void theSummonersPoolsAreTheUniversalOnes() {
        var roster = roster();
        List<String> normal = ids(EnchantRoll.normalPoolFor(EnchantRoll.Has.NOTHING, GearClass.SUMMONER, roster));
        List<String> unique = ids(EnchantRoll.uniquePoolFor(Optional.of("nature"), EnchantRoll.Has.NOTHING,
                GearClass.SUMMONER, roster));
        List<String> weapon = ids(EnchantRoll.weaponPoolFor("bone_whistle", EnchantRoll.Has.NOTHING, GearClass.SUMMONER,
                roster));
        assertEquals(List.of("unbreaking"), normal, "slots 2 and 3: the universal enchants alone");
        assertEquals(List.of(), unique, "slot 1: no nature enchant exists");
        assertEquals(List.of(), weapon, "slot 0: no enchant names a summoner weapon");
        // The control: the same roster gives a mage weapon its class enchant, so the summoner's short pool is the class
        // filter and not an empty roster.
        assertTrue(ids(EnchantRoll.normalPoolFor(EnchantRoll.Has.NOTHING, GearClass.MAGE, roster)).contains("attunement"));
    }
}
