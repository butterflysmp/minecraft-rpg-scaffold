package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WILDFIRE AS SHIPPED (the seat's brief of 2026-11-24, PR E), through the real loaders: Ben's name and numbers, guaranteed
 * on the Living Lantern, sharing slot 0 with Airburst Flames (so the bursts never meet a chain), its line, and the
 * loader's refusals by name.
 */
class WildfireContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("WildfireContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static io.github.butterflysmp.rpg.core.weapon.WeaponRegistry weapons() {
        var weapons = new WeaponLoader(capturing(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        assertTrue(weapons.find("living_lantern").isPresent(), "CONTROL: the Living Lantern loaded");
        return weapons;
    }

    private static EnchantRegistry shipped() {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir);
        assertTrue(r.find("wildfire").isPresent(), "wildfire did not load: " + warnings);
        return r;
    }

    @Test
    void bensNameAndNumbers() {
        EnchantDefinition w = shipped().find("wildfire").orElseThrow();
        assertEquals("Wildfire", w.displayName(), "Ben: \"change the name of Hot Potato to Wildfire\"");
        assertEquals("living_lantern", w.weapon());
        assertTrue(w.guaranteed(), "G1");
        assertEquals(EnchantEffect.HIT_CHAIN, w.effect());
        assertEquals(3, w.maxLevel());
        assertEquals(new EnchantBehaviour.HitChain("living_lantern", 60, 60, List.of(5.0, 10.0, 15.0)), w.behaviour(),
                "Ben's 3 s (60 ticks), +60% cap, +5/10/15% each hit");
        assertEquals("blaze_powder", w.icon(), "SEAT PLACEHOLDER");
    }

    /** It and Airburst Flames are the Living Lantern's weapon-slot enchants: ONE slot, so only one is ever active. */
    @Test
    void itSharesSlotZeroWithAirburstFlames() {
        EnchantRegistry r = shipped();
        assertEquals(List.of("airburst_flames", "wildfire"), r.all().stream()
                .filter(d -> "living_lantern".equals(d.weapon())).map(EnchantDefinition::id).sorted().toList());
        assertEquals(0, io.github.butterflysmp.rpg.core.enchant.EnchantRoll.WEAPON_SLOT,
                "both roll in the weapon slot, whose ONE active candidate is the only one that acts (B2)");
    }

    @Test
    void itsLine() {
        EnchantDefinition w = shipped().find("wildfire").orElseThrow();
        for (int level = 1; level <= 3; level++) {
            assertEquals("+" + (5 * level) + "% per hit on one foe within 3s of the last (max +60%)",
                    EnchantEffectLine.bare(w, level, GearClass.MAGE), "level " + level);
        }
    }

    @Test
    void aMalformedChainIsRefusedByName(@TempDir Path dir) throws IOException {
        String head = "display_name: \"W\"\nmax_level: 3\neffect: hit_chain\nclass: universal\n";
        String good = "hit_chain:\n  window_ticks: 60\n  cap_percent: 60\n  step_percent_by_level: [5, 10, 15]\n";
        Files.writeString(dir.resolve("good.yml"), head + "weapon: living_lantern\n" + good);
        Files.writeString(dir.resolve("unbound.yml"), head + good);
        Files.writeString(dir.resolve("no_window.yml"), head + "weapon: living_lantern\n" + good.replace("  window_ticks: 60\n", ""));
        Files.writeString(dir.resolve("no_cap.yml"), head + "weapon: living_lantern\n" + good.replace("cap_percent: 60", "cap_percent: 0"));
        Files.writeString(dir.resolve("two_levels.yml"), head + "weapon: living_lantern\n" + good.replace("[5, 10, 15]", "[5, 10]"));
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        for (String bad : List.of("unbound", "no_window", "no_cap", "two_levels")) {
            assertTrue(r.find(bad).isEmpty(), bad + " loaded: " + warnings);
        }
        java.util.function.Predicate<String> warned = s -> warnings.stream().anyMatch(w -> w.contains(s));
        assertTrue(warned.test("'unbound' hit_chain: chains one weapon's hits, so the enchant must declare weapon: <id>"), warnings.toString());
        assertTrue(warned.test("'no_window' hit_chain: is missing a whole-number 'window_ticks'"), warnings.toString());
        assertTrue(warned.test("hit_chain cap_percent must be above 0, was 0.0"), warnings.toString());
        assertTrue(warned.test("two_levels"), warnings.toString());
    }
}
