package io.github.butterflysmp.rpg.paper.mining;

import io.github.butterflysmp.rpg.core.mining.OreDefinition;
import io.github.butterflysmp.rpg.paper.content.MiningContentLoader;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The project resource pack, validated without a boot (PLAN-mining.md 5.8). Re-implements the
 * spike's {@code MineSpikePackTest} checks for slice 1's three models, and adds the two the seat
 * ruled S1-b must have: every ore model id is in the pack (the same lookup ContentValidator uses at
 * boot), and the pack's text files are LF so the hash does not depend on the checkout (F-M11).
 *
 * <p>It CANNOT validate what the client does with the pack: whether it accepts it, and how the
 * models look on screen. Those are gate rows (M-PACK, M-LOOK).
 */
class MiningPackTest {

    private static final Path ROOT = Path.of("src/main/resources/resourcepack");

    private static String read(String rel) {
        try {
            return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> treeFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(ROOT)) {
            return walk.filter(Files::isRegularFile)
                    .map(p -> ROOT.relativize(p).toString().replace('\\', '/'))
                    .sorted().toList();
        }
    }

    @Test
    void theFileListIsExactlyTheTree() throws IOException {
        assertEquals(new TreeSet<>(treeFiles()), new TreeSet<>(MiningPack.FILES),
                "MiningPack.FILES must name every file under resourcepack/ and nothing else");
        assertEquals(10, MiningPack.FILES.size(), "pack.mcmeta + 3 item definitions + 3 models + 3 textures");
    }

    @Test
    void packMcmetaCarriesFormat84() {
        String mcmeta = read("pack.mcmeta");
        assertTrue(mcmeta.contains("\"min_format\": 84"), mcmeta);
        assertTrue(mcmeta.contains("\"max_format\": 84"), mcmeta);
    }

    @Test
    void everyItemDefinitionResolvesToAModelAndEveryModelToATexture() throws IOException {
        Pattern model = Pattern.compile("\"model\":\\s*\"rpg:([^\"]+)\"");
        Pattern texture = Pattern.compile("\"(?:face|particle)\":\\s*\"rpg:([^\"]+)\"");
        int items = 0, textures = 0;
        for (String f : MiningPack.FILES) {
            if (!f.startsWith("assets/rpg/items/")) continue;
            items++;
            Matcher m = model.matcher(read(f));
            assertTrue(m.find(), f + " names no rpg model");
            String modelFile = "assets/rpg/models/" + m.group(1) + ".json";
            assertTrue(Files.isRegularFile(ROOT.resolve(modelFile)), f + " -> missing " + modelFile);
            Matcher t = texture.matcher(read(modelFile));
            while (t.find()) {
                textures++;
                String png = "assets/rpg/textures/" + t.group(1) + ".png";
                assertTrue(Files.isRegularFile(ROOT.resolve(png)), modelFile + " -> missing " + png);
            }
        }
        assertEquals(3, items);
        assertEquals(6, textures, "a face and a particle texture per model");
    }

    @Test
    void theTexturesDecodeAndThePatchIsDrawnAtTheSmallVeinsAspect() throws IOException {
        BufferedImage patch = ImageIO.read(ROOT.resolve("assets/rpg/textures/item/mining/iron_patch.png").toFile());
        assertNotNull(patch);
        assertEquals(48, patch.getWidth(), "3 blocks x 16");
        assertEquals(32, patch.getHeight(), "2 blocks x 16");
        for (String id : List.of("spot", "crack")) {
            BufferedImage img = ImageIO.read(ROOT.resolve("assets/rpg/textures/item/mining/" + id + ".png").toFile());
            assertNotNull(img, id);
            assertEquals(16, img.getWidth(), id);
            assertEquals(16, img.getHeight(), id);
        }
    }

    @Test
    void theZipIsTheSameBytesOnTwoBuilds() {
        ClassLoader loader = MiningPackTest.class.getClassLoader();
        byte[] a = MiningPack.build(name -> loader.getResourceAsStream(MiningPack.ROOT + name));
        byte[] b = MiningPack.build(name -> loader.getResourceAsStream(MiningPack.ROOT + name));
        assertArrayEquals(a, b);
        assertEquals(MiningPack.sha1Hex(a), MiningPack.sha1Hex(b));
        assertEquals(40, MiningPack.sha1Hex(a).length());
    }

    @Test
    void theTextFilesAreLfSoTheHashDoesNotDependOnTheCheckout() throws IOException {
        // F-M11: .gitattributes pins resourcepack/**/*.json and *.mcmeta to eol=lf. Maven copies the
        // working tree into the jar, so a CR here would change the zip and its SHA-1.
        List<String> withCr = new ArrayList<>();
        int checked = 0;
        for (String f : MiningPack.FILES) {
            if (!(f.endsWith(".json") || f.endsWith(".mcmeta"))) continue;
            checked++;
            for (byte by : Files.readAllBytes(ROOT.resolve(f))) {
                if (by == '\r') { withCr.add(f); break; }
            }
        }
        assertEquals(7, checked, "the scan reached every text file");
        assertEquals(List.of(), withCr);
    }

    @Test
    void everyShippedOreModelIsInThePackByTheValidatorsOwnLookup() {
        List<OreDefinition> ores = new ArrayList<>(new MiningContentLoader(Logger.getAnonymousLogger())
                .loadOres(new File("src/main/resources/content/ores")).all());
        assertFalse(ores.isEmpty(), "the shipped ores must load");
        ClassLoader loader = MiningPackTest.class.getClassLoader();
        for (OreDefinition ore : ores) {
            for (String id : List.of(ore.patchModel(), ore.spotModel(), ore.crackModel())) {
                NamespacedKey key = NamespacedKey.fromString(id);
                assertNotNull(key, id);
                assertTrue(MiningPack.hasItemModel(loader, key), ore.id() + ": " + id + " is not in the pack");
            }
        }
        // The positive control: a well-formed id the pack does NOT ship is refused by the same lookup.
        assertFalse(MiningPack.hasItemModel(loader, NamespacedKey.fromString("rpg:mining/gold_patch")));
    }
}
