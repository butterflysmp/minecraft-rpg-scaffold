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
        assertEquals(new CastSpec.Pulses(4, 30, 12.0, 15, null, "smolder_pulse",
                new CastSpec.Pulses.SpiralArms(9, 1.0, "smolder_spiral")), pulses,
                "M1, M2: 1.5 s apart, 0.75 s to the edge, arms of one turn, no ring; V1 (Ben, 2026-11-28: \"Can we try 9"
                        + " spirals\"): NINE arms, and only that number moved");
        assertEquals(List.of(new EffectSpec.Damage(160, "fire")), def.onHit());
    }

    /**
     * V1 (the seat, 2026-11-28): THE POINT COUNT AT NINE ARMS, EXECUTED, from the shipped file through the very calls
     * {@code CastExecutor.ringPass} makes each tick t of the travel ({@code PulseFront.spiralPoints} over
     * {@code (radiusAt(t - 1), radiusAt(t)]}). The points per arm per tick depend on the spacing and the arm's slope, not
     * on the number of arms, so every tick after the centre point draws exactly three times what three arms drew, and the
     * three-arm figures are the last plan's own (PLAN-smolder.md section 8, M2: 343 a pulse), recomputed here as the
     * control. The spiral's particle lives {@code duration_ticks} (8, READ: TRAIL's lifetime is the server's), so the
     * most alive at once is the largest sum over 8 consecutive ticks.
     */
    @Test
    void nineArmsDrawThreeTimesTheThreeArmPointsEveryTick() throws IOException {
        AbilityDefinition def = loadBundled("smolder.yml").find("smolder").orElseThrow();
        var p = (CastSpec.Pulses) def.cast();
        var origin = io.github.butterflysmp.rpg.core.Vec3.ZERO;
        List<Integer> nine = new ArrayList<>(), three = new ArrayList<>();
        for (int t = 0; t <= p.travelTicks(); t++) {
            double from = io.github.butterflysmp.rpg.core.combat.PulseFront.radiusAt(t - 1, p.radius(), p.travelTicks());
            double to = io.github.butterflysmp.rpg.core.combat.PulseFront.radiusAt(t, p.radius(), p.travelTicks());
            nine.add(io.github.butterflysmp.rpg.core.combat.PulseFront.spiralPoints(origin, from, to, p.spiral().arms(),
                    p.spiral().turns(), p.radius()).size());
            three.add(io.github.butterflysmp.rpg.core.combat.PulseFront.spiralPoints(origin, from, to, 3,
                    p.spiral().turns(), p.radius()).size());
        }
        assertEquals(List.of(1, 9, 9, 12, 12, 15, 18, 21, 21, 24, 27, 30, 33, 36, 36, 39), three,
                "CONTROL: the three-arm figures PLAN-smolder.md section 8 executed");
        assertEquals(343, three.stream().mapToInt(Integer::intValue).sum());
        assertEquals(List.of(1, 27, 27, 36, 36, 45, 54, 63, 63, 72, 81, 90, 99, 108, 108, 117), nine);
        assertEquals(1027, nine.stream().mapToInt(Integer::intValue).sum(), "points a pulse at nine arms");
        assertEquals(4 * 1027, p.count() * nine.stream().mapToInt(Integer::intValue).sum(), "points a cast (four pulses)");
        for (int t = 1; t < nine.size(); t++) assertEquals(3 * three.get(t), nine.get(t), "tick " + t);
        int alive = 0;
        for (int t = 0; t < nine.size(); t++) {
            int sum = 0;
            for (int u = Math.max(0, t - 7); u <= t; u++) sum += nine.get(u);
            alive = Math.max(alive, sum);
        }
        assertEquals(738, alive, "the most spiral particles alive at once, each living 8 ticks");
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
        String seconds = java.math.BigDecimal.valueOf(p.intervalTicks()).divide(java.math.BigDecimal.valueOf(20))
                .stripTrailingZeros().toPlainString();
        String first = words[p.count()] + " pulses, " + seconds + "s apart, burst from your feet.";
        String second = "Each deals " + Math.round(d.amount()) + " fire damage within " + Math.round(p.radius())
                + " blocks and scorches.";
        assertEquals(List.of(first, second), def.description());
        assertEquals("1.5", seconds, "N7: the seat's wording, 1.5s");
    }

    /**
     * M3 ("I also want the particles to disappear faster"): every PARTICLE step of the spiral's visual and the pulse's
     * is TRAIL, whose lifetime the server sets (READ: the client's {@code TrailParticle$Provider} hands the duration to
     * {@code setLifetime}), and each lives at most 10 ticks: gone within half a second of being drawn. The old FLAME
     * lived 12 to 44 ticks (READ: {@code RisingParticle}'s {@code (int)(8 / (r x 0.8 + 0.2)) + 4}). Parsed by the loader
     * we run, from the bundled files.
     */
    @Test
    void theSpiralAndThePulseDrawOnlyServerTimedParticlesThatDieWithinHalfASecond() throws IOException {
        for (String id : List.of("smolder_spiral", "smolder_pulse")) {
            try (var in = getClass().getResourceAsStream("/content/visuals/" + id + ".yml")) {
                assertNotNull(in, "bundled visual missing: " + id);
                Files.write(dir.resolve(id + ".yml"), in.readAllBytes());
            }
        }
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        VisualRegistry visuals = new VisualLoader(log).loadAll(dir.toFile());
        for (String id : List.of("smolder_spiral", "smolder_pulse")) {
            var steps = visuals.find(id).orElseThrow(() -> new AssertionError(id + " did not load")).steps();
            var particles = steps.stream().filter(VisualSpec.Particles.class::isInstance)
                    .map(VisualSpec.Particles.class::cast).toList();
            assertFalse(particles.isEmpty(), id + " draws something");
            for (var p : particles) {
                assertEquals(org.bukkit.Particle.TRAIL, p.particle(), id);
                assertTrue(p.trail().durationTicks() <= 10, id + " lives " + p.trail().durationTicks() + " ticks");
                assertTrue(p.longDistance(), id + ": long-distance (the seat's Q1: sent and drawn past 32)");
            }
        }
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
