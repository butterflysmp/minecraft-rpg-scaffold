package io.github.butterflysmp.rpg.paper.listener;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RULING 5 (the seat's brief of 2026-11-22): a minted crossbow never loads a vanilla projectile.
 * {@code RpgListeners.onWeaponCrossbowLoad} cancels an {@code EntityLoadCrossbowEvent} whose crossbow carries a weapon
 * id. Read from the source, bounded at the handler's closing brace, as {@code LivingLanternContentTest} reads the
 * placement guard: no unit test can raise the event (it needs a live entity and an ItemStack). NOT READ ON A SERVER;
 * no gate row loads an off-hand crossbow (listed for the seat in GATE-breacher.md).
 */
class WeaponCrossbowLoadGuardTest {

    @Test
    void aMintedCrossbowNeverLoads() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/listener/RpgListeners.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int start = src.indexOf(
                "public void onWeaponCrossbowLoad(io.papermc.paper.event.entity.EntityLoadCrossbowEvent event)");
        assertTrue(start > 0, "the crossbow-load guard is not found: the scan is blind");
        assertTrue(src.substring(src.lastIndexOf("@EventHandler", start), start).startsWith("@EventHandler"),
                "it is an event handler");
        int end = src.indexOf("\n    }\n", start);
        String body = src.substring(start, end);
        assertTrue(body.contains("WeaponItems.weaponId(event.getCrossbow(), adapters.keys())"),
                "keyed by weapon id, never the material: " + body);
        assertTrue(body.contains("event.setCancelled(true);"), "and it cancels");
    }
}
