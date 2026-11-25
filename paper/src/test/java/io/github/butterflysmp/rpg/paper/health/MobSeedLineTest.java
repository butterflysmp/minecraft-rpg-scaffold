package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * S1 (the seat's ruling of 2026-11-24): the MOBSEED line carries where the mob stands and how far that is from the
 * selftest pad's stand point, so a boot shows where seeded mobs stand before any sweep is decided.
 */
class MobSeedLineTest {

    private static final UUID ID = UUID.fromString("d0a45bad-0e58-471a-8f63-15efac945778");

    @Test
    void theLineEndsWithThePositionAndTheDistanceFromTheStand() {
        // The stand point of the boot at 5d72bec7: 104.50 201.00 -3.50. A bee 140 below and 3 east of it.
        String line = MobNameplateManager.seedLine(ID, "bee", "-", "passive", 50, 107.5, 61.0, -3.5, 104.5, 201.0, -3.5);
        assertEquals("MOBSEED d0a45bad-0e58-471a-8f63-15efac945778 bee gs=- source=passive max=50 at=107.50 61.00 -3.50"
                + " stand_d=140.03", line);
    }

    @Test
    void theOldPrefixIsUnchanged() {
        String line = MobNameplateManager.seedLine(ID, "drowned", "111", "stored", 111, 0, 0, 0, 0, 0, 0);
        assertEquals("MOBSEED d0a45bad-0e58-471a-8f63-15efac945778 drowned gs=111 source=stored max=111",
                line.substring(0, line.indexOf(" at=")), "every reading that matched the old line still matches");
        assertEquals(" at=0.00 0.00 0.00 stand_d=0.00", line.substring(line.indexOf(" at=")));
    }
}
