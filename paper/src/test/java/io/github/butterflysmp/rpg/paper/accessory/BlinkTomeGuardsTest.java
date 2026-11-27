package io.github.butterflysmp.rpg.paper.accessory;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K14 (the seat, 2026-11-26): THE BOOK'S GUARDS (H12) AND A4'S REFUSALS ALREADY COVER THE BLINK TOME, shown rather than
 * assumed. No unit test here can build an {@code ItemStack} (no MockBukkit in this module; see
 * {@code AccessoryItemsWritesOnlyItsIdTest}), so the claim is split into the three facts it rests on, each checked:
 *
 * <ol>
 *   <li>No main Java file names either tome's id, so no guard can treat one tome differently from the other.</li>
 *   <li>Every H12 guard asks {@code AccessoryItems.isAccessory}, which is the presence of {@code accessory_id}, the one
 *       key every minted accessory carries (the mint is shared: {@code AccessoryItemsWritesOnlyItsIdTest}).</li>
 *   <li>The Blink Tome's material is {@code book}, the material the H12 table was READ for, and in the allowlist.</li>
 * </ol>
 */
class BlinkTomeGuardsTest {

    private static final Path MAIN = Path.of("src/main/java");
    private static final Path LISTENERS = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/listener/RpgListeners.java");

    @Test
    void noMainJavaFileNamesATomesId() throws IOException {
        List<String> hits = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                scanned++;
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    if (line.contains("\"blink_tome\"") || line.contains("\"healing_tome\"")) hits.add(p + ": " + line.trim());
                }
            }
        }
        assertTrue(scanned > 100, "control: the walk found the main sources (" + scanned + ")");
        assertEquals(List.of(), hits, "a tome's id written in Java would let a guard cover one tome and not the other");
        // Mutation: add `if ("blink_tome".equals(id)) return;` to onAccessoryTrade -> reddens.
    }

    @Test
    void everyBookGuardAsksIsAccessory_theIdsPresence() throws IOException {
        String l = Files.readString(LISTENERS, StandardCharsets.UTF_8);
        for (String head : List.of(
                "public void onAccessoryTrade(io.papermc.paper.event.player.PlayerPurchaseEvent event) {",
                "public void onAccessoryShelved(PlayerInteractEvent event) {",
                "public void onAccessoryHoppered(org.bukkit.event.inventory.InventoryMoveItemEvent event) {")) {
            int at = l.indexOf(head);
            assertTrue(at >= 0, "the guard exists: " + head);
            String body = l.substring(at, l.indexOf("\n    }", at));
            assertTrue(body.contains("AccessoryItems.isAccessory("), head + " keys on the id's presence");
        }
    }

    @Test
    void theBlinkTomeIsABook_theMaterialH12WasReadFor() throws IOException {
        String yml = Files.readString(Path.of("src/main/resources/content/accessories/blink_tome.yml"), StandardCharsets.UTF_8);
        assertTrue(yml.lines().anyMatch(s -> s.equals("material: book")), "blink_tome.yml declares material: book");
        assertTrue(AccessoryDefinition.MATERIALS.contains("book"));
    }
}
