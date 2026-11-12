package io.github.butterflysmp.rpg.paper.mining.spike;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pack, validated WITHOUT A BOOT, as far as a server-side test can. SPIKE CODE.
 *
 * <p><b>What this proves:</b> the files parse, every reference resolves inside the pack, the
 * textures decode at 16x16, the format is the pinned 84, and the zip is byte-stable. <b>What it
 * CANNOT prove</b> is that a 26.1.2 client accepts the pack and draws the models. That is client
 * behaviour, and GATE-mining-spike.md MS7 reads it.
 */
class MineSpikePackTest {

    private static final Path SOURCE = Path.of("src", "main", "resources", "resourcepack");

    @Test
    void filesNamesEveryFileInTheSourceDirectoryAndNothingElse() throws IOException {
        // Mutation it forces red: a file added to the directory and not to FILES, which the zip would drop.
        TreeSet<String> onDisk = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(SOURCE)) {
            walk.filter(Files::isRegularFile)
                    .forEach(p -> onDisk.add(SOURCE.relativize(p).toString().replace('\\', '/')));
        }
        assertTrue(onDisk.size() >= 7, "the walk must have found the pack source; found " + onDisk);
        assertEquals(onDisk, new TreeSet<>(MineSpikePack.FILES));
    }

    @Test
    void theMcmetaCarriesFormat84AsMinAndMax() throws IOException {
        // 84 is version.json's resource_major and SharedConstants.RESOURCE_PACK_FORMAT_MAJOR (PLAN F6).
        JsonObject pack = json("pack.mcmeta").getAsJsonObject("pack");
        assertEquals(84, pack.get("min_format").getAsInt());
        assertEquals(84, pack.get("max_format").getAsInt());
        assertTrue(pack.has("description"));
    }

    @Test
    void everyItemDefinitionResolvesToAModelAndEveryModelTextureToAPng() throws IOException {
        for (String id : List.of("mining_spot", "mining_patch")) {
            JsonObject model = json("assets/rpg/items/" + id + ".json").getAsJsonObject("model");
            assertEquals("minecraft:model", model.get("type").getAsString());
            String modelRef = model.get("model").getAsString();
            assertEquals("rpg:item/" + id, modelRef);
            String modelPath = "assets/rpg/models/" + modelRef.substring("rpg:".length()) + ".json";
            JsonObject textures = json(modelPath).getAsJsonObject("textures");
            for (String key : textures.keySet()) {
                String tex = textures.get(key).getAsString();
                assertTrue(tex.startsWith("rpg:"), key + " must be in our namespace: " + tex);
                Path png = SOURCE.resolve("assets/rpg/textures/" + tex.substring("rpg:".length()) + ".png");
                assertTrue(Files.isRegularFile(png), modelPath + " names " + tex + ", which is not in the pack");
            }
        }
    }

    @Test
    void theTexturesDecodeAtSixteenBySixteen() throws IOException {
        for (String id : List.of("mining_spot", "mining_patch")) {
            BufferedImage img = ImageIO.read(SOURCE.resolve("assets/rpg/textures/item/" + id + ".png").toFile());
            assertNotNull(img, id + ".png must decode as an image");
            assertEquals(16, img.getWidth());
            assertEquals(16, img.getHeight());
        }
    }

    @Test
    void theZipHoldsExactlyFilesInOrderAndIsByteStable() throws IOException {
        byte[] first = MineSpikePack.build(this::open);
        byte[] second = MineSpikePack.build(this::open);
        assertArrayEquals(first, second, "the same sources must give the same bytes, or the SHA-1 moves");
        assertEquals(MineSpikePack.sha1Hex(first), MineSpikePack.sha1Hex(second));
        assertEquals(40, MineSpikePack.sha1Hex(first).length());

        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(first))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                names.add(e.getName());
                assertArrayEquals(Files.readAllBytes(SOURCE.resolve(e.getName())), in.readAllBytes(),
                        e.getName() + " must be stored byte for byte");
            }
        }
        assertEquals(MineSpikePack.FILES, names);
    }

    @Test
    void aMissingFileFailsTheBuildLoudly() {
        // A discovery that finds nothing must fail loudly (verification.md): a null stream is refused.
        assertThrows(IllegalStateException.class, () -> MineSpikePack.build(name -> null));
    }

    private InputStream open(String name) {
        try {
            return Files.newInputStream(SOURCE.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonObject json(String name) throws IOException {
        return JsonParser.parseString(Files.readString(SOURCE.resolve(name), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
