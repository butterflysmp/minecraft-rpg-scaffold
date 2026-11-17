package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The beam's sounds as content (the follow-up of 2026-11-17). Ben: "The bell plays with the warning. The real shot
 * comes when the resonance part starts." Which key is a real sound is the loader's question (it needs the server's
 * registry); this pins the numbers and the two lists.
 */
class BeamSoundTest {

    private static final BeamSound STRIKE = new BeamSound("block.bell.use", 1.0, 1.0);
    private static final BeamSound RESONANCE = new BeamSound("block.bell.resonate", 1.4, 1.0);
    private static final BeamSound CHIME = new BeamSound("block.amethyst_block.chime", 1.0, 1.0);

    @Test
    void theTwoListsArriveInOrderAndAreTheBeamsOwn() {
        MobBeam b = new MobBeam(100, 200, 26, List.of(0, 20), 40, 0.6, 1.0, 65, List.of(STRIKE),
                List.of(RESONANCE, CHIME));
        assertEquals(List.of(STRIKE), b.telegraphSounds());
        assertEquals(List.of(RESONANCE, CHIME), b.fireSounds(), "the resonance first, then the chime, as authored");
    }

    @Test
    void theEightNumberFormHasNoSounds() {
        MobBeam b = new MobBeam(100, 200, 12, List.of(0, 9), 40, 0.6, 1.0, 35);
        assertTrue(b.telegraphSounds().isEmpty());
        assertTrue(b.fireSounds().isEmpty());
    }

    @Test
    void eachBadSoundIsRefusedByName() {
        refused("beam.sounds key", () -> new BeamSound(" ", 1, 1));
        refused("beam.sounds key", () -> new BeamSound(null, 1, 1));
        refused("beam.sounds volume", () -> new BeamSound("block.bell.use", -0.1, 1));
        refused("beam.sounds volume", () -> new BeamSound("block.bell.use", Double.NaN, 1));
        refused("beam.sounds pitch", () -> new BeamSound("block.bell.use", 1, 0));
        refused("beam.sounds pitch", () -> new BeamSound("block.bell.use", 1, Double.POSITIVE_INFINITY));
    }

    @Test
    void aSilentSoundIsAllowed() {
        assertEquals(0.0, new BeamSound("block.bell.use", 0, 1).volume(), 0.0);
    }

    private static void refused(String name, Runnable r) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, r::run);
        assertTrue(e.getMessage().contains(name), e.getMessage());
    }
}
