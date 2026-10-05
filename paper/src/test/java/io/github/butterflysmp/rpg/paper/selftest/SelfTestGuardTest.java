package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The seat's ruling b: a dev server, the dev world, exactly one player -- each refusal on its own, and the one yes. */
class SelfTestGuardTest {

    @Test
    void itRunsOnlyOnADevServerInTheDevWorldWithOnePlayer() {
        assertEquals(Optional.empty(), SelfTestGuard.refusal(true, true, 1));
    }

    @Test
    void withoutTheDevMarkerItRefusesAndSaysWhereTheMarkerComesFrom() {
        String why = SelfTestGuard.refusal(false, true, 1).orElseThrow();
        assertTrue(why.contains("-Drpg.dev=true"), why);
        assertTrue(why.contains("dev-server.sh"), why);
    }

    @Test
    void outsideTheDevWorldItRefuses() {
        assertTrue(SelfTestGuard.refusal(true, false, 1).orElseThrow().contains("dev world"));
    }

    @Test
    void aSecondPlayerOnlineIsRefusedAndSoIsNoneAtAll() {
        assertTrue(SelfTestGuard.refusal(true, true, 2).orElseThrow().contains("2 players"));
        assertTrue(SelfTestGuard.refusal(true, true, 0).isPresent(), "a console run has no player to drive");
    }

    @Test
    void theMarkerIsCheckedFirstSoAProductionServerNeverReportsAnythingElse() {
        assertTrue(SelfTestGuard.refusal(false, false, 5).orElseThrow().contains("not a dev server"));
    }
}
