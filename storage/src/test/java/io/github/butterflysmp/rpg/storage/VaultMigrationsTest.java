package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vault's migration chain, and its newer-server refusal.
 *
 * <h2>EVERY ROW HERE CAUSES THE CONDITION. NONE ASSERTS THAT AN ARM EXISTS.</h2>
 *
 * The v0 step is unreachable from any file this project has ever written -- v1 is the first version
 * the vault ever had -- so its only exercise is this file. A test that asserted the arm was PRESENT
 * would pass against an arm that can never run, which is how a guard comes to carry a javadoc
 * promising protection it does not provide. So the fixture is staged at version 0 on purpose and the
 * stamp is read back.
 *
 * <p>The fixture stamps 9 for the newer-server case: not 2 (the next real version, which would pass
 * whether the comparison were {@code >} or {@code >=} against a bumped constant), and not equal to
 * any page, slot or count elsewhere in these tests.
 */
class VaultMigrationsTest {

    private static final UUID PLAYER = UUID.fromString("3f8c1a05-6b47-4e29-9d3a-c05e71b28f64");

    @Test
    void aVaultFromANewerServerIsRefusedRatherThanSilentlyDowngraded() {
        PlayerVault fromTheFuture = new PlayerVault(9, PLAYER, List.of(new VaultEntry(2, 17, "x")));

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> VaultMigrations.migrate(fromTheFuture));

        assertTrue(thrown.getMessage().contains("9"), "name the version found");
        assertTrue(thrown.getMessage().contains(String.valueOf(PlayerVault.CURRENT_SCHEMA_VERSION)),
                "and the version understood, so the reader knows which side is behind");
        assertTrue(thrown.getMessage().contains(PLAYER.toString()), "and whose vault it is");
        assertTrue(thrown.getMessage().contains("Refusing"),
                "loading it would drop the fields we do not know and the next save would make that"
                        + " loss permanent -- the refusal is the whole point");
    }

    /**
     * *** THE CONDITION IS CAUSED, NOT ASSERTED. *** Gson leaves an absent int at 0, so this is the
     * shape a vault file that lost its stamp arrives in.
     */
    @Test
    void aVaultWithNoStampIsBroughtUpToVersionOne() {
        PlayerVault unstamped = new PlayerVault(0, PLAYER, List.of(new VaultEntry(3, 24, "kept")));

        PlayerVault migrated = VaultMigrations.migrate(unstamped);

        assertEquals(1, migrated.schemaVersion(), "the v0 -> v1 step must actually stamp");
    }

    /** A stamp must not be an excuse to lose the contents it was stamped onto. */
    @Test
    void theStampCarriesTheContentsAndTheOwner() {
        PlayerVault unstamped = new PlayerVault(0, PLAYER,
                List.of(new VaultEntry(3, 24, "kept"), new VaultEntry(5, 31, "also kept")));

        PlayerVault migrated = VaultMigrations.migrate(unstamped);

        assertEquals(PLAYER, migrated.playerId());
        assertEquals(2, migrated.occupiedSlots());
        assertEquals("kept", migrated.page(3).get(24));
        assertEquals("also kept", migrated.page(5).get(31));
    }

    /**
     * A vault already at the current version passes through untouched -- the SAME instance, which is
     * the strongest available statement that no step fired. An equal-but-rebuilt record would look
     * identical here and would mean a step had run.
     */
    @Test
    void aCurrentVaultIsReturnedUnchanged() {
        PlayerVault current = new PlayerVault(PlayerVault.CURRENT_SCHEMA_VERSION, PLAYER,
                List.of(new VaultEntry(2, 17, "x")));

        assertSame(current, VaultMigrations.migrate(current));
    }
}
