package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.build.StoneInput.Input;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The whole grid: 3 inputs x main/off hand x screen open/closed = 12 cells, each asserted. Exactly 3
 * cells cast; the other 9 refuse.
 */
class StoneInputTest {

    private static final Map<Input, LoadoutSlot> EXPECTED = Map.of(
            Input.LEFT, LoadoutSlot.ACTIVE_1,
            Input.RIGHT, LoadoutSlot.ACTIVE_2,
            Input.DROP, LoadoutSlot.ULTIMATE);

    @Test
    void theWholeGrid() {
        int casting = 0;
        for (Input input : Input.values()) {
            for (boolean mainHand : new boolean[] {true, false}) {
                for (boolean screenOpen : new boolean[] {true, false}) {
                    Optional<LoadoutSlot> got = StoneInput.slotFor(input, mainHand, screenOpen);
                    Optional<LoadoutSlot> want = mainHand && !screenOpen
                            ? Optional.of(EXPECTED.get(input)) : Optional.empty();
                    assertEquals(want, got, input + " mainHand=" + mainHand + " screenOpen=" + screenOpen);
                    if (got.isPresent()) casting++;
                }
            }
        }
        assertEquals(3, casting, "exactly one casting cell per input");
    }

    /** Named on its own because it is the clarification under RULINGS, and ST13 reads it in play. */
    @Test
    void qWithAScreenOpenCastsNothing() {
        assertEquals(Optional.empty(), StoneInput.slotFor(Input.DROP, true, true));
    }

    /** The measured double-fire: a right-click on a block arrives once per hand in one tick. */
    @Test
    void theOffHandHalfOfARightClickCastsNothing() {
        assertEquals(Optional.empty(), StoneInput.slotFor(Input.RIGHT, false, false));
        assertEquals(Optional.of(LoadoutSlot.ACTIVE_2), StoneInput.slotFor(Input.RIGHT, true, false));
    }
}
