package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vault file: what it writes, what it refuses, and what an absent one means.
 *
 * <p>Same harness as {@link FilePlayerRepositoryTest} -- {@code @TempDir} plus an inline executor, so
 * every future is already complete and {@code join()} never actually blocks.
 */
class FileVaultRepositoryTest {

    @TempDir
    Path dir;

    /** Runs inline, so every future is already complete. Deterministic tests. */
    private static final Executor DIRECT = Runnable::run;

    private static final UUID PLAYER = UUID.fromString("5c2b9e70-14af-4d83-a6e1-b70f3d9285ca");

    private FileVaultRepository repo() {
        return new FileVaultRepository(dir, DIRECT);
    }

    private static PlayerVault stocked() {
        return PlayerVault.empty(PLAYER)
                .withPage(3, Map.of(17, "a-weapon-blob", 24, "a-second-blob"))
                .withPage(5, Map.of(31, "a-third-blob"));
    }

    /**
     * *** AN ABSENT FILE MEANS AN EMPTY VAULT, AND THAT IS THE WHOLE OF IT. ***
     *
     * <p>No stamp, no sentinel, nothing to disambiguate: no vault and an empty vault are the same
     * thing. Contrast {@code nexusSlot}, where 0 is a legal choice and absence therefore needed a
     * migration to tell the two apart.
     */
    @Test
    void loadingAPlayerWithNoVaultYieldsEmpty() {
        assertTrue(repo().load(PLAYER).join().isEmpty());
    }

    @Test
    void saveThenLoadRoundTripsEveryPageAndSlot() {
        FileVaultRepository repo = repo();
        repo.save(stocked()).join();

        PlayerVault back = repo.load(PLAYER).join().orElseThrow();

        assertEquals(3, back.occupiedSlots());
        assertEquals("a-weapon-blob", back.page(3).get(17));
        assertEquals("a-second-blob", back.page(3).get(24));
        assertEquals("a-third-blob", back.page(5).get(31));
        assertEquals(PLAYER, back.playerId());
        assertEquals(PlayerVault.CURRENT_SCHEMA_VERSION, back.schemaVersion());
    }

    /**
     * The item text crosses this layer UNREAD. Staged with characters Base64 actually produces --
     * {@code +}, {@code /} and the {@code =} pad -- because a codec that survived alphanumerics and
     * mangled the padding would round-trip every friendlier fixture.
     */
    @Test
    void theItemTextIsOpaqueAndSurvivesBase64Punctuation() {
        String blob = "H4sIAAAA+//=" + "AbC/9+xyz==";
        repo().save(PlayerVault.empty(PLAYER).withPage(2, Map.of(35, blob))).join();

        assertEquals(blob, repo().load(PLAYER).join().orElseThrow().page(2).get(35));
    }

    @Test
    void saveIsAtomicAndOverwritesLeavingNoTempFile() {
        FileVaultRepository repo = repo();

        repo.save(stocked()).join();
        repo.save(PlayerVault.empty(PLAYER).withPage(2, Map.of(31, "the only one left"))).join();

        PlayerVault back = repo.load(PLAYER).join().orElseThrow();
        assertEquals(1, back.occupiedSlots(), "the second save replaced the first, it did not merge");
        assertEquals(1, dir.toFile().listFiles((d, n) -> n.endsWith(".json")).length);
        assertEquals(0, dir.toFile().listFiles((d, n) -> n.endsWith(".tmp")).length,
                "the temp file must not survive -- a vault is written on every item moved, so this"
                        + " window is entered constantly rather than once per level");
    }

    @Test
    void eachPlayerGetsTheirOwnFileNamedForThem() {
        UUID other = UUID.fromString("9e3d7061-2b58-4c14-8fa2-6d05b97e3418");
        FileVaultRepository repo = repo();

        repo.save(stocked()).join();
        repo.save(PlayerVault.empty(other).withPage(2, Map.of(17, "theirs"))).join();

        assertTrue(Files.exists(dir.resolve(PLAYER + ".json")));
        assertTrue(Files.exists(dir.resolve(other + ".json")));
        assertEquals(3, repo.load(PLAYER).join().orElseThrow().occupiedSlots());
        assertEquals(1, repo.load(other).join().orElseThrow().occupiedSlots());
    }

    /** The constructor makes its own directory, as {@code FilePlayerRepository} does. */
    @Test
    void theVaultDirectoryIsCreatedIfItIsNotThere() {
        Path nested = dir.resolve("vaults");
        new FileVaultRepository(nested, DIRECT).save(stocked()).join();

        assertTrue(Files.isDirectory(nested));
        assertTrue(Files.exists(nested.resolve(PLAYER + ".json")));
    }

    /**
     * *** THE POSITIVE CONTROL ON THE WIRING: migrate() IS ACTUALLY ON THE LOAD PATH. ***
     *
     * <p>{@link VaultMigrationsTest} proves the refusal fires when called. It cannot prove anybody
     * calls it. Staging a newer stamp in the FILE and reading it through the repository is what says
     * the chain is wired in -- delete the {@code map(VaultMigrations::migrate)} and this row is the
     * only thing that reddens.
     */
    @Test
    void aFileFromANewerServerIsRefusedThroughTheRepository() throws IOException {
        Files.write(dir.resolve(PLAYER + ".json"), ("""
                {
                  "schemaVersion": 9,
                  "playerId": "%s",
                  "entries": []
                }
                """.formatted(PLAYER)).getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE);

        CompletionException thrown =
                assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());

        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertTrue(thrown.getCause().getMessage().contains("9"));
    }

    /**
     * An unparseable file must NOT read as "no vault".
     *
     * <p>That is the difference between an error the player sees and an empty screen whose first
     * click saves the emptiness over a file that might still have been recoverable. The failure is
     * loud on purpose.
     */
    @Test
    void anUnparseableFileIsRefusedRatherThanReadAsAnEmptyVault() throws IOException {
        Files.write(dir.resolve(PLAYER + ".json"),
                "{ this is not json".getBytes(StandardCharsets.UTF_8));

        CompletionException thrown =
                assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());

        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertTrue(thrown.getCause().getMessage().contains(PLAYER.toString()),
                "name whose file it is -- the operator has to go and look at it");
        assertTrue(thrown.getCause().getMessage().contains("overwrite"),
                "and say WHY it refused, or the next person 'fixes' it by returning empty");
    }

    /**
     * A file carrying two entries for one cell is structural damage and is refused on load, by the
     * record's own compact constructor. Staged through the FILE rather than the constructor, because
     * that is the route a corrupt vault actually arrives by.
     *
     * <h2>*** AND THE LAST ASSERTION IS THE ONE WITH TEETH: GSON'S MESSAGE MUST NOT SURVIVE. ***</h2>
     *
     * Gson wraps a record's constructor failure in a bare {@code RuntimeException} whose message
     * <b>inlines every constructor argument</b> -- measured: {@code "Failed to invoke constructor
     * 'PlayerVault(int, UUID, List)' with args [1, <uuid>, [VaultEntry[page=3, slot=24,
     * item=first], ...]]"}. For a full vault that is 252 Base64 item blobs in the server log.
     *
     * <p>So the repository promotes the CAUSE's message and drops Gson's. Without this row that
     * unwrapping is invisible: the load still fails, the test still goes red on a missing refusal,
     * and nobody notices the log has the player's whole vault in it.
     */
    @Test
    void aFileWithTwoEntriesForOneCellIsRefused() throws IOException {
        Files.write(dir.resolve(PLAYER + ".json"), ("""
                {
                  "schemaVersion": 1,
                  "playerId": "%s",
                  "entries": [
                    { "page": 3, "slot": 24, "item": "first" },
                    { "page": 3, "slot": 24, "item": "second" }
                  ]
                }
                """.formatted(PLAYER)).getBytes(StandardCharsets.UTF_8));

        CompletionException thrown =
                assertThrows(CompletionException.class, () -> repo().load(PLAYER).join());

        Throwable refusal = thrown.getCause();
        assertInstanceOf(IllegalStateException.class, refusal);
        assertTrue(refusal.getMessage().contains(PLAYER.toString()), "name whose file it is");
        assertTrue(refusal.getMessage().contains("page 3"), "and which cell is doubled");
        assertTrue(refusal.getMessage().contains("slot 24"));

        assertFalse(refusal.getMessage().contains("Failed to invoke constructor"),
                "Gson's own message inlines EVERY constructor argument -- for a real vault that is"
                        + " 252 item blobs in the log. The cause's message is promoted instead.");
        assertFalse(refusal.getMessage().contains("first"),
                "and no item text reaches the message, which is the same rule stated as a fact about"
                        + " the payload rather than about Gson's wording");
        assertInstanceOf(IllegalArgumentException.class, refusal.getCause(),
                "the real cause still hangs off it for anyone who wants the stack");
    }

    /** Written sparsely: an empty vault names no slots at all, rather than 252 nulls. */
    @Test
    void theFileIsSparseAndAnEmptyVaultNamesNoSlots() throws IOException {
        repo().save(PlayerVault.empty(PLAYER)).join();

        String json = Files.readString(dir.resolve(PLAYER + ".json"), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"entries\": []"), "an empty vault writes an empty array");
        assertTrue(json.length() < 200,
                "an empty vault is a few lines, not a quarter-megabyte of holes; measured well under"
                        + " 200 characters, and the bound is loose on purpose");
    }

    /** Canonical order on disk, so two saves of the same contents are byte-identical. */
    @Test
    void theFileIsWrittenInCanonicalOrder() throws IOException {
        FileVaultRepository repo = repo();

        repo.save(PlayerVault.empty(PLAYER)
                .withPage(5, Map.of(31, "five-thirtyone"))
                .withPage(2, Map.of(17, "two-seventeen"))).join();
        String first = Files.readString(dir.resolve(PLAYER + ".json"), StandardCharsets.UTF_8);

        repo.save(new PlayerVault(PlayerVault.CURRENT_SCHEMA_VERSION, PLAYER, List.of(
                new VaultEntry(5, 31, "five-thirtyone"),
                new VaultEntry(2, 17, "two-seventeen")))).join();
        String second = Files.readString(dir.resolve(PLAYER + ".json"), StandardCharsets.UTF_8);

        assertEquals(first, second,
                "same contents, different insertion order, identical bytes -- so a diff of a vault"
                        + " file shows what CHANGED rather than what moved");
    }

    /** Every task must run on the supplied executor, never the common pool. */
    @Test
    void allIoRunsOnTheSuppliedExecutor() {
        AtomicInteger used = new AtomicInteger();
        Executor counting = task -> { used.incrementAndGet(); task.run(); };
        FileVaultRepository repo = new FileVaultRepository(dir, counting);

        repo.save(stocked()).join();
        repo.load(PLAYER).join();

        assertEquals(2, used.get(), "save and load must each go through the executor");
    }
}
