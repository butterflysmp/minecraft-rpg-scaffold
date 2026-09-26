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
        repo.saved.put(player,
                new PlayerProfile(1, player, "hunter", "none", 9, 500, List.of("x"), 1L, 3, 4_200L, null, false));

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
                new PlayerProfile(1, player, "hunter", "none", 9, 500, List.of(), 1L, 3, 4_200L, null, false)));

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
                "hunter", "fire", 9, 500, List.of(), 1L, 3, 4_200L, null, false));
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

    // --- availability: the distinction profile()'s Optional throws away ---

    /**
     * THE FOUR ARMS, AND THE ONE PEOPLE ASSUME WRONG.
     *
     * <p>Three of them are the same empty {@code Optional} from {@code profile()}, and every caller
     * had to guess which -- they all guessed LOADING and said "try again in a moment". For
     * UNREADABLE that is a lie, and this enum is what lets a surface stop telling it.
     */
    @Test
    void availabilityTELLSTheThreeRefusalsAPART_whichProfileCannot() {
        assertEquals(ProfileService.Availability.UNTRACKED, service.availability(player),
                "never joined");
        assertTrue(service.profile(player).isEmpty(), "and profile() cannot tell you that");

        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);
        assertEquals(ProfileService.Availability.LOADING, service.availability(player),
                "in flight -- TRANSIENT, and retrying genuinely works");
        assertTrue(service.profile(player).isEmpty(), "same empty Optional as the other two");

        repo.pendingLoad.completeExceptionally(new IllegalStateException("schema version 999"));
        assertEquals(ProfileService.Availability.UNREADABLE, service.availability(player),
                "failed -- PERMANENT this session, so 'try again in a moment' would be a lie");
        assertTrue(service.profile(player).isEmpty(), "and still the same empty Optional");
    }

    /**
     * A player with NO stored file is READY, not any flavour of refusal. This is the arm that is
     * assumed wrong: onJoin maps a missing file to PlayerProfile.fresh, so "new player" is a
     * PRESENT profile carrying defaults.
     */
    @Test
    void aPlayerWithNoFileIsREADY_becauseFreshIsAProfile() {
        service.onJoin(player);
        assertEquals(ProfileService.Availability.READY, service.availability(player));
        assertEquals(PlayerProfile.DEFAULT_NEXUS_SLOT,
                service.profile(player).orElseThrow().nexusSlot());
    }

    @Test
    void theTwoMessagesDIFFER_andOnlyOneOffersARetry() {
        // The whole point of carrying two strings. If these ever collapse into one, the distinction
        // above becomes decorative and the unreadable player is told to wait again.
        assertNotEquals(ProfileService.STILL_LOADING, ProfileService.UNREADABLE_PROFILE);
        assertTrue(ProfileService.STILL_LOADING.contains("try again in a moment"),
                "the transient arm offers the retry that works");
        assertFalse(ProfileService.UNREADABLE_PROFILE.contains("in a moment"),
                "and the PERMANENT arm must not, because nothing is still happening");
    }

    // --- setNexusSlot: the second writer, and the first one a menu drives ---

    @Test
    void setNexusSlotWritesThroughAndPersistsOnce() {
        service.onJoin(player);
        assertEquals(PlayerProfile.DEFAULT_NEXUS_SLOT,
                service.profile(player).orElseThrow().nexusSlot(), "starts at the default");

        assertTrue(service.setNexusSlot(player, 3));

        assertEquals(3, service.profile(player).orElseThrow().nexusSlot(),
                "THE CACHED FUTURE IS REPLACED -- the lock reads this on the player's very next "
                        + "click, so a write that only reached disk would leave the guard stale");
        assertEquals(3, repo.saved.get(player).nexusSlot(), "and it reached the repository");
        assertEquals(1, repo.saveCount.get(), "exactly once");
    }

    @Test
    void setNexusSlotCARRIESEverythingElseOnTheProfile() {
        repo.saved.put(player, new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION, player,
                "ranger", "fire", 9, 500, List.of("arc_surge"), 1L, 8, 4_200L, null, false));
        service.onJoin(player);

        assertTrue(service.setNexusSlot(player, 0));

        var profile = service.profile(player).orElseThrow();
        assertEquals(0, profile.nexusSlot(), "slot 0 is a legal choice");
        assertEquals("ranger", profile.archetypeId(), "a slot change must not touch the kit");
        assertEquals("fire", profile.elementId());
        assertEquals(9, profile.level());
        assertEquals(500, profile.experience());
        assertEquals(List.of("arc_surge"), profile.unlockedAbilities());
    }

    @Test
    void setNexusSlotIsRefusedWhileTheProfileIsStillLoading() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        assertFalse(service.setNexusSlot(player, 3),
                "must not invent a profile out of an in-flight load");
        assertEquals(0, repo.saveCount.get());
        assertEquals(ProfileService.Availability.LOADING, service.availability(player),
                "and the caller can find out it was the TRANSIENT arm");
    }

    /**
     * THE ARM setKit (now setCell) HAD NEVER HAD A ROW FOR. Its guard includes isCompletedExceptionally(), and
     * nothing asserted that branch; this writer's does, because it is the arm the settings screen
     * must word differently.
     */
    @Test
    void setNexusSlotIsRefusedAfterAFAILEDLoad_andSaysSo() {
        repo.pendingLoad = CompletableFuture.failedFuture(
                new IllegalStateException("schema version 999"));
        service.onJoin(player);

        assertFalse(service.setNexusSlot(player, 3));
        assertEquals(0, repo.saveCount.get(),
                "MUST NOT WRITE OVER THE FILE IT COULD NOT READ -- the same invariant onQuit holds");
        assertEquals(ProfileService.Availability.UNREADABLE, service.availability(player),
                "and the caller can tell this is permanent rather than telling them to wait");
    }

    @Test
    void setNexusSlotIsRefusedForSomeoneWhoNeverJoined() {
        assertFalse(service.setNexusSlot(player, 3));
        assertEquals(0, repo.saveCount.get());
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

    // --- setCell: the (class, element) choice, and the retired grant it clears ---

    /**
     * Both identity axes land on the profile together and are persisted at once -- and NOTHING is
     * granted. Kits are gone (PLAN-build-system.md section 3.2): {@code unlockedAbilities} is retired, so a
     * cell change writes it EMPTY, even over a profile that still carried an old kit grant.
     */
    @Test
    void setCellSetsBothAxesPersistsOnceAndClearsTheRetiredGrant() {
        repo.saved.put(player, new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION, player, "mage", "fire", 1, 0,
                List.of("solar_grenade", "solar_lance"), 0L, 8, 0L, null, false));
        service.onJoin(player);
        assertEquals(List.of("solar_grenade", "solar_lance"),
                service.profile(player).orElseThrow().unlockedAbilities(), "the old grant loads as it was");

        boolean set = service.setCell(player, "ranger", "fire");

        assertTrue(set);
        var profile = service.profile(player).orElseThrow();
        assertEquals("ranger", profile.archetypeId());
        assertEquals("fire", profile.elementId());
        assertEquals(List.of(), profile.unlockedAbilities(), "RETIRED: a cell change grants nothing");
        assertEquals(1, repo.saveCount.get(), "the cell change must be persisted immediately");
        assertEquals(List.of(), repo.saved.get(player).unlockedAbilities());
        assertEquals("ranger", repo.saved.get(player).archetypeId());
    }

    @Test
    void setCellIsRefusedWhileTheProfileIsStillLoading() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        assertFalse(service.setCell(player, "ranger", "fire"),
                "must not invent a profile out of an in-flight load");
        assertEquals(0, repo.saveCount.get());
    }

    @Test
    void setCellIsRefusedForSomeoneWhoNeverJoined() {
        assertFalse(service.setCell(player, "ranger", "fire"));
        assertEquals(0, repo.saveCount.get());
    }

    // ---------------------------------------------------------------- lifetime XP
    //
    // The impure half of the progression hook. PlayerLevel owns the curve and is tested in core;
    // what is decidable here is ACCUMULATION and WHEN A DISK WRITE HAPPENS.

    @Test
    void xpACCUMULATESAcrossGains_andTheProfileCarriesTheRunningTotal() {
        service.onJoin(player);

        assertEquals(400L, service.addLifetimeXp(player, 400L).orElseThrow());
        assertEquals(550L, service.addLifetimeXp(player, 150L).orElseThrow(),
                "the second gain adds to the first rather than replacing it");
        assertEquals(550L, service.profile(player).orElseThrow().lifetimeXp());
        // Mutation MUTXP-REPLACE: `before + amount` -> `amount` -> kill set RECORDED in the PR body.
    }

    @Test
    void onlyALEVELCHANGEWritesToDisk_notEveryOrb() {
        // *** THE PERSISTENCE POLICY, AND IT IS THE ONE THING HERE THAT IS A JUDGEMENT CALL. ***
        // setNexusSlot writes through on every call; this fires once per orb, so it does not.
        // 1000 is the level-2 total, so the third gain below is the one that crosses.
        service.onJoin(player);
        int afterJoin = repo.saveCount.get();

        service.addLifetimeXp(player, 400L);
        service.addLifetimeXp(player, 400L);
        assertEquals(afterJoin, repo.saveCount.get(),
                "800 XP is still level 1 -- two orbs, no write");

        service.addLifetimeXp(player, 400L);
        assertEquals(afterJoin + 1, repo.saveCount.get(),
                "1200 crosses the level-2 boundary at 1000, and THAT is what gets persisted");
        assertEquals(1_200L, repo.saved.get(player).lifetimeXp(),
                "and the saved profile carries the whole total, not just the crossing gain");

        service.addLifetimeXp(player, 400L);
        assertEquals(afterJoin + 1, repo.saveCount.get(),
                "1600 is still level 2 -- back to no write");
        // Mutation MUTXP-ALWAYSSAVE: drop the level comparison and save unconditionally ->
        // kill set RECORDED in the PR body.
    }

    @Test
    void theCACHEIsUpdatedEvenWhenNoWriteHappens_whichIsWhatQuitPersists() {
        // WITHOUT THIS ROW THE ONE ABOVE IS SATISFIED BY A METHOD THAT DROPS SUB-LEVEL GAINS
        // ENTIRELY -- it counts writes, and zero writes is zero writes whether the XP was kept or
        // thrown away. This is the row that says the number survived.
        service.onJoin(player);
        service.addLifetimeXp(player, 800L);

        assertEquals(800L, service.profile(player).orElseThrow().lifetimeXp(),
                "held in the cache with no disk write");

        service.onQuit(player);

        assertEquals(800L, repo.saved.get(player).lifetimeXp(),
                "and quit is what puts it on disk -- the same path lastSeenEpochMillis uses");
    }

    @Test
    void aNonPositiveAmountIsIgnoredRatherThanApplied() {
        // setAmount(-1) from another plugin is reachable; losing progression to it is worse than
        // dropping it. Zero is refused too -- an empty return says "nothing happened" honestly.
        service.onJoin(player);
        service.addLifetimeXp(player, 500L);

        assertTrue(service.addLifetimeXp(player, -100L).isEmpty(), "negative is ignored");
        assertTrue(service.addLifetimeXp(player, 0L).isEmpty(), "and so is zero");
        assertEquals(500L, service.profile(player).orElseThrow().lifetimeXp(),
                "the total did not move in either direction");
    }

    @Test
    void xpIsDroppedWhileTheProfileIsStillLoading_andThatIsNotAnError() {
        // An orb picked up on the join tick, before the disk read settles. Refused the same way
        // setNexusSlot and setCell are -- the alternative is inventing a profile over an in-flight
        // load, which is the race onJoin's future-keying exists to prevent.
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);

        assertTrue(service.addLifetimeXp(player, 500L).isEmpty());
        assertEquals(0, repo.saveCount.get(), "and nothing was written");
    }

    @Test
    void xpIsDroppedForSomeoneWhoNeverJoined() {
        assertTrue(service.addLifetimeXp(player, 500L).isEmpty());
        assertEquals(0, repo.saveCount.get());
    }

    @Test
    void aLoadedProfilesSTOREDTotalIsTheBaseAndIsNotOverwritten() {
        // 4_200L is what this file's fixtures carry. A gain must ADD TO the stored number, not
        // start from zero -- which is the failure that would look correct for a fresh player and
        // wipe every returning one.
        repo.saved.put(player, new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION, player,
                "ranger", "fire", 9, 500, List.of(), 1L, 8, 4_200L, null, false));
        service.onJoin(player);

        assertEquals(4_700L, service.addLifetimeXp(player, 500L).orElseThrow(),
                "4200 stored plus 500 earned");
    }

    @Test
    void theTotalSATURATESRatherThanWrappingNegative() {
        // Unreachable in play -- the level-99 total is 11,642,250 -- but a hand-edited profile
        // reaches this method, and a wrap would read back as level 1, which is the worst possible
        // failure for a progression number: total loss that looks like a new player.
        repo.saved.put(player, new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION, player,
                "ranger", "fire", 9, 500, List.of(), 1L, 8, Long.MAX_VALUE - 5L, null, false));
        service.onJoin(player);

        assertEquals(Long.MAX_VALUE, service.addLifetimeXp(player, 1_000L).orElseThrow(),
                "clamped at the top, never wrapped");
    }

    // ------------------------------------------------------------ the Nexus star toggle (slice 10)

    @Test
    void anUNTOUCHEDToggleReadsENABLED_whichEveryExistingProfileIs() {
        service.onJoin(player);

        assertTrue(service.starEnabled(player), "a fresh profile has the star ON");
        assertNull(service.profile(player).orElseThrow().starEnabledOrNull(),
                "and it is NULL rather than TRUE -- the key is absent, which is what keeps a new "
                        + "file the same shape as every existing one");
        // Mutation MUTSTAR-DEFAULTFALSE: accessor returns `starEnabledOrNull != null &&
        // starEnabledOrNull` -> kill set RECORDED in the PR body.
    }

    @Test
    void theToggleWritesTHROUGHOnEveryCall_unlikeXp() {
        // A SETTING IS RARE AND DELIBERATE -- setNexusSlot's argument. The value must be on disk
        // before the player quits, or the star comes back on their next join and the toggle reads
        // as not sticking. Contrast onlyALEVELCHANGEWritesToDisk above, which is the other policy
        // and is right for its own reason.
        service.onJoin(player);
        int afterJoin = repo.saveCount.get();

        assertTrue(service.setStarEnabled(player, false));
        assertEquals(afterJoin + 1, repo.saveCount.get(), "off is persisted immediately");
        assertFalse(repo.saved.get(player).starEnabled(), "and it is the OFF value that landed");

        assertTrue(service.setStarEnabled(player, true));
        assertEquals(afterJoin + 2, repo.saveCount.get(), "and so is on");
        assertTrue(repo.saved.get(player).starEnabled());
        // Mutation MUTSTAR-NOSAVE: drop the repository.save call -> kill set RECORDED in the PR body.
    }

    @Test
    void theToggleSurvivesAReloadInBOTHDirections() {
        // BOTH DIRECTIONS, because `false` and "absent" are different values that an accessor bug
        // would collapse into one. Asserting only the OFF case passes on a build where everything
        // reads false; asserting only ON passes on a build where everything reads true.
        service.onJoin(player);
        service.setStarEnabled(player, false);
        service.onQuit(player);
        service.onJoin(player);
        assertFalse(service.starEnabled(player), "a deliberate OFF survives a rejoin");

        service.setStarEnabled(player, true);
        service.onQuit(player);
        service.onJoin(player);
        assertTrue(service.starEnabled(player), "and so does switching it back ON");
    }

    @Test
    void anUNAVAILABLEProfileReadsENABLED_theDirectionThatCannotSTRIPAStar() {
        // *** THE DEFAULT LEANS ON PURPOSE AND THE TWO ERRORS ARE NOT SYMMETRIC. ***
        // Reading unknown as OFF would strip the star from every player whose profile failed to
        // load -- silently, on the join path, where nobody is watching, and indistinguishable from
        // the feature being broken. The mirror risk is self-correcting: someone who HAS turned it
        // off sees it briefly return and it goes again on the next good read.
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);
        assertTrue(service.starEnabled(player), "still loading reads ENABLED");

        assertTrue(service.starEnabled(UUID.randomUUID()), "and so does never tracked");
        // Mutation MUTSTAR-UNKNOWNOFF: orElse(false) -> kill set RECORDED in the PR body.
    }

    @Test
    void theToggleIsRefusedWhileLoadingAndForSomeoneWhoNeverJoined() {
        repo.pendingLoad = new CompletableFuture<>();
        service.onJoin(player);
        assertFalse(service.setStarEnabled(player, false),
                "must not invent a profile out of an in-flight load");
        assertEquals(0, repo.saveCount.get());

        assertFalse(service.setStarEnabled(UUID.randomUUID(), false));
    }

    @Test
    void theToggleLeavesTheCHOSENSlotAlone_soTurningItBackOnRestoresTheSamePlace() {
        // The two settings are independent and the picker's javadoc says so: a player switching the
        // star off has said nothing about WHERE. If the toggle cleared the slot, re-enabling would
        // silently relocate their star to the default.
        repo.saved.put(player, new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION, player,
                "ranger", "fire", 9, 500, List.of(), 1L, 17, 4_200L, null, false));
        service.onJoin(player);

        service.setStarEnabled(player, false);
        assertEquals(17, service.profile(player).orElseThrow().nexusSlot(),
                "the chosen slot survives being switched off");
        service.setStarEnabled(player, true);
        assertEquals(17, service.profile(player).orElseThrow().nexusSlot(),
                "and survives being switched back on");
        assertEquals(4_200L, service.profile(player).orElseThrow().lifetimeXp(),
                "and the toggle touches no other field either");
    }
}
