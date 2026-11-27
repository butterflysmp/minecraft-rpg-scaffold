package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S R4 (2026-11-26, the readings of the boot of {@code 1f447197}): BF3's three item samples read {@code ITEM n=7}
 * and BF8 {@code ENTITIES ITEM count=4}, because four stray items lay on the pad and the probes could not say what they
 * were. Ruled: the item probes print each item's TYPE, and each gate's setup clears dropped items and names what it
 * removed. Never one of ours: a marker body ({@code marker_entity}) or a fused chunk's body ({@code fused}) is not a
 * dropped item (the linger pass and the fused store remove those, and say so on their own lines).
 */
class SelfTestItemTypesTest {

    @Test
    void aDroppedItemIsAnItemThatIsNotOneOfOurs() {
        assertTrue(SelfTestStaging.droppedItem(true, false, false), "a plain item on the ground");
        assertFalse(SelfTestStaging.droppedItem(true, true, false), "a marker body of ours");
        assertFalse(SelfTestStaging.droppedItem(true, false, true), "a fused chunk's body");
        assertFalse(SelfTestStaging.droppedItem(false, false, false), "not an item at all");
    }

    @Test
    void theTypesAreCountedAndSorted_soTheLineIsTheSameWhateverTheScanOrder() {
        assertEquals("[]", SelfTestStaging.itemTypes(List.of()));
        assertEquals("[bone x1, rotten_flesh x3]",
                SelfTestStaging.itemTypes(List.of("rotten_flesh", "bone", "rotten_flesh", "rotten_flesh")));
        assertEquals(SelfTestStaging.itemTypes(List.of("bone", "rotten_flesh")),
                SelfTestStaging.itemTypes(List.of("rotten_flesh", "bone")), "order-free");
    }

    @Test
    void theSetupsClearLine() {
        // F2 (2026-11-27): the line names its bound, the pad.
        assertEquals("ITEMS cleared=0 pad []", SelfTestStaging.itemsClearedLine(List.of()));
        assertEquals("ITEMS cleared=4 pad [nether_wart_block x1, rotten_flesh x3]",
                SelfTestStaging.itemsClearedLine(List.of("rotten_flesh", "nether_wart_block", "rotten_flesh", "rotten_flesh")));
    }

    /**
     * The scan itself (where it runs is {@code SelfTestWorldUntouchedBeforePadTest}: the pad step's completion, F1).
     * Mutation: a marker or a fused body cleared, or the removal off its own thread.
     */
    @Test
    void theClearTakesOnlyDroppedItems_onTheirOwnThread() throws IOException {
        String r = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java"))
                .replace("\r", "");
        assertTrue(r.length() > 20_000, "control: SelfTestRun was read");
        int scan = r.indexOf("private List<String> clearDroppedItems(SelfTestPadShape.Stand pad)");
        assertTrue(scan > 0, "one scan, by name");
        String body = r.substring(scan, r.indexOf("\n    }\n", scan));
        assertTrue(body.contains("SelfTestStaging.droppedItem(e instanceof org.bukkit.entity.Item"), "the pure test decides");
        assertTrue(body.contains("service.keys().markerEntity") && body.contains("service.keys().fused"),
                "both of our item bodies are left alone");
        assertTrue(body.contains("e::remove"), "and each is removed on its own thread");
    }

    /** Mutation: the type dropped from either probe's line. */
    @Test
    void bothItemProbesPrintTheType() throws IOException {
        String d = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java"))
                .replace("\r", "");
        assertTrue(d.length() > 50_000, "control: SelfTestDrivers was read");
        String positions = d.substring(d.indexOf("private Optional<String> probePositions("),
                d.indexOf("private Optional<String> saveBlocks("));
        assertTrue(positions.contains("+ itemType(e)"), "each POSITIONS entry carries its item's type");
        String entities = d.substring(d.indexOf("private Optional<String> probeEntities("),
                d.indexOf("private Optional<String> probeAttribute("));
        assertTrue(entities.contains("\" types=\" + SelfTestStaging.itemTypes(types)"), "the ENTITIES line names the types");
    }
}
