package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S P1 TO P4 (2026-11-27), after CC's F5 stop: until a PAD step has placed him, a run touches NOTHING in the
 * world. The row-start CLEAN of a run begun away from the pad (every first setup; every row of a gate with no pad step)
 * ends no work chain, removes no projectile, cloud or marker, and clears no item; it still puts back what is HIS OWN
 * (inventory, summons, fused chunks, the drum, the blink, the cooldown). On the pad, each of those cleanups is bounded to
 * the pad's box ({@code SelfTestPadShape.inRegion}), never a sphere. The item clear runs in the pad step's own completion,
 * so a command's FIRST gate starts with a clean pad too. The run's end removes an XP orb only on the pad, inside the box.
 *
 * <p>The run needs a server, so this reads the source; the decisions are {@code SelfTestPadRegionTest}'s. P6's "a run
 * begun away from the pad with an item, an arrow, a cloud, a chain and an orb beside the start removes none" is this
 * test's first and last assertions (no world call before {@code stagePoint}); its control, "the same things inside the box
 * are removed once he is on it", is the second (the calls on the pad take the box).
 */
class SelfTestWorldUntouchedBeforePadTest {

    private static String run() throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java")).replace("\r", "");
    }

    private static String body(String src, String sig) {
        int at = src.indexOf(sig);
        assertTrue(at > 0, "found: " + sig);
        return src.substring(at, src.indexOf("\n    }\n", at));
    }

    /** P1. Mutation: the guard removed, so the linger pass runs on his ground again. */
    @Test
    void beforeAPadStep_theCleanTouchesNothingInTheWorld() throws IOException {
        String r = run();
        assertTrue(r.length() > 20_000, "control: SelfTestRun was read");
        int pad = r.indexOf("java.util.Optional<SelfTestPadShape.Stand> pad = padStand();");
        int clean = r.indexOf("log(\"CLEAN blocks=", pad);
        int guard = r.indexOf("if (pad.isPresent()) log(SelfTestStaging.lingerLine(endWork(pad.get()), endLingering(pad.get())));", clean);
        int skipped = r.indexOf("else log(SelfTestStaging.LINGER_SKIPPED);", guard);
        assertTrue(pad > 0 && clean > pad && guard > clean && skipped > guard && skipped - guard < 140,
                "the pad's stand read first; the linger pass only with it; else the one skipped line: " + pad + " " + clean + " " + guard + " " + skipped);
        assertTrue(r.contains("+ \" world=\" + (pad.isPresent() ? \"pad\" : \"skipped\"));"), "the CLEAN line says which");
        assertEquals(1, r.split("endWork\\(", -1).length - 2, "endWork called once (and declared once)");
        assertEquals(1, r.split("endLingering\\(", -1).length - 2, "endLingering called once (and declared once)");
        String stand = body(r, "private java.util.Optional<SelfTestPadShape.Stand> padStand() {");
        assertTrue(stand.contains("if (stagePoint == null) return java.util.Optional.empty();"), "no stand point, no pad");
    }

    /** P2. Mutation: any of the three cleanups measured as a sphere round a Location again. */
    @Test
    void onThePad_everyCleanupIsBoundedToThePadsBox() throws IOException {
        String r = run();
        String work = body(r, "private int endWork(SelfTestPadShape.Stand pad) {");
        assertTrue(work.contains("service.work().cancelIn(stagePoint.getWorld().getUID(), SelfTestPadShape.bounds(pad));"), work);
        for (String sig : new String[] {"private List<String> endLingering(SelfTestPadShape.Stand pad) {",
                "private List<String> clearDroppedItems(SelfTestPadShape.Stand pad) {"}) {
            String b = body(r, sig);
            assertTrue(b.contains("padEntities(pad)"), sig + " scans the pad's box");
            assertFalse(b.contains("distanceSquared"), sig + " measures no sphere");
        }
        String scan = body(r, "private List<org.bukkit.entity.Entity> padEntities(SelfTestPadShape.Stand pad) {");
        assertTrue(scan.contains("SelfTestPadShape.inRegion(pad, e.getLocation().getX(), e.getLocation().getY(), e.getLocation().getZ())"),
                "each body tested against the region itself");
    }

    /** P4, F1, F3. Mutation: the item clear moved back to the CLEAN, or before the stand point is set. */
    @Test
    void theItemClearRunsInThePadStepsCompletion_onceHeIsOnIt() throws IOException {
        String r = run();
        assertEquals(1, r.split("clearDroppedItems\\(", -1).length - 2, "called once (and declared once)");
        String pad = body(r, "private void pad() {");
        int stood = pad.indexOf("stagePoint = stand.clone();");
        int items = pad.indexOf("log(SelfTestStaging.itemsClearedLine(clearDroppedItems(padStand().orElseThrow())));");
        int advance = pad.indexOf("advance();");
        assertTrue(stood > 0 && items > stood && advance > items, "after he stands on it, before the setup goes on: " + stood + " " + items + " " + advance);
    }

    /** P3. Mutation: the orb removed whatever the region, or the region passed when no pad step has run. */
    @Test
    void theRunsEndRemovesAnOrbOnlyOnThePad() throws IOException {
        String r = run();
        assertTrue(r.contains("scheduler, now, padStand().orElse(null));"), "the restore is handed the pad, or nothing");
        String s = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestSnapshot.java")).replace("\r", "");
        assertTrue(s.contains("boolean orbOnPad = orb && pad != null && SelfTestPadShape.inRegion(pad, e.getLocation().getX(), e.getLocation().getY(), e.getLocation().getZ());"), "the orb's own test");
        assertTrue(s.contains("if (!removedOnRestore(e instanceof Player, e.getScoreboardTags().contains(MOB_TAG), orb, orbOnPad)) continue;"));
    }
}
