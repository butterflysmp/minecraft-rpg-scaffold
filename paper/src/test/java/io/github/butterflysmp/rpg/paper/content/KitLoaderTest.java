package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.kit.KitRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A kit declares its (class, element) key and the weapons + abilities it grants. These pin
 * that, plus the fail-soft contract: a kit missing its key or granting nothing is skipped, and
 * every other kit still loads.
 */
class KitLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("KitLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private void write(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
    }

    private KitRegistry load() {
        return new KitLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void loadsAKitWithItsKeyGrantsAndEquipFlag() throws IOException {
        write("ranger_fire.yml", """
                class: ranger
                element: fire
                display_name: "Fire Ranger"
                weapons:
                  - { id: hunters_bow, equip: true }
                abilities:
                  - arc_surge
                """);

        KitRegistry registry = load();

        assertEquals(1, registry.size());
        KitDefinition kit = registry.find("ranger", "fire").orElseThrow();
        assertEquals("Fire Ranger", kit.displayName());
        assertEquals(1, kit.weapons().size());
        assertEquals("hunters_bow", kit.weapons().get(0).weaponId());
        assertTrue(kit.weapons().get(0).equip(), "equip: true is read");
        assertEquals(List.of("arc_surge"), kit.abilityIds());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void equipDefaultsToFalseWhenOmitted() throws IOException {
        write("mage_fire.yml", """
                class: mage
                element: fire
                weapons:
                  - { id: ember_staff }
                abilities: []
                """);

        var kit = load().find("mage", "fire").orElseThrow();
        assertFalse(kit.weapons().get(0).equip(), "no equip key -> not equipped");
    }

    @Test
    void aKitMissingItsClassIsSkippedNotCrashed() throws IOException {
        write("aaa_bad.yml", """
                element: fire
                abilities: [arc_surge]
                """);
        write("ranger_fire.yml", "class: ranger\nelement: fire\nabilities: [arc_surge]\n");

        KitRegistry registry = load();

        assertEquals(1, registry.size(), "the valid kit must still load");
        assertTrue(warningText().contains("aaa_bad.yml"), warningText());
        assertTrue(warningText().contains("class"), warningText());
    }

    @Test
    void aKitThatGrantsNothingIsSkippedNotCrashed() throws IOException {
        write("aaa_empty.yml", "class: ghost\nelement: fire\n");
        write("ranger_fire.yml", "class: ranger\nelement: fire\nabilities: [arc_surge]\n");

        KitRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_empty.yml"), warningText());
        assertTrue(warningText().contains("grants nothing"), warningText());
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new KitLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The shipped Ranger cell, parsed by the loader we actually run. */
    @Test
    void bundledRangerFireKitLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/kits/ranger_fire.yml")) {
            assertNotNull(in, "bundled ranger_fire is missing from the classpath");
            Files.write(dir.resolve("ranger_fire.yml"), in.readAllBytes());
        }

        KitRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        KitDefinition kit = registry.find("ranger", "fire").orElseThrow();
        assertTrue(kit.weapons().stream().anyMatch(w -> w.weaponId().equals("hunters_bow") && w.equip()),
                "the bow is granted and auto-equipped");
        assertTrue(kit.abilityIds().contains("arc_surge"));
    }

    /** The shipped Mage cell -- the commit half of the head-to-head. */
    @Test
    void bundledMageFireKitLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/kits/mage_fire.yml")) {
            assertNotNull(in, "bundled mage_fire is missing from the classpath");
            Files.write(dir.resolve("mage_fire.yml"), in.readAllBytes());
        }

        KitRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        KitDefinition kit = registry.find("mage", "fire").orElseThrow();
        assertTrue(kit.weapons().stream().anyMatch(w -> w.weaponId().equals("ember_staff") && w.equip()),
                "the staff is granted and auto-equipped");
        assertTrue(kit.abilityIds().containsAll(List.of("solar_grenade", "solar_lance")));
    }
}
