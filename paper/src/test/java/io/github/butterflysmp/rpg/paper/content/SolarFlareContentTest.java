package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.PulseLine;
import io.github.butterflysmp.rpg.core.combat.SpreadPattern;
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
 * SOLAR FLARE AS SHIPPED (the seat's F1, F3, F4, F6, F10 to F12, 2026-11-27): the bundled file, loaded through the real
 * loader, carries Ben's numbers; its fan is exactly -30, 0, +30; its flight reaches exactly 15.0 blocks; its burst is
 * Smolder's own ring; its prose states its own numbers; and the placeholder it replaced is gone.
 */
class SolarFlareContentTest {

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

    private AbilityDefinition flare() throws IOException {
        AbilityDefinition def = loadBundled("solar_flare.yml").find("solar_flare").orElseThrow();
        assertTrue(warnings.isEmpty(), warnings.toString());
        return def;
    }

    @Test
    void solarFlareLoadsWithBensNumbers() throws IOException {
        AbilityDefinition def = flare();
        assertEquals("fire", def.element());
        assertEquals(4200, def.cooldownTicks(), "3.5 minutes");
        assertEquals("mana", def.cost().resourceId());
        assertEquals(80, def.cost().amount());
        var p = assertInstanceOf(CastSpec.Projectile.class, def.cast());
        assertEquals(new CastSpec.Projectile(0.9375, 0.0, 16, "solar_flare_trail", "fire_charge", null, null,
                new CastSpec.Spread(3, 30), 0, new CastSpec.Pulses(1, 1, 6.0, 15, "smolder_ring", "smolder_pulse"), 2.5), p);
        assertEquals(List.of(new EffectSpec.Damage(250, "fire")), def.onHit(), "A1's rule: the damage only");
    }

    /** F1: the flight is straight and its sixteen steps reach exactly 15.0 blocks; Ben's 0.8 s is those 16 ticks. */
    @Test
    void theFlightReachesExactlyFifteenBlocksInSixteenTicks() throws IOException {
        var p = (CastSpec.Projectile) flare().cast();
        assertEquals(0.0, p.gravity());
        assertEquals(15.0, p.maxLifetimeTicks() * p.speed(), 0.0, "exact in binary: 16 x 0.9375");
        assertEquals(0.8, p.maxLifetimeTicks() / 20.0, 1e-12);
    }

    /** F1: the shipped spread, through the engine's own pattern, is the aim and the aim turned 30 degrees each way. */
    @Test
    void theFanIsMinusThirtyZeroPlusThirty() throws IOException {
        var p = (CastSpec.Projectile) flare().cast();
        Vec3 east = new Vec3(1, 0, 0);
        Vec3 right = new Vec3(0, 0, 1);
        List<Double> yaws = new ArrayList<>();
        for (Vec3 d : SpreadPattern.directionsFor(east, right, p.spread())) yaws.add(Math.round(PulseLine.yawOf(d) * 1e9) / 1e9);
        yaws.sort(Double::compare);
        assertEquals(List.of(-120.0, -90.0, -60.0), yaws, "east is -90: -30, 0, +30 about the aim");
        // And at a pitched aim the fan stays in his VIEW plane, each 30 degrees off the aim.
        Vec3 up = new Vec3(1, 1, 0).normalize();
        for (Vec3 d : SpreadPattern.directionsFor(up, right, p.spread())) {
            double angle = Math.toDegrees(Math.acos(Math.min(1, d.normalize().dot(up))));
            assertTrue(Math.abs(angle) < 1e-9 || Math.abs(angle - 30) < 1e-9, "30 off the aim, or on it: " + angle);
        }
    }

    /** F12: the menu's prose rebuilt from the parsed definition: the count, the burst's radius, the damage. */
    @Test
    void theDescriptionStatesTheFilesOwnNumbers() throws IOException {
        AbilityDefinition def = flare();
        var p = (CastSpec.Projectile) def.cast();
        var d = (EffectSpec.Damage) def.onHit().get(0);
        String[] words = {"zero", "one", "two", "three", "four"};
        assertEquals(List.of("Fire " + words[p.spread().count()] + " great fireballs in a fan.",
                "Each bursts within " + Math.round(p.burst().radius()) + " blocks for " + Math.round(d.amount())
                        + " fire damage and scorches."), def.description());
    }

    /** F10: the ring and the burst are SMOLDER's files, by name, and both files say they are shared. */
    @Test
    void theRingAndTheBurstAreSmoldersOwnFiles() throws IOException {
        var burst = ((CastSpec.Projectile) flare().cast()).burst();
        AbilityDefinition smolder = loadBundled("smolder.yml").find("smolder").orElseThrow();
        var pulses = (CastSpec.Pulses) smolder.cast();
        assertEquals(pulses.ring(), burst.ring());
        assertEquals(pulses.pulse(), burst.pulse());
        assertEquals(pulses.radius() / pulses.travelTicks(), burst.radius() / burst.travelTicks(), 0.0,
                "F4: Smolder's speed, 12 in 30 = 6 in 15");
        for (String v : List.of("smolder_ring", "smolder_pulse")) {
            String text = Files.readString(Path.of("src/main/resources/content/visuals/" + v + ".yml"), StandardCharsets.UTF_8);
            assertTrue(text.contains("Solar Flare's burst names this file too"), v);
        }
    }

    /** F11: the deleted placeholder is retired: a stale copy in a data folder is skipped, never registered. */
    @Test
    void aStaleCopyOfTheDeletedPlaceholderIsSkipped() throws IOException {
        Files.writeString(dir.resolve("ultimate_placeholder_ranger.yml"),
                "id: ultimate_placeholder_ranger\nelement: fire\ncast:\n  type: self\non_hit: []\n");
        AbilityRegistry registry = loadBundled("solar_flare.yml");
        assertTrue(registry.find("ultimate_placeholder_ranger").isEmpty());
        assertTrue(registry.find("solar_flare").isPresent(), "CONTROL: the file beside it loads");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("ultimate_placeholder_ranger.yml")), warnings.toString());
        assertFalse(new File("src/main/resources/content/abilities/ultimate_placeholder_ranger.yml").exists(),
                "the bundled file is deleted");
    }

    /** The schema's half of F3: a burst is ONE front, so a count or an interval in it is refused by name. */
    @Test
    void aBurstThatAuthorsACountIsRefusedByName() throws IOException {
        Files.writeString(dir.resolve("bad.yml"), "id: bad\nelement: fire\ncast:\n  type: projectile\n  speed: 1.0\n"
                + "  gravity: 0.0\n  max_lifetime_ticks: 10\n  burst:\n    count: 2\n    radius: 6.0\n    travel_ticks: 15\n"
                + "on_hit: []\n");
        AbilityRegistry registry = loadBundled();
        assertTrue(registry.find("bad").isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("a projectile's burst is ONE pulse front: remove 'count'")),
                warnings.toString());
    }
}
