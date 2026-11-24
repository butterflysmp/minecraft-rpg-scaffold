package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The {@code ORB} trace lines' exact shape (Lapis Crystal's D2, 2026-11-23): the gate's orb rows read them word for word. */
class OrbLineTest {

    @Test
    void theRestLineNamesTheOwnerThePointAndTheTick() {
        assertEquals("ORB rest owner=00000000-0000-0000-0000-000000000001 at=12.500,65.620,0.500 tick=200",
                PaperCombatWorld.orbLine(new UUID(0, 1), "rest", new Vec3(12.5, 65.62, 0.5), "", 200));
    }

    @Test
    void theRemovedLineAddsItsCause() {
        assertEquals("ORB removed owner=00000000-0000-0000-0000-000000000001 at=12.500,65.620,0.500 cause=expired tick=500",
                PaperCombatWorld.orbLine(new UUID(0, 1), "removed", new Vec3(12.5, 65.62, 0.5), "expired", 500));
    }
}
