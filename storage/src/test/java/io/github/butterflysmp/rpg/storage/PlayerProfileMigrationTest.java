package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileMigrationTest {

    private static PlayerProfile at(int schemaVersion) {
        return new PlayerProfile(schemaVersion, UUID.randomUUID(), "hunter", "fire", 7, 1234,
                List.of("solar_grenade"), 99L);
    }

    @Test
    void freshProfileIsStampedWithTheCurrentVersion() {
        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION,
                PlayerProfile.fresh(UUID.randomUUID()).schemaVersion());
    }

    /** A profile written before schemaVersion existed deserialises as 0. */
    @Test
    void versionZeroIsMigratedToCurrentWithoutLosingData() {
        PlayerProfile legacy = at(0);

        PlayerProfile migrated = ProfileMigrations.migrate(legacy);

        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertEquals(legacy.playerId(), migrated.playerId());
        assertEquals("hunter", migrated.archetypeId());
        assertEquals("fire", migrated.elementId());
        assertEquals(7, migrated.level());
        assertEquals(1234, migrated.experience());
        assertEquals(List.of("solar_grenade"), migrated.unlockedAbilities());
        assertEquals(99L, migrated.lastSeenEpochMillis());
    }

    /**
     * The v1->v2 step: a v1 profile has no elementId key, so Gson leaves it null and the
     * compact constructor defaults it to NONE -- a half-selected player who re-picks. The
     * class and every other field survive.
     */
    @Test
    void versionOneGainsAnElementOfNoneAndKeepsTheRest() {
        // A v1 JSON has no elementId -> null on read -> NONE via the compact constructor.
        PlayerProfile v1 = new PlayerProfile(1, UUID.randomUUID(), "hunter", null, 7, 1234,
                List.of("solar_grenade"), 99L);
        assertEquals(PlayerProfile.NONE, v1.elementId(), "absent elementId defaults to NONE");

        PlayerProfile migrated = ProfileMigrations.migrate(v1);

        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertEquals("hunter", migrated.archetypeId());
        assertEquals(PlayerProfile.NONE, migrated.elementId());
        assertEquals(List.of("solar_grenade"), migrated.unlockedAbilities());
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
        var profile = new PlayerProfile(2, UUID.randomUUID(), "none", "none", 1, 0, null, 0L);
        assertEquals(List.of(), profile.unlockedAbilities());
    }

    @Test
    void nullElementIdBecomesNone() {
        var profile = new PlayerProfile(2, UUID.randomUUID(), "ranger", null, 1, 0, List.of(), 0L);
        assertEquals(PlayerProfile.NONE, profile.elementId());
    }

    @Test
    void unlockedAbilitiesIsDefensivelyCopied() {
        var mutable = new java.util.ArrayList<>(List.of("a"));
        var profile = new PlayerProfile(2, UUID.randomUUID(), "none", "none", 1, 0, mutable, 0L);

        mutable.add("b");

        assertEquals(List.of("a"), profile.unlockedAbilities());
    }

    @Test
    void withKitSetsClassElementAndGrantsAndCarriesTheRest() {
        var unchosen = PlayerProfile.fresh(UUID.randomUUID());
        assertEquals("none", unchosen.archetypeId());
        assertEquals("none", unchosen.elementId());
        assertEquals(List.of(), unchosen.unlockedAbilities());

        var ranger = unchosen.withKit("ranger", "fire", List.of("arc_surge"));

        assertEquals("ranger", ranger.archetypeId());
        assertEquals("fire", ranger.elementId());
        assertEquals(List.of("arc_surge"), ranger.unlockedAbilities());
        // Everything else is carried unchanged.
        assertEquals(unchosen.playerId(), ranger.playerId());
        assertEquals(unchosen.schemaVersion(), ranger.schemaVersion());
        assertEquals(unchosen.level(), ranger.level());
        assertEquals(unchosen.experience(), ranger.experience());
        assertEquals(unchosen.lastSeenEpochMillis(), ranger.lastSeenEpochMillis());
    }

    @Test
    void withKitDefensivelyCopiesTheGrantedList() {
        var mutable = new java.util.ArrayList<>(List.of("arc_surge"));
        var ranger = PlayerProfile.fresh(UUID.randomUUID()).withKit("ranger", "fire", mutable);

        mutable.add("sneaked_in");

        assertEquals(List.of("arc_surge"), ranger.unlockedAbilities());
    }
}
