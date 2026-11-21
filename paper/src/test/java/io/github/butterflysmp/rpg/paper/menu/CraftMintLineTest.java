package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CRAFT MINT'S LOG LINE (2026-11-21, the seat's brief, PR B item 5). Before it, a craft that minted a weapon left no
 * server line at all, so the manual craft row (GATE-sword-tiers.md STM1, carried) could be read only from Ben's screen.
 * Now {@code InventoryCraft} logs one line per weapon it mints, the same shape as convert-on-arrival's ARRIVAL line.
 */
class CraftMintLineTest {

    @Test
    void theLineIsWordForWord() {
        assertEquals("CRAFT minted player=BaronVonYeetus weapon=iron_sword material=iron_sword score=100",
                InventoryCraft.craftMintLine("BaronVonYeetus", "iron_sword", "iron_sword", OptionalInt.of(100)));
        // Every field distinct from the others, so a transposition cannot pass.
        assertEquals("CRAFT minted player=p weapon=w material=m score=137",
                InventoryCraft.craftMintLine("p", "w", "m", OptionalInt.of(137)));
        assertEquals("CRAFT minted player=p weapon=w material=m score=none",
                InventoryCraft.craftMintLine("p", "w", "m", OptionalInt.empty()), "an unscored mint says so");
    }

    /** THE WIRING: the craft path logs the line for a WEAPON, after the score is stamped. A source read. */
    @Test
    void theCraftPathLogsItAfterTheStamp() throws IOException {
        String craft = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/menu/InventoryCraft.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int stamp = craft.indexOf("GearScoreItems.stampOnAcquire(minted, definition, viewer, adapters);");
        int log = craft.indexOf("adapters.log().info(craftMintLine(");
        assertTrue(stamp > 0, "the stamp call is not found: the scan is blind");
        assertTrue(log > stamp, "the line is logged, and after the stamp (so it carries the score)");
        assertTrue(craft.substring(stamp, log).contains("instanceof WeaponDefinition"),
                "logged only when the minted gear is a weapon");
    }
}
