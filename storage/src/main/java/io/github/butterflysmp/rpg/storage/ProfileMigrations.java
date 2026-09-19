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

        // *** starEnabled ALSO NEEDS NO STEP, AND FOR A DIFFERENT REASON AGAIN. ***
        //
        // lifetimeXp needs none because absent and 0 mean the same thing. starEnabled needs none
        // because IT IS BOXED: an absent key deserialises to null, which is a value the primitive
        // did not have, and PlayerProfile.starEnabled() turns null into ENABLED. The distinction
        // absence carries is preserved in the TYPE rather than reconstructed from a stamp.
        //
        // AND A STAMP WOULD HAVE HAD TO FIRE ON A FILE THAT NEVER CHANGES. serializeNulls is off,
        // so a player who never touches the toggle never gains the key on any number of saves --
        // absence here is the STEADY STATE, not a window the deploy passes through.
        //
        // So the three primitives now answer absence three different ways, and the chain has ONE
        // step. Adding a fourth field: ask what the absent value MEANS, not what type it is.

        // v3 -> v4: added vaultMigrated (has this player's ender chest been copied into the vault).
        //
        // *** A BARE STAMP, AND IT IS THE FIRST STEP HERE THAT EXISTS FOR THE REFUSAL ALONE. ***
        //
        // THE FIX-UP IS GENUINELY EMPTY, and that is not an oversight: an absent boolean reads as
        // false, and false -- "not migrated" -- is the correct value for every profile written
        // before the vault existed, because nobody's chest had been copied. That is lifetimeXp's
        // case exactly, and lifetimeXp needed NO STEP AT ALL.
        //
        // *** SO WHY IS THERE A STEP? BECAUSE THE STAMP AND THE VALUE ARE TWO QUESTIONS. ***
        //
        // The three notes above all answer ONE question -- what does an absent value mean -- and all
        // three then treat the stamp as a consequence of the answer. It is not. The stamp's only
        // job is the refusal at the top of this method, and the refusal protects against a
        // DIFFERENT event: an older build reading a file this build wrote.
        //
        // lifetimeXp declined that protection on Ben's ruling, and the note above prices it: a
        // rollback loses some XP. THE SAME ROLLBACK PRICES DIFFERENTLY HERE. vaultMigrated would
        // revert to false, the migration would run a second time, and page 1's free cells would
        // take another copy of an ender chest that is NEVER CLEARED -- so anything the player had
        // moved off page 1 in the meantime exists twice. A duplicate is an economy hole; re-earned
        // XP is an evening.
        //
        // The step therefore bumps and sets nothing. Written out at this length because AN EMPTY
        // STEP IS INDISTINGUISHABLE FROM A HALF-WRITTEN ONE, and the next reader's cheapest
        // resolution would be to add the fix-up that does not belong.
        //
        // AND THE PRICE OF THE BUMP, NAMED RATHER THAN LEFT TO BE DISCOVERED: every profile is
        // rewritten to 4 on first load, so an older build refuses EVERY profile, not just a
        // migrated one. That is a loud, total, roll-forward-able failure, which is the direction
        // this refusal was built to fail in -- and it is the opposite trade from lifetimeXp's.
        // Ben's to overrule, and overruling it is deleting this step and restoring the 3.
        if (profile.schemaVersion() < 4) {
            profile = profile.withSchemaVersion(4);
        }

        // v4 -> v5: add the next step here.

        return profile;
    }
}
