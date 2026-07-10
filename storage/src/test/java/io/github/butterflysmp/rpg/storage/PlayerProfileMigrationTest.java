package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileMigrationTest {

    private static PlayerProfile at(int schemaVersion) {
        return new PlayerProfile(schemaVersion, UUID.randomUUID(), "hunter", 7, 1234,
                List.of("solar_grenade"), 99L);
    }

    @Test
    void freshProfileIsStampedWithTheCurrentVersion() {
        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION,
                PlayerProfile.fresh(UUID.randomUUID()).schemaVersion());
    }

    /** A profile written before schemaVersion existed deserialises as 0. */
    @Test
    void versionZeroIsMigratedToOneWithoutLosingData() {
        PlayerProfile legacy = at(0);

        PlayerProfile migrated = ProfileMigrations.migrate(legacy);

        assertEquals(1, migrated.schemaVersion());
        assertEquals(legacy.playerId(), migrated.playerId());
        assertEquals("hunter", migrated.archetypeId());
        assertEquals(7, migrated.level());
        assertEquals(1234, migrated.experience());
        assertEquals(List.of("solar_grenade"), migrated.unlockedAbilities());
        assertEquals(99L, migrated.lastSeenEpochMillis());
    }

    @Test
    void currentVersionIsLeftAlone() {
        PlayerProfile current = at(PlayerProfile.CURRENT_SCHEMA_VERSION);
        assertEquals(current, ProfileMigrations.migrate(current));
    }

    /**
     * A profile from a newer server must not be silently downgraded -- loading
     * it would drop unknown fields, and the next quit would write that loss back.
     */
    @Test
    void profileFromANewerServerIsRefused() {
        PlayerProfile future = at(PlayerProfile.CURRENT_SCHEMA_VERSION + 1);

        var ex = assertThrows(IllegalStateException.class, () -> ProfileMigrations.migrate(future));
        assertTrue(ex.getMessage().contains("Refusing"), ex.getMessage());
    }

    /** Legacy JSON has no unlockedAbilities key at all; it must not NPE. */
    @Test
    void nullUnlockedAbilitiesBecomesEmptyList() {
        var profile = new PlayerProfile(1, UUID.randomUUID(), "none", 1, 0, null, 0L);
        assertEquals(List.of(), profile.unlockedAbilities());
    }

    @Test
    void unlockedAbilitiesIsDefensivelyCopied() {
        var mutable = new java.util.ArrayList<>(List.of("a"));
        var profile = new PlayerProfile(1, UUID.randomUUID(), "none", 1, 0, mutable, 0L);

        mutable.add("b");

        assertEquals(List.of("a"), profile.unlockedAbilities());
    }

    @Test
    void withArchetypeSetsClassAndGrantsAndCarriesTheRest() {
        var classless = PlayerProfile.fresh(UUID.randomUUID());
        assertEquals("none", classless.archetypeId());
        assertEquals(List.of(), classless.unlockedAbilities());

        var hunter = classless.withArchetype("hunter", List.of("solar_grenade", "solar_lance"));

        assertEquals("hunter", hunter.archetypeId());
        assertEquals(List.of("solar_grenade", "solar_lance"), hunter.unlockedAbilities());
        // Everything else is carried unchanged.
        assertEquals(classless.playerId(), hunter.playerId());
        assertEquals(classless.schemaVersion(), hunter.schemaVersion());
        assertEquals(classless.level(), hunter.level());
        assertEquals(classless.experience(), hunter.experience());
        assertEquals(classless.lastSeenEpochMillis(), hunter.lastSeenEpochMillis());
    }

    @Test
    void withArchetypeDefensivelyCopiesTheGrantedList() {
        var mutable = new java.util.ArrayList<>(List.of("solar_grenade"));
        var hunter = PlayerProfile.fresh(UUID.randomUUID()).withArchetype("hunter", mutable);

        mutable.add("sneaked_in");

        assertEquals(List.of("solar_grenade"), hunter.unlockedAbilities());
    }
}
