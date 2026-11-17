package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The teleport's numbers (PLAN-dragon-mobs.md step R6), and the load-time relation to the beam's cycle. */
class MobTeleportTest {

    private static final MobBeam BEAM = new MobBeam(100, 200, 12, List.of(0, 9), 40, 0.6, 1.0, 35);

    @Test
    void eachBadNumberIsRefusedByName() {
        refused("teleport.delay_ticks", () -> new MobTeleport(0, 30, 70, 5, 3, true));
        refused("teleport.arc_degrees.min", () -> new MobTeleport(12, -1, 70, 5, 3, true));
        refused("teleport.arc_degrees.max", () -> new MobTeleport(12, 30, 29, 5, 3, true));
        refused("teleport.arc_degrees.max", () -> new MobTeleport(12, 30, 181, 5, 3, true));
        refused("teleport.spots_per_side", () -> new MobTeleport(12, 30, 70, 0, 3, true));
        refused("teleport.level_tolerance", () -> new MobTeleport(12, 30, 70, 5, -1, true));
    }

    @Test
    void aTeleportNeedsABeam() {
        MobTeleport t = new MobTeleport(12, 30, 70, 5, 3, true);
        refused("needs a beam", () -> new MobDefinition("knell", "wither_skeleton", "Knell", 360, MobBody.VANILLA,
                "void", null, null, null, null, null, t));
    }

    /**
     * THE LOAD-TIME CHECK (the brief's ruling 5): a delay not shorter than the cycle's minimum is refused, the mob and
     * both numbers named. Equal is refused (the move and the next attempt could share a tick); one less is allowed.
     */
    @Test
    void aDelayNotShorterThanTheCycleMinimumIsRefusedNamingBothNumbers() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> knell(100));
        assertTrue(e.getMessage().contains("knell"), e.getMessage());
        assertTrue(e.getMessage().contains("teleport.delay_ticks (100)"), e.getMessage());
        assertTrue(e.getMessage().contains("beam.cycle_ticks.min (100)"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> knell(150));
        assertEquals(99, knell(99).teleport().delayTicks());
    }

    private static MobDefinition knell(int delay) {
        return new MobDefinition("knell", "wither_skeleton", "Knell", 360, MobBody.VANILLA, "void", null, null, null,
                null, BEAM, new MobTeleport(delay, 30, 70, 5, 3, true));
    }

    private static void refused(String name, Runnable r) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, r::run);
        assertTrue(e.getMessage().contains(name), e.getMessage());
    }
}
