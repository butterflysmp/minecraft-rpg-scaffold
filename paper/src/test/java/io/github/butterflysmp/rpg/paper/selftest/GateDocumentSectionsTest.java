package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY GATE DOCUMENT HAS A CARRIED STATE SECTION AND A SECTION SAYING WHAT BEN TYPES (the seat's ruling (a), 2026-11-26).
 * The seat's approval of GATE-broken-bottle named "SAFETY, CARRIED STATE, THE RUN SHEET" in a file that had neither of the
 * last two, and only CC's stop caught it. A section is a markdown HEADING containing {@code CARRIED STATE}, and one
 * containing {@code WHAT BEN TYPES} or {@code RUN SHEET} (the seat: "WHAT BEN TYPES is the run sheet"), any case.
 *
 * <p>The 50 shipped gates' documents are checked. The ones that already lack a section are PINNED EXACTLY and none is
 * edited; any other gate, the next one written included, fails. Mutations: the heading anchor dropped (prose would pass);
 * a name dropped from the grandfathered list; the list check made to skip.
 */
class GateDocumentSectionsTest {

    private static final Pattern CARRIED = Pattern.compile("(?im)^#+ .*CARRIED STATE");
    private static final Pattern BEN = Pattern.compile("(?im)^#+ .*(WHAT BEN TYPES|RUN SHEET)");

    /** What a gate document lacks: "CARRIED STATE", "WHAT BEN TYPES", both joined by +, or "" for neither. */
    static String missing(String text) {
        List<String> m = new ArrayList<>();
        if (!CARRIED.matcher(text).find()) m.add("CARRIED STATE");
        if (!BEN.matcher(text).find()) m.add("WHAT BEN TYPES");
        return String.join("+", m);
    }

    /** The gate documents that lacked a section on 2026-11-26. Edited by NOBODY; a gate leaves this list by gaining both. */
    private static final List<String> GRANDFATHERED = List.of(
            "melee-cell: CARRIED STATE+WHAT BEN TYPES",
            "level: CARRIED STATE+WHAT BEN TYPES",
            "nexus-polish: CARRIED STATE+WHAT BEN TYPES",
            "legacy-a: CARRIED STATE+WHAT BEN TYPES",
            "legacy-b: CARRIED STATE+WHAT BEN TYPES",
            "wither-status: CARRIED STATE+WHAT BEN TYPES",
            "withered-shortbow: CARRIED STATE+WHAT BEN TYPES",
            "sunder: CARRIED STATE+WHAT BEN TYPES",
            "deft-hands: WHAT BEN TYPES",
            "fight-site: CARRIED STATE+WHAT BEN TYPES",
            "fight-rules: CARRIED STATE+WHAT BEN TYPES",
            "fight-down: CARRIED STATE+WHAT BEN TYPES",
            "fight-template: CARRIED STATE+WHAT BEN TYPES",
            "sword-tiers: CARRIED STATE+WHAT BEN TYPES",
            "sword-smithing: CARRIED STATE+WHAT BEN TYPES");

    /** The planted control: the checker sees a missing section, and passes a document that has both, in any case. */
    @Test
    void theCheckerFindsAMissingSectionAndPassesBoth(@TempDir Path dir) throws IOException {
        assertEquals("CARRIED STATE+WHAT BEN TYPES", missing("# GATE\n\nSome prose about the carried state and what Ben types.\n"),
                "prose is not a section: only a heading counts");
        assertEquals("WHAT BEN TYPES", missing("# GATE\n## CARRIED STATE\n"));
        assertEquals("CARRIED STATE", missing("# GATE\n## The run sheet\n"));
        Path planted = dir.resolve("GATE-planted.md");
        Files.writeString(planted, "# GATE\n### Carried state, as run\n## WHAT BEN TYPES — ONE COMMAND\n", StandardCharsets.UTF_8);
        assertEquals("", missing(Files.readString(planted)));
    }

    /** Every shipped gate's document has both sections, or is on the pinned list exactly as it is. */
    @Test
    void everyShippedGateDocumentHasBothOrIsGrandfathered() throws IOException {
        List<String> gates = List.copyOf(ScenarioLoader.load(path -> GateDocumentSectionsTest.class.getClassLoader()
                .getResourceAsStream(path)).gates().keySet());
        assertEquals(51, gates.size(), "control: the 51 shipped gates were listed (true-tooltips, 2026-11-26)");
        List<String> lacking = new ArrayList<>();
        int both = 0;
        for (String g : gates) {
            Path doc = Path.of("..", "docs", "gates", "GATE-" + g + ".md");
            assertTrue(Files.exists(doc), "every shipped gate has its document: " + doc);
            String m = missing(Files.readString(doc, StandardCharsets.UTF_8));
            if (m.isEmpty()) both++; else lacking.add(g + ": " + m);
        }
        assertTrue(both >= 35, "control: the pattern finds both sections in the documents that have them, " + both);
        assertEquals(GRANDFATHERED, lacking, "a gate document lacks a CARRIED STATE or a WHAT BEN TYPES section");
        assertTrue(lacking.stream().noneMatch(l -> l.startsWith("broken-bottle:")), "the newest gate has both");
    }
}
