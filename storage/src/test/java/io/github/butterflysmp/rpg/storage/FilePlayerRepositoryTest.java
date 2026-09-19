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
                7, 1234, List.of("solar_grenade"), 99L, 3, 56_780L, null, false);
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
        // *** THIS WENT RED WHEN vaultMigrated BUMPED THE STAMP TO 4, AND THE NOTE IT TOLD YOU TO
        // READ IS WHY THE FIX IS A CHANGED NUMBER RATHER THAN A RAISED OBJECTION. ***
        //
        // The row's claim was "adding lifetimeXp needed no migration, so it needed no version", and
        // THAT IS STILL TRUE: no step touches lifetimeXp, and an absent key still reads as zero,
        // asserted two lines up. What moved is a different field's stamp, taken for the rollback
        // refusal rather than for a fix-up.
        //
        // The row's name keeps "NoStampBump" and is left alone, because it names what is true of
        // THIS FIELD -- which is the thing the row exists to pin. Renaming it to follow the schema
        // number would make it a row about the schema, and there is one of those in
        // PlayerProfileMigrationTest.
        assertEquals(4, loaded.schemaVersion(),
                "restamped to 4 by the vaultMigrated step. lifetimeXp itself still needs no "
                        + "migration -- see the note at the end of ProfileMigrations.migrate, and "
                        + "the v3 -> v4 step directly beneath it");

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

    /**
     * *** THE TOGGLE'S ABSENT KEY MEANS ENABLED, CAUSED AGAINST A REAL FILE. ***
     *
     * <p><b>This is the row that says the deploy is safe.</b> Every profile on disk today lacks
     * {@code starEnabled}, so this fixture is not a hypothetical -- it is what the server reads on
     * the first join after the merge, for every player.
     *
     * <p><b>A primitive {@code boolean} would have read FALSE here and switched off the star of
     * every player who has one.</b> The boxed type is what makes absence representable, and this
     * row is where that claim is executed rather than argued.
     */
    @Test
    void aProfileWithNoStarEnabledKeyIsENABLED_whichEveryExistingFileIs() throws Exception {
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
                  "nexusSlot": 17,
                  "lifetimeXp": 56780
                }
                """.formatted(id), StandardCharsets.UTF_8);

        PlayerProfile loaded = repo().load(id).join().orElseThrow();

        assertTrue(loaded.starEnabled(), "an absent starEnabled key means ENABLED");
        assertNull(loaded.starEnabledOrNull(),
                "and it arrives as NULL rather than false -- which is the whole reason the "
                        + "component is boxed. A primitive would read false and disable everyone");
        // *** THIS ASSERTION READ `3` UNTIL THE VAULT MIGRATION STAMP LANDED, AND THE CHANGE IS
        // NOT A WEAKENING. *** The original claim -- "no stamp bump: absence needs no migration" --
        // was about starEnabled and is still true of starEnabled: no step touches it, and the
        // absent key still arrives as null.
        //
        // What moved is a DIFFERENT field's stamp. vaultMigrated bumped the schema to 4 purely to
        // arm the newer-server refusal, so every v3 file is now restamped on load. The row goes red
        // on that bump, correctly -- it is the only row in the suite that loads a REAL v3 file --
        // and the honest fix is to say what the stamp is now and why, not to loosen it to
        // CURRENT_SCHEMA_VERSION, which would stop noticing the next one.
        assertEquals(4, loaded.schemaVersion(),
                "restamped to 4 by the vaultMigrated step, which sets no value and exists for the "
                        + "rollback refusal. starEnabled itself still needs no migration");

        // THE FIXTURE IS A POPULATED v3 FILE, not a parse failure yielding a blank profile --
        // which would satisfy the assertions above for free.
        assertEquals(17, loaded.nexusSlot());
        assertEquals(56_780L, loaded.lifetimeXp());
    }

    /**
     * *** THE MIGRATION FLAG'S ABSENT KEY MEANS NOT MIGRATED, CAUSED AGAINST A REAL FILE. ***
     *
     * <p><b>The other direction from the row above, and the contrast is the reason both exist.</b>
     * An absent {@code starEnabled} must read as {@code true}, so it is boxed; an absent
     * {@code vaultMigrated} must read as {@code false}, so it is a primitive. <b>The same absence,
     * in the same file, correctly answered two opposite ways</b> -- which is only checkable by
     * loading a file that has neither key.
     *
     * <p>If this ever read {@code true}, every existing player would be treated as already
     * migrated and <b>their ender chest would never be copied at all</b> -- a silent, permanent
     * loss of access to items that are still sitting in a container they can no longer open.
     */
    @Test
    void aProfileWithNoVaultMigratedKeyIsNOTMigrated_whichEveryExistingFileIs() throws Exception {
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
                  "nexusSlot": 17,
                  "lifetimeXp": 56780
                }
                """.formatted(id), StandardCharsets.UTF_8);

        PlayerProfile loaded = repo().load(id).join().orElseThrow();

        assertFalse(loaded.vaultMigrated(), "an absent vaultMigrated key means NOT migrated");
        assertTrue(loaded.starEnabled(),
                "and the SAME absence still means ENABLED for the boxed field beside it");

        // The fixture is a populated file rather than a parse failure yielding a blank profile,
        // which would satisfy a false-reading assertion for free.
        assertEquals(17, loaded.nexusSlot());
        assertEquals(56_780L, loaded.lifetimeXp());
    }

    /**
     * A migrated profile round-trips, and the key is WRITTEN rather than omitted.
     *
     * <p><b>The second half is the one that matters and it is the opposite of {@code starEnabled}'s
     * property.</b> {@code serializeNulls} is off, so a null field writes no key -- but this field
     * is a primitive, so {@code false} is a value and Gson writes it. That is what makes the flag
     * survive a save: an omitted {@code true} would read back as false on the next login and
     * re-run the migration.
     */
    @Test
    void aMigratedProfileRoundTripsAndTheKeyIsActuallyWritten() throws Exception {
        var id = UUID.randomUUID();
        var repo = repo();

        repo.save(PlayerProfile.fresh(id).withVaultMigrated(true)).join();

        String json = Files.readString(dir.resolve(id + ".json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"vaultMigrated\": true"),
                "a primitive true is written, not omitted: " + json);
        assertTrue(repo.load(id).join().orElseThrow().vaultMigrated(),
                "and it reads back migrated, so the copy does not run a second time");

        // AND THE FALSE CASE IS WRITTEN TOO, which is what makes the flag readable rather than
        // inferred from absence on a file this build wrote.
        var notMigrated = UUID.randomUUID();
        repo.save(PlayerProfile.fresh(notMigrated)).join();
        assertTrue(Files.readString(dir.resolve(notMigrated + ".json"), StandardCharsets.UTF_8)
                .contains("\"vaultMigrated\": false"));
    }

    /**
     * An EXPLICIT {@code false} survives, and a {@code true} written back leaves NO KEY.
     *
     * <p>The second half is the {@code serializeNulls} property, executed: a player who has never
     * touched the toggle never gains the key on any number of saves, so <b>absence here is the
     * steady state rather than a window the deploy passes through.</b>
     */
    @Test
    void anEXPLICITFalseSurvives_andTheDefaultWritesNOKEYAtAll() throws Exception {
        var id = UUID.randomUUID();
        var repo = repo();

        repo.save(PlayerProfile.fresh(id).withStarEnabled(false)).join();
        assertFalse(repo.load(id).join().orElseThrow().starEnabled(),
                "a deliberate disable is not absence and must survive a reload");
        assertTrue(Files.readString(dir.resolve(id + ".json"), StandardCharsets.UTF_8)
                        .contains("\"starEnabled\": false"),
                "and it is written under the key `starEnabled`, not the component's own name");

        // AND A PROFILE NOBODY HAS TOGGLED WRITES NOTHING. `fresh` carries null, serializeNulls is
        // off, so the key is absent on disk -- which keeps the absent-means-enabled path the ONE
        // path forever rather than a transitional fallback.
        var untouched = UUID.randomUUID();
        repo.save(PlayerProfile.fresh(untouched)).join();
        String json = Files.readString(dir.resolve(untouched + ".json"), StandardCharsets.UTF_8);
        assertFalse(json.contains("starEnabled"),
                "an untouched toggle stores NO KEY: " + json);
        assertTrue(repo.load(untouched).join().orElseThrow().starEnabled(),
                "and reads back enabled");
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
