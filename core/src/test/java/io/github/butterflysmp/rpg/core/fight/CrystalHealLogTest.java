package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9b, section 2 (Ben: "No crystals should not heal the dragon"). Vanilla heals the dragon from its nearest crystal
 * every ten ticks; each heal is refused, and the refusal is LOGGED ONCE PER CRYSTAL LIT, not per heal.
 */
class CrystalHealLogTest {

    @Test
    void theFirstRefusalUnderALitCrystalIsLoggedAndTheRestAreNot() {
        CrystalHealLog log = new CrystalHealLog();
        assertTrue(log.shouldLog(1));
        assertFalse(log.shouldLog(1));
        assertFalse(log.shouldLog(1), "one line per crystal, not one per heal");
    }

    @Test
    void theNextCrystalLitIsLoggedAgain() {
        CrystalHealLog log = new CrystalHealLog();
        assertTrue(log.shouldLog(1));
        assertTrue(log.shouldLog(2), "a second crystal lit is a new line");
        assertFalse(log.shouldLog(2));
    }

    @Test
    void afterTheCrystalsGoOutTheFirstOfTheNextCycleIsLoggedAgain() {
        CrystalHealLog log = new CrystalHealLog();
        assertTrue(log.shouldLog(1));
        log.crystalsNow(0);                    // the Mage died: every lit crystal went out
        assertTrue(log.shouldLog(1), "the next cycle's first crystal is a new crystal, though its count is the same");
    }

    @Test
    void aHealWithNoCrystalOfOursLitIsLoggedOnceUntilOneIs() {
        CrystalHealLog log = new CrystalHealLog();
        assertTrue(log.shouldLog(0));
        assertFalse(log.shouldLog(0));
        assertTrue(log.shouldLog(1));
    }

    @Test
    void aCountThatHasNotChangedDoesNotResetTheLine() {
        CrystalHealLog log = new CrystalHealLog();
        assertTrue(log.shouldLog(2));
        log.crystalsNow(2);
        assertFalse(log.shouldLog(2), "only the crystals going out starts a new cycle");
    }
}
