package io.github.butterflysmp.rpg.paper.profile;

import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.github.butterflysmp.rpg.storage.PlayerRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the profile of every player currently on this server, and pushes it back
 * to the repository when they leave.
 *
 * Every callback in here runs on the repository's I/O thread. NOTHING here may
 * touch the Bukkit API -- that is the same rule as the PacketEvents contract,
 * for the same reason. (setKit is the one method called FROM the command
 * thread instead; it still touches no Bukkit API, so the rule holds either way.)
 *
 * A player is keyed to the *future* of their profile, not the profile itself,
 * because a player can quit before the load completes. Chaining the save onto
 * the load future means a fast rejoin cannot race, and a failed load can never
 * be mistaken for "no data" and overwrite a good file with a fresh profile.
 */
public final class ProfileService {

    private final PlayerRepository repository;
    private final Logger log;
    private final LongSupplier clock;
    private final Map<UUID, CompletableFuture<PlayerProfile>> profiles = new ConcurrentHashMap<>();

    public ProfileService(PlayerRepository repository, Logger log, LongSupplier clock) {
        this.repository = repository;
        this.log = log;
        this.clock = clock;
    }

    public void onJoin(UUID playerId) {
        CompletableFuture<PlayerProfile> loading = repository.load(playerId)
                .thenApply(found -> found.orElseGet(() -> PlayerProfile.fresh(playerId)));

        loading.exceptionally(error -> {
            // A corrupt file, or one from a newer server. Leave it alone: the
            // player has no profile this session, and quit() will not save over it.
            log.log(Level.SEVERE, "Failed to load profile for " + playerId
                    + "; their data will not be touched this session", error);
            return null;
        });

        profiles.put(playerId, loading);
    }

    public void onQuit(UUID playerId) {
        CompletableFuture<PlayerProfile> loading = profiles.remove(playerId);
        if (loading == null) return;

        loading.thenCompose(profile -> repository.save(profile.withLastSeen(clock.getAsLong())))
                .exceptionally(error -> {
                    log.log(Level.SEVERE, "Failed to save profile for " + playerId, error);
                    return null;
                });
    }

    /**
     * Run something once this player's load has SETTLED -- succeeded or failed -- and never before.
     *
     * <h2>WHY THIS EXISTS: {@link #profile} CANNOT BE ASKED AT JOIN TIME</h2>
     *
     * Every other caller reads {@code profile()} from a COMMAND, which a player types seconds after
     * joining, so the load has long since finished. <b>The Nexus is the first consumer that runs on
     * the join tick itself</b>, twelve lines after {@code onJoin} starts the disk read -- where
     * {@code profile()} returns empty essentially always, and a caller that treats that as "no
     * preference" silently gets the default for every player on every join.
     *
     * <p>That is not a corner case; it is the DEFAULT outcome of the obvious code. So the obvious
     * code is not available, and this is the alternative.
     *
     * <h2>EMPTY MEANS "NO STORED PREFERENCE, PERMANENTLY", NOT "NOT YET"</h2>
     *
     * <b>This is the whole point of settling.</b> {@code profile()}'s empty collapses three
     * situations -- never tracked, still loading, failed -- and a caller cannot tell the transient
     * one from the permanent ones. Here the transient case is gone by construction: the action does
     * not run until the future completes. What is left is permanent for this session, so a caller
     * may treat empty as a final answer and use its default.
     *
     * <p><b>A fresh player is NOT an empty case</b>, which is worth stating because it is the one
     * people assume. {@code onJoin} maps a missing file to {@link PlayerProfile#fresh}, so someone
     * with no saved data arrives here as a PRESENT profile carrying defaults. Empty here means the
     * file was unreadable, or the player is not tracked at all.
     *
     * <p><b>THREADING: the action runs on the repository's I/O thread</b> -- or on the calling
     * thread, if the load already finished. Either way the class rule applies and this method
     * cannot relax it: <b>the action must not touch the Bukkit API.</b> A caller that needs to
     * (the Nexus does -- it writes an inventory) must hop with
     * {@code Scheduler.onEntity} inside its own action, and check the player is still online,
     * because a load can settle after they have left.
     */
    public void whenSettled(UUID playerId, Consumer<Optional<PlayerProfile>> action) {
        CompletableFuture<PlayerProfile> loading = profiles.get(playerId);
        if (loading == null) {
            // Never tracked, or already quit. Settled by definition, and the answer is "nothing".
            action.accept(Optional.empty());
            return;
        }
        loading.whenComplete((profile, error) ->
                action.accept(error == null ? Optional.ofNullable(profile) : Optional.empty()));
    }

    /** The profile, if it has finished loading and did not fail. */
    public Optional<PlayerProfile> profile(UUID playerId) {
        CompletableFuture<PlayerProfile> loading = profiles.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return Optional.empty();
        }
        return Optional.ofNullable(loading.getNow(null));
    }

    /**
     * Pick a (class, element) cell: set both axes and the grant that the cell resolves to,
     * then persist. Class, element, and abilities are set together because they are
     * re-derived as one whenever either axis changes -- the caller passes the new pair and
     * the abilities it resolves to (the kit's, or empty when half-selected or unauthored),
     * so a stale class's abilities cannot outlive a class change.
     *
     * Called on the command thread (not the I/O thread), so it reads the cached profile
     * synchronously the way {@link #profile(UUID)} does. Touches no Bukkit API -- weapon
     * minting is the command's job, on the command thread, after this returns.
     *
     * @return false if the profile is not loaded yet or failed to load -- the caller
     *         should tell the player to try again rather than silently doing nothing.
     */
    public boolean setKit(UUID playerId, String classId, String elementId, List<String> unlockedAbilities) {
        CompletableFuture<PlayerProfile> loading = profiles.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return false;
        }
        PlayerProfile current = loading.getNow(null);
        if (current == null) return false;

        PlayerProfile updated = current.withKit(classId, elementId, unlockedAbilities);
        // Replace the cached future so the very next cast sees the new grant.
        profiles.put(playerId, CompletableFuture.completedFuture(updated));
        repository.save(updated).exceptionally(error -> {
            log.log(Level.SEVERE, "Failed to persist kit change for " + playerId, error);
            return null;
        });
        return true;
    }

    public int trackedPlayers() {
        return profiles.size();
    }

    /**
     * Flush everyone still online. Called from onDisable, where the server is
     * shutting down and PlayerQuitEvent is not guaranteed to fire for everyone.
     */
    public CompletableFuture<Void> saveAllAndClear() {
        CompletableFuture<?>[] pending = profiles.keySet().stream()
                .toList().stream()
                .map(id -> {
                    CompletableFuture<PlayerProfile> loading = profiles.remove(id);
                    if (loading == null) return CompletableFuture.completedFuture(null);
                    return loading
                            .thenCompose(p -> repository.save(p.withLastSeen(clock.getAsLong())))
                            .exceptionally(error -> {
                                log.log(Level.SEVERE, "Failed to save profile for " + id
                                        + " during shutdown", error);
                                return null;
                            });
                })
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pending);
    }
}
