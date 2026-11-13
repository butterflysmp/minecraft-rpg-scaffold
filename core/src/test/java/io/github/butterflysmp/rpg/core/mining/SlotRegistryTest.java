package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Placed slots, in memory (the seat's ruling 4, 2026-11-13: no dev file; lost on restart). The
 * registry answers "is this block under a slot" for the BlockDamageEvent handler and "which slots
 * touch this chunk" for the chunk-load render. It is NOT the area model.
 */
class SlotRegistryTest {

    private static Slot slot(String world, int x, int y, int z, WallFace face) {
        return Slot.of(world, new FaceFrame(x, y, z, face, 3, 2), "iron", "small");
    }

    @Test
    void theIdNamesTheWorldTheAnchorAndTheFace() {
        assertEquals("w1:10,64,20:NORTH", slot("w1", 10, 64, 20, WallFace.NORTH).id());
    }

    @Test
    void everyFootprintBlockFindsItsSlotAndNoOtherBlockDoes() {
        SlotRegistry slots = new SlotRegistry();
        Slot s = slot("w1", 10, 64, 20, WallFace.NORTH);
        slots.place(s);
        for (BlockPos b : s.frame().footprint()) {
            assertEquals(s, slots.at("w1", b).orElseThrow(), b.toString());
        }
        assertTrue(slots.at("w1", new BlockPos(12, 64, 20)).isEmpty(), "beside the footprint");
        assertTrue(slots.at("w1", new BlockPos(10, 66, 20)).isEmpty(), "above it");
        assertTrue(slots.at("w2", new BlockPos(10, 64, 20)).isEmpty(), "another world");
    }

    @Test
    void anOverlappingSlotIsRefusedAndNamesTheSharedBlock() {
        SlotRegistry slots = new SlotRegistry();
        slots.place(slot("w1", 10, 64, 20, WallFace.NORTH));
        var ex = assertThrows(IllegalArgumentException.class,
                () -> slots.place(slot("w1", 12, 65, 20, WallFace.NORTH)));
        assertTrue(ex.getMessage().contains("11,65,20"), ex.getMessage());
        assertEquals(1, slots.all().size());
        // A slot in another world at the same coordinates is fine.
        slots.place(slot("w2", 10, 64, 20, WallFace.NORTH));
        assertEquals(2, slots.all().size());
        // Mutation: skip the overlap check -> the throw is missing -> reddens.
    }

    @Test
    void removingASlotFreesItsBlocks() {
        SlotRegistry slots = new SlotRegistry();
        Slot s = slot("w1", 10, 64, 20, WallFace.NORTH);
        slots.place(s);
        assertEquals(s, slots.remove(s.id()).orElseThrow());
        assertTrue(slots.at("w1", new BlockPos(10, 64, 20)).isEmpty());
        assertTrue(slots.inChunk("w1", 0, 1).isEmpty());
        slots.place(slot("w1", 12, 65, 20, WallFace.NORTH));   // the old overlap is legal now
    }

    @Test
    void aSlotStraddlingAChunkEdgeIsListedInBothChunks() {
        // x 15..17 crosses the chunk edge between cx 0 (x 0..15) and cx 1 (x 16..31). z 20 is cz 1.
        SlotRegistry slots = new SlotRegistry();
        Slot s = slot("w1", 16, 64, 20, WallFace.NORTH);
        slots.place(s);
        assertEquals(List.of(s), slots.inChunk("w1", 0, 1));
        assertEquals(List.of(s), slots.inChunk("w1", 1, 1));
        assertTrue(slots.inChunk("w1", 2, 1).isEmpty());
        assertTrue(slots.inChunk("w1", -1, 1).isEmpty());
    }

    @Test
    void negativeCoordinatesUseFloorChunks() {
        SlotRegistry slots = new SlotRegistry();
        Slot s = slot("w1", -1, 64, -1, WallFace.SOUTH);   // x -2..0, z -1
        slots.place(s);
        assertEquals(List.of(s), slots.inChunk("w1", -1, -1));
        assertEquals(List.of(s), slots.inChunk("w1", 0, -1), "x 0 is chunk 0");
        // Mutation: x / 16 instead of x >> 4 -> x -1 lands in chunk 0, not -1 -> reddens.
    }
}
