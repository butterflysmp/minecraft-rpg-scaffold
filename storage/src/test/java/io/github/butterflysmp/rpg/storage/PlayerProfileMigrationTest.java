package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileMigrationTest {

    /**
     * A profile at a given stamp. <b>nexusSlot is 3, which is DELIBERATELY NOT the default 8</b> --
     * a fixture staged on the default would pass whether the v2 -> v3 step set the value or not,
     * and that step's whole job is setting it. It is also distinct from every other number here
     * (level 7, experience 1234, lastSeen 99), so no two quantities this file reads are equal and
     * a transposition between any two has nowhere to hide.
     */
    private static PlayerProfile at(int schemaVersion) {
        return new PlayerProfile(schemaVersion, UUID.randomUUID(), "hunter", "fire", 7, 1234,
                List.of("solar_grenade"), 99L, 3, 56_780L, null, false);
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

        // AND ONE FIELD IS DELIBERATELY *NOT* CARRIED, WHICH IS WHY THIS ROW'S NAME IS NOW SLIGHTLY
        // WRONG AND IS LEFT ALONE. nexusSlot is RESET to the default rather than preserved: a
        // profile stamped below 3 predates the field, so the 3 this fixture carries could not have
        // come from disk -- on a real v0 file Gson would have left it 0. The step overwrites
        // whatever is there precisely because it cannot be trusted.
        assertEquals(PlayerProfile.DEFAULT_NEXUS_SLOT, migrated.nexusSlot(),
                "a pre-v3 profile's nexusSlot is absence, never a choice, so it is set not kept");
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
                List.of("solar_grenade"), 99L, 3, 56_780L, null, false);
        assertEquals(PlayerProfile.NONE, v1.elementId(), "absent elementId defaults to NONE");

        PlayerProfile migrated = ProfileMigrations.migrate(v1);

        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertEquals("hunter", migrated.archetypeId());
        assertEquals(PlayerProfile.NONE, migrated.elementId());
        assertEquals(List.of("solar_grenade"), migrated.unlockedAbilities());
    }

    /**
     * The v2->v3 step, and <b>it is the first step that SETS a value rather than only stamping.</b>
     *
     * <p>A real v2 JSON has no {@code nexusSlot} key, and Gson leaves an absent INT as <b>0</b> --
     * not null, because an int has no null. So this fixture stages the zero a real file would
     * produce. Zero is a legal slot, which is exactly why the compact constructor cannot default
     * it and the migration must.
     */
    @Test
    void versionTwoGainsTheDefaultNexusSlot_andZeroIsNOTTreatedAsAChoice() {
        PlayerProfile v2 = new PlayerProfile(2, UUID.randomUUID(), "hunter", "fire", 7, 1234,
                List.of("solar_grenade"), 99L, 0, 56_780L, null, false);
        assertEquals(0, v2.nexusSlot(), "the constructor leaves it alone -- it cannot tell 0 from 0");

        PlayerProfile migrated = ProfileMigrations.migrate(v2);

        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertEquals(PlayerProfile.DEFAULT_NEXUS_SLOT, migrated.nexusSlot(),
                "an absent int reads as 0, and a v2 profile's 0 is absence -- it becomes the default");
        assertNotEquals(0, migrated.nexusSlot(),
                "and it must NOT be left at 0, which is the leftmost hotbar cell");

        // Everything else survives, because this step sets ONE field.
        assertEquals("hunter", migrated.archetypeId());
        assertEquals("fire", migrated.elementId());
        assertEquals(7, migrated.level());
        assertEquals(1234, migrated.experience());
        assertEquals(List.of("solar_grenade"), migrated.unlockedAbilities());
        assertEquals(99L, migrated.lastSeenEpochMillis());
    }

    /**
     * The other half, and without it the row above is equally consistent with the step CLOBBERING
     * every profile's slot on every load -- which would silently undo the setting for everyone who
     * ever changes it.
     *
     * <h2>*** RENAMED FROM {@code aV3Profiles...} WHEN THE STAMP WENT TO 4, AND THE RENAME IS THE
     * POINT ***</h2>
     *
     * <p>This row stages {@code CURRENT_SCHEMA_VERSION}, symbolically -- which is correct for what
     * it tests (at the current stamp, no step runs) and means <b>it stopped being about v3 the
     * instant the constant moved.</b> The name said v3 and the fixture said 4; nothing went red,
     * because the row was still true of whatever it now staged.
     *
     * <p><b>A FIXTURE PINNED TO A MOVING CONSTANT SILENTLY CHANGES WHAT IT TESTS.</b> The coverage
     * that quietly vanished -- a profile at the PREVIOUS version keeping its chosen slot across a
     * real bump -- is restored below by a row staged on the literal {@code 3}.
     */
    @Test
    void aCurrentProfilesCHOSENSlotSurvivesMigration_includingSlotZero() {
        PlayerProfile chose3 = at(PlayerProfile.CURRENT_SCHEMA_VERSION);
        assertEquals(3, ProfileMigrations.migrate(chose3).nexusSlot(),
                "already at the current stamp: no step runs, so a chosen slot is untouched");

        PlayerProfile chose0 = new PlayerProfile(PlayerProfile.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID(), "hunter", "fire", 7, 1234, List.of(), 99L, 0, 56_780L, null, false);
        assertEquals(0, ProfileMigrations.migrate(chose0).nexusSlot(),
                "SLOT ZERO IS A LEGAL CHOICE at v3 and must survive -- this is the case the "
                        + "stamp is what distinguishes, and the whole reason the default is not "
                        + "applied in the compact constructor");
    }

    /**
     * The v3 -&gt; v4 step: <b>it stamps and sets nothing</b>, and both halves are asserted.
     *
     * <p>Staged on the LITERAL 3 rather than {@code CURRENT_SCHEMA_VERSION - 1}, so this row cannot
     * drift when the constant moves again -- the row above is the account of what that drift costs.
     *
     * <p><b>The {@code nexusSlot} assertion is the one that matters and it is not about the vault.</b>
     * A v3 profile's slot is a real CHOICE, and the new step runs over it. A step that reached for
     * {@code withNexusSlot} by copy-paste -- the shape of the step directly above it -- would reset
     * every existing player's star to slot 8 on their next login, with nothing going red.
     */
    @Test
    void versionThreeIsStampedToFourAndSetsNothing() {
        PlayerProfile v3 = new PlayerProfile(3, UUID.randomUUID(), "hunter", "fire", 7, 1234,
                List.of("solar_grenade"), 99L, 3, 56_780L, null, false);

        PlayerProfile migrated = ProfileMigrations.migrate(v3);

        assertEquals(4, migrated.schemaVersion());
        assertFalse(migrated.vaultMigrated(),
                "the step sets no value -- absent already means NOT MIGRATED, which is correct");
        assertEquals(3, migrated.nexusSlot(),
                "a v3 slot is a CHOICE and the v3 -> v4 step must not touch it");
        assertEquals("hunter", migrated.archetypeId());
        assertEquals(56_780L, migrated.lifetimeXp());
        assertTrue(migrated.starEnabled(), "an absent starEnabled still reads as ENABLED at v4");
    }

    /**
     * An already-migrated profile keeps the flag across a load.
     *
     * <p><b>The mirror of the row above, and without it that row is equally consistent with the step
     * CLEARING the flag</b> -- which would re-run every player's migration on every login, copying
     * their ender chest into page 1 again on each one. The most expensive possible version of this
     * bug, and a stamp-only step passes the row above whether it clears the flag or not.
     */
    @Test
    void anAlreadyMigratedProfileStaysMigrated() {
        PlayerProfile migratedAtV3 = new PlayerProfile(3, UUID.randomUUID(), "hunter", "fire", 7,
                1234, List.of("solar_grenade"), 99L, 3, 56_780L, null, true);

        PlayerProfile after = ProfileMigrations.migrate(migratedAtV3);

        assertEquals(4, after.schemaVersion());
        assertTrue(after.vaultMigrated(), "the flag is data, not something a stamp step rewrites");
    }

    /** {@code withVaultMigrated} carries every other field, both ways. */
    @Test
    void withVaultMigratedCarriesTheRestAndGoesBothWays() {
        PlayerProfile before = at(PlayerProfile.CURRENT_SCHEMA_VERSION);
        assertFalse(before.vaultMigrated());

        PlayerProfile after = before.withVaultMigrated(true);

        assertTrue(after.vaultMigrated());
        assertEquals(before.playerId(), after.playerId());
        assertEquals(before.nexusSlot(), after.nexusSlot());
        assertEquals(before.lifetimeXp(), after.lifetimeXp());
        assertEquals(before.schemaVersion(), after.schemaVersion());
        assertEquals(before.unlockedAbilities(), after.unlockedAbilities());

        // BOTH WAYS, because an operator re-running one player's migration is a supported action
        // and the accessor's javadoc prices what it costs.
        assertFalse(after.withVaultMigrated(false).vaultMigrated());
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
        var profile = new PlayerProfile(2, UUID.randomUUID(), "none", "none", 1, 0, null, 0L, 3, 0L, null, false);
        assertEquals(List.of(), profile.unlockedAbilities());
    }

    @Test
    void nullElementIdBecomesNone() {
        var profile = new PlayerProfile(2, UUID.randomUUID(), "ranger", null, 1, 0, List.of(), 0L, 3, 0L, null, false);
        assertEquals(PlayerProfile.NONE, profile.elementId());
    }

    @Test
    void unlockedAbilitiesIsDefensivelyCopied() {
        var mutable = new java.util.ArrayList<>(List.of("a"));
        var profile = new PlayerProfile(2, UUID.randomUUID(), "none", "none", 1, 0, mutable, 0L, 3, 0L, null, false);

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
