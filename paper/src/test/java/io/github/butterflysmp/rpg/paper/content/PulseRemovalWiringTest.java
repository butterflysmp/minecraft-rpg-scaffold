package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY REMOVAL OF A CASTER'S PENDING PULSES IS WIRED (Smolder, the seat's Z5 and A5, 2026-11-27), at each site the
 * orb's and the fused chunk's are: his quit, death and world change, server stop, and the selftest's clear_state and
 * row-start clean. Read off the source, as {@code FusedRemovalWiringTest} reads the chunk's, because the listeners cannot
 * run here; what an ended run does is {@code PulseCastTest}'s (no further pulse, remaining counted).
 */
class PulseRemovalWiringTest {

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
    void quitDeathAndWorldChangeEachEndHisPulsesWithTheirCause() throws IOException {
        String l = read("listener/RpgListeners.java");
        assertTrue(body(l, "public void onQuit(PlayerQuitEvent event) {")
                .contains("PulseRemovals.end(adapters, event.getPlayer().getUniqueId(), \"quit\");"), "quit");
        assertTrue(body(l, "public void onOrbOwnerDeath(PlayerDeathEvent event) {")
                .contains("PulseRemovals.end(adapters, event.getPlayer().getUniqueId(), \"death\");"), "death");
        assertTrue(body(l, "public void onOrbOwnerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {")
                .contains("PulseRemovals.end(adapters, event.getPlayer().getUniqueId(), \"world_change\");"), "world change");
    }

    @Test
    void stopEndsEveryRun() throws IOException {
        String p = read("RpgPlugin.java");
        int disable = p.indexOf("public void onDisable() {");
        assertTrue(p.indexOf("PulseRemovals.endAll(adapters);", disable) > disable, "stop");
    }

    @Test
    void theSelftestEndsHisPulsesAtClearStateAndRowStart() throws IOException {
        String d = read("selftest/SelfTestDrivers.java");
        String clear = d.substring(d.indexOf("private Optional<String> clearState(Player p) {"));
        clear = clear.substring(0, clear.indexOf("\n    }\n"));
        assertTrue(clear.contains("PulseRemovals.end(adapters, id, \"selftest\");"), "clear_state");
        String r = read("selftest/SelfTestRun.java");
        int clean = r.indexOf("log(\"CLEAN blocks=");
        int removal = r.indexOf("PulseRemovals.end(service.adapters(), player.getUniqueId(), \"selftest\");");
        assertTrue(removal > 0 && removal < clean, "row-start CLEAN, before its line");
    }
}
