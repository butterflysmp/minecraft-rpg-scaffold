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
        int nexusSlot
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

        // NOTE THAT nexusSlot IS NOT DEFAULTED HERE, AND THAT IS NOT AN OVERSIGHT. See its
        // accessor's javadoc: an int has no absent value to test for, so the defaulting this
        // constructor does for elementId and unlockedAbilities is IMPOSSIBLE for it. The v2 -> v3
        // migration does that job instead, which is the only place that can tell "absent" from
        // "chosen".
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

    public static PlayerProfile fresh(UUID id) {
        return new PlayerProfile(CURRENT_SCHEMA_VERSION, id, NONE, NONE, 1, 0, List.of(),
                System.currentTimeMillis(), DEFAULT_NEXUS_SLOT);
    }

    public PlayerProfile withSchemaVersion(int version) {
        return new PlayerProfile(version, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis, nexusSlot);
    }

    public PlayerProfile withLastSeen(long epochMillis) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, epochMillis, nexusSlot);
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
                unlockedAbilities, lastSeenEpochMillis, slot);
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
                unlockedAbilities, lastSeenEpochMillis, nexusSlot);
    }
}
