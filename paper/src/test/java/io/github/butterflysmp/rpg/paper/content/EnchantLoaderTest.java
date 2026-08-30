package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    /**
     * The whole shipped roster, DISCOVERED from the classpath rather than enumerated.
     *
     * <p>This was a hardcoded {@code List.of("unbreaking", "sharpness", "power", "attunement")},
     * and the count assertions below claimed "the shipped enchant roster is exactly these four
     * files" -- a claim the fixture could not make, because it copied only the four it already knew
     * about. A fifth shipped file would have been loaded by NO test at all: no schema check, no
     * class-token check, no curve check, and every count still green. That is CLAUDE.md's discovery
     * trap one directory over from the {@code getResource("content/")} case -- a scan that finds
     * only what it was told to look for is indistinguishable from a scan that works.
     *
     * <p>So it lists the directory, and <b>fails loudly on zero</b>: finding nothing is a defect,
     * not an empty roster. Proven by positive control rather than by argument -- a deliberately
     * malformed probe file dropped into {@code content/enchants/} reddens
     * {@link #everyShippedEnchantFileLoadsRatherThanOnlyTheOnesWeRemembered}, where the old fixture
     * stayed green.
     *
     * <p><b>It lists the CLASSPATH, which is {@code target/classes}, not {@code src}.</b> Maven's
     * resource copy adds but never removes, so a content file DELETED from {@code src} lingers in
     * {@code target/classes} and this reddens until {@code clean}. Measured, not predicted: after
     * removing the probe from {@code src} the suite still failed 7, naming {@code _probe}, until
     * {@code ./mvnw -pl paper -am clean test}. That is the right direction to fail in -- it is the
     * same stale-build-output family as the locked-jar deploy CLAUDE.md records -- but it means an
     * incremental run after deleting an enchant file is a red that {@code clean} explains.
     *
     * <p>Throws {@link IOException} rather than {@code URISyntaxException} so every caller's
     * signature is unchanged; the URL comes from our own classpath and cannot realistically be
     * malformed, but it fails loudly rather than silently if it ever is.
     */
    private static List<String> shippedIds() throws IOException {
        URL dir = EnchantLoaderTest.class.getResource("/content/enchants");
        assertNotNull(dir, "content/enchants is not on the test classpath at all");

        Path root;
        try {
            root = Path.of(dir.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("content/enchants is not a readable directory URL: " + dir, e);
        }

        try (var entries = Files.list(root)) {
            List<String> ids = entries.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".yml"))
                    .map(name -> name.substring(0, name.length() - ".yml".length()))
                    .sorted()
                    .toList();
            assertFalse(ids.isEmpty(), "discovered NO shipped enchant files under content/enchants "
                    + "-- a scan that finds nothing is a defect, not an empty roster");
            return ids;
        }
    }

    private static Path bundledEnchants(Path dir) throws IOException {
        Path enchantsDir = Files.createDirectory(dir.resolve("enchants"));
        for (String id : shippedIds()) {
            copyBundled("/content/enchants/" + id + ".yml", enchantsDir.resolve(id + ".yml"));
        }
        return enchantsDir;
    }

    /** A minimal damage enchant, so a test can vary ONE field and see what the loader does with it. */
    private static String damageYml(String maxLevel, String weaponClass, String percents) {
        return "display_name: \"Test\"\nmax_level: " + maxLevel + "\neffect: damage\nclass: "
                + weaponClass + "\nvalue_by_level: " + percents + "\n";
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

        // The WHOLE discovered roster, not merely non-empty. A discovery that finds nothing -- or
        // finds something unexpected -- is a defect, not a quiet no-op, and "non-empty" would pass
        // on both. shippedIds() itself refuses to return an empty list, so this cannot pass 0 == 0.
        assertEquals(shippedIds().size(), enchants.size(),
                "every shipped enchant file loads; none is skipped");

        EnchantDefinition unbreaking = enchants.find("unbreaking").orElseThrow(
                () -> new AssertionError("unbreaking.yml did not load -- every tooltip renders a raw id"));
        assertEquals("Unbreaking", unbreaking.displayName());
        assertEquals(3, unbreaking.maxLevel());
        assertEquals(EnchantEffect.DURABILITY, unbreaking.effect(),
                "unbreaking binds the durability mechanism, not the damage one");
        assertTrue(unbreaking.isUniversal(), "and it is gated on no class");
        assertEquals(List.of(), unbreaking.valueByLevel(), "a durability enchant carries no curve");
    }

    /**
     * THE ONE THAT CATCHES A NEW SHIPPED FILE. Every yml under {@code content/enchants/} loads, by
     * ID, not merely by count.
     *
     * <p>The count assertions elsewhere compare the loader's output against {@code shippedIds()},
     * which is derived from the same listing -- so a file that loads is counted and a file that is
     * skipped is not, and the two move together only if nothing is skipped. This asserts the SET,
     * which is what makes that non-circular: a shipped file the loader refuses is absent from the
     * registry but present in the listing, and the sets diverge by name.
     *
     * <p>The old enumerated fixture could not fail this way. It copied four ids it already knew,
     * so a fifth shipped file was never loaded by any test and every assertion stayed green --
     * schema unchecked, class token unchecked, curve unchecked. This is the test that broke under
     * the malformed-probe positive control; see {@link #shippedIds()}.
     */
    @Test
    void everyShippedEnchantFileLoadsRatherThanOnlyTheOnesWeRemembered(@TempDir Path dir) throws IOException {
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(bundledEnchants(dir).toFile());

        Set<String> loaded = enchants.all().stream()
                .map(EnchantDefinition::id).collect(Collectors.toSet());

        assertEquals(Set.copyOf(shippedIds()), loaded,
                "every file under content/enchants must LOAD -- a shipped file the loader skips is "
                        + "an enchant that renders on a tooltip and grants nothing");
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

        assertEquals(GearClass.MELEE, enchants.find("sharpness").orElseThrow().gearClass());
        assertEquals(GearClass.RANGER, enchants.find("power").orElseThrow().gearClass());
        assertEquals(GearClass.MAGE, enchants.find("attunement").orElseThrow().gearClass());

        for (String id : List.of("sharpness", "power", "attunement")) {
            EnchantDefinition d = enchants.find(id).orElseThrow();
            assertEquals(EnchantEffect.DAMAGE, d.effect(), id + " binds the damage mechanism");
            assertEquals(List.of(5, 10, 15), d.valueByLevel(), id + " carries the shipped curve");
            assertEquals(3, d.maxLevel());
        }
    }

    /**
     * The shipped SHIELD enchant, field for field.
     *
     * <p>The sibling of {@code theThreeShippedDamageEnchantsCarryTheirClassAndCurve}, and here for
     * the same reason: the class token is the thing most likely to rot. A Bulwark that loaded as
     * {@code universal} would be offered on every sword in the game and do nothing on any of them;
     * one that loaded as {@code melee} would be offered on swords alone and still do nothing. Both
     * are working, wrong, and invisible without a booted server -- except that the loader now
     * refuses them outright, which this asserts by asserting the value that survived.
     *
     * <p><b>This test could not have existed before the fixture stopped enumerating the roster.</b>
     * Positive control, measured: flipping {@code class: shield} to {@code class: universal} in the
     * shipped file makes the loader skip it and fails 7 tests, naming bulwark --
     * {@code expected: <[unbreaking, sharpness, power, attunement, bulwark]> but was:
     * <[attunement, unbreaking, power, sharpness]>}. Under the old fixture that edit was silent.
     */
    @Test
    void theShippedBulwarkCarriesItsShieldGateAndCurve(@TempDir Path dir) throws IOException {
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(bundledEnchants(dir).toFile());

        EnchantDefinition bulwark = enchants.find("bulwark").orElseThrow(
                () -> new AssertionError("bulwark.yml did not load -- shields would roll only Unbreaking"));

        assertEquals("Bulwark", bulwark.displayName());
        assertEquals(EnchantEffect.BLOCK_DR, bulwark.effect(), "bulwark binds the block mechanism");
        assertEquals(GearClass.SHIELD, bulwark.gearClass(),
                "a block enchant gated anywhere else would never fire");
        assertFalse(bulwark.isUniversal(), "universal would put it in every weapon's roll pool");
        assertEquals(List.of(5, 10, 15), bulwark.valueByLevel(), "the shipped curve");
        assertEquals(3, bulwark.maxLevel());
        assertEquals("shield", bulwark.icon());
    }

    /**
     * The shipped REFLECT enchant, field for field — and this one is load-bearing in a way the
     * directory-scan fixture cannot be.
     *
     * <p>{@code everyShippedEnchantFileLoads…} asserts the id SET, so it catches a file the loader
     * REFUSES. It cannot catch a file that loads perfectly well while saying the wrong thing.
     * Specifically: <b>{@code thorns.yml} authored with {@code effect: block_dr} gates fine on
     * {@code class: shield}, loads cleanly, and reddens nothing at all</b> — Thorns would silently
     * become a second Bulwark, granting +10/20/30% block and zero reflect. A {@code [5, 10, 15]}
     * curve would be an equally silent 3x nerf.
     *
     * <p>So the effect and the curve are asserted here by value. The {@code class} token is the one
     * field the loader itself guards (it refuses {@code universal} and the weapon classes outright),
     * and it is asserted anyway because reading it is free and a reader should not have to know
     * which fields are doubly covered.
     */
    @Test
    void theShippedThornsCarriesItsShieldGateAndCurve(@TempDir Path dir) throws IOException {
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(bundledEnchants(dir).toFile());

        EnchantDefinition thorns = enchants.find("thorns").orElseThrow(
                () -> new AssertionError("thorns.yml did not load -- shields would roll no reflect"));

        assertEquals("Thorns", thorns.displayName());
        assertEquals(EnchantEffect.REFLECT, thorns.effect(),
                "thorns binds the REFLECT mechanism -- block_dr here is a silent second Bulwark");
        assertEquals(GearClass.SHIELD, thorns.gearClass(),
                "a reflect gated anywhere else is read off a stack that cannot block");
        assertFalse(thorns.isUniversal(), "universal would put it in every weapon's roll pool");
        assertEquals(List.of(10, 20, 30), thorns.valueByLevel(),
                "the shipped curve -- 1.5/3.0/4.5 off a 15.0 mob, which the popup rounds to 2/3/5");
        assertEquals(3, thorns.maxLevel());

        // And it is NOT the same mechanism as the shield's other enchant, stated explicitly because
        // "both are shield enchants with a percent curve" is exactly how the two get conflated.
        assertNotEquals(enchants.find("bulwark").orElseThrow().effect(), thorns.effect(),
                "Bulwark and Thorns must bind DIFFERENT mechanisms");
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

        assertEquals(shippedIds().size(), enchants.size(), "the malformed file is skipped");
        assertTrue(enchants.find("noeffect").isEmpty(),
                "a file with no effect names no mechanism, so it cannot load as some default one");
        var ex = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "noeffect", "No Effect", 3, null, GearClass.MELEE, List.of()));
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

        assertEquals(shippedIds().size(), enchants.size(),
                "both malformed files are skipped, the whole shipped roster remains");
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

        assertEquals(shippedIds().size(), enchants.size());
        assertTrue(enchants.find("nocurve").isEmpty());
    }

    @Test
    void aCurveWhoseLengthDisagreesWithMaxLevelIsRefusedBothWays(@TempDir Path dir) throws IOException {
        // THE rule that makes level -> percent total. A SHORT list leaves a legal level with no
        // percent; a LONG one hides levels the enchant can never reach. Both directions, because a
        // ">= maxLevel" check would pass the second and a "<=" would pass the first.
        var tooShort = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "short", "Short", 3, EnchantEffect.DAMAGE, GearClass.MELEE, List.of(5, 10)));
        assertTrue(tooShort.getMessage().contains("short"), "names the file");
        assertTrue(tooShort.getMessage().contains("2"), "and echoes the bad length");

        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "long", "Long", 2, EnchantEffect.DAMAGE, GearClass.MELEE, List.of(5, 10, 15)));

        Path enchantsDir = bundledEnchants(dir);
        Files.writeString(enchantsDir.resolve("short.yml"), damageYml("3", "melee", "[5, 10]"));
        EnchantRegistry enchants = new EnchantLoader(quietLogger()).loadAll(enchantsDir.toFile());
        assertEquals(shippedIds().size(), enchants.size(),
                "and the loader skips it by name rather than crashing");
    }

    @Test
    void aDurabilityEnchantMayNotClaimAClassOrACurve() {
        // A file may not claim a control it does not have. Nothing gates wear by class and nothing
        // reads a percent off a durability enchant, so either key would be a lie the file tells
        // about itself -- the same defect the lore pass fixed by stripping authored colours from
        // display_name once rarity owned the colour.
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "u", "U", 3, EnchantEffect.DURABILITY, GearClass.MELEE, List.of()),
                "a class-gated durability enchant is a promise nothing keeps");

        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "u", "U", 3, EnchantEffect.DURABILITY, null, List.of(5, 10, 15)),
                "and nothing would ever read that curve");
    }

    /**
     * The same "a file may not claim a control it does not have" rule, on the two gates Slice 2
     * adds. Each says: this effect's mechanism only ever runs against one kind of gear, so gating
     * it elsewhere is a promise nothing keeps.
     */
    @Test
    void aBlockEnchantMustBeGatedOnShieldBecauseNothingElseCanBlock() {
        // The load-bearing one is UNIVERSAL, not the wrong-weapon case. A universal Bulwark enters
        // EVERY weapon's roll pool (poolFor admits a null gate unconditionally), so every sword
        // would offer a candidate that costs XP and does nothing -- exactly the mistake `class:` is
        // required and spelled out to prevent.
        var universal = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "b", "B", 3, EnchantEffect.BLOCK_DR, null, List.of(5, 10, 15)),
                "a universal block enchant would be offered on every weapon in the game");
        assertTrue(universal.getMessage().contains("every weapon's pool"),
                "the refusal must say WHY universal is the dangerous one: " + universal.getMessage());

        for (GearClass weapon : List.of(GearClass.MELEE, GearClass.RANGER, GearClass.MAGE)) {
            assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                    "b", "B", 3, EnchantEffect.BLOCK_DR, weapon, List.of(5, 10, 15)),
                    weapon + " cannot block, so a block enchant on it would never fire");
        }

        // And the one shape that IS legal, so this test cannot pass by refusing everything.
        assertDoesNotThrow(() -> new EnchantDefinition(
                "b", "B", 3, EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(5, 10, 15)));
    }

    /**
     * The same gate rule on REFLECT, and it is here because getting it wrong is the ONE mistake in
     * this arm that a shipped-content test would still catch. Its sibling below (the curve rules) is
     * the one nothing else would catch at all.
     */
    @Test
    void aReflectEnchantMustBeGatedOnShieldBecauseNothingElseCanBlock() {
        var universal = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "r", "R", 3, EnchantEffect.REFLECT, null, List.of(10, 20, 30)),
                "a universal reflect enchant would be offered on every weapon in the game");
        assertTrue(universal.getMessage().contains("every weapon's pool"),
                "the refusal must say WHY universal is the dangerous one: " + universal.getMessage());

        for (GearClass weapon : List.of(GearClass.MELEE, GearClass.RANGER, GearClass.MAGE)) {
            assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                    "r", "R", 3, EnchantEffect.REFLECT, weapon, List.of(10, 20, 30)),
                    weapon + " cannot block, so a reflect read off the blocking stack never fires");
        }

        assertDoesNotThrow(() -> new EnchantDefinition(
                "r", "R", 3, EnchantEffect.REFLECT, GearClass.SHIELD, List.of(10, 20, 30)));
    }

    /**
     * THE ONE NOTHING ELSE CATCHES. Omitting {@code requireCurve} from the REFLECT arm leaves
     * {@code thorns.yml} loading perfectly well -- it has a curve -- so no shipped-content test and
     * no loader test reddens. Only this does.
     *
     * <p>And the negative rule is worth more here than on any other effect: a negative reflect is not
     * a weak reflect. It goes through {@code applyDamage} to {@code stats.damage} and <b>HEALS the
     * mob that is attacking you</b>. That is the rule the shared validator exists to make
     * unforgettable, and this is what proves the REFLECT arm actually calls it.
     */
    @Test
    void aReflectEnchantIsHeldToTheSameCurveRulesAsABlockOne() {
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "r", "R", 3, EnchantEffect.REFLECT, GearClass.SHIELD, List.of()),
                "a reflect enchant with no curve has no numbers to reflect by");
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "r", "R", 3, EnchantEffect.REFLECT, GearClass.SHIELD, List.of(10, 20)),
                "a short curve leaves a legal level with no percent");
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "r", "R", 2, EnchantEffect.REFLECT, GearClass.SHIELD, List.of(10, 20, 30)),
                "a long curve hides levels the enchant can never reach");
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "r", "R", 3, EnchantEffect.REFLECT, GearClass.SHIELD, List.of(10, -20, 30)),
                "a NEGATIVE reflect would heal the attacking mob rather than hurt it");
    }

    @Test
    void aDamageEnchantMayNotBeGatedOnShieldBecauseTheDamageGateReadsTheWeapon() {
        // DamageEnchantItems reads the MAIN HAND's weapon and maps it through GearClass.of, which
        // can never yield SHIELD. So this is structurally unreachable, not merely useless.
        var ex = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "s", "S", 3, EnchantEffect.DAMAGE, GearClass.SHIELD, List.of(5, 10, 15)));
        assertTrue(ex.getMessage().contains("main hand"),
                "the refusal must name the reader that could never see it: " + ex.getMessage());

        // Every other gate stays legal, universal included -- this rule removes ONE value, and a
        // universal damage enchant (the KEEN shape) must keep working.
        assertDoesNotThrow(() -> new EnchantDefinition(
                "k", "K", 3, EnchantEffect.DAMAGE, null, List.of(5, 10, 15)));
        for (GearClass weapon : List.of(GearClass.MELEE, GearClass.RANGER, GearClass.MAGE)) {
            assertDoesNotThrow(() -> new EnchantDefinition(
                    "d", "D", 3, EnchantEffect.DAMAGE, weapon, List.of(5, 10, 15)));
        }
    }

    @Test
    void aBlockEnchantIsHeldToTheSameCurveRulesAsADamageOne() {
        // The requireCurve lift, from the other side: BLOCK_DR did not get a weaker validator by
        // being newer. A negative block percent silently WEAKENS the shield the file claims to
        // strengthen, which is the quiet direction of the same defect.
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "b", "B", 3, EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of()),
                "a block enchant with no curve has no numbers to compose");
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "b", "B", 3, EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(5, 10)),
                "a short curve leaves a legal level with no percent");
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "b", "B", 2, EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(5, 10, 15)),
                "a long curve hides levels the enchant can never reach");
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "b", "B", 3, EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(5, -10, 15)),
                "a negative block percent weakens the shield the file claims to strengthen");
    }

    @Test
    void aDamageEnchantMayNotBeGatedOnGearTheDamageScanCanNeverSEE() {
        // THE GATE THAT WAS NAMED FROM THE WRONG SIDE. It read `gearClass == SHIELD` -- refusing the
        // one kind that existed when it was written -- so the instant GearClass gained ARMOR, a file
        // with `effect: damage` + `class: armor` LOADED CLEAN. DamageEnchantItems reads the MAIN
        // HAND and maps it through GearClass.of, which yields only MELEE, RANGER or MAGE, so that
        // enchant could never fire: structurally unreachable, exactly the defect the shield refusal
        // was written to prevent, arriving through the door the check did not name.
        //
        // Now stated as what the gate CAN be, so the NEXT gear kind is refused by default instead of
        // silently admitted.
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "d", "D", 3, EnchantEffect.DAMAGE, GearClass.ARMOR, List.of(5, 10, 15)),
                "a damage enchant on armor could never fire -- the scan reads the main hand");
        assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "d", "D", 3, EnchantEffect.DAMAGE, GearClass.SHIELD, List.of(5, 10, 15)),
                "and the original shield refusal still holds");

        // The three that CAN be reached, plus universal, all still load.
        for (GearClass reachable : List.of(GearClass.MELEE, GearClass.RANGER, GearClass.MAGE)) {
            assertDoesNotThrow(() -> new EnchantDefinition(
                    "d", "D", 3, EnchantEffect.DAMAGE, reachable, List.of(5, 10, 15)),
                    reachable + " is a class the main-hand scan can actually produce");
        }
        assertDoesNotThrow(() -> new EnchantDefinition(
                "d", "D", 3, EnchantEffect.DAMAGE, null, List.of(5, 10, 15)),
                "universal is still legal for a damage enchant");
        // Mutation: revert the gate to `gearClass == SHIELD` -> the ARMOR case loads -> reddens.
    }

    @Test
    void anArmorStatEnchantIsRefusedAnywhereButArmorIncludingUniversal() {
        // Universal is the dangerous typo here, exactly as it is for a shield enchant: it would put
        // a Defense or Max Health enchant into EVERY weapon's roll pool and sell a player an XP
        // unlock that does nothing at all.
        for (EnchantEffect armorEffect : List.of(EnchantEffect.DEFENSE, EnchantEffect.MAX_HEALTH)) {
            assertDoesNotThrow(() -> new EnchantDefinition(
                    "a", "A", 3, armorEffect, GearClass.ARMOR, List.of(3, 6, 9)),
                    armorEffect + " on armor is the one legal gate");
            assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                    "a", "A", 3, armorEffect, null, List.of(3, 6, 9)),
                    armorEffect + " must not be universal -- it would enter every weapon's pool");
            assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                    "a", "A", 3, armorEffect, GearClass.SHIELD, List.of(3, 6, 9)),
                    armorEffect + " is read off worn pieces, so a shield could never fire it");
            assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                    "a", "A", 3, armorEffect, GearClass.MELEE, List.of(3, 6, 9)),
                    armorEffect + " on a weapon could never fire either");
        }
        // Mutation: drop the ARMOR_ONLY arm from the gate switch -> it no longer compiles, which is
        // the point of making requireGate a switch EXPRESSION in this slice. Weaken it to accept
        // null -> the universal assertions redden.
    }

    @Test
    void aNegativePercentIsRefusedRatherThanShippedAsACurse() {
        // Stat permits negative modifiers and a curse is a legitimate future idea, but a negative
        // PERCENT is far more likely a sign slip, and one below -100 flips a hit into a negative.
        // A curse wants its own naming and its own decision.
        var ex = assertThrows(IllegalArgumentException.class, () -> new EnchantDefinition(
                "cursed", "Cursed", 3, EnchantEffect.DAMAGE, GearClass.MELEE, List.of(5, -10, 15)));
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

        assertEquals(shippedIds().size(), enchants.size(), "the malformed file is skipped");
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

    @Test
    void theShippedEnchantsCarryTheirIcon(@TempDir Path dir) throws IOException {
        // The shipped VALUES, not merely that the key parses. The enchant table renders one icon
        // per candidate and three columns of identical books is most of what the layout exists to
        // avoid, so which material each file names is the thing worth pinning.
        EnchantRegistry registry = new EnchantLoader(quietLogger()).loadAll(bundledEnchants(dir).toFile());

        assertEquals("iron_sword", registry.find("sharpness").orElseThrow().icon());
        assertEquals("bow", registry.find("power").orElseThrow().icon());
        assertEquals("blaze_rod", registry.find("attunement").orElseThrow().icon());
        // Deliberately NOT enchanted_book: if the shipped value equalled DEFAULT_ICON this
        // assertion could not tell "the key was read" from "the key was missing and defaulted".
        assertEquals("anvil", registry.find("unbreaking").orElseThrow().icon());
        assertEquals("shield", registry.find("bulwark").orElseThrow().icon());
        assertEquals("arrow", registry.find("thorns").orElseThrow().icon());
        assertNotEquals(EnchantDefinition.DEFAULT_ICON,
                registry.find("unbreaking").orElseThrow().icon());
        // Mutation: change any yml's icon -> reddens.
        // Mutation: drop the icon arg from EnchantLoader.parse -> all four fall back -> reddens.
    }

    @Test
    void anEnchantWithNoIconFallsBackRatherThanBeingSkipped(@TempDir Path dir) throws IOException {
        // The split that matters: effect and class are MECHANICS and are refused when absent, but
        // an icon is DISPLAY and defaults. A cosmetic omission must never cost a working enchant --
        // skipping the file would turn "no picture" into "no enchant".
        Path enchants = Files.createDirectory(dir.resolve("enchants"));
        Files.writeString(enchants.resolve("nameless.yml"),
                "display_name: \"Nameless\"\nmax_level: 3\neffect: durability\nclass: universal\n");

        EnchantRegistry registry = new EnchantLoader(quietLogger()).loadAll(enchants.toFile());

        assertEquals(1, registry.size(), "an enchant without an icon must still LOAD");
        assertEquals(EnchantDefinition.DEFAULT_ICON, registry.find("nameless").orElseThrow().icon());
        // Mutation: make icon required (throw when null) -> the file is skipped, size is 0 -> reddens.
    }

    @Test
    void aBlankIconFallsBackRatherThanRenderingAsNothing(@TempDir Path dir) throws IOException {
        // Blank is not null and would sail past a null-only guard, then resolve to no Material at
        // all -- so the table would paint AIR where a candidate should be. An invisible candidate
        // is unclickable, which reads as the menu being broken rather than as a content typo.
        Path enchants = Files.createDirectory(dir.resolve("enchants"));
        Files.writeString(enchants.resolve("blank.yml"),
                "display_name: \"Blank\"\nicon: \"\"\nmax_level: 3\neffect: durability\nclass: universal\n");

        EnchantRegistry registry = new EnchantLoader(quietLogger()).loadAll(enchants.toFile());

        assertEquals(EnchantDefinition.DEFAULT_ICON, registry.find("blank").orElseThrow().icon());
        // Mutation: drop the isBlank() arm of the record's normalisation -> "" survives -> reddens.
    }

    @Test
    void theSixArgConstructorStillBuildsAUsableEnchant() {
        // The delegating constructor is what kept this whole commit from touching EnchantLoreTest
        // and EnchantEffectLineTest. If it stops defaulting, those two go red for a reason that has
        // nothing to do with them, so the promise is pinned here where it belongs.
        EnchantDefinition legacy = new EnchantDefinition(
                "legacy", "Legacy", 3, EnchantEffect.DURABILITY, null, List.of());

        assertEquals(EnchantDefinition.DEFAULT_ICON, legacy.icon());
        // Mutation: delete the 6-arg constructor -> paper's test sources stop compiling entirely.
    }
}
