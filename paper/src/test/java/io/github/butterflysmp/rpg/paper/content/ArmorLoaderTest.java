package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The armor schema, and whether the six shipped tier files actually satisfy it.
 *
 * Two headlines, because this loader does something no other loader here does.
 *
 * <p>{@link #theShippedArmorParses} loads the REAL {@code content/armor} tree rather than a
 * fixture, and DISCOVERS the tiers rather than enumerating a hardcoded roster. That shape is
 * deliberate and was learned twice on this repo: {@code getResource("content/")} on a shaded jar
 * returns a non-null URL that silently lists nothing, and {@code EnchantLoaderTest} enumerated its
 * roster and so never once loaded {@code bulwark.yml}. A discovery that finds nothing must FAIL,
 * not pass quietly, so the walk asserts it is non-empty before believing anything it found.
 *
 * <p>{@link #aTierFileYieldsExactlyFourCorrectlySlottedPieces} pins the one-file-four-definitions
 * expansion, which is the divergence armor makes from every other content type. Three pieces from a
 * four-slot file would leave a player finding three quarters of a set with nothing in the log to
 * explain the gap.
 *
 * Each test names the mutation it forces red.
 */
class ArmorLoaderTest {

    private static final Logger LOG = Logger.getLogger(ArmorLoaderTest.class.getName());

    /** The six tiers this slice ships. Used only to assert the DISCOVERED set matches. */
    private static final List<String> SHIPPED_TIERS =
            List.of("chainmail", "diamond", "golden", "iron", "leather", "netherite");

    @TempDir
    Path dir;

    private ArmorRegistry load(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
        return new ArmorLoader(LOG).loadAll(new File(dir.toString()));
    }

    private static String tier(String rarity, String body) {
        return "rarity: " + rarity + "\n"
                + "flavor:\n"
                + "  - \"a line\"\n"
                + "pieces:\n" + body;
    }

    /** A well-formed four-slot body for a tier named {@code prefix}. */
    private static String fourSlots(String prefix) {
        return "  head:\n"
                + "    material: " + prefix + "_helmet\n"
                + "    display_name: \"" + prefix + " Helmet\"\n"
                + "    defense: 1\n"
                + "  chest:\n"
                + "    material: " + prefix + "_chestplate\n"
                + "    display_name: \"" + prefix + " Chestplate\"\n"
                + "    defense: 2\n"
                + "  legs:\n"
                + "    material: " + prefix + "_leggings\n"
                + "    display_name: \"" + prefix + " Leggings\"\n"
                + "    defense: 3\n"
                + "  feet:\n"
                + "    material: " + prefix + "_boots\n"
                + "    display_name: \"" + prefix + " Boots\"\n"
                + "    defense: 4\n";
    }

    // --- The shipped content --------------------------------------------------------------------

    @Test
    void theShippedArmorParses() {
        // Pointed at the resources tree, NOT at the deployed run/ folder -- saveResource never
        // overwrites, so a deployed copy can be arbitrarily stale and testing against it would
        // assert nothing about what a fresh server would load.
        File shipped = new File("src/main/resources/content/armor");
        assertTrue(shipped.isDirectory(),
                "expected the shipped armor directory at " + shipped.getAbsolutePath());

        ArmorRegistry registry = new ArmorLoader(LOG).loadAll(shipped);

        // ZERO IS A DEFECT, NOT A QUIET NO-OP. Every assertion below would pass vacuously against
        // an empty registry, which is exactly how a silently-empty scan reads as a green test.
        assertFalse(registry.all().isEmpty(),
                "the shipped armor directory must not load empty -- finding zero files is the "
                        + "defect CLAUDE.md records twice, not a no-op");

        // DISCOVERED, not enumerated: the tier list is read off the directory and then compared, so
        // adding a seventh tier file without a test change is caught rather than ignored.
        List<String> discovered = java.util.Arrays.stream(
                        java.util.Objects.requireNonNull(shipped.listFiles((d, n) -> n.endsWith(".yml"))))
                .map(f -> f.getName().substring(0, f.getName().length() - 4))
                .sorted()
                .toList();
        assertEquals(SHIPPED_TIERS, discovered,
                "the shipped tiers changed; update SHIPPED_TIERS and the roster count deliberately");

        assertEquals(discovered.size() * ArmorSlot.values().length, registry.size(),
                "every shipped tier must yield all four slots -- a short count means a tier was "
                        + "skipped, and its own warning will say which");
        // Mutation: point the loader at a nonexistent directory -> the non-empty assertion reddens.
    }

    @Test
    void everyShippedPieceIsWellFormedAndUniquelyIdentified() {
        ArmorRegistry registry = new ArmorLoader(LOG).loadAll(new File("src/main/resources/content/armor"));
        assertFalse(registry.all().isEmpty(), "must not walk an empty registry");

        for (ArmorDefinition piece : registry.all()) {
            assertFalse(piece.displayName().isBlank(), piece.id() + " has a blank display name");
            assertNotNull(piece.slot(), piece.id() + " has no slot");
            assertTrue(piece.defense() >= 0, piece.id() + " has a negative defense");
            // The id IS the material token -- the divergence this loader makes on purpose.
            assertEquals(piece.material(), piece.id(),
                    "an armor piece's id is its material token, not its filename");
        }
        // Mutation: derive the id from the tier filename instead of the material -> reddens, and
        // all four pieces of a tier would collide on one id.
    }

    @Test
    void theShippedDiamondSetSumsToAFullVanillaArmorBar() {
        // 3 + 8 + 6 + 3 == 20, the ladder DefenseTest independently pins as 3 -> 11 -> 17 -> 20.
        // Two files now have to agree about diamond, and this is what makes them.
        ArmorRegistry registry = new ArmorLoader(LOG).loadAll(new File("src/main/resources/content/armor"));
        double sum = 0;
        for (String id : List.of("diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots")) {
            sum += registry.find(id)
                    .orElseThrow(() -> new AssertionError(id + " did not load"))
                    .defense();
        }
        assertEquals(20.0, sum, 1e-9, "a full diamond set is a full 20-point vanilla armor bar");
        // Mutation: change any diamond defense value in the content file -> reddens.
    }

    @Test
    void theShippedTiersCarryTheAuthoredRarityBand() {
        ArmorRegistry registry = new ArmorLoader(LOG).loadAll(new File("src/main/resources/content/armor"));
        assertEquals(Rarity.UNCOMMON, registry.find("diamond_helmet").orElseThrow().rarity());
        assertEquals(Rarity.UNCOMMON, registry.find("netherite_boots").orElseThrow().rarity());
        assertEquals(Rarity.COMMON, registry.find("iron_chestplate").orElseThrow().rarity());
        assertEquals(Rarity.COMMON, registry.find("leather_helmet").orElseThrow().rarity());
        // Mutation: default the rarity to common regardless of the file -> the two diamond/netherite
        // assertions redden.
    }

    @Test
    void leatherKeepsVanillaIrregularNamesRatherThanADerivedOne() {
        // The reason display_name is authored per piece at all. A derived scheme would rename three
        // vanilla items, which the brief for this slice forbids.
        ArmorRegistry registry = new ArmorLoader(LOG).loadAll(new File("src/main/resources/content/armor"));
        assertEquals("Leather Cap", registry.find("leather_helmet").orElseThrow().displayName());
        assertEquals("Leather Tunic", registry.find("leather_chestplate").orElseThrow().displayName());
        assertEquals("Leather Pants", registry.find("leather_leggings").orElseThrow().displayName());
        assertEquals("Leather Boots", registry.find("leather_boots").orElseThrow().displayName());
        // Mutation: default display_name to the material token -> "leather_helmet" -> reddens.
    }

    // --- The one-file-four-pieces expansion ------------------------------------------------------

    @Test
    void aTierFileYieldsExactlyFourCorrectlySlottedPieces() throws IOException {
        ArmorRegistry registry = load("tin.yml", tier("common", fourSlots("tin")));

        assertEquals(4, registry.size(), "one tier file, four pieces");
        assertEquals(ArmorSlot.HEAD, registry.find("tin_helmet").orElseThrow().slot());
        assertEquals(ArmorSlot.CHEST, registry.find("tin_chestplate").orElseThrow().slot());
        assertEquals(ArmorSlot.LEGS, registry.find("tin_leggings").orElseThrow().slot());
        assertEquals(ArmorSlot.FEET, registry.find("tin_boots").orElseThrow().slot());
        // Mutation: emit three pieces instead of four (drop a slot from the walk) -> reddens.
        // Mutation: assign every piece the HEAD slot -> the other three assertions redden.
    }

    @Test
    void theTierRarityAndFlavourReachAllFourPieces() throws IOException {
        ArmorRegistry registry = load("tin.yml", tier("rare", fourSlots("tin")));
        for (ArmorDefinition piece : registry.all()) {
            assertEquals(Rarity.RARE, piece.rarity(), piece.id() + " missed the tier rarity");
            assertEquals(List.of("a line"), piece.flavor(), piece.id() + " missed the tier flavour");
        }
        assertEquals(4, registry.size(), "must not walk an empty or short registry");
        // Mutation: read rarity inside parsePiece from the piece section -> every piece falls back
        // to common -> reddens.
    }

    @Test
    void eachPieceKeepsItsOwnDefenseRatherThanSharingTheTiers() throws IOException {
        // The per-slot values differ (1/2/3/4 in the fixture) precisely so a bug that broadcast one
        // piece's defense to all four cannot hide behind equal numbers.
        ArmorRegistry registry = load("tin.yml", tier("common", fourSlots("tin")));
        assertEquals(1, registry.find("tin_helmet").orElseThrow().defense(), 1e-9);
        assertEquals(2, registry.find("tin_chestplate").orElseThrow().defense(), 1e-9);
        assertEquals(3, registry.find("tin_leggings").orElseThrow().defense(), 1e-9);
        assertEquals(4, registry.find("tin_boots").orElseThrow().defense(), 1e-9);
        // Mutation: hoist defense to the tier section -> all four read the same -> reddens.
    }

    // --- Failing soft, and loudly ----------------------------------------------------------------

    @Test
    void aTierMissingASlotIsRefusedWholeRatherThanLoadedPartially() throws IOException {
        String threeSlots = fourSlots("tin").replace(
                "  feet:\n    material: tin_boots\n    display_name: \"tin Boots\"\n    defense: 4\n", "");
        ArmorRegistry registry = load("tin.yml", tier("common", threeSlots));

        // ALL FOUR are gone, not three-of-four. A partially-loaded tier is the worst outcome: a
        // player finds three quarters of a set and no log line explains the gap.
        assertEquals(0, registry.size(), "a tier missing a slot loads no pieces at all");
        // Mutation: skip the bad piece and keep the good ones -> registry holds 3 -> reddens.
    }

    @Test
    void aTierWithNoPiecesSectionIsSkippedByName() throws IOException {
        ArmorRegistry registry = load("tin.yml", "rarity: common\n");
        assertEquals(0, registry.size());
        // Mutation: treat a missing pieces: section as an empty tier -> silently loads nothing and
        // logs nothing -> reddens.
    }

    @Test
    void aPieceWithNoMaterialIsRefusedBecauseTheMaterialIsAlsoTheId() throws IOException {
        String noMaterial = fourSlots("tin").replace("    material: tin_helmet\n", "");
        ArmorRegistry registry = load("tin.yml", tier("common", noMaterial));
        assertEquals(0, registry.size(), "material has no default -- it is the id");
        // Mutation: default material to the slot name -> two tiers would collide on "head"
        // -> reddens.
    }

    @Test
    void anUnknownRarityIsRefusedRatherThanDefaulted() throws IOException {
        ArmorRegistry registry = load("tin.yml", tier("mythic", fourSlots("tin")));
        assertEquals(0, registry.size());
        // Mutation: fall back to COMMON on an unknown rarity -> a typo silently reprices a tier
        // -> reddens.
    }

    @Test
    void oneMalformedTierDoesNotTakeItsNeighboursWithIt() throws IOException {
        Files.writeString(dir.resolve("bad.yml"), tier("mythic", fourSlots("bad")), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("tin.yml"), tier("common", fourSlots("tin")), StandardCharsets.UTF_8);
        ArmorRegistry registry = new ArmorLoader(LOG).loadAll(new File(dir.toString()));

        assertEquals(4, registry.size(), "the good tier still loaded");
        assertTrue(registry.find("tin_helmet").isPresent());
        assertTrue(registry.find("bad_helmet").isEmpty());
        // Mutation: let the RuntimeException escape loadAll -> the whole boot loses all armor
        // -> reddens.
    }

    @Test
    void aMissingDirectoryLoadsEmptyRatherThanThrowing() {
        ArmorRegistry registry = new ArmorLoader(LOG).loadAll(new File(dir.toString(), "not_here"));
        assertEquals(0, registry.size());
        // Mutation: dereference listFiles() without the null check -> a server with no content/armor
        // NPEs during onEnable -> reddens.
    }

    @Test
    void everyKeyButTheMaterialsHasAWorkingDefault() throws IOException {
        // display_name falls back to the material token, defense to 0, rarity to common, flavor to
        // empty. Only material is required. A tier authored with the bare minimum must still load.
        String bare = "pieces:\n"
                + "  head:\n    material: tin_helmet\n"
                + "  chest:\n    material: tin_chestplate\n"
                + "  legs:\n    material: tin_leggings\n"
                + "  feet:\n    material: tin_boots\n";
        ArmorRegistry registry = load("tin.yml", bare);

        assertEquals(4, registry.size());
        ArmorDefinition helmet = registry.find("tin_helmet").orElseThrow();
        assertEquals("tin_helmet", helmet.displayName(), "display_name defaults to the material");
        assertEquals(0, helmet.defense(), 1e-9, "defense defaults to 0, not to an invented number");
        assertEquals(Rarity.COMMON, helmet.rarity());
        assertEquals(List.of(), helmet.flavor());
        // Mutation: make display_name required -> a minimal tier stops loading -> reddens.
    }
}
