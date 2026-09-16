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
        var profile = new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION, id, "hunter", "fire",
                7, 1234, List.of("solar_grenade"), 99L, 3);
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

        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, loaded.schemaVersion(),
                "a v0 profile must be migrated all the way to the current version");
        assertEquals(id, loaded.playerId());
        assertEquals("hunter", loaded.archetypeId());
        assertEquals(PlayerProfile.NONE, loaded.elementId(), "no elementId in old JSON -> NONE");
        assertEquals(7, loaded.level());
        assertEquals(List.of("solar_grenade"), loaded.unlockedAbilities());
        assertEquals(PlayerProfile.DEFAULT_NEXUS_SLOT, loaded.nexusSlot(),
                "no nexusSlot in old JSON -> the default, NOT the 0 Gson left behind");
    }

    /**
     * The v2 -> v3 migration through REAL Gson, which is the only instrument that proves the
     * absent-int hazard is handled.
     *
     * <p>{@code PlayerProfileMigrationTest} stages the zero by hand. This writes JSON that genuinely
     * has no {@code nexusSlot} key and lets Gson produce the zero itself, so the test cannot pass
     * because the fixture happened to be built the way the code expects.
     *
     * <p><b>The stamp is 2, not absent</b> -- this is a profile from the build immediately before
     * the field existed, which is the file almost every real player currently has on disk.
     */
    @Test
    void v2JsonWithNoNexusSlotKeyLoadsAtTheDefaultRatherThanSlotZero() throws Exception {
        var id = UUID.randomUUID();
        Files.writeString(dir.resolve(id + ".json"), """
                {
                  "schemaVersion": 2,
                  "playerId": "%s",
                  "archetypeId": "ranger",
                  "elementId": "fire",
                  "level": 7,
                  "experience": 1234,
                  "unlockedAbilities": ["solar_grenade"],
                  "lastSeenEpochMillis": 99
                }
                """.formatted(id), StandardCharsets.UTF_8);

        PlayerProfile loaded = repo().load(id).join().orElseThrow();

        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, loaded.schemaVersion());
        assertEquals(PlayerProfile.DEFAULT_NEXUS_SLOT, loaded.nexusSlot(),
                "an absent nexusSlot key must become the default, not the 0 Gson leaves");
        assertNotEquals(0, loaded.nexusSlot(),
                "0 is the leftmost hotbar cell -- shipping every existing player there is the "
                        + "defect this migration step exists to prevent");
        // The rest of a v2 profile is untouched by the step.
        assertEquals("ranger", loaded.archetypeId());
        assertEquals("fire", loaded.elementId());
        assertEquals(1234, loaded.experience());
    }

    /** And a v3 file's chosen slot round-trips through Gson, including the zero. */
    @Test
    void aV3JsonsChosenNexusSlotIsReadBackAsWritten() throws Exception {
        var id = UUID.randomUUID();
        Files.writeString(dir.resolve(id + ".json"), """
                {
                  "schemaVersion": 3,
                  "playerId": "%s",
                  "archetypeId": "ranger",
                  "elementId": "fire",
                  "level": 7,
                  "experience": 1234,
                  "unlockedAbilities": [],
                  "lastSeenEpochMillis": 99,
                  "nexusSlot": 0
                }
                """.formatted(id), StandardCharsets.UTF_8);

        assertEquals(0, repo().load(id).join().orElseThrow().nexusSlot(),
                "at v3 a zero is a CHOICE and must survive -- the stamp is what tells them apart");
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
        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, loaded.schemaVersion());
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
