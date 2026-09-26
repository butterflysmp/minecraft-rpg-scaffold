package io.github.butterflysmp.rpg.storage;

import java.util.List;
import java.util.UUID;

/**
 * Everything about a player that must survive a server hop.
 *
 * schemaVersion exists so that adding a field later does not strand every
 * existing player's JSON. Bump CURRENT_SCHEMA_VERSION and add a step to
 * ProfileMigrations; never reinterpret an old field in place.
 *
 * A record is an immutable data carrier: the constructor, accessors, equals and
 * hashCode are generated. To "change" one, build a new one -- see withLastSeen.
 */
public record PlayerProfile(
        int schemaVersion,
        UUID playerId,
        String archetypeId,
        String elementId,
        int level,
        long experience,
        /** RETIRED (build system slice 2): unread, and written empty by {@link #withCell}. Kept for schema stability. */
        List<String> unlockedAbilities,
        long lastSeenEpochMillis,
        int nexusSlot,
        long lifetimeXp,
        /**
         * <b>NAMED {@code ...OrNull} AND KEYED {@code starEnabled}, AND BOTH HALVES ARE DELIBERATE.</b>
         *
         * <p>A record's generated accessor must return the COMPONENT'S type, so a component called
         * {@code starEnabled} would force {@code starEnabled()} to return a nullable
         * {@code Boolean} -- <b>handing every caller the null this design exists to absorb</b>, and
         * an unboxing NPE on the first one who forgets. The component therefore carries a name that
         * <i>reads as a warning</i>, and {@link #starEnabled()} is an ordinary method returning a
         * plain {@code boolean}.
         *
         * <p>{@code @SerializedName} keeps the ON-DISK key {@code starEnabled} regardless.
         * <b>MEASURED against this project's own Gson instance on {@code gson-2.11.0}</b>, with the
         * control that matters: a JSON key of {@code starEnabledOrNull} is <b>IGNORED</b>, so the
         * component name is not a second accepted spelling.
         */
        @com.google.gson.annotations.SerializedName("starEnabled")
        Boolean starEnabledOrNull,

        /**
         * Has this player's vanilla ender chest been copied into vault page 1 yet?
         *
         * <h2>*** A PRIMITIVE, AND THE FOURTH FIELD ANSWERS ABSENCE A FOURTH WAY ***</h2>
         *
         * <p>The compact constructor's note says to ask what an absent value MEANS rather than what
         * type it is. <b>Absent means "not migrated", which is exactly right</b> for every profile
         * written before this field existed -- nobody's chest had been copied, because the copy did
         * not exist. So Gson's absent-boolean {@code false} is the correct value and no constructor
         * default, no boxing and no fix-up step is needed.
         *
         * <p>That is {@code lifetimeXp}'s case, not {@code starEnabled}'s: {@code false} is a legal
         * value AND the right reading of absence, so the primitive has nothing to disambiguate.
         *
         * <h2>*** BUT THE STAMP MOVED ANYWAY, AND THE REASON IS THE ROLLBACK, NOT THE FIELD ***</h2>
         *
         * {@code lifetimeXp} shipped with <b>no</b> stamp bump on Ben's ruling, and
         * {@code ProfileMigrations} names the bill: an older build loads the profile, drops the key
         * it does not know, and writes the loss back. <b>There it costs a rollback some XP. Here it
         * costs the player a SECOND COPY OF THEIR ENDER CHEST</b> -- the flag reverts to false, the
         * migration runs again, and page 1's free cells take another copy of a chest that was never
         * cleared.
         *
         * <p>So this field <b>does</b> bump the stamp, purely so the newer-server refusal at the top
         * of {@code ProfileMigrations} can fire. See the v3 -&gt; v4 step: it sets no value.
         */
        boolean vaultMigrated,
        /**
         * The Ability Stone's chosen hotbar slot, or {@code null} for "never chosen" (PLAN-build-system.md
         * section 2.1). BOXED for the reason {@code starEnabledOrNull} is: absence is the steady state for
         * every player who never opens the picker, and null carries that in the type. The effective slot
         * -- default 7, and never the star's -- is {@code LockedSlots.stoneSlot}, not this field.
         */
        @com.google.gson.annotations.SerializedName("stoneSlot")
        Integer stoneSlotOrNull,
        /** Is the Ability Stone switched on? {@code null} means never touched, which reads as ON (ruling 13). */
        @com.google.gson.annotations.SerializedName("stoneEnabled")
        Boolean stoneEnabledOrNull
) {
    /**
     * Bump when the on-disk shape changes, and add a ProfileMigrations step.
     *
     * <p><b>5 since the Ability Stone's two fields.</b> Like 4, the bump fixes up no field -- both are
     * boxed, so absence already reads correctly -- and buys only the newer-server refusal. See the
     * v4 -&gt; v5 step for what that refusal is worth here.
     */
    public static final int CURRENT_SCHEMA_VERSION = 5;

    /**
     * The pre-v5 shape: every field up to {@code vaultMigrated}, with the stone's two fields absent.
     *
     * <p>Kept so the many fixtures that build a profile field-by-field did not all have to change in the
     * slice that added the stone. It is exactly what a v4 file deserialises to.
     */
    public PlayerProfile(int schemaVersion, UUID playerId, String archetypeId, String elementId, int level,
                         long experience, List<String> unlockedAbilities, long lastSeenEpochMillis,
                         int nexusSlot, long lifetimeXp, Boolean starEnabledOrNull, boolean vaultMigrated) {
        this(schemaVersion, playerId, archetypeId, elementId, level, experience, unlockedAbilities,
                lastSeenEpochMillis, nexusSlot, lifetimeXp, starEnabledOrNull, vaultMigrated, null, null);
    }

    /**
     * Where the Nexus star sits for a player who has never chosen: the rightmost hotbar slot.
     *
     * <h2>IT LIVES IN STORAGE, WHICH IS NOT WHERE YOU WOULD LOOK FOR IT</h2>
     *
     * The Nexus owns this concept and the constant "should" live beside the lock in {@code paper}.
     * It is here because <b>ProfileMigrations needs it</b> -- the v2 -> v3 step writes it into every
     * existing profile -- and {@code storage} cannot depend on {@code paper}. A copy in each module
     * would be two sources of truth for one number, which is the defect this project keeps
     * recording; one import in the direction that is already allowed is the cheaper half of that
     * trade.
     *
     * <p>{@code NexusLock} reads it from here. <b>Do not re-declare it there.</b>
     */
    public static final int DEFAULT_NEXUS_SLOT = 8;

    /** The two identity axes a player has not yet chosen. Never null; never absent. */
    public static final String NONE = "none";

    /**
     * A compact constructor runs before the fields are assigned, so it can
     * normalise them. Gson leaves an absent JSON field null, which is exactly
     * what a profile written by an older build looks like -- a v1 profile has no
     * elementId, so it reads as null and defaults to NONE.
     */
    public PlayerProfile {
        elementId = elementId == null ? NONE : elementId;
        unlockedAbilities = unlockedAbilities == null ? List.of() : List.copyOf(unlockedAbilities);

        // NONE OF THE FOUR NON-STRING FIELDS IS DEFAULTED HERE, AND ALL FOUR HAVE DIFFERENT
        // REASONS. Read each accessor's javadoc before adding a fifth; the answers do NOT
        // generalise from one another, and the thing that differs is the VALUE SPACE, never the
        // type.
        //
        //   nexusSlot      IMPOSSIBLE here -- 0 is a legal slot, so absence is indistinguishable
        //                  from choice, and only the v2 -> v3 migration can tell them apart.
        //   lifetimeXp     UNNECESSARY here -- Gson's absent-long 0 is already the correct value,
        //                  so there is nothing for a default to do.
        //   starEnabled    DELIBERATELY NOT DONE here -- it is boxed, so null survives to the
        //                  ACCESSOR, which is the one place absence is interpreted. Defaulting it
        //                  here would write `true` into the record and thereby into the FILE,
        //                  destroying the property that an untouched setting stores no key at all.
        //   vaultMigrated  UNNECESSARY here, for lifetimeXp's reason -- absent means "not
        //                  migrated", which is the correct reading for every profile written before
        //                  the vault existed. THIS IS THE ONE WHERE THE VALUE SPACE AND THE STAMP
        //                  DISAGREE: nothing needs a fix-up step, and the stamp moved anyway, for
        //                  the ROLLBACK refusal. See its accessor and the v3 -> v4 step.
        //
        // Impossible, unnecessary, deliberately deferred, and unnecessary-but-stamped. All four
        // look like the same blank line, which is why they are named rather than left to be
        // inferred -- and the fourth is the one that shows the absent-value question and the
        // schema-stamp question are TWO questions, not one asked twice.
    }

    /**
     * The hotbar slot this player's Nexus star lives in.
     *
     * <h2>THE ONE FIELD THE COMPACT CONSTRUCTOR CANNOT DEFAULT</h2>
     *
     * <b>Gson leaves an absent JSON field null for a reference type, and ZERO for an int.</b> That
     * is not a quirk -- this record already depends on it: the v0 -> v1 step exists precisely
     * because a profile written before {@code schemaVersion} existed reads back as {@code 0}.
     *
     * <p>So {@code elementId == null ? NONE : elementId} has no analogue here. <b>Zero is a legal
     * slot</b> -- the leftmost hotbar cell -- and is indistinguishable from a player who chose it.
     * A constructor-side default would silently move every v2 player's star to slot 0 if it tested
     * for zero, or leave it there if it did not.
     *
     * <p><b>{@code ProfileMigrations} does it instead, and it is the only thing that can</b>,
     * because it knows what the constructor cannot: a profile stamped below 3 predates the field
     * entirely, so its zero is always absence and never a choice.
     */
    public int nexusSlot() {
        return nexusSlot;
    }

    /**
     * Total XP this player has ever earned. <b>Monotonic: nothing decreases it.</b>
     *
     * <p>The player's LEVEL is computed from this on read, through {@code PlayerLevel.levelFor}.
     * The level is never stored; that class's javadoc carries the argument.
     *
     * <h2>*** THE ABSENT-FIELD ZERO IS CORRECT HERE. NO MIGRATION, NO STAMP, NO SENTINEL. ***</h2>
     *
     * Gson leaves an absent {@code long} as <b>0</b>, so a profile written before this field
     * existed reads back as zero lifetime XP -- and <b>that is the right answer</b>: a player with
     * no record has earned none, and {@code PlayerLevel.levelFor(0)} is level 1, which is where
     * they belong. <b>Absence and zero mean the same thing for this quantity</b>, so there is
     * nothing for a migration step to decide.
     *
     * <h2>AND THE OPPOSITE PRECEDENT IS ONE FIELD AWAY, WHICH IS WHY THIS PARAGRAPH EXISTS</h2>
     *
     * <b>{@link #nexusSlot()} needed a stamped migration for what looks like the identical
     * situation</b> -- an absent primitive defaulting to 0. The difference is not the type and not
     * Gson's behaviour, which are the same in both cases. <b>It is that ZERO IS A LEGAL SLOT.</b>
     * "Never set" and "chose the leftmost hotbar cell" are the same bytes on disk, so only the
     * schema stamp could separate them, and only at the one instant before it was raised.
     *
     * <p><b>Nothing here is ambiguous, because zero is not a choice a player can make.</b> You
     * cannot elect to have earned no XP in a way that differs from never having earned any.
     *
     * <p><b>{@code ProfileMigrations}' v2 -> v3 step ends by saying "Do not copy the two steps
     * above when adding a primitive. Copy this one." THAT INSTRUCTION IS WRONG FOR THIS FIELD</b>,
     * and it is wrong for exactly the reason it was written: it generalised from the type when the
     * real premise was the VALUE SPACE. See the note at that step's end.
     *
     * <h2>THIS IS NOT THE DEAD {@code experience} FIELD TWO COMPONENTS UP</h2>
     *
     * <b>{@code level} and {@code experience} are unread.</b> Measured 2026-09-17 across
     * {@code core}, {@code storage} and {@code paper}: <b>zero production call sites</b> for either
     * accessor, against 2 for {@code archetypeId()} as a control. They are original-schema
     * scaffolding, they serialise, and nothing consumes them. <b>Do not write progression into
     * them and do not read progression out of them.</b>
     */
    public long lifetimeXp() {
        return lifetimeXp;
    }

    /**
     * Is this player's Nexus star switched on? <b>Absent means YES.</b>
     *
     * <h2>*** A BOXED {@code Boolean}, AND THE BOX IS THE WHOLE DESIGN ***</h2>
     *
     * The three primitives in this record differ in their VALUE SPACE, not their type, and that is
     * what decides how each handles absence:
     *
     * <pre>
     *   nexusSlot    int      0 is a LEGAL VALUE        -> absent and chosen are the same bytes
     *   lifetimeXp   long     0 is the CORRECT answer   -> nothing to distinguish
     *   starEnabled  boolean  false is a legal value AND THE WRONG ANSWER for absent
     * </pre>
     *
     * <b>A primitive {@code boolean} has NO ROOM for "absent"</b>, and its Java default is
     * {@code false} -- so an absent key would read as DISABLED and <b>switch off the star of every
     * player who has one</b>. So it is given room: a boxed {@code Boolean} deserialises to
     * {@code null}, and the accessor below is the single place that null is interpreted.
     *
     * <p><b>NO STAMP, and not because a stamp would not work.</b> A stamp is a second field that
     * must stay consistent with the first; {@code nexusSlot} needed one only because an {@code int}
     * has no spare value. A boxed type has one. <b>And {@code starDisabled} was refused too</b>: it
     * is correct and costs a negation at every read forever -- a name you must invert to understand
     * is a tax on every future line, paid to save one accessor now.
     *
     * <h2>*** ABSENCE IS PERMANENT HERE, NOT A TRANSITION STATE. MEASURED. ***</h2>
     *
     * {@code FilePlayerRepository}'s Gson is {@code new GsonBuilder().setPrettyPrinting().create()}
     * -- <b>{@code serializeNulls} is OFF</b>, so a null field <b>writes no key at all</b>.
     * Measured 2026-09-17 against that exact instance on {@code gson-2.11.0}:
     *
     * <pre>
     *   absent key      -> boxed = null    accessor -> true     (primitive control: false)
     *   explicit false  -> boxed = false   accessor -> false
     *   write null      -> {"id":"p"}      the key is OMITTED
     * </pre>
     *
     * <b>So a player who never touches the toggle NEVER GAINS THE KEY, on any number of saves.</b>
     * "Every existing profile is missing this" is not a state the deploy passes through -- it is
     * the steady state for everyone who leaves the setting alone.
     *
     * <p><b>Two consequences worth having: the accessor is LOAD-BEARING FOREVER</b>, not just
     * across one migration window -- and <b>a stamp would have had to fire on a file that never
     * changes</b>, which is the sharpest argument against it.
     */
    public boolean starEnabled() {
        return starEnabledOrNull == null || starEnabledOrNull;
    }

    /** Is the Ability Stone switched on? Absent reads as ON (ruling 13), exactly as {@link #starEnabled} does. */
    public boolean stoneEnabled() {
        return stoneEnabledOrNull == null || stoneEnabledOrNull;
    }

    /** Switch the Ability Stone on or off. A primitive, for {@link #withStarEnabled}'s reason. */
    public PlayerProfile withStoneEnabled(boolean enabled) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, lifetimeXp, starEnabledOrNull, vaultMigrated,
                stoneSlotOrNull, enabled);
    }

    /** Move the Ability Stone. Unvalidated here, for {@link #withNexusSlot}'s reason: {@code LockedSlots} bounds it. */
    public PlayerProfile withStoneSlot(int slot) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, lifetimeXp, starEnabledOrNull, vaultMigrated,
                slot, stoneEnabledOrNull);
    }

    public static PlayerProfile fresh(UUID id) {
        // null, NOT Boolean.TRUE. A fresh profile is written with serializeNulls off, so it gains
        // no key either -- which keeps a brand-new file byte-identical in shape to every existing
        // one, and keeps the absent-means-enabled path the ONE path rather than a fallback.
        return new PlayerProfile(CURRENT_SCHEMA_VERSION, id, NONE, NONE, 1, 0, List.of(),
                System.currentTimeMillis(), DEFAULT_NEXUS_SLOT, 0L, null, false, null, null);
    }

    /**
     * Switch the Nexus star on or off.
     *
     * <p>Takes a primitive: <b>a caller always knows which it means.</b> The {@code null} is a
     * fact about the FILE, not a state anyone sets, and letting a caller write one back would
     * reintroduce the ambiguity the box exists to resolve.
     */
    public PlayerProfile withStarEnabled(boolean enabled) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, lifetimeXp, enabled, vaultMigrated,
                stoneSlotOrNull, stoneEnabledOrNull);
    }

    public PlayerProfile withSchemaVersion(int version) {
        return new PlayerProfile(version, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, lifetimeXp, starEnabledOrNull, vaultMigrated,
                stoneSlotOrNull, stoneEnabledOrNull);
    }

    public PlayerProfile withLastSeen(long epochMillis) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, epochMillis, nexusSlot, lifetimeXp, starEnabledOrNull, vaultMigrated,
                stoneSlotOrNull, stoneEnabledOrNull);
    }

    /**
     * Move this player's Nexus star to a different hotbar slot.
     *
     * <p><b>UNVALIDATED HERE, DELIBERATELY.</b> This record is a data carrier and knows nothing
     * about inventories; a slot it rejected would be a slot the caller had to handle anyway. The
     * bound is enforced where the value is USED, at {@code NexusSlots}, because that is the only
     * place that knows what an inventory index means -- and it must be enforced there regardless,
     * since a hand-edited JSON file reaches that code without passing through this method.
     */
    public PlayerProfile withNexusSlot(int slot) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, slot, lifetimeXp, starEnabledOrNull, vaultMigrated,
                stoneSlotOrNull, stoneEnabledOrNull);
    }

    /**
     * Set this player's lifetime XP.
     *
     * <p><b>NOT VALIDATED AS MONOTONIC HERE, AND THAT IS THE SAME TRADE {@link #withNexusSlot}
     * MAKES.</b> This record is a data carrier; a refusal here would still leave the caller holding
     * the decision, and a hand-edited JSON file reaches {@code PlayerLevel.levelFor} without
     * passing through this method at all -- which is why that function clamps rather than throws.
     *
     * <p>The monotonicity is a property of <b>the one writer</b>: {@code PlayerLevelListener} only
     * ever adds a positive amount. Nothing else writes this field.
     */
    public PlayerProfile withLifetimeXp(long xp) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, xp, starEnabledOrNull, vaultMigrated,
                stoneSlotOrNull, stoneEnabledOrNull);
    }

    /**
     * Record that this player's ender chest has been copied into the vault.
     *
     * <h2>*** TAKES A PRIMITIVE AND IS MEANT TO BE CALLED WITH {@code true}, ONCE, FOREVER ***</h2>
     *
     * <b>It accepts {@code false} deliberately, and that is not symmetry for its own sake.</b> An
     * operator re-running a migration for one player -- because it was interrupted, or because a
     * restore put an old chest back -- is a real support action, and the alternative is hand-editing
     * JSON on a live server. {@code withStarEnabled} takes a primitive for the same reason.
     *
     * <p><b>WHAT SETTING IT BACK TO {@code false} COSTS, because the caller must know:</b> the next
     * open at level 20+ copies the chest again, into whatever cells of page 1 are free. The chest is
     * never cleared, so nothing is lost -- but anything the player has since MOVED OFF page 1 is
     * copied a second time. That is the duplication arm, reachable by hand and only by hand.
     */
    public PlayerProfile withVaultMigrated(boolean migrated) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, lifetimeXp, starEnabledOrNull,
                migrated, stoneSlotOrNull, stoneEnabledOrNull);
    }

    /**
     * Pick a (class, element) cell. archetypeId carries the class id (the field name is kept for
     * schema stability; its value is a class, e.g. "ranger").
     *
     * <p><b>It writes {@code unlockedAbilities} EMPTY, and that is the field's retirement</b>
     * (PLAN-build-system.md section 2.1). The field was the kit's grant; kits are gone, and what a player
     * may cast is now their cell's loadout. The field is KEPT in the record -- removing it would be a schema
     * change for nothing -- but nothing reads it any more, and every cell change clears it so an old kit
     * grant cannot linger in a file as if it still meant something.
     */
    public PlayerProfile withCell(String classId, String elementId) {
        return new PlayerProfile(schemaVersion, playerId, classId, elementId, level, experience,
                List.of(), lastSeenEpochMillis, nexusSlot, lifetimeXp, starEnabledOrNull, vaultMigrated,
                stoneSlotOrNull, stoneEnabledOrNull);
    }
}
