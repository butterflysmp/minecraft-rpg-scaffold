package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.HeldWeaponCrit;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE BROKEN BOTTLE AS SHIPPED (the seat's brief, 2026-11-25). Ben, verbatim: "1. 18 Damage, 2.0 Speed. +10% Crit Chance
 * 2. Yes it's damage scales with gear score. Not it's crit chance 3. No sweep 4. Common Kinetic, outside of the tiers 5.
 * Only lootable from Shipwrecks and Sunken Treasure". Every figure below is read from the shipped file.
 */
class BrokenBottleContentTest {

    private static Path content() {
        try {
            var url = BrokenBottleContentTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Logger quiet(List<String> warnings) {
        Logger log = Logger.getLogger("BrokenBottleContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) {
                if (r.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(r.getMessage());
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static WeaponDefinition bottle(List<String> warnings) {
        WeaponRegistry weapons = new WeaponLoader(quiet(warnings)).loadAll(content().resolve("weapons").toFile());
        return weapons.find("broken_bottle").orElseThrow(() -> new AssertionError("broken_bottle did not load: " + warnings));
    }

    /** Ben's numbers and names, and NO warning (an unknown key, crit_chance before the loader knew it, warns). */
    @Test
    void itLoadsWithBensNumbersAndNoWarning() {
        List<String> warnings = new ArrayList<>();
        WeaponDefinition b = bottle(warnings);
        assertEquals(List.of(), warnings.stream().filter(w -> w.contains("broken_bottle")).toList());
        assertEquals("Broken Bottle", b.displayName());
        assertEquals(WeaponClass.MELEE, b.weaponClass());
        assertEquals(Rarity.COMMON, b.rarity());
        assertEquals("kinetic", b.element());
        assertEquals("glass_bottle", b.material());
        assertEquals(18.0, b.attackDamage());
        assertEquals(2.0, b.attackSpeed());
        assertEquals(2.0, b.meleeCadence(), "the speed reaches the wielder: its hit is vanilla-driven");
        assertEquals(0.10, b.critChance());
        assertEquals(List.of(), b.flavor(), "SEAT PLACEHOLDER: Ben has given no flavor");
    }

    /** K4: NO SWEEP. Absent is 0, which SweepShare reads as "does not sweep", and the sweep rider cancels on that. */
    @Test
    void itDoesNotSweep() {
        WeaponDefinition b = bottle(new ArrayList<>());
        assertEquals(SweepShare.NONE, b.sweep());
        assertFalse(SweepShare.sweeps(b.sweep()));
    }

    /** K1 and K5: a melee basic, scored like a sword (18 x score / 100), with the swords' reach and arc (placeholders). */
    @Test
    void itHitsAndScoresAsASwordDoes() {
        WeaponDefinition b = bottle(new ArrayList<>());
        var basic = b.vanillaMeleeTrigger().orElseThrow();
        CastSpec.Melee melee = assertInstanceOf(CastSpec.Melee.class, basic.cast());
        assertEquals(3.0, melee.reach());
        assertEquals(120.0, melee.arcDegrees());
        assertEquals(List.of(new EffectSpec.WeaponDamage("kinetic")), basic.onHit());
        assertTrue(GearScore.carriesScore(GearItems.gearClassOf(b), b.unscored()), "it carries a gear score");
        assertEquals(18.0, GearScore.scaledDamage(18.0, 100));
        assertEquals(36.0, GearScore.scaledDamage(18.0, 200));
    }

    /** K2: the held source is the authored 0.10, the same at any score (the source takes none). */
    @Test
    void itsCritIsOneFlatSource() {
        assertEquals(Map.of(HeldWeaponCrit.SOURCE, 0.10), HeldWeaponCrit.sources(Optional.of(bottle(new ArrayList<>()))));
    }

    private static List<Path> files(String dir) throws IOException {
        try (Stream<Path> s = Files.list(content().resolve(dir))) {
            return s.filter(p -> p.toString().endsWith(".yml")).toList();
        }
    }

    private static boolean names(Path file, String id) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8).contains(id);
    }

    /**
     * K5 and K8: no craft claim, no recipe file names it, and no loot source or dev source does. Each absence has a
     * control: the same search finds a weapon that IS there.
     */
    @Test
    void itIsObtainedByNoRecipeAndNoLootSource() throws IOException {
        WeaponDefinition b = bottle(new ArrayList<>());
        assertEquals(Optional.empty(), b.craftResult(), "outside of the tiers: no vanilla craft mints it");

        List<Path> recipes = files("recipes");
        assertTrue(recipes.size() >= 8, "control: the recipes were listed, " + recipes.size());
        assertTrue(recipes.stream().anyMatch(p -> { try { return names(p, "flint_staff"); } catch (IOException e) { throw new RuntimeException(e); } }),
                "control: the search finds a weapon a recipe does name");
        for (Path p : recipes) assertFalse(names(p, "broken_bottle"), p.getFileName() + " names it");

        Path cascade = content().resolve("loot").resolve("cascade.yml");
        assertTrue(names(cascade, "flint_staff"), "control: the search finds a weapon the dev source does pool");
        assertFalse(names(cascade, "broken_bottle"), "it is in no source and no dev source until the world-loot pass");
    }
}
