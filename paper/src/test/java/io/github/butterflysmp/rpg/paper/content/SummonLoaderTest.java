package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.summon.SummonDefinition;
import io.github.butterflysmp.rpg.core.summon.SummonRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

/** D2 (the seat's 3a, 2026-11-24): the schema, the TEST-ONLY fixture, and each refusal, caused on purpose. */
class SummonLoaderTest {

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();

    private SummonLoader loader() {
        Logger log = Logger.getLogger("SummonLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { warnings.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return new SummonLoader(log, Set.of("wolf", "zombie")::contains);
    }

    private static final Path FIXTURE = Path.of("src", "test", "resources", "fixtures", "summons", "test_wolf.yml");

    @Test
    void theTestOnlyFixtureLoadsEveryNumber() throws IOException {
        Files.copy(FIXTURE, dir.resolve("test_wolf.yml"));
        SummonRegistry r = loader().loadAll(dir.toFile());
        assertEquals(List.of(), warnings);
        SummonDefinition w = r.find("test_wolf").orElseThrow();
        assertEquals(new SummonDefinition("test_wolf", "wolf", "{owner}'s Wolf", 100, 20, 8, 30, 32, 48, 20, 100), w);
    }

    @Test
    void exactlyTheBoneWhistlesWolfShips() {
        // 3a said "no summon ships"; PR 5 (2026-11-25) ships one. Recount: ls content/summons/*.yml | wc -l = 1.
        SummonRegistry shipped = loader().loadAll(Path.of("src", "main", "resources", "content", "summons").toFile());
        assertEquals(List.of(), warnings, "the shipped file loads clean");
        assertEquals(1, shipped.size());
        assertEquals(new SummonDefinition("bone_whistle_wolf", "wolf", "{owner}'s Wolf", 100, 20, 8, 30, 32, 48, 20, 100),
                shipped.find("bone_whistle_wolf").orElseThrow(), "Ben's 100 and 20, 8 and 30; the seat's placeholders");
        assertEquals(0, loader().loadAll(dir.resolve("absent").toFile()).size(), "a missing directory is no summons");
    }

    private void refuses(String yaml, String why) throws IOException {
        warnings.clear();
        Files.writeString(dir.resolve("bad.yml"), yaml);
        SummonRegistry r = loader().loadAll(dir.toFile());
        assertEquals(0, r.size(), why);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("bad.yml") && w.contains(why)), why + ": " + warnings);
    }

    private static String fixtureWith(String key, String value) throws IOException {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(FIXTURE)) {
            if (line.startsWith(key + ":")) {
                if (value != null) out.append(key).append(": ").append(value).append('\n');
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    @Test
    void eachRefusalIsCausedAndNamed() throws IOException {
        refuses(fixtureWith("entity", "dragonish"), "unknown entity 'dragonish'");
        refuses(fixtureWith("entity", "wolf") + "element: nature\n", "declares an element");   // E1
        refuses(fixtureWith("health", "0"), "health must be more than 0");
        refuses(fixtureWith("damage", "-5"), "damage must be more than 0");
        for (String key : SummonLoader.REQUIRED) {
            refuses(fixtureWith(key, null), "'" + key + "' is missing");
        }
        refuses(fixtureWith("attack_cadence_ticks", "20.5"), "must be a whole number of ticks");
        refuses(fixtureWith("chase_limit", "16"), "is below its trigger_radius");
        refuses(fixtureWith("teleport_back", "8"), "must be beyond its guard_radius");
    }
}
