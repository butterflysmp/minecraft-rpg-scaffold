package io.github.butterflysmp.rpg.paper.vault;

import io.github.butterflysmp.rpg.core.vault.VaultCell;
import io.github.butterflysmp.rpg.storage.PlayerVault;
import io.github.butterflysmp.rpg.storage.VaultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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

        /**
         * When set, save() returns THIS future instead of completing -- so a test can decide when,
         * and after which other events, the failure arrives.
         *
         * <p>The hand-cranked counterpart of {@code pendingLoad}, and it exists for one row: a
         * failure that lands AFTER the player has quit, which is the ordering the owed-set capture
         * is there to survive.
         */
        CompletableFuture<Void> pendingSave;

        @Override public CompletableFuture<Optional<PlayerVault>> load(UUID playerId) {
            if (pendingLoad != null) return pendingLoad;
            return CompletableFuture.completedFuture(Optional.ofNullable(saved.get(playerId)));
        }

        @Override public CompletableFuture<Void> save(PlayerVault vault) {
            saveCount.incrementAndGet();
            if (pendingSave != null) return pendingSave;
            if (saveFailure != null) return CompletableFuture.failedFuture(saveFailure);
            saved.put(vault.playerId(), vault);
            savedInOrder.add(vault);
            return CompletableFuture.completedFuture(null);
        }
    }

    private FakeRepository repo;
    private VaultService service;
    private UUID player;

    /** How many times the write's failure callback has run. */
    private final AtomicInteger failures = new AtomicInteger();

    /**
     * Every record the service logged, in order.
     *
     * <h2>*** ADDED BECAUSE A MUTATION SURVIVED, AND THE MUTATION WAS RIGHT TO ***</h2>
     *
     * {@code MUTFLUSHPOISON} deletes the explicit poisoned-vault skip from
     * {@code saveAllAndClear} and <b>killed nothing</b>: {@code thenCompose} short-circuits on a
     * failed future anyway, so the BEHAVIOUR is identical either way. The arm's whole effect is
     * WHICH MESSAGE an operator reads -- a WARNING saying this vault was abandoned earlier, or a
     * SEVERE saying the shutdown failed to save it.
     *
     * <p>That is not cosmetic. <b>An operator chasing "Failed to save vault during shutdown" is
     * chasing the wrong incident</b> -- the real one happened minutes earlier and its report names
     * the page and the items. So the log is captured and asserted, which turns an unkillable guard
     * into a killable one rather than leaving it as decoration.
     */
    private final List<LogRecord> logged = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repo = new FakeRepository();
        Logger quiet = Logger.getLogger("VaultServiceTest");
        quiet.setUseParentHandlers(false);
        // NOT Level.OFF any more: a logger set to OFF hands nothing to its handlers, so the capture
        // below would be silently empty and every assertion on it would pass by finding nothing.
        quiet.setLevel(Level.ALL);
        logged.clear();
        quiet.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { logged.add(record); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        service = new VaultService(repo, quiet);
        player = UUID.fromString("c41a9b03-5e7d-4268-91bf-3a0d6c85e29b");
        failures.set(0);
    }

    /** Every captured message at or above a level, joined -- for asserting what an operator sees. */
    private String loggedAt(Level level) {
        StringBuilder text = new StringBuilder();
        for (LogRecord record : logged) {
            if (record.getLevel().intValue() >= level.intValue()) {
                text.append(record.getMessage()).append('\n');
            }
        }
        return text.toString();
    }

    /**
     * Write a page for {@code player}, in the terms the rows are written in.
     *
     * <h2>THE DESCRIPTION IS DERIVED FROM THE ITEM TEXT, WHICH MAKES A SWAP VISIBLE</h2>
     *
     * Every cell's description is {@code "TEST <item>"}, so a build that stored the DESCRIPTION
     * where the item belongs would put {@code "TEST a-blob"} on disk and every content assertion
     * below would fail loudly. A constant description would have let that swap through.
     */
    private boolean write(int page, Map<Integer, String> items) {
        Map<Integer, VaultCell> cells = new LinkedHashMap<>();
        items.forEach((slot, item) -> cells.put(slot, new VaultCell(item, "TEST " + item)));
        return service.writePage(player, page, cells, owed -> {
            failures.incrementAndGet();
            lastOwed = owed;
        });
    }

    /**
     * The unpersisted set from the most recent failure.
     *
     * <p>The callback's ARGUMENT is what a caller with no screen left uses to decide which cells to
     * drop, so a test that only counted the callback could not tell a correct set from an empty one
     * -- and an empty one silently destroys, while a full one silently duplicates.
     */
    private Set<Integer> lastOwed;

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
        assertFalse(write(3, Map.of(17, "would overwrite a good file")),
                "the write must be REFUSED, not queued");
        assertEquals(0, repo.saveCount.get(), "and nothing may reach the repository");
    }

    // --- write-through -----------------------------------------------------------------------

    @Test
    void writingAPageSavesImmediatelyAndUpdatesTheCache() {
        service.onJoin(player);

        assertTrue(write(3, Map.of(17, "a-blob", 24, "another")));

        assertEquals(1, repo.saveCount.get(), "write-through: one mutation, one write");
        assertEquals("a-blob", service.vault(player).orElseThrow().page(3).get(17));
        assertEquals("a-blob", repo.saved.get(player).page(3).get(17),
                "and the version on 'disk' is the new one, not the one before the mutation");
    }

    /** The cache is swapped BEFORE the save is issued, so the very next read sees the new contents. */
    @Test
    void theNextReadAfterAWriteSeesTheNewContents() {
        service.onJoin(player);
        write(5, Map.of(31, "first"));

        assertEquals("first", service.vault(player).orElseThrow().page(5).get(31));

        write(5, Map.of(31, "second"));
        assertEquals("second", service.vault(player).orElseThrow().page(5).get(31));
    }

    @Test
    void everyMutationWritesRatherThanBatching() {
        service.onJoin(player);

        write(2, Map.of(17, "one"));
        write(3, Map.of(24, "two"));
        write(5, Map.of(31, "three"));

        assertEquals(3, repo.saveCount.get(),
                "three mutations, three writes -- never batched across a page change, because a"
                        + " flip discards the live slots and an unwritten page at that instant is a"
                        + " lost page");
    }

    @Test
    void writingOnePageLeavesTheOthersIntact() {
        service.onJoin(player);
        write(2, Map.of(17, "keep"));
        write(5, Map.of(31, "also keep"));

        write(3, Map.of(24, "new"));

        PlayerVault after = service.vault(player).orElseThrow();
        assertEquals("keep", after.page(2).get(17));
        assertEquals("also keep", after.page(5).get(31));
        assertEquals("new", after.page(3).get(24));
        assertEquals(3, after.occupiedSlots());
    }

    @Test
    void anEmptyMapClearsThePageSoTheLastItemCanLeaveIt() {
        service.onJoin(player);
        write(3, Map.of(17, "here for now"));

        write(3, Map.of());

        assertTrue(service.vault(player).orElseThrow().page(3).isEmpty());
        assertEquals(0, repo.saved.get(player).occupiedSlots());
    }

    @Test
    void writingForAnUntrackedPlayerIsRefusedAndWritesNothing() {
        assertFalse(write(3, Map.of(17, "nobody is here")));
        assertEquals(0, repo.saveCount.get());
    }

    /**
     * A failed disk write must not take the process with it, and must not silently look like a
     * success to the caller either.
     *
     * <h2>*** THIS ROW USED TO ASSERT THE OPPOSITE, AND THE OLD ASSERTION IS THE DEFECT ***</h2>
     *
     * <p>In PR 1 it read: <i>"the cache still holds it, which is exactly why PR 2 needs the async
     * failure surfaced: the menu would otherwise believe it is persisted"</i>. <b>That was a correct
     * description of a cache that is AHEAD OF DISK</b> -- and the consequence it did not name is
     * that {@code repository.save} writes the WHOLE vault, so any later successful write, and the
     * shutdown flush, would have republished the failed page from that cache.
     *
     * <p>Now the vault is POISONED instead: the callback runs, every later write is refused, and
     * the shutdown flush skips it. The rows below take those one at a time.
     */
    @Test
    void aFailedDiskWritePoisonsTheVaultRatherThanLeavingTheCacheAhead() {
        service.onJoin(player);
        repo.saveFailure = new IllegalStateException("disk full");

        assertTrue(write(3, Map.of(17, "did not reach disk")),
                "the synchronous part succeeded -- the failure is asynchronous");

        assertEquals(1, failures.get(), "the failure callback is what the screen degrades on");
        assertTrue(service.unusable(player), "and the vault is poisoned for the session");
        assertTrue(service.vault(player).isEmpty(),
                "a poisoned vault reads as absent, exactly as an unreadable one does -- so every"
                        + " existing reader refuses it without having to learn a second condition");
    }

    /**
     * *** A POISONED VAULT REFUSES EVERY LATER WRITE, INCLUDING TO A DIFFERENT PAGE. ***
     *
     * <p>The different page is the point. {@code withPage} carries every other page's entries
     * forward, so a successful write of page 5 would have carried the failed page 3 along with it
     * and put it on disk -- <b>the item the player was told was not saved, appearing in the file
     * later, while they are also holding it.</b>
     */
    @Test
    void aPoisonedVaultRefusesAWriteToADifferentPageToo() {
        service.onJoin(player);
        repo.saveFailure = new IllegalStateException("disk full");
        write(3, Map.of(17, "did not reach disk"));

        int savesAtFailure = repo.saveCount.get();
        repo.saveFailure = null;      // the disk "recovers" -- it must not matter

        assertFalse(write(5, Map.of(31, "a different page entirely")),
                "refused: the cache is ahead of disk and writing it would republish page 3");
        assertEquals(savesAtFailure, repo.saveCount.get(),
                "and nothing reached the repository, so the failed page cannot ride along");
    }

    /**
     * *** THE SHUTDOWN FLUSH SKIPS A POISONED VAULT, AND THIS IS THE SECOND REPUBLICATION ROUTE. ***
     *
     * <p>A player who never touches the vault again after a failed write still triggers
     * {@code saveAllAndClear} at {@code /stop}. That writes the CACHE -- so without the skip, a
     * clean shutdown alone would put the failed page on disk.
     */
    @Test
    void theShutdownFlushDoesNotWriteAPoisonedVault() {
        service.onJoin(player);
        write(3, Map.of(17, "the good contents"));
        assertEquals("the good contents", repo.saved.get(player).page(3).get(17));

        repo.saveFailure = new IllegalStateException("disk full");
        write(3, Map.of(17, "the good contents", 24, "never reached disk"));

        repo.saveFailure = null;
        int savesBeforeShutdown = repo.saveCount.get();

        service.saveAllAndClear().join();

        assertEquals(savesBeforeShutdown, repo.saveCount.get(),
                "the flush must not attempt a poisoned vault at all");
        assertFalse(repo.saved.get(player).page(3).containsKey(24),
                "and the file still holds its LAST GOOD contents -- slot 24 never appears in it");
    }

    /**
     * *** THE SHUTDOWN NAMES THE RIGHT INCIDENT, AND THIS IS WHAT MAKES THE SKIP KILLABLE. ***
     *
     * <p>Behaviourally the explicit skip is redundant -- {@code thenCompose} short-circuits on a
     * failed future, so deleting it writes nothing either way, and {@code MUTFLUSHPOISON} killed
     * NOTHING before this row existed.
     *
     * <p>What it changes is the sentence an operator reads at {@code /stop}. Without the skip the
     * flush reports <i>"Failed to save vault ... during shutdown"</i>, which describes an event that
     * did not happen and points at the wrong minute of the log. <b>The real incident is the write
     * that failed earlier, whose report names the page and every item.</b>
     */
    @Test
    void theShutdownSaysTheVaultWasABANDONEDRatherThanThatTheShutdownFailed() {
        service.onJoin(player);
        repo.saveFailure = new IllegalStateException("disk full");
        write(3, Map.of(17, "never reached disk"));
        repo.saveFailure = null;

        logged.clear();
        service.saveAllAndClear().join();

        String warnings = loggedAt(Level.WARNING);
        assertTrue(warnings.contains("was not written at shutdown"),
                "the flush must say this vault was already abandoned: " + warnings);
        assertTrue(warnings.contains("see the earlier SEVERE report"),
                "and must point at the incident that actually names the items: " + warnings);
        assertFalse(warnings.contains("Failed to save vault"),
                "and must NOT report a shutdown failure, which is a different event at a different"
                        + " time and sends the operator to the wrong place: " + warnings);
    }

    /**
     * The control on the capture itself: a clean shutdown logs NOTHING at WARNING or above.
     *
     * <p>Without it, every assertion above is equally consistent with a handler that captures
     * nothing -- the row would pass by finding no text to object to. This is the positive control
     * the {@code Level.OFF} trap would have defeated silently.
     */
    @Test
    void aCleanShutdownLogsNothingAtWarningOrAbove() {
        service.onJoin(player);
        write(3, Map.of(17, "fine"));

        logged.clear();
        service.saveAllAndClear().join();

        assertEquals("", loggedAt(Level.WARNING),
                "a healthy flush is silent, so the rows above are reading real output");
    }

    /**
     * The control, and without it every row above passes on a build that poisons unconditionally.
     *
     * <p>A vault that poisoned itself on a SUCCESSFUL write would refuse the second gesture of every
     * session, and the rows above -- all of which stage a failure first -- could not tell.
     */
    @Test
    void aSuccessfulWriteDoesNotPoisonAnything() {
        service.onJoin(player);

        assertTrue(write(3, Map.of(17, "fine")));
        assertFalse(service.unusable(player));
        assertEquals(0, failures.get());

        assertTrue(write(5, Map.of(31, "also fine")), "a second write still works");
        assertEquals("fine", repo.saved.get(player).page(3).get(17));
        assertEquals("also fine", repo.saved.get(player).page(5).get(31));
    }

    /** {@code unusable} is false for a player nobody has loaded, not true by accident. */
    @Test
    void anUntrackedPlayerIsNotReportedAsPoisoned() {
        assertFalse(service.unusable(player),
                "untracked is not poisoned -- a caller that confused them would refuse to open a"
                        + " screen for anyone whose load has not started");
    }

    /**
     * *** THE PERSISTED VIEW IS WHAT IS ON DISK, NOT WHAT THE CACHE INTENDS. ***
     *
     * <p>It exists so a degraded close can hand back the DIFFERENCE rather than the whole page.
     * After a failed write the cache holds the new contents and the file holds the old ones; this
     * row pins that the two have actually diverged, which is the premise the subtraction rests on.
     */
    @Test
    void thePersistedViewStaysAtTheLASTGOODWriteWhileTheCacheMovesOn() {
        service.onJoin(player);
        write(3, Map.of(17, "first"));
        assertEquals("first", service.persistedPage(player, 3).get(17),
                "a successful write advances the persisted view");

        repo.saveFailure = new IllegalStateException("disk full");
        write(3, Map.of(17, "first", 24, "never reached disk"));

        assertEquals(Map.of(17, "first"), service.persistedPage(player, 3),
                "the failed write must NOT advance it -- slot 24 is not in the file");
    }

    /**
     * *** THE FAILURE CALLBACK NAMES THE CELLS THAT REACHED NOBODY, NOT THE PAGE. ***
     *
     * <p>This is what a caller whose screen is already torn down acts on: those cells are dropped on
     * the ground, and the ones the file still holds are not. <b>Hand it the whole page and every
     * persisted stack is duplicated; hand it nothing and the new one is destroyed.</b>
     */
    @Test
    void theFailureCallbackReportsONLYTheCellsThatDidNotReachDisk() {
        service.onJoin(player);
        write(3, Map.of(17, "first", 31, "second"));
        assertEquals(0, failures.get(), "the baseline write must succeed for this row to mean anything");

        repo.saveFailure = new IllegalStateException("disk full");
        write(3, Map.of(17, "first", 24, "the new one", 31, "second"));

        assertEquals(1, failures.get());
        assertEquals(Set.of(24), lastOwed,
                "slot 24 alone: 17 and 31 are byte-identical to what the file already holds");
    }

    /**
     * A cell whose CONTENTS changed is owed, even though the file has that slot.
     *
     * <p>The swap case. A comparison by slot presence would call 17 persisted and destroy the item
     * the player actually put there.
     */
    @Test
    void aSwappedCellIsReportedEvenThoughTheFileHasThatSlot() {
        service.onJoin(player);
        write(3, Map.of(17, "original"));

        repo.saveFailure = new IllegalStateException("disk full");
        write(3, Map.of(17, "swapped in"));

        assertEquals(Set.of(17), lastOwed);
    }

    /**
     * *** THE OWED SET IS COMPUTED AGAINST THE DISK VIEW AS IT WAS AT WRITE TIME. ***
     *
     * <p>By the time the failure arrives the player may have QUIT, and {@code onQuit} clears the
     * persisted map. A callback that looked the view up at failure time would find nothing, report
     * every cell as unpersisted, and hand the caller a page to drop that the file already has --
     * <b>the duplicator, rebuilt inside the fix for it.</b>
     */
    @Test
    void theOwedSetSurvivesThePlayerQuittingBeforeTheFailureArrives() {
        service.onJoin(player);
        write(3, Map.of(17, "on disk"));

        // A save that never completes, so the failure can be fired AFTER the quit.
        CompletableFuture<Void> hanging = new CompletableFuture<>();
        repo.pendingSave = hanging;
        write(3, Map.of(17, "on disk", 24, "not on disk"));

        service.onQuit(player);
        assertEquals(Map.of(), service.persistedPage(player, 3), "the quit cleared the view");

        hanging.completeExceptionally(new IllegalStateException("disk full"));

        assertEquals(Set.of(24), lastOwed,
                "the view was CAPTURED before the save, so slot 17 is still known to be on disk");
    }

    /** An absent file seeds an EMPTY persisted view, which is the correct baseline rather than none. */
    @Test
    void aPlayerWithNoFileStartsWithNothingKnownToBeOnDisk() {
        service.onJoin(player);

        assertEquals(Map.of(), service.persistedPage(player, 3));
    }

    /** A loaded file seeds the persisted view with exactly what was loaded. */
    @Test
    void anExistingFileSeedsThePersistedViewWithItsContents() {
        repo.saved.put(player, PlayerVault.empty(player).withPage(5, Map.of(31, "on disk")));

        service.onJoin(player);

        assertEquals("on disk", service.persistedPage(player, 5).get(31),
                "what we loaded IS what is on disk, so a close before any write hands back nothing");
    }

    /**
     * An untracked player knows nothing, and that is the SAFE direction.
     *
     * <p>Empty means "no cell is known to be on disk", so a degraded close hands everything back.
     * Wrong that way costs a duplicate; wrong the other way costs the item.
     */
    @Test
    void anUntrackedPlayerHasNothingKnownToBeOnDisk() {
        assertEquals(Map.of(), service.persistedPage(player, 3));
    }

    /** The description on a cell is a log-time concern and must never reach the file. */
    @Test
    void theCellDescriptionIsNotStored() {
        service.onJoin(player);

        write(3, Map.of(17, "a-blob"));

        assertEquals("a-blob", repo.saved.get(player).page(3).get(17),
                "the ITEM is stored, not the 'TEST a-blob' description the helper attached");
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
        write(3, Map.of(17, "already on disk"));
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
        write(3, Map.of(17, "mine"));
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
