package io.github.butterflysmp.rpg.paper.mining;

import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The project resource pack, built from the plugin jar and offered in DEV (PLAN-mining.md 5.8).
 * Re-implemented from the spike's {@code MineSpikePack}; nothing was cherry-picked.
 *
 * <ul>
 *   <li><b>Built</b> in memory from {@code resourcepack/} in the jar, in the fixed order of
 *       {@link #FILES}, with every entry's time fixed, so the SHA-1 is the same for the same bytes.
 *       F-M11: {@code .gitattributes} pins the JSON and mcmeta files to LF and the PNGs to binary,
 *       so the bytes are the same on every checkout.</li>
 *   <li><b>Served</b> by a {@code com.sun.net.httpserver.HttpServer} bound to 127.0.0.1 ONLY,
 *       started on the first offer and stopped on plugin disable (the standing rule). That assumes
 *       the client runs on the server's machine, which spike 1 confirmed for Ben's seat (F-M17).</li>
 *   <li><b>Offered</b> with {@code required(false)}: by {@code /rpg mining pack}, and at join when
 *       {@code mining.dev-pack-on-join} is true (default false). Ben: "it might be better to have it
 *       prompt or toggle for dev work". <b>REQUIRED on the real server is the target, NOT built.</b></li>
 * </ul>
 * Main thread only, apart from the HTTP server's own thread, which only writes the built bytes.
 */
public final class MiningPack {

    static final String ROOT = "resourcepack/";

    /** Every file in the pack, in zip order. {@code MiningPackTest} asserts this matches the tree. */
    static final List<String> FILES = List.of(
            "pack.mcmeta",
            "assets/rpg/items/mining/crack.json",
            "assets/rpg/items/mining/iron_patch_small.json",
            "assets/rpg/items/mining/iron_patch_medium.json",
            "assets/rpg/items/mining/iron_patch_large.json",
            "assets/rpg/items/mining/iron_patch_extra_large.json",
            "assets/rpg/items/mining/spot.json",
            "assets/rpg/models/item/mining/crack.json",
            "assets/rpg/models/item/mining/iron_patch_small.json",
            "assets/rpg/models/item/mining/iron_patch_medium.json",
            "assets/rpg/models/item/mining/iron_patch_large.json",
            "assets/rpg/models/item/mining/iron_patch_extra_large.json",
            "assets/rpg/models/item/mining/spot.json",
            "assets/rpg/textures/item/mining/crack.png",
            "assets/rpg/textures/item/mining/iron_patch_small.png",
            "assets/rpg/textures/item/mining/iron_patch_medium.png",
            "assets/rpg/textures/item/mining/iron_patch_large.png",
            "assets/rpg/textures/item/mining/iron_patch_extra_large.png",
            "assets/rpg/textures/item/mining/spot.png");

    static final String PATH = "/rpg-pack.zip";
    static final UUID PACK_ID = UUID.nameUUIDFromBytes("rpg-project-pack".getBytes(StandardCharsets.UTF_8));
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);

    private final Logger log;
    private HttpServer server;
    private byte[] zip;
    private String sha1;

    public MiningPack(Logger log) {
        this.log = log;
    }

    /**
     * Does the jar's pack ship an item model definition for this key, as
     * {@code assets/<ns>/items/<path>.json}? ContentValidator's in-pack check (the seat's ruling,
     * 2026-11-13: S1-b must add it).
     */
    public static boolean hasItemModel(ClassLoader loader, NamespacedKey key) {
        return loader.getResource(ROOT + itemModelPath(key)) != null;
    }

    static String itemModelPath(NamespacedKey key) {
        return "assets/" + key.getNamespace() + "/items/" + key.getKey() + ".json";
    }

    /** Offers the pack to one player, starting the server on first use. */
    public void offer(Player player) throws IOException {
        if (server == null) start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + PATH;
        log.info("MINING pack offer player=" + player.getName() + " url=" + url + " sha1=" + sha1
                + " bytes=" + zip.length);
        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), sha1))
                .required(false)
                .replace(false)
                .prompt(Component.text("RPG project pack (dev): ore patch art"))
                .build());
    }

    /** Stops the server. Called from onDisable. */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void start() throws IOException {
        ClassLoader loader = MiningPack.class.getClassLoader();
        zip = build(name -> loader.getResourceAsStream(ROOT + name));
        sha1 = sha1Hex(zip);
        byte[] body = zip;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(PATH, exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    /** Zips {@link #FILES} in order with fixed entry times. Throws if any file is missing. */
    static byte[] build(Function<String, InputStream> open) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (String name : FILES) {
                try (InputStream in = open.apply(name)) {
                    if (in == null) throw new IllegalStateException("resource pack file missing: " + ROOT + name);
                    ZipEntry entry = new ZipEntry(name);
                    entry.setTimeLocal(ENTRY_TIME);
                    out.putNextEntry(entry);
                    in.transferTo(out);
                    out.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    static String sha1Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
