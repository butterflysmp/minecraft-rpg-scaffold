package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** A HELD ACCESSORY GRANTS NOTHING, because the item carries its id and no stat key. The row the
 * held-accessory mutation must redden. ***
 *
 * <p>The all-slot scanners ({@code HealthModifierItems}, {@code CritModifierItems}, the regen scans)
 * read their keys off ANY equipped stack, main hand included. An accessory whose mint wrote one of
 * those keys would grant its stat while merely held -- and nothing would say so, because the stat
 * would also arrive, correctly, from the accessory store once it was worn. No unit test can build an
 * {@code ItemStack} here (there is no MockBukkit), so this is a SOURCE SCAN of the one file that
 * writes an accessory item's container: it must write exactly one key, {@code keys.accessoryId}.
 *
 * <p>The scan's shape is stated so it can be checked: every {@code .set(keys.<field>, ...)} in the
 * file, by regex over the whole source. A control asserts the regex finds the known id write, so an
 * empty scan cannot pass.
 */
class AccessoryItemsWritesOnlyItsIdTest {

    private static final Path ITEMS = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/weapon/AccessoryItems.java");
    private static final Path KEYS = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/adapter/Keys.java");

    private static final Pattern PDC_SET = Pattern.compile("\\.set\\(\\s*keys\\.(\\w+)");

    @Test
    void theMintWritesExactlyOneContainerKey_theAccessoryId() throws IOException {
        String source = Files.readString(ITEMS, StandardCharsets.UTF_8);
        Matcher m = PDC_SET.matcher(source);
        List<String> written = m.results().map(r -> r.group(1)).toList();
        assertEquals(List.of("accessoryId"), written,
                "an accessory item must carry its id and NOTHING else -- a stat key on it would be read"
                        + " by the all-slot scanners and granted while merely HELD");
        // Mutation: add `meta.getPersistentDataContainer().set(keys.healthBoost, DOUBLE, 10.0)` to
        // AccessoryItems.mint -> reddens.
    }

    @Test
    void theMintDoesNotCallTheCarryThatWouldMoveAScoreOrEnchants() throws IOException {
        String source = Files.readString(ITEMS, StandardCharsets.UTF_8);
        assertFalse(source.contains("carryInstanceData("),
                "A4: an accessory carries no score and no enchants, so remint moves only its id tag");
        assertTrue(source.contains("carryTag("), "control: the id tag IS carried");
    }

    /** Ruling A2: the new key's name contains no "quiver". */
    @Test
    void theAccessoryKeyNameContainsNoQuiver() throws IOException {
        String line = Files.readAllLines(KEYS, StandardCharsets.UTF_8).stream()
                .filter(l -> l.contains("this.accessoryId ="))
                .findFirst().orElseThrow(() -> new AssertionError("Keys must build accessoryId"));
        assertTrue(line.contains("\"accessory_id\""), line.trim());
        assertFalse(line.toLowerCase().contains("quiver"), line.trim());
    }
}
