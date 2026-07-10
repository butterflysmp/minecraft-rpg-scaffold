package io.github.butterflysmp.rpg.storage;

/**
 * Brings a profile read from disk up to CURRENT_SCHEMA_VERSION.
 *
 * Called by every PlayerRepository implementation on the way out of load(), so
 * nothing above storage ever sees a stale shape. Migrations run in order and
 * each one is a pure function; add a step, never edit an old one.
 */
public final class ProfileMigrations {

    private ProfileMigrations() {}

    /**
     * @throws IllegalStateException if the profile was written by a newer server
     *         than this one. Refusing is deliberate: loading it would silently
     *         drop the fields we do not know about, and quitting would then
     *         write that loss back to disk.
     */
    public static PlayerProfile migrate(PlayerProfile loaded) {
        if (loaded.schemaVersion() > PlayerProfile.CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Profile " + loaded.playerId() + " has schema version " + loaded.schemaVersion()
                            + " but this server understands at most "
                            + PlayerProfile.CURRENT_SCHEMA_VERSION
                            + ". Refusing to load it rather than silently discarding data.");
        }

        PlayerProfile profile = loaded;

        // v0 -> v1: written before schemaVersion existed, so Gson left it 0.
        // The field set is otherwise identical; only the stamp is new.
        if (profile.schemaVersion() < 1) {
            profile = profile.withSchemaVersion(1);
        }

        // v1 -> v2: add the next step here.

        return profile;
    }
}
