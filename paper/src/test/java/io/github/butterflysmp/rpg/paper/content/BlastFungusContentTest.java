package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.weapon.MintedMushroomUse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE BLAST FUNGUS AS SHIPPED (the seat's brief of 2026-11-26, via Ben; PLAN-blast-fungus.md). Ben's numbers (55 a blast x
 * 3, radius 3, 30 mana, 140 ticks), the kept fuse (40 from the throw, rests, early on a mob), mage / uncommon / nature / a
 * red mushroom, outside the tiers, in no loot source (F12), and F11's decisions for a minted red mushroom.
 */
class BlastFungusContentTest {

    private static Path content() {
        try {
            var url = BlastFungusContentTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Logger quiet(List<String> warnings) {
        Logger log = Logger.getLogger("BlastFungusContentTest-" + System.nanoTime());
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

    private static WeaponDefinition fungus(List<String> warnings) {
        WeaponRegistry weapons = new WeaponLoader(quiet(warnings)).loadAll(content().resolve("weapons").toFile());
        return weapons.find("blast_fungus").orElseThrow(() -> new AssertionError("blast_fungus did not load: " + warnings));
    }

    @Test
    void itLoadsWithBensNumbersAndNoWarning() {
        List<String> warnings = new ArrayList<>();
        WeaponDefinition f = fungus(warnings);
        assertEquals(List.of(), warnings.stream().filter(w -> w.contains("blast_fungus")).toList());
        assertEquals("Blast Fungus", f.displayName());
        assertEquals(WeaponClass.MAGE, f.weaponClass());
        assertEquals(Rarity.UNCOMMON, f.rarity());
        assertEquals("nature", f.element());
        assertEquals("red_mushroom", f.material());
        assertEquals(1, f.triggers().size(), "one trigger, the right click");
    }

    /** Ben's 55 x 3, radius 3, 30 mana, 7 s; the kept fuse; the three chunks are the flight's spread (F5). */
    @Test
    void theRightClickIsThreeFusedChunksEachABurstOf55() {
        AbilityDefinition a = fungus(new ArrayList<>()).trigger("right_click").orElseThrow().ability();
        assertEquals("blast_fungus/right_click", a.id());
        assertEquals(140, a.cooldownTicks(), "BEN: 7 s");
        assertEquals(0, a.cooldownTicks() % 4, "on the 4-tick grid");
        assertEquals(new ResourceCost("mana", 30), a.cost());
        CastSpec.Projectile p = assertInstanceOf(CastSpec.Projectile.class, a.cast());
        assertEquals(40, p.fuseTicks(), "BEN: Keep -- 2 s from the throw");
        assertEquals(new CastSpec.Spread(3, 25, 0), p.spread());
        assertEquals(0.7, p.speed());
        assertEquals(0.04, p.gravity());
        assertEquals("nether_wart_block", p.item());
        assertNull(p.homing());
        assertNull(p.body());
        assertEquals(List.of(new EffectSpec.Visual("fungus_blast"),
                new EffectSpec.Burst(3, List.of(new EffectSpec.Damage(55, "nature")))), a.onHit());
    }

    private static List<Path> files(String dir) throws IOException {
        try (Stream<Path> s = Files.list(content().resolve(dir))) {
            return s.filter(p -> p.toString().endsWith(".yml")).toList();
        }
    }

    private static boolean names(Path file, String id) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).contains(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** OUTSIDE THE TIERS (K5's manner) and F12: no craft mints it, no recipe and no loot source names it. */
    @Test
    void itIsObtainedByNoRecipeAndNoLootSource() throws IOException {
        assertEquals(Optional.empty(), fungus(new ArrayList<>()).craftResult(), "no vanilla craft mints it");
        List<Path> recipes = files("recipes");
        assertTrue(recipes.stream().anyMatch(p -> names(p, "flint_staff")), "control: the search finds a named weapon");
        for (Path p : recipes) assertFalse(names(p, "blast_fungus"), p.getFileName() + " names it");
        Path cascade = content().resolve("loot").resolve("cascade.yml");
        assertTrue(names(cascade, "flint_staff"), "control: the search finds a pooled weapon");
        assertFalse(names(cascade, "blast_fungus"), "in no source until the world-loot pass (F12)");
    }

    /** F11: a MINTED red mushroom only; the two blocks whose use takes one; a hopper into a composter. */
    @Test
    void mintedMushroomUseDecidesForTheMintedItemOnly() {
        assertTrue(MintedMushroomUse.isMintedMushroom(true, "red_mushroom"));
        assertFalse(MintedMushroomUse.isMintedMushroom(false, "red_mushroom"), "a plain red mushroom is untouched");
        assertFalse(MintedMushroomUse.isMintedMushroom(true, "brown_mushroom"));
        assertFalse(MintedMushroomUse.isMintedMushroom(true, "glass_bottle"), "the bottle is K7's, not this");
        assertTrue(MintedMushroomUse.denyBlock("flower_pot"));
        assertTrue(MintedMushroomUse.denyBlock("composter"));
        assertFalse(MintedMushroomUse.denyBlock("chest"), "a chest still opens with it in hand");
        assertTrue(MintedMushroomUse.refuseMoveInto(true, "composter"));
        assertFalse(MintedMushroomUse.refuseMoveInto(false, "composter"));
        assertFalse(MintedMushroomUse.refuseMoveInto(true, "chest"));
    }
}
