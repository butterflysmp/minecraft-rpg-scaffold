package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The {@code SPLIT} trace line's exact shape (Airburst Flames, 2026-11-23): the gate's split row reads it word for word. */
class SplitLineTest {

    @Test
    void theLineNamesTheCasterTheSourceThePointTheBurstsAndTheTick() {
        assertEquals("SPLIT caster=00000000-0000-0000-0000-000000000001 source=living_lantern at=14.000,65.620,-0.500"
                        + " bursts=3 tick=1234",
                PaperCombatWorld.splitLine(new UUID(0, 1), "living_lantern", new Vec3(14, 65.62, -0.5), 3, 1234));
    }
}
