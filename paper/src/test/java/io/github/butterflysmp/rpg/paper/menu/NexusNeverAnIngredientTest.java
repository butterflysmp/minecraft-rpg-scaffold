package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE NEXUS ITEM IS NEVER AN INGREDIENT, IN ANY MATERIAL IT WEARS (the seat's ruling A, 2026-11-22). The defect was
 * master's own: our crafting menu's ingredient walk ({@code RecipeProbe.groups}) skipped only gear, and the Nexus star
 * is a NETHER_STAR, which a beacon takes. With the quiver mirror the same item can be ARROWS, which a spectral or tipped
 * arrow takes. The rule is keyed on the TAG, so the material never enters the decision.
 *
 * <p>No unit test can build an {@code ItemStack} (no RegistryAccess headless), so the decision is pinned pure
 * ({@code CraftMatrixScreen.refusedAsIngredient}: the tag's presence, whatever the material), and the walk is read from
 * source to prove it consults it. The server half is listed UNREAD in GATE-vanilla-bows (a craft that would take the
 * star), with this as its witness.
 */
class NexusNeverAnIngredientTest {

    /** THE STAR (a NETHER_STAR carrying the Nexus tag): refused. A plain nether star stays an ingredient. */
    @Test
    void theStarIsNeverAnIngredientButAPlainNetherStarIs() {
        assertTrue(CraftMatrixScreen.refusedAsIngredient(false, true), "the Nexus star, a beacon's nether star");
        assertFalse(CraftMatrixScreen.refusedAsIngredient(false, false), "a plain nether star: still an ingredient");
    }

    /** THE MIRROR (ARROWS carrying the Nexus tag): refused by the same tag. Plain arrows stay ingredients. */
    @Test
    void theMirrorIsNeverAnIngredientButPlainArrowsAre() {
        assertTrue(CraftMatrixScreen.refusedAsIngredient(false, true), "the mirror's arrows, a spectral arrow's arrow");
        assertFalse(CraftMatrixScreen.refusedAsIngredient(false, false), "plain arrows: still an ingredient");
        assertTrue(CraftMatrixScreen.refusedAsIngredient(true, false), "CONTROL: gear, as before");
    }

    /** The walk consults it: RecipeProbe.groups' skip is neverAnIngredient, which reads the Nexus tag. */
    @Test
    void theIngredientWalkConsultsIt() throws IOException {
        String probe = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/menu/RecipeProbe.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int start = probe.indexOf("private static List<Group> groups(PlayerInventory inventory, Keys keys) {");
        assertTrue(start > 0, "RecipeProbe.groups is not found: the scan is blind");
        String body = probe.substring(start, probe.indexOf("\n    }\n", start));
        assertTrue(body.contains("if (CraftMatrixScreen.neverAnIngredient(item, keys)) continue;"), body);
        assertFalse(body.contains("if (CraftMatrixScreen.isGear(item, keys)) continue;"), "the gear-only skip is gone");

        String screen = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/menu/CraftMatrixScreen.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int at = screen.indexOf("public static boolean neverAnIngredient(ItemStack item, Keys keys) {");
        assertTrue(at > 0, "neverAnIngredient is not found");
        String predicate = screen.substring(at, screen.indexOf("\n    }\n", at));
        assertTrue(predicate.contains("NexusItems.isNexus(item, keys)"), "keyed on the Nexus TAG: " + predicate);
    }
}
