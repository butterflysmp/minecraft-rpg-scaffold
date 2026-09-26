package io.github.butterflysmp.rpg.storage;

/**
 * Brings a loaded {@link PlayerBuild} up to the current schema. v1 is the first shape, so today this only
 * stamps -- and refuses a NEWER version, the rule every store here follows: an older server must not load a
 * file a newer one wrote and then write its losses back.
 */
public final class BuildMigrations {

    private BuildMigrations() {}

    public static PlayerBuild migrate(PlayerBuild loaded) {
        if (loaded.schemaVersion() > PlayerBuild.CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Build " + loaded.playerId() + " has schema version " + loaded.schemaVersion()
                            + " but this server understands at most " + PlayerBuild.CURRENT_SCHEMA_VERSION
                            + ". Refusing to load it rather than silently discarding data.");
        }
        PlayerBuild build = loaded;
        if (build.schemaVersion() < 1) {
            build = build.withSchemaVersion(1);
        }
        // v1 -> v2: add the next step here.
        return build;
    }
}
