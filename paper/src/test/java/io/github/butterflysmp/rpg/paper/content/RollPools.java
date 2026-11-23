package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SHIPPED ROLL POOLS, BEFORE AND AFTER THE SEAT'S RULING H (2026-11-22), for the tests that pin it. Loaded from the
 * shipped content with the production loaders, and the roster and each weapon's quantities built by production's own
 * {@code EnchantRollItems.roster} and {@code EnchantRollItems.has}, so a test sees the pool the roll sees.
 *
 * <ul>
 *   <li>{@link #before}: the class filter alone ({@code Has.EVERY}), which is every pool as it was built before ruling H.</li>
 *   <li>{@link #after}: the weapon's own quantities, as {@code rollOnAcquire} now asks.</li>
 * </ul>
 */
record RollPools(WeaponRegistry weapons, EnchantRegistry enchants, List<EnchantRoll.Rollable> roster) {

    static RollPools load() {
        Logger quiet = Logger.getLogger("RollPools-" + System.nanoTime());
        quiet.setUseParentHandlers(false);
        WeaponRegistry weapons = new WeaponLoader(quiet).loadAll(dir("weapons"));
        EnchantRegistry enchants = new EnchantLoader(quiet).loadAll(dir("enchants"));
        assertTrue(weapons.size() > 20, "CONTROL: the shipped weapons loaded, " + weapons.size());
        assertTrue(enchants.all().size() >= 10, "CONTROL: the shipped enchants loaded, " + enchants.all().size());
        return new RollPools(weapons, enchants, EnchantRollItems.roster(enchants.all()));
    }

    private static File dir(String name) {
        File d = Path.of("src", "main", "resources", "content", name).toFile();
        assertTrue(d.isDirectory(), "no content directory " + d.getAbsolutePath());
        return d;
    }

    List<String> before(WeaponDefinition w) {
        return ids(EnchantRoll.poolFor(EnchantRoll.Has.EVERY, GearItems.gearClassOf(w), roster));
    }

    List<String> after(WeaponDefinition w) {
        return ids(EnchantRoll.poolFor(EnchantRollItems.has(w), GearItems.gearClassOf(w), roster));
    }

    private static List<String> ids(List<EnchantRoll.Rollable> pool) {
        return pool.stream().map(EnchantRoll.Rollable::enchantId).sorted().toList();
    }
}
