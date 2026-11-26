package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SAVED-BLOCKS READ-BACK (the seat's follow-up, 2026-11-26): GATE-broken-bottle BB8's "no water or glass left" was
 * INFERRED from blocks=75. Now every restore of saved blocks reads them back and logs blocksBack=<matched>/<restored>, on
 * the row-start CLEAN line and on RESTORED. Mutations named per test.
 */
class SelfTestBlockReadBackTest {

    /** Mutation: the counter counts every state (a read-back that always agrees), or none. */
    @Test
    void theReadBackCountsOnlyTheStatesTheWorldHoldsAgain() {
        assertEquals(2, SelfTestStaging.readBack(List.of("glass", "air", "water"), s -> !s.equals("water")));
        assertEquals(0, SelfTestStaging.readBack(List.<String>of(), s -> true));
        assertEquals("blocksBack=74/75", SelfTestStaging.readBackField(74, 75));
    }

    private static String src(String name) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/" + name)).replace("\r", "");
    }

    /** Mutation: either restore stops reading back, or the field leaves its line, or the read runs BEFORE the update. */
    @Test
    void bothRestoresReadBackAfterTheyWrite() throws IOException {
        String s = src("SelfTestSnapshot.java");
        assertTrue(s.length() > 10_000, "control: SelfTestSnapshot was read");
        int row = s.indexOf("RowRestore restoreRowBlocks() {");
        int rowUpdate = s.indexOf("for (BlockState state : newestFirst) state.update(true, false);", row);
        int rowRead = s.indexOf("SelfTestStaging.readBack(newestFirst, SelfTestSnapshot::holdsAgain)", row);
        assertTrue(row > 0 && rowUpdate > row && rowRead > rowUpdate, "the row restore writes, then reads back");
        int end = s.indexOf("List<BlockState> lastChain = blocks.drainNewestFirst();");
        int endUpdate = s.indexOf("for (BlockState state : lastChain) state.update(true, false);", end);
        int endRead = s.indexOf("SelfTestStaging.readBack(lastChain, SelfTestSnapshot::holdsAgain)", end);
        assertTrue(end > 0 && endUpdate > end && endRead > endUpdate, "the run's restore writes, then reads back");
        assertTrue(s.contains("+ \" blocks=\" + blocks.total() + \" \" + blocksBack + \" mobsRemoved=\""), "on RESTORED");
        assertTrue(s.contains("return saved.getBlock().getBlockData().equals(saved.getBlockData());"), "the world's block data");
        String r = src("SelfTestRun.java");
        assertTrue(r.contains("log(\"CLEAN blocks=\" + blocks + \" \" + SelfTestStaging.readBackField(rowBlocks.readBack(), blocks)"),
                "on the CLEAN line");
    }
}
