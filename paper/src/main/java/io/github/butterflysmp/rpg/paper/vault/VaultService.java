package io.github.butterflysmp.rpg.paper.vault;

import io.github.butterflysmp.rpg.core.vault.VaultShape;
import io.github.butterflysmp.rpg.storage.PlayerVault;
import io.github.butterflysmp.rpg.storage.VaultRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the vault of every player currently on this server, and writes it back on every change.
 *
 * <p>Modelled on {@code ProfileService} down to the cache shape, deliberately: a player is keyed to
 * the FUTURE of their vault, not the vault itself, because a player can quit before the load
 * completes. That is what stops a fast rejoin racing, and what stops a failed load being mistaken
 * for "no vault" and overwriting a good file with an empty one.
 *
 * <p>Every callback in here runs on the repository's I/O thread. <b>NOTHING here may touch the
 * Bukkit API</b> -- the same rule as {@code ProfileService}, for the same reason.
 *
 * <h2>*** THIS CLASS NEVER SEES AN ItemStack, AND THAT IS THE SEAM RATHER THAN AN OMISSION ***</h2>
 *
 * Every method here takes and returns <b>encoded text</b>. Turning an {@code ItemStack} into that
 * text needs {@code Bukkit.getUnsafe()}, which is main-thread work and untestable without a server,
 * so it happens in {@link VaultCodec} on the thread that owns the inventory and only the string
 * crosses. That is {@code CombatantSnapshot}'s rule applied to items: <b>capture a value on the
 * thread that owns it, then hop.</b>
 *
 * <h2>*** WRITE-THROUGH ON EVERY MUTATION. THIS IS THE BRIEFER'S DECISION, NOT BEN'S. ***</h2>
 *
 * Flagged because the project already has the opposite policy sitting next door, and a reader who
 * finds two answers to one question deserves to know which is whose:
 *
 * <pre>
 *   ProfileService.addLifetimeXp   writes only on a LEVEL CHANGE   Ben's ruling
 *   VaultService.writePage         writes on EVERY mutation        the briefer's
 * </pre>
 *
 * <b>The reason they differ is what a crash costs.</b> A crash between XP writes costs PROGRESS,
 * which is re-earnable and bounded by one rung of the curve. A crash between vault writes costs
 * ITEMS: players notice, they report it, and they cannot get them back. 252 stacks is a small file
 * and a page is smaller; the write is cheap and the alternative is not.
 *
 * <p><b>Overrulable.</b> If this is ever judged too chatty, the thing to change is the batching, and
 * the rule that must not move with it is <i>never batch a write across a page change</i> -- a page
 * flip discards the live slots, so an unwritten page at that instant is a lost page.
 */
public final class VaultService {

    private final VaultRepository repository;
    private final Logger log;
    private final Map<UUID, CompletableFuture<PlayerVault>> vaults = new ConcurrentHashMap<>();

    public VaultService(VaultRepository repository, Logger log) {
        this.repository = repository;
        this.log = log;
    }

    /**
     * Start this player's vault loading.
     *
     * <p>A missing file becomes {@link PlayerVault#empty}, because an absent vault and an empty
     * vault are the same thing and there is nothing to disambiguate. <b>An unreadable one does
     * NOT</b> -- it completes exceptionally and stays that way for the session, so every write is
     * refused and the bad file is left exactly as it is for someone to look at.
     */
    public void onJoin(UUID playerId) {
        CompletableFuture<PlayerVault> loading = repository.load(playerId)
                .thenApply(found -> found.orElseGet(() -> PlayerVault.empty(playerId)));

        loading.exceptionally(error -> {
            // A corrupt file, or one from a newer server. Leave it alone: the player has no vault
            // this session, every write is refused, and nothing saves over it.
            log.log(Level.SEVERE, "Failed to load vault for " + playerId
                    + "; their vault will not be touched this session", error);
            return null;
        });

        vaults.put(playerId, loading);
    }

    /**
     * Drop this player's cached vault. <b>Deliberately does NOT save.</b>
     *
     * <h2>*** THE ABSENT SAVE IS THE POINT, NOT AN OVERSIGHT ***</h2>
     *
     * {@code ProfileService.onQuit} saves, because the profile is written on a level change and a
     * session's worth of XP may be unwritten. Write-through means a vault on disk is already current
     * at the instant of every close, so there is nothing here to flush.
     *
     * <p><b>A save here would be worse than redundant: it would MASK a broken write-through.</b>
     * With it, a build whose per-mutation write silently did nothing would still look correct on
     * every ordinary quit, and would lose a page only on a crash -- the one path nobody tests. Not
     * saving is what makes the write-through falsifiable.
     *
     * <p>An in-flight write is unaffected. The repository's future is independent of this cache and
     * the I/O executor finishes what it was given.
     */
    public void onQuit(UUID playerId) {
        vaults.remove(playerId);
    }

    /**
     * Run something once this player's vault has SETTLED -- succeeded or failed -- and never before.
     *
     * <p>Exists for the reason {@code ProfileService.whenSettled} does: a consumer running on the
     * join tick would otherwise read "not loaded" essentially always and take it for "empty".
     *
     * <p><b>THREADING: the action runs on the repository's I/O thread</b>, or on the calling thread
     * if the load has already finished. Either way <b>it must not touch the Bukkit API</b>; a caller
     * that needs to hops with {@code Scheduler.onEntity} inside its own action and re-checks that
     * the player is still online, because a load can settle after they have left.
     */
    public void whenSettled(UUID playerId, Consumer<Optional<PlayerVault>> action) {
        CompletableFuture<PlayerVault> loading = vaults.get(playerId);
        if (loading == null) {
            action.accept(Optional.empty());
            return;
        }
        loading.whenComplete((vault, error) ->
                action.accept(error == null ? Optional.ofNullable(vault) : Optional.empty()));
    }

    /**
     * This player's vault, if it is loaded and readable.
     *
     * <p><b>Empty collapses three situations</b> -- never tracked, still loading, and unreadable --
     * exactly as {@code ProfileService.profile} does. A caller that must tell them apart at the join
     * tick uses {@link #whenSettled}.
     */
    public Optional<PlayerVault> vault(UUID playerId) {
        CompletableFuture<PlayerVault> loading = vaults.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return Optional.empty();
        }
        return Optional.ofNullable(loading.getNow(null));
    }

    /**
     * Replace one page wholesale and write the whole vault to disk.
     *
     * <p>The cache is swapped FIRST so the very next read sees the new contents, then the save is
     * issued -- the same ordering as {@code ProfileService.setNexusSlot}, and for the same reason.
     *
     * <p>Wholesale rather than per-slot because the caller is handing over a snapshot of thirty-six
     * live inventory cells. A per-slot API would invite writing only the cell that changed, which is
     * right until a gesture moves two and a drag moves nine.
     *
     * <h2>OWED TO PR 2, NAMED HERE RATHER THAN PRE-BUILT</h2>
     *
     * This reports a SYNCHRONOUS refusal (the vault is not loaded) and logs an ASYNCHRONOUS failure
     * (the disk write threw). <b>The screen will need the second one surfaced</b>, so that a failed
     * write can put the menu into a degraded state that hands the page back to the player instead of
     * holding items it has not persisted. That hook is not written here because PR 1 has no caller
     * for it, and a guard nothing exercises is a guard nobody maintains.
     *
     * @param contents slot index to encoded item text. Empty and blank values are dropped; an empty
     *                 map clears the page, which is how the last item leaves it.
     * @return false if the vault is not loaded or could not be read -- in which case <b>nothing was
     *         written and nothing was cached</b>, so the caller still holds the only copy.
     */
    public boolean writePage(UUID playerId, int page, Map<Integer, String> contents) {
        VaultShape.requirePage(page);

        CompletableFuture<PlayerVault> loading = vaults.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return false;
        }
        PlayerVault current = loading.getNow(null);
        if (current == null) return false;

        PlayerVault updated = current.withPage(page, contents);
        vaults.put(playerId, CompletableFuture.completedFuture(updated));
        repository.save(updated).exceptionally(error -> {
            log.log(Level.SEVERE, "Failed to persist vault page " + page + " for " + playerId
                    + "; the items in that page are NOT on disk", error);
            return null;
        });
        return true;
    }

    /**
     * Flush everyone still online, for {@code onDisable}.
     *
     * <h2>WITH WRITE-THROUGH THIS SHOULD HAVE NOTHING TO DO, AND IT IS STILL NOT OPTIONAL</h2>
     *
     * Every page is already on disk, so this writes contents identical to what is there. It exists
     * for the case write-through cannot cover: a vault that was <b>loaded and never written</b> is
     * not in danger, but a vault whose last write is still <b>queued</b> on the I/O executor is --
     * and the future returned here is what {@code onDisable} waits on before the executor is shut
     * down.
     *
     * <p><b>So the guarantee is about DRAINING, not about content.</b> Said explicitly because
     * "write-through means the shutdown flush is redundant" is a true-sounding sentence that would
     * justify deleting the one thing making the last write survive a stop.
     */
    public CompletableFuture<Void> saveAllAndClear() {
        CompletableFuture<?>[] pending = vaults.keySet().stream()
                .toList().stream()
                .map(id -> {
                    CompletableFuture<PlayerVault> loading = vaults.remove(id);
                    if (loading == null) return CompletableFuture.completedFuture(null);
                    return loading
                            .thenCompose(repository::save)
                            .exceptionally(error -> {
                                log.log(Level.SEVERE, "Failed to save vault for " + id
                                        + " during shutdown", error);
                                return null;
                            });
                })
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pending);
    }

    /** Who this service is currently holding a vault for. For the dev read-back and for tests. */
    public java.util.Set<UUID> trackedPlayers() {
        return java.util.Set.copyOf(vaults.keySet());
    }
}
