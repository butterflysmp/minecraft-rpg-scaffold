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
        int level,
        long experience,
        List<String> unlockedAbilities,
        long lastSeenEpochMillis
) {
    /** Bump when the on-disk shape changes, and add a ProfileMigrations step. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /**
     * A compact constructor runs before the fields are assigned, so it can
     * normalise them. Gson leaves an absent JSON field null, which is exactly
     * what a profile written by an older build looks like.
     */
    public PlayerProfile {
        unlockedAbilities = unlockedAbilities == null ? List.of() : List.copyOf(unlockedAbilities);
    }

    public static PlayerProfile fresh(UUID id) {
        return new PlayerProfile(CURRENT_SCHEMA_VERSION, id, "none", 1, 0, List.of(),
                System.currentTimeMillis());
    }

    public PlayerProfile withSchemaVersion(int version) {
        return new PlayerProfile(version, playerId, archetypeId, level, experience,
                unlockedAbilities, lastSeenEpochMillis);
    }

    public PlayerProfile withLastSeen(long epochMillis) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, level, experience,
                unlockedAbilities, epochMillis);
    }

    /**
     * Pick a class. Set together because they move together: choosing an archetype
     * grants exactly that archetype's abilities. The compact constructor copies the
     * list, so a caller cannot alias it into the stored profile.
     */
    public PlayerProfile withArchetype(String archetypeId, List<String> unlockedAbilities) {
        return new PlayerProfile(schemaVersion, playerId, archetypeId, level, experience,
                unlockedAbilities, lastSeenEpochMillis);
    }
}
