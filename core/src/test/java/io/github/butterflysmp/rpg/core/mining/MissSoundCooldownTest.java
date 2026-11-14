package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat's S2 ruling 9, on Ben's "Miss sound should have a slight cooldown, let's say .4 seconds":
 * {@link MiningNotices#MISS_SOUND_COOLDOWN_TICKS} = 8 server ticks, per player, counted from the last
 * miss sound that actually PLAYED. A miss inside it is still a miss; it only plays nothing.
 */
class MissSoundCooldownTest {

    private static final UUID BEN = new UUID(1, 1);
    private static final UUID OTHER = new UUID(2, 2);

    @Test
    void theFirstMissPlays() {
        assertTrue(new MissSoundCooldown().tryPlay(BEN, 100));
    }

    @Test
    void sevenTicksLaterIsSilentAndEightTicksLaterPlays() {
        MissSoundCooldown c = new MissSoundCooldown();
        assertTrue(c.tryPlay(BEN, 100));
        assertFalse(c.tryPlay(BEN, 107), "7 ticks: inside the cooldown");
        assertTrue(c.tryPlay(BEN, 108), "8 ticks: exactly the cooldown, so it plays");
        // Mutations: "<" -> "<=" (8 is silent) reddens the 108 row; "< 8" -> "< 7" reddens the 107 row.
    }

    @Test
    void theCooldownCountsFromTheLastSoundThatPlayedNotTheLastMiss() {
        MissSoundCooldown c = new MissSoundCooldown();
        assertTrue(c.tryPlay(BEN, 100));
        assertFalse(c.tryPlay(BEN, 107), "a silent miss");
        assertTrue(c.tryPlay(BEN, 114), "14 after the last PLAYED (100); only 7 after the last miss (107)");
        // Mutation: record every miss, not only a played one -> 114 - 107 = 7 is silent -> reddens.
    }

    @Test
    void eachPlayerHasTheirOwnCooldown() {
        MissSoundCooldown c = new MissSoundCooldown();
        assertTrue(c.tryPlay(BEN, 100));
        assertTrue(c.tryPlay(OTHER, 101), "OTHER never played");
        assertFalse(c.tryPlay(BEN, 102));
    }

    @Test
    void forgettingAPlayerLetsTheNextMissPlay() {
        MissSoundCooldown c = new MissSoundCooldown();
        assertTrue(c.tryPlay(BEN, 100));
        c.forget(BEN);
        assertTrue(c.tryPlay(BEN, 101));
    }
}
