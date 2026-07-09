package io.github.yourname.rpg.storage;

import java.util.List;
import java.util.UUID;

/** Everything about a player that must survive a server hop. */
public record PlayerProfile(
        UUID playerId,
        String archetypeId,
        int level,
        long experience,
        List<String> unlockedAbilities,
        long lastSeenEpochMillis
) {
    public static PlayerProfile fresh(UUID id) {
        return new PlayerProfile(id, "none", 1, 0, List.of(), System.currentTimeMillis());
    }
}
