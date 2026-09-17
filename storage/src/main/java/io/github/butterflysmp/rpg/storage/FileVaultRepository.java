package io.github.butterflysmp.rpg.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Milestone-1 vault storage: one JSON file per player, beside the profiles.
 *
 * <p>Deliberately the same shape as {@link FilePlayerRepository} -- same Gson, same tmp-then-move
 * write, same constructor-injected {@link Executor} for the reason that class states. Two storage
 * implementations that differ only where they must is worth more than one that is cleverer.
 *
 * <h2>THE TMP-THEN-ATOMIC-MOVE IS NOT COPIED FOR SYMMETRY. IT IS THE CRASH GUARANTEE.</h2>
 *
 * A vault is written on <b>every item the player moves</b>, so the window in which a process can die
 * mid-write is entered constantly rather than once per level. Writing in place would mean a kill at
 * the wrong instant leaves a truncated file -- which is not "the last move is lost", it is
 * <b>the whole vault fails to parse</b>. Write beside, then move; the move either happened or it did
 * not, and the previous file is intact either way.
 *
 * <p><b>The same reasoning is why the executor is single-threaded at the call site.</b> Two writes
 * for one player racing each other would interleave two temp files onto one target, and the loser is
 * whichever finishes second rather than whichever was issued second.
 */
public final class FileVaultRepository implements VaultRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root;
    private final Executor io;

    public FileVaultRepository(Path root, Executor io) {
        this.root = Objects.requireNonNull(root, "root");
        this.io = Objects.requireNonNull(io, "io executor");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path fileFor(UUID id) { return root.resolve(id + ".json"); }

    @Override
    public CompletableFuture<Optional<PlayerVault>> load(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Path f = fileFor(playerId);
            if (!Files.exists(f)) return Optional.empty();
            String json;
            try {
                json = Files.readString(f, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            PlayerVault raw;
            try {
                raw = GSON.fromJson(json, PlayerVault.class);
            } catch (JsonParseException e) {
                throw refusal(playerId, "is not valid JSON", e);
            } catch (RuntimeException e) {
                // *** GSON WRAPS AN EXCEPTION THROWN BY A RECORD'S CANONICAL CONSTRUCTOR, AND ITS
                // *** MESSAGE DUMPS EVERY CONSTRUCTOR ARGUMENT. MEASURED, NOT ASSUMED.
                //
                // Staging a duplicate cell and parsing it yields a bare java.lang.RuntimeException
                // reading "Failed to invoke constructor 'PlayerVault(int, UUID, List)' with args
                // [1, <uuid>, [VaultEntry[page=3, slot=24, item=first], ...]]" -- the whole entry
                // list, inline. For a real vault that is up to 252 Base64 item blobs written into
                // the server log, with the actual reason buried underneath them.
                //
                // So the cause's message is promoted and Gson's is dropped. The cause still hangs
                // off this exception for anyone who wants the stack.
                if (e.getCause() instanceof IllegalArgumentException structural) {
                    throw refusal(playerId, "is structurally invalid -- " + structural.getMessage(),
                            structural);
                }
                throw e;
            }

            // Bring an older on-disk shape up to date before anyone sees it. Deliberately OUTSIDE
            // the catches above: VaultMigrations' newer-server refusal already says exactly what is
            // wrong, and re-wrapping it here would bury a good message inside a vaguer one.
            return Optional.ofNullable(raw).map(VaultMigrations::migrate);
        }, io);
    }

    /**
     * One refusal shape for every way a vault file can be unreadable.
     *
     * <p><b>NAMED, not swallowed, and never turned into "no vault".</b> An empty {@code Optional}
     * here would hand the player an empty screen, and the first thing they did with it would save
     * that emptiness over a file that might still have been recoverable. Refusing loudly is the only
     * outcome with no path through it that ends with the items gone.
     *
     * <p>It says whose file, what is wrong, and why it stopped -- the operator has to go and open
     * the file, so the message has to be enough to find it and enough to resist being "fixed" by
     * returning empty.
     */
    private static IllegalStateException refusal(UUID playerId, String problem, Throwable cause) {
        return new IllegalStateException(
                "Vault file for " + playerId + " " + problem + ". Refusing to load it rather than"
                        + " presenting an empty vault that would overwrite it.",
                cause);
    }

    @Override
    public CompletableFuture<Void> save(PlayerVault vault) {
        return CompletableFuture.runAsync(() -> {
            Path f = fileFor(vault.playerId());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try {
                Files.writeString(tmp, GSON.toJson(vault), StandardCharsets.UTF_8);
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, io);
    }
}
