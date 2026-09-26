package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Ability Stone's IDENTITY is its PDC key, never its material (PLAN-build-system.md section 2.5.2).
 *
 * <p><b>WHY THIS READS SOURCE AND REFLECTION INSTEAD OF MINTING TWO ITEMS.</b> The plan asked for a
 * {@code StoneItemsTest} that mints a stone and a Ward Charm and compares them. That is not writable
 * here: {@code new ItemStack(...)} throws without a running server and there is no MockBukkit -- the
 * limit {@code NexusSlots}' javadoc records. So the item-level half is gate row ST16, played, and this
 * pins what CAN be pinned without a server: the key exists under its own name, the dead
 * {@code ability_id} key is gone and nothing named {@code ability*} replaced it, and {@code isStone}
 * reads the key and not the material.
 */
class StoneIdentityTest {

    private static final Path KEYS_SOURCE = Path.of("src", "main", "java", "io", "github", "butterflysmp",
            "rpg", "paper", "adapter", "Keys.java");
    private static final Path STONE_ITEMS_SOURCE = Path.of("src", "main", "java", "io", "github", "butterflysmp",
            "rpg", "paper", "build", "StoneItems.java");

    private static List<String> keyFieldNames() {
        List<String> names = new ArrayList<>();
        for (Field f : Keys.class.getDeclaredFields()) {
            if (f.getType() == NamespacedKey.class) names.add(f.getName());
        }
        return names;
    }

    /** The control: the scan sees the keys it must see, so its "absent" below is not a blind zero. */
    @Test
    void theScanSeesTheKeysItShould() {
        List<String> names = keyFieldNames();
        assertTrue(names.contains("buildStone"), "the stone's key: " + names);
        assertTrue(names.contains("nexus"), "the star's key, the control: " + names);
        assertTrue(names.contains("accessoryId"), "the accessory key, the control: " + names);
    }

    @Test
    void noKeyIsNamedAbilityAnything() {
        for (String name : keyFieldNames()) {
            assertFalse(name.toLowerCase().startsWith("ability"),
                    "Keys." + name + " -- the dead ability_id key was deleted in the Ability Stone slice, and "
                            + "the plan forbids the new item sharing the old weapon's id, keys or PDC names");
        }
    }

    @Test
    void theStoneKeyStringIsBuildStoneAndNoAbilityKeyStringIsBuilt() throws IOException {
        String source = Files.readString(KEYS_SOURCE, StandardCharsets.UTF_8);
        assertEquals(1, count(source, "new NamespacedKey(plugin, \"build_stone\")"), "the stone's key string");
        assertEquals(0, count(source, "new NamespacedKey(plugin, \"ability_"), "no ability_* key string");
        // Control for the zero: the same pattern shape finds a key that IS built.
        assertEquals(1, count(source, "new NamespacedKey(plugin, \"nexus\")"));
    }

    /**
     * THE MUTATION THIS KILLS: {@code isStone} keying on {@code Material.ECHO_SHARD}. Every universal
     * accessory and {@code volley_stone} are echo shards too, so a material test would make a Ward Charm
     * cast. The body of {@code isStone} is read and must name the key and never the item's type.
     */
    @Test
    void isStoneReadsTheKeyNotTheMaterial() throws IOException {
        String source = Files.readString(STONE_ITEMS_SOURCE, StandardCharsets.UTF_8);
        int start = source.indexOf("public static boolean isStone(");
        assertTrue(start >= 0, "isStone is gone -- rename this test with it");
        int end = source.indexOf("\n    }", start);
        String body = source.substring(start, end);
        assertTrue(body.contains("keys.buildStone"), "isStone must read the PDC key: " + body);
        assertFalse(body.contains("getType()"), "isStone must not read the material: " + body);
        assertFalse(body.contains("Material."), "isStone must not name a material: " + body);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) n++;
        return n;
    }
}
