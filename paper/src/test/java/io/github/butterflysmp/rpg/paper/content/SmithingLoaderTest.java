package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.SmithingUpgrades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code content/smithing.yml} (the seat's brief, part g): the upgrades the smithing table makes. Each refused row is named
 * and the rest load; one {@code Loaded smithing} line names every upgrade.
 */
class SmithingLoaderTest {

    @TempDir
    Path dir;

    private final List<String> lines = new ArrayList<>();
    private Logger log;

    private static final Set<String> WEAPONS = Set.of("diamond_sword", "netherite_sword", "iron_sword");
    private static final Set<String> MATERIALS = Set.of("netherite_upgrade_smithing_template", "netherite_ingot");

    @BeforeEach
    void setUp() {
        log = Logger.getLogger("SmithingLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { lines.add(r.getLevel() + " " + r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private SmithingUpgrades load(String yaml) throws IOException {
        Path file = dir.resolve("smithing.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return new SmithingLoader(log, WEAPONS::contains, MATERIALS::contains).load(file.toFile());
    }

    private String text() { return String.join("\n", lines); }

    @Test
    void theShippedFileDeclaresTheNetheriteUpgrade() {
        SmithingUpgrades u = new SmithingLoader(log, WEAPONS::contains, MATERIALS::contains)
                .load(Path.of("src/main/resources/content/smithing.yml").toFile());
        assertEquals(List.of(new SmithingUpgrades.Upgrade("diamond_sword", "netherite_upgrade_smithing_template",
                "netherite_ingot", "netherite_sword")), u.all());
        assertEquals(List.of("INFO Loaded smithing: 1 upgrades [diamond_sword + netherite_upgrade_smithing_template"
                + " + netherite_ingot -> netherite_sword]"), lines);
    }

    @Test
    void aRowNamingNoWeaponOrNoMaterialIsRefusedByNameAndTheRestLoads() throws IOException {
        SmithingUpgrades u = load("""
                upgrades:
                  - base: no_such_sword
                    template: netherite_upgrade_smithing_template
                    addition: netherite_ingot
                    result: netherite_sword
                  - base: diamond_sword
                    template: netherite_upgrade_smithing_template
                    addition: no_such_ingot
                    result: netherite_sword
                  - base: diamond_sword
                    template: netherite_upgrade_smithing_template
                    addition: netherite_ingot
                    result: no_such_result
                  - base: diamond_sword
                    template: netherite_upgrade_smithing_template
                    addition: netherite_ingot
                    result: netherite_sword
                """);
        assertEquals(1, u.size(), "the sound row loads: " + text());
        assertTrue(text().contains("Refusing smithing upgrade 1: base 'no_such_sword' is not a weapon"), text());
        assertTrue(text().contains("Refusing smithing upgrade 2: addition 'no_such_ingot' is not a material"), text());
        assertTrue(text().contains("Refusing smithing upgrade 3: result 'no_such_result' is not a weapon"), text());
    }

    @Test
    void aMissingFieldOrAClashIsRefusedByName() throws IOException {
        SmithingUpgrades u = load("""
                upgrades:
                  - base: diamond_sword
                    template: netherite_upgrade_smithing_template
                    result: netherite_sword
                  - base: diamond_sword
                    template: netherite_upgrade_smithing_template
                    addition: netherite_ingot
                    result: netherite_sword
                  - base: diamond_sword
                    template: netherite_upgrade_smithing_template
                    addition: netherite_ingot
                    result: iron_sword
                """);
        assertEquals(1, u.size(), text());
        assertTrue(text().contains("Refusing smithing upgrade 1: it needs base, template, addition and result"), text());
        assertTrue(text().contains("Refusing smithing upgrade 3: it clashes with an earlier upgrade"), text());
    }

    @Test
    void anAbsentFileLoadsNoUpgradesAndSaysSo() {
        SmithingUpgrades u = new SmithingLoader(log, WEAPONS::contains, MATERIALS::contains)
                .load(dir.resolve("nope.yml").toFile());
        assertEquals(0, u.size());
        assertTrue(text().contains("Refusing smithing: content/smithing.yml is absent"), text());
    }
}
