package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W1 (2026-11-23): a weapon-specific enchant's {@code weapon:} must name a LOADED weapon, so {@code RpgPlugin} loads the
 * weapons BEFORE the enchants and hands the loader the registry's lookup. Read off the source, because the enable cannot
 * run in a unit test: with the order reversed, {@code weapons} is still null when the lookup is first called, and the
 * first file declaring {@code weapon:} would throw inside the loader's catch, be skipped, and warn, on every boot.
 */
class EnchantLoadOrderTest {

    @Test
    void theWeaponsLoadBeforeTheEnchantsAndTheLoaderAsksTheWeaponRegistry() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/RpgPlugin.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int weapons = src.indexOf("this.weapons = new WeaponLoader(getLogger())");
        int enchants = src.indexOf("this.enchants = EnchantLoader.withWeapons(getLogger()");
        assertTrue(weapons > 0, "the weapon load is not found");
        assertTrue(enchants > 0, "the enchant load is not found");
        assertTrue(weapons < enchants, "the weapons must load first: " + weapons + " / " + enchants);
        assertEquals(1, count(src, "EnchantLoader.withWeapons("), "exactly one enchant load in the enable");
        assertEquals(0, count(src, "new EnchantLoader("), "and no loader that cannot see the weapons (D1)");
        assertTrue(src.contains("EnchantLoader.withWeapons(getLogger(), id -> elements.find(id).isPresent(), weapons::find, devServer)"),
                "the loader is handed the weapon registry itself (D1)");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) n++;
        return n;
    }
}
