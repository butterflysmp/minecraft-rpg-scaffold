package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class FilePlayerRepositoryTest {

    @TempDir
    Path dir;

    /** Runs inline, so every future is already complete. Deterministic tests. */
    private static final Executor DIRECT = Runnable::run;

    private FilePlayerRepository repo() {
        return new FilePlayerRepository(dir, DIRECT);
    }

    @Test
    void loadingAnUnknownPlayerYieldsEmpty() {
        assertTrue(repo().load(UUID.randomUUID()).join().isEmpty());
    }

    @Test
    void saveThenLoadRoundTrips() {
        var id = UUID.randomUUID();
        var profile = new PlayerProfile(1, id, "hunter", 7, 1234, List.of("solar_grenade"), 99L);
        var repo = repo();

        repo.save(profile).join();

        assertEquals(profile, repo.load(id).join().orElseThrow());
    }

    /**
     * The real migration path: JSON written by a build that predates
     * schemaVersion. Gson leaves the absent int at 0, and load() must stamp it.
     */
    @Test
    void legacyJsonWithoutSchemaVersionIsMigratedOnLoad() throws Exception {
        var id = UUID.randomUUID();
        Files.writeString(dir.resolve(id + ".json"), """
                {
                  "playerId": "%s",
                  "archetypeId": "hunter",
                  "level": 7,
                  "experience": 1234,
                  "unlockedAbilities": ["solar_grenade"],
                  "lastSeenEpochMillis": 99
                }
                """.formatted(id), StandardCharsets.UTF_8);

        PlayerProfile loaded = repo().load(id).join().orElseThrow();

        assertEquals(1, loaded.schemaVersion(), "v0 profile must be stamped as v1");
        assertEquals(id, loaded.playerId());
        assertEquals("hunter", loaded.archetypeId());
        assertEquals(7, loaded.level());
        assertEquals(List.of("solar_grenade"), loaded.unlockedAbilities());
    }

    /** Legacy JSON missing a whole field must not blow up the compact ctor. */
    @Test
    void legacyJsonWithoutUnlockedAbilitiesLoads() throws Exception {
        var id = UUID.randomUUID();
        Files.writeString(dir.resolve(id + ".json"),
                "{\"playerId\": \"" + id + "\", \"archetypeId\": \"none\", \"level\": 1}",
                StandardCharsets.UTF_8);

        PlayerProfile loaded = repo().load(id).join().orElseThrow();

        assertEquals(List.of(), loaded.unlockedAbilities());
        assertEquals(1, loaded.schemaVersion());
    }

    @Test
    void aProfileFromANewerServerFailsTheFutureRatherThanReturningJunk() throws Exception {
        var id = UUID.randomUUID();
        Files.writeString(dir.resolve(id + ".json"),
                "{\"schemaVersion\": 999, \"playerId\": \"" + id + "\"}", StandardCharsets.UTF_8);

        var ex = assertThrows(CompletionException.class, () -> repo().load(id).join());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void saveIsAtomicAndOverwrites() {
        var id = UUID.randomUUID();
        var repo = repo();

        repo.save(PlayerProfile.fresh(id)).join();
        repo.save(PlayerProfile.fresh(id).withLastSeen(4242L)).join();

        assertEquals(4242L, repo.load(id).join().orElseThrow().lastSeenEpochMillis());
        assertEquals(1, dir.toFile().listFiles((d, n) -> n.endsWith(".json")).length);
        assertEquals(0, dir.toFile().listFiles((d, n) -> n.endsWith(".tmp")).length,
                "the temp file must not survive");
    }

    /** Every task must run on the supplied executor, never the common pool. */
    @Test
    void allIoRunsOnTheSuppliedExecutor() {
        var used = new java.util.concurrent.atomic.AtomicInteger();
        Executor counting = task -> { used.incrementAndGet(); task.run(); };
        var repo = new FilePlayerRepository(dir, counting);
        var id = UUID.randomUUID();

        repo.save(PlayerProfile.fresh(id)).join();
        repo.load(id).join();

        assertEquals(2, used.get(), "save and load must each go through the executor");
    }
}
