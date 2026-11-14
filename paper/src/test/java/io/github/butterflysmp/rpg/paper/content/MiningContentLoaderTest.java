package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mining.OreDefinition;
import io.github.butterflysmp.rpg.core.mining.OreRegistry;
import io.github.butterflysmp.rpg.core.mining.PayoutRule;
import io.github.butterflysmp.rpg.core.mining.VeinSize;
import io.github.butterflysmp.rpg.core.mining.VeinSizeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mining schema, and whether the shipped files satisfy it (PLAN-mining.md 5.3).
 *
 * <p>The headline rows load the REAL {@code content/ores/} and {@code content/vein_sizes/} from the
 * resources tree, and assert a NON-EMPTY registry: finding zero files is a defect, not a quiet
 * no-op. Every refusal row checks the registry is empty AND that the warning names the cause, so a
 * row cannot pass because the file simply failed to parse for some other reason.
 */
class MiningContentLoaderTest {

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();
    private final Logger log = capturing();

    private Logger capturing() {
        Logger l = Logger.getAnonymousLogger();
        l.setUseParentHandlers(false);
        l.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { warnings.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return l;
    }

    private OreRegistry ores(String yaml) throws IOException {
        Files.writeString(dir.resolve("ore.yml"), yaml, StandardCharsets.UTF_8);
        return new MiningContentLoader(log).loadOres(new File(dir.toString()));
    }

    private VeinSizeRegistry sizes(String yaml) throws IOException {
        Files.writeString(dir.resolve("size.yml"), yaml, StandardCharsets.UTF_8);
        return new MiningContentLoader(log).loadVeinSizes(new File(dir.toString()));
    }

    private void assertRefused(OreRegistry registry, String needle) {
        assertEquals(0, registry.size(), "the ore must be refused; warnings: " + warnings);
        assertTrue(warnings.stream().anyMatch(w -> w.contains(needle)),
                "a warning must name '" + needle + "'; got " + warnings);
    }

    /** A valid ore, with one line replaced, so each refusal row differs from a passing file by one cause. */
    private static String ore(String replace, String with) {
        String valid = """
                ores:
                  iron:
                    display_name: "Iron"
                    payout:
                      item: raw_iron
                      per_spot:
                        min: 2
                        max: 4
                      completion_bonus:
                        small: 5
                    regrowth_ticks: 1200
                    sizes:
                      small: 50
                    models:
                      patch:
                        small: rpg:mining/iron_patch_small
                      spot: rpg:mining/spot
                      crack: rpg:mining/crack
                    sounds:
                      miss: minecraft:block.stone.hit
                      break: minecraft:block.stone.break
                      denied: minecraft:entity.villager.no
                    particles:
                      break: minecraft:poof
                """;
        if (replace == null) return valid;
        if (!valid.contains(replace)) throw new AssertionError("fixture has no '" + replace + "'");
        return valid.replace(replace, with);
    }

    // --- The shipped content --------------------------------------------------------------------

    @Test
    void theShippedVeinSizesAreTheFourOfTheDesignDoc() {
        File shipped = new File("src/main/resources/content/vein_sizes");
        assertTrue(shipped.isDirectory(), shipped.getAbsolutePath());
        VeinSizeRegistry registry = new MiningContentLoader(log).loadVeinSizes(shipped);
        assertEquals(4, registry.size(), "S2 ships four sizes; warnings: " + warnings);
        assertEquals(new VeinSize("small", 3, 2, 2, 3), registry.find("small").orElseThrow(), "RULED, Ben 2026-11-13");
        // Ben's design doc, relayed by the seat 2026-11-14.
        assertEquals(new VeinSize("medium", 5, 3, 4, 5), registry.find("medium").orElseThrow());
        assertEquals(new VeinSize("large", 7, 3, 6, 7), registry.find("large").orElseThrow());
        assertEquals(new VeinSize("extra_large", 7, 4, 8, 9), registry.find("extra_large").orElseThrow());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void theShippedIronOreCarriesBensNumbers() {
        File shipped = new File("src/main/resources/content/ores");
        assertTrue(shipped.isDirectory(), shipped.getAbsolutePath());
        OreRegistry registry = new MiningContentLoader(log).loadOres(shipped);
        assertEquals(1, registry.size(), "slice 1 ships iron only; warnings: " + warnings);
        OreDefinition iron = registry.find("iron").orElseThrow();
        assertEquals("raw_iron", iron.payoutItem(), "Ben: \"raw iron\"");
        assertEquals(new PayoutRule(2, 4, 5), iron.payoutRule("small"),
                "Ben: \"2-4 per spot with a 5 completion bonus\"");
        assertEquals(new PayoutRule(2, 4, 6), iron.payoutRule("medium"), "Ben 2026-11-14: \"This is fine for now\"");
        assertEquals(new PayoutRule(2, 4, 8), iron.payoutRule("large"));
        assertEquals(new PayoutRule(2, 4, 10), iron.payoutRule("extra_large"));
        assertEquals(1200, iron.regrowthTicks(), "Ben: \"Regrowth can be 1 minute across the board\"");
        assertEquals(List.of("small", "medium", "large", "extra_large"), iron.sizes(), "authored order");
        assertEquals(List.of(50, 30, 15, 5), List.copyOf(iron.sizeWeights().values()), "PROVISIONAL, design doc");
        assertEquals("minecraft:block.stone.hit", iron.missSound());
        assertEquals("minecraft:block.stone.break", iron.breakSound());
        assertEquals("minecraft:entity.villager.no", iron.deniedSound(), "PLACEHOLDER, the seat's ruling 8");
        for (String size : iron.sizes()) {
            assertEquals("rpg:mining/iron_patch_" + size, iron.patchModel(size));
        }
        assertEquals("minecraft:poof", iron.breakParticle());
        assertTrue(warnings.isEmpty(), "the shipped file loads with no warning: " + warnings);
    }

    @Test
    void theFixtureItselfLoads() throws IOException {
        // The positive control for every refusal row below: the unmodified fixture loads.
        assertEquals(1, ores(ore(null, null)).size(), warnings.toString());
    }

    // --- Refusals -------------------------------------------------------------------------------

    @Test
    void anOreWithNoPayoutIsRefused() throws IOException {
        String yaml = ore(null, null).replaceAll("(?s)    payout:.*?    regrowth_ticks", "    regrowth_ticks");
        assertRefused(ores(yaml), "'payout:'");
    }

    @Test
    void anOreWithNoPerSpotIsRefused() throws IOException {
        assertRefused(ores(ore("      per_spot:\n        min: 2\n        max: 4\n", "")), "'per_spot:'");
    }

    @Test
    void aScalarPerSpotIsRefusedNotReadAsZero() throws IOException {
        assertRefused(ores(ore("      per_spot:\n        min: 2\n        max: 4\n", "      per_spot: 3\n")),
                "'per_spot:'");
    }

    @Test
    void aMissingPerSpotMaxIsRefusedNotDefaultedToZero() throws IOException {
        assertRefused(ores(ore("        max: 4\n", "")), "'max:'");
        // Mutation: read it with getInt(key), which defaults to 0 -> the ore is still refused, but by
        // PayoutRule ("per_spot.min 2 is above per_spot.max 0"), whose message has no "'max:'" ->
        // reddens. With min 0 the default would not be refused at all, which is why requireInt exists.
    }

    @Test
    void aSizeWithNoBonusIsRefused() throws IOException {
        assertRefused(ores(ore("      small: 50\n", "      small: 50\n      medium: 30\n")), "medium");
    }

    @Test
    void aSizeWeightMustBeAWholeNumberAtLeastOne() throws IOException {
        assertRefused(ores(ore("      small: 50\n", "      small: 0\n")), "weight");
        warnings.clear();
        assertRefused(ores(ore("      small: 50\n", "      small: heavy\n")), "'small:'");
    }

    @Test
    void aSizeWithNoPatchModelIsRefused() throws IOException {
        // large is weighted and has a bonus, but no models.patch entry.
        assertRefused(ores(ore("      small: 50\n", "      small: 50\n      large: 15\n")
                .replace("        small: 5\n", "        small: 5\n        large: 8\n")), "models.patch");
    }

    @Test
    void slice1sListSizesAndSinglePatchModelAreRefusedByName() throws IOException {
        assertRefused(ores(ore("    sizes:\n      small: 50\n", "    sizes: [small]\n")), "map of size id to roll weight");
        warnings.clear();
        assertRefused(ores(ore("      patch:\n        small: rpg:mining/iron_patch_small\n",
                "      patch: rpg:mining/iron_patch\n")), "single 'models.patch:'");
    }

    @Test
    void aMissingDeniedSoundIsRefused() throws IOException {
        assertRefused(ores(ore("      denied: minecraft:entity.villager.no\n", "")), "sounds.denied");
    }

    @Test
    void aBonusForAnUnlistedSizeIsRefused() throws IOException {
        assertRefused(ores(ore("        small: 5\n", "        small: 5\n        large: 9\n")), "large");
    }

    @Test
    void aMissingRegrowthIsRefusedNotDefaulted() throws IOException {
        assertRefused(ores(ore("    regrowth_ticks: 1200\n", "")), "'regrowth_ticks:'");
    }

    @Test
    void aMissingSoundIsRefused() throws IOException {
        assertRefused(ores(ore("      miss: minecraft:block.stone.hit\n", "")), "sounds.miss");
    }

    @Test
    void aMissingParticlesSectionIsRefused() throws IOException {
        assertRefused(ores(ore("    particles:\n      break: minecraft:poof\n", "")), "'particles:'");
    }

    @Test
    void aScalarSizesIsRefused() throws IOException {
        assertRefused(ores(ore("    sizes:\n      small: 50\n", "    sizes: small\n")), "a list or a scalar");
    }

    @Test
    void aVeinSizeMissingItsWidthIsRefused() throws IOException {
        VeinSizeRegistry registry = sizes("""
                vein_sizes:
                  small:
                    height: 2
                    spots:
                      min: 2
                      max: 3
                """);
        assertEquals(0, registry.size());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("'width:'")), warnings.toString());
    }

    @Test
    void anEmptyFileIsNamed() throws IOException {
        assertEquals(0, ores("# nothing\n").size());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("no 'ores:' entries")), warnings.toString());
    }
}
