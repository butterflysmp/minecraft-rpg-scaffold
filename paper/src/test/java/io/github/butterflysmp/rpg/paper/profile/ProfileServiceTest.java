package io.github.butterflysmp.rpg.paper.profile;

import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.github.butterflysmp.rpg.storage.PlayerRepository;
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
        repo.saved.put(player, new PlayerProfile(1, player, "hunter", "none", 9, 500, List.of("x"), 1L, 3));

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
                new PlayerProfile(1, player, "hunter", "none", 9, 500, List.of(), 1L, 3)));

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

    /**
     * THE JOIN RACE, AND THIS IS THE ONLY UNIT-TESTABLE HALF OF IT.
     *
     * <p>The Nexus needs the player's locked slot on the join tick, and {@code profile()} answers
     * empty until the disk read finishes -- so a caller using it gets the default for every player
     * on every join. This row stages that exact ordering: the load is held open, the action must
     * NOT have run, and it must run with the real profile once the load completes.
     *
     * <p>What it cannot cover is the scheduler hop and the inventory write in
     * {@code RpgListeners.onJoin}, which need a live server. {@code GATE-nexus.md} carries those.
     */
    @Test
    void whenSettledDoesNotRunUntilTheLoadCompletes_andThenSeesTheRealProfile() {
        repo.saved.put(player, new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION, player,
                "hunter", "fire", 9, 500, List.of(), 1L, 3));
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        var seen = new java.util.concurrent.atomic.AtomicReference<Optional<PlayerProfile>>();
        var ran = new java.util.concurrent.atomic.AtomicInteger();
        service.whenSettled(player, profile -> { ran.incrementAndGet(); seen.set(profile); });

        assertEquals(0, ran.get(),
                "IT MUST NOT RUN YET. This is the whole defect: profile() would have answered "
                        + "empty here and a caller would have taken the default");

        repo.pendingLoad.complete(Optional.of(repo.saved.get(player)));

        assertEquals(1, ran.get(), "exactly once, after the load settles");
        assertTrue(seen.get().isPresent(), "and with the profile, not an empty");
        assertEquals(3, seen.get().orElseThrow().nexusSlot(),
                "the STORED slot, not the default -- 3 is what this player chose");
    }

    /**
     * A failed load settles too, and empty there means PERMANENTLY no preference rather than
     * "not yet" -- which is what lets a caller use its default without racing anything.
     */
    @Test
    void whenSettledStillRunsWhenTheLoadFAILS_andReportsEmpty() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        var seen = new java.util.concurrent.atomic.AtomicReference<Optional<PlayerProfile>>();
        var ran = new java.util.concurrent.atomic.AtomicInteger();
        service.whenSettled(player, profile -> { ran.incrementAndGet(); seen.set(profile); });
        assertEquals(0, ran.get(), "not before it settles, failure included");

        repo.pendingLoad.completeExceptionally(new java.io.IOException("corrupt"));

        assertEquals(1, ran.get(),
                "A FAILED LOAD MUST STILL RUN THE ACTION. If it did not, a player whose file is "
                        + "corrupt would never get a star placed at all");
        assertTrue(seen.get().isEmpty(), "and reports empty, so the caller uses its default");
    }

    /** Someone who never joined is settled by definition: there is nothing to wait for. */
    @Test
    void whenSettledForSomeoneWhoNeverJoinedRunsImmediatelyWithEmpty() {
        var ran = new java.util.concurrent.atomic.AtomicInteger();
        service.whenSettled(UUID.randomUUID(), profile -> {
            ran.incrementAndGet();
            assertTrue(profile.isEmpty());
        });
        assertEquals(1, ran.get(), "must not hang waiting for a load that was never started");
    }

    /**
     * A player with NO stored file is NOT an empty case, and this is the one people assume wrong.
     * onJoin maps a missing file to PlayerProfile.fresh, so they settle as a PRESENT profile
     * carrying the default -- which is why empty can be read as "unreadable", not "new player".
     */
    @Test
    void whenSettledSeesAFRESHProfileForAPlayerWithNoFile_notAnEmpty() {
        service.onJoin(player);

        var seen = new java.util.concurrent.atomic.AtomicReference<Optional<PlayerProfile>>();
        service.whenSettled(player, seen::set);

        assertTrue(seen.get().isPresent(), "a brand-new player is present, not absent");
        assertEquals(PlayerProfile.DEFAULT_NEXUS_SLOT, seen.get().orElseThrow().nexusSlot(),
                "carrying the default slot");
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

    // --- setKit: the (class, element) -> castable-set resolution core cannot defend ---

    /**
     * The load-bearing paper-side test. core proves the gate works given a set; it is
     * structurally blind to paper handing it the wrong one. This is where that is
     * caught: a kit must grant EXACTLY the abilities it names, no more, no fewer, and
     * both identity axes must land on the profile together.
     */
    @Test
    void setKitGrantsExactlyTheNamedAbilitiesAndPersistsOnce() {
        service.onJoin(player);
        assertEquals("none", service.profile(player).orElseThrow().archetypeId());
        assertEquals("none", service.profile(player).orElseThrow().elementId());
        assertEquals(List.of(), service.profile(player).orElseThrow().unlockedAbilities());

        boolean set = service.setKit(player, "ranger", "fire",
                List.of("arc_surge", "solar_lance"));

        assertTrue(set);
        var profile = service.profile(player).orElseThrow();
        assertEquals("ranger", profile.archetypeId());
        assertEquals("fire", profile.elementId());
        assertEquals(List.of("arc_surge", "solar_lance"), profile.unlockedAbilities(),
                "the granted set must be exactly what the kit names -- not a superset, not empty");
        assertEquals(1, repo.saveCount.get(), "the kit change must be persisted immediately");
        assertEquals(List.of("arc_surge", "solar_lance"),
                repo.saved.get(player).unlockedAbilities());
    }

    @Test
    void setKitIsRefusedWhileTheProfileIsStillLoading() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        assertFalse(service.setKit(player, "ranger", "fire", List.of("arc_surge")),
                "must not invent a profile out of an in-flight load");
        assertEquals(0, repo.saveCount.get());
    }

    @Test
    void setKitIsRefusedForSomeoneWhoNeverJoined() {
        assertFalse(service.setKit(player, "ranger", "fire", List.of("arc_surge")));
        assertEquals(0, repo.saveCount.get());
    }
}
