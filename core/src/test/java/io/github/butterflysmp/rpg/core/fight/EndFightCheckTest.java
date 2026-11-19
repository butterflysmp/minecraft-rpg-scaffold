package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9c, section 3 c: IS VANILLA'S DRAGON FIGHT INERT IN THIS END WORLD? From READ facts only (the pinned server jar):
 * <ul>
 *   <li>{@code EnderDragonFight.tick} starts with {@code dragonEvent.setVisible(!dragonKilled)}, so after one tick its
 *       bar is hidden exactly when {@code dragonKilled} is true;</li>
 *   <li>Paper's branch in {@code init} sets {@code dragonKilled} true together with {@code needsStateScanning} false
 *       (no scan, so no exit portal and nothing adopted), and while {@code dragonKilled} the tick never calls
 *       {@code findOrCreateDragon};</li>
 *   <li>so: the bar hidden, no dragon of its own, no exit portal recorded, no bedrock at the centre, and no dragon in
 *       the world before ours.</li>
 * </ul>
 */
class EndFightCheckTest {

    private static EndFightCheck.Reading inert() {
        return new EndFightCheck.Reading(true, false, false, false, 0, 0);
    }

    @Test
    void theBarHiddenAndNothingElseIsInert() {
        assertTrue(EndFightCheck.inert(inert()));
    }

    @Test
    void aVisibleBarMeansTheSwitchWasNotReadAndTheFightIsLive() {
        assertFalse(EndFightCheck.inert(new EndFightCheck.Reading(true, true, false, false, 0, 0)));
    }

    @Test
    void aDragonOfItsOwnIsNotInert() {
        assertFalse(EndFightCheck.inert(new EndFightCheck.Reading(true, false, true, false, 0, 0)));
    }

    @Test
    void anExitPortalRecordedIsNotInert() {
        assertFalse(EndFightCheck.inert(new EndFightCheck.Reading(true, false, false, true, 0, 0)));
    }

    @Test
    void bedrockAtTheCentreIsNotInert() {
        assertFalse(EndFightCheck.inert(new EndFightCheck.Reading(true, false, false, false, 4, 0)));
    }

    @Test
    void aDragonInTheWorldBeforeOursIsNotInert() {
        assertFalse(EndFightCheck.inert(new EndFightCheck.Reading(true, false, false, false, 0, 1)));
    }

    @Test
    void noFightObjectAtAllIsInertToo() {
        // An END world whose type builds no fight (READ: ServerLevel builds one only if has_ender_dragon_fight).
        assertTrue(EndFightCheck.inert(new EndFightCheck.Reading(false, false, false, false, 0, 0)));
    }

    @Test
    void theLineNamesEveryReadingAndTheVerdict() {
        assertEquals("FIGHTWORLD end-check name=w inert=true fight=present bar_visible=false fight_dragon=none"
                + " portal=none bedrock_at_centre=0 dragons=0", EndFightCheck.line("w", inert()));
        assertEquals("FIGHTWORLD end-check name=w inert=FALSE fight=present bar_visible=true fight_dragon=none"
                        + " portal=none bedrock_at_centre=0 dragons=0",
                EndFightCheck.line("w", new EndFightCheck.Reading(true, true, false, false, 0, 0)));
    }
}
