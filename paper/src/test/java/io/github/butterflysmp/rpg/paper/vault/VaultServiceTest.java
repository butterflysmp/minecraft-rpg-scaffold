package io.github.butterflysmp.rpg.paper.vault;

import io.github.butterflysmp.rpg.storage.PlayerVault;
import io.github.butterflysmp.rpg.storage.VaultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vault cache and its write-through.
 *
 * <p>Same harness as {@code ProfileServiceTest}: an in-memory repository with a hand-cranked load
 * future, so the races are deterministic rather than timed.
 *
 * <p>Fixtures use pages 2, 3 and 5 and slots 17, 24 and 31 -- no two quantities a row reads are
 * equal, and none of them is a bound.
 */
class VaultServiceTest {

    /** In-memory repository with a hand-cranked load future, so races are deterministic. */
    private static final class FakeRepository implements VaultRepository {
        final Map<UUID, PlayerVault> saved = new ConcurrentHashMap<>();
        final AtomicInteger saveCount = new AtomicInteger();
        final List<PlayerVault> savedInOrder = new ArrayList<>();

        /** When set, load() returns this instead of completing immediately. */
        CompletableFuture<Optional<PlayerVault>> pendingLoad;

        /** When set, save() returns a future already failed with this. */
        RuntimeException saveFailure;

        @Override public CompletableFuture<Optional<PlayerVault>> load(UUID playerId) {
            if (pendingLoad != null) return pendingLoad;
            return CompletableFuture.completedFuture(Optional.ofNullable(saved.get(playerId)));
        }

        @Override public CompletableFuture<Void> save(PlayerVault vault) {
            saveCount.incrementAndGet();
            if (saveFailure != null) return CompletableFuture.failedFuture(saveFailure);
            saved.put(vault.playerId(), vault);
            savedInOrder.add(vault);
            return CompletableFuture.completedFuture(null);
        }
    }

    private FakeRepository repo;
    private VaultService service;
    private UUID player;

    @BeforeEach
    void setUp() {
        repo = new FakeRepository();
        Logger quiet = Logger.getLogger("VaultServiceTest");
        quiet.setUseParentHandlers(false);
        quiet.setLevel(Level.OFF);
        service = new VaultService(repo, quiet);
        player = UUID.fromString("c41a9b03-5e7d-4268-91bf-3a0d6c85e29b");
    }

    // --- loading -----------------------------------------------------------------------------

    @Test
    void aPlayerWithNoFileJoinsWithAnEmptyVaultRatherThanNothing() {
        service.onJoin(player);

        PlayerVault loaded = service.vault(player).orElseThrow();
        assertEquals(0, loaded.occupiedSlots());
        assertEquals(player, loaded.playerId());
    }

    @Test
    void anExistingVaultIsLoadedWithItsContents() {
        repo.saved.put(player, PlayerVault.empty(player).withPage(3, Map.of(17, "stored")));

        service.onJoin(player);

        assertEquals("stored", service.vault(player).orElseThrow().page(3).get(17));
    }

    @Test
    void anUntrackedPlayerHasNoVault() {
        assertTrue(service.vault(player).isEmpty());
    }

    @Test
    void aVaultStillLoadingReadsAsAbsentRatherThanEmpty() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        assertTrue(service.vault(player).isEmpty(),
                "still loading is NOT an empty vault -- a caller that confused them would show the"
                        + " player an empty screen and then save it");
    }

    /**
     * *** AN UNREADABLE VAULT MUST NOT DEGRADE INTO AN EMPTY ONE. ***
     *
     * <p>If it did, the first write of the session would overwrite a file that might have been
     * recoverable. So the load stays failed, and every write is refused for the session.
     */
    @Test
    void anUnreadableVaultRefusesWritesForTheWholeSessionRatherThanStartingEmpty() {
        CompletableFuture<Optional<PlayerVault>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("corrupt on disk"));
        repo.pendingLoad = failed;

        service.onJoin(player);

        assertTrue(service.vault(player).isEmpty());
        assertFalse(service.writePage(player, 3, Map.of(17, "would overwrite a good file")),
                "the write must be REFUSED, not queued");
        assertEquals(0, repo.saveCount.get(), "and nothing may reach the repository");
    }

    // --- write-through -----------------------------------------------------------------------

    @Test
    void writingAPageSavesImmediatelyAndUpdatesTheCache() {
        service.onJoin(player);

        assertTrue(service.writePage(player, 3, Map.of(17, "a-blob", 24, "another")));

        assertEquals(1, repo.saveCount.get(), "write-through: one mutation, one write");
        assertEquals("a-blob", service.vault(player).orElseThrow().page(3).get(17));
        assertEquals("a-blob", repo.saved.get(player).page(3).get(17),
                "and the version on 'disk' is the new one, not the one before the mutation");
    }

    /** The cache is swapped BEFORE the save is issued, so the very next read sees the new contents. */
    @Test
    void theNextReadAfterAWriteSeesTheNewContents() {
        service.onJoin(player);
        service.writePage(player, 5, Map.of(31, "first"));

        assertEquals("first", service.vault(player).orElseThrow().page(5).get(31));

        service.writePage(player, 5, Map.of(31, "second"));
        assertEquals("second", service.vault(player).orElseThrow().page(5).get(31));
    }

    @Test
    void everyMutationWritesRatherThanBatching() {
        service.onJoin(player);

        service.writePage(player, 2, Map.of(17, "one"));
        service.writePage(player, 3, Map.of(24, "two"));
        service.writePage(player, 5, Map.of(31, "three"));

        assertEquals(3, repo.saveCount.get(),
                "three mutations, three writes -- never batched across a page change, because a"
                        + " flip discards the live slots and an unwritten page at that instant is a"
                        + " lost page");
    }

    @Test
    void writingOnePageLeavesTheOthersIntact() {
        service.onJoin(player);
        service.writePage(player, 2, Map.of(17, "keep"));
        service.writePage(player, 5, Map.of(31, "also keep"));

        service.writePage(player, 3, Map.of(24, "new"));

        PlayerVault after = service.vault(player).orElseThrow();
        assertEquals("keep", after.page(2).get(17));
        assertEquals("also keep", after.page(5).get(31));
        assertEquals("new", after.page(3).get(24));
        assertEquals(3, after.occupiedSlots());
    }

    @Test
    void anEmptyMapClearsThePageSoTheLastItemCanLeaveIt() {
        service.onJoin(player);
        service.writePage(player, 3, Map.of(17, "here for now"));

        service.writePage(player, 3, Map.of());

        assertTrue(service.vault(player).orElseThrow().page(3).isEmpty());
        assertEquals(0, repo.saved.get(player).occupiedSlots());
    }

    @Test
    void writingForAnUntrackedPlayerIsRefusedAndWritesNothing() {
        assertFalse(service.writePage(player, 3, Map.of(17, "nobody is here")));
        assertEquals(0, repo.saveCount.get());
    }

    /**
     * A failed disk write must not take the process with it, and must not silently look like a
     * success to the caller either. PR 1 logs it; surfacing it to a screen is owed to PR 2.
     */
    @Test
    void aFailedDiskWriteIsSwallowedAndLoggedRatherThanThrown() {
        service.onJoin(player);
        repo.saveFailure = new IllegalStateException("disk full");

        assertTrue(service.writePage(player, 3, Map.of(17, "did not reach disk")),
                "the synchronous part succeeded -- the failure is asynchronous");
        assertEquals("did not reach disk", service.vault(player).orElseThrow().page(3).get(17),
                "the cache still holds it, which is exactly why PR 2 needs the async failure"
                        + " surfaced: the menu would otherwise believe it is persisted");
    }

    // --- quit and shutdown -------------------------------------------------------------------

    /**
     * *** THE ABSENT SAVE ON QUIT IS ASSERTED, NOT ASSUMED. ***
     *
     * <p>A save here would be redundant under write-through AND would mask a broken write-through:
     * a build whose per-mutation write did nothing would still look correct on every ordinary quit.
     * This row is what makes that impossible to add back by accident.
     */
    @Test
    void quittingDoesNotWriteBecauseWriteThroughAlreadyDid() {
        service.onJoin(player);
        service.writePage(player, 3, Map.of(17, "already on disk"));
        int afterWrite = repo.saveCount.get();

        service.onQuit(player);

        assertEquals(afterWrite, repo.saveCount.get(), "quit must issue NO further write");
        assertTrue(service.vault(player).isEmpty(), "and the cache entry is dropped");
    }

    @Test
    void quittingAnUntrackedPlayerIsHarmless() {
        service.onQuit(player);
        assertEquals(0, repo.saveCount.get());
    }

    @Test
    void shutdownFlushesEveryTrackedPlayerAndClearsThem() {
        UUID other = UUID.fromString("8d05f7a2-6c31-4be9-a074-2f1e93c8b56d");
        service.onJoin(player);
        service.onJoin(other);
        service.writePage(player, 3, Map.of(17, "mine"));
        int afterWrites = repo.saveCount.get();

        service.saveAllAndClear().join();

        assertEquals(afterWrites + 2, repo.saveCount.get(),
                "both tracked players are flushed -- the guarantee is DRAINING the queue, not"
                        + " content, and onDisable waits on this future before the executor stops");
        assertTrue(service.trackedPlayers().isEmpty());
    }

    @Test
    void aFailedShutdownSaveDoesNotStopTheOthers() {
        UUID other = UUID.fromString("8d05f7a2-6c31-4be9-a074-2f1e93c8b56d");
        service.onJoin(player);
        service.onJoin(other);
        repo.saveFailure = new IllegalStateException("disk full");

        service.saveAllAndClear().join();

        assertTrue(service.trackedPlayers().isEmpty(),
                "a shutdown that abandoned the remaining players on the first failure would lose"
                        + " every vault after the broken one");
    }

    // --- whenSettled -------------------------------------------------------------------------

    @Test
    void whenSettledRunsOnlyAfterTheLoadCompletes() {
        CompletableFuture<Optional<PlayerVault>> pending = new CompletableFuture<>();
        repo.pendingLoad = pending;
        service.onJoin(player);

        List<Optional<PlayerVault>> seen = new ArrayList<>();
        service.whenSettled(player, seen::add);

        assertTrue(seen.isEmpty(), "must not fire while the load is in flight");

        pending.complete(Optional.of(PlayerVault.empty(player).withPage(2, Map.of(17, "late"))));

        assertEquals(1, seen.size());
        assertEquals("late", seen.get(0).orElseThrow().page(2).get(17));
    }

    @Test
    void whenSettledReportsEmptyForAFailedLoad() {
        CompletableFuture<Optional<PlayerVault>> failed = new CompletableFuture<>();
        repo.pendingLoad = failed;
        service.onJoin(player);

        List<Optional<PlayerVault>> seen = new ArrayList<>();
        service.whenSettled(player, seen::add);
        failed.completeExceptionally(new IllegalStateException("corrupt"));

        assertEquals(1, seen.size());
        assertTrue(seen.get(0).isEmpty());
    }

    @Test
    void whenSettledOnAnUntrackedPlayerIsSettledByDefinition() {
        List<Optional<PlayerVault>> seen = new ArrayList<>();
        service.whenSettled(player, seen::add);

        assertEquals(1, seen.size());
        assertTrue(seen.get(0).isEmpty());
    }

    /**
     * A quit before the load lands must not resurrect the player in the cache. Chaining on the
     * future rather than the value is what makes a fast rejoin safe.
     */
    @Test
    void quittingBeforeTheLoadCompletesLeavesNothingBehind() {
        CompletableFuture<Optional<PlayerVault>> pending = new CompletableFuture<>();
        repo.pendingLoad = pending;
        service.onJoin(player);

        service.onQuit(player);
        pending.complete(Optional.of(PlayerVault.empty(player)));

        assertTrue(service.trackedPlayers().isEmpty());
        assertEquals(0, repo.saveCount.get(), "a quit before the load must not write anything");
    }

    @Test
    void trackedPlayersIsASnapshotAndNotALiveView() {
        service.onJoin(player);
        var snapshot = service.trackedPlayers();
        service.onQuit(player);

        assertEquals(1, snapshot.size(), "the returned set must not change under the caller");
        assertTrue(snapshot.contains(player));
        assertTrue(service.trackedPlayers().isEmpty(), "while the live answer has moved on");
    }
}
