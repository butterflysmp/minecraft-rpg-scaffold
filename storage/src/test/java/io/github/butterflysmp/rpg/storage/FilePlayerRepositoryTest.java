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
                7, 1234, List.of("solar_grenade"), 99L, 3, 56_780L);
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

    /**
     * *** THE ABSENT-{@code long} ZERO, CAUSED RATHER THAN ASSERTED. ***
     *
     * <p>Every profile on disk today is a v3 file with no {@code lifetimeXp} key, and this is that
     * file. <b>Gson leaves the absent long at 0, and 0 is the answer we want</b> -- a player with no
     * record has earned no XP -- so unlike {@code nexusSlot} there is no migration step, no stamp
     * bump and no sentinel. {@code PlayerProfile.lifetimeXp()} carries the argument.
     *
     * <p><b>The stamp assertion is the one that makes the ruling falsifiable.</b> "No migration"
     * is otherwise an absence, and an absence in {@code ProfileMigrations} is indistinguishable
     * from a step someone forgot to write. If a later slice bumps the schema for this field, THIS
     * ROW REDDENS and points at the note that says why it was not bumped.
     *
     * <p>The surviving v3 keys are asserted too, so the row cannot pass on a fixture that failed to
     * parse at all and handed back a blank profile -- which would satisfy the zero for free.
     */
    @Test
    void aV3JsonWithNoLifetimeXpKeyLoadsAtZERO_withNoMigrationAndNoStampBump() throws Exception {
        var id = UUID.randomUUID();
        Files.writeString(dir.resolve(id + ".json"), """
                {
                  "schemaVersion": 3,
                  "playerId": "%s",
                  "archetypeId": "ranger",
                  "elementId": "fire",
                  "level": 7,
                  "experience": 1234,
                  "unlockedAbilities": ["solar_grenade"],
                  "lastSeenEpochMillis": 99,
                  "nexusSlot": 17
                }
                """.formatted(id), StandardCharsets.UTF_8);

        PlayerProfile loaded = repo().load(id).join().orElseThrow();

        assertEquals(0L, loaded.lifetimeXp(),
                "an absent lifetimeXp is zero, and zero is CORRECT -- level 1 is where a player "
                        + "with no record belongs");
        assertEquals(3, loaded.schemaVersion(),
                "AND THE STAMP DOES NOT MOVE. Adding this field needed no migration, so it needed "
                        + "no version; if you are here because this went red, read the note at the "
                        + "end of ProfileMigrations.migrate before raising it");

        // THE FIXTURE REALLY IS A POPULATED v3 FILE -- without these, a parse failure yielding a
        // blank profile would satisfy the zero above and read as a pass.
        assertEquals(17, loaded.nexusSlot(), "the v3 field that DID need a migration is untouched");
        assertEquals("ranger", loaded.archetypeId());
        assertEquals(99L, loaded.lastSeenEpochMillis());

        // AND 1234 IS THE DEAD `experience` FIELD, WHICH IS NOT THIS ONE. Two longs, adjacent in
        // the record, and only one of them carries progression. Staged unequal on purpose so a
        // transposition between them cannot pass.
        assertEquals(1234L, loaded.experience(), "the dead field keeps its own value");
        assertNotEquals(loaded.experience(), loaded.lifetimeXp(),
                "if these two ever read the same, the row can no longer see a swap");
    }

    /** And a written lifetimeXp round-trips -- the absent case above is not the only one. */
    @Test
    void aWrittenLifetimeXpSurvivesSaveAndLoad() {
        var id = UUID.randomUUID();
        var repo = repo();

        repo.save(PlayerProfile.fresh(id).withLifetimeXp(12_940L)).join();

        assertEquals(12_940L, repo.load(id).join().orElseThrow().lifetimeXp(),
                "12,940 is the level-10 total -- a real curve value, not a round number that "
                        + "would also match a field left at its default");
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
