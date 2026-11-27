package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SHIPPED ROWS THE SEAT'S R2 AND R6 CHANGED (2026-11-26, the readings of the boot of {@code 1f447197}), read from the
 * scenarios the server loads.
 */
class OwedRowShapesTest {

    private static List<Scenario.Step> steps(String gate, String row) {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(
                path -> OwedRowShapesTest.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), loaded.refusals());
        return loaded.gates().get(gate).stream().filter(s -> s.row().equals(row)).findFirst().orElseThrow().steps();
    }

    /**
     * R6: BF6's points read 0.029 east of the aligned stand, because his feet moved during the 40 ticks between the align
     * and the press. The row now aligns him again RIGHT BEFORE its press, with the shortest wait the TURN_THEN_PRESS lint
     * allows. Mutation: the second align removed, or the wait before the press lengthened past 2.
     */
    @Test
    void bf6AlignsHimRightBeforeItsPress() {
        List<Scenario.Step> s = steps("blast-fungus", "BF6");
        int press = -1;
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).verb() == Scenario.Verb.USE && s.get(i).arg().trim().equals("right_click")) press = i;
        }
        assertTrue(press >= 2, "control: BF6's right-click press was found");
        Scenario.Step wait = s.get(press - 1);
        Scenario.Step align = s.get(press - 2);
        assertEquals(Scenario.Verb.WAIT, wait.verb());
        long ticks = Long.parseLong(wait.arg().trim());
        assertTrue(ticks >= 1 && ticks <= 2, "one or two ticks between the align and the press, read " + ticks);
        assertEquals(Scenario.Verb.CMD, align.verb());
        assertEquals("execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 90", align.arg().trim(),
                "straight down, block-centred, as BF5");
    }

    /**
     * R2: every row of the Blink Tome's gate that equips the tome takes it off and waits past the reconcile pass before
     * its LAST clear_state, so no cleared cooldown can let him blink again. Mutation: one row's pair removed (the lint's
     * own test is {@code AccessoryArmedAtClearLintTest}; this is the shipped file's).
     */
    @Test
    void everyBlinkingRowTakesTheTomeOffBeforeItsLastClear() {
        for (int n = 2; n <= 10; n++) {
            List<Scenario.Step> s = steps("blink-tome", "BT" + n);
            int last = -1;
            for (int i = 0; i < s.size(); i++) if (s.get(i).verb() == Scenario.Verb.CLEAR_STATE) last = i;
            assertTrue(last >= 2, "BT" + n + ": its last clear_state was found");
            assertEquals(Scenario.Verb.ACCESSORY, s.get(last - 2).verb(), "BT" + n);
            assertEquals("clear", s.get(last - 2).arg().trim(), "BT" + n);
            assertEquals(Scenario.Verb.WAIT, s.get(last - 1).verb(), "BT" + n);
            assertEquals(ScenarioLint.ACCESSORY_SETTLE_TICKS, Long.parseLong(s.get(last - 1).arg().trim()), "BT" + n);
        }
    }
}
