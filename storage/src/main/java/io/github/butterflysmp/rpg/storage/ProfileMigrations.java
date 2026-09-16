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

        // v1 -> v2: added elementId (the second identity axis). A v1 profile has no such
        // field, so Gson leaves it null and the compact constructor already defaulted it to
        // NONE -- a v1 player becomes half-selected (a class, no element) and re-picks. Only
        // the stamp is new here.
        if (profile.schemaVersion() < 2) {
            profile = profile.withSchemaVersion(2);
        }

        // v2 -> v3: added nexusSlot (the per-player Nexus star slot).
        //
        // *** THIS STEP SETS A VALUE. THE TWO ABOVE ONLY STAMP, AND THE DIFFERENCE IS THE TYPE. ***
        //
        // Every previous step could say "only the stamp is new" because every previous field was a
        // REFERENCE type: Gson leaves an absent one null, and PlayerProfile's compact constructor
        // turns null into the default. An int has no null. Gson leaves an absent int as ZERO --
        // which is exactly how the v0 -> v1 step above detects a pre-schemaVersion profile.
        //
        // Zero is a LEGAL nexusSlot: the leftmost hotbar cell. So the constructor cannot default it
        // without either stealing slot 0 from everyone who chose it, or stranding every v2 player
        // there. THIS is the only place that can tell the two apart, and the reason is the stamp
        // itself: a profile below v3 predates the field, so its zero is ALWAYS absence.
        //
        // Do not copy the two steps above when adding a primitive. Copy this one.
        if (profile.schemaVersion() < 3) {
            profile = profile.withNexusSlot(PlayerProfile.DEFAULT_NEXUS_SLOT).withSchemaVersion(3);
        }

        // v3 -> v4: add the next step here.

        return profile;
    }
}
