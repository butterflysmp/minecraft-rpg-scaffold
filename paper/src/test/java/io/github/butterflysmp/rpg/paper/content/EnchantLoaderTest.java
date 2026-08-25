package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The enchant content pipeline, end to end from the bundled yml.
 *
 * Two distinct things are guarded. First, that the SHIPPED file actually loads and says what it is
 * supposed to say -- a schema typo would otherwise only show as a missing tooltip line on a booted
 * server. Second, and this is the one that would really hurt: that the file's id and
 * {@link Unbreaking#ID} agree. The durability seam compares that constant against the item's state
 * and never consults this registry, so renaming one without the other leaves an enchant that
 * renders and does nothing, or works and shows nothing, with no error anywhere.
 */
class EnchantLoaderTest {

    private static Logger quietLogger() {
        return Logger.getLogger("EnchantLoaderTest-" + System.nanoTime());
    }

    private static Path bundledEnchants(Path dir) throws IOException {
        Path enchantsDir = Files.createDirectory(dir.resolve("enchants"));
        copyBundled("/content/enchants/unbreaking.yml", enchantsDir.resolve("unbreaking.yml"));
        return enchantsDir;
    }

    private static void copyBundled(String resource, Path target) throws IOException {
        try (var in = EnchantLoaderTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "bundled content is missing from the classpath: " + resource);
            Files.write(target, in.readAllBytes());
        }
    }

    @Test
    void theShippedUnbreakingFileLoadsAndSaysWhatTheTooltipNeeds(@TempDir Path dir) throws IOException {
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(bundledEnchants(dir).toFile());

        // EXACTLY one, not merely non-empty. A discovery that finds nothing -- or finds something
        // unexpected -- is a defect, not a quiet no-op, and "non-empty" would pass on both.
        assertEquals(1, enchants.size(), "the shipped enchant roster is exactly unbreaking.yml");

        EnchantDefinition unbreaking = enchants.find("unbreaking").orElseThrow(
                () -> new AssertionError("unbreaking.yml did not load -- every tooltip renders a raw id"));
        assertEquals("Unbreaking", unbreaking.displayName());
        assertEquals(3, unbreaking.maxLevel());
    }

    @Test
    void theShippedFilesIdIsTheOneTheDurabilitySeamLooksFor(@TempDir Path dir) throws IOException {
        // THE DRIFT GUARD. Unbreaking.ID is what WeaponDurability compares against; this file's
        // name is what the registry keys on. Nothing at runtime checks they match -- a rename of
        // either leaves an enchant that renders but never skips wear, or skips wear under a raw id.
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(bundledEnchants(dir).toFile());

        assertTrue(enchants.find(Unbreaking.ID).isPresent(),
                "no shipped content file is named after Unbreaking.ID (" + Unbreaking.ID + ")");
        // Mutation: rename unbreaking.yml, or change Unbreaking.ID -> reddens.
    }

    @Test
    void theIdIsTheFilenameNotAFieldInTheFile(@TempDir Path dir) throws IOException {
        // The file carries a CONFLICTING id: field on purpose. Writing one without it would pass
        // whether the loader read the filename or a field defaulting to it -- so the test would
        // check nothing while reading as though it checked the rule in its own name. A mutation run
        // caught exactly that: switching the loader to s.getString("id", id) reddened nothing.
        Path enchantsDir = Files.createDirectory(dir.resolve("enchants"));
        Files.writeString(enchantsDir.resolve("swiftness.yml"),
                "id: \"something_else\"\ndisplay_name: \"Swiftness\"\nmax_level: 2\n");

        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(enchantsDir.toFile());

        assertTrue(enchants.find("swiftness").isPresent(), "swiftness.yml -> id swiftness");
        assertTrue(enchants.find("something_else").isEmpty(),
                "an id: field in the file is ignored; the filename is the id, as for every content type");
        assertEquals(2, enchants.find("swiftness").orElseThrow().maxLevel());
        // Mutation: read the id from the file -> reddens.
    }

    @Test
    void aMalformedFileIsSkippedAndEveryOtherEnchantStillLoads(@TempDir Path dir) throws IOException {
        // The fail-soft contract every loader in this project keeps: one bad file must not take the
        // server's whole enchant roster down with it.
        Path enchantsDir = bundledEnchants(dir);
        // max_level 9 is past EnchantState.MAX_LEVEL, so EnchantDefinition throws and the loader
        // logs it, names it, and moves on.
        Files.writeString(enchantsDir.resolve("overpowered.yml"), "display_name: \"Overpowered\"\nmax_level: 9\n");

        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(enchantsDir.toFile());

        assertEquals(1, enchants.size(), "the malformed file is skipped");
        assertTrue(enchants.find("unbreaking").isPresent(), "and the good one still loaded");
        assertTrue(enchants.find("overpowered").isEmpty());
    }

    @Test
    void aMaxLevelPastTheModelsMaximumIsRefusedRatherThanSilentlyClamped() {
        // Content-authored, so it THROWS and the loader reports the file by name. Clamping here
        // would leave the yml saying 9 and the game doing 3, which is a lie that survives a restart.
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new EnchantDefinition("overpowered", "Overpowered", EnchantState.MAX_LEVEL + 1));
        assertTrue(ex.getMessage().contains("overpowered"), "the message must name the file at fault");
        assertTrue(ex.getMessage().contains("4"), "and echo the bad value");

        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition("x", "X", 0),
                "max_level 0 is an enchant that can never be unlocked");
    }

    @Test
    void anEmptyDirectoryLoadsNothingRatherThanThrowing(@TempDir Path dir) throws IOException {
        // Not a silent success: RpgPlugin WARNS on a zero-size registry, which is the loud half of
        // this. Here we only pin that the loader itself survives, so that warning is what a
        // missing content folder produces rather than a stack trace on boot.
        Path empty = Files.createDirectory(dir.resolve("enchants"));
        assertEquals(0, new EnchantLoader(quietLogger()).loadAll(empty.toFile()).size());

        assertEquals(0, new EnchantLoader(quietLogger())
                .loadAll(dir.resolve("does-not-exist").toFile()).size());
    }
}
