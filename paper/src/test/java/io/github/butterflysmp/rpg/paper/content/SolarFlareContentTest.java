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
 * SOLAR FLARE AS SHIPPED (the seat's F3, F6, F11, F12, 2026-11-27, and N1 to N7 as Ben changed it on 2026-11-28): the
 * bundled file, loaded through the real loader, carries Ben's numbers; three shots one after another, 10 ticks apart;
 * each flight reaches exactly 32.0 blocks in 0.8 s; each burst is instant within 6 blocks and draws its own firework;
 * its prose states its own numbers; and the placeholder it replaced is gone.
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
        assertEquals(new CastSpec.Projectile(2.0, 0.0, 16, "solar_flare_trail", "fire_charge", null, null, null, 0,
                new CastSpec.Pulses(1, 1, 6.0, 0, null, "solar_flare_burst"), 2.5, new CastSpec.Projectile.Salvo(3, 10)), p,
                "N1 to N4: three shots 10 apart, 2.0 a tick for 16, an instant burst of 6, its own firework, no fan, no ring");
        assertEquals(List.of(new EffectSpec.Damage(250, "fire")), def.onHit(), "A1's rule: the damage only");
    }

    /**
     * N1, N2 (Ben: ".5 seconds", "0.8 seconds for the 32 blocks"): the shots are 10 ticks (0.5 s) apart, the flight is
     * straight and its sixteen steps reach exactly 32.0 blocks (16 x 2.0, exact in binary), in 16 ticks (0.8 s).
     */
    @Test
    void threeShotsHalfASecondApartEachFlyingExactlyThirtyTwoBlocksInPointEightSeconds() throws IOException {
        var p = (CastSpec.Projectile) flare().cast();
        assertEquals(3, p.salvo().count());
        assertEquals(0.5, p.salvo().intervalTicks() / 20.0, 1e-12);
        assertEquals(0.0, p.gravity());
        assertEquals(32.0, p.maxLifetimeTicks() * p.speed(), 0.0);
        assertEquals(0.8, p.maxLifetimeTicks() / 20.0, 1e-12);
        assertEquals(null, p.spread(), "no fan");
    }

    /**
     * N3, N4 (Route B): the burst is instant (travel 0) within 6 blocks, and its visual, parsed by the loader we run, is
     * an orange ball thrown out to the burst's own 6 blocks, a yellow one inside it, firework sparks and the firework's
     * blast and twinkle; nothing in it can spawn an entity (a visual step is a particle or a sound).
     */
    @Test
    void theBurstIsInstantAndDrawsAFireworkOfParticlesAndSounds() throws IOException {
        var p = (CastSpec.Projectile) flare().cast();
        assertEquals(0, p.burst().travelTicks());
        assertEquals(6.0, p.burst().radius());
        try (var in = getClass().getResourceAsStream("/content/visuals/solar_flare_burst.yml")) {
            assertNotNull(in);
            Files.createDirectories(dir.resolve("v"));
            Files.write(dir.resolve("v/solar_flare_burst.yml"), in.readAllBytes());
        }
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        var steps = new VisualLoader(log).loadAll(dir.resolve("v").toFile()).find("solar_flare_burst").orElseThrow().steps();
        var balls = steps.stream().filter(VisualSpec.Particles.class::isInstance).map(VisualSpec.Particles.class::cast)
                .filter(s -> s.trail() != null).toList();
        assertEquals(List.of(6.0, 4.5), balls.stream().map(b -> b.trail().burstRadius()).toList());
        assertEquals(List.of(org.bukkit.Color.fromRGB(255, 120, 0), org.bukkit.Color.fromRGB(255, 220, 40)),
                balls.stream().map(b -> b.trail().color()).toList(), "orange, then yellow");
        assertEquals(p.burst().radius(), balls.get(0).trail().burstRadius(), "the orange ball shows the burst's reach");
        assertTrue(steps.stream().anyMatch(s -> s instanceof VisualSpec.Particles q && q.particle() == org.bukkit.Particle.FIREWORK));
        assertEquals(List.of("entity.firework_rocket.large_blast", "entity.firework_rocket.twinkle"),
                steps.stream().filter(VisualSpec.Sound.class::isInstance).map(s -> ((VisualSpec.Sound) s).key()).toList());
        assertTrue(steps.stream().allMatch(s -> s instanceof VisualSpec.Particles || s instanceof VisualSpec.Sound),
                "particles and sounds only");
    }

    /**
     * THE SEAT'S Q1 (2026-11-28): A BURST 32 BLOCKS AWAY IS SENT, DRAWN AND HEARD. Every particle step of the burst and of
     * the trail is {@code long_distance} (READ: without force the server sends within 32.0 of his block centre and the
     * client drops beyond 32; the range burst's centre is about 32.02 from his block centre). Every sound of the burst has
     * a volume whose reach, 16 x volume (READ: the server's {@code SoundEvent.getRange} and the client's {@code max(volume,
     * 1) x attenuation 16}, sounds.json setting none for these), covers the farthest he can be from a burst, 32 + 1.62
     * of height, with margin: at least 3.0 (48 blocks).
     */
    @Test
    void theBurstAndTheTrailAreSentDrawnAndHeardFromThirtyTwoBlocks() throws IOException {
        Files.createDirectories(dir.resolve("q1"));
        for (String id : List.of("solar_flare_burst", "solar_flare_trail")) {
            try (var in = getClass().getResourceAsStream("/content/visuals/" + id + ".yml")) {
                assertNotNull(in);
                Files.write(dir.resolve("q1/" + id + ".yml"), in.readAllBytes());
            }
        }
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        var visuals = new VisualLoader(log).loadAll(dir.resolve("q1").toFile());
        for (String id : List.of("solar_flare_burst", "solar_flare_trail")) {
            var steps = visuals.find(id).orElseThrow(() -> new AssertionError(id)).steps();
            var particles = steps.stream().filter(VisualSpec.Particles.class::isInstance).map(VisualSpec.Particles.class::cast).toList();
            assertFalse(particles.isEmpty(), id);
            assertTrue(particles.stream().allMatch(VisualSpec.Particles::longDistance), id + ": every particle step long-distance");
        }
        var sounds = visuals.find("solar_flare_burst").orElseThrow().steps().stream().filter(VisualSpec.Sound.class::isInstance)
                .map(VisualSpec.Sound.class::cast).toList();
        assertEquals(2, sounds.size());
        for (var s : sounds) assertTrue(16 * s.volume() >= 48, s.key() + " reaches " + 16 * s.volume());
    }

    /** F12: the menu's prose rebuilt from the parsed definition: the count, the burst's radius, the damage. */
    @Test
    void theDescriptionStatesTheFilesOwnNumbers() throws IOException {
        AbilityDefinition def = flare();
        var p = (CastSpec.Projectile) def.cast();
        var d = (EffectSpec.Damage) def.onHit().get(0);
        String[] words = {"zero", "one", "two", "three", "four"};
        assertEquals(List.of("Fire " + words[p.salvo().count()] + " great fireballs, one after another.",
                "Each bursts within " + Math.round(p.burst().radius()) + " blocks for " + Math.round(d.amount())
                        + " fire damage and scorches."), def.description());
    }

    /**
     * N5 (the seat, 2026-11-28; it REPLACES F10's shared-files test): EACH ULTIMATE HAS ITS OWN VISUAL FILES. Every visual
     * id Solar Flare names (its cast, trail, ring, burst) is one Smolder does not name, and the reverse; and no shipped
     * visual still says it is shared ("names this file too", F10's note). Forces red: either YAML naming the other's file
     * again.
     */
    @Test
    void eachUltimateNamesOnlyItsOwnVisualFiles() throws IOException {
        var flare = (CastSpec.Projectile) flare().cast();
        AbilityDefinition smolder = loadBundled("smolder.yml").find("smolder").orElseThrow();
        var pulses = (CastSpec.Pulses) smolder.cast();
        java.util.Set<String> flareIds = new java.util.HashSet<>(java.util.Arrays.asList(
                flare.trail(), flare.burst().ring(), flare.burst().pulse(), "solar_flare_cast"));
        flareIds.remove(null);
        java.util.Set<String> smolderIds = new java.util.HashSet<>(java.util.Arrays.asList(
                pulses.ring(), pulses.pulse(), pulses.spiral() == null ? null : pulses.spiral().visual()));
        smolderIds.remove(null);
        assertFalse(smolderIds.isEmpty() || flareIds.size() < 3, "CONTROL: both name visuals " + flareIds + smolderIds);
        for (String id : flareIds) assertFalse(smolderIds.contains(id), id + " is named by both");
        try (var files = Files.list(Path.of("src/main/resources/content/visuals"))) {
            for (Path p : files.toList()) {
                assertFalse(Files.readString(p, StandardCharsets.UTF_8).contains("names this file too"), p.toString());
            }
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
