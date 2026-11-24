package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 * SHATTER CAPS AS SHIPPED (the seat's brief of 2026-11-24), read through the real loaders: Ben's numbers and name, the
 * seat's placeholders, the weapon it names, its line at each level, a malformed block refused by name, and the file's
 * lines pinned character for character against Ben's words.
 */
class ShatterCapsContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("ShatterCapsContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static java.util.function.Function<String, java.util.Optional<io.github.butterflysmp.rpg.core.weapon.WeaponDefinition>> shippedWeapons() {
        var weapons = new WeaponLoader(capturing(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        assertTrue(weapons.find("breacher").isPresent(), "CONTROL: the Breacher loaded");
        return weapons::find;
    }

    private static EnchantDefinition shipped() {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, shippedWeapons(), false).loadAll(dir);
        return r.find("shatter_caps").orElseThrow(() -> new AssertionError("shatter_caps did not load: " + warnings));
    }

    @Test
    void bensNumbersAndTheSeatsPlaceholders() {
        EnchantDefinition s = shipped();
        assertEquals("Shatter Caps", s.displayName(), "Ben: \"Shatter Caps\"");
        assertEquals("breacher", s.weapon(), "W1: the Breacher's weapon slot");
        assertTrue(s.isWeaponSpecific());
        assertEquals(null, s.element(), "weapon-specific, never also unique");
        assertEquals(EnchantEffect.SHATTER, s.effect());
        assertEquals(new EnchantBehaviour.Shatter("breacher", 5, 2.5, "stone", List.of(35.0, 60.0, 85.0), "kinetic"),
                s.behaviour(), "Ben's 5 blocks and 35/60/85; the seat's 2.5 a tick and stone; kinetic, the Breacher's");
        assertEquals(3, s.maxLevel());
        assertEquals("cobblestone", s.icon(), "SEAT PLACEHOLDER (B7)");
    }

    /** B5: the level's real numbers. */
    @Test
    void itsLineShowsEachLevelsStoneDamage() {
        EnchantDefinition s = shipped();
        for (int level = 1; level <= 3; level++) {
            assertEquals("fires one stone of " + new String[]{"35", "60", "85"}[level - 1]
                            + " damage that shatters after 5 blocks into the usual chunks",
                    EnchantEffectLine.bare(s, level, GearClass.RANGER), "level " + level);
        }
    }

    /** Ben's words against the file, character for character, comments stripped (a value written otherwise is caught). */
    @Test
    void theFileCarriesBensNumbersAsWritten() throws IOException {
        List<String> lines = Files.readString(Path.of("src/main/resources/content/enchants/shatter_caps.yml"),
                StandardCharsets.UTF_8).replace("\r", "").lines().map(l -> l.replaceAll("\\s+#.*$", "")).toList();
        for (String want : List.of("display_name: \"Shatter Caps\"", "effect: shatter", "weapon: breacher", "max_level: 3",
                "shatter:", "  travel_blocks: 5", "  speed: 2.5", "  item: stone", "  damage_by_level: [35, 60, 85]",
                "  element: kinetic")) {
            assertTrue(lines.contains(want), "shatter_caps.yml holds '" + want + "': " + lines);
        }
    }

    /**
     * The loader refuses, by name: a weapon whose right click is not a projectile with a spread; no weapon at all; a level
     * list not matching max_level; a non-positive distance or speed. And one good file, the control.
     */
    @Test
    void aMalformedShatterIsRefusedByName(@TempDir Path dir) throws IOException {
        String head = "display_name: \"S\"\nmax_level: 3\neffect: shatter\nclass: universal\n";
        String good = "shatter:\n  travel_blocks: 5\n  speed: 2.5\n  item: stone\n  damage_by_level: [35, 60, 85]\n"
                + "  element: kinetic\n";
        Files.writeString(dir.resolve("good.yml"), head + "weapon: breacher\n" + good);
        Files.writeString(dir.resolve("ray.yml"), head + "weapon: boltor\n" + good);
        Files.writeString(dir.resolve("no_spread.yml"), head + "weapon: living_lantern\n" + good);
        Files.writeString(dir.resolve("unbound.yml"), head + good);
        Files.writeString(dir.resolve("two_levels.yml"), head + "weapon: breacher\n" + good.replace("[35, 60, 85]", "[35, 60]"));
        Files.writeString(dir.resolve("zero_travel.yml"), head + "weapon: breacher\n" + good.replace("travel_blocks: 5", "travel_blocks: 0"));
        Files.writeString(dir.resolve("zero_speed.yml"), head + "weapon: breacher\n" + good.replace("speed: 2.5", "speed: 0"));
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, shippedWeapons(), false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        for (String bad : List.of("ray", "no_spread", "unbound", "two_levels", "zero_travel", "zero_speed")) {
            assertTrue(r.find(bad).isEmpty(), bad + " loaded: " + warnings);
        }
        java.util.function.Predicate<String> warned = s -> warnings.stream().anyMatch(w -> w.contains(s));
        assertTrue(warned.test("'ray' shatter: weapon 'boltor''s right click is not a projectile with a spread"), warnings.toString());
        assertTrue(warned.test("'no_spread' shatter: weapon 'living_lantern''s right click is not a projectile with a spread"),
                warnings.toString());
        assertTrue(warned.test("'unbound' shatter: replaces one weapon's spread, so the enchant must declare weapon: <id>"),
                warnings.toString());
        assertTrue(warned.test("shatter travel_blocks must be above 0, was 0.0"), warnings.toString());
        assertTrue(warned.test("shatter speed must be above 0, was 0.0"), warnings.toString());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("two_levels")), "two_levels named: " + warnings);
    }
}
