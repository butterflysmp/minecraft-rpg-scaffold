package io.github.butterflysmp.rpg.paper.mining.spike;

import com.sun.net.httpserver.HttpServer;
import io.github.butterflysmp.rpg.core.mining.spike.MineSpikeLine;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
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
 * The spike's resource pack: zipped from the plugin's own resources, served on 127.0.0.1, offered
 * to one player. SPIKE CODE (PLAN-mining.md §5, THE RESOURCE PACK).
 *
 * <p><b>The server never serves a pack</b> (F6). The client downloads it over HTTP from the URL it
 * is sent. So the dev offer needs an HTTP server, and this is the smallest one:
 * {@code com.sun.net.httpserver}, bound to the LOOPBACK address only, on an ephemeral port, and
 * stopped on disable. Its handler only writes bytes that are already built, and touches no Bukkit.
 *
 * <p><b>The zip is deterministic</b>: fixed entry order ({@link #FILES}) and fixed timestamps, so the
 * SHA-1 is stable across builds of the same sources. {@code MineSpikePackTest} pins that, and also
 * pins that {@link #FILES} names every file under the source directory and nothing else.
 */
final class MineSpikePack {

    /** The pack source, as a classpath prefix. The source directory is paper/src/main/resources/resourcepack/. */
    static final String ROOT = "resourcepack/";

    /** Every file in the pack, in zip order. */
    static final List<String> FILES = List.of(
            "pack.mcmeta",
            "assets/rpg/items/mining_patch.json",
            "assets/rpg/items/mining_spot.json",
            "assets/rpg/models/item/mining_patch.json",
            "assets/rpg/models/item/mining_spot.json",
            "assets/rpg/textures/item/mining_patch.png",
            "assets/rpg/textures/item/mining_spot.png");

    static final String PATH = "/mining-spike.zip";
    /** One fixed id, so a re-offer replaces the pack on the client rather than stacking a second. */
    static final UUID PACK_ID = UUID.nameUUIDFromBytes("rpg-mining-spike".getBytes(StandardCharsets.UTF_8));

    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);

    private final Logger log;
    private HttpServer server;
    private byte[] zip;
    private String sha1;

    MineSpikePack(Logger log) {
        this.log = log;
    }

    /** Offer the pack to {@code player}, starting the server on first use. Main thread. */
    void offer(Player player) throws IOException {
        if (server == null) start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + PATH;
        log.info(MineSpikeLine.pack(url, sha1, zip.length));
        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), sha1))
                .required(false)
                .replace(false)
                .prompt(Component.text("Mining spike pack (dev only)"))
                .build());
    }

    void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void start() throws IOException {
        ClassLoader loader = MineSpikePack.class.getClassLoader();
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

    /** Build the zip from {@link #FILES}, opened through {@code open}. Fails loudly on a missing file. */
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
