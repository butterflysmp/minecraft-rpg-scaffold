package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BEN'S NUMBERS, PINNED CHARACTER FOR CHARACTER IN THE FILES (the seat's brief, 2026-11-23): "2. I want it to split into 3
 * and 6 blocks instead of 12" (asked, "B": "Split after 6 blocks into 3 pieces; pieces keep their 14"), "1. I want the
 * Airburst Flames to seek as well", "5. Increase the damage of the crystal to 30/50/70%". The parsed values are the
 * content tests'; this reads the lines as written, so a value that parses the same but is written otherwise is caught.
 * And the removal's half of the seat's item 6: OrbRemovals cancels the orb's expiry (read from its source).
 */
class BensEnchantChangesTest {

    private static List<String> lines(String file) throws IOException {
        return Files.readString(Path.of("src/main/resources/content/enchants/" + file), StandardCharsets.UTF_8)
                .replace("\r", "").lines().map(l -> l.replaceAll("\\s+#.*$", "")).toList();
    }

    @Test
    void airburstSplitsAtSixIntoThreeThatKeepTheirFourteenAndSeek() throws IOException {
        List<String> a = lines("airburst_flames.yml");
        for (String want : List.of("  travel_blocks: 6", "  count: 3", "  burst_travel_blocks: 14",
                "  damage_by_level: [25, 30, 35]", "  homing:", "    lerp: 0.45", "    activation_blocks: 0",
                "    search_radius: 7")) {
            assertTrue(a.contains(want), "airburst_flames.yml holds '" + want + "': " + a);
        }
        assertTrue(!a.contains("  travel_blocks: 14"), "the old 14 is gone");
    }

    @Test
    void theCrystalStrikesForThirtyFiftyAndSeventy() throws IOException {
        List<String> c = lines("lapis_crystal.yml");
        assertTrue(c.contains("        percent_by_level: [30, 50, 70]"), c.toString());
        assertTrue(!c.contains("        percent_by_level: [20, 30, 40]"), "the old 20/30/40 is gone");
    }

    @Test
    void everyOrbRemovalCancelsItsExpiry() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/weapon/OrbRemovals.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        String discard = src.substring(src.indexOf("private static void discard("));
        assertTrue(discard.contains("if (orb.expiryTask() >= 0) ctx.work().take(orb.expiryTask());"),
                "discard, which every paper removal (quit, death, world change, stop, the selftest's clear_state) runs");
        assertTrue(src.contains("ctx.orbs().remove(owner).ifPresent(orb -> discard(ctx, orb, cause));")
                && src.contains("for (Orbs.Orb orb : ctx.orbs().removeAll()) discard(ctx, orb, \"stop\");"),
                "and both removal entries go through discard");
    }

    @Test
    void theOrbShotLineNamesItsEndsAndItsVisual() {
        assertTrue(io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld.orbShotLine(new java.util.UUID(0, 1),
                new io.github.butterflysmp.rpg.core.Vec3(116.5, 202.62, -3.5),
                new io.github.butterflysmp.rpg.core.Vec3(116.5, 202.74, 1.5), "lapis_beam", 6680)
                .equals("ORBSHOT owner=00000000-0000-0000-0000-000000000001 from=116.500,202.620,-3.500"
                        + " to=116.500,202.740,1.500 visual=lapis_beam tick=6680"));
    }
}
