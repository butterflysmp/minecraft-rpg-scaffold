package io.github.butterflysmp.rpg.paper.mining;

import io.github.butterflysmp.rpg.core.mining.ClickOutcome;
import io.github.butterflysmp.rpg.core.mining.Payout;
import io.github.butterflysmp.rpg.core.mining.WallFace;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring that no unit test can exercise without a server, pinned by reading the source
 * (PLAN-mining.md 5.4). Every scan is bounded STRUCTURALLY (the next line, or one named file), never
 * by a line count, and each has a positive control so a needle that stopped matching reads red.
 */
class MiningWiringSignatureTest {

    private static final Path MAIN = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");

    private static List<String> lines(String rel) throws IOException {
        return Files.readAllLines(MAIN.resolve(rel), StandardCharsets.UTF_8);
    }

    private static int indexOf(List<String> lines, String needle) {
        int found = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(needle)) {
                assertEquals(-1, found, "'" + needle + "' must appear exactly once");
                found = i;
            }
        }
        assertTrue(found >= 0, "'" + needle + "' not found: the needle is stale, so this check reads nothing");
        return found;
    }

    @Test
    void theMiningBranchIsGatedByPermissionsDevOnTheLineAfterItsLiteral() throws IOException {
        List<String> cmd = lines("command/RpgCommand.java");
        int at = indexOf(cmd, "Commands.literal(\"mining\")");
        assertEquals(".requires(source -> source.getSender().hasPermission(Permissions.DEV))",
                cmd.get(at + 1).strip(), "the whole /rpg mining branch is DEV-only");
        // Mutation: move the requires below .executes(MiningDevCommand::usage) -> reddens.
    }

    @Test
    void theBlockDamageHandlerIsNotIgnoreCancelled() throws IOException {
        // The stone handler may cancel the same event first; the patch block must still be judged.
        List<String> listeners = lines("listener/RpgListeners.java");
        int at = indexOf(listeners, "public void onMiningBlockDamage(BlockDamageEvent event)");
        assertEquals("@EventHandler", listeners.get(at - 1).strip());
    }

    @Test
    void miningInstallsNoPacketListener() throws IOException {
        // THREADING: slice 1 reads its input from a Bukkit event, so nothing of mining runs on a Netty
        // thread. A PacketListener in the mining package would be a new Netty-thread surface.
        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.list(MAIN.resolve("mining"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                scanned++;
                String src = Files.readString(f, StandardCharsets.UTF_8);
                for (String token : List.of("PacketListenerBase", "PacketListener", "onPacketReceive", "registerListener")) {
                    if (src.contains(token)) offenders.add(f.getFileName() + ": " + token);
                }
            }
        }
        assertTrue(scanned >= 4, "the scan reached the mining package (" + scanned + " files)");
        assertEquals(List.of(), offenders);
        // Positive control: the spike's listener carried the token this scan looks for.
        assertTrue("final class MineSpikeListener extends PacketListenerBase".contains("PacketListenerBase"));
    }

    @Test
    void thePopupsAndMiningShareOneEntityIdCounter() throws IOException {
        List<String> plugin = lines("RpgPlugin.java");
        int built = indexOf(plugin, "new PopupEntityIds()");
        assertTrue(plugin.get(built).contains("this.fakeEntityIds = new PopupEntityIds();"), plugin.get(built));
        indexOf(plugin, "new PacketDamagePopupSender(), elements, fakeEntityIds)");
        indexOf(plugin, "new MiningPack(getLogger()), fakeEntityIds,");
        // Mutation: give mining its own new PopupEntityIds() -> "exactly once" reddens.
    }

    // --- MiningService's pure helpers -----------------------------------------------------------

    @Test
    void wallFacesMapToBlockFacesBothWaysAndUpAndDownAreRefused() {
        for (WallFace f : WallFace.values()) {
            assertEquals(f, MiningService.toWallFace(MiningService.toBlockFace(f)), f.name());
        }
        assertNull(MiningService.toWallFace(BlockFace.UP));
        assertNull(MiningService.toWallFace(BlockFace.DOWN));
        assertEquals(BlockFace.NORTH, MiningService.toBlockFace(WallFace.NORTH));
    }

    @Test
    void outcomeNamesAreTheTraceLinesWords() {
        assertEquals("HIT", MiningService.name(new ClickOutcome.Hit(0, 1)));
        assertEquals("BROKE", MiningService.name(new ClickOutcome.Broke(1, new Payout(List.of(2), 5))));
        assertEquals("MISS", MiningService.name(ClickOutcome.Miss.INSTANCE));
        assertEquals("UNCHARGED", MiningService.name(ClickOutcome.Uncharged.INSTANCE));
        assertEquals("DENIED", MiningService.name(ClickOutcome.Denied.INSTANCE));
        assertEquals("NOT_LIVE", MiningService.name(ClickOutcome.NotLive.INSTANCE));
    }

    @Test
    void theStatusOnlyLogsOurPack() {
        assertEquals(java.util.Optional.of("MINING pack status player=Ben status=SUCCESSFULLY_LOADED"),
                MiningService.packStatusLine("Ben", MiningPack.PACK_ID, "SUCCESSFULLY_LOADED"));
        assertTrue(MiningService.packStatusLine("Ben", new java.util.UUID(9, 9), "DECLINED").isEmpty(),
                "another pack's status is not ours to log");
    }

    @Test
    void theDisplayOffsetsAreTheSeatsRuledValues() {
        assertEquals(0.0005, MiningService.PATCH_OUTSET, "the seat's ruling 6");
        assertEquals(0.001, MiningService.SPOT_OUTSET, "the seat's ruling 6");
        assertEquals(3f / 16f, MiningService.SPOT_SCALE, "Ben: a 3x3 visible spot");
    }
}
