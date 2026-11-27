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
 * THE BONE CLUB AS SHIPPED (the seat's brief, 2026-11-26; PLAN-bone-club.md M1). Ben, verbatim: "1. 44 damage, 0.8s ...
 * 6. Melee, Kinetic, common, outside the tiers 7. Yes 8. No sweep". Built exactly as the Broken Bottle was (K1, K4, K5,
 * K6). Every figure below is read from the shipped file.
 */
class BoneClubContentTest {

    private static Path content() {
        try {
            var url = BoneClubContentTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Logger quiet(List<String> warnings) {
        Logger log = Logger.getLogger("BoneClubContentTest-" + System.nanoTime());
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

    private static WeaponDefinition club(List<String> warnings) {
        WeaponRegistry weapons = new WeaponLoader(quiet(warnings)).loadAll(content().resolve("weapons").toFile());
        return weapons.find("bone_club").orElseThrow(() -> new AssertionError("bone_club did not load: " + warnings));
    }

    /** Ben's numbers and names, and NO warning. */
    @Test
    void itLoadsWithBensNumbersAndNoWarning() {
        List<String> warnings = new ArrayList<>();
        WeaponDefinition c = club(warnings);
        assertEquals(List.of(), warnings.stream().filter(w -> w.contains("bone_club")).toList());
        assertEquals("Bone Club", c.displayName());
        assertEquals(WeaponClass.MELEE, c.weaponClass());
        assertEquals(Rarity.COMMON, c.rarity());
        assertEquals("kinetic", c.element());
        assertEquals("bone", c.material());
        assertEquals(44.0, c.attackDamage());
        assertEquals(0.8, c.attackSpeed());
        assertEquals(0.8, c.meleeCadence(), "the speed reaches the wielder: its hit is vanilla-driven");
        assertEquals(0.0, c.critChance(), "the bare club has no crit bonus (Ben: Drumming Beat IS the enchant)");
        assertEquals(List.of(), c.flavor(), "SEAT PLACEHOLDER: Ben has given no flavor");
    }

    /** M1 (as K4): NO SWEEP. Absent is 0, which SweepShare reads as "does not sweep", and the sweep rider cancels on that. */
    @Test
    void itDoesNotSweep() {
        WeaponDefinition c = club(new ArrayList<>());
        assertEquals(SweepShare.NONE, c.sweep());
        assertFalse(SweepShare.sweeps(c.sweep()));
    }

    /** M1 (as K1, K5): a melee basic, scored like a sword (44 x score / 100), with the swords' reach and arc (placeholders). */
    @Test
    void itHitsAndScoresAsASwordDoes() {
        WeaponDefinition c = club(new ArrayList<>());
        var basic = c.vanillaMeleeTrigger().orElseThrow();
        CastSpec.Melee melee = assertInstanceOf(CastSpec.Melee.class, basic.cast());
        assertEquals(3.0, melee.reach());
        assertEquals(120.0, melee.arcDegrees());
        assertEquals(List.of(new EffectSpec.WeaponDamage("kinetic")), basic.onHit());
        assertTrue(GearScore.carriesScore(GearItems.gearClassOf(c), c.unscored()), "it carries a gear score");
        assertEquals(44.0, GearScore.scaledDamage(44.0, 100));
        assertEquals(88.0, GearScore.scaledDamage(44.0, 200));
    }

    /** The bare club holds no crit source (the bottle's control: it holds one). */
    @Test
    void itHoldsNoCritSource() {
        assertEquals(Map.of(), HeldWeaponCrit.sources(Optional.of(club(new ArrayList<>()))));
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
     * M1 and M11: no craft claim, no recipe file names it, and no loot source or dev source does. Each absence has a
     * control: the same search finds a weapon that IS there.
     */
    @Test
    void itIsObtainedByNoRecipeAndNoLootSource() throws IOException {
        WeaponDefinition c = club(new ArrayList<>());
        assertEquals(Optional.empty(), c.craftResult(), "outside the tiers: no vanilla craft mints it");

        List<Path> recipes = files("recipes");
        assertTrue(recipes.size() >= 8, "control: the recipes were listed, " + recipes.size());
        assertTrue(recipes.stream().anyMatch(p -> { try { return names(p, "flint_staff"); } catch (IOException e) { throw new RuntimeException(e); } }),
                "control: the search finds a weapon a recipe does name");
        for (Path p : recipes) assertFalse(names(p, "bone_club"), p.getFileName() + " names it");

        Path cascade = content().resolve("loot").resolve("cascade.yml");
        assertTrue(names(cascade, "flint_staff"), "control: the search finds a weapon the dev source does pool");
        assertFalse(names(cascade, "bone_club"), "it is in no source and no dev source until the loot pass");
        try (Stream<Path> loot = Files.list(content().resolve("loot"))) {
            for (Path p : loot.filter(f -> f.toString().endsWith(".yml")).toList()) {
                assertFalse(names(p, "bone_club"), p.getFileName() + " names it");
            }
        }
    }
}
