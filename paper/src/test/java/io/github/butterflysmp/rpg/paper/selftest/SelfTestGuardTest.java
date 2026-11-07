package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat's ruling b: a dev server, the dev world, exactly one player -- and, since 2026-11-06, SURVIVAL. Each
 * refusal on its own, and the one yes.
 */
class SelfTestGuardTest {

    @Test
    void itRunsOnlyOnADevServerInTheDevWorldWithOnePlayerInSurvival() {
        assertEquals(Optional.empty(), SelfTestGuard.refusal(true, true, 1, "SURVIVAL"));
    }

    @Test
    void withoutTheDevMarkerItRefusesAndSaysWhereTheMarkerComesFrom() {
        String why = SelfTestGuard.refusal(false, true, 1, "SURVIVAL").orElseThrow();
        assertTrue(why.contains("-Drpg.dev=true"), why);
        assertTrue(why.contains("dev-server.sh"), why);
    }

    @Test
    void outsideTheDevWorldItRefuses() {
        assertTrue(SelfTestGuard.refusal(true, false, 1, "SURVIVAL").orElseThrow().contains("dev world"));
    }

    @Test
    void aSecondPlayerOnlineIsRefusedAndSoIsNoneAtAll() {
        assertTrue(SelfTestGuard.refusal(true, true, 2, "SURVIVAL").orElseThrow().contains("2 players"));
        assertTrue(SelfTestGuard.refusal(true, true, 0, "SURVIVAL").isPresent(), "a console run has no player to drive");
    }

    /** The 38cc9903 boot's run 1 was CREATIVE and certified nothing. Every other mode is refused the same way. */
    @Test
    void everyModeButSurvivalIsRefusedAndTheReplyNamesTheModeAndTheFix() {
        for (String mode : new String[] {"CREATIVE", "ADVENTURE", "SPECTATOR"}) {
            String why = SelfTestGuard.refusal(true, true, 1, mode).orElseThrow();
            assertTrue(why.contains("you are in " + mode), why);
            assertTrue(why.contains("/gamemode survival"), why);
        }
    }

    @Test
    void theMarkerIsCheckedFirstSoAProductionServerNeverReportsAnythingElse() {
        assertTrue(SelfTestGuard.refusal(false, false, 5, "CREATIVE").orElseThrow().contains("not a dev server"));
    }

    /** The mode is checked last: a wrong world or a second player is the more basic reason, and is reported first. */
    @Test
    void theModeIsCheckedAfterTheWorldAndThePlayerCount() {
        assertTrue(SelfTestGuard.refusal(true, false, 1, "CREATIVE").orElseThrow().contains("dev world"));
        assertTrue(SelfTestGuard.refusal(true, true, 2, "CREATIVE").orElseThrow().contains("2 players"));
    }
}
