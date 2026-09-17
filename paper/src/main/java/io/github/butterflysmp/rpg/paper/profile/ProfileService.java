package io.github.butterflysmp.rpg.paper.profile;

import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.github.butterflysmp.rpg.storage.PlayerRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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

    /**
     * WHY a read or a write was refused -- the distinction {@link #profile} throws away.
     *
     * <h2>TWO OF THESE ARE THE SAME EMPTY OPTIONAL AND THE PLAYER MUST BE TOLD DIFFERENT THINGS</h2>
     *
     * {@code profile()} answers {@code Optional.empty()} for three situations and every caller has
     * had to guess which; they all guessed <b>LOADING</b>, and say <i>"try again in a moment"</i>.
     *
     * <p><b>For {@link #UNREADABLE} that sentence is a lie.</b> The load already finished and
     * failed. Nothing will ever complete, so "in a moment" never arrives, and the player retries
     * until they give up and report it as the feature being broken. It is the difference between
     * <i>wait</i> and <i>this will not work today</i>, and only this type can carry it.
     *
     * <p><b>Note what is NOT here: a fresh player.</b> {@link #onJoin} maps a missing file to
     * {@link PlayerProfile#fresh}, so someone with no saved data is {@link #READY} carrying
     * defaults. "No file" is not a failure and never reaches these arms.
     */
    public enum Availability {
        /** Loaded, present, writable. */
        READY,
        /** The disk read is still in flight. TRANSIENT -- retrying genuinely works. */
        LOADING,
        /** The read finished and failed: corrupt, or from a newer server. PERMANENT this session. */
        UNREADABLE,
        /** Not tracked at all -- never joined, or already quit. */
        UNTRACKED
    }

    /**
     * What every surface says while a profile is still on its way.
     *
     * <p>ONE constant because FOUR command sites and the settings screen say it. It was four copies
     * of a literal before the settings screen would have made a fifth.
     */
    public static final String STILL_LOADING = "Your profile is still loading -- try again in a moment.";

    /**
     * What every surface says when the profile cannot be read at all.
     *
     * <p><b>It deliberately does NOT say "try again".</b> This is the permanent arm: the read
     * finished and failed, so a retry is the one thing that cannot help. It names the consequence
     * the player can act on -- their settings will not stick -- and rejoining is the only thing
     * that re-attempts the read.
     */
    public static final String UNREADABLE_PROFILE =
            "Your profile could not be read, so changes cannot be saved. Try rejoining.";

    /**
     * Why a read or write would be refused right now, without performing one.
     *
     * <p>The three refusal arms are the same triple {@link #profile} and {@link #setKit} guard with;
     * this returns WHICH rather than collapsing them.
     */
    public Availability availability(UUID playerId) {
        CompletableFuture<PlayerProfile> loading = profiles.get(playerId);
        if (loading == null) return Availability.UNTRACKED;
        if (!loading.isDone()) return Availability.LOADING;
        if (loading.isCompletedExceptionally()) return Availability.UNREADABLE;
        return loading.getNow(null) == null ? Availability.UNREADABLE : Availability.READY;
    }

    /**
     * Move this player's Nexus star to a different hotbar slot, and persist it.
     *
     * <h2>THE SECOND WRITER, AND IT IS THE FIRST ONE A MENU DRIVES</h2>
     *
     * Modelled on {@link #setKit}, deliberately, down to the guard and the cache replacement: the
     * cached future is swapped so the very next read -- which is {@code NexusSlots.lockedSlotOf},
     * a few lines later on the click -- sees the new slot rather than the old one.
     *
     * <p><b>UNVALIDATED HERE.</b> The hotbar bound lives at {@code NexusSlots.validSlotOr} and the
     * caller applies it; this class knows nothing about inventories, the same way
     * {@code PlayerProfile.withNexusSlot} does not.
     *
     * <p>Touches no Bukkit API, per the class rule. <b>Re-placing the star is the caller's job</b>,
     * on the caller's thread, after this returns -- exactly as minting weapons is
     * {@code applyKit}'s job after {@code setKit}.
     *
     * @return false if the profile is not loaded or could not be read. <b>The caller must say WHICH
     *         using {@link #availability}</b> -- "try again in a moment" is wrong for the
     *         unreadable arm and that is the whole reason that enum exists.
     */
    public boolean setNexusSlot(UUID playerId, int slot) {
        CompletableFuture<PlayerProfile> loading = profiles.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return false;
        }
        PlayerProfile current = loading.getNow(null);
        if (current == null) return false;

        PlayerProfile updated = current.withNexusSlot(slot);
        profiles.put(playerId, CompletableFuture.completedFuture(updated));
        repository.save(updated).exceptionally(error -> {
            log.log(Level.SEVERE, "Failed to persist Nexus slot change for " + playerId, error);
            return null;
        });
        return true;
    }

    /**
     * Add earned XP to a player's lifetime total, and say whether that crossed a level boundary.
     *
     * <p>Called from the {@code PlayerExpChangeEvent} handler in {@code RpgListeners}, on the main
     * thread, once per orb picked up. Touches no Bukkit API, like everything else here.
     *
     * <h2>*** THIS IS THE ONLY WRITER OF {@code lifetimeXp}, AND IT ONLY EVER ADDS. ***</h2>
     *
     * The monotonicity {@code PlayerLevel} relies on is a property of this method, not of the
     * record -- {@code PlayerProfile.withLifetimeXp} would happily take a smaller number.
     * <b>A negative or zero amount is ignored</b> rather than refused: {@code setAmount(-1)} from
     * another plugin is a thing that can happen, and losing progression to it is worse than
     * dropping it on the floor.
     *
     * <h2>WHEN IT WRITES TO DISK, AND THE COST OF THE ANSWER</h2>
     *
     * <b>Every gain updates the cache; only a LEVEL CHANGE writes to disk.</b>
     *
     * <p>{@link #setNexusSlot} writes through on every call and is right to -- it is a deliberate,
     * rare choice. <b>This fires once per orb</b>, so the same policy would be a file write per orb
     * per player, which is the shape of thing this project is told to treat as a forty-player
     * problem rather than a one-player one.
     *
     * <p>The cache is persisted by {@link #onQuit} and {@link #saveAllAndClear}, exactly as
     * {@code lastSeenEpochMillis} always has been. <b>The accepted cost, said out loud: a server
     * that dies without running either loses whatever has been earned since the last level-up.</b>
     *
     * <p><b>THE BOUND IS ONE RUNG OF THE CURVE, AND NEAR THE CAP THAT IS NOT SMALL.</b> An earlier
     * draft of this sentence said <i>"bounded by one level, never by a whole session"</i>, and the
     * second half is <b>false</b>: rung 98 is <b>400,000 XP</b>, which can span several sessions of
     * play. <b>The write-on-level-change bounds the loss in LEVELS, not in TIME</b>, and the two
     * diverge exactly where a player has the most to lose.
     *
     * <p>It is still the right trade against a file write per orb per player -- but the cost is
     * stated honestly so the next person can re-rule it rather than inheriting a comfortable
     * number.
     *
     * @return the new lifetime total, or empty if the profile is not loaded or could not be read.
     *         <b>Empty is not an error the caller should report</b>: an orb picked up during the
     *         join-tick load window is a normal event, and the alternative is a chat line nobody
     *         can act on.
     */
    public OptionalLong addLifetimeXp(UUID playerId, long amount) {
        if (amount <= 0) return OptionalLong.empty();

        CompletableFuture<PlayerProfile> loading = profiles.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return OptionalLong.empty();
        }
        PlayerProfile current = loading.getNow(null);
        if (current == null) return OptionalLong.empty();

        long before = current.lifetimeXp();
        long after = PlayerLevel.plus(before, amount);

        PlayerProfile updated = current.withLifetimeXp(after);
        profiles.put(playerId, CompletableFuture.completedFuture(updated));

        if (PlayerLevel.levelFor(before) != PlayerLevel.levelFor(after)) {
            repository.save(updated).exceptionally(error -> {
                log.log(Level.SEVERE, "Failed to persist level-up for " + playerId, error);
                return null;
            });
        }
        return OptionalLong.of(after);
    }

    /**
     * Set a player's lifetime XP to an absolute value. <b>Operator tooling only.</b>
     *
     * <h2>*** THIS IS THE ONE PLACE THE MONOTONIC INVARIANT CAN BE BROKEN, AND IT IS DELIBERATE ***</h2>
     *
     * {@code PlayerLevel} states that lifetime XP never decreases, and every other writer obeys it:
     * {@link #addLifetimeXp} only adds, the enchant table and the grindstone cannot reach the field
     * at all, and {@code PlayerExpChangeEvent} only ever carries an earned amount.
     * <b>{@code /rpg playerxp set} can lower it, on purpose.</b>
     *
     * <p><b>Read that as an OPERATOR EXCEPTION, not as the invariant being false.</b> The rule is
     * what the GAME does; this is a hand on the dial. A reader who finds the invariant asserted in
     * {@code PlayerLevel} and contradicted here has found this sentence, which is the point --
     * without it the natural move is to "fix" one of the two, and either fix is wrong.
     *
     * <p><b>Writes through on every call</b>, unlike {@link #addLifetimeXp}. A command is rare and
     * deliberate, which is exactly {@link #setNexusSlot}'s argument; and an operator staging a gate
     * row wants the value on disk before they do anything else with it.
     *
     * <p>Negative input is floored at zero -- below zero is not a lower level, it is the same
     * level 1 with a number that reads as corrupt to the next person who opens the file.
     *
     * @return false if the profile is not loaded or could not be read, so the command can say so.
     */
    public boolean setLifetimeXp(UUID playerId, long lifetimeXp) {
        CompletableFuture<PlayerProfile> loading = profiles.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return false;
        }
        PlayerProfile current = loading.getNow(null);
        if (current == null) return false;

        PlayerProfile updated = current.withLifetimeXp(Math.max(0L, lifetimeXp));
        profiles.put(playerId, CompletableFuture.completedFuture(updated));
        repository.save(updated).exceptionally(error -> {
            log.log(Level.SEVERE, "Failed to persist lifetime XP change for " + playerId, error);
            return null;
        });
        return true;
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
