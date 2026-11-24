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
        assertTrue(src("SelfTestRun.java").contains("end(\"ABORTED the player left\", service.scheduler(), true);"),
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
}
