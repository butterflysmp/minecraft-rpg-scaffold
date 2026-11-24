package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PAD'S WIRING (the seat's P4-P7, 2026-11-23). The run, the snapshot and the service need a live server, so this reads
 * their source, as {@code SelfTestVanillaXpRestoreTest} and {@code SelfTestDisableWiringTest} do. Each assertion is one
 * ruling's load-bearing line.
 */
class SelfTestPadWiringTest {

    private static String src(String name) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/" + name),
                StandardCharsets.UTF_8).replace("\r", "");
    }

    @Test
    void everyUnchainedRowStagesFromTheStagePointAndSoDoesReturn() throws IOException {
        String run = src("SelfTestRun.java");
        assertTrue(run.contains("Location start = stage();\n        player.teleportAsync(start).thenRun("),
                "the row-start clean teleports him to stage(), not to the run's start");
        assertTrue(run.contains("return stagePoint != null ? stagePoint.clone() : snapshot.start();"),
                "stage() is the stand point once set, else the run's start");
        assertTrue(src("SelfTestDrivers.java").contains("org.bukkit.Location start = run.stage();"), "RETURN reads stage()");
    }

    @Test
    void theTeleportComesOnlyAfterTheBuildSucceededAndTheStagePointOnlyAfterTheTeleport() throws IOException {
        String run = src("SelfTestRun.java");
        int pad = run.indexOf("private void pad() {");
        assertTrue(pad > 0);
        String body = run.substring(pad, run.indexOf("private void padFailed(", pad));
        int notOk = body.indexOf("if (!r.ok()) {");
        int teleport = body.indexOf("player.teleportAsync(stand)");
        int moved = body.indexOf("!Boolean.TRUE.equals(moved)");
        int set = body.indexOf("stagePoint = stand.clone();");
        assertTrue(notOk > 0 && teleport > notOk, "a failed build returns before any teleport");
        assertTrue(moved > teleport && set > moved, "the stage point is set only once the teleport has landed");
        assertTrue(run.substring(run.indexOf("private void padFailed(")).contains("end(\"ABORTED pad: \" + why);"),
                "a failure ends the run, whose restore runs");
    }

    @Test
    void aQuitMidRunReturnsHimSynchronously() throws IOException {
        assertTrue(src("SelfTestService.java").contains("active.endAtQuit();"), "the quit path calls endAtQuit");
        assertTrue(src("SelfTestRun.java").contains("end(\"ABORTED the player left\", service.scheduler(), true, \"quit\");"),
                "endAtQuit restores with now=true");
        assertTrue(src("SelfTestSnapshot.java").contains("if (now) p.teleport(location); else p.teleportAsync(location);"),
                "and now=true is the synchronous teleport");
    }

    @Test
    void theSnapshotCapturesAndRestoresSpawnPhantoms() throws IOException {
        String snap = src("SelfTestSnapshot.java");
        assertTrue(snap.contains("this.spawnPhantoms = world.getGameRuleValue(GameRules.SPAWN_PHANTOMS);"), "captured");
        String restore = snap.substring(snap.indexOf("String restore(Player p,"));
        assertTrue(restore.contains("world.setGameRule(GameRules.SPAWN_PHANTOMS, spawnPhantoms);"), "restored");
        assertTrue(restore.contains("\" spawn_phantoms=\" + spawnPhantoms"), "and named on the RESTORED line");
    }

    @Test
    void thePadCommandsAreTheServicesAndTheServiceRefusesOffTheDevServer() throws IOException {
        String cmd = src("SelfTestCommand.java");
        assertTrue(cmd.contains("Commands.literal(\"build\").executes(ctx -> reply(ctx, service::padBuild))"));
        assertTrue(cmd.contains("Commands.literal(\"remove\").executes(ctx -> reply(ctx, service::padRemove))"));
        String svc = src("SelfTestService.java");
        String pad = svc.substring(svc.indexOf("private String pad(Player player, boolean build) {"));
        assertTrue(pad.startsWith("private String pad(Player player, boolean build) {\n        if (!devServer) return"),
                "the dev marker is the first thing the pad command checks (P1)");
    }

    @Test
    void thePadTouchesOnlyItsOwnCells() throws IOException {
        String pad = src("SelfTestPad.java");
        // Every getBlockAt in the builder is offset from the stand point by a cell the shape visited (or is the stand
        // point's own floor block). No other block is read or written.
        int n = 0;
        for (int i = pad.indexOf("world.getBlockAt("); i >= 0; i = pad.indexOf("world.getBlockAt(", i + 1)) n++;
        assertTrue(n == 3, "three getBlockAt: the build's cell, the removal's cell, the floor under the stand: " + n);
        assertTrue(pad.contains("world.getBlockAt(s.x() + dx, s.floorY() + dy, s.z() + dz)"));
        assertTrue(pad.contains("world.getBlockAt(s.x() + c[0], s.floorY() + c[1], s.z() + c[2])"));
        assertTrue(pad.contains("world.getBlockAt(s.x(), s.floorY(), s.z())"));
    }

    // --- P10, THE PAD HOLDS ITS CHUNKS (the seat's rulings of 2026-11-24) --------------------------------------------

    /**
     * ALL TWELVE HELD BEFORE A BLOCK IS READ OR WRITTEN: in {@code onLoaded} the work runs only inside the acquire's
     * callback, after a failed outcome has returned, after the hop to the region, and after the alarm check (a4).
     */
    @Test
    void theWorkRunsOnlyOnceEveryTicketIsHeldAndTheAlarmPassed() throws IOException {
        String pad = src("SelfTestPad.java");
        String on = pad.substring(pad.indexOf("private static CompletableFuture<Result> onLoaded("));
        int acquire = on.indexOf("tickets.acquire(o -> {");
        int failed = on.indexOf("if (!o.ok()) {");
        int hop = on.indexOf("scheduler.onRegion(");
        int alarm = on.indexOf("if (!world.isChunkLoaded(c.x(), c.z())) {");
        int work = on.indexOf("work.apply(s)");
        assertTrue(acquire > 0 && failed > acquire && hop > failed && alarm > hop && work > alarm,
                "acquire, then the failure return, then the hop, then the alarm, then the work");
        assertTrue(on.indexOf("work.apply(s)", work + 1) < 0, "and the work is applied nowhere else");
        assertTrue(!on.contains("getChunkAtAsync"), "no load of its own: the tickets' requests are the one source");
        assertTrue(pad.contains("new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(standOf(world)))"),
                "the footprint is SelfTestPadShape.chunks, the one source");
    }

    /** The only ticket take in the plugin is the seam's, and the seam is reached only through SelfTestPadTickets. */
    @Test
    void theOnlyTakeIsTheSeamsBehindTheLoadedGuard() throws IOException {
        int takes = 0;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String s = Files.readString(f, StandardCharsets.UTF_8);
                for (int i = s.indexOf(".addPluginChunkTicket("); i >= 0; i = s.indexOf(".addPluginChunkTicket(", i + 1)) {
                    takes++;
                    assertTrue(f.endsWith("BukkitPadChunks.java"), "a take outside the seam: " + f);
                }
            }
        }
        assertTrue(takes == 1, "exactly one take in main code, the seam's: " + takes);
        String t = src("SelfTestPadTickets.java");
        int guard = t.indexOf("if (!chunks.isLoaded(c.x(), c.z())) {");
        int take = t.indexOf("if (!chunks.take(c.x(), c.z())) {");
        assertTrue(guard > 0 && take > guard, "a2: the take comes after the loaded check");
    }

    /** EVERY EXIT RELEASES: the one private end is the only release, and every exit reaches it with its cause. */
    @Test
    void everyExitReachesTheOneReleaseWithItsCause() throws IOException {
        String run = src("SelfTestRun.java");
        int releases = 0;
        for (int i = run.indexOf("padTickets.release()"); i >= 0; i = run.indexOf("padTickets.release()", i + 1)) releases++;
        assertTrue(releases == 1, "one release site: " + releases);
        String end = run.substring(run.indexOf("private void end(String why, io.github.butterflysmp.rpg.paper.scheduler.Scheduler scheduler, boolean now,"));
        end = end.substring(0, end.indexOf("\n    }\n"));
        assertTrue(end.contains("if (ended) return;"), "once: the second exit finds it ended");
        assertTrue(end.contains("SelfTestPadTickets.releasedLine(padTickets.release(), cause)"), "the release is in the funnel");
        assertTrue(run.contains("end(why, service.scheduler(), false, SelfTestPadTickets.causeOf(why));"),
                "FINISHED, every abort and stop name their cause by the reason");
        assertTrue(run.contains("end(\"ABORTED the player left\", service.scheduler(), true, \"quit\");"), "quit");
        assertTrue(run.contains("end(\"ABORTED reason=server-stop\", inline, true, \"disable\");"), "disable");
        String svc = src("SelfTestService.java");
        assertTrue(svc.contains("active.end(\"STOPPED by /rpg selftest stop\");"), "stop reaches end");
        assertTrue(svc.contains("active.endAtQuit();") && svc.contains("if (active != null) active.endAtDisable();"));
    }

    /** The pad command takes and releases its own, marks itself busy, and a server stop releases it too. */
    @Test
    void thePadCommandHoldsItsOwnTicketsAndTheBusyGuardBothWays() throws IOException {
        String svc = src("SelfTestService.java");
        String pad = svc.substring(svc.indexOf("private String pad(Player player, boolean build) {"));
        assertTrue(pad.contains("String busy = occupancy.refusePad();"), "refused while a run holds the pad");
        assertTrue(pad.contains("occupancy.padStarted();"));
        assertTrue(pad.contains("SelfTestPadTickets.releasedLine(tickets.release(), ok ? \"end\" : \"abort\")"));
        assertTrue(pad.contains("occupancy.padEnded();"));
        String start = svc.substring(svc.indexOf("public String start(Player player, String gate, String row) {"));
        assertTrue(start.contains("String busy = occupancy.refuseRun();"), "a run refused while the pad command works");
        assertTrue(svc.contains("SelfTestPadTickets.releasedLine(padCommandTickets.release(), \"disable\")"));
        assertTrue(svc.contains("occupancy.runEnded();"), "the run's end frees it");
    }
}
