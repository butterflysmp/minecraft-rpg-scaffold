package io.github.yourname.rpg.paper.profile;

import io.github.yourname.rpg.storage.PlayerProfile;
import io.github.yourname.rpg.storage.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceTest {

    /** In-memory repository with a hand-cranked load future, so races are deterministic. */
    private static final class FakeRepository implements PlayerRepository {
        final Map<UUID, PlayerProfile> saved = new ConcurrentHashMap<>();
        final AtomicInteger saveCount = new AtomicInteger();

        /** When set, load() returns this instead of completing immediately. */
        CompletableFuture<Optional<PlayerProfile>> pendingLoad;

        @Override public CompletableFuture<Optional<PlayerProfile>> load(UUID playerId) {
            if (pendingLoad != null) return pendingLoad;
            return CompletableFuture.completedFuture(Optional.ofNullable(saved.get(playerId)));
        }

        @Override public CompletableFuture<Void> save(PlayerProfile profile) {
            saveCount.incrementAndGet();
            saved.put(profile.playerId(), profile);
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Boolean> tryAcquireLock(UUID p, String s) {
            return CompletableFuture.completedFuture(true);
        }

        @Override public CompletableFuture<Void> releaseLock(UUID p, String s) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private FakeRepository repo;
    private ProfileService service;
    private UUID player;

    @BeforeEach
    void setUp() {
        repo = new FakeRepository();
        Logger quiet = Logger.getLogger("ProfileServiceTest");
        quiet.setUseParentHandlers(false);
        quiet.setLevel(Level.OFF);
        service = new ProfileService(repo, quiet, () -> 4242L);
        player = UUID.randomUUID();
    }

    @Test
    void joinWithNoStoredProfileCreatesAFreshOne() {
        service.onJoin(player);

        PlayerProfile profile = service.profile(player).orElseThrow();
        assertEquals(player, profile.playerId());
        assertEquals(1, profile.level());
        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, profile.schemaVersion());
    }

    @Test
    void joinLoadsAnExistingProfile() {
        repo.saved.put(player, new PlayerProfile(1, player, "hunter", 9, 500, List.of("x"), 1L));

        service.onJoin(player);

        assertEquals(9, service.profile(player).orElseThrow().level());
    }

    @Test
    void quitSavesTheProfileAndStampsLastSeen() {
        service.onJoin(player);

        service.onQuit(player);

        assertEquals(1, repo.saveCount.get());
        assertEquals(4242L, repo.saved.get(player).lastSeenEpochMillis());
        assertEquals(0, service.trackedPlayers(), "the player must be dropped from memory");
        assertTrue(service.profile(player).isEmpty());
    }

    @Test
    void quittingSomeoneWhoNeverJoinedDoesNothing() {
        assertDoesNotThrow(() -> service.onQuit(player));
        assertEquals(0, repo.saveCount.get());
    }

    /**
     * A player can log out before their profile has finished loading. The save
     * must still happen, once, after the load resolves -- not be dropped, and
     * not leak the entry.
     */
    @Test
    void quitBeforeLoadCompletesStillSavesOnceTheLoadResolves() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        service.onQuit(player); // quits while the load is still in flight
        assertEquals(0, repo.saveCount.get(), "nothing to save yet");
        assertEquals(0, service.trackedPlayers(), "entry removed immediately");

        // The read finally lands.
        repo.pendingLoad.complete(Optional.of(
                new PlayerProfile(1, player, "hunter", 9, 500, List.of(), 1L)));

        assertEquals(1, repo.saveCount.get());
        assertEquals(9, repo.saved.get(player).level());
        assertEquals(4242L, repo.saved.get(player).lastSeenEpochMillis());
    }

    /**
     * The dangerous case. A corrupt or too-new profile must never be replaced by
     * a fresh one on quit -- that would silently destroy the player's progress.
     */
    @Test
    void aFailedLoadNeverOverwritesTheStoredProfile() {
        repo.pendingLoad = CompletableFuture.failedFuture(
                new IllegalStateException("schema version 999"));

        service.onJoin(player);
        assertTrue(service.profile(player).isEmpty(), "a failed load exposes no profile");

        service.onQuit(player);

        assertEquals(0, repo.saveCount.get(), "must not write over the file it could not read");
    }

    @Test
    void profileIsAbsentWhileTheLoadIsStillInFlight() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        assertTrue(service.profile(player).isEmpty());

        repo.pendingLoad.complete(Optional.empty());
        assertTrue(service.profile(player).isPresent());
    }

    @Test
    void saveAllAndClearFlushesEveryoneOnline() {
        var second = UUID.randomUUID();
        service.onJoin(player);
        service.onJoin(second);

        service.saveAllAndClear().join();

        assertEquals(2, repo.saveCount.get());
        assertEquals(0, service.trackedPlayers());
        assertEquals(4242L, repo.saved.get(player).lastSeenEpochMillis());
        assertEquals(4242L, repo.saved.get(second).lastSeenEpochMillis());
    }

    @Test
    void saveAllAndClearOnAnEmptyServerCompletes() {
        assertDoesNotThrow(() -> service.saveAllAndClear().join());
    }
}
