package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FOCUSED BEAM AS SHIPPED (the seat's brief of 2026-11-24, PR 2), read through the real loaders: Ben's numbers and name,
 * the seat's placeholders, the weapon it names and that weapon's volley as the brief read it, its line at each level, a
 * malformed block refused by name, and the file's lines pinned character for character against Ben's words.
 */
class FocusedBeamContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("FocusedBeamContentTest-" + System.nanoTime());
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
        assertTrue(weapons.find("cursed_emerald").isPresent(), "CONTROL: the Cursed Emerald loaded");
        return weapons;
    }

    private static EnchantDefinition shipped() {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir);
        return r.find("focused_beam").orElseThrow(() -> new AssertionError("focused_beam did not load: " + warnings));
    }

    @Test
    void bensNumbersAndTheSeatsPlaceholders() {
        EnchantDefinition f = shipped();
        assertEquals("Focused Beam", f.displayName(), "Ben: \"Focused Beam\"");
        assertEquals("cursed_emerald", f.weapon(), "W1: the Cursed Emerald's weapon slot");
        assertTrue(f.isWeaponSpecific());
        assertEquals(null, f.element(), "weapon-specific, never also unique");
        assertEquals(EnchantEffect.FOCUSED_BEAM, f.effect());
        assertEquals(new EnchantBehaviour.FocusedBeam("cursed_emerald", 48, List.of(108.0, 162.0, 216.0), "kinetic"),
                f.behaviour(), "Ben's 48 and 108/162/216; kinetic, the Cursed Emerald's");
        assertEquals(3, f.maxLevel());
        assertEquals("ender_eye", f.icon(), "SEAT PLACEHOLDER (B7)");
    }

    /**
     * What the seat read at {@code ee46a493}, confirmed from the loaded weapon: the right click "Cursed Volley", cooldown
     * 30, mana 40, on_cast emerald_windup, a volley of wind-up 20, 6 shots 2 apart, of a ray of range 32 drawing
     * emerald_beam; on_hit a visual and 27 kinetic. Everything Ben said stays the same is read from here at run time.
     */
    @Test
    void theCursedEmeraldsVolleyIsAsTheSeatReadIt() {
        WeaponDefinition ce = weapons().find("cursed_emerald").orElseThrow();
        var ability = ce.trigger("right_click").orElseThrow().ability();
        assertEquals("Cursed Volley", ability.displayName());
        assertEquals(30, ability.cooldownTicks());
        assertEquals("mana", ability.cost().resourceId());
        assertEquals(40.0, ability.cost().amount());
        assertEquals("emerald_windup", ability.onCast().getFirst().visualId());
        CastSpec.Volley v = assertInstanceOf(CastSpec.Volley.class, ability.cast());
        assertEquals(20, v.windupTicks());
        assertEquals(3, v.shots()); // Ben, 2026-11-24: "decrease the normal volley count to 3"
        assertEquals(2, v.intervalTicks());
        CastSpec.Ray ray = assertInstanceOf(CastSpec.Ray.class, v.of());
        assertEquals(32.0, ray.range());
        assertEquals("emerald_beam", ray.beam());
        assertEquals(false, ray.pierce());
        assertEquals(2, ability.onHit().size());
        assertEquals(new io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage(27, "kinetic"), ability.onHit().get(1));
        assertEquals(List.of("Channel, then loose three", "bolts down your line of sight."), ability.description(),
                "only six became three, 2026-11-24 (the wording OPEN FOR BEN)");
    }

    /** B5: the level's real numbers. */
    @Test
    void itsLineShowsEachLevelsBeamDamage() {
        EnchantDefinition f = shipped();
        for (int level = 1; level <= 3; level++) {
            assertEquals("after the wind-up, fires one beam of " + new String[]{"108", "162", "216"}[level - 1]
                    + " damage reaching 48 blocks", EnchantEffectLine.bare(f, level, GearClass.MAGE), "level " + level);
        }
    }

    /** Ben's words against the file, character for character, comments stripped (a value written otherwise is caught). */
    @Test
    void theFileCarriesBensNumbersAsWritten() throws IOException {
        List<String> lines = Files.readString(Path.of("src/main/resources/content/enchants/focused_beam.yml"),
                StandardCharsets.UTF_8).replace("\r", "").lines().map(l -> l.replaceAll("\\s+#.*$", "")).toList();
        for (String want : List.of("display_name: \"Focused Beam\"", "effect: focused_beam", "weapon: cursed_emerald",
                "max_level: 3", "focused_beam:", "  range: 48", "  damage_by_level: [108, 162, 216]", "  element: kinetic")) {
            assertTrue(lines.contains(want), "focused_beam.yml holds '" + want + "': " + lines);
        }
    }

    /**
     * The loader refuses, by name: a weapon whose right click is not a volley of rays (the Boltor's is a plain ray, the
     * Breacher's a projectile); no weapon at all; a level list not matching max_level; a non-positive range. And one good
     * file, the control.
     */
    @Test
    void aMalformedFocusedBeamIsRefusedByName(@TempDir Path dir) throws IOException {
        String head = "display_name: \"F\"\nmax_level: 3\neffect: focused_beam\nclass: universal\n";
        String good = "focused_beam:\n  range: 48\n  damage_by_level: [108, 162, 216]\n  element: kinetic\n";
        Files.writeString(dir.resolve("good.yml"), head + "weapon: cursed_emerald\n" + good);
        Files.writeString(dir.resolve("plain_ray.yml"), head + "weapon: boltor\n" + good);
        Files.writeString(dir.resolve("projectile.yml"), head + "weapon: breacher\n" + good);
        Files.writeString(dir.resolve("unbound.yml"), head + good);
        Files.writeString(dir.resolve("two_levels.yml"), head + "weapon: cursed_emerald\n"
                + good.replace("[108, 162, 216]", "[108, 162]"));
        Files.writeString(dir.resolve("zero_range.yml"), head + "weapon: cursed_emerald\n" + good.replace("range: 48", "range: 0"));
        Files.writeString(dir.resolve("negative_range.yml"), head + "weapon: cursed_emerald\n"
                + good.replace("range: 48", "range: -48"));
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        for (String bad : List.of("plain_ray", "projectile", "unbound", "two_levels", "zero_range", "negative_range")) {
            assertTrue(r.find(bad).isEmpty(), bad + " loaded: " + warnings);
        }
        java.util.function.Predicate<String> warned = s -> warnings.stream().anyMatch(w -> w.contains(s));
        assertTrue(warned.test("'plain_ray' focused_beam: weapon 'boltor''s right click is not a volley of rays"), warnings.toString());
        assertTrue(warned.test("'projectile' focused_beam: weapon 'breacher''s right click is not a volley of rays"),
                warnings.toString());
        assertTrue(warned.test("'unbound' focused_beam: replaces one weapon's volley, so the enchant must declare weapon: <id>"),
                warnings.toString());
        assertTrue(warned.test("focused_beam range must be above 0, was 0.0"), warnings.toString());
        assertTrue(warned.test("focused_beam range must be above 0, was -48.0"), warnings.toString());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("two_levels")), "two_levels named: " + warnings);
    }
}
