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
        long lastSeenEpochMillis
) {
    /** Bump when the on-disk shape changes, and add a ProfileMigrations step. */
    public static final int CURRENT_SCHEMA_VERSION = 2;

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
    }

    public static PlayerProfile fresh(UUID id) {
        return new PlayerProfile(CURRENT_SCHEMA_VERSION, id, NONE, NONE, 1, 0, List.of(),
                System.currentTimeMillis());
    }

    public PlayerProfile withSchemaVersion(int version) {
        return new PlayerProfile(version, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, lastSeenEpochMillis);
    }

    public PlayerProfile withLastSeen(long epochMillis) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, elementId, level, experience,
                unlockedAbilities, epochMillis);
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
                unlockedAbilities, lastSeenEpochMillis);
    }
}
