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
        List<String> unlockedAbilities,
        long lastSeenEpochMillis,
        int nexusSlot,
        long lifetimeXp
) {
    /** Bump when the on-disk shape changes, and add a ProfileMigrations step. */
    public static final int CURRENT_SCHEMA_VERSION = 3;

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

        // NEITHER PRIMITIVE IS DEFAULTED HERE, AND THE TWO HAVE OPPOSITE REASONS. Read both
        // accessors' javadocs before adding a third; the answers do not generalise from each other.
        //
        //   nexusSlot   CANNOT be defaulted here -- zero is a legal slot, so absence is
        //               indistinguishable from choice, and only the v2 -> v3 migration can tell
        //               them apart.
        //   lifetimeXp  NEED NOT be defaulted here -- Gson's absent-long zero is already the
        //               correct value, so there is nothing for a default to do.
        //
        // One is impossible and one is unnecessary. Both look like the same blank line.
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

    public static PlayerProfile fresh(UUID id) {
        return new PlayerProfile(CURRENT_SCHEMA_VERSION, id, NONE, NONE, 1, 0, List.of(),
                System.currentTimeMillis(), DEFAULT_NEXUS_SLOT, 0L);
    }

    public PlayerProfile withSchemaVersion(int version) {
        return new PlayerProfile(version, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, lifetimeXp);
    }

    public PlayerProfile withLastSeen(long epochMillis) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, epochMillis, nexusSlot, lifetimeXp);
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
                unlockedAbilities, lastSeenEpochMillis, slot, lifetimeXp);
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
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, xp);
    }

    /**
     * Pick a (class, element) cell and its grant. Class, element, and unlocked abilities
     * move together: they are re-derived as one whenever either axis changes, so a stale
     * class's abilities can never outlive a class change. archetypeId carries the class id
     * (the field name is kept for schema stability; its value is now a class, e.g. "ranger").
     * The compact constructor copies the list, so a caller cannot alias it into the profile.
     */
    public PlayerProfile withKit(String classId, String elementId, List<String> unlockedAbilities) {
        return new PlayerProfile(schemaVersion, playerId, classId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot, lifetimeXp);
    }
}
