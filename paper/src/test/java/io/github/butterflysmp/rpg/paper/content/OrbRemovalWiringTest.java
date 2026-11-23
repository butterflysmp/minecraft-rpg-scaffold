package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY B6 REMOVAL OF AN ORB IS WIRED (Lapis Crystal's D2, the seat's message of 2026-11-23): his quit, his death, a world
 * change, server stop (before the worlds save), and the sweep both where a body appears and at enable. Read off the
 * source, because the listeners cannot run in a unit test; the logic each calls is {@code OrbThrowTest}'s and
 * {@code OrbRemovals}'. Expiry and replacement are {@code CastExecutor}'s ({@code OrbThrowTest}).
 */
class OrbRemovalWiringTest {

    private static String read(String path) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper", path), StandardCharsets.UTF_8)
                .replace("\r", "");
    }

    /** The body of the method whose signature line is {@code signature}, up to the next method's annotation. */
    private static String body(String src, String signature) {
        int at = src.indexOf(signature);
        assertTrue(at > 0, "not found: " + signature);
        int end = src.indexOf("\n    @EventHandler", at);
        return src.substring(at, end < 0 ? src.length() : end);
    }

    @Test
    void quitDeathAndWorldChangeEachRemoveHisOrbWithTheirCause() throws IOException {
        String l = read("listener/RpgListeners.java");
        assertTrue(body(l, "public void onQuit(PlayerQuitEvent event) {")
                .contains("OrbRemovals.remove(adapters, event.getPlayer().getUniqueId(), \"quit\");"), "quit");
        assertTrue(body(l, "public void onOrbOwnerDeath(PlayerDeathEvent event) {")
                .contains("OrbRemovals.remove(adapters, event.getPlayer().getUniqueId(), \"death\");"), "death");
        assertTrue(body(l, "public void onOrbOwnerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {")
                .contains("OrbRemovals.remove(adapters, event.getPlayer().getUniqueId(), \"world_change\");"), "world change");
        assertTrue(body(l, "public void onOrbBodyAppear(EntityAddToWorldEvent event) {")
                .contains("OrbRemovals.sweep(adapters, event.getEntity());"), "the sweep where a body appears");
    }

    @Test
    void stopRemovesEveryOrbBeforeTheSaveAndEnableSweeps() throws IOException {
        String p = read("RpgPlugin.java");
        int disable = p.indexOf("public void onDisable() {");
        int removeAll = p.indexOf("OrbRemovals.removeAll(adapters);", disable);
        int flush = p.indexOf("restoreStar(online, adapters.keys());", disable);
        assertTrue(disable > 0 && removeAll > disable, "stop removes every orb");
        assertTrue(flush < 0 || removeAll < flush, "and does it early in the disable, beside the selftest's restore");
        assertTrue(p.contains("scheduler.onEntity(e, () -> io.github.butterflysmp.rpg.paper.weapon.OrbRemovals.sweep(adapters, e));"),
                "the enable walk sweeps every entity");
    }
}
