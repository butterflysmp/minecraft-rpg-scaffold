package io.github.butterflysmp.rpg.storage;

/**
 * Brings a vault read from disk up to {@link PlayerVault#CURRENT_SCHEMA_VERSION}.
 *
 * <p>Called by every {@link VaultRepository} implementation on the way out of {@code load()}, so
 * nothing above {@code storage} ever sees a stale shape. Steps run in order and each is a pure
 * function; <b>add a step, never edit an old one.</b> Same contract as {@link ProfileMigrations},
 * deliberately, so a reader who has read one has read both.
 *
 * <h2>*** THIS IS NOT CEREMONY. {@code ProfileMigrations} CANNOT SEE THIS FILE. ***</h2>
 *
 * The profile's newer-server refusal guards the profile. The vault is a <b>sibling file</b> with its
 * own lifetime, and nothing in the existing chain reaches it. Without this class, a vault written by
 * a newer build and opened by an older one would be loaded without complaint, would lose every field
 * that build did not understand, and would <b>write the loss back on the next save</b>.
 *
 * <p>{@code ProfileMigrations} names that exact bill as the cost of shipping {@code lifetimeXp} with
 * no stamp, and accepts it, because there it costs a rollback some XP. <b>Here the same bill is paid
 * in the player's items</b>, so it is not accepted.
 */
public final class VaultMigrations {

    private VaultMigrations() {}

    /**
     * @throws IllegalStateException if the vault was written by a newer server than this one.
     *         Refusing is deliberate: loading it would silently drop the fields we do not know
     *         about, and the next write would then make that loss permanent.
     */
    public static PlayerVault migrate(PlayerVault loaded) {
        if (loaded.schemaVersion() > PlayerVault.CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Vault " + loaded.playerId() + " has schema version " + loaded.schemaVersion()
                            + " but this server understands at most "
                            + PlayerVault.CURRENT_SCHEMA_VERSION
                            + ". Refusing to load it rather than silently discarding data.");
        }

        PlayerVault vault = loaded;

        // v0 -> v1: a bare stamp.
        //
        // *** NO SHIPPED FILE REACHES THIS ARM, AND IT IS WRITTEN DOWN BECAUSE THAT IS INVISIBLE. ***
        //
        // v1 is the FIRST version this feature ever had, so there is no pre-schema era for a vault
        // the way there was for a profile. Gson leaves an absent int as 0, so the only things that
        // arrive here are a hand-authored file, a file that lost its stamp, or a test staging one on
        // purpose. Its ONLY exercise is VaultMigrationsTest, which CAUSES the condition rather than
        // asserting the arm exists.
        //
        // It is a stamp and not a value-setter because the SHAPE is unchanged -- there is no field
        // whose absence is ambiguous. That is lifetimeXp's case, not nexusSlot's, and the difference
        // is the value space rather than the type: an absent entry list means an empty vault, which
        // is the correct reading and not a choice anybody could have made differently.
        if (vault.schemaVersion() < 1) {
            vault = vault.withSchemaVersion(1);
        }

        // v1 -> v2: add the next step here.

        return vault;
    }
}
