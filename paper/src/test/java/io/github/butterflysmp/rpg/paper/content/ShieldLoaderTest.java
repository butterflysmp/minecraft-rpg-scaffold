package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shield schema, and whether the shipped content file actually satisfies it.
 *
 * The headline is {@link #theShippedRoundshieldParses} -- it loads the REAL
 * {@code content/shields/roundshield.yml} out of the resources tree rather than a fixture, so a
 * typo in the shipped file reddens here instead of arriving as "0 shields" in a boot log. Content
 * and schema drifting apart is not hypothetical on this repo: commit 117168e records a boot that
 * loaded 3 weapons instead of 5, both swords silently skipped for a missing key.
 *
 * That test also carries the discovers-nothing rule CLAUDE.md states: it asserts a NON-EMPTY
 * registry, because finding zero files is a defect and not a quiet no-op.
 *
 * Each test names the mutation it forces red.
 */
class ShieldLoaderTest {

    private static final Logger LOG = Logger.getLogger(ShieldLoaderTest.class.getName());

    @TempDir
    Path dir;

    private ShieldRegistry load(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
        return new ShieldLoader(LOG).loadAll(new File(dir.toString()));
    }

    // --- The shipped content --------------------------------------------------------------------

    @Test
    void theShippedRoundshieldParses() {
        // THE headline. The schema is only correct relative to the files it has to read, and this
        // is the one that ships. Pointed at the resources tree, NOT at the deployed run/ folder --
        // saveResource never overwrites, so the deployed copy can be arbitrarily stale and testing
        // against it would assert nothing about what a fresh server would load.
        File shipped = new File("src/main/resources/content/shields");
        assertTrue(shipped.isDirectory(),
                "expected the shipped shields directory at " + shipped.getAbsolutePath());

        ShieldRegistry registry = new ShieldLoader(LOG).loadAll(shipped);

        // ZERO IS A DEFECT, NOT A QUIET NO-OP. A loader that discovers nothing reads exactly like
        // one that worked, and an assertion on roundshield's fields alone would pass vacuously if
        // the directory scan came back empty.
        assertFalse(registry.all().isEmpty(),
                "the shipped shields directory must not load empty -- finding zero files is the "
                        + "defect CLAUDE.md records twice, not a no-op");

        ShieldDefinition roundshield = registry.find("roundshield").orElseThrow(
                () -> new AssertionError("roundshield.yml did not load; registry holds "
                        + registry.all().stream().map(ShieldDefinition::id).toList()));

        assertEquals("Roundshield", roundshield.displayName());
        assertEquals(Rarity.COMMON, roundshield.rarity());
        assertEquals("shield", roundshield.material(), "must be a material vanilla actually blocks with");
        assertEquals(0.5, roundshield.blockDr(), 1e-9, "the common shield stops half");
        assertEquals(2, roundshield.flavor().size(), "and its flavour is a LIST, not a scalar");
        // Mutation: rename any key in roundshield.yml (block_dr -> blockDr, say) -> the shield
        // loads with a defaulted value and this reddens, where a boot would just print "Block: 0%"
        // -> reddens.
    }

    // --- The schema -----------------------------------------------------------------------------

    @Test
    void theIdIsTheFilenameAndNotAnythingInsideTheFile() throws IOException {
        // Every content type in this project keys on the filename. An `id:` key inside the file is
        // ignored, exactly as WeaponLoader ignores it -- two sources of truth for an id is how you
        // get a file that cannot be given by the name it appears to have.
        ShieldRegistry registry = load("buckler.yml", "id: something_else\ndisplay_name: Buckler\n");
        assertTrue(registry.find("buckler").isPresent(), "the filename wins");
        assertTrue(registry.find("something_else").isEmpty(), "the id: key is ignored");
        // Mutation: read the id from s.getString("id") -> /rpg give buckler stops working while
        // the tooltip still says Buckler -> reddens.
    }

    @Test
    void everyKeyButTheFilenameHasAWorkingDefault() throws IOException {
        // A one-line shield file must load. The defaults are the schema's promise that authoring a
        // shield is cheap; only the block fraction is worth stating, and even that defaults to 0.
        ShieldRegistry registry = load("plain.yml", "display_name: Plain\n");
        ShieldDefinition plain = registry.find("plain").orElseThrow();
        assertEquals(Rarity.COMMON, plain.rarity());
        assertEquals(ShieldDefinition.DEFAULT_MATERIAL, plain.material());
        assertEquals(0.0, plain.blockDr(), 1e-9, "no block_dr means no block, never an invented one");
        assertTrue(plain.flavor().isEmpty());
        // Mutation: default block_dr to anything but 0 -> a shield silently acquires a mechanic its
        // file never asked for -> reddens.
    }

    @Test
    void anOutOfRangeBlockDrSkipsTheFileRatherThanLoadingAHealingShield() throws IOException {
        // The refusal lives in ShieldDefinition's constructor; this is the half that matters
        // operationally -- that the loader catches it, names the file and keeps booting, instead of
        // letting a RuntimeException take the whole plugin down or letting the shield through.
        assertTrue(load("cursed.yml", "display_name: Cursed\nblock_dr: 2.0\n").all().isEmpty(),
                "a block_dr above 1 would make damage NEGATIVE and heal the holder");
        assertTrue(load("cursed.yml", "display_name: Cursed\nblock_dr: -1.0\n").all().isEmpty(),
                "and below 0 would DOUBLE the hit");
        // Mutation: catch Throwable and register a fallback definition instead of skipping -> a
        // malformed shield ships -> reddens.
    }

    @Test
    void anUnknownRarityIsANamedSkipRatherThanASilentDowngradeToCommon() throws IOException {
        assertTrue(load("odd.yml", "display_name: Odd\nrarity: mythic\n").all().isEmpty());
        // Mutation: fall back to Rarity.COMMON on an unknown name -> a typo'd tier ships as common
        // and nothing says so -> reddens.
    }

    @Test
    void aMissingDirectoryIsAnEmptyRegistryRatherThanACrash() {
        // A server that has never had a shields folder must still boot. The EMPTY result is then
        // the plugin's zero-check to shout about, which is where that belongs -- the loader's job
        // is to not throw.
        ShieldRegistry registry = new ShieldLoader(LOG).loadAll(new File(dir.toFile(), "nope"));
        assertEquals(0, registry.size());
        // Mutation: drop the `files == null` guard -> NullPointerException on a fresh server, at
        // boot, before anything else loads -> reddens.
    }

    @Test
    void oneMalformedFileDoesNotTakeItsNeighboursWithIt() throws IOException {
        // Fails SOFT and per file. A shields directory is going to grow; one bad file costing every
        // other shield would turn a typo into an outage.
        Files.writeString(dir.resolve("good.yml"), "display_name: Good\nblock_dr: 0.5\n",
                StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("bad.yml"), "display_name: Bad\nblock_dr: 5.0\n",
                StandardCharsets.UTF_8);
        ShieldRegistry registry = new ShieldLoader(LOG).loadAll(new File(dir.toString()));
        assertEquals(1, registry.size());
        assertTrue(registry.find("good").isPresent());
        // Mutation: move the try/catch outside the for loop -> the first bad file ends the scan and
        // every shield after it alphabetically vanishes -> reddens.
    }
}
