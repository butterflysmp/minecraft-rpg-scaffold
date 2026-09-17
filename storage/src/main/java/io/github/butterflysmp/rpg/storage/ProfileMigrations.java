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
        // *** THE STAMP IS THE EVIDENCE, AND IT EXPIRES. *** This is the LAST MOMENT at which the
        // distinction exists. Once a profile is stamped 3, lockedSlot == 0 is ambiguous FOREVER --
        // the stamp no longer separates "absent" from "chosen", because both are now v3. That is
        // why this cannot be deferred to a later read, a lazy default, or a getter: there is
        // exactly one instant in a profile's life when absence is still knowable, and it is here.
        //
        // AND THE PRECEDENT ABOVE IS A TRAP RATHER THAN A TEMPLATE. The v1 -> v2 comment explains
        // why a bare stamp bump is SAFE -- "Gson leaves it null and the compact constructor already
        // defaulted it to NONE" -- and that explanation is CORRECT for a reference type, where
        // absent is distinguishable. The explanation stopped applying when the type changed;
        // nothing about it announced that it had. A pattern that was right twice can be wrong the
        // third time because its precondition quietly left.
        //
        // Do not copy the two steps above when adding a primitive. Copy this one.
        if (profile.schemaVersion() < 3) {
            profile = profile.withNexusSlot(PlayerProfile.DEFAULT_NEXUS_SLOT).withSchemaVersion(3);
        }

        // *** lifetimeXp WAS ADDED WITH NO STEP AND NO STAMP BUMP, ON PURPOSE. ***
        //
        // Recorded here because AN ABSENT STEP IS INDISTINGUISHABLE FROM A FORGOTTEN ONE. The
        // profile gained a `long lifetimeXp` and this chain did not grow; without this note the
        // next reader has to decide whether that was a decision or an oversight, and the cheapest
        // way to resolve it looks like adding the step.
        //
        // Gson leaves an absent long as 0, and ZERO IS THE CORRECT VALUE: a player with no record
        // has earned no XP, and PlayerLevel.levelFor(0) is level 1. There is nothing to decide, so
        // there is nothing for a step to do.
        //
        // *** AND THE v2 -> v3 STEP ABOVE ENDS WITH ADVICE THAT IS WRONG FOR THIS FIELD. ***
        //
        // It says: "Do not copy the two steps above when adding a primitive. Copy this one." That
        // sentence is true of nexusSlot and false here, and the reason is worth more than the rule:
        // IT GENERALISED FROM THE TYPE WHEN THE REAL PREMISE WAS THE VALUE SPACE.
        //
        //   nexusSlot   0 is a LEGAL CHOICE, so absence and choice are the same bytes -> needs the
        //               stamp, and only at the one instant before it is raised.
        //   lifetimeXp  0 is NOT a choice anyone can make differently from absence -> needs nothing.
        //
        // The v2 -> v3 comment is itself an account of a precondition quietly leaving -- it says so,
        // about the v1 -> v2 comment it replaced. THIS IS THE THIRD TURN OF THE SAME WHEEL, and the
        // advice in question is the correction from the second. A rule right twice can be wrong the
        // third time; a rule written BECAUSE of that can be too.
        //
        // The bill this leaves, named rather than paid: the stamp does not move, so a profile
        // written by this build is stamped 3 and an OLDER build will load it without complaint,
        // drop the key it does not know, and write that loss back on quit. The newer-server refusal
        // at the top of this method exists to stop exactly that and CANNOT FIRE HERE. Ben ruled no
        // stamp; this is what no stamp costs, and it costs it only on a rollback.

        // v3 -> v4: add the next step here.

        return profile;
    }
}
