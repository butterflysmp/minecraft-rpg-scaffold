package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@code FOCUSED} trace line's exact shape (Focused Beam, 2026-11-24): the gate's rows read the press's tick and the
 * beam's from it word for word, since PLAYERHIT carries no tick.
 */
class FocusedLineTest {

    @Test
    void theLineNamesTheCasterTheSourceTheOriginTheRangeThePressAndTheTick() {
        assertEquals("FOCUSED caster=00000000-0000-0000-0000-000000000001 source=cursed_emerald/right_click"
                        + " from=104.500,202.620,-3.500 range=48.000 pressed=1214 tick=1234",
                PaperCombatWorld.focusedLine(new UUID(0, 1), "cursed_emerald/right_click", new Vec3(104.5, 202.62, -3.5),
                        48, 1214, 1234));
    }
}
