package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A SERVER STOP DURING A SELFTEST RUN RESTORES (GATE-dragon-knell-4.md). On 2026-11-17 a /stop mid-run disabled the
 * plugin with no ABORTED and no RESTORED line, and the row's staging was saved into the world: MinecraftServer#stopServer
 * disables plugins BEFORE it saves players and chunks (read from the jar), and the player's quit then arrives after our
 * listener is gone. So onDisable must reach the run's restore itself, synchronously, before the profile flush (the restore
 * writes the profile's lifetime XP and cell). This path is pinned HERE ONLY; no gate row stops a run on purpose.
 */
class SelfTestDisableWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    @Test
    void onDisableReachesTheRunsRestoreBeforeTheProfileFlush() throws IOException {
        List<String> plugin = codeLines(MAIN.resolve("RpgPlugin.java"));
        int disable = indexOf(plugin, "public void onDisable() {", 0);
        int restore = indexOf(plugin, "if (selfTest != null) selfTest.onDisable();", disable);
        int flush = indexOf(plugin, "profiles.saveAllAndClear()", disable);
        assertTrue(disable > 0 && restore > disable && flush > restore,
                "onDisable, then the selftest's restore, then the profile flush: " + disable + " < " + restore + " < " + flush);
    }

    @Test
    void theServiceEndsAnActiveRunAtDisableAndTheRunRestoresInline() throws IOException {
        List<String> service = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestService.java")));
        int method = indexOf(service, "public void onDisable() {", 0);
        assertTrue(method > 0 && indexOf(service, "if (active != null) active.endAtDisable();", method) > method,
                "SelfTestService.onDisable ends the active run through endAtDisable");

        List<String> run = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestRun.java")));
        int at = indexOf(run, "void endAtDisable() {", 0);
        assertTrue(at > 0, "SelfTestRun.endAtDisable exists");
        int inline = indexOf(run, "new io.github.butterflysmp.rpg.paper.scheduler.InlineScheduler(", at);
        int end = indexOf(run, "end(\"ABORTED reason=server-stop\", inline, true, \"disable\");", at);
        assertTrue(inline > at && end > inline, "it builds an InlineScheduler, then ends with reason=server-stop and the "
                + "synchronous teleport: " + at + " < " + inline + " < " + end);

        List<String> snapshot = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestSnapshot.java")));
        assertTrue(snapshot.stream().anyMatch(l -> l.contains("if (now) p.teleport(location); else p.teleportAsync(location);")),
                "the restore teleports synchronously at disable (teleportAsync would complete after the save)");
    }

    private static int indexOf(List<String> code, String needle, int from) {
        for (int i = Math.max(0, from); i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    private static List<String> codeLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(l -> {
                    String t = l.trim();
                    return !(t.startsWith("//") || t.startsWith("*") || t.startsWith("/*"));
                })
                .toList();
    }
}
