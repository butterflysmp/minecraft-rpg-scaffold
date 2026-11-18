package io.github.butterflysmp.rpg.paper.fight;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Who takes part in the dev fixture (GATE-fight-ring.md): C's world, within 128 of C, survival or adventure. */
class FightParticipantsTest {

    @Test
    void survivalAndAdventureWithinTheRadiusTakePart() {
        assertTrue(FightParticipants.takesPart(true, 0.0, GameMode.SURVIVAL, 128.0));
        assertTrue(FightParticipants.takesPart(true, 128.0, GameMode.ADVENTURE, 128.0), "at the radius, inclusive");
    }

    @Test
    void creativeAndSpectatorNeverTakePart() {
        assertFalse(FightParticipants.takesPart(true, 1.0, GameMode.CREATIVE, 128.0));
        assertFalse(FightParticipants.takesPart(true, 1.0, GameMode.SPECTATOR, 128.0));
    }

    @Test
    void anotherWorldOrPastTheRadiusDoesNot() {
        assertFalse(FightParticipants.takesPart(false, 1.0, GameMode.SURVIVAL, 128.0));
        assertFalse(FightParticipants.takesPart(true, 128.01, GameMode.SURVIVAL, 128.0));
    }
}
