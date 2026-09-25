package io.github.butterflysmp.rpg.storage;

/**
 * Brings a loaded accessories file up to {@link PlayerAccessories#CURRENT_SCHEMA_VERSION}. The
 * vault's {@code VaultMigrations} shape: a newer version is REFUSED rather than read, because a
 * build that does not understand a file must not rewrite it.
 */
public final class AccessoryMigrations {

    private AccessoryMigrations() {}

    public static PlayerAccessories migrate(PlayerAccessories loaded) {
        if (loaded.schemaVersion() > PlayerAccessories.CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException(
                    "Accessories " + loaded.playerId() + " have schema version " + loaded.schemaVersion()
                            + " but this server understands at most "
                            + PlayerAccessories.CURRENT_SCHEMA_VERSION
                            + ". Refusing to load them rather than silently discarding data.");
        }
        PlayerAccessories accessories = loaded;
        // v0 -> v1: the stamp only. A file written before the field existed reads as 0.
        if (accessories.schemaVersion() < 1) {
            accessories = accessories.withSchemaVersion(1);
        }
        return accessories;
    }
}
