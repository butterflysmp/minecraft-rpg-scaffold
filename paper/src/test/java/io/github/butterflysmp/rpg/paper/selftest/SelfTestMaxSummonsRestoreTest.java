package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C1 (the seat's ruling, 2026-11-24): {@code /rpg maxsummons} writes his REAL profile base, which survives a restart, so the
 * selftest's SNAPSHOT captures the stored base (null included) at the run's start and restores it at every end, as it does
 * his lifetime XP. Every end (END, abort, stop, quit, disable) reaches the one {@code restore} through
 * {@code SelfTestRun.end} ({@code SelfTestDisableWiringTest} pins the disable path); this pins the field inside it.
 * Read from the source because the snapshot needs a live Player.
 */
class SelfTestMaxSummonsRestoreTest {

    private static final Path SNAPSHOT = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
            "selftest", "SelfTestSnapshot.java");

    @Test
    void theSnapshotCapturesTheStoredBaseNullIncludedAndRestoresItInsideRestore() throws IOException {
        List<String> code = codeLines();
        int ctor = indexOf(code, "private SelfTestSnapshot(Player p,", 0);
        int take = indexOf(code, "static SelfTestSnapshot take(", ctor);
        int captured = indexOf(code, "this.maxSummonsBase = SelfTestSummonsBase.capture(profile);", ctor);
        assertTrue(ctor > 0 && captured > ctor && captured < take,
                "captured in the constructor, the BOXED value (null = never set), not the read: " + ctor + " " + captured);

        int restore = indexOf(code, "String restore(Player p, ProfileService profiles,", 0);
        int write = indexOf(code, "boolean summons = maxSummonsBase.writes() && profiles.setMaxSummonsBase(id, maxSummonsBase.base());",
                restore);
        int ret = indexOf(code, "return \"RESTORED xp=", restore);
        assertTrue(restore > 0 && write > restore && ret > write, "written back inside restore, before its line: "
                + restore + " < " + write + " < " + ret);
        assertTrue(indexOf(code, "+ \" max_summons_base=\" + (summons ? String.valueOf(maxSummonsBase.base()) : \"NOT-RESTORED\")",
                ret) > ret, "and the RESTORED line names it, so a log shows it came back");
        assertEquals(1, code.stream().filter(l -> l.contains("setMaxSummonsBase(")).count(),
                "one write, the restore's");
    }

    private static int indexOf(List<String> code, String needle, int from) {
        for (int i = Math.max(0, from); i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    private static List<String> codeLines() throws IOException {
        return Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8).stream()
                .filter(l -> {
                    String t = l.trim();
                    return !(t.startsWith("//") || t.startsWith("*") || t.startsWith("/*"));
                })
                .toList();
    }
}
