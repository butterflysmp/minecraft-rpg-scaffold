package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
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
 * AIRBURST FLAMES AS SHIPPED (the seat's brief of 2026-11-23, PR E), read through the real loaders: Ben's numbers and
 * name, the seat's placeholders, the weapon it names, its line at each level, and a malformed block refused by name.
 */
class AirburstFlamesContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("AirburstFlamesContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static java.util.function.Function<String, java.util.Optional<GearClass>> shippedWeapons() {
        var weapons = new WeaponLoader(capturing(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        assertTrue(weapons.find("living_lantern").isPresent(), "CONTROL: the lantern loaded");
        return id -> weapons.find(id).map(GearItems::gearClassOf);
    }

    private static EnchantDefinition shipped() {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = new EnchantLoader(capturing(warnings), ELEMENTS::contains, shippedWeapons(), false).loadAll(dir);
        return r.find("airburst_flames").orElseThrow(() -> new AssertionError("airburst_flames did not load: " + warnings));
    }

    @Test
    void bensNumbersAndTheSeatsPlaceholders() {
        EnchantDefinition a = shipped();
        assertEquals("Airburst Flames", a.displayName(), "Ben: \"The name of the enchantment is Airburst Flames\"");
        assertEquals("living_lantern", a.weapon(), "W1: the lantern's weapon slot");
        assertTrue(a.isWeaponSpecific());
        assertEquals(null, a.element(), "weapon-specific, never also unique");
        assertEquals(EnchantEffect.AIRBURST, a.effect());
        assertEquals(new EnchantBehaviour.Airburst(14, 3, 12, 0.7, 14, List.of(25.0, 30.0, 35.0), "fire"), a.behaviour(),
                "Ben's 14, three, 14 more and 25/30/35; the seat's 12 degrees and 0.7");
        assertEquals(3, a.maxLevel());
        assertEquals("fire_charge", a.icon(), "SEAT PLACEHOLDER (B7)");
    }

    /** B5: the level's real numbers. */
    @Test
    void itsLineShowsEachLevelsBurstDamage() {
        EnchantDefinition a = shipped();
        assertEquals("splits after 14 blocks into 3 bursts of 25 damage", EnchantEffectLine.bare(a, 1, GearClass.MAGE));
        assertEquals("splits after 14 blocks into 3 bursts of 30 damage", EnchantEffectLine.bare(a, 2, GearClass.MAGE));
        assertEquals("splits after 14 blocks into 3 bursts of 35 damage", EnchantEffectLine.bare(a, 3, GearClass.MAGE));
    }

    /** B1: a malformed airburst block is refused by the key's name; one file may not declare two behaviour blocks. */
    @Test
    void aMalformedAirburstIsRefusedByName(@TempDir Path dir) throws IOException {
        String head = "display_name: \"A\"\nmax_level: 3\neffect: airburst\nclass: universal\nweapon: living_lantern\n";
        String good = "airburst:\n  travel_blocks: 14\n  count: 3\n  angle_degrees: 12\n  speed: 0.7\n"
                + "  burst_travel_blocks: 14\n  damage_by_level: [25, 30, 35]\n  element: fire\n";
        Files.writeString(dir.resolve("good.yml"), head + good);
        Files.writeString(dir.resolve("no_speed.yml"), head + good.replace("  speed: 0.7\n", ""));
        Files.writeString(dir.resolve("one_burst.yml"), head + good.replace("count: 3", "count: 1"));
        Files.writeString(dir.resolve("two_blocks.yml"), head + good
                + "shockwave:\n  hits: 3\n  window_ticks: 60\n  cooldown_ticks: 5\n  damage_by_level: [1, 2, 3]\n"
                + "  radius_by_level: [1, 2, 3]\n  element: fire\n");
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = new EnchantLoader(capturing(warnings), ELEMENTS::contains, shippedWeapons(), false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        for (String bad : List.of("no_speed", "one_burst", "two_blocks")) assertTrue(r.find(bad).isEmpty(), bad);
        java.util.function.Predicate<String> warned = s -> warnings.stream().anyMatch(w -> w.contains(s));
        assertTrue(warned.test("'no_speed' airburst: is missing the number 'speed'"), warnings.toString());
        assertTrue(warned.test("'one_burst' airburst: airburst count must be 2 or more, was 1"), warnings.toString());
        assertTrue(warned.test("'two_blocks' declares [shockwave, airburst]; one behaviour block at most"), warnings.toString());
    }
}
