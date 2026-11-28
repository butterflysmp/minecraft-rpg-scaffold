package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
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
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SMOLDER AS SHIPPED (Ben's numbers and the seat's Z1, Z9, A1, A3, 2026-11-27), parsed by the loader we run. Every
 * number is Ben's: 160 per pulse, 12 blocks, 80 mana, 300 s, 4 pulses 3 s apart, 1.5 s to the edge.
 */
class SmolderContentTest {

    @TempDir Path dir;

    private final List<String> warnings = new ArrayList<>();

    private AbilityRegistry loadBundled(String... files) throws IOException {
        for (String f : files) {
            try (var in = getClass().getResourceAsStream("/content/abilities/" + f)) {
                assertNotNull(in, "bundled content is missing: " + f);
                Files.write(dir.resolve(f), in.readAllBytes());
            }
        }
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { warnings.add(r.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        });
        return new AbilityLoader(log).loadAll(dir.toFile());
    }

    @Test
    void smolderLoadsWithBensNumbers() throws IOException {
        AbilityDefinition def = loadBundled("smolder.yml").find("smolder").orElseThrow();
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals("fire", def.element());
        assertEquals(6000, def.cooldownTicks(), "300 seconds");
        assertEquals("mana", def.cost().resourceId());
        assertEquals(80, def.cost().amount());
        var pulses = assertInstanceOf(CastSpec.Pulses.class, def.cast());
        assertEquals(new CastSpec.Pulses(4, 60, 12.0, 30, "smolder_ring", "smolder_pulse"), pulses);
        assertEquals(List.of(new EffectSpec.Damage(160, "fire")), def.onHit());
    }

    /**
     * A1: THE DAMAGE ONLY. Scorch is applied by the element, so no status step is authored (a second route, capped at the
     * unhalved hit). Control: the one effect IS fire, the element that applies Scorch.
     */
    @Test
    void smolderAuthorsNoStatusStepAndItsDamageIsFire() throws IOException {
        AbilityDefinition def = loadBundled("smolder.yml").find("smolder").orElseThrow();
        assertFalse(def.onHit().stream().anyMatch(EffectSpec.Status.class::isInstance), "no status step");
        assertEquals("fire", ((EffectSpec.Damage) def.onHit().get(0)).element());
        String fire = Files.readString(Path.of("src/main/resources/content/elements/fire.yml"), StandardCharsets.UTF_8);
        assertTrue(fire.lines().anyMatch(l -> l.strip().equals("applies_status: scorch")), "fire applies scorch");
    }

    /**
     * Z9: THE MENU'S PROSE STATES THE FILE'S OWN NUMBERS. Each sentence is rebuilt from the parsed definition, so a
     * retuned number with stale prose is a red row: the count as a word, the interval in seconds, the damage, the
     * radius in whole blocks.
     */
    @Test
    void theDescriptionStatesTheFilesOwnNumbers() throws IOException {
        AbilityDefinition def = loadBundled("smolder.yml").find("smolder").orElseThrow();
        var p = (CastSpec.Pulses) def.cast();
        var d = (EffectSpec.Damage) def.onHit().get(0);
        String[] words = {"Zero", "One", "Two", "Three", "Four", "Five", "Six"};
        String first = words[p.count()] + " pulses, " + (p.intervalTicks() / 20) + "s apart, burst from your feet.";
        String second = "Each deals " + Math.round(d.amount()) + " fire damage within " + Math.round(p.radius())
                + " blocks and scorches.";
        assertEquals(List.of(first, second), def.description());
        assertEquals(0, p.intervalTicks() % 20, "the interval is a whole number of seconds, as the prose says");
    }

    /** The deleted placeholder is retired: a stale copy in a data folder is skipped, never registered. */
    @Test
    void aStaleCopyOfTheDeletedPlaceholderIsSkipped() throws IOException {
        Files.writeString(dir.resolve("ultimate_placeholder_mage.yml"),
                "id: ultimate_placeholder_mage\nelement: fire\ncast:\n  type: self\non_hit: []\n");
        AbilityRegistry registry = loadBundled("smolder.yml");
        assertTrue(registry.find("ultimate_placeholder_mage").isEmpty());
        assertTrue(registry.find("smolder").isPresent(), "CONTROL: the file beside it loads");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("ultimate_placeholder_mage.yml")), warnings.toString());
        assertFalse(new File("src/main/resources/content/abilities/ultimate_placeholder_mage.yml").exists(),
                "the bundled file is deleted");
    }

    /** A description authored as one string would read as NO lines (getStringList): refused by name. */
    @Test
    void aDescriptionThatIsOneStringIsRefusedByName() throws IOException {
        Files.writeString(dir.resolve("bad.yml"),
                "id: bad\nelement: fire\ndescription: \"one line\"\ncast:\n  type: self\non_hit: []\n");
        AbilityRegistry registry = loadBundled();
        assertTrue(registry.find("bad").isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("description: must be a YAML list")), warnings.toString());
    }
}
