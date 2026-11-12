package io.github.butterflysmp.rpg.paper.mining.spike;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spike's three wiring facts, read from source: the DEV gate, the packet registration, and the
 * disable that stops the pack server. SPIKE CODE. Same shape as {@code GearScoreWiringSignatureTest}.
 */
class MineSpikeWiringSignatureTest {

    private static final Path COMMAND = Path.of("src", "main", "java", "io", "github", "butterflysmp",
            "rpg", "paper", "command", "RpgCommand.java");
    private static final Path PLUGIN = Path.of("src", "main", "java", "io", "github", "butterflysmp",
            "rpg", "paper", "RpgPlugin.java");

    @Test
    void theMineSpikeCommandIsGatedOnDEV() throws IOException {
        List<String> lines = read(COMMAND, 2000);
        int literal = indexOf(lines, "Commands.literal(\"minespike\")", 0);
        assertTrue(literal > 0, "the /rpg minespike subcommand must exist");
        String next = lines.get(literal + 1).trim();
        assertTrue(next.startsWith(".requires(") && next.contains("Permissions.DEV"),
                "the DEV gate must be the very next line after the literal (the seat's ruling,"
                        + " 2026-11-12). Found: " + next);
    }

    @Test
    void thePacketWitnessIsRegistered() throws IOException {
        List<String> lines = read(PLUGIN, 500);
        assertTrue(indexOf(lines, "registerListener(new MineSpikeListener(scheduler, mineSpike));", 0) > 0,
                "the MINESPIKE packet listener must be registered with PacketEvents, or no PACKET line is ever logged");
    }

    @Test
    void disableStopsThePackServerBeforeAnythingElse() throws IOException {
        List<String> lines = read(PLUGIN, 500);
        int disable = indexOf(lines, "public void onDisable() {", 0);
        assertTrue(disable > 0, "onDisable must exist");
        int shutdown = indexOf(lines, "mineSpike.shutdown();", disable);
        assertTrue(shutdown == disable + 2,
                "onDisable must stop the spike (and its 127.0.0.1 pack server) first; found at line "
                        + (shutdown + 1) + ", onDisable at " + (disable + 1));
    }

    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast, "read " + lines.size() + " lines of " + path + ", expected more than "
                + atLeast + ": the scan must have read the real file");
        return lines;
    }

    /** The first line at or after {@code from} holding {@code needle} outside a comment, or -1. */
    private static int indexOf(List<String> lines, String needle, int from) {
        for (int i = Math.max(0, from); i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }
}
