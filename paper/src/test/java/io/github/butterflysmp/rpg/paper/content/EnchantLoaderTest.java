package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    /** The whole shipped roster, so a count assertion means "exactly these and nothing else". */
    private static final List<String> SHIPPED =
            List.of("unbreaking", "sharpness", "power", "attunement");

    private static Path bundledEnchants(Path dir) throws IOException {
        Path enchantsDir = Files.createDirectory(dir.resolve("enchants"));
        for (String id : SHIPPED) {
            copyBundled("/content/enchants/" + id + ".yml", enchantsDir.resolve(id + ".yml"));
        }
        return enchantsDir;
    }

    /** A minimal damage enchant, so a test can vary ONE field and see what the loader does with it. */
    private static String damageYml(String maxLevel, String weaponClass, String percents) {
        return "display_name: \"Test\"\nmax_level: " + maxLevel + "\neffect: damage\nclass: "
                + weaponClass + "\npercent_by_level: " + percents + "\n";
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

        // EXACTLY four, not merely non-empty. A discovery that finds nothing -- or finds something
        // unexpected -- is a defect, not a quiet no-op, and "non-empty" would pass on both.
        assertEquals(4, enchants.size(), "the shipped enchant roster is exactly these four files");

        EnchantDefinition unbreaking = enchants.find("unbreaking").orElseThrow(
                () -> new AssertionError("unbreaking.yml did not load -- every tooltip renders a raw id"));
        assertEquals("Unbreaking", unbreaking.displayName());
        assertEquals(3, unbreaking.maxLevel());
        assertEquals(EnchantEffect.DURABILITY, unbreaking.effect(),
                "unbreaking binds the durability mechanism, not the damage one");
        assertTrue(unbreaking.isUniversal(), "and it is gated on no class");
        assertEquals(List.of(), unbreaking.percentByLevel(), "a durability enchant carries no curve");
    }

    /**
     * The three SHIPPED damage enchants, one per active class, with the curve the boot gate and
     * {@code DamageEnchantsTest} both derive their numbers from.
     *
     * This is a content assertion, and it is here rather than in prose because the class tokens are
     * the thing most likely to rot: "ranged" would not parse (the enum is RANGER), and a Sharpness
     * that loaded as {@code mage} would silently boost staves and leave swords alone -- working,
     * wrong, and invisible without a booted server.
     */
    @Test
    void theThreeShippedDamageEnchantsCarryTheirClassAndCurve(@TempDir Path dir) throws IOException {
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(bundledEnchants(dir).toFile());

        assertEquals(WeaponClass.MELEE, enchants.find("sharpness").orElseThrow().weaponClass());
        assertEquals(WeaponClass.RANGER, enchants.find("power").orElseThrow().weaponClass());
        assertEquals(WeaponClass.MAGE, enchants.find("attunement").orElseThrow().weaponClass());

        for (String id : List.of("sharpness", "power", "attunement")) {
            EnchantDefinition d = enchants.find(id).orElseThrow();
            assertEquals(EnchantEffect.DAMAGE, d.effect(), id + " binds the damage mechanism");
            assertEquals(List.of(5, 10, 15), d.percentByLevel(), id + " carries the shipped curve");
            assertEquals(3, d.maxLevel());
        }
    }

    // --- The schema rules, one test each ---------------------------------------------------------

    @Test
    void anEnchantWithNoEffectIsRefusedRatherThanDefaultingToDurability(@TempDir Path dir) throws IOException {
        // A default would have to be `durability`, which would turn a damage enchant whose effect
        // line was misspelled into an Unbreaking -- granting no damage and skipping wear instead.
        // Working, wrong, and invisible. Same reasoning that makes `class` required on a weapon.
        //
        // THE FIXTURE IS `class: universal` ON PURPOSE, and an earlier version using `class: melee`
        // was a test that could not fail. Under the mutation this test exists to catch -- effect
        // defaulting to durability -- a melee fixture is STILL rejected, by the unrelated
        // durability-must-be-universal rule, so the file was skipped either way and the assertion
        // passed without ever exercising the rule in its own name. A mutation run caught exactly
        // that: defaulting the effect reddened nothing. Universal makes the defaulted enchant
        // otherwise VALID, so only the missing-effect rule can reject it.
        Path enchantsDir = bundledEnchants(dir);
        Files.writeString(enchantsDir.resolve("noeffect.yml"),
                "display_name: \"No Effect\"\nmax_level: 3\nclass: universal\n");

        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(enchantsDir.toFile());

        assertEquals(4, enchants.size(), "the malformed file is skipped");
        assertTrue(enchants.find("noeffect").isEmpty(),
                "a file with no effect names no mechanism, so it cannot load as some default one");
        var ex = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "noeffect", "No Effect", 3, null, WeaponClass.MELEE, List.of()));
        assertTrue(ex.getMessage().contains("noeffect"), "the message must name the file at fault");
    }

    @Test
    void anUnknownEffectOrClassIsRefusedAndEchoesTheBadValue(@TempDir Path dir) throws IOException {
        Path enchantsDir = bundledEnchants(dir);
        Files.writeString(enchantsDir.resolve("bogus.yml"),
                "display_name: \"Bogus\"\nmax_level: 3\neffect: teleportation\nclass: melee\n");
        // "ranged" is the LABEL, not the token. This is the exact typo the design brief carried, and
        // it must fail loudly rather than load as some default class.
        Files.writeString(enchantsDir.resolve("mistoken.yml"), damageYml("3", "ranged", "[5, 10, 15]"));

        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(enchantsDir.toFile());

        assertEquals(4, enchants.size(), "both malformed files are skipped, the shipped four remain");
        assertTrue(enchants.find("bogus").isEmpty());
        assertTrue(enchants.find("mistoken").isEmpty(), "'ranged' is not a class token -- 'ranger' is");
    }

    @Test
    void aDamageEnchantWithoutACurveIsRefused(@TempDir Path dir) throws IOException {
        // Silently granting 0% would be the worst outcome: it renders on the tooltip, promises a
        // multiplier, and multiplies by exactly 1.
        Path enchantsDir = bundledEnchants(dir);
        Files.writeString(enchantsDir.resolve("nocurve.yml"),
                "display_name: \"No Curve\"\nmax_level: 3\neffect: damage\nclass: melee\n");

        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(enchantsDir.toFile());

        assertEquals(4, enchants.size());
        assertTrue(enchants.find("nocurve").isEmpty());
    }

    @Test
    void aCurveWhoseLengthDisagreesWithMaxLevelIsRefusedBothWays(@TempDir Path dir) throws IOException {
        // THE rule that makes level -> percent total. A SHORT list leaves a legal level with no
        // percent; a LONG one hides levels the enchant can never reach. Both directions, because a
        // ">= maxLevel" check would pass the second and a "<=" would pass the first.
        var tooShort = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "short", "Short", 3, EnchantEffect.DAMAGE, WeaponClass.MELEE, List.of(5, 10)));
        assertTrue(tooShort.getMessage().contains("short"), "names the file");
        assertTrue(tooShort.getMessage().contains("2"), "and echoes the bad length");

        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "long", "Long", 2, EnchantEffect.DAMAGE, WeaponClass.MELEE, List.of(5, 10, 15)));

        Path enchantsDir = bundledEnchants(dir);
        Files.writeString(enchantsDir.resolve("short.yml"), damageYml("3", "melee", "[5, 10]"));
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(enchantsDir.toFile());
        assertEquals(4, enchants.size(), "and the loader skips it by name rather than crashing");
    }

    @Test
    void aDurabilityEnchantMayNotClaimAClassOrACurve() {
        // A file may not claim a control it does not have. Nothing gates wear by class and nothing
        // reads a percent off a durability enchant, so either key would be a lie the file tells
        // about itself -- the same defect the lore pass fixed by stripping authored colours from
        // display_name once rarity owned the colour.
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "u", "U", 3, EnchantEffect.DURABILITY, WeaponClass.MELEE, List.of()),
                "a class-gated durability enchant is a promise nothing keeps");

        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "u", "U", 3, EnchantEffect.DURABILITY, null, List.of(5, 10, 15)),
                "and nothing would ever read that curve");
    }

    @Test
    void aNegativePercentIsRefusedRatherThanShippedAsACurse() {
        // Stat permits negative modifiers and a curse is a legitimate future idea, but a negative
        // PERCENT is far more likely a sign slip, and one below -100 flips a hit into a negative.
        // A curse wants its own naming and its own decision.
        var ex = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "cursed", "Cursed", 3, EnchantEffect.DAMAGE, WeaponClass.MELEE, List.of(5, -10, 15)));
        assertTrue(ex.getMessage().contains("cursed"), "names the file");
        assertTrue(ex.getMessage().contains("-10"), "and echoes the bad value");
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
                "id: \"something_else\"\ndisplay_name: \"Swiftness\"\nmax_level: 2\n"
                        + "effect: durability\nclass: universal\n");

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

        assertEquals(4, enchants.size(), "the malformed file is skipped");
        assertTrue(enchants.find("unbreaking").isPresent(), "and the good ones still loaded");
        assertTrue(enchants.find("overpowered").isEmpty());
    }

    @Test
    void aMaxLevelPastTheModelsMaximumIsRefusedRatherThanSilentlyClamped() {
        // Content-authored, so it THROWS and the loader reports the file by name. Clamping here
        // would leave the yml saying 9 and the game doing 3, which is a lie that survives a restart.
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new EnchantDefinition("overpowered", "Overpowered", EnchantState.MAX_LEVEL + 1,
                        EnchantEffect.DURABILITY, null, List.of()));
        assertTrue(ex.getMessage().contains("overpowered"), "the message must name the file at fault");
        assertTrue(ex.getMessage().contains("4"), "and echo the bad value");

        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition("x", "X", 0,
                        EnchantEffect.DURABILITY, null, List.of()),
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
