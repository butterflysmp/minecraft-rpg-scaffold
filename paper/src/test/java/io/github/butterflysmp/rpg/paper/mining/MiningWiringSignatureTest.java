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

    /** Lines from {@code from} up to the next line that closes a block at {@code indent}. */
    private static List<String> blockAt(List<String> lines, int from, String indent) {
        int end = from + 1;
        while (!lines.get(end).equals(indent + "}")) end++;
        return lines.subList(from, end + 1);
    }

    @Test
    void showReArmsTheRegrowthTaskWhenItFindsThePatchRegrowing() throws IOException {
        // The seat's D1: a rejoin's chunk load reaches show(), finds the patch regrowing, and must
        // schedule the task the quit dropped. Bounded by the if-block's own closing brace.
        List<String> svc = lines("mining/MiningService.java");
        int at = indexOf(svc, "if (patch.isEmpty()) {");
        List<String> branch = blockAt(svc, at, "        ");
        assertTrue(branch.stream().anyMatch(l -> l.strip().equals("ensureRegrowthTask(player, slot);")), branch.toString());
        assertTrue(branch.size() < 10, "the brace scan found the if's own close: " + branch.size());
        // Mutation: delete the ensureRegrowthTask call in that branch -> reddens.
    }

    @Test
    void aQuitForgetsTheSessionsRegrowthTasks() throws IOException {
        List<String> svc = lines("mining/MiningService.java");
        int at = indexOf(svc, "public void onQuit(Player player) {");
        List<String> body = blockAt(svc, at, "    ");
        assertTrue(body.stream().anyMatch(l -> l.strip().startsWith("book.forgetSession(player.getUniqueId());")), body.toString());
    }

    // --- S1-c: the DENIED hold on the action bar, and the logged place refusals --------------------

    @Test
    void inAHoldTheStatsBarReSendsTheHeldMessageAndDrawsNothingElse() throws IOException {
        // The seat's ruling 1: on each beat inside a hold, RE-SEND the held message, and return before
        // the stats line is drawn. Bounded by the case's own next two lines.
        List<String> bar = lines("hud/StatsBarSystem.java");
        int at = indexOf(bar, "case ActionBarHolds.Beat.Held<Component> held -> {");
        assertEquals("player.sendActionBar(held.hold().message());", bar.get(at + 1).strip(), "the re-send");
        assertEquals("return true;", bar.get(at + 2).strip(), "and nothing else this beat");
        // Mutations: delete the re-send, or the return (the stats line would overwrite it) -> reddens.
    }

    @Test
    void aQuitEndsTheHold() throws IOException {
        List<String> bar = lines("hud/StatsBarSystem.java");
        int at = indexOf(bar, "public void onQuit(UUID playerId) {");
        List<String> body = blockAt(bar, at, "    ");
        assertTrue(body.stream().anyMatch(l -> l.strip().startsWith("holds.clear(playerId);")), body.toString());
    }

    @Test
    void deniedTakesTheBarForTheNamedTicksAndSendsAtOnce() throws IOException {
        List<String> svc = lines("mining/MiningService.java");
        assertEquals("case ClickOutcome.Denied denied -> deny(player, tick);",
                svc.get(indexOf(svc, "case ClickOutcome.Denied denied ->")).strip());
        int at = indexOf(svc, "private void deny(Player player, long tick) {");
        List<String> body = blockAt(svc, at, "    ");
        String joined = String.join("\n", body);
        assertTrue(joined.contains("MiningNotices.DENIED_HOLD_TICKS"), "the ONE named value (ruling 3)");
        assertTrue(joined.contains("MiningNotices.DENIED_TEXT"), joined);
        assertTrue(body.stream().anyMatch(l -> l.strip().equals("player.sendActionBar(text);")), "sent at once");
        assertTrue(body.stream().noneMatch(l -> l.matches(".*[^A-Za-z_]30[^0-9].*")), "no literal 30 at the call site");
        // Mutation: replace MiningNotices.DENIED_HOLD_TICKS with 30 -> reddens twice over.
    }

    @Test
    void everyPlaceRefusalIsLoggedWithItsChatText() throws IOException {
        List<String> svc = lines("mining/MiningService.java");
        List<String> place = blockAt(svc, indexOf(svc, "public Component place(Player player, String oreId, String sizeId) {"), "    ");
        long refusals = place.stream().filter(l -> l.contains("return refuse(player, ")).count();
        assertEquals(8, refusals, "every refusal in place() goes through refuse(): the 8 counted when S1-c wired it");
        assertTrue(place.stream().noneMatch(l -> l.contains("return error(")), "no refusal bypasses the log");
        List<String> refuse = blockAt(svc, indexOf(svc, "private Component refuse(Player player, String text) {"), "    ");
        assertEquals("log.info(MiningTraceLine.placeRefused(player.getName(), text));", refuse.get(1).strip());
        // Mutation: refuse() without its log line -> reddens.
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
