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
 * W1 (the seat, 2026-11-25), the paper wiring, by reading the source: the snapshot needs a live player, so the rule is
 * pinned in {@code SelfTestWornSlotsTest} and this pins that EVERY inventory restore calls it.
 * <ul>
 *   <li>Captured in the constructor, beside the whole-inventory copy.</li>
 *   <li>Restored and read back at the gate boundary ({@code restoreInventory}) and in the final {@code restore}, which
 *       is the ONE funnel every exit passes (END, every abort, stop, quit, disable: {@code SelfTestRun.end}).</li>
 *   <li>The CAPTURED line at run start and the RESTORED line carry {@code armour=<n> offhand=<0|1>}.</li>
 * </ul>
 */
class SelfTestWornWiringTest {

    private static final Path DIR = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "selftest");

    private static List<String> code(String file) throws IOException {
        return Files.readAllLines(DIR.resolve(file), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    private static int at(List<String> c, String needle, int from) {
        for (int i = Math.max(0, from); i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    @Test
    void everyInventoryRestoreWritesAndReadsBackTheFiveSlots() throws IOException {
        List<String> s = code("SelfTestSnapshot.java");
        int ctor = at(s, "private SelfTestSnapshot(Player p,", 0);
        int copy = at(s, "for (int i = 0; i < raw.length; i++) contents[i] = raw[i] == null ? null : raw[i].clone();", ctor);
        int cap = at(s, "this.worn = SelfTestWornSlots.capture(wornPort(p), ItemStack::clone, SelfTestSnapshot::emptyItem);", ctor);
        assertTrue(ctor > 0 && copy > ctor && cap == copy + 1, "captured beside the whole-inventory copy");

        int boundary = at(s, "String restoreInventory(Player p) {", 0);
        int bSet = at(s, "p.getInventory().setContents(copy);", boundary);
        int bWorn = at(s, "return restoreWorn(p);", boundary);
        assertTrue(boundary > 0 && bSet > boundary && bWorn > bSet, "the gate boundary: the array, then the five by name");

        int fin = at(s, "String restore(Player p, ProfileService profiles,", 0);
        int fSet = at(s, "p.getInventory().setContents(contents);", fin);
        int fWorn = at(s, "String wornLine = restoreWorn(p);", fin);
        int line = at(s, "+ \" \" + wornLine;", fin);
        assertTrue(fin > 0 && fSet > fin && fWorn == fSet + 2 && line > fWorn,
                "the final restore: the array, the held slot, then the five by name, and their fields end the RESTORED line");

        int rw = at(s, "private String restoreWorn(Player p) {", 0);
        assertTrue(rw > 0 && s.get(rw + 1).contains("worn.restore(wornPort(p), ItemStack::clone,")
                && s.get(rw + 2).contains("SelfTestSnapshot::emptyItem, java.util.Objects::equals);")
                && s.get(rw + 3).contains("SelfTestWornSlots.notRestored(differ);"), "written, read back, named");
        assertEquals(2, s.stream().filter(l -> l.contains("restoreWorn(p)")).count(), "exactly the two restore paths");
    }

    @Test
    void theRunLogsTheCapturedLineAndTheBoundaryCarriesTheFields() throws IOException {
        List<String> r = code("SelfTestRun.java");
        int start = at(r, "service.log(\"SELFTEST RUN START \" + what", 0);
        int captured = at(r, "service.log(\"SELFTEST RUN CAPTURED \" + snapshot.capturedFields());", start);
        assertTrue(start > 0 && captured > start, "CAPTURED follows START");
        int b = at(r, "String worn = gateBoundary ? snapshot.restoreInventory(player) : null;", 0);
        assertTrue(b > 0 && at(r, "+ (worn == null ? \"\" : \" \" + worn)", b) > b, "the boundary's CLEAN line carries them");
        assertEquals(1, r.stream().filter(l -> l.contains("snapshot.restore(player,")).count(), "one final restore, in end()");
    }
}
