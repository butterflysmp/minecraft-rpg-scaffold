package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K7 (the seat's brief, 2026-11-25): a MINTED glass bottle (the Broken Bottle) is never filled, and a plain glass bottle is
 * untouched. The decision is pure and pinned here; the four listener hooks need a live server, so their wiring is pinned
 * by reading the source, each needle with a control. Mutations named per test.
 */
class MintedBottleUseTest {

    /** Mutation: the minted check dropped (a plain bottle is blocked), or the material check dropped (a sword is). */
    @Test
    void onlyOurItemOnAGlassBottleIsBlocked() {
        assertTrue(MintedBottleUse.isMintedBottle(true, "glass_bottle"));
        assertFalse(MintedBottleUse.isMintedBottle(false, "glass_bottle"), "a plain glass bottle is untouched");
        assertFalse(MintedBottleUse.isMintedBottle(true, "iron_sword"), "a minted sword is not a bottle");
        assertFalse(MintedBottleUse.isMintedBottle(true, null));
    }

    /**
     * READ (the pinned server jar): the blocks whose OWN interaction takes a glass bottle are a WATER cauldron
     * ({@code CauldronInteractions}, the WATER dispatcher's GLASS_BOTTLE entry) and a beehive or bee nest
     * ({@code BeehiveBlock.useItemOn}). Mutation: a chest added (he could not open one with the bottle in hand), or one of
     * the three dropped.
     */
    @Test
    void theInteractedBlockIsDeniedOnlyOnTheBlocksThatTakeABottle() {
        for (String b : List.of("water_cauldron", "beehive", "bee_nest")) {
            assertTrue(MintedBottleUse.denyBlock(b), b);
        }
        for (String b : List.of("chest", "cauldron", "lava_cauldron", "crafting_table", "oak_door")) {
            assertFalse(MintedBottleUse.denyBlock(b), b + " still opens with the bottle in hand");
        }
        assertEquals(3, MintedBottleUse.BOTTLE_BLOCKS.size());
    }

    private static List<String> listeners() throws IOException {
        return Files.readAllLines(Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "listener",
                "RpgListeners.java"), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    private static int at(List<String> c, String needle) {
        for (int i = 0; i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    /** Mutation: any of the three hooks removed, or the item-in-hand deny dropped from the interact hook. */
    @Test
    void theThreeHooksAreWired() throws IOException {
        List<String> c = listeners();
        assertTrue(c.size() > 1000, "control: RpgListeners was read, " + c.size() + " code lines");
        int use = at(c, "public void onMintedBottleUse(PlayerInteractEvent event)");
        assertTrue(use > 0, "the interact hook");
        List<String> body = c.subList(use, Math.min(c.size(), use + 14));
        assertTrue(body.stream().anyMatch(l -> l.contains("event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);")),
                "BottleItem.use (a water source, dragon's breath) is the item's own use");
        assertTrue(body.stream().anyMatch(l -> l.contains("event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);")),
                "a cauldron's and a beehive's are the BLOCK's");
        assertTrue(at(c, "public void onMintedBottleDispense(org.bukkit.event.block.BlockDispenseEvent event)") > 0, "the dispenser");
        assertTrue(at(c, "public void onMintedGearBrew(org.bukkit.event.inventory.BrewEvent event)") > 0, "the brewing stand");
    }
}
