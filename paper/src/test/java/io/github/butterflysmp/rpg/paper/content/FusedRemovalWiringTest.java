package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY B6 REMOVAL OF A FUSED CHUNK IS WIRED (the seat's F2, 2026-11-26, the Blast Fungus), at each site the orb's is:
 * his quit, death and world change, server stop, the sweep where a body appears and at enable, and the selftest's
 * clear_state, row-start CLEAN and run end. Read off the source, as {@code OrbRemovalWiringTest} reads the orb's,
 * because the listeners cannot run here; what each call does is {@code FusedProjectileTest}'s (a taken chunk never goes
 * off). The {@code FUSED} line is pinned to three places.
 */
class FusedRemovalWiringTest {

    private static String read(String path) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper", path), StandardCharsets.UTF_8)
                .replace("\r", "");
    }

    private static String body(String src, String signature) {
        int at = src.indexOf(signature);
        assertTrue(at > 0, "not found: " + signature);
        int end = src.indexOf("\n    @EventHandler", at);
        return src.substring(at, end < 0 ? src.length() : end);
    }

    @Test
    void quitDeathAndWorldChangeEachRemoveHisChunksWithTheirCause() throws IOException {
        String l = read("listener/RpgListeners.java");
        assertTrue(body(l, "public void onQuit(PlayerQuitEvent event) {")
                .contains("FusedRemovals.removeAllOf(adapters, event.getPlayer().getUniqueId(), \"quit\");"), "quit");
        assertTrue(body(l, "public void onOrbOwnerDeath(PlayerDeathEvent event) {")
                .contains("FusedRemovals.removeAllOf(adapters, event.getPlayer().getUniqueId(), \"death\");"), "death");
        assertTrue(body(l, "public void onOrbOwnerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {")
                .contains("FusedRemovals.removeAllOf(adapters, event.getPlayer().getUniqueId(), \"world_change\");"),
                "world change");
        assertTrue(body(l, "public void onOrbBodyAppear(EntityAddToWorldEvent event) {")
                .contains("FusedRemovals.sweepAppeared(adapters, event.getEntity());"), "the sweep where a body appears");
    }

    @Test
    void stopRemovesEveryChunkAndEnableSweeps() throws IOException {
        String p = read("RpgPlugin.java");
        int disable = p.indexOf("public void onDisable() {");
        assertTrue(p.indexOf("FusedRemovals.removeAll(adapters);", disable) > disable, "stop");
        assertTrue(p.contains("scheduler.onEntity(e, () -> io.github.butterflysmp.rpg.paper.weapon.FusedRemovals.sweep(adapters, e));"),
                "the enable walk");
    }

    @Test
    void theSelftestRemovesHisChunksAtClearStateRowStartAndRunEnd() throws IOException {
        String d = read("selftest/SelfTestDrivers.java");
        String clear = d.substring(d.indexOf("private Optional<String> clearState(Player p) {"));
        clear = clear.substring(0, clear.indexOf("\n    }\n"));
        assertTrue(clear.contains("FusedRemovals.removeAllOf(adapters, id, \"selftest\");"), "clear_state");
        assertTrue(clear.contains("\" fused=\" + fused"), "and says how many");
        String r = read("selftest/SelfTestRun.java");
        int clean = r.indexOf("log(\"CLEAN blocks=");
        int removal = r.indexOf("FusedRemovals.removeAllOf(service.adapters(), player.getUniqueId(), \"selftest\");");
        assertTrue(removal > 0 && removal < clean, "row-start CLEAN, before its line");
        assertTrue(r.indexOf("restored += \" fused=\" + io.github.butterflysmp.rpg.paper.weapon.FusedRemovals.removeAllOf(")
                > removal, "run end");
    }

    @Test
    void theFusedLine() {
        assertEquals("FUSED rest owner=00000000-0000-0000-0000-000000000001 id=7 at=12.500,64.000,0.500 tick=200",
                PaperCombatWorld.fusedLine(new UUID(0, 1), 7, "rest", new Vec3(12.5, 64, 0.5), null, 200));
        assertEquals("FUSED detonate owner=00000000-0000-0000-0000-000000000001 id=7 at=12.500,64.000,0.500 cause=fuse tick=241",
                PaperCombatWorld.fusedLine(new UUID(0, 1), 7, "detonate", new Vec3(12.5, 64, 0.5), "fuse", 241));
    }
}
