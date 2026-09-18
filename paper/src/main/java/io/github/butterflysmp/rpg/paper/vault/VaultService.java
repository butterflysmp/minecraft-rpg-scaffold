package io.github.butterflysmp.rpg.paper.vault;

import io.github.butterflysmp.rpg.core.vault.VaultCell;
import io.github.butterflysmp.rpg.core.vault.VaultShape;
import io.github.butterflysmp.rpg.core.vault.VaultWriteFailure;
import io.github.butterflysmp.rpg.storage.PlayerVault;
import io.github.butterflysmp.rpg.storage.VaultRepository;

import java.util.LinkedHashMap;
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

    /**
     * The last vault this service has seen <b>actually reach disk</b>, per player.
     *
     * <h2>*** NOT THE CACHE. THE CACHE IS WHAT WE INTEND; THIS IS WHAT IS THERE. ***</h2>
     *
     * The two agree on every successful write and diverge on exactly one event: a write that
     * completes exceptionally. {@link #vaults} is swapped BEFORE the save is issued, so after a
     * failure it holds contents no file has.
     *
     * <p><b>It exists because the degraded close needs to hand back the DIFFERENCE.</b> Returning
     * the whole page would hand the player every stack on it while the file still holds its last
     * good copy of the same page -- a full-page duplicator, and a far larger one than the
     * single-cursor residual the design accepts as irreducible.
     *
     * <p>Seeded on LOAD, because what we loaded is by definition what was on disk.
     */
    private final Map<UUID, PlayerVault> persisted = new ConcurrentHashMap<>();

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

        // SEEDED HERE, and an absent file seeds an EMPTY vault rather than nothing. What we loaded
        // is by definition what is on disk, and "nothing is on disk" is the correct baseline for a
        // player whose file does not exist -- every cell they then fill is unpersisted until a write
        // succeeds, which is exactly what the degraded close needs to know.
        loading.thenAccept(vault -> persisted.put(playerId, vault));

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
        persisted.remove(playerId);
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
     * <h2>*** THE ASYNCHRONOUS FAILURE IS NOW SURFACED, AND PR 1's JAVADOC OWED IT ***</h2>
     *
     * Two failures live here and they are not the same event:
     *
     * <pre>
     *   SYNCHRONOUS   the vault is not loaded          -> returns false, NOTHING was cached
     *   ASYNCHRONOUS  the disk write threw             -> {@code onFailure}, and the vault is POISONED
     * </pre>
     *
     * <h2>*** WHY A FAILED WRITE POISONS THE CACHE, WHICH IS THE HALF THE FIRST DESIGN MISSED ***</h2>
     *
     * The cache is swapped <b>before</b> the save is issued, and {@code repository.save} writes the
     * <b>whole vault</b>. So after a failed write the cache is AHEAD of disk, and two ordinary
     * events then republish it:
     *
     * <ol>
     *   <li>any later successful write of <b>any other page</b> -- {@code withPage} carries every
     *       other page's entries forward, so the failed page rides along;</li>
     *   <li>{@link #saveAllAndClear} at {@code /stop}, which writes the cache.</li>
     * </ol>
     *
     * <b>Combine either with a degraded close that hands the page back and the failure path is a
     * duplicator</b> -- the fix for one arm building the other. Poisoning closes both: every later
     * write is refused, the shutdown flush skips this player, and the file on disk stays at its last
     * good state.
     *
     * <p>It is the same policy {@link #onJoin} already applies to an unreadable FILE -- <i>"stays
     * that way for the session, so every write is refused and nothing saves over it"</i> -- reached
     * from the write side rather than the read side.
     *
     * @param cells    slot index to cell. Empty and blank items are dropped; an empty map clears the
     *                 page, which is how the last item leaves it. The {@code description} on each
     *                 cell is never stored -- it exists so {@link VaultWriteFailure} can name what
     *                 did not reach disk in a form an operator can match.
     * @param onFailure run when the disk write completes EXCEPTIONALLY, <b>on the I/O thread</b>.
     *                 A caller that needs the Bukkit API hops inside it, and must re-check that the
     *                 player is still online. Never called for the synchronous refusal below, which
     *                 the {@code false} return already reports.
     * @return false if the vault is not loaded, unreadable or poisoned -- in which case <b>nothing
     *         was written and nothing was cached</b>, so the caller still holds the only copy.
     */
    public boolean writePage(UUID playerId, int page, Map<Integer, VaultCell> cells,
                             Runnable onFailure) {
        VaultShape.requirePage(page);

        CompletableFuture<PlayerVault> loading = vaults.get(playerId);
        if (loading == null || !loading.isDone() || loading.isCompletedExceptionally()) {
            return false;
        }
        PlayerVault current = loading.getNow(null);
        if (current == null) return false;

        // The stored shape is text. The descriptions stay here, in the failure closure below, and
        // never reach the record -- which is why VaultCell is a write-time type and VaultEntry is
        // the persisted one.
        Map<Integer, String> contents = new LinkedHashMap<>();
        for (Map.Entry<Integer, VaultCell> cell : cells.entrySet()) {
            if (cell.getValue() == null || cell.getValue().isEmpty()) continue;
            contents.put(cell.getKey(), cell.getValue().item());
        }

        PlayerVault updated = current.withPage(page, contents);
        vaults.put(playerId, CompletableFuture.completedFuture(updated));
        repository.save(updated).whenComplete((ignored, error) -> {
            if (error == null) {
                // IT REACHED DISK. This is the only place that may advance the persisted view, and
                // it is deliberately NOT beside the cache swap above -- the whole point of the two
                // maps is that one moves when we decide and the other when the disk agrees.
                persisted.put(playerId, updated);
                return;
            }
            // ORDER MATTERS: poison BEFORE the callback. The callback hops to the main thread and
            // may write, message or close a screen, and every one of those must find a vault that
            // already refuses writes.
            poison(playerId, error);
            log.log(Level.SEVERE, VaultWriteFailure.report(playerId, page, cells), error);
            onFailure.run();
        });
        return true;
    }

    /**
     * One page of the last vault that actually reached disk, or an empty map if none has.
     *
     * <h2>*** THIS IS WHAT A DEGRADED CLOSE SUBTRACTS, AND IT IS THE FIX FOR A FULL-PAGE DUPLICATOR ***</h2>
     *
     * A poisoned vault's file keeps its LAST GOOD copy of the page. A close that handed back every
     * cell would therefore give the player twenty stacks while the file still holds the same twenty
     * -- <b>a duplicator the size of the page</b>, not the single-cursor residual the design accepts
     * as irreducible.
     *
     * <p>So the screen hands back only the DIFFERENCE: the cells whose current contents are not in
     * this map. That is exactly the set that would otherwise be destroyed, and the minimum that has
     * to come back.
     *
     * <p><b>Empty means "nothing is known to be on disk", which is the SAFE direction</b> -- every
     * cell then counts as unpersisted and is handed back. Wrong that way costs a duplicate; wrong
     * the other way costs the item.
     */
    public Map<Integer, String> persistedPage(UUID playerId, int page) {
        VaultShape.requirePage(page);
        PlayerVault known = persisted.get(playerId);
        return known == null ? Map.of() : known.page(page);
    }

    /**
     * Mark this player's vault untouchable for the rest of the session.
     *
     * <h2>*** A FAILED FUTURE RATHER THAN A SECOND Set&lt;UUID&gt;, AND THE REASON IS REUSE ***</h2>
     *
     * Every refusal this needs already exists and is already tested: {@link #vault} and
     * {@link #writePage} both check {@code isCompletedExceptionally}, and {@link #whenSettled}
     * already reports a failed load as empty. A parallel "poisoned" set would be a second condition
     * that every one of those call sites would have to learn about, and <b>the one that forgot would
     * be the one that wrote over the file.</b>
     *
     * <p>So a poisoned vault is <b>exactly</b> a vault whose load failed, as far as every reader is
     * concerned. The only place the two must differ is {@link #saveAllAndClear}, whose message says
     * which happened.
     *
     * <p>Package-private and called from the failure closure above; there is no route to it from
     * outside, because a caller deciding to poison a vault is a caller that should have failed a
     * write.
     */
    void poison(UUID playerId, Throwable cause) {
        vaults.put(playerId, CompletableFuture.failedFuture(
                new IllegalStateException("vault write failed for " + playerId
                        + "; this vault is poisoned for the session", cause)));
    }

    /**
     * Is this player's vault poisoned or unreadable? <b>Collapses the two deliberately.</b>
     *
     * <p>For the screen, which needs to know whether to stay degraded across a page flip, and for
     * the dev command, which should say so rather than reporting an ordinary refusal. A caller that
     * needs to tell a failed LOAD from a failed WRITE has the log, which says which.
     */
    public boolean unusable(UUID playerId) {
        CompletableFuture<PlayerVault> loading = vaults.get(playerId);
        return loading != null && loading.isDone() && loading.isCompletedExceptionally();
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
                    persisted.remove(id);
                    CompletableFuture<PlayerVault> loading = vaults.remove(id);
                    if (loading == null) return CompletableFuture.completedFuture(null);

                    // *** A POISONED VAULT IS SKIPPED EXPLICITLY, AND NOT BECAUSE thenCompose WOULD
                    // SKIP IT ANYWAY. ***
                    //
                    // thenCompose does short-circuit on a failed future, so deleting these four
                    // lines leaves the behaviour correct and the MESSAGE wrong: the flush would log
                    // "Failed to save vault ... during shutdown", which reads as the shutdown
                    // having broken something. The real event is that this vault was ALREADY
                    // abandoned, minutes earlier, by a write whose report named the page and the
                    // items. An operator chasing the wrong message is chasing the wrong incident.
                    //
                    // It is also the arm that makes the poison load-bearing rather than incidental:
                    // if the cache were ever poisoned by something that left the future INTACT,
                    // this is the line that still refuses to write it.
                    if (loading.isDone() && loading.isCompletedExceptionally()) {
                        log.log(Level.WARNING, "Vault for " + id + " was not written at shutdown:"
                                + " it is poisoned or was never readable. The file on disk keeps"
                                + " its last good contents; see the earlier SEVERE report for what"
                                + " is missing from it.");
                        return CompletableFuture.completedFuture(null);
                    }

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
