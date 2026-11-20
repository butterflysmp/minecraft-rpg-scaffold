package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code content/world_damage.yml}: the game-wide damage rules (PLAN-world-damage.md). Its one value today is Ben's void
 * share (2026-11-19, 0.10). The loader logs one {@code Loaded world damage} line, or a {@code Refusing} line naming why,
 * and a refused or absent value leaves the void as the server's own (VOID is PASS).
 */
class WorldDamageLoaderTest {

    @TempDir
    Path dir;

    private final List<String> lines = new ArrayList<>();
    private Logger log;

    @BeforeEach
    void setUp() {
        log = Logger.getLogger("WorldDamageLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { lines.add(record.getLevel() + " " + record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private OptionalDouble load(String yaml) throws IOException {
        Path file = dir.resolve("world_damage.yml");
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
        return new WorldDamageLoader(log).load(file.toFile()).voidShare();
    }

    private String text() { return String.join("\n", lines); }

    /** The shipped file, read from the source tree: Ben's 0.10, and the exact Loaded line R0c predicts. */
    @Test
    void theShippedFileCarriesBensShare() {
        var rules = new WorldDamageLoader(log).load(Path.of("src/main/resources/content/world_damage.yml").toFile());
        assertEquals(0.10, rules.voidShare().orElseThrow(), 0.0);
        assertEquals(List.of("INFO Loaded world damage: void takes 0.10 of a player's maximum health per void tick,"
                + " defence bypassed; mobs unchanged"), lines);
    }

    /** The value arrives, in both directions: a round trip of two different shares. */
    @Test
    void theValueArrives() throws IOException {
        assertEquals(0.25, load("void:\n  share_of_max_per_tick: 0.25\n").orElseThrow(), 0.0);
        assertEquals(1.0, load("void:\n  share_of_max_per_tick: 1.0\n").orElseThrow(), 0.0);
    }

    @Test
    void anAbsentFileIsRefusedByName() {
        var rules = new WorldDamageLoader(log).load(dir.resolve("nope.yml").toFile());
        assertTrue(rules.voidShare().isEmpty());
        assertTrue(text().contains("Refusing the void rule: content/world_damage.yml is absent"), text());
    }

    @Test
    void aMissingKeyIsRefusedByName() throws IOException {
        assertTrue(load("void:\n  share: 0.1\n").isEmpty());
        assertTrue(text().contains("Refusing the void rule: void.share_of_max_per_tick is missing"), text());
    }

    @Test
    void aShareOutsideZeroToOneIsRefusedByName() throws IOException {
        assertTrue(load("void:\n  share_of_max_per_tick: 0\n").isEmpty());
        assertTrue(load("void:\n  share_of_max_per_tick: 1.5\n").isEmpty());
        assertTrue(load("void:\n  share_of_max_per_tick: ten\n").isEmpty());
        assertEquals(3, lines.stream().filter(l -> l.contains("Refusing the void rule: void.share_of_max_per_tick")).count(),
                text());
    }
}
